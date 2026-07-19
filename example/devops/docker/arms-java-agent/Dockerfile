FROM busybox:stable

WORKDIR /app

COPY aliyun-java-agent-v5.1.1.jar /app/agent/aliyun-java-agent.jar
COPY entrypoint.sh /app/docker/entrypoint.sh

RUN chmod +x /app/docker/entrypoint.sh

ENTRYPOINT ["/app/docker/entrypoint.sh"]
