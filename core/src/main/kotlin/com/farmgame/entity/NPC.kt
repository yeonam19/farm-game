package com.farmgame.entity

import com.farmgame.data.NPCData

class NPC(
    val data: NPCData,
    var tileX: Int,
    var tileY: Int
) {
    val homeX: Int = tileX
    val homeY: Int = tileY
    var renderX: Float = tileX.toFloat()
    var renderY: Float = tileY.toFloat()
    private var moveTimer: Float = 0f
    private var currentDialogueIndex: Int = 0
    private val wanderRadius: Int = 4

    fun update(delta: Float, isWalkable: ((Int, Int) -> Boolean)? = null) {
        moveTimer += delta
        if (moveTimer > 5f) {
            moveTimer = 0f
            val dx = (-1..1).random()
            val dy = (-1..1).random()
            val newX = tileX + dx
            val newY = tileY + dy
            // Stay within wander radius of home position and only walk on walkable tiles
            if (Math.abs(newX - homeX) <= wanderRadius && Math.abs(newY - homeY) <= wanderRadius) {
                if (isWalkable == null || isWalkable(newX, newY)) {
                    tileX = newX
                    tileY = newY
                }
            }
        }
        val speed = 2f * delta
        renderX += (tileX.toFloat() - renderX) * speed.coerceAtMost(1f)
        renderY += (tileY.toFloat() - renderY) * speed.coerceAtMost(1f)
    }

    fun getDialogue(): String {
        val dialogue = data.dialogues[currentDialogueIndex]
        currentDialogueIndex = (currentDialogueIndex + 1) % data.dialogues.size
        return "${data.koreanName}: $dialogue"
    }
}
