package com.example.smartswine.ui.dashboard.components.grid

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.ui.market.MarketViewModel
import com.example.smartswine.ui.market.ProviderListing
import com.example.smartswine.ui.market.Suggestion
import com.example.smartswine.utils.stringResource

@Composable
fun MarketSubOptionCard(
    title: String,
    icon: ImageVector,
    count: Int? = null,
    isExpanded: Boolean,
    primaryColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isExpanded) primaryColor else primaryColor.copy(alpha = 0.22f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (count != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ProviderCardItem(
    provider: ProviderListing,
    primaryColor: Color
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (provider.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (provider.location.isNotBlank()) {
                    Text(
                        text = provider.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (provider.description.isNotBlank()) {
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (provider.contact.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, "tel:${provider.contact}".toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = primaryColor)
                        Spacer(Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp, color = primaryColor)
                    }

                    Button(
                        onClick = {
                            val cleanNumber = provider.contact.replace("+", "").replace(" ", "").replace("-", "")
                            val intent = Intent(Intent.ACTION_VIEW, "https://wa.me/$cleanNumber".toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineSuggestProviderForm(
    defaultCountry: String,
    onSubmit: (name: String, type: String, contact: String, email: String, city: String, country: String) -> Unit,
    mySuggestions: List<Suggestion>,
    primaryColor: Color
) {
    var name by remember { mutableStateOf("") }
    val serviceTypes = listOf(
        "veterinary_services" to "Veterinary Services",
        "feed_equipment_vendor" to "Feed & Equipment Vendor",
        "pork_buyer_abattoir" to "Pork Buyer / Abattoir",
        "other" to "Other"
    )
    var selectedServiceTypeKey by remember { mutableStateOf("veterinary_services") }
    var serviceMenuExpanded by remember { mutableStateOf(false) }
    var contact by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(defaultCountry) }
    var submitted by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (submitted) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text(stringResource("suggestion_submitted_review"), style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20))
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource("provider_name")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        ExposedDropdownMenuBox(
            expanded = serviceMenuExpanded,
            onExpandedChange = { serviceMenuExpanded = !serviceMenuExpanded }
        ) {
            OutlinedTextField(
                value = stringResource(selectedServiceTypeKey),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource("service_type")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceMenuExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = serviceMenuExpanded,
                onDismissRequest = { serviceMenuExpanded = false }
            ) {
                serviceTypes.forEach { (typeKey, _) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(typeKey)) },
                        onClick = {
                            selectedServiceTypeKey = typeKey
                            serviceMenuExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text(stringResource("contact_number")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource("email_address")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text(stringResource("city_town")) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text(stringResource("country")) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        Button(
            onClick = {
                if (name.isNotBlank() && contact.isNotBlank()) {
                    val standardType = serviceTypes.find { it.first == selectedServiceTypeKey }?.second ?: "Veterinary Services"
                    onSubmit(name, standardType, contact, email, city, country)
                    submitted = true
                    name = ""
                    contact = ""
                    email = ""
                    city = ""
                }
            },
            enabled = name.isNotBlank() && contact.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource("submit_suggestion"), fontWeight = FontWeight.Bold)
        }

        if (mySuggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Previous Suggestions (${mySuggestions.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            mySuggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(suggestion.providerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${suggestion.serviceType} • ${suggestion.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val statusColor = when (suggestion.status.lowercase()) {
                            "approved" -> Color(0xFF2E7D32)
                            "rejected" -> Color(0xFFC62828)
                            else -> Color(0xFFEF6C00)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = suggestion.status.replaceFirstChar { it.uppercase() },
                                color = statusColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketSectionContent(
    userCountry: String,
    openMarketSubOption: String?,
    onToggleSubOption: (String) -> Unit,
    primaryColor: Color,
    onRegisterCoordinates: (String, LayoutCoordinates) -> Unit,
    onItemExpanded: (String) -> Unit
) {
    val marketViewModel: MarketViewModel = viewModel()
    val providers by marketViewModel.providers.collectAsStateWithLifecycle()
    val mySuggestions by marketViewModel.mySuggestions.collectAsStateWithLifecycle()

    val directoryCountry = remember(userCountry) { userCountry.ifBlank { "Ghana" } }
    val regionalProviders = remember(providers, directoryCountry) {
        providers.filter { it.country.trim().equals(directoryCountry.trim(), ignoreCase = true) }
    }
    val regionalVendors = remember(regionalProviders) { regionalProviders.filter { it.category == "vendors" } }
    val regionalBuyers = remember(regionalProviders) { regionalProviders.filter { it.category == "buyers" } }
    val regionalVets = remember(regionalProviders) { regionalProviders.filter { it.category == "vets" } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFEBEE),
            border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFC2185B), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${stringResource("directory_region")}: $directoryCountry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF880E4F)
                )
            }
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("vendors", it) },
            title = stringResource("verified_vendors"),
            icon = Icons.Default.Storefront,
            count = regionalVendors.size,
            isExpanded = openMarketSubOption == "vendors",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("vendors")
                if (openMarketSubOption != "vendors") onItemExpanded("vendors")
            }
        ) {
            if (regionalVendors.isEmpty()) {
                Text("${stringResource("no_verified_vendors")} ($directoryCountry)", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(8.dp))
            } else {
                regionalVendors.forEach { provider ->
                    ProviderCardItem(provider = provider, primaryColor = primaryColor)
                }
            }
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("buyers", it) },
            title = stringResource("pork_buyers_abattoirs"),
            icon = Icons.Default.ShoppingCart,
            count = regionalBuyers.size,
            isExpanded = openMarketSubOption == "buyers",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("buyers")
                if (openMarketSubOption != "buyers") onItemExpanded("buyers")
            }
        ) {
            if (regionalBuyers.isEmpty()) {
                Text("${stringResource("no_buyers_abattoirs")} ($directoryCountry)", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(8.dp))
            } else {
                regionalBuyers.forEach { provider ->
                    ProviderCardItem(provider = provider, primaryColor = primaryColor)
                }
            }
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("vets", it) },
            title = stringResource("veterinary_services"),
            icon = Icons.Default.LocalHospital,
            count = regionalVets.size,
            isExpanded = openMarketSubOption == "vets",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("vets")
                if (openMarketSubOption != "vets") onItemExpanded("vets")
            }
        ) {
            if (regionalVets.isEmpty()) {
                Text("${stringResource("no_vets_services")} ($directoryCountry)", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(8.dp))
            } else {
                regionalVets.forEach { provider ->
                    ProviderCardItem(provider = provider, primaryColor = primaryColor)
                }
            }
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("suggest", it) },
            title = stringResource("suggest_provider"),
            icon = Icons.Default.AddCircleOutline,
            count = null,
            isExpanded = openMarketSubOption == "suggest",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("suggest")
                if (openMarketSubOption != "suggest") onItemExpanded("suggest")
            }
        ) {
            InlineSuggestProviderForm(
                defaultCountry = directoryCountry,
                onSubmit = { name, type, contact, email, city, ctry ->
                    marketViewModel.submitSuggestion(name, type, contact, email, city, ctry) { _, _ -> }
                },
                mySuggestions = mySuggestions,
                primaryColor = primaryColor
            )
        }
    }
}
