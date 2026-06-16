package com.example.smartswine.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.StaffMember
import com.example.smartswine.utils.Translator
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

    fun setActiveFarmId(uid: String) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            fetchStaff()
        }
    }

    private val _staff = MutableStateFlow<List<StaffMember>>(emptyList())
    val staff: StateFlow<List<StaffMember>> = _staff.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchStaff()
    }

    private fun fetchStaff() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        staffListener?.remove()
        staffListener = db.collection("users").document(userId)
            .collection("staff")
            .addSnapshotListener { snapshot, _ ->
                if (auth.currentUser == null) {
                    staffListener?.remove()
                    staffListener = null
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val staffList = snapshot.toObjects(StaffMember::class.java)
                    _staff.value = staffList
                }
            }
    }

    fun addStaff(context: Context, staffMember: StaffMember) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        val trimmedEmail = staffMember.email.trim().lowercase()
        val cleanedStaff = staffMember.copy(email = trimmedEmail)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Verify premium status before allowing registry operations
                val userDoc = db.collection("users").document(userId).get().await()
                val isPremium = userDoc.getBoolean("isPremium") == true

                // 1. Create the staff record in Firestore first
                val ref = db.collection("users").document(userId)
                    .collection("staff").document()
                val newStaff = cleanedStaff.copy(id = ref.id)
                ref.set(newStaff).await()

                // 2. If app access is enabled AND user is premium, create the account and send invitation
                if (isPremium && newStaff.allowAppAccess && newStaff.email.isNotBlank()) {
                    // Register in staff_registry for automated discovery
                    db.collection("staff_registry").document(newStaff.email)
                        .set(mapOf("managerUid" to userId))
                        .await()
                    
                    inviteStaffMember(context, newStaff.email)
                }
            } catch (e: Exception) {
                Log.e("HRViewModel", "Error adding staff: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
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
            responseCode == 200
        } catch (e: Exception) {
            Log.e("HRViewModel", "REST signUp failed: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun inviteStaffMember(context: Context, email: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return

        try {
            val apiKey = FirebaseApp.getInstance().options.apiKey
            if (!apiKey.isNullOrEmpty()) {
                // Create the user account via REST API to avoid mutating default Auth instance state
                signUpUserRest(apiKey, cleanEmail)
            }
            
            // Send the password reset email using the default Auth instance (safe, stateless operation)
            auth.sendPasswordResetEmail(cleanEmail).await()
            Log.d("HRViewModel", "Invitation email sent to $cleanEmail")
        } catch (e: Exception) {
            Log.e("HRViewModel", "Failed to invite staff: ${e.message}")
        }
    }

    fun logSalaryPayment(member: StaffMember, month: String, notes: String, languageCode: String = "en") {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                
                val salaryLabel = Translator.getString("cat_sale", languageCode) // Need a better key for "Salary" or use category
                val monthLabel = Translator.getString("month", languageCode)
                val notesLabel = Translator.getString("notes", languageCode)
                
                val record = FinancialRecord(
                    id = "",
                    date = today,
                    type = "Expense",
                    category = "Salary",
                    description = "Salary for ${member.name} - $monthLabel: $month. $notesLabel: $notes",
                    amount = member.salary,
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

    fun updateStaff(context: Context, staffMember: StaffMember) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        val trimmedEmail = staffMember.email.trim().lowercase()
        val cleanedStaff = staffMember.copy(email = trimmedEmail)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Verify premium status before allowing registry operations
                val userDoc = db.collection("users").document(userId).get().await()
                val isPremium = userDoc.getBoolean("isPremium") == true

                // Fetch the existing member first to see if they already had access
                val oldMemberDoc = db.collection("users").document(userId)
                    .collection("staff").document(cleanedStaff.id)
                    .get()
                    .await()
                val oldMember = oldMemberDoc.toObject(StaffMember::class.java)
                
                val shouldInvite = isPremium && cleanedStaff.allowAppAccess && cleanedStaff.email.isNotBlank() &&
                        (oldMember == null || !oldMember.allowAppAccess || oldMember.email.trim().lowercase() != cleanedStaff.email)

                // Update registry based on app access status and premium status
                if (isPremium && cleanedStaff.allowAppAccess && cleanedStaff.email.isNotBlank()) {
                    db.collection("staff_registry").document(cleanedStaff.email)
                        .set(mapOf("managerUid" to userId))
                        .await()
                } else if (cleanedStaff.email.isNotBlank()) {
                    // Remove from registry if manager is not premium or access is revoked
                    db.collection("staff_registry").document(cleanedStaff.email)
                        .delete()
                        .await()
                }

                db.collection("users").document(userId)
                    .collection("staff").document(cleanedStaff.id)
                    .set(cleanedStaff).await()

                if (shouldInvite) {
                    inviteStaffMember(context, cleanedStaff.email)
                }
            } catch (e: Exception) {
                Log.e("HRViewModel", "Error updating staff: ${e.message}")
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
