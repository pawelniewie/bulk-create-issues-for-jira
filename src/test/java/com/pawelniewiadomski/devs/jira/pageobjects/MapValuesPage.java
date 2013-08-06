package com.pawelniewiadomski.devs.jira.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.GlobalElementFinder;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.google.inject.Inject;
import com.pawelniewiadomski.devs.jira.webwork.MapValues;
import org.openqa.selenium.By;

import javax.annotation.Nonnull;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.0.1
 */
public class MapValuesPage extends AbstractJiraPage {
	@ElementBy(id="create")
	private PageElement create;

	@Inject
	private GlobalElementFinder finder;

	@Override
	public TimedCondition isAt() {
		return create.timed().isVisible();
	}

	@Override
	public String getUrl() {
		return null;
	}

	@Nonnull
	public TrackProgressPage create() {
		create.click();
		return pageBinder.bind(TrackProgressPage.class);
	}

	public MapValuesPage setMapping(@Nonnull String csvColumn, @Nonnull String csvValue, @Nonnull String jiraValue) {
		finder.find(By.name(MapValues.getValueMappingId(csvColumn, csvValue)), SelectElement.class).select(Options.text(jiraValue));
		return this;
	}
}
