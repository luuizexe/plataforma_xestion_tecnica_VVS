package gal.udc.fic.prperez.pleste.service.integration;

import gal.udc.fic.prperez.pleste.service.Application;
import gal.udc.fic.prperez.pleste.service.TemplateResource;
import gal.udc.fic.prperez.pleste.service.UsersResource;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Integration Tests: authorization over the real HTTP endpoints")
public class AuthorizationIT {

	private static final String PASSWORD = "secret";
	private static String passwordHash;

	@LocalServerPort
	private int port;

	@Autowired
	private UsersResource usersResource;

	@Autowired
	private TemplateResource templateResource;

	private ApiTestClient api;
	private final List<Long> createdUserIds = new ArrayList<>();
	private final List<Long> createdTemplateIds = new ArrayList<>();

	private String adminToken;
	private String normalToken;
	private Long normalId;
	private Long otherId;

	@BeforeAll
	static void hashPassword() {
		passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
	}

	@BeforeEach
	void setUp() {
		api = new ApiTestClient(port);
		adminToken = api.login("root", "root");

		String normalName = unique("normal");
		normalId = createUser(normalName);
		normalToken = api.login(normalName, PASSWORD);

		otherId = createUser(unique("other"));
	}

	@AfterEach
	void tearDown() {
		for (Long id : createdTemplateIds) {
			try {
				templateResource.removeTemplate(id.toString());
			} catch (Exception ignored) {
			}
		}
		for (Long id : createdUserIds) {
			try {
				usersResource.deleteUser(id.toString());
			} catch (Exception ignored) {
			}
		}
		createdTemplateIds.clear();
		createdUserIds.clear();
	}

	private static String unique(String prefix) {
		return prefix + "_" + System.nanoTime();
	}

	private Long createUser(String username) {
		Long id = usersResource.addUser(new User(null, username, username + "@udc.es", passwordHash, Roles.NORMAL_USER));
		createdUserIds.add(id);
		return id;
	}

	private static Map<String, Object> newUserBody() {
		String username = unique("created");
		return Map.of("username", username, "email", username + "@udc.es", "password", passwordHash, "role", "NORMAL_USER");
	}

	private static Map<String, Object> newTemplateBody() {
		return Map.of("name", unique("AuthTemplate"), "description", "Template for authorization tests", "fields", List.of());
	}

	// ==========================================
	// Without session
	// ==========================================

