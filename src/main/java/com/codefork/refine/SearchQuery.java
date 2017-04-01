
package com.codefork.refine;

import com.codefork.refine.resources.NameType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Collections;
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
    private Map<String, String> extraParams;

    /**
     * Factory method that builds SearchQuery instances out of the JSON structure
     * representing a single name query.
     * @return SearchQuery
     */
    public static SearchQuery createFromJson(JsonNode queryStruct, Map<String, String> extraParams) {

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

        Map<String, PropertyValue> properties = new HashMap<String, PropertyValue>();
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

        SearchQuery searchQuery = new SearchQuery(
                queryStruct.path("query").asText().trim(),
                limit,
                nameType,
                typeStrict,
                properties,
                extraParams
        );

        return searchQuery;
    }

    public SearchQuery(String query, int limit, NameType nameType, String typeStrict,
                       Map<String, PropertyValue> properties,
                       Map<String, String> extraParams) {
        this.query = query;
        this.limit = limit;
        this.nameType = nameType;
        this.typeStrict = typeStrict;
        this.properties = properties;
        this.extraParams = extraParams;
    }

    public SearchQuery(String query, int limit, NameType nameType, String typeStrict,
                       Map<String, String> extraParams) {
        this(query, limit, nameType, typeStrict, null, extraParams);
    }

    /**
     * Constructor setting proxyMode = false
     */
    public SearchQuery(String query, int limit, NameType nameType, String typeStrict) {
        this(query, limit, nameType, typeStrict, null, Collections.EMPTY_MAP);
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

    public Map<String, String> getExtraParams() {
        return extraParams;
    }

    public String getHashKey() {
        StringBuilder buf = new StringBuilder();
        buf.append((query != null ? query : "") + "|" +
                limit + "|" +
                (nameType != null ? nameType.getId() : "") + "|" +
                (typeStrict != null ? typeStrict : ""));

        Map<String, String> extraParams = getExtraParams();
        Object[] keysAsObj = extraParams.keySet().toArray();
        String[] keys = Arrays.copyOf(keysAsObj, keysAsObj.length, String[].class);
        Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        for(String key : keys) {
            buf.append("|" + key + "=" + extraParams.get(key));
        }
        return buf.toString();
    }

}
