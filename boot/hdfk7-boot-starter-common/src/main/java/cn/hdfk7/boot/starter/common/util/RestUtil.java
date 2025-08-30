package cn.hdfk7.boot.starter.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class RestUtil {
    private static volatile RestTemplate restTemplate;

    private static RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            synchronized (RestUtil.class) {
                if (restTemplate == null) {
                    SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
                    clientHttpRequestFactory.setConnectTimeout(60 * 1000);
                    clientHttpRequestFactory.setReadTimeout(60 * 1000);
                    restTemplate = new RestTemplate(clientHttpRequestFactory);
                }
            }
        }
        return restTemplate;
    }

    public static <T> T get(String url, Class<T> t, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(null, h);
        try {
            return getRestTemplate().exchange(url, HttpMethod.GET, httpEntity, t).getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T get(String url, Class<T> t) {
        return get(url, t, null);
    }

    public static <T> T post(String url, Class<T> t, LinkedMultiValueMap<String, ?> o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        try {
            return getRestTemplate().postForObject(url, httpEntity, t);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T post(String url, Class<T> t, Object o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        try {
            return getRestTemplate().postForObject(url, httpEntity, t);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T post(String url, Class<T> t, LinkedMultiValueMap<String, ?> o) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return post(url, t, o, h);
    }

    public static <T> T postJson(String url, Class<T> t, Object o, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return post(url, t, o, headers);
    }

    public static <T> T postJson(String url, Class<T> t, Object o) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return postJson(url, t, o, h);
    }

    public static <T> T put(String url, Class<T> t, Object o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        try {
            return getRestTemplate().exchange(url, HttpMethod.PUT, httpEntity, t).getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T put(String url, Class<T> t, LinkedMultiValueMap<String, ?> o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        try {
            return getRestTemplate().exchange(url, HttpMethod.PUT, httpEntity, t).getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T put(String url, Class<T> t, LinkedMultiValueMap<String, ?> o) {
        return put(url, t, o, null);
    }

    public static <T> T putJson(String url, Class<T> t, Object o, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return put(url, t, o, headers);
    }

    public static <T> T putJson(String url, Class<T> t, Object o) {
        return putJson(url, t, o, null);
    }

    public static <T> T delete(String url, Class<T> t, Object o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        try {
            return getRestTemplate().exchange(url, HttpMethod.DELETE, httpEntity, t).getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T delete(String url, Class<T> t, LinkedMultiValueMap<String, ?> o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        try {
            return getRestTemplate().exchange(url, HttpMethod.DELETE, httpEntity, t).getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T delete(String url, Class<T> t, LinkedMultiValueMap<String, ?> o) {
        return delete(url, t, o, null);
    }

    public static <T> T deleteJson(String url, Class<T> t, Object o, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return delete(url, t, o, headers);
    }

    public static <T> T deleteJson(String url, Class<T> t, Object o) {
        return deleteJson(url, t, o, null);
    }
}
