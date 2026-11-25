package gal.udc.fic.prperez.pleste.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSONString Unit Tests")
public class JSONStringTest {

	@Test
	@DisplayName("Constructors, getter and toString")
	void testBasics() {
		JSONString empty = new JSONString();
		assertNull(empty.getContent());
		assertNull(empty.toString());

		JSONString str = new JSONString("hello world");
		assertEquals("hello world", str.getContent());
		assertEquals("hello world", str.toString());
	}
}
