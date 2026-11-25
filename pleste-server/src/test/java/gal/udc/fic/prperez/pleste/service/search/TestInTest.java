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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestIn Unit Tests")
public class TestInTest {

	@Test
	@DisplayName("TestIn toString format")
	void testToString() {
		TestIn ti = new TestIn("user.name", List.of("alice", "bob"), true);
		assertEquals("USER_NAME IN ( [alice, bob] )", ti.toString());
	}

	@Test
	@DisplayName("TestIn matches on Template fields")
	void testMatchesTemplate() {
		Template t = new Template(1L, "TemplateA", "DescriptionA", null);
		TemplateField tf = new TemplateField(10L, "field1", false, FieldTypes.TEXT);
		t.setFields(List.of(tf));

		// TEMPLATE_NAME
		assertTrue(new TestIn("template.name", List.of("TemplateA", "Other"), true).matches(t));
		assertFalse(new TestIn("template.name", List.of("Other1", "Other2"), true).matches(t));
		assertTrue(new TestIn("template.name", List.of("Other1", "Other2"), false).matches(t)); // inverted

		// TEMPLATE_DESCRIPTION
		assertTrue(new TestIn("template.description", List.of("DescriptionA"), true).matches(t));
		assertFalse(new TestIn("template.description", List.of("NonMatching"), true).matches(t));

		// TEMPLATE_FIELD_NAME
		assertTrue(new TestIn("template.field.name", List.of("field1", "field2"), true).matches(t));
		assertFalse(new TestIn("template.field.name", List.of("field2"), true).matches(t));

		// TEMPLATE_FIELD_TYPE
		assertTrue(new TestIn("template.field.type", List.of("TEXT", "NUMBER"), true).matches(t));
		assertFalse(new TestIn("template.field.type", List.of("LINK"), true).matches(t));

		// Invalid property for template throws ParseSearchException
		TestIn invalid = new TestIn("component.name", List.of("val"), true);
		assertThrows(ParseSearchException.class, () -> invalid.matches(t));
	}

	@Test
	@DisplayName("TestIn matches on Component fields (names and values)")
	void testMatchesComponent() {
		Template template = new Template(1L);
		TemplateField tfText = new TemplateField(1L, "code", false, FieldTypes.TEXT);
		TemplateField tfNum = new TemplateField(2L, "count", false, FieldTypes.NUMBER);
		TemplateField tfDate = new TemplateField(3L, "when", false, FieldTypes.DATETIME);
		TemplateField tfLink = new TemplateField(4L, "parent", false, FieldTypes.LINK);

		TextField fText = new TextField(10L, "ABC", tfText);
		NumberField fNum = new NumberField(11L, new BigDecimal("100"), tfNum);
		OffsetDateTime dt = OffsetDateTime.parse("2026-11-25T10:00:00Z");
		DatetimeField fDate = new DatetimeField(12L, new JSONDatetime(dt), tfDate);
		LinkField fLink = new LinkField(13L, new Component(42L), tfLink);

		Component comp = new Component(1L, "Pump", "Water pump", template, List.of(fText, fNum, fDate, fLink));

		// COMPONENT_NAME
		assertTrue(new TestIn("component.name", List.of("Pump", "Motor"), true).matches(comp));
		assertFalse(new TestIn("component.name", List.of("Valve"), true).matches(comp));

		// COMPONENT_DESCRIPTION
		assertTrue(new TestIn("component.description", List.of("Water pump"), true).matches(comp));
		assertFalse(new TestIn("component.description", List.of("Air pump"), true).matches(comp));

		// COMPONENT_FIELD_NAME
		assertTrue(new TestIn("component.field.name", List.of("code"), true).matches(comp));
		assertFalse(new TestIn("component.field.name", List.of("unknown"), true).matches(comp));

		// COMPONENT_FIELD_TYPE
		assertTrue(new TestIn("component.field.type", List.of("TEXT", "LINK"), true).matches(comp));
		assertFalse(new TestIn("component.field.type", List.of("CUSTOM"), true).matches(comp));

		// COMPONENT_FIELD_VALUE - TEXT
		assertTrue(new TestIn("component.field.value", List.of("ABC", "DEF"), true).matches(comp));
		assertFalse(new TestIn("component.field.value", List.of("XYZ"), true).matches(comp));

		// COMPONENT_FIELD_VALUE - NUMBER
		assertTrue(new TestIn("component.field.value", List.of("100", "200"), true).matches(comp));
		assertFalse(new TestIn("component.field.value", List.of("50"), true).matches(comp));

		// COMPONENT_FIELD_VALUE - DATETIME
		// In TestIn.java line 62: ((DatetimeField) f).getContent().toString() produces "gal.udc.fic.prperez.pleste.service.JSONDatetime@..."
		assertTrue(new TestIn("component.field.value", List.of(fDate.getContent().toString()), true).matches(comp));

		// COMPONENT_FIELD_VALUE - LINK
		assertTrue(new TestIn("component.field.value", List.of("42", "99"), true).matches(comp));
		assertFalse(new TestIn("component.field.value", List.of("99"), true).matches(comp));

		// Non-number in values for number / link field ignores NumberFormatException and returns false
		assertFalse(new TestIn("component.field.value", List.of("not-a-number"), true).matches(
				new Component(2L, "C", "D", null, List.of(fNum))));

		// Invalid property for component
		TestIn invalid = new TestIn("user.name", List.of("alice"), true);
		assertThrows(ParseSearchException.class, () -> invalid.matches(comp));
	}

	@Test
	@DisplayName("TestIn matches on User fields")
	void testMatchesUser() {
		User u = new User(1L, "bob", "bob@udc.es", "hash", Roles.NORMAL_USER);

		// USER_NAME
		assertTrue(new TestIn("user.name", List.of("alice", "bob"), true).matches(u));
		assertFalse(new TestIn("user.name", List.of("charlie"), true).matches(u));

		// USER_EMAIL
		assertTrue(new TestIn("user.email", List.of("bob@udc.es"), true).matches(u));
		assertFalse(new TestIn("user.email", List.of("other@udc.es"), true).matches(u));

		// USER_ROLE
		assertTrue(new TestIn("user.role", List.of("NORMAL_USER"), true).matches(u));
		assertFalse(new TestIn("user.role", List.of("ADMINISTRATOR"), true).matches(u));

		// Invalid property for user
		TestIn invalid = new TestIn("template.name", List.of("T"), true);
		assertThrows(ParseSearchException.class, () -> invalid.matches(u));
	}

	@Test
	@DisplayName("TestIn matches on non-target object returns false")
	void testMatchesOtherObject() {
		TestIn ti = new TestIn("user.name", List.of("a"), true);
		assertFalse(ti.matches(12345));
	}

	@Disabled("ISSUE #9: NullPointerException en TestIn cuando un campo LINK tiene content null")
	@Test
	@DisplayName("TestIn matches Component with null LINK content")
	void testMatchesComponentLinkNull() {
		TemplateField tf = new TemplateField(1L, "parent", false, FieldTypes.LINK);
		LinkField lf = new LinkField(10L, null, tf);
		Component comp = new Component(1L, "C", "D", null, List.of(lf));

		assertDoesNotThrow(() -> new TestIn("component.field.value", List.of("42"), true).matches(comp));
	}
}
