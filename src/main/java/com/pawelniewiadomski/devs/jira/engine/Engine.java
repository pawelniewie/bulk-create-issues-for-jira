package com.pawelniewiadomski.devs.jira.engine;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.context.LazyIssueContext;
import com.atlassian.jira.issue.context.ProjectContext;
import com.atlassian.jira.issue.context.manager.JiraContextTreeManager;
import com.atlassian.jira.issue.customfields.MultipleSettableCustomFieldType;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.pawelniewiadomski.devs.jira.csv.CsvData;
import com.pawelniewiadomski.devs.jira.webwork.MapFields;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class Engine {
	private final Logger log = Logger.getLogger(this.getClass());
	private final String id = UUID.randomUUID().toString();
	private final Project destination;
	private final CsvData data;
	private final Map<String, String> fieldsMapping;
    private final Map<String, String> reverseFieldsMapping;
	private final ProgressModel progress;
	private final IssueService issueService;
	private final JiraAuthenticationContext authenticationContext;
	private final ConstantsManager constantsManager;
	private final ProjectComponentManager projectComponentManager;
	private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final Map<String, String> issueIdsMapping;
    private final SubTaskManager subTaskManager;
    private final CustomFieldManager customFieldManager;

    public Engine(@Nonnull Project destination, @Nonnull CsvData data, @Nonnull Map<String, String> fieldsMapping) {
		this.destination = destination;
		this.data = data;
		this.fieldsMapping = fieldsMapping;
        this.reverseFieldsMapping = createReverseMapping(fieldsMapping);
		this.progress = new ProgressModel(data.getData().size());
		this.issueService = ComponentAccessor.getIssueService();
		this.authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
		this.constantsManager = ComponentAccessor.getConstantsManager();
		this.issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
		this.projectComponentManager = ComponentAccessor.getProjectComponentManager();
        this.subTaskManager = ComponentAccessor.getSubTaskManager();
        this.customFieldManager= ComponentAccessor.getCustomFieldManager();
        this.issueIdsMapping = Maps.newHashMap();
	}

    @Nonnull
    private Map<String, String> createReverseMapping(@Nonnull Map<String, String> fieldsMapping) {
        final Map<String, String> result = Maps.newHashMap();
        for(Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            result.put(mapping.getValue(), mapping.getKey());
        }
        return result;
    }

    public void execute() {
		try {
			this.progress.setRunning(true);
            this.issueIdsMapping.clear();
			for(LinkedListMultimap<String, String> row : data.getData()) {
				try {
                    final String parentKey = getParentKey(row), parentId = getParentId(row);
                    IssueService.IssueResult parentResult = null;
                    if (parentKey != null) {
                        parentResult = issueService.getIssue(authenticationContext.getLoggedInUser(), parentKey);
                    } else if (parentId != null) {
                        parentResult = issueService.getIssue(authenticationContext.getLoggedInUser(), issueIdsMapping.get(parentId));
                    }

                    ErrorCollection errors = null;
                    IssueService.CreateValidationResult validation = null;

                    if (parentResult != null) {
                        if (parentResult.isValid()) {
                            validation = issueService.validateSubTaskCreate(authenticationContext.getLoggedInUser(),
                                    parentResult.getIssue().getId(), getIssueInputParams(row, true));
                        } else {
                            errors = parentResult.getErrorCollection();
                        }
                    }

                    if (errors == null || !errors.hasAnyErrors()) {
                        if (validation == null) {
                            validation = issueService.validateCreate(authenticationContext.getLoggedInUser(), getIssueInputParams(row, false));
                        }

                        if (validation.isValid()) {
                            IssueService.IssueResult issue = issueService.create(authenticationContext.getLoggedInUser(),
                                    validation);
                            if (issue.isValid()) {
                                final String issueKey = issue.getIssue().getKey();
                                final String issueId = getIssueId(row);
                                if (issueId != null) {
                                    this.issueIdsMapping.put(issueId, issueKey);
                                }
                                if (parentResult != null && parentResult.isValid()) {
                                    subTaskManager.createSubTaskIssueLink(parentResult.getIssue(), issue.getIssue(), authenticationContext.getLoggedInUser());
                                }
                                progress.addIssueKey(issueKey);
                                continue;
                            } else {
                                errors = issue.getErrorCollection();
                            }
                        } else {
                            errors = validation.getErrorCollection();
                        }
                    }

					if (errors != null && errors.hasAnyErrors()) {
						final String msg = getI18n().getText("jbcp.Engine.unable.to.import", getSummary(row),
								Iterables.getFirst(Iterables.concat(validation.getErrorCollection().getErrorMessages(),
										validation.getErrorCollection().getErrors().values()), null));
						log.error(msg);
						progress.addMessage(msg);
					}
				} catch(Exception e) {
					final String msg = getI18n().getText("jbcp.Engine.unable.to.import", getSummary(row), e.getMessage());
					log.error(msg, e);
					progress.addMessage(msg);
				}
				progress.increaseRow();
			}
		} catch (Throwable e) {
			final String msg = getI18n().getText("jbcp.Engine.error", e.getMessage());
			log.error(msg, e);
			progress.addMessage(msg);
		} finally {
			this.progress.setRunning(false);
		}
	}

    @Nullable
    private String getIssueId(LinkedListMultimap<String, String> row) {
        return StringUtils.defaultIfEmpty(Iterables.get(row.get(reverseFieldsMapping.get(MapFields.ISSUE_ID)), 0, null), null);
    }

    @Nullable
    private String getParentId(LinkedListMultimap<String, String> row) {
        return StringUtils.defaultIfEmpty(Iterables.get(row.get(reverseFieldsMapping.get(MapFields.PARENT_ID)), 0, null), null);
    }

    @Nullable
    private String getParentKey(LinkedListMultimap<String, String> row) {
        return StringUtils.defaultIfEmpty(Iterables.get(row.get(reverseFieldsMapping.get(MapFields.PARENT_KEY)), 0, null), null);
    }

    @Nullable
	private String getSummary(LinkedListMultimap<String, String> row) {
		for(Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
			if (IssueFieldConstants.SUMMARY.equals(mapping.getValue())) {
				return Iterables.getFirst(row.get(mapping.getKey()), null);
			}
		}
		return null;
	}

	private IssueInputParameters getIssueInputParams(LinkedListMultimap<String, String> row, boolean subtask) {
		final IssueInputParameters params = new IssueInputParametersImpl(getActionParameters(row, subtask));

		params.setProjectId(destination.getId());
		params.setReporterId(authenticationContext.getLoggedInUser().getName());

		return params;
	}

	@Nonnull
	private Map<String, String[]> getActionParameters(@Nonnull LinkedListMultimap<String, String> row, boolean subtask) {
		final Map<String, String[]> result = Maps.newHashMap();
		final Set<String> affectedVersions = Sets.newHashSet();
		final Set<String> fixedVersions = Sets.newHashSet();
		final Set<String> componentsToSet = Sets.newHashSet();
        final Multimap<String, String> multiCustomfields = HashMultimap.create();

		// detect issue type
		for(Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
			final String csvColumn = mapping.getKey();
			final String jiraField = mapping.getValue();
			if (jiraField.equals(IssueFieldConstants.ISSUE_TYPE)) {
				final String it = Iterables.getFirst(row.get(csvColumn), null);
				if (StringUtils.isNotBlank(it)) {
					IssueType issueType = Iterables.getFirst(
							Iterables.filter(destination.getIssueTypes(), new Predicate<IssueType>() {
								@Override
								public boolean apply(@Nullable IssueType input) {
									return input != null && it.equalsIgnoreCase(input.getName());
								}
							}), null);
					if (issueType != null) {
						result.put(IssueFieldConstants.ISSUE_TYPE, new String[] { issueType.getId() });
					}
				}
			}
		}

		if (!result.containsKey(IssueFieldConstants.ISSUE_TYPE)) {
			if (!subtask) {
				// set default issue type if it exists
				@SuppressWarnings("deprecation")
				IssueType defaultIt = issueTypeSchemeManager.getDefaultValue(destination.getGenericValue());
				if (defaultIt != null) {
					result.put(IssueFieldConstants.ISSUE_TYPE, new String[] { defaultIt.getId() });
				}
			} else {
				final Collection<IssueType> subtasks = issueTypeSchemeManager.getSubTaskIssueTypesForProject(destination);
				if (subtasks != null && !subtasks.isEmpty()) {
					result.put(IssueFieldConstants.ISSUE_TYPE, new String[] { Iterables.getFirst(subtasks, null).getId() });
				}
			}
		}

		// process other fields
		for(Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
			final String csvColumn = mapping.getKey();
			final String jiraField = mapping.getValue();
			if (jiraField.equals(IssueFieldConstants.PRIORITY)) {
				final String priority = Iterables.getFirst(row.get(csvColumn), null);
				if (StringUtils.isNotBlank(priority)) {
					try {
						result.put(IssueFieldConstants.PRIORITY, new String[]{
								constantsManager.getIssueConstantByName(ConstantsManager.PRIORITY_CONSTANT_TYPE,
										priority).getId() });
					} catch(NullPointerException e) {
						throw new RuntimeException(getI18n().getText("jbcp.priority.does.not.exist", priority));
					}
				}
			} else if (jiraField.equals(IssueFieldConstants.AFFECTED_VERSIONS)) {
				final Set<String> versions = Sets.newHashSet(row.get(csvColumn));

				Iterables.addAll(affectedVersions, getVersions(versions));
			} else if (jiraField.equals(IssueFieldConstants.COMPONENTS)) {
				final Set<String> components = Sets.newHashSet(row.get(csvColumn));

				Iterables.addAll(componentsToSet, Iterables.transform(Iterables.filter(
						projectComponentManager.findAllForProject(destination.getId()),
						new Predicate<ProjectComponent>() {
							@Override
							public boolean apply(@Nullable ProjectComponent input) {
								return input != null && components.contains(input.getName());
							}
						}),
						new Function<ProjectComponent, String>() {
							@Override
							public String apply(@Nullable ProjectComponent input) {
								return input != null ? Long.toString(input.getId()) : null;
							}
						}
					));
			} else if (jiraField.equals(IssueFieldConstants.FIX_FOR_VERSIONS)) {
				final Set<String> versions = Sets.newHashSet(row.get(csvColumn));

				Iterables.addAll(fixedVersions, getVersions(versions));
            } else if (jiraField.startsWith(FieldManager.CUSTOM_FIELD_PREFIX)
                    && customFieldManager.getCustomFieldObject(jiraField).getCustomFieldType() instanceof MultipleSettableCustomFieldType) {
                final CustomField customField = customFieldManager.getCustomFieldObject(jiraField);
                final Set<String> options = Sets.newHashSet(row.get(csvColumn));

                multiCustomfields.putAll(jiraField, getOptionIds(result.get(IssueFieldConstants.ISSUE_TYPE)[0], customField, options));
            } else if (!jiraField.equals(MapFields.PARENT_KEY) && !jiraField.equals(MapFields.PARENT_ID) && !jiraField.equals(MapFields.ISSUE_ID)
					&& !jiraField.equals(IssueFieldConstants.ISSUE_TYPE)) {
				final List<String> values = ImmutableList.copyOf(Iterables.filter(row.get(csvColumn), new Predicate<String>() {
					@Override
					public boolean apply(@Nullable String input) {
						return StringUtils.isNotBlank(input);
					}
				}));

				if (!values.isEmpty()) {
					result.put(jiraField, values.toArray(new String[values.size()]));
				}
			}
		}

		if (affectedVersions.size() > 0) {
			result.put(IssueFieldConstants.AFFECTED_VERSIONS, Iterables.toArray(affectedVersions, String.class));
		}

		if (componentsToSet.size() > 0) {
			result.put(IssueFieldConstants.COMPONENTS, Iterables.toArray(componentsToSet, String.class));
		}

		if (fixedVersions.size() > 0) {
			result.put(IssueFieldConstants.FIX_FOR_VERSIONS, Iterables.toArray(fixedVersions, String.class));
		}

		if (!result.containsKey(IssueFieldConstants.ASSIGNEE)) {
			final Long assigneeType = destination.getAssigneeType();
			if (assigneeType == null || assigneeType == AssigneeTypes.PROJECT_LEAD) {
				result.put(IssueFieldConstants.ASSIGNEE, new String[] { destination.getLeadUserName() });
			}
		}

        for(String customFieldId : multiCustomfields.keySet()) {
            result.put(customFieldId, Iterables.toArray(multiCustomfields.get(customFieldId), String.class));
        }

		return result;
	}

    private Iterable<? extends String> getOptionIds(@Nonnull String issueTypeId, @Nonnull CustomField customField, @Nonnull Set<String> optionValues) {
        final FieldConfig config = customField.getRelevantConfig(new LazyIssueContext(destination.getId(), issueTypeId));
        final Options cfOptions = customField.getOptions(null, config, null);

        return Iterables.transform(optionValues, new Function<String, String>() {
			@Override
			public String apply(@Nullable String input) {
				return Long.toString(cfOptions.getOptionForValue(input, null).getOptionId());
			}
		});
    }

    private Iterable<String> getVersions(final Set<String> versions) {
		return Iterables.transform(Iterables.filter(
				destination.getVersions(),
				new Predicate<Version>() {
					@Override
					public boolean apply(@Nullable Version input) {
						return input != null && versions.contains(input.getName());
					}
				}),
				new Function<Version, String>() {
					@Override
					public String apply(@Nullable Version input) {
						return input != null ? Long.toString(input.getId()) : null;
					}
				}
		);
	}

	public String getId() {
		return id;
	}

	protected I18nHelper getI18n() {
		return ComponentAccessor.getI18nHelperFactory().getInstance(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
	}

	public ProgressModel getProgress() {
		return progress;
	}
}
