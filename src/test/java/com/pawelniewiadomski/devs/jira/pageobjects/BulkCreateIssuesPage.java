package com.pawelniewiadomski.devs.jira.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.annotation.Nonnull;
import java.io.File;

public class BulkCreateIssuesPage extends AbstractJiraPage {
	@ElementBy(id="next")
	private PageElement next;

	@FindBy(name = "file")
	private WebElement file;

	@Override
	public TimedCondition isAt() {
		return next.timed().isVisible();
	}

	@Override
	public String getUrl() {
		return null;
	}

	@Nonnull
	public static String getCsvResource(@Nonnull String resource) {
		return new File(".").getAbsolutePath() + "/src/test/resources/csv/" + resource;
	}

	@Nonnull
	public BulkCreateIssuesPage setCsvFile(@Nonnull String file) {
		this.file.sendKeys(file);
		return this;
	}

	@Nonnull
	public MapFieldsPage next() {
		next.click();
		return pageBinder.bind(MapFieldsPage.class);
	}
}
