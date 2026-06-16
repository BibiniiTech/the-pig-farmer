package com.example.smartswine.ui.market

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.ui.feed.FeedViewModel
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource
import kotlinx.coroutines.launch
import com.example.smartswine.ui.training.TrainingViewModel
import com.example.smartswine.model.TrainingVideo
import com.example.smartswine.ui.training.resolveVideoTitle
import com.example.smartswine.ui.theme.SmartSwineTheme

@Composable
fun AdminPanelScreen(
    marketViewModel: MarketViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    feedViewModel: FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    trainingViewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    val allSuggestions by marketViewModel.allSuggestions.collectAsStateWithLifecycle()
    val isMarketLoading by marketViewModel.isLoading.collectAsStateWithLifecycle()
    val marketError by marketViewModel.error.collectAsStateWithLifecycle()

    val videos by trainingViewModel.videos.collectAsStateWithLifecycle()
    val isVideosLoading by trainingViewModel.isLoading.collectAsStateWithLifecycle()
    val videosError by trainingViewModel.error.collectAsStateWithLifecycle()

    val globalIngredients by feedViewModel.globalIngredients.collectAsStateWithLifecycle()
    val isFeedLoading by feedViewModel.isLoading.collectAsStateWithLifecycle()

    val providers by marketViewModel.providers.collectAsStateWithLifecycle()

    // Initial triggers
    LaunchedEffect(Unit) {
        marketViewModel.fetchAllSuggestions()
        feedViewModel.loadGlobalIngredients()
        trainingViewModel.fetchVideos()
    }

    AdminPanelContent(
        allSuggestions = allSuggestions,
        isMarketLoading = isMarketLoading,
        marketError = marketError,
        videos = videos,
        isVideosLoading = isVideosLoading,
        videosError = videosError,
        globalIngredients = globalIngredients,
        isFeedLoading = isFeedLoading,
        providers = providers,
        onBack = onBack,
        onApproveSuggestion = marketViewModel::approveSuggestion,
        onRejectSuggestion = marketViewModel::rejectSuggestion,
        onUpdateSuggestion = marketViewModel::updateSuggestion,
        onDeleteSuggestion = marketViewModel::deleteSuggestion,
        onUpdateProvider = marketViewModel::updateProvider,
        onDeleteProvider = marketViewModel::deleteProvider,
        onAddGlobalIngredient = feedViewModel::addGlobalIngredient,
        onUpdateGlobalIngredient = feedViewModel::updateGlobalIngredient,
        onDeleteGlobalIngredient = feedViewModel::deleteGlobalIngredient,
        onAddVideo = trainingViewModel::addVideo,
        onUpdateVideo = trainingViewModel::updateVideo,
        onDeleteVideo = trainingViewModel::deleteVideo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelContent(
    allSuggestions: List<Suggestion> = emptyList(),
    isMarketLoading: Boolean = false,
    marketError: String? = null,
    videos: List<TrainingVideo> = emptyList(),
    isVideosLoading: Boolean = false,
    videosError: String? = null,
    globalIngredients: List<FeedIngredient> = emptyList(),
    isFeedLoading: Boolean = false,
    providers: List<ProviderListing> = emptyList(),
    onBack: () -> Unit = {},
    onApproveSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onRejectSuggestion: (Suggestion, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onUpdateSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onUpdateProvider: (ProviderListing, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteProvider: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onAddGlobalIngredient: (FeedIngredient, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onUpdateGlobalIngredient: (FeedIngredient, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteGlobalIngredient: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onAddVideo: (String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onUpdateVideo: (TrainingVideo, String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _, _ -> },
    onDeleteVideo: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Master Tab state: 0 = Provider Suggestions, 1 = Ingredients Manager, 2 = Video Tutorials
    var activeMasterTab by remember { mutableIntStateOf(0) }

    // Sub-states for Provider Suggestions collapsible cards
    var pendingExpanded by remember { mutableStateOf(true) }
    var approvedExpanded by remember { mutableStateOf(false) }
    var rejectedExpanded by remember { mutableStateOf(false) }
    var existingExpanded by remember { mutableStateOf(false) }

    // Dialogue/Sheet states for Suggestions
    var suggestionToReject by remember { mutableStateOf<Suggestion?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var suggestionToEdit by remember { mutableStateOf<Suggestion?>(null) }
    var suggestionToDelete by remember { mutableStateOf<Suggestion?>(null) }

    // Dialogue/Sheet states for Providers
    var providerToEdit by remember { mutableStateOf<ProviderListing?>(null) }
    var providerToDelete by remember { mutableStateOf<ProviderListing?>(null) }

    // Dialogue/Sheet states for Ingredients
    var ingredientToEdit by remember { mutableStateOf<FeedIngredient?>(null) }
    var ingredientToDelete by remember { mutableStateOf<FeedIngredient?>(null) }
    var showAddIngredientDialog by remember { mutableStateOf(false) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Dialogue/Sheet states for Videos
    var videoToEdit by remember { mutableStateOf<TrainingVideo?>(null) }
    var videoToDelete by remember { mutableStateOf<TrainingVideo?>(null) }
    var showAddVideoDialog by remember { mutableStateOf(false) }

    // Search query for Ingredients
    var ingredientSearchQuery by remember { mutableStateOf("") }

    // --- REJECTION DIALOG ---
    if (suggestionToReject != null) {
        AlertDialog(
            onDismissRequest = { suggestionToReject = null },
            title = { Text("Reject Suggestion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter feedback / reason for rejection:")
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectSuggestion(suggestionToReject!!, rejectionReason) { success, err ->
                            if (success) {
                                suggestionToReject = null
                                rejectionReason = ""
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error rejecting: $err")
                                }
                            }
                        }
                    },
                    enabled = rejectionReason.isNotBlank()
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    suggestionToReject = null
                    rejectionReason = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DELETE SUGGESTION DIALOG ---
    if (suggestionToDelete != null) {
        AlertDialog(
            onDismissRequest = { suggestionToDelete = null },
            title = { Text("Delete Suggestion") },
            text = { Text("Are you sure you want to permanently delete '${suggestionToDelete?.providerName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSuggestion(suggestionToDelete!!) { success, err ->
                            if (success) {
                                suggestionToDelete = null
                                scope.launch { snackbarHostState.showSnackbar("Suggestion deleted successfully.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error deleting: $err") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { suggestionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- EDIT SUGGESTION DIALOG ---
    if (suggestionToEdit != null) {
        var name by remember { mutableStateOf(suggestionToEdit!!.providerName) }
        var serviceType by remember { mutableStateOf(suggestionToEdit!!.serviceType) }
        var contact by remember { mutableStateOf(suggestionToEdit!!.contact) }
        var email by remember { mutableStateOf(suggestionToEdit!!.email) }
        var city by remember { mutableStateOf(suggestionToEdit!!.city) }
        var country by remember { mutableStateOf(suggestionToEdit!!.country) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        val services = listOf("Butcher", "Meat Processor", "Abattoir", "Feed Supplier", "Tools Supplier", "Vet Shop", "Vet Services", "Other")

        AlertDialog(
            onDismissRequest = { suggestionToEdit = null },
            title = { Text("Edit Suggestion Details") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Provider Name") }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = serviceType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            services.forEach { service ->
                                DropdownMenuItem(
                                    text = { Text(service) },
                                    onClick = {
                                        serviceType = service
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City / Town") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = suggestionToEdit!!.copy(
                            providerName = name,
                            serviceType = serviceType,
                            contact = contact,
                            email = email,
                            city = city,
                            country = country
                        )
                        onUpdateSuggestion(updated) { success, err ->
                            if (success) {
                                suggestionToEdit = null
                                scope.launch { snackbarHostState.showSnackbar("Suggestion updated.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error updating: $err") }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && serviceType.isNotBlank() && contact.isNotBlank() && country.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { suggestionToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- PROVIDER EDIT DIALOG ---
    if (providerToEdit != null) {
        var name by remember { mutableStateOf(providerToEdit!!.name) }
        var category by remember { mutableStateOf(providerToEdit!!.category) }
        var contact by remember { mutableStateOf(providerToEdit!!.contact) }
        var email by remember { mutableStateOf(providerToEdit!!.email) }
        var location by remember { mutableStateOf(providerToEdit!!.location) }
        var description by remember { mutableStateOf(providerToEdit!!.description) }
        var country by remember { mutableStateOf(providerToEdit!!.country) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        val providerCategories = listOf("vendors", "buyers", "vets")

        AlertDialog(
            onDismissRequest = { providerToEdit = null },
            title = { Text("Edit Provider Listing") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        val categoryDisplay = when (category.lowercase()) {
                            "vendors" -> "Vendors"
                            "buyers" -> "Buyers"
                            "vets" -> "Vets"
                            else -> category
                        }
                        OutlinedTextField(
                            value = categoryDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            providerCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        val disp = when (cat) {
                                            "vendors" -> "Vendors"
                                            "buyers" -> "Buyers"
                                            "vets" -> "Vets"
                                            else -> cat
                                        }
                                        Text(disp)
                                    },
                                    onClick = {
                                        category = cat
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = providerToEdit!!.copy(
                            name = name,
                            category = category.lowercase(),
                            contact = contact,
                            email = email,
                            location = location,
                            description = description,
                            country = country
                        )
                        onUpdateProvider(updated) { success, err ->
                            if (success) {
                                providerToEdit = null
                                scope.launch { snackbarHostState.showSnackbar("Provider listing updated.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error updating: $err") }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && category.isNotBlank() && contact.isNotBlank() && location.isNotBlank() && country.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- PROVIDER DELETE DIALOG ---
    if (providerToDelete != null) {
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text("Delete Provider Listing") },
            text = { Text("Are you sure you want to permanently delete '${providerToDelete?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProvider(providerToDelete!!.id) { success, err ->
                            if (success) {
                                providerToDelete = null
                                scope.launch { snackbarHostState.showSnackbar("Provider deleted successfully.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error deleting: $err") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- INGREDIENT DELETE DIALOG ---
    if (ingredientToDelete != null) {
        AlertDialog(
            onDismissRequest = { ingredientToDelete = null },
            title = { Text("Delete Global Ingredient") },
            text = { Text("Are you sure you want to delete '${ingredientToDelete?.name}' from the global formulation database? This will stop seeding this ingredient to new users.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGlobalIngredient(ingredientToDelete!!.id) { success, err ->
                            if (success) {
                                ingredientToDelete = null
                                scope.launch { snackbarHostState.showSnackbar("Ingredient deleted successfully.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error deleting: $err") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ingredientToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- INGREDIENT ADD/EDIT DIALOG ---
    if (showAddIngredientDialog || ingredientToEdit != null) {
        val editing = ingredientToEdit != null
        var name by remember { mutableStateOf(if (editing) ingredientToEdit!!.name else "") }
        var category by remember { mutableStateOf(if (editing) ingredientToEdit!!.mainCategory else "Energy") }
        var cp by remember { mutableStateOf(if (editing) ingredientToEdit!!.crudeProtein.toString() else "0.0") }
        var me by remember { mutableStateOf(if (editing) (ingredientToEdit!!.metabolizableEnergy / 239.0).toString() else "0.0") } // conversion factor ME
        var cf by remember { mutableStateOf(if (editing) ingredientToEdit!!.crudeFiber.toString() else "0.0") }
        var dm by remember { mutableStateOf(if (editing) ingredientToEdit!!.dryMatter.toString() else "0.0") }
        var ca by remember { mutableStateOf(if (editing) ingredientToEdit!!.calcium.toString() else "0.0") }
        var ph by remember { mutableStateOf(if (editing) ingredientToEdit!!.phosphorus.toString() else "0.0") }
        var lys by remember { mutableStateOf(if (editing) ingredientToEdit!!.lysine.toString() else "0.0") }
        var met by remember { mutableStateOf(if (editing) ingredientToEdit!!.methionine.toString() else "0.0") }
        var maxSt by remember { mutableStateOf(if (editing) ingredientToEdit!!.maxStarter.toString() else "100.0") }
        var maxGr by remember { mutableStateOf(if (editing) ingredientToEdit!!.maxGrower.toString() else "100.0") }
        var maxFi by remember { mutableStateOf(if (editing) ingredientToEdit!!.maxFinisher.toString() else "100.0") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        var showTranslations by remember { mutableStateOf(false) }
        var transFr by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["fr"] ?: "" else "") }
        var transZh by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["zh"] ?: "" else "") }
        var transEs by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["es"] ?: "" else "") }
        var transTl by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["tl"] ?: "" else "") }
        var transVi by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["vi"] ?: "" else "") }
        var transTh by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["th"] ?: "" else "") }
        var transPt by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["pt"] ?: "" else "") }
        var transHi by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["hi"] ?: "" else "") }

        val categories = listOf("Energy", "Protein", "Vitamins, Minerals & Salt")

        AlertDialog(
            onDismissRequest = {
                showAddIngredientDialog = false
                ingredientToEdit = null
            },
            title = { Text(if (editing) "Edit Feed Ingredient" else "Add Feed Ingredient") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ingredient Name") }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Main Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cp, onValueChange = { cp = it }, label = { Text("Crude Protein (%)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = me, onValueChange = { me = it }, label = { Text("ME (MJ/kg)") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cf, onValueChange = { cf = it }, label = { Text("Crude Fiber (%)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = dm, onValueChange = { dm = it }, label = { Text("Dry Matter (%)") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ca, onValueChange = { ca = it }, label = { Text("Calcium (%)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ph, onValueChange = { ph = it }, label = { Text("Phosphorus (%)") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = lys, onValueChange = { lys = it }, label = { Text("Lysine (%)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = met, onValueChange = { met = it }, label = { Text("Methionine (%)") }, modifier = Modifier.weight(1f))
                    }

                    Text("Inclusion Limits (%)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = maxSt, onValueChange = { maxSt = it }, label = { Text("Starter") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = maxGr, onValueChange = { maxGr = it }, label = { Text("Grower") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = maxFi, onValueChange = { maxFi = it }, label = { Text("Finisher") }, modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = { showTranslations = !showTranslations }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showTranslations) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Name Translations (Optional)")
                        }
                    }

                    if (showTranslations) {
                        OutlinedTextField(value = transFr, onValueChange = { transFr = it }, label = { Text("French (Français)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transZh, onValueChange = { transZh = it }, label = { Text("Chinese (中文)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transEs, onValueChange = { transEs = it }, label = { Text("Spanish (Español)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transTl, onValueChange = { transTl = it }, label = { Text("Filipino (Tagalog)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transVi, onValueChange = { transVi = it }, label = { Text("Vietnamese (Tiếng Việt)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transTh, onValueChange = { transTh = it }, label = { Text("Thai (ไทย)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transPt, onValueChange = { transPt = it }, label = { Text("Portuguese (Português)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transHi, onValueChange = { transHi = it }, label = { Text("Hindi (हिन्दी)") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedCp = cp.toDoubleOrNull() ?: 0.0
                        val parsedMe = (me.toDoubleOrNull() ?: 0.0) * 239.0 // save as kcal/kg internally
                        val parsedCf = cf.toDoubleOrNull() ?: 0.0
                        val parsedDm = dm.toDoubleOrNull() ?: 0.0
                        val parsedCa = ca.toDoubleOrNull() ?: 0.0
                        val parsedPh = ph.toDoubleOrNull() ?: 0.0
                        val parsedLys = lys.toDoubleOrNull() ?: 0.0
                        val parsedMet = met.toDoubleOrNull() ?: 0.0
                        val parsedMaxSt = maxSt.toDoubleOrNull() ?: 100.0
                        val parsedMaxGr = maxGr.toDoubleOrNull() ?: 100.0
                        val parsedMaxFi = maxFi.toDoubleOrNull() ?: 100.0

                        val translationsMap = mutableMapOf<String, String>()
                        if (transFr.isNotBlank()) translationsMap["fr"] = transFr
                        if (transZh.isNotBlank()) translationsMap["zh"] = transZh
                        if (transEs.isNotBlank()) translationsMap["es"] = transEs
                        if (transTl.isNotBlank()) translationsMap["tl"] = transTl
                        if (transVi.isNotBlank()) translationsMap["vi"] = transVi
                        if (transTh.isNotBlank()) translationsMap["th"] = transTh
                        if (transPt.isNotBlank()) translationsMap["pt"] = transPt
                        if (transHi.isNotBlank()) translationsMap["hi"] = transHi

                        val toSave = FeedIngredient(
                            id = if (editing) ingredientToEdit!!.id else "",
                            name = name,
                            mainCategory = category,
                            crudeProtein = parsedCp,
                            metabolizableEnergy = parsedMe,
                            crudeFiber = parsedCf,
                            dryMatter = parsedDm,
                            calcium = parsedCa,
                            phosphorus = parsedPh,
                            lysine = parsedLys,
                            methionine = parsedMet,
                            nameTranslations = translationsMap,
                            maxStarter = parsedMaxSt,
                            maxGrower = parsedMaxGr,
                            maxFinisher = parsedMaxFi,
                            visible = true,
                            unit = "kg"
                        )

                        if (editing) {
                            onUpdateGlobalIngredient(toSave) { success, err ->
                                if (success) {
                                    ingredientToEdit = null
                                    scope.launch { snackbarHostState.showSnackbar("Ingredient updated successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error saving: $err") }
                                }
                            }
                        } else {
                            onAddGlobalIngredient(toSave) { success, err ->
                                if (success) {
                                    showAddIngredientDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Ingredient created successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error creating: $err") }
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(if (editing) "Save Changes" else "Add Ingredient")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddIngredientDialog = false
                    ingredientToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- VIDEO ADD/EDIT DIALOG ---
    if (showAddVideoDialog || videoToEdit != null) {
        val editing = videoToEdit != null
        var title by remember { mutableStateOf(if (editing) videoToEdit!!.title else "") }
        var youtubeLink by remember { mutableStateOf(if (editing) "https://www.youtube.com/watch?v=${videoToEdit!!.youtubeId}" else "") }

        AlertDialog(
            onDismissRequest = {
                showAddVideoDialog = false
                videoToEdit = null
            },
            title = { Text(if (editing) "Edit Video Tutorial" else "Add Video Tutorial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Video Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = youtubeLink,
                        onValueChange = { youtubeLink = it },
                        label = { Text("YouTube Link or Video ID") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. https://www.youtube.com/watch?v=...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editing) {
                            onUpdateVideo(videoToEdit!!, title, youtubeLink) { success, err ->
                                if (success) {
                                    videoToEdit = null
                                    scope.launch { snackbarHostState.showSnackbar("Video updated successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                }
                            }
                        } else {
                            onAddVideo(title, youtubeLink) { success, err ->
                                if (success) {
                                    showAddVideoDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Video added successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                }
                            }
                        }
                    },
                    enabled = title.isNotBlank() && youtubeLink.isNotBlank()
                ) {
                    Text(if (editing) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddVideoDialog = false
                    videoToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- VIDEO DELETE DIALOG ---
    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video Tutorial") },
            text = { Text("Are you sure you want to permanently delete '${resolveVideoTitle(videoToDelete!!.title)}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVideo(videoToDelete!!.id) { success, err ->
                            if (success) {
                                videoToDelete = null
                                scope.launch { snackbarHostState.showSnackbar("Video deleted successfully.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error deleting: $err") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                    Text(
                        text = "ADMIN PANEL",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                // Master selection: Suggestions vs Ingredients vs Videos
                TabRow(
                    selectedTabIndex = activeMasterTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeMasterTab == 0,
                        onClick = { activeMasterTab = 0 },
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suggestions")
                        } }
                    )
                    Tab(
                        selected = activeMasterTab == 1,
                        onClick = { activeMasterTab = 1 },
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ingredients")
                        } }
                    )
                    Tab(
                        selected = activeMasterTab == 2,
                        onClick = { activeMasterTab = 2 },
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Videos")
                        } }
                    )
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (activeMasterTab == 0) {
                // --- SUGGESTIONS MODULE ---
                if (isMarketLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                if (marketError != null) {
                    Text(
                        text = "Error: $marketError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                val pendingSuggestions = allSuggestions.filter { it.status == "pending" }
                val approvedSuggestions = allSuggestions.filter { it.status == "approved" }
                val rejectedSuggestions = allSuggestions.filter { it.status == "rejected" }
                val sortedProviders = providers.sortedBy { it.name.lowercase() }

                // 1. Pending Suggestions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingExpanded = !pendingExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pending Suggestions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (pendingSuggestions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = pendingSuggestions.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (pendingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (pendingExpanded) "Collapse" else "Expand"
                            )
                        }

                        AnimatedVisibility(
                            visible = pendingExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (pendingSuggestions.isEmpty()) {
                                    Text(
                                        text = "No pending suggestions.",
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    pendingSuggestions.forEach { suggestion ->
                                        AdminSuggestionItem(
                                            suggestion = suggestion,
                                            onApprove = { 
                                                onApproveSuggestion(suggestion) { success, err ->
                                                    if (!success) {
                                                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                                    }
                                                }
                                            },
                                            onEdit = { suggestionToEdit = it },
                                            onDelete = { suggestionToDelete = it },
                                            onRejectClick = {
                                                rejectionReason = ""
                                                suggestionToReject = it
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // 2. Approved Suggestions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { approvedExpanded = !approvedExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Approved Suggestions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (approvedSuggestions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = approvedSuggestions.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (approvedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (approvedExpanded) "Collapse" else "Expand"
                            )
                        }

                        AnimatedVisibility(
                            visible = approvedExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (approvedSuggestions.isEmpty()) {
                                    Text(
                                        text = "No approved suggestions.",
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    approvedSuggestions.forEach { suggestion ->
                                        AdminSuggestionItem(
                                            suggestion = suggestion,
                                            onApprove = { 
                                                onApproveSuggestion(suggestion) { success, err ->
                                                    if (!success) {
                                                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                                    }
                                                }
                                            },
                                            onEdit = { suggestionToEdit = it },
                                            onDelete = { suggestionToDelete = it }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // 3. Rejected Suggestions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rejectedExpanded = !rejectedExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Rejected Suggestions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (rejectedSuggestions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = rejectedSuggestions.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onError,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (rejectedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (rejectedExpanded) "Collapse" else "Expand"
                            )
                        }

                        AnimatedVisibility(
                            visible = rejectedExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (rejectedSuggestions.isEmpty()) {
                                    Text(
                                        text = "No rejected suggestions.",
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    rejectedSuggestions.forEach { suggestion ->
                                        AdminSuggestionItem(
                                            suggestion = suggestion,
                                            onApprove = { 
                                                onApproveSuggestion(suggestion) { success, err ->
                                                    if (!success) {
                                                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                                    }
                                                }
                                            },
                                            onEdit = { suggestionToEdit = it },
                                            onDelete = { suggestionToDelete = it }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // 4. Existing Providers Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { existingExpanded = !existingExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Existing Directory",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (sortedProviders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = sortedProviders.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (existingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (existingExpanded) "Collapse" else "Expand"
                            )
                        }

                        AnimatedVisibility(
                            visible = existingExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (sortedProviders.isEmpty()) {
                                    Text(
                                        text = "No existing directory listings.",
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    sortedProviders.forEach { provider ->
                                        AdminProviderListingItem(
                                            provider = provider,
                                            onEdit = { providerToEdit = it },
                                            onDelete = { providerToDelete = it }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } else if (activeMasterTab == 1) {
                // --- INGREDIENTS MODULE ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ingredientSearchQuery,
                        onValueChange = { ingredientSearchQuery = it },
                        placeholder = { Text("Search Ingredients...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { showAddIngredientDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                if (isFeedLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                val filteredIngredients = globalIngredients.filter {
                    it.name.contains(ingredientSearchQuery, ignoreCase = true) ||
                    it.mainCategory.contains(ingredientSearchQuery, ignoreCase = true)
                }

                if (filteredIngredients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        Text("No global ingredients found.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    val grouped = filteredIngredients.groupBy { it.mainCategory }

                    grouped.forEach { (cat, list) ->
                        val isExpanded = expandedCategories[cat] ?: true
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedCategories[cat] = !isExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (isExpanded) {
                            val sortedList = list.sortedBy { it.name }
                            sortedList.forEach { ing ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = ing.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Row {
                                                IconButton(onClick = { ingredientToEdit = ing }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { ingredientToDelete = ing }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }

                                        // Nutritional breakdown
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = "Protein: ${ing.crudeProtein}%", style = MaterialTheme.typography.bodySmall)
                                                Text(text = "ME: ${(ing.metabolizableEnergy / 239.0).formatDecimal(2)} MJ/kg", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = "Lysine: ${ing.lysine}%", style = MaterialTheme.typography.bodySmall)
                                                Text(text = "Methionine: ${ing.methionine}%", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- VIDEOS MODULE ---
                var videoSearchQuery by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = videoSearchQuery,
                        onValueChange = { videoSearchQuery = it },
                        placeholder = { Text("Search Videos...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { showAddVideoDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                if (isVideosLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                if (videosError != null) {
                    Text(
                        text = "Error: $videosError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                val filteredVideos = videos.filter {
                    resolveVideoTitle(it.title).contains(videoSearchQuery, ignoreCase = true) ||
                    it.youtubeId.contains(videoSearchQuery, ignoreCase = true)
                }

                if (filteredVideos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        Text("No video tutorials found.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    filteredVideos.forEach { video ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = resolveVideoTitle(video.title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "YouTube ID: ${video.youtubeId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Row {
                                    IconButton(onClick = { videoToEdit = video }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { videoToDelete = video }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// Utility to format decimals cleanly in Compose
private fun Double.formatDecimal(digits: Int): String {
    return String.format("%.${digits}f", this)
}

@Composable
private fun AdminSuggestionItem(
    suggestion: Suggestion,
    onApprove: () -> Unit,
    onEdit: (Suggestion) -> Unit,
    onDelete: (Suggestion) -> Unit,
    onRejectClick: ((Suggestion) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion.providerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = suggestion.serviceType,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(text = "Suggested by: ${suggestion.userId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text(text = "Contact: ${suggestion.contact}", style = MaterialTheme.typography.bodySmall)
            if (suggestion.email.isNotBlank()) {
                Text(text = "Email: ${suggestion.email}", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "Location: ${suggestion.city}, ${suggestion.country}", style = MaterialTheme.typography.bodySmall)
            if (suggestion.status == "rejected" && suggestion.adminFeedback.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Rejection Reason: ${suggestion.adminFeedback}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            StylishDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (suggestion.status == "pending") {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }

                    Button(
                        onClick = {
                            onRejectClick?.invoke(suggestion)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(
                    onClick = { onEdit(suggestion) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { onDelete(suggestion) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AdminProviderListingItem(
    provider: ProviderListing,
    onEdit: (ProviderListing) -> Unit,
    onDelete: (ProviderListing) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val categoryDisplay = when (provider.category.lowercase()) {
                    "vendors" -> "Vendors"
                    "buyers" -> "Buyers"
                    "vets" -> "Vets"
                    else -> provider.category.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = categoryDisplay,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (provider.description.isNotBlank()) {
                Text(text = provider.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = "Contact: ${provider.contact}", style = MaterialTheme.typography.bodySmall)
            if (provider.email.isNotBlank()) {
                Text(text = "Email: ${provider.email}", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "Location: ${provider.location}", style = MaterialTheme.typography.bodySmall)
            if (provider.country.isNotBlank()) {
                Text(text = "Country: ${provider.country}", style = MaterialTheme.typography.bodySmall)
            }

            StylishDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onEdit(provider) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { onDelete(provider) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminPanelScreenPreview() {
    SmartSwineTheme {
        AdminPanelContent(
            allSuggestions = listOf(
                Suggestion(id = "1", providerName = "Best Feed Supplier", serviceType = "Feed Supplier", status = "pending"),
                Suggestion(id = "2", providerName = "City Butcher", serviceType = "Butcher", status = "approved"),
                Suggestion(id = "3", providerName = "Old Tools", serviceType = "Tools Supplier", status = "rejected", adminFeedback = "Inaccurate info")
            ),
            globalIngredients = listOf(
                FeedIngredient(id = "1", name = "Yellow Maize", crudeProtein = 8.5, metabolizableEnergy = 3300.0, mainCategory = "Energy"),
                FeedIngredient(id = "2", name = "Soybean Meal", crudeProtein = 44.0, metabolizableEnergy = 2200.0, mainCategory = "Protein")
            ),
            videos = listOf(
                TrainingVideo(id = "1", title = "Pig Farming 101", youtubeId = "q_v_tYp6V8M"),
                TrainingVideo(id = "2", title = "Advanced Nutrition", youtubeId = "d6p-T8S8pS0")
            )
        )
    }
}
