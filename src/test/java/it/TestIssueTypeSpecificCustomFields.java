package it;


import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.pageobjects.pages.project.BrowseProjectPage;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.tests.TestBase;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.google.common.collect.Iterables;
import com.pawelniewiadomski.devs.jira.pageobjects.BulkCreateIssuesPage;
import com.pawelniewiadomski.devs.jira.pageobjects.LicensePage;
import com.pawelniewiadomski.devs.jira.pageobjects.TrackProgressPage;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static it.TestUtils.createRestClient;
import static org.junit.Assert.*;

@Restore("xml/issueTypeSpecificCustomField.xml")
public class TestIssueTypeSpecificCustomFields extends TestBase {

    private JiraRestClient rest;
    @Inject
    private PageElementFinder elementFinder;
    private BulkCreateIssuesPage createPage;


    @Before
    public void setUp() {
        rest = createRestClient(jira().environmentData());

        jira().gotoLoginPage().loginAsSysAdmin(LicensePage.class).timeBomb3Hours();
    }

    @Test
    public void issueTypeSpecificCustomField() {
        final String customFieldName = "Defect Severity";

        jira().visit(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);

        TrackProgressPage progressPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("issueTypeSpecificCustomField.csv")).next().mapValues()
                .setMapping(customFieldName, "DDD", "4").create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-6", new NullProgressMonitor());
            assertEquals("An example of defect", is1.getSummary());
            assertThat(is1.getFieldByName(customFieldName).getValue().toString(), Matchers.containsString("\"value\":\"2\""));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-7", new NullProgressMonitor());
            assertEquals("An example of defect mapping", is1.getSummary());
            assertThat(is1.getFieldByName(customFieldName).getValue().toString(), Matchers.containsString("\"value\":\"4\""));
        }
    }
}
