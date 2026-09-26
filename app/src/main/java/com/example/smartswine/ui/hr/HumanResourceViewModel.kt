package com.example.smartswine.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.StaffMember
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.TierLimiter
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

import com.example.smartswine.model.FinancialRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HumanResourceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null
    private var staffListener: ListenerRegistration? = null

    fun setActiveFarmId(uid: String?) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            if (uid == null) {
                staffListener?.remove()
                staffListener = null
                _staff.value = emptyList()
            } else {
                fetchStaff()
            }
        }
    }

    private val _staff = MutableStateFlow<List<StaffMember>>(emptyList())
    val staff: StateFlow<List<StaffMember>> = _staff.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    init {
        fetchStaff()
    }

    private fun fetchStaff() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        staffListener?.remove()
        staffListener = db.collection("users").document(userId)
            .collection("staff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HRViewModel", "Error listening to staff: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (auth.currentUser == null) {
                    staffListener?.remove()
                    staffListener = null
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val staffList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val member = doc.toObject(StaffMember::class.java) ?: return@mapNotNull null
                            val salaryValue = when (val s = doc.get("salary")) {
                                is Number -> s.toDouble()
                                is String -> s.toDoubleOrNull() ?: member.salary
                                else -> member.salary
                            }
                            member.copy(
                                id = if (member.id.isNotBlank()) member.id else doc.id,
                                salary = salaryValue
                            )
                        } catch (e: Exception) {
                            Log.e("HRViewModel", "Error parsing staff doc ${doc.id}", e)
                            null
                        }
                    }
                    _staff.value = staffList
                }
            }
    }

    fun addStaff(staffMember: StaffMember) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        val trimmedEmail = staffMember.email.trim().lowercase()
        val cleanedStaff = staffMember.copy(email = trimmedEmail)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Tier limit check for free accounts
                var isPremium = false
                try {
                    val userDoc = db.collection("users").document(userId).get().await()
                    isPremium = userDoc.getBoolean("isPremium") == true
                    if (!isPremium && _staff.value.size >= TierLimiter.FREE_MAX_STAFF) {
                        _error.value = "Staff limit of ${TierLimiter.FREE_MAX_STAFF} reached for free tier. Please upgrade to add more."
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.w("HRViewModel", "Offline/error checking tier limit: ${e.message}")
                }

                // Free accounts cannot share app access
                val finalStaff = if (!isPremium) cleanedStaff.copy(allowAppAccess = false) else cleanedStaff

                // 1. Create and save the staff record in Firestore directly
                val staffRef = db.collection("users").document(userId)
                    .collection("staff").document()
                val newStaff = finalStaff.copy(id = staffRef.id)
                staffRef.set(newStaff).await()

                // 2. If app access is enabled, check premium and register in background
                if (isPremium && newStaff.allowAppAccess && newStaff.email.isNotBlank()) {
                    try {
                        val registryRef = db.collection("staff_registry").document(newStaff.email)
                        registryRef.set(mapOf("managerUid" to userId)).await()
                        inviteStaffMember(userId, newStaff.id, newStaff.email)
                    } catch (inviteEx: Exception) {
                        Log.e("HRViewModel", "Error in staff registry or invitation: ${inviteEx.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("HRViewModel", "Error adding staff: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addStaff(context: Context, staffMember: StaffMember) {
        addStaff(staffMember)
    }

    private suspend fun signUpUserRest(apiKey: String, email: String): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            
            // Create a random temporary password
            val tempPassword = UUID.randomUUID().toString()
            val jsonInputString = "{\"email\":\"$email\",\"password\":\"$tempPassword\",\"returnSecureToken\":false}"

            conn.outputStream.use { os ->
                val input = jsonInputString.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }

            val responseCode = conn.responseCode
            Log.d("HRViewModel", "REST signUp response code: $responseCode")
            // 200 is success, 400 with EMAIL_EXISTS is also "fine" for our flow
            responseCode == 200
        } catch (e: Exception) {
            Log.e("HRViewModel", "REST signUp failed: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun inviteStaffMember(managerUid: String, staffId: String, email: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return

        val staffRef = db.collection("users").document(managerUid)
            .collection("staff").document(staffId)

        try {
            staffRef.update("inviteStatus", "pending").await()

            val apiKey = FirebaseApp.getInstance().options.apiKey
            if (!apiKey.isNullOrEmpty()) {
                // Create the user account via REST API to avoid mutating default Auth instance state
                signUpUserRest(apiKey, cleanEmail)
            }
            
            // Send the password reset email using the default Auth instance (safe, stateless operation)
            auth.sendPasswordResetEmail(cleanEmail).await()
            staffRef.update("inviteStatus", "sent").await()
            Log.d("HRViewModel", "Invitation email sent to $cleanEmail")
        } catch (e: Exception) {
            Log.e("HRViewModel", "Failed to invite staff: ${e.message}")
            staffRef.update("inviteStatus", "failed").await()
        }
    }

    fun resendInvitation(context: Context, staffMember: StaffMember) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (staffMember.email.isBlank()) return
        viewModelScope.launch {
            inviteStaffMember(userId, staffMember.id, staffMember.email)
        }
    }

    fun logSalaryPayment(
        member: StaffMember,
        month: String,
        notes: String,
        bonus: Double = 0.0,
        deduction: Double = 0.0,
        adjustmentDescription: String = "",
        languageCode: String = "en"
    ) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                
                val monthLabel = Translator.getString("month", languageCode)
                val notesLabel = Translator.getString("notes", languageCode)
                val netAmount = maxOf(0.0, member.salary + bonus - deduction)
                
                val descBuilder = java.lang.StringBuilder()
                descBuilder.append("Salary for ${member.name} - $monthLabel: $month.")
                descBuilder.append(" Base: ${String.format(Locale.getDefault(), "%.2f", member.salary)}")
                if (bonus > 0.0) {
                    descBuilder.append(" | Bonus: +${String.format(Locale.getDefault(), "%.2f", bonus)}")
                }
                if (deduction > 0.0) {
                    descBuilder.append(" | Deduction: -${String.format(Locale.getDefault(), "%.2f", deduction)}")
                }
                if (adjustmentDescription.isNotBlank()) {
                    descBuilder.append(" (${adjustmentDescription.trim()})")
                }
                descBuilder.append(" | Net: ${String.format(Locale.getDefault(), "%.2f", netAmount)}")
                if (notes.isNotBlank()) {
                    descBuilder.append(". $notesLabel: $notes")
                }
                
                val record = FinancialRecord(
                    id = "",
                    date = today,
                    type = "Expense",
                    category = "Salary",
                    description = descBuilder.toString(),
                    amount = netAmount,
                )
                
                val ref = db.collection("users").document(userId)
                    .collection("financials").document()
                db.collection("users").document(userId)
                    .collection("financials").document(ref.id)
                    .set(record.copy(id = ref.id)).await()

            } catch (_: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStaff(staffMember: StaffMember) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        val trimmedEmail = staffMember.email.trim().lowercase()
        val cleanedStaff = staffMember.copy(email = trimmedEmail)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val isPremium = userDoc.getBoolean("isPremium") == true

                val oldMemberDoc = db.collection("users").document(userId)
                    .collection("staff").document(cleanedStaff.id)
                    .get().await()
                val oldMember = oldMemberDoc.toObject(StaffMember::class.java)
                
                val shouldInvite = isPremium && cleanedStaff.allowAppAccess && cleanedStaff.email.isNotBlank() &&
                        (oldMember == null || !oldMember.allowAppAccess || oldMember.email.trim().lowercase() != cleanedStaff.email)

                val batch = db.batch()

                // Update registry
                if (isPremium && cleanedStaff.allowAppAccess && cleanedStaff.email.isNotBlank()) {
                    val registryRef = db.collection("staff_registry").document(cleanedStaff.email)
                    batch.set(registryRef, mapOf("managerUid" to userId))
                } else if (cleanedStaff.email.isNotBlank()) {
                    val registryRef = db.collection("staff_registry").document(cleanedStaff.email)
                    batch.delete(registryRef)
                }
                
                // Cleanup old email if changed
                val oldEmail = oldMember?.email?.trim()?.lowercase() ?: ""
                if (oldEmail.isNotBlank() && oldEmail != cleanedStaff.email) {
                    val oldRegistryRef = db.collection("staff_registry").document(oldEmail)
                    batch.delete(oldRegistryRef)
                }

                val staffRef = db.collection("users").document(userId)
                    .collection("staff").document(cleanedStaff.id)
                batch.set(staffRef, cleanedStaff)

                batch.commit().await()

                if (shouldInvite) {
                    inviteStaffMember(userId, cleanedStaff.id, cleanedStaff.email)
                }
            } catch (e: Exception) {
                Log.e("HRViewModel", "Error updating staff: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStaff(context: Context, staffMember: StaffMember) {
        updateStaff(staffMember)
    }

    fun archiveStaff(staffMember: StaffMember) {
        updateStaff(staffMember.copy(status = "Archived", allowAppAccess = false))
    }

    fun paySalary(
        member: StaffMember,
        date: String,
        notes: String,
        base: Double,
        bonus: Double,
        total: Double
    ) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val record = FinancialRecord(
                    id = "",
                    date = date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                    type = "Expense",
                    category = "Salary",
                    description = "Salary payment to ${member.name} (${member.role}). Base: ${String.format(Locale.getDefault(), "%.2f", base)}, Bonus: ${String.format(Locale.getDefault(), "%.2f", bonus)}. Notes: $notes",
                    amount = total
                )
                val ref = db.collection("users").document(userId)
                    .collection("financials").document()
                db.collection("users").document(userId)
                    .collection("financials").document(ref.id)
                    .set(record.copy(id = ref.id)).await()
            } catch (e: Exception) {
                Log.e("HRViewModel", "Error paying salary: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteStaff(staffId: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch staff email to remove from registry
                val staffDoc = db.collection("users").document(userId)
                    .collection("staff").document(staffId).get().await()
                val email = staffDoc.getString("email")
                
                if (!email.isNullOrBlank()) {
                    val cleanEmail = email.trim().lowercase()
                    db.collection("staff_registry").document(cleanEmail).delete().await()
                }

                db.collection("users").document(userId)
                    .collection("staff").document(staffId)
                    .delete().await()
            } catch (e: Exception) {
                Log.e("HRViewModel", "Error deleting staff: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        staffListener?.remove()
    }
}
