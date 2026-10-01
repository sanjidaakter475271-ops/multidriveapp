package com.multidrive.app.data.repository

import com.multidrive.app.data.local.dao.SettingsDao
import com.multidrive.app.data.local.entity.AppSettingEntity
import com.multidrive.app.domain.model.RoutingMode
import com.multidrive.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override fun getSetting(key: String): Flow<AppSettingEntity?> {
        return settingsDao.getSetting(key)
    }

    override suspend fun saveSetting(key: String, value: String) {
        settingsDao.insertSetting(AppSettingEntity(key, value))
    }

    override fun getRoutingMode(): Flow<RoutingMode> {
        return settingsDao.getSetting("routing_mode").map { setting ->
            setting?.value?.let {
                runCatching { RoutingMode.valueOf(it) }.getOrDefault(RoutingMode.MOST_AVAILABLE)
            } ?: RoutingMode.MOST_AVAILABLE
        }
    }

    override suspend fun setRoutingMode(mode: RoutingMode) {
        settingsDao.insertSetting(AppSettingEntity("routing_mode", mode.name))
    }

    override fun getApiKey(): Flow<String?> {
        return settingsDao.getSetting("api_key").map { it?.value }
    }

    override suspend fun setApiKey(apiKey: String) {
        settingsDao.insertSetting(AppSettingEntity("api_key", apiKey))
    }
}
