package com.example.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.AtelierDate
import com.example.domain.model.Worker
import com.example.domain.model.WorkerDraft
import com.example.domain.model.normalizeForFrenchSearch
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCoralBackground
import com.example.ui.theme.NabdCoralBorder
import com.example.ui.theme.NabdCoralText
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdMintBackground
import com.example.ui.theme.NabdMintBorder
import com.example.ui.theme.NabdMintText
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Validated dialog to create a real worker profile.
 * Incorporates the Section 2 (Équipe) refined specifications:
 * - Separate first name and last name fields.
 * - Whitespace trimming (preserves valid internal spacing).
 * - Draft management (PENDING_SAVE) prompt on close without saving.
 * - Duplicate name detection (Homonym review card) without sensitive ID requirement.
 * - Role suggestions chips.
 * - Exact preview of what will be persisted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddWorkerDialog(
    initialDraft: WorkerDraft? = null,
    existingWorkers: List<Worker> = emptyList(),
    onDismiss: () -> Unit,
    onSaveDraft: (WorkerDraft) -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    onConfirm: (firstName: String, lastName: String, role: String, hourlyRateMillimes: Long, hireDateIso: String) -> Unit
) {
    var firstName by remember { mutableStateOf(initialDraft?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialDraft?.lastName ?: "") }
    var role by remember { mutableStateOf(initialDraft?.role ?: "Piqueuse") }
    var rateInput by remember { mutableStateOf(initialDraft?.rateInput ?: "6.500") }
    var hireDateIso by remember {
        mutableStateOf(
            if (!initialDraft?.hireDateIso.isNullOrBlank()) initialDraft!!.hireDateIso
            else AtelierDate.todayTunis().isoString
        )
    }
    var notes by remember { mutableStateOf(initialDraft?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Draft exit dialog state
    var showDraftExitPrompt by remember { mutableStateOf(false) }

    // Homonym review modal state
    var duplicateCandidate by remember { mutableStateOf<Worker?>(null) }

    val roleSuggestions = listOf("Piqueuse", "Coupeuse", "Finition", "Repassage", "Modéliste", "Contrôle")

    // Helper to check if user has modified anything
    val hasUnsavedContent = remember(firstName, lastName, role, rateInput, notes) {
        firstName.isNotBlank() || lastName.isNotBlank() || notes.isNotBlank() ||
                (rateInput.isNotBlank() && rateInput != "6.500")
    }

    fun handleRequestClose() {
        if (hasUnsavedContent) {
            showDraftExitPrompt = true
        } else {
            onDismiss()
        }
    }

    fun cleanInputName(raw: String): String {
        return raw.trim().replace(Regex("\\s+"), " ")
    }

    fun proceedToCreate() {
        val cleanFirst = cleanInputName(firstName)
        val cleanLast = cleanInputName(lastName)
        val cleanRate = rateInput.replace(",", ".").trim()
        val rateDinars = cleanRate.toDoubleOrNull()
        if (rateDinars == null || rateDinars <= 0) {
            errorMessage = "Taux horaire invalide. Exemple : 6.500"
            return
        }
        val millimes = (rateDinars * 1000).toLong()
        onConfirm(cleanFirst, cleanLast, role.trim(), millimes, hireDateIso.trim())
    }

    Dialog(onDismissRequest = { handleRequestClose() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, NabdBorder, RoundedCornerShape(20.dp))
                .testTag("dialog_add_worker"),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ajouter une ouvrière",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                        Text(
                            text = "Création d'une fiche d'atelier permanente",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMineralGray
                        )
                    }
                    IconButton(
                        onClick = { handleRequestClose() },
                        modifier = Modifier.testTag("btn_close_add_worker")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Identity Fields (Separated firstName & lastName)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            errorMessage = null
                        },
                        label = { Text("Prénom") },
                        placeholder = { Text("ex: Fatma") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_worker_firstname")
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it
                            errorMessage = null
                        },
                        label = { Text("Nom") },
                        placeholder = { Text("ex: Ben Ali") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_worker_lastname")
                    )
                }

                // Live Preview of Cleaned Identity
                if (firstName.isNotBlank() || lastName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NabdSurfaceSubtle)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = NabdPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nom affiché : ${cleanInputName(firstName)} ${cleanInputName(lastName)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = NabdObsidian
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Workshop Role Field
                OutlinedTextField(
                    value = role,
                    onValueChange = {
                        role = it
                        errorMessage = null
                    },
                    label = { Text("Rôle d'atelier") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_role")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Role Quick Suggestion Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    roleSuggestions.forEach { suggestion ->
                        val isSelected = role.equals(suggestion, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NabdPurple.copy(alpha = 0.15f) else NabdSurfaceSubtle)
                                .border(
                                    1.dp,
                                    if (isSelected) NabdPurple else NabdBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { role = suggestion }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                color = if (isSelected) NabdPurple else NabdMineralGray,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hourly Rate Field
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = {
                        rateInput = it
                        errorMessage = null
                    },
                    label = { Text("Taux horaire en TND") },
                    placeholder = { Text("ex: 6.500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_rate")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hire Date (Date d'embauche)
                OutlinedTextField(
                    value = hireDateIso,
                    onValueChange = {
                        hireDateIso = it
                        errorMessage = null
                    },
                    label = { Text("Date d'embauche (AAAA-MM-JJ)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_hiredate")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Optional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observations / Notes d'atelier (Optionnel)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_notes")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NabdCoralBackground)
                            .border(1.dp, NabdCoralBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = NabdCoralText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        val cleanFirst = cleanInputName(firstName)
                        val cleanLast = cleanInputName(lastName)
                        if (cleanFirst.isBlank() || cleanLast.isBlank()) {
                            errorMessage = "Veuillez renseigner le prénom et le nom de l'ouvrière."
                            return@Button
                        }
                        val cleanRate = rateInput.replace(",", ".").trim()
                        val rateDinars = cleanRate.toDoubleOrNull()
                        if (rateDinars == null || rateDinars <= 0) {
                            errorMessage = "Taux horaire invalide. Exemple : 6.500"
                            return@Button
                        }
                        if (!hireDateIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            errorMessage = "Format de date d'embauche invalide (attendu: AAAA-MM-JJ)."
                            return@Button
                        }

                        // Duplicate name check (homonym review)
                        val normFirst = normalizeForFrenchSearch(cleanFirst)
                        val normLast = normalizeForFrenchSearch(cleanLast)
                        val match = existingWorkers.firstOrNull {
                            normalizeForFrenchSearch(it.firstName) == normFirst &&
                                    normalizeForFrenchSearch(it.lastName) == normLast
                        }

                        if (match != null) {
                            // Prompt homonym review
                            duplicateCandidate = match
                        } else {
                            proceedToCreate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_add_worker"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Enregistrer la fiche", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }

    // Modal 1: Draft Exit Confirmation (PENDING_SAVE)
    if (showDraftExitPrompt) {
        AlertDialog(
            onDismissRequest = { showDraftExitPrompt = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NabdPurple
                )
            },
            title = {
                Text(
                    text = "Fiche non enregistrée",
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
            },
            text = {
                Text(
                    text = "Vous avez saisi des informations qui ne sont pas encore enregistrées. Souhaitez-vous conserver ce brouillon pour terminer plus tard ou l'abandonner ?",
                    color = NabdMineralGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val draft = WorkerDraft(
                            firstName = firstName,
                            lastName = lastName,
                            role = role,
                            rateInput = rateInput,
                            hireDateIso = hireDateIso,
                            notes = notes
                        )
                        onSaveDraft(draft)
                        showDraftExitPrompt = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("Conserver le brouillon")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onDiscardDraft()
                            showDraftExitPrompt = false
                            onDismiss()
                        }
                    ) {
                        Text("Abandonner", color = NabdCoralText)
                    }
                    OutlinedButton(
                        onClick = { showDraftExitPrompt = false }
                    ) {
                        Text("Continuer la saisie")
                    }
                }
            }
        )
    }

    // Modal 2: Homonym Duplicate Review Card
    duplicateCandidate?.let { existing ->
        AlertDialog(
            onDismissRequest = { duplicateCandidate = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NabdDeepViolet
                )
            },
            title = {
                Text(
                    text = "Homonyme déjà présent",
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Une ouvrière portant ce nom exact existe déjà dans l'atelier :",
                        color = NabdMineralGray,
                        fontSize = 13.sp
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, NabdBorder, RoundedCornerShape(10.dp)),
                        color = NabdBone
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = existing.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NabdObsidian
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (existing.isActive) NabdMintBackground else NabdCoralBackground)
                                        .border(
                                            1.dp,
                                            if (existing.isActive) NabdMintBorder else NabdCoralBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (existing.isActive) "Active" else "Archivée",
                                        color = if (existing.isActive) NabdMintText else NabdCoralText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rôle : ${existing.role}",
                                fontSize = 12.sp,
                                color = NabdMineralGray
                            )
                            Text(
                                text = "Embauchée le : ${existing.hireDateIso}",
                                fontSize = 12.sp,
                                color = NabdMineralGray
                            )
                        }
                    }

                    Text(
                        text = "Deux personnes peuvent porter le même nom. Souhaitez-vous continuer la création d'une fiche distincte avec son propre identifiant unique ?",
                        color = NabdObsidian,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        duplicateCandidate = null
                        proceedToCreate()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("Créer une fiche distincte")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { duplicateCandidate = null }
                ) {
                    Text("Vérifier la saisie")
                }
            }
        )
    }
}
