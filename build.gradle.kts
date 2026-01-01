plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotbugs)
    checkstyle
    jacoco
}

group = "com.tateca"
version = "0.5.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters (using bundle)
    implementation(libs.bundles.spring.boot.starters)

    // Database
    implementation(libs.bundles.flyway)
    implementation(libs.mysql.connector.j)

    // Firebase
    implementation(libs.firebase.admin)

    // Resilience
    implementation(libs.resilience4j.spring.boot3)

    // Documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Development
    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.dotenv)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Testing
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.bundles.wiremock)
    testImplementation(libs.bundles.rest.assured)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

// ===========================
// Test Configuration
// ===========================

tasks.test {
    useJUnitPlatform()

    // Disable parallel execution to avoid connection pool issues with Testcontainers
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")

    // Only generate coverage reports in CI environment
    if (System.getenv("CI") == "true") {
        finalizedBy(tasks.jacocoTestReport)
    }

    // Set test environment variables
    environment("FIREBASE_SERVICE_ACCOUNT_KEY", "mock-service-account-key")
    environment("FIREBASE_PROJECT_ID", "test-project-id")
    environment("EXCHANGE_RATE_API_KEY", "test-exchange-rate-api-key")
}

// ===========================
// JaCoCo Code Coverage
// ===========================

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/controller/**",
                    "**/dto/**",
                    "**/entity/**",
                    "**/config/**",
                    "**/TatecaBackendApplication.class",
                    "**/constants/**",
                    "**/model/**",
                    "**/repository/**",
                    "**/accessor/**",
                    "**/interceptor/**",
                    "**/annotation/**",
                    "**/exception/**",
                    "**/util/**",
                    "**/scheduler/**",
                    "**/security/**"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    // Enable verification based on CI environment variable
    // To enable: export JACOCO_VERIFICATION_ENABLED=true
    enabled = System.getenv("JACOCO_VERIFICATION_ENABLED") == "true"

    violationRules {
        rule {
            element = "CLASS"
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                // Start with 50% to get baseline, increase gradually
                minimum = "0.50".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                // Start with 50% to get baseline, increase gradually
                minimum = "0.50".toBigDecimal()
            }
        }
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/controller/**",
                    "**/dto/**",
                    "**/entity/**",
                    "**/config/**",
                    "**/TatecaBackendApplication.class",
                    "**/constants/**",
                    "**/model/**",
                    "**/repository/**",
                    "**/accessor/**",
                    "**/interceptor/**",
                    "**/annotation/**",
                    "**/exception/**",
                    "**/util/**",
                    "**/scheduler/**",
                    "**/security/**"
                )
            }
        })
    )
}

// ===========================
// Checkstyle Configuration
// ===========================

checkstyle {
    toolVersion = "10.21.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = true // Warning-only mode for gradual adoption
    maxWarnings = Integer.MAX_VALUE // Report all warnings
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// ===========================
// SpotBugs Configuration
// ===========================

spotbugs {
    toolVersion = "4.8.6"
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    excludeFilter = file("config/spotbugs/spotbugs-exclude.xml")
    ignoreFailures = true // Warning-only mode for gradual adoption
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") {
        required.set(true)
        outputLocation.set(layout.buildDirectory.file("reports/spotbugs/${name}.html"))
        setStylesheet("fancy-hist.xsl")
    }
    reports.create("xml") {
        required.set(true)
        outputLocation.set(layout.buildDirectory.file("reports/spotbugs/${name}.xml"))
    }
}

// ===========================
// Build Optimizations
// ===========================

tasks.withType<JavaCompile> {
    options.isIncremental = true
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("-Xmx1g")
}

// ===========================
// Quality Gate Task
// ===========================

// Custom task to run all quality checks
tasks.register("qualityGate") {
    group = "verification"
    description = "Runs all quality checks (tests, coverage, checkstyle, spotbugs)"
    dependsOn(
        tasks.test,
        tasks.jacocoTestReport,
        tasks.jacocoTestCoverageVerification,
        tasks.checkstyleMain,
        tasks.checkstyleTest,
        tasks.spotbugsMain,
        tasks.spotbugsTest
    )
}

// Make 'check' task include all quality checks
tasks.check {
    dependsOn(
        tasks.checkstyleMain,
        tasks.checkstyleTest,
        tasks.spotbugsMain,
        tasks.spotbugsTest
    )
}
