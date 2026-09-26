package com.example.smartswine.ui.market.admin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartswine.ui.market.ProviderListing
import com.example.smartswine.ui.market.Suggestion
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSuggestionsTab(
    allSuggestions: List<Suggestion>,
    providers: List<ProviderListing>,
    isMarketLoading: Boolean,
    marketError: String?,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onApproveSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit,
    onRejectSuggestion: (Suggestion, String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit,
    onDeleteSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit,
    onUpdateProvider: (ProviderListing, (Boolean, String?) -> Unit) -> Unit,
    onDeleteProvider: (String, (Boolean, String?) -> Unit) -> Unit
) {
    // Collapsible states
    var pendingExpanded by remember { mutableStateOf(true) }
    var approvedExpanded by remember { mutableStateOf(false) }
    var rejectedExpanded by remember { mutableStateOf(false) }
    var existingExpanded by remember { mutableStateOf(false) }

    // Dialog states
    var suggestionToReject by remember { mutableStateOf<Suggestion?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var suggestionToEdit by remember { mutableStateOf<Suggestion?>(null) }
    var suggestionToDelete by remember { mutableStateOf<Suggestion?>(null) }
    var providerToEdit by remember { mutableStateOf<ProviderListing?>(null) }
    var providerToDelete by remember { mutableStateOf<ProviderListing?>(null) }
    val expandedCountries = remember { mutableStateMapOf<String, Boolean>() }

    // --- REJECTION DIALOG ---
    if (suggestionToReject != null) {
        AlertDialog(
            onDismissRequest = { suggestionToReject = null },
            title = { Text(stringResource("reject_suggestion")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource("enter_rejection_reason"))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text(stringResource("reason")) },
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
                    Text(stringResource("confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    suggestionToReject = null
                    rejectionReason = ""
                }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    // --- DELETE SUGGESTION DIALOG ---
    if (suggestionToDelete != null) {
        AlertDialog(
            onDismissRequest = { suggestionToDelete = null },
            title = { Text(stringResource("delete_suggestion")) },
            text = { Text("${stringResource("confirm_delete_suggestion")}\n'${suggestionToDelete?.providerName}'") },
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
                    Text(stringResource("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { suggestionToDelete = null }) {
                    Text(stringResource("cancel"))
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
            title = { Text(stringResource("edit_suggestion_details")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource("provider_name")) }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = serviceType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("service_type")) },
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

                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text(stringResource("contact_number")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource("email_address")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(stringResource("city_town")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text(stringResource("country")) }, modifier = Modifier.fillMaxWidth())
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
                    Text(stringResource("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { suggestionToEdit = null }) {
                    Text(stringResource("cancel"))
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
            title = { Text(stringResource("edit_provider_listing")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource("name")) },
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
                            label = { Text(stringResource("category")) },
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
                        label = { Text(stringResource("contact")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource("email")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(stringResource("location")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource("description")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text(stringResource("country")) },
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
                    Text(stringResource("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToEdit = null }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    // --- PROVIDER DELETE DIALOG ---
    if (providerToDelete != null) {
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text(stringResource("delete_provider_listing")) },
            text = { Text("${stringResource("confirm_delete_provider")}\n'${providerToDelete?.name}'?") },
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
                    Text(stringResource("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToDelete = null }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

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
                        val providersByCountry = providers
                            .groupBy { it.country.ifBlank { "Unknown" } }
                            .entries
                            .sortedBy { it.key }

                        providersByCountry.forEach { (country, countryProviders) ->
                            val isCountryExpanded = expandedCountries[country] ?: true

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedCountries[country] = !isCountryExpanded }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = country,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = countryProviders.size.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isCountryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            AnimatedVisibility(
                                visible = isCountryExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    countryProviders.sortedBy { it.name }.forEach { provider ->
                                        AdminProviderListingItem(
                                            provider = provider,
                                            onEdit = { providerToEdit = it },
                                            onDelete = { providerToDelete = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AdminSuggestionItem(
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
                        Text(stringResource("approve"))
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
                        Text(stringResource("reject"))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(
                    onClick = { onEdit(suggestion) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource("edit"), tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { onDelete(suggestion) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AdminProviderListingItem(
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
            Text(text = "${stringResource("contact")}: ${provider.contact}", style = MaterialTheme.typography.bodySmall)
            if (provider.email.isNotBlank()) {
                Text(text = "${stringResource("email")}: ${provider.email}", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "${stringResource("location")}: ${provider.location}", style = MaterialTheme.typography.bodySmall)
            if (provider.country.isNotBlank()) {
                Text(text = "${stringResource("country")}: ${provider.country}", style = MaterialTheme.typography.bodySmall)
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
                    Icon(Icons.Default.Edit, contentDescription = stringResource("edit"), tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { onDelete(provider) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
