# 基于 Spring Boot 4 和 Spring Cloud 2025 的微服务基础框架 hdfk7-boot

在微服务项目落地时，最耗时间的往往不是业务代码，而是基础能力的重复整合：父 POM 版本管理、注册中心、配置中心、网关、OpenFeign、统一返回、异常处理、日志切面、限流降级、分布式 ID、MyBatis-Plus、接口文档等。

`hdfk7-boot` 的目标就是把这些通用能力沉淀成一套可复用的 Spring Boot 微服务基础框架，让新项目可以更快启动，业务模块只关注自己的领域逻辑。

## 一、项目定位

`hdfk7-boot` 是一套基于 `Spring Boot 4.0.7`、`Spring Cloud 2025.1.1`、`Spring Cloud Alibaba 2025.1.0.0` 和 `Java 21` 构建的微服务项目脚手架与 starter 集合。

它不是单一业务系统，而是一个基础框架工程，主要解决三类问题：

1. 统一项目依赖版本和构建规范；
2. 封装微服务常用基础能力；
3. 提供普通 Web 服务和 Gateway 网关示例。

项目整体采用 Maven 多模块结构，核心模块放在 `boot` 目录下，示例工程放在 `example` 目录下。

## 二、技术栈

项目主要技术栈如下：

| 类型 | 技术 |
| --- | --- |
| 基础框架 | Spring Boot 4 |
| 微服务体系 | Spring Cloud 2025 |
| 服务注册与配置 | Nacos |
| 网关 | Spring Cloud Gateway WebFlux |
| 服务调用 | OpenFeign、LoadBalancer |
| 限流降级 | Sentinel |
| 数据访问 | MyBatis-Plus |
| 缓存与分布式能力 | Redis、Redisson |
| 消息队列 | Kafka、RabbitMQ |
| 接口文档 | Springdoc OpenAPI、Scalar |
| 对象转换 | MapStruct |
| 工具库 | Hutool |
| 构建工具 | Maven |
| JDK | Java 21 |

## 三、模块设计

项目的模块划分比较清晰：

| 模块 | 作用 |
| --- | --- |
| `hdfk7-boot-parent` | 父 POM，统一依赖版本、插件版本和构建配置 |
| `hdfk7-boot-proto` | 公共协议与模型聚合模块 |
| `hdfk7-boot-base-proto` | 公共模型、注解、异常、统一返回结果 |
| `hdfk7-boot-starter-common` | 通用自动配置和公共组件 |
| `hdfk7-boot-starter-discovery` | 注册中心、配置中心、OpenFeign、网关、OpenAPI 聚合配置 |
| `hdfk7-boot-starter-code-generator` | MyBatis-Plus Generator 代码生成依赖聚合 |
| `hdfk7-gateway` | 网关示例工程 |
| `hdfk7-module` | 普通 Web 服务示例工程 |

这种结构的好处是：基础能力和业务示例解耦，真正业务项目可以只继承 parent，再按需引入 starter。

## 四、父 POM 统一版本管理

`hdfk7-boot-parent` 是整个框架的基础。它统一管理 Spring Boot、Spring Cloud、Spring Cloud Alibaba、MyBatis-Plus、Redisson、Hutool、MapStruct、Springdoc 等依赖版本。

业务工程只需要继承父 POM：

```xml
<parent>
    <groupId>cn.hdfk7</groupId>
    <artifactId>hdfk7-boot-parent</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</parent>
```

这样可以避免每个业务模块重复声明版本，也降低了依赖冲突风险。

父 POM 还统一配置了 Maven 编译插件、资源插件、测试插件、Spring Boot 打包插件和 flatten 插件，对多模块发布和版本管理比较友好。

## 五、统一返回和异常模型

`hdfk7-boot-base-proto` 提供了统一返回对象 `Result<T>` 和结果码 `ResultCode`。

统一返回结果的价值在于让所有接口保持一致的响应结构，例如：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

同时，项目中定义了基础异常体系，比如：

- `BaseException`
- `UnauthorizedException`
- `TokenInvalidException`
- `ResubmitException`
- `RemoteCallException`
- `ServiceDowngradeException`

业务代码可以通过抛出标准异常，让全局异常处理器统一转换为接口响应，避免每个 Controller 手写 try-catch。

普通 Web 服务中可以继承 `AbstractGlobalExceptionHandler`，网关服务中可以继承 `AbstractGatewayExceptionHandler`，分别适配 Spring MVC 和 WebFlux Gateway 场景。

## 六、starter-common：通用基础能力封装

`hdfk7-boot-starter-common` 是项目里最核心的通用 starter，主要提供以下自动配置：

```text
BootStarterCommonAutoConfiguration
MybatisPlusAutoConfiguration
SnowflakeIdGeneratorAutoConfiguration
IdGeneratorAutoConfiguration
ClientIpResolverAutoConfiguration
KafkaMessageSenderAutoConfiguration
RabbitMessageSenderAutoConfiguration
ValidatorAutoConfiguration
XxlJobAutoConfiguration
SentinelGatewayBlockRequestHandlerAutoConfiguration
SentinelMvcBlockExceptionHandlerAutoConfiguration
```

它封装的能力包括：

1. MyBatis-Plus 分页插件配置；
2. Redis 辅助的雪花算法 ID 生成器；
3. 客户端 IP 解析；
4. Kafka 消息发送工具；
5. RabbitMQ 消息发送工具；
6. 参数校验配置；
7. XXL-JOB 配置；
8. Sentinel MVC 限流异常处理；
9. Sentinel Gateway 限流异常处理；
10. 日志切面和防重复提交切面抽象类。

其中防重复提交能力基于 Redisson 分布式锁实现，业务模块只需要继承抽象切面并声明切点，就可以复用底层锁逻辑。

