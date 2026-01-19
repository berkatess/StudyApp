package com.ar.studyapp.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notu sil") },
        text = { Text("Bu not kalıcı olarak silinsin mi?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
