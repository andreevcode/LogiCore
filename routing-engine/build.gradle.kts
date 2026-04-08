plugins {
    id("logicore.java-conventions")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency)
}

description = "routing-engine"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    implementation("org.springframework.kafka:spring-kafka")
    // 1. Для автоподстановки пропертей в IDEA (Metadata)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor(libs.lombok)

    runtimeOnly("org.postgresql:postgresql")
    
    testImplementation(libs.bundles.spring.test.infrastructure)
    testImplementation("org.springframework.kafka:spring-kafka-test")

    testRuntimeOnly(libs.junit.platform)

    // Основной Lombok для приложения
    compileOnly(libs.lombok)
    
    // Подключение Lombok для тестов
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}