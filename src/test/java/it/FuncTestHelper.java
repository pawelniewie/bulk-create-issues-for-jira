package it;

import com.atlassian.jira.functest.framework.*;
import com.atlassian.jira.functest.framework.assertions.Assertions;
import com.atlassian.jira.functest.framework.assertions.AssertionsImpl;
import com.atlassian.jira.webtests.WebTesterFactory;
import com.atlassian.jira.webtests.util.JIRAEnvironmentData;
import com.meterware.httpunit.HttpUnitOptions;
import net.sourceforge.jwebunit.WebTester;
import com.atlassian.jira.webtests.util.JIRAEnvironmentData;

public class FuncTestHelper {

    private Administration administration;
    private WebTester tester;
    private Navigation navigation;
    private Assertions assertions;
    private LocatorFactory locator;

    public FuncTestHelper(JIRAEnvironmentData environmentData) {
        HttpUnitOptions.reset();

        HttpUnitOptions.setExceptionsThrownOnScriptError(false);
        //as we don't need to use Rhino - we can disable for a (slight) performance improvement
        HttpUnitOptions.setScriptingEnabled(false);

        tester = new WebTester();

        initWebTester(environmentData);

        navigation = new NavigationImpl(tester, environmentData);
        locator = new LocatorFactoryImpl(tester);
        assertions = new AssertionsImpl(tester, environmentData, navigation, locator);
        administration = new AdministrationImpl(tester, environmentData, navigation, assertions);
    }

    private void initWebTester(JIRAEnvironmentData environmentData)
    {
        // setup things ready for testing
        WebTesterFactory.setupWebTester(tester, environmentData);
        tester.beginAt("/");
    }

    public Administration getAdministration() {
        return administration;
    }

    public WebTester getTester() {
        return tester;
    }

    public Navigation getNavigation() {
        return navigation;
    }

    public Assertions getAssertions() {
        return assertions;
    }

    public LocatorFactory getLocator() {
        return locator;
    }
}
