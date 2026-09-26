package com.example.smartswine.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionalRequirementTest {

    @Test
    fun testTargetCrudeProtein_explicitCrudeProtein() {
        val req = NutritionalRequirement(stage = "Grower", crudeProtein = 16.5, digestibleProtein = 14.0)
        assertEquals(16.5, req.getTargetCrudeProtein(), 0.001)
    }

    @Test
    fun testTargetCrudeProtein_fallbackFromDigestibleProtein() {
        val req = NutritionalRequirement(stage = "Grower", crudeProtein = 0.0, digestibleProtein = 14.0)
        // 14.0 / 0.85 = 16.4705...
        assertEquals(14.0 / 0.85, req.getTargetCrudeProtein(), 0.001)
    }

    @Test
    fun testTargetDietaryLysine_explicitDietaryLysine() {
        val req = NutritionalRequirement(stage = "Starter", dietaryLysine = 1.15, lysine = 6.1)
        assertEquals(1.15, req.getTargetDietaryLysine(), 0.001)
    }

    @Test
    fun testTargetDietaryLysine_fallbackFromProteinPercentage() {
        val req = NutritionalRequirement(stage = "Grower", dietaryLysine = 0.0, lysine = 6.1, digestibleProtein = 14.0)
        // (6.1 / 100.0) * 14.0 = 0.854
        assertEquals(0.854, req.getTargetDietaryLysine(), 0.001)
    }

    @Test
    fun testTargetDietaryMethionine_explicitDietaryMethionine() {
        val req = NutritionalRequirement(stage = "Finisher", dietaryMethionine = 0.50)
        assertEquals(0.50, req.getTargetDietaryMethionine(), 0.001)
    }
}
