package gal.udc.fic.prperez.pleste.service.integration;

import gal.udc.fic.prperez.pleste.service.Application;
import gal.udc.fic.prperez.pleste.service.UsersResource;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proba as transicións do diagrama de estados da sesión (docs/diagramas/estado-sesion.md):
 * sen sesión → login → con sesión (normal ou administrador) → logout → sen sesión.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Integration Tests: session state transitions")
public class SessionStateIT {

	private static final String PASSWORD = "secret";
	private static String passwordHash;

	@LocalServerPort
	private int port;

	@Autowired
	private UsersResource usersResource;

	private ApiTestClient api;
	private final List<Long> createdUserIds = new ArrayList<>();

	private String adminToken;
	private String username;
	private Long userId;

	@BeforeAll
	static void hashPassword() {
		passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
	}

	@BeforeEach
	void setUp() {
		api = new ApiTestClient(port);
		adminToken = api.login("root", "root");
		username = unique("session");
		userId = createUser(username, Roles.NORMAL_USER);
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

	private Long createUser(String name, Roles role) {
		Long id = usersResource.addUser(new User(null, name, name + "@udc.es", passwordHash, role));
		createdUserIds.add(id);
		return id;
	}

	private int logout(Long id, String token) {
		return api.status("POST", "/users/" + id + "/logout", token, Map.of("content", token));
	}

	/** Petición que só pode facer un administrador: crear un usuario. */
	private int adminOnlyRequest(String token) {
		String name = unique("created");
		HttpResponse<String> response = api.send("POST", "/users", token,
				Map.of("username", name, "email", name + "@udc.es", "password", passwordHash, "role", "NORMAL_USER"));
		if (response.statusCode() == 200) {
			createdUserIds.add(Long.valueOf(response.body()));
		}
		return response.statusCode();
	}

	// ==========================================
	// Sen sesión
	// ==========================================

	@Test
	@DisplayName("Without session a protected request is rejected with 401")
	void testNoSessionProtectedRequest() {
		assertEquals(401, api.status("GET", "/templates", null));
	}

	@Test
	@DisplayName("Without session a login with a wrong password is rejected")
	void testNoSessionWrongLoginStatus() {
		assertEquals(404, api.status("POST", "/users/login?user=" + username, null, Map.of("content", "wrong")));
	}

	@Test
	@DisplayName("Without session a login with a wrong password does not open a session")
	void testNoSessionWrongLoginGivesNoToken() {
		assertNull(api.login(username, "wrong"));
	}

	@Test
	@DisplayName("Login is still possible when the client sends an expired token")
	void testLoginWithExpiredToken() {
		String expired = api.login(username, PASSWORD);
		logout(userId, expired);
		assertEquals(200, api.status("POST", "/users/login?user=" + username, expired, Map.of("content", PASSWORD)));
	}

	// ==========================================
	// Login: sen sesión → con sesión
	// ==========================================

	@Test
	@DisplayName("Login of a normal user opens a session that can read templates")
	void testLoginOpensNormalSession() {
		String token = api.login(username, PASSWORD);
		assertEquals(200, api.status("GET", "/templates", token));
	}

	@Test
	@DisplayName("A normal session cannot do administrator requests")
	void testNormalSessionIsNotAdministrator() {
		String token = api.login(username, PASSWORD);
		assertEquals(403, adminOnlyRequest(token));
	}

	@Test
	@DisplayName("Login of an administrator opens an administrator session")
	void testLoginOpensAdministratorSession() {
		assertEquals(200, adminOnlyRequest(adminToken));
	}

	@Test
	@DisplayName("A forbidden request does not close the session")
	void testForbiddenRequestKeepsSession() {
		String token = api.login(username, PASSWORD);
		adminOnlyRequest(token);
		assertEquals(200, api.status("GET", "/templates", token));
	}

	@Test
	@DisplayName("Two logins of the same user open two independent sessions")
	void testTwoIndependentSessions() {
		String first = api.login(username, PASSWORD);
		String second = api.login(username, PASSWORD);
		logout(userId, first);
		assertEquals(200, api.status("GET", "/templates", second));
	}

	// ==========================================
	// Cambio de rol: con sesión normal ↔ con sesión de administrador
	// ==========================================

	@Test
	@DisplayName("Promoting a logged user turns its session into an administrator session")
	void testPromotionWhileLoggedIn() {
		String token = api.login(username, PASSWORD);
		api.status("PUT", "/users/" + userId + "/role", adminToken, Roles.ADMINISTRATOR);
		assertEquals(200, adminOnlyRequest(token));
	}

	@Test
	@DisplayName("Demoting a logged administrator turns its session into a normal session")
	void testDemotionWhileLoggedIn() {
		String adminName = unique("admin");
		Long adminId = createUser(adminName, Roles.ADMINISTRATOR);
		String token = api.login(adminName, PASSWORD);
		api.status("PUT", "/users/" + adminId + "/role", adminToken, Roles.NORMAL_USER);
		assertEquals(403, adminOnlyRequest(token));
	}

	// ==========================================
	// Logout: con sesión → sen sesión
	// ==========================================

	@Test
	@DisplayName("Logout answers 204 No Content")
	void testLogoutStatus() {
		String token = api.login(username, PASSWORD);
		assertEquals(204, logout(userId, token));
	}

	@Test
	@DisplayName("After logout the token is rejected with 401")
	void testLogoutClosesSession() {
		String token = api.login(username, PASSWORD);
		logout(userId, token);
		assertEquals(401, api.status("GET", "/templates", token));
	}

	@Test
	@DisplayName("After logout an administrator token is rejected with 401")
	void testAdministratorLogoutClosesSession() {
		String adminName = unique("admin");
		Long adminId = createUser(adminName, Roles.ADMINISTRATOR);
		String token = api.login(adminName, PASSWORD);
		logout(adminId, token);
		assertEquals(401, adminOnlyRequest(token));
	}

	@Test
	@DisplayName("Logging out twice with the same token is rejected with 401")
	void testLogoutTwice() {
		String token = api.login(username, PASSWORD);
		logout(userId, token);
		assertEquals(401, logout(userId, token));
	}

	@Test
	@DisplayName("Logout with the id of another user is rejected")
	void testLogoutWithAnotherUserId() {
		Long otherId = createUser(unique("other"), Roles.NORMAL_USER);
		String token = api.login(username, PASSWORD);
		assertEquals(404, logout(otherId, token));
	}

	@Test
	@DisplayName("Logout with the id of another user keeps the session open")
	void testLogoutWithAnotherUserIdKeepsSession() {
		Long otherId = createUser(unique("other"), Roles.NORMAL_USER);
		String token = api.login(username, PASSWORD);
		logout(otherId, token);
		assertEquals(200, api.status("GET", "/templates", token));
	}

	// ==========================================
	// Borrado do usuario: con sesión → sen sesión
	// ==========================================

	@Test
	@DisplayName("Deleting a logged user closes its session")
	void testDeletedUserLosesSession() {
		String token = api.login(username, PASSWORD);
		api.status("DELETE", "/users/" + userId, adminToken);
		assertEquals(401, api.status("GET", "/templates", token));
	}
}
