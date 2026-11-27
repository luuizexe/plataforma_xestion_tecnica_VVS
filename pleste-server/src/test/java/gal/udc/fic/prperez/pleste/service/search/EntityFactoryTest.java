package gal.udc.fic.prperez.pleste.service.search;

import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.Field;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.template.TemplateField;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityFactory Unit Tests")
public class EntityFactoryTest {

	@Mock
	private CriteriaQuery<?> criteriaQuery;

	@Mock
	private Root<Template> templateRoot;

	@Mock
	private Root<TemplateField> templateFieldRoot;

	@Mock
	private Root<Component> componentRoot;

	@Mock
	private Root<Field> fieldRoot;

	@Mock
	private Root<User> userRoot;

	@Test
	@DisplayName("Default constructor getTemplateRoot throws NullPointerException")
	void testDefaultConstructorTemplateRootThrows() {
		assertThrows(NullPointerException.class, () -> new EntityFactory().getTemplateRoot());
	}

	@Test
	@DisplayName("Default constructor getTemplateFieldRoot throws NullPointerException")
	void testDefaultConstructorTemplateFieldRootThrows() {
		assertThrows(NullPointerException.class, () -> new EntityFactory().getTemplateFieldRoot());
	}

	@Test
	@DisplayName("Default constructor getComponentRoot throws NullPointerException")
	void testDefaultConstructorComponentRootThrows() {
		assertThrows(NullPointerException.class, () -> new EntityFactory().getComponentRoot());
	}

	@Test
	@DisplayName("Default constructor getFieldRoot throws NullPointerException")
	void testDefaultConstructorFieldRootThrows() {
		assertThrows(NullPointerException.class, () -> new EntityFactory().getFieldRoot());
	}

	@Test
	@DisplayName("Default constructor getUserRoot throws NullPointerException")
	void testDefaultConstructorUserRootThrows() {
		assertThrows(NullPointerException.class, () -> new EntityFactory().getUserRoot());
	}

	@Test
	@DisplayName("Null CriteriaQuery constructor throws NullPointerException on root access")
	void testNullQueryConstructorThrows() {
		assertThrows(NullPointerException.class, () -> new EntityFactory(null).getUserRoot());
	}

	@Test
	@DisplayName("getTemplateRoot returns root created by CriteriaQuery")
	void testGetTemplateRootReturnsRoot() {
		doReturn(templateRoot).when(criteriaQuery).from(Template.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		assertSame(templateRoot, factory.getTemplateRoot());
	}

	@Test
	@DisplayName("getTemplateFieldRoot returns root created by CriteriaQuery")
	void testGetTemplateFieldRootReturnsRoot() {
		doReturn(templateFieldRoot).when(criteriaQuery).from(TemplateField.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		assertSame(templateFieldRoot, factory.getTemplateFieldRoot());
	}

	@Test
	@DisplayName("getComponentRoot returns root created by CriteriaQuery")
	void testGetComponentRootReturnsRoot() {
		doReturn(componentRoot).when(criteriaQuery).from(Component.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		assertSame(componentRoot, factory.getComponentRoot());
	}

	@Test
	@DisplayName("getFieldRoot returns root created by CriteriaQuery")
	void testGetFieldRootReturnsRoot() {
		doReturn(fieldRoot).when(criteriaQuery).from(Field.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		assertSame(fieldRoot, factory.getFieldRoot());
	}

	@Test
	@DisplayName("getUserRoot returns root created by CriteriaQuery")
	void testGetUserRootReturnsRoot() {
		doReturn(userRoot).when(criteriaQuery).from(User.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		assertSame(userRoot, factory.getUserRoot());
	}

	@Test
	@DisplayName("getTemplateRoot caches root across repeated calls")
	void testGetTemplateRootCachesInstance() {
		doReturn(templateRoot).when(criteriaQuery).from(Template.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		factory.getTemplateRoot();
		factory.getTemplateRoot();
		verify(criteriaQuery, times(1)).from(Template.class);
	}

	@Test
	@DisplayName("getTemplateFieldRoot caches root across repeated calls")
	void testGetTemplateFieldRootCachesInstance() {
		doReturn(templateFieldRoot).when(criteriaQuery).from(TemplateField.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		factory.getTemplateFieldRoot();
		factory.getTemplateFieldRoot();
		verify(criteriaQuery, times(1)).from(TemplateField.class);
	}

	@Test
	@DisplayName("getComponentRoot caches root across repeated calls")
	void testGetComponentRootCachesInstance() {
		doReturn(componentRoot).when(criteriaQuery).from(Component.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		factory.getComponentRoot();
		factory.getComponentRoot();
		verify(criteriaQuery, times(1)).from(Component.class);
	}

	@Test
	@DisplayName("getFieldRoot caches root across repeated calls")
	void testGetFieldRootCachesInstance() {
		doReturn(fieldRoot).when(criteriaQuery).from(Field.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		factory.getFieldRoot();
		factory.getFieldRoot();
		verify(criteriaQuery, times(1)).from(Field.class);
	}

	@Test
	@DisplayName("getUserRoot caches root across repeated calls")
	void testGetUserRootCachesInstance() {
		doReturn(userRoot).when(criteriaQuery).from(User.class);
		EntityFactory factory = new EntityFactory(criteriaQuery);
		factory.getUserRoot();
		factory.getUserRoot();
		verify(criteriaQuery, times(1)).from(User.class);
	}
}
