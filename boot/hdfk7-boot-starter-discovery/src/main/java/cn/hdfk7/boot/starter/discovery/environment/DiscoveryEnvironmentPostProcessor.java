package cn.hdfk7.boot.starter.discovery.environment;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

public class DiscoveryEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = DiscoveryEnvironmentPostProcessor.class.getName();
    private static final String WEBMVC_SCALAR_CONFIGURATION = "org.springdoc.webmvc.scalar.ScalarConfiguration";

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("spring.cloud.loadbalancer.cache.enabled", false);
        if (supportsWebMvcScalar(environment, application)) {
            defaults.put("springdoc.api-docs.enabled", true);
            defaults.put("scalar.enabled", true);
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    private boolean supportsWebMvcScalar(ConfigurableEnvironment environment, SpringApplication application) {
        if (!ClassUtils.isPresent(WEBMVC_SCALAR_CONFIGURATION, application.getClassLoader())) {
            return false;
        }

        BindResult<WebApplicationType> configuredType = Binder.get(environment)
                .bind("spring.main.web-application-type", WebApplicationType.class);
        WebApplicationType webApplicationType = configuredType.isBound()
                ? configuredType.get()
                : application.getWebApplicationType();
        return webApplicationType == WebApplicationType.SERVLET;
    }
}
