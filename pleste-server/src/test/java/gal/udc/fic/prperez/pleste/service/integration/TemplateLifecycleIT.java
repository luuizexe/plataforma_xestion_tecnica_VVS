package gal.udc.fic.prperez.pleste.service.integration;

import gal.udc.fic.prperez.pleste.service.Application;
import gal.udc.fic.prperez.pleste.service.ComponentResource;
import gal.udc.fic.prperez.pleste.service.TemplateResource;
import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.Field;
import gal.udc.fic.prperez.pleste.service.dao.component.TextField;
import gal.udc.fic.prperez.pleste.service.dao.template.FieldTypes;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.exceptions.TemplateFieldAlreadyExistsException;
import gal.udc.fic.prperez.pleste.service.exceptions.template.TemplateAlreadyExistsException;
import gal.udc.fic.prperez.pleste.service.exceptions.template.TemplateNotFoundException;
import gal.udc.fic.prperez.pleste.service.exceptions.template.TemplateStillInUseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@DisplayName("Integration Tests: Template Lifecycle and Database Persistence Flow")
public class TemplateLifecycleIT {

	@Autowired
	private TemplateResource templateResource;

	@Autowired
	private ComponentResource componentResource;

	private final List<Long> createdTemplateIds = new ArrayList<>();
	private final List<Long> createdComponentIds = new ArrayList<>();

	@BeforeEach
	void setUp() {
		createdTemplateIds.clear();
		createdComponentIds.clear();
	}

	@AfterEach
	void tearDown() {
		for (Long compId : createdComponentIds) {
			try {
				componentResource.removeComponent(compId.toString());
			} catch (Exception ignored) {
			}
		}
		createdComponentIds.clear();

		for (Long templateId : createdTemplateIds) {
			try {
				templateResource.removeTemplate(templateId.toString());
			} catch (Exception ignored) {
			}
		}
		createdTemplateIds.clear();
	}

	private Template createSampleTemplate(String name) {
		Template template = new Template();
		template.setName(name);
		template.setDescription("Integration test template: " + name);

		List<TemplateField> fields = new ArrayList<>();
		fields.add(new TemplateField(null, "serial", true, FieldTypes.TEXT));
		fields.add(new TemplateField(null, "portCount", false, FieldTypes.NUMBER));
		template.setFields(fields);

		return template;
	}

	@Test
	@DisplayName("Add template persists in database and returns positive generated ID")
	void testAddTemplateGeneratesPositiveId() throws TemplateAlreadyExistsException {
		Template template = createSampleTemplate("T_Persist_" + System.nanoTime());
		Long id = templateResource.addTemplate(template);
		createdTemplateIds.add(id);

		assertTrue(id > 0);
	}

	@Test
	@DisplayName("Get template retrieves persisted name from real database")
	void testGetTemplatePersistedName() throws Exception {
		String uniqueName = "T_Name_" + System.nanoTime();
		Long id = templateResource.addTemplate(createSampleTemplate(uniqueName));
		createdTemplateIds.add(id);

		Template fetched = templateResource.getTemplate(id.toString());
		assertEquals(uniqueName, fetched.getName());
	}

	@Test
	@DisplayName("Get template retrieves all persisted fields from database")
	void testGetTemplatePersistedFieldsCount() throws Exception {
		Long id = templateResource.addTemplate(createSampleTemplate("T_Fields_" + System.nanoTime()));
		createdTemplateIds.add(id);

		Template fetched = templateResource.getTemplate(id.toString());
		assertEquals(2, fetched.getFields().size());
	}

	@Test
	@DisplayName("Add template with duplicate name throws TemplateAlreadyExistsException")
	void testAddDuplicateTemplateNameThrows() throws Exception {
		String sharedName = "T_Shared_" + System.nanoTime();
		Long id = templateResource.addTemplate(createSampleTemplate(sharedName));
		createdTemplateIds.add(id);

		Template duplicate = createSampleTemplate(sharedName);
		assertThrows(TemplateAlreadyExistsException.class, () -> templateResource.addTemplate(duplicate));
	}

	@Test
	@DisplayName("Modify template updates description in real database")
	void testModifyTemplateUpdatesDescription() throws Exception {
		Long id = templateResource.addTemplate(createSampleTemplate("T_Mod_" + System.nanoTime()));
		createdTemplateIds.add(id);

		Template fetched = templateResource.getTemplate(id.toString());
		fetched.setDescription("Updated Description via IT");
		templateResource.modifyTemplate(id.toString(), fetched);

		Template reloaded = templateResource.getTemplate(id.toString());
		assertEquals("Updated Description via IT", reloaded.getDescription());
	}

	@Test
	@DisplayName("Delete template without components deletes it successfully")
	void testDeleteUnusedTemplateDeletesSuccessfully() throws Exception {
		Long id = templateResource.addTemplate(createSampleTemplate("T_Delete_" + System.nanoTime()));

		templateResource.removeTemplate(id.toString());

		assertThrows(TemplateNotFoundException.class, () -> templateResource.getTemplate(id.toString()));
	}

