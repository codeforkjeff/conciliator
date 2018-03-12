package com.codefork.refine;

import org.apache.commons.logging.LogFactory;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.expiry.Expirations;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableCaching
public class Application {
    public static final String CACHE_DEFAULT = "default";

    /**
     * Needed for jsonp support 
     */
    @ControllerAdvice
    public static class JsonpAdvice extends AbstractJsonpResponseBodyAdvice {
        public JsonpAdvice() {
            super("callback");
        }
    }

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
    }

    @Bean
    public CacheManager cacheManager(@Autowired Config config) {
        long ttl = Long.valueOf(config.getProperties().getProperty(Config.PROP_CACHE_TTL));

        config.getProperties().getProperty(Config.PROP_CACHE_SIZE);

        MemSize memSize = MemSize.valueOf(config.getProperties().getProperty(Config.PROP_CACHE_SIZE));

        LogFactory.getLog(getClass()).info(
                String.format("Initializing cache TTL=%d secs, size=%d %s",
                        ttl, memSize.getSize(), memSize.getUnit().toString()));

        org.ehcache.config.CacheConfiguration<Object, Object> cacheConfiguration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(Object.class, Object.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(memSize.getSize(), memSize.getUnit()))
                .withExpiry(Expirations.timeToLiveExpiration(new org.ehcache.expiry.Duration(ttl, TimeUnit.SECONDS)))
                .build();

        Map<String, CacheConfiguration<?, ?>> caches = new HashMap<>();
        caches.put(CACHE_DEFAULT, cacheConfiguration);

        EhcacheCachingProvider provider = (EhcacheCachingProvider) javax.cache.Caching.getCachingProvider();

        // when our cacheManager bean is re-created several times for
        // diff test configurations, this provider seems to hang on to state
        // causing cache settings to not be right. so we always close().
        provider.close();

        DefaultConfiguration configuration = new DefaultConfiguration(
                caches, provider.getDefaultClassLoader());

        return new JCacheCacheManager(
                provider.getCacheManager(provider.getDefaultURI(), configuration));
    }

    /**
     * set property logging.level.org.springframework.web.filter=DEBUG
     * for logging output
     * @return
     */
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter
                = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }

}