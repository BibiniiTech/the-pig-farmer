package com.example.smartswine.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

@Keep
data class Pig(
    val id: String = "",
    val tagNumber: String = "",
    val birthDate: String = "",
    val breed: String = "",
    val gender: String = "Male",
    
    @get:Exclude
    @set:Exclude
    var castrated: Boolean? = null, // Only for males
    
    val castrationDate: String = "",
    val weight: Double = 0.0,
    val purpose: String = "Porker", // Breeder or Porker
    val sowTag: String = "",
    val boarTag: String = "",
    val location: String = "", // Pen number or section
    val source: String = "Born on farm", // Born on farm or Brought to farm
    val status: String = "Piglet", // Calculated based on age/weight/rules
    val lastBreedingDate: String = "",
    val lastBoarTag: String = "",
    val hasFarrowed: Boolean = false,
    
    @get:Exclude
    @set:Exclude
    var teethClipped: Boolean = false,
    
    @get:Exclude
    @set:Exclude
    var tailDocked: Boolean = false,
    
    val ironInjections: Int = 0,
    
    @get:Exclude
    @set:Exclude
    var weaned: Boolean = false,
    
    val notes: String = ""
) {
    // Firestore mapping for 'is' prefixed fields
    @Suppress("unused")
    @get:PropertyName("isCastrated")
    @set:PropertyName("isCastrated")
    var firestoreIsCastrated: Boolean?
        get() = castrated
        set(value) { castrated = value }

    @Suppress("unused")
    @get:PropertyName("isTeethClipped")
    @set:PropertyName("isTeethClipped")
    var firestoreIsTeethClipped: Boolean
        get() = teethClipped
        set(value) { teethClipped = value }

    @Suppress("unused")
    @get:PropertyName("isTailDocked")
    @set:PropertyName("isTailDocked")
    var firestoreIsTailDocked: Boolean
        get() = tailDocked
        set(value) { tailDocked = value }

    @Suppress("unused")
    @get:PropertyName("isWeaned")
    @set:PropertyName("isWeaned")
    var firestoreIsWeaned: Boolean
        get() = weaned
        set(value) { weaned = value }

    // Legacy support for fields without 'is' prefix in Firestore
    @Suppress("unused")
    @get:PropertyName("castrated")
    @set:PropertyName("castrated")
    var legacyCastrated: Boolean?
        get() = castrated
        set(value) { castrated = value }

    @Suppress("unused")
    @get:PropertyName("teethClipped")
    @set:PropertyName("teethClipped")
    var legacyTeethClipped: Boolean
        get() = teethClipped
        set(value) { teethClipped = value }

    @Suppress("unused")
    @get:PropertyName("tailDocked")
    @set:PropertyName("tailDocked")
    var legacyTailDocked: Boolean
        get() = tailDocked
        set(value) { tailDocked = value }

    @Suppress("unused")
    @get:PropertyName("weaned")
    @set:PropertyName("weaned")
    var legacyWeaned: Boolean
        get() = weaned
        set(value) { weaned = value }
}
