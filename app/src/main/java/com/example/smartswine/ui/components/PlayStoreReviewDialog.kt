package com.example.smartswine.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.stringResource

object PlayStoreReviewManager {
    private const val PREFS_NAME = "smart_swine_review_prefs"
    private const val KEY_DO_NOT_SHOW = "key_do_not_show_review"
    private const val KEY_HAS_RATED = "key_has_rated_play_store"
    private const val KEY_LAUNCH_COUNT = "key_review_launch_count"
    private const val KEY_ACTION_COUNT = "key_review_action_count"
    private const val KEY_LAST_PROMPT_TIME = "key_last_review_prompt_time"

    private const val MIN_LAUNCHES = 3
    private const val MIN_ACTIONS = 2
    private const val COOLDOWN_MILLIS = 2 * 24 * 60 * 60 * 1000L // 2 days

    fun shouldShowReviewReminder(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DO_NOT_SHOW, false) || prefs.getBoolean(KEY_HAS_RATED, false)) {
            return false
        }

        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_LAUNCH_COUNT, launchCount).apply()

        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)
        val now = System.currentTimeMillis()

        return launchCount >= MIN_LAUNCHES && (now - lastPromptTime > COOLDOWN_MILLIS)
    }

    fun recordActionCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentActions = prefs.getInt(KEY_ACTION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_ACTION_COUNT, currentActions).apply()
    }

    fun shouldShowAfterAction(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DO_NOT_SHOW, false) || prefs.getBoolean(KEY_HAS_RATED, false)) {
            return false
        }

        recordActionCompleted(context)

        val actionCount = prefs.getInt(KEY_ACTION_COUNT, 0)
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)
        val now = System.currentTimeMillis()

        // Require at least MIN_ACTIONS performed or MIN_LAUNCHES, plus cooldown
        val thresholdMet = (actionCount >= MIN_ACTIONS) || (launchCount >= MIN_LAUNCHES)
        return thresholdMet && (now - lastPromptTime > COOLDOWN_MILLIS)
    }

    fun recordPromptShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis()).apply()
    }

    fun setDoNotShowAgain(context: Context, doNotShow: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DO_NOT_SHOW, doNotShow).apply()
    }

    fun markRated(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_HAS_RATED, true)
            .putBoolean(KEY_DO_NOT_SHOW, true)
            .apply()
    }

    const val PLAY_STORE_PACKAGE_NAME = "com.bibiniitech.smartswine"

    fun openPlayStoreForReview(context: Context) {
        markRated(context)
        val packageName = PLAY_STORE_PACKAGE_NAME
        val playStoreMarketUri = Uri.parse("market://details?id=$packageName")
        val playStoreWebUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

        // 1. First priority: Target the Google Play Store app directly
        val playStoreAppIntent = Intent(Intent.ACTION_VIEW, playStoreMarketUri).apply {
            setPackage("com.android.vending")
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }

        try {
            context.startActivity(playStoreAppIntent)
        } catch (_: Exception) {
            // 2. Second priority: Fallback to the HTTPS Play Store URL in a browser
            val webIntent = Intent(Intent.ACTION_VIEW, playStoreWebUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
                // 3. Third priority: Generic market URI fallback (if any other market handler exists)
                val genericMarketIntent = Intent(Intent.ACTION_VIEW, playStoreMarketUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(genericMarketIntent)
                } catch (_: Exception) {
                    // Fail gracefully if no handler is installed
                }
            }
        }
    }

    fun sendFeedbackEmail(context: Context) {
        recordPromptShown(context)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:bibiniitech@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "SmartSwine App Feedback & Suggestions")
            putExtra(Intent.EXTRA_TEXT, "Hello SmartSwine Team,\n\nHere is my feedback on the app:\n\n")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback: open Play Store anyway if no email client installed
            openPlayStoreForReview(context)
        }
    }
}

@Composable
fun PlayStoreReviewDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var doNotShowAgain by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableIntStateOf(5) }

    Dialog(
        onDismissRequest = {
            if (doNotShowAgain) {
                PlayStoreReviewManager.setDoNotShowAgain(context, true)
            }
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shimmering Golden Star Emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFB300).copy(alpha = 0.3f),
                                    Color(0xFF2E7D32).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = Color(0xFFFFF8E1),
                        border = BorderStroke(2.dp, Color(0xFFFFB300)),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedRating >= 4) stringResource("rate_app_title") else "Help Us Improve",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedRating >= 4)
                            stringResource("rate_app_body")
                        else
                            "Tell us what you'd like to see improved in SmartSwine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Interactive 5-Star Row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..5).forEach { starIndex ->
                                val isFilled = starIndex <= selectedRating
                                val starTint by animateColorAsState(
                                    targetValue = if (isFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                                    label = "starColor"
                                )
                                IconButton(
                                    onClick = { selectedRating = starIndex },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Rate $starIndex stars",
                                        tint = starTint,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        val feedbackText = when (selectedRating) {
                            5 -> "⭐⭐⭐⭐⭐ Outstanding!"
                            4 -> "⭐⭐⭐⭐ Great Experience!"
                            3 -> "⭐⭐⭐ Average - Could be better"
                            2 -> "⭐⭐ Needs Improvement"
                            else -> "⭐ Poor Experience"
                        }
                        Text(
                            text = feedbackText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRating >= 4) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // "Do not show again" Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { doNotShowAgain = !doNotShowAgain }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = doNotShowAgain,
                        onCheckedChange = { doNotShowAgain = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource("do_not_show_again"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Dynamic Primary Action Button
                if (selectedRating >= 4) {
                    Button(
                        onClick = {
                            PlayStoreReviewManager.openPlayStoreForReview(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource("rate_5_stars"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            PlayStoreReviewManager.sendFeedbackEmail(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email Us Your Feedback",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                // Maybe Later / Alternative Option
                OutlinedButton(
                    onClick = {
                        if (doNotShowAgain) {
                            PlayStoreReviewManager.setDoNotShowAgain(context, true)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = stringResource("maybe_later"),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayStoreReviewDialogPreview() {
    SmartSwineTheme {
        PlayStoreReviewDialog(
            onDismiss = {}
        )
    }
}
