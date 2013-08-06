package com.pawelniewiadomski.devs.jira.components;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.context.LazyIssueContext;
import com.atlassian.jira.issue.context.ProjectContext;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.util.lang.Pair;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.pawelniewiadomski.devs.jira.csv.CsvData;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValuesHelper {

	private final FieldsHelper fieldsHelper;
	private final ConstantsManager constantsManager;
	private final CustomFieldManager customFieldManager;

	private final Function<IssueConstant,Pair<String,String>> issueConstantTransformer = new Function<IssueConstant, Pair<String, String>>() {
		@Override
		public Pair<String, String> apply(@Nullable IssueConstant input) {
			return Pair.of(input.getId(), StringUtils.defaultString(input.getNameTranslation(), input.getName()));
		}
	};

	public ValuesHelper(ConstantsManager constantsManager, FieldsHelper fieldsHelper, CustomFieldManager customFieldManager) {
		this.constantsManager = constantsManager;
		this.fieldsHelper = fieldsHelper;
		this.customFieldManager = customFieldManager;
	}

	@Nonnull
	public Multimap<String, String> getCsvValuesToMap(@Nonnull CsvData csvData, @Nonnull final Map<String, String> fieldMappings, @Nonnull Project project) {
		final Multimap<String, String> values = LinkedHashMultimap.create();
		final LinkedHashMap<String, LinkedHashMap<String, String>> jiraValues = getAllPossibleValues(project);
		for (LinkedListMultimap<String, String> row : csvData.getData()) {
			for (final String csvColumn : fieldMappings.keySet()) {
				if (!jiraValues.containsKey(fieldMappings.get(csvColumn))) {
					continue;
				}

				final Iterable<String> valuesToMap = Iterables.filter(row.get(csvColumn), new Predicate<String>() {
					@Override
					public boolean apply(@Nullable String csvValue) {
						for(Map.Entry<String, String> jiraValue : jiraValues.get(fieldMappings.get(csvColumn)).entrySet()) {
							if (jiraValue.getValue().equalsIgnoreCase(csvValue)) {
								return false;
							}
						}
						return true;
					}
				});

				if (!Iterables.isEmpty(valuesToMap)) {
					values.putAll(csvColumn, valuesToMap);
				}
			}
		}
		return values;
	}

	@Nonnull
	public LinkedHashMap<String, LinkedHashMap<String, String>> getAllPossibleValues(@Nonnull Project project) {
		final Collection<IssueType> issueTypes = project.getIssueTypes();
		final LinkedHashMap<String, LinkedHashMap<String, String>> values = Maps.newLinkedHashMap();
		for (IssueType it : issueTypes) {
			values.putAll(getAllPossibleValues(project, it));
		}
		return values;
	}

	@Nonnull
	public LinkedHashMap<String, LinkedHashMap<String, String>> getAllPossibleValues(@Nonnull Project project, @Nonnull IssueType it) {
		final LinkedHashMap<String, LinkedHashMap<String, String>> values = Maps.newLinkedHashMap();

		for(Map.Entry<String, JiraField> field : fieldsHelper.getAllFields(project, it).entrySet()) {
			final String fieldId = field.getKey();
			final LinkedHashMap<String, String> map = Maps.newLinkedHashMap();

			if (IssueFieldConstants.PRIORITY.equals(fieldId)) {
				for(Pair<String, String> p : Iterables.transform(constantsManager.getPriorityObjects(), issueConstantTransformer)) {
					map.put(p.first(), p.second());
				}
			} else if (IssueFieldConstants.ISSUE_TYPE.equals(fieldId)) {
				for(Pair<String, String> p : Iterables.transform(project.getIssueTypes(), issueConstantTransformer)) {
					map.put(p.first(), p.second());
				}
				values.put(fieldId, map);
			} else if (IssueFieldConstants.FIX_FOR_VERSIONS.equals(fieldId) || IssueFieldConstants.AFFECTED_VERSIONS.equals(fieldId)) {
				for(Version v : project.getVersions()) {
					map.put(Long.toString(v.getId()), v.getName());
				}
			} else if (IssueFieldConstants.COMPONENTS.equals(fieldId)) {
				for(ProjectComponent pc : project.getProjectComponents()) {
					map.put(Long.toString(pc.getId()), pc.getName());
				}
			} else if (fieldId.startsWith(FieldManager.CUSTOM_FIELD_PREFIX)) {
				final CustomField customField = customFieldManager.getCustomFieldObject(fieldId);
                final FieldConfig config = customField.getRelevantConfig(new LazyIssueContext(project.getId(), it.getId()));
				final Options options = customField.getOptions(null, config, null);
				if (options != null) {
					for(Object o : options.getRootOptions()) {
						final Option option = (Option) o; // in 4.4 it's list of objects, so we need to force cast
						map.put(Long.toString(option.getOptionId()), option.getValue());
					}
				}
			}

			if (!map.isEmpty()) {
				values.put(fieldId, map);
			}
		}

		return values;
	}

}
