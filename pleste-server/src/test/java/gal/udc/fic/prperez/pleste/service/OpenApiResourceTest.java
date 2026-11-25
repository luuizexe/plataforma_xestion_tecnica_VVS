package gal.udc.fic.prperez.pleste.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenApiResource Unit Tests")
public class OpenApiResourceTest {

	@Test
	@DisplayName("OpenApiResource instantiation")
	void testInstantiation() {
		OpenApiResource resource = new OpenApiResource();
		assertNotNull(resource);
	}
}
