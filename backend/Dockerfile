# =============================================
# Backend Dockerfile - 生产环境
# Spring Boot + JDK 21
# 多阶段构建，优化镜像大小
# =============================================

# 阶段 1: 构建
FROM eclipse-temurin:21-jdk-alpine AS builder

# 安装 Maven
RUN apk add --no-cache maven

WORKDIR /build

# 复制 Maven 配置
COPY pom.xml .

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建 JAR
RUN mvn clean package -DskipTests -B -q \
    && mkdir -p target/dependency \
    && cd target/dependency \
    && jar -xf ../*.jar

# 阶段 2: 运行
FROM eclipse-temurin:21-jre-alpine AS production

# 安装必要的工具
RUN apk add --no-cache \
    curl \
    ca-certificates \
    tzdata \
    && rm -rf /var/cache/apk/*

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建非 root 用户
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /build/target/dependency/BOOT-INF/lib /app/lib
COPY --from=builder /build/target/dependency/META-INF /app/META-INF
COPY --from=builder /build/target/dependency/BOOT-INF/classes /app

# 设置权限
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-cp", \
    "app:app/lib/*", \
    "com.usermanagement.Application"]
