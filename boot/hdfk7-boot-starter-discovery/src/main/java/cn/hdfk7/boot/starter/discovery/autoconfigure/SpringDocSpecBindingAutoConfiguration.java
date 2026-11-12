package cn.hdfk7.boot.starter.discovery.autoconfigure;

import org.springdoc.core.conditions.SpecPropertiesCondition;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration.SpecificationStringPropertiesCustomizerBeanPostProcessor;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;

@AutoConfiguration(after = SpringDocConfiguration.class, before = SpringDocSpecPropertiesConfiguration.class)
@ConditionalOnClass(SpecificationStringPropertiesCustomizerBeanPostProcessor.class)
@ConditionalOnWebApplication
@ConditionalOnBean(SpringDocConfiguration.class)
@Conditional(SpecPropertiesCondition.class)
public class SpringDocSpecBindingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    static SpecificationStringPropertiesCustomizerBeanPostProcessor specificationStringPropertiesCustomizerBeanPostProcessor(@Lazy SpringDocConfigProperties properties) {
        // Defer SpringDoc property binding until the post-processor handles a GroupedOpenApi bean.
        return new SpecificationStringPropertiesCustomizerBeanPostProcessor(properties);
    }
}
