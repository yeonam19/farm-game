package com.farmgame.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.farmgame.data.*
import com.farmgame.world.GameWorld
import com.farmgame.world.TileType

/**
 * 월드 렌더링 — 타일, 작물, 동물, NPC, 자동낚시 장치
 */
class WorldRenderer(
    private val shapeRenderer: ShapeRenderer,
    private val world: GameWorld,
    private val tileSize: Float
) {
    var animTimer: Float = 0f

    // ===== TILE RENDERING =====

    fun renderTiles(daylight: Float, camX: Float, camY: Float, viewW: Float, viewH: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        val minTileX = ((camX - viewW) / tileSize).toInt().coerceAtLeast(0)
        val maxTileX = ((camX + viewW) / tileSize).toInt().coerceAtMost(world.mapWidth - 1)
        val minTileY = ((camY - viewH) / tileSize).toInt().coerceAtLeast(0)
        val maxTileY = ((camY + viewH) / tileSize).toInt().coerceAtMost(world.mapHeight - 1)

        val seasonTint = world.timeSystem.season.tint

        for (x in minTileX..maxTileX) {
            for (y in minTileY..maxTileY) {
                val tile = world.tiles[x][y]
                val tx = x * tileSize
                val ty = y * tileSize
                val c = tile.type.color
                val dl = daylight

                shapeRenderer.color = Color(c.r * dl * seasonTint.r, c.g * dl * seasonTint.g, c.b * dl * seasonTint.b, 1f)
                shapeRenderer.rect(tx, ty, tileSize, tileSize)

                when (tile.type) {
                    TileType.WATER -> renderWater(tx, ty, dl, x, y)
                    TileType.TREE -> renderTree(tx, ty, dl)
                    TileType.FENCE -> renderFence(tx, ty, dl, x, y)
                    TileType.ROCK -> renderRock(tx, ty, dl, x, y)
                    TileType.MARKET -> renderMarket(tx, ty, dl)
                    TileType.HOME -> renderHome(tx, ty, dl)
                    TileType.SAND -> renderSand(tx, ty, dl)
                    TileType.FARMLAND, TileType.FARMLAND_WET -> renderFarmland(tx, ty, dl, tile.type == TileType.FARMLAND_WET)
                    TileType.GRASS -> renderGrassDetail(tx, ty, dl, x, y)
                    TileType.PATH -> renderPath(tx, ty, dl)
                    else -> {}
                }
            }
        }
        shapeRenderer.end()
    }

    private fun renderWater(tx: Float, ty: Float, d: Float, x: Int, y: Int) {
        val wave1 = MathUtils.sin((x + y).toFloat() * 0.8f + animTimer * 1.5f) * 0.08f
        val wave2 = MathUtils.sin((x - y).toFloat() * 1.2f + animTimer * 2f) * 0.05f
        shapeRenderer.color = Color((0.15f + wave1) * d, (0.35f + wave2) * d, (0.7f + wave1) * d, 1f)
        shapeRenderer.rect(tx, ty, tileSize, tileSize)
        val shimmer = MathUtils.sin(x.toFloat() * 2f + animTimer * 3f) * 0.5f + 0.5f
        shapeRenderer.color = Color(0.4f * d, 0.6f * d, 0.9f * d, 0.15f * shimmer)
        shapeRenderer.rect(tx + 4f + wave1 * 20f, ty + tileSize * 0.3f, tileSize * 0.4f, 3f)
        shapeRenderer.rect(tx + 16f + wave2 * 15f, ty + tileSize * 0.65f, tileSize * 0.3f, 2f)
    }

    private fun renderTree(tx: Float, ty: Float, d: Float) {
        val isAutumn = world.timeSystem.season == Season.AUTUMN
        val isWinter = world.timeSystem.season == Season.WINTER

        shapeRenderer.color = Color(0.35f * d, 0.22f * d, 0.08f * d, 1f)
        shapeRenderer.rect(tx + 18f, ty, 12f, 22f)
        shapeRenderer.color = Color(0.25f * d, 0.15f * d, 0.05f * d, 1f)
        shapeRenderer.rect(tx + 21f, ty + 2f, 3f, 18f)
        shapeRenderer.color = Color(0.42f * d, 0.28f * d, 0.12f * d, 1f)
        shapeRenderer.rect(tx + 26f, ty + 3f, 2f, 14f)
        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.07f * d, 1f)
        shapeRenderer.rect(tx + 14f, ty, 6f, 4f)
        shapeRenderer.rect(tx + 28f, ty, 6f, 3f)

        if (isWinter) {
            shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.1f * d, 1f)
            shapeRenderer.rect(tx + 8f, ty + 30f, 32f, 4f)
            shapeRenderer.rect(tx + 12f, ty + 34f, 6f, 8f)
            shapeRenderer.rect(tx + 28f, ty + 34f, 6f, 6f)
            shapeRenderer.color = Color(0.9f * d, 0.92f * d, 0.95f * d, 0.7f)
            shapeRenderer.rect(tx + 8f, ty + 33f, 32f, 3f)
        } else {
            val greenBase = if (isAutumn) 0.25f else 0.5f
            val redBase = if (isAutumn) 0.65f else 0.12f
            val greenBright = if (isAutumn) 0.3f else 0.6f
            val redBright = if (isAutumn) 0.75f else 0.15f

            shapeRenderer.color = Color(redBase * d, greenBase * d, 0.08f * d, 1f)
            shapeRenderer.rect(tx + 4f, ty + 18f, 40f, 16f)
            shapeRenderer.color = Color(redBright * d, greenBright * d, 0.12f * d, 1f)
            shapeRenderer.rect(tx + 8f, ty + 22f, 32f, 16f)
            shapeRenderer.color = Color((redBright + 0.05f) * d, (greenBright + 0.05f) * d, 0.15f * d, 1f)
            shapeRenderer.rect(tx + 12f, ty + 34f, 24f, 10f)
            shapeRenderer.color = Color((redBright + 0.1f) * d, (greenBright + 0.1f) * d, 0.18f * d, 0.6f)
            shapeRenderer.rect(tx + 14f, ty + 36f, 12f, 6f)

            if (isAutumn) {
                shapeRenderer.color = Color(0.85f * d, 0.4f * d, 0.1f * d, 0.7f)
                shapeRenderer.rect(tx + 10f, ty + 28f, 4f, 4f)
                shapeRenderer.rect(tx + 30f, ty + 24f, 4f, 4f)
                shapeRenderer.color = Color(0.9f * d, 0.2f * d, 0.1f * d, 0.5f)
                shapeRenderer.rect(tx + 20f, ty + 32f, 3f, 3f)
            }
        }
    }

    private fun renderFence(tx: Float, ty: Float, d: Float, x: Int, y: Int) {
        val hasLeft = world.getTile(x - 1, y)?.type == TileType.FENCE
        val hasRight = world.getTile(x + 1, y)?.type == TileType.FENCE
        val hasUp = world.getTile(x, y + 1)?.type == TileType.FENCE
        val hasDown = world.getTile(x, y - 1)?.type == TileType.FENCE

        shapeRenderer.color = Color(0.35f * d, 0.55f * d, 0.2f * d, 1f)
        shapeRenderer.rect(tx, ty, tileSize, tileSize)

        val woodDark = Color(0.4f * d, 0.28f * d, 0.12f * d, 1f)
        val woodLight = Color(0.55f * d, 0.38f * d, 0.18f * d, 1f)
        val woodHighlight = Color(0.62f * d, 0.45f * d, 0.22f * d, 1f)

        shapeRenderer.color = woodDark
        shapeRenderer.rect(tx + 20f, ty + 2f, 8f, 44f)
        shapeRenderer.color = woodLight
        shapeRenderer.rect(tx + 22f, ty + 4f, 4f, 40f)
        shapeRenderer.color = woodHighlight
        shapeRenderer.rect(tx + 19f, ty + 42f, 10f, 4f)

        if (hasLeft || hasRight) {
            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx, ty + 30f, tileSize, 5f)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 2f, ty + 31f, tileSize - 4f, 3f)
            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx, ty + 14f, tileSize, 5f)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 2f, ty + 15f, tileSize - 4f, 3f)
        }

        if (hasUp || hasDown) {
            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx + 12f, ty, 5f, tileSize)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 13f, ty + 2f, 3f, tileSize - 4f)
            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx + 31f, ty, 5f, tileSize)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 32f, ty + 2f, 3f, tileSize - 4f)
        }

        shapeRenderer.color = Color(0.35f * d, 0.35f * d, 0.38f * d, 1f)
        if (hasLeft || hasRight) {
            shapeRenderer.rect(tx + 23f, ty + 31f, 2f, 2f)
            shapeRenderer.rect(tx + 23f, ty + 15f, 2f, 2f)
        }
    }

    private fun renderRock(tx: Float, ty: Float, d: Float, x: Int, y: Int) {
        val hash = ((x * 7919 + y * 104729) % 10) / 10f
        shapeRenderer.color = Color((0.5f + hash * 0.1f) * d, (0.48f + hash * 0.08f) * d, (0.45f + hash * 0.05f) * d, 1f)
        shapeRenderer.rect(tx + 6f, ty + 4f, 36f, 28f)
        shapeRenderer.rect(tx + 10f, ty + 28f, 28f, 10f)
        shapeRenderer.color = Color(0.65f * d, 0.62f * d, 0.58f * d, 0.6f)
        shapeRenderer.rect(tx + 14f, ty + 22f, 16f, 8f)
        shapeRenderer.color = Color(0.3f * d, 0.28f * d, 0.25f * d, 0.7f)
        shapeRenderer.rect(tx + 20f, ty + 12f, 12f, 3f)
        shapeRenderer.color = Color(0.25f * d, 0.45f * d, 0.15f * d, 0.4f)
        shapeRenderer.rect(tx + 8f, ty + 4f, 10f, 6f)
    }

    private fun renderMarket(tx: Float, ty: Float, d: Float) {
        shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f)
        shapeRenderer.rect(tx + 2f, ty + 2f, 44f, 24f)
        shapeRenderer.color = Color(0.6f * d, 0.45f * d, 0.2f * d, 1f)
        shapeRenderer.rect(tx + 2f, ty + 22f, 44f, 6f)
        val pulse = (MathUtils.sin(animTimer * 2f) + 1) / 2 * 0.15f
        shapeRenderer.color = Color((0.8f + pulse) * d, (0.2f + pulse) * d, 0.1f * d, 1f)
        shapeRenderer.rect(tx, ty + 30f, tileSize, 10f)
        shapeRenderer.color = Color(0.9f * d, 0.85f * d, 0.4f * d, 1f)
        shapeRenderer.rect(tx + 4f, ty + 32f, 8f, 6f)
        shapeRenderer.rect(tx + 20f, ty + 32f, 8f, 6f)
        shapeRenderer.rect(tx + 36f, ty + 32f, 8f, 6f)
        shapeRenderer.color = Color(1f * d, 0.85f * d, 0.2f * d, 0.6f + pulse)
        shapeRenderer.rect(tx + 18f, ty + 10f, 12f, 12f)
    }

    private fun renderHome(tx: Float, ty: Float, d: Float) {
        shapeRenderer.color = Color(0.75f * d, 0.55f * d, 0.35f * d, 1f)
        shapeRenderer.rect(tx + 2f, ty + 2f, 44f, 28f)
        shapeRenderer.color = Color(0.6f * d, 0.2f * d, 0.15f * d, 1f)
        shapeRenderer.rect(tx, ty + 30f, tileSize, 14f)
        shapeRenderer.color = Color(0.5f * d, 0.15f * d, 0.1f * d, 1f)
        shapeRenderer.rect(tx + 8f, ty + 40f, 32f, 6f)
        shapeRenderer.color = Color(0.4f * d, 0.25f * d, 0.12f * d, 1f)
        shapeRenderer.rect(tx + 18f, ty + 2f, 12f, 18f)
        shapeRenderer.color = Color(0.8f * d, 0.7f * d, 0.2f * d, 1f)
        shapeRenderer.rect(tx + 27f, ty + 10f, 2f, 2f)
        if (world.timeSystem.isNight) {
            shapeRenderer.color = Color(1f * d, 0.9f * d, 0.5f * d, 0.6f)
        } else {
            shapeRenderer.color = Color(0.5f * d, 0.7f * d, 0.9f * d, 0.7f)
        }
        shapeRenderer.rect(tx + 6f, ty + 14f, 10f, 10f)
        shapeRenderer.rect(tx + 32f, ty + 14f, 10f, 10f)
        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.1f * d, 1f)
        shapeRenderer.rect(tx + 10f, ty + 14f, 2f, 10f)
        shapeRenderer.rect(tx + 36f, ty + 14f, 2f, 10f)
    }

    private fun renderSand(tx: Float, ty: Float, d: Float) {
        shapeRenderer.color = Color(0.82f * d, 0.72f * d, 0.5f * d, 0.4f)
        shapeRenderer.rect(tx + 8f, ty + 6f, 3f, 2f)
        shapeRenderer.rect(tx + 28f, ty + 18f, 4f, 2f)
        shapeRenderer.rect(tx + 16f, ty + 34f, 3f, 2f)
        shapeRenderer.color = Color(0.9f * d, 0.88f * d, 0.8f * d, 0.3f)
        shapeRenderer.rect(tx + 36f, ty + 10f, 4f, 3f)
    }

    private fun renderFarmland(tx: Float, ty: Float, d: Float, isWet: Boolean) {
        val furrowColor = if (isWet) Color(0.25f * d, 0.18f * d, 0.1f * d, 0.5f)
        else Color(0.35f * d, 0.25f * d, 0.12f * d, 0.4f)
        shapeRenderer.color = furrowColor
        for (i in 0..3) {
            shapeRenderer.rect(tx + 2f, ty + 4f + i * 12f, tileSize - 4f, 2f)
        }
        if (isWet) {
            shapeRenderer.color = Color(0.3f * d, 0.4f * d, 0.6f * d, 0.15f)
            shapeRenderer.rect(tx + 4f, ty + 4f, tileSize - 8f, tileSize - 8f)
        }
    }

    private fun renderGrassDetail(tx: Float, ty: Float, d: Float, x: Int, y: Int) {
        val hash = ((x * 3571 + y * 7823) % 100)
        if (hash < 30) {
            val grassGreen = if (world.timeSystem.season == Season.WINTER) 0.35f else 0.55f
            shapeRenderer.color = Color(0.18f * d, grassGreen * d, 0.12f * d, 0.5f)
            shapeRenderer.rect(tx + 8f + (hash % 5) * 2f, ty + 12f, 2f, 6f)
            shapeRenderer.rect(tx + 20f + (hash % 3) * 3f, ty + 28f, 2f, 5f)
            shapeRenderer.rect(tx + 34f, ty + 8f + (hash % 4) * 2f, 2f, 7f)
        }
        if (hash < 10) {
            val season = world.timeSystem.season
            if (season == Season.SPRING || season == Season.SUMMER) {
                shapeRenderer.color = Color(0.9f * d, 0.7f * d, 0.2f * d, 0.6f)
                shapeRenderer.rect(tx + 14f + (hash % 7) * 2f, ty + 20f, 3f, 3f)
            }
        }
    }

    private fun renderPath(tx: Float, ty: Float, d: Float) {
        shapeRenderer.color = Color(0.52f * d, 0.45f * d, 0.35f * d, 0.4f)
        shapeRenderer.rect(tx + 4f, ty + 4f, 14f, 12f)
        shapeRenderer.rect(tx + 22f, ty + 6f, 16f, 10f)
        shapeRenderer.rect(tx + 8f, ty + 24f, 18f, 12f)
        shapeRenderer.rect(tx + 30f, ty + 22f, 12f, 14f)
        shapeRenderer.color = Color(0.6f * d, 0.55f * d, 0.42f * d, 0.25f)
        shapeRenderer.rect(tx + 6f, ty + 8f, 8f, 4f)
        shapeRenderer.rect(tx + 10f, ty + 28f, 10f, 4f)
    }

    // ===== CROP RENDERING =====

    fun renderCrops(daylight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (x in world.farmMinX..world.farmMaxX) {
            for (y in world.farmMinY..world.farmMaxY) {
                if (x >= world.mapWidth || y >= world.mapHeight) continue
                val crop = world.tiles[x][y].crop ?: continue
                val tx = x * tileSize
                val ty = y * tileSize
                val d = daylight
                val progress = crop.sizeRatio

                if (crop.isDead) {
                    shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.1f * d, 0.7f)
                    shapeRenderer.rect(tx + 12f, ty + 8f, 24f, 4f)
                    shapeRenderer.rect(tx + 18f, ty + 4f, 12f, 8f)
                    continue
                }

                if (crop.isReady) {
                    renderFullyGrownCrop(tx, ty, d, crop.type)
                } else {
                    renderGrowingCrop(tx, ty, d, crop.type, progress)
                }

                if (crop.isReady) {
                    val pulse = (MathUtils.sin(animTimer * 4f) + 1) / 2
                    shapeRenderer.color = Color(1f, 1f, 0.5f, 0.3f * pulse)
                    shapeRenderer.rect(tx + 6f, ty + 6f, tileSize - 12f, tileSize - 12f)
                }
            }
        }
        shapeRenderer.end()
    }

    private fun renderGrowingCrop(tx: Float, ty: Float, d: Float, type: CropType, progress: Float) {
        val height = 4f + progress * 24f
        val width = 2f + progress * 6f
        shapeRenderer.color = Color(0.2f * d, 0.5f * d, 0.15f * d, 1f)
        shapeRenderer.rect(tx + 22f, ty + 6f, 4f, height)
        if (progress > 0.3f) {
            shapeRenderer.color = Color(0.25f * d, 0.6f * d, 0.18f * d, 1f)
            shapeRenderer.rect(tx + 16f, ty + 6f + height * 0.5f, 8f, width)
            shapeRenderer.rect(tx + 24f, ty + 6f + height * 0.6f, 8f, width * 0.8f)
        }
        if (progress > 0.7f) {
            val c = type.grownColor
            shapeRenderer.color = Color(c.r * 0.7f * d, c.g * 0.7f * d, c.b * 0.7f * d, 0.6f)
            shapeRenderer.rect(tx + 18f, ty + 6f + height - 4f, 6f, 6f)
        }
    }

    private fun renderFullyGrownCrop(tx: Float, ty: Float, d: Float, type: CropType) {
        when (type) {
            CropType.STRAWBERRY -> {
                shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 16f, 32f, 14f)
                shapeRenderer.rect(tx + 12f, ty + 28f, 24f, 8f)
                shapeRenderer.color = Color(0.9f * d, 0.15f * d, 0.1f * d, 1f)
                shapeRenderer.rect(tx + 10f, ty + 6f, 8f, 10f)
                shapeRenderer.rect(tx + 22f, ty + 8f, 7f, 9f)
                shapeRenderer.rect(tx + 32f, ty + 6f, 8f, 10f)
                shapeRenderer.color = Color(0.95f * d, 0.8f * d, 0.2f * d, 0.7f)
                shapeRenderer.rect(tx + 12f, ty + 10f, 2f, 2f)
                shapeRenderer.rect(tx + 24f, ty + 11f, 2f, 2f)
                shapeRenderer.rect(tx + 35f, ty + 9f, 2f, 2f)
            }
            CropType.TOMATO -> {
                shapeRenderer.color = Color(0.2f * d, 0.5f * d, 0.12f * d, 1f)
                shapeRenderer.rect(tx + 22f, ty + 4f, 4f, 36f)
                shapeRenderer.color = Color(0.22f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 10f, ty + 24f, 28f, 10f)
                shapeRenderer.rect(tx + 14f, ty + 32f, 20f, 8f)
                shapeRenderer.color = Color(0.9f * d, 0.12f * d, 0.08f * d, 1f)
                shapeRenderer.rect(tx + 10f, ty + 8f, 12f, 12f)
                shapeRenderer.rect(tx + 28f, ty + 10f, 10f, 10f)
                shapeRenderer.color = Color(1f * d, 0.3f * d, 0.2f * d, 0.5f)
                shapeRenderer.rect(tx + 12f, ty + 14f, 4f, 4f)
                shapeRenderer.color = Color(0.15f * d, 0.45f * d, 0.1f * d, 1f)
                shapeRenderer.rect(tx + 13f, ty + 18f, 6f, 3f)
                shapeRenderer.rect(tx + 31f, ty + 18f, 4f, 3f)
            }
            CropType.CORN -> {
                shapeRenderer.color = Color(0.25f * d, 0.5f * d, 0.12f * d, 1f)
                shapeRenderer.rect(tx + 22f, ty + 2f, 4f, 42f)
                shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 18f, 16f, 5f)
                shapeRenderer.rect(tx + 24f, ty + 24f, 16f, 5f)
                shapeRenderer.rect(tx + 10f, ty + 32f, 14f, 4f)
                shapeRenderer.color = Color(0.95f * d, 0.85f * d, 0.2f * d, 1f)
                shapeRenderer.rect(tx + 26f, ty + 14f, 8f, 16f)
                shapeRenderer.color = Color(0.3f * d, 0.5f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 25f, ty + 12f, 10f, 4f)
                shapeRenderer.rect(tx + 25f, ty + 28f, 10f, 4f)
                shapeRenderer.color = Color(0.7f * d, 0.6f * d, 0.3f * d, 0.6f)
                shapeRenderer.rect(tx + 20f, ty + 40f, 8f, 6f)
            }
            CropType.POTATO -> {
                shapeRenderer.color = Color(0.22f * d, 0.52f * d, 0.16f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 14f, 32f, 16f)
                shapeRenderer.rect(tx + 12f, ty + 28f, 24f, 10f)
                shapeRenderer.color = Color(0.65f * d, 0.48f * d, 0.28f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 4f, 12f, 10f)
                shapeRenderer.rect(tx + 26f, ty + 6f, 14f, 8f)
                shapeRenderer.color = Color(0.55f * d, 0.4f * d, 0.22f * d, 0.6f)
                shapeRenderer.rect(tx + 12f, ty + 8f, 3f, 3f)
                shapeRenderer.rect(tx + 30f, ty + 9f, 3f, 3f)
            }
            CropType.CARROT -> {
                shapeRenderer.color = Color(0.18f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 12f, ty + 22f, 6f, 20f)
                shapeRenderer.rect(tx + 20f, ty + 24f, 5f, 18f)
                shapeRenderer.rect(tx + 28f, ty + 20f, 6f, 22f)
                shapeRenderer.color = Color(0.22f * d, 0.6f * d, 0.18f * d, 0.7f)
                shapeRenderer.rect(tx + 8f, ty + 30f, 4f, 10f)
                shapeRenderer.rect(tx + 34f, ty + 28f, 4f, 12f)
                shapeRenderer.color = Color(0.95f * d, 0.5f * d, 0.1f * d, 1f)
                shapeRenderer.rect(tx + 14f, ty + 4f, 8f, 18f)
                shapeRenderer.rect(tx + 26f, ty + 6f, 7f, 16f)
                shapeRenderer.color = Color(0.9f * d, 0.45f * d, 0.08f * d, 1f)
                shapeRenderer.rect(tx + 16f, ty + 2f, 4f, 4f)
                shapeRenderer.rect(tx + 28f, ty + 4f, 3f, 4f)
            }
            CropType.CABBAGE -> {
                shapeRenderer.color = Color(0.3f * d, 0.65f * d, 0.2f * d, 1f)
                shapeRenderer.rect(tx + 6f, ty + 6f, 36f, 28f)
                shapeRenderer.color = Color(0.4f * d, 0.75f * d, 0.3f * d, 1f)
                shapeRenderer.rect(tx + 12f, ty + 10f, 24f, 20f)
                shapeRenderer.color = Color(0.6f * d, 0.85f * d, 0.5f * d, 1f)
                shapeRenderer.rect(tx + 18f, ty + 14f, 12f, 12f)
                shapeRenderer.color = Color(0.25f * d, 0.58f * d, 0.18f * d, 0.6f)
                shapeRenderer.rect(tx + 4f, ty + 8f, 6f, 12f)
                shapeRenderer.rect(tx + 38f, ty + 10f, 6f, 10f)
                shapeRenderer.rect(tx + 14f, ty + 32f, 20f, 8f)
            }
            CropType.RADISH -> {
                shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 14f, ty + 24f, 20f, 18f)
                shapeRenderer.rect(tx + 10f, ty + 30f, 28f, 10f)
                shapeRenderer.color = Color(0.9f * d, 0.9f * d, 0.85f * d, 1f)
                shapeRenderer.rect(tx + 16f, ty + 4f, 16f, 22f)
                shapeRenderer.color = Color(0.85f * d, 0.85f * d, 0.8f * d, 1f)
                shapeRenderer.rect(tx + 19f, ty + 2f, 10f, 4f)
                shapeRenderer.color = Color(0.95f * d, 0.95f * d, 0.92f * d, 0.4f)
                shapeRenderer.rect(tx + 20f, ty + 12f, 4f, 10f)
            }
            CropType.SPINACH -> {
                shapeRenderer.color = Color(0.15f * d, 0.5f * d, 0.12f * d, 1f)
                shapeRenderer.rect(tx + 6f, ty + 6f, 36f, 24f)
                shapeRenderer.color = Color(0.2f * d, 0.6f * d, 0.16f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 10f, 14f, 10f)
                shapeRenderer.rect(tx + 24f, ty + 8f, 14f, 12f)
                shapeRenderer.rect(tx + 14f, ty + 22f, 20f, 12f)
                shapeRenderer.color = Color(0.18f * d, 0.45f * d, 0.12f * d, 0.5f)
                shapeRenderer.rect(tx + 14f, ty + 14f, 6f, 2f)
                shapeRenderer.rect(tx + 30f, ty + 12f, 6f, 2f)
            }
        }
    }

    // ===== ANIMAL RENDERING =====

    fun renderAnimals(daylight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (animal in world.animals) {
            val ax = animal.renderX * tileSize
            val ay = animal.renderY * tileSize
            val d = daylight
            val wobble = MathUtils.sin(animTimer * 2f + animal.renderX * 5f) * 1.5f

            when (animal.type) {
                AnimalType.COW -> renderCow(ax, ay, d, wobble)
                AnimalType.CHICKEN -> renderChicken(ax, ay, d, wobble)
                AnimalType.SHEEP -> renderSheep(ax, ay, d, wobble)
                AnimalType.PIG -> renderPig(ax, ay, d, wobble)
            }

            if (animal.hasProduct) {
                val pulse = (MathUtils.sin(animTimer * 3f) + 1) / 2
                shapeRenderer.color = Color(1f, 1f, 0f, 0.5f + 0.5f * pulse)
                shapeRenderer.rect(ax + tileSize / 2 - 4, ay + tileSize - 4, 8f, 8f)
            }
        }
        shapeRenderer.end()
    }

    private fun renderCow(x: Float, y: Float, d: Float, w: Float) {
        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
        shapeRenderer.rect(x + 6, y, 36f, 4f)
        shapeRenderer.color = Color(0.85f * d, 0.78f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 10, y + 2 + w * 0.3f, 6f, 12f)
        shapeRenderer.rect(x + 18, y + 2 - w * 0.3f, 6f, 12f)
        shapeRenderer.rect(x + 26, y + 2 + w * 0.3f, 6f, 12f)
        shapeRenderer.rect(x + 34, y + 2 - w * 0.3f, 6f, 12f)
        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.15f * d, 1f)
        shapeRenderer.rect(x + 10, y + 1, 6f, 3f); shapeRenderer.rect(x + 18, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 26, y + 1, 6f, 3f); shapeRenderer.rect(x + 34, y + 1, 6f, 3f)
        shapeRenderer.color = Color(0.92f * d, 0.87f * d, 0.82f * d, 1f)
        shapeRenderer.rect(x + 8, y + 13, 34f, 18f); shapeRenderer.rect(x + 10, y + 11, 30f, 22f)
        shapeRenderer.color = Color(0.2f * d, 0.18f * d, 0.16f * d, 1f)
        shapeRenderer.rect(x + 14, y + 18, 8f, 7f); shapeRenderer.rect(x + 28, y + 22, 6f, 5f); shapeRenderer.rect(x + 20, y + 26, 7f, 4f)
        shapeRenderer.color = Color(0.92f * d, 0.87f * d, 0.82f * d, 1f)
        shapeRenderer.rect(x + 4, y + 24, 14f, 14f)
        shapeRenderer.color = Color(0.95f * d, 0.75f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 2, y + 24, 10f, 8f)
        shapeRenderer.color = Color(0.6f * d, 0.4f * d, 0.35f * d, 1f)
        shapeRenderer.rect(x + 3, y + 27, 3f, 2f); shapeRenderer.rect(x + 8, y + 27, 3f, 2f)
        shapeRenderer.color = Color.WHITE; shapeRenderer.rect(x + 7, y + 34, 5f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f); shapeRenderer.rect(x + 8, y + 34, 3f, 3f)
        shapeRenderer.color = Color(0.85f * d, 0.78f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 3, y + 36, 5f, 6f); shapeRenderer.rect(x + 14, y + 36, 5f, 6f)
        shapeRenderer.color = Color(0.9f * d, 0.85f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 5, y + 40, 3f, 5f); shapeRenderer.rect(x + 13, y + 40, 3f, 5f)
        shapeRenderer.color = Color(0.85f * d, 0.78f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 40, y + 28, 3f, 10f); shapeRenderer.rect(x + 39, y + 36, 5f, 3f)
        shapeRenderer.color = Color(0.95f * d, 0.78f * d, 0.75f * d, 1f)
        shapeRenderer.rect(x + 20, y + 11, 10f, 4f)
    }

    private fun renderChicken(x: Float, y: Float, d: Float, w: Float) {
        shapeRenderer.color = Color(0f, 0f, 0f, 0.12f); shapeRenderer.rect(x + 14, y, 20f, 3f)
        shapeRenderer.color = Color(0.9f * d, 0.6f * d, 0.15f * d, 1f)
        shapeRenderer.rect(x + 18, y + 2 + w * 0.5f, 3f, 10f); shapeRenderer.rect(x + 27, y + 2 - w * 0.5f, 3f, 10f)
        shapeRenderer.rect(x + 16, y + 1, 7f, 3f); shapeRenderer.rect(x + 25, y + 1, 7f, 3f)
        shapeRenderer.color = Color(1f * d, 0.97f * d, 0.85f * d, 1f)
        shapeRenderer.rect(x + 14, y + 11, 20f, 16f); shapeRenderer.rect(x + 16, y + 9, 16f, 20f)
        shapeRenderer.color = Color(0.95f * d, 0.9f * d, 0.75f * d, 1f); shapeRenderer.rect(x + 28, y + 14, 8f, 10f)
        shapeRenderer.color = Color(1f * d, 0.97f * d, 0.85f * d, 1f); shapeRenderer.rect(x + 12, y + 26, 14f, 12f)
        shapeRenderer.color = Color(0.95f * d, 0.15f * d, 0.1f * d, 1f)
        shapeRenderer.rect(x + 15, y + 36, 8f, 6f); shapeRenderer.rect(x + 13, y + 38, 4f, 4f); shapeRenderer.rect(x + 21, y + 38, 4f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f); shapeRenderer.rect(x + 14, y + 32, 3f, 3f)
        shapeRenderer.color = Color(0.95f * d, 0.7f * d, 0.1f * d, 1f); shapeRenderer.rect(x + 8, y + 28, 6f, 4f)
        shapeRenderer.color = Color(0.9f * d, 0.2f * d, 0.15f * d, 1f); shapeRenderer.rect(x + 12, y + 24, 4f, 4f)
        shapeRenderer.color = Color(0.9f * d, 0.85f * d, 0.65f * d, 1f)
        shapeRenderer.rect(x + 32, y + 20, 6f, 12f); shapeRenderer.rect(x + 34, y + 24, 4f, 10f)
    }

    private fun renderSheep(x: Float, y: Float, d: Float, w: Float) {
        shapeRenderer.color = Color(0f, 0f, 0f, 0.12f); shapeRenderer.rect(x + 8, y, 32f, 4f)
        shapeRenderer.color = Color(0.25f * d, 0.2f * d, 0.18f * d, 1f)
        shapeRenderer.rect(x + 12, y + 2 + w * 0.3f, 5f, 11f); shapeRenderer.rect(x + 19, y + 2 - w * 0.3f, 5f, 11f)
        shapeRenderer.rect(x + 26, y + 2 + w * 0.3f, 5f, 11f); shapeRenderer.rect(x + 33, y + 2 - w * 0.3f, 5f, 11f)
        shapeRenderer.rect(x + 12, y + 1, 5f, 3f); shapeRenderer.rect(x + 19, y + 1, 5f, 3f)
        shapeRenderer.rect(x + 26, y + 1, 5f, 3f); shapeRenderer.rect(x + 33, y + 1, 5f, 3f)
        shapeRenderer.color = Color(0.97f * d, 0.97f * d, 0.95f * d, 1f)
        shapeRenderer.rect(x + 10, y + 14, 30f, 16f); shapeRenderer.rect(x + 8, y + 16, 34f, 12f)
        shapeRenderer.color = Color(0.95f * d, 0.95f * d, 0.93f * d, 1f)
        shapeRenderer.rect(x + 12, y + 28, 8f, 5f); shapeRenderer.rect(x + 22, y + 29, 8f, 5f); shapeRenderer.rect(x + 32, y + 27, 6f, 4f)
        shapeRenderer.color = Color(0.3f * d, 0.25f * d, 0.22f * d, 1f)
        shapeRenderer.rect(x + 4, y + 22, 12f, 14f); shapeRenderer.rect(x + 2, y + 32, 5f, 6f); shapeRenderer.rect(x + 13, y + 32, 5f, 6f)
        shapeRenderer.color = Color(0.9f * d, 0.9f * d, 0.6f * d, 1f); shapeRenderer.rect(x + 6, y + 30, 4f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f); shapeRenderer.rect(x + 7, y + 30, 2f, 3f)
        shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.3f * d, 1f); shapeRenderer.rect(x + 6, y + 24, 6f, 3f)
        shapeRenderer.color = Color(0.97f * d, 0.97f * d, 0.95f * d, 1f); shapeRenderer.rect(x + 38, y + 24, 6f, 6f)
    }

    private fun renderPig(x: Float, y: Float, d: Float, w: Float) {
        shapeRenderer.color = Color(0f, 0f, 0f, 0.12f); shapeRenderer.rect(x + 8, y, 32f, 4f)
        shapeRenderer.color = Color(0.9f * d, 0.6f * d, 0.55f * d, 1f)
        shapeRenderer.rect(x + 12, y + 2 + w * 0.3f, 6f, 9f); shapeRenderer.rect(x + 20, y + 2 - w * 0.3f, 6f, 9f)
        shapeRenderer.rect(x + 28, y + 2 + w * 0.3f, 6f, 9f); shapeRenderer.rect(x + 34, y + 2 - w * 0.3f, 6f, 9f)
        shapeRenderer.color = Color(0.4f * d, 0.28f * d, 0.22f * d, 1f)
        shapeRenderer.rect(x + 12, y + 1, 6f, 3f); shapeRenderer.rect(x + 20, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 28, y + 1, 6f, 3f); shapeRenderer.rect(x + 34, y + 1, 6f, 3f)
        shapeRenderer.color = Color(1f * d, 0.78f * d, 0.73f * d, 1f)
        shapeRenderer.rect(x + 10, y + 10, 32f, 18f); shapeRenderer.rect(x + 12, y + 8, 28f, 22f)
        shapeRenderer.color = Color(1f * d, 0.85f * d, 0.82f * d, 1f); shapeRenderer.rect(x + 16, y + 10, 20f, 8f)
        shapeRenderer.color = Color(1f * d, 0.78f * d, 0.73f * d, 1f); shapeRenderer.rect(x + 4, y + 18, 14f, 14f)
        shapeRenderer.color = Color(1f * d, 0.65f * d, 0.6f * d, 1f); shapeRenderer.rect(x + 1, y + 20, 10f, 8f)
        shapeRenderer.color = Color(0.7f * d, 0.4f * d, 0.38f * d, 1f)
        shapeRenderer.rect(x + 2, y + 23, 3f, 3f); shapeRenderer.rect(x + 7, y + 23, 3f, 3f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f); shapeRenderer.rect(x + 8, y + 28, 3f, 3f)
        shapeRenderer.color = Color(0.95f * d, 0.7f * d, 0.65f * d, 1f)
        shapeRenderer.rect(x + 3, y + 30, 6f, 6f); shapeRenderer.rect(x + 12, y + 31, 6f, 5f)
        shapeRenderer.color = Color(1f * d, 0.72f * d, 0.68f * d, 1f)
        shapeRenderer.rect(x + 40, y + 24, 3f, 6f); shapeRenderer.rect(x + 38, y + 28, 3f, 3f); shapeRenderer.rect(x + 40, y + 30, 3f, 3f)
    }

    // ===== AUTO FISH DEVICE =====

    fun renderAutoFishDevice(daylight: Float) {
        if (world.autoFishLevel <= 0) return
        val d = daylight
        val fx = world.autoFishX.toFloat() * tileSize
        val fy = world.autoFishY.toFloat() * tileSize
        val bobbing = MathUtils.sin(animTimer * 2f) * 2f
        val catching = world.autoFishCatchAnim > 0
        val catchBounce = if (catching) MathUtils.sin(animTimer * 12f) * 4f else 0f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        when (world.autoFishLevel) {
            1 -> {
                shapeRenderer.color = Color(0f, 0f, 0f, 0.15f); shapeRenderer.rect(fx + 8, fy, 32f, 4f)
                shapeRenderer.color = Color(0.55f * d, 0.35f * d, 0.15f * d, 1f); shapeRenderer.rect(fx + 10, fy + 4 + bobbing, 28f, 20f)
                shapeRenderer.color = Color(0.45f * d, 0.28f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 10, fy + 10 + bobbing, 28f, 2f); shapeRenderer.rect(fx + 10, fy + 16 + bobbing, 28f, 2f)
                shapeRenderer.rect(fx + 20, fy + 4 + bobbing, 2f, 20f)
                shapeRenderer.color = Color(0.15f * d, 0.1f * d, 0.05f * d, 1f); shapeRenderer.rect(fx + 16, fy + 22 + bobbing, 16f, 4f)
                shapeRenderer.color = Color(0.6f * d, 0.5f * d, 0.3f * d, 1f)
                shapeRenderer.rect(fx + 22, fy + 24 + bobbing, 3f, 12f); shapeRenderer.rect(fx + 20, fy + 34, 7f, 3f)
            }
            2 -> {
                shapeRenderer.color = Color(0f, 0f, 0f, 0.15f); shapeRenderer.rect(fx + 6, fy, 36f, 4f)
                shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.55f * d, 1f); shapeRenderer.rect(fx + 8, fy + 4 + bobbing, 32f, 24f)
                shapeRenderer.color = Color(0.35f * d, 0.35f * d, 0.4f * d, 1f)
                for (i in 0..3) {
                    shapeRenderer.rect(fx + 8, fy + 8 + i * 5 + bobbing, 32f, 1f)
                    shapeRenderer.rect(fx + 12 + i * 7, fy + 4 + bobbing, 1f, 24f)
                }
                shapeRenderer.color = Color(0.9f * d, 0.3f * d, 0.1f * d, 1f); shapeRenderer.rect(fx + 18, fy + 28 + bobbing, 12f, 6f)
                shapeRenderer.color = Color(0.9f * d, 0.8f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 36, fy + 24 + bobbing, 2f, 16f); shapeRenderer.rect(fx + 38, fy + 34 + bobbing, 8f, 6f)
            }
            3 -> {
                shapeRenderer.color = Color(0f, 0f, 0f, 0.15f); shapeRenderer.rect(fx + 4, fy, 40f, 4f)
                shapeRenderer.color = Color(0.4f * d, 0.25f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 6, fy + 4 + bobbing, 36f, 10f); shapeRenderer.rect(fx + 10, fy + 2 + bobbing, 28f, 4f)
                shapeRenderer.color = Color(0.55f * d, 0.4f * d, 0.2f * d, 1f); shapeRenderer.rect(fx + 8, fy + 14 + bobbing, 32f, 6f)
                shapeRenderer.color = Color(0.6f * d, 0.55f * d, 0.45f * d, 1f); shapeRenderer.rect(fx + 14, fy + 20 + bobbing, 16f, 12f)
                shapeRenderer.color = Color(0.5f * d, 0.7f * d, 0.9f * d, 1f); shapeRenderer.rect(fx + 18, fy + 26 + bobbing, 8f, 4f)
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f); shapeRenderer.rect(fx + 22, fy + 32 + bobbing, 3f, 16f)
                shapeRenderer.color = Color(0.2f * d, 0.5f * d, 0.9f * d, 1f); shapeRenderer.rect(fx + 25, fy + 42 + bobbing, 10f, 6f)
                shapeRenderer.color = Color(0.5f * d, 0.4f * d, 0.2f * d, 1f); shapeRenderer.rect(fx + 36, fy + 18 + bobbing, 2f, 20f)
                shapeRenderer.color = Color(0.8f * d, 0.8f * d, 0.8f * d, 0.6f); shapeRenderer.rect(fx + 37, fy + 6 + bobbing, 1f, 12f)
            }
        }

        if (catching) {
            shapeRenderer.color = Color(0.4f * d, 0.6f * d, 0.9f * d, 0.8f)
            shapeRenderer.rect(fx + 14 + catchBounce, fy + 30 + Math.abs(catchBounce) * 2, 8f, 4f)
            shapeRenderer.rect(fx + 16 + catchBounce, fy + 28 + Math.abs(catchBounce) * 2, 4f, 8f)
            shapeRenderer.color = Color(0.5f * d, 0.7f * d, 1f * d, 0.5f)
            for (i in 0..2) {
                val sx = fx + 10 + MathUtils.sin(animTimer * 10f + i * 2f) * 12f
                val sy = fy + 6 + MathUtils.cos(animTimer * 8f + i * 1.5f).coerceAtLeast(0f) * 8f
                shapeRenderer.rect(sx, sy, 3f, 3f)
            }
        }

        shapeRenderer.end()
    }
}
