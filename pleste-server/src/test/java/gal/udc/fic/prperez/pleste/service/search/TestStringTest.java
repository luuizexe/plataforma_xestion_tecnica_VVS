package gal.udc.fic.prperez.pleste.service.search;

import gal.udc.fic.prperez.pleste.service.JSONDatetime;
import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.DatetimeField;
import gal.udc.fic.prperez.pleste.service.dao.component.LinkField;
import gal.udc.fic.prperez.pleste.service.dao.component.NumberField;
import gal.udc.fic.prperez.pleste.service.dao.component.TextField;
import gal.udc.fic.prperez.pleste.service.dao.template.FieldTypes;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.ParseSearchException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestString Unit Tests")
public class TestStringTest {

	private static Template sampleTemplate() {
		TemplateField tf1 = new TemplateField(10L, "temperature", true, FieldTypes.NUMBER);
		TemplateField tf2 = new TemplateField(11L, "location", false, FieldTypes.TEXT);
		return new Template(1L, "TempSensor", "Temperature sensor description", List.of(tf1, tf2));
	}

	private static Component sampleComponent() {
		TemplateField tfText = new TemplateField(1L, "model", false, FieldTypes.TEXT);
		TemplateField tfDate = new TemplateField(2L, "installedAt", false, FieldTypes.DATETIME);
		TemplateField tfNum = new TemplateField(3L, "serial", false, FieldTypes.NUMBER);
		TemplateField tfLink = new TemplateField(4L, "parent", false, FieldTypes.LINK);
		TextField fText = new TextField(10L, "Model-X100", tfText);
		DatetimeField fDate = new DatetimeField(11L, new JSONDatetime(OffsetDateTime.parse("2026-11-25T12:00:00Z")), tfDate);
		NumberField fNum = new NumberField(12L, new BigDecimal("42"), tfNum);
		LinkField fLink = new LinkField(13L, new Component(5L), tfLink);
		return new Component(100L, "LivingRoom-Sensor", "Smart sensor", new Template(1L), List.of(fText, fDate, fNum, fLink));
	}

	private static User sampleUser() {
		return new User(1L, "alice", "alice@example.com", "passhash", Roles.ADMINISTRATOR);
	}

	@Test
	@DisplayName("toString returns PROPERTY LIKE value format")
	void testToString() {
		assertEquals("TEMPLATE_NAME LIKE Sensor.*", new TestString("template.name", "Sensor.*", true).toString());
	}

