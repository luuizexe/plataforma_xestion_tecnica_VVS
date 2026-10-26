package gal.udc.fic.prperez.pleste.service;

import gal.udc.fic.prperez.pleste.service.dao.SQLDaoFactoryUtil;
import gal.udc.fic.prperez.pleste.service.dao.users.*;
import gal.udc.fic.prperez.pleste.service.exceptions.RESTException;
import gal.udc.fic.prperez.pleste.service.exceptions.authentication.UserAlreadyExistsException;
import gal.udc.fic.prperez.pleste.service.exceptions.authentication.UserNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsersResource Unit Tests with Mocks")
public class UsersResourceTest {

	private static final String PASSWORD = "secret";
	private static final String TOO_BIG_ID = "9223372036854775808"; // Long.MAX_VALUE + 1
	private static String passwordHash;

	@Mock
	private SQLUserDao userDatabase;

	@Mock
	private SQLTokenDao tokenDatabase;

	private UsersResource resource;

	@BeforeAll
	static void hashPassword() {
		passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
	}

	@BeforeEach
	void setUp() {
		SQLDaoFactoryUtil databaseFactory = new SQLDaoFactoryUtil(null, null, null, null, userDatabase, tokenDatabase);
		resource = new UsersResource(userDatabase, databaseFactory);
	}

	private static User user(long id, String username) {
		return new User(id, username, username + "@udc.es", passwordHash, Roles.NORMAL_USER);
	}

	/** Returns the HTTP status of the RESTException thrown by the action, or null if it threw something else. */
	private static Integer statusOf(Executable action) {
		try {
			action.execute();
		} catch (RESTException e) {
			return e.getStatus();
		} catch (Throwable ignored) {
		}
		return null;
	}

	/** Makes the mocked DAO behave like a table holding users with ids 1..total, ordered by id. */
	private void givenPagedUsers(int total) {
		List<User> all = LongStream.rangeClosed(1, total).mapToObj(id -> user(id, "user" + id)).toList();
		when(userDatabase.findAll(any(Pageable.class))).thenAnswer(invocation -> {
			Pageable page = invocation.getArgument(0);
			int from = (int) Math.min(page.getOffset(), all.size());
			int to = (int) Math.min(page.getOffset() + page.getPageSize(), all.size());
			return new PageImpl<>(all.subList(from, to), page, all.size());
		});
	}

	private void givenExistingUser(User user) {
		when(userDatabase.existsByUsername(user.getUsername())).thenReturn(true);
		when(userDatabase.getByUsername(user.getUsername())).thenReturn(user);
	}

	// ==========================================
	// getAllUsers
	// ==========================================

	@Test
	@DisplayName("getAllUsers without count returns every user id")
	void testGetAllUsersWithoutCount() {
		when(userDatabase.findAll()).thenReturn(List.of(user(1, "a"), user(2, "b"), user(3, "c")));
		assertEquals(List.of(1L, 2L, 3L), resource.getAllUsers(null, null));
	}

	@Test
	@DisplayName("getAllUsers without count on an empty table returns an empty list")
	void testGetAllUsersWithoutCountEmpty() {
		when(userDatabase.findAll()).thenReturn(List.of());
		assertTrue(resource.getAllUsers(null, null).isEmpty());
	}

	@Test
	@DisplayName("getAllUsers with count and no skip starts at the first user")
	void testGetAllUsersSkipDefaultsToZero() {
		givenPagedUsers(6);
		assertEquals(List.of(1L, 2L), resource.getAllUsers(null, 2));
	}

	@Test
	@DisplayName("getAllUsers with skip 0 returns the first users")
	void testGetAllUsersSkipZero() {
		givenPagedUsers(6);
		assertEquals(List.of(1L, 2L), resource.getAllUsers(0, 2));
	}

