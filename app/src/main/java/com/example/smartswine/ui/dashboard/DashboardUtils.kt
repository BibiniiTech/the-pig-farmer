package com.example.smartswine.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.bibiniitech.smartswine.R
import com.example.smartswine.utils.stringResource

sealed class TaskIcon {
    data class Vector(val imageVector: ImageVector) : TaskIcon()
    data class Resource(val resId: Int) : TaskIcon()
}

@Composable
fun getTranslatedActivityName(activityName: String): String {
    return when {
        activityName.contains("Heat Detection", ignoreCase = true) -> stringResource("heat_detection")
        activityName.contains("Breeding", ignoreCase = true) || activityName.contains("Mating", ignoreCase = true) -> stringResource("breeding_mating")
        activityName.contains("Confirm Pregnancy", ignoreCase = true) || activityName.contains("Pregnancy Check", ignoreCase = true) -> stringResource("pregnancy_check")
        activityName.contains("Farrowing", ignoreCase = true) -> stringResource("farrowing")
        activityName.contains("Weaning", ignoreCase = true) -> stringResource("weaning")
        activityName.contains("Castration", ignoreCase = true) -> stringResource("castration")
        activityName.contains("Teeth Clipping", ignoreCase = true) -> stringResource("teeth_clipping")
        activityName.contains("Tail Docking", ignoreCase = true) -> stringResource("tail_docking")
        activityName.contains("Deworming", ignoreCase = true) -> stringResource("deworming")
        activityName.contains("Iron Injection", ignoreCase = true) -> stringResource("iron_injection")
        activityName.contains("Vaccination", ignoreCase = true) -> stringResource("vaccination")
        activityName.contains("Medication", ignoreCase = true) -> stringResource("medication")
        activityName.contains("Weight Check", ignoreCase = true) -> stringResource("weight_check")
        activityName.contains("Culling", ignoreCase = true) -> stringResource("culling")
        activityName.contains("Feed", ignoreCase = true) -> stringResource("feed_pigs")
        else -> activityName
    }
}

fun getTaskIcon(activity: String): TaskIcon {
    return when {
        activity.contains("Heat", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_heat)
        activity.contains("Breeding", ignoreCase = true) || activity.contains("Mating", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_breeding)
        activity.contains("Pregnancy", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_pregnancy_check)
        activity.contains("Farrowing", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_farrowing)
        activity.contains("Weaning", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_weaning)
        activity.contains("Iron", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_iron)
        activity.contains("Vaccination", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_vaccination)
        activity.contains("Medication", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_medication)
        activity.contains("Weight", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_weight_checker)
        activity.contains("Castration", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_castration)
        activity.contains("Teeth Clipping", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_teeth_clipping)
        activity.contains("Tail Docking", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_tail_docking)
        activity.contains("Deworming", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_deworming)
        activity.contains("Culling", ignoreCase = true) -> TaskIcon.Resource(R.drawable.ic_culling)
        activity.contains("Feed", ignoreCase = true) -> TaskIcon.Vector(Icons.Default.Agriculture)
        else -> TaskIcon.Vector(Icons.AutoMirrored.Filled.Assignment)
    }
}
