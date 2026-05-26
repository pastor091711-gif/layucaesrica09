package com.example.model

import java.util.UUID

/**
 * Representación de esquemas relacionales para StreamRewards Pro
 * Mapeado a clases Kotlin para soporte en la app Android interactiva con Firebase Realtime Database.
 */

enum class AccountStatus { ACTIVE, REPROTED, EN_ROTACION, DOWN }
enum class ServiceType { NETFLIX, PRIME_VIDEO, DISNEY_PLUS, HBO_MAX }
enum class AwardSource { DAILY_CHECKIN, AD_REWARD, REFERRAL, REDEMPTION_REFUND, TOURNAMENT_BONUS, REPORT_COMMISSION, MISSION }

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val rachaCheckIn: Int = 0,
    val pointsHistoric: Int = 0,
    val pointsCurrent: Int = 0,
    val level: Int = 1,
    val friendsReferred: Int = 0,
    val lastCheckInClaimed: Long? = null
)

data class StreamingAccount(
    val id: String = "",
    val serviceType: ServiceType = ServiceType.NETFLIX,
    val email: String = "",
    val passwordDecoded: String = "",
    val status: AccountStatus = AccountStatus.ACTIVE,
    val lastRotation: Long = System.currentTimeMillis(),
    val reports24h: Int = 0
)

data class ActiveRedemption(
    val id: String = "",
    val accountId: String = "",
    val userId: String = "", // Links to the owner user
    val serviceType: ServiceType = ServiceType.NETFLIX,
    val email: String = "",
    val passwordDecoded: String = "",
    val redeemCost: Int = 0,
    val expiresAt: Long = 0L, // 0 For indefinite/permanent access (not leased by time as per instructions)
    val reported: Boolean = false
)

data class PointsLog(
    val id: String = "",
    val delta: Int = 0,
    val source: AwardSource = AwardSource.DAILY_CHECKIN,
    val detail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class DailyMission(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val targetCount: Int = 0,
    val rewardPoints: Int = 0
)

data class MissionProgress(
    val missionId: String = "",
    val currentCount: Int = 0,
    val isClaimed: Boolean = false
)

data class Contestant(
    val name: String = "",
    val pointsInTournament: Int = 0,
    val isMe: Boolean = false
)

data class RaffleTicketHolder(
    val name: String = "",
    val ticketCount: Int = 0,
    val isMe: Boolean = false
)

data class ActivityLog(
    val id: String = "",
    val action: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class StreamNotification(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val category: String = "info" // "points", "reward", "system", "announcement"
)

data class Raffle(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val prizePoints: Int = 1000,
    val ticketCost: Int = 20,
    val isActive: Boolean = true,
    val participantsCount: Int = 0,
    val winnerName: String? = null,
    val winnerEmail: String? = null
)
