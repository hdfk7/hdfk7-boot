package cn.hdfk7.boot.starter.common.component;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

@Component
@ConditionalOnClass(value = {Validator.class, HibernateValidator.class, AutowireCapableBeanFactory.class})
public class ValidatorComponent {

    @Bean
    @ConditionalOnMissingBean(value = {Validator.class})
    public Validator validator(AutowireCapableBeanFactory springFactory) {
        try (ValidatorFactory factory = Validation.byProvider(HibernateValidator.class)
                .configure()
                .failFast(true)
                .constraintValidatorFactory(new SpringConstraintValidatorFactory(springFactory))
                .buildValidatorFactory()) {
            return factory.getValidator();
        }
    }

}
