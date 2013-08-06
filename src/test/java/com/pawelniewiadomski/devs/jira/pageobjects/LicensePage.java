package com.pawelniewiadomski.devs.jira.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;

import javax.annotation.Nonnull;

public class LicensePage extends AbstractJiraPage {

    @ElementBy(name="license")
    private PageElement license;

    @ElementBy(id="update")
    private PageElement update;

    public LicensePage updateLicense(@Nonnull String license) {
        this.license.clear().type(license);
        this.update.click();
        return pageBinder.bind(LicensePage.class);
    }

    public LicensePage timeBomb3Hours() {
        return updateLicense(
                "AAABpA0ODAoPeNp9U11P2zAUfc+vsLQ3pFRxB9paKQ+08UY3aKOSAkJ7cd3b1sy1o+uk0H+P5xDhR\n" +
                "IzX43PPOffDX26MJgtREZoQOhyffx8nI5JnBRkmlEY7BNB7U5aAg2spQFtgG1lJo1M2L9gyX85uW\n" +
                "bTmh7UxnxC20u7hBC2jOJWQ/nAYO8GfMcngCMo4i0goc3x38rSphzqseX1YAy62Kwto04uoVPVOa\n" +
                "js4G3BRySOkFdYQCaO3HSCvUey5hYxXkA4ppXEyium3KHCb8wOkGbtj14ucLdsX9lJKPPmy/OtV2\n" +
                "20ofQvoQs6ydPJzVMQPq7vz+Pfj41U8Seh99CSRd1r6NVteEqYrwBKl7U3gX+pO/w5QNWjxAa8dx\n" +
                "FTV1qnNzQZsmvTW4VUmHvqfaZjwg+0JrIVcq+76pm9gR+iGS+eguUvbG5pA87zpCTikU/326sa40\n" +
                "n+1edbRAndcS8t9ostKcWsl1++Bwh1METyvv97GOWS21xhiGViBsvRGBdiKqCYM2RokzYGRTZvUd\n" +
                "v5FKOMnGQLsyFXd5G+usrnwkPIKi15AZTAsAhRNXNoDT5n8OQAYwm8pA7TwPhPMTgIUNl+tH3CnC\n" +
                "dVz6gON9AZUeMFjWSQ=X02k8");
    }

    @Override
    public TimedCondition isAt() {
        return license.timed().isVisible();
    }

    @Override
    public String getUrl() {
        return "/plugins/servlet/com.pawelniewiadomski.devs.jira.jira-bulk-create-plugin/license";
    }
}
