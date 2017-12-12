
package com.codefork.refine;

import com.codefork.refine.resources.NameType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a single query in a request sent by Open Refine.
 * For the JSON format of this query, see
 * https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation-Service-API
 *
 * There's not much documentation about the type_strict field:
 * when it's set, it's always "should" although the docs say there
 * are other possible values.
 */
public class SearchQuery {

    private String query;
    private int limit;
    private NameType nameType;
    private String typeStrict;
    private Map<String, PropertyValue> properties;

    // below are data source specific search parameters; we don't subclass SearchQuery
    // b/c it's a nightmare to get the types right in all the code in the DataSource
    // hierarchy

    private String viafSource = null;
    private boolean isViafProxyMode = false;

    private boolean isOrcidSmartNamesMode = false;

    public SearchQuery(String query, int limit, NameType nameType, String typeStrict,
                       Map<String, PropertyValue> properties) {
        this.query = query;
        this.limit = limit;
        this.nameType = nameType;
        this.typeStrict = typeStrict;
        this.properties = properties;
    }

    public SearchQuery(String query, int limit, NameType nameType, String typeStrict) {
        this(query, limit, nameType, typeStrict, null);
    }

    public SearchQuery(JsonNode queryStruct) {
        int limit = queryStruct.path("limit").asInt();
        if(limit == 0) {
            limit = 3;
        }

        String typeFromJson = queryStruct.path("type").asText();
        NameType nameType = null;
        if(typeFromJson != null && typeFromJson.length() > 0) {
            nameType = new NameType(typeFromJson, null);
        }

        String typeStrict = null;
        if(!queryStruct.path("type_strict").isMissingNode()) {
            typeStrict = queryStruct.path("type_strict").asText();
        }

        Map<String, PropertyValue> properties = new HashMap<>();
        if(!queryStruct.path("properties").isMissingNode()) {
            Iterator<JsonNode> propObjects = queryStruct.path("properties").elements();
            while(propObjects.hasNext()) {
                JsonNode prop = propObjects.next();
                String key = null;
                if(!prop.path("p").isMissingNode()) {
                    key = prop.path("p").asText();
                } else if(!prop.path("pid").isMissingNode()) {
                    key = prop.path("pid").asText();
                }

                JsonNode valNode = prop.path("v");
                PropertyValue val = null;
                if(!valNode.isMissingNode()) {
                    if(valNode.isTextual()) {
                        val = new PropertyValueString(valNode.asText());
                    } else if(valNode.isNumber()) {
                        val = new PropertyValueNumber(valNode.asLong());
                    } else if(!valNode.path("id").isMissingNode()) {
                        val = new PropertyValueId(valNode.path("id").asText());
                    }
                }

                properties.put(key, val);
            }
        }

        this.query = queryStruct.path("query").asText().trim();
        this.limit = limit;
        this.nameType = nameType;
        this.typeStrict = typeStrict;
        this.properties = properties;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getLimit() {
        return limit;
    }

    public NameType getNameType() {
        return nameType;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setNameType(NameType nameType) {
        this.nameType = nameType;
    }

    public String getTypeStrict() {
        return typeStrict;
    }

    public void setTypeStrict(String typeStrict) {
        this.typeStrict = typeStrict;
    }

    public Map<String, PropertyValue> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, PropertyValue> properties) {
        this.properties = properties;
    }

    public String getViafSource() {
        return viafSource;
    }

    public void setViafSource(String viafSource) {
        this.viafSource = viafSource;
    }

    public boolean isViafProxyMode() {
        return isViafProxyMode;
    }

    public void setViafProxyMode(boolean viafProxyMode) {
        isViafProxyMode = viafProxyMode;
    }

    public boolean isOrcidSmartNamesMode() {
        return isOrcidSmartNamesMode;
    }

    public void setOrcidSmartNamesMode(boolean orcidSmartNamesMode) {
        isOrcidSmartNamesMode = orcidSmartNamesMode;
    }

    public String getHashKey() {
        StringBuilder buf = new StringBuilder();
        buf.append((query != null ? query : "") + "|" +
                limit + "|" +
                (nameType != null ? nameType.getId() : "") + "|" +
                (typeStrict != null ? typeStrict : "") + "|" +
                (viafSource != null ? viafSource : "") + "|" +
                String.valueOf(isViafProxyMode) + "|" +
                String.valueOf(isOrcidSmartNamesMode));

        return buf.toString();
    }

}
