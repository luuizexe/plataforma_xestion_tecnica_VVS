package gal.udc.fic.prperez.pleste.service.dao.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Token Unit Tests")
public class TokenTest {

	private static final User OWNER = new User(1L, "alice", "alice@udc.es", "hash", Roles.NORMAL_USER);

	// ==========================================
	// Constructors, getters and setters
	// ==========================================

	@Test
	@DisplayName("Default constructor leaves id null")
	void testDefaultConstructorIdIsNull() {
		assertNull(new Token().getId());
	}

	@Test
	@DisplayName("Default constructor leaves token null")
	void testDefaultConstructorTokenIsNull() {
		assertNull(new Token().getToken());
	}

	@Test
	@DisplayName("Full constructor stores the id")
	void testFullConstructorStoresId() {
		assertEquals(3L, new Token(3L, OWNER, "abc").getId());
	}

	@Test
	@DisplayName("Full constructor stores the user")
	void testFullConstructorStoresUser() {
		assertSame(OWNER, new Token(3L, OWNER, "abc").getUser());
	}

	@Test
	@DisplayName("Full constructor stores the token")
	void testFullConstructorStoresToken() {
		assertEquals("abc", new Token(3L, OWNER, "abc").getToken());
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, 0L, 1L, Long.MAX_VALUE})
	@DisplayName("setId stores boundary values")
	void testSetIdBoundaries(long id) {
		Token token = new Token();
		token.setId(id);
		assertEquals(id, token.getId());
	}

	@Test
	@DisplayName("setUser stores the user")
	void testSetUser() {
		Token token = new Token();
		token.setUser(OWNER);
		assertSame(OWNER, token.getUser());
	}

	@Test
	@DisplayName("setUser accepts null")
	void testSetUserNull() {
		Token token = new Token(3L, OWNER, "abc");
		token.setUser(null);
		assertNull(token.getUser());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"0123456789abcdef0123456789abcdef01234567", "f"})
	@DisplayName("setToken stores the given value")
	void testSetToken(String value) {
		Token token = new Token();
		token.setToken(value);
		assertEquals(value, token.getToken());
	}

	// ==========================================
	// equals and hashCode
	// ==========================================

	@Test
	@DisplayName("A token is equal to itself")
	void testEqualsReflexive() {
		Token token = new Token(1L, OWNER, "abc");
		assertEquals(token, token);
	}

	@Test
	@DisplayName("Tokens with the same id are equal")
	void testEqualsSameId() {
		assertEquals(new Token(1L, OWNER, "abc"), new Token(1L, null, "other"));
	}

	@Test
	@DisplayName("Tokens with different ids are not equal")
	void testEqualsDifferentId() {
		assertNotEquals(new Token(1L, OWNER, "abc"), new Token(2L, OWNER, "abc"));
	}

	@Test
	@DisplayName("Tokens with random different ids are not equal")
	void testEqualsRandomDifferentId() {
		long id = new Random().nextLong(Long.MAX_VALUE - 1);
		assertNotEquals(new Token(id, OWNER, "abc"), new Token(id + 1, OWNER, "abc"));
	}

	@Test
	@DisplayName("A token is not equal to null")
	void testEqualsNull() {
		assertFalse(new Token(1L, OWNER, "abc").equals(null));
	}

	@Test
	@DisplayName("A token is not equal to an object of another type")
	void testEqualsOtherType() {
		assertNotEquals(new Token(1L, OWNER, "abc"), "abc");
	}

	@Test
	@DisplayName("A saved token is not equal to an unsaved one")
	void testEqualsSavedAndUnsaved() {
		assertNotEquals(new Token(1L, OWNER, "abc"), new Token(null, OWNER, "abc"));
	}

	@Test
	@DisplayName("Two different unsaved tokens are not equal")
	void testEqualsTwoUnsavedTokens() {
		assertNotEquals(new Token(null, OWNER, "abc"), new Token(null, OWNER, "def"));
	}

	@Test
	@DisplayName("Two different unsaved tokens are both kept in a set")
	void testUnsavedTokensInSet() {
		Set<Token> tokens = new HashSet<>();
		tokens.add(new Token(null, OWNER, "abc"));
		tokens.add(new Token(null, OWNER, "def"));
		assertEquals(2, tokens.size());
	}

	@Test
	@DisplayName("Equal tokens have the same hash code")
	void testHashCodeConsistentWithEquals() {
		assertEquals(new Token(5L, OWNER, "abc").hashCode(), new Token(5L, null, "other").hashCode());
	}
}
