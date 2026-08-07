package com.example.chess.network.wifi

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.example.chess.network.GameEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

sealed class WifiState {
    object Disconnected : WifiState()
    object Discovering : WifiState()
    data class Challenging(val opponentName: String) : WifiState()
    data class ChallengePending(val request: GameEvent.MatchRequest, val fromOpponent: String) : WifiState()
    object ChallengeRejected : WifiState()
    data class Connected(val deviceName: String, val request: com.example.chess.network.GameEvent.MatchRequest? = null) : WifiState()
    data class Error(val message: String) : WifiState()
}

class WifiController(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPE = "_chess._tcp."
    private var activeServiceName = "ChessMultiplayer"

    private val _connectionState = MutableStateFlow<WifiState>(WifiState.Disconnected)
    val connectionState: StateFlow<WifiState> = _connectionState.asStateFlow()

    var isHost: Boolean = false
        private set

    private val _incomingEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 10)
    val incomingEvents: SharedFlow<GameEvent> = _incomingEvents.asSharedFlow()

    private val _discoveredServices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredServices: StateFlow<List<NsdServiceInfo>> = _discoveredServices.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var printWriter: PrintWriter? = null
    private var bufferedReader: BufferedReader? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val controllerScope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var lastHeartbeatTime = 0L

    fun startLobby(username: String, wins: Int, losses: Int, avatarUri: String) {
        disconnect()
        _discoveredServices.value = emptyList()
        _connectionState.value = WifiState.Discovering
        
        startHostingInternal(username, wins, losses, avatarUri)
        startDiscoveryInternal()
    }

    private fun startHostingInternal(username: String, wins: Int, losses: Int, avatarUri: String) {
        isHost = true
        activeServiceName = "ChessPeer-$username"
        controllerScope.launch {
            try {
                serverSocket = ServerSocket(0)
                val port = serverSocket?.localPort ?: throw IOException("Could not get port")
                registerService(port, wins, losses, avatarUri)
                
                var shouldLoop = true
                while (shouldLoop) {
                    val socket = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        shouldLoop = false
                        null
                    }
                    socket?.let {
                        // Keep server socket open so others can still discover us if they want, 
                        // or we can close it if we only accept one connection at a time.
                        // For a lobby, we should close it once a match is accepted.
                        handleIncomingConnection(it)
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = WifiState.Error(e.message ?: "Failed to host")
                disconnect()
            }
        }
    }

    private fun registerService(port: Int, wins: Int, losses: Int, avatarUri: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = activeServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("wins", wins.toString())
            setAttribute("losses", losses.toString())
            setAttribute("avatar", avatarUri)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _connectionState.value = WifiState.Error("NSD Registration Failed: $errorCode")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {}
        }
        registrationListener = null
    }

    private fun startDiscoveryInternal() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == SERVICE_TYPE && service.serviceName.startsWith("ChessPeer-") && service.serviceName != activeServiceName) {
                    val currentList = _discoveredServices.value.toMutableList()
                    // Avoid duplicates
                    if (currentList.none { it.serviceName == service.serviceName }) {
                        currentList.add(service)
                        _discoveredServices.value = currentList
                    }
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                val currentList = _discoveredServices.value.toMutableList()
                currentList.removeAll { it.serviceName == service.serviceName }
                _discoveredServices.value = currentList
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {}
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {}
        }
        discoveryListener = null
    }

    private suspend fun handleIncomingConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            val message = reader.readLine()
            if (message != null) {
                val event = Json.decodeFromString<GameEvent>(message)
                if (event is GameEvent.MatchRequest) {
                    clientSocket = socket
                    bufferedReader = reader
                    printWriter = writer
                    _connectionState.value = WifiState.ChallengePending(event, event.challengerName)
                } else {
                    socket.close()
                }
            } else {
                socket.close()
            }
        } catch (e: Exception) {
            socket.close()
        }
    }

    fun sendChallenge(serviceInfo: NsdServiceInfo, request: GameEvent.MatchRequest) {
        val targetName = serviceInfo.serviceName.removePrefix("ChessPeer-")
        controllerScope.launch {
            _connectionState.value = WifiState.Challenging(targetName)
            isHost = false // Challenger is the client
            try {
                @Suppress("DEPRECATION")
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        _connectionState.value = WifiState.Error("Failed to resolve peer")
                    }
                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        controllerScope.launch {
                            connectAndSendChallenge(resolvedInfo, targetName, request)
                        }
                    }
                })
            } catch (e: Exception) {
                _connectionState.value = WifiState.Error("Failed to resolve")
            }
        }
    }

    private suspend fun connectAndSendChallenge(serviceInfo: NsdServiceInfo, opponentName: String, request: GameEvent.MatchRequest) {
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val host = serviceInfo.host
                val socket = Socket(host, serviceInfo.port)
                
                clientSocket = socket
                bufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
                printWriter = PrintWriter(socket.getOutputStream(), true)
                
                // Send request
                val jsonString = Json.encodeToString<GameEvent>(request)
                printWriter?.println(jsonString)
                
                // Wait for response
                val responseMsg = bufferedReader?.readLine()
                if (responseMsg != null) {
                    val event = Json.decodeFromString<GameEvent>(responseMsg)
                    if (event is GameEvent.MatchResponse && event.accepted) {
                        stopDiscovery()
                        unregisterService()
                        serverSocket?.close() // Stop hosting
                        manageConnectedSocket(socket, opponentName, request)
                    } else {
                        _connectionState.value = WifiState.ChallengeRejected
                        socket.close()
                    }
                } else {
                    _connectionState.value = WifiState.Error("No response from opponent")
                    socket.close()
                }
            } catch (e: Exception) {
                _connectionState.value = WifiState.Error("Failed to connect")
                disconnect()
            }
        }
    }

    fun acceptChallenge(opponentName: String) {
        controllerScope.launch(Dispatchers.IO) {
            try {
                val response = GameEvent.MatchResponse(true)
                printWriter?.println(Json.encodeToString<GameEvent>(response))
                
                stopDiscovery()
                unregisterService()
                serverSocket?.close() // Stop hosting
                
                clientSocket?.let {
                    val req = (connectionState.value as? WifiState.ChallengePending)?.request
                    manageConnectedSocket(it, opponentName, req)
                }
            } catch (e: Exception) {
                _connectionState.value = WifiState.Error("Failed to accept")
            }
        }
    }

    fun rejectChallenge() {
        controllerScope.launch(Dispatchers.IO) {
            try {
                val response = GameEvent.MatchResponse(false)
                printWriter?.println(Json.encodeToString<GameEvent>(response))
                clientSocket?.close()
                clientSocket = null
                bufferedReader = null
                printWriter = null
                _connectionState.value = WifiState.Discovering
            } catch (e: Exception) {
                _connectionState.value = WifiState.Error("Failed to reject")
            }
        }
    }

    private suspend fun manageConnectedSocket(socket: Socket, deviceName: String, request: com.example.chess.network.GameEvent.MatchRequest? = null) {
        clientSocket = socket
        bufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
        printWriter = PrintWriter(socket.getOutputStream(), true)

        _connectionState.value = WifiState.Connected(deviceName, request)
        lastHeartbeatTime = System.currentTimeMillis()
        
        startHeartbeat()

        withContext(Dispatchers.IO) {
            try {
                while (isActive) {
                    val message = bufferedReader?.readLine()
                    if (message == null) {
                        break // Connection closed
                    }
                    
                    try {
                        val event = Json.decodeFromString<GameEvent>(message)
                        lastHeartbeatTime = System.currentTimeMillis()
                        
                        when (event) {
                            is GameEvent.HeartbeatPing -> sendEvent(GameEvent.HeartbeatPong)
                            is GameEvent.HeartbeatPong -> {} // Handled by time update above
                            else -> _incomingEvents.tryEmit(event)
                        }
                    } catch (e: Exception) {
                        // Ignore parse errors
                    }
                }
            } catch (e: IOException) { }
            
            _connectionState.value = WifiState.Error("Connection lost")
            disconnect()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = controllerScope.launch {
            while (isActive && _connectionState.value is WifiState.Connected) {
                delay(3000)
                sendEvent(GameEvent.HeartbeatPing)
                
                if (System.currentTimeMillis() - lastHeartbeatTime > 10000) {
                    // Dead connection
                    _connectionState.value = WifiState.Error("Connection timed out")
                    disconnect()
                    break
                }
            }
        }
    }

    fun sendEvent(event: GameEvent) {
        controllerScope.launch {
            try {
                val jsonString = Json.encodeToString(event)
                printWriter?.println(jsonString)
            } catch (e: Exception) {
                _connectionState.value = WifiState.Error("Failed to send data")
            }
        }
    }

    fun disconnect() {
        unregisterService()
        stopDiscovery()
        heartbeatJob?.cancel()
        
        try {
            serverSocket?.close()
            clientSocket?.close()
            bufferedReader?.close()
            printWriter?.close()
        } catch (e: Exception) { }
        finally {
            serverSocket = null
            clientSocket = null
            bufferedReader = null
            printWriter = null
            if (_connectionState.value !is WifiState.Error) {
                _connectionState.value = WifiState.Disconnected
            }
        }
    }
}
