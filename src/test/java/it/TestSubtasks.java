package it;

import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.pageobjects.pages.project.BrowseProjectPage;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.tests.TestBase;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.google.common.collect.Iterables;
import com.pawelniewiadomski.devs.jira.pageobjects.BulkCreateIssuesPage;
import com.pawelniewiadomski.devs.jira.pageobjects.LicensePage;
import com.pawelniewiadomski.devs.jira.pageobjects.MapFieldsPage;
import com.pawelniewiadomski.devs.jira.pageobjects.TrackProgressPage;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static it.TestUtils.createRestClient;
import static org.junit.Assert.assertTrue;

@Restore("xml/testproject.xml")
public class TestSubtasks extends TestBase {
    @Inject
    private PageElementFinder elementFinder;
    private JiraRestClient rest;
    private BulkCreateIssuesPage createPage;

    @Before
    public void setUpTest() {
        rest = createRestClient(jira().environmentData());

        jira().gotoLoginPage().loginAsSysAdmin(LicensePage.class).timeBomb3Hours();
        jira().visit(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);
    }

    @Test
    public void importSucceeded() {
        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("subtasks.csv")).next();
        TrackProgressPage progressPage = fieldsPage.create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));
    }

    @Test
    public void importToExistingParent() {
        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("subtasks-existing.csv")).next();
        TrackProgressPage progressPage = fieldsPage.create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));
    }
}
