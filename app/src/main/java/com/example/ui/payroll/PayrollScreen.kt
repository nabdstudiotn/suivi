package com.example.ui.payroll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.calculation.PayrollAuditGrade
import com.example.domain.calculation.PayrollEligibilityStatus
import com.example.domain.calculation.WorkerPayrollBreakdown
import com.example.domain.model.AdjustmentType
import com.example.domain.model.Worker
import com.example.domain.model.formatMillimesAsTnd
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
import kotlinx.coroutines.delay

/**
 * Section 3: Paie (Calcul déterministe et enregistrement des paiements).
 *
 * Implements strict Stage 3 Paie privacy & visibility rules:
 * - Worker names and rows remain visible and alphabetically ordered by full name.
 * - All financial amounts are HIDDEN by default (•••••• / Calcul masqué).
 * - During an open month, final calculation is NOT shown automatically; eligible only when month has ended
 *   or worker has a real persisted employment stop before month end.
 * - Conventional Eye action at top: manager selects exactly ONE worker; only that worker's calculation is revealed.
 * - Automatic re-masking after exactly 30 seconds.
 * - Non-persisted state: re-masked on timeout, leaving screen, backgrounding, worker switch, or app reopening.
 * - Grade 3 (Incomplet / À revoir) blocks approval and payment.
 */
