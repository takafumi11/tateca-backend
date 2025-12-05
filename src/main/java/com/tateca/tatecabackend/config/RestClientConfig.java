package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.api.client.ExchangeRateHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableScheduling
public class RestClientConfig {

    @Bean
    public RestClient exchangeRateRestClient(
            @Value("${exchange.rate.base-url}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
    }

    @Bean
    public ExchangeRateHttpClient exchangeRateHttpClient(
            RestClient exchangeRateRestClient
    ) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(exchangeRateRestClient))
                .build();
        return factory.createClient(ExchangeRateHttpClient.class);
    }
}
