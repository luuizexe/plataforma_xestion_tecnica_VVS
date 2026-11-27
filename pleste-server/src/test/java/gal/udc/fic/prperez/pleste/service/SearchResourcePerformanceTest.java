package gal.udc.fic.prperez.pleste.service;

import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;
import etm.core.monitor.FlatMonitor;
import etm.core.renderer.SimpleTextRenderer;
import etm.core.timer.Java15NanoTimer;
import gal.udc.fic.prperez.pleste.service.dao.SQLDaoFactoryUtil;
import gal.udc.fic.prperez.pleste.service.dao.template.SQLTemplateDao;
import gal.udc.fic.prperez.pleste.service.dao.template.Template;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("JETM unit performance measurements for SearchResource")
class SearchResourcePerformanceTest {

	private static final int TEMPLATE_COUNT = 2_000;
	private static final int SAMPLE_COUNT = 10;
	private static final String QUERY = "template.name~\".*\"";
	private static final EtmMonitor MONITOR = new FlatMonitor(new Java15NanoTimer());

	private SearchResource resource;

	@BeforeAll
	static void startMonitor() {
		MONITOR.start();
	}

	@BeforeEach
	void initializeResourceWithRepresentativeCatalog() {
		List<Template> templates = IntStream.range(0, TEMPLATE_COUNT)
				.mapToObj(index -> {
					Template template = new Template();
					template.setName("template-" + index);
					return template;
				})
				.toList();
		SQLTemplateDao templateDao = (SQLTemplateDao) Proxy.newProxyInstance(
				SQLTemplateDao.class.getClassLoader(),
				new Class<?>[] {SQLTemplateDao.class},
				(proxy, method, arguments) -> {
					if (method.getName().equals("findAll") && method.getParameterCount() == 0) {
						return templates;
					}
					throw new UnsupportedOperationException("Unexpected DAO call: " + method.getName());
				});
		SQLDaoFactoryUtil daoFactory = new SQLDaoFactoryUtil(templateDao, null, null, null, null, null);
		resource = new SearchResource(daoFactory);

		resource.searchTemplates(QUERY, 0, TEMPLATE_COUNT);
		resource.clearTemplateCache(null);
	}

	@Test
	@DisplayName("Cold-cache template search measures filtering over a representative catalog")
	void coldCacheTemplateSearchMeasuresExecutionTime() {
		assertEquals(TEMPLATE_COUNT, measureTemplateSearch("cold cache", true).size());
	}

	@Test
	@DisplayName("Warm-cache template search measures cache-hit execution")
	void warmCacheTemplateSearchMeasuresExecutionTime() {
		assertEquals(TEMPLATE_COUNT, measureTemplateSearch("warm cache", false).size());
	}

	private List<Template> measureTemplateSearch(String cacheState, boolean clearCachePerSample) {
		if (!clearCachePerSample) {
			resource.searchTemplates(QUERY, 0, TEMPLATE_COUNT);
		}

		List<Template> result = List.of();
		for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
			if (clearCachePerSample) {
				resource.clearTemplateCache(QUERY);
			}
			EtmPoint point = MONITOR.createPoint("SearchResource.searchTemplates (" + cacheState + ")");
			try {
				result = resource.searchTemplates(QUERY, 0, TEMPLATE_COUNT);
			} finally {
				point.collect();
			}
		}
		return result;
	}

	@AfterAll
	static void writeMeasurements() throws IOException {
		MONITOR.stop();
		MONITOR.aggregate();
		Path report = Path.of("target", "jetm-reports", "unit-measurements.txt");
		Files.createDirectories(report.getParent());
		try (Writer writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
			MONITOR.render(new SimpleTextRenderer(writer));
		}
	}
}
