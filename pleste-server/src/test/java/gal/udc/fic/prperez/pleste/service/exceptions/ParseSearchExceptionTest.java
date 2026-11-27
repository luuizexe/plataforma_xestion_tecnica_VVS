package gal.udc.fic.prperez.pleste.service.exceptions;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParseSearchException Unit Tests")
public class ParseSearchExceptionTest {

	@Test
	@DisplayName("ParseSearchException inherits from RESTException")
	void testInheritsFromRestException() {
		assertInstanceOf(RESTException.class, new ParseSearchException("error"));
	}

	@Test
	@DisplayName("ParseSearchException uses HTTP 400 Bad Request status")
	void testStatusIsBadRequest() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), new ParseSearchException("error").getStatus());
	}

	@ParameterizedTest
	@ValueSource(strings = {"Invalid query", "", "Unknown property: foo"})
	@DisplayName("ParseSearchException preserves provided message")
	void testPreservesMessage(String message) {
		assertEquals(message, new ParseSearchException(message).getMessage());
	}

	@Test
	@DisplayName("ParseSearchException preserves null message")
	void testPreservesNullMessage() {
		assertNull(new ParseSearchException(null).getMessage());
	}

	@RepeatedTest(5)
	@DisplayName("ParseSearchException preserves random messages")
	void testPreservesRandomMessage() {
		String message = "err-" + UUID.randomUUID();
		assertEquals(message, new ParseSearchException(message).getMessage());
	}
}
