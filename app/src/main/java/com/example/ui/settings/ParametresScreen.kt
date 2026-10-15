package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AuditActionCategory
import com.example.domain.model.AuditDateFilter
import com.example.domain.model.AuditEvent
import com.example.domain.model.DeviceSession
import com.example.domain.model.MemberRole
import com.example.domain.model.MessageSyncStatus
import com.example.domain.model.SettingsTab
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdEmerald
import com.example.ui.theme.NabdGold
import com.example.ui.theme.NabdMuted
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdRuby
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite
import com.example.ui.theme.NabdTeal

@Composable
fun ParametresScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
    ) {
        // Section Header with Tunis Time Context
        Surface(
            color = NabdSurfaceWhite,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Paramètres d'Atelier",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = NabdObsidian
                        )
                        Text(
                            text = "Gestion centralisée • Heure de Tunis (UTC+1)",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMuted
                        )
                    }

                    Surface(
                        color = NabdDeepViolet.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NabdDeepViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.userProfile?.role?.labelFrench ?: "Directrice",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NabdDeepViolet
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsTab.entries.forEach { tab ->
                        val isSelected = state.selectedTab == tab
                        val badgeCount = if (tab == SettingsTab.AUDIT_LOG) state.auditEvents.size else 0

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectTab(tab) }
                                .testTag("settings_tab_${tab.name.lowercase()}"),
                            color = if (isSelected) NabdPurple else NabdSurfaceWhite,
                            shape = RoundedCornerShape(20.dp),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, NabdBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = when (tab) {
                                    SettingsTab.COMPTE -> Icons.Default.AccountCircle
                                    SettingsTab.ENTREPRISE -> Icons.Default.Business
                                    SettingsTab.PERMISSIONS -> Icons.Default.Security
                                    SettingsTab.APPAREILS -> Icons.Default.Devices
                                    SettingsTab.AUDIT_LOG -> Icons.Default.History
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else NabdObsidian,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.labelFr,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color.White else NabdObsidian
                                )
                                if (badgeCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else NabdPurple.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "$badgeCount",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isSelected) Color.White else NabdPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feedback Banner
        if (state.feedbackMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (state.isErrorFeedback) NabdRuby.copy(alpha = 0.1f) else NabdEmerald.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isErrorFeedback) NabdRuby.copy(alpha = 0.3f) else NabdEmerald.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (state.isErrorFeedback) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (state.isErrorFeedback) NabdRuby else NabdEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.feedbackMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = NabdObsidian,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearFeedback() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = NabdMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (state.selectedTab) {
                SettingsTab.COMPTE -> AccountSettingsPanel(state = state, viewModel = viewModel)
                SettingsTab.ENTREPRISE -> CompanySettingsPanel(state = state, viewModel = viewModel)
                SettingsTab.PERMISSIONS -> PermissionsSettingsPanel(state = state, viewModel = viewModel)
                SettingsTab.APPAREILS -> DevicesSettingsPanel(state = state, viewModel = viewModel)
                SettingsTab.AUDIT_LOG -> AuditLogPanel(state = state, viewModel = viewModel)
            }
        }
    }

    // Revocation Confirmation Dialog
    if (state.sessionToRevoke != null) {
        val session = state.sessionToRevoke!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissRevokeDialog() },
            title = {
                Text(
                    text = "Révoquer l'accès à cet appareil ?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Voulez-vous vraiment déconnecter et révoquer l'accès pour l'appareil '${session.deviceName}' utilisé par ${session.userName} ? Cette action sera consignée dans l'audit d'atelier.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRevokeSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdRuby),
                    modifier = Modifier.testTag("confirm_revoke_button")
                ) {
                    Text("Révoquer l'accès", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRevokeDialog() }) {
                    Text("Annuler", color = NabdMuted)
                }
            }
        )
    }

    // Join Request Approval Dialog (Real Acceptance Flow)
    if (state.requestToApprove != null) {
        val req = state.requestToApprove!!
        val suggestedWorkerId = state.workerProfiles.firstOrNull { worker ->
            worker.fullName.trim().lowercase() == req.userFullName.trim().lowercase()
        }?.id
        var assignedRole by remember(req.id) { mutableStateOf(req.requestedRole) }
        var assignedWorkerId by remember(req.id, suggestedWorkerId) { mutableStateOf(req.assignedWorkerId ?: suggestedWorkerId) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissApproveDialog() },
            title = {
                Text(
                    text = "Valider l'adhésion à l'atelier",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NabdObsidian)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Voulez-vous accepter ${req.userFullName} dans l'équipe de l'atelier ?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NabdDeepViolet
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Email : ${req.userEmail}", style = MaterialTheme.typography.bodySmall, color = NabdMuted)
                    Text(text = "Téléphone : ${req.userPhone}", style = MaterialTheme.typography.bodySmall, color = NabdMuted)
                    if (!req.note.isNullOrBlank()) {
                        Text(text = "Note : \"${req.note}\"", style = MaterialTheme.typography.bodySmall, color = NabdDeepViolet)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Confirmer le rôle attribué :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = assignedRole == MemberRole.OUVRIERE, onClick = { assignedRole = MemberRole.OUVRIERE })
                        Text("Ouvrière", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = assignedRole == MemberRole.CHAFE, onClick = { assignedRole = MemberRole.CHAFE })
                        Text("Châfa (Superviseuse)", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (suggestedWorkerId != null) "Profil correspondant proposé :" else "Associer à un profil existant :",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { assignedWorkerId = null },
                        color = if (assignedWorkerId == null) NabdPurple.copy(alpha = 0.12f) else NabdSurfaceSubtle,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (assignedWorkerId == null) NabdPurple else NabdBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = assignedWorkerId == null, onClick = { assignedWorkerId = null })
                            Column {
                                Text("Créer un nouveau profil", fontWeight = FontWeight.SemiBold, color = NabdObsidian)
                                Text("À partir du nom de la demande", style = MaterialTheme.typography.labelSmall, color = NabdMuted)
                            }
                        }
                    }
                    state.workerProfiles.forEach { worker ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { assignedWorkerId = worker.id },
                            color = if (assignedWorkerId == worker.id) NabdTeal.copy(alpha = 0.10f) else NabdSurfaceSubtle,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (assignedWorkerId == worker.id) NabdTeal else NabdBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = assignedWorkerId == worker.id,
                                    onClick = { assignedWorkerId = worker.id }
                                )
                                Column {
                                    Text(worker.fullName, fontWeight = FontWeight.SemiBold, color = NabdObsidian)
                                    Text(worker.role, style = MaterialTheme.typography.labelSmall, color = NabdMuted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.approveJoinRequest(req.id, assignedRole, assignedWorkerId) },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                    modifier = Modifier.testTag("confirm_approve_join_request")
                ) {
                    Text("Approuver l'adhésion")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissApproveDialog() }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * 1. Mon Compte (Account) Panel
 */
