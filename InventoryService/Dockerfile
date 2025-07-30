# Этап сборки (build)
FROM gradle:8.4-jdk21-alpine AS builder

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем файлы Gradle-конфигурации и wrapper
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./

# Предзагрузка зависимостей (опционально, улучшает кэширование)
RUN ./gradlew dependencies --no-daemon || true

# Копируем остальной исходный код проекта
COPY . .

# Собираем jar-файл, исключая тесты
RUN ./gradlew clean build -x test --no-daemon

# Этап выполнения (runtime)
FROM eclipse-temurin:21-jre-jammy

# Создаем рабочую директорию
WORKDIR /app

# Копируем собранный jar-файл из builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Открываем порт приложения
EXPOSE 8080

# Запускаем Spring Boot приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
