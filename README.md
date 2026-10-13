# hdfk7-boot

`hdfk7-boot` 是一套基于 Spring Boot 4.1.0、Spring Cloud 2025.1.2、Spring Cloud Alibaba 2025.1.0.0 和 Java 21 的微服务基础框架。项目通过 BOM、父 POM、公共协议模块和多个 starter 统一依赖版本与基础能力，并提供 Spring MVC 服务和 Gateway 示例。

## 功能

- Nacos 注册中心、配置中心
- OpenFeign、LoadBalancer、Gateway 相关配置
- MyBatis-Plus、MyBatis-Plus Generator
- ShardingSphere JDBC 运行时依赖聚合
- Redis、Redisson、Kafka、RabbitMQ 条件自动配置（业务工程需引入对应运行时依赖）
- Sentinel WebMVC / Gateway 限流处理
- 统一返回结果、统一异常处理
- 雪花算法 ID 生成器
- 日志切面、防重复提交切面
- Hutool、Jackson、MapStruct、Springdoc OpenAPI 3.1.0、Scalar
- Gateway 和普通 Web 服务示例工程

## 模块

| 模块 | 说明                                                                      |
| --- |---------------------------------------------------------------------------|
| `hdfk7-boot-dependencies` | BOM，统一管理 Spring Boot、Spring Cloud、第三方库和框架模块版本           |
| `hdfk7-boot-parent` | 父 POM，继承 BOM 并统一 Java 21、源码包、Javadoc 和 flatten 配置          |
| `hdfk7-boot-proto` | 公共协议模块的聚合 POM                                                    |
| `hdfk7-boot-base-proto` | 公共数据模型、注解、异常、统一返回结果                                    |
| `hdfk7-boot-starter-common` | 通用自动配置与公共组件                                                    |
| `hdfk7-boot-starter-discovery` | Nacos Config/Discovery、OpenFeign、LoadBalancer 及网关文档聚合支持        |
| `hdfk7-boot-starter-code-generator` | 基于 MyBatis-Plus Generator 的代码生成依赖聚合，推荐仅在测试/开发阶段使用 |
| `hdfk7-boot-starter-shardingsphere` | ShardingSphere JDBC 运行时依赖聚合与 DataSource 自动配置                  |
| `gateway` | 网关示例工程                                                              |
| `service` | 普通 Web 服务示例工程                                                     |

## 版本

当前版本：

```text
4.0.1-SNAPSHOT
```

适配版本：

| 依赖 | 版本 |
| --- | --- |
| Spring Boot | `4.1.0` |
| Spring Cloud | `2025.1.2` |
| Spring Cloud Alibaba | `2025.1.0.0` |
| Springdoc OpenAPI | `3.1.0` |
| Swagger Annotations | `2.2.53` |
| Redisson | `4.7.0` |
| Java | `21` |

## 使用方式

业务工程建议继承 `hdfk7-boot-parent`：

```xml
<parent>
    <groupId>cn.hdfk7.boot</groupId>
    <artifactId>hdfk7-boot-parent</artifactId>
    <version>4.0.1-SNAPSHOT</version>
</parent>
```

已有父 POM 的工程可以只导入 BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.hdfk7.boot</groupId>
            <artifactId>hdfk7-boot-dependencies</artifactId>
            <version>4.0.1-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

普通 Web 服务可按需引入：

```xml
<dependency>
    <groupId>cn.hdfk7.boot</groupId>
    <artifactId>hdfk7-boot-starter-common</artifactId>
</dependency>

<dependency>
    <groupId>cn.hdfk7.boot</groupId>
    <artifactId>hdfk7-boot-starter-discovery</artifactId>
</dependency>
```

`hdfk7-boot-starter-common` 对 Redis、Redisson、Kafka、RabbitMQ、MyBatis-Plus、Sentinel 和 XXL-JOB 使用 `provided` 依赖实现条件自动配置，不会替业务工程强制引入全部中间件。业务工程使用哪项能力，就需要自行声明对应 starter 或客户端依赖。

`hdfk7-boot-starter-discovery` 直接引入 Nacos Config、Nacos Discovery、OpenFeign 和 LoadBalancer；Gateway 与 Springdoc WebFlux Scalar 为 `provided`，普通服务和网关应按实际 Web 类型显式选择 Springdoc WebMVC/WebFlux 依赖。

## 基础配置

示例工程使用 Spring Boot Config Data 导入 Nacos 配置：

```yaml
spring:
  application:
    name: service
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

当前使用 Springdoc 3.1.0。项目暂时显式生成 OpenAPI 3.0 文档，以规避 Springdoc 3.1.0 在部分 OpenAPI 3.1 Schema 转换路径产生的回退 WARN：

```yaml
springdoc:
  api-docs:
    version: OPENAPI_3_0
```

该配置不影响 `@Schema`、`@Operation` 等常用注解。确认后续版本修复并需要 OpenAPI 3.1 特性时，可切换为 `OPENAPI_3_1`。

网关如需通过服务发现聚合 Scalar 文档，可启用：

```yaml
scalar:
  discovery:
    enabled: true
