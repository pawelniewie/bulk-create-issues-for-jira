package com.pawelniewiadomski.devs.jira.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.0.1
 */
public class TrackProgressPage extends AbstractJiraPage {

	@ElementBy(id = "progress-wrapper")
	private PageElement progress;

	@Override
	public TimedCondition isAt() {
		return progress.timed().isVisible();
	}

	@Override
	public String getUrl() {
		return null;
	}

	@Nonnull
	public TrackProgressPage waitUntilFinished() {
		driver.waitUntil(new Function<WebDriver, Boolean>() {
			@Override
			public Boolean apply(@Nullable WebDriver input) {
				return "true".equals(input.findElement(By.id("finished")).getAttribute("value"));
			}
		});
		return this;
	}

	@Nonnull
	public Iterable<String> getMessages() {
		WebElement messages = Iterables.getFirst(driver.findElements(By.id("messages")), null);
		return Iterables.transform(messages.findElements(By.tagName("div")), new TextFromWebElement());
	}

	private static class TextFromWebElement implements Function<WebElement, String> {
		@Override
		public String apply(@Nullable WebElement input) {
			return input != null ? input.getText() : null;
		}
	}
}
