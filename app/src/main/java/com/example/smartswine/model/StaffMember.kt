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
    val inviteStatus: String = "none", // none, pending, sent, failed
    val gender: String = "",
    val residentialAddress: String = "",
    val dateOfBirth: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactAddress: String = "",
    val emergencyContactRelation: String = "",
    val photoUrl: String = ""
) {
    fun isRestrictedRole(): Boolean {
        val r = role.lowercase().trim()
        return r.contains("hand") || r.contains("labor") || r.contains("worker") || r.contains("herdsman") || r.contains("attendant")
    }
}
