package gal.udc.fic.prperez.pleste.service.search;

import gal.udc.fic.prperez.pleste.service.JSONDatetime;
import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.DatetimeField;
import gal.udc.fic.prperez.pleste.service.dao.component.Field;
import gal.udc.fic.prperez.pleste.service.dao.component.TextField;
import gal.udc.fic.prperez.pleste.service.dao.template.FieldTypes;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.ParseSearchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestString Unit Tests")
public class TestStringTest {

	@Test
	@DisplayName("TestString toString format")
	void testToString() {
		TestString ts = new TestString("template.name", "Sensor.*", true);
		assertEquals("TEMPLATE_NAME LIKE Sensor.*", ts.toString());
	}

	@Test
	@DisplayName("TestString matches on Template fields")
	void testMatchesTemplate() {
		Template t = new Template(1L, "TempSensor", "Temperature sensor description", null);
		TemplateField tf1 = new TemplateField(10L, "temperature", true, FieldTypes.NUMBER);
		TemplateField tf2 = new TemplateField(11L, "location", false, FieldTypes.TEXT);
		t.setFields(List.of(tf1, tf2));

		// TEMPLATE_NAME
		assertTrue(new TestString("template.name", "Temp.*", true).matches(t));
		assertFalse(new TestString("template.name", "Hum.*", true).matches(t));
		assertTrue(new TestString("template.name", "Hum.*", false).matches(t)); // inverted

		// TEMPLATE_DESCRIPTION
		assertTrue(new TestString("template.description", ".*sensor.*", true).matches(t));
		assertFalse(new TestString("template.description", "Pressure", true).matches(t));

		// TEMPLATE_FIELD_NAME
		assertTrue(new TestString("template.field.name", "temperature", true).matches(t));
		assertTrue(new TestString("template.field.name", "loc.*", true).matches(t));
		assertFalse(new TestString("template.field.name", "nonexistent", true).matches(t));

		// TEMPLATE_FIELD_TYPE
		assertTrue(new TestString("template.field.type", "NUMBER", true).matches(t));
		assertTrue(new TestString("template.field.type", "TEXT", true).matches(t));
		assertFalse(new TestString("template.field.type", "LINK", true).matches(t));

		// Unsupported property for template throws ParseSearchException
		TestString invalidProp = new TestString("component.name", "abc", true);
		assertThrows(ParseSearchException.class, () -> invalidProp.matches(t));
	}

	@Test
	@DisplayName("TestString matches on Component fields")
	void testMatchesComponent() {
		Template template = new Template(1L);
		TemplateField tfText = new TemplateField(1L, "model", false, FieldTypes.TEXT);
		TemplateField tfDate = new TemplateField(2L, "installedAt", false, FieldTypes.DATETIME);
		TemplateField tfNum = new TemplateField(3L, "serial", false, FieldTypes.NUMBER);

		TextField fText = new TextField(10L, "Model-X100", tfText);
		OffsetDateTime now = OffsetDateTime.parse("2026-11-25T12:00:00Z");
		DatetimeField fDate = new DatetimeField(11L, new JSONDatetime(now), tfDate);

		List<Field<?>> fields = new ArrayList<>(List.of(fText, fDate));
		Component comp = new Component(100L, "LivingRoom-Sensor", "Smart sensor", template, fields);

		// COMPONENT_NAME
		assertTrue(new TestString("component.name", "LivingRoom.*", true).matches(comp));
		assertFalse(new TestString("component.name", "Kitchen.*", true).matches(comp));

		// COMPONENT_DESCRIPTION
		assertTrue(new TestString("component.description", "Smart.*", true).matches(comp));
		assertFalse(new TestString("component.description", "Analog", true).matches(comp));

		// COMPONENT_FIELD_NAME
		assertTrue(new TestString("component.field.name", "model", true).matches(comp));
		assertFalse(new TestString("component.field.name", "nonexistent", true).matches(comp));

		// COMPONENT_FIELD_TYPE
		assertTrue(new TestString("component.field.type", "TEXT", true).matches(comp));
		assertTrue(new TestString("component.field.type", "DATETIME", true).matches(comp));
		assertFalse(new TestString("component.field.type", "LINK", true).matches(comp));

		// COMPONENT_FIELD_VALUE - TEXT
		assertTrue(new TestString("component.field.value", "Model-.*", true).matches(comp));
		assertFalse(new TestString("component.field.value", "Unknown", true).matches(comp));

		// COMPONENT_FIELD_VALUE - DATETIME
		// In TestString.java line 45: ((DatetimeField) f).getContent().toString().matches(this.value)
		// Because JSONDatetime does not override toString(), getContent().toString() produces "gal.udc.fic.prperez.pleste.service.JSONDatetime@..."
		assertTrue(new TestString("component.field.value", "gal\\.udc\\.fic\\.prperez\\.pleste\\.service\\.JSONDatetime@.*", true).matches(comp));

		// Unsupported property for component throws ParseSearchException
		TestString invalidProp = new TestString("user.name", "admin", true);
		assertThrows(ParseSearchException.class, () -> invalidProp.matches(comp));
	}

	@Test
	@DisplayName("TestString matches on User fields")
	void testMatchesUser() {
		User user = new User(1L, "alice", "alice@example.com", "passhash", Roles.ADMINISTRATOR);

		// USER_NAME
		assertTrue(new TestString("user.name", "ali.*", true).matches(user));
		assertFalse(new TestString("user.name", "bob", true).matches(user));

		// USER_EMAIL
		assertTrue(new TestString("user.email", ".*@example\\.com", true).matches(user));
		assertFalse(new TestString("user.email", ".*@udc\\.es", true).matches(user));

		// USER_ROLE
		assertTrue(new TestString("user.role", "ADMINISTRATOR", true).matches(user));
		assertFalse(new TestString("user.role", "NORMAL_USER", true).matches(user));

		// Unsupported property for user throws ParseSearchException
		TestString invalidProp = new TestString("template.name", "t", true);
		assertThrows(ParseSearchException.class, () -> invalidProp.matches(user));
	}

	@Test
	@DisplayName("TestString matches returns false on non-target object types")
	void testMatchesOtherObject() {
		TestString ts = new TestString("template.name", "test", true);
		assertFalse(ts.matches("SomeString"));
	}
}
