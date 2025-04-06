package com.ndriqa.musicky.navigation

sealed class Screens(val route: String) {

    object Songs : Screens(route = "songs")
    object About : Screens(route = "about")
    object Options : Screens(route = "options")
    object ScreenTwo : Screens(route = "screenTwo/{parameter}") {
        fun createRoute(parameter: String) = "screenTwo/$parameter"
    }
}