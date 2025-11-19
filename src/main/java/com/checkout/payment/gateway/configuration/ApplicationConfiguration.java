package com.checkout.payment.gateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public RestTemplate restTemplate(
      RestTemplateBuilder builder,
      @Value("${bank.simulator.connect-timeout-ms:5000}") int connectTimeout,
      @Value("${bank.simulator.read-timeout-ms:5000}") int readTimeout) {
    return builder
        .setConnectTimeout(Duration.ofMillis(connectTimeout))
        .setReadTimeout(Duration.ofMillis(readTimeout))
        .build();
  }
}