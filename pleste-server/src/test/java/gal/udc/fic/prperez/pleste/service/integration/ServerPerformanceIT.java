package gal.udc.fic.prperez.pleste.service.integration;

import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;
import etm.core.monitor.FlatMonitor;
import etm.core.renderer.SimpleTextRenderer;
import etm.core.timer.Java15NanoTimer;
import gal.udc.fic.prperez.pleste.service.Application;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("JETM execution-time measurements over real server endpoints")
public class ServerPerformanceIT {

	private static final int SAMPLE_COUNT = 10;
	private static final EtmMonitor MONITOR = new FlatMonitor(new Java15NanoTimer());

	@LocalServerPort
	private int port;

	private ApiTestClient api;
	private String adminToken;

	@BeforeAll
	static void startMonitor() {
		MONITOR.start();
	}

	@BeforeEach
	void initializeApiClient() {
		api = new ApiTestClient(port);
		adminToken = api.login("root", "root");
		if (adminToken == null) {
			throw new IllegalStateException("Root login failed; cannot measure protected endpoints");
		}
	}

	@Test
	@DisplayName("GET /templates responds successfully and is measured")
	void getTemplatesRespondsSuccessfully() {
		assertEndpointReturnsOk("/templates");
	}

	@Test
	@DisplayName("GET /components responds successfully and is measured")
	void getComponentsRespondsSuccessfully() {
		assertEndpointReturnsOk("/components");
	}

	@Test
	@DisplayName("GET /users responds successfully and is measured")
	void getUsersRespondsSuccessfully() {
		assertEndpointReturnsOk("/users");
	}

	@Test
	@DisplayName("GET /search/templates responds successfully and is measured")
	void searchTemplatesRespondsSuccessfully() {
		assertEndpointReturnsOk("/search/templates?query=template.name%7E%22.*%22");
	}

	private void assertEndpointReturnsOk(String path) {
		boolean allRequestsSuccessful = true;
		for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
			EtmPoint point = MONITOR.createPoint("GET " + path);
			int status;
			try {
				status = api.status("GET", path, adminToken);
			} finally {
				point.collect();
			}
			allRequestsSuccessful &= status == 200;
		}

		assertTrue(allRequestsSuccessful, "One or more requests to " + path + " failed");
	}

	@AfterAll
	static void writeMeasurements() throws IOException {
		MONITOR.stop();
		MONITOR.aggregate();
		Path report = Path.of("target", "jetm-reports", "integration-measurements.txt");
		Files.createDirectories(report.getParent());
		try (Writer writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
			MONITOR.render(new SimpleTextRenderer(writer));
		}
	}
}
