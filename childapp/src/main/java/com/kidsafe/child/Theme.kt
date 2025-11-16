package com.kidsafe.child

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
    headlineLarge = Typography().headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 22.sp)
)

@Composable
fun KidSafeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}