package com.example.echowithin.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.BrandInk
import com.example.echowithin.ui.theme.BrandOrange

@Composable
fun EchoWithinLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showSparkles: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ring
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandOrange, BrandAmber)
                    )
                )
        )

        // Inner dark badge
        Box(
            modifier = Modifier
                .size(size * 0.75f)
                .clip(CircleShape)
                .background(BrandInk),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎙",
                fontSize = (size.value * 0.38f).sp,
                color = Color.White
            )
        }

        if (showSparkles) {
            Text(
                text = "✦",
                color = BrandAmber,
                fontSize = (size.value * 0.18f).sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 2.dp)
            )
            Text(
                text = "✦",
                color = BrandAmber,
                fontSize = (size.value * 0.14f).sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 4.dp)
            )
            Text(
                text = "✦",
                color = BrandOrange,
                fontSize = (size.value * 0.16f).sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
fun EchoWithinBrandHeader(
    modifier: Modifier = Modifier,
    showTagline: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EchoWithinLogoBadge(size = 100.dp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "ECHO WITHIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = BrandOrange,
            letterSpacing = 2.sp
        )
        
        if (showTagline) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = "\"Unspoken but real\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandAmber.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EchoWithinTopBarTitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EchoWithinLogoBadge(size = 32.dp, showSparkles = false)
        Column {
            Text(
                text = "Echo Within",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandOrange
            )
            Text(
                text = "Unspoken but real",
                style = MaterialTheme.typography.labelSmall,
                color = BrandAmber.copy(alpha = 0.8f)
            )
        }
    }
}
