package com.codefork.refine.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertySetting<T> {

    @JsonProperty("default")
    private T defaultValue;
    private String type;
    private String label;
    private String name;

    private String helpText;
    private List<PropertySettingChoice<T>> choices;

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public List<PropertySettingChoice<T>> getChoices() {
        return choices;
    }

    public void setChoices(List<PropertySettingChoice<T>> choices) {
        this.choices = choices;
    }
}
