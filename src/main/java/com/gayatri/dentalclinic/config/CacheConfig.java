package com.gayatri.dentalclinic.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppCacheProperties.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(AppCacheProperties cacheProperties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(false);
        registerCache(cacheManager, CacheNames.DENTISTS, cacheProperties.getDentists());
        registerCache(cacheManager, CacheNames.DENTIST_BY_ID, cacheProperties.getDentistById());
        registerCache(cacheManager, CacheNames.PATIENTS, cacheProperties.getPatients());
        registerCache(cacheManager, CacheNames.PATIENT_BY_ID, cacheProperties.getPatientById());
        return cacheManager;
    }

    private void registerCache(
            CaffeineCacheManager cacheManager,
            String cacheName,
            AppCacheProperties.CacheSpec cacheSpec
    ) {
        cacheManager.registerCustomCache(
                cacheName,
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheSpec.getTtl())
                        .maximumSize(cacheSpec.getMaximumSize())
                        .recordStats()
                        .build()
        );
    }
}
