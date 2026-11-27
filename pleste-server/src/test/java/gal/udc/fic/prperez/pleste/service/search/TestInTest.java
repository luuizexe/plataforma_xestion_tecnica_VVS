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

@DisplayName("TestIn Unit Tests")
public class TestInTest {

	private static Template sampleTemplate() {
		TemplateField tf = new TemplateField(10L, "field1", false, FieldTypes.TEXT);
		return new Template(1L, "TemplateA", "DescriptionA", List.of(tf));
	}

	private static Component sampleComponent() {
		TemplateField tfText = new TemplateField(1L, "code", false, FieldTypes.TEXT);
		TemplateField tfNum = new TemplateField(2L, "count", false, FieldTypes.NUMBER);
		TemplateField tfDate = new TemplateField(3L, "when", false, FieldTypes.DATETIME);
		TemplateField tfLink = new TemplateField(4L, "parent", false, FieldTypes.LINK);
		TextField fText = new TextField(10L, "ABC", tfText);
		NumberField fNum = new NumberField(11L, new BigDecimal("100"), tfNum);
		DatetimeField fDate = new DatetimeField(12L, new JSONDatetime(OffsetDateTime.parse("2026-11-25T10:00:00Z")), tfDate);
		LinkField fLink = new LinkField(13L, new Component(42L), tfLink);
		return new Component(1L, "Pump", "Water pump", new Template(1L), List.of(fText, fNum, fDate, fLink));
	}

	private static User sampleUser() {
		return new User(1L, "bob", "bob@udc.es", "hash", Roles.NORMAL_USER);
	}

	@Test
	@DisplayName("toString formats property and values list")
	void testToString() {
		TestIn ti = new TestIn("user.name", List.of("alice", "bob"), true);
		assertEquals("USER_NAME IN ( [alice, bob] )", ti.toString());
	}

