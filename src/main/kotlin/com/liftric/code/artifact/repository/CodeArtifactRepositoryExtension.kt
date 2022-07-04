package com.liftric.code.artifact.repository

import org.gradle.api.provider.Property
import software.amazon.awssdk.regions.Region

interface CodeArtifactRepositoryExtension {
    val region: Property<Region>
    val profile: Property<String>
    val domain: Property<String>
    val timeout: Property<Long>
    val shouldResolveCredentialsByEnvironment: Property<Boolean>
}
