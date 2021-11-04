docker-build-plugin
===
docker 镜像构建插件，对历史镜像数量控制，支持构建后直接更新 kubernetes 镜像。

## 当前版本
v0.0.1

## 执行命令参数样例
```shell
java -Dspring.config.location=/usr/local/docker-build-plugin/application.yml \  # 指定 jar 读取的配置文件位置
  -jar /usr/local/docker-build-plugin/app.jar \   # jar 包所在位置
  --docker-file=/doc/Dockerfile/manyi-api-dev.dockerfile \    # Dockerfile 位置,需要使用绝对路径，也可配合 jenkins 环境变量
  --history-count=1 \   # 历史镜像数量
  --after-script="kubectl set image deployment/manyi-api-dev manyi-api-dev={imageName} -n app-dev"  # 构建成功后所要执行的脚本，支持变量：{imageName}，为最新镜像名称
```

## 配置文件参数样例
```yml
app:
  docker-api-domain: http://127.0.0.1:8072    # docker api 地址，这里暂时仅支持 docker-api
  docker-image-version-regex: "(?<=image-version=).*(?=(\r)?\n)"  # 将从 --docker-file 启动参数中指定的文件内通过正则匹配 镜像名称
  docker-image-name-regex: "(?<=image-name=).*(?=(\r)?\n)"  # 将从 --docker-file 启动参数中指定的文件内通过正则匹配 镜像版本号，如果不填写版本号则默认使用时间戳递增
```

## Dockerfile 样例
```dockerfile
# 这里配置的 image-name 注释将被插件读取
# image-name=manyi-api-dev
FROM java:8
ADD ./manyi-api/target/manyi-api-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Xmx64m","-jar","/app.jar","--server.port=8080"]
```