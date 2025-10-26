# Imagem base do Amazon
FROM amazoncorretto:17-alpine

# Diretório de trabalho no contêiner
WORKDIR /app

# JAR da aplicação para o diretório de trabalho
COPY build/libs/sba_server-0.0.1-SNAPSHOT.jar app.jar

# Porta que a aplicação irá utilizar
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-Dspring.devtools.restart.enabled=true", "-jar", "app.jar"]