## 七、starter-discovery：微服务发现与网关增强

`hdfk7-boot-starter-discovery` 主要面向微服务治理场景，集成了：

- Nacos Config
- Nacos Discovery
- OpenFeign
- LoadBalancer
- Gateway
- Scalar / OpenAPI 文档聚合
- Nacos 服务监听
- 网关路由刷新

它的自动配置包括：

```text
BootStarterDiscoveryAutoConfiguration
NacosServiceLookupAutoConfiguration
RestTemplateAutoConfiguration
ScalarAutoConfiguration
OpenApiDocsForwardedHeaderFilterAutoConfiguration
GatewayLoadBalancerEventListenerAutoConfiguration
LoadBalancerEventListenerAutoConfiguration
```

比较有价值的一点是：框架通过监听 Nacos 服务变化事件，在服务上下线时刷新网关路由缓存，使 Gateway 能更及时地感知后端服务变化。

同时，`ScalarAutoConfiguration` 支持从服务发现中聚合接口文档，让网关可以作为统一 API 文档入口。

网关示例中只需要开启配置：

```yaml
scalar:
  discovery:
    enabled: true
```

即可启用服务发现模式下的文档聚合能力。

## 八、普通服务示例

普通 Web 服务示例位于：

```text
example/hdfk7-module
```

它引入了：

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

启动类中启用了 MyBatis Mapper 扫描和 Feign：

```java
@MapperScan("cn.hdfk7.app.module.infrastructure.mapper")
@EnableFeignClients
@SpringBootApplication
public class ModuleApplication {
}
```

配置文件中接入 Nacos：

```yaml
spring:
  application:
    name: module
  cloud:
    nacos:
      discovery:
        server-addr: ip:8848
      config:
        server-addr: ip:8848
        file-extension: yaml
  config:
    import:
      - nacos:${spring.application.name}?refreshEnabled=true
```

这说明业务服务可以同时获得配置中心、注册中心、OpenFeign、统一异常、MyBatis-Plus、接口文档等基础能力。

## 九、网关示例

网关示例位于：

```text
example/hdfk7-gateway
```

它引入了 Gateway、Sentinel Gateway、Actuator、OpenTelemetry、Scalar WebFlux 等依赖。

核心配置同样接入 Nacos：

```yaml
spring:
  application:
    name: gateway
  cloud:
    nacos:
      discovery:
        server-addr: ip:8848
      config:
        server-addr: ip:8848
        file-extension: yaml
  config:
    import:
      - nacos:${spring.application.name}?refreshEnabled=true
springdoc:
  api-docs:
    enabled: true
scalar:
  discovery:
    enabled: true
```

网关侧可以通过继承 `AbstractGatewayFilter` 实现统一过滤逻辑，也可以通过继承 `AbstractGatewayExceptionHandler` 实现 WebFlux 网关异常统一输出。

## 十、代码生成器

`hdfk7-boot-starter-code-generator` 是一个轻量级代码生成依赖聚合模块，主要聚合：

- MyBatis-Plus Generator
- MyBatis-Plus Extension
- Freemarker
- MySQL Driver

它不提供自动配置，更适合在开发或测试阶段使用：

```xml
<dependency>
    <groupId>cn.hdfk7</groupId>
    <artifactId>hdfk7-boot-starter-code-generator</artifactId>
    <scope>test</scope>
</dependency>
```

示例入口在：

```text
example/hdfk7-module/src/test/java/cn/hdfk7/app/module/CodeGenerator.java
```

这种设计比较合理，因为代码生成器通常只在开发期使用，不应该污染线上运行时依赖。

## 十一、项目亮点

我认为这个项目有几个值得借鉴的设计点。

第一，基础能力 starter 化。

通用能力放进 starter，业务服务按需引入，避免复制粘贴配置和工具类。

第二，协议模型独立。

统一返回、分页模型、基础异常、注解等放在 `base-proto` 中，便于多个服务共享。

第三，普通服务和网关分别适配。

Spring MVC 和 Gateway WebFlux 的异常处理模型不同，项目分别提供了 `AbstractGlobalExceptionHandler` 和 `AbstractGatewayExceptionHandler`，没有强行用一套逻辑覆盖所有场景。

第四，服务发现和文档聚合结合。

通过 Nacos 发现服务，再配合 Scalar/OpenAPI 聚合文档，网关不仅承担流量入口，也可以作为接口文档入口。

第五，兼容较新的技术版本。

项目基于 Spring Boot 4、Spring Cloud 2025 和 Java 21，适合用来探索新版本微服务项目的基础搭建方式。

## 十二、适用场景

`hdfk7-boot` 比较适合以下场景：

1. 新建 Spring Boot 微服务项目；
2. 多个业务服务需要统一依赖版本；
3. 项目需要 Nacos、Gateway、OpenFeign、Sentinel 等微服务组件；
4. 团队希望沉淀自己的基础 starter；
5. 希望普通服务和网关服务有统一的异常、日志、限流返回格式；
6. 需要一个可参考的 Spring Boot 4 微服务工程模板。

## 十三、总结

`hdfk7-boot` 本质上是一套面向微服务项目的基础框架，它把项目初始化阶段最常见、最重复的基础能力进行了封装，包括父 POM 版本管理、公共模型、统一异常、分布式 ID、MyBatis-Plus、Nacos、OpenFeign、Gateway、Sentinel、接口文档和代码生成器等。

对于业务项目来说，它的价值不是替代业务架构，而是把通用技术底座先搭好，让业务服务可以用更低成本接入微服务体系。

如果你正在搭建基于 Spring Boot 4 和 Spring Cloud 2025 的微服务项目，这个项目可以作为一个不错的参考模板。
