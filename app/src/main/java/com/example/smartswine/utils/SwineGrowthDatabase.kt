package com.example.smartswine.utils

import java.util.Locale

data class GrowthPoint(
    val ageDays: Int,
    val weightKg: Double
)

data class BreedCurve(
    val key: String,
    val displayName: String,
    val points: List<GrowthPoint>
)

object SwineGrowthDatabase {

    // 10% reduced curves from veterinary standards
    val LargeWhite = BreedCurve(
        key = "Large White",
        displayName = "Large White",
        points = listOf(
            GrowthPoint(0, 1.26),
            GrowthPoint(28, 7.20),
            GrowthPoint(56, 19.80),
            GrowthPoint(84, 39.15),
            GrowthPoint(112, 63.90),
            GrowthPoint(140, 87.75),
            GrowthPoint(168, 105.75),
            GrowthPoint(252, 144.00),
            GrowthPoint(364, 180.00)
        )
    )

    val Landrace = BreedCurve(
        key = "Landrace",
        displayName = "Landrace",
        points = listOf(
            GrowthPoint(0, 1.22),
            GrowthPoint(28, 6.93),
            GrowthPoint(56, 19.35),
            GrowthPoint(84, 37.80),
            GrowthPoint(112, 62.10),
            GrowthPoint(140, 85.50),
            GrowthPoint(168, 103.50),
            GrowthPoint(252, 139.50),
            GrowthPoint(364, 171.00)
        )
    )

    val Duroc = BreedCurve(
        key = "Duroc",
        displayName = "Duroc",
        points = listOf(
            GrowthPoint(0, 1.31),
            GrowthPoint(28, 7.47),
            GrowthPoint(56, 21.60),
            GrowthPoint(84, 41.40),
            GrowthPoint(112, 67.50),
            GrowthPoint(140, 91.80),
            GrowthPoint(168, 112.50),
            GrowthPoint(252, 153.00),
            GrowthPoint(364, 189.00)
        )
    )

    val Hampshire = BreedCurve(
        key = "Hampshire",
        displayName = "Hampshire",
        points = listOf(
            GrowthPoint(0, 1.17),
            GrowthPoint(28, 6.75),
            GrowthPoint(56, 18.00),
            GrowthPoint(84, 36.00),
            GrowthPoint(112, 58.50),
            GrowthPoint(140, 81.00),
            GrowthPoint(168, 99.00),
            GrowthPoint(252, 130.50),
            GrowthPoint(364, 162.00)
        )
    )

    val Berkshire = BreedCurve(
        key = "Berkshire",
        displayName = "Berkshire",
        points = listOf(
            GrowthPoint(0, 1.13),
            GrowthPoint(28, 6.30),
            GrowthPoint(56, 17.10),
            GrowthPoint(84, 34.20),
            GrowthPoint(112, 54.00),
            GrowthPoint(140, 76.50),
            GrowthPoint(168, 94.50),
            GrowthPoint(252, 126.00),
            GrowthPoint(364, 153.00)
        )
    )

    val LocalHeritage = BreedCurve(
        key = "Local / Heritage",
        displayName = "Local / Heritage",
        points = listOf(
            GrowthPoint(0, 0.81),
            GrowthPoint(28, 4.50),
            GrowthPoint(56, 9.90),
            GrowthPoint(84, 17.10),
            GrowthPoint(112, 27.00),
            GrowthPoint(140, 36.90),
            GrowthPoint(168, 46.80),
            GrowthPoint(252, 67.50),
            GrowthPoint(364, 90.00)
        )
    )


    val standardOptions = listOf(
        "Large White",
        "Landrace",
        "Duroc",
        "Hampshire",
        "Berkshire",
        "Large White x Landrace (F1)",
        "Duroc x (Large White x Landrace)",
        "Duroc x Landrace",
        "Duroc x Large White",
        "Hampshire x Landrace",
        "Hampshire x Large White",
        "Local / Heritage",
        "Other"
    )

