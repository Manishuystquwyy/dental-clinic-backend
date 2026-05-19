package com.gayatri.dentalclinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    private final CacheSpec dentists = new CacheSpec(Duration.ofMinutes(10), 100);
    private final CacheSpec dentistById = new CacheSpec(Duration.ofMinutes(10), 500);
    private final CacheSpec patients = new CacheSpec(Duration.ofMinutes(5), 100);
    private final CacheSpec patientById = new CacheSpec(Duration.ofMinutes(5), 1_000);

    public CacheSpec getDentists() {
        return dentists;
    }

    public CacheSpec getDentistById() {
        return dentistById;
    }

    public CacheSpec getPatients() {
        return patients;
    }

    public CacheSpec getPatientById() {
        return patientById;
    }

    public static class CacheSpec {
        private Duration ttl;
        private long maximumSize;

        public CacheSpec(Duration ttl, long maximumSize) {
            this.ttl = ttl;
            this.maximumSize = maximumSize;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }
    }
}
