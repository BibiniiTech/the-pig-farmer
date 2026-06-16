package com.example.smartswine.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartswine.ui.theme.SmartSwineTheme

val LocalIsPremium = compositionLocalOf { false }

@Composable
fun PremiumIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Premium",
        tint = tint,
        modifier = modifier.size(16.dp)
    )
}

@Composable
fun PremiumWrapper(
    isPremium: Boolean,
    onLockedClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(modifier = Modifier.then(
            if (!isPremium) Modifier.clickable { onLockedClick() } else Modifier
        )) {
            content()
        }
        if (!isPremium) {
            PremiumIcon(
                modifier = Modifier.padding(4.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true, name = "Premium Status - Locked")
@Composable
fun PremiumWrapperLockedPreview() {
    SmartSwineTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PremiumWrapper(
                isPremium = false,
                onLockedClick = {}
            ) {
                Button(onClick = {}) {
                    Text("Export PDF (Premium)")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Premium Status - Unlocked")
@Composable
fun PremiumWrapperUnlockedPreview() {
    SmartSwineTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PremiumWrapper(
                isPremium = true,
                onLockedClick = {}
            ) {
                Button(onClick = {}) {
                    Text("Export PDF (Premium)")
                }
            }
        }
    }
}
