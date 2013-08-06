package com.pawelniewiadomski.devs.jira.webwork;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.BrowserUtils;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.pawelniewiadomski.devs.jira.csv.CsvData;
import org.apache.velocity.tools.generic.SortTool;
import webwork.action.ActionContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class Common extends JiraWebActionSupport {

	private final String PROJECT_SESSION_KEY = "jbcp.project.key";
	private final String DATA_SESSION_KEY = "jbcp.csv.data";
	private final String FIELD_MAPPINGS_KEY = "jbcp.field.mappings";

	public String getModifierKey() {
		return BrowserUtils.getModifierKey();
	}

	@Override
	public ResourceBundle getTexts(String aBundleName) {
		try {
			return super.getTexts(aBundleName);
		} catch (MissingResourceException e) {
			if (aBundleName.startsWith("com.pawelniewiadomski")) {
				return ResourceBundle.getBundle("com.pawelniewiadomski.devs.jira.messages", getLocale(), getClass().getClassLoader());
			}
			throw e;
		}
	}

	public boolean isJira5() {
		BuildUtilsInfo info = ComponentAccessor.getComponent(BuildUtilsInfo.class);
		return Integer.valueOf(info.getCurrentBuildNumber()) >= 700;
	}

	@Nullable
	public Long getProjectIdFromSession() {
		Object projectKey = ActionContext.getSession().get(PROJECT_SESSION_KEY);
		return projectKey != null ? Long.parseLong(projectKey.toString()) : null;
	}

	public void setProjectIdInSession(@Nonnull Long projectId) {
		ActionContext.getSession().put(PROJECT_SESSION_KEY, Long.toString(projectId));
	}

	@Nullable
	private String getStringFromSession(@Nonnull String key) {
		Object dataObj = ActionContext.getSession().get(key);
		return dataObj != null ? String.valueOf(dataObj) : null;
	}

	@Nullable
	public CsvData getDataFromSession() {
		final String data = getStringFromSession(DATA_SESSION_KEY);
		if (data == null) {
			return null;
		}
		final ObjectMapper mapper = createObjectMapper();
		try {
			return mapper.readValue(data, CsvData.class);
		} catch (IOException e) {
			return null;
		}
	}

	public void setDataInSession(@Nonnull CsvData data) throws IOException {
		final ObjectMapper mapper = createObjectMapper();
		ActionContext.getSession().put(DATA_SESSION_KEY, mapper.writeValueAsString(data));
	}

	@Nonnull
	private ObjectMapper createObjectMapper() {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new GuavaModule());
		return mapper;
	}

	public void setFieldMappingsInSession(@Nonnull Map<String, String> fieldMappings) throws IOException {
		ActionContext.getSession().put(FIELD_MAPPINGS_KEY, createObjectMapper().writeValueAsString(fieldMappings));
	}

	@Nullable
	public Map<String, String> getFieldMappingsFromSession() {
		final String fieldMappings = getStringFromSession(FIELD_MAPPINGS_KEY);
		if (fieldMappings == null) {
			return null;
		}
		try {
			return createObjectMapper().readValue(fieldMappings, new TypeReference<Map<String,String>>() {});
		} catch(IOException e) {
			return null;
		}
	}

	protected boolean canAccessSelectedProject() {
		final Project project = getSelectedProjectObject();

		if (!isHasPermission(Permissions.BULK_CHANGE) || project == null || !isHasProjectPermission(Permissions.CREATE_ISSUE, project.getGenericValue())) {
			return false;
		}

		return true;
	}

	@Nonnull
	public SortTool getSorter() {
		return new SortTool();
	}

	public boolean isSubtasksEnabled() {
		return getApplicationProperties().getOption(APKeys.JIRA_OPTION_ALLOWSUBTASKS);
	}
}