```

Sentinel Dashboard 默认只把规则推送到客户端内存，服务重启后规则会丢失。本框架提供 WebMVC/Gateway BlockException 返回处理，但不提供 Dashboard 规则持久化；生产环境应使用 Nacos 等动态数据源并采用 Push 模式。

代码生成器 starter 仅聚合 MyBatis-Plus Generator、Freemarker 和数据库驱动等依赖，不提供自动配置。推荐在业务工程中使用 `test` scope 引入，并按项目实际数据库、包名、表名编写生成入口：

```xml
<dependency>
    <groupId>cn.hdfk7.boot</groupId>
    <artifactId>hdfk7-boot-starter-code-generator</artifactId>
    <scope>test</scope>
</dependency>
```

示例入口可参考 `example/service/src/test/java/cn/hdfk7/app/service/CodeGenerator.java`。

ShardingSphere starter 聚合 JDBC、分片、MySQL SQL Parser、Hikari 数据源池、Standalone Memory Repository 和 Simple Authority 等运行时依赖，并在存在 `spring.shardingsphere.raw` 配置时自动创建 `DataSource`。业务工程可以用它替换分散声明的 ShardingSphere 依赖：

```xml
<dependency>
    <groupId>cn.hdfk7.boot</groupId>
    <artifactId>hdfk7-boot-starter-shardingsphere</artifactId>
</dependency>
```

内置半年和按月分表算法：

| 算法 | 表名规则 | 示例 |
| --- | --- | --- |
| `HalfYearRangeShardingAlgorithm` | `逻辑表名_年份_半年序号`，1～6 月为 1，7～12 月为 2 | `example_record_2026_1`、`example_record_2026_2` |
| `MonthRangeShardingAlgorithm` | `逻辑表名_yyyyMM`，月份补齐两位 | `example_record_202601`、`example_record_202610` |

下面是放入 `spring.shardingsphere.raw` 的按月分片规则片段；`dataSources` 需另行配置名为 `ds` 的实际数据源：

```yaml
rules:
  - !SHARDING
    tables:
      example_record:
        actualDataNodes: "ds.example_record_$->{2023..2026}$->{['01','02','03','04','05','06','07','08','09','10','11','12']}"
        tableStrategy:
          standard:
            shardingColumn: partition_time
            shardingAlgorithmName: month-sharding-algorithm
    shardingAlgorithms:
      month-sharding-algorithm:
        type: CLASS_BASED
        props:
          strategy: STANDARD
          algorithmClassName: cn.hdfk7.boot.starter.shardingsphere.algorithm.MonthRangeShardingAlgorithm
```

若使用半年分表，替换节点表达式和算法类名，并保持 `shardingAlgorithmName` 与 `shardingAlgorithms` 下的名称一致：

```yaml
actualDataNodes: ds.example_record_$->{2023..2026}_$->{1..2}
algorithmClassName: cn.hdfk7.boot.starter.shardingsphere.algorithm.HalfYearRangeShardingAlgorithm
```

两个算法支持 `LocalDateTime`、`LocalDate`、`OffsetDateTime`、`ZonedDateTime`、`Instant`、`Date`（含 JDBC `Date` 和 `Timestamp`）以及 `Long` 毫秒时间戳；字符串应在业务入口转换为时间对象。`Instant`、`Date` 和毫秒时间戳按服务器默认时区转换，带时区类型使用自身本地日期时间。

精确分片会校验目标表是否已配置，缺失时抛出包含表名的异常。范围分片筛选已配置的表，支持无界范围、空范围和开闭端点。例如 `< 2026-07-01 00:00:00` 不包含下半年表，按月时 `< 2026-11-01 00:00:00` 不包含 11 月表。开区间下界按输入时间类型的精度判断周期末尾。无法按表名规则解析的候选表会保守保留。

分片依据配置的时间字段。示例中的 example_record 和 partition_time 为虚构名称，接入时需替换为业务逻辑表名和分片字段，并提前创建、配置对应的物理表。

公共逻辑统一放在 `ShardingUtils`：两个算法共用时间转换、范围路由、周期边界和目标表校验，算法类保留各自的表名生成及解析规则。

配置可通过 Nacos 导入，但当前自动配置仅在启动时创建数据源；修改分片规则或节点后，需要重启服务才能应用新配置。

## 示例工程

- `example/gateway`：网关服务示例
- `example/service`：普通 Web 服务示例，包含代码生成器测试入口

## 构建

```bash
mvn -f pom.xml clean install
```

默认版本来自 `.mvn/maven.config` 中的 `revision=4.0.1` 和 `changelist=-SNAPSHOT`。发布或覆盖单个框架模块版本时，可使用 `hdfk7-boot-dependencies` 中对应的模块版本属性。

## 项目地址

GitHub: https://github.com/hdfk7/hdfk7-boot
