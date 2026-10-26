package gal.udc.fic.prperez.pleste.service.dao.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Roles Unit Tests")
public class RolesTest {

	@Test
	@DisplayName("There are exactly two roles")
	void testNumberOfRoles() {
		assertEquals(2, Roles.values().length);
	}

	@ParameterizedTest
	@EnumSource(Roles.class)
	@DisplayName("valueOf returns the role for its own name")
	void testValueOfOwnName(Roles role) {
		assertEquals(role, Roles.valueOf(role.name()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "admin", "administrator", "NORMAL", "normal_user"})
	@DisplayName("valueOf rejects unknown or wrongly cased names")
	void testValueOfInvalidName(String name) {
		assertThrows(IllegalArgumentException.class, () -> Roles.valueOf(name));
	}

	@Test
	@DisplayName("valueOf rejects null")
	void testValueOfNull() {
		assertThrows(NullPointerException.class, () -> Roles.valueOf(null));
	}
}
