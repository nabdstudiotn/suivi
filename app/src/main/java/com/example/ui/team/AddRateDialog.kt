package com.example.ui.team

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import com.example.domain.model.AtelierDate
import com.example.domain.model.Worker
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Validated dialog to record a new effective-dated hourly rate with mandatory reason (motif).
 * Persisted to hourly_rate_history table.
 */
@Composable
fun AddRateDialog(
    worker: Worker,
    onDismiss: () -> Unit,
    onConfirm: (newRateMillimes: Long, effectiveDateIso: String, motif: String) -> Unit
) {
    var rateInput by remember { mutableStateOf("") }
    var effectiveDateIso by remember { mutableStateOf(AtelierDate.todayTunis().isoString) }
    var motif by remember { mutableStateOf("Augmentation décidée par la gérante") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, NabdBorder, RoundedCornerShape(20.dp))
                .testTag("dialog_add_rate"),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nouveau taux horaire",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_add_rate")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ouvrière : ${worker.fullName} (Taux actuel : ${worker.rateFormattedTnd}/h)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it; errorMessage = null },
                    label = { Text("Nouveau taux en TND (ex: 7.200)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_rate")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = effectiveDateIso,
                    onValueChange = { effectiveDateIso = it; errorMessage = null },
                    label = { Text("Date d'effet (AAAA-MM-JJ)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rate_effective_date")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it; errorMessage = null },
                    label = { Text("Motif de modification") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rate_motif")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val cleanRate = rateInput.replace(",", ".").trim()
                        val rateDinars = cleanRate.toDoubleOrNull()
                        if (rateDinars == null || rateDinars <= 0) {
                            errorMessage = "Veuillez entrer un montant valide. Exemple : 7.200"
                            return@Button
                        }
                        if (motif.isBlank()) {
                            errorMessage = "Le motif est obligatoire pour la traçabilité d'audit."
                            return@Button
                        }
                        val millimes = (rateDinars * 1000).toLong()
                        onConfirm(millimes, effectiveDateIso.trim(), motif.trim())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_submit_add_rate"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Appliquer et enregistrer", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
