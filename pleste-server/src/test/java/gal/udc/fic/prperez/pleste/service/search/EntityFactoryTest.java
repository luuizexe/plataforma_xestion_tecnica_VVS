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
	@DisplayName("EntityFactory default constructor creates roots as null")
	void testDefaultConstructor() {
		EntityFactory factory = new EntityFactory();
		assertThrows(NullPointerException.class, factory::getTemplateRoot);
	}

	@Test
	@DisplayName("EntityFactory lazily initializes and caches entity roots")
	void testLazyRoots() {
		doReturn(templateRoot).when(criteriaQuery).from(Template.class);
		doReturn(templateFieldRoot).when(criteriaQuery).from(TemplateField.class);
		doReturn(componentRoot).when(criteriaQuery).from(Component.class);
		doReturn(fieldRoot).when(criteriaQuery).from(Field.class);
		doReturn(userRoot).when(criteriaQuery).from(User.class);

		EntityFactory factory = new EntityFactory(criteriaQuery);

		// First call calls criteriaQuery.from()
		assertSame(templateRoot, factory.getTemplateRoot());
		assertSame(templateFieldRoot, factory.getTemplateFieldRoot());
		assertSame(componentRoot, factory.getComponentRoot());
		assertSame(fieldRoot, factory.getFieldRoot());
		assertSame(userRoot, factory.getUserRoot());

		// Second call returns cached instance without re-querying
		assertSame(templateRoot, factory.getTemplateRoot());
		assertSame(templateFieldRoot, factory.getTemplateFieldRoot());
		assertSame(componentRoot, factory.getComponentRoot());
		assertSame(fieldRoot, factory.getFieldRoot());
		assertSame(userRoot, factory.getUserRoot());

		verify(criteriaQuery, times(1)).from(Template.class);
		verify(criteriaQuery, times(1)).from(TemplateField.class);
		verify(criteriaQuery, times(1)).from(Component.class);
		verify(criteriaQuery, times(1)).from(Field.class);
		verify(criteriaQuery, times(1)).from(User.class);
	}
}
