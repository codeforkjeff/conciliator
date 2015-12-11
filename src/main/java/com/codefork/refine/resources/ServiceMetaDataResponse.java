package com.codefork.refine.resources;

import com.codefork.refine.Config;
import com.codefork.refine.NameType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata about this reconciliation service.
 */
public class ServiceMetaDataResponse {

    Log log = LogFactory.getLog(ServiceMetaDataResponse.class);

    public static class View {
        private String url = "http://viaf.org/viaf/{{id}}";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    
    }
    
    private String name = "";
    private final static String IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/user/hangy/viaf";
    private final static View VIEW = new View();
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<VIAFNameType> DEFAULT_TYPES = new ArrayList<VIAFNameType>();

    static {
        for(NameType nameType : NameType.values()) {
            DEFAULT_TYPES.add(nameType.asVIAFNameType());
        }
    }
    
    public ServiceMetaDataResponse(Config config) {
        setName(config.getServiceName());
    }

    public ServiceMetaDataResponse(Config config, String source) {
        this(config);
        if(source != null) {
            setName(getName() + " - " + source);
        }
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifierSpace() {
        return IDENTIFIER_SPACE;
    }

    public String getSchemaSpace() {
        return SCHEMA_SPACE;
    }

    public View getView() {
        return VIEW;
    }

    public List<VIAFNameType> getDefaultTypes() {
        return DEFAULT_TYPES;
    }
    
}
