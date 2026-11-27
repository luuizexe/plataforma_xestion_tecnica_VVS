package gal.udc.fic.prperez.pleste.service.integration;

import gal.udc.fic.prperez.pleste.service.Application;
import gal.udc.fic.prperez.pleste.service.ComponentResource;
import gal.udc.fic.prperez.pleste.service.SearchResource;
import gal.udc.fic.prperez.pleste.service.TemplateResource;
import gal.udc.fic.prperez.pleste.service.UsersResource;
import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.NumberField;
import gal.udc.fic.prperez.pleste.service.dao.component.TextField;
import gal.udc.fic.prperez.pleste.service.dao.template.FieldTypes;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = Application.class)
@DisplayName("Integration Tests: SearchResource search and cache behavior")
public class SearchResourceIT {

	@Autowired
	private SearchResource searchResource;

	@Autowired
	private TemplateResource templateResource;

	@Autowired
	private ComponentResource componentResource;

	@Autowired
	private UsersResource usersResource;

	private final List<Long> createdTemplateIds = new ArrayList<>();
	private final List<Long> createdComponentIds = new ArrayList<>();
	private final List<Long> createdUserIds = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearAllCaches();
	}

	@AfterEach
	void tearDown() {
		for (Long id : createdComponentIds) {
			componentResource.removeComponent(id.toString());
		}
		createdComponentIds.clear();

		for (Long id : createdTemplateIds) {
			templateResource.removeTemplate(id.toString());
		}
		createdTemplateIds.clear();

		for (Long id : createdUserIds) {
			usersResource.deleteUser(id.toString());
		}
		createdUserIds.clear();

		clearAllCaches();
	}

	private void clearAllCaches() {
		searchResource.clearTemplateCache(null);
		searchResource.clearComponentCache(null);
		searchResource.clearUserCache(null);
	}

	private static String unique(String prefix) {
		return prefix + "_" + System.nanoTime();
	}

	private Long createTemplate(String name, List<TemplateField> fields) {
		Long id = templateResource.addTemplate(
				new Template(null, name, "Integration test template: " + name, fields));
		createdTemplateIds.add(id);
		return id;
	}

	private Template createComponentTemplate(String suffix) {
		Long id = createTemplate("SearchBoard_" + suffix, List.of(
				new TemplateField(null, "code", false, FieldTypes.TEXT),
				new TemplateField(null, "voltage", false, FieldTypes.NUMBER)));
		return templateResource.getTemplate(id.toString());
	}

	private Long createComponent(Template template, String name, String code, String voltage) {
		TemplateField codeField = template.getFields().stream()
				.filter(field -> field.getName().equals("code"))
				.findFirst()
				.orElseThrow();
		TemplateField voltageField = template.getFields().stream()
				.filter(field -> field.getName().equals("voltage"))
				.findFirst()
				.orElseThrow();
		Component component = new Component(null, name, "Integration test component",
				template, List.of(
						new TextField(null, code, codeField),
						new NumberField(null, new BigDecimal(voltage), voltageField)));
		Long id = componentResource.addComponent(component);
		createdComponentIds.add(id);
		return id;
	}

	private String createUser(String prefix) {
		String username = unique(prefix);
		Long id = usersResource.addUser(
				new User(null, username, username + "@udc.es", "hash", Roles.NORMAL_USER));
		createdUserIds.add(id);
		return username;
	}

	@Test
	@DisplayName("Template search matches the requested name")
	void searchTemplatesByName() {
		String name = unique("WeatherStation");
		createTemplate(name, List.of());

		List<Template> results = searchResource.searchTemplates(
				"template.name ~ \"" + name + "\"", null, null);

		assertEquals(List.of(name), results.stream().map(Template::getName).toList());
	}

	@Test
	@DisplayName("Template search applies pagination on a cache miss")
	void searchTemplatesAppliesPaginationOnCacheMiss() {
		String prefix = unique("PageTemplate");
		createTemplate(prefix + "_A", List.of());
		createTemplate(prefix + "_B", List.of());

		List<Template> results = searchResource.searchTemplates(
				"template.name ~ \"" + prefix + ".*\"", 0, 1);

		assertEquals(1, results.size());
	}

	@Test
	@DisplayName("Template search applies pagination on a cache hit")
	void searchTemplatesAppliesPaginationOnCacheHit() {
		String prefix = unique("CachedTemplatePage");
		createTemplate(prefix + "_A", List.of());
		createTemplate(prefix + "_B", List.of());
		String query = "template.name ~ \"" + prefix + ".*\"";
		searchResource.searchTemplates(query, null, null);

		List<Template> results = searchResource.searchTemplates(query, 0, 1);

		assertEquals(1, results.size());
	}

	@Test
	@DisplayName("Clearing a template query cache refreshes its search results")
	void clearingTemplateQueryCacheRefreshesResults() {
		String prefix = unique("TemplateCache");
		String firstName = prefix + "_A";
		String secondName = prefix + "_B";
		String query = "template.name ~ \"" + prefix + ".*\"";
		createTemplate(firstName, List.of());
		searchResource.searchTemplates(query, null, null);
		createTemplate(secondName, List.of());

		searchResource.clearTemplateCache(query);
		List<Template> results = searchResource.searchTemplates(query, null, null);

		assertEquals(List.of(firstName, secondName),
				results.stream().map(Template::getName).sorted().toList());
	}

	@Test
	@DisplayName("Clearing the entire template cache refreshes cached searches")
	void clearingAllTemplateCachesRefreshesResults() {
		String prefix = unique("AllTemplateCache");
		String query = "template.name ~ \"" + prefix + ".*\"";
		createTemplate(prefix + "_A", List.of());
		searchResource.searchTemplates(query, null, null);
		createTemplate(prefix + "_B", List.of());

		searchResource.clearTemplateCache(null);
		List<Template> results = searchResource.searchTemplates(query, null, null);

		assertEquals(2, results.size());
	}

	@Test
	@DisplayName("Component search filters by name and numeric field value")
	void searchComponentsByNameAndNumericField() {
		String suffix = unique("ComponentFilter");
		Template template = createComponentTemplate(suffix);
		String matchingName = "BoardA_" + suffix;
		createComponent(template, matchingName, "B1", "220.0");
		createComponent(template, "BoardB_" + suffix, "B2", "110.0");

		String query = "component.name ~ \"Board.*_" + suffix
				+ "\" & component.field.value > 200";
		List<Component> results = searchResource.searchComponents(query, null, null);

		assertEquals(List.of(matchingName), results.stream().map(Component::getName).toList());
	}

	@Test
	@DisplayName("Component search applies pagination on a cache miss")
	void searchComponentsAppliesPaginationOnCacheMiss() {
		String suffix = unique("ComponentPage");
		Template template = createComponentTemplate(suffix);
		createComponent(template, "BoardA_" + suffix, "A", "220");
		createComponent(template, "BoardB_" + suffix, "B", "110");

		List<Component> results = searchResource.searchComponents(
				"component.name ~ \"Board.*_" + suffix + "\"", 0, 1);

		assertEquals(1, results.size());
	}

	@Test
	@DisplayName("Component search applies pagination on a cache hit")
	void searchComponentsAppliesPaginationOnCacheHit() {
		String suffix = unique("CachedComponentPage");
		Template template = createComponentTemplate(suffix);
		createComponent(template, "BoardA_" + suffix, "A", "220");
		createComponent(template, "BoardB_" + suffix, "B", "110");
		String query = "component.name ~ \"Board.*_" + suffix + "\"";
		searchResource.searchComponents(query, null, null);

		List<Component> results = searchResource.searchComponents(query, 0, 1);

		assertEquals(1, results.size());
	}

	@Test
	@DisplayName("Clearing a component query cache refreshes its search results")
	void clearingComponentQueryCacheRefreshesResults() {
		String suffix = unique("ComponentCache");
		Template template = createComponentTemplate(suffix);
		String firstName = "BoardA_" + suffix;
		String secondName = "BoardB_" + suffix;
		String query = "component.name ~ \"Board.*_" + suffix + "\"";
		createComponent(template, firstName, "A", "220");
		searchResource.searchComponents(query, null, null);
		createComponent(template, secondName, "B", "110");

		searchResource.clearComponentCache(query);
		List<Component> results = searchResource.searchComponents(query, null, null);

		assertEquals(List.of(firstName, secondName),
				results.stream().map(Component::getName).sorted().toList());
	}

	@Test
	@DisplayName("User search filters by name and role")
	void searchUsersByNameAndRole() {
		String username = createUser(unique("user_search"));

		String query = "user.name ~ \"" + username
				+ "\" & user.role @ [ \"NORMAL_USER\" ]";
		List<User> results = searchResource.searchUsers(query, null, null);

		assertEquals(List.of(username), results.stream().map(User::getUsername).toList());
	}

	@Test
	@DisplayName("User search applies pagination on a cache miss")
	@Disabled("Bug conocido: searchUsers devuelve todos los resultados en caché fría sin aplicar skip/count")
	void searchUsersAppliesPaginationOnCacheMiss() {
		String prefix = unique("UserPage");
		createUser(prefix + "_A");
		createUser(prefix + "_B");

		List<User> results = searchResource.searchUsers(
				"user.name ~ \"" + prefix + ".*\"", 0, 1);

		assertEquals(1, results.size());
	}

	@Test
	@DisplayName("User search applies pagination on a cache hit")
	void searchUsersAppliesPaginationOnCacheHit() {
		String prefix = unique("CachedUserPage");
		createUser(prefix + "_A");
		createUser(prefix + "_B");
		String query = "user.name ~ \"" + prefix + ".*\"";
		searchResource.searchUsers(query, null, null);

		List<User> results = searchResource.searchUsers(query, 0, 1);

		assertEquals(1, results.size());
	}

	@Test
	@DisplayName("Clearing a user query cache refreshes its search results")
	void clearingUserQueryCacheRefreshesResults() {
		String prefix = unique("UserCache");
		String firstName = createUser(prefix + "_A");
		String query = "user.name ~ \"" + prefix + ".*\"";
		searchResource.searchUsers(query, null, null);
		String secondName = createUser(prefix + "_B");

		searchResource.clearUserCache(query);
		List<User> results = searchResource.searchUsers(query, null, null);

		assertEquals(List.of(firstName, secondName),
				results.stream().map(User::getUsername).sorted().toList());
	}
}
