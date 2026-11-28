plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.asciidoctor.jvm.convert") version "4.0.5"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val asciidoctorExt: Configuration by configurations.creating

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.redisson:redisson:3.50.0")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("software.amazon.awssdk:s3:2.20.56")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testImplementation("io.rest-assured:spring-mock-mvc:5.5.0")
    testImplementation("org.springframework.restdocs:spring-restdocs-restassured")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

val contractUpdate = gradle.startParameter.projectProperties["contract.update"] ?: "false"
val testForkSlots = 15
val defaultTestForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, testForkSlots / 2)
val testForks = if (contractUpdate.toBoolean()) {
    1
} else {
    providers.gradleProperty("testForks").map { parseTestForks(it) }.getOrElse(defaultTestForks)
}

fun parseTestForks(value: String): Int = requireNotNull(value.toIntOrNull()?.takeIf { it in 1..testForkSlots }) {
    "-PtestForks는 1~$testForkSlots 사이 정수여야 합니다(fork마다 Redis DB 1~$testForkSlots 중 하나를 씁니다): '$value'"
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = testForks
    systemProperty("user.timezone", "Asia/Seoul")
    systemProperty("contract.update", contractUpdate)
    outputs.dir("build/generated-snippets")
    inputs.files(fileTree("mykku-be-config/templates"))
        .withPropertyName("adminTemplates")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    jvmArgs(
        "-XX:+UseParallelGC",
        "-Xmx1g",
        "-Xms512m"
    )

    testLogging {
        events(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    addTestListener(FailedForkClassOrder(logger))
}

class FailedForkClassOrder(private val logger: Logger) : TestListener {

    private val classesByFork = mutableMapOf<String, MutableList<String>>()

    override fun beforeSuite(suite: TestDescriptor) {
        val fork = suite.parent?.name?.takeIf { it.startsWith(FORK_PREFIX) } ?: return
        suite.className?.let { classesByFork.getOrPut(fork) { mutableListOf() }.add(it) }
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        if (suite.name.startsWith(FORK_PREFIX) && result.failedTestCount > 0) {
            val classes = classesByFork[suite.name].orEmpty()
            logger.lifecycle("${suite.name} 실패 ${result.failedTestCount}건, 이 fork의 클래스 순서: $classes")
        }
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {}

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

    private companion object {
        const val FORK_PREFIX = "Gradle Test Executor"
    }
}

tasks.asciidoctor {
    dependsOn(tasks.test)
    configurations(asciidoctorExt.name)
    inputs.dir(layout.buildDirectory.dir("generated-snippets"))
    baseDirFollowsSourceFile()
    sources {
        include("index.adoc")
    }
}

asciidoctorj {
    fatalWarnings(missingIncludes())
    fatalWarnings(Regex("Snippet .+ not found").toPattern())
    fatalWarnings(Regex("No snippets were found for operation").toPattern())
}

tasks.bootJar {
    from(tasks.asciidoctor) {
        into("static/docs")
    }
    from("mykku-be-config/templates") {
        into("templates")
    }
}
