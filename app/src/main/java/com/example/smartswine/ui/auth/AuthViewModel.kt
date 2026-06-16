package com.example.smartswine.ui.auth

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Keep
data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val farmName: String = "",
    val country: String = "",
    val countryCode: String = "",
    val email: String = "",
    
    @get:PropertyName("isPremium")
    @set:PropertyName("isPremium")
    var isPremium: Boolean = false,
    
    val appLanguage: String = "",

    @get:PropertyName("admin")
    @set:PropertyName("admin")
    var isAdmin: Boolean = false,

    @get:PropertyName("isKofisPerson")
    @set:PropertyName("isKofisPerson")
    var isKofisPerson: Boolean = false,

    val subscriptionSource: String = "",
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _activeFarmUid = MutableStateFlow<String?>(null)
    val activeFarmUid: StateFlow<String?> = _activeFarmUid.asStateFlow()

    private val _isProfileComplete = MutableStateFlow<Boolean?>(null)
    val isProfileComplete: StateFlow<Boolean?> = _isProfileComplete.asStateFlow()

    private val _isStaffAccessDenied = MutableStateFlow<Boolean>(false)
    val isStaffAccessDenied: StateFlow<Boolean> = _isStaffAccessDenied.asStateFlow()

    private var profileListener: ListenerRegistration? = null
    private var registryListener: ListenerRegistration? = null
    private var managerProfileListener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            _user.value = currentUser
            if (currentUser != null) {
                fetchUserProfile(currentUser.uid)
            } else {
                cleanupListeners()
                _userProfile.value = null
                _activeFarmUid.value = null
                _isProfileComplete.value = null
                _isStaffAccessDenied.value = false
            }
        }
    }

    private fun cleanupListeners() {
        profileListener?.remove()
        profileListener = null
        registryListener?.remove()
        registryListener = null
        managerProfileListener?.remove()
        managerProfileListener = null
        _isStaffAccessDenied.value = false
    }

    private fun fetchUserProfile(uid: String) {
        profileListener?.remove()
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener { document, error ->
                if (auth.currentUser == null) {
                    cleanupListeners()
                    return@addSnapshotListener
                }
                if (error != null) return@addSnapshotListener
                if (document != null && document.exists()) {
                    val profile = document.toObject(UserProfile::class.java)
                    _userProfile.value = profile
                    _activeFarmUid.value = uid
                    _isProfileComplete.value = true
                    _isStaffAccessDenied.value = false
                    registryListener?.remove() // If we are owner, we don't need registry

                    // Auto-fix admin permission for super-admin email
                    if (profile?.email == "bibiniitech@gmail.com" && !profile.isAdmin) {
                        db.collection("users").document(uid).update("admin", true)
                    }
                } else if (document != null && !document.exists()) {
                    // 2. Try to fetch as Staff via registry
                    val email = auth.currentUser?.email
                    if (email != null) {
                        fetchStaffRegistry(email)
                    } else {
                        _isProfileComplete.value = false
                        _isStaffAccessDenied.value = false
                    }
                }
            }
    }

    private fun fetchStaffRegistry(email: String) {
        val cleanEmail = email.trim().lowercase()
        registryListener?.remove()
        registryListener = db.collection("staff_registry").document(cleanEmail)
            .addSnapshotListener { registryDoc, error ->
                if (auth.currentUser == null) {
                    cleanupListeners()
                    return@addSnapshotListener
                }
                if (error != null) return@addSnapshotListener
                if (registryDoc != null && registryDoc.exists()) {
                    val managerUid = registryDoc.getString("managerUid")
                    if (managerUid != null) {
                        // Load manager's profile to get farm info and verify premium status
                        managerProfileListener?.remove()
                        managerProfileListener = db.collection("users").document(managerUid)
                            .addSnapshotListener { managerDoc, managerError ->
                                if (auth.currentUser == null) {
                                    cleanupListeners()
                                    return@addSnapshotListener
                                }
                                if (managerError != null) return@addSnapshotListener
                                if (managerDoc != null && managerDoc.exists()) {
                                    val managerProfile = managerDoc.toObject(UserProfile::class.java)
                                    val staffEmail = auth.currentUser?.email?.lowercase() ?: ""
                                    
                                    // Multi-layer security check for staff access:
                                    // 1. Manager must be currently Premium
                                    if (managerProfile?.isPremium == true) {
                                        // 2. Verify staff member still exists in manager's list and has access enabled
                                        db.collection("users").document(managerUid)
                                            .collection("staff")
                                            .whereEqualTo("email", staffEmail)
                                            .whereEqualTo("allowAppAccess", true)
                                            .get()
                                            .addOnSuccessListener { staffSnapshot ->
                                                if (!staffSnapshot.isEmpty) {
                                                    _userProfile.value = managerProfile
                                                    _activeFarmUid.value = managerUid
                                                    _isProfileComplete.value = true
                                                    _isStaffAccessDenied.value = false
                                                } else {
                                                    // Access revoked by manager or staff deleted
                                                    _userProfile.value = null
                                                    _activeFarmUid.value = null
                                                    _isProfileComplete.value = false
                                                    _isStaffAccessDenied.value = true
                                                }
                                            }
                                            .addOnFailureListener {
                                                _userProfile.value = null
                                                _activeFarmUid.value = null
                                                _isProfileComplete.value = false
                                                _isStaffAccessDenied.value = true
                                            }
                                    } else {
                                        // Manager lost premium status, revoke staff access
                                        _userProfile.value = null
                                        _activeFarmUid.value = null
                                        _isProfileComplete.value = false
                                        _isStaffAccessDenied.value = true
                                    }
                                } else {
                                    // Manager profile deleted or doesn't exist
                                    _userProfile.value = null
                                    _activeFarmUid.value = null
                                    _isProfileComplete.value = false
                                    _isStaffAccessDenied.value = true
                                }
                            }
                    }
                } else if (registryDoc != null && !registryDoc.exists()) {
                    _isProfileComplete.value = false
                    _isStaffAccessDenied.value = false
                }
            }
    }

    fun signOut() {
        cleanupListeners()
        auth.signOut()
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(false, "No authenticated user found")
            return
        }
        val uid = user.uid
        val collections = listOf("pigs", "archived_pigs", "financials", "ingredients", "requirements", "staff", "salaries")
        
        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(uid)
                
                // 1. Delete all subcollections first
                for (coll in collections) {
                    val snapshot = userRef.collection(coll).get().await()
                    if (!snapshot.isEmpty) {
                        val batch = db.batch()
                        for (doc in snapshot.documents) {
                            batch.delete(doc.reference)
                        }
                        batch.commit().await()
                    }
                }
                
                // 2. Delete the main user document
                userRef.delete().await()
                
                // 3. Delete the authentication account
                user.delete().await()
                
                signOut()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun updateProfile(profile: UserProfile, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            db.collection("users").document(uid).set(profile, SetOptions.merge())
                .addOnSuccessListener {
                    _userProfile.value = profile
                    onComplete(true, null)
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
        }
    }

    fun signInWithGoogle(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun createGoogleUserProfile(profile: UserProfile, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val finalProfile = if (profile.email == "bibiniitech@gmail.com") {
            profile.copy(isAdmin = true)
        } else {
            profile
        }
        db.collection("users").document(uid).set(finalProfile)
            .addOnSuccessListener {
                _userProfile.value = finalProfile
                _isProfileComplete.value = true
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }

    fun updateEmail(newEmail: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        user.verifyBeforeUpdateEmail(newEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun updatePassword(newPassword: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onPasswordFailure(onComplete, task.exception?.message)
                }
            }
    }

    private fun onPasswordFailure(onComplete: (Boolean, String?) -> Unit, message: String?) {
        onComplete(false, message)
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }
}
