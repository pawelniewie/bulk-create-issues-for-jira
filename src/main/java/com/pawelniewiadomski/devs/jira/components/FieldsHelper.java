package com.pawelniewiadomski.devs.jira.components;

import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.MultipleSettableCustomFieldType;
import com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType;
import com.atlassian.jira.issue.customfields.impl.LabelsCFType;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.project.Project;
import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class FieldsHelper {

	private final IssueFactory issueFactory;
	private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;

	public FieldsHelper(IssueFactory issueFactory, IssueTypeScreenSchemeManager issueTypeScreenSchemeManager) {
		this.issueFactory = issueFactory;
		this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
	}

	@Nonnull
	public LinkedHashMap<String, JiraField> getAllFields(@Nonnull Project project) {
		final LinkedHashMap<String, JiraField> values = Maps.newLinkedHashMap();
		final Collection<IssueType> issueTypes = project.getIssueTypes();
		for(IssueType it : issueTypes) {
			values.putAll(getAllFields(project, it));
		}
		return values;
	}

	@Nonnull
	public LinkedHashMap<String, JiraField> getAllFields(@Nonnull Project project, @Nonnull IssueType it) {
		final LinkedHashMap<String, JiraField> values = Maps.newLinkedHashMap();
		final MutableIssue issue = issueFactory.getIssue();
		issue.setProjectId(project.getId());
		issue.setIssueTypeId(it.getId());

		final List<FieldScreenTab> tabs = issueTypeScreenSchemeManager.getFieldScreenScheme(issue)
				.getFieldScreen(IssueOperations.CREATE_ISSUE_OPERATION).getTabs();
		for (FieldScreenTab tab : tabs) {
			final List<FieldScreenLayoutItem> items = tab.getFieldScreenLayoutItems();
			for(FieldScreenLayoutItem item : items) {
                final OrderableField orderableField = item.getOrderableField();
                final String id = orderableField.getId();
				if (!IssueFieldConstants.ATTACHMENT.equals(id) && !IssueFieldConstants.REPORTER.equals(id)) {
					values.put(id, new JiraField(id, orderableField.getName(), isMulti(orderableField), isCustom(orderableField)));
				}
			}
		}
		return values;
	}

    private boolean isMulti(OrderableField orderableField) {
        final String id = orderableField.getId();
        if (IssueFieldConstants.COMPONENTS.equals(id) || IssueFieldConstants.AFFECTED_VERSIONS
                .equals(id) || IssueFieldConstants.FIX_FOR_VERSIONS.equals(id)
                || IssueFieldConstants.LABELS.equals(id)) {
            return true;
        }

        if (orderableField instanceof CustomField) {
            final CustomFieldType customFieldType = ((CustomField) orderableField).getCustomFieldType();
            if (customFieldType instanceof MultiSelectCFType || customFieldType instanceof LabelsCFType) {
                return true;
            }
        }

        return false;
    }

    private boolean isCustom(OrderableField orderableField) {
        return (orderableField instanceof CustomField);
    }

}
