package gal.udc.fic.prperez.pleste.service;

import gal.udc.fic.prperez.pleste.service.dao.SQLDaoFactoryUtil;
import gal.udc.fic.prperez.pleste.service.dao.component.Component;
import gal.udc.fic.prperez.pleste.service.dao.component.SQLComponentDao;
import gal.udc.fic.prperez.pleste.service.dao.template.SQLTemplateDao;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLUserDao;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.ParseSearchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchResource Unit Tests with Mocks")
public class SearchResourceTest {

	@Mock
	private SQLTemplateDao sqlTemplateDao;

	@Mock
	private SQLComponentDao sqlComponentDao;

	@Mock
	private SQLUserDao sqlUserDao;

	private SearchResource searchResource;

	@BeforeEach
	void setUp() {
		SQLDaoFactoryUtil daoFactoryUtil = new SQLDaoFactoryUtil(
				sqlTemplateDao,
				sqlComponentDao,
				null,
				null,
				sqlUserDao,
				null
		);
		searchResource = new SearchResource(daoFactoryUtil);
	}

	private static List<Template> templates(int count) {
		return LongStream.rangeClosed(1, count)
				.mapToObj(i -> new Template(i, "T" + i, "desc" + i, List.of()))
				.toList();
	}

	private static List<Component> components(int count) {
		return LongStream.rangeClosed(1, count)
				.mapToObj(i -> new Component(i, "C" + i, "desc" + i, null, List.of()))
				.toList();
	}

	private static List<User> users(int count) {
		return LongStream.rangeClosed(1, count)
				.mapToObj(i -> new User(i, "user" + i, "u" + i + "@udc.es", "h", Roles.NORMAL_USER))
				.toList();
	}

	@Test
	@DisplayName("searchTemplates filters matching templates")
	void testSearchTemplatesFiltersMatches() {
		when(sqlTemplateDao.findAll()).thenReturn(List.of(
				new Template(1L, "SensorTemp", "d", List.of()),
				new Template(2L, "Motor", "d", List.of()),
				new Template(3L, "SensorHum", "d", List.of())));
		List<Template> res = searchResource.searchTemplates("template.name ~ \"Sensor.*\"", null, null);
		assertEquals(List.of("SensorTemp", "SensorHum"), res.stream().map(Template::getName).toList());
	}

	@Test
	@DisplayName("searchTemplates returns empty list when DAO has no templates")
	void testSearchTemplatesEmptyDao() {
		when(sqlTemplateDao.findAll()).thenReturn(List.of());
		assertTrue(searchResource.searchTemplates("template.name ~ \".*\"", null, null).isEmpty());
	}