@Composable
fun PayrollScreen(
    currentMonthIso: String,
    workers: List<Worker>,
    breakdowns: Map<String, WorkerPayrollBreakdown>,
    onRecordAdvance: (workerId: String, amountMillimes: Long, motif: String) -> Unit,
    onRecordAdjustment: (type: AdjustmentType, workerId: String, amountMillimes: Long, motif: String, note: String?) -> Unit,
    onRecordPayment: (workerId: String, amountMillimes: Long, method: String, note: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only ONE worker can have revealed calculation at a time. None revealed by default.
    var revealedWorkerId by remember { mutableStateOf<String?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(30) }

    // Dialog state for worker selection via Eye button
    var showEyeWorkerPicker by remember { mutableStateOf(false) }

    // Dialogs for financial operations
    var workerForAdvance by remember { mutableStateOf<Worker?>(null) }
    var workerForAdjustment by remember { mutableStateOf<Worker?>(null) }
    var workerForPayment by remember { mutableStateOf<Worker?>(null) }
    var blockedPaymentWarningWorker by remember { mutableStateOf<Worker?>(null) }

    // Alphabetically ordered active workers by full name (French collation)
    val sortedWorkers = remember(workers) {
        workers.filter { it.isActive }.sortedWith(compareBy(java.text.Collator.getInstance(java.util.Locale.FRENCH)) { it.fullName })
    }

    // Lifecycle observer to re-mask immediately when leaving screen or backgrounding
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                revealedWorkerId = null
                secondsRemaining = 30
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            revealedWorkerId = null
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 30-second countdown timer for revealed worker
    LaunchedEffect(revealedWorkerId) {
        if (revealedWorkerId != null) {
            secondsRemaining = 30
            while (secondsRemaining > 0) {
                delay(1000L)
                secondsRemaining--
            }
            // Automatically re-mask after exactly 30 seconds
            revealedWorkerId = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("payroll_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Screen Header with Eye action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Paie & Règlements",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Text(
                    text = "Mois : $currentMonthIso • Calculs masqués par défaut",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdMineralGray
                )
            }

            // Top Eye Action (Password visibility style for manager)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (revealedWorkerId != null) {
                    // Badge showing active reveal countdown
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NabdPurple.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NabdPurple.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("payroll_timer_badge")
                    ) {
                        Text(
                            text = "${secondsRemaining}s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NabdPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (revealedWorkerId != null) {
                            // Mask immediately on click
                            revealedWorkerId = null
                        } else {
                            // Open selector to choose ONE worker
                            showEyeWorkerPicker = true
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (revealedWorkerId != null) NabdPurple.copy(alpha = 0.15f) else NabdSurfaceWhite,
                            RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, if (revealedWorkerId != null) NabdPurple else NabdBorder, RoundedCornerShape(10.dp))
                        .testTag("btn_payroll_eye_visibility")
                ) {
                    Icon(
                        imageVector = if (revealedWorkerId != null) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (revealedWorkerId != null) "Masquer les montants" else "Afficher le calcul d'une ouvrière (30 s)",
                        tint = if (revealedWorkerId != null) NabdPurple else NabdObsidian,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Active Reveal Banner notice if worker revealed
        if (revealedWorkerId != null) {
            val revealedWorker = sortedWorkers.firstOrNull { it.id == revealedWorkerId }
            if (revealedWorker != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NabdSurfaceWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NabdPurple.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payroll_reveal_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = NabdPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Affichage temporaire : ${revealedWorker.fullName} (${secondsRemaining}s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NabdPurple
                            )
                        }

                        Text(
                            text = "Masquer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NabdMineralGray,
                            modifier = Modifier
                                .clickable { revealedWorkerId = null }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sortedWorkers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NabdSurfaceWhite)
                    .border(1.dp, NabdBorder, RoundedCornerShape(16.dp))
                    .padding(28.dp)
                    .testTag("payroll_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Aucune ouvrière active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enregistrez d'abord des ouvrières et saisissez des heures pour calculer les salaires.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NabdMineralGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedWorkers, key = { it.id }) { worker ->
                    val breakdown = breakdowns[worker.id]
                    val isRevealed = (revealedWorkerId == worker.id)

                    WorkerPayrollCard(
                        worker = worker,
                        breakdown = breakdown,
                        isRevealed = isRevealed,
                        onRevealClicked = {
                            if (revealedWorkerId == worker.id) {
                                revealedWorkerId = null
                            } else {
                                revealedWorkerId = worker.id
                            }
                        },
                        onAddAdvance = { workerForAdvance = worker },
                        onAddAdjustment = { workerForAdjustment = worker },
                        onAddPayment = {
                            if (breakdown?.isGrade3Blocked == true) {
                                blockedPaymentWarningWorker = worker
                            } else {
                                workerForPayment = worker
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal: Select ONE worker to reveal calculation for 30s
    if (showEyeWorkerPicker) {
        Dialog(onDismissRequest = { showEyeWorkerPicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("dialog_eye_worker_picker"),
                color = NabdSurfaceWhite,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Afficher un calcul",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NabdObsidian
                            )
                            Text(
                                text = "Choisissez une ouvrière • Visibilité 30 secondes",
                                style = MaterialTheme.typography.bodySmall,
                                color = NabdMineralGray
                            )
                        }
                        IconButton(onClick = { showEyeWorkerPicker = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedWorkers, key = { it.id }) { worker ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NabdSurfaceSubtle,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        revealedWorkerId = worker.id
                                        showEyeWorkerPicker = false
                                    }
                                    .testTag("picker_worker_${worker.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = worker.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NabdObsidian
                                        )
                                        Text(
                                            text = worker.role,
                                            fontSize = 12.sp,
                                            color = NabdMineralGray
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Révéler",
                                        tint = NabdPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Warning Dialog when Grade 3 blocks payment
    blockedPaymentWarningWorker?.let { worker ->
        Dialog(onDismissRequest = { blockedPaymentWarningWorker = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("dialog_grade3_blocked"),
                color = NabdSurfaceWhite,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NabdCoralText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paiement bloqué — Grade 3",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Le calcul pour ${worker.fullName} comporte des journées incomplètes (À renseigner) ou un statut non approuvé. Tout règlement est bloqué tant que les pointages ne sont pas régularisés.",
                        fontSize = 13.sp,
                        color = NabdMineralGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { blockedPaymentWarningWorker = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                    ) {
                        Text("Compris")
                    }
                }
            }
        }
    }

    // Dialog: Enregistrer une avance
    workerForAdvance?.let { worker ->
        AdvanceDialog(
            worker = worker,
            onDismiss = { workerForAdvance = null },
            onConfirm = { amount, motif ->
                onRecordAdvance(worker.id, amount, motif)
                workerForAdvance = null
            }
        )
    }

    // Dialog: Enregistrer Prime / Retenue / Prime de l'Aïd
    workerForAdjustment?.let { worker ->
        AdjustmentDialog(
            worker = worker,
            onDismiss = { workerForAdjustment = null },
            onConfirm = { type, amount, motif, note ->
                onRecordAdjustment(type, worker.id, amount, motif, note)
                workerForAdjustment = null
            }
        )
    }

    // Dialog: Enregistrer un paiement (Espèces par défaut)
    workerForPayment?.let { worker ->
        val remaining = breakdowns[worker.id]?.remainingDueMillimes ?: 0L
        PaymentDialog(
            worker = worker,
            suggestedAmountMillimes = remaining,
            onDismiss = { workerForPayment = null },
            onConfirm = { amount, method, note ->
                onRecordPayment(worker.id, amount, method, note)
                workerForPayment = null
            }
        )
    }
}

@Composable
private fun WorkerPayrollCard(
    worker: Worker,
    breakdown: WorkerPayrollBreakdown?,
    isRevealed: Boolean,
    onRevealClicked: () -> Unit,
    onAddAdvance: () -> Unit,
    onAddAdjustment: () -> Unit,
    onAddPayment: () -> Unit
) {
    val auditGrade = breakdown?.auditGrade ?: PayrollAuditGrade.GRADE_1_APPROUVE
    val eligibilityStatus = breakdown?.eligibilityStatus ?: PayrollEligibilityStatus.EN_COURS_MOIS_OUVERT
    val isEligibleForFinal = breakdown?.isEligibleForFinal ?: false
    val isGrade3 = breakdown?.isGrade3Blocked ?: false

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isRevealed) NabdPurple.copy(alpha = 0.5f) else NabdBorder,
                RoundedCornerShape(16.dp)
            )
            .testTag("payroll_card_${worker.id}"),
        color = NabdSurfaceWhite,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Worker info & Individual Eye reveal toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isRevealed) NabdPurple.copy(alpha = 0.15f) else NabdSurfaceSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.initials,
                            fontWeight = FontWeight.Bold,
                            color = if (isRevealed) NabdPurple else NabdMineralGray,
                            fontSize = 15.sp
                        )
                    }

                    Column {
                        Text(
                            text = worker.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                        Text(
                            text = worker.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMineralGray
                        )
                    }
                }

                // Individual eye button for this worker card
                IconButton(
                    onClick = onRevealClicked,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isRevealed) NabdPurple.copy(alpha = 0.1f) else Color.Transparent,
                            CircleShape
                        )
                        .testTag("btn_card_eye_${worker.id}")
                ) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isRevealed) "Masquer" else "Afficher (30 s)",
                        tint = if (isRevealed) NabdPurple else NabdMineralGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Non-financial status badges row (Visible even when masked)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calculation status badge
                StatusBadge(
                    label = when {
                        breakdown?.blankDaysCount ?: 0 > 0 -> "Incomplet"
                        isGrade3 -> "Grade 3 — À revoir"
                        !isEligibleForFinal -> "Calcul en cours"
                        eligibilityStatus == PayrollEligibilityStatus.ELIGIBLE_ARRET_ACTIVITE -> "Arrêt d'activité — Final"
                        else -> "Calcul final"
                    },
                    isError = isGrade3 || (breakdown?.blankDaysCount ?: 0 > 0),
                    isSuccess = isEligibleForFinal && !isGrade3
                )

                // Audit Grade badge (Grade 1 / 2 / 3)
                if (isRevealed) {
                    StatusBadge(
                        label = auditGrade.labelFrench,
                        isError = auditGrade == PayrollAuditGrade.GRADE_3_BLOQUE,
                        isSuccess = auditGrade == PayrollAuditGrade.GRADE_1_APPROUVE
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown Box: MASKED by default, revealed only when isRevealed == true
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NabdSurfaceSubtle)
                    .padding(12.dp)
            ) {
                if (!isRevealed) {
                    // MASKED STATE (Default): No financial amounts leak into UI, logs, accessibility, or previews
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NabdMineralGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Calcul masqué",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NabdMineralGray
                            )
                        }

                        Text(
                            text = "••••••",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NabdMineralGray,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = if (isEligibleForFinal) "Calcul final disponible • Touchez l'œil pour afficher" else "Mois en cours • Aperçu provisoire masqué",
                            fontSize = 11.sp,
                            color = NabdMineralGray
                        )
                    }
                } else {
                    // REVEALED STATE (Temporary 30 seconds only)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Header label indicating provisional vs final
                        if (!isEligibleForFinal) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF92400E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Aperçu provisoire (Mois non clôturé)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Temps : ${breakdown?.totalHoursFormatted ?: "0 h"} (${breakdown?.workedDaysCount ?: 0} j)",
                                fontSize = 12.sp,
                                color = NabdObsidian
                            )
                            Text(
                                text = "Salaire de base : ${breakdown?.basePayFormattedTnd ?: "0,000 TND"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NabdObsidian
                            )
                        }

                        if ((breakdown?.advancesMillimes ?: 0L) > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "- Avances sur salaire", fontSize = 12.sp, color = NabdCoralText)
                                Text(
                                    text = "- ${breakdown?.advancesFormattedTnd}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdCoralText
                                )
                            }
                        }

                        val primesTotal = (breakdown?.primesMillimes ?: 0L) + (breakdown?.primeAidMillimes ?: 0L)
                        if (primesTotal > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "+ Primes (Assiduité / Aïd)", fontSize = 12.sp, color = NabdMintText)
                                Text(
                                    text = "+ ${breakdown?.primesFormattedTnd}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdMintText
                                )
                            }
                        }

                        if ((breakdown?.retenuesMillimes ?: 0L) > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "- Retenues", fontSize = 12.sp, color = NabdCoralText)
                                Text(
                                    text = "- ${breakdown?.retenuesFormattedTnd}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdCoralText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Totals row: Total Dû vs Payé vs Reste
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(width = 1.dp, color = NabdBorder, shape = RoundedCornerShape(8.dp))
                                .background(NabdSurfaceWhite)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Total Dû", fontSize = 10.sp, color = NabdMineralGray)
                                Text(
                                    text = breakdown?.totalDueFormattedTnd ?: "0,000 TND",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdDeepViolet
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Payé", fontSize = 10.sp, color = NabdMineralGray)
                                Text(
                                    text = breakdown?.paidAmountFormattedTnd ?: "0,000 TND",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdMintText
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Reste à payer", fontSize = 10.sp, color = NabdMineralGray)
                                Text(
                                    text = breakdown?.remainingDueFormattedTnd ?: "0,000 TND",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((breakdown?.remainingDueMillimes ?: 0L) > 0L) NabdPurple else NabdMineralGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: + Avance, + Prime/Retenue, Paiement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddAdvance,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("btn_advance_${worker.id}")
                ) {
                    Text(text = "+ Avance", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onAddAdjustment,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("btn_adjust_${worker.id}")
                ) {
                    Text(text = "+ Prime/Retenue", fontSize = 10.sp)
                }

                Button(
                    onClick = onAddPayment,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGrade3) NabdMineralGray else NabdPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).testTag("btn_pay_${worker.id}")
                ) {
                    Text(text = "Paiement", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    isError: Boolean = false,
    isSuccess: Boolean = false
) {
    val bgColor = when {
        isError -> NabdCoralBackground
        isSuccess -> NabdMintBackground
        else -> NabdSurfaceSubtle
    }
    val borderColor = when {
        isError -> NabdCoralBorder
        isSuccess -> NabdMintBorder
        else -> NabdBorder
    }
    val textColor = when {
        isError -> NabdCoralText
        isSuccess -> NabdMintText
        else -> NabdObsidian
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AdvanceDialog(
    worker: Worker,
    onDismiss: () -> Unit,
    onConfirm: (amountMillimes: Long, motif: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("Avance sur salaire") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Enregistrer une Avance sur salaire",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = worker.fullName, style = MaterialTheme.typography.bodySmall, color = NabdMineralGray)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; error = null },
                    label = { Text("Montant en TND (ex: 50.000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it; error = null },
                    label = { Text("Motif de l'avance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val d = amountInput.replace(",", ".").toDoubleOrNull()
                        if (d == null || d <= 0) {
                            error = "Montant invalide"
                            return@Button
                        }
                        onConfirm((d * 1000).toLong(), motif)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                ) {
                    Text("Valider l'avance")
                }
            }
        }
    }
}

@Composable
private fun AdjustmentDialog(
    worker: Worker,
    onDismiss: () -> Unit,
    onConfirm: (type: AdjustmentType, amountMillimes: Long, motif: String, note: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(AdjustmentType.PRIME) }
    var amountInput by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("Prime de rendement") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Ajouter Prime / Retenue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = worker.fullName, style = MaterialTheme.typography.bodySmall, color = NabdMineralGray)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdjustmentType.values().forEach { t ->
                        val isSel = t == selectedType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NabdPurple else NabdSurfaceSubtle)
                                .clickable { selectedType = t }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t.labelFrench,
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else NabdObsidian,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; error = null },
                    label = { Text("Montant en TND") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it; error = null },
                    label = { Text("Motif (obligatoire)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note facultative") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val d = amountInput.replace(",", ".").toDoubleOrNull()
                        if (d == null || d <= 0) {
                            error = "Montant invalide"
                            return@Button
                        }
                        if (motif.isBlank()) {
                            error = "Le motif est obligatoire"
                            return@Button
                        }
                        onConfirm(selectedType, (d * 1000).toLong(), motif, note.ifBlank { null })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                ) {
                    Text("Valider")
                }
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    worker: Worker,
    suggestedAmountMillimes: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountMillimes: Long, method: String, note: String?) -> Unit
) {
    val initialText = if (suggestedAmountMillimes > 0L) {
        String.format(java.util.Locale.US, "%.3f", suggestedAmountMillimes / 1000.0)
    } else ""

    var amountInput by remember { mutableStateOf(initialText) }
    var method by remember { mutableStateOf("Espèces") }
    var note by remember { mutableStateOf("Remis en main propre") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Enregistrer un Paiement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${worker.fullName} • Solde restant : ${formatMillimesAsTnd(suggestedAmountMillimes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdMineralGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; error = null },
                    label = { Text("Montant payé en TND") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Mode de paiement (Défaut : Espèces)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Reçu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val d = amountInput.replace(",", ".").toDoubleOrNull()
                        if (d == null || d <= 0) {
                            error = "Montant invalide"
                            return@Button
                        }
                        onConfirm((d * 1000).toLong(), method.ifBlank { "Espèces" }, note.ifBlank { null })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                ) {
                    Text("Valider le paiement")
                }
            }
        }
    }
}
