package com.codefork.refine;

public class PropertyValueString extends PropertyValue {

    private String string;

    public PropertyValueString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public PropertyValueType getValueType() {
        return PropertyValueType.STRING;
    }

    @Override
    public String asString() {
        return string;
    }
}
