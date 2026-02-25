package com.ar.studyapp.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ar.domain.settings.model.ThemeMode
import com.ar.studyapp.AppViewModel
import com.ar.studyapp.BuildConfig
import com.ar.studyapp.R
import kotlinx.coroutines.launch

enum class ThemeModeUi { LIGHT, DARK }

data class SettingsUserUi(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean
)

/**
 * Settings route.
 *
 * Language is resolved from the device locale automatically (Android resources).
 * No in-app language overrides.
 */
@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as ComponentActivity

    val appViewModel: AppViewModel = hiltViewModel(activity)

    val themeModeDomain by appViewModel.themeMode.collectAsStateWithLifecycle()
    val userDomain by viewModel.user.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

    val themeModeUi = themeModeDomain.toUi()
    val userUi = userDomain?.toUi()

    val credentialManager = remember(context) { CredentialManager.create(context) }
    val webClientId = remember(context) { context.getString(R.string.default_web_client_id) }

    val googleSignIn = GoogleCredentialManagerSignIn(
        credentialManager = credentialManager,
        webClientId = webClientId
    )

    val locales = context.resources.configuration.locales
    android.util.Log.d("LOCALE", "locales[0]=${locales[0]}, all=$locales")

    SettingsScreen(
        user = userUi,
        authState = authUiState,
        themeMode = themeModeUi,
        onBackClick = onBackClick,

        onGoogleSignInClick = { activity: Activity ->
            if (webClientId.isBlank()) {
                val message =
                    "Missing default_web_client_id. Download an updated google-services.json from Firebase and ensure Google sign-in is enabled."
                android.util.Log.e("AUTH", message)
                viewModel.reportAuthError(message)
                return@SettingsScreen
            }

            val idToken = runCatching { googleSignIn.getIdToken(activity) }
                .getOrElse {
                    val message = "Failed to obtain Google ID token. ${it.message ?: ""}".trim()
                    android.util.Log.e("AUTH", message, it)
                    viewModel.reportAuthError(message)
                    return@SettingsScreen
                }

            if (idToken.isBlank()) {
                val message =
                    "Google ID token is empty. Check that you are using the Web client ID (default_web_client_id) and that SHA-1 is configured in Firebase/Google Cloud."
                android.util.Log.e("AUTH", message)
                viewModel.reportAuthError(message)
                return@SettingsScreen
            }

            viewModel.signInWithGoogleIdToken(idToken)
        },

        onSignOutClick = viewModel::signOut,
        onDeleteAccountClick = viewModel::deleteAccount,

        onThemeModeChange = { uiMode ->
            appViewModel.setThemeMode(uiMode.toDomain())
        },

        privacyPolicyUrl = "https://example.com/privacy",
        termsUrl = "https://example.com/terms",
        feedbackEmail = "support@example.com",
        appPackageName = context.packageName,
        appVersionName = BuildConfig.VERSION_NAME
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: SettingsUserUi?,
    authState: SettingsAuthUiState,
    themeMode: ThemeModeUi,

    onBackClick: () -> Unit,

    onGoogleSignInClick: suspend (Activity) -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,

    onThemeModeChange: (ThemeModeUi) -> Unit,

    privacyPolicyUrl: String,
    termsUrl: String,
    feedbackEmail: String,
    appPackageName: String,
    appVersionName: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) Account
            SectionCard(
                title = stringResource(R.string.settings_section_account),
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
            ) {
                authState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (user == null || user.isAnonymous) {
                    Text(
                        text = stringResource(R.string.settings_account_guest_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch { activity?.let { onGoogleSignInClick(it) } }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !authState.isInProgress
                    ) {
                        Text(stringResource(R.string.settings_google_sign_in))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = user.email ?: stringResource(R.string.settings_google_account_fallback),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_uid_short, user.uid.take(8)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSignOutClick,
                            modifier = Modifier.weight(1f),
                            enabled = !authState.isInProgress
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_sign_out))
                        }

                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !authState.isInProgress
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_delete_account))
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_delete_account_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // 2) Appearance
            SectionCard(
                title = stringResource(R.string.settings_section_appearance),
                leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) }
            ) {
                SettingRow(
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = when (themeMode) {
                        ThemeModeUi.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeModeUi.DARK -> stringResource(R.string.settings_theme_dark)
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = themeMode == ThemeModeUi.LIGHT,
                                onClick = { onThemeModeChange(ThemeModeUi.LIGHT) },
                                label = { Text(stringResource(R.string.settings_theme_light)) },
                                colors = FilterChipDefaults.filterChipColors()
                            )

                            FilterChip(
                                selected = themeMode == ThemeModeUi.DARK,
                                onClick = { onThemeModeChange(ThemeModeUi.DARK) },
                                label = { Text(stringResource(R.string.settings_theme_dark)) },
                                colors = FilterChipDefaults.filterChipColors()
                            )
                        }
                    }
                )
            }

            // 3) Privacy & Legal
            SectionCard(
                title = stringResource(R.string.settings_section_privacy_legal),
                leadingIcon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) }
            ) {
                SettingRow(
                    title = stringResource(R.string.settings_privacy_policy),
                    subtitle = stringResource(R.string.settings_open_in_browser),
                    trailing = {
                        IconButton(onClick = { openUrl(context, privacyPolicyUrl) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                )
                Divider(Modifier.padding(vertical = 6.dp))
                SettingRow(
                    title = stringResource(R.string.settings_terms),
                    subtitle = stringResource(R.string.settings_open_in_browser),
                    trailing = {
                        IconButton(onClick = { openUrl(context, termsUrl) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                )
            }

            // 4) App
            SectionCard(
                title = stringResource(R.string.settings_section_app),
                leadingIcon = { Icon(Icons.Filled.RateReview, contentDescription = null) }
            ) {
                SettingRow(
                    title = stringResource(R.string.settings_version),
                    subtitle = appVersionName,
                    trailing = {}
                )
                Divider(Modifier.padding(vertical = 6.dp))
                SettingRow(
                    title = stringResource(R.string.settings_feedback),
                    subtitle = feedbackEmail,
                    trailing = {
                        IconButton(onClick = { sendFeedbackEmail(context, feedbackEmail) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                )
                Divider(Modifier.padding(vertical = 6.dp))
                SettingRow(
                    title = stringResource(R.string.settings_rate_app),
                    subtitle = stringResource(R.string.settings_play_store),
                    trailing = {
                        IconButton(onClick = { openPlayStore(context, appPackageName) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = { Text(stringResource(R.string.settings_delete_account_dialog_message)) },
            confirmButton = {
                Button(
                    enabled = !authState.isInProgress,
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccountClick()
                    }
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !authState.isInProgress
                ) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    leadingIcon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingIcon()
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun sendFeedbackEmail(context: Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
        putExtra(Intent.EXTRA_SUBJECT, "Feedback")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No email client installed.
    }
}

private fun openPlayStore(context: Context, packageName: String) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(webIntent)
    }
}

/* -------------------- MAPPERS -------------------- */

private fun ThemeMode.toUi(): ThemeModeUi = when (this) {
    ThemeMode.LIGHT -> ThemeModeUi.LIGHT
    ThemeMode.DARK -> ThemeModeUi.DARK
}

private fun ThemeModeUi.toDomain(): ThemeMode = when (this) {
    ThemeModeUi.LIGHT -> ThemeMode.LIGHT
    ThemeModeUi.DARK -> ThemeMode.DARK
}

/**
 * Adjust this mapper if your domain UserInfo differs.
 */
private fun com.ar.domain.auth.model.UserInfo.toUi(): SettingsUserUi {
    return SettingsUserUi(
        uid = uid,
        email = email,
        isAnonymous = isAnonymous
    )
}