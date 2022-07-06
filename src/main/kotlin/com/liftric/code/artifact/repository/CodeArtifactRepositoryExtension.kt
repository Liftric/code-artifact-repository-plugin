package com.liftric.code.artifact.repository

import org.gradle.api.plugins.ExtensionContainer

abstract class CodeArtifactRepositoryExtension(private val extensionContainer: ExtensionContainer) : CodeArtifact() {
    fun additional(name: String, block: CodeArtifact.() -> Unit) {
        if (name.isEmpty()) error("Empty domain is not supported!")
        store[name] = extensionContainer.create(
            "${name}${CodeArtifactRepositoryPlugin.extensionName}",
            CodeArtifactRepositoryExtension::class.java,
            extensionContainer
        ).apply {
            block.invoke(this)
            region.convention(this@CodeArtifactRepositoryExtension.region)
            profile.convention(this@CodeArtifactRepositoryExtension.profile)
            tokenExpiresIn.convention(this@CodeArtifactRepositoryExtension.tokenExpiresIn)
        }
    }

    companion object {
        internal val store = mutableMapOf<String, CodeArtifact>()
    }
}
