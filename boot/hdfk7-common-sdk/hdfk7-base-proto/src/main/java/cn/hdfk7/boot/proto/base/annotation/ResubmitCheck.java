package cn.hdfk7.boot.proto.base.annotation;

import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface ResubmitCheck {
    RequestMethod[] methods() default {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE};
}

