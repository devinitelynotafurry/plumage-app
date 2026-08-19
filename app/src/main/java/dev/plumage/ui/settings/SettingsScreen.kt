package dev.plumage.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.plumage.ui.common.PlumageTopBar
import dev.plumage.ui.theme.dynamicColorAvailable

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val seenCount by viewModel.seenCount.collectAsState()
    var confirmErase by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 32.dp)
    ) {
        PlumageTopBar(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            SectionHeader("Appearance")
            ToggleRow(
                title = "Use device colors",
                subtitle = if (dynamicColorAvailable) {
                    "Pulls the palette from your wallpaper"
                } else {
                    "Unavailable on this device, using the built-in palette"
                },
                checked = settings.useDynamicColor && dynamicColorAvailable,
                enabled = dynamicColorAvailable,
                onCheckedChange = viewModel::setDynamicColor
            )

            SectionHeader("Identity")
            FieldRow(
                label = "e926 username",
                value = settings.username,
                onValueChange = viewModel::setUsername,
                helper = "e926 requires every client to identify itself and blocks " +
                    "anything pretending to be a browser. Your name travels with each " +
                    "request so they can contact you instead of banning the app."
            )

            SectionHeader("Filtering")
            FieldRow(
                label = "Blocked tags",
                value = settings.blockedTags,
                onValueChange = viewModel::setBlockedTags,
                helper = "Space separated. Posts carrying any of these never reach the " +
                    "stack. The safe-rating check runs underneath this and cannot be " +
                    "turned off."
            )
            ToggleRow(
                title = "Hide AI generated art",
                subtitle = "Excludes -ai_generated at the API and drops anything tagged " +
                    "as AI that slips through",
                checked = settings.filterAiContent,
                onCheckedChange = viewModel::setFilterAi
            )

            SectionHeader("Data")
            Text(
                text = "Collections and swipe history stay on this device. Nothing is uploaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            DangerAction(
                label = "Forget swipe history",
                detail = if (seenCount == 0) "Nothing recorded yet"
                else "$seenCount posts remembered",
                onClick = viewModel::forgetSwipeHistory
            )
            DangerAction(
                label = "Erase everything",
                detail = "Collections, history and settings",
                onClick = { confirmErase = true }
            )
        }
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase everything?") },
            text = {
                Text(
                    "Every collection, your swipe history and all settings go away. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmErase = false
                    viewModel.eraseEverything()
                }) { Text("Erase") }
            },
            dismissButton = {
                TextButton(onClick = { confirmErase = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helper: String
) {
    // Local state to ensure smooth typing. We only pull from the external 'value' 
    // when it actually changes to a new value from the data store, rather than 
    // re-initializing on every recomposition which causes character deletion lag.
    var lastExternalValue by remember { mutableStateOf(value) }
    var text by remember { mutableStateOf(value) }

    if (value != lastExternalValue) {
        lastExternalValue = value
        text = value
    }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        TextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Text(
            text = helper,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun DangerAction(label: String, detail: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
