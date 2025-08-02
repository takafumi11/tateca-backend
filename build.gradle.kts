plugins {
	java
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.4"
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
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	implementation ("com.google.firebase:firebase-admin:9.2.0")
	implementation ("io.github.resilience4j:resilience4j-retry:1.7.0")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	implementation("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
//	useJUnitPlatform()
}

// Gradle optimization for faster builds
tasks.withType<JavaCompile> {
	options.isIncremental = true
	options.isFork = true
	options.forkOptions.jvmArgs = listOf("-Xmx1g")
}
