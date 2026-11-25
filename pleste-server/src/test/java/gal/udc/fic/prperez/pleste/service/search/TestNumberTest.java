package gal.udc.fic.prperez.pleste.service.search;

import gal.udc.fic.prperez.pleste.service.JSONDatetime;
import gal.udc.fic.prperez.pleste.service.dao.component.*;
import gal.udc.fic.prperez.pleste.service.dao.template.FieldTypes;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.ParseSearchException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestNumber Unit Tests")
public class TestNumberTest {

	@Test
	@DisplayName("TestNumber constructor error cases")
	void testConstructorErrors() {
		// Unknown property
		assertThrows(ParseSearchException.class, () -> new TestNumber("invalid.prop", "=", "10"));

		// Unknown operator
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", "LIKE", "10"));

		// Invalid value (neither BigDecimal nor ISO Date)
		assertThrows(ParseSearchException.class, () -> new TestNumber("component.field.value", "=", "not-a-number"));
	}

	@Test
	@DisplayName("TestNumber toString")
	void testToString() {
		TestNumber tn = new TestNumber("component.field.value", ">=", "10");
		assertEquals("COMPONENT_FIELD_VALUE >= ", tn.toString());
	}

	@Test
	@DisplayName("TestNumber matches Template or User throws ParseSearchException")
	void testMatchesTemplateAndUserThrows() {
		TestNumber tn = new TestNumber("component.field.value", "=", "10");
		Template t = new Template(1L);
		User u = new User(1L, "user", "u@udc.es", "h", Roles.NORMAL_USER);

		assertThrows(ParseSearchException.class, () -> tn.matches(t));
		assertThrows(ParseSearchException.class, () -> tn.matches(u));
		assertFalse(tn.matches("other object"));

		// Non-COMPONENT_FIELD_VALUE property with Component throws ParseSearchException
		TestNumber invalidPropComp = new TestNumber("component.name", "=", "10");
		assertThrows(ParseSearchException.class, () -> invalidPropComp.matches(new Component(1L)));
	}

	@Test
	@DisplayName("TestNumber matches Component with NUMBER field for all operators")
	void testMatchesComponentNumber() {
		TemplateField tf = new TemplateField(1L, "price", false, FieldTypes.NUMBER);
		NumberField nf = new NumberField(10L, new BigDecimal("50.00"), tf);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(nf));

		// Equal
		assertTrue(new TestNumber("component.field.value", "=", "50.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", "=", "40.00").matches(comp));

		// Not Equal
		assertTrue(new TestNumber("component.field.value", "!=", "40.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", "!=", "50.00").matches(comp));

		// Greater
		assertTrue(new TestNumber("component.field.value", ">", "40.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", ">", "50.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", ">", "60.00").matches(comp));

		// Less
		assertTrue(new TestNumber("component.field.value", "<", "60.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", "<", "50.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", "<", "40.00").matches(comp));

		// Greater or Equal
		assertTrue(new TestNumber("component.field.value", ">=", "40.00").matches(comp));
		assertTrue(new TestNumber("component.field.value", ">=", "50.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", ">=", "60.00").matches(comp));

