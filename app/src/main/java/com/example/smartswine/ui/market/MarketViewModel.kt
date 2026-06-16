package com.example.smartswine.ui.market

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Keep
data class ProviderListing(
    val id: String = "",
    val name: String = "",
    val contact: String = "",
    val email: String = "",
    val location: String = "",
    val description: String = "",
    val isVerified: Boolean = true,
    val category: String = "", // "vendors", "buyers", "vets"
    val country: String = "",
    val createdAt: Long = 0L
)

@Keep
data class Suggestion(
    val id: String = "",
    val userId: String = "",
    val providerName: String = "",
    val serviceType: String = "",
    val contact: String = "",
    val email: String = "",
    val city: String = "",
    val country: String = "",
    val timestamp: Timestamp? = null,
    val status: String = "pending", // "pending", "approved", "rejected"
    val adminFeedback: String = ""
)

class MarketViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _providers = MutableStateFlow<List<ProviderListing>>(emptyList())
    val providers: StateFlow<List<ProviderListing>> = _providers.asStateFlow()

    private val _mySuggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val mySuggestions: StateFlow<List<Suggestion>> = _mySuggestions.asStateFlow()

    private val _allSuggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val allSuggestions: StateFlow<List<Suggestion>> = _allSuggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var providersListener: ListenerRegistration? = null
    private var mySuggestionsListener: ListenerRegistration? = null
    private var allSuggestionsListener: ListenerRegistration? = null

    init {
        fetchProviders()
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            fetchMySuggestions(currentUserId)
        }
    }

    fun fetchProviders() {
        _isLoading.value = true
        providersListener?.remove()
        providersListener = db.collection("market_providers")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    _error.value = error.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ProviderListing::class.java)
                    if (list.isEmpty()) {
                        seedDefaultProviders()
                    } else {
                        _providers.value = list.sortedByDescending { it.createdAt }
                    }
                }
            }
    }

    fun fetchMySuggestions(userId: String) {
        mySuggestionsListener?.remove()
        mySuggestionsListener = db.collection("market_suggestions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Suggestion::class.java)?.copy(id = doc.id)
                    }
                    _mySuggestions.value = list.sortedByDescending { it.timestamp }
                }
            }
    }

    fun fetchAllSuggestions() {
        allSuggestionsListener?.remove()
        allSuggestionsListener = db.collection("market_suggestions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Suggestion::class.java)?.copy(id = doc.id)
                    }
                    _allSuggestions.value = list.sortedByDescending { it.timestamp }
                }
            }
    }

    fun submitSuggestion(
        name: String,
        serviceType: String,
        contact: String,
        email: String,
        city: String,
        country: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        val ref = db.collection("market_suggestions").document()
        val newSuggestion = Suggestion(
            id = ref.id,
            userId = userId,
            providerName = name,
            serviceType = serviceType,
            contact = contact,
            email = email,
            city = city,
            country = country,
            timestamp = Timestamp.now(),
            status = "pending",
            adminFeedback = ""
        )

        viewModelScope.launch {
            try {
                // Verify duplicate
                val lowercaseName = name.trim().lowercase()
                val isDuplicate = _providers.value.any { 
                    it.name.trim().lowercase() == lowercaseName && it.contact == contact 
                } || _mySuggestions.value.any {
                    it.providerName.trim().lowercase() == lowercaseName && it.contact == contact
                }
                if (isDuplicate) {
                    onComplete(false, "duplicate")
                    return@launch
                }

                ref.set(newSuggestion).await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun approveSuggestion(suggestion: Suggestion, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val providerRef = db.collection("market_providers").document()
                val provider = ProviderListing(
                    id = providerRef.id,
                    name = suggestion.providerName,
                    contact = suggestion.contact,
                    email = suggestion.email,
                    location = if (suggestion.city.isNotBlank()) "${suggestion.city}, ${suggestion.country}" else suggestion.country,
                    description = "Suggested ${suggestion.serviceType} from community.",
                    isVerified = true,
                    category = when (suggestion.serviceType.lowercase()) {
                        "butcher", "meat_processor", "abattoir", "meat processor" -> "buyers"
                        "vet_shop", "vet_services", "vet services", "vet shop" -> "vets"
                        else -> "vendors" // default fallback
                    },
                    country = suggestion.country,
                    createdAt = System.currentTimeMillis()
                )

                // Transaction to ensure atomic write
                db.runTransaction { transaction ->
                    transaction.set(providerRef, provider)
                    transaction.update(db.collection("market_suggestions").document(suggestion.id), "status", "approved")
                }.await()

                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun rejectSuggestion(suggestion: Suggestion, reason: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                db.collection("market_suggestions").document(suggestion.id).update(
                    mapOf(
                        "status" to "rejected",
                        "adminFeedback" to reason
                    )
                ).await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun updateSuggestion(suggestion: Suggestion, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                db.collection("market_suggestions").document(suggestion.id).set(suggestion).await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun deleteSuggestion(suggestion: Suggestion, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                db.collection("market_suggestions").document(suggestion.id).delete().await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun updateProvider(provider: ProviderListing, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                db.collection("market_providers").document(provider.id).set(provider).await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun deleteProvider(providerId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                db.collection("market_providers").document(providerId).delete().await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    private fun seedDefaultProviders() {
        viewModelScope.launch {
            try {
                val batch = db.batch()
                val seedData = getSeedData()
                seedData.forEach { provider ->
                    val docRef = db.collection("market_providers").document()
                    batch.set(docRef, provider.copy(id = docRef.id))
                }
                batch.commit().await()
            } catch (_: Exception) {
                // Ignore seed failure, fallback to empty
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        providersListener?.remove()
        mySuggestionsListener?.remove()
        allSuggestionsListener?.remove()
    }

    private fun getSeedData(): List<ProviderListing> {
        val list = mutableListOf<ProviderListing>()
        val timestamp = System.currentTimeMillis()

        // Ghana
        list.add(ProviderListing(name = "Agricare Feed Mills", contact = "+233244123456", email = "agricare@feed.gh", location = "Kumasi, Ghana", description = "High-quality custom swine feed formulations, concentrates, and raw materials.", category = "vendors", country = "Ghana", createdAt = timestamp))
        list.add(ProviderListing(name = "Koudijs Ghana", contact = "+233302908274", email = "info@koudijs.com.gh", location = "Tema, Ghana", description = "Premium imported concentrates, piglet prestarters, and expert nutritional advice.", category = "vendors", country = "Ghana", createdAt = timestamp))
        list.add(ProviderListing(name = "PorkCity Ghana", contact = "+233208812345", email = "sales@porkcity.gh", location = "Accra, Ghana", description = "Leading buyer of commercial weight pigs. Quick payment and transport services.", category = "buyers", country = "Ghana", createdAt = timestamp))
        list.add(ProviderListing(name = "Bodwesango Abattoir", contact = "+233551239876", email = "bodwesango@abattoir.gh", location = "Ashanti Region, Ghana", description = "Licensed processing facility offering competitive wholesale prices for live pigs.", category = "buyers", country = "Ghana", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Emmanuel Mensah (Swine Vet)", contact = "+233243987654", email = "e.mensah@vetghana.org", location = "Accra/Eastern Region, Ghana", description = "Mobile swine specialist. Herd health plans, disease treatment, and vaccination protocols.", category = "vets", country = "Ghana", createdAt = timestamp))
        list.add(ProviderListing(name = "Vetafrica Consult", contact = "+233201122334", email = "consult@vetafrica.gh", location = "Sunyani, Ghana", description = "Diagnostic services, post-mortem examinations, and farm biosecurity audits.", category = "vets", country = "Ghana", createdAt = timestamp))

        // Uganda
        list.add(ProviderListing(name = "Nalweyo Seed Company (NASECO)", contact = "+256772123456", email = "info@naseco.co.ug", location = "Kampala, Uganda", description = "Certified high-yield maize bran and animal feed ingredients.", category = "vendors", country = "Uganda", createdAt = timestamp))
        list.add(ProviderListing(name = "Koudijs Uganda", contact = "+256701987654", email = "sales@koudijs.co.ug", location = "Mukono, Uganda", description = "Premium concentrates, pig feed starters, and nutritional consulting.", category = "vendors", country = "Uganda", createdAt = timestamp))
        list.add(ProviderListing(name = "Wambizi Cooperative Abattoir", contact = "+256782555123", email = "info@wambizi.org", location = "Nalukolongo, Kampala, Uganda", description = "Uganda's largest licensed pig slaughterhouse and wholesale buyer.", category = "buyers", country = "Uganda", createdAt = timestamp))
        list.add(ProviderListing(name = "FreshCuts Uganda", contact = "+256414250320", email = "orders@freshcuts.co.ug", location = "Kampala, Uganda", description = "Major meat processor purchasing premium baconers and porkers.", category = "buyers", country = "Uganda", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Charles Ssewanyana", contact = "+256774444321", email = "charles@swinevet.ug", location = "Wakiso, Uganda", description = "Expert swine clinician specializing in African Swine Fever control and herd management.", category = "vets", country = "Uganda", createdAt = timestamp))
        list.add(ProviderListing(name = "Makerere Swine Health Clinic", contact = "+256752999888", email = "swineclinic@covab.mak.ac.ug", location = "Kampala, Uganda", description = "University-backed diagnostic lab and clinical outreach service for pig farmers.", category = "vets", country = "Uganda", createdAt = timestamp))

        // Kenya
        list.add(ProviderListing(name = "Ungama Feeds Ltd", contact = "+254722000111", email = "info@ungamafeeds.co.ke", location = "Nairobi, Kenya", description = "Balanced pig feeds from weaner to sow. High quality standards.", category = "vendors", country = "Kenya", createdAt = timestamp))
        list.add(ProviderListing(name = "Sigma Feeds", contact = "+254733999000", email = "sales@sigmafeeds.com", location = "Thika, Kenya", description = "Certified swine feeds, vitamins, and premixes.", category = "vendors", country = "Kenya", createdAt = timestamp))
        list.add(ProviderListing(name = "Farmers Choice Ltd", contact = "+254722300900", email = "porkbuyer@farmerschoice.co.ke", location = "Nairobi, Kenya", description = "The largest commercial pork processor in Kenya. Reliable buyer of quality pigs.", category = "buyers", country = "Kenya", createdAt = timestamp))
        list.add(ProviderListing(name = "Quality Meat Packers", contact = "+25420354000", email = "buyers@qmp.co.ke", location = "Ruai, Nairobi, Kenya", description = "Modern abattoir purchasing porkers and sows at competitive rates.", category = "buyers", country = "Kenya", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Joseph Kiprop", contact = "+254711222333", email = "kiprop@kenya-swinevets.co.ke", location = "Eldoret, Kenya", description = "Specialized swine health consultant, vaccination, and biosecurity setups.", category = "vets", country = "Kenya", createdAt = timestamp))
        list.add(ProviderListing(name = "Rift Valley Vet Lab", contact = "+254720444555", email = "rvlab@nakuru.go.ke", location = "Nakuru, Kenya", description = "Government-backed diagnostic clinic offering swift disease testing.", category = "vets", country = "Kenya", createdAt = timestamp))

        // Colombia
        list.add(ProviderListing(name = "Nutresa Alimentos", contact = "+576043210000", email = "contacto@nutresa.com.co", location = "Medellín, Colombia", description = "Balanced commercial feed concentrates and premium genetic breeders.", category = "vendors", country = "Colombia", createdAt = timestamp))
        list.add(ProviderListing(name = "Contegral", contact = "+576014440050", email = "ventas@contegral.co", location = "Bogotá, Colombia", description = "High-quality swine feeds and nutritional consulting for farms.", category = "vendors", country = "Colombia", createdAt = timestamp))
        list.add(ProviderListing(name = "Colanta Pork Division", contact = "+576044441111", email = "compra.cerdos@colanta.com.co", location = "Antioquia, Colombia", description = "Leading dairy and pork cooperative purchasing slaughter-ready pigs.", category = "buyers", country = "Colombia", createdAt = timestamp))
        list.add(ProviderListing(name = "Frigorífico Guadalupe", contact = "+576013704040", email = "info@frigoguadalupe.com", location = "Bogotá, Colombia", description = "Certified national abattoir and pork processor.", category = "buyers", country = "Colombia", createdAt = timestamp))
        list.add(ProviderListing(name = "Dra. Maria Camila Gomez", contact = "+573124567890", email = "mariac@asoporcicultores.co", location = "Cali, Colombia", description = "Expert swine reproduction and herd health veterinarian.", category = "vets", country = "Colombia", createdAt = timestamp))
        list.add(ProviderListing(name = "Laboratorio Vecol", contact = "+576014235700", email = "servicio.cliente@vecol.com.co", location = "Bogotá, Colombia", description = "Leading manufacturer of veterinary pharmaceuticals and diagnostic services.", category = "vets", country = "Colombia", createdAt = timestamp))

        // Brazil
        list.add(ProviderListing(name = "Rações Presence", contact = "+551938818000", email = "contato@presence.com.br", location = "Campinas, SP, Brazil", description = "High-performance pig feeds and concentrates for all production stages.", category = "vendors", country = "Brazil", createdAt = timestamp))
        list.add(ProviderListing(name = "Topigs Norsvin Brasil", contact = "+554133028900", email = "brasil@topigsnorsvin.com.br", location = "Curitiba, PR, Brazil", description = "World-class swine genetics, terminal boars, and replacement gilts.", category = "vendors", country = "Brazil", createdAt = timestamp))
        list.add(ProviderListing(name = "Seara Alimentos", contact = "+551131444000", email = "compra.suinos@seara.com.br", location = "Chapecó, SC, Brazil", description = "Major meat packer buying commercial weight finishing pigs.", category = "buyers", country = "Brazil", createdAt = timestamp))
        list.add(ProviderListing(name = "Frimesa Cooperativa", contact = "+554532648100", email = "suinos@frimesa.com.br", location = "Medianeira, PR, Brazil", description = "Large cooperative meat plant with high-capacity slaughterhouse.", category = "buyers", country = "Brazil", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Rodrigo Silva", contact = "+5549999123456", email = "rodrigo.vet@suinohealth.com.br", location = "Concórdia, SC, Brazil", description = "Clinical veterinarian specializing in swine respiratory complex and biosecurity.", category = "vets", country = "Brazil", createdAt = timestamp))
        list.add(ProviderListing(name = "LabTec Suínos UFRGS", contact = "+555133086123", email = "labtecsuinos@ufrgs.br", location = "Porto Alegre, RS, Brazil", description = "University reference laboratory for diagnostic testing and pathology.", category = "vets", country = "Brazil", createdAt = timestamp))

        // Philippines
        list.add(ProviderListing(name = "B-MEG Feeds", contact = "+63286322000", email = "bmegfeeds@sanmiguel.com.ph", location = "Manila, Philippines", description = "Most trusted animal feed brand in the Philippines. Complete swine feeds.", category = "vendors", country = "Philippines", createdAt = timestamp))
        list.add(ProviderListing(name = "Pilmico Foods Corp", contact = "+63288862800", email = "customer.service@pilmico.com", location = "Cebu, Philippines", description = "Premium swine feeds, concentrates, and technical breeding support.", category = "vendors", country = "Philippines", createdAt = timestamp))
        list.add(ProviderListing(name = "Jaka Distribution / Slaughterhouse", contact = "+63288100756", email = "jaka@slaughter.com.ph", location = "Bulacan, Philippines", description = "Major wholesale hog buyer and certified slaughterhouse.", category = "buyers", country = "Philippines", createdAt = timestamp))
        list.add(ProviderListing(name = "Monterey Meatshop (San Miguel)", contact = "+63286323000", email = "monterey@sanmiguel.com.ph", location = "Cavite, Philippines", description = "Leading commercial pork buyer with nationwide distribution networks.", category = "buyers", country = "Philippines", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Raymond Cruz", contact = "+639178881234", email = "raymondcruz@pvs.org.ph", location = "Pampanga, Philippines", description = "Swine practitioner specializing in PRRS eradication and farm biosecurity.", category = "vets", country = "Philippines", createdAt = timestamp))
        list.add(ProviderListing(name = "BAI Swine Disease Diagnostic Lab", contact = "+63289282836", email = "ahwd@bai.gov.ph", location = "Quezon City, Philippines", description = "Bureau of Animal Industry reference lab for hog cholera and ASF testing.", category = "vets", country = "Philippines", createdAt = timestamp))

        // Vietnam
        list.add(ProviderListing(name = "CP Vietnam Corporation", contact = "+842513836251", email = "info@cp.com.vn", location = "Dong Nai, Vietnam", description = "Premium commercial feeds and high-performance hybrid breeders.", category = "vendors", country = "Vietnam", createdAt = timestamp))
        list.add(ProviderListing(name = "Cargill Vietnam", contact = "+842838213467", email = "vietnam_feeds@cargill.com", location = "Binh Duong, Vietnam", description = "World-class nutritional concentrates and complete swine rations.", category = "vendors", country = "Vietnam", createdAt = timestamp))
        list.add(ProviderListing(name = "Vissan Joint Stock Company", contact = "+842838294567", email = "vissan@vissan.com.vn", location = "Ho Chi Minh City, Vietnam", description = "Major meat packer and national distributor purchasing high-quality porkers.", category = "buyers", country = "Vietnam", createdAt = timestamp))
        list.add(ProviderListing(name = "Masan MEATDeli", contact = "+842437181234", email = "info@meatdeli.com.vn", location = "Ha Nam, Vietnam", description = "State-of-the-art chilled meat processor buying commercial hogs.", category = "buyers", country = "Vietnam", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Nguyen Van Hai", contact = "+84903123456", email = "hai.nv@vietnamvet.vn", location = "Dong Nai, Vietnam", description = "Specialist in vaccination programs and prevention of porcine epidemic diarrhea.", category = "vets", country = "Vietnam", createdAt = timestamp))
        list.add(ProviderListing(name = "NAVETCO Lab", contact = "+842838225063", email = "navetco@navetco.com.vn", location = "Ho Chi Minh City, Vietnam", description = "National veterinary diagnostic and vaccine manufacturing reference laboratory.", category = "vets", country = "Vietnam", createdAt = timestamp))

        // India
        list.add(ProviderListing(name = "Godrej Agrovet Ltd", contact = "+912225188010", email = "pigfeeds@godrejagrovet.com", location = "Assam/Northeast India", description = "Leading pig feed manufacturer providing scientific formulations.", category = "vendors", country = "India", createdAt = timestamp))
        list.add(ProviderListing(name = "Kargil Animal Nutrition", contact = "+911244009000", email = "india_nutrition@cargill.com", location = "Punjab, India", description = "Nutritious creep feeds and grower concentrates for commercial pig farms.", category = "vendors", country = "India", createdAt = timestamp))
        list.add(ProviderListing(name = "Northeast Pork Producers Coop", contact = "+913612459876", email = "neporkcoop@gmail.com", location = "Guwahati, Assam, India", description = "Cooperative buyer aggregating live hogs for local and regional markets.", category = "buyers", country = "India", createdAt = timestamp))
        list.add(ProviderListing(name = "Arohan Foods", contact = "+919435012345", email = "procurement@arohanfoods.com", location = "Guwahati, India", description = "India's first commercial pork processor purchasing farm pigs.", category = "buyers", country = "India", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Rajeev Barua", contact = "+919864055555", email = "rbarua@nrcp.org.in", location = "Guwahati, Assam, India", description = "Swine health specialist focusing on Classical Swine Fever vaccinations.", category = "vets", country = "India", createdAt = timestamp))
        list.add(ProviderListing(name = "ICAR-National Research Centre on Pig", contact = "+913612847221", email = "nrconpig@icar.gov.in", location = "Rani, Guwahati, India", description = "Premier central research institute for swine disease diagnostics and support.", category = "vets", country = "India", createdAt = timestamp))

        // China
        list.add(ProviderListing(name = "New Hope Liuhe Co., Ltd.", contact = "+861053218888", email = "feed@newhope.cn", location = "Sichuan, China", description = "World's leading feed manufacturer offering specialized swine diets.", category = "vendors", country = "China", createdAt = timestamp))
        list.add(ProviderListing(name = "Muyuan Foods Genetics", contact = "+863776599888", email = "Muyuan@muyuan.cn", location = "Henan, China", description = "Premium breeding stock, terminal boars, and high-health gilts.", category = "vendors", country = "China", createdAt = timestamp))
        list.add(ProviderListing(name = "WH Group / Shuanghui Development", contact = "+863952626262", email = "purchasing@shuanghui.net", location = "Luohe, Henan, China", description = "World's largest pork processor purchasing high-quality live market hogs.", category = "buyers", country = "China", createdAt = timestamp))
        list.add(ProviderListing(name = "Wens Foodstuff Group", contact = "+867662291111", email = "caigou@wens.com.cn", location = "Guangdong, China", description = "Major agricultural group buying commercial porkers at market index rates.", category = "buyers", country = "China", createdAt = timestamp))
        list.add(ProviderListing(name = "Dr. Li Wei", contact = "+8613912345678", email = "li.wei@caas.cn", location = "Harbin, China", description = "Swine medicine researcher specializing in biosafety and ASF prevention.", category = "vets", country = "China", createdAt = timestamp))
        list.add(ProviderListing(name = "Harbin Veterinary Research Institute (HVRI)", contact = "+8645151997166", email = "hvri@caas.cn", location = "Harbin, Heilongjiang, China", description = "National reference lab for swine viral diseases and molecular diagnostics.", category = "vets", country = "China", createdAt = timestamp))

        return list
    }
}
