package com.farmgame.system

import com.farmgame.data.Season
import com.farmgame.data.Weather

class WeatherSystem {
    var current: Weather = Weather.SUNNY
    private var weatherTimer: Float = 0f
    private var weatherDuration: Float = randomDuration()

    private fun randomDuration(): Float = 60f + Math.random().toFloat() * 180f // 1-4 minutes

    fun update(delta: Float, season: Season) {
        weatherTimer += delta
        if (weatherTimer >= weatherDuration) {
            weatherTimer = 0f
            weatherDuration = randomDuration()
            changeWeather(season)
        }
    }

    private fun changeWeather(season: Season) {
        val possible = Weather.entries.filter { it.canOccurIn(season) }
        val weights = when (season) {
            Season.SPRING -> mapOf(Weather.SUNNY to 3, Weather.CLOUDY to 2, Weather.RAINY to 3)
            Season.SUMMER -> mapOf(Weather.SUNNY to 4, Weather.CLOUDY to 1, Weather.RAINY to 2, Weather.STORMY to 1)
            Season.AUTUMN -> mapOf(Weather.SUNNY to 2, Weather.CLOUDY to 3, Weather.RAINY to 3, Weather.STORMY to 1)
            Season.WINTER -> mapOf(Weather.SUNNY to 2, Weather.CLOUDY to 3, Weather.RAINY to 1, Weather.SNOWY to 3)
        }

        val totalWeight = possible.sumOf { weights[it] ?: 1 }
        var roll = (Math.random() * totalWeight).toInt()
        for (weather in possible) {
            roll -= weights[weather] ?: 1
            if (roll <= 0) {
                current = weather
                return
            }
        }
        current = Weather.SUNNY
    }
}
