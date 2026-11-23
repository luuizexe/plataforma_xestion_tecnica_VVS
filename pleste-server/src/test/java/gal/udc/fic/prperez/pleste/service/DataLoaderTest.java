package gal.udc.fic.prperez.pleste.service;

import gal.udc.fic.prperez.pleste.service.dao.SQLDaoFactoryUtil;
import gal.udc.fic.prperez.pleste.service.dao.template.SQLTemplateDao;
import gal.udc.fic.prperez.pleste.service.dao.template.SQLTemplateFieldDao;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLUserDao;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataLoader Unit Tests with Mocks")
public class DataLoaderTest {

	@Mock
	private SQLUserDao userDatabase;

	@Mock
	private SQLTemplateDao templateDatabase;

	@Mock
	private SQLTemplateFieldDao templateFieldDatabase;

	private DataLoader dataLoader;

	@BeforeEach
	void setUp() {
		SQLDaoFactoryUtil factory = new SQLDaoFactoryUtil(templateDatabase, null, templateFieldDatabase, null, userDatabase, null);
		when(templateDatabase.save(any(Template.class))).thenAnswer(invocation -> invocation.getArgument(0));
		dataLoader = new DataLoader(userDatabase, factory);
	}

	private User savedRoot() {
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		dataLoader.run(null);
		verify(userDatabase).save(saved.capture());
		return saved.getValue();
	}

	private Template savedTemplate(String name) {
		ArgumentCaptor<Template> saved = ArgumentCaptor.forClass(Template.class);
		dataLoader.run(null);
		verify(templateDatabase, atLeastOnce()).save(saved.capture());
		return saved.getAllValues().stream().filter(t -> name.equals(t.getName())).findFirst().orElseThrow();
	}

	// ==========================================
	// Usuario root
	// ==========================================

	@Test
	@DisplayName("Without root user, a user called root is created")
	void testRootUsername() {
		assertEquals("root", savedRoot().getUsername());
	}

	@Test
	@DisplayName("The root user gets the root@localhost email")
	void testRootEmail() {
		assertEquals("root@localhost", savedRoot().getEmail());
	}

	@Test
	@DisplayName("The root user password is the BCrypt hash of root")
	void testRootPassword() {
		assertTrue(new BCryptPasswordEncoder().matches("root", savedRoot().getPassword()));
	}

	@Test
	@DisplayName("The root user is an administrator")
	void testRootRole() {
		assertEquals(Roles.ADMINISTRATOR, savedRoot().getRole());
	}

	@Test
	@DisplayName("If the root user already exists, no user is created")
	void testRootAlreadyExists() {
		when(userDatabase.existsByUsername("root")).thenReturn(true);
		dataLoader.run(null);
		verify(userDatabase, never()).save(any());
	}

	// ==========================================
	// Plantillas por defecto
	// ==========================================

	@Test
	@DisplayName("Without default templates, the three of them are created in order")
	void testDefaultTemplatesCreated() {
		ArgumentCaptor<Template> saved = ArgumentCaptor.forClass(Template.class);
		dataLoader.run(null);
		verify(templateDatabase, times(3)).save(saved.capture());
		assertEquals(List.of("Ordenador", "Licencia", "Programa"), saved.getAllValues().stream().map(Template::getName).toList());
	}

	@ParameterizedTest
	@CsvSource({
			"Ordenador, Ordenadores",
			"Licencia, Licencias mercadas",
			"Programa, Programas instalados"
	})
	@DisplayName("Each default template gets its description")
	void testDefaultTemplateDescription(String name, String description) {
		assertEquals(description, savedTemplate(name).getDescription());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Ordenador | Modelo;Data de compra;Sistema operativo",
			"Licencia  | Programa;Data de compra;Data limite",
			"Programa  | Nome;Data de compra;Instalacións"
	})
	@DisplayName("Each default template gets its fields")
	void testDefaultTemplateFields(String name, String fields) {
		assertEquals(List.of(fields.split(";")), savedTemplate(name).getFields().stream().map(TemplateField::getName).toList());
	}

	@Test
	@DisplayName("If the default templates already exist, no template is created")
	void testDefaultTemplatesAlreadyExist() {
		when(templateDatabase.findByName(any())).thenReturn(List.of(new Template()));
		dataLoader.run(null);
		verify(templateDatabase, never()).save(any());
	}
}
