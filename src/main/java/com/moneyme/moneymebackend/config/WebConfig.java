package com.moneyme.moneymebackend.config;

import com.moneyme.moneymebackend.annotation.BearerTokenArgumentResolver;
import com.moneyme.moneymebackend.annotation.RequestTimeArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final BearerTokenArgumentResolver bearerTokenArgumentResolver;
    private final RequestTimeArgumentResolver requestTimeArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requestTimeArgumentResolver);
        resolvers.add(bearerTokenArgumentResolver);
    }
}