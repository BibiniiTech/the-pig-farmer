package com.example.smartswine.model

enum class PigGender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female");

    companion object {
        fun fromString(value: String): PigGender = 
            entries.find { it.displayName.equals(value, ignoreCase = true) } ?: MALE
    }
}

enum class PigPurpose(val displayName: String) {
    PORKER("Porker"),
    BREEDER("Breeder");

    companion object {
        fun fromString(value: String): PigPurpose = 
            entries.find { it.displayName.equals(value, ignoreCase = true) } ?: PORKER
    }
}

enum class PigStatus(val displayName: String) {
    PIGLET("Piglet"),
    STARTER("Starter"),
    GROWER("Grower"),
    FINISHER("Finisher"),
    SOW("Sow"),
    GILT("Gilt"),
    BARROW("Barrow"),
    BOAR("Boar"),
    PREGNANT("Pregnant"),
    LACTATING("Lactating"),
    NURSING("Nursing"),
    UNKNOWN("Unknown"),
    ARCHIVED("Archived"),
    CULLED("Culled");

    companion object {
        fun fromString(value: String): PigStatus {
            return when {
                value.startsWith("Archived", ignoreCase = true) -> ARCHIVED
                value.startsWith("Culled", ignoreCase = true) -> CULLED
                else -> entries.find { it.displayName.equals(value, ignoreCase = true) } ?: UNKNOWN
            }
        }
    }
}
