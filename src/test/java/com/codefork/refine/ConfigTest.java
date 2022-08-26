package com.codefork.refine;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigTest {

    @Test
    public void testBasic() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("datasource.viaf.somekey", "2000");
        properties.setProperty("junk", "junk");

        Config config = new Config();
        config.merge(properties);
        Properties dsProps = config.getDataSourceProperties("viaf");
        assertEquals(1, dsProps.size());
        assertEquals(dsProps.getProperty("somekey"), "2000");

        Properties dsProps2 = config.getDataSourceProperties("nonexistent");
        assertEquals(0, dsProps2.size());
    }

}
