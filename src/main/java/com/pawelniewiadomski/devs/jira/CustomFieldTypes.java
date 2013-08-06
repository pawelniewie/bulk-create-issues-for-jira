package com.pawelniewiadomski.devs.jira;

import com.atlassian.jira.jelly.tag.admin.CreateCustomField;

public class CustomFieldTypes {

    private CustomFieldTypes() {};

    public static final String LABELS_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "labels";
    public static final String FREE_TEXT_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "textarea";
    public static final String NUMBER_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "float";
    public static final String SELECT_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "select";
    public static final String MULTICHECKBOXES_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "multicheckboxes";
    public static final String RADIOBUTTONS_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "radiobuttons";
    public static final String URL_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "url";
    public static final String MULTISELECT_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "multiselect";
    public static final String TEXT_CF_TYPE = CreateCustomField.FIELD_TYPE_PREFIX + "textfield";

}
