package com.example.smartswine.ui.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartswine.utils.stringResource

/**
 * Renders an annotated, cropped screenshot mockup of live SmartSwine screens
 * with visual indication callouts and pointers for a given guide topic.
 */
@Composable
fun GuidePictorialReference(
    topicId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Pictorial Reference Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource("pictorial_reference"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = stringResource("indication_visual_guide"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cropped Mockup Container
            CroppedMockupViewport {
                when (topicId) {
                    "herd_management" -> HerdPictorialMockup()
                    "feed_formulation" -> FeedPictorialMockup()
                    "breeding_and_activities" -> BreedingPictorialMockup()
                    "financials_tracking" -> FinancialsPictorialMockup()
                    "disease_finder" -> HealthPictorialMockup()
                    "weight_checker" -> WeightPictorialMockup()
                    "human_resources" -> StaffPictorialMockup()
                    "offline_sync" -> OfflinePictorialMockup()
                    else -> HerdPictorialMockup()
                }
            }
        }
    }
}

/**
 * Simulated mobile screen frame wrapping the cropped content.
 */
@Composable
private fun CroppedMockupViewport(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
    ) {
        Column {
            // Simulated Status Bar / Window Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF5F56), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF27C93F), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SmartSwine UI",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Screen Content
            Box(modifier = Modifier.padding(10.dp)) {
                content()
            }
        }
    }
}

/**
 * Visual Indication Callout Tag with step number, icon, and explanatory label
 */
@Composable
private fun VisualIndicationTag(
    number: Int,
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    val bgColor = if (isPrimary) Color(0xFFE53935) else Color(0xFF1976D2)
    Surface(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    color = bgColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =========================================================================
// 1. HERD MANAGEMENT MOCKUP
// =========================================================================
@Composable
private fun HerdPictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search & Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("Search tag, pen...", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text("Active (24)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }

        // Mock Pig Card with Indication Frame
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF1976D2).copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Pets, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tag #SW-042", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                                Text("Active", fontSize = 9.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text("Duroc Sow • Pen 4B • 145 kg", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Indication for Tap Card
        VisualIndicationTag(
            number = 2,
            text = stringResource("indication_tap_card"),
            isPrimary = false
        )

        // Mock Bottom Area with Indicated "+" FAB
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VisualIndicationTag(
                    number = 1,
                    text = stringResource("indication_tap_fab"),
                    isPrimary = true
                )

                // The FAB with highlighted pulsing ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, Color(0xFFE53935), CircleShape)
                        .padding(2.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// =========================================================================
// 2. FEED FORMULATION MOCKUP
// =========================================================================
@Composable
private fun FeedPictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Target Stage Selector with Indication
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.5.dp, Color(0xFFE53935))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Adjust, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Target: Grower (30-60kg)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                }
            }

            VisualIndicationTag(number = 1, text = stringResource("indication_select_stage"), isPrimary = true)
        }

        // Mock Nutrient Progress Bars & Ca:P Gauge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Crude Protein: 16.5%", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("Optimal 16-18%", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { 0.85f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = Color(0xFF4CAF50))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ca : P Ratio: 1.35 : 1", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("Balanced (1.2 - 1.6)", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { 0.7f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = Color(0xFF4CAF50))
            }
        }

        // Ingredient Slider Row with Indication
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Maize (55%) • Soya Meal (25%) • Bran (15%)", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                LinearProgressIndicator(progress = { 0.95f }, modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp), color = MaterialTheme.colorScheme.primary)
            }
        }

        VisualIndicationTag(number = 2, text = stringResource("indication_adjust_sliders"), isPrimary = false)

        // Bottom Action Buttons with Indication 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Save Recipe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            VisualIndicationTag(number = 3, text = stringResource("indication_save_mix"), isPrimary = true)
        }
    }
}

// =========================================================================
// 3. BREEDING & FARROWING MOCKUP
// =========================================================================
@Composable
private fun BreedingPictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mating Service Entry with Indication 1
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Sow #SOW-108 × Boar #BOAR-02", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("AI Batch: AI-DUROC-2024 • Mated: Aug 12", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VisualIndicationTag(number = 1, text = stringResource("indication_record_mating"), isPrimary = true)
            }
        }

        // 114-Day Gestation Timeline with Indication 2
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gestation Progress: Day 84 / 114", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Due: Dec 4 (30d left)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { 84f / 114f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary)
                Text("🔔 Prep farrowing pen alert on Day 110", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        VisualIndicationTag(number = 2, text = stringResource("indication_gestation_timer"), isPrimary = false)

        // Litter Logger Preview with Indication 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                    Text("Alive: 12", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE53935).copy(alpha = 0.15f)) {
                    Text("Stillborn: 1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Avg: 1.4kg", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            VisualIndicationTag(number = 3, text = stringResource("indication_log_litter"), isPrimary = true)
        }
    }
}

