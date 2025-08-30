package cn.hdfk7.app.module;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("cn.hdfk7.app.module.infrastructure.mapper")
@EnableFeignClients
@SpringBootApplication
public class ModuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModuleApplication.class, args);
    }
}
