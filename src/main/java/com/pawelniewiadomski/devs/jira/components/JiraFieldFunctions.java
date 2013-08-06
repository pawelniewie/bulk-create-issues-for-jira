package com.pawelniewiadomski.devs.jira.components;

import com.google.common.base.Function;

import javax.annotation.Nullable;

public class JiraFieldFunctions {

    public static final Function<JiraField, String> GET_NAME = new Function<JiraField, String>() {
        @Override
        public String apply(@Nullable JiraField jiraField) {
            return jiraField.getName();
        }
    };

}
