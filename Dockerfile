# Imagen del servicio de reservas. Multi-etapa: una etapa compila con Maven y otra solo ejecuta con el JRE.

# ---------- Etapa 1: compilar ----------
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app

# Primero solo el pom.xml: si las dependencias no cambian, Docker reutiliza esta capa.
COPY pom.xml .
RUN mvn -B -q -ntp dependency:go-offline

# Luego el código. Las pruebas se corren aparte con ./mvnw test.
COPY src ./src
RUN mvn -B -q -ntp package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21.0.12.1_1-jre-noble

# Usuario sin privilegios: el proceso no corre como root dentro del contenedor.
RUN groupadd --system --gid 10001 reservas \
 && useradd --system --uid 10001 --gid reservas --no-create-home --shell /usr/sbin/nologin reservas

WORKDIR /app
COPY --from=build /app/target/reservas.jar app.jar
USER 10001:10001

EXPOSE 8080

# Docker y Compose consultan este endpoint para saber si el servicio está listo.
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
