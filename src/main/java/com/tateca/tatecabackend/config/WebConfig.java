package com.tateca.tatecabackend.config;

import com.tateca.tatecabackend.annotation.UIdArgumentResolver;
import com.tateca.tatecabackend.annotation.RequestTimeArgumentResolver;
import com.tateca.tatecabackend.interceptor.BearerTokenInterceptor;
import com.tateca.tatecabackend.interceptor.LoggingInterceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final UIdArgumentResolver UIdArgumentResolver;
    private final RequestTimeArgumentResolver requestTimeArgumentResolver;
    private final BearerTokenInterceptor bearerTokenInterceptor;
    private final LoggingInterceptor loggingInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requestTimeArgumentResolver);
        resolvers.add(UIdArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .order(1);

        registry.addInterceptor(bearerTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/public/**", "/error")
                .order(2);
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> contentCachingFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
                ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
                try {
                    filterChain.doFilter(wrappedRequest, wrappedResponse);
                } finally {
                    wrappedResponse.copyBodyToResponse();
                }
            }
        };
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}