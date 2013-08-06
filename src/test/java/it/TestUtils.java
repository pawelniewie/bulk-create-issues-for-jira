package it;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.domain.BasicComponent;
import com.atlassian.jira.rest.client.domain.Version;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.atlassian.jira.webtests.util.JIRAEnvironmentData;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.Ignore;

import javax.annotation.Nullable;
import java.net.URISyntaxException;

@Ignore
public class TestUtils  {
	public static JiraRestClient createRestClient(JIRAEnvironmentData jiraEnvironmentData) {
		final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
		try {
			return factory.createWithBasicHttpAuthentication(jiraEnvironmentData.getBaseUrl().toURI(), "admin", "admin");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static Function<BasicComponent, String> GetName = new Function<BasicComponent, String>() {
		@Override
		public String apply(@Nullable BasicComponent input) {
			return input.getName();
		}
	};

	public static Function<Version, String> GetVersionName = new Function<Version, String>() {
		@Override
		public String apply(@Nullable Version input) {
			return input.getName();
		}
	};

	public static ImmutableSet<String> getComponentNames(Iterable<BasicComponent> components) {
		return ImmutableSet.copyOf(Iterables.transform(components, GetName));
	}

	public static ImmutableSet<String> getVersionNames(Iterable<Version> fixVersions) {
		return ImmutableSet.copyOf(Iterables.transform(fixVersions, GetVersionName));
	}
}
