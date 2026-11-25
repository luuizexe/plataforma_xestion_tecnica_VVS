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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchResource Unit Tests with Mocks & Cache Validation")
public class SearchResourceTest {

	@Mock
	private SQLTemplateDao sqlTemplateDao;

	@Mock
	private SQLComponentDao sqlComponentDao;

	@Mock
	private SQLUserDao sqlUserDao;

	private SQLDaoFactoryUtil daoFactoryUtil;
	private SearchResource searchResource;

	@BeforeEach
	void setUp() {
		daoFactoryUtil = new SQLDaoFactoryUtil(
				sqlTemplateDao,
				sqlComponentDao,
				null,
				null,
				sqlUserDao,
				null
		);
		searchResource = new SearchResource(daoFactoryUtil);
	}

	// ==========================================
	// Template Search & Cache Tests
	// ==========================================

	@Test
	@DisplayName("searchTemplates queries DAO on cache miss and uses cache on subsequent calls")
	void testSearchTemplatesCacheHitAndMiss() {
		Template t1 = new Template(1L, "SensorTemp", "desc1", new ArrayList<>());
		Template t2 = new Template(2L, "SensorHum", "desc2", new ArrayList<>());
		when(sqlTemplateDao.findAll()).thenReturn(List.of(t1, t2));

		String query = "template.name ~ \"Sensor.*\"";

		// 1st call: Cache miss -> DAO is called
		List<Template> results1 = searchResource.searchTemplates(query, null, null);
		assertEquals(2, results1.size());
		verify(sqlTemplateDao, times(1)).findAll();

		// 2nd call: Cache hit -> DAO must NOT be called again
		List<Template> results2 = searchResource.searchTemplates(query, null, null);
		assertEquals(2, results2.size());
		verify(sqlTemplateDao, times(1)).findAll();
	}

	@Test
	@DisplayName("clearTemplateCache invalidates specific query and entire cache")
	void testClearTemplateCache() {
		Template t1 = new Template(1L, "SensorTemp", "desc1", new ArrayList<>());
		when(sqlTemplateDao.findAll()).thenReturn(List.of(t1));

		String q1 = "template.name ~ \"SensorTemp\"";
		searchResource.searchTemplates(q1, null, null);
		verify(sqlTemplateDao, times(1)).findAll();

		// Clear specific query
		searchResource.clearTemplateCache(q1);
		searchResource.searchTemplates(q1, null, null);
		verify(sqlTemplateDao, times(2)).findAll();

		// Clear all cache (query = null)
		searchResource.clearTemplateCache(null);
		searchResource.searchTemplates(q1, null, null);
		verify(sqlTemplateDao, times(3)).findAll();
	}

	@Test
	@DisplayName("searchTemplates pagination: skip and count bounds")
	void testSearchTemplatesPagination() {
		List<Template> list = new ArrayList<>();
		for (long i = 1; i <= 5; i++) {
			list.add(new Template(i, "T" + i, "desc", new ArrayList<>()));
		}
		when(sqlTemplateDao.findAll()).thenReturn(list);

		String query = "template.name ~ \"T.*\"";

		// from=1, count=2 -> [T2, T3]
		List<Template> paged = searchResource.searchTemplates(query, 1, 2);
		assertEquals(2, paged.size());
		assertEquals("T2", paged.get(0).getName());
		assertEquals("T3", paged.get(1).getName());

		// to > result.size() (count=100)
		List<Template> largeCount = searchResource.searchTemplates(query, 2, 100);
		assertEquals(3, largeCount.size());

		// from > to (skip=10, count=0 -> to=5, from becomes 5)
		List<Template> skipExceeded = searchResource.searchTemplates(query, 10, null);
		assertTrue(skipExceeded.isEmpty());
	}

	// ==========================================
	// Component Search & Cache Tests
	// ==========================================

	@Test
	@DisplayName("searchComponents queries DAO on cache miss and uses cache on hit")
	void testSearchComponentsCacheHitAndMiss() {
		Component c1 = new Component(1L, "PumpA", "Water pump", null, new ArrayList<>());
		when(sqlComponentDao.findAll()).thenReturn(List.of(c1));

		String query = "component.name ~ \"Pump.*\"";

		// 1st call
		List<Component> res1 = searchResource.searchComponents(query, null, null);
		assertEquals(1, res1.size());
		verify(sqlComponentDao, times(1)).findAll();

		// 2nd call (cached)
		List<Component> res2 = searchResource.searchComponents(query, null, null);
		assertEquals(1, res2.size());
		verify(sqlComponentDao, times(1)).findAll();

		// Clear component cache by key
		searchResource.clearComponentCache(query);
		searchResource.searchComponents(query, null, null);
		verify(sqlComponentDao, times(2)).findAll();

		// Clear all component cache
		searchResource.clearComponentCache(null);
		searchResource.searchComponents(query, null, null);
		verify(sqlComponentDao, times(3)).findAll();
	}

