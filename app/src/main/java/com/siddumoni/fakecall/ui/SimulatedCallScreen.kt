package com.siddumoni.fakecall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.callers.CallerPreset

@Composable
fun SimulatedCallScreen(
    caller: CallerPreset,
    answered: Boolean,
    onAnswer: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.simulated_call_banner),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(caller.displayName, style = MaterialTheme.typography.headlineLarge)
                if (caller.numberLabel.isNotBlank()) {
                    Text(caller.numberLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = stringResource(if (answered) R.string.simulated_call_connected else R.string.simulated_call_incoming),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (answered) {
                Button(onClick = onEnd, modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp)) {
                    Text(stringResource(R.string.simulated_call_end))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(onClick = onEnd, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.simulated_call_decline))
                    }
                    Button(onClick = onAnswer, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.simulated_call_answer))
                    }
                }
            }
            Text(
                text = stringResource(R.string.simulated_call_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