@Composable
private fun AccountSettingsPanel(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val profile = state.userProfile

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = NabdPurple.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile?.displayName?.take(2)?.uppercase() ?: "DI",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NabdPurple
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = profile?.displayName ?: "Directrice Générale",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NabdObsidian
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = NabdEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Vérifié Google Auth",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = NabdEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = profile?.role?.labelFrench ?: "Directrice",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NabdMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Adresse E-mail Google",
                        style = MaterialTheme.typography.labelMedium,
                        color = NabdMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile?.email ?: "direction.nabd@gmail.com",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NabdObsidian
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Dernière connexion",
                        style = MaterialTheme.typography.labelMedium,
                        color = NabdMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile?.formattedLastLoginTunis ?: "Aujourd'hui à Tunis",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdDeepViolet
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Modifier mes informations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NabdObsidian
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = state.accountNameInput,
                        onValueChange = { viewModel.setAccountName(it) },
                        label = { Text("Nom complet") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.accountPhoneInput,
                        onValueChange = { viewModel.setAccountPhone(it) },
                        label = { Text("Numéro de téléphone") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { viewModel.saveUserProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_account_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enregistrer les modifications")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_sign_out_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NabdRuby)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Se déconnecter de Suivi")
                    }
                }
            }
        }
    }
}

/**
 * 2. Atelier & Entreprise (Company Profile) Panel
 */
