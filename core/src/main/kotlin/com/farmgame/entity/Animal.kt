package com.farmgame.entity

import com.farmgame.data.AnimalType

class Animal(
    val type: AnimalType,
    var tileX: Int,
    var tileY: Int
) {
    var productTimer: Float = 0f
    var hasProduct: Boolean = false
    var happiness: Float = 0.7f
    private var moveTimer: Float = 0f
    private var targetX: Float = tileX.toFloat()
    private var targetY: Float = tileY.toFloat()
    var renderX: Float = tileX.toFloat()
    var renderY: Float = tileY.toFloat()

    fun update(delta: Float, mapWidth: Int, mapHeight: Int) {
        // Product generation
        if (!hasProduct) {
            productTimer += delta * happiness
            if (productTimer >= type.productionInterval) {
                hasProduct = true
                productTimer = 0f
            }
        }

        // Random movement
        moveTimer += delta
        if (moveTimer > 3f) {
            moveTimer = 0f
            val dx = (-1..1).random()
            val dy = (-1..1).random()
            val newX = (tileX + dx).coerceIn(0, mapWidth - 1)
            val newY = (tileY + dy).coerceIn(0, mapHeight - 1)
            tileX = newX
            tileY = newY
            targetX = newX.toFloat()
            targetY = newY.toFloat()
        }

        // Smooth movement
        val speed = 2f * delta
        renderX += (targetX - renderX) * speed.coerceAtMost(1f)
        renderY += (targetY - renderY) * speed.coerceAtMost(1f)
    }

    fun collect(): Boolean {
        if (hasProduct) {
            hasProduct = false
            return true
        }
        return false
    }

    fun feed() {
        happiness = (happiness + 0.1f).coerceAtMost(1f)
    }
}
