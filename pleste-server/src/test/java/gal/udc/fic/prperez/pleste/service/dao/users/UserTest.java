package gal.udc.fic.prperez.pleste.service.dao.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Unit Tests")
public class UserTest {

	private static final String LONG_TEXT = "x".repeat(255);
	private static final String BCRYPT_HASH = "$2a$10$abcdefghijklmnopqrstuuJ5sZ2kCwYBpXQ0FqN1y1N3aQ2V5yJ8e";

	private static User fullUser() {
		return new User(7L, "alice", "alice@udc.es", BCRYPT_HASH, Roles.ADMINISTRATOR);
	}

	// ==========================================
	// Constructors
	// ==========================================

	@Test
	@DisplayName("Default constructor leaves id null")
	void testDefaultConstructorIdIsNull() {
		assertNull(new User().getId());
	}

	@Test
	@DisplayName("Default constructor leaves role null")
	void testDefaultConstructorRoleIsNull() {
		assertNull(new User().getRole());
	}

	@Test
	@DisplayName("Id constructor stores the id")
	void testIdConstructorStoresId() {
		assertEquals(42L, new User(42L).getId());
	}

	@Test
	@DisplayName("Id constructor leaves username null")
	void testIdConstructorUsernameIsNull() {
		assertNull(new User(42L).getUsername());
	}

	@Test
	@DisplayName("Full constructor stores the id")
	void testFullConstructorStoresId() {
		assertEquals(7L, fullUser().getId());
	}

	@Test
	@DisplayName("Full constructor stores the username")
	void testFullConstructorStoresUsername() {
		assertEquals("alice", fullUser().getUsername());
	}

	@Test
	@DisplayName("Full constructor stores the email")
	void testFullConstructorStoresEmail() {
		assertEquals("alice@udc.es", fullUser().getEmail());
	}

	@Test
	@DisplayName("Full constructor stores the password")
	void testFullConstructorStoresPassword() {
		assertEquals(BCRYPT_HASH, fullUser().getPassword());
	}

	@Test
	@DisplayName("Full constructor stores the role")
	void testFullConstructorStoresRole() {
		assertEquals(Roles.ADMINISTRATOR, fullUser().getRole());
	}

	// ==========================================
	// Getters and setters
	// ==========================================

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE})
	@DisplayName("setId stores boundary values")
	void testSetIdBoundaries(long id) {
		User user = new User();
		user.setId(id);
		assertEquals(id, user.getId());
	}

	@Test
	@DisplayName("setId stores a random value")
	void testSetIdRandom() {
		long id = new Random().nextLong();
		User user = new User();
		user.setId(id);
		assertEquals(id, user.getId());
	}

	@Test
	@DisplayName("setId accepts null")
	void testSetIdNull() {
		User user = fullUser();
		user.setId(null);
		assertNull(user.getId());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"a", "bob", "user with spaces", "ñandú"})
	@DisplayName("setUsername stores the given value")
	void testSetUsername(String username) {
		User user = new User();
		user.setUsername(username);
		assertEquals(username, user.getUsername());
	}

	@Test
	@DisplayName("setUsername stores a 255 character value")
	void testSetUsernameLong() {
		User user = new User();
		user.setUsername(LONG_TEXT);
		assertEquals(LONG_TEXT, user.getUsername());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"root@localhost", "a@b.c"})
	@DisplayName("setEmail stores the given value")
	void testSetEmail(String email) {
		User user = new User();
		user.setEmail(email);
		assertEquals(email, user.getEmail());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {BCRYPT_HASH, "plain"})
	@DisplayName("setPassword stores the given value")
	void testSetPassword(String password) {
		User user = new User();
		user.setPassword(password);
		assertEquals(password, user.getPassword());
	}

	@ParameterizedTest
	@EnumSource(Roles.class)
	@NullSource
	@DisplayName("setRole stores every role and null")
	void testSetRole(Roles role) {
		User user = new User();
		user.setRole(role);
		assertEquals(role, user.getRole());
	}

	// ==========================================
	// JSON representation
	// ==========================================

	@Test
	@DisplayName("Serialized user does not expose the password hash")
	void testJsonDoesNotExposePassword() throws JsonProcessingException {
		String json = new ObjectMapper().writeValueAsString(fullUser());
		assertFalse(json.contains("password"));
	}

	@Test
	@DisplayName("Serialized user includes the username")
	void testJsonIncludesUsername() throws JsonProcessingException {
		String json = new ObjectMapper().writeValueAsString(fullUser());
		assertTrue(json.contains("\"username\":\"alice\""));
	}

	@Test
	@DisplayName("Deserialized user reads the password sent by the client")
	void testJsonReadsPassword() throws JsonProcessingException {
		User user = new ObjectMapper().readValue("{\"username\":\"bob\",\"password\":\"secret\"}", User.class);
		assertEquals("secret", user.getPassword());
	}

	@Test
	@DisplayName("Deserialized user reads the role by name")
	void testJsonReadsRole() throws JsonProcessingException {
		User user = new ObjectMapper().readValue("{\"role\":\"NORMAL_USER\"}", User.class);
		assertEquals(Roles.NORMAL_USER, user.getRole());
	}
}
