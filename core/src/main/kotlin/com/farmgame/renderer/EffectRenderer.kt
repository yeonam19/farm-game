package com.farmgame.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.farmgame.data.Weather
import com.farmgame.world.GameWorld

class EffectRenderer(
    private val shapeRenderer: ShapeRenderer,
    private val world: GameWorld,
    private val tileSize: Float
) {
    var animTimer: Float = 0f
    val raindrops = mutableListOf<FloatArray>()

    fun renderFacingIndicator() {
        val (fx, fy) = world.player.getFacingTile()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        val pulse = (MathUtils.sin(animTimer * 3f) + 1) / 2
        shapeRenderer.color = Color(1f, 1f, 1f, 0.3f + 0.3f * pulse)
        shapeRenderer.rect(fx * tileSize + 1, fy * tileSize + 1, tileSize - 2, tileSize - 2)
        shapeRenderer.end()
    }

    fun renderWeatherEffects() {
        if (raindrops.isEmpty()) return

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        when (world.weatherSystem.current) {
            Weather.RAINY, Weather.STORMY -> {
                for (drop in raindrops) {
                    shapeRenderer.color = Color(0.6f, 0.7f, 1f, 0.35f)
                    shapeRenderer.rect(drop[0], drop[1], 1.5f, 6f)
                }
            }
            Weather.SNOWY -> {
                for (drop in raindrops) {
                    shapeRenderer.color = Color(1f, 1f, 1f, 0.6f)
                    shapeRenderer.rect(drop[0], drop[1], 3f, 3f)
                }
            }
            else -> {}
        }
        shapeRenderer.end()
    }

    fun renderStormOverlay(camera: OrthographicCamera, zoom: Float) {
        if (world.weatherSystem.current != Weather.RAINY && world.weatherSystem.current != Weather.STORMY) return
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val alpha = if (world.weatherSystem.current == Weather.STORMY) 0.1f else 0.05f
        shapeRenderer.color = Color(0.05f, 0.05f, 0.05f, alpha)
        val hw = camera.viewportWidth * zoom / 2
        val hh = camera.viewportHeight * zoom / 2
        shapeRenderer.rect(camera.position.x - hw, camera.position.y - hh, hw * 2, hh * 2)
        shapeRenderer.end()
    }

    fun renderNightOverlay(camera: OrthographicCamera, zoom: Float) {
        if (!world.timeSystem.isNight) return
        val nightAlpha = (1f - world.timeSystem.daylight) * 0.12f
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.02f, 0.02f, 0.05f, nightAlpha)
        val hw = camera.viewportWidth * zoom / 2
        val hh = camera.viewportHeight * zoom / 2
        shapeRenderer.rect(camera.position.x - hw, camera.position.y - hh, hw * 2, hh * 2)
        shapeRenderer.end()
    }

    fun updateRain(delta: Float, camera: OrthographicCamera, zoom: Float) {
        val isRaining = world.weatherSystem.current == Weather.RAINY || world.weatherSystem.current == Weather.STORMY
        val isSnowing = world.weatherSystem.current == Weather.SNOWY

        if (isRaining || isSnowing) {
            val intensity = if (world.weatherSystem.current == Weather.STORMY) 6 else 3
            repeat(intensity) {
                val x = camera.position.x + MathUtils.random(-400f, 400f) * zoom
                val y = camera.position.y + camera.viewportHeight * zoom / 2 + 20f
                val speed = if (isSnowing) MathUtils.random(30f, 60f) else MathUtils.random(150f, 300f)
                raindrops.add(floatArrayOf(x, y, speed))
            }
            val iter = raindrops.iterator()
            while (iter.hasNext()) {
                val drop = iter.next()
                drop[1] -= drop[2] * delta
                if (isSnowing) drop[0] += MathUtils.sin(drop[1] * 0.05f) * 20f * delta
                if (drop[1] < camera.position.y - camera.viewportHeight * zoom / 2 - 20f) iter.remove()
            }
            while (raindrops.size > 500) raindrops.removeAt(0)
        } else {
            raindrops.clear()
        }
    }
}
