package com.pawelniewiadomski.devs.jira.webwork;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.task.TaskContext;
import com.atlassian.jira.task.TaskDescriptor;
import com.atlassian.jira.task.TaskManager;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.pawelniewiadomski.devs.jira.components.FieldsHelper;
import com.pawelniewiadomski.devs.jira.components.JiraField;
import com.pawelniewiadomski.devs.jira.components.JiraFieldFunctions;
import com.pawelniewiadomski.devs.jira.components.ValuesHelper;
import com.pawelniewiadomski.devs.jira.csv.CsvData;
import com.pawelniewiadomski.devs.jira.engine.Engine;
import com.pawelniewiadomski.devs.jira.engine.EngineCallable;
import com.pawelniewiadomski.devs.jira.engine.EngineManager;
import org.apache.commons.lang.StringUtils;
import webwork.action.ActionContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapValues extends Common {

	final CsvData data = getDataFromSession();
	final Long projectId = getProjectIdFromSession();
	final Map<String, String> fieldMappings = getFieldMappingsFromSession();
	final JiraAuthenticationContext authenticationContext;
	private final EngineManager engineManager;
	private final TaskManager taskManager;
	private final ValuesHelper valuesHelper;
	private final FieldsHelper fieldsHelper;

	private final Supplier<Multimap<String, String>> fieldValues = Suppliers
			.memoize(new Supplier<Multimap<String, String>>() {
				@Override
				public Multimap<String, String> get() {
					return valuesHelper.getCsvValuesToMap(data, fieldMappings, getSelectedProjectObject());
				}
			});

	private final Supplier<LinkedHashMap<String, LinkedHashMap<String, String>>> jiraValues = Suppliers.memoize(
			new Supplier<LinkedHashMap<String, LinkedHashMap<String, String>>>() {
				@Override
				public LinkedHashMap<String, LinkedHashMap<String, String>> get() {
					return valuesHelper.getAllPossibleValues(getSelectedProjectObject());
				}
			});

	final Supplier<LinkedHashMap<String, JiraField>> allFields = Suppliers.memoize(new Supplier<LinkedHashMap<String, JiraField>>() {
		@Override
		public LinkedHashMap<String, JiraField> get() {
			return fieldsHelper.getAllFields(getSelectedProjectObject());
		}
	});

	public MapValues(JiraAuthenticationContext authenticationContext, EngineManager engineManager, TaskManager taskManager,
			ValuesHelper valuesHelper, FieldsHelper fieldsHelper) {
		this.authenticationContext = authenticationContext;
		this.engineManager = engineManager;
		this.taskManager = taskManager;
		this.valuesHelper = valuesHelper;
		this.fieldsHelper = fieldsHelper;
	}

	@Override
	public String doDefault() throws Exception {
		if (projectId == null || data == null || data.getData().size() == 0 || fieldMappings == null || fieldMappings.size() == 0) {
			return getRedirect("/");
		}

		setSelectedProjectId(projectId);

		if (!canAccessSelectedProject()) {
			return "denied";
		}

		if (fieldValues.get().isEmpty()) {
			return doExecute();
		}

		return super.doDefault();
	}

	@Nonnull
	public Map<String, String> getFieldMappings() {
		return fieldMappings;
	}

	@Nonnull
	public Multimap<String, String> getFieldValues() {
		return fieldValues.get();
	}

	@Nonnull
	public static String getValueMappingId(@Nonnull String csvColumn, @Nonnull String csvValue) {
		return csvColumn + "-" + csvValue;
	}

	@Override
	protected void doValidation() {
		super.doValidation();

		final Map params = ActionContext.getSingleValueParameters();
		final Multimap<String, String> valuesToMap = valuesHelper.getCsvValuesToMap(data, fieldMappings,
				getSelectedProjectObject());
		final Map<String, Map<String, String>> valuesMapping = Maps.newHashMapWithExpectedSize(valuesToMap.size());

		for(String csvColumn : valuesToMap.keySet()) {
			valuesMapping.put(csvColumn, Maps.<String, String>newHashMap());

			for(String csvValue : valuesToMap.get(csvColumn)) {
				Object mappingObj = params.get(getValueMappingId(csvColumn, csvValue));
				if (mappingObj != null) {
					valuesMapping.get(csvColumn).put(csvValue, mappingObj.toString());
				}
			}
		}

		if (!hasAnyErrors()) {
			final List<LinkedListMultimap<String, String>> newData = Lists.newArrayList();
			for(LinkedListMultimap<String, String> row : data.getData()) {
				LinkedListMultimap newRow = LinkedListMultimap.create(row.size());
				for(Map.Entry<String, String> colValue : row.entries()) {
					if (valuesMapping.containsKey(colValue.getKey())) {
						newRow.put(colValue.getKey(), StringUtils.defaultIfEmpty(valuesMapping.get(colValue.getKey()).get(colValue.getValue()), colValue.getValue()));
					} else {
						newRow.put(colValue.getKey(), colValue.getValue());
					}
				}
				newData.add(newRow);
			}

			data.getData().clear();
			data.getData().addAll(newData);
		}
	}

	@Override
	protected String doExecute() throws Exception {
		final Engine engine = engineManager.createEngine(getSelectedProjectObject(), data, fieldMappings);
		try {
			TaskDescriptor<Void> task = taskManager.submitTask(new EngineCallable(engine, getLoggedInUser()),
					"Bulk Issue Create Plugin Task ID " + engine.getId(), new TaskContext() {
				public String buildProgressURL(Long aLong) {
					return null;
				}
			});
		} catch (Exception e) {
			log.error(getText("jbcp.BulkCreateIssues.execute.task", e.getMessage()), e);
			addErrorMessage(getText("jbcp.BulkCreateIssues.execute.task", e.getMessage()));
			return INPUT;
		}

		return getRedirect("TrackProgress.jspa?eid=" + engine.getId() + (isInlineDialogMode() ? "&decorator=dialog&inline=true" : ""));
	}

	@Nullable
	public String getValueMapping(@Nonnull String csvField, @Nonnull String csvValue) {
		return null;
	}

	@Nonnull
	public LinkedHashMap<String, String> getJiraValuesForColumn(@Nonnull String csvColumn) {
		return jiraValues.get().get(fieldMappings.get(csvColumn));
	}

	@Nonnull
	public LinkedHashMap<String, LinkedHashMap<String, String>> getJiraValues() {
		return jiraValues.get();
	}

	@Nonnull
	public Collection<String> getCsvValues(@Nonnull String csvColumn) {
		return fieldValues.get().get(csvColumn);
	}

	@Nonnull
	public Map<String, String> getAllJiraFields() {
		return Maps.transformValues(allFields.get(), JiraFieldFunctions.GET_NAME);
	}
}
