package it;

import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.pageobjects.pages.project.BrowseProjectPage;
import com.atlassian.jira.tests.TestBase;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertTrue;

@Restore("xml/testproject.xml")
public class TestBrowseProject extends TestBase {

	@Test
	public void seeIfLinkIsVisible() {
		jira().gotoLoginPage().loginAsSysAdmin(BrowseProjectPage.class, "TST");
		assertTrue(jira().getTester().getDriver().elementIsVisible(By.cssSelector(".bulk-create-issues")));
	}

}
