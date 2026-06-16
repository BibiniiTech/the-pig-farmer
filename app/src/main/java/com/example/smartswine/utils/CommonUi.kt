package com.example.smartswine.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bibiniitech.smartswine.R

@Composable
fun StylishDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, color)
                    )
                )
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_divider),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(24.dp),
            tint = color
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color, Color.Transparent)
                    )
                )
        )
    }
}
