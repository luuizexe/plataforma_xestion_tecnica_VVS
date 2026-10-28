package gal.udc.fic.prperez.pleste.service;

import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLTokenDao;
import gal.udc.fic.prperez.pleste.service.dao.users.Token;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.RESTException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthorizationManager Unit Tests with Mocks")
public class AuthorizationManagerTest {

	private static final String ADMIN_TOKEN = "admin-token";
	private static final String NORMAL_TOKEN = "normal-token";
	private static final String NO_ROLE_TOKEN = "no-role-token";

	@Mock
	private SQLTokenDao tokenDatabase;

	@Mock
	private ContainerRequestContext request;

	@Mock
	private UriInfo uriInfo;

	private AuthorizationManager filter;

	@BeforeEach
	void setUp() {
		givenToken(ADMIN_TOKEN, Roles.ADMINISTRATOR);
		givenToken(NORMAL_TOKEN, Roles.NORMAL_USER);
		givenToken(NO_ROLE_TOKEN, null);
		when(request.getUriInfo()).thenReturn(uriInfo);

		filter = new AuthorizationManager(tokenDatabase);
	}

	private void givenToken(String value, Roles role) {
		User user = new User(1L, "user-" + value, value + "@udc.es", "hash", role);
		when(tokenDatabase.existsByToken(value)).thenReturn(true);
		when(tokenDatabase.getByToken(value)).thenReturn(new Token(1L, user, value));
	}

	private Executable request(String token, String method, String path) {
		when(request.getHeaderString("X-API-KEY")).thenReturn(token);
		when(request.getMethod()).thenReturn(method);
		when(uriInfo.getPath()).thenReturn(path);
		return () -> filter.filter(request);
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

	// ==========================================
	// Requests without session
	// ==========================================

	@ParameterizedTest
	@NullAndEmptySource
	@DisplayName("Login is allowed without token")
	void testLoginWithoutToken(String token) {
		assertDoesNotThrow(request(token, "POST", "users/login"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"unknown", NORMAL_TOKEN, ADMIN_TOKEN})
	@DisplayName("Login is allowed with any token, even an expired one")
	void testLoginWithAnyToken(String token) {
		assertDoesNotThrow(request(token, "POST", "users/login"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@DisplayName("The OpenAPI document is allowed without token")
	void testOpenApiWithoutToken(String token) {
		assertDoesNotThrow(request(token, "GET", "openapi.json"));
	}

	@ParameterizedTest
	@CsvSource({
			"GET, templates",
			"GET, components/3",
			"GET, users",
			"POST, users",
			"DELETE, users/3",
			"GET, search/templates",
			"GET, users/login/extra"
	})
	@DisplayName("Any other request without token answers 401 Unauthorized")
	void testRequestWithoutToken(String method, String path) {
		assertEquals(401, statusOf(request(null, method, path)));
	}

	@Test
	@DisplayName("A request with an empty token answers 401 Unauthorized")
	void testRequestWithEmptyToken() {
		assertEquals(401, statusOf(request("", "GET", "templates")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"unknown", "ADMIN-TOKEN", "admin-token "})
	@DisplayName("A request with an unknown token answers 401 Unauthorized")
	void testRequestWithUnknownToken(String token) {
		assertEquals(401, statusOf(request(token, "GET", "templates")));
	}

	// ==========================================
	// Administrator
	// ==========================================

	@ParameterizedTest
	@CsvSource({
			"GET, templates",
			"POST, templates",
			"PUT, templates/3",
			"DELETE, templates/3",
			"GET, users",
			"POST, users",
			"POST, users/3/",
			"PUT, users/3/role",
			"DELETE, users/3",
			"POST, users/3/logout",
			"POST, components",
			"DELETE, components/3"
	})
	@DisplayName("An administrator can do any request")
	void testAdministratorAllowed(String method, String path) {
		assertDoesNotThrow(request(ADMIN_TOKEN, method, path));
	}

	// ==========================================
	// Normal user
	// ==========================================

	@ParameterizedTest
	@ValueSource(strings = {"templates", "templates/3", "templates/find"})
	@DisplayName("A normal user can read templates")
	void testNormalUserReadsTemplates(String path) {
		assertDoesNotThrow(request(NORMAL_TOKEN, "GET", path));
	}

	@ParameterizedTest
	@CsvSource({
			"POST, templates",
			"PUT, templates/3",
			"DELETE, templates/3",
			"POST, templates/3/fields"
	})
	@DisplayName("A normal user cannot modify templates (403 Forbidden)")
	void testNormalUserCannotModifyTemplates(String method, String path) {
		assertEquals(403, statusOf(request(NORMAL_TOKEN, method, path)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"users", "users/3", "users/3/role", "users/find"})
	@DisplayName("A normal user can read users")
	void testNormalUserReadsUsers(String path) {
		assertDoesNotThrow(request(NORMAL_TOKEN, "GET", path));
	}

	@Test
	@DisplayName("A normal user can log out")
	void testNormalUserLogout() {
		assertDoesNotThrow(request(NORMAL_TOKEN, "POST", "users/3/logout"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"users", "users/3/", "users/3"})
	@DisplayName("A normal user cannot create or modify users (403 Forbidden)")
	void testNormalUserCannotPostUsers(String path) {
		assertEquals(403, statusOf(request(NORMAL_TOKEN, "POST", path)));
	}

	@Test
	@DisplayName("A normal user cannot change roles (403 Forbidden)")
	void testNormalUserCannotChangeRole() {
		assertEquals(403, statusOf(request(NORMAL_TOKEN, "PUT", "users/1/role")));
	}

	@Test
	@DisplayName("A normal user cannot delete users (403 Forbidden)")
	void testNormalUserCannotDeleteUsers() {
		assertEquals(403, statusOf(request(NORMAL_TOKEN, "DELETE", "users/3")));
	}

	@ParameterizedTest
	@CsvSource({
			"GET, components",
			"POST, components",
			"PUT, components/3",
			"DELETE, components/3",
			"GET, search/components"
	})
	@DisplayName("A normal user can manage components and search")
	void testNormalUserComponents(String method, String path) {
		assertDoesNotThrow(request(NORMAL_TOKEN, method, path));
	}

	// ==========================================
	// User without role
	// ==========================================

	@Test
	@DisplayName("A user without role can read templates like a normal user")
	void testNoRoleReadsTemplates() {
		assertDoesNotThrow(request(NO_ROLE_TOKEN, "GET", "templates"));
	}

	@Test
	@DisplayName("A user without role cannot modify templates (403 Forbidden)")
	void testNoRoleCannotModifyTemplates() {
		assertEquals(403, statusOf(request(NO_ROLE_TOKEN, "POST", "templates")));
	}

	@Test
	@DisplayName("A user without role cannot change roles (403 Forbidden)")
	void testNoRoleCannotChangeRole() {
		assertEquals(403, statusOf(request(NO_ROLE_TOKEN, "PUT", "users/1/role")));
	}
}
