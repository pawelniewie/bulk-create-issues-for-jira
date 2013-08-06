package it;

import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.pageobjects.pages.project.BrowseProjectPage;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.rest.client.domain.input.ComponentInput;
import com.atlassian.jira.rest.client.domain.input.VersionInput;
import com.atlassian.jira.tests.TestBase;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.pawelniewiadomski.devs.jira.pageobjects.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static it.TestUtils.createRestClient;
import static org.junit.Assert.*;

public class TestAllCsv extends TestBase {

    @Inject
    private PageElementFinder elementFinder;
    private JiraRestClient rest;
    private BulkCreateIssuesPage createPage;

    @Before
    public void setUpTest() throws JSONException {
        backdoor().restoreDataFromResource("xml/testproject.xml");

        rest = createRestClient(jira().environmentData());
        backdoor().plugins().setPluginLicense("com.pawelniewiadomski.devs.jira.bulk-create-plugin",
                "AAABpA0ODAoPeNp9U11P2zAUfc+vsLQ3pFRxB9paKQ+08UY3aKOSAkJ7cd3b1sy1o+uk0H+P5xDhR\n" +
                        "IzX43PPOffDX26MJgtREZoQOhyffx8nI5JnBRkmlEY7BNB7U5aAg2spQFtgG1lJo1M2L9gyX85uW\n" +
                        "bTmh7UxnxC20u7hBC2jOJWQ/nAYO8GfMcngCMo4i0goc3x38rSphzqseX1YAy62Kwto04uoVPVOa\n" +
                        "js4G3BRySOkFdYQCaO3HSCvUey5hYxXkA4ppXEyium3KHCb8wOkGbtj14ucLdsX9lJKPPmy/OtV2\n" +
                        "20ofQvoQs6ydPJzVMQPq7vz+Pfj41U8Seh99CSRd1r6NVteEqYrwBKl7U3gX+pO/w5QNWjxAa8dx\n" +
                        "FTV1qnNzQZsmvTW4VUmHvqfaZjwg+0JrIVcq+76pm9gR+iGS+eguUvbG5pA87zpCTikU/326sa40\n" +
                        "n+1edbRAndcS8t9ostKcWsl1++Bwh1METyvv97GOWS21xhiGViBsvRGBdiKqCYM2RokzYGRTZvUd\n" +
                        "v5FKOMnGQLsyFXd5G+usrnwkPIKi15AZTAsAhRNXNoDT5n8OQAYwm8pA7TwPhPMTgIUNl+tH3CnC\n" +
                        "dVz6gON9AZUeMFjWSQ=X02k8");

        jira().gotoLoginPage().loginAsSysAdmin(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);

        rest.getComponentClient().createComponent("TST", new ComponentInput("Core", null, null, null), new NullProgressMonitor());
        rest.getComponentClient().createComponent("TST", new ComponentInput("Test", null, null, null), new NullProgressMonitor());
        rest.getVersionRestClient().createVersion(VersionInput.create("TST", "1.0", null, null, false, true), new NullProgressMonitor());
        rest.getVersionRestClient().createVersion(VersionInput.create("TST", "2.0", null, null, false, false), new NullProgressMonitor());
        rest.getVersionRestClient().createVersion(VersionInput.create("TST", "3.0", null, null, false, false), new NullProgressMonitor());
    }

    static void checkMate(final JiraRestClient rest) {
        SearchResult search = rest.getSearchClient().searchJql("project=TST", new NullProgressMonitor());
        assertEquals(3, search.getTotal());

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
            assertEquals("Test", is1.getSummary());
            assertEquals("admin", is1.getReporter().getName());
            assertEquals("Major", is1.getPriority().getName());
            assertEquals("Bug", is1.getIssueType().getName());
            assertEquals("Description it is", is1.getDescription());
            assertEquals(ImmutableSet.of("Core", "Test"), TestUtils.getComponentNames(is1.getComponents()));
            assertEquals(ImmutableSet.of("1.0"), TestUtils.getVersionNames(is1.getAffectedVersions()));
            assertEquals(ImmutableSet.of("2.0"), TestUtils.getVersionNames(is1.getFixVersions()));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-2", new NullProgressMonitor());
            assertEquals("Test 1", is1.getSummary());
            assertEquals("admin", is1.getReporter().getName());
            assertEquals("luser", is1.getAssignee().getName());
            assertNull(is1.getPriority());
            assertEquals("Bug", is1.getIssueType().getName());
            assertEquals("Description $2", is1.getDescription());
            assertTrue(Iterables.isEmpty(is1.getComponents()));
            assertTrue(Iterables.isEmpty(is1.getFixVersions()));
            assertTrue(Iterables.isEmpty(is1.getAffectedVersions()));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-3", new NullProgressMonitor());
            assertEquals("Test 2", is1.getSummary());
            assertEquals("admin", is1.getReporter().getName());
            assertNull(is1.getPriority());
            assertEquals("Bug", is1.getIssueType().getName());
            assertEquals("Description it is", is1.getDescription());
            assertTrue(Iterables.isEmpty(is1.getComponents()));
            assertEquals(ImmutableSet.of("1.0"), TestUtils.getVersionNames(is1.getAffectedVersions()));
            assertEquals(ImmutableSet.of("2.0", "3.0"), TestUtils.getVersionNames(is1.getFixVersions()));
        }
    }

    @Test
    public void testSimpleImport() {
        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("all.csv")).next();
        fieldsPage.setMapping("Who", "Assignee").setMapping("Prio", "Priority").setMapping("Env", "Environment")
                .setMapping("Component1", "Component/s").setMapping("Affects", "Affects Version/s").setMapping("Fix", "Fix Version/s");
        TrackProgressPage progressPage = fieldsPage.create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        checkMate(rest);
    }

    @Test
    public void testImportWithMappings() {
        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("needToMapValues.csv")).next();
        fieldsPage.setMapping("Who", "Assignee").setMapping("Prio", "Priority").setMapping("Env", "Environment")
                .setMapping("Component1", "Component/s").setMapping("Affects", "Affects Version/s").setMapping("Fix", "Fix Version/s");
        MapValuesPage valuesPage = fieldsPage.mapValues();

        valuesPage.setMapping("Component", "c-o-r-e", "Core").setMapping("Fix", "two", "2.0").setMapping("Fix", "three", "3.0");

        TrackProgressPage progressPage = valuesPage.create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        checkMate(rest);
    }

}
