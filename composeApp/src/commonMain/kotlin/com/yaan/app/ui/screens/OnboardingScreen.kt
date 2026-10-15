package com.yaan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ui.components.YaanMonogram
import com.yaan.app.ui.components.YaanPrimaryButton
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.UserProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    CREATE_ACCOUNT,
    LOCAL_VAULT
}

/**
 * Modern YAAN Welcome & Authentication Portal.
 * Features:
 * 1. Quantum Orbital Beacon Hero animation with dynamic rotating cosmic rings and breathing glow halo.
 * 2. Unified authentication modes: Sign In (Supabase), Create Account (Supabase), and Local Vault (Offline 0ms).
 * 3. Strict 18+ DPDP Act 2023 compliance declaration.
 * 4. Reliable virtual keyboard focus management and dismissal on tap outside, Done action, and submission.
 */
@Composable
fun OnboardingScreen(
    onComplete: suspend (displayName: String, ageConfirmed: Boolean) -> YaanResult<UserProfile>,
    onSignIn: (suspend (email: String, password: String) -> YaanResult<UserProfile>)? = null,
    onSignUp: (suspend (email: String, password: String, displayName: String, ageConfirmed: Boolean) -> YaanResult<UserProfile>)? = null,
    initialDisplayName: String = "Elena Rostova",
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var is18PlusConfirmed by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                })
            }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    })
                }
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ── 1. Animated Quantum Orbital Beacon Hero ──────────────────────────────
            AnimatedQuantumBeacon()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to YAAN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your Autonomous Action Navigator",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ambient Neural Status Pill
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Zero-Knowledge Core • On-Device Neural Engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 2. Segmented Auth Mode Switcher ──────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AuthTabButton(
                        text = "Sign In",
                        icon = Icons.Outlined.Lock,
                        isSelected = selectedMode == AuthMode.SIGN_IN,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            errorMessage = null
                            selectedMode = AuthMode.SIGN_IN
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AuthTabButton(
                        text = "Sign Up",
                        icon = Icons.Outlined.Person,
                        isSelected = selectedMode == AuthMode.CREATE_ACCOUNT,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            errorMessage = null
                            selectedMode = AuthMode.CREATE_ACCOUNT
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AuthTabButton(
                        text = "Local Vault",
                        icon = Icons.Outlined.Shield,
                        isSelected = selectedMode == AuthMode.LOCAL_VAULT,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            errorMessage = null
                            selectedMode = AuthMode.LOCAL_VAULT
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 3. Interactive Auth Form ─────────────────────────────────────────────
            when (selectedMode) {
                AuthMode.SIGN_IN -> {
                    Text(
                        text = "Sign in to synchronize your encrypted cloud vault across all paired devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                AuthMode.CREATE_ACCOUNT -> {
                    Text(
                        text = "Create your private account powered by Supabase GoTrue Auth & Zero-Knowledge Vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            errorMessage = null
                        },
                        label = { Text("Your Display Name") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password (min 6 characters)") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Age Gate Declaration (F-006: DPDP Act 2023)
                    AgeGateDeclarationCard(
                        isConfirmed = is18PlusConfirmed,
                        onToggle = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            is18PlusConfirmed = !is18PlusConfirmed
                        }
                    )
                }

                AuthMode.LOCAL_VAULT -> {
                    Text(
                        text = "100% offline on-device storage. Hardware-backed encryption with zero cloud latency and no account required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            errorMessage = null
                        },
                        label = { Text("Your Display Name") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        trailingIcon = {
                            if (displayName.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Age Gate Declaration (F-006: DPDP Act 2023)
                    AgeGateDeclarationCard(
                        isConfirmed = is18PlusConfirmed,
                        onToggle = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            is18PlusConfirmed = !is18PlusConfirmed
                        }
                    )
                }
            }

            // ── Error Feedback Banner ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                errorMessage?.let { errorText ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 4. Primary Action Button ─────────────────────────────────────────────
            val buttonText = when (selectedMode) {
                AuthMode.SIGN_IN -> "Sign In to YAAN"
                AuthMode.CREATE_ACCOUNT -> "Create Account & Enter"
                AuthMode.LOCAL_VAULT -> "Initialize Local Vault & Enter"
            }

            val buttonIcon = when (selectedMode) {
                AuthMode.SIGN_IN -> Icons.Outlined.Lock
                AuthMode.CREATE_ACCOUNT -> Icons.Outlined.Cloud
                AuthMode.LOCAL_VAULT -> Icons.Outlined.Shield
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    YaanPrimaryButton(
                        text = buttonText,
                        leadingIcon = buttonIcon,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            errorMessage = null

                            when (selectedMode) {
                                AuthMode.SIGN_IN -> {
                                    val trimmedEmail = email.trim()
                                    if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                                        errorMessage = "Please enter a valid email address."
                                        return@YaanPrimaryButton
                                    }
                                    if (password.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@YaanPrimaryButton
                                    }

                                    coroutineScope.launch {
                                        isSubmitting = true
                                        val res = onSignIn?.invoke(trimmedEmail, password)
                                        isSubmitting = false
                                        if (res is YaanResult.Failure) {
                                            errorMessage = res.error.message
                                        }
                                    }
                                }

                                AuthMode.CREATE_ACCOUNT -> {
                                    val trimmedEmail = email.trim()
                                    val cleanName = displayName.trim().ifEmpty { "Elena Rostova" }
                                    if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                                        errorMessage = "Please enter a valid email address."
                                        return@YaanPrimaryButton
                                    }
                                    if (password.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@YaanPrimaryButton
                                    }
                                    if (!is18PlusConfirmed) {
                                        errorMessage = "YAAN Beta requires users to confirm they are 18 years or older under DPDP Act 2023."
                                        return@YaanPrimaryButton
                                    }

                                    coroutineScope.launch {
                                        isSubmitting = true
                                        val res = onSignUp?.invoke(trimmedEmail, password, cleanName, true)
                                        isSubmitting = false
                                        if (res is YaanResult.Failure) {
                                            errorMessage = res.error.message
                                        }
                                    }
                                }

                                AuthMode.LOCAL_VAULT -> {
                                    if (!is18PlusConfirmed) {
                                        errorMessage = "YAAN Beta requires users to confirm they are 18 years or older under DPDP Act 2023."
                                        return@YaanPrimaryButton
                                    }
                                    val cleanName = displayName.trim().ifEmpty { "Elena Rostova" }
                                    coroutineScope.launch {
                                        isSubmitting = true
                                        val res = onComplete(cleanName, true)
                                        isSubmitting = false
                                        if (res is YaanResult.Failure) {
                                            errorMessage = res.error.message
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer Invariant Note
            Text(
                text = "End-to-End Encrypted • AES-GCM-256 • DPDP Act 2023 Compliant",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Animated Quantum Orbital Beacon Hero
 * Renders an energetic, breathing AI core with dual counter-rotating orbital rings and micro-satellites.
 */
@Composable
private fun AnimatedQuantumBeacon(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 1. Breathing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 2. Primary rotating inner orbital ring
    val rotateInner by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        )
    )

    // 3. Counter-rotating outer orbital ring
    val rotateOuter by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing)
        )
    )

    // 4. Ambient radial aura glow
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.size(136.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Canvas drawing orbital rings & micro satellite nodes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2f

            // Radial ambient glow behind core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = auraAlpha),
                        primaryColor.copy(alpha = auraAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.95f
                ),
                radius = baseRadius * 0.95f,
                center = center
            )

            // Inner dashed orbital ring (Primary Gold/Cyan)
            drawCircle(
                color = primaryColor.copy(alpha = 0.45f),
                radius = baseRadius * 0.76f,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), phase = rotateInner * 1.5f)
                )
            )

            // Outer delicate orbital ring (Secondary accent)
            drawCircle(
                color = secondaryColor.copy(alpha = 0.35f),
                radius = baseRadius * 0.96f,
                center = center,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 20f), phase = rotateOuter * 1.2f)
                )
            )

            // Micro-satellite node 1 (on inner orbit)
            val rad1 = (rotateInner * PI / 180.0).toFloat()
            val r1 = baseRadius * 0.76f
            val node1 = Offset(center.x + r1 * cos(rad1), center.y + r1 * sin(rad1))
            drawCircle(
                color = primaryColor,
                radius = 3.5.dp.toPx(),
                center = node1
            )

            // Micro-satellite node 2 (on outer orbit, 180 deg offset)
            val rad2 = ((rotateOuter + 180f) * PI / 180.0).toFloat()
            val r2 = baseRadius * 0.96f
            val node2 = Offset(center.x + r2 * cos(rad2), center.y + r2 * sin(rad2))
            drawCircle(
                color = secondaryColor,
                radius = 2.5.dp.toPx(),
                center = node2
            )
        }

        // Centered Holographic Monogram Core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .border(
                    width = 1.5.dp,
                    color = primaryColor.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            YaanMonogram(modifier = Modifier.size(46.dp))
        }
    }
}

/**
 * Segmented Tab Button with reactive high-contrast selection pill.
 */
@Composable
private fun AuthTabButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Mandatory Age Eligibility Declaration Card (Finding F-006: DPDP Act 2023 / COPPA compliance).
 */
@Composable
private fun AgeGateDeclarationCard(
    isConfirmed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConfirmed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        color = if (isConfirmed) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isConfirmed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "I declare that I am 18 years of age or older",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Mandatory under India DPDP Act 2023 & COPPA. YAAN does not knowingly process personal data of minors without parental consent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}
