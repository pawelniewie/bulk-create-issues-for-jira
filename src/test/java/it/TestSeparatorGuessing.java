package it;

import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.pageobjects.pages.project.BrowseProjectPage;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.input.ComponentInput;
import com.atlassian.jira.rest.client.domain.input.VersionInput;
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
public class TestSeparatorGuessing extends TestBase {
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

        rest.getComponentClient().createComponent("TST", new ComponentInput("Core", null, null, null), new NullProgressMonitor());
        rest.getComponentClient().createComponent("TST", new ComponentInput("Test", null, null, null), new NullProgressMonitor());
        rest.getVersionRestClient().createVersion(VersionInput.create("TST", "1.0", null, null, false, true), new NullProgressMonitor());
        rest.getVersionRestClient().createVersion(VersionInput.create("TST", "2.0", null, null, false, false), new NullProgressMonitor());
        rest.getVersionRestClient().createVersion(VersionInput.create("TST", "3.0", null, null, false, false), new NullProgressMonitor());
    }

    @Test
    public void separatedByTabs() {
        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("all-tabs.csv")).next();
        fieldsPage.setMapping("Who", "Assignee").setMapping("Prio", "Priority").setMapping("Env", "Environment")
                .setMapping("Component1", "Component/s").setMapping("Affects", "Affects Version/s").setMapping("Fix", "Fix Version/s");
        TrackProgressPage progressPage = fieldsPage.create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        TestAllCsv.checkMate(rest);
    }

    @Test
    public void separatedBySemicolon() {
        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("all-semicolons.csv")).next();
        fieldsPage.setMapping("Who", "Assignee").setMapping("Prio", "Priority").setMapping("Env", "Environment")
                .setMapping("Component1", "Component/s").setMapping("Affects", "Affects Version/s").setMapping("Fix", "Fix Version/s");
        TrackProgressPage progressPage = fieldsPage.create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        TestAllCsv.checkMate(rest);
    }

}
