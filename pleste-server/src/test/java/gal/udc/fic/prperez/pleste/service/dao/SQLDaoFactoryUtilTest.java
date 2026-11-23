package gal.udc.fic.prperez.pleste.service.dao;

import gal.udc.fic.prperez.pleste.service.dao.component.SQLComponentDao;
import gal.udc.fic.prperez.pleste.service.dao.component.SQLFieldDao;
import gal.udc.fic.prperez.pleste.service.dao.template.SQLTemplateDao;
import gal.udc.fic.prperez.pleste.service.dao.template.SQLTemplateFieldDao;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLTokenDao;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
@DisplayName("SQLDaoFactoryUtil Unit Tests")
public class SQLDaoFactoryUtilTest {

	@Mock
	private SQLTemplateDao templateDao;

	@Mock
	private SQLComponentDao componentDao;

	@Mock
	private SQLTemplateFieldDao templateFieldDao;

	@Mock
	private SQLFieldDao fieldDao;

	@Mock
	private SQLUserDao userDao;

	@Mock
	private SQLTokenDao tokenDao;

	private SQLDaoFactoryUtil factory;

	@BeforeEach
	void setUp() {
		factory = new SQLDaoFactoryUtil(templateDao, componentDao, templateFieldDao, fieldDao, userDao, tokenDao);
	}

	@Test
	@DisplayName("getSqlTemplateDao returns the template DAO it was built with")
	void testGetSqlTemplateDao() {
		assertSame(templateDao, factory.getSqlTemplateDao());
	}

	@Test
	@DisplayName("getSqlComponentDao returns the component DAO it was built with")
	void testGetSqlComponentDao() {
		assertSame(componentDao, factory.getSqlComponentDao());
	}

	@Test
	@DisplayName("getSqlTemplateFieldDao returns the template field DAO it was built with")
	void testGetSqlTemplateFieldDao() {
		assertSame(templateFieldDao, factory.getSqlTemplateFieldDao());
	}

	@Test
	@DisplayName("getSqlFieldDao returns the field DAO it was built with")
	void testGetSqlFieldDao() {
		assertSame(fieldDao, factory.getSqlFieldDao());
	}

	@Test
	@DisplayName("getSqlUserDao returns the user DAO it was built with")
	void testGetSqlUserDao() {
		assertSame(userDao, factory.getSqlUserDao());
	}

	@Test
	@DisplayName("getSqlTokenDao returns the token DAO it was built with")
	void testGetSqlTokenDao() {
		assertSame(tokenDao, factory.getSqlTokenDao());
	}
}
