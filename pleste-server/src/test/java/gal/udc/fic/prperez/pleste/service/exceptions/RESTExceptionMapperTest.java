package gal.udc.fic.prperez.pleste.service.exceptions;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RESTExceptionMapper Unit Tests")
public class RESTExceptionMapperTest {

	private final RESTExceptionMapper mapper = new RESTExceptionMapper();

	@ParameterizedTest
	@ValueSource(ints = {400, 401, 403, 404, 409})
	@DisplayName("A RESTException is answered with its own status")
	void testRESTExceptionStatus(int status) {
		assertEquals(status, mapper.toResponse(new RESTException("error", status)).getStatus());
	}

	@Test
	@DisplayName("A RESTException is answered as JSON")
	void testRESTExceptionMediaType() {
		assertEquals(MediaType.APPLICATION_JSON_TYPE, mapper.toResponse(new RESTException("error", Status.NOT_FOUND)).getMediaType());
	}

	@Test
	@DisplayName("A RESTException body contains its message")
	void testRESTExceptionBodyMessage() {
		assertTrue(mapper.toResponse(new RESTException("Template 3 wasn't found", Status.NOT_FOUND)).getEntity().toString()
				.contains("\"message\":\"Template 3 wasn't found\""));
	}

	@Test
	@DisplayName("A RESTException body contains its status")
	void testRESTExceptionBodyStatus() {
		assertTrue(mapper.toResponse(new RESTException("error", Status.CONFLICT)).getEntity().toString()
				.contains("\"status\":409"));
	}

	@Test
	@DisplayName("A JAX-RS NotFoundException keeps its 404 response")
	void testClientErrorNotFound() {
		assertEquals(404, mapper.toResponse(new NotFoundException()).getStatus());
	}

	@Test
	@DisplayName("A JAX-RS BadRequestException keeps its 400 response")
	void testClientErrorBadRequest() {
		assertEquals(400, mapper.toResponse(new BadRequestException()).getStatus());
	}

	@Test
	@DisplayName("A JAX-RS NotAllowedException keeps its 405 response")
	void testClientErrorNotAllowed() {
		assertEquals(405, mapper.toResponse(new NotAllowedException("GET")).getStatus());
	}

	@Test
	@DisplayName("Any other exception is rethrown so the server answers 500")
	void testOtherExceptionIsRethrown() {
		assertThrows(RuntimeException.class, () -> mapper.toResponse(new IllegalStateException("boom")));
	}
}
