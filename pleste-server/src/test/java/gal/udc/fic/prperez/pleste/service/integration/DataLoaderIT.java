package gal.udc.fic.prperez.pleste.service.integration;

import gal.udc.fic.prperez.pleste.service.Application;
import gal.udc.fic.prperez.pleste.service.DataLoader;
import gal.udc.fic.prperez.pleste.service.TemplateResource;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@DisplayName("Integration Tests: initial data loading")
public class DataLoaderIT {

	@Autowired
	private DataLoader dataLoader;

	@Autowired
	private SQLUserDao userDatabase;

	@Autowired
	private TemplateResource templateResource;

	@BeforeEach
	void runLoaderAgain() {
		// O DataLoader xa se executou ao arrancar; volver executalo simula un reinicio con datos persistidos
		dataLoader.run(null);
	}

	@Test
	@DisplayName("Running the loader again keeps a single root user")
	void testSingleRootUser() {
		assertEquals(1, userDatabase.findAll().stream().filter(u -> "root".equals(u.getUsername())).count());
	}

	@Test
	@DisplayName("The root user is an administrator")
	void testRootIsAdministrator() {
		assertEquals(Roles.ADMINISTRATOR, userDatabase.getByUsername("root").getRole());
	}

	@ParameterizedTest
	@ValueSource(strings = {"Ordenador", "Licencia", "Programa"})
	@DisplayName("Running the loader again keeps a single copy of each default template")
	void testSingleDefaultTemplate(String name) {
		assertEquals(1, templateResource.getTemplatesByName(name).size());
	}
}
