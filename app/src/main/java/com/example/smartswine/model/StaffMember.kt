package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class StaffMember(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val phone: String = "",
    val salary: Double = 0.0,
    val joinDate: String = "",
    val status: String = "Active", // Active, Inactive, On Leave
    val allowAppAccess: Boolean = false,
    val email: String = "",
)
