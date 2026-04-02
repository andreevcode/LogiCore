plugins {
    id("logicore.java-conventions")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency)
}

description = "core-logistics"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    implementation("org.springframework.kafka:spring-kafka")
    // 1. Для автоподстановки пропертей в IDEA (Metadata)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")


    runtimeOnly("org.postgresql:postgresql")
    
    testImplementation(libs.bundles.spring.test.infrastructure)
    testImplementation("org.springframework.kafka:spring-kafka-test")

    testRuntimeOnly(libs.junit.platform)
}