@Composable
private fun CompanySettingsPanel(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Atelier Invitation Code Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NabdPurple.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NabdPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = NabdPurple, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Code d'invitation de l'atelier",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NabdObsidian
                            )
                            Text(
                                text = "Partagez ce code avec vos collaboratrices pour rejoindre l'équipe",
                                style = MaterialTheme.typography.bodySmall,
                                color = NabdMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = NabdPurple.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.companyCode.ifBlank { state.companyProfile?.companyId ?: "Atelier" },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = NabdPurple
                                )
                            )
                            Surface(
                                color = NabdEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Code Actif",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = NabdEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending Join Requests Card
        if (state.pendingRequests.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NabdGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Demandes d'adhésion en attente (${state.pendingRequests.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NabdObsidian
                                )
                                Text(
                                    text = "Examinez et validez les candidatures d'ouvrières et châfas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NabdMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        state.pendingRequests.forEach { req ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                color = NabdSurfaceSubtle,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = req.userFullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NabdObsidian)
                                        Surface(
                                            color = NabdPurple.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = req.requestedRole.labelFrench,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = NabdPurple,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Tél : ${req.userPhone} • ${req.userEmail}", style = MaterialTheme.typography.bodySmall, color = NabdDeepViolet)
                                    if (!req.note.isNullOrBlank()) {
                                        Text(text = "Note : \"${req.note}\"", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = NabdMuted)
                                    }
                                    Text(text = "Reçue le ${req.formattedDateFr}", style = MaterialTheme.typography.labelSmall, color = NabdMuted)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.refuseJoinRequest(req.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NabdRuby)
                                        ) {
                                            Text("Refuser")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.openApproveDialog(req) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                                        ) {
                                            Text("Approuver")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = NabdPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Identité Légale & Exploitation d'Atelier",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NabdObsidian
                            )
                            Text(
                                text = "Isolation des données selon le droit tunisien",
                                style = MaterialTheme.typography.bodySmall,
                                color = NabdMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = state.companyNameInput,
                        onValueChange = { viewModel.setCompanyName(it) },
                        label = { Text("Raison sociale / Nom de l'atelier") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.matriculeInput,
                        onValueChange = { viewModel.setMatricule(it) },
                        label = { Text("Matricule Fiscal tunisien") },
                        placeholder = { Text("1234567/A/M/000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("matricule_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.addressInput,
                        onValueChange = { viewModel.setAddress(it) },
                        label = { Text("Adresse de l'atelier") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.companyPhoneInput,
                        onValueChange = { viewModel.setCompanyPhone(it) },
                        label = { Text("Téléphone d'atelier") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Paramètres de Production & Pointage",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NabdDeepViolet
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.dailyHoursInput,
                            onValueChange = { viewModel.setDailyHours(it) },
                            label = { Text("Heures / jour") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("daily_hours_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NabdPurple,
                                unfocusedBorderColor = NabdBorder
                            )
                        )

                        OutlinedTextField(
                            value = state.referenceRateInput,
                            onValueChange = { viewModel.setReferenceRate(it) },
                            label = { Text("Taux réf. (TND)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reference_rate_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NabdPurple,
                                unfocusedBorderColor = NabdBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Devise du système : Dinar Tunisien (TND / Millimes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.saveCompanyProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_company_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enregistrer les informations de l'atelier")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Permissions & Droits (Roles & Permissions) Panel
 */
@Composable
private fun PermissionsSettingsPanel(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val perms = state.permissions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NabdDeepViolet.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdDeepViolet.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = NabdDeepViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Matrice des rôles : Directrice vs Châfa",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NabdDeepViolet
                        )
                        Text(
                            text = "La Directrice détient les pleins pouvoirs. Configurez ci-dessous les habilitations opérationnelles accordées à la Châfa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdObsidian
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Habilitations de la Châfa (Superviseure)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NabdObsidian
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    PermissionSwitchTile(
                        title = "Modifier le pointage des jours passés",
                        description = "Permet à la Châfa de modifier les heures et statuts de dates antérieures.",
                        checked = perms.canEditPastAttendance,
                        testTag = "switch_perm_past_attendance",
                        onCheckedChange = { viewModel.togglePermission("canEditPastAttendance", it) }
                    )

                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    PermissionSwitchTile(
                        title = "Enregistrer des acomptes aux ouvrières",
                        description = "Autorise la saisie d'avances sur salaire jusqu'à la clôture du mois.",
                        checked = perms.canRecordAdvances,
                        testTag = "switch_perm_advances",
                        onCheckedChange = { viewModel.togglePermission("canRecordAdvances", it) }
                    )

                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    PermissionSwitchTile(
                        title = "Publier des annonces d'atelier",
                        description = "Permet de diffuser des communications officielles à toute l'équipe.",
                        checked = perms.canPublishAnnouncements,
                        testTag = "switch_perm_announcements",
                        onCheckedChange = { viewModel.togglePermission("canPublishAnnouncements", it) }
                    )

                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    PermissionSwitchTile(
                        title = "Consulter les totaux financiers de paie",
                        description = "Accès en lecture aux montants des salaires et décomptes nets.",
                        checked = perms.canViewPayrollFinancials,
                        testTag = "switch_perm_payroll",
                        onCheckedChange = { viewModel.togglePermission("canViewPayrollFinancials", it) }
                    )

                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    PermissionSwitchTile(
                        title = "Gérer les fiches des ouvrières",
                        description = "Ajout de nouvelles recrues et modification des statuts actifs/archivés.",
                        checked = perms.canManageWorkers,
                        testTag = "switch_perm_workers",
                        onCheckedChange = { viewModel.togglePermission("canManageWorkers", it) }
                    )

                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    PermissionSwitchTile(
                        title = "Consulter le journal d'activité",
                        description = "Accès à l'historique et au registre chronologique des opérations.",
                        checked = perms.canViewAuditLogs,
                        testTag = "switch_perm_audit",
                        onCheckedChange = { viewModel.togglePermission("canViewAuditLogs", it) }
                    )

                    HorizontalDivider(color = NabdBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    PermissionSwitchTile(
                        title = "Clôturer et réviser les archives mensuelles",
                        description = "Autorise la Châfa à certifier, verrouiller ou réviser les archives de pointage mensuel.",
                        checked = perms.canCloseArchive,
                        testTag = "switch_perm_archive_close",
                        onCheckedChange = { viewModel.togglePermission("canCloseArchive", it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.savePermissions() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_permissions_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                        enabled = !state.isLoading
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enregistrer les permissions & auditer")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSwitchTile(
    title: String,
    description: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NabdObsidian
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = NabdMuted
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NabdPurple,
                uncheckedThumbColor = NabdMuted,
                uncheckedTrackColor = NabdBone
            )
        )
    }
}

/**
 * 4. Appareils & Sessions (Devices & Sessions) Panel
 */
@Composable
private fun DevicesSettingsPanel(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = NabdPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sessions & Terminaux Connectés",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NabdObsidian
                        )
                        Text(
                            text = "Contrôle des tablettes et téléphones autorisés à accéder à cet atelier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMuted
                        )
                    }
                }
            }
        }

        items(state.sessions) { session ->
            DeviceSessionCard(
                session = session,
                onRevoke = { viewModel.openRevokeDialog(session) }
            )
        }
    }
}

@Composable
private fun DeviceSessionCard(
    session: DeviceSession,
    onRevoke: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (session.isRevoked) NabdBone else NabdSurfaceWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (session.isCurrentDevice) NabdEmerald.copy(alpha = 0.5f) else NabdBorder
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when (session.deviceType) {
                        "TABLET" -> Icons.Default.Tablet
                        "PHONE" -> Icons.Default.Smartphone
                        else -> Icons.Default.Laptop
                    }
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (session.isCurrentDevice) NabdEmerald.copy(alpha = 0.15f) else NabdPurple.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (session.isCurrentDevice) NabdEmerald else NabdPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = session.deviceName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (session.isRevoked) NabdMuted else NabdObsidian
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${session.userName} • ${session.userRole.labelFrench}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMuted
                        )
                    }
                }

                if (session.isCurrentDevice) {
                    Surface(
                        color = NabdEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cet appareil",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NabdEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (session.isRevoked) {
                    Surface(
                        color = NabdRuby.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Accès révoqué",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NabdRuby,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = NabdBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dernière activité : ${session.formattedLastActiveTunis}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = NabdMuted
                    )
                    Text(
                        text = "Localisation : ${session.ipOrLocation}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = NabdMuted
                    )
                }

                if (!session.isCurrentDevice && !session.isRevoked) {
                    OutlinedButton(
                        onClick = onRevoke,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NabdRuby),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NabdRuby.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("revoke_session_${session.id}")
                    ) {
                        Text("Révoquer", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * 5. Journal d'Activité (Audit Log) Panel
 */
@Composable
private fun AuditLogPanel(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary stats bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuditStatBadge(
                label = "Total événements",
                value = "${state.auditEvents.size}",
                color = NabdPurple,
                modifier = Modifier.weight(1f)
            )
            AuditStatBadge(
                label = "Directrice",
                value = "${state.auditEvents.count { it.actorRole == MemberRole.DIRECTRICE }}",
                color = NabdDeepViolet,
                modifier = Modifier.weight(1f)
            )
            AuditStatBadge(
                label = "Châfa",
                value = "${state.auditEvents.count { it.actorRole == MemberRole.CHAFE }}",
                color = NabdTeal,
                modifier = Modifier.weight(1f)
            )
        }

        // Search & Filter Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                OutlinedTextField(
                    value = state.auditSearchQuery,
                    onValueChange = { viewModel.setAuditSearchQuery(it) },
                    placeholder = { Text("Rechercher dans l'audit (opérateur, action, détail)...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NabdMuted)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NabdPurple,
                        unfocusedBorderColor = NabdBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditDateFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.selectedDateFilter == filter,
                            onClick = { viewModel.selectDateFilter(filter) },
                            label = { Text(filter.labelFr, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NabdDeepViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Category Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditActionCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.selectedAuditCategory == cat,
                            onClick = { viewModel.selectAuditCategory(cat) },
                            label = { Text(cat.labelFr, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NabdPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Audit Events Chronological List
        if (state.filteredAuditEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NabdMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucun événement d'audit ne correspond à vos filtres.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NabdMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredAuditEvents, key = { it.id }) { event ->
                    AuditEventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun AuditStatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = NabdObsidian,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AuditEventCard(event: AuditEvent) {
    val categoryColor = when (event.actionType.category) {
        AuditActionCategory.POINTAGE -> NabdPurple
        AuditActionCategory.EQUIPE -> NabdTeal
        AuditActionCategory.PAIE -> NabdEmerald
        AuditActionCategory.COMMUNICATION -> NabdDeepViolet
        AuditActionCategory.PARAMETRES -> NabdGold
        AuditActionCategory.SECURITE -> NabdRuby
        else -> NabdObsidian
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (event.actionType.category) {
                                    AuditActionCategory.POINTAGE -> Icons.Default.Edit
                                    AuditActionCategory.EQUIPE -> Icons.Default.AccountCircle
                                    AuditActionCategory.PAIE -> Icons.Default.Receipt
                                    AuditActionCategory.COMMUNICATION -> Icons.Default.History
                                    AuditActionCategory.PARAMETRES -> Icons.Default.Business
                                    AuditActionCategory.SECURITE -> Icons.Default.Security
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = event.actionType.labelFr,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NabdObsidian
                        )
                        Text(
                            text = "${event.actorName} (${event.actorRole.labelFrench})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (event.actorRole == MemberRole.DIRECTRICE) NabdDeepViolet else NabdTeal
                        )
                    }
                }

                Surface(
                    color = categoryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = event.actionType.category.labelFr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.detailsFr,
                style = MaterialTheme.typography.bodyMedium,
                color = NabdObsidian
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = NabdBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.formattedDateTimeTunis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = NabdMuted
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isSynced = event.syncStatus == MessageSyncStatus.SYNCED
                    Icon(
                        imageVector = if (isSynced) Icons.Default.CheckCircle else Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (isSynced) NabdEmerald else NabdGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSynced) "Firestore vérifié" else "En attente sync",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isSynced) NabdEmerald else NabdGold
                    )
                }
            }
        }
    }
}
