package com.example.echowithin.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    hasPin: Boolean,
    isLocked: Boolean,
    isLoading: Boolean,
    error: String?,
    onSetup: (String) -> Unit,
    onVerify: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }
    var lastErrorSignature by remember { mutableStateOf<String?>(null) }

    // Shake the whole content column every time a new error arrives. We
    // dedupe by content so repeated recompositions (e.g. a state update
    // that doesn't change the error) don't keep re-firing the animation.
    LaunchedEffect(error) {
        if (!error.isNullOrBlank() && error != lastErrorSignature) {
            lastErrorSignature = error
            // 4 oscillations, 70dp amplitude — visible but not vomit-inducing.
            val sequence = listOf(70f, -70f, 50f, -50f, 30f, -30f, 0f)
            for (x in sequence) {
                shakeOffset.animateTo(x, tween(durationMillis = 55, easing = FastOutSlowInEasing))
            }
        } else if (error == null) {
            lastErrorSignature = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { EchoWithinTopBarTitle() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .graphicsLayer { translationX = shakeOffset.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Lock State Icon
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = when {
                    isLocked -> ErrorRed.copy(alpha = 0.1f)
                    hasPin -> BrandAmber.copy(alpha = 0.1f)
                    else -> BrandOrange.copy(alpha = 0.1f)
                },
                border = BorderStroke(2.dp, when {
                    isLocked -> ErrorRed
                    hasPin -> BrandAmber
                    else -> BrandOrange
                }),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Status",
                        tint = when {
                            isLocked -> ErrorRed
                            hasPin -> BrandAmber
                            else -> BrandOrange
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when {
                        isLocked -> "App Security Lock"
                        hasPin -> "PIN Lock Active"
                        else -> "Setup Security PIN"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange
                )
                Text(
                    text = when {
                        isLocked -> "Please enter your 4-digit PIN to continue"
                        hasPin -> "Your notes are protected. You verified your PIN recently, so they are currently unlocked. You can remove PIN protection below."
                        else -> "Create a 4-digit PIN to protect your notes from unauthorized access"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // PIN Field Card (only if setting up or locked)
            if (!hasPin || isLocked) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (error != null) ErrorRed.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    pin = it
                                }
                            },
                            label = { Text("Enter 4-digit PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = error != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (error != null) ErrorRed else BrandOrange,
                                unfocusedBorderColor = if (error != null) ErrorRed.copy(alpha = 0.5f)
                                                          else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedLabelColor = if (error != null) ErrorRed else BrandOrange
                            )
                        )

                        if (error != null) {
                            Text(
                                text = error,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isLoading) {
                            CircularProgressIndicator(
                                color = BrandOrange,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(24.dp)
                            )
                        }
                    }
                }
            }

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasPin) {
                    Button(
                        onClick = {
                            onSetup(pin)
                            pin = ""
                        },
                        enabled = pin.length == 4 && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text("Set Security PIN", fontWeight = FontWeight.Bold)
                    }
                } else if (isLocked) {
                    Button(
                        onClick = {
                            onVerify(pin)
                            pin = ""
                        },
                        enabled = pin.length == 4 && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text("Unlock App", fontWeight = FontWeight.Bold)
                    }

                } else {
                    Button(
                        onClick = {
                            onRemove()
                            pin = ""
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.1f),
                            contentColor = ErrorRed
                        ),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                    ) {
                        Text("Remove PIN Protection", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
