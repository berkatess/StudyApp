package com.ar.studyapp.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// If you don't already have these icons, add:
// implementation("androidx.compose.material:material-icons-extended:<compose_version>")
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ar.domain.settings.model.ThemeMode
import com.ar.studyapp.BuildConfig
import com.ar.studyapp.R
import kotlinx.coroutines.launch

/**
 * Keep this in app/presentation.
 * Wire it from SettingsRoute with your ViewModel + Credential Manager.
 */

enum class ThemeModeUi { SYSTEM, LIGHT, DARK }

data class SettingsUserUi(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean
)

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Collect state
    val themeModeDomain by viewModel.themeMode.collectAsStateWithLifecycle()
    val languageTag by viewModel.languageTag.collectAsStateWithLifecycle()
    val userDomain by viewModel.user.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

    // Map domain -> UI
    val themeModeUi = themeModeDomain.toUi()

    val userUi = userDomain?.toUi()

    // Google sign-in helper (Credential Manager)
    // Important: Credential Manager expects the *Web* OAuth client ID (server client ID),
    // not the Android client ID. With Firebase, `default_web_client_id` is generated from
    // app/google-services.json by the Google Services Gradle plugin.
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val webClientId = remember(context) { context.getString(R.string.default_web_client_id) }

    val googleSignIn = GoogleCredentialManagerSignIn(
        credentialManager = credentialManager,
        webClientId = webClientId
    )

    if (BuildConfig.DEBUG) {
        android.util.Log.d("AUTH", "Using default_web_client_id from google-services.json")
    }

    SettingsScreen(
        // State
        user = userUi,
        authState = authUiState,
        themeMode = themeModeUi,
        languageTag = languageTag,

        // Navigation
        onBackClick = onBackClick,

        // Account actions
        onGoogleSignInClick = { activity: Activity ->
            // Do not fail silently; surface errors so it is clear why sign-in did not complete.
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

        // Settings actions
        onThemeModeChange = { uiMode ->
            viewModel.setThemeMode(uiMode.toDomain())
        },
        onLanguageChange = viewModel::setLanguage,

        // Links/actions
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
    // State
    user: SettingsUserUi?,                // null or anonymous => show Google sign-in button
    authState: SettingsAuthUiState,
    themeMode: ThemeModeUi,
    languageTag: String?,                 // null => System default

    // Navigation
    onBackClick: () -> Unit,

    // Account actions
    onGoogleSignInClick: suspend (Activity) -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,

    // Settings actions
    onThemeModeChange: (ThemeModeUi) -> Unit,
    onLanguageChange: (String?) -> Unit,

    // Links/actions
    privacyPolicyUrl: String,
    termsUrl: String,
    feedbackEmail: String,
    appPackageName: String,
    appVersionName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
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
                title = "Hesap",
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
                        text = "Giriş yapmadan kullanabilirsin. Google ile giriş yaparsan verilerin hesabına bağlanır ve cihaz değiştirince geri alabilirsin.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                activity?.let { onGoogleSignInClick(it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !authState.isInProgress
                    ) {
                        Text("Google ile giriş")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = user.email ?: "Google hesabı",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "UID: ${user.uid.take(8)}…",
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
                            Text("Çıkış")
                        }

                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !authState.isInProgress
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Hesabı sil")
                        }
                    }

                    Text(
                        text = "Not: Hesap silme işlemi bazı durumlarda “yeniden giriş gerekli” hatası verebilir. Böyle olursa tekrar Google ile giriş yapıp yeniden dene.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // 2) Appearance
            SectionCard(
                title = "Görünüm",
                leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) }
            ) {
                SettingRow(
                    title = "Tema",
                    subtitle = when (themeMode) {
                        ThemeModeUi.SYSTEM -> "Sistem"
                        ThemeModeUi.LIGHT -> "Açık"
                        ThemeModeUi.DARK -> "Koyu"
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onThemeModeChange(ThemeModeUi.SYSTEM) }) { Text("Sistem") }
                            OutlinedButton(onClick = { onThemeModeChange(ThemeModeUi.LIGHT) }) { Text("Açık") }
                            OutlinedButton(onClick = { onThemeModeChange(ThemeModeUi.DARK) }) { Text("Koyu") }
                        }
                    }
                )
            }

            // 3) Language
            SectionCard(
                title = "Dil",
                leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) }
            ) {
                val currentLangLabel = when (languageTag) {
                    null -> "Sistem"
                    "tr" -> "Türkçe"
                    "en" -> "English"
                    "es" -> "Español"
                    else -> languageTag
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Uygulama dili",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Seçili: $currentLangLabel",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onLanguageChange(null) }) { Text("Sistem") }
                        OutlinedButton(onClick = { onLanguageChange("tr") }) { Text("TR") }
                        OutlinedButton(onClick = { onLanguageChange("en") }) { Text("EN") }
                        OutlinedButton(onClick = { onLanguageChange("es") }) { Text("ES") }
                    }

                    Text(
                        text = "Bazı cihazlarda dil değişikliği için uygulamayı yeniden açman gerekebilir.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 4) Privacy & Legal
            SectionCard(
                title = "Gizlilik & Yasal",
                leadingIcon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) }
            ) {
                SettingRow(
                    title = "Gizlilik Politikası",
                    subtitle = "Tarayıcıda aç",
                    trailing = {
                        IconButton(onClick = { openUrl(context, privacyPolicyUrl) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                )
                Divider(Modifier.padding(vertical = 6.dp))
                SettingRow(
                    title = "Kullanım Şartları",
                    subtitle = "Tarayıcıda aç",
                    trailing = {
                        IconButton(onClick = { openUrl(context, termsUrl) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                )
            }

            // 5) App info
            SectionCard(
                title = "Uygulama",
                leadingIcon = { Icon(Icons.Filled.RateReview, contentDescription = null) }
            ) {
                SettingRow(
                    title = "Sürüm",
                    subtitle = appVersionName,
                    trailing = {}
                )
                Divider(Modifier.padding(vertical = 6.dp))
                SettingRow(
                    title = "Geri bildirim",
                    subtitle = feedbackEmail,
                    trailing = {
                        IconButton(onClick = { sendFeedbackEmail(context, feedbackEmail) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                )
                Divider(Modifier.padding(vertical = 6.dp))
                SettingRow(
                    title = "Uygulamayı değerlendir",
                    subtitle = "Play Store",
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
            title = { Text("Hesabı sil") },
            text = {
                Text(
                    "Hesabını silmek istediğine emin misin? Bu işlem geri alınamaz.\n\n" +
                            "Not: Bulutta tuttuğun verileri de siliyorsan ayrıca Firestore tarafında da temizleme yapman gerekebilir."
                )
            },
            confirmButton = {
                Button(
                    enabled = !authState.isInProgress,
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccountClick()
                    }
                ) { Text("Sil") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !authState.isInProgress
                ) { Text("Vazgeç") }
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
        // No email client
    }
}

private fun openPlayStore(context: Context, packageName: String) {
    // Try market:// first
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
    ThemeMode.SYSTEM -> ThemeModeUi.SYSTEM
    ThemeMode.LIGHT -> ThemeModeUi.LIGHT
    ThemeMode.DARK -> ThemeModeUi.DARK
}

private fun ThemeModeUi.toDomain(): ThemeMode = when (this) {
    ThemeModeUi.SYSTEM -> ThemeMode.SYSTEM
    ThemeModeUi.LIGHT -> ThemeMode.LIGHT
    ThemeModeUi.DARK -> ThemeMode.DARK
}

/**
 * Your domain model probably looks like this:
 * data class UserInfo(val uid: String, val email: String?, val isAnonymous: Boolean)
 *
 * If your domain UserInfo is different, adjust this mapper accordingly.
 */
private fun com.ar.domain.auth.model.UserInfo.toUi(): SettingsUserUi {
    return SettingsUserUi(
        uid = uid,
        email = email,
        isAnonymous = isAnonymous
    )
}
