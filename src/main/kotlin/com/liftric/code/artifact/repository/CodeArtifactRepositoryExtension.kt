package com.liftric.code.artifact.repository

import org.gradle.api.plugins.ExtensionContainer

abstract class CodeArtifactRepositoryExtension(private val extensionContainer: ExtensionContainer) : CodeArtifact() {
    fun additional(
        name: String,
        block: CodeArtifact.() -> Unit,
    ) {
        if (name.isEmpty()) error("Empty domain is not supported!")
        store[name] =
            extensionContainer.create(
                "${name}${CodeArtifactRepositoryPlugin.EXTENSION_NAME}",
                CodeArtifactRepositoryExtension::class.java,
                extensionContainer,
            ).apply {
                block.invoke(this)
                region.convention(this@CodeArtifactRepositoryExtension.region)
                profile.convention(this@CodeArtifactRepositoryExtension.profile)
                accessKeyId.convention(this@CodeArtifactRepositoryExtension.accessKeyId)
                secretAccessKey.convention(this@CodeArtifactRepositoryExtension.secretAccessKey)
                tokenExpiresIn.convention(this@CodeArtifactRepositoryExtension.tokenExpiresIn)
            }
    }

    companion object {
        internal val store = mutableMapOf<String, CodeArtifact>()
    }
}
