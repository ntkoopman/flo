import groovy.json.JsonSlurper

plugins { id("com.diffplug.spotless") }

val lint: Configuration by configurations.creating

fun resolve(module: String): String =
    lint.resolvedConfiguration.resolvedArtifacts
        .map { it.moduleVersion.id }
        .first { it.module.toString() == module }
        .version

// Read dependencies from package.json so they can be updated by dependabot
@Suppress("UNCHECKED_CAST")
fun packages() = (JsonSlurper().parse(file("package.json")) as Map<*, *>)["dependencies"] as Map<String, String>

spotless {
  isEnforceCheck = System.getenv().containsKey("CI")
  if (plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
    kotlin {
      target("src/main/kotlin/**/*.kt", "src/test/kotlin/**/*.kt")
      ktfmt(resolve("com.facebook:ktfmt")).configure { it.setMaxWidth(120) }
    }
  }
  kotlinGradle {
    target("*.gradle.kts", "buildSrc/*.gradle.kts", "buildSrc/src/**/*.gradle.kts")
    ktfmt(resolve("com.facebook:ktfmt")).configure { it.setMaxWidth(120) }
  }
  if (plugins.hasPlugin("java")) {
    java {
      target("src/main/java/**/*.java", "src/test/java/**/*.java", "buildSrc/src/main/java/**/*.java")
      removeUnusedImports()
      toggleOffOn()
      googleJavaFormat(resolve("com.google.googlejavaformat:google-java-format"))
      formatAnnotations()
    }
  }
  format("misc") {
    target("*.md", "src/**/*.html", "*.json*", "*.yaml", ".github/**/*.yaml", "docs/**/*.md")
    prettier(packages()).config(mapOf("printWidth" to 120))
  }
  format("properties") {
    target("src/**/*.properties")
    endWithNewline()
  }
  sql {
    // flyway calculates a checksum for every migration, so there is a ratchetFrom here to avoid
    // future plugin updates from changing the formatting and invalidating all migrations
    // ratchetFrom("origin/main")
    target("src/**/*.sql")
    prettier(packages())
        .config(
            mapOf(
                "plugins" to listOf("prettier-plugin-sql"),
                "language" to "postgresql",
                "keywordCase" to "lower",
                "printWidth" to 120))
  }
}