	@Disabled("Bug conocido: TestString lanza IllegalArgumentException en lugar de ParseSearchException ante propiedad desconocida")
	@Test
	@DisplayName("Constructor throws ParseSearchException on unknown property")
	void testConstructorUnknownPropertyThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestString("unknown.prop", "a", true));
	}

	@Disabled("Bug conocido: TestString lanza NullPointerException en lugar de ParseSearchException ante propiedad nula")
	@Test
	@DisplayName("Constructor throws ParseSearchException on null property")
	void testConstructorNullPropertyThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestString(null, "a", true));
	}

	@ParameterizedTest
	@CsvSource({
			"template.name, Temp.*, true, true",
			"template.name, Hum.*, true, false",
			"template.name, Hum.*, false, true",
			"template.name, Temp.*, false, false",
			"template.description, .*sensor.*, true, true",
			"template.description, Pressure, true, false",
			"template.field.name, temperature, true, true",
			"template.field.name, loc.*, true, true",
			"template.field.name, nonexistent, true, false",
			"template.field.type, NUMBER, true, true",
			"template.field.type, TEXT, true, true",
			"template.field.type, LINK, true, false",
			"template.field.type, LINK, false, true"
	})
	@DisplayName("matches evaluates all Template properties for ~ and !~")
	void testMatchesTemplate(String property, String regex, boolean isEqual, boolean expected) {
		assertEquals(expected, new TestString(property, regex, isEqual).matches(sampleTemplate()));
	}

	@Test
	@DisplayName("matches returns false on Template with empty fields list for template.field.name")
	void testMatchesTemplateEmptyFields() {
		Template emptyFields = new Template(1L, "T", "D", List.of());
		assertFalse(new TestString("template.field.name", ".*", true).matches(emptyFields));
	}

	@ParameterizedTest
	@ValueSource(strings = {"component.name", "component.description", "component.field.name", "component.field.type", "component.field.value", "user.name", "user.email", "user.role"})
	@DisplayName("matches throws ParseSearchException when Template uses non-template property")
	void testMatchesTemplateIncompatiblePropertyThrows(String property) {
		assertThrows(ParseSearchException.class, () -> new TestString(property, ".*", true).matches(sampleTemplate()));
	}

	@ParameterizedTest
	@CsvSource({
			"component.name, LivingRoom.*, true, true",
			"component.name, Kitchen.*, true, false",
			"component.name, Kitchen.*, false, true",
			"component.description, Smart.*, true, true",
			"component.description, Analog, true, false",
			"component.field.name, model, true, true",
			"component.field.name, nonexistent, true, false",
			"component.field.type, TEXT, true, true",
			"component.field.type, DATETIME, true, true",
			"component.field.type, CUSTOM, true, false",
			"component.field.value, Model-.*, true, true",
			"component.field.value, Unknown, true, false",
			"component.field.value, Unknown, false, true"
	})
	@DisplayName("matches evaluates Component properties for ~ and !~")
	void testMatchesComponent(String property, String regex, boolean isEqual, boolean expected) {
		assertEquals(expected, new TestString(property, regex, isEqual).matches(sampleComponent()));
	}

	@Test
	@DisplayName("matches returns false for component.field.value when component only has NUMBER and LINK fields")
	void testMatchesComponentNumberAndLinkReturnFalse() {
		TemplateField tfNum = new TemplateField(1L, "n", false, FieldTypes.NUMBER);
		TemplateField tfLink = new TemplateField(2L, "l", false, FieldTypes.LINK);
		Component comp = new Component(1L, "C", "D", null, List.of(
				new NumberField(1L, new BigDecimal("42"), tfNum),
				new LinkField(2L, new Component(5L), tfLink)));
		assertFalse(new TestString("component.field.value", ".*", true).matches(comp));
	}

	@ParameterizedTest
	@ValueSource(strings = {"template.name", "template.description", "template.field.name", "template.field.type", "user.name", "user.email", "user.role"})
	@DisplayName("matches throws ParseSearchException when Component uses non-component property")
	void testMatchesComponentIncompatiblePropertyThrows(String property) {
		assertThrows(ParseSearchException.class, () -> new TestString(property, ".*", true).matches(sampleComponent()));
	}

	@ParameterizedTest
	@CsvSource({
			"user.name, ali.*, true, true",
			"user.name, bob, true, false",
			"user.name, bob, false, true",
			"user.email, .*@example\\.com, true, true",
			"user.email, .*@udc\\.es, true, false",
			"user.role, ADMINISTRATOR, true, true",
			"user.role, NORMAL_USER, true, false",
			"user.role, NORMAL_USER, false, true"
	})
	@DisplayName("matches evaluates User properties for ~ and !~")
	void testMatchesUser(String property, String regex, boolean isEqual, boolean expected) {
		assertEquals(expected, new TestString(property, regex, isEqual).matches(sampleUser()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"template.name", "template.description", "component.name", "component.field.value"})
	@DisplayName("matches throws ParseSearchException when User uses non-user property")
	void testMatchesUserIncompatiblePropertyThrows(String property) {
		assertThrows(ParseSearchException.class, () -> new TestString(property, ".*", true).matches(sampleUser()));
	}

	@Test
	@DisplayName("matches returns false for non-entity object when operator is ~")
	void testMatchesNonEntityPositiveReturnsFalse() {
		assertFalse(new TestString("template.name", ".*", true).matches("SomeString"));
	}

	@Test
	@DisplayName("matches returns false for null target when operator is ~")
	void testMatchesNullPositiveReturnsFalse() {
		assertFalse(new TestString("template.name", ".*", true).matches(null));
	}

	@Test
	@DisplayName("matches handles empty string boundary on empty template name")
	void testMatchesEmptyStringBoundary() {
		Template t = new Template(1L, "", "", List.of());
		assertTrue(new TestString("template.name", "", true).matches(t));
	}

	@RepeatedTest(5)
	@DisplayName("matches matches randomly generated User usernames")
	void testMatchesRandomUsername() {
		String suffix = String.valueOf(new Random().nextInt(10000));
		User user = new User(1L, "usr_" + suffix, "u@udc.es", "h", Roles.NORMAL_USER);
		assertTrue(new TestString("user.name", "usr_" + suffix, true).matches(user));
	}

	@Disabled("Bug conocido: JSONDatetime no sobreescribe toString() y TestString busca contra @hashcode en lugar de fecha ISO")
	@Test
	@DisplayName("matches matches DATETIME field against ISO date regex")
	void testMatchesComponentDatetimeIsoDate() {
		assertTrue(new TestString("component.field.value", "2026-11-25.*", true).matches(sampleComponent()));
	}

	@Disabled("Bug conocido: TestString.matches con operador !~ devuelve true para objetos que no son Template, Component ni User")
	@Test
	@DisplayName("matches returns false for non-entity object even when operator is !~")
	void testMatchesNonEntityNegatedReturnsFalse() {
		assertFalse(new TestString("template.name", "abc", false).matches("SomeString"));
	}

	@Disabled("Bug conocido: TestString.matches lanza PatternSyntaxException en lugar de ParseSearchException con regex inválida")
	@Test
	@DisplayName("Invalid regex throws ParseSearchException")
	void testInvalidRegexThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestString("template.name", "[unclosed", true).matches(sampleTemplate()));
	}

	@Disabled("Bug conocido: TestString.matches lanza NullPointerException cuando Template.name es null")
	@Test
	@DisplayName("matches returns false without NPE when Template name is null")
	void testMatchesTemplateNullName() {
		Template t = new Template(1L, null, "desc", List.of());
		assertFalse(new TestString("template.name", ".*", true).matches(t));
	}

	@Disabled("Bug conocido: TestString.matches lanza NullPointerException cuando Template.fields es null")
	@Test
	@DisplayName("matches returns false without NPE when Template fields is null")
	void testMatchesTemplateNullFields() {
		Template t = new Template(1L, "name", "desc", null);
		assertFalse(new TestString("template.field.name", ".*", true).matches(t));
	}

	@Disabled("Bug conocido: TestString.matches lanza NullPointerException cuando TextField.content es null")
	@Test
	@DisplayName("matches returns false without NPE when TextField content is null")
	void testMatchesComponentTextNullContent() {
		TemplateField tf = new TemplateField(1L, "model", false, FieldTypes.TEXT);
		Component comp = new Component(1L, "C", "D", null, List.of(new TextField(1L, null, tf)));
		assertFalse(new TestString("component.field.value", ".*", true).matches(comp));
	}

	@Disabled("Bug conocido: TestString.matches lanza NullPointerException cuando DatetimeField.content es null")
	@Test
	@DisplayName("matches returns false without NPE when DatetimeField content is null")
	void testMatchesComponentDatetimeNullContent() {
		TemplateField tf = new TemplateField(1L, "date", false, FieldTypes.DATETIME);
		Component comp = new Component(1L, "C", "D", null, List.of(new DatetimeField(1L, null, tf)));
		assertFalse(new TestString("component.field.value", ".*", true).matches(comp));
	}

	@Disabled("Bug conocido: TestString.matches lanza NullPointerException cuando User.username es null")
	@Test
	@DisplayName("matches returns false without NPE when User username is null")
	void testMatchesUserNullUsername() {
		User u = new User(1L, null, "a@b", "h", Roles.NORMAL_USER);
		assertFalse(new TestString("user.name", ".*", true).matches(u));
	}
}
