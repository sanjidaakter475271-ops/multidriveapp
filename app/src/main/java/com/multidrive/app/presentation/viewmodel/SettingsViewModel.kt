package com.multidrive.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multidrive.app.data.local.dao.SettingsDao
import com.multidrive.app.data.local.entity.AppSettingEntity
import com.multidrive.app.domain.model.RoutingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    val routingMode: StateFlow<RoutingMode> = settingsDao.getSetting("routing_mode")
        .map { entity ->
            try {
                RoutingMode.valueOf(entity?.value ?: RoutingMode.MOST_AVAILABLE.name)
            } catch (_: Exception) {
                RoutingMode.MOST_AVAILABLE
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RoutingMode.MOST_AVAILABLE
        )

    fun setRoutingMode(mode: RoutingMode) {
        viewModelScope.launch {
            settingsDao.insertSetting(AppSettingEntity("routing_mode", mode.name))
        }
    }
}
