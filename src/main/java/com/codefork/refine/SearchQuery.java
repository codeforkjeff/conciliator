
package com.codefork.refine;

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
    private String source;
    private boolean throughMode;

    public SearchQuery(String query, int limit, NameType nameType, String typeStrict, boolean throughMode) {
        this.query = query;
        this.limit = limit;
        this.nameType = nameType;
        this.typeStrict = typeStrict;
        this.throughMode = throughMode;
    }

    /**
     * Constructor setting throughMode = false
     */
    public SearchQuery(String query, int limit, NameType nameType, String typeStrict) {
        this(query, limit, nameType, typeStrict, false);
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isThroughMode() {
        return throughMode;
    }

    public void setThroughMode(boolean throughMode) {
        this.throughMode = throughMode;
    }

    /**
     * @return String used for the cql 'query' URL param passed to VIAF 
     */
   public String createCqlQueryString() {
        String cqlTemplate = "local.mainHeadingEl all \"%s\"";
        if(getNameType() != null) {
            cqlTemplate = getNameType().getCqlString();
        }
        String cql = String.format(cqlTemplate, getQuery());

        // NOTE: this query means return all the name records that 
        // have an entry for this source; it does NOT mean search the name 
        // values for this source ONLY. I think.
        if(getSource() != null) {
            cql += String.format(" and local.sources = \"%s\"", getSource().toLowerCase());
        }

        return cql;
    }

    public String getHashKey() {
        return (query != null ? query : "") + "|" +
                limit + "|" +
                (nameType != null ? nameType.getId() : "") + "|" +
                (typeStrict != null ? typeStrict : "") + "|" +
                (source != null ? source : "") + "|" +
                throughMode;
    }

}
