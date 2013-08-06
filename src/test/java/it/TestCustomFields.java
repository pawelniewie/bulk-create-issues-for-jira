package it;

import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.functest.framework.Administration;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.pageobjects.pages.project.BrowseProjectPage;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.tests.TestBase;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.pawelniewiadomski.devs.jira.pageobjects.BulkCreateIssuesPage;
import com.pawelniewiadomski.devs.jira.pageobjects.LicensePage;
import com.pawelniewiadomski.devs.jira.pageobjects.MapFieldsPage;
import com.pawelniewiadomski.devs.jira.pageobjects.TrackProgressPage;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.pawelniewiadomski.devs.jira.CustomFieldTypes.*;
import static it.TestUtils.createRestClient;
import static org.junit.Assert.*;

@Restore("xml/testproject.xml")
public class TestCustomFields extends TestBase {

    @Inject
    private PageElementFinder elementFinder;
    private BulkCreateIssuesPage createPage;
    private FuncTestHelper helper;
    private JiraRestClient rest;
    private Administration administration;

    @Before
    public void setUp() {
        rest = createRestClient(jira().environmentData());

        helper = new FuncTestHelper(jira().environmentData());
        helper.getNavigation().login("admin", "admin");
        helper.getNavigation().gotoAdmin();

        administration = helper.getAdministration();

        jira().gotoLoginPage().loginAsSysAdmin(LicensePage.class).timeBomb3Hours();
    }

    @Test
    public void singleSelectFieldsAreAvailableOnlyForSingleValueColumn() {
        administration.customFields().addCustomField(LABELS_CF_TYPE, "Lbls");
        administration.customFields().addCustomField(FREE_TEXT_CF_TYPE, "Free Text");
        administration.customFields().addCustomField(NUMBER_CF_TYPE, "Number");
        administration.customFields().addCustomField(SELECT_CF_TYPE, "Select");
        administration.customFields().addCustomField(MULTICHECKBOXES_CF_TYPE, "Checkboxes");
        administration.customFields().addCustomField(RADIOBUTTONS_CF_TYPE, "Radiobuttons");
        administration.customFields().addCustomField(URL_CF_TYPE, "URL");
        administration.customFields().addCustomField(MULTISELECT_CF_TYPE, "Multiselect");
        administration.customFields().addCustomField(TEXT_CF_TYPE, "Text");

        jira().visit(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);

        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("multiselect.csv")).next();

