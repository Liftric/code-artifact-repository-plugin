import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt

@Suppress("DSL_SCOPE_VIOLATION") // IntelliJ incorrectly marks libs as not callable
plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.versioning)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.version.check)
    alias(libs.plugins.version.catalog.update)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(libs.aws.ecs)
    implementation(libs.aws.codeartifact)
    implementation(libs.aws.sts)
}

ktlint {
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

detekt {
    ignoreFailures = true
    config = rootProject.files("config/detekt/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt.html"))
    }
}

versionCatalogUpdate {
    sortByKey.set(true)
    keep {
        keepUnusedVersions.set(false)
        keepUnusedLibraries.set(false)
        keepUnusedPlugins.set(false)
    }
}

tasks.withType<DependencyUpdatesTask> {
    fun String.isNonStable() = "^[0-9,.v-]+(-r)?$".toRegex().matches(this).not()
    rejectVersionIf {
        candidate.version.isNonStable()
    }
}

group = "com.liftric.code.artifact.repository"
version =
    with(versioning.info) {
        if (branch == "HEAD" && dirty.not()) {
            tag
        } else {
            full
        }
    }

gradlePlugin {
    website.set("https://github.com/Liftric/code-artifact-repository-plugin")
    vcsUrl.set("https://github.com/Liftric/code-artifact-repository-plugin")
    plugins {
        create("CodeArtifactRepositoryPlugin") {
            id = "com.liftric.code-artifact-repository-plugin"
            displayName = "code-artifact-repository-plugin"
            description = "Apply AWS CodeArtifact repositories"
            implementationClass = "com.liftric.code.artifact.repository.CodeArtifactRepositoryPlugin"
            tags.set(listOf("aws", "codeartifact", "maven", "repository"))
        }
    }
}
