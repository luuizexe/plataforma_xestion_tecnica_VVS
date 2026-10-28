package gal.udc.fic.prperez.pleste.service.integration;

import gal.udc.fic.prperez.pleste.service.Application;
import gal.udc.fic.prperez.pleste.service.JSONString;
import gal.udc.fic.prperez.pleste.service.UsersResource;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLTokenDao;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.authentication.UserAlreadyExistsException;
import gal.udc.fic.prperez.pleste.service.exceptions.authentication.UserNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@DisplayName("Integration Tests: UsersResource over the real database")
public class UsersResourceIT {

	private static final String PASSWORD = "secret";
	private static String passwordHash;

	@Autowired
	private UsersResource usersResource;

	@Autowired
	private SQLTokenDao tokenDatabase;

	private final List<Long> createdUserIds = new ArrayList<>();
	private Long aliceId;

	@BeforeAll
	static void hashPassword() {
		passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
	}

	@BeforeEach
	void setUp() {
		aliceId = createUser("alice");
	}

	@AfterEach
	void tearDown() {
		for (Long id : createdUserIds) {
			try {
				usersResource.deleteUser(id.toString());
			} catch (Exception ignored) {
			}
		}
		createdUserIds.clear();
	}

	private static String unique(String prefix) {
		return prefix + "_" + System.nanoTime();
	}

	private Long createUser(String prefix) {
		String username = unique(prefix);
		Long id = usersResource.addUser(new User(null, username, username + "@udc.es", passwordHash, Roles.NORMAL_USER));
		createdUserIds.add(id);
		return id;
	}

	private String usernameOf(Long id) {
		return usersResource.getUser(id.toString()).getUsername();
	}

	private String login(Long id) {
		return usersResource.login(usernameOf(id), new JSONString(PASSWORD)).toString();
	}

	// ==========================================
	// Creation and lookup
	// ==========================================

	@Test
	@DisplayName("A created user can be read back by id")
	void testCreatedUserCanBeRead() {
		assertTrue(usernameOf(aliceId).startsWith("alice_"));
	}

	@Test
	@DisplayName("A created user can be found by name")
	void testCreatedUserCanBeFoundByName() {
		assertEquals(aliceId, usersResource.getUserByName(usernameOf(aliceId)));
	}

	@Test
	@DisplayName("Creating a user with an existing name throws UserAlreadyExistsException")
	void testCreateDuplicatedUser() {
		User duplicated = new User(null, usernameOf(aliceId), "other@udc.es", passwordHash, Roles.NORMAL_USER);
		assertThrows(UserAlreadyExistsException.class, () -> usersResource.addUser(duplicated));
	}

	@Test
	@DisplayName("A created user appears in the list of users")
	void testCreatedUserIsListed() {
		assertTrue(usersResource.getAllUsers(null, null).contains(aliceId));
	}

	// ==========================================
	// Pagination over the real database
	// ==========================================

	@Test
	@DisplayName("Paginating with skip 1 and count 1 returns the second user ordered by id")
	void testPaginationSecondUser() {
		createUser("bob");
		List<Long> all = usersResource.getAllUsers(null, null).stream().sorted().toList();
		assertEquals(all.subList(1, 2), usersResource.getAllUsers(1, 1));
	}

	@Test
	@DisplayName("Paginating past the end with a big count returns the remaining users")
	void testPaginationRemainingUsers() {
		createUser("bob");
		List<Long> all = usersResource.getAllUsers(null, null).stream().sorted().toList();
		assertEquals(all.subList(all.size() - 1, all.size()), usersResource.getAllUsers(all.size() - 1, 100));
	}

	@Test
	@DisplayName("Consecutive pages do not overlap nor leave gaps")
	void testPaginationConsecutivePages() {
		createUser("bob");
		createUser("carol");
		List<Long> all = usersResource.getAllUsers(null, null).stream().sorted().toList();
		List<Long> paged = new ArrayList<>();
		for (int skip = 0; skip < all.size(); skip += 2) {
			paged.addAll(usersResource.getAllUsers(skip, 2));
		}
		assertEquals(all, paged);
	}

	// ==========================================
	// Modification
	// ==========================================

	@Test
	@DisplayName("Modifying the username is persisted")
	void testModifyUsername() {
		String newName = unique("alice_renamed");
		usersResource.setUser(aliceId.toString(), new User(null, newName, "alice@udc.es", null, Roles.NORMAL_USER));
		assertEquals(newName, usernameOf(aliceId));
	}

	@Test
	@DisplayName("Modifying a user without password keeps the old password valid")
	void testModifyWithoutPasswordKeepsLogin() {
		String newName = unique("alice_renamed");
		usersResource.setUser(aliceId.toString(), new User(null, newName, "alice@udc.es", null, Roles.NORMAL_USER));
		assertNotNull(usersResource.login(newName, new JSONString(PASSWORD)));
	}

	@ParameterizedTest
	@EnumSource(Roles.class)
	@DisplayName("Changing the role is persisted")
	void testChangeRole(Roles role) {
		usersResource.setUserRole(aliceId.toString(), role);
		assertEquals(role, usersResource.getUserRole(aliceId.toString()));
	}

	// ==========================================
	// Deletion
	// ==========================================

	@Test
	@DisplayName("A deleted user can no longer be read")
	void testDeletedUserNotFound() {
		usersResource.deleteUser(aliceId.toString());
		assertThrows(UserNotFoundException.class, () -> usersResource.getUser(aliceId.toString()));
	}

	@Test
	@DisplayName("A user with an open session can be deleted")
	void testDeleteUserWithSession() {
		login(aliceId);
		usersResource.deleteUser(aliceId.toString());
		assertThrows(UserNotFoundException.class, () -> usersResource.getUser(aliceId.toString()));
	}

	@Test
	@DisplayName("Deleting a user removes its session tokens")
	void testDeleteUserRemovesTokens() {
		String token = login(aliceId);
		try {
			usersResource.deleteUser(aliceId.toString());
		} catch (Exception ignored) {
		}
		assertFalse(tokenDatabase.existsByToken(token));
	}

	// ==========================================
	// Login and logout
	// ==========================================

	@Test
	@DisplayName("Login stores the returned token in the database")
	void testLoginStoresToken() {
		assertTrue(tokenDatabase.existsByToken(login(aliceId)));
	}

	@Test
	@DisplayName("Login with a wrong password throws UserNotFoundException")
	void testLoginWrongPassword() {
		String username = usernameOf(aliceId);
		assertThrows(UserNotFoundException.class, () -> usersResource.login(username, new JSONString("wrong")));
	}

	@Test
	@DisplayName("Logout removes the token from the database")
	void testLogoutRemovesToken() {
		String token = login(aliceId);
		usersResource.logout(aliceId.toString(), new JSONString(token));
		assertFalse(tokenDatabase.existsByToken(token));
	}

	@Test
	@DisplayName("Logout with the token of another user throws UserNotFoundException")
	void testLogoutWithTokenOfAnotherUser() {
		Long bobId = createUser("bob");
		String bobToken = login(bobId);
		assertThrows(UserNotFoundException.class, () -> usersResource.logout(aliceId.toString(), new JSONString(bobToken)));
	}

	@Test
	@DisplayName("Logout with the token of another user keeps that token")
	void testLogoutWithTokenOfAnotherUserKeepsToken() {
		Long bobId = createUser("bob");
		String bobToken = login(bobId);
		try {
			usersResource.logout(aliceId.toString(), new JSONString(bobToken));
		} catch (UserNotFoundException ignored) {
		}
		assertTrue(tokenDatabase.existsByToken(bobToken));
	}
}
