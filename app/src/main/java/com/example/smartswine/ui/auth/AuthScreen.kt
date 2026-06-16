package com.example.smartswine.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibiniitech.smartswine.R
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.AppLanguage
import com.example.smartswine.utils.LanguageSelectionGrid
import com.example.smartswine.utils.LanguageViewModel
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.stringResource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

data class Country(val name: String, val code: String, val flag: String)

val countries = listOf(
    Country("Afghanistan", "AF", "🇦🇫"), Country("Albania", "AL", "🇦🇱"), Country("Algeria", "DZ", "🇩🇿"),
    Country("Andorra", "AD", "🇦🇩"), Country("Angola", "AO", "🇦🇴"), Country("Antigua and Barbuda", "AG", "🇦🇬"),
    Country("Argentina", "AR", "🇦🇷"), Country("Armenia", "AM", "🇦🇲"), Country("Australia", "AU", "🇦🇺"),
    Country("Austria", "AT", "🇦🇹"), Country("Azerbaijan", "AZ", "🇦🇿"), Country("Bahamas", "BS", "🇧🇸"),
    Country("Bahrain", "BH", "🇧🇭"), Country("Bangladesh", "BD", "🇧🇩"), Country("Barbados", "BB", "🇧🇧"),
    Country("Belarus", "BY", "🇧🇾"), Country("Belgium", "BE", "🇧🇪"), Country("Belize", "BZ", "🇧🇿"),
    Country("Benin", "BJ", "🇧🇯"), Country("Bhutan", "BT", "🇧🇹"), Country("Bolivia", "BO", "🇧🇴"),
    Country("Bosnia and Herzegovina", "BA", "🇧🇦"), Country("Botswana", "BW", "🇧🇼"), Country("Brazil", "BR", "🇧🇷"),
    Country("Brunei", "BN", "🇧🇳"), Country("Bulgaria", "BG", "🇧🇬"), Country("Burkina Faso", "BF", "🇧🇫"),
    Country("Burundi", "BI", "🇧🇮"), Country("Cabo Verde", "CV", "🇨🇻"), Country("Cambodia", "KH", "🇰🇭"),
    Country("Cameroon", "CM", "🇨🇲"), Country("Canada", "CA", "🇨🇦"), Country("Central African Republic", "CF", "🇨🇫"),
    Country("Chad", "TD", "🇹🇩"), Country("Chile", "CL", "🇨🇱"), Country("China", "CN", "🇨🇳"),
    Country("Colombia", "CO", "🇨🇴"), Country("Comoros", "KM", "🇰🇲"), Country("Congo", "CG", "🇨🇬"),
    Country("Costa Rica", "CR", "🇨🇷"), Country("Croatia", "HR", "🇭🇷"), Country("Cuba", "CU", "🇨🇺"),
    Country("Cyprus", "CY", "🇨🇾"), Country("Czech Republic", "CZ", "🇨🇿"), Country("Denmark", "DK", "🇩🇰"),
    Country("Djibouti", "DJ", "🇩🇯"), Country("Dominica", "DM", "🇩🇲"), Country("Dominican Republic", "DO", "🇩🇴"),
    Country("Ecuador", "EC", "🇪🇨"), Country("Egypt", "EG", "🇪🇬"), Country("El Salvador", "SV", "🇸🇻"),
    Country("Equatorial Guinea", "GQ", "🇬🇶"), Country("Eritrea", "ER", "🇪🇷"), Country("Estonia", "EE", "🇪🇪"),
    Country("Eswatini", "SZ", "🇸🇿"), Country("Ethiopia", "ET", "🇪🇹"), Country("Fiji", "FJ", "🇫🇯"),
    Country("Finland", "FI", "🇫🇮"), Country("France", "FR", "🇫🇷"), Country("Gabon", "GA", "🇬🇦"),
    Country("Gambia", "GM", "🇬🇲"), Country("Georgia", "GE", "🇬🇪"), Country("Germany", "DE", "🇩🇪"),
    Country("Ghana", "GH", "🇬🇭"), Country("Greece", "GR", "🇬🇷"), Country("Grenada", "GD", "🇬🇩"),
    Country("Guatemala", "GT", "🇬🇹"), Country("Guinea", "GN", "🇬🇳"), Country("Guinea-Bissau", "GW", "🇬🇼"),
    Country("Guyana", "GY", "🇬🇾"), Country("Haiti", "HT", "🇭🇹"), Country("Honduras", "HN", "🇭🇳"),
    Country("Hungary", "HU", "🇭🇺"), Country("Iceland", "IS", "🇮🇸"), Country("India", "IN", "🇮🇳"),
    Country("Indonesia", "ID", "🇮🇩"), Country("Iran", "IR", "🇮🇷"), Country("Iraq", "IQ", "🇮🇶"),
    Country("Ireland", "IE", "🇮🇪"), Country("Israel", "IL", "🇮🇱"), Country("Italy", "IT", "🇮🇹"),
    Country("Jamaica", "JM", "🇯🇲"), Country("Japan", "JP", "🇯🇵"), Country("Jordan", "JO", "🇯🇴"),
    Country("Kazakhstan", "KZ", "🇰🇿"), Country("Kenya", "KE", "🇰🇪"), Country("Kiribati", "KI", "🇰🇮"),
    Country("Korea, North", "KP", "🇰🇵"), Country("Korea, South", "KR", "🇰🇷"), Country("Kosovo", "XK", "🇽🇰"),
    Country("Kuwait", "KW", "🇰🇼"), Country("Kyrgyzstan", "KG", "🇰🇬"), Country("Laos", "LA", "🇱🇦"),
    Country("Latvia", "LV", "🇱🇻"), Country("Lebanon", "LB", "🇱🇧"), Country("Lesotho", "LS", "🇱🇸"),
    Country("Liberia", "LR", "🇱🇷"), Country("Libya", "LY", "🇱🇾"), Country("Liechtenstein", "LI", "🇱🇮"),
    Country("Lithuania", "LT", "🇱🇹"), Country("Luxembourg", "LU", "🇱🇺"), Country("Madagascar", "MG", "🇲🇬"),
    Country("Malawi", "MW", "🇲🇼"), Country("Malaysia", "MY", "🇲🇾"), Country("Maldives", "MV", "🇲🇻"),
    Country("Mali", "ML", "🇲🇱"), Country("Malta", "MT", "🇲🇹"), Country("Marshall Islands", "MH", "🇲🇭"),
    Country("Mauritania", "MR", "🇲🇷"), Country("Mauritius", "MU", "🇲🇺"), Country("Mexico", "MX", "🇲🇽"),
    Country("Micronesia", "FM", "🇫🇲"), Country("Moldova", "MD", "🇲🇩"), Country("Monaco", "MC", "🇲🇨"),
    Country("Mongolia", "MN", "🇲🇳"), Country("Montenegro", "ME", "🇲🇪"), Country("Morocco", "MA", "🇲🇦"),
    Country("Mozambique", "MZ", "🇲🇿"), Country("Myanmar", "MM", "🇲🇲"), Country("Namibia", "NA", "🇳🇦"),
    Country("Nauru", "NR", "🇳🇷"), Country("Nepal", "NP", "🇳🇵"), Country("Netherlands", "NL", "🇳🇱"),
    Country("New Zealand", "NZ", "🇳🇿"), Country("Nicaragua", "NI", "🇳🇮"), Country("Niger", "NE", "🇳🇪"),
    Country("Nigeria", "NG", "🇳🇬"), Country("North Macedonia", "MK", "🇲🇰"), Country("Norway", "NO", "🇳🇴"),
    Country("Oman", "OM", "🇴🇲"), Country("Pakistan", "PK", "🇵🇰"), Country("Palau", "PW", "🇵🇼"),
    Country("Panama", "PA", "🇵🇦"), Country("Papua New Guinea", "PG", "🇵🇬"), Country("Paraguay", "PY", "🇵🇾"),
    Country("Peru", "PE", "🇵🇪"), Country("Philippines", "PH", "🇵🇭"), Country("Poland", "PL", "🇵🇱"),
    Country("Portugal", "PT", "🇵🇹"), Country("Qatar", "QA", "🇶🇦"), Country("Romania", "RO", "🇷🇴"),
    Country("Russia", "RU", "🇷🇺"), Country("Rwanda", "RW", "🇷🇼"), Country("Saint Kitts and Nevis", "KN", "🇰🇳"),
    Country("Saint Lucia", "LC", "🇱🇨"), Country("Saint Vincent", "VC", "🇻🇨"), Country("Samoa", "WS", "🇼🇸"),
    Country("San Marino", "SM", "🇸🇲"), Country("Sao Tome and Principe", "ST", "🇸🇹"), Country("Saudi Arabia", "SA", "🇸🇦"),
    Country("Senegal", "SN", "🇸🇳"), Country("Serbia", "RS", "🇷🇸"), Country("Seychelles", "SC", "🇸🇨"),
    Country("Sierra Leone", "SL", "🇸🇱"), Country("Singapore", "SG", "🇸🇬"), Country("Slovakia", "SK", "🇸🇰"),
    Country("Slovenia", "SI", "🇸🇮"), Country("Solomon Islands", "SB", "🇸🇧"), Country("Somalia", "SO", "🇸🇴"),
    Country("South Africa", "ZA", "🇿🇦"), Country("South Sudan", "SS", "🇸🇸"), Country("Spain", "ES", "🇪🇸"),
    Country("Sri Lanka", "LK", "🇱🇰"), Country("Sudan", "SD", "🇸🇩"), Country("Suriname", "SR", "🇸🇷"),
    Country("Sweden", "SE", "🇸🇪"), Country("Switzerland", "CH", "🇨🇭"), Country("Syria", "SY", "🇸🇾"),
    Country("Taiwan", "TW", "🇹🇼"), Country("Tajikistan", "TJ", "🇹🇯"), Country("Tanzania", "TZ", "🇹🇿"),
    Country("Thailand", "TH", "🇹🇭"), Country("Timor-Leste", "TL", "🇹🇱"), Country("Togo", "TG", "🇹🇬"),
    Country("Tonga", "TO", "🇹🇴"), Country("Trinidad and Tobago", "TT", "🇹🇹"), Country("Tunisia", "TN", "🇹🇳"),
    Country("Turkey", "TR", "🇹🇷"), Country("Turkmenistan", "TM", "🇹🇲"), Country("Tuvalu", "TV", "🇹🇻"),
    Country("Uganda", "UG", "🇺🇬"), Country("Ukraine", "UA", "🇺🇦"), Country("United Arab Emirates", "AE", "🇦🇪"),
    Country("United Kingdom", "GB", "🇬🇧"), Country("United States", "US", "🇺🇸"), Country("Uruguay", "UY", "🇺🇾"),
    Country("Uzbekistan", "UZ", "🇺🇿"), Country("Vanuatu", "VU", "🇻🇺"), Country("Vatican City", "VA", "🇻🇦"),
    Country("Venezuela", "VE", "🇻🇪"), Country("Vietnam", "VN", "🇻🇳"), Country("Yemen", "YE", "🇾🇪"),
    Country("Zambia", "ZM", "🇿🇲"), Country("Zimbabwe", "ZW", "🇿🇼"),
)

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    languageViewModel: LanguageViewModel = viewModel()
) {
    val isLoginState = remember { mutableStateOf(value = true) }
    val firstNameState = remember { mutableStateOf("") }
    val lastNameState = remember { mutableStateOf("") }
    val farmNameState = remember { mutableStateOf("") }
    val countryState = remember { mutableStateOf<Country?>(null) }
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val confirmPasswordState = remember { mutableStateOf("") }
    val passwordVisibleState = remember { mutableStateOf(value = false) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val isLoadingState = remember { mutableStateOf(false) }
    val showResetDialogState = remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    AuthContent(
        isLogin = isLoginState.value,
        firstName = firstNameState.value,
        onFirstNameChange = { firstNameState.value = it },
        lastName = lastNameState.value,
        onLastNameChange = { lastNameState.value = it },
        farmName = farmNameState.value,
        onFarmNameChange = { farmNameState.value = it },
        country = countryState.value,
        onCountryChange = { countryState.value = it },
        email = emailState.value,
        onEmailChange = { emailState.value = it },
        password = passwordState.value,
        onPasswordChange = { passwordState.value = it },
        confirmPassword = confirmPasswordState.value,
        onConfirmPasswordChange = { confirmPasswordState.value = it },
        passwordVisible = passwordVisibleState.value,
        onPasswordVisibleToggle = { passwordVisibleState.value = !passwordVisibleState.value },
        error = errorState.value,
        isLoading = isLoadingState.value,
        onAuthClick = {
            val fillAllFieldsMsg = Translator.getString("fill_all_fields", languageViewModel.currentLanguage.value.code)
            val passwordsMismatchMsg = Translator.getString("passwords_mismatch", languageViewModel.currentLanguage.value.code)
            val passwordShortMsg = Translator.getString("password_short", languageViewModel.currentLanguage.value.code)

            if (isLoginState.value) {
                if (emailState.value.isBlank() || passwordState.value.isBlank()) {
                    errorState.value = fillAllFieldsMsg
                } else {
                    isLoadingState.value = true
                    errorState.value = null
                    auth.signInWithEmailAndPassword(emailState.value, passwordState.value)
                        .addOnCompleteListener { task ->
                            isLoadingState.value = false
                            if (task.isSuccessful) {
                                onAuthSuccess()
                            } else {
                                val loginFailedMsg = Translator.getString("login_failed", languageViewModel.currentLanguage.value.code)
                                errorState.value = Translator.translateError(task.exception?.message ?: loginFailedMsg, languageViewModel.currentLanguage.value.code)
                            }
                        }
                }
            } else {
                // Sign Up validation
                if ((firstNameState.value.isBlank()) || (lastNameState.value.isBlank()) || 
                    (farmNameState.value.isBlank()) || (countryState.value == null) ||
                    (emailState.value.isBlank()) || (passwordState.value.isBlank()) || (confirmPasswordState.value.isBlank())
                ) {
                    errorState.value = fillAllFieldsMsg
                } else if (passwordState.value != confirmPasswordState.value) {
                    errorState.value = passwordsMismatchMsg
                } else if (passwordState.value.length < 6) {
                    errorState.value = passwordShortMsg
                } else {
                    isLoadingState.value = true
                    errorState.value = null
                    auth.createUserWithEmailAndPassword(emailState.value, passwordState.value)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = task.result?.user?.uid ?: ""
                                if (userId.isBlank()) {
                                    isLoadingState.value = false
                                    errorState.value = "Failed to get user ID"
                                    return@addOnCompleteListener
                                }
                                val userData = hashMapOf(
                                    "firstName" to firstNameState.value,
                                    "lastName" to lastNameState.value,
                                    "farmName" to farmNameState.value,
                                    "country" to countryState.value?.name,
                                    "countryCode" to countryState.value?.code,
                                    "email" to emailState.value,
                                    "isPremium" to false,
                                    "appLanguage" to languageViewModel.currentLanguage.value.code,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                                db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        isLoadingState.value = false
                                        onAuthSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoadingState.value = false
                                        val profileSaveFailedMsg = Translator.getString("profile_save_failed", languageViewModel.currentLanguage.value.code)
                                        errorState.value = Translator.translateError("$profileSaveFailedMsg: ${e.message}", languageViewModel.currentLanguage.value.code)
                                    }
                            } else {
                                isLoadingState.value = false
                                val signupFailedMsg = Translator.getString("signup_failed", languageViewModel.currentLanguage.value.code)
                                errorState.value = Translator.translateError(task.exception?.message ?: signupFailedMsg, languageViewModel.currentLanguage.value.code)
                            }
                        }
                }
            }
        },
        onToggleMode = { 
            isLoginState.value = !isLoginState.value 
            errorState.value = null
        },
        onForgotPasswordClick = { showResetDialogState.value = true },
        onLanguageChange = { languageViewModel.setLanguage(it) },
        onGoogleSignInClick = {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId("988665107482-4meu3habci5lio15ao2q8hitddvt5ntf.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            coroutineScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        context = context,
                        request = request
                    )
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        isLoadingState.value = true
                        errorState.value = null
                        authViewModel.signInWithGoogle(idToken) { success, errMsg ->
                            isLoadingState.value = false
                            if (success) {
                                onAuthSuccess()
                            } else {
                                errorState.value = Translator.translateError(errMsg, languageViewModel.currentLanguage.value.code)
                            }
                        }
                    } else {
                        errorState.value = "Unexpected credential type"
                    }
                } catch (e: Exception) {
                    if (e is GetCredentialCancellationException) {
                        // User cancelled the sign-in flow, do not show an error
                    } else {
                        errorState.value = Translator.translateError(e.message, languageViewModel.currentLanguage.value.code)
                    }
                }
            }
        }
    )

    if (showResetDialogState.value) {
        val resetEmailState = remember { mutableStateOf(emailState.value) }
        val resetErrorState = remember { mutableStateOf<String?>(null) }
        val resetSentState = remember { mutableStateOf(false) }

        ResetPasswordDialog(
            email = resetEmailState.value,
            onEmailChange = { resetEmailState.value = it },
            error = resetErrorState.value,
            sent = resetSentState.value,
            onDismiss = { showResetDialogState.value = false },
        ) {
            if (resetEmailState.value.isBlank()) {
                resetErrorState.value = "Please enter your email"
            } else {
                auth.sendPasswordResetEmail(resetEmailState.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            resetSentState.value = true
                            resetErrorState.value = null
                        } else {
                            resetErrorState.value = Translator.translateError(task.exception?.message, languageViewModel.currentLanguage.value.code)
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthContent(
    isLogin: Boolean,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    farmName: String,
    onFarmNameChange: (String) -> Unit,
    country: Country?,
    onCountryChange: (Country) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleToggle: () -> Unit,
    error: String?,
    isLoading: Boolean,
    onAuthClick: () -> Unit,
    onToggleMode: () -> Unit,
    onForgotPasswordClick: (() -> Unit)?,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val countryExpandedState = remember { mutableStateOf(false) }
    val countrySearchQueryState = remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val filteredCountries = remember(countrySearchQueryState.value) {
        if (countrySearchQueryState.value.isEmpty()) countries
        else countries.filter { it.name.contains(countrySearchQueryState.value, ignoreCase = true) }
    }

    Column(
        modifier = modifier
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
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = stringResource("app_name"),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-30).dp)
        )

        Text(
            text = stringResource("app_tagline"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-20).dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text(stringResource("first_name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text(stringResource("last_name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = farmName,
                onValueChange = onFarmNameChange,
                label = { Text(stringResource("farm_name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Country Dropdown
            ExposedDropdownMenuBox(
                expanded = countryExpandedState.value,
                onExpandedChange = { countryExpandedState.value = !countryExpandedState.value },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (countryExpandedState.value) countrySearchQueryState.value else (country?.let { "${it.flag} ${it.name}" } ?: ""),
                    onValueChange = { 
                        if (countryExpandedState.value) countrySearchQueryState.value = it 
                    },
                    label = { Text(stringResource("country")) },
                    placeholder = { if (countryExpandedState.value) Text(stringResource("search_country")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpandedState.value) },
                    modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth(),
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
                                onCountryChange(item)
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource("email")) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource("password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isLogin) ImeAction.Done else ImeAction.Next),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = onPasswordVisibleToggle) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            singleLine = true
        )

        if (!isLogin) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text(stringResource("confirm_password")) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = onPasswordVisibleToggle) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                singleLine = true
            )
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAuthClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (isLogin) stringResource("login_btn") else stringResource("create_account_btn"))
            }
        }

        Button(
            onClick = onToggleMode,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = if (isLogin) stringResource("no_account") else stringResource("have_account"),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (isLogin && onForgotPasswordClick != null) {
            TextButton(onClick = { onForgotPasswordClick.invoke() }) {
                Text(stringResource("forgot_password"))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = " " + stringResource("or_use") + " ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            enabled = !isLoading
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google Icon",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource("continue_with_Google"),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Language Selection
        Text(
            text = stringResource("select_language"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LanguageSelectionGrid(onLanguageChange = onLanguageChange)
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}


@Composable
fun ResetPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    error: String?,
    sent: Boolean,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("reset_password_title")) },
        text = {
            Column {
                Text(stringResource("reset_password_msg"))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource("email")) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
                if (sent) {
                    Text(text = stringResource("reset_link_sent"), color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSend) {
                Text(stringResource("send_btn"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("close_btn"))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AuthContentLoginPreview() {
    SmartSwineTheme {
        AuthContent(
            isLogin = true,
            firstName = "",
            onFirstNameChange = {},
            lastName = "",
            onLastNameChange = {},
            farmName = "",
            onFarmNameChange = {},
            country = null,
            onCountryChange = {},
            email = "",
            onEmailChange = {},
            password = "",
            onPasswordChange = {},
            confirmPassword = "",
            onConfirmPasswordChange = {},
            passwordVisible = false,
            onPasswordVisibleToggle = {},
            error = null,
            isLoading = false,
            onAuthClick = {},
            onToggleMode = {},
            onForgotPasswordClick = {},
            onGoogleSignInClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthContentSignUpPreview() {
    SmartSwineTheme {
        AuthContent(
            isLogin = false,
            firstName = "John",
            onFirstNameChange = {},
            lastName = "Doe",
            onLastNameChange = {},
            farmName = "Happy Farm",
            onFarmNameChange = {},
            country = countries[0],
            onCountryChange = {},
            email = "test@example.com",
            onEmailChange = {},
            password = "password",
            onPasswordChange = {},
            confirmPassword = "password",
            onConfirmPasswordChange = {},
            passwordVisible = true,
            onPasswordVisibleToggle = {},
            error = "Invalid password",
            isLoading = false,
            onAuthClick = {},
            onToggleMode = {},
            onForgotPasswordClick = {},
            onGoogleSignInClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResetPasswordDialogPreview() {
    SmartSwineTheme {
        ResetPasswordDialog(
            email = "test@example.com",
            onEmailChange = {},
            error = null,
            sent = false,
            onDismiss = {},
            onSend = {}
        )
    }
}
