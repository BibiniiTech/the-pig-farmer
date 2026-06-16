package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class NutritionalRequirement(
    val stage: String = "",
    val digestibleProtein: Double = 0.0, // as %
    val metabolizableEnergy: Double = 0.0, // ME (kcal/kg)
    val calcium: Double = 0.0, // as %
    val phosphorus: Double = 0.0, // as %
    val lysine: Double = 0.0, // as % ptn
    val methionineCystine: Double = 0.0, // as % ptn
    val tryptophan: Double = 0.0, // as % ptn
    val crudeFiber: Double = 0.0, // as %
    val minDailyFeed: Double = 0.0, // kg/day
    val maxDailyFeed: Double = 0.0, // kg/day
)