	@ParameterizedTest
	@CsvSource({
			"GET, /templates",
			"GET, /components",
			"GET, /users",
			"POST, /users",
			"DELETE, /users/1",
			"GET, /search/templates?query=name"
	})
	@DisplayName("A request without token answers 401 Unauthorized")
	void testRequestWithoutToken(String method, String path) {
		assertEquals(401, api.status(method, path, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "unknown-token", "0123456789abcdef0123456789abcdef01234567"})
	@DisplayName("A request with an empty or unknown token answers 401 Unauthorized")
	void testRequestWithInvalidToken(String token) {
		assertEquals(401, api.status("GET", "/templates", token));
	}

	@Test
	@DisplayName("Login is reachable without token")
	void testLoginWithoutToken() {
		assertEquals(200, api.status("POST", "/users/login?user=root", null, Map.of("content", "root")));
	}

	@Test
	@DisplayName("The OpenAPI document is reachable without token")
	void testOpenApiWithoutToken() {
		assertEquals(200, api.status("GET", "/openapi.json", null));
	}

	// ==========================================
	// Normal user
	// ==========================================

	@ParameterizedTest
	@ValueSource(strings = {"/templates", "/components", "/users"})
	@DisplayName("A normal user can list templates, components and users")
	void testNormalUserCanList(String path) {
		assertEquals(200, api.status("GET", path, normalToken));
	}

	@Test
	@DisplayName("A normal user can read another user")
	void testNormalUserCanReadUser() {
		assertEquals(200, api.status("GET", "/users/" + otherId, normalToken));
	}

	@Test
	@DisplayName("A user read by a normal user does not contain the password hash")
	void testNormalUserDoesNotSeePasswordHash() {
		HttpResponse<String> response = api.send("GET", "/users/" + otherId, normalToken, null);
		assertFalse(response.body().contains("password"));
	}

	@Test
	@DisplayName("A normal user cannot create templates (403 Forbidden)")
	void testNormalUserCannotCreateTemplate() {
		assertEquals(403, api.status("POST", "/templates", normalToken, newTemplateBody()));
	}

	@Test
	@DisplayName("A normal user cannot create users (403 Forbidden)")
	void testNormalUserCannotCreateUser() {
		assertEquals(403, api.status("POST", "/users", normalToken, newUserBody()));
	}

	@Test
	@DisplayName("A normal user cannot promote itself to administrator (403 Forbidden)")
	void testNormalUserCannotPromoteItself() {
		assertEquals(403, api.status("PUT", "/users/" + normalId + "/role", normalToken, Roles.ADMINISTRATOR));
	}

	@Test
	@DisplayName("After a forbidden promotion the role of the normal user is unchanged")
	void testNormalUserRoleUnchangedAfterForbiddenPromotion() {
		api.status("PUT", "/users/" + normalId + "/role", normalToken, Roles.ADMINISTRATOR);
		assertEquals(Roles.NORMAL_USER, usersResource.getUserRole(normalId.toString()));
	}

	@Test
	@DisplayName("A normal user cannot delete another user (403 Forbidden)")
	void testNormalUserCannotDeleteUser() {
		assertEquals(403, api.status("DELETE", "/users/" + otherId, normalToken));
	}

	@Test
	@DisplayName("After a forbidden deletion the other user still exists")
	void testUserStillExistsAfterForbiddenDeletion() {
		api.status("DELETE", "/users/" + otherId, normalToken);
		assertDoesNotThrow(() -> usersResource.getUser(otherId.toString()));
	}

	@Test
	@DisplayName("A normal user can log out")
	void testNormalUserCanLogout() {
		assertEquals(204, api.status("POST", "/users/" + normalId + "/logout", normalToken, Map.of("content", normalToken)));
	}

	// ==========================================
	// Administrator
	// ==========================================

	@Test
	@DisplayName("An administrator can create users")
	void testAdministratorCanCreateUser() {
		HttpResponse<String> response = api.send("POST", "/users", adminToken, newUserBody());
		createdUserIds.add(Long.valueOf(response.body()));
		assertEquals(200, response.statusCode());
	}

	@Test
	@DisplayName("An administrator can change the role of a user")
	void testAdministratorCanChangeRole() {
		assertEquals(204, api.status("PUT", "/users/" + otherId + "/role", adminToken, Roles.ADMINISTRATOR));
	}

	@Test
	@DisplayName("A role changed by an administrator is persisted")
	void testRoleChangedByAdministratorIsPersisted() {
		api.status("PUT", "/users/" + otherId + "/role", adminToken, Roles.ADMINISTRATOR);
		assertEquals(Roles.ADMINISTRATOR, usersResource.getUserRole(otherId.toString()));
	}

	@Test
	@DisplayName("An administrator can delete a user")
	void testAdministratorCanDeleteUser() {
		assertEquals(204, api.status("DELETE", "/users/" + otherId, adminToken));
	}

	@Test
	@DisplayName("An administrator can delete a user that has an open session")
	void testAdministratorCanDeleteUserWithSession() {
		assertEquals(204, api.status("DELETE", "/users/" + normalId, adminToken));
	}

	@Test
	@DisplayName("An administrator can create templates")
	void testAdministratorCanCreateTemplate() {
		HttpResponse<String> response = api.send("POST", "/templates", adminToken, newTemplateBody());
		createdTemplateIds.add(Long.valueOf(response.body()));
		assertEquals(200, response.statusCode());
	}
}
