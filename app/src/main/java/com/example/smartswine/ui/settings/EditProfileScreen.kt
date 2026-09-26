package com.example.smartswine.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.ui.auth.Country
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.auth.countries
import com.example.smartswine.ui.components.FarmLogoImage
import com.example.smartswine.utils.stringResource
import java.io.ByteArrayOutputStream

private fun loadOrientedBitmap(context: android.content.Context, uri: Uri, maxDimension: Int = 1920): Bitmap? {
    return try {
        // 1. Decode bounds to safely check dimensions
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        // 2. Compute sample size to avoid OOM on huge photos
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        // 3. Decode bitmap safely
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        // 4. Inspect EXIF orientation
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }

        // 5. Rotate if required by EXIF orientation
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(
                decodedBitmap, 0, 0,
                decodedBitmap.width, decodedBitmap.height,
                matrix, true
            )
            if (rotated != decodedBitmap) {
                decodedBitmap.recycle()
            }
            rotated
        } else {
            decodedBitmap
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    EditProfileContent(
        userProfile = userProfile,
        onNavigateBack = onNavigateBack,
        onUpdateProfile = { updatedProfile, email, password, _, callback ->
            authViewModel.updateProfile(updatedProfile) { success, errorMsg ->
                if (success) {
                    if (email != userProfile?.email) {
                        authViewModel.updateEmail(email) { emailSuccess, emailError ->
                            if (!emailSuccess) {
                                callback(false, "Profile updated but email change failed: $emailError")
                            } else {
                                if (password.isNotEmpty()) {
                                    authViewModel.updatePassword(password) { passSuccess, passError ->
                                        if (passSuccess) {
                                            callback(true, "Profile, email, and password updated successfully")
                                        } else {
                                            callback(false, "Profile and email updated, but password change failed: $passError")
                                        }
                                    }
                                } else {
                                    callback(true, "Profile and email updated successfully")
                                }
                            }
                        }
                    } else if (password.isNotEmpty()) {
                        authViewModel.updatePassword(password) { passSuccess, passError ->
                            if (passSuccess) {
                                callback(true, "Profile and password updated successfully")
                            } else {
                                callback(false, "Profile updated, but password change failed: $passError")
                            }
                        }
                    } else {
                        callback(true, "Profile updated successfully")
                    }
                } else {
                    callback(false, "Failed to update profile: $errorMsg")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    onUpdateProfile: (UserProfile, String, String, String, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val farmName = remember { mutableStateOf("") }
    val farmLogo = remember { mutableStateOf("") }
    val country = remember { mutableStateOf<Country?>(null) }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    
    val passwordVisible = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    val message = remember { mutableStateOf<String?>(null) }
    val isError = remember { mutableStateOf(false) }
    
    val passwordsMismatchError = stringResource("passwords_mismatch")
    val bitmapToCrop = remember { mutableStateOf<Bitmap?>(null) }
    val showCropDialog = remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val loadedBitmap = loadOrientedBitmap(context, it)
            if (loadedBitmap != null) {
                bitmapToCrop.value = loadedBitmap
                showCropDialog.value = true
            }
        }
    }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            firstName.value = it.firstName
            lastName.value = it.lastName
            farmName.value = it.farmName
            farmLogo.value = it.farmLogo
            email.value = it.email
            country.value = countries.find { c -> c.name == it.country }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource("edit_profile_title")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── FARM LOGO UPLOAD SECTION ─────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        shadowElevation = 2.dp
                    ) {
                        FarmLogoImage(
                            farmLogo = farmLogo.value,
                            contentDescription = stringResource("farm_logo"),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource("farm_logo"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource("upload_custom_logo_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (farmLogo.value.isEmpty()) stringResource("upload") else stringResource("change"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (farmLogo.value.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { farmLogo.value = "" },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource("remove_logo"),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = firstName.value,
                onValueChange = { firstName.value = it },
                label = { Text(stringResource("first_name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName.value,
                onValueChange = { lastName.value = it },
                label = { Text(stringResource("last_name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = farmName.value,
                onValueChange = { farmName.value = it },
                label = { Text(stringResource("farm_name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            val countryExpanded = remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = countryExpanded.value,
                onExpandedChange = { countryExpanded.value = !countryExpanded.value },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = country.value?.let { "${it.flag} ${it.name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource("country")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded.value) },
                    modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = countryExpanded.value,
                    onDismissRequest = { countryExpanded.value = false }
                ) {
                    countries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("${item.flag} ${item.name}") },
                            onClick = {
                                country.value = item
                                countryExpanded.value = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource("login_credentials"), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text(stringResource("email")) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text(stringResource("new_password_hint")) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                singleLine = true
            )
            
            if (password.value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword.value,
                    onValueChange = { confirmPassword.value = it },
                    label = { Text(stringResource("confirm_new_password")) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }

            message.value?.let { msg ->
                Text(
                    text = msg,
                    color = if (isError.value) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password.value.isNotEmpty() && password.value != confirmPassword.value) {
                        message.value = passwordsMismatchError
                        isError.value = true
                        return@Button
                    }
                    
                    isLoading.value = true
                    val current = userProfile ?: UserProfile()
                    val updatedProfile = current.copy(
                        firstName = firstName.value,
                        lastName = lastName.value,
                        farmName = farmName.value,
                        farmLogo = farmLogo.value,
                        country = country.value?.name ?: "",
                        countryCode = country.value?.code ?: "",
                        email = email.value
                    )

                    onUpdateProfile(updatedProfile, email.value, password.value, confirmPassword.value) { success, msg ->
                        isLoading.value = false
                        message.value = msg
                        isError.value = !success
                        if (success) {
                            val selectedCountryName = country.value?.name ?: ""
                            val selectedCountryCode = country.value?.code ?: ""
                            if (selectedCountryName.isNotBlank() && selectedCountryName != userProfile?.country) {
                                SettingsViewModel.getInstance().updateCurrencyFromCountry(selectedCountryName, selectedCountryCode, autoSave = true)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading.value
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource("save_changes"))
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showCropDialog.value && bitmapToCrop.value != null) {
        LogoZoomCropDialog(
            bitmap = bitmapToCrop.value!!,
            onDismiss = { showCropDialog.value = false },
            onApply = { base64 ->
                farmLogo.value = base64
                showCropDialog.value = false
            }
        )
    }
}

@Composable
private fun LogoZoomCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val density = LocalDensity.current
    val previewSizePx = with(density) { 200.dp.toPx() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource("adjust_farm_logo"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource("zoom_and_crop"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Preview container (200.dp square)
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }

                // Zoom Slider Row with - and + buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale - 0.2f).coerceAtLeast(1f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..4f,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { scale = (scale + 0.2f).coerceAtMost(4f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                }

                TextButton(
                    onClick = {
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                ) {
                    Text(stringResource("reset"))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val targetSize = 384f
                        val resultBitmap = Bitmap.createBitmap(targetSize.toInt(), targetSize.toInt(), Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(resultBitmap)

                        val previewToTarget = targetSize / previewSizePx
                        val baseScaleTarget = maxOf(targetSize / bitmap.width.toFloat(), targetSize / bitmap.height.toFloat())

                        val matrix = Matrix()
                        // 1. Center the bitmap at origin (0, 0)
                        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                        // 2. Scale by base scale (to fit/crop canvas) and user zoom scale
                        matrix.postScale(baseScaleTarget * scale, baseScaleTarget * scale)
                        // 3. Move center to canvas center
                        matrix.postTranslate(targetSize / 2f, targetSize / 2f)
                        // 4. Move by pan offset mapped from preview coordinate space to canvas
                        matrix.postTranslate(offset.x * previewToTarget, offset.y * previewToTarget)

                        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG)
                        canvas.drawBitmap(bitmap, matrix, paint)

                        val outputStream = ByteArrayOutputStream()
                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        val byteArray = outputStream.toByteArray()
                        val base64String = "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        onApply(base64String)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            ) {
                Text(stringResource("apply_logo"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun EditProfilePreview() {
    com.example.smartswine.ui.theme.SmartSwineTheme {
        EditProfileContent(
            userProfile = UserProfile(
                firstName = "John",
                lastName = "Doe",
                farmName = "Happy Pig Farm",
                email = "john@example.com",
                country = "Kenya"
            ),
            onNavigateBack = {},
            onUpdateProfile = { _, _, _, _, _ -> }
        )
    }
}