		// Less or Equal
		assertTrue(new TestNumber("component.field.value", "<=", "60.00").matches(comp));
		assertTrue(new TestNumber("component.field.value", "<=", "50.00").matches(comp));
		assertFalse(new TestNumber("component.field.value", "<=", "40.00").matches(comp));
	}

	@Test
	@DisplayName("TestNumber matches Component with DATETIME field for all operators")
	void testMatchesComponentDatetime() {
		TemplateField tf = new TemplateField(1L, "created", false, FieldTypes.DATETIME);
		OffsetDateTime dt = OffsetDateTime.parse("2026-11-25T12:00:00Z");
		DatetimeField df = new DatetimeField(10L, new JSONDatetime(dt), tf);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(df));

		// Equal
		assertTrue(new TestNumber("component.field.value", "=", "2026-11-25T12:00:00Z").matches(comp));
		assertFalse(new TestNumber("component.field.value", "=", "2026-11-25T11:00:00Z").matches(comp));

		// Not Equal
		assertTrue(new TestNumber("component.field.value", "!=", "2026-11-25T11:00:00Z").matches(comp));
		assertFalse(new TestNumber("component.field.value", "!=", "2026-11-25T12:00:00Z").matches(comp));

		// Greater
		assertTrue(new TestNumber("component.field.value", ">", "2026-11-25T11:00:00Z").matches(comp));
		assertFalse(new TestNumber("component.field.value", ">", "2026-11-25T12:00:00Z").matches(comp));
		assertFalse(new TestNumber("component.field.value", ">", "2026-11-25T13:00:00Z").matches(comp));

		// Less
		assertTrue(new TestNumber("component.field.value", "<", "2026-11-25T13:00:00Z").matches(comp));
		assertFalse(new TestNumber("component.field.value", "<", "2026-11-25T12:00:00Z").matches(comp));

		// Greater or Equal
		assertTrue(new TestNumber("component.field.value", ">=", "2026-11-25T11:00:00Z").matches(comp));
		assertTrue(new TestNumber("component.field.value", ">=", "2026-11-25T12:00:00Z").matches(comp));
		assertFalse(new TestNumber("component.field.value", ">=", "2026-11-25T13:00:00Z").matches(comp));

		// Less or Equal
		assertTrue(new TestNumber("component.field.value", "<=", "2026-11-25T13:00:00Z").matches(comp));
		assertTrue(new TestNumber("component.field.value", "<=", "2026-11-25T12:00:00Z").matches(comp));
		assertFalse(new TestNumber("component.field.value", "<=", "2026-11-25T11:00:00Z").matches(comp));
	}

	@Test
	@DisplayName("TestNumber matches Component with LINK field")
	void testMatchesComponentLink() {
		TemplateField tf = new TemplateField(1L, "target", false, FieldTypes.LINK);
		Component targetComp = new Component(42L);
		LinkField lf = new LinkField(10L, targetComp, tf);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(lf));

		assertTrue(new TestNumber("component.field.value", "=", "42").matches(comp));
		assertFalse(new TestNumber("component.field.value", "=", "99").matches(comp));
		assertTrue(new TestNumber("component.field.value", "!=", "99").matches(comp));
		assertFalse(new TestNumber("component.field.value", "!=", "42").matches(comp));
		// Default operator on LINK returns false
		assertFalse(new TestNumber("component.field.value", ">", "40").matches(comp));
	}

	@Test
	@DisplayName("TestNumber matches Component with TEXT field returns false")
	void testMatchesComponentOtherFieldsReturnsFalse() {
		TemplateField tf = new TemplateField(1L, "notes", false, FieldTypes.TEXT);
		TextField txt = new TextField(10L, "Hello", tf);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(txt));

		assertFalse(new TestNumber("component.field.value", "=", "10").matches(comp));
	}

	@Disabled("ISSUE #6: NullPointerException cuando un campo LINK tiene contenido null (link no establecido)")
	@Test
	@DisplayName("TestNumber matches Component with null LINK field content")
	void testMatchesComponentLinkNullContent() {
		TemplateField tf = new TemplateField(1L, "target", false, FieldTypes.LINK);
		LinkField lf = new LinkField(10L, null, tf);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(lf));

		assertDoesNotThrow(() -> new TestNumber("component.field.value", "=", "42").matches(comp));
	}

	@Disabled("ISSUE #7: NullPointerException cuando un campo NUMBER tiene contenido null")
	@Test
	@DisplayName("TestNumber matches Component with null NUMBER content")
	void testMatchesComponentNumberNullContent() {
		TemplateField tf = new TemplateField(1L, "num", false, FieldTypes.NUMBER);
		NumberField nf = new NumberField(10L, null, tf);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(nf));

		assertDoesNotThrow(() -> new TestNumber("component.field.value", ">", "10").matches(comp));
	}

	@Disabled("ISSUE #8: NullPointerException cuando un campo DATETIME tiene contenido null")
	@Test
	@DisplayName("TestNumber matches Component with null DATETIME content")
	void testMatchesComponentDatetimeNullContent() {
		TemplateField tf = new TemplateField(1L, "dt", false, FieldTypes.DATETIME);
		DatetimeField df = new DatetimeField(10L, null, tf);
		Component comp = new Component(100L, "Comp", "Desc", null, List.of(df));

		assertDoesNotThrow(() -> new TestNumber("component.field.value", "=", "2026-11-25T12:00:00Z").matches(comp));
	}
}
