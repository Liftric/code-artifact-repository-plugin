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
