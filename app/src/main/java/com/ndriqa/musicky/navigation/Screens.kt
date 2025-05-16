package com.ndriqa.musicky.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screens {
    @Serializable object Songs : Screens()
    @Serializable data class Settings(
        val preSelectedSetting: String? = null
    ) : Screens()
}