package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.SuccessGreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.echowithin.presentation.viewmodel.PremiumViewModel
import com.example.echowithin.data.network.SessionManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = viewModel(factory = PremiumViewModel.factory()),
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPremiumStatus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.isPremium && !uiState.isTrial) {
                // ── Fully Premium Active State ──
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SuccessGreen.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, SuccessGreen),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Premium Active",
                            tint = SuccessGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = "Premium Active ✓",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = SuccessGreen,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You have full access to all premium features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else if (uiState.isPremium && uiState.isTrial) {
                // ── Premium Trial Active State ──
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = BrandOrange.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, BrandOrange),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Trial",
                            tint = BrandAmber,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = "You're on a 1-day premium trial",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = BrandOrange,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Enjoy premium features during your trial period. Upgrade to keep cloud sync and attachments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                // ── Upgrade State (not premium) ──
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = BrandOrange.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, BrandOrange),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Star",
                            tint = BrandAmber,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = BrandOrange,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Unlock advanced sharing, cloud community sync, and unlimited document versions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Plan Comparison Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Compare Server Sync & Backup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                        Text("Free / Guest", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Premium", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = BrandOrange)
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    
                    PlanComparisonRow(feature = "Local Notes Count", free = "Unlimited", premium = "Unlimited")
                    PlanComparisonRow(feature = "Local Note Size", free = "Unlimited", premium = "Unlimited")
                    PlanComparisonRow(feature = "Cloud Backup Limit", free = "No Backup (Offline)", premium = "Unlimited (Backed Up)")
                    PlanComparisonRow(feature = "Sync Note Size", free = "No Sync (Offline)", premium = "Up to 100k chars")
                    PlanComparisonRow(feature = "Share Links", free = "Up to 3 links", premium = "Unlimited links")
                    PlanComparisonRow(feature = "Note Locking", free = "No lock", premium = "Secure PIN lock")
                    PlanComparisonRow(feature = "Media Attachments", free = "No media", premium = "Up to 20 media")
                }
            }

            if (!uiState.isPremium || uiState.isTrial) {
                Spacer(modifier = Modifier.height(12.dp))

                // Start upgrade button
                Button(
                    onClick = {
                        try {
                            val username = SessionManager.username ?: ""
                            val upgradeUrl = if (username.isNotBlank()) {
                                "https://echowithin.xyz/profile/$username/settings"
                            } else {
                                "https://echowithin.xyz/profile_settings"
                            }
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(upgradeUrl)
                            ).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandOrange,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Upgrade via Website (KSh 50/mo)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Please open the EchoWithin profile settings page on the web platform to complete your subscription payments safely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
fun PlanComparisonRow(feature: String, free: String, premium: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = free,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = premium,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = BrandOrange
        )
    }
}

@Composable
fun PremiumFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Included Feature",
            tint = BrandAmber,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