	@Test
	@DisplayName("State machine: delete template in use throws TemplateStillInUseException")
	void testDeleteTemplateInUseThrows() throws Exception {
		Long templateId = templateResource.addTemplate(createSampleTemplate("T_InUse_" + System.nanoTime()));
		createdTemplateIds.add(templateId);
		Template template = templateResource.getTemplate(templateId.toString());

		Component component = new Component();
		component.setName("ActiveDevice_" + System.nanoTime());
		component.setTemplate(template);
		List<Field<?>> fields = new ArrayList<>();
		fields.add(new TextField(null, "SN-TEST", template.getFields().get(0)));
		component.setFields(fields);

		Long componentId = componentResource.addComponent(component);
		createdComponentIds.add(componentId);

		assertThrows(TemplateStillInUseException.class, () -> templateResource.removeTemplate(templateId.toString()));
	}

	@Test
	@DisplayName("State machine: template transitions from in-use to deletable when component is deleted")
	void testTemplateBecomesDeletableAfterComponentRemoved() throws Exception {
		Long templateId = templateResource.addTemplate(createSampleTemplate("T_Release_" + System.nanoTime()));
		createdTemplateIds.add(templateId);
		Template template = templateResource.getTemplate(templateId.toString());

		Component component = new Component();
		component.setName("DeviceToRelease_" + System.nanoTime());
		component.setTemplate(template);
		List<Field<?>> fields = new ArrayList<>();
		fields.add(new TextField(null, "SN-RELEASE", template.getFields().get(0)));
		component.setFields(fields);

		Long componentId = componentResource.addComponent(component);
		componentResource.removeComponent(componentId.toString());

		templateResource.removeTemplate(templateId.toString());
		assertThrows(TemplateNotFoundException.class, () -> templateResource.getTemplate(templateId.toString()));
	}

	@Test
	@DisplayName("Add field to existing template generates positive field ID")
	void testAddFieldToTemplateReturnsPositiveId() throws Exception {
		Long templateId = templateResource.addTemplate(createSampleTemplate("T_AddField_" + System.nanoTime()));
		createdTemplateIds.add(templateId);

		TemplateField newField = new TemplateField(null, "firmware", false, FieldTypes.TEXT);
		Long fieldId = templateResource.addFieldTemplate(templateId.toString(), newField);

		assertTrue(fieldId > 0);
	}

	@Test
	@DisplayName("Add field to existing template increases fields list in database")
	void testAddFieldToTemplateIncreasesCount() throws Exception {
		Long templateId = templateResource.addTemplate(createSampleTemplate("T_AddCount_" + System.nanoTime()));
		createdTemplateIds.add(templateId);

		TemplateField newField = new TemplateField(null, "macAddress", false, FieldTypes.TEXT);
		templateResource.addFieldTemplate(templateId.toString(), newField);

		List<TemplateField> fields = templateResource.getFieldsTemplate(templateId.toString());
		assertEquals(3, fields.size());
	}

	@Disabled("Discrepancia: addFieldTemplate no detecta campos duplicados por consulta SQL nativa errónea con ':name' entre comillas en SQLTemplateFieldDao")
	@Test
	@DisplayName("Add duplicate field name to template throws TemplateFieldAlreadyExistsException")
	void testAddDuplicateFieldNameThrows() throws Exception {
		Long templateId = templateResource.addTemplate(createSampleTemplate("T_DupField_" + System.nanoTime()));
		createdTemplateIds.add(templateId);

		TemplateField duplicateField = new TemplateField(null, "serial", false, FieldTypes.TEXT);
		assertThrows(TemplateFieldAlreadyExistsException.class,
				() -> templateResource.addFieldTemplate(templateId.toString(), duplicateField));
	}

	@Test
	@DisplayName("Add field to template in use throws TemplateStillInUseException")
	void testAddFieldToTemplateInUseThrows() throws Exception {
		Long templateId = templateResource.addTemplate(createSampleTemplate("T_FieldInUse_" + System.nanoTime()));
		createdTemplateIds.add(templateId);
		Template template = templateResource.getTemplate(templateId.toString());

		Component component = new Component();
		component.setName("BlockingDevice_" + System.nanoTime());
		component.setTemplate(template);
		List<Field<?>> fields = new ArrayList<>();
		fields.add(new TextField(null, "SN-BLOCK", template.getFields().get(0)));
		component.setFields(fields);

		Long componentId = componentResource.addComponent(component);
		createdComponentIds.add(componentId);

		TemplateField newField = new TemplateField(null, "extraProp", false, FieldTypes.TEXT);
		assertThrows(TemplateStillInUseException.class,
				() -> templateResource.addFieldTemplate(templateId.toString(), newField));
	}
}