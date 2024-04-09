package com.liftric.code.artifact.repository

import org.gradle.api.provider.Property
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.codeartifact.CodeartifactClient
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse
import software.amazon.awssdk.services.codeartifact.model.GetRepositoryEndpointResponse
import software.amazon.awssdk.services.sts.StsClient

/**
 * internal class handling all aws client config and calls
 */
abstract class CodeArtifact {
    abstract val region: Property<Region>
    abstract val profile: Property<String>
    abstract val tokenExpiresIn: Property<Long>
    abstract val accessKeyId: Property<String>
    abstract val secretAccessKey: Property<String>

    private val stsClient by lazy {
        StsClient.builder().apply {
            region.orNull?.let {
                region(it)
            }
            if (accessKeyId.orNull != null && secretAccessKey.orNull != null) {
                credentialsProvider {
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            accessKeyId.get(),
                            secretAccessKey.get(),
                        )
                    ).resolveCredentials()
                }
            } else {
                profile.orNull?.let {
                    credentialsProvider {
                        ProfileCredentialsProvider.create(profile.get()).resolveCredentials()
                    }
                }
            }
        }.build()
    }

    private val client by lazy {
        CodeartifactClient.builder().apply {
            region.orNull?.let {
                region(it)
            }
            if (accessKeyId.orNull != null && secretAccessKey.orNull != null) {
                credentialsProvider {
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            accessKeyId.get(),
                            secretAccessKey.get(),
                        )
                    ).resolveCredentials()
                }
            } else {
                profile.orNull?.let {
                    credentialsProvider {
                        ProfileCredentialsProvider.create(profile.get()).resolveCredentials()
                    }
                }
            }
        }.build()
    }

    private val accountId by lazy {
        stsClient.getCallerIdentity {}.account()
    }

    internal fun authorizationTokenResponse(domain: String): GetAuthorizationTokenResponse {
        return client.getAuthorizationToken {
            it.domain(domain)
            it.domainOwner(accountId)
            it.durationSeconds(tokenExpiresIn.getOrElse(1_800))
        }
    }

    internal fun repositoryEndpointResponse(
        domain: String,
        repository: String,
        format: String = "maven"
    ): GetRepositoryEndpointResponse {
        return client.getRepositoryEndpoint {
            it.domain(domain)
            it.domainOwner(accountId)
            it.repository(repository)
            it.format(format)
        }
    }
}
