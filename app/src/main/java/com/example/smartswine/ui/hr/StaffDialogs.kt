package com.example.smartswine.ui.hr

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.utils.LocalIsPaidPremium
import com.example.smartswine.model.StaffMember
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.stringResource
import java.io.ByteArrayOutputStream
import java.util.*

fun bitmapToJpegBase64(originalBitmap: Bitmap, maxDim: Int = 256): String {
    val width = originalBitmap.width
    val height = originalBitmap.height
    val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height, 1f)
    val scaledBitmap = Bitmap.createScaledBitmap(
        originalBitmap,
        (width * ratio).toInt().coerceAtLeast(1),
        (height * ratio).toInt().coerceAtLeast(1),
        true
    )
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    val byteArray = outputStream.toByteArray()
    return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

fun uriToJpegBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (originalBitmap != null) {
            bitmapToJpegBase64(originalBitmap)
        } else null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun StaffAvatar(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val bitmap = remember(photoUrl) {
        if (!photoUrl.isNullOrBlank()) {
            try {
                val cleanBase64 = if (photoUrl.contains(",")) photoUrl.substringAfter(",") else photoUrl
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color(0xFF7B1FA2).copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, Color(0xFF7B1FA2).copy(alpha = 0.35f))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.trim().take(1).uppercase().ifEmpty { "S" },
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42).sp,
                    color = Color(0xFF7B1FA2)
                )
            }
        }
    }
}

