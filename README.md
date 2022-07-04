# Code Artifact Repository Plugin

Convenience plugin to apply AWS CodeArtifact repositories to your Gradle project.

Configure the plugin extension and then apply your repositories.

```kotlin
plugins {
    id("com.liftric.code-artifact-repository-plugin") version "<latest>"
}

codeArtifactRepository {
    region.set(Region.EU_CENTRAL_1)
    profile.set("liftric")
    domain.set("liftric-frankfurt")
    // Determines how long the generated authentication token is valid in seconds
    tokenExpiresIn.set(1_800)
    // Set to true if you want to resolve the credentials via the environment,
    // e.g. when using the AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY env variables
    shouldResolveCredentialsByEnvironment.set(System.getenv("CI") != null)
}

repositories {
    codeArtifact("my_repository")
    codeArtifact("my_other_domain", "my_other_repository")
}
```
