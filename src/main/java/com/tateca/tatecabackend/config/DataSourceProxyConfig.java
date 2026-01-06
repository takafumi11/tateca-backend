package com.tateca.tatecabackend.config;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for datasource-proxy to log database queries with execution time.
 * <p>
 * Wraps the DataSource bean (HikariCP) with a proxy that logs:
 * - All SQL queries with execution time
 * - Slow queries (>1000ms) at WARN level
 * - Query results and parameters
 * <p>
 * Only active in 'dev' and 'prod' profiles (disabled in 'test' profile).
 */
@Configuration
@Profile({"dev", "prod"})
public class DataSourceProxyConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof net.ttddyy.dsproxy.support.ProxyDataSource)) {
            // Wrap DataSource with datasource-proxy
            ProxyFactory proxyFactory = new ProxyFactory(bean);
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new ProxyDataSourceInterceptor((DataSource) bean));
            return proxyFactory.getProxy();
        }
        return bean;
    }

    private static class ProxyDataSourceInterceptor implements MethodInterceptor {
        private final DataSource dataSource;

        public ProxyDataSourceInterceptor(DataSource dataSource) {
            this.dataSource = ProxyDataSourceBuilder.create(dataSource)
                    .name("tateca-backend-datasource")
                    // Log all queries with execution time
                    .logQueryBySlf4j(SLF4JLogLevel.INFO)
                    // Log slow queries (>1000ms) at WARN level
                    .logSlowQueryBySlf4j(1000, TimeUnit.MILLISECONDS, SLF4JLogLevel.WARN)
                    // Include query parameters in logs
                    .multiline()
                    .countQuery()
                    .build();
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method proxyMethod = ReflectionUtils.findMethod(dataSource.getClass(),
                    invocation.getMethod().getName());
            if (proxyMethod != null) {
                return proxyMethod.invoke(dataSource, invocation.getArguments());
            }
            return invocation.proceed();
        }
    }
}
