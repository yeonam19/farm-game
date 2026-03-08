package com.farmgame.system

import com.farmgame.data.Season

class TimeSystem {
    var timeOfDay: Float = 0.3f   // 0.0 = midnight, 0.5 = noon, 1.0 = midnight
    var day: Int = 1
    var season: Season = Season.SPRING
    var year: Int = 1

    private val dayLengthSeconds: Float = 720f  // 12 minutes = 1 game day
    val daysPerSeason: Int = 28

    val hour: Int get() = ((timeOfDay * 24) % 24).toInt()
    val minute: Int get() = ((timeOfDay * 24 * 60) % 60).toInt()
    val timeString: String get() = String.format("%02d:%02d", hour, minute)

    val isDaytime: Boolean get() = timeOfDay in 0.25f..0.83f
    val isNight: Boolean get() = !isDaytime

    val daylight: Float
        get() {
            // Smooth day/night cycle - minimum 0.4 so player can always see
            val noon = 0.5f
            val dist = Math.abs(timeOfDay - noon)
            return (1f - dist * 2.0f).coerceIn(0.4f, 1f)
        }

    var onNewDay: (() -> Unit)? = null
    var onNewSeason: (() -> Unit)? = null

    fun update(delta: Float) {
        val prevDay = day
        timeOfDay += delta / dayLengthSeconds

        if (timeOfDay >= 1f) {
            timeOfDay -= 1f
            day++

            if (day > daysPerSeason) {
                day = 1
                season = season.next()
                if (season == Season.SPRING) {
                    year++
                }
                onNewSeason?.invoke()
            }

            onNewDay?.invoke()
        }
    }

    fun getSeasonDay(): String = "${season.koreanName} ${day}일"
    fun getFullDate(): String = "${year}년 ${season.koreanName} ${day}일 $timeString"
}
