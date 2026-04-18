package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalNotificationHttpConfig {

    @Bean(name = "externalNotificationRestClient")
    public RestClient externalNotificationRestClient(
            @Value("${app.external.notification.base-url:http://localhost:8080}") String baseUrl
    ) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
