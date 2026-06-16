package com.example.smartswine.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibiniitech.smartswine.R
import com.example.smartswine.utils.stringResource
import com.google.firebase.auth.FirebaseUser
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartswine.ui.theme.SmartSwineTheme

@Composable
fun CompleteProfileScreen(
    firebaseUser: FirebaseUser,
    onProfileCreated: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    // Parse Google Display Name to pre-fill First/Last Name
    val displayName = firebaseUser.displayName ?: ""
    val nameParts = displayName.split(" ", limit = 2)
    val initialFirstName = nameParts.getOrNull(0) ?: ""
    val initialLastName = nameParts.getOrNull(1) ?: ""
    val email = firebaseUser.email ?: ""

    val errorState = remember { mutableStateOf<String?>(null) }
    val isLoadingState = remember { mutableStateOf(false) }

    CompleteProfileContent(
        initialFirstName = initialFirstName,
        initialLastName = initialLastName,
        email = email,
        isLoading = isLoadingState.value,
        errorMessage = errorState.value,
        onComplete = { userProfile ->
            isLoadingState.value = true
            errorState.value = null

            authViewModel.createGoogleUserProfile(userProfile) { success, errMsg ->
                isLoadingState.value = false
                if (success) {
                    onProfileCreated()
                } else {
                    errorState.value = errMsg ?: "Failed to save profile"
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileContent(
    initialFirstName: String,
    initialLastName: String,
    email: String,
    onComplete: (UserProfile) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val firstNameState = remember { mutableStateOf(initialFirstName) }
    val lastNameState = remember { mutableStateOf(initialLastName) }
    val farmNameState = remember { mutableStateOf("") }
    val countryState = remember { mutableStateOf<Country?>(null) }
    val localErrorState = remember { mutableStateOf<String?>(null) }

    val countryExpandedState = remember { mutableStateOf(false) }
    val countrySearchQueryState = remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val filteredCountries = remember(countrySearchQueryState.value) {
        if (countrySearchQueryState.value.isEmpty()) countries
        else countries.filter { it.name.contains(countrySearchQueryState.value, ignoreCase = true) }
    }

    val displayError = errorMessage ?: localErrorState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(150.dp)
        )

        Text(
            text = stringResource("complete_profile"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource("setup_your_farm"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // First Name (Editable, prepopulated)
        OutlinedTextField(
            value = firstNameState.value,
            onValueChange = { firstNameState.value = it },
            label = { Text(stringResource("first_name")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Last Name (Editable, prepopulated)
        OutlinedTextField(
            value = lastNameState.value,
            onValueChange = { lastNameState.value = it },
            label = { Text(stringResource("last_name")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email (Read-Only)
        OutlinedTextField(
            value = email,
            onValueChange = {},
            label = { Text(stringResource("email")) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Farm Name (Required)
        OutlinedTextField(
            value = farmNameState.value,
            onValueChange = { farmNameState.value = it },
            label = { Text(stringResource("farm_name")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Country Selection (Required)
        ExposedDropdownMenuBox(
            expanded = countryExpandedState.value,
            onExpandedChange = { countryExpandedState.value = !countryExpandedState.value },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (countryExpandedState.value) countrySearchQueryState.value else (countryState.value?.let { "${it.flag} ${it.name}" } ?: ""),
                onValueChange = { 
                    if (countryExpandedState.value) countrySearchQueryState.value = it 
                },
                label = { Text(stringResource("country")) },
                placeholder = { if (countryExpandedState.value) Text(stringResource("search_country")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpandedState.value) },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = countryExpandedState.value,
                onDismissRequest = { 
                    countryExpandedState.value = false
                    countrySearchQueryState.value = ""
                }
            ) {
                filteredCountries.take(10).forEach { item ->
                    DropdownMenuItem(
                        text = { 
                            Row {
                                Text(item.flag, modifier = Modifier.padding(end = 8.dp))
                                Text(item.name)
                            }
                        },
                        onClick = {
                            countryState.value = item
                            countryExpandedState.value = false
                            countrySearchQueryState.value = ""
                        }
                    )
                }
                if (filteredCountries.size > 10) {
                    DropdownMenuItem(
                        text = { Text(stringResource("keep_typing"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                        onClick = {},
                        enabled = false
                    )
                }
            }
        }

        displayError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                if (firstNameState.value.isBlank() || lastNameState.value.isBlank() ||
                    farmNameState.value.isBlank() || countryState.value == null
                ) {
                    localErrorState.value = "Please fill in all fields"
                } else {
                    localErrorState.value = null

                    val userProfile = UserProfile(
                        firstName = firstNameState.value.trim(),
                        lastName = lastNameState.value.trim(),
                        farmName = farmNameState.value.trim(),
                        country = countryState.value?.name ?: "",
                        countryCode = countryState.value?.code ?: "",
                        email = email,
                        isPremium = false,
                        appLanguage = "en" // Default, will sync from settings
                    )
                    onComplete(userProfile)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource("finish_registration"))
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun CompleteProfileContentPreview() {
    SmartSwineTheme {
        CompleteProfileContent(
            initialFirstName = "John",
            initialLastName = "Doe",
            email = "john.doe@example.com",
            onComplete = {}
        )
    }
}
