package gal.udc.fic.prperez.pleste.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import gal.udc.fic.prperez.pleste.service.Application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Cliente HTTP mínimo para as probas de integración que precisan pasar polo filtro de autorización de Jersey.
 */
class ApiTestClient {
	static final String TOKEN_HEADER = "X-API-KEY";

	private final HttpClient http = HttpClient.newHttpClient();
	private final ObjectMapper mapper = new ObjectMapper();
	private final String baseUrl;

	ApiTestClient(int port) {
		this.baseUrl = "http://localhost:" + port + Application.BASE_URL;
	}

	HttpResponse<String> send(String method, String path, String token, Object body) {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
				.header("Accept", "application/json")
				.header("Content-Type", "application/json")
				.method(method, body == null
						? HttpRequest.BodyPublishers.noBody()
						: HttpRequest.BodyPublishers.ofString(toJson(body)));
		if (token != null) {
			request.header(TOKEN_HEADER, token);
		}

		try {
			return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	int status(String method, String path, String token, Object body) {
		return send(method, path, token, body).statusCode();
	}

	int status(String method, String path, String token) {
		return status(method, path, token, null);
	}

	/** Fai login e devolve o token, ou null se o servidor non o acepta. */
	String login(String username, String password) {
		HttpResponse<String> response = send("POST", "/users/login?user=" + username, null, Map.of("content", password));
		if (response.statusCode() != 200) {
			return null;
		}
		try {
			return mapper.readTree(response.body()).get("content").asText();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	String toJson(Object body) {
		try {
			return mapper.writeValueAsString(body);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
