package cn.hdfk7.boot.starter.common.component;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(value = {BCryptPasswordEncoder.class})
public class SecurityComponent {

    @Bean
    @ConditionalOnMissingBean(value = {BCryptPasswordEncoder.class})
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
