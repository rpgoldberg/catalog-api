package com.catalogcollector.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderConfig {

    @Bean
    public RestClient providerRestClient() {
        return RestClient.create();
    }
}
