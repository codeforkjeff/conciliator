package com.codefork.refine.controllers;

import com.codefork.refine.ExtensionQuery;
import com.codefork.refine.PropertyValueIdAndSettings;
import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.datasource.ServiceNotImplementedException;
import com.codefork.refine.resources.ExtensionResponse;
import com.codefork.refine.resources.ProposePropertiesResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface DataExtensionAPI {

    public static String PATH_PROPOSE_PROPERTIES = "/propose_properties";

    Log getLog();

    DataSource getDataSource();

    // Data Extension API
    // https://github.com/OpenRefine/OpenRefine/wiki/Data-Extension-API
    @RequestMapping(value = { "", "/" }, params = "extend")
    @ResponseBody
    default ExtensionResponse extend(@RequestParam(value = "extend") String extend)
            throws ServiceNotImplementedException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = null;
        try {
            root = mapper.readTree(extend);
        } catch(IOException ioe) {
            getLog().error("Error parsing JSON: " + extend);
        }

        List<String> ids = new ArrayList<>();
        if(root != null) {
            JsonNode idNodes = root.get("ids");
            if(idNodes.isArray()) {
                Iterator<JsonNode> iter = idNodes.iterator();
                while(iter.hasNext()) {
                    JsonNode id = iter.next();
                    ids.add(id.asText());
                }
            }
        }

        List<PropertyValueIdAndSettings> properties = new ArrayList<>();
        if(root != null) {
            JsonNode propertyNodes = root.get("properties");
            if(propertyNodes.isArray()) {
                Iterator<JsonNode> iter = propertyNodes.iterator();
                while(iter.hasNext()) {
                    JsonNode propertyNode = iter.next();
                    if(propertyNode.isObject()) {
                        properties.add(new PropertyValueIdAndSettings(propertyNode.get("id").asText()));
                    }
                }
            }
        }

        return getDataSource().extend(new ExtensionQuery(ids, properties));
    }

    @RequestMapping(value = { PATH_PROPOSE_PROPERTIES })
    @ResponseBody
    default ProposePropertiesResponse proposeProperties(
            @RequestParam(value = "type") String type, @RequestParam(value = "limit", required = false) String limit)
            throws ServiceNotImplementedException {
        int limitArg = limit != null ? Integer.valueOf(limit) : -1;
        return getDataSource().proposeProperties(type, limitArg);
    }

}
