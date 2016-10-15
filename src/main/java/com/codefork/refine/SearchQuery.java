
package com.codefork.refine;

import com.codefork.refine.resources.NameType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a single query in a request sent by Open Refine.
 * For the JSON format of this query, see
 * https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation-Service-API
 *
 * There's not much documentation about the type_strict field:
 * when it's set, it's always "should" although the docs say there
 * are other possible values.
 *
 * TODO: there's a "properties" key not yet modeled here, which
 * could be useful for specifying per-name search parameters
 */
public class SearchQuery {

    private String query;
    private int limit;
    private NameType nameType;
    private String typeStrict;
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

        SearchQuery searchQuery = new SearchQuery(
                queryStruct.path("query").asText().trim(),
                limit,
                nameType,
                typeStrict,
                extraParams
        );

        return searchQuery;
    }

    public SearchQuery(String query, int limit, NameType nameType, String typeStrict, Map<String, String> extraParams) {
        this.query = query;
        this.limit = limit;
        this.nameType = nameType;
        this.typeStrict = typeStrict;
        this.extraParams = extraParams;
    }

    /**
     * Constructor setting proxyMode = false
     */
    public SearchQuery(String query, int limit, NameType nameType, String typeStrict) {
        this(query, limit, nameType, typeStrict, Collections.EMPTY_MAP);
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
