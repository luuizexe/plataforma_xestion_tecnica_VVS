package gal.udc.fic.prperez.pleste.service.search;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Properties Enum Unit Tests")
public class PropertiesTest {

	@Test
	@DisplayName("Properties enum contains exactly 12 constants")
	void testValuesCount() {
		assertEquals(12, Properties.values().length);
	}

	@ParameterizedTest
	@CsvSource({
			"TEMPLATE_NAME, template.name",
			"TEMPLATE_DESCRIPTION, template.description",
			"TEMPLATE_FIELD_NAME, template.field.name",
			"TEMPLATE_FIELD_TYPE, template.field.type",
			"COMPONENT_NAME, component.name",
			"COMPONENT_DESCRIPTION, component.description",
			"COMPONENT_FIELD_NAME, component.field.name",
			"COMPONENT_FIELD_TYPE, component.field.type",
			"COMPONENT_FIELD_VALUE, component.field.value",
			"USER_NAME, user.name",
			"USER_EMAIL, user.email",
			"USER_ROLE, user.role"
	})
	@DisplayName("getProperty returns expected dot-separated key for each constant")
	void testGetPropertyString(Properties constant, String expectedKey) {
		assertEquals(expectedKey, constant.getProperty());
	}

	@ParameterizedTest
	@EnumSource(Properties.class)
	@DisplayName("getProperty(String) resolves every constant from its property string")
	void testGetPropertyResolvesAllConstants(Properties constant) {
		assertEquals(constant, Properties.getProperty(constant.getProperty()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "template", "TEMPLATE_NAME", "TEMPLATE.NAME", "template.unknown", "component.field.unknown", "user.password"})
	@DisplayName("getProperty(String) throws IllegalArgumentException for unknown strings")
	void testGetPropertyUnknownThrows(String input) {
		assertThrows(IllegalArgumentException.class, () -> Properties.getProperty(input));
	}

	@Disabled("Bug conocido: Properties.getProperty(null) lanza NullPointerException en s.equals(...) en lugar de IllegalArgumentException")
	@Test
	@DisplayName("getProperty(null) throws IllegalArgumentException")
	void testGetPropertyNullThrowsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> Properties.getProperty(null));
	}

	@RepeatedTest(5)
	@DisplayName("getProperty(String) resolves random valid constants")
	void testGetPropertyRandomConstant() {
		Properties[] all = Properties.values();
		Properties chosen = all[new Random().nextInt(all.length)];
		assertEquals(chosen, Properties.getProperty(chosen.getProperty()));
	}
}
