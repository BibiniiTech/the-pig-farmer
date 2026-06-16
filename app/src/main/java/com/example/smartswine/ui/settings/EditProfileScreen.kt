package com.example.smartswine.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.ui.auth.Country
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.auth.countries
import com.example.smartswine.utils.stringResource

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
    val scrollState = rememberScrollState()

    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val farmName = remember { mutableStateOf("") }
    val country = remember { mutableStateOf<Country?>(null) }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    
    val passwordVisible = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    val message = remember { mutableStateOf<String?>(null) }
    val isError = remember { mutableStateOf(false) }
    
    val passwordsMismatchError = stringResource("passwords_mismatch")

    LaunchedEffect(userProfile) {
        userProfile?.let {
            firstName.value = it.firstName
            lastName.value = it.lastName
            farmName.value = it.farmName
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
                    val updatedProfile = UserProfile(
                        firstName = firstName.value,
                        lastName = lastName.value,
                        farmName = farmName.value,
                        country = country.value?.name ?: "",
                        countryCode = country.value?.code ?: "",
                        email = email.value,
                        isPremium = userProfile?.isPremium ?: false,
                        appLanguage = userProfile?.appLanguage ?: ""
                    )

                    onUpdateProfile(updatedProfile, email.value, password.value, confirmPassword.value) { success, msg ->
                        isLoading.value = false
                        message.value = msg
                        isError.value = !success
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