    /**
     * Resolves a breed name string to a BreedCurve.
     * Supports dynamic cross-breed blending if parent breeds are separated by 'x', '/' or '+'.
     */
    fun resolveBreedCurve(breedInput: String): BreedCurve {
        val normalized = breedInput.trim().lowercase(Locale.ROOT)

        // 0. Handle empty or unknown breed - Use Local / Heritage as the least performing standard
        if (normalized.isEmpty() || normalized == "other" || normalized == "none" || normalized == "unknown") {
            return LocalHeritage
        }

        // 1. Exact or keyword checks for predefined crosses
        if (normalized.contains("duroc") && normalized.contains("large white") && normalized.contains("landrace")) {
            return blendCurves(listOf(Duroc, LargeWhite, Landrace), "Duroc x (Large White x Landrace)", 1.08) // 8% heterosis
        }
        if (normalized.contains("large white") && normalized.contains("landrace")) {
            return blendCurves(listOf(LargeWhite, Landrace), "Large White x Landrace (F1)", 1.05) // 5% heterosis
        }

        // 2. Parse cross-breeding tokens
        val separators = regexSeparators()
        val parts = normalized.split(separators).map { it.trim() }.filter { it.isNotEmpty() }

        if (parts.size > 1) {
            val matchedCurves = parts.mapNotNull { findBaseBreed(it) }
            if (matchedCurves.isNotEmpty()) {
                val crossName = matchedCurves.joinToString(" x ") { it.displayName }
                return blendCurves(matchedCurves, crossName, 1.05) // 5% heterosis
            }
        }

        // 3. Match single base breed keyword
        val singleMatch = findBaseBreed(normalized)
        if (singleMatch != null) {
            return singleMatch
        }

        // 4. Fallback to Local / Heritage standard if no breed is selected or matched
        return LocalHeritage
    }

    private fun findBaseBreed(token: String): BreedCurve? {
        return when {
            token.contains("duroc") -> Duroc
            token.contains("landrace") -> Landrace
            token.contains("large white") || token.contains("yorkshire") || token.contains("white") -> LargeWhite
            token.contains("hampshire") || token.contains("hamp") -> Hampshire
            token.contains("berkshire") || token.contains("berk") -> Berkshire
            token.contains("local") || token.contains("heritage") || token.contains("indigenous") || token.contains("native") || token.contains("kolbroek") || token.contains("mukota") -> LocalHeritage
            else -> null
        }
    }

    private fun regexSeparators() = Regex("[x/+]|\\bcross\\b|\\bhybrid\\b")

    /**
     * Blends multiple growth curves by averaging the expected weights at each standard age milestone.
     */
    private fun blendCurves(curves: List<BreedCurve>, displayName: String, vigorFactor: Double = 1.0): BreedCurve {
        val standardAges = listOf(0, 28, 56, 84, 112, 140, 168, 252, 364)
        val blendedPoints = standardAges.map { age ->
            val sumWeights = curves.sumOf { curve ->
                interpolateCurve(curve.points, age)
            }
            val avgWeight = sumWeights / curves.size
            GrowthPoint(age, (avgWeight * vigorFactor).round(2))
        }
        return BreedCurve(
            key = displayName,
            displayName = displayName,
            points = blendedPoints
        )
    }

    /**
     * Estimates weight for a given age using linear interpolation between known data points.
     */
    fun interpolateCurve(points: List<GrowthPoint>, ageDays: Int): Double {
        if (points.isEmpty()) return 0.0
        val sorted = points.sortedBy { it.ageDays }
        
        // Exact age matches or edge limits
        if (ageDays <= sorted.first().ageDays) return sorted.first().weightKg
        if (ageDays >= sorted.last().ageDays) return sorted.last().weightKg

        // Find intervals to interpolate
        for (i in 0 until sorted.size - 1) {
            val p1 = sorted[i]
            val p2 = sorted[i + 1]
            if (ageDays in p1.ageDays..p2.ageDays) {
                val ratio = (ageDays - p1.ageDays).toDouble() / (p2.ageDays - p1.ageDays).toDouble()
                return p1.weightKg + ratio * (p2.weightKg - p1.weightKg)
            }
        }
        return sorted.last().weightKg
    }

    /**
     * Scores the pig's performance based on age (days) and weight.
     * Returns "Excellent", "Good", "Caution", "Poor" or "Blank" (if weight is 0 or age is invalid).
     */
    fun evaluatePerformance(breed: String, ageDays: Int, actualWeight: Double): String {
        if (actualWeight <= 0.0) {
            return "Blank"
        }
        val safeAgeDays = if (ageDays < 0) 0 else ageDays
        val curve = resolveBreedCurve(breed)
        val expectedWeight = interpolateCurve(curve.points, safeAgeDays)
        val safeExpectedWeight = if (expectedWeight <= 0.0) {
            curve.points.firstOrNull()?.weightKg ?: 1.0
        } else {
            expectedWeight
        }

        val ratio = actualWeight / safeExpectedWeight
        
        // For mature pigs (age > 270 days / 9 months), growth plateaus.
        // We broaden the "Good" range to account for normal variations in mature size
        // without falsely flagging healthy mature animals whose growth has slowed down.
        return if (safeAgeDays > 270) {
            when {
                ratio >= 1.35 -> "Excellent"
                ratio >= 0.85 -> "Good"
                ratio >= 0.70 -> "Caution"
                else -> "Poor"
            }
        } else {
            when {
                ratio >= 1.15 -> "Excellent"
                ratio >= 0.90 -> "Good"
                ratio >= 0.75 -> "Caution"
                else -> "Poor"
            }
        }
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10.0 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}
