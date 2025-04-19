package com.ndriqa.musicky.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screens {
    @Serializable object Songs : Screens()
    @Serializable object Settings : Screens()
}