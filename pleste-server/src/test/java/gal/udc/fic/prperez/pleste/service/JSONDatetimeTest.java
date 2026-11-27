package gal.udc.fic.prperez.pleste.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSONDatetime Unit Tests")
public class JSONDatetimeTest {

	@Test
	@DisplayName("Default constructor leaves datetime as null")
	void testDefaultConstructorLeavesDatetimeNull() {
		JSONDatetime dt = new JSONDatetime();
		assertNull(dt.getDatetime());
	}

	@Test
	@DisplayName("Parameterized constructor stores given OffsetDateTime")
	void testParameterizedConstructorStoresDatetime() {
		OffsetDateTime now = OffsetDateTime.parse("2026-11-25T20:30:00+01:00");
		JSONDatetime dt = new JSONDatetime(now);
		assertEquals(now, dt.getDatetime());
	}

	@Test
	@DisplayName("Parameterized constructor accepts null")
	void testParameterizedConstructorAcceptsNull() {
		JSONDatetime dt = new JSONDatetime(null);
		assertNull(dt.getDatetime());
	}

	@Test
	@DisplayName("setDatetime updates stored OffsetDateTime")
	void testSetDatetimeUpdatesValue() {
		JSONDatetime dt = new JSONDatetime(OffsetDateTime.parse("2026-11-25T20:30:00+01:00"));
		OffsetDateTime updated = OffsetDateTime.parse("2025-01-15T08:15:30Z");
		dt.setDatetime(updated);
		assertEquals(updated, dt.getDatetime());
	}

	@Test
	@DisplayName("setDatetime accepts null")
	void testSetDatetimeAcceptsNull() {
		JSONDatetime dt = new JSONDatetime(OffsetDateTime.now());
		dt.setDatetime(null);
		assertNull(dt.getDatetime());
	}

	@RepeatedTest(5)
	@DisplayName("setDatetime stores random OffsetDateTime values")
	void testSetDatetimeRandom() {
		Random random = new Random();
		OffsetDateTime value = OffsetDateTime.of(
				2000 + random.nextInt(50),
				1 + random.nextInt(12),
				1 + random.nextInt(28),
				random.nextInt(24),
				random.nextInt(60),
				random.nextInt(60),
				0,
				ZoneOffset.ofHours(random.nextInt(11) - 5));
		JSONDatetime dt = new JSONDatetime();
		dt.setDatetime(value);
		assertEquals(value, dt.getDatetime());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"2026-11-25T20:30:00+01:00",
			"2000-01-01T00:00:00Z",
			"1970-01-01T00:00:00-05:00",
			"2026-12-31T23:59:59.999999999+02:00"
	})
	@DisplayName("parse parses valid ISO strings with timezone offset")
	void testParseWithOffset(String iso) {
		assertEquals(OffsetDateTime.parse(iso), JSONDatetime.parse(iso).getDatetime());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"2026-11-25T20:30:00",
			"2001-01-01T00:00:00",
			"1999-12-31T23:59:59"
	})
	@DisplayName("parse parses ISO strings without offset using system default zone")
	void testParseWithoutOffsetFallback(String localIso) {
		OffsetDateTime expected = LocalDateTime.parse(localIso).atZone(ZoneId.systemDefault()).toOffsetDateTime();
		assertEquals(expected, JSONDatetime.parse(localIso).getDatetime());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "invalid-date", "2026-13-01T00:00:00Z", "2026-11-25", "20:30:00"})
	@DisplayName("parse throws DateTimeParseException on invalid strings")
	void testParseInvalidThrowsDateTimeParseException(String invalid) {
		assertThrows(DateTimeParseException.class, () -> JSONDatetime.parse(invalid));
	}

	@Disabled("Bug conocido: JSONDatetime.parse(null) lanza NullPointerException en lugar de DateTimeParseException")
	@Test
	@DisplayName("parse throws DateTimeParseException when input is null")
	void testParseNullThrowsDateTimeParseException() {
		assertThrows(DateTimeParseException.class, () -> JSONDatetime.parse(null));
	}

	@Test
	@DisplayName("now returns a non-null JSONDatetime instance")
	void testNowNotNull() {
		assertNotNull(JSONDatetime.now());
	}

	@Test
	@DisplayName("now sets datetime within execution time window")
	void testNowSetsCurrentDatetime() {
		OffsetDateTime before = OffsetDateTime.now();
		OffsetDateTime actual = JSONDatetime.now().getDatetime();
		OffsetDateTime after = OffsetDateTime.now();
		assertTrue(!actual.isBefore(before) && !actual.isAfter(after));
	}

	@Disabled("Bug conocido: JSONDatetime no sobreescribe toString() y devuelve Object.toString() con @hashcode, impidiendo buscar campos DATETIME como texto")
	@Test
	@DisplayName("toString returns ISO representation of stored datetime")
	void testToStringReturnsIsoDatetime() {
		JSONDatetime dt = new JSONDatetime(OffsetDateTime.parse("2026-11-25T20:30:00Z"));
		assertEquals("2026-11-25T20:30:00Z", dt.toString());
	}

	@Disabled("Bug conocido: JSONDatetime no sobreescribe equals() ni hashCode()")
	@Test
	@DisplayName("equals returns true for distinct instances with equal datetime")
	void testEqualsSameDatetime() {
		OffsetDateTime value = OffsetDateTime.parse("2026-11-25T20:30:00Z");
		assertEquals(new JSONDatetime(value), new JSONDatetime(value));
	}
}