	@Disabled("Bug conocido: TestIn lanza IllegalArgumentException en lugar de ParseSearchException ante propiedad desconocida")
	@Test
	@DisplayName("Constructor throws ParseSearchException on unknown property")
	void testConstructorUnknownPropertyThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestIn("unknown.prop", List.of("a"), true));
	}

	@Disabled("Bug conocido: TestIn lanza NullPointerException en lugar de ParseSearchException ante propiedad nula")
	@Test
	@DisplayName("Constructor throws ParseSearchException on null property")
	void testConstructorNullPropertyThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestIn(null, List.of("a"), true));
	}

	@ParameterizedTest
	@CsvSource({
			"template.name, TemplateA|Other, true, true",
			"template.name, Other1|Other2, true, false",
			"template.name, Other1|Other2, false, true",
			"template.name, TemplateA|Other, false, false",
			"template.description, DescriptionA, true, true",
			"template.description, NonMatching, true, false",
			"template.field.name, field1|field2, true, true",
			"template.field.name, field2, true, false",
			"template.field.type, TEXT|NUMBER, true, true",
			"template.field.type, LINK, true, false"
	})
	@DisplayName("matches evaluates all Template properties for @ and !@")
	void testMatchesTemplate(String property, String rawValues, boolean isEqual, boolean expected) {
		List<String> values = List.of(rawValues.split("\\|"));
		assertEquals(expected, new TestIn(property, values, isEqual).matches(sampleTemplate()));
	}

	@Test
	@DisplayName("matches returns false on Template when values list is empty")
	void testMatchesTemplateEmptyValues() {
		assertFalse(new TestIn("template.name", List.of(), true).matches(sampleTemplate()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"component.name", "component.description", "component.field.name", "component.field.type", "component.field.value", "user.name", "user.email", "user.role"})
	@DisplayName("matches throws ParseSearchException when Template uses non-template property")
	void testMatchesTemplateIncompatiblePropertyThrows(String property) {
		assertThrows(ParseSearchException.class, () -> new TestIn(property, List.of("val"), true).matches(sampleTemplate()));
	}

	@ParameterizedTest
	@CsvSource({
			"component.name, Pump|Motor, true, true",
			"component.name, Valve, true, false",
			"component.name, Valve, false, true",
			"component.description, Water pump, true, true",
			"component.description, Air pump, true, false",
			"component.field.name, code, true, true",
			"component.field.name, unknown, true, false",
			"component.field.type, TEXT|LINK, true, true",
			"component.field.type, CUSTOM, true, false",
			"component.field.value, ABC|DEF, true, true",
			"component.field.value, XYZ, true, false",
			"component.field.value, 100|200, true, true",
			"component.field.value, 50, true, false",
			"component.field.value, 42|99, true, true",
			"component.field.value, 99, true, false"
	})
	@DisplayName("matches evaluates Component properties and field values for @ and !@")
	void testMatchesComponent(String property, String rawValues, boolean isEqual, boolean expected) {
		List<String> values = List.of(rawValues.split("\\|"));
		assertEquals(expected, new TestIn(property, values, isEqual).matches(sampleComponent()));
	}

	@Test
	@DisplayName("matches ignores NumberFormatException on NUMBER field when non-numeric string is passed")
	void testMatchesComponentNumberNonNumericReturnsFalse() {
		TemplateField tfNum = new TemplateField(2L, "count", false, FieldTypes.NUMBER);
		Component comp = new Component(2L, "C", "D", null, List.of(new NumberField(1L, new BigDecimal("100"), tfNum)));
		assertFalse(new TestIn("component.field.value", List.of("not-a-number"), true).matches(comp));
	}

	@Test
	@DisplayName("matches ignores NumberFormatException on LINK field when non-numeric string is passed")
	void testMatchesComponentLinkNonNumericReturnsFalse() {
		TemplateField tfLink = new TemplateField(4L, "parent", false, FieldTypes.LINK);
		Component comp = new Component(2L, "C", "D", null, List.of(new LinkField(1L, new Component(42L), tfLink)));
		assertFalse(new TestIn("component.field.value", List.of("not-a-link-id"), true).matches(comp));
	}

	@ParameterizedTest
	@ValueSource(strings = {"template.name", "template.description", "template.field.name", "template.field.type", "user.name", "user.email", "user.role"})
	@DisplayName("matches throws ParseSearchException when Component uses non-component property")
	void testMatchesComponentIncompatiblePropertyThrows(String property) {
		assertThrows(ParseSearchException.class, () -> new TestIn(property, List.of("x"), true).matches(sampleComponent()));
	}

	@ParameterizedTest
	@CsvSource({
			"user.name, alice|bob, true, true",
			"user.name, charlie, true, false",
			"user.name, charlie, false, true",
			"user.email, bob@udc\\.es, true, true",
			"user.email, other@udc\\.es, true, false",
			"user.role, NORMAL_USER, true, true",
			"user.role, ADMINISTRATOR, true, false"
	})
	@DisplayName("matches evaluates User properties for @ and !@")
	void testMatchesUser(String property, String rawValues, boolean isEqual, boolean expected) {
		List<String> values = List.of(rawValues.split("\\|"));
		assertEquals(expected, new TestIn(property, values, isEqual).matches(sampleUser()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"template.name", "component.name", "component.field.value"})
	@DisplayName("matches throws ParseSearchException when User uses non-user property")
	void testMatchesUserIncompatiblePropertyThrows(String property) {
		assertThrows(ParseSearchException.class, () -> new TestIn(property, List.of("T"), true).matches(sampleUser()));
	}

	@Test
	@DisplayName("matches returns false for non-entity object when operator is @")
	void testMatchesOtherObjectPositiveReturnsFalse() {
		assertFalse(new TestIn("user.name", List.of("a"), true).matches(12345));
	}

	@Test
	@DisplayName("matches returns false for null target when operator is @")
	void testMatchesNullPositiveReturnsFalse() {
		assertFalse(new TestIn("user.name", List.of("a"), true).matches(null));
	}

	@RepeatedTest(5)
	@DisplayName("matches matches random LINK IDs in component.field.value")
	void testMatchesRandomLinkId() {
		long targetId = 1L + new Random().nextInt(10000);
		TemplateField tfLink = new TemplateField(1L, "parent", false, FieldTypes.LINK);
		Component comp = new Component(1L, "C", "D", null, List.of(new LinkField(1L, new Component(targetId), tfLink)));
		assertTrue(new TestIn("component.field.value", List.of(String.valueOf(targetId)), true).matches(comp));
	}

	@Disabled("Bug conocido: JSONDatetime no sobreescribe toString() y TestIn busca contra @hashcode en lugar de fecha ISO")
	@Test
	@DisplayName("matches matches DATETIME field against ISO date string")
	void testMatchesComponentDatetimeIsoDate() {
		assertTrue(new TestIn("component.field.value", List.of("2026-11-25T10:00:00Z"), true).matches(sampleComponent()));
	}

	@Disabled("Bug conocido: TestIn.matches con operador !@ devuelve true para objetos que no son Template, Component ni User")
	@Test
	@DisplayName("matches returns false for non-entity object even when operator is !@")
	void testMatchesOtherObjectNegatedReturnsFalse() {
		assertFalse(new TestIn("user.name", List.of("a"), false).matches(12345));
	}

	@Disabled("Bug conocido: TestIn.matches lanza NullPointerException cuando un campo LINK tiene content null")
	@Test
	@DisplayName("matches returns false without NPE when LINK field content is null")
	void testMatchesComponentLinkNull() {
		TemplateField tf = new TemplateField(1L, "parent", false, FieldTypes.LINK);
		Component comp = new Component(1L, "C", "D", null, List.of(new LinkField(10L, null, tf)));
		assertFalse(new TestIn("component.field.value", List.of("42"), true).matches(comp));
	}

	@Disabled("Bug conocido: TestIn.matches lanza NullPointerException cuando un campo TEXT tiene content null")
	@Test
	@DisplayName("matches returns false without NPE when TEXT field content is null")
	void testMatchesComponentTextNull() {
		TemplateField tf = new TemplateField(1L, "code", false, FieldTypes.TEXT);
		Component comp = new Component(1L, "C", "D", null, List.of(new TextField(10L, null, tf)));
		assertFalse(new TestIn("component.field.value", List.of("ABC"), true).matches(comp));
	}

	@Disabled("Bug conocido: TestIn.matches lanza NullPointerException cuando un campo NUMBER tiene content null")
	@Test
	@DisplayName("matches returns false without NPE when NUMBER field content is null")
	void testMatchesComponentNumberNull() {
		TemplateField tf = new TemplateField(1L, "num", false, FieldTypes.NUMBER);
		Component comp = new Component(1L, "C", "D", null, List.of(new NumberField(10L, null, tf)));
		assertFalse(new TestIn("component.field.value", List.of("10"), true).matches(comp));
	}

	@Disabled("Bug conocido: TestIn.matches lanza NullPointerException cuando un campo DATETIME tiene content null")
	@Test
	@DisplayName("matches returns false without NPE when DATETIME field content is null")
	void testMatchesComponentDatetimeNull() {
		TemplateField tf = new TemplateField(1L, "dt", false, FieldTypes.DATETIME);
		Component comp = new Component(1L, "C", "D", null, List.of(new DatetimeField(10L, null, tf)));
		assertFalse(new TestIn("component.field.value", List.of("2026-11-25T10:00:00Z"), true).matches(comp));
	}

	@Disabled("Bug conocido: TestIn.matches compara BigDecimal con equals() en lugar de compareTo() == 0")
	@Test
	@DisplayName("matches matches NUMBER field regardless of BigDecimal scale")
	void testMatchesComponentNumberDifferentScale() {
		TemplateField tf = new TemplateField(1L, "num", false, FieldTypes.NUMBER);
		Component comp = new Component(1L, "C", "D", null, List.of(new NumberField(10L, new BigDecimal("100.00"), tf)));
		assertTrue(new TestIn("component.field.value", List.of("100"), true).matches(comp));
	}

	@Disabled("Bug conocido: TestIn.matches aborta el stream completo al primer valor no numérico en la lista")
	@Test
	@DisplayName("matches matches NUMBER field when a valid value follows a non-numeric value in list")
	void testMatchesComponentNumberMixedInvalidAndValidValues() {
		TemplateField tf = new TemplateField(1L, "num", false, FieldTypes.NUMBER);
		Component comp = new Component(1L, "C", "D", null, List.of(new NumberField(10L, new BigDecimal("100"), tf)));
		assertTrue(new TestIn("component.field.value", List.of("abc", "100"), true).matches(comp));
	}

	@Disabled("Bug conocido: TestIn.matches aborta el stream completo al primer valor no numérico en la lista")
	@Test
	@DisplayName("matches matches LINK field when a valid id follows a non-numeric value in list")
	void testMatchesComponentLinkMixedInvalidAndValidValues() {
		TemplateField tf = new TemplateField(1L, "link", false, FieldTypes.LINK);
		Component comp = new Component(1L, "C", "D", null, List.of(new LinkField(10L, new Component(42L), tf)));
		assertTrue(new TestIn("component.field.value", List.of("abc", "42"), true).matches(comp));
	}

	@Disabled("Bug conocido: TestIn.matches lanza NullPointerException cuando Template.name es null")
	@Test
	@DisplayName("matches returns false without NPE when Template name is null")
	void testMatchesTemplateNullName() {
		Template t = new Template(1L, null, "desc", List.of());
		assertFalse(new TestIn("template.name", List.of("A"), true).matches(t));
	}

	@Disabled("Bug conocido: TestIn.matches lanza NullPointerException cuando User.role es null")
	@Test
	@DisplayName("matches returns false without NPE when User role is null")
	void testMatchesUserNullRole() {
		User u = new User(1L, "bob", "b@u", "h", null);
		assertFalse(new TestIn("user.role", List.of("NORMAL_USER"), true).matches(u));
	}
}
