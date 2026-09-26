package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class NutritionalRequirement(
    val stage: String = "",
    val crudeProtein: Double = 0.0, // as % of diet (NRC standard)
    val digestibleProtein: Double = 0.0, // as %
    val metabolizableEnergy: Double = 0.0, // ME (kcal/kg)
    val calcium: Double = 0.0, // as %
    val phosphorus: Double = 0.0, // as %
    val lysine: Double = 0.0, // as % ptn (legacy) or dietary %
    val methionineCystine: Double = 0.0, // as % ptn (legacy) or dietary %
    val dietaryLysine: Double = 0.0, // as % of diet
    val dietaryMethionine: Double = 0.0, // as % of diet
    val tryptophan: Double = 0.0, // as % ptn
    val crudeFiber: Double = 0.0, // as %
    val minDailyFeed: Double = 0.0, // kg/day
    val maxDailyFeed: Double = 0.0, // kg/day
) {
    fun getTargetCrudeProtein(): Double =
        if (crudeProtein > 0.0) crudeProtein else (if (digestibleProtein > 0.0) digestibleProtein / 0.85 else 16.0)

    fun getTargetDigestibleProtein(): Double =
        if (digestibleProtein > 0.0) digestibleProtein else (getTargetCrudeProtein() * 0.85)

    fun getTargetDietaryLysine(): Double =
        if (dietaryLysine > 0.0) dietaryLysine
        else if (lysine > 0.0) (lysine / 100.0) * getTargetDigestibleProtein()
        else 0.95

    fun getTargetDietaryMethionine(): Double =
        if (dietaryMethionine > 0.0) dietaryMethionine
        else if (methionineCystine > 0.0) (methionineCystine / 100.0) * getTargetDigestibleProtein()
        else 0.55
}
