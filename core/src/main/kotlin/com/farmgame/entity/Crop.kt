package com.farmgame.entity

import com.badlogic.gdx.graphics.Color
import com.farmgame.data.CropType
import com.farmgame.data.Weather

class Crop(val type: CropType) {
    var currentStage: Int = 0
    var growthProgress: Float = 0f
    var isWatered: Boolean = false
    var isDead: Boolean = false
    private var daysWithoutWater: Int = 0

    val isReady: Boolean get() = currentStage >= type.growthStages && !isDead

    val displayColor: Color
        get() {
            if (isDead) return Color(0.4f, 0.35f, 0.3f, 1f)
            val ratio = currentStage.toFloat() / type.growthStages
            return Color(
                type.seedColor.r + (type.grownColor.r - type.seedColor.r) * ratio,
                type.seedColor.g + (type.grownColor.g - type.seedColor.g) * ratio,
                type.seedColor.b + (type.grownColor.b - type.seedColor.b) * ratio,
                1f
            )
        }

    val sizeRatio: Float
        get() = if (isDead) 0.3f else 0.3f + 0.7f * (currentStage.toFloat() / type.growthStages)

    fun update(delta: Float, watered: Boolean, weather: Weather) {
        if (isDead || isReady) return

        isWatered = watered || weather.autoWater

        if (isWatered) {
            daysWithoutWater = 0
            growthProgress += delta * weather.growthMultiplier
            if (growthProgress >= type.growthTime) {
                growthProgress = 0f
                currentStage++
            }
        } else {
            // Crops can survive a bit without water but grow slowly
            growthProgress += delta * 0.2f * weather.growthMultiplier
            if (growthProgress >= type.growthTime) {
                growthProgress = 0f
                currentStage++
            }
        }
    }

    fun onNewDay(wasWatered: Boolean) {
        if (!wasWatered && !isDead) {
            daysWithoutWater++
            if (daysWithoutWater >= 2) {
                isDead = true
            }
        } else {
            daysWithoutWater = 0
        }
    }
}
