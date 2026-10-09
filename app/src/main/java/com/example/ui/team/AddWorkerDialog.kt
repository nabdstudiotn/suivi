package com.example.ui.team

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.AtelierDate
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Validated dialog to create a real worker profile in Room database.
 * No dummy or fake data.
 */
@Composable
fun AddWorkerDialog(
    onDismiss: () -> Unit,
    onConfirm: (firstName: String, lastName: String, role: String, hourlyRateMillimes: Long, hireDateIso: String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Piqueuse") }
    var rateInput by remember { mutableStateOf("6.500") }
    var hireDateIso by remember { mutableStateOf(AtelierDate.todayTunis().isoString) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajouter une ouvrière",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_add_worker")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it; errorMessage = null },
                    label = { Text("Prénom") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_firstname")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it; errorMessage = null },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_lastname")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it; errorMessage = null },
                    label = { Text("Rôle d'atelier (ex: Piqueuse, Coupeuse)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_role")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it; errorMessage = null },
                    label = { Text("Taux horaire en TND (ex: 6.500)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_rate")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = hireDateIso,
                    onValueChange = { hireDateIso = it; errorMessage = null },
                    label = { Text("Date d'embauche (AAAA-MM-JJ)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_hiredate")
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
                        if (firstName.isBlank() || lastName.isBlank()) {
                            errorMessage = "Veuillez renseigner le prénom et le nom."
                            return@Button
                        }
                        val cleanRate = rateInput.replace(",", ".").trim()
                        val rateDinars = cleanRate.toDoubleOrNull()
                        if (rateDinars == null || rateDinars <= 0) {
                            errorMessage = "Taux horaire invalide. Exemple : 6.500"
                            return@Button
                        }
                        val millimes = (rateDinars * 1000).toLong()
                        onConfirm(firstName, lastName, role, millimes, hireDateIso.trim())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_submit_add_worker"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Enregistrer la fiche", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