	@Test
	@DisplayName("getAllUsers skips the given number of users, not pages")
	void testGetAllUsersSkipIsAnOffset() {
		givenPagedUsers(6);
		assertEquals(List.of(3L, 4L), resource.getAllUsers(2, 2));
	}

	@Test
	@DisplayName("getAllUsers returns the last partial page")
	void testGetAllUsersLastPartialPage() {
		givenPagedUsers(6);
		assertEquals(List.of(6L), resource.getAllUsers(5, 2));
	}

	@Test
	@DisplayName("getAllUsers past the end returns an empty list")
	void testGetAllUsersPastTheEnd() {
		givenPagedUsers(6);
		assertTrue(resource.getAllUsers(6, 2).isEmpty());
	}

	@Test
	@DisplayName("getAllUsers with a random skip and count 1 returns the user at that position")
	void testGetAllUsersRandomSkip() {
		givenPagedUsers(20);
		int skip = new Random().nextInt(20);
		assertEquals(List.of(skip + 1L), resource.getAllUsers(skip, 1));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1, Integer.MIN_VALUE})
	@DisplayName("getAllUsers with a count lower than 1 is a bad request")
	void testGetAllUsersInvalidCount(int count) {
		assertEquals(400, statusOf(() -> resource.getAllUsers(0, count)));
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, Integer.MIN_VALUE})
	@DisplayName("getAllUsers with a negative skip is a bad request")
	void testGetAllUsersNegativeSkip(int skip) {
		assertEquals(400, statusOf(() -> resource.getAllUsers(skip, 2)));
	}

	// ==========================================
	// addUser
	// ==========================================

	@Test
	@DisplayName("addUser returns the id given by the database")
	void testAddUserReturnsId() {
		when(userDatabase.save(any(User.class))).thenAnswer(invocation -> {
			User saved = invocation.getArgument(0);
			saved.setId(10L);
			return saved;
		});
		assertEquals(10L, resource.addUser(new User(null, "alice", "alice@udc.es", passwordHash, Roles.NORMAL_USER)));
	}

	@Test
	@DisplayName("addUser with an existing username throws UserAlreadyExistsException")
	void testAddUserDuplicated() {
		when(userDatabase.existsByUsername("alice")).thenReturn(true);
		assertThrows(UserAlreadyExistsException.class, () -> resource.addUser(user(1, "alice")));
	}

	@Test
	@DisplayName("addUser with an existing username answers 409 Conflict as documented")
	void testAddUserDuplicatedIsConflict() {
		when(userDatabase.existsByUsername("alice")).thenReturn(true);
		assertEquals(409, statusOf(() -> resource.addUser(user(1, "alice"))));
	}

	@Test
	@DisplayName("addUser with an existing username does not save anything")
	void testAddUserDuplicatedDoesNotSave() {
		when(userDatabase.existsByUsername("alice")).thenReturn(true);
		statusOf(() -> resource.addUser(user(1, "alice")));
		verify(userDatabase, never()).save(any());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "\t"})
	@DisplayName("addUser without a username is a bad request")
	void testAddUserWithoutUsername(String username) {
		lenient().when(userDatabase.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		assertEquals(400, statusOf(() -> resource.addUser(new User(null, username, "x@udc.es", passwordHash, Roles.NORMAL_USER))));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@DisplayName("addUser without a password is a bad request")
	void testAddUserWithoutPassword(String password) {
		lenient().when(userDatabase.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		assertEquals(400, statusOf(() -> resource.addUser(new User(null, "alice", "x@udc.es", password, Roles.NORMAL_USER))));
	}

	// ==========================================
	// login
	// ==========================================

	@RepeatedTest(10)
	@DisplayName("login returns a 40 character hexadecimal token")
	void testLoginTokenFormat() {
		givenExistingUser(user(1, "alice"));
		when(tokenDatabase.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));
		assertTrue(resource.login("alice", new JSONString(PASSWORD)).toString().matches("^[0-9a-f]{40}$"));
	}

	@Test
	@DisplayName("login stores the token for the logged user")
	void testLoginStoresTokenForUser() {
		User alice = user(1, "alice");
		givenExistingUser(alice);
		when(tokenDatabase.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Token> saved = ArgumentCaptor.forClass(Token.class);

		resource.login("alice", new JSONString(PASSWORD));

		verify(tokenDatabase).save(saved.capture());
		assertSame(alice, saved.getValue().getUser());
	}

	@Test
	@DisplayName("login returns the token that was stored")
	void testLoginReturnsStoredToken() {
		givenExistingUser(user(1, "alice"));
		when(tokenDatabase.save(any(Token.class))).thenReturn(new Token(5L, null, "stored-token"));
		assertEquals("stored-token", resource.login("alice", new JSONString(PASSWORD)).toString());
	}

	@Test
	@DisplayName("Two logins of the same user return different tokens")
	void testLoginTokensAreDifferent() {
		givenExistingUser(user(1, "alice"));
		when(tokenDatabase.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));
		String first = resource.login("alice", new JSONString(PASSWORD)).toString();
		assertNotEquals(first, resource.login("alice", new JSONString(PASSWORD)).toString());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "Secret", "secret ", "wrong"})
	@DisplayName("login with a wrong password throws UserNotFoundException")
	void testLoginWrongPassword(String password) {
		givenExistingUser(user(1, "alice"));
		assertThrows(UserNotFoundException.class, () -> resource.login("alice", new JSONString(password)));
	}

	@Test
	@DisplayName("login with a wrong password does not create a token")
	void testLoginWrongPasswordNoToken() {
		givenExistingUser(user(1, "alice"));
		statusOf(() -> resource.login("alice", new JSONString("wrong")));
		verify(tokenDatabase, never()).save(any());
	}

	@Test
	@DisplayName("login with an unknown user throws UserNotFoundException")
	void testLoginUnknownUser() {
		assertThrows(UserNotFoundException.class, () -> resource.login("ghost", new JSONString(PASSWORD)));
	}

	@Test
	@DisplayName("login without username throws UserNotFoundException")
	void testLoginNullUsername() {
		assertThrows(UserNotFoundException.class, () -> resource.login(null, new JSONString(PASSWORD)));
	}

	@Test
	@DisplayName("login without body is a bad request")
	void testLoginWithoutBody() {
		lenient().when(userDatabase.existsByUsername("alice")).thenReturn(true);
		lenient().when(userDatabase.getByUsername("alice")).thenReturn(user(1, "alice"));
		assertEquals(400, statusOf(() -> resource.login("alice", null)));
	}

	@Test
	@DisplayName("login with a null password is a bad request")
	void testLoginNullPassword() {
		lenient().when(userDatabase.existsByUsername("alice")).thenReturn(true);
		lenient().when(userDatabase.getByUsername("alice")).thenReturn(user(1, "alice"));
		assertEquals(400, statusOf(() -> resource.login("alice", new JSONString(null))));
	}

	// ==========================================
	// getUserByName
	// ==========================================

	@Test
	@DisplayName("getUserByName returns the id of an existing user")
	void testGetUserByName() {
		givenExistingUser(user(4, "alice"));
		assertEquals(4L, resource.getUserByName("alice"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"ghost"})
	@DisplayName("getUserByName with an unknown name throws UserNotFoundException")
	void testGetUserByNameUnknown(String name) {
		assertThrows(UserNotFoundException.class, () -> resource.getUserByName(name));
	}

	// ==========================================
	// getUser
	// ==========================================

	@Test
	@DisplayName("getUser returns the stored user")
	void testGetUser() {
		User alice = user(1, "alice");
		when(userDatabase.findById(1L)).thenReturn(Optional.of(alice));
		assertSame(alice, resource.getUser("1"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "1", "9223372036854775807"})
	@DisplayName("getUser with an unknown id throws UserNotFoundException")
	void testGetUserUnknown(String id) {
		assertThrows(UserNotFoundException.class, () -> resource.getUser(id));
	}

	@Test
	@DisplayName("getUser with an id bigger than a long throws UserNotFoundException")
	void testGetUserIdOverflow() {
		assertThrows(UserNotFoundException.class, () -> resource.getUser(TOO_BIG_ID));
	}

	// ==========================================
	// setUser
	// ==========================================

	private void givenStoredUser(User stored) {
		lenient().when(userDatabase.existsById(stored.getId())).thenReturn(true);
		lenient().when(userDatabase.findById(stored.getId())).thenReturn(Optional.of(stored));
		lenient().when(userDatabase.getReferenceById(stored.getId())).thenReturn(stored);
		lenient().when(userDatabase.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private User savedUser() {
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userDatabase).save(saved.capture());
		return saved.getValue();
	}

	@Test
	@DisplayName("setUser saves the user with the id of the path")
	void testSetUserUsesPathId() {
		givenStoredUser(user(3, "alice"));
		resource.setUser("3", new User(99L, "bob", "bob@udc.es", passwordHash, Roles.NORMAL_USER));
		assertEquals(3L, savedUser().getId());
	}

	@Test
	@DisplayName("setUser saves the new username")
	void testSetUserUpdatesUsername() {
		givenStoredUser(user(3, "alice"));
		resource.setUser("3", new User(null, "bob", "bob@udc.es", passwordHash, Roles.NORMAL_USER));
		assertEquals("bob", savedUser().getUsername());
	}

	@Test
	@DisplayName("setUser with a new password stores the new password")
	void testSetUserNewPassword() {
		givenStoredUser(user(3, "alice"));
		resource.setUser("3", new User(null, "alice", "alice@udc.es", "newHash", Roles.NORMAL_USER));
		assertEquals("newHash", savedUser().getPassword());
	}

	@Test
	@DisplayName("setUser without password keeps the stored password")
	void testSetUserWithoutPasswordKeepsPassword() {
		givenStoredUser(user(3, "alice"));
		resource.setUser("3", new User(null, "alice", "alice@udc.es", null, Roles.NORMAL_USER));
		assertEquals(passwordHash, savedUser().getPassword());
	}

	@Test
	@DisplayName("setUser with an unknown id throws UserNotFoundException")
	void testSetUserUnknown() {
		assertThrows(UserNotFoundException.class, () -> resource.setUser("3", user(3, "alice")));
	}

	@Test
	@DisplayName("setUser with an unknown id does not save anything")
	void testSetUserUnknownDoesNotSave() {
		statusOf(() -> resource.setUser("3", user(3, "alice")));
		verify(userDatabase, never()).save(any());
	}

	@Test
	@DisplayName("setUser with an id bigger than a long throws UserNotFoundException")
	void testSetUserIdOverflow() {
		assertThrows(UserNotFoundException.class, () -> resource.setUser(TOO_BIG_ID, user(3, "alice")));
	}

	// ==========================================
	// deleteUser
	// ==========================================

	@Test
	@DisplayName("deleteUser deletes an existing user")
	void testDeleteUser() {
		givenStoredUser(user(3, "alice"));
		resource.deleteUser("3");
		verify(userDatabase).deleteById(3L);
	}

	@Test
	@DisplayName("deleteUser deletes the session tokens of the user")
	void testDeleteUserDeletesTokens() {
		User alice = user(3, "alice");
		List<Token> tokens = List.of(new Token(1L, alice, "abc"), new Token(2L, alice, "def"));
		givenStoredUser(alice);
		when(tokenDatabase.findByUser(alice)).thenReturn(tokens);
		resource.deleteUser("3");
		verify(tokenDatabase).deleteAll(tokens);
	}

	@Test
	@DisplayName("deleteUser with an unknown id throws UserNotFoundException")
	void testDeleteUserUnknown() {
		assertThrows(UserNotFoundException.class, () -> resource.deleteUser("3"));
	}

	@Test
	@DisplayName("deleteUser with an unknown id does not delete anything")
	void testDeleteUserUnknownDoesNotDelete() {
		statusOf(() -> resource.deleteUser("3"));
		verify(userDatabase, never()).deleteById(any());
	}

	@Test
	@DisplayName("deleteUser with an id bigger than a long throws UserNotFoundException")
	void testDeleteUserIdOverflow() {
		assertThrows(UserNotFoundException.class, () -> resource.deleteUser(TOO_BIG_ID));
	}

	// ==========================================
	// getUserRole / setUserRole
	// ==========================================

	@ParameterizedTest
	@EnumSource(Roles.class)
	@DisplayName("getUserRole returns the role of the user")
	void testGetUserRole(Roles role) {
		givenStoredUser(new User(3L, "alice", "alice@udc.es", passwordHash, role));
		assertEquals(role, resource.getUserRole("3"));
	}

	@Test
	@DisplayName("getUserRole with an unknown id throws UserNotFoundException")
	void testGetUserRoleUnknown() {
		assertThrows(UserNotFoundException.class, () -> resource.getUserRole("3"));
	}

	@Test
	@DisplayName("getUserRole with an id bigger than a long throws UserNotFoundException")
	void testGetUserRoleIdOverflow() {
		assertThrows(UserNotFoundException.class, () -> resource.getUserRole(TOO_BIG_ID));
	}

	@ParameterizedTest
	@EnumSource(Roles.class)
	@DisplayName("setUserRole saves the new role")
	void testSetUserRole(Roles role) {
		givenStoredUser(user(3, "alice"));
		resource.setUserRole("3", role);
		assertEquals(role, savedUser().getRole());
	}

	@Test
	@DisplayName("setUserRole with an unknown id throws UserNotFoundException")
	void testSetUserRoleUnknown() {
		assertThrows(UserNotFoundException.class, () -> resource.setUserRole("3", Roles.ADMINISTRATOR));
	}

	// ==========================================
	// logout
	// ==========================================

	private void givenToken(String value, User owner) {
		lenient().when(tokenDatabase.existsByToken(value)).thenReturn(true);
		lenient().when(tokenDatabase.getByToken(value)).thenReturn(new Token(1L, owner, value));
	}

	@Test
	@DisplayName("logout deletes the token of the user")
	void testLogout() {
		givenStoredUser(user(3, "alice"));
		givenToken("abc", user(3, "alice"));
		resource.logout("3", new JSONString("abc"));
		verify(tokenDatabase).deleteByToken("abc");
	}

	@Test
	@DisplayName("logout of an unknown user throws UserNotFoundException")
	void testLogoutUnknownUser() {
		givenToken("abc", user(3, "alice"));
		assertThrows(UserNotFoundException.class, () -> resource.logout("3", new JSONString("abc")));
	}

	@Test
	@DisplayName("logout with an unknown token throws UserNotFoundException")
	void testLogoutUnknownToken() {
		givenStoredUser(user(3, "alice"));
		assertThrows(UserNotFoundException.class, () -> resource.logout("3", new JSONString("abc")));
	}

	@Test
	@DisplayName("logout with the token of another user throws UserNotFoundException")
	void testLogoutTokenOfAnotherUser() {
		givenStoredUser(user(3, "alice"));
		givenToken("abc", user(4, "bob"));
		assertThrows(UserNotFoundException.class, () -> resource.logout("3", new JSONString("abc")));
	}

	@Test
	@DisplayName("logout with the token of another user does not delete it")
	void testLogoutTokenOfAnotherUserNotDeleted() {
		givenStoredUser(user(3, "alice"));
		givenToken("abc", user(4, "bob"));
		statusOf(() -> resource.logout("3", new JSONString("abc")));
		verify(tokenDatabase, never()).deleteByToken(any());
	}
}
