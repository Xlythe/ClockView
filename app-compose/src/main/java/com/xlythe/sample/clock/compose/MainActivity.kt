package com.xlythe.sample.clock.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xlythe.compose.clock.Clock
import com.xlythe.compose.clock.ClockController
import com.xlythe.compose.clock.ClockStyle

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    primary = Color(0xFFBB86FC),
                    secondary = Color(0xFF03DAC6),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var clockStyle by remember { mutableStateOf(ClockStyle.ANALOG) }
                    var showSeconds by remember { mutableStateOf(true) }
                    var showMilliseconds by remember { mutableStateOf(false) }
                    var partialRotation by remember { mutableStateOf(false) }
                    var ambientMode by remember { mutableStateOf(false) }
                    var lowBitAmbient by remember { mutableStateOf(false) }
                    var burnInProtection by remember { mutableStateOf(false) }

                    val clockController = remember { mutableStateOf<ClockController?>(null) }

                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        "Clock Compose",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color(0xFF1E1E1E)
                                )
                            )
                        },
                        containerColor = Color(0xFF121212)
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Clock Display Card
                            Card(
                                modifier = Modifier
                                    .size(320.dp)
                                    .clip(RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E1E1E)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Clock(
                                        modifier = Modifier.fillMaxSize(),
                                        clockStyle = clockStyle,
                                        clockFaceRes = R.drawable.tick_roman,
                                        hourHandRes = R.drawable.hour_hand,
                                        minuteHandRes = R.drawable.minute_hand,
                                        secondHandRes = R.drawable.second_hand,
                                        showSeconds = showSeconds,
                                        showMilliseconds = showMilliseconds,
                                        partialRotation = partialRotation,
                                        lowBitAmbient = lowBitAmbient,
                                        hasBurnInProtection = burnInProtection,
                                        ambientModeEnabled = ambientMode,
                                        controller = clockController
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Controller Imperative Actions
                            Text(
                                text = "Controller Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { clockController.value?.start() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start")
                                }
                                Button(
                                    onClick = { clockController.value?.stop() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { clockController.value?.resetTime() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset")
                                }
                                Button(
                                    onClick = { clockController.value?.setTime(12, 30, 0) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = "12:30", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("12:30")
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Parameters / Configuration
                            Text(
                                text = "Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    ToggleOption(
                                        label = "Digital Clock Style",
                                        checked = clockStyle == ClockStyle.DIGITAL,
                                        onCheckedChange = { isDigital ->
                                            clockStyle = if (isDigital) ClockStyle.DIGITAL else ClockStyle.ANALOG
                                        }
                                    )
                                    ToggleOption(
                                        label = "Show Seconds",
                                        checked = showSeconds,
                                        onCheckedChange = { showSeconds = it }
                                    )
                                    ToggleOption(
                                        label = "Show Milliseconds",
                                        checked = showMilliseconds,
                                        onCheckedChange = { showMilliseconds = it }
                                    )
                                    ToggleOption(
                                        label = "Partial Rotation",
                                        checked = partialRotation,
                                        onCheckedChange = { partialRotation = it }
                                    )
                                    ToggleOption(
                                        label = "Ambient Mode",
                                        checked = ambientMode,
                                        onCheckedChange = { ambientMode = it }
                                    )
                                    ToggleOption(
                                        label = "Low Bit Ambient",
                                        checked = lowBitAmbient,
                                        onCheckedChange = { lowBitAmbient = it }
                                    )
                                    ToggleOption(
                                        label = "Burn-in Protection",
                                        checked = burnInProtection,
                                        onCheckedChange = { burnInProtection = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF121212)
            )
        )
    }
}
