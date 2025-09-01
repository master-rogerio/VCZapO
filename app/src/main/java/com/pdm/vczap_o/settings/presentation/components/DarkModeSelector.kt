package com.pdm.vczap_o.settings.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdm.vczap_o.core.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(currentMode) }
    val options = listOf("Padrão do sistema", "Claro", "Escuro")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.LightMode, contentDescription = "")
            Column {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    options[currentMode.ordinal],
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = !expanded },
                title = { Text("Escolha o tema") },
                text = {
                    Column {
                        options.forEachIndexed { index, selectionOption ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        selectedMode = ThemeMode.entries[index]
                                    }
                            ) {
                                RadioButton(
                                    selected = index == ThemeMode.entries[selectedMode.ordinal].ordinal,
                                    onClick = { selectedMode = ThemeMode.entries[index] })
                                Text(
                                    selectionOption, modifier = Modifier
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onModeSelected(selectedMode)
                        expanded = !expanded
                    }) {
                        Text("Ok")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
