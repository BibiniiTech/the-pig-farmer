package com.example.smartswine.ui.market

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource
import kotlinx.coroutines.launch

@Composable
fun CollapsibleMarketCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    badgeCount: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeCount != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ProviderItem(provider: ProviderListing) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (provider.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Text(
                text = provider.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = provider.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, "tel:${provider.contact}".toUri())
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Fallback
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Call", style = MaterialTheme.typography.labelMedium)
                }
                
                if (provider.email.isNotBlank()) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = "mailto:${provider.email}".toUri()
                                    putExtra(Intent.EXTRA_SUBJECT, "SmartSwine Inquiry")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Fallback
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Email", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    userCountry: String = "",
    viewModel: MarketViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val mySuggestions by viewModel.mySuggestions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val nameState = remember { mutableStateOf("") }
    val emailState = remember { mutableStateOf("") }
    val contactState = remember { mutableStateOf("") }
    val countryState = remember { mutableStateOf("") }
    val cityState = remember { mutableStateOf("") }
    val serviceTypeState = remember { mutableStateOf("") }
    val expandedState = remember { mutableStateOf(value = false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val showSuccessDialog = remember { mutableStateOf(value = false) }

    // Collapsible card expand states
    var vendorsExpanded by remember { mutableStateOf(false) }
    var buyersExpanded by remember { mutableStateOf(false) }
    var vetsExpanded by remember { mutableStateOf(false) }
    var suggestionsExpanded by remember { mutableStateOf(false) }
    var suggestExpanded by remember { mutableStateOf(false) }

    // Pre-populate suggestion country with user's country
    LaunchedEffect(userCountry) {
        if (userCountry.isNotBlank() && countryState.value.isBlank()) {
            countryState.value = userCountry
        }
    }

    if (showSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog.value = false },
            confirmButton = { Button(onClick = { showSuccessDialog.value = false }) { Text("OK") } },
            title = { Text(stringResource("suggestion_success_title"), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text(stringResource("suggestion_success_msg"), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        )
    }

    val services = listOf(
        stringResource("butcher"),
        stringResource("meat_processor"),
        stringResource("abattoir"),
        stringResource("feed_supplier"),
        stringResource("tools_supplier"),
        stringResource("vet_shop"),
        stringResource("vet_services"),
        stringResource("other"),
    )

    // Country Filtering based strictly on user's profile country
    val filteredProviders = if (userCountry.isNotBlank()) {
        providers.filter { it.country.trim().lowercase() == userCountry.trim().lowercase() }
    } else {
        providers
    }

    val vendorsList = filteredProviders.filter { it.category == "vendors" }
    val buyersList = filteredProviders.filter { it.category == "buyers" }
    val vetsList = filteredProviders.filter { it.category == "vets" }

    val countryDisplayName = if (userCountry.isNotBlank()) {
        userCountry.trim().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    } else {
        "All Regions"
    }

    // Input Validations
    val isEmailValid = emailState.value.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(emailState.value.trim()).matches()
    val isPhoneValid = contactState.value.isBlank() || android.util.Patterns.PHONE.matcher(contactState.value.trim()).matches()

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
                        text = stringResource("market").uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (error != null) {
                Text(
                    text = "Error loading listings: $error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            // Static country indicator badge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Directory Region: $countryDisplayName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 1. Verified Vendors Card
            CollapsibleMarketCard(
                title = stringResource("market_vendors"),
                icon = Icons.Default.Storefront,
                expanded = vendorsExpanded,
                onToggle = { vendorsExpanded = !vendorsExpanded },
                badgeCount = if (vendorsList.isNotEmpty()) vendorsList.size else null
            ) {
                if (vendorsList.isEmpty()) {
                    Text(
                        text = "No vendors listed for this region yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    vendorsList.forEach { vendor ->
                        ProviderItem(provider = vendor)
                    }
                }
            }

            // 2. Pork Buyers & Abattoirs Card
            CollapsibleMarketCard(
                title = stringResource("market_buyers"),
                icon = Icons.Default.ShoppingBag,
                expanded = buyersExpanded,
                onToggle = { buyersExpanded = !buyersExpanded },
                badgeCount = if (buyersList.isNotEmpty()) buyersList.size else null
            ) {
                if (buyersList.isEmpty()) {
                    Text(
                        text = "No buyers listed for this region yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    buyersList.forEach { buyer ->
                        ProviderItem(provider = buyer)
                    }
                }
            }

            // 3. Veterinary Services Card
            CollapsibleMarketCard(
                title = stringResource("market_vets"),
                icon = Icons.Default.MedicalServices,
                expanded = vetsExpanded,
                onToggle = { vetsExpanded = !vetsExpanded },
                badgeCount = if (vetsList.isNotEmpty()) vetsList.size else null
            ) {
                if (vetsList.isEmpty()) {
                    Text(
                        text = "No veterinary services listed for this region yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    vetsList.forEach { vet ->
                        ProviderItem(provider = vet)
                    }
                }
            }

            // 4. My Suggestions Card
            CollapsibleMarketCard(
                title = "My Suggestions",
                icon = Icons.Default.History,
                expanded = suggestionsExpanded,
                onToggle = { suggestionsExpanded = !suggestionsExpanded },
                badgeCount = if (mySuggestions.isNotEmpty()) mySuggestions.size else null
            ) {
                if (mySuggestions.isEmpty()) {
                    Text(
                        text = "You have not submitted any suggestions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        mySuggestions.forEach { suggestion ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        
                                        val (statusText, statusColor) = when (suggestion.status) {
                                            "approved" -> "Approved" to Color(0xFF4CAF50)
                                            "rejected" -> "Rejected" to Color(0xFFF44336)
                                            else -> "Pending" to Color(0xFFFFC107)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = statusColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = statusText,
                                                color = statusColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Category: ${suggestion.serviceType}", style = MaterialTheme.typography.bodyMedium)
                                    if (suggestion.contact.isNotBlank()) {
                                        Text("Contact: ${suggestion.contact}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (suggestion.email.isNotBlank()) {
                                        Text("Email: ${suggestion.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Location: ${suggestion.city}, ${suggestion.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    if (suggestion.status == "rejected" && suggestion.adminFeedback.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Feedback: ${suggestion.adminFeedback}",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Suggest a Swine Provider Card
            CollapsibleMarketCard(
                title = "Suggest a Provider",
                icon = Icons.Default.Add,
                expanded = suggestExpanded,
                onToggle = { suggestExpanded = !suggestExpanded }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nameState.value,
                        onValueChange = { nameState.value = it },
                        label = { Text(stringResource("provider_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedState.value,
                        onExpandedChange = { expandedState.value = !expandedState.value }
                    ) {
                        OutlinedTextField(
                            value = serviceTypeState.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("service_type")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState.value) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedState.value,
                            onDismissRequest = { expandedState.value = false }
                        ) {
                            services.forEach { service ->
                                DropdownMenuItem(
                                    text = { Text(service) },
                                    onClick = {
                                        serviceTypeState.value = service
                                        expandedState.value = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contactState.value,
                        onValueChange = { contactState.value = it },
                        label = { Text(stringResource("contact_number")) },
                        isError = !isPhoneValid,
                        supportingText = {
                            if (!isPhoneValid) {
                                Text("Invalid phone number format", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = emailState.value,
                        onValueChange = { emailState.value = it },
                        label = { Text(stringResource("email_address")) },
                        isError = !isEmailValid,
                        supportingText = {
                            if (!isEmailValid) {
                                Text("Invalid email format", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cityState.value,
                            onValueChange = { cityState.value = it },
                            label = { Text(stringResource("city_town")) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = countryState.value,
                            onValueChange = { countryState.value = it },
                            label = { Text(stringResource("country")) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.submitSuggestion(
                                name = nameState.value,
                                serviceType = serviceTypeState.value,
                                contact = contactState.value,
                                email = emailState.value,
                                city = cityState.value,
                                country = countryState.value
                            ) { success, errorMsg ->
                                if (success) {
                                    showSuccessDialog.value = true
                                    // Reset fields
                                    nameState.value = ""
                                    emailState.value = ""
                                    contactState.value = ""
                                    cityState.value = ""
                                    serviceTypeState.value = ""
                                } else {
                                    scope.launch {
                                        if (errorMsg == "duplicate") {
                                            snackbarHostState.showSnackbar("This provider is already listed or suggested.")
                                        } else {
                                            snackbarHostState.showSnackbar("Error submitting suggestion: $errorMsg")
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = nameState.value.isNotBlank() && 
                                serviceTypeState.value.isNotBlank() && 
                                contactState.value.isNotBlank() &&
                                isEmailValid &&
                                isPhoneValid
                    ) {
                        Text(stringResource("submit_suggestion"))
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
