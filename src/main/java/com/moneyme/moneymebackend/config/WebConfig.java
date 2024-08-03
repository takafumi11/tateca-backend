package com.moneyme.moneymebackend.config;

import com.moneyme.moneymebackend.annotation.UIdArgumentResolver;
import com.moneyme.moneymebackend.annotation.RequestTimeArgumentResolver;
import com.moneyme.moneymebackend.interceptor.BearerTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final UIdArgumentResolver UIdArgumentResolver;
    private final RequestTimeArgumentResolver requestTimeArgumentResolver;
    private final BearerTokenInterceptor bearerTokenInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requestTimeArgumentResolver);
        resolvers.add(UIdArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bearerTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/public/**", "/error"); // 必要に応じて除外パスを追加
    }
}