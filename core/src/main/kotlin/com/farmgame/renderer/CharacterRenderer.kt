package com.farmgame.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.farmgame.data.Tool
import com.farmgame.entity.Direction
import com.farmgame.entity.NPC
import com.farmgame.world.GameWorld
import com.farmgame.world.HiredWorker

class CharacterRenderer(
    private val shapeRenderer: ShapeRenderer,
    private val world: GameWorld,
    private val tileSize: Float
) {
    var animTimer: Float = 0f

    // ===== NPC RENDERING =====

    fun renderNPCs(daylight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (npc in world.npcs) renderSingleNPC(npc, daylight)
        shapeRenderer.end()
    }

    private fun renderSingleNPC(npc: NPC, d: Float) {
        val nx = npc.renderX * tileSize
        val ny = npc.renderY * tileSize
        val c = npc.data.color
        val walkCycle = MathUtils.sin(animTimer * 5f + npc.renderX * 3f)

        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
        shapeRenderer.rect(nx + 8, ny - 1, 32f, 5f)

        shapeRenderer.color = Color(c.r * 0.5f * d, c.g * 0.5f * d, c.b * 0.5f * d, 1f)
        val legOff = walkCycle * 2f
        shapeRenderer.rect(nx + 14, ny + legOff.coerceAtLeast(0f), 7f, 12f)
        shapeRenderer.rect(nx + 27, ny + (-legOff).coerceAtLeast(0f), 7f, 12f)

        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.15f * d, 1f)
        shapeRenderer.rect(nx + 13, ny + legOff.coerceAtLeast(0f), 9f, 4f)
        shapeRenderer.rect(nx + 26, ny + (-legOff).coerceAtLeast(0f), 9f, 4f)

        shapeRenderer.color = Color(c.r * d, c.g * d, c.b * d, 1f)
        shapeRenderer.rect(nx + 10, ny + 12, 28f, 18f)

        val skinR = 0.9f * d; val skinG = 0.75f * d; val skinB = 0.6f * d
        shapeRenderer.color = Color(skinR, skinG, skinB, 1f)
        shapeRenderer.rect(nx + 4, ny + 13 + legOff, 7f, 12f)
        shapeRenderer.rect(nx + 37, ny + 13 - legOff, 7f, 12f)

        shapeRenderer.color = Color(0.95f * d, 0.8f * d, 0.65f * d, 1f)
        shapeRenderer.rect(nx + 13, ny + 30, 22f, 14f)

        val hairR = ((c.r * 0.5f + 0.1f) * d).coerceAtMost(1f)
        val hairG = ((c.g * 0.3f + 0.05f) * d).coerceAtMost(1f)
        val hairB = ((c.b * 0.2f) * d).coerceAtMost(1f)
        shapeRenderer.color = Color(hairR, hairG, hairB, 1f)
        shapeRenderer.rect(nx + 12, ny + 40, 24f, 6f)
        shapeRenderer.rect(nx + 13, ny + 38, 22f, 4f)

        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(nx + 16, ny + 36, 6f, 4f); shapeRenderer.rect(nx + 26, ny + 36, 6f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.15f * d, 1f)
        shapeRenderer.rect(nx + 18, ny + 36, 3f, 3f); shapeRenderer.rect(nx + 28, ny + 36, 3f, 3f)
        shapeRenderer.color = Color(0.8f * d, 0.45f * d, 0.35f * d, 1f)
        shapeRenderer.rect(nx + 20, ny + 32, 8f, 2f)

        when (npc.data.name) {
            "Merchant" -> { shapeRenderer.color = Color(0.8f * d, 0.2f * d, 0.2f * d, 1f); shapeRenderer.rect(nx + 10, ny + 44, 28f, 4f) }
            "Elder" -> { shapeRenderer.color = Color(0.85f * d, 0.85f * d, 0.82f * d, 1f); shapeRenderer.rect(nx + 12, ny + 40, 24f, 6f); shapeRenderer.rect(nx + 13, ny + 38, 22f, 4f) }
            "Fisher" -> { shapeRenderer.color = Color(0.3f * d, 0.5f * d, 0.7f * d, 1f); shapeRenderer.rect(nx + 9, ny + 44, 30f, 4f); shapeRenderer.rect(nx + 13, ny + 47, 22f, 5f) }
            "Farmer" -> { shapeRenderer.color = Color(0.8f * d, 0.65f * d, 0.2f * d, 1f); shapeRenderer.rect(nx + 7, ny + 44, 34f, 4f); shapeRenderer.rect(nx + 13, ny + 47, 22f, 5f) }
        }
    }

    // ===== FARMER RENDERING =====

    fun renderFarmers(daylight: Float) {
        if (world.farmers.isEmpty()) return
        for (farmer in world.farmers) renderSingleFarmer(farmer, daylight)
    }

    private fun renderSingleFarmer(farmer: HiredWorker, daylight: Float) {
        val d = daylight; val fx = farmer.renderX * tileSize; val fy = farmer.renderY * tileSize
        val walkCycle = MathUtils.sin(animTimer * 5f + farmer.renderX * 3f)
        val isWorking = farmer.action in listOf("till", "water", "plant", "harvest")
        val workBob = if (isWorking) MathUtils.sin(animTimer * 8f) * 3f else 0f
        val level = farmer.level

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f); shapeRenderer.rect(fx + 8, fy - 1, 32f, 5f)

        shapeRenderer.color = Color(0.45f * d, 0.35f * d, 0.2f * d, 1f)
        val legOff = if (farmer.action == "walk") walkCycle * 2.5f else 0f
        shapeRenderer.rect(fx + 14, fy + legOff.coerceAtLeast(0f), 7f, 12f)
        shapeRenderer.rect(fx + 27, fy + (-legOff).coerceAtLeast(0f), 7f, 12f)

        shapeRenderer.color = Color(0.25f * d, 0.15f * d, 0.08f * d, 1f)
        shapeRenderer.rect(fx + 13, fy + legOff.coerceAtLeast(0f), 9f, 4f)
        shapeRenderer.rect(fx + 26, fy + (-legOff).coerceAtLeast(0f), 9f, 4f)

        shapeRenderer.color = when (level) {
            1 -> Color(0.25f * d, 0.55f * d, 0.2f * d, 1f)
            2 -> Color(0.3f * d, 0.4f * d, 0.7f * d, 1f)
            3 -> Color(0.7f * d, 0.15f * d, 0.15f * d, 1f)
            else -> Color(0.5f * d, 0.5f * d, 0.5f * d, 1f)
        }
        shapeRenderer.rect(fx + 10, fy + 12 + workBob, 28f, 18f)

        val skinR = 0.85f * d; val skinG = 0.65f * d; val skinB = 0.45f * d
        shapeRenderer.color = Color(skinR, skinG, skinB, 1f)
        if (isWorking) {
            val armSwing = MathUtils.sin(animTimer * 8f) * 6f
            shapeRenderer.rect(fx + 4, fy + 16 + armSwing, 7f, 12f); shapeRenderer.rect(fx + 37, fy + 16 - armSwing, 7f, 12f)
        } else {
            shapeRenderer.rect(fx + 4, fy + 13 + legOff, 7f, 12f); shapeRenderer.rect(fx + 37, fy + 13 - legOff, 7f, 12f)
        }

        shapeRenderer.color = Color(0.9f * d, 0.72f * d, 0.5f * d, 1f)
        shapeRenderer.rect(fx + 13, fy + 30 + workBob, 22f, 14f)
        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(fx + 16, fy + 36 + workBob, 6f, 4f); shapeRenderer.rect(fx + 26, fy + 36 + workBob, 6f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.15f * d, 1f)
        shapeRenderer.rect(fx + 18, fy + 36 + workBob, 3f, 3f); shapeRenderer.rect(fx + 28, fy + 36 + workBob, 3f, 3f)
        shapeRenderer.color = Color(0.75f * d, 0.4f * d, 0.3f * d, 1f)
        shapeRenderer.rect(fx + 19, fy + 32 + workBob, 10f, 2f)

        when (level) {
            1 -> {
                shapeRenderer.color = Color(0.85f * d, 0.75f * d, 0.35f * d, 1f)
                shapeRenderer.rect(fx + 6, fy + 43 + workBob, 36f, 4f); shapeRenderer.rect(fx + 12, fy + 47 + workBob, 24f, 6f)
                shapeRenderer.color = Color(0.7f * d, 0.6f * d, 0.25f * d, 1f); shapeRenderer.rect(fx + 14, fy + 47 + workBob, 20f, 2f)
            }
            2 -> {
                shapeRenderer.color = Color(0.2f * d, 0.35f * d, 0.65f * d, 1f)
                shapeRenderer.rect(fx + 10, fy + 42 + workBob, 28f, 8f); shapeRenderer.rect(fx + 12, fy + 40 + workBob, 24f, 4f)
                shapeRenderer.color = Color(0.2f * d, 0.12f * d, 0.05f * d, 1f)
                shapeRenderer.rect(fx + 10, fy + 38 + workBob, 5f, 6f); shapeRenderer.rect(fx + 33, fy + 38 + workBob, 5f, 6f)
            }
            3 -> {
                shapeRenderer.color = Color(0.9f * d, 0.75f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 4, fy + 44 + workBob, 40f, 3f); shapeRenderer.rect(fx + 10, fy + 47 + workBob, 28f, 7f)
                shapeRenderer.color = Color(0.7f * d, 0.55f * d, 0.05f * d, 1f); shapeRenderer.rect(fx + 12, fy + 47 + workBob, 24f, 2f)
            }
        }

        if (isWorking) {
            val toolSwing = MathUtils.sin(animTimer * 8f) * 4f
            when (farmer.action) {
                "till" -> {
                    shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.2f * d, 1f); shapeRenderer.rect(fx + 40, fy + 20 + toolSwing, 4f, 18f)
                    shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.6f * d, 1f); shapeRenderer.rect(fx + 38, fy + 36 + toolSwing, 8f, 4f)
                }
                "water" -> {
                    shapeRenderer.color = Color(0.3f * d, 0.45f * d, 0.7f * d, 1f); shapeRenderer.rect(fx + 39, fy + 18 + toolSwing, 8f, 10f)
                    shapeRenderer.rect(fx + 46, fy + 22 + toolSwing, 4f, 3f)
                    if (MathUtils.sin(animTimer * 12f) > 0) {
                        shapeRenderer.color = Color(0.4f * d, 0.6f * d, 0.9f * d, 0.7f)
                        shapeRenderer.rect(fx + 47, fy + 16 + toolSwing, 2f, 3f); shapeRenderer.rect(fx + 50, fy + 14 + toolSwing, 2f, 3f)
                    }
                }
                "plant" -> {
                    shapeRenderer.color = Color(0.6f * d, 0.45f * d, 0.15f * d, 1f); shapeRenderer.rect(fx + 40, fy + 20 + toolSwing, 5f, 5f)
                    if (MathUtils.sin(animTimer * 10f) > 0) {
                        shapeRenderer.color = Color(0.5f * d, 0.4f * d, 0.1f * d, 0.6f)
                        shapeRenderer.rect(fx + 41, fy + 14 + toolSwing, 2f, 2f); shapeRenderer.rect(fx + 44, fy + 12 + toolSwing, 2f, 2f)
                    }
                }
                "harvest" -> {
                    shapeRenderer.color = Color(0.65f * d, 0.5f * d, 0.25f * d, 1f)
                    shapeRenderer.rect(fx + 38, fy + 16 + toolSwing, 10f, 8f); shapeRenderer.rect(fx + 36, fy + 24 + toolSwing, 14f, 2f)
                    shapeRenderer.color = Color(0.3f * d, 0.7f * d, 0.2f * d, 1f); shapeRenderer.rect(fx + 39, fy + 24 + toolSwing, 4f, 4f)
                    shapeRenderer.color = Color(0.9f * d, 0.3f * d, 0.2f * d, 1f); shapeRenderer.rect(fx + 44, fy + 24 + toolSwing, 3f, 3f)
                }
            }

            val pTime = animTimer * 6f
            when (farmer.action) {
                "till" -> {
                    shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 0.6f)
                    for (i in 0..2) { shapeRenderer.rect(fx + 20 + MathUtils.sin(pTime + i * 2f) * 8f, fy + 4 + MathUtils.cos(pTime + i * 1.5f).coerceAtLeast(0f) * 6f, 3f, 3f) }
                }
                "water" -> {
                    shapeRenderer.color = Color(0.3f * d, 0.5f * d, 0.9f * d, 0.5f)
                    for (i in 0..2) { shapeRenderer.rect(fx + 18 + MathUtils.sin(pTime + i * 2.5f) * 10f, fy + 2 + MathUtils.cos(pTime + i * 2f).coerceAtLeast(0f) * 5f, 2f, 2f) }
                }
            }
        }
        shapeRenderer.end()
    }

    // ===== HERDER RENDERING =====

    fun renderHerders(daylight: Float) {
        if (world.herders.isEmpty()) return
        for (herder in world.herders) renderSingleHerder(herder, daylight)
    }

    private fun renderSingleHerder(herder: HiredWorker, daylight: Float) {
        val d = daylight; val hx = herder.renderX * tileSize; val hy = herder.renderY * tileSize
        val walkCycle = MathUtils.sin(animTimer * 5f + herder.renderX * 3f)
        val isWorking = herder.action in listOf("collect", "feed")
        val workBob = if (isWorking) MathUtils.sin(animTimer * 6f) * 2f else 0f
        val level = herder.level

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f); shapeRenderer.rect(hx + 8, hy - 1, 32f, 5f)

        val legOff = if (herder.action == "walk") walkCycle * 2.5f else 0f
        shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.2f * d, 1f)
        shapeRenderer.rect(hx + 14, hy + legOff.coerceAtLeast(0f), 7f, 12f)
        shapeRenderer.rect(hx + 27, hy + (-legOff).coerceAtLeast(0f), 7f, 12f)

        shapeRenderer.color = Color(0.3f * d, 0.18f * d, 0.08f * d, 1f)
        shapeRenderer.rect(hx + 13, hy + legOff.coerceAtLeast(0f), 9f, 4f)
        shapeRenderer.rect(hx + 26, hy + (-legOff).coerceAtLeast(0f), 9f, 4f)

        shapeRenderer.color = when (level) {
            1 -> Color(0.7f * d, 0.45f * d, 0.15f * d, 1f)
            2 -> Color(0.55f * d, 0.25f * d, 0.6f * d, 1f)
            3 -> Color(0.75f * d, 0.65f * d, 0.1f * d, 1f)
            else -> Color(0.5f * d, 0.5f * d, 0.5f * d, 1f)
        }
        shapeRenderer.rect(hx + 10, hy + 12 + workBob, 28f, 18f)

        val skinR = 0.85f * d; val skinG = 0.65f * d; val skinB = 0.45f * d
        shapeRenderer.color = Color(skinR, skinG, skinB, 1f)
        if (isWorking) {
            val armSwing = MathUtils.sin(animTimer * 6f) * 5f
            shapeRenderer.rect(hx + 4, hy + 16 + armSwing, 7f, 12f); shapeRenderer.rect(hx + 37, hy + 16 - armSwing, 7f, 12f)
        } else {
            shapeRenderer.rect(hx + 4, hy + 13 + legOff, 7f, 12f); shapeRenderer.rect(hx + 37, hy + 13 - legOff, 7f, 12f)
        }

        shapeRenderer.color = Color(0.9f * d, 0.72f * d, 0.5f * d, 1f)
        shapeRenderer.rect(hx + 13, hy + 30 + workBob, 22f, 14f)
        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(hx + 16, hy + 36 + workBob, 6f, 4f); shapeRenderer.rect(hx + 26, hy + 36 + workBob, 6f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.15f * d, 1f)
        shapeRenderer.rect(hx + 18, hy + 36 + workBob, 3f, 3f); shapeRenderer.rect(hx + 28, hy + 36 + workBob, 3f, 3f)
        shapeRenderer.color = Color(0.75f * d, 0.4f * d, 0.3f * d, 1f)
        shapeRenderer.rect(hx + 19, hy + 32 + workBob, 10f, 2f)

        when (level) {
            1 -> { shapeRenderer.color = Color(0.9f * d, 0.5f * d, 0.1f * d, 1f); shapeRenderer.rect(hx + 11, hy + 42 + workBob, 26f, 5f); shapeRenderer.rect(hx + 34, hy + 40 + workBob, 8f, 4f) }
            2 -> { shapeRenderer.color = Color(0.5f * d, 0.2f * d, 0.6f * d, 1f); shapeRenderer.rect(hx + 8, hy + 43 + workBob, 32f, 4f); shapeRenderer.rect(hx + 12, hy + 47 + workBob, 24f, 6f) }
            3 -> {
                shapeRenderer.color = Color(1f * d, 0.85f * d, 0.1f * d, 1f)
                shapeRenderer.rect(hx + 12, hy + 44 + workBob, 24f, 4f)
                shapeRenderer.rect(hx + 14, hy + 48 + workBob, 4f, 5f); shapeRenderer.rect(hx + 22, hy + 48 + workBob, 4f, 7f); shapeRenderer.rect(hx + 30, hy + 48 + workBob, 4f, 5f)
            }
        }

        if (isWorking) {
            val swing = MathUtils.sin(animTimer * 6f) * 3f
            when (herder.action) {
                "collect" -> {
                    shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.55f * d, 1f); shapeRenderer.rect(hx + 39, hy + 16 + swing, 8f, 10f)
                    shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.65f * d, 1f); shapeRenderer.rect(hx + 38, hy + 26 + swing, 10f, 2f)
                }
                "feed" -> {
                    shapeRenderer.color = Color(0.65f * d, 0.55f * d, 0.3f * d, 1f); shapeRenderer.rect(hx + 39, hy + 16 + swing, 9f, 12f)
                    if (MathUtils.sin(animTimer * 10f) > 0) {
                        shapeRenderer.color = Color(0.8f * d, 0.7f * d, 0.2f * d, 0.6f)
                        shapeRenderer.rect(hx + 40, hy + 12 + swing, 2f, 2f); shapeRenderer.rect(hx + 44, hy + 10 + swing, 2f, 2f)
                    }
                }
            }
        }
        shapeRenderer.end()
    }

    // ===== PLAYER RENDERING =====

    fun renderPlayer(daylight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val px = world.player.renderX * tileSize; val py = world.player.renderY * tileSize; val d = daylight
        val isMoving = world.player.renderX != world.player.tileX.toFloat() || world.player.renderY != world.player.tileY.toFloat()
        val walkCycle = if (isMoving) MathUtils.sin(animTimer * 12f) else 0f

        shapeRenderer.color = Color(0f, 0f, 0f, 0.2f); shapeRenderer.rect(px + 8, py - 1, 32f, 5f)

        shapeRenderer.color = Color(0.3f * d, 0.25f * d, 0.6f * d, 1f)
        when (world.player.facing) {
            Direction.LEFT, Direction.RIGHT -> {
                val legOff = walkCycle * 4f
                shapeRenderer.rect(px + 14, py + legOff.coerceAtLeast(0f), 7f, 14f); shapeRenderer.rect(px + 27, py + (-legOff).coerceAtLeast(0f), 7f, 14f)
            }
            else -> {
                val legOff = walkCycle * 3f
                shapeRenderer.rect(px + 13, py + legOff.coerceAtLeast(0f), 7f, 14f); shapeRenderer.rect(px + 28, py + (-legOff).coerceAtLeast(0f), 7f, 14f)
            }
        }

        shapeRenderer.color = Color(0.4f * d, 0.2f * d, 0.1f * d, 1f)
        when (world.player.facing) {
            Direction.LEFT, Direction.RIGHT -> {
                val legOff = walkCycle * 4f
                shapeRenderer.rect(px + 13, py + legOff.coerceAtLeast(0f), 9f, 4f); shapeRenderer.rect(px + 26, py + (-legOff).coerceAtLeast(0f), 9f, 4f)
            }
            else -> {
                val legOff = walkCycle * 3f
                shapeRenderer.rect(px + 12, py + legOff.coerceAtLeast(0f), 9f, 4f); shapeRenderer.rect(px + 27, py + (-legOff).coerceAtLeast(0f), 9f, 4f)
            }
        }

        shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.3f * d, 1f); shapeRenderer.rect(px + 10, py + 14, 28f, 16f)
        shapeRenderer.color = Color(0.25f * d, 0.5f * d, 0.7f * d, 1f)
        shapeRenderer.rect(px + 14, py + 15, 4f, 14f); shapeRenderer.rect(px + 30, py + 15, 4f, 14f)

        val armSwing = walkCycle * 3f
        shapeRenderer.color = Color(0.9f * d, 0.75f * d, 0.6f * d, 1f)
        when (world.player.facing) {
            Direction.UP -> { shapeRenderer.rect(px + 4, py + 14 + armSwing, 7f, 14f); shapeRenderer.rect(px + 37, py + 14 - armSwing, 7f, 14f); renderToolInHand(px + 39, py + 14, d) }
            Direction.DOWN -> { shapeRenderer.rect(px + 4, py + 14 - armSwing, 7f, 14f); shapeRenderer.rect(px + 37, py + 14 + armSwing, 7f, 14f); renderToolInHand(px + 39, py + 12, d) }
            Direction.LEFT -> { shapeRenderer.rect(px + 5, py + 14, 7f, 14f); renderToolInHand(px + 1, py + 12, d) }
            Direction.RIGHT -> { shapeRenderer.rect(px + 36, py + 14, 7f, 14f); renderToolInHand(px + 39, py + 12, d) }
        }

        shapeRenderer.color = Color(0.95f * d, 0.8f * d, 0.65f * d, 1f); shapeRenderer.rect(px + 13, py + 30, 22f, 16f)
        shapeRenderer.color = Color(0.35f * d, 0.2f * d, 0.1f * d, 1f)
        when (world.player.facing) {
            Direction.UP -> { shapeRenderer.rect(px + 12, py + 40, 24f, 8f); shapeRenderer.rect(px + 12, py + 32, 24f, 6f) }
            Direction.DOWN -> { shapeRenderer.rect(px + 12, py + 42, 24f, 6f); shapeRenderer.rect(px + 13, py + 40, 22f, 4f) }
            Direction.LEFT -> { shapeRenderer.rect(px + 12, py + 40, 24f, 8f); shapeRenderer.rect(px + 12, py + 30, 6f, 14f) }
            Direction.RIGHT -> { shapeRenderer.rect(px + 12, py + 40, 24f, 8f); shapeRenderer.rect(px + 30, py + 30, 6f, 14f) }
        }

        when (world.player.facing) {
            Direction.DOWN -> {
                shapeRenderer.color = Color.WHITE; shapeRenderer.rect(px + 16, py + 36, 6f, 4f); shapeRenderer.rect(px + 26, py + 36, 6f, 4f)
                shapeRenderer.color = Color(0.15f * d, 0.15f * d, 0.2f * d, 1f); shapeRenderer.rect(px + 18, py + 36, 3f, 3f); shapeRenderer.rect(px + 28, py + 36, 3f, 3f)
                shapeRenderer.color = Color(0.8f * d, 0.4f * d, 0.35f * d, 1f); shapeRenderer.rect(px + 20, py + 32, 8f, 2f)
            }
            Direction.LEFT -> {
                shapeRenderer.color = Color.WHITE; shapeRenderer.rect(px + 14, py + 36, 6f, 4f)
                shapeRenderer.color = Color(0.15f * d, 0.15f * d, 0.2f * d, 1f); shapeRenderer.rect(px + 14, py + 36, 3f, 3f)
                shapeRenderer.color = Color(0.8f * d, 0.4f * d, 0.35f * d, 1f); shapeRenderer.rect(px + 13, py + 32, 6f, 2f)
            }
            Direction.RIGHT -> {
                shapeRenderer.color = Color.WHITE; shapeRenderer.rect(px + 28, py + 36, 6f, 4f)
                shapeRenderer.color = Color(0.15f * d, 0.15f * d, 0.2f * d, 1f); shapeRenderer.rect(px + 31, py + 36, 3f, 3f)
                shapeRenderer.color = Color(0.8f * d, 0.4f * d, 0.35f * d, 1f); shapeRenderer.rect(px + 29, py + 32, 6f, 2f)
            }
            Direction.UP -> {}
        }

        shapeRenderer.color = Color(0.9f * d, 0.7f * d, 0.2f * d, 1f)
        shapeRenderer.rect(px + 8, py + 44, 32f, 4f); shapeRenderer.rect(px + 13, py + 47, 22f, 5f)

        if (world.player.isFishing) renderFishingLine(px, py, d)
        shapeRenderer.end()
    }

    private fun renderFishingLine(px: Float, py: Float, d: Float) {
        val (fx, fy) = world.player.getFacingTile()
        val targetX = fx * tileSize + tileSize / 2; val targetY = fy * tileSize + tileSize / 2
        shapeRenderer.color = Color(0.7f * d, 0.7f * d, 0.7f * d, 0.8f)
        val handX = px + 24f; val handY = py + 28f
        val midX = (handX + targetX) / 2; val midY = (handY + targetY) / 2 + 10f
        shapeRenderer.rect(handX, handY, 2f, midY - handY)
        shapeRenderer.rect(midX - 1, midY - 2f, targetX - midX + 2f, 2f)
        shapeRenderer.rect(targetX, targetY, 2f, midY - targetY)

        val bobble = MathUtils.sin(animTimer * 3f) * 2f
        if (world.player.hasBite) {
            shapeRenderer.color = Color(1f * d, 0.2f * d, 0.1f * d, 1f)
            shapeRenderer.rect(targetX - 3f, targetY - 4f + MathUtils.sin(animTimer * 10f) * 4f, 6f, 8f)
        } else {
            shapeRenderer.color = Color(1f * d, 0.3f * d, 0.1f * d, 1f)
            shapeRenderer.rect(targetX - 2f, targetY - 2f + bobble, 5f, 6f)
            shapeRenderer.color = Color(0.9f * d, 0.9f * d, 0.9f * d, 1f)
            shapeRenderer.rect(targetX - 1f, targetY + 3f + bobble, 3f, 3f)
        }
    }

    private fun renderToolInHand(x: Float, y: Float, d: Float) {
        when (world.player.currentTool) {
            Tool.HOE -> {
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f); shapeRenderer.rect(x, y, 3f, 20f)
                shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.5f * d, 1f); shapeRenderer.rect(x - 3, y + 17, 9f, 4f)
            }
            Tool.WATERING_CAN -> {
                shapeRenderer.color = Color(0.3f * d, 0.4f * d, 0.8f * d, 1f); shapeRenderer.rect(x, y, 9f, 8f); shapeRenderer.rect(x + 7, y + 5, 5f, 3f)
            }
            Tool.AXE -> {
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f); shapeRenderer.rect(x + 1, y, 3f, 20f)
                shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.65f * d, 1f); shapeRenderer.rect(x - 2, y + 16, 9f, 5f)
            }
            Tool.PICKAXE -> {
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f); shapeRenderer.rect(x + 1, y, 3f, 20f)
                shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.65f * d, 1f); shapeRenderer.rect(x - 3, y + 17, 12f, 4f)
            }
            Tool.SEED_BAG -> {
                shapeRenderer.color = Color(0.6f * d, 0.5f * d, 0.3f * d, 1f); shapeRenderer.rect(x, y + 2, 7f, 10f)
                shapeRenderer.color = Color(0.3f * d, 0.7f * d, 0.2f * d, 1f); shapeRenderer.rect(x + 1, y + 10, 5f, 4f)
            }
            Tool.FISHING_ROD -> {
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f); shapeRenderer.rect(x, y, 3f, 24f)
                shapeRenderer.color = Color(0.55f * d, 0.45f * d, 0.25f * d, 1f); shapeRenderer.rect(x + 1, y + 22f, 2f, 10f)
                shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.55f * d, 1f); shapeRenderer.rect(x - 1, y + 8, 5f, 4f)
            }
            Tool.HAND -> {}
        }
    }
}
