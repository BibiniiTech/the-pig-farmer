package com.example.smartswine.ui.settings

import androidx.compose.ui.tooling.preview.Preview
import com.example.smartswine.ui.theme.SmartSwineTheme
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartswine.utils.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource("terms_of_service")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource("last_updated", "April 2026"),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TermSection(
                title = stringResource("tos_acceptance_title"),
                content = stringResource("tos_acceptance_content")
            )

            TermSection(
                title = stringResource("tos_description_title"),
                content = stringResource("tos_description_content")
            )

            TermSection(
                title = stringResource("tos_data_privacy_title"),
                content = stringResource("tos_data_privacy_content")
            )

            TermSection(
                title = stringResource("tos_no_medical_title"),
                content = stringResource("tos_no_medical_content")
            )

            TermSection(
                title = stringResource("tos_user_resp_title"),
                content = stringResource("tos_user_resp_content")
            )

            TermSection(
                title = stringResource("tos_prohibited_title"),
                content = stringResource("tos_prohibited_content")
            )

            TermSection(
                title = stringResource("tos_availability_title"),
                content = stringResource("tos_availability_content")
            )

            TermSection(
                title = stringResource("tos_liability_title"),
                content = stringResource("tos_liability_content")
            )

            TermSection(
                title = stringResource("tos_financial_data_title"),
                content = stringResource("tos_financial_data_content")
            )

            TermSection(
                title = stringResource("tos_changes_title"),
                content = stringResource("tos_changes_content")
            )

            TermSection(
                title = stringResource("tos_contact_title"),
                content = stringResource("tos_contact_content")
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource("contact_support", "bibiniitech@gmail.com"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TermSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TermsOfServiceScreenPreview() {
    SmartSwineTheme {
        TermsOfServiceScreen(onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
fun TermSectionPreview() {
    SmartSwineTheme {
        TermSection(
            title = "1. Sample Title",
            content = "This is sample content for the TermSection preview."
        )
    }
}
