package io.aira.aira_call


import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.aira.aira_call.ui.theme.AIDLClientTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val serviceName = "io.aira.aira_call.AiraAidlInterface"
    private var targetPackage by mutableStateOf("io.aira.aira_call_example")
    private val availablePackages = listOf(
        "io.aira.explorer",
        "io.aira.explorer.dev",
        "io.aira.explorer.staging",
        "io.aira.asl",
        "io.aira.asl.dev",
        "io.aira.asl.staging",
        "io.aira.aira_call_example",
    )

    private val mAiraService = mutableStateOf<AiraAidlInterface?>(null)
    private val mErrorMessage = mutableStateOf<String?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            mAiraService.value = AiraAidlInterface.Stub.asInterface(service)
            mErrorMessage.value = null // Clear any previous errors
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            mAiraService.value = null
        }
    }

    // State variables
    private val mMicrophoneEnabled = mutableStateOf(false)
    private val mCameraEnabled = mutableStateOf(false)
    private val mIsInCall = mutableStateOf(false)
    private val mLoggedInUser = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIDLClientTheme {
                val airaService by remember { mAiraService }
                val microphoneEnabled by remember { mMicrophoneEnabled }
                val cameraEnabled by remember { mCameraEnabled }
                val inCall by remember { mIsInCall }
                val loggedInUser by remember { mLoggedInUser }
                val errorMessage by remember { mErrorMessage }

                // Initial sync
                LaunchedEffect(airaService) {
                    syncStates()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        PackageInput(
                            packageName = targetPackage,
                            availablePackages = availablePackages,
                            onPackageChange = {
                                targetPackage = it
                                unbindService()
                                bindService()
                            },
                        )
                        ServiceControlScreen(
                            connected = airaService != null,
                            microphoneEnabled = microphoneEnabled,
                            cameraEnabled = cameraEnabled,
                            isInCall = inCall,
                            loggedInUser = loggedInUser,
                            errorMessage = errorMessage,
                            onToggleMicrophone = { enabled ->
                                lifecycleScope.launch {
                                    airaService?.setMicrophoneEnabled(enabled)
                                    // Give some time for the request to take effect before syncing
                                    delay(1000)
                                    syncStates()
                                }
                            },
                            onToggleCamera = { enabled ->
                                lifecycleScope.launch {
                                    airaService?.setCameraEnabled(enabled)
                                    delay(1000)
                                    syncStates()
                                }
                            },
                            onEndCall = {
                                lifecycleScope.launch {
                                    airaService?.endCall()
                                    delay(1000)
                                    syncStates()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    private fun syncStates() {
        mMicrophoneEnabled.value = mAiraService.value?.isMicrophoneEnabled() ?: false
        mCameraEnabled.value = mAiraService.value?.isCameraEnabled() ?: false
        mIsInCall.value = mAiraService.value?.isInCall() ?: false
        mLoggedInUser.value = mAiraService.value?.loggedInUser
    }

    private fun bindService() {
        val intent = Intent(serviceName)
        intent.setPackage(targetPackage)
        try {
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            mErrorMessage.value = "Failed to bind service: ${e.message}"
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            mErrorMessage.value = "An error occurred: ${e.message}"
        }
    }


    private fun unbindService() {
        if (mAiraService.value != null) {
            unbindService(serviceConnection)
            mAiraService.value = null
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService()
    }
}

@Composable
fun ServiceControlScreen(
    modifier: Modifier = Modifier,
    connected: Boolean,
    microphoneEnabled: Boolean,
    cameraEnabled: Boolean,
    isInCall: Boolean,
    loggedInUser: String?,
    errorMessage: String?,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onEndCall: () -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (errorMessage != null) {
            ErrorMessage(errorMessage)
            return
        }

        ServiceStatusCard(connected, isInCall, loggedInUser)

        if (!connected) {
            LoadingIndicator()
            return
        }

        if (!isInCall) {
            return
        }

        DeviceControls(
            micEnabled = microphoneEnabled,
            cameraEnabled = cameraEnabled,
            onMicToggle = onToggleMicrophone,
            onCameraToggle = onToggleCamera,
        )
        EndCallButton(onEndCall)
    }
}

@Composable
private fun ServiceStatusCard(isBound: Boolean, isInCall: Boolean, loggedInUser: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isBound -> MaterialTheme.colorScheme.errorContainer
                loggedInUser == null -> MaterialTheme.colorScheme.tertiaryContainer
                isInCall -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when {
                    !isBound -> "Disconnected"
                    loggedInUser == null -> "Not Logged In"
                    isInCall -> "In Call"
                    else -> "Connected"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = when {
                    !isBound -> "Waiting for service connection..."
                    loggedInUser == null -> "The target app doesn't have logged in user"
                    isInCall -> "Call in progress (User: ${loggedInUser})"
                    else -> "Ready (User: ${loggedInUser})"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DeviceControls(
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    onMicToggle: (Boolean) -> Unit,
    onCameraToggle: (Boolean) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            DeviceToggle(
                title = "Microphone",
                enabled = micEnabled,
                onToggle = onMicToggle,
                icon = if (micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff
            )
            Spacer(modifier = Modifier.height(16.dp))
            DeviceToggle(
                title = "Camera",
                enabled = cameraEnabled,
                onToggle = onCameraToggle,
                icon = if (cameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff
            )
        }
    }
}

@Composable
private fun DeviceToggle(
    title: String, enabled: Boolean, onToggle: (Boolean) -> Unit, icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(title)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = enabled, onCheckedChange = onToggle
        )
    }
}

@Composable
private fun EndCallButton(onEndCall: () -> Unit) {
    Button(
        onClick = onEndCall,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CallEnd, contentDescription = null)
            Text("End Call")
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageInput(
    packageName: String,
    availablePackages: List<String>,
    onPackageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = packageName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Target Package") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availablePackages.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p) },
                    trailingIcon = {
                        if (p == packageName) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        onPackageChange(p)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String?) {
    if (message != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
