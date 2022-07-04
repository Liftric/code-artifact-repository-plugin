package com.liftric.code.artifact.repository

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.codeartifact.CodeartifactClient
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse
import software.amazon.awssdk.services.codeartifact.model.GetRepositoryEndpointResponse
import software.amazon.awssdk.services.sts.StsClient
import java.net.URI

private const val extensionName = "CodeArtifactRepository"

abstract class CodeArtifactRepositoryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<CodeArtifactRepositoryExtension>(extensionName)

        extension.timeout.getOrElse(1_800) // 30min
        extension.shouldResolveCredentialsByEnvironment.getOrElse(false)

        val codeArtifact = CodeArtifact(extension)

        fun RepositoryHandler.codeArtifact(domain: String? = null, repository: String): MavenArtifactRepository = maven {
            name = listOf("CodeArtifact", domain ?: extension.domain.get(), repository).joinToString("") { it.capitalized() }
            url = URI(codeArtifact.repositoryEndpointResponse(repository).repositoryEndpoint())
            credentials {
                username = "aws"
                password = codeArtifact.authorizationTokenRepsponse().authorizationToken()
            }
        }
    }
}

class CodeArtifact(private val extension: CodeArtifactRepositoryExtension) {
    private val account: String
        get() = stsClient.getCallerIdentity {}.account()

    private val stsClient by lazy {
        StsClient.builder().apply {
            region(extension.region.get())
            if (extension.shouldResolveCredentialsByEnvironment.get().not()) {
                credentialsProvider {
                    ProfileCredentialsProvider.create(extension.profile.get()).resolveCredentials()
                }
            }
        }.build()
    }

    private val codeArtifactClient by lazy {
        CodeartifactClient.builder().apply {
            region(extension.region.get())
            if (extension.shouldResolveCredentialsByEnvironment.get().not()) {
                credentialsProvider {
                    ProfileCredentialsProvider.create(extension.profile.get()).resolveCredentials()
                }
            }
        }.build()
    }

    fun authorizationTokenRepsponse(): GetAuthorizationTokenResponse {
        return codeArtifactClient.getAuthorizationToken {
            it.domain(extension.domain.get())
            it.domainOwner(account)
            it.durationSeconds(extension.timeout.get())
        }
    }

    fun repositoryEndpointResponse(repository: String, format: String = "maven"): GetRepositoryEndpointResponse {
        return codeArtifactClient.getRepositoryEndpoint {
            it.domain(extension.domain.get())
            it.domainOwner(account)
            it.repository(repository)
            it.format(format)
        }
    }
}
