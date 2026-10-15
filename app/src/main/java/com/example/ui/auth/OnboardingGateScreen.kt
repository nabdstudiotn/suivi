package com.example.ui.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.domain.model.AuthGateState
import com.example.domain.model.Company
import com.example.domain.model.CompanyMember
import com.example.domain.model.JoinRequest
import com.example.domain.model.MemberRole
import com.example.domain.model.UserAccount
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdEmerald
import com.example.ui.theme.NabdGold
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdRuby
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite
import kotlinx.coroutines.launch

private enum class OnboardingMode {
    SELECTION,
    CREATE_COMPANY_DIRECTRICE,
    JOIN_COMPANY_WORKER
}

@Composable
fun OnboardingGateScreen(
    gateState: AuthGateState,
    authRepository: AuthRepository,
    onSelectWorkspace: (CompanyMember) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(OnboardingMode.SELECTION) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_gate_screen"),
        color = NabdBone
    ) {
        when (gateState) {
            is AuthGateState.ResolvingMembership -> {
                ResolvingMembershipView()
            }
            is AuthGateState.CloudUnavailable -> {
                CloudUnavailableView(
                    message = gateState.message,
                    onSignOut = { authRepository.signOut() }
                )
            }
            is AuthGateState.PendingApproval -> {
                PendingApprovalView(
                    user = gateState.user,
                    request = gateState.request,
                    onRefresh = {
                        coroutineScope.launch {
                            authRepository.pullUserMembershipsFromCloud(gateState.user.uid)
                        }
                    },
                    onSignOut = { authRepository.signOut() }
                )
            }
            is AuthGateState.Blocked -> {
                BlockedMembershipView(
                    user = gateState.user,
                    membership = gateState.membership,
                    onSignOut = { authRepository.signOut() }
                )
            }
            is AuthGateState.MultipleCompanies -> {
                MultipleCompaniesView(
                    user = gateState.user,
                    memberships = gateState.memberships,
                    onSelect = onSelectWorkspace,
                    onSignOut = { authRepository.signOut() }
                )
            }
            is AuthGateState.NoCompany -> {
                when (mode) {
                    OnboardingMode.SELECTION -> {
                        OnboardingChoiceView(
                            user = gateState.user,
                            onCreateSelected = { mode = OnboardingMode.CREATE_COMPANY_DIRECTRICE },
                            onJoinSelected = { mode = OnboardingMode.JOIN_COMPANY_WORKER },
                            onSignOut = { authRepository.signOut() }
                        )
                    }
                    OnboardingMode.CREATE_COMPANY_DIRECTRICE -> {
                        CreateCompanyDirectriceView(
                            user = gateState.user,
                            authRepository = authRepository,
                            onBack = { mode = OnboardingMode.SELECTION }
                        )
                    }
                    OnboardingMode.JOIN_COMPANY_WORKER -> {
                        JoinCompanyWorkerView(
                            user = gateState.user,
                            authRepository = authRepository,
                            onBack = { mode = OnboardingMode.SELECTION }
                        )
                    }
                }
            }
            else -> {
                ResolvingMembershipView()
            }
        }
    }
}

@Composable
private fun ResolvingMembershipView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = NabdPurple,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Vérification des accès à l'atelier...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = NabdDeepViolet
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Synchronisation sécurisée avec Firestore et le registre local",
                style = MaterialTheme.typography.bodySmall.copy(color = NabdMineralGray)
            )
        }
    }
}

@Composable
private fun OnboardingChoiceView(
    user: UserAccount,
    onCreateSelected: () -> Unit,
    onJoinSelected: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_choice_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bienvenue sur Suivi",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Bonjour ${user.displayName}, choisissez votre rôle d'accès :",
                    style = MaterialTheme.typography.bodyMedium.copy(color = NabdDeepViolet),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Card 1: Directrice
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onCreateSelected() }
                        .border(1.5.dp, NabdPurple.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .testTag("choice_create_company"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NabdSurfaceSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(NabdPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Directrice",
                                tint = NabdPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Créer un atelier (Directrice)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NabdObsidian
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Je gère mon atelier de confection, mon équipe de couturières et mes registres de paie.",
                                style = MaterialTheme.typography.bodySmall.copy(color = NabdDeepViolet, lineHeight = 16.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 2: Ouvrière / Châfa
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onJoinSelected() }
                        .border(1.dp, NabdBorder, RoundedCornerShape(18.dp))
                        .testTag("choice_join_company"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NabdSurfaceSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(NabdGold.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Ouvrière ou Châfa",
                                tint = Color(0xFFB7791F),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rejoindre un atelier (Ouvrière / Châfa)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NabdObsidian
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rejoindre l'équipe de ma directrice grâce au code d'invitation de l'atelier.",
                                style = MaterialTheme.typography.bodySmall.copy(color = NabdDeepViolet, lineHeight = 16.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_sign_out_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Déconnexion", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se déconnecter")
                }
            }
        }
    }
}

