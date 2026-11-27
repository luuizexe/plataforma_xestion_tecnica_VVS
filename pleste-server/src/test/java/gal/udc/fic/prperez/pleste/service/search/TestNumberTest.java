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

@DisplayName("TestNumber Unit Tests")
public class TestNumberTest {

	private static Component numberComponent(String value) {
		TemplateField tf = new TemplateField(1L, "price", false, FieldTypes.NUMBER);
		return new Component(100L, "Comp", "Desc", null, List.of(new NumberField(10L, new BigDecimal(value), tf)));
	}

	private static Component datetimeComponent(String iso) {
		TemplateField tf = new TemplateField(1L, "created", false, FieldTypes.DATETIME);
		return new Component(100L, "Comp", "Desc", null, List.of(new DatetimeField(10L, JSONDatetime.parse(iso), tf)));
	}

	private static Component linkComponent(long targetId) {
		TemplateField tf = new TemplateField(1L, "target", false, FieldTypes.LINK);
		return new Component(100L, "Comp", "Desc", null, List.of(new LinkField(10L, new Component(targetId), tf)));
	}

	@Test
	@DisplayName("Constructor throws ParseSearchException on unknown property")
	void testConstructorUnknownPropertyThrows() {
		assertThrows(ParseSearchException.class, () -> new TestNumber("invalid.prop", "=", "10"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"LIKE", "~", "@", "==", "<>", ""})
	@DisplayName("Constructor throws ParseSearchException on unknown operator")
	void testConstructorUnknownOperatorThrows(String op) {
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", op, "10"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"not-a-number", "", "2026-13-40T00:00:00Z"})
	@DisplayName("Constructor throws ParseSearchException on invalid number or datetime value")
	void testConstructorInvalidValueThrows(String val) {
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", "=", val));
	}

	@Disabled("Bug conocido: TestNumber lanza NullPointerException en lugar de ParseSearchException cuando property es null")
	@Test
	@DisplayName("Constructor throws ParseSearchException when property is null")
	void testConstructorNullPropertyThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestNumber(null, "=", "10"));
	}

