package com.multidrive.app.domain.repository

import com.multidrive.app.data.local.entity.AppSettingEntity
import com.multidrive.app.domain.model.RoutingMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSetting(key: String): Flow<AppSettingEntity?>
    suspend fun saveSetting(key: String, value: String)
    fun getRoutingMode(): Flow<RoutingMode>
    suspend fun setRoutingMode(mode: RoutingMode)
    fun getApiKey(): Flow<String?>
    suspend fun setApiKey(apiKey: String)
}
