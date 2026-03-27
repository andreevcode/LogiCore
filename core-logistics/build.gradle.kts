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
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(libs.bundles.spring.test.infrastructure)

    testRuntimeOnly(libs.junit.platform)
}