@Composable
fun StaffProfileDialog(
    member: StaffMember,
    onDismiss: () -> Unit,
    onViewDetails: (StaffMember) -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val dialogBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val titleColor = if (isDark) Color(0xFFCE93D8) else Color(0xFF4A148C)
    val subtitleColor = if (isDark) Color(0xFFBA68C8) else Color(0xFF7B1FA2)
    val dividerColor = if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.3f) else Color(0xFFF3E5F5)
    val phoneCardBg = if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.25f) else Color(0xFFF3E5F5).copy(alpha = 0.5f)
    val phoneCardBorder = if (isDark) Color(0xFFCE93D8).copy(alpha = 0.3f) else Color(0xFFCE93D8).copy(alpha = 0.5f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource("staff_profile"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StaffAvatar(
                    photoUrl = member.photoUrl,
                    name = member.name,
                    size = 96.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = subtitleColor,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(color = dividerColor)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (member.phone.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, "tel:${member.phone}".toUri())
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = phoneCardBg,
                            border = BorderStroke(1.dp, phoneCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource("phone_number"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(member.phone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE1BEE7) else Color(0xFF4A148C))
                                }
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    if (member.email.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(20.dp))
                            Column {
                                Text(stringResource("staff_email"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(member.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (member.residentialAddress.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(20.dp))
                            Column {
                                Text(stringResource("residential_address"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(member.residentialAddress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (member.joinDate.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(20.dp))
                            Column {
                                Text(stringResource("join_date"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(member.joinDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (member.emergencyContactName.isNotBlank() || member.emergencyContactPhone.isNotBlank()) {
                        HorizontalDivider(color = dividerColor)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource("emergency_contact_details"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = subtitleColor
                            )
                            if (member.emergencyContactName.isNotBlank()) {
                                Text(
                                    text = "${member.emergencyContactName} (${member.emergencyContactRelation.ifBlank { "Contact" }})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (member.emergencyContactPhone.isNotBlank()) {
                                Text(
                                    text = member.emergencyContactPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onViewDetails(member) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2))
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource("view_details"),
                        color = if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF9C27B0) else Color(0xFF7B1FA2))
                ) {
                    Text(stringResource("close"))
                }
            }
        }
    )
}

@Composable
fun StaffFullDetailsDialog(
    member: StaffMember,
    financialRecords: List<FinancialRecord>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit
) {
    val context = LocalContext.current
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR).toString() }

    val matchingSalaryPayments = remember(member, financialRecords) {
        financialRecords.filter {
            (it.category.equals("Salary", ignoreCase = true) || it.category.equals("Labor/Salary", ignoreCase = true)) &&
            it.description.contains(member.name, ignoreCase = true)
        }.sortedByDescending { DateUtils.parseAnyDateNonNull(it.date) }
    }

    val ytdPaid = remember(matchingSalaryPayments, currentYear) {
        matchingSalaryPayments.filter { record ->
            val d = DateUtils.parseAnyDate(record.date)
            if (d != null) {
                val cal = Calendar.getInstance().apply { time = d }
                cal.get(Calendar.YEAR).toString() == currentYear
            } else {
                record.date.contains(currentYear)
            }
        }.sumOf { it.amount }
    }
    val totalPaid = remember(matchingSalaryPayments) {
        matchingSalaryPayments.sumOf { it.amount }
    }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val dialogBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val titleColor = if (isDark) Color(0xFFCE93D8) else Color(0xFF4A148C)
    val sectionHeaderColor = if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
    val subtitleColor = if (isDark) Color(0xFFBA68C8) else Color(0xFF7B1FA2)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFBF8FC)
    val cardBorder = if (isDark) Color(0xFFCE93D8).copy(alpha = 0.25f) else Color(0xFFE1BEE7).copy(alpha = 0.5f)
    val compCardBg = if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.25f) else Color(0xFF7B1FA2).copy(alpha = 0.08f)
    val compCardBorder = if (isDark) Color(0xFFCE93D8).copy(alpha = 0.4f) else Color(0xFF7B1FA2).copy(alpha = 0.25f)
    val historyCardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val historyCardBorder = if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.3f) else Color(0xFFE0E0E0)
    val dividerColor = if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.3f) else Color(0xFFF3E5F5)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource("employee_details"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header: Avatar, Name, Role, Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StaffAvatar(
                        photoUrl = member.photoUrl,
                        name = member.name,
                        size = 72.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = member.role,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = subtitleColor
                        )
                        Spacer(Modifier.height(4.dp))
                        val statusBg = when (member.status.lowercase()) {
                            "active" -> if (isDark) Color(0xFF1B5E20).copy(alpha = 0.5f) else Color(0xFFE8F5E9)
                            "on leave" -> if (isDark) Color(0xFFE65100).copy(alpha = 0.4f) else Color(0xFFFFF3E0)
                            else -> if (isDark) Color(0xFFB71C1C).copy(alpha = 0.4f) else Color(0xFFFFEBEE)
                        }
                        val statusTextColor = when (member.status.lowercase()) {
                            "active" -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                            "on leave" -> if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                            else -> if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBg,
                            border = BorderStroke(1.dp, statusTextColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = member.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor
                            )
                        }
                    }
                }

                HorizontalDivider(color = dividerColor)

                // Section 1: Personal & Contact Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource("staff_details"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor
                        )

                        if (member.phone.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, "tel:${member.phone}".toUri())
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource("phone_number"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(member.phone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFFE1BEE7) else Color(0xFF4A148C))
                                }
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            }
                        }

                        if (member.email.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(stringResource("staff_email"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(member.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        if (member.residentialAddress.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(stringResource("residential_address"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(member.residentialAddress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        if (member.dateOfBirth.isNotBlank() || member.gender.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(18.dp))
                                Column {
                                    val appLanguage = LocalAppLanguage.current
                                    val localizedGender = when (member.gender.trim()) {
                                        "Male" -> stringResource("male")
                                        "Female" -> stringResource("female")
                                        "Other" -> stringResource("other_gender")
                                        else -> member.gender
                                    }
                                    val formattedDob = if (member.dateOfBirth.isNotBlank()) {
                                        val parsed = DateUtils.parseAnyDateNonNull(member.dateOfBirth)
                                        DateUtils.formatDateToDisplay(parsed, appLanguage.toLocale())
                                    } else null
                                    Text(
                                        text = listOfNotNull(
                                            localizedGender.ifBlank { null },
                                            if (formattedDob != null) "${stringResource("date_of_birth")}: $formattedDob" else null
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (member.joinDate.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(stringResource("join_date"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(member.joinDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Section 2: Emergency Contact
                if (member.emergencyContactName.isNotBlank() || member.emergencyContactPhone.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource("emergency_contact_details"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = sectionHeaderColor
                            )
                            if (member.emergencyContactName.isNotBlank()) {
                                Text(
                                    text = "${member.emergencyContactName} (${member.emergencyContactRelation.ifBlank { "Contact" }})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (member.emergencyContactPhone.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, "tel:${member.emergencyContactPhone}".toUri())
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = member.emergencyContactPhone,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                            if (member.emergencyContactAddress.isNotBlank()) {
                                Text(
                                    text = member.emergencyContactAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Section 3: Compensation & YTD Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = compCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, compCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource("compensation_summary"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource("monthly_salary"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", member.salary)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = subtitleColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stringResource("ytd_salary")} ($currentYear)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", ytdPaid)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource("total_paid"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", totalPaid)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor
                                )
                            }
                        }
                    }
                }

                // Section 4: Salary Payment History
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource("salary_payment_history")} (${matchingSalaryPayments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    }

                    if (matchingSalaryPayments.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
                        ) {
                            Text(
                                text = stringResource("no_salary_records_found"),
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        matchingSalaryPayments.forEach { record ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = historyCardBg,
                                border = BorderStroke(1.dp, historyCardBorder),
                                shadowElevation = 0.5.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = subtitleColor
                                        )
                                        Text(
                                            text = record.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", record.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2))
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource("export_to_pdf"),
                        color = if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2),
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF9C27B0) else Color(0xFF7B1FA2))
                ) {
                    Text(stringResource("close"))
                }
            }
        }
    )
}

@Composable
fun AddEditStaffDialog(
    member: StaffMember? = null,
    onDismiss: () -> Unit,
    onConfirm: (StaffMember) -> Unit,
    onArchive: ((StaffMember) -> Unit)? = null
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var name by remember(member) { mutableStateOf(member?.name ?: "") }
    var role by remember(member) { mutableStateOf(member?.role ?: "") }
    var gender by remember(member) { mutableStateOf(member?.gender?.takeIf { it.isNotBlank() } ?: "Male") }
    var dob by remember(member) { mutableStateOf(member?.dateOfBirth ?: "") }
    var residentialAddress by remember(member) { mutableStateOf(member?.residentialAddress ?: "") }
    var phone by remember(member) { mutableStateOf(member?.phone ?: "") }
    var email by remember(member) { mutableStateOf(member?.email ?: "") }
    var salary by remember(member) { mutableStateOf(if (member != null && member.salary > 0.0) member.salary.toString() else "") }
    var joinDate by remember(member) { mutableStateOf(member?.joinDate ?: "") }
    var allowAppAccess by remember(member) { mutableStateOf(member?.allowAppAccess ?: false) }
    var photoUrl by remember(member) { mutableStateOf(member?.photoUrl ?: "") }

    var emergencyName by remember(member) { mutableStateOf(member?.emergencyContactName ?: "") }
    var emergencyPhone by remember(member) { mutableStateOf(member?.emergencyContactPhone ?: "") }
    var emergencyAddress by remember(member) { mutableStateOf(member?.emergencyContactAddress ?: "") }
    var emergencyRelation by remember(member) { mutableStateOf(member?.emergencyContactRelation ?: "") }

    var showArchiveConfirm by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val encoded = uriToJpegBase64(context, it)
            if (encoded != null) photoUrl = encoded
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            photoUrl = bitmapToJpegBase64(it)
        }
    }

    val joinDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            joinDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar[Calendar.YEAR],
        calendar[Calendar.MONTH],
        calendar[Calendar.DAY_OF_MONTH]
    )

    val dobPickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dob = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar[Calendar.YEAR] - 25,
        calendar[Calendar.MONTH],
        calendar[Calendar.DAY_OF_MONTH]
    )

    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFBF8FC),
        title = {
            Text(
                text = stringResource(if (member == null) "add_staff_member" else "edit_staff_member"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFCE93D8) else Color(0xFF4A148C)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StaffAvatar(
                        photoUrl = photoUrl,
                        name = name,
                        size = 80.dp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF7B1FA2))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource("take_photo"), fontSize = 11.sp, color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF7B1FA2))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource("upload_photo"), fontSize = 11.sp, color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold)
                        }

                        if (photoUrl.isNotBlank()) {
                            TextButton(
                                onClick = { photoUrl = "" },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text(stringResource("remove_photo"), fontSize = 11.sp, color = Color.Red)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource("full_name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text(stringResource("role_hint")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(stringResource("gender"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        val isSelected = gender.equals(g, ignoreCase = true)
                        val localizedGender = when (g) {
                            "Male" -> stringResource("male")
                            "Female" -> stringResource("female")
                            "Other" -> stringResource("other_gender")
                            else -> g
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF7B1FA2).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { gender = g }
                        ) {
                            Text(
                                text = localizedGender,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dob,
                    onValueChange = { },
                    label = { Text(stringResource("date_of_birth")) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { dobPickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    }
                )

                OutlinedTextField(
                    value = residentialAddress,
                    onValueChange = { residentialAddress = it },
                    label = { Text(stringResource("residential_address")) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource("phone_number")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource("staff_email")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                OutlinedTextField(
                    value = salary,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) salary = it },
                    label = { Text(stringResource("monthly_payroll")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = joinDate,
                    onValueChange = { },
                    label = { Text(stringResource("join_date")) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { joinDatePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    }
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF3E5F5).copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, Color(0xFFCE93D8).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource("emergency_contact_details"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B1FA2)
                        )
                        OutlinedTextField(
                            value = emergencyName,
                            onValueChange = { emergencyName = it },
                            label = { Text(stringResource("emergency_contact_name")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it },
                            label = { Text(stringResource("emergency_contact_phone")) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        OutlinedTextField(
                            value = emergencyAddress,
                            onValueChange = { emergencyAddress = it },
                            label = { Text(stringResource("emergency_contact_address")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = emergencyRelation,
                            onValueChange = { emergencyRelation = it },
                            label = { Text(stringResource("emergency_contact_relation")) },
                            placeholder = { Text(stringResource("emergency_relation_placeholder")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isPaidPremium = LocalIsPaidPremium.current
                    Checkbox(
                        checked = allowAppAccess && isPaidPremium,
                        onCheckedChange = { 
                            if (isPaidPremium) {
                                allowAppAccess = it
                            }
                        },
                        enabled = isPaidPremium
                    )
                    Text(stringResource("allow_app_access"))
                    if (!isPaidPremium) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Premium",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "(Premium Only)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (allowAppAccess && LocalIsPaidPremium.current) {
                    Text(
                        stringResource("invitation_note"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (member != null && onArchive != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showArchiveConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.Red),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource("archive_staff"), fontWeight = FontWeight.Bold)
                    }
                }

                // Smooth scrolling runway for keyboard
                Spacer(modifier = Modifier.height(140.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = (member ?: StaffMember()).copy(
                        name = name,
                        role = role,
                        phone = phone,
                        salary = salary.toDoubleOrNull() ?: 0.0,
                        joinDate = joinDate,
                        allowAppAccess = allowAppAccess,
                        email = email,
                        gender = gender,
                        residentialAddress = residentialAddress,
                        dateOfBirth = dob,
                        photoUrl = photoUrl,
                        emergencyContactName = emergencyName,
                        emergencyContactPhone = emergencyPhone,
                        emergencyContactAddress = emergencyAddress,
                        emergencyContactRelation = emergencyRelation
                    )
                    onConfirm(updated)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B1FA2),
                    contentColor = Color.White
                ),
                enabled = name.isNotBlank() && role.isNotBlank() && (!allowAppAccess || email.isNotBlank())
            ) {
                Text(stringResource("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"), color = Color(0xFF7B1FA2))
            }
        }
    )

    if (showArchiveConfirm && member != null && onArchive != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(stringResource("archive_staff"), fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text(stringResource("archive_staff_confirm", member.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showArchiveConfirm = false
                        onArchive(member)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource("archive_staff"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }
}

@Composable
fun LogSalaryDialog(
    member: StaffMember,
    onDismiss: () -> Unit,
    onConfirm: (StaffMember, month: String, notes: String, bonus: Double, deduction: Double, adjDesc: String) -> Unit
) {
    val calendar = Calendar.getInstance()
    val currentYear = calendar[Calendar.YEAR]
    val currentMonth = calendar[Calendar.MONTH]

    var salaryState by remember { mutableStateOf(member.salary.toString()) }
    var bonusState by remember { mutableStateOf("") }
    var deductionState by remember { mutableStateOf("") }
    var adjustmentDescState by remember { mutableStateOf("") }
    var selectedMonthIndex by remember { mutableIntStateOf(currentMonth) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var notesState by remember { mutableStateOf("") }

    val months = listOf(
        stringResource("january"), stringResource("february"), stringResource("march"),
        stringResource("april"), stringResource("may"), stringResource("june"),
        stringResource("july"), stringResource("august"), stringResource("september"),
        stringResource("october"), stringResource("november"), stringResource("december")
    )
    val years = ((currentYear - 2)..(currentYear + 1)).map { it.toString() }

    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    val baseVal = salaryState.toDoubleOrNull() ?: 0.0
    val bonusVal = bonusState.toDoubleOrNull() ?: 0.0
    val dedVal = deductionState.toDoubleOrNull() ?: 0.0
    val netPayout = maxOf(0.0, baseVal + bonusVal - dedVal)

    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFBF8FC),
        title = { Text(stringResource("log_salary"), fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFCE93D8) else Color(0xFF4A148C)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource("payment_details_for", member.name),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFCE93D8) else Color(0xFF4A148C)
                )

                OutlinedTextField(
                    value = salaryState,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) salaryState = it },
                    label = { Text(stringResource("salary")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bonusState,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) bonusState = it },
                    label = { Text(stringResource("special_bonuses")) },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = deductionState,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) deductionState = it },
                    label = { Text(stringResource("salary_advance_deductions")) },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adjustmentDescState,
                    onValueChange = { adjustmentDescState = it },
                    label = { Text(stringResource("adjustment_description")) },
                    placeholder = { Text(stringResource("payroll_notes_placeholder")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        OutlinedButton(
                            onClick = { monthExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(months.getOrElse(selectedMonthIndex) { "Month" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEachIndexed { index, m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        selectedMonthIndex = index
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { yearExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(selectedYear.toString())
                        }
                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            years.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y) },
                                    onClick = {
                                        selectedYear = y.toIntOrNull() ?: currentYear
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notesState,
                    onValueChange = { notesState = it },
                    label = { Text(stringResource("notes")) },
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF7B1FA2).copy(alpha = 0.25f) else Color(0xFFF3E5F5),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFFCE93D8).copy(alpha = 0.4f) else Color(0xFFCE93D8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource("net_payout"), fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFCE93D8) else Color(0xFF4A148C))
                        Text(
                            String.format(Locale.getDefault(), "%.2f", netPayout),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monthStr = "${months.getOrElse(selectedMonthIndex) { "" }} $selectedYear"
                    onConfirm(member, monthStr, notesState, bonusVal, dedVal, adjustmentDescState)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                enabled = baseVal > 0 || bonusVal > 0
            ) {
                Text(stringResource("confirm_payout"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"), color = Color(0xFF7B1FA2))
            }
        }
    )
}
