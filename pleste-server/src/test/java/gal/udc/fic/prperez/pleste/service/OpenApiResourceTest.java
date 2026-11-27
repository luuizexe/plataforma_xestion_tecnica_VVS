package gal.udc.fic.prperez.pleste.service;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenApiResource Unit Tests")
public class OpenApiResourceTest {

	@ApplicationPath("/api/v0")
	private static final class StubApplication extends Application {
	}

	@Mock
	private ServletConfig servletConfig;

	@Mock
	private ServletContext servletContext;

	@Mock
	private HttpHeaders httpHeaders;

	@Mock
	private UriInfo uriInfo;

	@Test
	@DisplayName("OpenApiResource instantiation succeeds")
	void testInstantiation() {
		assertNotNull(new OpenApiResource());
	}

	@Test
	@DisplayName("OpenApiResource stores configured resource packages")
	void testSetResourcePackages() {
		OpenApiResource resource = new OpenApiResource();
		Set<String> packages = Set.of("gal.udc.fic.prperez.pleste.service");
		resource.setResourcePackages(packages);
		assertEquals(packages, resource.getResourcePackages());
	}

	@Test
	@DisplayName("OpenApiResource stores configured config location")
	void testSetConfigLocation() {
		OpenApiResource resource = new OpenApiResource();
		resource.setConfigLocation("openapi.yaml");
		assertEquals("openapi.yaml", resource.getConfigLocation());
	}

	@Test
	@DisplayName("getOpenApi returns HTTP 200 response with mocked JAX-RS context")
	void testGetOpenApiReturnsOkWithMockedContext() throws Exception {
		OpenApiResource resource = new OpenApiResource();
		resource.config = servletConfig;
		resource.app = new StubApplication();
		resource.setResourcePackages(Set.of("gal.udc.fic.prperez.pleste.service"));

		when(servletConfig.getServletName()).thenReturn("pleste-unit-test");
		when(servletConfig.getServletContext()).thenReturn(servletContext);

		Response response = resource.getOpenApi(httpHeaders, uriInfo);
		assertEquals(200, response.getStatus());
	}
}
