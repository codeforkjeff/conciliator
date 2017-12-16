package com.codefork.refine.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Extend {

    @JsonProperty("propose_properties")
    private ProposeProperties proposeProperties;

    @JsonProperty("property_settings")
    private List<PropertySetting> propertySettings;

    public Extend(ProposeProperties proposeProperties) {
        this.proposeProperties = proposeProperties;
    }

    public ProposeProperties getProposeProperties() {
        return proposeProperties;
    }

    public void setProposeProperties(ProposeProperties proposeProperties) {
        this.proposeProperties = proposeProperties;
    }

    public List<PropertySetting> getPropertySettings() {
        return propertySettings;
    }

    public void setPropertySettings(List<PropertySetting> propertySettings) {
        this.propertySettings = propertySettings;
    }
}
