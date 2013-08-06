package com.pawelniewiadomski.devs.jira.engine;

import com.atlassian.jira.project.Project;
import com.google.common.collect.Maps;
import com.pawelniewiadomski.devs.jira.csv.CsvData;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@Component
public class EngineManager {
	private final Map<String, Engine> cache = Maps.newConcurrentMap();

	@Nonnull
	public Engine createEngine(@Nonnull Project project, @Nonnull CsvData file, @Nonnull Map<String, String> fieldsMapping) {
		Engine engine = new Engine(project, file, fieldsMapping);
		cache.put(engine.getId(), engine);
		return engine;
	}

	@Nullable
	public Engine getEngine(String id) {
		return cache.get(id);
	}

	@Nullable
	public Engine removeEngine(String id) {
		return cache.remove(id);
	}
}
