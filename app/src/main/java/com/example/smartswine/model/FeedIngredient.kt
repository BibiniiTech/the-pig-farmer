package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class FeedIngredient(
    val id: String = "",
    val name: String = "",
    val nameResourceId: Int = 0,
    val crudeProtein: Double = 0.0,
    val crudeFiber: Double = 0.0,
    val calcium: Double = 0.0,
    val phosphorus: Double = 0.0,
    val sodium: Double = 0.0,
    val chloride: Double = 0.0,
    val potassium: Double = 0.0,
    val sulfur: Double = 0.0,
    val metabolizableEnergy: Double = 0.0, // ME (kcal/kg)
    val dryMatter: Double = 0.0,
    val fat: Double = 0.0,
    val lysine: Double = 0.0,
    val methionine: Double = 0.0,
    val cystine: Double = 0.0,
    val threonine: Double = 0.0,
    val tryptophan: Double = 0.0,
    val arginine: Double = 0.0,
    val isoleucine: Double = 0.0,
    val valine: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val costPerKg: Double = 0.0,
    val mainCategory: String = "", // Energy, Protein, Vitamins, Minerals & Salt
    val visible: Boolean = false, // Whether it should show in the inventory table
    val maxStarter: Double = 100.0,
    val maxGrower: Double = 100.0,
    val maxFinisher: Double = 100.0,
    val nameTranslations: Map<String, String> = emptyMap(),
)
