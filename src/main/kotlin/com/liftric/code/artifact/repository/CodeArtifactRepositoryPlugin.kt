@file:Suppress("unused")

package com.liftric.code.artifact.repository

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.getByName
import java.net.URI

abstract class CodeArtifactRepositoryPlugin : Plugin<Any> {
    override fun apply(scope: Any) {
        when (scope) {
            is Settings -> {
                scope.extensions.create(extensionName, CodeArtifactRepositoryExtension::class.java, scope.extensions)
                    .also {
                        CodeArtifactRepositoryExtension.store[""] = it
                    }
            }
            is Project -> {
                scope.extensions.create(extensionName, CodeArtifactRepositoryExtension::class.java, scope.extensions)
                    .also {
                        CodeArtifactRepositoryExtension.store[""] = it
                    }
            }
            is Gradle -> {
                scope.beforeSettings {
                    extensions.create(extensionName, CodeArtifactRepositoryExtension::class.java, extensions)
                        .also {
                            CodeArtifactRepositoryExtension.store[""] = it
                        }
                }
            }
            else -> {
                throw GradleException("Should only get applied on Settings or Project")
            }
        }
    }

    companion object {
        const val extensionName = "CodeArtifactRepository"
    }
}

inline fun Settings.codeArtifactRepository(configure: CodeArtifactRepositoryExtension.() -> Unit) {
    extensions.getByName<CodeArtifactRepositoryExtension>(CodeArtifactRepositoryPlugin.extensionName).configure()
}

inline fun Project.codeArtifactRepository(configure: CodeArtifactRepositoryExtension.() -> Unit) {
    extensions.getByName<CodeArtifactRepositoryExtension>(CodeArtifactRepositoryPlugin.extensionName).configure()
}

inline fun Gradle.codeArtifactRepository(crossinline configure: CodeArtifactRepositoryExtension.() -> Unit) {
    settingsEvaluated {
        extensions.getByName<CodeArtifactRepositoryExtension>(CodeArtifactRepositoryPlugin.extensionName).configure()
    }
}

/**
 * Use the default CodeArtifact config (and therefore extension)
 */
fun RepositoryHandler.codeArtifact(domain: String, repository: String): MavenArtifactRepository =
    codeArtifact("", domain, repository)

/**
 * Use CodeArtifact by additional name
 */
fun RepositoryHandler.codeArtifact(additionalName: String, domain: String, repository: String) = maven {
    CodeArtifactRepositoryExtension.store[additionalName]?.let {
        name = listOf(additionalName, domain, repository).joinToString("") { it.capitalized() }
        url = URI.create(it.repositoryEndpointResponse(domain, repository).repositoryEndpoint())
        credentials {
            username = "aws"
            password = it.authorizationTokenResponse(domain).authorizationToken()
        }
    } ?: throw GradleException("Couldn't find CodeArtifactRepositoryExtension named '$additionalName'")
}

/**
 * If you need the plain token
 */
fun codeArtifactToken(domain: String): String = codeArtifactToken("", domain)

/**
 * If you need the plain endpoint uri
 */
fun codeArtifactUri(domain: String, repository: String, format: String): URI =
    codeArtifactUri("", domain, repository, format)

/**
 * If you need the plain token
 *
 * @param additionalName this is the name (prefix) of the codeArtifactRepository configuration. Use an empty string to use
 * the default extension
 */
fun codeArtifactToken(additionalName: String, domain: String): String {
    val settings = CodeArtifactRepositoryExtension.store[additionalName]
        ?: throw GradleException("didn't find CodeArtifactRepositoryExtension with the name: $")
    return settings.authorizationTokenResponse(domain).authorizationToken()
}

/**
 * If you need the plain endpoint uri
 *
 * @param additionalName this is the name (prefix) of the codeArtifactRepository configuration. Use an empty string to use
 * the default extension
 */
fun codeArtifactUri(additionalName: String, domain: String, repository: String, format: String): URI {
    val settings = CodeArtifactRepositoryExtension.store[additionalName]
        ?: throw GradleException("didn't find CodeArtifactRepositoryExtension with the name: $")
    return settings.repositoryEndpointResponse(domain, repository, format).repositoryEndpoint().let { URI.create(it) }
}
