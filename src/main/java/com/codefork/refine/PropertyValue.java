package com.codefork.refine;

public abstract class PropertyValue {

    public enum PropertyValueType {
        ID, NUMBER, STRING
    }

    public abstract PropertyValueType getValueType();

    public boolean isId() {
        return PropertyValueType.ID.equals(getValueType());
    }

    public boolean isNumber() {
        return PropertyValueType.NUMBER.equals(getValueType());
    }

    public boolean isString() {
        return PropertyValueType.STRING.equals(getValueType());
    }

    public abstract String asString();
}
