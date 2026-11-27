package gal.udc.fic.prperez.pleste.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSONString Unit Tests")
public class JSONStringTest {

	@Test
	@DisplayName("Default constructor leaves content as null")
	void testDefaultConstructorContentNull() {
		assertNull(new JSONString().getContent());
	}

	@Test
	@DisplayName("Default constructor leaves toString as null")
	void testDefaultConstructorToStringNull() {
		assertNull(new JSONString().toString());
	}

	@ParameterizedTest
	@ValueSource(strings = {"hello world", "", " ", "line1\nline2", "áéíóúñ"})
	@DisplayName("Parameterized constructor stores given content")
	void testParameterizedConstructorStoresContent(String value) {
		assertEquals(value, new JSONString(value).getContent());
	}

	@Test
	@DisplayName("Parameterized constructor accepts null")
	void testParameterizedConstructorNull() {
		assertNull(new JSONString(null).getContent());
	}

	@ParameterizedTest
	@ValueSource(strings = {"hello world", "", "special !@#$%"})
	@DisplayName("toString returns stored content")
	void testToStringReturnsContent(String value) {
		assertEquals(value, new JSONString(value).toString());
	}

	@RepeatedTest(5)
	@DisplayName("Random strings are preserved by getContent")
	void testRandomStringPreserved() {
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		int len = random.nextInt(32);
		for (int i = 0; i < len; i++) {
			sb.append((char) ('a' + random.nextInt(26)));
		}
		String generated = sb.toString();
		assertEquals(generated, new JSONString(generated).getContent());
	}
}
