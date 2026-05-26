package com.example.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.ui.UnityAdsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class StreamRewardsViewModel : ViewModel() {

    // --- FIREBASE AUTHENTICATION CONFIGURATION ---
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://jose-83986-default-rtdb.firebaseio.com")
    }

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun setAuthError(msg: String?) {
        _authError.value = msg
    }

    private val _authSuccessLog = MutableStateFlow<String?>("Conexión Firebase Inicializada")
    val authSuccessLog: StateFlow<String?> = _authSuccessLog.asStateFlow()

    // --- STATE FLOWS ---
    private val _currentUser = MutableStateFlow(User(id = "visitante", name = "Usuario Invitado", email = "visitante@streamrewards.pro", pointsCurrent = 0, pointsHistoric = 0))
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _accountsPool = MutableStateFlow<List<StreamingAccount>>(emptyList())
    val accountsPool: StateFlow<List<StreamingAccount>> = _accountsPool.asStateFlow()

    private val _activeRedemptions = MutableStateFlow<List<ActiveRedemption>>(emptyList())
    val activeRedemptions: StateFlow<List<ActiveRedemption>> = _activeRedemptions.asStateFlow()

    private val _pastRedemptions = MutableStateFlow<List<ActiveRedemption>>(emptyList())
    val pastRedemptions: StateFlow<List<ActiveRedemption>> = _pastRedemptions.asStateFlow()

    private val _allRedemptions = MutableStateFlow<List<ActiveRedemption>>(emptyList())

    private val _pointsLogs = MutableStateFlow<List<PointsLog>>(emptyList())
    val pointsLogs: StateFlow<List<PointsLog>> = _pointsLogs.asStateFlow()

    private val _dailyMissions = MutableStateFlow<List<DailyMission>>(emptyList())
    val dailyMissions: StateFlow<List<DailyMission>> = _dailyMissions.asStateFlow()

    private val _misionesProgress = MutableStateFlow<Map<String, MissionProgress>>(emptyMap())
    val misionesProgress: StateFlow<Map<String, MissionProgress>> = _misionesProgress.asStateFlow()

    private val _tournamentsLeaderboard = MutableStateFlow<List<Contestant>>(emptyList())
    val tournamentsLeaderboard: StateFlow<List<Contestant>> = _tournamentsLeaderboard.asStateFlow()

    private val _raffleParticipants = MutableStateFlow<List<RaffleTicketHolder>>(emptyList())
    val raffleParticipants: StateFlow<List<RaffleTicketHolder>> = _raffleParticipants.asStateFlow()

    private val _adminLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val adminLogs: StateFlow<List<ActivityLog>> = _adminLogs.asStateFlow()

    // --- NOTIFICATION STREAMS ---
    private val _notifications = MutableStateFlow<List<StreamNotification>>(emptyList())
    val notifications: StateFlow<List<StreamNotification>> = _notifications.asStateFlow()

    private val _notificationEvents = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    val notificationEvents: SharedFlow<Pair<String, String>> = _notificationEvents.asSharedFlow()

    private val _notifiedIds = mutableSetOf<String>()
    private val appLaunchTime = System.currentTimeMillis()

    // --- REALTIME DYNAMIC ADMIN & PRICING CONFIGS ---
    private val _adminEmails = MutableStateFlow<List<String>>(emptyList())
    val adminEmails: StateFlow<List<String>> = _adminEmails.asStateFlow()

    private val _servicePrices = MutableStateFlow<Map<ServiceType, Int>>(mapOf(
        ServiceType.NETFLIX to 100,
        ServiceType.PRIME_VIDEO to 250,
        ServiceType.DISNEY_PLUS to 500,
        ServiceType.HBO_MAX to 500
    ))
    val servicePrices: StateFlow<Map<ServiceType, Int>> = _servicePrices.asStateFlow()

    private val _rafflesList = MutableStateFlow<List<Raffle>>(emptyList())
    val rafflesList: StateFlow<List<Raffle>> = _rafflesList.asStateFlow()

    // --- OTHER UI STATES ---
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _simulatedAdLimit = MutableStateFlow(0) // max 100 points via ads daily, e.g. 20 ads
    val simulatedAdLimit: StateFlow<Int> = _simulatedAdLimit.asStateFlow()

    private val _unityGameId = MutableStateFlow("5712345")
    val unityGameId: StateFlow<String> = _unityGameId.asStateFlow()

    private val _unityPlacementId = MutableStateFlow("Rewarded_Android")
    val unityPlacementId: StateFlow<String> = _unityPlacementId.asStateFlow()

    private val _lastRedemptionTime = MutableStateFlow<Long>(0)

    init {
        // Automatically check for saved sessions to keep user logged in across restarts!
        checkAutoLogin()

        // Configure default structures
        resetDailyMissions()
        resetTournament()
        resetRaffle()

        // Sync visual loops
        viewModelScope.launch {
            while (true) {
                delay(1500)
                countdownActiveRedemptions()
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun postToast(msg: String) {
        _toastMessage.value = msg
    }

    private fun logAdminActivity(action: String) {
        try {
            val logId = UUID.randomUUID().toString()
            val newLog = ActivityLog(id = logId, action = action)
            database.getReference("admin_logs").child(logId).setValue(newLog)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    // --- CONFIG & RESET fallback ---

    private fun resetDailyMissions() {
        val list = listOf(
            DailyMission("m1", "Ver 3 anuncios", "Completa la reproducción de videos publicitarios cortos.", 3, 15),
            DailyMission("m2", "Invitar 1 amigo", "Comparte tu código con un amigo y haz que se registre.", 1, 50),
            DailyMission("m3", "Canjear 1 cuenta", "Realiza el canje de cualquier servicio de streaming.", 1, 30),
            DailyMission("m4", "Reportar 1 cuenta caída", "Identifica y reporta un acceso inactivo.", 1, 10)
        )
        _dailyMissions.value = list
        _misionesProgress.value = list.associate { it.id to MissionProgress(missionId = it.id) }
    }

    private fun resetTournament() {
        _tournamentsLeaderboard.value = listOf(
            Contestant("Mateo R.", 450),
            Contestant("Sofía G.", 390),
            Contestant("Carlos D.", 320),
            Contestant("Guillermo Pérez", 0, isMe = true),
            Contestant("Valentina K.", 180),
            Contestant("Diego P.", 140)
        ).sortedByDescending { it.pointsInTournament }
    }

    private fun resetRaffle() {
        _raffleParticipants.value = listOf(
            RaffleTicketHolder("Sonia L.", 4),
            RaffleTicketHolder("Felipe J.", 2),
            RaffleTicketHolder("Camila M.", 5),
            RaffleTicketHolder("Alejandro B.", 3)
        )
    }

    // --- RECOLLECT AUTO-LOGIN & FIREBASE REALTIME ---

    fun checkAutoLogin() {
        val auth = firebaseAuth ?: return
        val fbUser = auth.currentUser
        if (fbUser != null) {
            val uid = fbUser.uid
            val email = fbUser.email ?: ""
            Log.d("StreamRewards", "Autologin detectado para UID: $uid ($email)")
            _isLoggedIn.value = true
            _authSuccessLog.value = "Sesión persistente"
            listenToUserProfile(uid, email)
            startGlobalListeners()
        }
    }

    private var userListener: ValueEventListener? = null

    fun listenToUserProfile(uid: String, email: String) {
        try {
            val userRef = database.getReference("users").child(uid)
            userListener?.let { userRef.removeEventListener(it) }

            userListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            _currentUser.value = user
                        }
                    } else {
                        // Create default user entry
                        val isPrincipal = email.equals("blandonjose6788@gmail.com", ignoreCase = true)
                        val startPoints = if (isPrincipal) 99999 else 250
                        val defaultUser = User(
                            id = uid,
                            name = email.substringBefore("@"),
                            email = email,
                            pointsCurrent = startPoints,
                            pointsHistoric = startPoints,
                            level = if (isPrincipal) 4 else 1,
                            rachaCheckIn = 1
                        )
                        userRef.setValue(defaultUser)
                        _currentUser.value = defaultUser
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("StreamRewards", "Error loading user profile: ${error.message}")
                }
            }
            userRef.addValueEventListener(userListener!!)
        } catch (e: Exception) {
            Log.e("StreamRewards", "Error initializing listener profile: ${e.message}")
        }
    }

    fun startGlobalListeners() {
        try {
            val db = database

            // 1. Whitelisted admins list
            db.getReference("admins").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<String>()
                    for (child in snapshot.children) {
                        val email = child.child("email").getValue(String::class.java) ?: child.getValue(String::class.java)
                        if (email != null) list.add(email)
                    }
                    _adminEmails.value = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 2. Custom Point prices
            db.getReference("prices").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val prices = mutableMapOf<ServiceType, Int>()
                    for (type in ServiceType.values()) {
                        val price = snapshot.child(type.name).getValue(Int::class.java)
                        prices[type] = price ?: when (type) {
                            ServiceType.NETFLIX -> 100
                            ServiceType.PRIME_VIDEO -> 250
                            ServiceType.DISNEY_PLUS -> 500
                            ServiceType.HBO_MAX -> 500
                        }
                    }
                    _servicePrices.value = prices
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 3. Accounts pool
            db.getReference("accounts").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<StreamingAccount>()
                    for (child in snapshot.children) {
                        try {
                            val acc = child.getValue(StreamingAccount::class.java)
                            if (acc != null) list.add(acc)
                        } catch (e: Exception) {
                            Log.e("StreamRewards", "Error reading account item: ${e.message}")
                        }
                    }
                    if (list.isEmpty()) {
                        seedAccountsIntoDatabase()
                    } else {
                        _accountsPool.value = list
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 4. Redemptions
            db.getReference("redemptions").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ActiveRedemption>()
                    for (child in snapshot.children) {
                        try {
                            val red = child.getValue(ActiveRedemption::class.java)
                            if (red != null) list.add(red)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    _allRedemptions.value = list

                    // Filter only my redemptions for visual use
                    val myUid = _currentUser.value.id
                    _activeRedemptions.value = list.filter { it.userId == myUid }
                    _pastRedemptions.value = list.filter { it.userId == myUid && it.reported }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 5. Global announcements (triggers push notifications natively!)
            db.getReference("notifications").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<StreamNotification>()
                    for (child in snapshot.children) {
                        try {
                            val notif = child.getValue(StreamNotification::class.java)
                            if (notif != null) list.add(notif)
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val sorted = list.sortedBy { it.timestamp }
                    _notifications.value = sorted.reversed()

                    val latest = sorted.lastOrNull()
                    if (latest != null && latest.timestamp > appLaunchTime) {
                        if (!_notifiedIds.contains(latest.id)) {
                            _notifiedIds.add(latest.id)
                            _notificationEvents.tryEmit(Pair(latest.title, latest.text))
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 6. Sorteos list
            db.getReference("raffles").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Raffle>()
                    for (child in snapshot.children) {
                        try {
                            val rf = child.getValue(Raffle::class.java)
                            if (rf != null) list.add(rf)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    _rafflesList.value = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 7. Users (Admin view)
            db.getReference("users").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<User>()
                    for (child in snapshot.children) {
                        try {
                            val userObj = child.getValue(User::class.java)
                            if (userObj != null) list.add(userObj)
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    _allUsers.value = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 8. Admin Logs
            db.getReference("admin_logs").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ActivityLog>()
                    for (child in snapshot.children) {
                        try {
                            val log = child.getValue(ActivityLog::class.java)
                            if (log != null) list.add(log)
                        } catch(e: Exception) {
                            Log.e("StreamRewards", "Error reading log: ${e.message}")
                        }
                    }
                    _adminLogs.value = list.sortedByDescending { it.timestamp }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 9. Unity Ads Config
            db.getReference("unity_ads_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val dbGameId = snapshot.child("gameId").getValue(String::class.java) ?: "5712345"
                        val dbPlacementId = snapshot.child("placementId").getValue(String::class.java) ?: "Rewarded_Android"
                        _unityGameId.value = dbGameId
                        _unityPlacementId.value = dbPlacementId
                        UnityAdsHelper.gameId = dbGameId
                        UnityAdsHelper.placementId = dbPlacementId
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        } catch (e: Exception) {
            Log.e("StreamRewards", "Error placing Firebase active listeners: ${e.message}")
        }
    }

    private fun seedAccountsIntoDatabase() {
        val list = listOf(
            StreamingAccount(id = UUID.randomUUID().toString(), serviceType = ServiceType.NETFLIX, email = "netflix.premium1@streamrepo.com", passwordDecoded = "NfxPass_773"),
            StreamingAccount(id = UUID.randomUUID().toString(), serviceType = ServiceType.NETFLIX, email = "netflix.premium2@streamrepo.com", passwordDecoded = "NfxPass_992"),
            StreamingAccount(id = UUID.randomUUID().toString(), serviceType = ServiceType.PRIME_VIDEO, email = "prime.rewards1@streamrepo.com", passwordDecoded = "PrimeX_112"),
            StreamingAccount(id = UUID.randomUUID().toString(), serviceType = ServiceType.PRIME_VIDEO, email = "prime.rewards2@streamrepo.com", passwordDecoded = "PrimeCode_Alpha"),
            StreamingAccount(id = UUID.randomUUID().toString(), serviceType = ServiceType.DISNEY_PLUS, email = "disney.family@streamrepo.com", passwordDecoded = "DnyMagic_101"),
            StreamingAccount(id = UUID.randomUUID().toString(), serviceType = ServiceType.HBO_MAX, email = "hbo.premium@streamrepo.com", passwordDecoded = "HboHouseDragon_24")
        )
        try {
            val ref = database.getReference("accounts")
            for (acc in list) {
                ref.child(acc.id).setValue(acc)
            }
            logAdminActivity("Administración cargó base de cuentas inicial en Firebase RTDB")
        } catch (e: Exception) {
            Log.e("StreamRewards", "Error seeding accounts: ${e.message}")
        }
    }

    // --- MAIN FUNCTIONAL LOGIC ---

    fun claimDailyReward() {
        val user = _currentUser.value
        val now = System.currentTimeMillis()

        // 24 hours cooldown
        val hoursInMs = 24 * 60 * 60 * 1000L
        if (user.lastCheckInClaimed != null && (now - user.lastCheckInClaimed) < hoursInMs) {
            val remainMs = hoursInMs - (now - user.lastCheckInClaimed)
            val hoursLeft = remainMs / (1000 * 60 * 60)
            val minsLeft = (remainMs / (1000 * 60)) % 60
            postToast("Ya reclamaste hoy. Espera $hoursLeft horas y $minsLeft minutos.")
            return
        }

        var newStreak = user.rachaCheckIn + 1
        if (user.lastCheckInClaimed != null && (now - user.lastCheckInClaimed) > (48 * 60 * 60 * 1000L)) {
            newStreak = 1
            postToast("¡Racha perdida por inactividad! Iniciando en Día 1.")
        }

        if (newStreak > 7) {
            newStreak = 1 // reset
        }

        val rewardAmount = when (newStreak) {
            1 -> 5
            2 -> 10
            3 -> 15
            4 -> 20
            5 -> 25
            6 -> 30
            7 -> 50
            else -> 5
        }

        addPointsToUser(rewardAmount, AwardSource.DAILY_CHECKIN, "Check-In diario - Día $newStreak") {
            val updateRef = database.getReference("users").child(user.id)
            updateRef.child("rachaCheckIn").setValue(newStreak)
            updateRef.child("lastCheckInClaimed").setValue(now)

            postToast("¡Excelente! Reclamaste la recompensa del Día $newStreak: +$rewardAmount puntos.")
            logAdminActivity("Usuario '${user.name}' reclamó check-in del Día $newStreak (+${rewardAmount} pts).")
        }
    }

    fun watchRewardedAd(activity: Activity? = null) {
        if (_simulatedAdLimit.value >= 100) {
            postToast("Límite diario de anuncios alcanzado (100 pts máximo). Vuelve mañana.")
            return
        }

        if (activity != null) {
            UnityAdsHelper.showAd(activity) {
                val pointsPerAd = 5
                _simulatedAdLimit.value += pointsPerAd

                addPointsToUser(pointsPerAd, AwardSource.AD_REWARD, "Anuncio bonificado Unity Ads") {
                    incrementMissionProgress("m1", 1)
                    postToast("¡Video completado! Has ganado +$pointsPerAd puntos.")
                    logAdminActivity("Usuario '${_currentUser.value.name}' vio un anuncio de Unity Ads (+5 pts).")
                    incrementTournamentScore(pointsPerAd)
                }
            }
        } else {
            val pointsPerAd = 5
            _simulatedAdLimit.value += pointsPerAd

            addPointsToUser(pointsPerAd, AwardSource.AD_REWARD, "Anuncio bonificado (Simulado Unity Ads)") {
                incrementMissionProgress("m1", 1)
                postToast("¡Video completado! Has ganado +$pointsPerAd puntos.")
                logAdminActivity("Usuario '${_currentUser.value.name}' vio un anuncio fallback (+5 pts).")
                incrementTournamentScore(pointsPerAd)
            }
        }
    }

    fun recalculateRaffleTickets() {
        // Automatically computed on Firebase sync. Provided to keep UI bindings clean.
    }

    fun reportFallenAccount(redemptionId: String) {
        reportAccount(redemptionId)
    }

    fun claimMissionReward(missionId: String) {
        val mission = _dailyMissions.value.find { it.id == missionId } ?: return
        val progress = _misionesProgress.value[missionId] ?: return
        if (progress.currentCount >= mission.targetCount && !progress.isClaimed) {
            _misionesProgress.value = _misionesProgress.value + (missionId to progress.copy(isClaimed = true))
            addPointsToUser(mission.rewardPoints, AwardSource.MISSION, "Misión completada: ${mission.title}") {
                postToast("¡Excelente! Reclamaste la recompensa de la misión: +${mission.rewardPoints} puntos.")
                logAdminActivity("Usuario '${_currentUser.value.name}' cobró misión '${mission.title}' (+${mission.rewardPoints} pts).")
            }
        }
    }

    fun redeemStreamingService(serviceType: ServiceType, days: Int = 7) {
        redeemAccount(serviceType)
    }

    fun simulateFriendReferral() {
        val user = _currentUser.value
        val code = "REF-${user.name.take(3).uppercase()}-${(100..999).random()}"

        addPointsToUser(50, AwardSource.REFERRAL, "Amigo registrado con código $code") {
            database.getReference("users").child(user.id).child("friendsReferred")
                .setValue(user.friendsReferred + 1)
            incrementMissionProgress("m2", 1)
            postToast("¡Amigo registrado! Bono de referido recibido: +50 puntos.")
            logAdminActivity("Bono de referido de usuario '${user.name}' procesado.")
            incrementTournamentScore(50)
        }
    }

    private fun getDiscountForLevel(level: Int): Double {
        return when (level) {
            1 -> 0.0
            2 -> 0.10
            3 -> 0.20
            4 -> 0.30
            else -> 0.0
        }
    }

    fun calculateRedeemCost(serviceType: ServiceType, durationDays: Int, level: Int): Int {
        val baseCost = _servicePrices.value[serviceType] ?: when (serviceType) {
            ServiceType.NETFLIX -> 100
            ServiceType.PRIME_VIDEO -> 250
            ServiceType.DISNEY_PLUS -> 500
            ServiceType.HBO_MAX -> 500
        }
        val discount = getDiscountForLevel(level)
        return (baseCost * (1 - discount)).toInt()
    }

    fun redeemAccount(serviceType: ServiceType) {
        val user = _currentUser.value
        val cost = calculateRedeemCost(serviceType, 7, user.level) // Defaulting duration as indefinite as per user request

        if (user.pointsCurrent < cost) {
            postToast("Puntos insuficientes. Necesitas $cost puntos.")
            return
        }

        // Find available StreamingAccount of this service type in Firebase
        val availableAccount = _accountsPool.value.firstOrNull {
            it.serviceType == serviceType &&
            it.status == AccountStatus.ACTIVE &&
            !_allRedemptions.value.any { red -> red.accountId == it.id && (red.expiresAt > System.currentTimeMillis() || red.expiresAt == 0L) && !red.reported }
        }

        if (availableAccount == null) {
            postToast("No hay cuentas de ${serviceType.name} disponibles. Intenta más tarde.")
            return
        }

        // Deduct points as redeem price
        addPointsToUser(-cost, AwardSource.DAILY_CHECKIN, "Canje ${serviceType.name}") {
            val redemptionId = UUID.randomUUID().toString()
            val redemption = ActiveRedemption(
                id = redemptionId,
                accountId = availableAccount.id,
                userId = user.id,
                serviceType = serviceType,
                email = availableAccount.email,
                passwordDecoded = availableAccount.passwordDecoded,
                redeemCost = cost,
                expiresAt = 0L, // 0 For indefinite access! (no se compran por tiempo!)
                reported = false
            )

            // Save redemption to Firebase
            database.getReference("redemptions").child(redemptionId).setValue(redemption).addOnSuccessListener {
                postToast("¡Canje correcto! Credenciales disponibles.")
                logAdminActivity("Usuario '${user.name}' canjeó cuenta de ${serviceType.name} (-$cost pts)")
                incrementMissionProgress("m3", 1)
            }
        }
    }

    fun reportAccount(redemptionId: String) {
        val redemption = _activeRedemptions.value.firstOrNull { it.id == redemptionId } ?: return
        if (redemption.reported) {
            postToast("Ya reportaste esta cuenta.")
            return
        }

        // Record flag in Database
        database.getReference("redemptions").child(redemptionId).child("reported").setValue(true)

        val accountRef = database.getReference("accounts").child(redemption.accountId)
        accountRef.child("reports24h").get().addOnSuccessListener { snapshot ->
            val curReports = snapshot.getValue(Int::class.java) ?: 0
            val newReports = curReports + 1
            accountRef.child("reports24h").setValue(newReports)

            if (newReports >= 3) {
                // Set status DOWN
                accountRef.child("status").setValue(AccountStatus.DOWN)
                // Automatic refund
                addPointsToUser(redemption.redeemCost, AwardSource.REDEMPTION_REFUND, "Reembolso automático por cuenta caída") {
                    postToast("Reportado. Se confirmó inactividad. Reembolso: +${redemption.redeemCost} puntos.")
                    logAdminActivity("Cuenta '${redemption.email}' marcada DOWN. Reembolso emitido con éxito.")
                }
            } else {
                addPointsToUser(10, AwardSource.REPORT_COMMISSION, "Bono por reporte de acceso") {
                    postToast("Reporte registrado. Estamos reparándolo. Compensación: +10 puntos.")
                }
            }
            incrementMissionProgress("m4", 1)
        }
    }

    private fun countdownActiveRedemptions() {
        // Redemptions are no longer removed by time countdown as they are INDEFINITE (expiresAt = 0L).
        // This is safe to keep as no-op or just visually updating countdowns for offline mocks if any existed.
    }

    fun isUserAdmin(email: String): Boolean {
        if (email.trim().equals("blandonjose6788@gmail.com", ignoreCase = true)) return true
        return _adminEmails.value.any { it.trim().equals(email.trim(), ignoreCase = true) }
    }

    // --- COCKPIT & LOGS SIMULATION ADMINS ---

    fun runVirtualExpiryCron() {
        logAdminActivity("Simulación de Cron de Expiración ejecutada de forma manual.")
        postToast("Cron Expiración completado.")
    }

    fun runDailyResetCron() {
        _simulatedAdLimit.value = 0
        resetDailyMissions()
        logAdminActivity("Simulación de Cron diario de reinicio de misiones ejecutado.")
        postToast("Misiones diarias reiniciadas.")
    }

    fun runWeeklySundayCron() {
        logAdminActivity("Simulación de Cron de Sorteo Semanal ejecutada.")
        postToast("Sorteo de domingo finalizado.")
    }

    fun rotateAllPasswords() {
        val pool = _accountsPool.value
        try {
            val ref = database.getReference("accounts")
            for (acc in pool) {
                val newPass = "RotatedPass_" + (100..999).random()
                ref.child(acc.id).child("passwordDecoded").setValue(newPass)
            }
            logAdminActivity("Administración rotó las contraseñas de todas las cuentas registradas.")
            postToast("Rotación masiva completada exitosamente.")
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    // --- ADMIN CONTROLS CALLED BY ADMIN TAB ---

    fun addStreamingAccount(serviceType: ServiceType, email: String, passDecoded: String) {
        val id = UUID.randomUUID().toString()
        val acc = StreamingAccount(
            id = id,
            serviceType = serviceType,
            email = email.trim(),
            passwordDecoded = passDecoded,
            status = AccountStatus.ACTIVE
        )
        database.getReference("accounts").child(id).setValue(acc).addOnSuccessListener {
            postToast("Cuenta añadida con éxito.")
            logAdminActivity("Admin agregó cuenta streaming: ${email.trim()}")
        }
    }

    fun deleteStreamingAccount(accountId: String) {
        database.getReference("accounts").child(accountId).removeValue().addOnSuccessListener {
            postToast("Cuenta eliminada.")
            logAdminActivity("Admin eliminó cuenta streaming ID: $accountId")
        }
    }

    fun updateStreamingAccountStatus(accountId: String, status: AccountStatus) {
        val rRef = database.getReference("accounts").child(accountId)
        rRef.child("status").setValue(status)
        rRef.child("reports24h").setValue(0)
        logAdminActivity("Admin actualizó cuenta ID $accountId a estado: $status")
    }

    fun deleteUserAccount(userId: String) {
        if (userId == _currentUser.value.id) {
            postToast("No puedes eliminarte a ti mismo de administrador.")
            return
        }
        database.getReference("users").child(userId).removeValue().addOnSuccessListener {
            postToast("Usuario eliminado correctamente.")
            logAdminActivity("Admin eliminó usuario ID: $userId")
        }
    }

    fun adjustUserPointsDirectly(userId: String, pointsDelta: Int, reason: String) {
        val userRef = database.getReference("users").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            val userObj = snapshot.getValue(User::class.java)
            if (userObj != null) {
                val newPoints = maxOf(0, userObj.pointsCurrent + pointsDelta)
                val newHistoric = if (pointsDelta > 0) userObj.pointsHistoric + pointsDelta else userObj.pointsHistoric
                val newLevel = when {
                    newHistoric <= 500 -> 1
                    newHistoric <= 2000 -> 2
                    newHistoric <= 5000 -> 3
                    else -> 4
                }
                userRef.child("pointsCurrent").setValue(newPoints)
                userRef.child("pointsHistoric").setValue(newHistoric)
                userRef.child("level").setValue(newLevel)

                val logId = UUID.randomUUID().toString()
                val log = PointsLog(id = logId, delta = pointsDelta, source = AwardSource.TOURNAMENT_BONUS, detail = reason)
                userRef.child("pointsLogs").child(logId).setValue(log)

                postToast("Puntos ajustados para ${userObj.name}: $pointsDelta")
                logAdminActivity("Admin ajustó puntos de ${userObj.name} en $pointsDelta pts. Motivo: $reason.")
            }
        }
    }

    fun sendGlobalNotification(title: String, text: String, category: String = "announcement") {
        val notifId = UUID.randomUUID().toString()
        val notif = StreamNotification(
            id = notifId,
            title = title,
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            category = category
        )
        database.getReference("notifications").child(notifId).setValue(notif).addOnSuccessListener {
            postToast("Notificación global despachada a todos los usuarios.")
            logAdminActivity("Admin envió alerta global: $title")
        }
    }

    fun addAdminEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        val id = UUID.randomUUID().toString()
        database.getReference("admins").child(id).setValue(trimmed).addOnSuccessListener {
            postToast("Admin whitelisteado: $trimmed")
            logAdminActivity("Admin whitelist agregó: $trimmed")
        }
    }

    fun removeAdminEmail(email: String) {
        val trimmed = email.trim()
        database.getReference("admins").get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                val value = child.getValue(String::class.java)
                if (value.equals(trimmed, ignoreCase = true)) {
                    child.ref.removeValue().addOnSuccessListener {
                        postToast("Se retiró acceso admin a: $trimmed")
                        logAdminActivity("Admin whitelist retiró: $trimmed")
                    }
                    return@addOnSuccessListener
                }
            }
        }
    }

    fun updateServicePrice(serviceType: ServiceType, newPrice: Int) {
        database.getReference("prices").child(serviceType.name).setValue(newPrice).addOnSuccessListener {
            postToast("Precio de ${serviceType.name} cambiado a $newPrice pts.")
            logAdminActivity("Admin modificó precio de ${serviceType.name} a $newPrice")
        }
    }

    fun updateUnityAdsConfig(gameId: String, placementId: String) {
        val config = mapOf("gameId" to gameId, "placementId" to placementId)
        database.getReference("unity_ads_config").setValue(config).addOnSuccessListener {
            postToast("Configuración de Unity Ads guardada en Firebase.")
            logAdminActivity("Admin actualizó Unity Ads Config: GameId $gameId, Placement: $placementId")
        }.addOnFailureListener {
            postToast("Error guardando Unity Ads Config: ${it.message}")
        }
    }

    // --- RAFFLES ENGINE IMPLEMENTATIONS ---

    fun createRaffle(title: String, description: String, prizePoints: Int, ticketCost: Int) {
        val raffleId = UUID.randomUUID().toString()
        val raffle = Raffle(
            id = raffleId,
            title = title,
            description = description,
            prizePoints = prizePoints,
            ticketCost = ticketCost,
            isActive = true,
            participantsCount = 0
        )
        database.getReference("raffles").child(raffleId).setValue(raffle).addOnSuccessListener {
            postToast("Sorteo '$title' creado con éxito.")
            logAdminActivity("Admin creó sorteo: $title (Premio: $prizePoints pts, Ticket: $ticketCost)")

            sendGlobalNotification(
                title = "🎉 ¡NUEVO SORTEO ACTIVO! 🎉",
                text = "Participa ya en '$title' por solo $ticketCost puntos. ¡Gran premio de $prizePoints puntos!",
                category = "reward"
            )
        }
    }

    fun buyRaffleTicket(raffleId: String) {
        val dbUser = _currentUser.value
        val raffle = _rafflesList.value.firstOrNull { it.id == raffleId } ?: return
        if (!raffle.isActive) {
            postToast("Sorteo finalizado.")
            return
        }

        if (dbUser.pointsCurrent < raffle.ticketCost) {
            postToast("No tienes puntos suficientes (${raffle.ticketCost} necesarios).")
            return
        }

        val ticketRef = database.getReference("raffles").child(raffleId).child("participants").child(dbUser.id)
        ticketRef.get().addOnSuccessListener { snapshot ->
            val curCount = snapshot.child("ticketCount").getValue(Int::class.java) ?: 0
            if (curCount >= 5) {
                postToast("Límite de 5 boletos alcanzado.")
                return@addOnSuccessListener
            }

            addPointsToUser(-raffle.ticketCost, AwardSource.TOURNAMENT_BONUS, "Boleto Sorteo: ${raffle.title}") {
                val newCount = curCount + 1
                val participant = RaffleTicketHolder(name = dbUser.name, ticketCount = newCount, isMe = true)
                ticketRef.setValue(participant)

                val rRef = database.getReference("raffles").child(raffleId)
                rRef.child("participantsCount").get().addOnSuccessListener { countSnp ->
                    val ct = countSnp.getValue(Int::class.java) ?: 0
                    rRef.child("participantsCount").setValue(ct + 1)
                }

                postToast("¡Boleto adquirido con éxito! Tienes $newCount boletos.")
                logAdminActivity("Usuario '${dbUser.name}' compró boleto para sorteo ID: $raffleId")
            }
        }
    }

    fun endRaffleAndPickWinner(raffleId: String) {
        val raffle = _rafflesList.value.firstOrNull { it.id == raffleId } ?: return
        if (!raffle.isActive) return

        val participantsRef = database.getReference("raffles").child(raffleId).child("participants")
        participantsRef.get().addOnSuccessListener { snapshot ->
            val participants = mutableListOf<Pair<String, RaffleTicketHolder>>()
            for (child in snapshot.children) {
                val holder = child.getValue(RaffleTicketHolder::class.java)
                val uid = child.key
                if (holder != null && uid != null) {
                    participants.add(Pair(uid, holder))
                }
            }

            if (participants.isEmpty()) {
                database.getReference("raffles").child(raffleId).child("isActive").setValue(false)
                database.getReference("raffles").child(raffleId).child("winnerName").setValue("Nadie (Sin participantes)")
                postToast("Sorteo cerrado sin participantes.")
                return@addOnSuccessListener
            }

            val weightedPool = mutableListOf<String>()
            for (p in participants) {
                repeat(p.second.ticketCount) {
                    weightedPool.add(p.first)
                }
            }

            val winnerUid = weightedPool.random()
            val winnerPair = participants.first { it.first == winnerUid }
            val winnerName = winnerPair.second.name

            database.getReference("users").child(winnerUid).get().addOnSuccessListener { userSnp ->
                val winnerEmail = userSnp.child("email").getValue(String::class.java) ?: "ganador@streamrewards.pro"

                adjustUserPointsDirectly(winnerUid, raffle.prizePoints, "Premio de Ganador de Sorteo: ${raffle.title}")

                val raffleRef = database.getReference("raffles").child(raffleId)
                raffleRef.child("isActive").setValue(false)
                raffleRef.child("winnerName").setValue(winnerName)
                raffleRef.child("winnerEmail").setValue(winnerEmail)

                sendGlobalNotification(
                    title = "🎉 ¡Ganador del Sorteo 🎉",
                    text = "Felicitaciones a '$winnerName' ($winnerEmail) por ganar el sorteo '${raffle.title}' y llevarse +${raffle.prizePoints} puntos.",
                    category = "system"
                )

                postToast("¡Ganador seleccionado: $winnerName! Puntos otorgados.")
            }
        }
    }

    // --- HELPER WRAPPERS ---

    private fun postNotification(title: String, text: String, category: String = "info") {
        try {
            val notifId = UUID.randomUUID().toString()
            val notif = StreamNotification(
                id = notifId,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                category = category
            )
            // Save log locally in user flow log
            _notifications.value = listOf(notif) + _notifications.value
            _notificationEvents.tryEmit(Pair(title, text))
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addPointsToUser(pointsDelta: Int, source: AwardSource, description: String, onComplete: (() -> Unit)? = null) {
        val user = _currentUser.value
        val newCurrent = maxOf(0, user.pointsCurrent + pointsDelta)
        val newHistoric = if (pointsDelta > 0) user.pointsHistoric + pointsDelta else user.pointsHistoric

        val newLevel = when {
            newHistoric <= 500 -> 1
            newHistoric <= 2000 -> 2
            newHistoric <= 5000 -> 3
            else -> 4
        }

        val updatedUser = user.copy(
            pointsCurrent = newCurrent,
            pointsHistoric = newHistoric,
            level = newLevel
        )

        try {
            database.getReference("users").child(user.id).setValue(updatedUser).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete?.invoke()
                }
            }

            val logId = UUID.randomUUID().toString()
            val log = PointsLog(id = logId, delta = pointsDelta, source = source, detail = description)
            _pointsLogs.value = listOf(log) + _pointsLogs.value
            database.getReference("users").child(user.id).child("pointsLogs").child(logId).setValue(log)

            if (pointsDelta > 0) {
                postNotification(
                    title = "¡Ganaste Puntos! 🪙",
                    text = "Has recibido +$pointsDelta puntos de: $description.",
                    category = "points"
                )
            } else if (pointsDelta < 0) {
                postNotification(
                    title = "Puntos Canjeados 🛍️",
                    text = "Has canjeado ${-pointsDelta} puntos por: $description.",
                    category = "reward"
                )
            }

            if (newLevel != user.level) {
                postToast("¡Felicidades! Subiste al Nivel $newLevel")
                logAdminActivity("Usuario '${user.name}' ascendió al nivel $newLevel (${newHistoric} pts históricos).")
                postNotification(
                    title = "¡Ascenso de Nivel! 🏆",
                    text = "¡Felicidades! Ahora tienes excelentes beneficios de Nivel $newLevel.",
                    category = "system"
                )
            }
        } catch (e: Exception) {
            Log.e("StreamRewards", "Error updating user points: ${e.message}")
        }
    }

    private fun incrementTournamentScore(points: Int) {
        val me = _tournamentsLeaderboard.value.firstOrNull { it.isMe }
        _tournamentsLeaderboard.value = _tournamentsLeaderboard.value.map {
            if (it.isMe) it.copy(pointsInTournament = it.pointsInTournament + points) else it
        }.sortedByDescending { it.pointsInTournament }
    }

    private fun incrementMissionProgress(missionId: String, delta: Int) {
        val currentProgress = _misionesProgress.value[missionId] ?: return
        if (currentProgress.isClaimed) return

        val mission = _dailyMissions.value.firstOrNull { it.id == missionId } ?: return
        val newCount = minOf(mission.targetCount, currentProgress.currentCount + delta)
        _misionesProgress.value = _misionesProgress.value + (missionId to currentProgress.copy(currentCount = newCount))

        if (newCount >= mission.targetCount && !currentProgress.isClaimed) {
            postToast("¡Misión del día completada! ${mission.title}: +${mission.rewardPoints} pts.")
            addPointsToUser(mission.rewardPoints, AwardSource.TOURNAMENT_BONUS, "Misión diaria: ${mission.title}") {
                _misionesProgress.value = _misionesProgress.value + (missionId to currentProgress.copy(currentCount = newCount, isClaimed = true))
            }
        }
    }

    fun loginWithEmailAndPassword(email: String, passwordDecoded: String) {
        _authLoading.value = true
        _authError.value = null

        val auth = firebaseAuth
        if (auth != null) {
            auth.signInWithEmailAndPassword(email, passwordDecoded)
                .addOnCompleteListener { task ->
                    _authLoading.value = false
                    if (task.isSuccessful) {
                        val fbUser = task.result?.user
                        val uid = fbUser?.uid ?: ""
                        val mailStr = fbUser?.email ?: email

                        _isLoggedIn.value = true
                        _authSuccessLog.value = "Sesión iniciada"
                        postToast("Sesión iniciada con Firebase Auth")
                        listenToUserProfile(uid, mailStr)
                        startGlobalListeners()
                        logAdminActivity("Inicio de sesión exitoso mediante Firebase: $mailStr")
                    } else {
                        val errorMessage = task.exception?.localizedMessage ?: "Credenciales incorrectas de Firebase"
                        _authError.value = errorMessage
                        postToast(errorMessage)
                    }
                }
        } else {
            _authLoading.value = false
            val errMsg = "Error: Firebase Authentication no disponible."
            _authError.value = errMsg
            postToast(errMsg)
        }
    }

    fun registerWithEmailAndPassword(email: String, passwordDecoded: String, fullName: String, referralCode: String = "") {
        _authLoading.value = true
        _authError.value = null

        val auth = firebaseAuth
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email, passwordDecoded)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = task.result?.user
                        val uid = fbUser?.uid ?: ""
                        val mailStr = email.trim()
                        val pointsBonus = if (referralCode.isNotBlank()) 100 else 50

                        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                            displayName = fullName
                        }
                        fbUser?.updateProfile(profileUpdates)

                        _isLoggedIn.value = true
                        _authLoading.value = false
                        _authSuccessLog.value = "Cuenta registrada con éxito"
                        listenToUserProfile(uid, mailStr)
                        startGlobalListeners()

                        // Initialize user document in Database with welcome bonus points
                        val isPrincipal = mailStr.equals("blandonjose6788@gmail.com", ignoreCase = true)
                        val startPoints = if (isPrincipal) 99999 else pointsBonus
                        val defaultUser = User(
                            id = uid,
                            name = fullName,
                            email = mailStr,
                            pointsCurrent = startPoints,
                            pointsHistoric = startPoints,
                            level = if (isPrincipal) 4 else 1,
                            rachaCheckIn = 1
                        )
                        database.getReference("users").child(uid).setValue(defaultUser)

                        postToast("Cuenta creada. ¡Bono de bienvenida recibido!")
                        logAdminActivity("Nuevo registro en Firebase: $mailStr (Referido: ${referralCode.ifBlank { "Ninguno" }})")
                    } else {
                        _authLoading.value = false
                        val errorMessage = task.exception?.localizedMessage ?: "Error al registrar en Firebase"
                        _authError.value = errorMessage
                        postToast(errorMessage)
                    }
                }
        } else {
            _authLoading.value = false
            val errMsg = "Error: El servicio de Firebase no está disponible."
            _authError.value = errMsg
            postToast(errMsg)
        }
    }

    fun loginWithGoogle(email: String, displayName: String) {
        _authLoading.value = true
        _authError.value = null

        // Since GoogleSignIn gives an authorized email, check if whitelisted admin or create default user in Firebase Database
        viewModelScope.launch {
            delay(500)
            _authLoading.value = false
            _isLoggedIn.value = true
            _authSuccessLog.value = "Conectado vía Google"
            postToast("Sesión iniciada con Google: $email")
            logAdminActivity("Google Sign-In exitoso: $email ($displayName)")

            // Programmatically sign in or load profile using UID mapped to email hash
            val sanitizedUid = "google_" + email.replace(".", "_").replace("@", "_")
            listenToUserProfile(sanitizedUid, email)
            startGlobalListeners()
        }
    }

    fun logoutUser() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentUser.value = User(id = "visitante", name = "Usuario Invitado", email = "visitante@streamrewards.pro", pointsCurrent = 0, pointsHistoric = 0)
        _isLoggedIn.value = false
        _authSuccessLog.value = null
        postToast("Sesión cerrada")
        logAdminActivity("Sesión de usuario finalizada.")
    }
}
