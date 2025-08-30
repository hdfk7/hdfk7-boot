package cn.hdfk7.boot.starter.common.component;

import cn.hdfk7.boot.proto.base.json.JacksonInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnClass(value = {HttpMessageConverters.class, HttpMessageConverter.class})
public class HttpMessageComponent {

    @Bean
    @ConditionalOnMissingBean(value = {HttpMessageConverters.class})
    public HttpMessageConverters httpMessageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
        List<HttpMessageConverter<?>> list = converters.orderedStream().collect(Collectors.toList());
        list.removeIf(o -> o instanceof MappingJackson2HttpMessageConverter);
        list.add(new MappingJackson2HttpMessageConverter(JacksonInstance.getMapper()));
        return new HttpMessageConverters(list);
    }

}
