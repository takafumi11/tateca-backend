plugins {
	java
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.4"
	jacoco
	id("org.owasp.dependencycheck") version "11.1.1"
}

group = "com.tateca"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-mysql")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	implementation ("com.google.firebase:firebase-admin:9.2.0")
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	implementation("com.mysql:mysql-connector-j")

	// Testing dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:mysql:1.19.3")
	testImplementation("org.testcontainers:junit-jupiter:1.19.3")
	testImplementation("org.wiremock.integrations.testcontainers:wiremock-testcontainers-module:1.0-alpha-13")
	testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	enabled = false
}

// JaCoCo configuration for code coverage
jacoco {
	toolVersion = "0.8.12"
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
					"**/dto/request/**",
					"**/entity/**",
					"**/config/**",
					"**/TatecaBackendApplication.class",
					"**/constants/**",
					"**/model/**",
					"**/repository/**",
					"**/interceptor/**",
					"**/annotation/**",
					"**/exception/**",
					"**/util/**",
					"**/scheduler/**"
				)
			}
		})
	)
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.jacocoTestReport)
	violationRules {
		rule {
			element = "CLASS"
			limit {
				counter = "BRANCH"
				//noinspection SpellCheckingInspection - COVEREDRATIO is a valid JaCoCo API constant
				value = "COVEREDRATIO"
				minimum = "0.95".toBigDecimal()
			}
		}
		rule {
			element = "CLASS"
			limit {
				counter = "LINE"
				//noinspection SpellCheckingInspection - COVEREDRATIO is a valid JaCoCo API constant
				value = "COVEREDRATIO"
				minimum = "0.90".toBigDecimal()
			}
		}
	}
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				exclude(
					"**/dto/request/**",
					"**/entity/**",
					"**/config/**",
					"**/TatecaBackendApplication.class",
					"**/constants/**",
					"**/model/**",
					"**/repository/**",
					"**/interceptor/**",
					"**/annotation/**",
					"**/exception/**",
					"**/util/**",
					"**/scheduler/**"
				)
			}
		})
	)
}

// Gradle optimization for faster builds
tasks.withType<JavaCompile> {
	options.isIncremental = true
	options.isFork = true
	options.forkOptions.jvmArgs = listOf("-Xmx1g")
}

// OWASP Dependency Check configuration
dependencyCheck {
	formats = listOf("HTML", "JSON")
	suppressionFile = "${rootDir}/config/owasp/suppressions.xml"
	failBuildOnCVSS = 7.0f
	analyzers.assemblyEnabled = false
	analyzers.nugetconfEnabled = false
	analyzers.nodeEnabled = false
}
