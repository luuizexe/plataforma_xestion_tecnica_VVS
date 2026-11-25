package gal.udc.fic.prperez.pleste.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSONDatetime Unit Tests")
public class JSONDatetimeTest {

	@Test
	@DisplayName("Constructors, getters, setters and toString")
	void testBasics() {
		JSONDatetime dt = new JSONDatetime();
		assertNull(dt.getDatetime());

		OffsetDateTime now = OffsetDateTime.now();
		JSONDatetime dtParam = new JSONDatetime(now);
		assertEquals(now, dtParam.getDatetime());
		// JSONDatetime does not override toString(), so it inherits Object.toString()
		assertTrue(dtParam.toString().startsWith("gal.udc.fic.prperez.pleste.service.JSONDatetime@"));

		OffsetDateTime other = now.minusDays(1);
		dtParam.setDatetime(other);
		assertEquals(other, dtParam.getDatetime());
	}

	@Test
	@DisplayName("parse static method parses valid ISO strings")
	void testParseSuccess() {
		String iso = "2026-11-25T20:30:00+01:00";
		JSONDatetime parsed = JSONDatetime.parse(iso);
		assertNotNull(parsed);
		assertEquals(OffsetDateTime.parse(iso), parsed.getDatetime());
	}

	@Test
	@DisplayName("parse static method throws DateTimeParseException on invalid string")
	void testParseFailure() {
		assertThrows(DateTimeParseException.class, () -> JSONDatetime.parse("invalid-date"));
	}

	@Test
	@DisplayName("now static method returns current datetime")
	void testNow() {
		JSONDatetime now = JSONDatetime.now();
		assertNotNull(now);
		assertNotNull(now.getDatetime());
	}
}
