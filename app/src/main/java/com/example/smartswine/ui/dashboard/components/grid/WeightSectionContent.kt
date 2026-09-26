package com.example.smartswine.ui.dashboard.components.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.smartswine.model.Pig
import com.example.smartswine.ui.weight.CarcassWeightCard
import com.example.smartswine.ui.weight.ScaleMeasurementCard
import com.example.smartswine.ui.weight.TapeMeasurementCard
import com.example.smartswine.ui.weight.WeightConverterCard
import com.example.smartswine.utils.stringResource

@Composable
fun WeightSectionContent(
    allPigs: List<Pig>,
    targetPigTag: String?,
    openWeightSubOption: String?,
    onToggleSubOption: (String) -> Unit,
    onSubOptionChange: (String?) -> Unit,
    primaryColor: Color,
    onRegisterCoordinates: (String, LayoutCoordinates) -> Unit,
    onItemExpanded: (String) -> Unit,
    onUpdatePigWeight: (String, Double) -> Unit
) {
    var isKg by rememberSaveable { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("convert", it) },
            title = stringResource("convert_weight"),
            icon = Icons.Default.Scale,
            count = null,
            isExpanded = openWeightSubOption == "convert",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("convert")
                if (openWeightSubOption != "convert") onItemExpanded("convert")
            }
        ) {
            WeightConverterCard()
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("tape", it) },
            title = stringResource("weigh_with_tape"),
            icon = Icons.Default.Straighten,
            count = null,
            isExpanded = openWeightSubOption == "tape",
            primaryColor = primaryColor,
            onToggle = { 
                val next = if (openWeightSubOption == "tape") null else "tape"
                onToggleSubOption("tape")
                onSubOptionChange(next)
                if (next != null) onItemExpanded("tape")
            }
        ) {
            TapeMeasurementCard(
                isKg = isKg,
                onUnitChange = { isKg = it },
                pigs = allPigs,
                initialPigTag = targetPigTag,
                onUpdatePigWeight = onUpdatePigWeight
            )
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("scale", it) },
            title = stringResource("weigh_with_scale"),
            icon = Icons.Default.Scale,
            count = null,
            isExpanded = openWeightSubOption == "scale",
            primaryColor = primaryColor,
            onToggle = {
                val next = if (openWeightSubOption == "scale") null else "scale"
                onToggleSubOption("scale")
                onSubOptionChange(next)
                if (next != null) onItemExpanded("scale")
            }
        ) {
            ScaleMeasurementCard(
                isKg = isKg,
                onUnitChange = { isKg = it },
                pigs = allPigs,
                initialPigTag = targetPigTag,
                onUpdatePigWeight = onUpdatePigWeight
            )
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("carcass", it) },
            title = stringResource("find_carcass_weight"),
            icon = Icons.Default.Calculate,
            count = null,
            isExpanded = openWeightSubOption == "carcass",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("carcass")
                if (openWeightSubOption != "carcass") onItemExpanded("carcass")
            }
        ) {
            CarcassWeightCard()
        }
    }
}
