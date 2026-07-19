FROM registry.cn-chengdu.aliyuncs.com/hd-app/prod:open-jdk-25

WORKDIR /app

COPY docker/entrypoint.sh /app/docker/entrypoint.sh
COPY target/*.jar /app/

RUN chmod +x /app/docker/entrypoint.sh

ENTRYPOINT ["/app/docker/entrypoint.sh"]
