package com.agentskills.sharing.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration with named caches for GitHub API responses.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES));
        manager.setCacheNames(java.util.List.of(
                "githubRepoContents",
                "githubFileContent",
                "githubRepoInfo"
        ));
        // Allow dynamic cache creation for other @Cacheable usages
        manager.setAllowNullValues(false);
        return manager;
    }
}
