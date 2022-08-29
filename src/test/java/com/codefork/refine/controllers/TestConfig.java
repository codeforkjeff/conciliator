package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@Profile("test")
public class TestConfig extends Config {
    public static final int TTL_SECONDS = 1;

    public TestConfig() {
        super();
        Properties props = new Properties();
        props.setProperty(Config.PROP_CACHE_TTL, String.valueOf(TTL_SECONDS));
        merge(props);
    }

}
