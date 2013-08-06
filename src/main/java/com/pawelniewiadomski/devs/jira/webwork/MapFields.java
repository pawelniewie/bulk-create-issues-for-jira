package com.pawelniewiadomski.devs.jira.webwork;

import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pawelniewiadomski.devs.jira.components.FieldsHelper;
import com.pawelniewiadomski.devs.jira.components.JiraField;
import com.pawelniewiadomski.devs.jira.components.JiraFieldFunctions;
import com.pawelniewiadomski.devs.jira.csv.CsvData;
import org.apache.commons.lang.StringUtils;
import webwork.action.ActionContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class MapFields extends Common {
	private final FieldsHelper fieldsHelper;

	public static final String PARENT_ID = "__parent_id__";
	public static final String ISSUE_ID = "__issue_id__";
    public static final String PARENT_KEY = "__parent_key__";

	final CsvData data = getDataFromSession();
	final Long projectId = getProjectIdFromSession();
	final Map<String, String> fieldMappings = Maps.newLinkedHashMap();
	final JiraAuthenticationContext authenticationContext;
	final Supplier<LinkedHashMap<String, JiraField>> allFields = Suppliers.memoize(new Supplier<LinkedHashMap<String, JiraField>>() {
		@Override
		public LinkedHashMap<String, JiraField> get() {
			return fieldsHelper.getAllFields(getSelectedProjectObject());
		}
	});

	final Supplier<Map<String, JiraField>> singleValueFields = Suppliers.memoize(new Supplier<Map<String, JiraField>>() {
		@Override
		public Map<String, JiraField> get() {
			return Maps.difference(allFields.get(), multiValueFields.get()).entriesOnlyOnLeft();
		}
	});

	final Supplier<ImmutableMap<String, JiraField>> multiValueFields = Suppliers.memoize(
			new Supplier<ImmutableMap<String, JiraField>>() {
				@Override
				public ImmutableMap<String, JiraField> get() {
					return ImmutableMap.copyOf(Maps.filterValues(allFields.get(), new Predicate<JiraField>() {
                        @Override
                        public boolean apply(@Nullable JiraField input) {
                            return input.isMulti();
                        }
                    }));
				}
			});

	public MapFields(JiraAuthenticationContext authenticationContext, FieldsHelper fieldsHelper) {
		this.authenticationContext = authenticationContext;
		this.fieldsHelper = fieldsHelper;
	}

	@Override
	public String doDefault() throws Exception {
		if (projectId == null || data == null || data.getData().size() == 0) {
			return getRedirect("/");
		}

		setSelectedProjectId(projectId);

		if (!canAccessSelectedProject()) {
			return "denied";
		}

		for(String column : getColumnNames()) {
			fieldMappings.put(column, getDefaultMapping(column));
		}

		return super.doDefault();
	}

	@Nullable
	private String getDefaultMapping(@Nonnull String column) {
		final String lowerCaseColumn = column.toLowerCase(authenticationContext.getLocale());
		for(Map<String, String> jiraFields : getAvailableJiraFields(column).values()) {
			for (Map.Entry<String, String> jira : jiraFields.entrySet()) {
				final String lowerJira = jira.getValue().toLowerCase(authenticationContext.getLocale());
				if (lowerCaseColumn.equals(lowerJira)) {
					return jira.getKey();
				}
				if (lowerJira.startsWith(lowerCaseColumn) || lowerJira.endsWith(lowerCaseColumn)
						|| lowerCaseColumn.startsWith(lowerJira) || lowerCaseColumn.endsWith(lowerJira)) {
					return jira.getKey();
				}
			}
		}
		return null;
	}

	@Override
	protected void doValidation() {
		super.doValidation();
		final Map params = ActionContext.getSingleValueParameters();
		fieldMappings.clear();

		final Set<String> singleUsed = Sets.newHashSet();
		int summaryTimes = 0;
		for(String column : getColumnNames()) {
			Object mappingObj = params.get(column);
			if (mappingObj != null) {
				final String mapping = mappingObj.toString();
				if (StringUtils.isBlank(mapping)) {
					continue;
				}

				fieldMappings.put(column, mapping);

				if (singleValueFields.get().containsKey(mapping)) {
					if(singleUsed.contains(mapping)) {
						addErrorMessage(getText("jbcp.you.can.map.once", singleValueFields.get().get(mapping).getName()));
					} else {
						singleUsed.add(mapping);
					}
				}

				if (IssueFieldConstants.SUMMARY.equals(mappingObj)) {
					++summaryTimes;
				}
			}
		}

		if (summaryTimes == 0) {
			addErrorMessage(getText("jbcp.need.at.least.summary", singleValueFields.get().get(IssueFieldConstants.SUMMARY).getName()));
		}
	}

	@Nonnull
	public LinkedHashSet<String> getColumnNames() {
		return Sets.newLinkedHashSet(data.getHeaders().keySet());
	}

	@Nonnull
	public Map<String, Map<String, String>> getAvailableJiraFields(@Nonnull String columnName) {
		final Map<String, Map<String, String>> fields = Maps.newLinkedHashMap();
		if (data.getHeaders().get(columnName)) {
            fields.put(getText("jbcp.jira.fields"), Maps.transformValues(multiValueFields.get(), JiraFieldFunctions.GET_NAME));
		} else {
			fields.put(getText("jbcp.jira.fields"), Maps.transformValues(allFields.get(), JiraFieldFunctions.GET_NAME));

			if (isSubtasksEnabled()) {
				fields.put(getText("jbcp.subtasks"), ImmutableMap.of(
                        ISSUE_ID, getText("jbcp.issue.id"),
                        PARENT_ID, getText("jbcp.parent.id"),
                        PARENT_KEY, getText("jbcp.parent.key")));
			}
		}

		return fields;
	}

	@Nullable
	public String getColumnMapping(@Nonnull String column) {
		return fieldMappings.get(column);
	}

	@Override
	protected String doExecute() throws Exception {
		setFieldMappingsInSession(fieldMappings);

		return getRedirect("MapValues!default.jspa" + (isInlineDialogMode() ? "?decorator=dialog&inline=true" : ""));
	}
}