@Composable
private fun CreateCompanyDirectriceView(
    user: UserAccount,
    authRepository: AuthRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var companyName by remember { mutableStateOf("") }
    var managerFullName by remember { mutableStateOf(user.displayName) }
    var managerPhone by remember { mutableStateOf(user.phone) }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_company_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = NabdObsidian)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nouvel Atelier (Directrice)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                    )
                }

                Text(
                    text = "Configurez votre entreprise. Un code d'invitation unique sera généré pour vos employées.",
                    style = MaterialTheme.typography.bodySmall.copy(color = NabdMineralGray),
                    modifier = Modifier.padding(start = 8.dp, bottom = 14.dp)
                )

                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        color = Color(0xFFFBEBEB),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC0392B),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Nom de l'atelier *") },
                    placeholder = { Text("ex: Atelier de Confection Nabd") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = NabdPurple) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_company_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = managerFullName,
                    onValueChange = { managerFullName = it },
                    label = { Text("Nom complet de la directrice *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NabdPurple) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manager_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = managerPhone,
                    onValueChange = { managerPhone = it },
                    label = { Text("Téléphone personnel *") },
                    placeholder = { Text("ex: +216 98 123 456") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NabdPurple) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manager_phone"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = companyPhone,
                    onValueChange = { companyPhone = it },
                    label = { Text("Téléphone de l'atelier") },
                    placeholder = { Text("ex: +216 71 840 220") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NabdDeepViolet) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = companyAddress,
                    onValueChange = { companyAddress = it },
                    label = { Text("Adresse de l'atelier") },
                    placeholder = { Text("ex: Zone Industrielle, Tunis") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = NabdDeepViolet) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val cName = companyName.trim()
                        val mName = managerFullName.trim()
                        val mPhone = managerPhone.trim()

                        if (cName.length < 2) {
                            errorMessage = "Veuillez saisir le nom de votre atelier."
                            return@Button
                        }
                        if (mName.length < 2) {
                            errorMessage = "Veuillez renseigner votre nom complet."
                            return@Button
                        }
                        if (mPhone.length < 4) {
                            errorMessage = "Veuillez saisir un numéro de téléphone valide."
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = authRepository.createCompanyAndOwner(
                                companyName = cName,
                                managerFullName = mName,
                                managerPhone = mPhone,
                                companyPhone = companyPhone.trim().ifBlank { mPhone },
                                companyAddress = companyAddress.trim()
                            )
                            isLoading = false
                            if (result.isFailure) {
                                errorMessage = "Erreur de création : ${result.exceptionOrNull()?.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_confirm_create_company"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Créer l'atelier et accéder", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinCompanyWorkerView(
    user: UserAccount,
    authRepository: AuthRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var invitationCode by remember { mutableStateOf("") }
    var searchedCompany by remember { mutableStateOf<Company?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    var selectedRole by remember { mutableStateOf(MemberRole.OUVRIERE) }
    var userFullName by remember { mutableStateOf(user.displayName) }
    var userPhone by remember { mutableStateOf(user.phone) }
    var candidateNote by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("join_company_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = !isSubmitting) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = NabdObsidian)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rejoindre un Atelier",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                    )
                }

                Text(
                    text = "Demandez à votre directrice son code d'atelier (ex: NABD-7821) pour soumettre votre adhésion.",
                    style = MaterialTheme.typography.bodySmall.copy(color = NabdMineralGray),
                    modifier = Modifier.padding(start = 8.dp, bottom = 14.dp)
                )

                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        color = Color(0xFFFBEBEB),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC0392B),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Invitation Code Input & Lookup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = invitationCode,
                        onValueChange = {
                            invitationCode = it.uppercase()
                            searchedCompany = null
                        },
                        label = { Text("Code atelier d'invitation") },
                        placeholder = { Text("ex: NABD-7821") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = NabdPurple) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_invitation_code"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val code = invitationCode.trim()
                            if (code.isEmpty()) return@Button
                            isSearching = true
                            errorMessage = null
                            coroutineScope.launch {
                                val found = authRepository.searchCompanyByCodeOnline(code)
                                isSearching = false
                                if (found != null) {
                                    searchedCompany = found
                                } else {
                                    errorMessage = "Aucun atelier trouvé avec le code $code. Vérifiez auprès de votre directrice."
                                }
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("btn_search_company"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                        enabled = !isSearching && invitationCode.isNotBlank()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Chercher")
                        }
                    }
                }

                if (searchedCompany != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NabdEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = searchedCompany!!.name,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdObsidian,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Gérante : ${searchedCompany!!.ownerName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NabdDeepViolet
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Role Selection
                Text(
                    text = "Rôle demandé :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = NabdObsidian)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { selectedRole = MemberRole.OUVRIERE }
                            .padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = selectedRole == MemberRole.OUVRIERE,
                            onClick = { selectedRole = MemberRole.OUVRIERE },
                            colors = RadioButtonDefaults.colors(selectedColor = NabdPurple)
                        )
                        Text("Ouvrière (Couturière)", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedRole = MemberRole.CHAFE }
                    ) {
                        RadioButton(
                            selected = selectedRole == MemberRole.CHAFE,
                            onClick = { selectedRole = MemberRole.CHAFE },
                            colors = RadioButtonDefaults.colors(selectedColor = NabdPurple)
                        )
                        Text("Châfa (Superviseuse)", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = userFullName,
                    onValueChange = { userFullName = it },
                    label = { Text("Votre nom et prénom *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NabdDeepViolet) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = userPhone,
                    onValueChange = { userPhone = it },
                    label = { Text("Votre numéro de téléphone *") },
                    placeholder = { Text("ex: +216 98 123 456") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NabdDeepViolet) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_worker_phone"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = candidateNote,
                    onValueChange = { candidateNote = it },
                    label = { Text("Note pour la directrice (facultatif)") },
                    placeholder = { Text("ex: Piqueuse confirmée, 5 ans d'expérience") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val company = searchedCompany
                        if (company == null) {
                            errorMessage = "Veuillez d'abord rechercher et valider le code d'atelier."
                            return@Button
                        }
                        val name = userFullName.trim()
                        val phone = userPhone.trim()

                        if (name.length < 2) {
                            errorMessage = "Veuillez renseigner votre nom complet."
                            return@Button
                        }
                        if (phone.length < 4) {
                            errorMessage = "Veuillez saisir votre numéro de téléphone."
                            return@Button
                        }

                        isSubmitting = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = authRepository.submitJoinRequest(
                                companyId = company.id,
                                companyName = company.name,
                                requestedRole = selectedRole,
                                userFullName = name,
                                userPhone = phone,
                                note = candidateNote.trim().ifBlank { null }
                            )
                            isSubmitting = false
                            if (result.isFailure) {
                                errorMessage = "Erreur : ${result.exceptionOrNull()?.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_submit_join_request"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                    enabled = !isSubmitting && searchedCompany != null
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Envoyer ma demande d'adhésion", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudUnavailableView(
    message: String,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = NabdRuby,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Connexion Firebase indisponible",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NabdObsidian,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdDeepViolet,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedButton(onClick = onSignOut) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se déconnecter et revenir à l'accueil")
                }
            }
        }
    }
}

@Composable
private fun PendingApprovalView(
    user: UserAccount,
    request: JoinRequest,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pending_approval_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(NabdGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "En attente",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Demande en attente de validation",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = NabdSurfaceSubtle,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Atelier : ${request.companyName}",
                            fontWeight = FontWeight.Bold,
                            color = NabdPurple,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rôle demandé : ${request.requestedRole.labelFrench}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdDeepViolet
                        )
                        Text(
                            text = "Envoyée le : ${request.formattedDateFr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NabdMineralGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Votre demande a été transmise à la directrice de l'atelier. Dès son approbation, votre espace de travail Suivi s'activera automatiquement.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NabdDeepViolet,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_refresh_status"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualiser le statut", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se déconnecter")
                }
            }
        }
    }
}

