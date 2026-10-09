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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriceCheck
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
import com.example.domain.calculation.WorkerPayrollBreakdown
import com.example.domain.model.AdjustmentType
import com.example.domain.model.AtelierDate
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

/**
 * Section 3: Paie (Calcul déterministe et enregistrement des paiements).
 * Conforme à la section 5 du document des corrections.
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
    var workerForAdvance by remember { mutableStateOf<Worker?>(null) }
    var workerForAdjustment by remember { mutableStateOf<Worker?>(null) }
    var workerForPayment by remember { mutableStateOf<Worker?>(null) }

    val activeWorkers = workers.filter { it.isActive }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("payroll_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Paie & Règlements",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Text(
                    text = "Mois de calcul : $currentMonthIso • Arithmétique exacte en millimes",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdMineralGray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeWorkers.isEmpty()) {
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
                items(activeWorkers, key = { it.id }) { worker ->
                    val breakdown = breakdowns[worker.id]
                    WorkerPayrollCard(
                        worker = worker,
                        breakdown = breakdown,
                        onAddAdvance = { workerForAdvance = worker },
                        onAddAdjustment = { workerForAdjustment = worker },
                        onAddPayment = { workerForPayment = worker }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
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
    onAddAdvance: () -> Unit,
    onAddAdjustment: () -> Unit,
    onAddPayment: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(16.dp))
            .testTag("payroll_card_${worker.id}"),
        color = NabdSurfaceWhite,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Worker info & Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NabdPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.initials,
                            fontWeight = FontWeight.Bold,
                            color = NabdPurple,
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
                            text = "${worker.role} • Taux courant : ${worker.rateFormattedTnd}/h",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMineralGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Grid (Base + Primes - Retenues - Avances + Solde)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NabdSurfaceSubtle)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            Text(text = "- ${breakdown?.advancesFormattedTnd}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NabdCoralText)
                        }
                    }

                    val primesTotal = (breakdown?.primesMillimes ?: 0L) + (breakdown?.primeAidMillimes ?: 0L)
                    if (primesTotal > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "+ Primes (Assiduité / Aïd)", fontSize = 12.sp, color = NabdMintText)
                            Text(text = "+ ${breakdown?.primesFormattedTnd}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NabdMintText)
                        }
                    }

                    if ((breakdown?.retenuesMillimes ?: 0L) > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "- Retenues", fontSize = 12.sp, color = NabdCoralText)
                            Text(text = "- ${breakdown?.retenuesFormattedTnd}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NabdCoralText)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: + Avance, + Prime/Retenue, Payer
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
                        containerColor = NabdPurple,
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
