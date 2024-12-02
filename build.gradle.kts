import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.allopen") version "2.0.21"
  kotlin("plugin.serialization") version "2.1.0"
  id("io.quarkus")
  id("spotless")
}

repositories { mavenCentral() }

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
  implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
  implementation("io.quarkus:quarkus-arc")
  implementation("io.quarkus:quarkus-flyway")
  implementation("io.quarkus:quarkus-jdbc-postgresql")
  implementation("io.quarkus:quarkus-kotlin")
  implementation("io.quarkus:quarkus-rest")
  implementation("io.quarkus:quarkus-rest-jackson")
  implementation("io.quarkus:quarkus-smallrye-openapi")
  implementation("org.flywaydb:flyway-database-postgresql")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  lint("com.facebook:ktfmt:0.46")
  lint("com.google.googlejavaformat:google-java-format:1.24.0")
  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("io.rest-assured:rest-assured")
}

group = "dev.cat6"

version = "1.0.0-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test> { systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager") }

allOpen {
  annotation("jakarta.ws.rs.Path")
  annotation("jakarta.enterprise.context.ApplicationScoped")
  annotation("jakarta.persistence.Entity")
  annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
  compilerOptions.javaParameters.set(true)
}
