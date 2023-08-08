# Code Artifact Repository Plugin

Convenience plugin to apply AWS CodeArtifact repositories to your Gradle project.

Configure the plugin extension either in the settings.gradle.kts or build.gradle.kts and then apply your repositories.

```kotlin
plugins {
    id("com.liftric.code-artifact-repository-plugin") version "<latest>"
}

codeArtifactRepository {
    region.set(Region.EU_CENTRAL_1)
    // use profile credentials provider, otherwise the default credentials chain of aws will be used
    profile.set("liftric")
    // Determines how long the generated authentication token is valid in seconds
    tokenExpiresIn.set(1_800)
}

dependencyResolutionManagement {
    repositories {
        codeArtifact("my_domain", "my_repository")
        codeArtifact("my_other_domain", "my_other_repository")
    }
}
```

You can also use multiple CodeArtifact endpoints:
```kotlin
plugins {
    id("com.liftric.code-artifact-repository-plugin") version "<latest>"
}

codeArtifactRepository {
    region.set(Region.EU_CENTRAL_1)
    profile.set("liftric")
    additional("customer1") {
        profile.set("customer1")
        // reuses properties of the default extension if not explicitly specified
    }
}

dependencyResolutionManagement {
    repositories {
        // uses the default extension (liftric profile)
        codeArtifact(domain = "my_domain", repository = "my_repository")
        // uses the customer1 extension (customer1 profile)
        codeArtifact(additionalName = "customer1", domain = "my_other_domain", repository = "my_other_repository")
    }
}
```

You can also just get the token and endpoint, if you wan't to configure something different, like https://npm-publish.petuska.dev/:

```kotlin
val token = codeArtifactToken(domain = "my_domain")
val uri = codeArtifactUri(domain = "my_domain", repository = "my_repository")
```

## Use from gradle init script
The plugin can also be used during gradle init time.

The example (the `init.gradle.kts` file) configures a custom plugin repository, in our case used for a custom and private fargate plugin which is hosted
in a private code artifact repository.

**Note:** We have to use a gradle init script, no other way to apply the CodeArtifactRepositoryPlugin before the pluginManagement block inside a `settings.gradle.kts` file.

```
import com.liftric.code.artifact.repository.codeArtifact
import com.liftric.code.artifact.repository.codeArtifactRepository
import software.amazon.awssdk.regions.Region.*

initscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.liftric.code.artifact.repository:code-artifact-repository-plugin:<latest>")
    }
}
apply<com.liftric.code.artifact.repository.CodeArtifactRepositoryPlugin>()
codeArtifactRepository {
    region.set(EU_CENTRAL_1)
    // use profile credentials provider, otherwise the default credentials chain of aws will be used
    if (System.getenv("CI") == null) {
        profile.set("liftric")
    }
    // Determines how long the generated authentication token is valid in seconds
    tokenExpiresIn.set(1_800)
}
settingsEvaluated {
    pluginManagement {
        repositories {
            codeArtifact("<private-domain>", "fargate-gradle-plugin")
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

```
