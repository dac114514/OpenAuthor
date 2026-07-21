package com.author.faster.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterContextLevelTest {
    @Test
    fun appliesHiddenMentionOnlyAndFullBoundaries() {
        val character = character(mentionAllowed = 3, firstAppearance = 8)

        assertEquals(CharacterContextLevel.HIDDEN, character.contextLevelAt(2))
        assertEquals(CharacterContextLevel.MENTION_ONLY, character.contextLevelAt(3))
        assertEquals(CharacterContextLevel.MENTION_ONLY, character.contextLevelAt(7))
        assertEquals(CharacterContextLevel.FULL, character.contextLevelAt(8))
    }

    @Test
    fun staysHiddenUntilFirstAppearanceWhenEarlyMentionIsUnset() {
        val character = character(mentionAllowed = null, firstAppearance = 5)

        assertEquals(CharacterContextLevel.HIDDEN, character.contextLevelAt(4))
        assertEquals(CharacterContextLevel.FULL, character.contextLevelAt(5))
    }

    private fun character(mentionAllowed: Int?, firstAppearance: Int) = Character(
        id = "character",
        projectId = "project",
        name = "沈潮",
        aliases = "",
        gender = "",
        age = "",
        identity = "领航员",
        publicRumors = "来自外海",
        factionId = null,
        appearance = "",
        mentionAllowedFromChapter = mentionAllowed,
        firstAppearanceChapter = firstAppearance,
        importance = "主要",
        currentStatus = "正常",
        thinkingStyle = "",
        speechStyle = "",
        behaviorHabits = "",
        decisionPattern = "",
        valuesAndLimits = "",
        relationshipHandling = "",
        informationHandling = "",
        background = "",
        currentGoal = "",
        longTermGoal = "",
        coreDesire = "",
        coreFear = "",
        secret = "不可提前泄露",
        abilitiesAndResources = "",
        weaknesses = "",
        isArchived = false,
        createdAt = 0,
        updatedAt = 0,
    )
}
