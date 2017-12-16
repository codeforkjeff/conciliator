package com.codefork.refine;

import com.codefork.refine.resources.NameType;
import com.fasterxml.jackson.databind.JsonNode;

public interface SearchQueryFactory {

    SearchQuery createSearchQuery(JsonNode queryStruct);

    SearchQuery createSearchQuery(String query, int limit, NameType nameType, String typeStrict);

}
