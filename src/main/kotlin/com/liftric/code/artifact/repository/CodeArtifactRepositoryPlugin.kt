package com.liftric.code.artifact.repository

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.codeartifact.CodeartifactClient
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse
import software.amazon.awssdk.services.codeartifact.model.GetRepositoryEndpointResponse
import software.amazon.awssdk.services.sts.StsClient

private const val extensionName = "CodeArtifactRepository"

private lateinit var codeArtifact: CodeArtifact
private lateinit var extension: CodeArtifactRepositoryExtension

abstract class CodeArtifactRepositoryPlugin : Plugin<Any> {
    override fun apply(scope: Any)  {
        when (scope) {
            is Settings -> {
                extension = scope.extensions.create(extensionName)
                codeArtifact = CodeArtifact(extension)
            }
            is Project -> {
                extension = scope.extensions.create(extensionName)
                codeArtifact = CodeArtifact(extension)
            }
            else -> {
                throw GradleException("Should only get applied on Settings or Project")
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
            if (!extension.shouldResolveCredentialsByEnvironment.getOrElse(true)) {
                credentialsProvider {
                    ProfileCredentialsProvider.create(extension.profile.get()).resolveCredentials()
                }
            }
        }.build()
    }

    private val codeArtifactClient by lazy {
        CodeartifactClient.builder().apply {
            region(extension.region.get())
            if (!extension.shouldResolveCredentialsByEnvironment.getOrElse(true)) {
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
            it.durationSeconds(extension.tokenExpiresIn.getOrElse(1_800))
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

inline fun Settings.codeArtifactRepository(configure: CodeArtifactRepositoryExtension.() -> Unit) {
    extensions.getByType<CodeArtifactRepositoryExtension>().configure()
}

inline fun Project.codeArtifactRepository(configure: CodeArtifactRepositoryExtension.() -> Unit) {
    extensions.getByType<CodeArtifactRepositoryExtension>().configure()
}

fun RepositoryHandler.codeArtifact(repository: String): MavenArtifactRepository = codeArtifact(extension.domain.get(), repository)

fun RepositoryHandler.codeArtifact(domain: String, repository: String): MavenArtifactRepository = maven {
    setName(listOf("CodeArtifact", domain, repository).joinToString("") { it.capitalized() })
    setUrl(codeArtifact.repositoryEndpointResponse(repository).repositoryEndpoint())
    credentials {
        username = "aws"
        password = codeArtifact.authorizationTokenRepsponse().authorizationToken()
    }
}
