package com.codefork.refine;

public class PropertyValueId extends PropertyValue {

    private String id;

    public PropertyValueId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public PropertyValueType getValueType() {
        return PropertyValueType.ID;
    }

    @Override
    public String asString() {
        return id;
    }

}
