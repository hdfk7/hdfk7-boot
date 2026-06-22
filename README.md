# hdfk7-boot

`hdfk7-boot` 是一套基于 Spring Boot 4.1.0 和 Spring Cloud 2025.1.2 的微服务项目基础框架，封装常用依赖、公共模型、自动配置和示例工程，减少新项目初始化时的重复整合工作。

## 功能

- Nacos 注册中心、配置中心
- OpenFeign、LoadBalancer、Gateway 相关配置
- MyBatis-Plus、MyBatis-Plus Generator
- ShardingSphere JDBC 运行时依赖聚合
- Redisson、Redis、Kafka、RabbitMQ
- Sentinel WebMVC / Gateway 限流处理
- 统一返回结果、统一异常处理
- 雪花算法 ID 生成器
- 日志切面、防重复提交切面
- Hutool、Jackson、MapStruct、Swagger / OpenAPI
- Gateway 和普通 Web 服务示例工程

## 模块

| 模块 | 说明 |
| --- | --- |
| `hdfk7-boot-parent` | 父 POM，统一依赖版本、插件版本和构建配置 |
| `hdfk7-boot-proto` | 公共协议与模型聚合模块 |
| `hdfk7-boot-base-proto` | 公共数据模型、注解、异常、统一返回结果 |
| `hdfk7-boot-starter-common` | 通用自动配置与公共组件 |
| `hdfk7-boot-starter-discovery` | 服务发现、配置中心、OpenFeign、网关和 OpenAPI 聚合配置 |
| `hdfk7-boot-starter-code-generator` | 基于 MyBatis-Plus Generator 的代码生成依赖聚合，推荐仅在测试/开发阶段使用 |
| `hdfk7-boot-starter-shardingsphere` | ShardingSphere JDBC 运行时依赖聚合与 DataSource 自动配置 |
| `hdfk7-gateway` | 网关示例工程 |
| `hdfk7-module` | 普通 Web 服务示例工程 |

## 版本

当前版本：

```text
4.0.0-SNAPSHOT
```

适配版本：

| 依赖 | 版本 |
| --- | --- |
| Spring Boot | `4.1.0` |
| Spring Cloud | `2025.1.2` |
| Spring Cloud Alibaba | `2025.1.0.0` |
| Java | `21` |

## 使用方式

业务工程建议继承 `hdfk7-boot-parent`：

```xml
<parent>
    <groupId>cn.hdfk7</groupId>
    <artifactId>hdfk7-boot-parent</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</parent>
```

普通 Web 服务可按需引入：

```xml
<dependency>
    <groupId>cn.hdfk7</groupId>
    <artifactId>hdfk7-boot-starter-common</artifactId>
</dependency>

<dependency>
    <groupId>cn.hdfk7</groupId>
    <artifactId>hdfk7-boot-starter-discovery</artifactId>
</dependency>
```

代码生成器 starter 仅聚合 MyBatis-Plus Generator、Freemarker 和数据库驱动等依赖，不提供自动配置。推荐在业务工程中使用 `test` scope 引入，并按项目实际数据库、包名、表名编写生成入口：

```xml
<dependency>
    <groupId>cn.hdfk7</groupId>
    <artifactId>hdfk7-boot-starter-code-generator</artifactId>
    <scope>test</scope>
</dependency>
```

示例入口可参考 `example/hdfk7-module/src/test/java/cn/hdfk7/app/module/CodeGenerator.java`。

ShardingSphere starter 聚合 JDBC、分片、MySQL SQL Parser、Hikari 数据源池、Standalone Memory Repository 和 Simple Authority 等运行时依赖，并在存在 `spring.shardingsphere.raw` 配置时自动创建 `DataSource`。业务工程可以用它替换分散声明的 ShardingSphere 依赖：

```xml
<dependency>
    <groupId>cn.hdfk7</groupId>
    <artifactId>hdfk7-boot-starter-shardingsphere</artifactId>
</dependency>
```

内置半年分表算法：

```yaml
algorithmClassName: cn.hdfk7.boot.starter.shardingsphere.algorithm.HalfYearRangeShardingAlgorithm
```

## 示例工程

- `example/hdfk7-gateway`：网关服务示例
- `example/hdfk7-module`：普通 Web 服务示例，包含代码生成器测试入口

## 项目地址

GitHub: https://github.com/hdfk7/hdfk7-boot
