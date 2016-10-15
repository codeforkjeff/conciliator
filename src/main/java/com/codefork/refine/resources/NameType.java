
package com.codefork.refine.resources;

/**
 * Name Types are a JSON object found in the service metadata
 * and in the results data.
 */
public class NameType {

    private String id;
    private String name;

    public NameType(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NameType) {
            NameType obj2 = (NameType) obj;
            return obj2.getId().equals(getId())
                    && obj2.getName().equals(getName());
        }
        return false;
    }
}
