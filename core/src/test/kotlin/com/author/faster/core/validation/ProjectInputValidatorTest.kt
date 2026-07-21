package com.author.faster.core.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectInputValidatorTest {
    @Test
    fun validProjectHasNoErrors() {
        val errors = ProjectInputValidator.validateProject(
            ProjectInput(
                title = "群星归途",
                author = "林野",
                creativePremise = "失落舰队寻找回家的道路",
                targetChapterCount = 240,
                defaultWordsPerChapter = 3000,
            ),
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun invalidModelReturnsReadableErrors() {
        val errors = ProjectInputValidator.validateModel(
            ModelConfigInput(
                name = "",
                baseUrl = "http://insecure.example.com",
                apiKey = "",
                modelId = "",
                maxContextTokens = 0,
                maxOutputTokens = 0,
                temperature = 3.0,
            ),
        )

        assertEquals(7, errors.size)
        assertTrue(errors.contains("Base URL 必须使用 HTTPS"))
    }
}

