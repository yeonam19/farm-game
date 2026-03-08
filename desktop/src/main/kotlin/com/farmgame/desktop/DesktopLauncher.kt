package com.farmgame.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.farmgame.FarmGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("힐링 농장 - Healing Farm")
        setWindowedMode(1920, 1080)
        setResizable(true)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(FarmGame(), config)
}
