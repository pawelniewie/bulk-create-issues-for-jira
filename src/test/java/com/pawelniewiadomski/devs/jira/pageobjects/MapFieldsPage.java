package com.pawelniewiadomski.devs.jira.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.*;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.openqa.selenium.By;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.0.1
 */
public class MapFieldsPage extends AbstractJiraPage {
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

	@Nonnull
	public MapValuesPage mapValues() {
		create.click();
		return pageBinder.bind(MapValuesPage.class);
	}

	public MapFieldsPage setMapping(@Nonnull String csvColumn, @Nonnull String jiraField) {
		finder.find(By.name(csvColumn), SelectElement.class).select(Options.text(jiraField));
		return this;
	}

    public List<Option> getPossibleMappings(@Nonnull String csvColumn) {
        return finder.find(By.name(csvColumn), SelectElement.class).getAllOptions();
    }

    public static List<String> getText(List<Option> options) {
        return ImmutableList.copyOf(Iterables.transform(options, new Function<Option, String>() {
            @Override
            public String apply(@Nullable Option input) {
                return input.text();
            }
        }));
    }
}