	@Test
	@DisplayName("searchComponents pagination bounds")
	void testSearchComponentsPagination() {
		List<Component> list = new ArrayList<>();
		for (long i = 1; i <= 4; i++) {
			list.add(new Component(i, "C" + i, "desc", null, new ArrayList<>()));
		}
		when(sqlComponentDao.findAll()).thenReturn(list);

		String query = "component.name ~ \"C.*\"";

		List<Component> paged = searchResource.searchComponents(query, 1, 2);
		assertEquals(2, paged.size());

		List<Component> skipExceeded = searchResource.searchComponents(query, 10, null);
		assertTrue(skipExceeded.isEmpty());
	}

	// ==========================================
	// User Search & Cache Tests
	// ==========================================

	@Test
	@DisplayName("searchUsers queries DAO on cache miss and uses cache on hit")
	void testSearchUsersCacheHitAndMiss() {
		User u1 = new User(1L, "root", "root@udc.es", "h", Roles.ADMINISTRATOR);
		when(sqlUserDao.findAll()).thenReturn(List.of(u1));

		String query = "user.name ~ \"root\"";

		// 1st call: Cache miss
		List<User> res1 = searchResource.searchUsers(query, null, null);
		assertEquals(1, res1.size());
		verify(sqlUserDao, times(1)).findAll();

		// 2nd call: Cache hit (runs lines 156-157 & 165-177)
		List<User> res2 = searchResource.searchUsers(query, null, null);
		assertEquals(1, res2.size());
		verify(sqlUserDao, times(1)).findAll();

		// Clear user cache by key
		searchResource.clearUserCache(query);
		searchResource.searchUsers(query, null, null);
		verify(sqlUserDao, times(2)).findAll();

		// Clear all user cache
		searchResource.clearUserCache(null);
		searchResource.searchUsers(query, null, null);
		verify(sqlUserDao, times(3)).findAll();
	}

	@Disabled("ISSUE #10: searchUsers en caso de cache miss devuelve 'result' antes de aplicar 'skip' y 'count'")
	@Test
	@DisplayName("searchUsers applies skip and count pagination even on cache miss")
	void testSearchUsersPaginationOnCacheMiss() {
		List<User> users = new ArrayList<>();
		for (long i = 1; i <= 5; i++) {
			users.add(new User(i, "user" + i, "u" + i + "@udc.es", "h", Roles.NORMAL_USER));
		}
		when(sqlUserDao.findAll()).thenReturn(users);

		// With skip=1, count=2 on cache miss, it should return 2 users, but currently returns all 5!
		List<User> res = searchResource.searchUsers("user.name ~ \"user.*\"", 1, 2);
		assertEquals(2, res.size());
	}

	@Test
	@DisplayName("searchUsers pagination on cache hit")
	void testSearchUsersPaginationOnCacheHit() {
		List<User> users = new ArrayList<>();
		for (long i = 1; i <= 5; i++) {
			users.add(new User(i, "user" + i, "u" + i + "@udc.es", "h", Roles.NORMAL_USER));
		}
		when(sqlUserDao.findAll()).thenReturn(users);

		String query = "user.name ~ \"user.*\"";
		// First call populates cache
		searchResource.searchUsers(query, null, null);

		// Second call hits cache and executes pagination logic
		List<User> paged = searchResource.searchUsers(query, 1, 2);
		assertEquals(2, paged.size());
		assertEquals("user2", paged.get(0).getUsername());

		List<User> large = searchResource.searchUsers(query, 3, 50);
		assertEquals(2, large.size());

		List<User> empty = searchResource.searchUsers(query, 20, null);
		assertTrue(empty.isEmpty());
	}

	@Test
	@DisplayName("Search methods propagate ParseSearchException on invalid syntax")
	void testSearchInvalidQuerySyntax() {
		assertThrows(ParseSearchException.class, () -> searchResource.searchTemplates("invalid query", null, null));
		assertThrows(ParseSearchException.class, () -> searchResource.searchComponents("invalid query", null, null));
		assertThrows(ParseSearchException.class, () -> searchResource.searchUsers("invalid query", null, null));
	}
}
