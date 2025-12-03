**前言**

众所周知，在项目搭建初期，会耗费大量时间在整合jar包、配置上面，即便有了springboot，由于各个版本之间的依赖、兼容关系，也是十分的麻烦。

所以本人在此整合了一份基于springboot 3.3.13搭建的parent项目。

包含了nacos注册/配置中心、mybatisplus、mybatisplus代码生成器、redisson、雪花算法id生成器、日志切面、防重复提交切面、kafka消息工具、jackson工具、Hutool工具、openfeign、knife4j、skywalking、mapstruct、统一返回结果、统一异常处理，另外还有两个空白项目，分别是
gateway和普通的web项目，主要是方便以后做新项目时可以直接把整个parent换一下名字就能进行开发。

**模块**

* hdfk7-boot-starter-parent 项目父包
* hdfk7-common-sdk 项目间共享的文件
* hdfk7-boot-starter-discovery 服务发现配置
* hdfk7-boot-starter-common 整合2、3和其它一些配置
* hdfk7-code-generator 代码生成器
* hdfk7-gateway 空白服务网关
* hdfk7-module 空白模块

以上1、2、3、4、5模块都已经发布在maven中央仓库，可以尽情享受开源的快乐。

**项目地址**

GitHub - https://github.com/hdfk7/hdfk7-boot

**依赖版本**

* parent=>3.0.3
* common=>3.0.3
* discovery=>3.0.3
* common-sdk=>3.0.3
* generator=>3.0.3

**说明**

上述版本都是在skywalking9.5版本上适配的。

gateway特别配置，需要手动配置spring-cloud-starter-gateway版本为4.1.1及以下，不然skywalking不生效。