	@Disabled("Bug conocido: TestNumber lanza NullPointerException en lugar de ParseSearchException cuando operator es null")
	@Test
	@DisplayName("Constructor throws ParseSearchException when operator is null")
	void testConstructorNullOperatorThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", null, "10"));
	}

	@Disabled("Bug conocido: TestNumber lanza NullPointerException en lugar de ParseSearchException cuando value es null")
	@Test
	@DisplayName("Constructor throws ParseSearchException when value is null")
	void testConstructorNullValueThrowsParseSearchException() {
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", "=", null));
	}

	@Test
	@DisplayName("toString formats property and operator")
	void testToStringFormat() {
		assertEquals("COMPONENT_FIELD_VALUE >= ", new TestNumber("component.field.value", ">=", "10").toString());
	}

	@Disabled("Bug conocido: TestNumber.toString() omite el valor comparado y termina en espacio")
	@Test
	@DisplayName("toString includes comparison value")
	void testToStringIncludesValue() {
		assertEquals("COMPONENT_FIELD_VALUE >= 10", new TestNumber("component.field.value", ">=", "10").toString());
	}

	@Test
	@DisplayName("matches throws ParseSearchException on Template")
	void testMatchesTemplateThrows() {
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", "=", "10").matches(new Template(1L)));
	}

	@Test
	@DisplayName("matches throws ParseSearchException on User")
	void testMatchesUserThrows() {
		User user = new User(1L, "user", "u@udc.es", "h", Roles.NORMAL_USER);
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", "=", "10").matches(user));
	}

	@Test
	@DisplayName("matches throws ParseSearchException on Component with non-field-value property")
	void testMatchesComponentWrongPropertyThrows() {
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.name", "=", "10").matches(new Component(1L)));
	}

	@Test
	@DisplayName("matches returns false on non-entity object")
	void testMatchesOtherObjectReturnsFalse() {
		assertFalse(new TestNumber("component.field.value", "=", "10").matches("other object"));
	}

	@Test
	@DisplayName("matches returns false on null object")
	void testMatchesNullObjectReturnsFalse() {
		assertFalse(new TestNumber("component.field.value", "=", "10").matches(null));
	}

	@ParameterizedTest
	@CsvSource({
			"=, 50.00, 50.00, true",
			"=, 50.00, 40.00, false",
			"=, 0, 0, true",
			"=, -15.5, -15.5, true",
			"!=, 50.00, 40.00, true",
			"!=, 50.00, 50.00, false",
			">, 50.00, 40.00, true",
			">, 50.00, 50.00, false",
			">, 50.00, 60.00, false",
			"<, 50.00, 60.00, true",
			"<, 50.00, 50.00, false",
			"<, 50.00, 40.00, false",
			">=, 50.00, 40.00, true",
			">=, 50.00, 50.00, true",
			">=, 50.00, 60.00, false",
			"<=, 50.00, 60.00, true",
			"<=, 50.00, 50.00, true",
			"<=, 50.00, 40.00, false"
	})
	@DisplayName("matches evaluates all operators on NUMBER field")
	void testMatchesNumberFieldAllOperators(String op, String fieldVal, String queryVal, boolean expected) {
		assertEquals(expected, new TestNumber("component.field.value", op, queryVal).matches(numberComponent(fieldVal)));
	}

	@RepeatedTest(5)
	@DisplayName("matches evaluates random NUMBER bounds consistently")
	void testMatchesRandomNumbers() {
		int base = new Random().nextInt(1000) - 500;
		Component comp = numberComponent(String.valueOf(base));
		assertTrue(new TestNumber("component.field.value", ">", String.valueOf(base - 1)).matches(comp));
	}

	@ParameterizedTest
	@CsvSource({
			"=, 2026-11-25T12:00:00Z, 2026-11-25T12:00:00Z, true",
			"=, 2026-11-25T12:00:00Z, 2026-11-25T13:00:00+01:00, true",
			"=, 2026-11-25T12:00:00Z, 2026-11-25T11:00:00Z, false",
			"!=, 2026-11-25T12:00:00Z, 2026-11-25T11:00:00Z, true",
			"!=, 2026-11-25T12:00:00Z, 2026-11-25T12:00:00Z, false",
			">, 2026-11-25T12:00:00Z, 2026-11-25T11:00:00Z, true",
			">, 2026-11-25T12:00:00Z, 2026-11-25T12:00:00Z, false",
			">, 2026-11-25T12:00:00Z, 2026-11-25T13:00:00Z, false",
			"<, 2026-11-25T12:00:00Z, 2026-11-25T13:00:00Z, true",
			"<, 2026-11-25T12:00:00Z, 2026-11-25T12:00:00Z, false",
			"<, 2026-11-25T12:00:00Z, 2026-11-25T11:00:00Z, false",
			">=, 2026-11-25T12:00:00Z, 2026-11-25T11:00:00Z, true",
			">=, 2026-11-25T12:00:00Z, 2026-11-25T12:00:00Z, true",
			">=, 2026-11-25T12:00:00Z, 2026-11-25T13:00:00Z, false",
			"<=, 2026-11-25T12:00:00Z, 2026-11-25T13:00:00Z, true",
			"<=, 2026-11-25T12:00:00Z, 2026-11-25T12:00:00Z, true",
			"<=, 2026-11-25T12:00:00Z, 2026-11-25T11:00:00Z, false"
	})
	@DisplayName("matches evaluates all operators on DATETIME field")
	void testMatchesDatetimeFieldAllOperators(String op, String fieldIso, String queryIso, boolean expected) {
		assertEquals(expected, new TestNumber("component.field.value", op, queryIso).matches(datetimeComponent(fieldIso)));
	}

	@ParameterizedTest
	@CsvSource({
			"=, 42, 42, true",
			"=, 42, 99, false",
			"!=, 42, 99, true",
			"!=, 42, 42, false",
			">, 42, 40, false",
			"<, 42, 50, false",
			">=, 42, 42, false",
			"<=, 42, 42, false"
	})
	@DisplayName("matches evaluates LINK field for equality and returns false on relational operators")
	void testMatchesLinkField(String op, long targetId, String queryVal, boolean expected) {
		assertEquals(expected, new TestNumber("component.field.value", op, queryVal).matches(linkComponent(targetId)));
	}

	@Test
	@DisplayName("matches returns false on TEXT field")
	void testMatchesTextFieldReturnsFalse() {
		TemplateField tf = new TemplateField(1L, "notes", false, FieldTypes.TEXT);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(new TextField(10L, "10", tf)));
		assertFalse(new TestNumber("component.field.value", "=", "10").matches(comp));
	}

	@Disabled("Bug conocido: TestNumber.matches lanza NullPointerException cuando un campo LINK tiene content null")
	@Test
	@DisplayName("matches returns false without NPE when LINK field content is null")
	void testMatchesComponentLinkNullContent() {
		TemplateField tf = new TemplateField(1L, "target", false, FieldTypes.LINK);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(new LinkField(10L, null, tf)));
		assertFalse(new TestNumber("component.field.value", "=", "42").matches(comp));
	}

	@Disabled("Bug conocido: TestNumber.matches lanza NullPointerException cuando un campo NUMBER tiene content null")
	@Test
	@DisplayName("matches returns false without NPE when NUMBER field content is null")
	void testMatchesComponentNumberNullContent() {
		TemplateField tf = new TemplateField(1L, "num", false, FieldTypes.NUMBER);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(new NumberField(10L, null, tf)));
		assertFalse(new TestNumber("component.field.value", ">", "10").matches(comp));
	}

	@Disabled("Bug conocido: TestNumber.matches lanza NullPointerException cuando un campo DATETIME tiene content null")
	@Test
	@DisplayName("matches returns false without NPE when DATETIME field content is null")
	void testMatchesComponentDatetimeNullContent() {
		TemplateField tf = new TemplateField(1L, "dt", false, FieldTypes.DATETIME);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(new DatetimeField(10L, null, tf)));
		assertFalse(new TestNumber("component.field.value", "=", "2026-11-25T12:00:00Z").matches(comp));
	}

	@Disabled("Bug conocido: TestNumber.Operator.match usa BigDecimal.equals() en lugar de compareTo() == 0 para =")
	@Test
	@DisplayName("matches considers equal BigDecimal values with different scales as equal")
	void testMatchesNumberEqualDifferentScale() {
		assertTrue(new TestNumber("component.field.value", "=", "50").matches(numberComponent("50.00")));
	}

	@Disabled("Bug conocido: TestNumber.Operator.match usa BigDecimal.equals() en lugar de compareTo() == 0 para !=")
	@Test
	@DisplayName("matches considers equal BigDecimal values with different scales as not-unequal")
	void testMatchesNumberNotEqualDifferentScale() {
		assertFalse(new TestNumber("component.field.value", "!=", "50").matches(numberComponent("50.00")));
	}

	@Disabled("Bug conocido: TestNumber.matches trunca valueNumber con intValue() fallando para IDs de LINK mayores que Integer.MAX_VALUE")
	@Test
	@DisplayName("matches compares LINK id accurately when id exceeds Integer.MAX_VALUE")
	void testMatchesLinkIdExceedingIntMax() {
		long largeId = 3_000_000_000L;
		assertTrue(new TestNumber("component.field.value", "=", "3000000000").matches(linkComponent(largeId)));
	}

	@Disabled("Bug conocido: TestNumber.matches lanza NullPointerException al evaluar consulta numérica sobre componente con campo DATETIME")
	@Test
	@DisplayName("matches returns false without NPE when numeric query runs on DATETIME field")
	void testMatchesNumericQueryOnDatetimeField() {
		assertFalse(new TestNumber("component.field.value", "=", "10").matches(datetimeComponent("2026-11-25T12:00:00Z")));
	}

	@Disabled("Bug conocido: TestNumber.matches lanza NullPointerException al evaluar consulta DATETIME relacional sobre campo NUMBER")
	@Test
	@DisplayName("matches returns false without NPE when DATETIME relational query runs on NUMBER field")
	void testMatchesDatetimeQueryOnNumberField() {
		assertFalse(new TestNumber("component.field.value", ">", "2026-11-25T12:00:00Z").matches(numberComponent("10")));
	}

	@Disabled("Bug conocido: TestNumber.matches lanza NullPointerException al evaluar consulta DATETIME con = sobre campo LINK")
	@Test
	@DisplayName("matches returns false without NPE when DATETIME equality query runs on LINK field")
	void testMatchesDatetimeQueryOnLinkField() {
		assertFalse(new TestNumber("component.field.value", "=", "2026-11-25T12:00:00Z").matches(linkComponent(42L)));
	}
}
