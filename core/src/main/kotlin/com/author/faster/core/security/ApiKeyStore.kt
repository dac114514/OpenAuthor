package com.author.faster.core.security

interface ApiKeyStore {
    suspend fun save(alias: String, apiKey: String)

    suspend fun get(alias: String): String?

    suspend fun delete(alias: String)
}

