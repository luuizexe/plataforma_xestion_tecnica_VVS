package gal.udc.fic.prperez.pleste.service.integration;

import gal.udc.fic.prperez.pleste.service.*;
import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.DatetimeField;
import gal.udc.fic.prperez.pleste.service.dao.component.Field;
import gal.udc.fic.prperez.pleste.service.dao.component.NumberField;
import gal.udc.fic.prperez.pleste.service.dao.component.TextField;
import gal.udc.fic.prperez.pleste.service.dao.template.FieldTypes;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@DisplayName("Integration Tests: SearchResource over real database & cache lifecycle")
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
		// Clean any previous cache entries
		searchResource.clearTemplateCache(null);
		searchResource.clearComponentCache(null);
		searchResource.clearUserCache(null);
	}

	@AfterEach
	void tearDown() {
		for (Long id : createdComponentIds) {
			try {
				componentResource.removeComponent(id.toString());
			} catch (Exception ignored) {
			}
		}
		createdComponentIds.clear();

		for (Long id : createdTemplateIds) {
			try {
				templateResource.removeTemplate(id.toString());
			} catch (Exception ignored) {
			}
		}
		createdTemplateIds.clear();

		for (Long id : createdUserIds) {
			try {
				usersResource.deleteUser(id.toString());
			} catch (Exception ignored) {
			}
		}
		createdUserIds.clear();

		searchResource.clearTemplateCache(null);
		searchResource.clearComponentCache(null);
		searchResource.clearUserCache(null);
	}

	private static String unique(String prefix) {
		return prefix + "_" + System.nanoTime();
	}

	@Test
	@DisplayName("End-to-end Template search by regex and cache invalidation")
	void testSearchTemplatesIntegration() {
		String suffix = unique("IT_TPL");
		Template tpl1 = new Template(null, "WeatherStation_" + suffix, "Station description", new ArrayList<>());
		Long id1 = templateResource.addTemplate(tpl1);
		createdTemplateIds.add(id1);

		String query = "template.name ~ \"WeatherStation_" + suffix + "\"";

		// Initial search
		List<Template> res1 = searchResource.searchTemplates(query, null, null);
		assertEquals(1, res1.size());
		assertEquals("WeatherStation_" + suffix, res1.get(0).getName());

		// Add second matching template
		Template tpl2 = new Template(null, "WeatherStation_" + suffix + "_B", "Station B", new ArrayList<>());
		Long id2 = templateResource.addTemplate(tpl2);
		createdTemplateIds.add(id2);

		// Without clearing cache, still returns 1 cached element
		List<Template> cached = searchResource.searchTemplates(query, null, null);
		assertEquals(1, cached.size());

		// Invalidate cache
		searchResource.clearTemplateCache(query);

		// Now finds both
		String broadQuery = "template.name ~ \"WeatherStation_" + suffix + ".*\"";
		List<Template> refreshed = searchResource.searchTemplates(broadQuery, null, null);
		assertEquals(2, refreshed.size());
	}

	@Test
	@DisplayName("End-to-end Component search with numerical and text filters")
	void testSearchComponentsIntegration() {
		String suffix = unique("IT_CMP");

		// Create template with TEXT and NUMBER fields
		TemplateField tfCode = new TemplateField(null, "code", false, FieldTypes.TEXT);
		TemplateField tfVolt = new TemplateField(null, "voltage", false, FieldTypes.NUMBER);
		Template tpl = new Template(null, "Board_" + suffix, "Board template", List.of(tfCode, tfVolt));
		Long tplId = templateResource.addTemplate(tpl);
		createdTemplateIds.add(tplId);

		Template loadedTpl = templateResource.getTemplate(tplId.toString());
		TemplateField loadedCode = loadedTpl.getFields().stream().filter(f -> f.getName().equals("code")).findFirst().orElseThrow();
		TemplateField loadedVolt = loadedTpl.getFields().stream().filter(f -> f.getName().equals("voltage")).findFirst().orElseThrow();

		// Component 1: volt = 220
		TextField fCode1 = new TextField(null, "B1", loadedCode);
		NumberField fVolt1 = new NumberField(null, new BigDecimal("220.0"), loadedVolt);
		Component c1 = new Component(null, "BoardA_" + suffix, "Unit A", loadedTpl, List.of(fCode1, fVolt1));
		Long c1Id = componentResource.addComponent(c1);
		createdComponentIds.add(c1Id);

		// Component 2: volt = 110
		TextField fCode2 = new TextField(null, "B2", loadedCode);
		NumberField fVolt2 = new NumberField(null, new BigDecimal("110.0"), loadedVolt);
		Component c2 = new Component(null, "BoardB_" + suffix, "Unit B", loadedTpl, List.of(fCode2, fVolt2));
		Long c2Id = componentResource.addComponent(c2);
		createdComponentIds.add(c2Id);

		// Search by component name and numerical voltage > 200
		String query = "component.name ~ \"BoardA_" + suffix + "\" & component.field.value > 200";
		List<Component> results = searchResource.searchComponents(query, null, null);
		assertEquals(1, results.size());
		assertEquals("BoardA_" + suffix, results.get(0).getName());
	}

	@Test
	@DisplayName("End-to-end User search by name and role")
	void testSearchUsersIntegration() {
		String username = unique("user_search");
		User u = new User(null, username, username + "@udc.es", "hash", Roles.NORMAL_USER);
		Long uid = usersResource.addUser(u);
		createdUserIds.add(uid);

		String query = "user.name ~ \"" + username + "\" & user.role @ [ \"NORMAL_USER\" ]";
		List<User> results = searchResource.searchUsers(query, null, null);
		assertEquals(1, results.size());
		assertEquals(username, results.get(0).getUsername());
	}
}