// =========================================================================
// 4. FINANCIALS & UNIT ECONOMICS MOCKUP
// =========================================================================
@Composable
private fun FinancialsPictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // COP & Breakeven with Indication 1
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Cost of Production: $2.40 / kg", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Finisher Breakeven Price: $215.00 / pig", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VisualIndicationTag(number = 1, text = stringResource("indication_cop_breakeven"), isPrimary = true)
            }
        }

        // Expense Distribution with Feed Benchmark Indication 2
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Expense Breakdown", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Feed 68% (Benchmark 65-75%)", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                // Multi-colored mock progress bar
                Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.weight(0.68f).fillMaxHeight().background(Color(0xFF4CAF50)))
                    Box(modifier = Modifier.weight(0.12f).fillMaxHeight().background(Color(0xFF2196F3)))
                    Box(modifier = Modifier.weight(0.20f).fillMaxHeight().background(Color(0xFFFF9800)))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Feed: 68%", fontSize = 9.sp, color = Color(0xFF2E7D32))
                    Text("Vet: 12%", fontSize = 9.sp, color = Color(0xFF1976D2))
                    Text("Labor & Ops: 20%", fontSize = 9.sp, color = Color(0xFFE65100))
                }
            }
        }

        VisualIndicationTag(number = 2, text = stringResource("indication_feed_benchmark"), isPrimary = false)
    }
}

// =========================================================================
// 5. HEALTH & BIOSECURITY MOCKUP
// =========================================================================
@Composable
private fun HealthPictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Symptom Selector Chips with Indication 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary, border = BorderStroke(1.dp, Color(0xFFE53935))) {
                    Text("Coughing ✔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary, border = BorderStroke(1.dp, Color(0xFFE53935))) {
                    Text("Labored Breath ✔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }
            VisualIndicationTag(number = 1, text = stringResource("indication_select_symptoms"), isPrimary = true)
        }

        // Matched Disease Result
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("Match: Swine Respiratory Disease (High)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Meat Withdrawal Safety Warning Banner with Indication 2
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFEBEE)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    Column {
                        Text("CRITICAL: Meat Withdrawal Active", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFB71C1C))
                        Text("Safe for slaughter on Oct 28 (14d left)", fontSize = 9.sp, color = Color(0xFFB71C1C))
                    }
                }
            }
        }

        VisualIndicationTag(number = 2, text = stringResource("indication_withdrawal_safety"), isPrimary = false)
    }
}

// =========================================================================
// 6. WEIGHT CHECKER MOCKUP
// =========================================================================
@Composable
private fun WeightPictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Measurement Input Row with Indication 1
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Tape Mode: Heart Girth 94 cm • Length 108 cm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Calculated Weight: 68.2 kg (±1.5 kg error)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                VisualIndicationTag(number = 1, text = stringResource("indication_enter_measurements"), isPrimary = true)
            }
        }

        // ADG & Projected Slaughter Date with Indication 2
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ADG: 750 g/day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF4CAF50).copy(alpha = 0.2f)) {
                            Text("Optimal Growth", fontSize = 9.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    Text("Projected Slaughter Date: Dec 12 (32d left)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        VisualIndicationTag(number = 2, text = stringResource("indication_adg_forecast"), isPrimary = false)
    }
}

// =========================================================================
// 7. STAFF & HR MOCKUP
// =========================================================================
@Composable
private fun StaffPictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Staff Member Card with Indication 1
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Text("JD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column {
                        Text("John Doe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Role: Farm Supervisor", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                VisualIndicationTag(number = 1, text = stringResource("indication_assign_roles"), isPrimary = true)
            }
        }

        // Chore Checklist with Indication 2
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Assigned Tasks Today", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                    Text("Morning Weaner Feeding (Completed)", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Schedule, null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                    Text("Pen 4 Disinfection (Due 2:00 PM)", fontSize = 10.sp)
                }
            }
        }

        VisualIndicationTag(number = 2, text = stringResource("indication_track_chores"), isPrimary = false)
    }
}

// =========================================================================
// 8. OFFLINE SYNC MOCKUP
// =========================================================================
@Composable
private fun OfflinePictorialMockup() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Offline Banner with Indication 1
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Column {
                        Text("Working Offline", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("4 pending records saved to local SQLite", fontSize = 9.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                VisualIndicationTag(number = 1, text = stringResource("indication_offline_logging"), isPrimary = true)
            }
        }

        // Cloud Auto-Sync Indicator with Indication 2
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CloudSync, null, tint = Color(0xFF00897B), modifier = Modifier.size(18.dp))
                    Column {
                        Text("Automatic Cloud Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Syncs immediately when Wi-Fi / Data reconnects", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        VisualIndicationTag(number = 2, text = stringResource("indication_cloud_sync"), isPrimary = false)
    }
}