        assertEquals(ImmutableList.of("", "Component/s", "Affects Version/s", "Fix Version/s", "Labels", "Lbls",
                "Checkboxes", "Multiselect"), MapFieldsPage.getText(fieldsPage.getPossibleMappings("Multi")));
        assertEquals(ImmutableList.of("", "Summary", "Issue Type", "Security Level", "Priority", "Due Date",
                "Component/s", "Affects Version/s", "Fix Version/s", "Assignee", "Environment", "Description",
                "Time Tracking", "Labels", "Lbls", "Free Text", "Number", "Select", "Checkboxes", "Radiobuttons",
                "URL", "Multiselect", "Text", "Issue ID", "Parent ID", "Parent Key"), MapFieldsPage.getText(fieldsPage.getPossibleMappings("Summary")));
    }

    @Test
    public void importMulticheckboxesCustomFields() {
        final String customFieldName = "Multi";
        final String cid = StringUtils.removeStart(administration.customFields().addCustomField(MULTICHECKBOXES_CF_TYPE, customFieldName), FieldManager.CUSTOM_FIELD_PREFIX);
        administration.customFields().addOptions(cid, "Core", "Subsystem", "API", "UX");

        assertMultiCustomFields(customFieldName);
    }

    @Test
    public void importMultiselectCustomFields() {
        final String customFieldName = "Multi";
        final String cid = StringUtils.removeStart(administration.customFields().addCustomField(MULTISELECT_CF_TYPE, customFieldName), FieldManager.CUSTOM_FIELD_PREFIX);
        administration.customFields().addOptions(cid, "Core", "Subsystem", "API", "UX");

        assertMultiCustomFields(customFieldName);
    }

    private void assertMultiCustomFields(String customFieldName) {
        jira().visit(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);

        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("multiselect.csv")).next();
        TrackProgressPage progressPage = fieldsPage.mapValues().setMapping("Multi", "DDD", "Core").create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        SearchResult search = rest.getSearchClient().searchJql("project=TST", new NullProgressMonitor());
        assertEquals(5, search.getTotal());

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
            assertEquals("Only API", is1.getSummary());
            assertThat(is1.getFieldByName(customFieldName).getValue().toString(), Matchers.containsString("\"value\":\"API\""));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-2", new NullProgressMonitor());
            assertEquals("Only UX", is1.getSummary());
            assertThat(is1.getFieldByName(customFieldName).getValue().toString(), Matchers.containsString("\"value\":\"UX\""));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-3", new NullProgressMonitor());
            assertEquals("Invalid one", is1.getSummary());
            assertThat(is1.getFieldByName(customFieldName).getValue().toString(), Matchers.containsString("\"value\":\"Core\""));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-4", new NullProgressMonitor());
            assertEquals("None", is1.getSummary());
            assertNull(is1.getFieldByName(customFieldName).getValue());
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-5", new NullProgressMonitor());
            assertEquals("All", is1.getSummary());
            final String value = is1.getFieldByName(customFieldName).getValue().toString();
            assertThat(value, Matchers.containsString("\"value\":\"API\""));
            assertThat(value, Matchers.containsString("\"value\":\"Core\""));
            assertThat(value, Matchers.containsString("\"value\":\"UX\""));
        }
    }

    @Test
    public void importingAdditionalLabels() {
        administration.customFields().addCustomField(LABELS_CF_TYPE, "Additional labels");

        jira().visit(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);

        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("labels.csv")).next();
        TrackProgressPage progressPage = fieldsPage.setMapping("Additional labels", "Additional labels").create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        SearchResult search = rest.getSearchClient().searchJql("project=TST", new NullProgressMonitor());
        assertEquals(2, search.getTotal());

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
            assertEquals("All 3", is1.getSummary());
            assertEquals("[\"label\",\"os\",\"test\"]", is1.getFieldByName("Additional labels").getValue());
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-2", new NullProgressMonitor());
            assertEquals("Only one", is1.getSummary());
            assertEquals("[\"one\"]", is1.getFieldByName("Additional labels").getValue());
        }
    }

    @Test
    public void importingPrimitiveCustomFields() {
        administration.customFields().addCustomField(NUMBER_CF_TYPE, "Number");
        administration.customFields().addCustomField(FREE_TEXT_CF_TYPE, "Free Text");
        administration.customFields().addCustomField(URL_CF_TYPE, "URL");
        administration.customFields().addCustomField(TEXT_CF_TYPE, "Text");

        jira().visit(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);

        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("customFields.csv")).next();
        TrackProgressPage progressPage = fieldsPage.setMapping("Text", "Text").create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        SearchResult search = rest.getSearchClient().searchJql("project=TST", new NullProgressMonitor());
        assertEquals(3, search.getTotal());

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
            assertEquals("All of them", is1.getSummary());
            assertEquals("this is a text", is1.getFieldByName("Text").getValue());
            assertEquals("line 1\nline 2\nline 3", is1.getFieldByName("Free Text").getValue());
            assertEquals("http://pawelniewiadomski.com", is1.getFieldByName("URL").getValue());
            assertEquals(Double.valueOf(34.0), is1.getFieldByName("Number").getValue());
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-2", new NullProgressMonitor());
            assertEquals("Only number", is1.getSummary());
            assertEquals(Double.valueOf(123.0), is1.getFieldByName("Number").getValue());
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-3", new NullProgressMonitor());
            assertEquals("Text", is1.getSummary());
            assertEquals("text", is1.getFieldByName("Text").getValue());
        }
    }

    @Test
    public void importingWithSelectCustomField() {
        final String cid = StringUtils.removeStart(administration.customFields().addCustomField(SELECT_CF_TYPE, "Select"), FieldManager.CUSTOM_FIELD_PREFIX);
        administration.customFields().addOptions(cid, "Core", "Subsystem", "API", "UX");

        assertSelectOrRadio();
    }

    @Test
    public void importingWithRadioCustomField() {
        final String cid = StringUtils.removeStart(administration.customFields().addCustomField(RADIOBUTTONS_CF_TYPE, "Select"), FieldManager.CUSTOM_FIELD_PREFIX);
        administration.customFields().addOptions(cid, "Core", "Subsystem", "API", "UX");

        assertSelectOrRadio();
    }

    private void assertSelectOrRadio() {
        jira().visit(BrowseProjectPage.class, "TST");
        elementFinder.find(By.cssSelector("a.bulk-create-issues")).click();
        createPage = jira().getPageBinder().bind(BulkCreateIssuesPage.class);

        MapFieldsPage fieldsPage = createPage.setCsvFile(BulkCreateIssuesPage.getCsvResource("select.csv")).next();
        TrackProgressPage progressPage = fieldsPage.mapValues().setMapping("Select", "DDD", "Core").create().waitUntilFinished();
        assertTrue(Iterables.isEmpty(progressPage.getMessages()));

        SearchResult search = rest.getSearchClient().searchJql("project=TST", new NullProgressMonitor());
        assertEquals(4, search.getTotal());

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
            assertEquals("Only API", is1.getSummary());
            assertThat(is1.getFieldByName("Select").getValue().toString(), Matchers.containsString("\"value\":\"API\""));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-2", new NullProgressMonitor());
            assertEquals("Only UX", is1.getSummary());
            assertThat(is1.getFieldByName("Select").getValue().toString(), Matchers.containsString("\"value\":\"UX\""));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-3", new NullProgressMonitor());
            assertEquals("Invalid one", is1.getSummary());
            assertThat(is1.getFieldByName("Select").getValue().toString(), Matchers.containsString("\"value\":\"Core\""));
        }

        {
            Issue is1 = rest.getIssueClient().getIssue("TST-4", new NullProgressMonitor());
            assertEquals("None", is1.getSummary());
            assertNull(is1.getFieldByName("Select").getValue());
        }
    }
}
