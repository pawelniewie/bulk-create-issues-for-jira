package it;

import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.pageobjects.pages.project.BrowseProjectPage;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.tests.TestBase;
import com.google.common.collect.Iterables;
import com.pawelniewiadomski.devs.jira.pageobjects.BulkCreateIssuesPage;
import com.pawelniewiadomski.devs.jira.pageobjects.LicensePage;
import com.pawelniewiadomski.devs.jira.pageobjects.MapFieldsPage;
import com.pawelniewiadomski.devs.jira.pageobjects.TrackProgressPage;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import static it.TestUtils.createRestClient;
import static org.junit.Assert.*;

@Restore("xml/testproject.xml")
public class TestImport2 extends TestBase {

	private JiraRestClient rest;
	private BulkCreateIssuesPage createPage;

	@Before
	public void setUpTest() {
		rest = createRestClient(jira().environmentData());

		jira().gotoLoginPage().loginAsSysAdmin(LicensePage.class).timeBomb3Hours();
        jira().visit(BrowseProjectPage.class, "TST");
		jira().getTester().getDriver().findElement(By.cssSelector(".bulk-create-issues")).click();
		createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);
	}

	@Test
	public void summaryOnly() {
        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("Summary.csv")).next();
		TrackProgressPage progressPage = fieldsPage.create().waitUntilFinished();
		assertTrue(Iterables.isEmpty(progressPage.getMessages()));

		SearchResult search = rest.getSearchClient().searchJql("project=TST", new NullProgressMonitor());
		assertEquals(2, search.getTotal());

		{
			Issue is1 = rest.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
			assertEquals("Test", is1.getSummary());
			assertEquals("admin", is1.getReporter().getName());
			assertEquals("admin", is1.getAssignee().getName());
			assertNull(is1.getPriority());
			assertEquals("Bug", is1.getIssueType().getName());
			assertNull(is1.getDescription());
			assertTrue(Iterables.isEmpty(is1.getComponents()));
			assertTrue(Iterables.isEmpty(is1.getFixVersions()));
			assertTrue(Iterables.isEmpty(is1.getAffectedVersions()));
		}

		{
			Issue is1 = rest.getIssueClient().getIssue("TST-2", new NullProgressMonitor());
			assertEquals("Raz", is1.getSummary());
			assertEquals("admin", is1.getReporter().getName());
			assertEquals("admin", is1.getAssignee().getName());
			assertNull(is1.getPriority());
			assertEquals("Bug", is1.getIssueType().getName());
			assertNull(is1.getDescription());
			assertTrue(Iterables.isEmpty(is1.getComponents()));
			assertTrue(Iterables.isEmpty(is1.getFixVersions()));
			assertTrue(Iterables.isEmpty(is1.getAffectedVersions()));
		}
	}

	@Test
	public void checkMessageIncludesSummary() {
		MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("InvalidAssignee.csv")).next();
		fieldsPage.setMapping("UMRY", "Summary").setMapping("AGNE", "Assignee");
		TrackProgressPage progressPage = fieldsPage.create().waitUntilFinished();
		String message = Iterables.getFirst(progressPage.getMessages(), null);
		assertThat(message, Matchers.containsString("Error importing issue Test: User 'i don't exist'"));
	}

}