	@Test
	@DisplayName("searchTemplates queries DAO once on cache miss")
	void testSearchTemplatesQueriesDaoOnMiss() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(2));
		searchResource.searchTemplates("template.name ~ \"T.*\"", null, null);
		verify(sqlTemplateDao, times(1)).findAll();
	}

	@Test
	@DisplayName("searchTemplates serves repeated query from cache without calling DAO again")
	void testSearchTemplatesUsesCacheOnHit() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(2));
		String query = "template.name ~ \"T.*\"";
		searchResource.searchTemplates(query, null, null);
		searchResource.searchTemplates(query, null, null);
		verify(sqlTemplateDao, times(1)).findAll();
	}

	@Test
	@DisplayName("searchTemplates returns cached result on hit")
	void testSearchTemplatesCacheHitReturnsSameContent() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(2));
		String query = "template.name ~ \"T.*\"";
		searchResource.searchTemplates(query, null, null);
		assertEquals(List.of(1L, 2L), searchResource.searchTemplates(query, null, null).stream().map(Template::getId).toList());
	}

	@Test
	@DisplayName("clearTemplateCache with query invalidates only that query")
	void testClearTemplateCacheByQuery() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(2));
		String q1 = "template.name ~ \"T1\"";
		String q2 = "template.name ~ \"T2\"";
		searchResource.searchTemplates(q1, null, null);
		searchResource.searchTemplates(q2, null, null);
		searchResource.clearTemplateCache(q1);
		searchResource.searchTemplates(q1, null, null);
		searchResource.searchTemplates(q2, null, null);
		verify(sqlTemplateDao, times(3)).findAll();
	}

	@Test
	@DisplayName("clearTemplateCache with null invalidates entire template cache")
	void testClearTemplateCacheAll() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(2));
		String q1 = "template.name ~ \"T1\"";
		String q2 = "template.name ~ \"T2\"";
		searchResource.searchTemplates(q1, null, null);
		searchResource.searchTemplates(q2, null, null);
		searchResource.clearTemplateCache(null);
		searchResource.searchTemplates(q1, null, null);
		searchResource.searchTemplates(q2, null, null);
		verify(sqlTemplateDao, times(4)).findAll();
	}

	@Test
	@DisplayName("searchTemplates applies skip and count window")
	void testSearchTemplatesPaginationWindow() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(5));
		List<Template> res = searchResource.searchTemplates("template.name ~ \"T.*\"", 1, 2);
		assertEquals(List.of("T2", "T3"), res.stream().map(Template::getName).toList());
	}

	@Test
	@DisplayName("searchTemplates clamps count when skip + count exceeds total size")
	void testSearchTemplatesPaginationCountClamped() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(5));
		List<Template> res = searchResource.searchTemplates("template.name ~ \"T.*\"", 3, 100);
		assertEquals(List.of("T4", "T5"), res.stream().map(Template::getName).toList());
	}

	@Test
	@DisplayName("searchTemplates returns exact full list when skip is 0 and count equals size")
	void testSearchTemplatesPaginationExactSize() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(4));
		List<Template> res = searchResource.searchTemplates("template.name ~ \"T.*\"", 0, 4);
		assertEquals(List.of("T1", "T2", "T3", "T4"), res.stream().map(Template::getName).toList());
	}

	@Test
	@DisplayName("searchTemplates returns empty list when count is 0")
	void testSearchTemplatesPaginationZeroCount() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(4));
		assertTrue(searchResource.searchTemplates("template.name ~ \"T.*\"", 1, 0).isEmpty());
	}

	@Test
	@DisplayName("searchTemplates returns empty list when skip equals size")
	void testSearchTemplatesPaginationSkipAtEnd() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(4));
		assertTrue(searchResource.searchTemplates("template.name ~ \"T.*\"", 4, null).isEmpty());
	}

	@Test
	@DisplayName("searchTemplates returns empty list when skip exceeds size")
	void testSearchTemplatesPaginationSkipExceeded() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(4));
		assertTrue(searchResource.searchTemplates("template.name ~ \"T.*\"", 10, null).isEmpty());
	}

	@RepeatedTest(5)
	@DisplayName("searchTemplates returns expected template for random valid skip and count 1")
	void testSearchTemplatesRandomSkip() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(10));
		int skip = new Random().nextInt(10);
		List<Template> res = searchResource.searchTemplates("template.name ~ \"T.*\"", skip, 1);
		assertEquals(List.of("T" + (skip + 1)), res.stream().map(Template::getName).toList());
	}

	@Test
	@DisplayName("searchComponents filters matching components")
	void testSearchComponentsFiltersMatches() {
		when(sqlComponentDao.findAll()).thenReturn(List.of(
				new Component(1L, "PumpA", "Water", null, List.of()),
				new Component(2L, "ValveB", "Gas", null, List.of())));
		List<Component> res = searchResource.searchComponents("component.name ~ \"Pump.*\"", null, null);
		assertEquals(List.of("PumpA"), res.stream().map(Component::getName).toList());
	}

	@Test
	@DisplayName("searchComponents serves repeated query from cache")
	void testSearchComponentsUsesCacheOnHit() {
		when(sqlComponentDao.findAll()).thenReturn(components(2));
		String query = "component.name ~ \"C.*\"";
		searchResource.searchComponents(query, null, null);
		searchResource.searchComponents(query, null, null);
		verify(sqlComponentDao, times(1)).findAll();
	}

	@Test
	@DisplayName("clearComponentCache with query invalidates only that query")
	void testClearComponentCacheByQuery() {
		when(sqlComponentDao.findAll()).thenReturn(components(2));
		String q1 = "component.name ~ \"C1\"";
		String q2 = "component.name ~ \"C2\"";
		searchResource.searchComponents(q1, null, null);
		searchResource.searchComponents(q2, null, null);
		searchResource.clearComponentCache(q1);
		searchResource.searchComponents(q1, null, null);
		searchResource.searchComponents(q2, null, null);
		verify(sqlComponentDao, times(3)).findAll();
	}

	@Test
	@DisplayName("clearComponentCache with null invalidates entire component cache")
	void testClearComponentCacheAll() {
		when(sqlComponentDao.findAll()).thenReturn(components(2));
		String q1 = "component.name ~ \"C1\"";
		searchResource.searchComponents(q1, null, null);
		searchResource.clearComponentCache(null);
		searchResource.searchComponents(q1, null, null);
		verify(sqlComponentDao, times(2)).findAll();
	}

	@Test
	@DisplayName("searchComponents applies skip and count window")
	void testSearchComponentsPaginationWindow() {
		when(sqlComponentDao.findAll()).thenReturn(components(5));
		List<Component> res = searchResource.searchComponents("component.name ~ \"C.*\"", 2, 2);
		assertEquals(List.of("C3", "C4"), res.stream().map(Component::getName).toList());
	}

	@Test
	@DisplayName("searchComponents clamps count when skip + count exceeds total size")
	void testSearchComponentsPaginationCountClamped() {
		when(sqlComponentDao.findAll()).thenReturn(components(4));
		List<Component> res = searchResource.searchComponents("component.name ~ \"C.*\"", 2, 50);
		assertEquals(List.of("C3", "C4"), res.stream().map(Component::getName).toList());
	}

	@Test
	@DisplayName("searchComponents returns empty list when skip exceeds size")
	void testSearchComponentsPaginationSkipExceeded() {
		when(sqlComponentDao.findAll()).thenReturn(components(4));
		assertTrue(searchResource.searchComponents("component.name ~ \"C.*\"", 10, null).isEmpty());
	}

	@Test
	@DisplayName("searchUsers filters matching users")
	void testSearchUsersFiltersMatches() {
		when(sqlUserDao.findAll()).thenReturn(List.of(
				new User(1L, "root", "root@udc.es", "h", Roles.ADMINISTRATOR),
				new User(2L, "guest", "guest@udc.es", "h", Roles.NORMAL_USER)));
		List<User> res = searchResource.searchUsers("user.name ~ \"root\"", null, null);
		assertEquals(List.of("root"), res.stream().map(User::getUsername).toList());
	}

	@Test
	@DisplayName("searchUsers serves repeated query from cache")
	void testSearchUsersUsesCacheOnHit() {
		when(sqlUserDao.findAll()).thenReturn(users(2));
		String query = "user.name ~ \"user.*\"";
		searchResource.searchUsers(query, null, null);
		searchResource.searchUsers(query, null, null);
		verify(sqlUserDao, times(1)).findAll();
	}

	@Test
	@DisplayName("clearUserCache with query invalidates only that query")
	void testClearUserCacheByQuery() {
		when(sqlUserDao.findAll()).thenReturn(users(2));
		String q1 = "user.name ~ \"user1\"";
		String q2 = "user.name ~ \"user2\"";
		searchResource.searchUsers(q1, null, null);
		searchResource.searchUsers(q2, null, null);
		searchResource.clearUserCache(q1);
		searchResource.searchUsers(q1, null, null);
		searchResource.searchUsers(q2, null, null);
		verify(sqlUserDao, times(3)).findAll();
	}

	@Test
	@DisplayName("clearUserCache with null invalidates entire user cache")
	void testClearUserCacheAll() {
		when(sqlUserDao.findAll()).thenReturn(users(2));
		String q1 = "user.name ~ \"user1\"";
		searchResource.searchUsers(q1, null, null);
		searchResource.clearUserCache(null);
		searchResource.searchUsers(q1, null, null);
		verify(sqlUserDao, times(2)).findAll();
	}

	@Test
	@DisplayName("searchUsers applies skip and count window on cache hit")
	void testSearchUsersPaginationOnCacheHit() {
		when(sqlUserDao.findAll()).thenReturn(users(5));
		String query = "user.name ~ \"user.*\"";
		searchResource.searchUsers(query, null, null);
		List<User> paged = searchResource.searchUsers(query, 1, 2);
		assertEquals(List.of("user2", "user3"), paged.stream().map(User::getUsername).toList());
	}

	@Test
	@DisplayName("searchUsers clamps count on cache hit when skip + count exceeds total size")
	void testSearchUsersPaginationCountClampedOnCacheHit() {
		when(sqlUserDao.findAll()).thenReturn(users(5));
		String query = "user.name ~ \"user.*\"";
		searchResource.searchUsers(query, null, null);
		List<User> paged = searchResource.searchUsers(query, 3, 50);
		assertEquals(List.of("user4", "user5"), paged.stream().map(User::getUsername).toList());
	}

	@Test
	@DisplayName("searchUsers returns empty list on cache hit when skip exceeds size")
	void testSearchUsersPaginationSkipExceededOnCacheHit() {
		when(sqlUserDao.findAll()).thenReturn(users(5));
		String query = "user.name ~ \"user.*\"";
		searchResource.searchUsers(query, null, null);
		assertTrue(searchResource.searchUsers(query, 20, null).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"invalid query", "template.name ~ \"unclosed"})
	@DisplayName("searchTemplates throws ParseSearchException on invalid query syntax")
	void testSearchTemplatesInvalidSyntax(String query) {
		assertThrows(ParseSearchException.class, () -> searchResource.searchTemplates(query, null, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"invalid query", "component.name ~ \"unclosed"})
	@DisplayName("searchComponents throws ParseSearchException on invalid query syntax")
	void testSearchComponentsInvalidSyntax(String query) {
		assertThrows(ParseSearchException.class, () -> searchResource.searchComponents(query, null, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"invalid query", "user.name ~ \"unclosed"})
	@DisplayName("searchUsers throws ParseSearchException on invalid query syntax")
	void testSearchUsersInvalidSyntax(String query) {
		assertThrows(ParseSearchException.class, () -> searchResource.searchUsers(query, null, null));
	}

	@Disabled("Bug conocido: consultas vacías o solo con espacios no lanzan ParseSearchException y devuelven lista vacía")
	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	@DisplayName("searchTemplates throws ParseSearchException on empty or blank query")
	void testSearchTemplatesEmptyQuery(String query) {
		assertThrows(ParseSearchException.class, () -> searchResource.searchTemplates(query, null, null));
	}

	@Disabled("Bug conocido: consultas vacías o solo con espacios no lanzan ParseSearchException y devuelven lista vacía")
	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	@DisplayName("searchComponents throws ParseSearchException on empty or blank query")
	void testSearchComponentsEmptyQuery(String query) {
		assertThrows(ParseSearchException.class, () -> searchResource.searchComponents(query, null, null));
	}

	@Disabled("Bug conocido: consultas vacías o solo con espacios no lanzan ParseSearchException y devuelven lista vacía")
	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	@DisplayName("searchUsers throws ParseSearchException on empty or blank query")
	void testSearchUsersEmptyQuery(String query) {
		assertThrows(ParseSearchException.class, () -> searchResource.searchUsers(query, null, null));
	}

	@Disabled("Bug conocido: searchUsers en cache miss devuelve result antes de aplicar skip y count")
	@Test
	@DisplayName("searchUsers applies skip and count pagination on cache miss")
	void testSearchUsersPaginationOnCacheMiss() {
		when(sqlUserDao.findAll()).thenReturn(users(5));
		List<User> res = searchResource.searchUsers("user.name ~ \"user.*\"", 1, 2);
		assertEquals(List.of("user2", "user3"), res.stream().map(User::getUsername).toList());
	}

	@Disabled("Bug conocido: searchTemplates con query null lanza NullPointerException en CharStreams.fromString(null) en lugar de ParseSearchException")
	@Test
	@DisplayName("searchTemplates with null query throws ParseSearchException")
	void testSearchTemplatesNullQuery() {
		assertThrows(ParseSearchException.class, () -> searchResource.searchTemplates(null, null, null));
	}

	@Disabled("Bug conocido: searchComponents con query null lanza NullPointerException en CharStreams.fromString(null) en lugar de ParseSearchException")
	@Test
	@DisplayName("searchComponents with null query throws ParseSearchException")
	void testSearchComponentsNullQuery() {
		assertThrows(ParseSearchException.class, () -> searchResource.searchComponents(null, null, null));
	}

	@Disabled("Bug conocido: searchUsers con query null lanza NullPointerException en CharStreams.fromString(null) en lugar de ParseSearchException")
	@Test
	@DisplayName("searchUsers with null query throws ParseSearchException")
	void testSearchUsersNullQuery() {
		assertThrows(ParseSearchException.class, () -> searchResource.searchUsers(null, null, null));
	}

	@Disabled("Bug conocido: searchTemplates con skip negativo es capturado por IndexOutOfBoundsException y devuelve lista vacía en lugar de error 400 o skip=0")
	@Test
	@DisplayName("searchTemplates with negative skip throws ParseSearchException or rejects invalid pagination")
	void testSearchTemplatesNegativeSkip() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(5));
		assertEquals(List.of("T1", "T2"), searchResource.searchTemplates("template.name ~ \"T.*\"", -1, 2).stream().map(Template::getName).toList());
	}

	@Disabled("Bug conocido: searchTemplates con count muy grande y skip > 0 produce desbordamiento entero (count + from < 0) y devuelve lista vacía")
	@Test
	@DisplayName("searchTemplates does not overflow when skip > 0 and count is Integer.MAX_VALUE")
	void testSearchTemplatesPaginationIntegerOverflow() {
		when(sqlTemplateDao.findAll()).thenReturn(templates(5));
		List<Template> res = searchResource.searchTemplates("template.name ~ \"T.*\"", 2, Integer.MAX_VALUE);
		assertEquals(List.of("T3", "T4", "T5"), res.stream().map(Template::getName).toList());
	}

	@Disabled("Bug conocido: searchComponents con count muy grande y skip > 0 produce desbordamiento entero (count + from < 0) y devuelve lista vacía")
	@Test
	@DisplayName("searchComponents does not overflow when skip > 0 and count is Integer.MAX_VALUE")
	void testSearchComponentsPaginationIntegerOverflow() {
		when(sqlComponentDao.findAll()).thenReturn(components(5));
		List<Component> res = searchResource.searchComponents("component.name ~ \"C.*\"", 2, Integer.MAX_VALUE);
		assertEquals(List.of("C3", "C4", "C5"), res.stream().map(Component::getName).toList());
	}

	@Disabled("Bug conocido: searchUsers con count muy grande y skip > 0 en cache hit produce desbordamiento entero y devuelve lista vacía")
	@Test
	@DisplayName("searchUsers does not overflow when skip > 0 and count is Integer.MAX_VALUE")
	void testSearchUsersPaginationIntegerOverflow() {
		when(sqlUserDao.findAll()).thenReturn(users(5));
		String query = "user.name ~ \"user.*\"";
		searchResource.searchUsers(query, null, null);
		List<User> res = searchResource.searchUsers(query, 2, Integer.MAX_VALUE);
		assertEquals(List.of("user3", "user4", "user5"), res.stream().map(User::getUsername).toList());
	}

	@Disabled("Bug conocido: con tabla vacía en DAO, SearchResource no valida compatibilidad propiedad-entidad y cachea lista vacía sin lanzar ParseSearchException")
	@Test
	@DisplayName("searchTemplates rejects incompatible property even when DAO returns empty list")
	void testSearchTemplatesIncompatiblePropertyWithEmptyDao() {
		when(sqlTemplateDao.findAll()).thenReturn(List.of());
		assertThrows(ParseSearchException.class, () -> searchResource.searchTemplates("user.name ~ \"root\"", null, null));
	}
}