@Composable
private fun BlockedMembershipView(
    user: UserAccount,
    membership: CompanyMember,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("blocked_membership_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(NabdRuby.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Bloqué",
                        tint = NabdRuby,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Accès suspendu",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NabdRuby
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Votre accès à l'atelier \"${membership.companyName}\" a été suspendu par la direction. Veuillez contacter votre directrice pour plus de précisions.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NabdDeepViolet,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdObsidian)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se déconnecter")
                }
            }
        }
    }
}

@Composable
private fun MultipleCompaniesView(
    user: UserAccount,
    memberships: List<CompanyMember>,
    onSelect: (CompanyMember) -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("multiple_companies_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Vos Ateliers",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                )
                Text(
                    text = "Sélectionnez l'atelier à ouvrir :",
                    style = MaterialTheme.typography.bodySmall.copy(color = NabdMineralGray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                memberships.forEach { member ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSelect(member) },
                        colors = CardDefaults.cardColors(containerColor = NabdSurfaceSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(NabdPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = NabdPurple)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = member.companyName, fontWeight = FontWeight.Bold, color = NabdObsidian)
                                Text(text = "Rôle : ${member.role.labelFrench}", style = MaterialTheme.typography.bodySmall, color = NabdDeepViolet)
                            }
                            Button(
                                onClick = { onSelect(member) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                            ) {
                                Text("Ouvrir")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Se déconnecter")
                }
            }
        }
    }
}
