package com.example.smartswine.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.ui.theme.SmartSwineTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.PremiumWrapper
import java.util.Locale

@Composable
fun FeedCalculationResultScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
) {
    val feedRequirements by viewModel.feedRequirements.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lang = LocalAppLanguage.current.code
    val isPremium = LocalIsPremium.current

    FeedCalculationResultContent(
        feedRequirements = feedRequirements,
        onBack = onBack,
        isPremium = isPremium,
        onNavigateToPaywall = onNavigateToPaywall,
    ) { reqs ->
        PdfGenerator.generateFeedRequirementPdf(context, reqs, lang)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCalculationResultContent(
    feedRequirements: Map<String, Double>?,
    onBack: () -> Unit,
    isPremium: Boolean = true,
    onNavigateToPaywall: () -> Unit = {},
    onExportPdf: (Map<String, Double>) -> Unit = {},
) {
    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                    Text(
                        text = stringResource("calculation_results"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            feedRequirements?.let { reqs ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val days = reqs["__days"]?.toInt() ?: 1
                        val header = if (days > 1) stringResource("feed_requirements_days", days) else stringResource("daily_requirements")
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(header, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        reqs.filter { !it.key.startsWith("__") }.forEach { (label, qty) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val weight = if (label.contains("Total")) FontWeight.Bold else FontWeight.Normal
                                
                                val localizedLabel = when {
                                    label.contains("Total Daily Requirement") -> stringResource("total_daily_requirement")
                                    label.contains("Daily Total") -> stringResource("daily_total")
                                    label.contains("Total for") -> {
                                        val parts = label.split(" ")
                                        val d = parts.find { it.all { c -> c.isDigit() } }?.toInt() ?: 1
                                        stringResource("total_for_days", d)
                                    }
                                    label.contains("(") -> {
                                        val category = label.substringBefore(" (")
                                        val count = label.substringAfter("(").substringBefore(")")
                                        val localizedCategory = when (category.lowercase()) {
                                            "breeders_starter" -> "${stringResource("breeder")} ${stringResource("starter")}"
                                            "breeders_grower" -> "${stringResource("breeder")} ${stringResource("grower")}"
                                            else -> stringResource(category.lowercase().replace(" ", "_"))
                                        }
                                        "$localizedCategory ($count)"
                                    }
                                    else -> stringResource(label.lowercase().replace(" ", "_"))
                                }

                                Text(localizedLabel, fontWeight = weight)
                                Text("${String.format(Locale.getDefault(), "%.1f", qty)}kg", fontWeight = weight)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        PremiumWrapper(
                            isPremium = isPremium,
                            onLockedClick = onNavigateToPaywall
                        ) {
                            Button(
                                onClick = { 
                                    if (isPremium) onExportPdf(reqs) else onNavigateToPaywall() 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isPremium) Icons.Default.PictureAsPdf else Icons.Default.Lock,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource("export_requirements_pdf"))
                            }
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource("no_calculation_results"))
            }
            
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedCalculationResultPreview() {
    val sampleFeedRequirements = mapOf(
        "__days" to 7.0,
        "Maize" to 350.5,
        "Soybean Meal" to 140.2,
        "Fish Meal" to 35.0,
        "Total Feed" to 525.7
    )
    SmartSwineTheme {
        FeedCalculationResultContent(
            feedRequirements = sampleFeedRequirements,
            onBack = {}
        )
    }
}
