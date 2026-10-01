package com.multidrive.app.domain.usecase

import com.multidrive.app.domain.model.Account
import com.multidrive.app.domain.model.RoutingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectTargetAccountUseCase @Inject constructor() {
    private var roundRobinIndex = 0

    fun execute(
        accounts: List<Account>,
        mode: RoutingMode,
        fileSize: Long,
        reservedBytesMap: Map<Int, Long> = emptyMap(),
        manualSelectedId: Int? = null,
        priorityAccountIds: List<Int> = emptyList()
    ): Account? {
        val eligibleAccounts = accounts.filter { account ->
            if (!account.isActive) return@filter false
            val reserved = reservedBytesMap[account.id] ?: 0L
            val available = account.storageAvailable - reserved
            account.storageQuota == 0L || available >= fileSize
        }

        if (eligibleAccounts.isEmpty()) return null

        return when (mode) {
            RoutingMode.MANUAL -> {
                eligibleAccounts.find { it.id == manualSelectedId } ?: eligibleAccounts.first()
            }
            RoutingMode.MOST_AVAILABLE -> {
                eligibleAccounts.maxByOrNull { account ->
                    val reserved = reservedBytesMap[account.id] ?: 0L
                    account.storageAvailable - reserved
                }
            }
            RoutingMode.ROUND_ROBIN -> {
                val ordered = if (priorityAccountIds.isNotEmpty()) {
                    eligibleAccounts.sortedBy { account ->
                        val idx = priorityAccountIds.indexOf(account.id)
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }
                } else {
                    eligibleAccounts
                }
                val selected = ordered[roundRobinIndex % ordered.size]
                roundRobinIndex++
                selected
            }
            RoutingMode.PRIORITY -> {
                if (priorityAccountIds.isNotEmpty()) {
                    priorityAccountIds.firstNotNullOfOrNull { id ->
                        eligibleAccounts.find { it.id == id }
                    } ?: eligibleAccounts.first()
                } else {
                    eligibleAccounts.first()
                }
            }
        }
    }
}
