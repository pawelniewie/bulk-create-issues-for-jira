package com.pawelniewiadomski.devs.jira.components;

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JiraField {
    private final String id;
    private final String name;
    private boolean multi;
    private boolean custom;

    public JiraField(@Nonnull String id, @Nonnull String name, boolean multi, boolean custom) {
        this.multi = multi;
        this.name = name;
        this.id = id;
        this.custom = custom;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public boolean isMulti() {
        return multi;
    }

    public boolean isCustom() {
        return custom;
    }
}
