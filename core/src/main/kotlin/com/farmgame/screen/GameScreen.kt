package com.farmgame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.farmgame.data.*
import com.farmgame.entity.Direction
import com.farmgame.entity.NPC
import com.farmgame.ui.FontManager
import com.farmgame.ui.GameHUD
import com.farmgame.world.GameWorld
import com.farmgame.world.TileType

class GameScreen : ScreenAdapter() {
    private val world = GameWorld()
    private val camera = OrthographicCamera()
    private val shapeRenderer = ShapeRenderer(40000)
    private val batch = SpriteBatch()
    private val hud = GameHUD(world)

    private val tileSize = 48f
    private var zoom = 1f
    private var animTimer = 0f

    private val raindrops = mutableListOf<FloatArray>()

    init {
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        // Auto-save callback
        world.onAutoSave = {
            if (com.farmgame.system.SaveSystem.save(world, com.farmgame.system.SaveSystem.AUTO_SAVE_SLOT)) {
                world.lastAutoSaveTime = world.timeSystem.getFullDate()
                world.notify("자동 저장 완료!")
            }
        }
    }

    private fun enableBlending() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun render(delta: Float) {
        try { doRender(delta) } catch (e: Exception) {
            e.printStackTrace()
            // Write crash log
            try {
                val logFile = java.io.File("crash_log.txt")
                logFile.appendText("=== ${java.time.LocalDateTime.now()} ===\n${e.stackTraceToString()}\n\n")
            } catch (_: Exception) {}
            emergencySave()
        }
    }

    private fun doRender(delta: Float) {
        animTimer += delta

        if (!hud.showShop && !hud.showInventory && !hud.showSaveLoad && !hud.showMissions && !hud.showAchievements) {
            handleMovementInput(delta)
        }
        handleActionInput()
        world.update(delta)
        updateRain(delta)

        camera.position.x = world.player.renderX * tileSize + tileSize / 2
        camera.position.y = world.player.renderY * tileSize + tileSize / 2
        camera.zoom = zoom
        camera.update()

        val daylight = world.timeSystem.daylight
        Gdx.gl.glClearColor(0.08f * daylight, 0.12f * daylight, 0.06f * daylight, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        enableBlending()

        shapeRenderer.projectionMatrix = camera.combined

        // ===== ALL SHAPE RENDERING =====
        renderTiles(daylight)
        renderCrops(daylight)
        renderAnimalsShapes(daylight)
        renderAutoFishDevice(daylight)
        renderNPCsShapes(daylight)
        renderHiredFarmer(daylight)
        renderHerder(daylight)
        renderPlayer(daylight)
        renderFacingIndicator()
        renderWeatherEffects()
        renderNightOverlay()

        // ===== ALL TEXT RENDERING =====
        enableBlending()
        batch.projectionMatrix = camera.combined
        batch.begin()
        FontManager.fontSmall.color = Color.WHITE
        for (animal in world.animals) {
            FontManager.fontSmall.draw(batch, animal.type.koreanName,
                animal.renderX * tileSize + 2, animal.renderY * tileSize - 2)
        }
        FontManager.fontSmall.color = Color.YELLOW
        for (npc in world.npcs) {
            FontManager.fontSmall.draw(batch, npc.data.koreanName,
                npc.renderX * tileSize - 10, npc.renderY * tileSize + tileSize + 18)
        }
        // Hired farmer names
        val farmerColors = listOf(Color(0.2f, 1f, 0.4f, 1f), Color(0.4f, 0.6f, 1f, 1f), Color(1f, 0.85f, 0.2f, 1f))
        for ((i, farmer) in world.farmers.withIndex()) {
            FontManager.fontSmall.color = farmerColors.getOrElse(i) { Color.WHITE }
            val actionText = when (farmer.action) {
                "till" -> "경작중"; "water" -> "물주는중"; "plant" -> "심는중"
                "harvest" -> "수확중"; "walk" -> "이동중"; else -> "대기중"
            }
            FontManager.fontSmall.draw(batch, "${farmer.name} ($actionText)",
                farmer.renderX * tileSize - 10, farmer.renderY * tileSize + tileSize + 18)
        }
        // Herder names
        val herderColors = listOf(Color(1f, 0.7f, 0.3f, 1f), Color(0.8f, 0.4f, 0.9f, 1f), Color(1f, 0.95f, 0.3f, 1f))
        for ((i, herder) in world.herders.withIndex()) {
            FontManager.fontSmall.color = herderColors.getOrElse(i) { Color.WHITE }
            val hAction = when (herder.action) {
                "collect" -> "수확중"; "feed" -> "먹이주는중"; "walk" -> "이동중"; else -> "대기중"
            }
            FontManager.fontSmall.draw(batch, "${herder.name} ($hAction)",
                herder.renderX * tileSize - 10, herder.renderY * tileSize + tileSize + 18)
        }
        batch.end()

        hud.render()
    }

    // ===== TILE RENDERING (HIGH DETAIL) =====

    private fun renderTiles(daylight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        val camX = camera.position.x
        val camY = camera.position.y
        val viewW = camera.viewportWidth * zoom / 2 + tileSize * 2
        val viewH = camera.viewportHeight * zoom / 2 + tileSize * 2

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

                // Base tile
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
        // Animated water with ripples
        val wave1 = MathUtils.sin((x + y).toFloat() * 0.8f + animTimer * 1.5f) * 0.08f
        val wave2 = MathUtils.sin((x - y).toFloat() * 1.2f + animTimer * 2f) * 0.05f
        shapeRenderer.color = Color((0.15f + wave1) * d, (0.35f + wave2) * d, (0.7f + wave1) * d, 1f)
        shapeRenderer.rect(tx, ty, tileSize, tileSize)

        // Shimmer highlights
        val shimmer = MathUtils.sin(x.toFloat() * 2f + animTimer * 3f) * 0.5f + 0.5f
        shapeRenderer.color = Color(0.4f * d, 0.6f * d, 0.9f * d, 0.15f * shimmer)
        shapeRenderer.rect(tx + 4f + wave1 * 20f, ty + tileSize * 0.3f, tileSize * 0.4f, 3f)
        shapeRenderer.rect(tx + 16f + wave2 * 15f, ty + tileSize * 0.65f, tileSize * 0.3f, 2f)
    }

    private fun renderTree(tx: Float, ty: Float, d: Float) {
        val isAutumn = world.timeSystem.season == Season.AUTUMN
        val isWinter = world.timeSystem.season == Season.WINTER

        // Trunk - bark texture
        shapeRenderer.color = Color(0.35f * d, 0.22f * d, 0.08f * d, 1f)
        shapeRenderer.rect(tx + 18f, ty, 12f, 22f)
        // Bark detail - darker stripe
        shapeRenderer.color = Color(0.25f * d, 0.15f * d, 0.05f * d, 1f)
        shapeRenderer.rect(tx + 21f, ty + 2f, 3f, 18f)
        // Bark highlight
        shapeRenderer.color = Color(0.42f * d, 0.28f * d, 0.12f * d, 1f)
        shapeRenderer.rect(tx + 26f, ty + 3f, 2f, 14f)

        // Roots
        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.07f * d, 1f)
        shapeRenderer.rect(tx + 14f, ty, 6f, 4f)
        shapeRenderer.rect(tx + 28f, ty, 6f, 3f)

        if (isWinter) {
            // Bare branches
            shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.1f * d, 1f)
            shapeRenderer.rect(tx + 8f, ty + 30f, 32f, 4f)
            shapeRenderer.rect(tx + 12f, ty + 34f, 6f, 8f)
            shapeRenderer.rect(tx + 28f, ty + 34f, 6f, 6f)
            // Snow on branches
            shapeRenderer.color = Color(0.9f * d, 0.92f * d, 0.95f * d, 0.7f)
            shapeRenderer.rect(tx + 8f, ty + 33f, 32f, 3f)
        } else {
            // Canopy - multiple layers for fullness
            val greenBase = if (isAutumn) 0.25f else 0.5f
            val redBase = if (isAutumn) 0.65f else 0.12f
            val greenBright = if (isAutumn) 0.3f else 0.6f
            val redBright = if (isAutumn) 0.75f else 0.15f

            // Bottom canopy layer
            shapeRenderer.color = Color(redBase * d, greenBase * d, 0.08f * d, 1f)
            shapeRenderer.rect(tx + 4f, ty + 18f, 40f, 16f)
            // Mid canopy (brighter)
            shapeRenderer.color = Color(redBright * d, greenBright * d, 0.12f * d, 1f)
            shapeRenderer.rect(tx + 8f, ty + 22f, 32f, 16f)
            // Top canopy (rounded effect)
            shapeRenderer.color = Color((redBright + 0.05f) * d, (greenBright + 0.05f) * d, 0.15f * d, 1f)
            shapeRenderer.rect(tx + 12f, ty + 34f, 24f, 10f)
            // Highlight
            shapeRenderer.color = Color((redBright + 0.1f) * d, (greenBright + 0.1f) * d, 0.18f * d, 0.6f)
            shapeRenderer.rect(tx + 14f, ty + 36f, 12f, 6f)

            // Leaf detail dots
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
        // Check neighbors for fence connectivity
        val hasLeft = world.getTile(x - 1, y)?.type == TileType.FENCE
        val hasRight = world.getTile(x + 1, y)?.type == TileType.FENCE
        val hasUp = world.getTile(x, y + 1)?.type == TileType.FENCE
        val hasDown = world.getTile(x, y - 1)?.type == TileType.FENCE

        // Grass base
        shapeRenderer.color = Color(0.35f * d, 0.55f * d, 0.2f * d, 1f)
        shapeRenderer.rect(tx, ty, tileSize, tileSize)

        val woodDark = Color(0.4f * d, 0.28f * d, 0.12f * d, 1f)
        val woodLight = Color(0.55f * d, 0.38f * d, 0.18f * d, 1f)
        val woodHighlight = Color(0.62f * d, 0.45f * d, 0.22f * d, 1f)

        // Vertical post (always)
        shapeRenderer.color = woodDark
        shapeRenderer.rect(tx + 20f, ty + 2f, 8f, 44f)
        // Post highlight
        shapeRenderer.color = woodLight
        shapeRenderer.rect(tx + 22f, ty + 4f, 4f, 40f)
        // Post cap
        shapeRenderer.color = woodHighlight
        shapeRenderer.rect(tx + 19f, ty + 42f, 10f, 4f)

        // Horizontal bars (connect to neighbors)
        if (hasLeft || hasRight) {
            // Upper bar
            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx, ty + 30f, tileSize, 5f)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 2f, ty + 31f, tileSize - 4f, 3f)

            // Lower bar
            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx, ty + 14f, tileSize, 5f)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 2f, ty + 15f, tileSize - 4f, 3f)
        }

        if (hasUp || hasDown) {
            // Vertical connector bars
            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx + 12f, ty, 5f, tileSize)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 13f, ty + 2f, 3f, tileSize - 4f)

            shapeRenderer.color = woodDark
            shapeRenderer.rect(tx + 31f, ty, 5f, tileSize)
            shapeRenderer.color = woodLight
            shapeRenderer.rect(tx + 32f, ty + 2f, 3f, tileSize - 4f)
        }

        // Nail details
        shapeRenderer.color = Color(0.35f * d, 0.35f * d, 0.38f * d, 1f)
        if (hasLeft || hasRight) {
            shapeRenderer.rect(tx + 23f, ty + 31f, 2f, 2f)
            shapeRenderer.rect(tx + 23f, ty + 15f, 2f, 2f)
        }
    }

    private fun renderRock(tx: Float, ty: Float, d: Float, x: Int, y: Int) {
        // Big rock body
        val hash = ((x * 7919 + y * 104729) % 10) / 10f
        shapeRenderer.color = Color((0.5f + hash * 0.1f) * d, (0.48f + hash * 0.08f) * d, (0.45f + hash * 0.05f) * d, 1f)
        shapeRenderer.rect(tx + 6f, ty + 4f, 36f, 28f)
        // Top rounded
        shapeRenderer.rect(tx + 10f, ty + 28f, 28f, 10f)
        // Highlight
        shapeRenderer.color = Color(0.65f * d, 0.62f * d, 0.58f * d, 0.6f)
        shapeRenderer.rect(tx + 14f, ty + 22f, 16f, 8f)
        // Shadow crevice
        shapeRenderer.color = Color(0.3f * d, 0.28f * d, 0.25f * d, 0.7f)
        shapeRenderer.rect(tx + 20f, ty + 12f, 12f, 3f)
        // Moss
        shapeRenderer.color = Color(0.25f * d, 0.45f * d, 0.15f * d, 0.4f)
        shapeRenderer.rect(tx + 8f, ty + 4f, 10f, 6f)
    }

    private fun renderMarket(tx: Float, ty: Float, d: Float) {
        // Stall base
        shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f)
        shapeRenderer.rect(tx + 2f, ty + 2f, 44f, 24f)
        // Counter top
        shapeRenderer.color = Color(0.6f * d, 0.45f * d, 0.2f * d, 1f)
        shapeRenderer.rect(tx + 2f, ty + 22f, 44f, 6f)
        // Awning
        val pulse = (MathUtils.sin(animTimer * 2f) + 1) / 2 * 0.15f
        shapeRenderer.color = Color((0.8f + pulse) * d, (0.2f + pulse) * d, 0.1f * d, 1f)
        shapeRenderer.rect(tx, ty + 30f, tileSize, 10f)
        // Awning stripes
        shapeRenderer.color = Color(0.9f * d, 0.85f * d, 0.4f * d, 1f)
        shapeRenderer.rect(tx + 4f, ty + 32f, 8f, 6f)
        shapeRenderer.rect(tx + 20f, ty + 32f, 8f, 6f)
        shapeRenderer.rect(tx + 36f, ty + 32f, 8f, 6f)
        // Gold coin indicator
        shapeRenderer.color = Color(1f * d, 0.85f * d, 0.2f * d, 0.6f + pulse)
        shapeRenderer.rect(tx + 18f, ty + 10f, 12f, 12f)
    }

    private fun renderHome(tx: Float, ty: Float, d: Float) {
        // Walls
        shapeRenderer.color = Color(0.75f * d, 0.55f * d, 0.35f * d, 1f)
        shapeRenderer.rect(tx + 2f, ty + 2f, 44f, 28f)
        // Roof
        shapeRenderer.color = Color(0.6f * d, 0.2f * d, 0.15f * d, 1f)
        shapeRenderer.rect(tx, ty + 30f, tileSize, 14f)
        // Roof ridge
        shapeRenderer.color = Color(0.5f * d, 0.15f * d, 0.1f * d, 1f)
        shapeRenderer.rect(tx + 8f, ty + 40f, 32f, 6f)
        // Door
        shapeRenderer.color = Color(0.4f * d, 0.25f * d, 0.12f * d, 1f)
        shapeRenderer.rect(tx + 18f, ty + 2f, 12f, 18f)
        // Door knob
        shapeRenderer.color = Color(0.8f * d, 0.7f * d, 0.2f * d, 1f)
        shapeRenderer.rect(tx + 27f, ty + 10f, 2f, 2f)
        // Window
        if (world.timeSystem.isNight) {
            shapeRenderer.color = Color(1f * d, 0.9f * d, 0.5f * d, 0.6f)
        } else {
            shapeRenderer.color = Color(0.5f * d, 0.7f * d, 0.9f * d, 0.7f)
        }
        shapeRenderer.rect(tx + 6f, ty + 14f, 10f, 10f)
        shapeRenderer.rect(tx + 32f, ty + 14f, 10f, 10f)
        // Window frame
        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.1f * d, 1f)
        shapeRenderer.rect(tx + 10f, ty + 14f, 2f, 10f)
        shapeRenderer.rect(tx + 36f, ty + 14f, 2f, 10f)
    }

    private fun renderSand(tx: Float, ty: Float, d: Float) {
        // Sand grain details
        shapeRenderer.color = Color(0.82f * d, 0.72f * d, 0.5f * d, 0.4f)
        shapeRenderer.rect(tx + 8f, ty + 6f, 3f, 2f)
        shapeRenderer.rect(tx + 28f, ty + 18f, 4f, 2f)
        shapeRenderer.rect(tx + 16f, ty + 34f, 3f, 2f)
        // Shell
        shapeRenderer.color = Color(0.9f * d, 0.88f * d, 0.8f * d, 0.3f)
        shapeRenderer.rect(tx + 36f, ty + 10f, 4f, 3f)
    }

    private fun renderFarmland(tx: Float, ty: Float, d: Float, isWet: Boolean) {
        // Furrow lines
        val furrowColor = if (isWet) Color(0.25f * d, 0.18f * d, 0.1f * d, 0.5f)
                          else Color(0.35f * d, 0.25f * d, 0.12f * d, 0.4f)
        shapeRenderer.color = furrowColor
        for (i in 0..3) {
            shapeRenderer.rect(tx + 2f, ty + 4f + i * 12f, tileSize - 4f, 2f)
        }
        if (isWet) {
            // Wet sheen
            shapeRenderer.color = Color(0.3f * d, 0.4f * d, 0.6f * d, 0.15f)
            shapeRenderer.rect(tx + 4f, ty + 4f, tileSize - 8f, tileSize - 8f)
        }
    }

    private fun renderGrassDetail(tx: Float, ty: Float, d: Float, x: Int, y: Int) {
        // Grass tufts
        val hash = ((x * 3571 + y * 7823) % 100)
        if (hash < 30) {
            val grassGreen = if (world.timeSystem.season == Season.WINTER) 0.35f else 0.55f
            shapeRenderer.color = Color(0.18f * d, grassGreen * d, 0.12f * d, 0.5f)
            shapeRenderer.rect(tx + 8f + (hash % 5) * 2f, ty + 12f, 2f, 6f)
            shapeRenderer.rect(tx + 20f + (hash % 3) * 3f, ty + 28f, 2f, 5f)
            shapeRenderer.rect(tx + 34f, ty + 8f + (hash % 4) * 2f, 2f, 7f)
        }
        if (hash < 10) {
            // Flower
            val season = world.timeSystem.season
            if (season == Season.SPRING || season == Season.SUMMER) {
                shapeRenderer.color = Color(0.9f * d, 0.7f * d, 0.2f * d, 0.6f)
                shapeRenderer.rect(tx + 14f + (hash % 7) * 2f, ty + 20f, 3f, 3f)
            }
        }
    }

    private fun renderPath(tx: Float, ty: Float, d: Float) {
        // Cobblestone texture
        shapeRenderer.color = Color(0.52f * d, 0.45f * d, 0.35f * d, 0.4f)
        shapeRenderer.rect(tx + 4f, ty + 4f, 14f, 12f)
        shapeRenderer.rect(tx + 22f, ty + 6f, 16f, 10f)
        shapeRenderer.rect(tx + 8f, ty + 24f, 18f, 12f)
        shapeRenderer.rect(tx + 30f, ty + 22f, 12f, 14f)
        // Highlight
        shapeRenderer.color = Color(0.6f * d, 0.55f * d, 0.42f * d, 0.25f)
        shapeRenderer.rect(tx + 6f, ty + 8f, 8f, 4f)
        shapeRenderer.rect(tx + 10f, ty + 28f, 10f, 4f)
    }

    // ===== CROP RENDERING (HIGH DETAIL) =====

    private fun renderCrops(daylight: Float) {
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
        // Seedling - small green sprout growing
        val height = 4f + progress * 24f
        val width = 2f + progress * 6f

        // Stem
        shapeRenderer.color = Color(0.2f * d, 0.5f * d, 0.15f * d, 1f)
        shapeRenderer.rect(tx + 22f, ty + 6f, 4f, height)

        // Leaves emerge at higher growth
        if (progress > 0.3f) {
            shapeRenderer.color = Color(0.25f * d, 0.6f * d, 0.18f * d, 1f)
            shapeRenderer.rect(tx + 16f, ty + 6f + height * 0.5f, 8f, width)
            shapeRenderer.rect(tx + 24f, ty + 6f + height * 0.6f, 8f, width * 0.8f)
        }

        // Tiny fruit/bud hint at higher growth
        if (progress > 0.7f) {
            val c = type.grownColor
            shapeRenderer.color = Color(c.r * 0.7f * d, c.g * 0.7f * d, c.b * 0.7f * d, 0.6f)
            shapeRenderer.rect(tx + 18f, ty + 6f + height - 4f, 6f, 6f)
        }
    }

    private fun renderFullyGrownCrop(tx: Float, ty: Float, d: Float, type: CropType) {
        when (type) {
            CropType.STRAWBERRY -> {
                // Bushy leaves
                shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 16f, 32f, 14f)
                shapeRenderer.rect(tx + 12f, ty + 28f, 24f, 8f)
                // Red berries
                shapeRenderer.color = Color(0.9f * d, 0.15f * d, 0.1f * d, 1f)
                shapeRenderer.rect(tx + 10f, ty + 6f, 8f, 10f)
                shapeRenderer.rect(tx + 22f, ty + 8f, 7f, 9f)
                shapeRenderer.rect(tx + 32f, ty + 6f, 8f, 10f)
                // Seed dots on berries
                shapeRenderer.color = Color(0.95f * d, 0.8f * d, 0.2f * d, 0.7f)
                shapeRenderer.rect(tx + 12f, ty + 10f, 2f, 2f)
                shapeRenderer.rect(tx + 24f, ty + 11f, 2f, 2f)
                shapeRenderer.rect(tx + 35f, ty + 9f, 2f, 2f)
            }
            CropType.TOMATO -> {
                // Vine/stem
                shapeRenderer.color = Color(0.2f * d, 0.5f * d, 0.12f * d, 1f)
                shapeRenderer.rect(tx + 22f, ty + 4f, 4f, 36f)
                // Leaves
                shapeRenderer.color = Color(0.22f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 10f, ty + 24f, 28f, 10f)
                shapeRenderer.rect(tx + 14f, ty + 32f, 20f, 8f)
                // Red tomatoes (round)
                shapeRenderer.color = Color(0.9f * d, 0.12f * d, 0.08f * d, 1f)
                shapeRenderer.rect(tx + 10f, ty + 8f, 12f, 12f)
                shapeRenderer.rect(tx + 28f, ty + 10f, 10f, 10f)
                // Tomato highlight
                shapeRenderer.color = Color(1f * d, 0.3f * d, 0.2f * d, 0.5f)
                shapeRenderer.rect(tx + 12f, ty + 14f, 4f, 4f)
                // Green stem top on tomato
                shapeRenderer.color = Color(0.15f * d, 0.45f * d, 0.1f * d, 1f)
                shapeRenderer.rect(tx + 13f, ty + 18f, 6f, 3f)
                shapeRenderer.rect(tx + 31f, ty + 18f, 4f, 3f)
            }
            CropType.CORN -> {
                // Tall stalk
                shapeRenderer.color = Color(0.25f * d, 0.5f * d, 0.12f * d, 1f)
                shapeRenderer.rect(tx + 22f, ty + 2f, 4f, 42f)
                // Long leaves
                shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 18f, 16f, 5f)
                shapeRenderer.rect(tx + 24f, ty + 24f, 16f, 5f)
                shapeRenderer.rect(tx + 10f, ty + 32f, 14f, 4f)
                // Corn cob
                shapeRenderer.color = Color(0.95f * d, 0.85f * d, 0.2f * d, 1f)
                shapeRenderer.rect(tx + 26f, ty + 14f, 8f, 16f)
                // Cob husk
                shapeRenderer.color = Color(0.3f * d, 0.5f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 25f, ty + 12f, 10f, 4f)
                shapeRenderer.rect(tx + 25f, ty + 28f, 10f, 4f)
                // Silk on top
                shapeRenderer.color = Color(0.7f * d, 0.6f * d, 0.3f * d, 0.6f)
                shapeRenderer.rect(tx + 20f, ty + 40f, 8f, 6f)
            }
            CropType.POTATO -> {
                // Low leafy bush
                shapeRenderer.color = Color(0.22f * d, 0.52f * d, 0.16f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 14f, 32f, 16f)
                shapeRenderer.rect(tx + 12f, ty + 28f, 24f, 10f)
                // Potato tubers (peeking from soil)
                shapeRenderer.color = Color(0.65f * d, 0.48f * d, 0.28f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 4f, 12f, 10f)
                shapeRenderer.rect(tx + 26f, ty + 6f, 14f, 8f)
                // Potato spots
                shapeRenderer.color = Color(0.55f * d, 0.4f * d, 0.22f * d, 0.6f)
                shapeRenderer.rect(tx + 12f, ty + 8f, 3f, 3f)
                shapeRenderer.rect(tx + 30f, ty + 9f, 3f, 3f)
            }
            CropType.CARROT -> {
                // Feathery greens (tall)
                shapeRenderer.color = Color(0.18f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 12f, ty + 22f, 6f, 20f)
                shapeRenderer.rect(tx + 20f, ty + 24f, 5f, 18f)
                shapeRenderer.rect(tx + 28f, ty + 20f, 6f, 22f)
                // More feathery detail
                shapeRenderer.color = Color(0.22f * d, 0.6f * d, 0.18f * d, 0.7f)
                shapeRenderer.rect(tx + 8f, ty + 30f, 4f, 10f)
                shapeRenderer.rect(tx + 34f, ty + 28f, 4f, 12f)
                // Orange carrot (peeking from soil)
                shapeRenderer.color = Color(0.95f * d, 0.5f * d, 0.1f * d, 1f)
                shapeRenderer.rect(tx + 14f, ty + 4f, 8f, 18f)
                shapeRenderer.rect(tx + 26f, ty + 6f, 7f, 16f)
                // Carrot tip
                shapeRenderer.color = Color(0.9f * d, 0.45f * d, 0.08f * d, 1f)
                shapeRenderer.rect(tx + 16f, ty + 2f, 4f, 4f)
                shapeRenderer.rect(tx + 28f, ty + 4f, 3f, 4f)
            }
            CropType.CABBAGE -> {
                // Large green head
                shapeRenderer.color = Color(0.3f * d, 0.65f * d, 0.2f * d, 1f)
                shapeRenderer.rect(tx + 6f, ty + 6f, 36f, 28f)
                // Inner lighter layers
                shapeRenderer.color = Color(0.4f * d, 0.75f * d, 0.3f * d, 1f)
                shapeRenderer.rect(tx + 12f, ty + 10f, 24f, 20f)
                // Center (pale green/white)
                shapeRenderer.color = Color(0.6f * d, 0.85f * d, 0.5f * d, 1f)
                shapeRenderer.rect(tx + 18f, ty + 14f, 12f, 12f)
                // Outer leaf details
                shapeRenderer.color = Color(0.25f * d, 0.58f * d, 0.18f * d, 0.6f)
                shapeRenderer.rect(tx + 4f, ty + 8f, 6f, 12f)
                shapeRenderer.rect(tx + 38f, ty + 10f, 6f, 10f)
                // Top leaf
                shapeRenderer.rect(tx + 14f, ty + 32f, 20f, 8f)
            }
            CropType.RADISH -> {
                // Green tops
                shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.15f * d, 1f)
                shapeRenderer.rect(tx + 14f, ty + 24f, 20f, 18f)
                shapeRenderer.rect(tx + 10f, ty + 30f, 28f, 10f)
                // White radish body
                shapeRenderer.color = Color(0.9f * d, 0.9f * d, 0.85f * d, 1f)
                shapeRenderer.rect(tx + 16f, ty + 4f, 16f, 22f)
                // Radish tip
                shapeRenderer.color = Color(0.85f * d, 0.85f * d, 0.8f * d, 1f)
                shapeRenderer.rect(tx + 19f, ty + 2f, 10f, 4f)
                // Highlight
                shapeRenderer.color = Color(0.95f * d, 0.95f * d, 0.92f * d, 0.4f)
                shapeRenderer.rect(tx + 20f, ty + 12f, 4f, 10f)
            }
            CropType.SPINACH -> {
                // Dark green leafy spread
                shapeRenderer.color = Color(0.15f * d, 0.5f * d, 0.12f * d, 1f)
                shapeRenderer.rect(tx + 6f, ty + 6f, 36f, 24f)
                // Individual leaf shapes
                shapeRenderer.color = Color(0.2f * d, 0.6f * d, 0.16f * d, 1f)
                shapeRenderer.rect(tx + 8f, ty + 10f, 14f, 10f)
                shapeRenderer.rect(tx + 24f, ty + 8f, 14f, 12f)
                shapeRenderer.rect(tx + 14f, ty + 22f, 20f, 12f)
                // Leaf veins
                shapeRenderer.color = Color(0.18f * d, 0.45f * d, 0.12f * d, 0.5f)
                shapeRenderer.rect(tx + 14f, ty + 14f, 6f, 2f)
                shapeRenderer.rect(tx + 30f, ty + 12f, 6f, 2f)
            }
        }
    }

    // ===== ANIMAL RENDERING =====

    private fun renderAnimalsShapes(daylight: Float) {
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
        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
        shapeRenderer.rect(x + 6, y, 36f, 4f)
        // Legs (4)
        shapeRenderer.color = Color(0.85f * d, 0.78f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 10, y + 2 + w * 0.3f, 6f, 12f)
        shapeRenderer.rect(x + 18, y + 2 - w * 0.3f, 6f, 12f)
        shapeRenderer.rect(x + 26, y + 2 + w * 0.3f, 6f, 12f)
        shapeRenderer.rect(x + 34, y + 2 - w * 0.3f, 6f, 12f)
        // Hooves
        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.15f * d, 1f)
        shapeRenderer.rect(x + 10, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 18, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 26, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 34, y + 1, 6f, 3f)
        // Body (large oval shape)
        shapeRenderer.color = Color(0.92f * d, 0.87f * d, 0.82f * d, 1f)
        shapeRenderer.rect(x + 8, y + 13, 34f, 18f)
        shapeRenderer.rect(x + 10, y + 11, 30f, 22f)
        // Black spots
        shapeRenderer.color = Color(0.2f * d, 0.18f * d, 0.16f * d, 1f)
        shapeRenderer.rect(x + 14, y + 18, 8f, 7f)
        shapeRenderer.rect(x + 28, y + 22, 6f, 5f)
        shapeRenderer.rect(x + 20, y + 26, 7f, 4f)
        // Head
        shapeRenderer.color = Color(0.92f * d, 0.87f * d, 0.82f * d, 1f)
        shapeRenderer.rect(x + 4, y + 24, 14f, 14f)
        // Snout (pink)
        shapeRenderer.color = Color(0.95f * d, 0.75f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 2, y + 24, 10f, 8f)
        // Nostrils
        shapeRenderer.color = Color(0.6f * d, 0.4f * d, 0.35f * d, 1f)
        shapeRenderer.rect(x + 3, y + 27, 3f, 2f)
        shapeRenderer.rect(x + 8, y + 27, 3f, 2f)
        // Eyes
        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(x + 7, y + 34, 5f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f)
        shapeRenderer.rect(x + 8, y + 34, 3f, 3f)
        // Ears
        shapeRenderer.color = Color(0.85f * d, 0.78f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 3, y + 36, 5f, 6f)
        shapeRenderer.rect(x + 14, y + 36, 5f, 6f)
        // Horns
        shapeRenderer.color = Color(0.9f * d, 0.85f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 5, y + 40, 3f, 5f)
        shapeRenderer.rect(x + 13, y + 40, 3f, 5f)
        // Tail
        shapeRenderer.color = Color(0.85f * d, 0.78f * d, 0.7f * d, 1f)
        shapeRenderer.rect(x + 40, y + 28, 3f, 10f)
        shapeRenderer.rect(x + 39, y + 36, 5f, 3f)
        // Udder
        shapeRenderer.color = Color(0.95f * d, 0.78f * d, 0.75f * d, 1f)
        shapeRenderer.rect(x + 20, y + 11, 10f, 4f)
    }

    private fun renderChicken(x: Float, y: Float, d: Float, w: Float) {
        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.12f)
        shapeRenderer.rect(x + 14, y, 20f, 3f)
        // Legs (orange)
        shapeRenderer.color = Color(0.9f * d, 0.6f * d, 0.15f * d, 1f)
        shapeRenderer.rect(x + 18, y + 2 + w * 0.5f, 3f, 10f)
        shapeRenderer.rect(x + 27, y + 2 - w * 0.5f, 3f, 10f)
        // Feet
        shapeRenderer.rect(x + 16, y + 1, 7f, 3f)
        shapeRenderer.rect(x + 25, y + 1, 7f, 3f)
        // Body (round)
        shapeRenderer.color = Color(1f * d, 0.97f * d, 0.85f * d, 1f)
        shapeRenderer.rect(x + 14, y + 11, 20f, 16f)
        shapeRenderer.rect(x + 16, y + 9, 16f, 20f)
        // Wing
        shapeRenderer.color = Color(0.95f * d, 0.9f * d, 0.75f * d, 1f)
        shapeRenderer.rect(x + 28, y + 14, 8f, 10f)
        // Head
        shapeRenderer.color = Color(1f * d, 0.97f * d, 0.85f * d, 1f)
        shapeRenderer.rect(x + 12, y + 26, 14f, 12f)
        // Comb (red)
        shapeRenderer.color = Color(0.95f * d, 0.15f * d, 0.1f * d, 1f)
        shapeRenderer.rect(x + 15, y + 36, 8f, 6f)
        shapeRenderer.rect(x + 13, y + 38, 4f, 4f)
        shapeRenderer.rect(x + 21, y + 38, 4f, 4f)
        // Eye
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f)
        shapeRenderer.rect(x + 14, y + 32, 3f, 3f)
        // Beak
        shapeRenderer.color = Color(0.95f * d, 0.7f * d, 0.1f * d, 1f)
        shapeRenderer.rect(x + 8, y + 28, 6f, 4f)
        // Wattle
        shapeRenderer.color = Color(0.9f * d, 0.2f * d, 0.15f * d, 1f)
        shapeRenderer.rect(x + 12, y + 24, 4f, 4f)
        // Tail feathers
        shapeRenderer.color = Color(0.9f * d, 0.85f * d, 0.65f * d, 1f)
        shapeRenderer.rect(x + 32, y + 20, 6f, 12f)
        shapeRenderer.rect(x + 34, y + 24, 4f, 10f)
    }

    private fun renderSheep(x: Float, y: Float, d: Float, w: Float) {
        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.12f)
        shapeRenderer.rect(x + 8, y, 32f, 4f)
        // Legs (dark)
        shapeRenderer.color = Color(0.25f * d, 0.2f * d, 0.18f * d, 1f)
        shapeRenderer.rect(x + 12, y + 2 + w * 0.3f, 5f, 11f)
        shapeRenderer.rect(x + 19, y + 2 - w * 0.3f, 5f, 11f)
        shapeRenderer.rect(x + 26, y + 2 + w * 0.3f, 5f, 11f)
        shapeRenderer.rect(x + 33, y + 2 - w * 0.3f, 5f, 11f)
        // Hooves
        shapeRenderer.rect(x + 12, y + 1, 5f, 3f)
        shapeRenderer.rect(x + 19, y + 1, 5f, 3f)
        shapeRenderer.rect(x + 26, y + 1, 5f, 3f)
        shapeRenderer.rect(x + 33, y + 1, 5f, 3f)
        // Fluffy wool body (layered rects to look round)
        shapeRenderer.color = Color(0.97f * d, 0.97f * d, 0.95f * d, 1f)
        shapeRenderer.rect(x + 10, y + 14, 30f, 16f)
        shapeRenderer.rect(x + 8, y + 16, 34f, 12f)
        // Wool bumps on top
        shapeRenderer.color = Color(0.95f * d, 0.95f * d, 0.93f * d, 1f)
        shapeRenderer.rect(x + 12, y + 28, 8f, 5f)
        shapeRenderer.rect(x + 22, y + 29, 8f, 5f)
        shapeRenderer.rect(x + 32, y + 27, 6f, 4f)
        // Head (dark face)
        shapeRenderer.color = Color(0.3f * d, 0.25f * d, 0.22f * d, 1f)
        shapeRenderer.rect(x + 4, y + 22, 12f, 14f)
        // Ears
        shapeRenderer.rect(x + 2, y + 32, 5f, 6f)
        shapeRenderer.rect(x + 13, y + 32, 5f, 6f)
        // Eyes
        shapeRenderer.color = Color(0.9f * d, 0.9f * d, 0.6f * d, 1f)
        shapeRenderer.rect(x + 6, y + 30, 4f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f)
        shapeRenderer.rect(x + 7, y + 30, 2f, 3f)
        // Nose
        shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.3f * d, 1f)
        shapeRenderer.rect(x + 6, y + 24, 6f, 3f)
        // Tail (small wool puff)
        shapeRenderer.color = Color(0.97f * d, 0.97f * d, 0.95f * d, 1f)
        shapeRenderer.rect(x + 38, y + 24, 6f, 6f)
    }

    private fun renderPig(x: Float, y: Float, d: Float, w: Float) {
        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.12f)
        shapeRenderer.rect(x + 8, y, 32f, 4f)
        // Legs (short & stubby)
        shapeRenderer.color = Color(0.9f * d, 0.6f * d, 0.55f * d, 1f)
        shapeRenderer.rect(x + 12, y + 2 + w * 0.3f, 6f, 9f)
        shapeRenderer.rect(x + 20, y + 2 - w * 0.3f, 6f, 9f)
        shapeRenderer.rect(x + 28, y + 2 + w * 0.3f, 6f, 9f)
        shapeRenderer.rect(x + 34, y + 2 - w * 0.3f, 6f, 9f)
        // Hooves
        shapeRenderer.color = Color(0.4f * d, 0.28f * d, 0.22f * d, 1f)
        shapeRenderer.rect(x + 12, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 20, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 28, y + 1, 6f, 3f)
        shapeRenderer.rect(x + 34, y + 1, 6f, 3f)
        // Body (round, pink)
        shapeRenderer.color = Color(1f * d, 0.78f * d, 0.73f * d, 1f)
        shapeRenderer.rect(x + 10, y + 10, 32f, 18f)
        shapeRenderer.rect(x + 12, y + 8, 28f, 22f)
        // Belly (lighter)
        shapeRenderer.color = Color(1f * d, 0.85f * d, 0.82f * d, 1f)
        shapeRenderer.rect(x + 16, y + 10, 20f, 8f)
        // Head
        shapeRenderer.color = Color(1f * d, 0.78f * d, 0.73f * d, 1f)
        shapeRenderer.rect(x + 4, y + 18, 14f, 14f)
        // Snout (big round)
        shapeRenderer.color = Color(1f * d, 0.65f * d, 0.6f * d, 1f)
        shapeRenderer.rect(x + 1, y + 20, 10f, 8f)
        // Nostrils
        shapeRenderer.color = Color(0.7f * d, 0.4f * d, 0.38f * d, 1f)
        shapeRenderer.rect(x + 2, y + 23, 3f, 3f)
        shapeRenderer.rect(x + 7, y + 23, 3f, 3f)
        // Eyes
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.1f * d, 1f)
        shapeRenderer.rect(x + 8, y + 28, 3f, 3f)
        // Ears (floppy)
        shapeRenderer.color = Color(0.95f * d, 0.7f * d, 0.65f * d, 1f)
        shapeRenderer.rect(x + 3, y + 30, 6f, 6f)
        shapeRenderer.rect(x + 12, y + 31, 6f, 5f)
        // Curly tail
        shapeRenderer.color = Color(1f * d, 0.72f * d, 0.68f * d, 1f)
        shapeRenderer.rect(x + 40, y + 24, 3f, 6f)
        shapeRenderer.rect(x + 38, y + 28, 3f, 3f)
        shapeRenderer.rect(x + 40, y + 30, 3f, 3f)
    }

    // ===== NPC RENDERING =====

    private fun renderNPCsShapes(daylight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (npc in world.npcs) {
            renderSingleNPC(npc, daylight)
        }
        shapeRenderer.end()
    }

    private fun renderSingleNPC(npc: NPC, d: Float) {
        val nx = npc.renderX * tileSize
        val ny = npc.renderY * tileSize
        val c = npc.data.color
        val walkCycle = MathUtils.sin(animTimer * 5f + npc.renderX * 3f)

        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
        shapeRenderer.rect(nx + 8, ny - 1, 32f, 5f)

        // Legs
        shapeRenderer.color = Color(c.r * 0.5f * d, c.g * 0.5f * d, c.b * 0.5f * d, 1f)
        val legOff = walkCycle * 2f
        shapeRenderer.rect(nx + 14, ny + legOff.coerceAtLeast(0f), 7f, 12f)
        shapeRenderer.rect(nx + 27, ny + (-legOff).coerceAtLeast(0f), 7f, 12f)

        // Shoes
        shapeRenderer.color = Color(0.3f * d, 0.2f * d, 0.15f * d, 1f)
        shapeRenderer.rect(nx + 13, ny + legOff.coerceAtLeast(0f), 9f, 4f)
        shapeRenderer.rect(nx + 26, ny + (-legOff).coerceAtLeast(0f), 9f, 4f)

        // Body
        shapeRenderer.color = Color(c.r * d, c.g * d, c.b * d, 1f)
        shapeRenderer.rect(nx + 10, ny + 12, 28f, 18f)

        // Arms (skin)
        val skinR = 0.9f * d; val skinG = 0.75f * d; val skinB = 0.6f * d
        shapeRenderer.color = Color(skinR, skinG, skinB, 1f)
        shapeRenderer.rect(nx + 4, ny + 13 + legOff, 7f, 12f)
        shapeRenderer.rect(nx + 37, ny + 13 - legOff, 7f, 12f)

        // Head
        shapeRenderer.color = Color(0.95f * d, 0.8f * d, 0.65f * d, 1f)
        shapeRenderer.rect(nx + 13, ny + 30, 22f, 14f)

        // Hair
        val hairR = ((c.r * 0.5f + 0.1f) * d).coerceAtMost(1f)
        val hairG = ((c.g * 0.3f + 0.05f) * d).coerceAtMost(1f)
        val hairB = ((c.b * 0.2f) * d).coerceAtMost(1f)
        shapeRenderer.color = Color(hairR, hairG, hairB, 1f)
        shapeRenderer.rect(nx + 12, ny + 40, 24f, 6f)
        shapeRenderer.rect(nx + 13, ny + 38, 22f, 4f)

        // Eyes
        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(nx + 16, ny + 36, 6f, 4f)
        shapeRenderer.rect(nx + 26, ny + 36, 6f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.15f * d, 1f)
        shapeRenderer.rect(nx + 18, ny + 36, 3f, 3f)
        shapeRenderer.rect(nx + 28, ny + 36, 3f, 3f)

        // Mouth
        shapeRenderer.color = Color(0.8f * d, 0.45f * d, 0.35f * d, 1f)
        shapeRenderer.rect(nx + 20, ny + 32, 8f, 2f)

        // Accessories
        when (npc.data.name) {
            "Merchant" -> {
                shapeRenderer.color = Color(0.8f * d, 0.2f * d, 0.2f * d, 1f)
                shapeRenderer.rect(nx + 10, ny + 44, 28f, 4f)
            }
            "Elder" -> {
                shapeRenderer.color = Color(0.85f * d, 0.85f * d, 0.82f * d, 1f)
                shapeRenderer.rect(nx + 12, ny + 40, 24f, 6f)
                shapeRenderer.rect(nx + 13, ny + 38, 22f, 4f)
            }
            "Fisher" -> {
                shapeRenderer.color = Color(0.3f * d, 0.5f * d, 0.7f * d, 1f)
                shapeRenderer.rect(nx + 9, ny + 44, 30f, 4f)
                shapeRenderer.rect(nx + 13, ny + 47, 22f, 5f)
            }
            "Farmer" -> {
                shapeRenderer.color = Color(0.8f * d, 0.65f * d, 0.2f * d, 1f)
                shapeRenderer.rect(nx + 7, ny + 44, 34f, 4f)
                shapeRenderer.rect(nx + 13, ny + 47, 22f, 5f)
            }
        }
    }

    // ===== AUTO FISH DEVICE RENDERING =====

    private fun renderAutoFishDevice(daylight: Float) {
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
                // 기본 통발 - 나무 바구니 형태
                // Shadow
                shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
                shapeRenderer.rect(fx + 8, fy, 32f, 4f)
                // Basket body
                shapeRenderer.color = Color(0.55f * d, 0.35f * d, 0.15f * d, 1f)
                shapeRenderer.rect(fx + 10, fy + 4 + bobbing, 28f, 20f)
                // Basket weave pattern
                shapeRenderer.color = Color(0.45f * d, 0.28f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 10, fy + 10 + bobbing, 28f, 2f)
                shapeRenderer.rect(fx + 10, fy + 16 + bobbing, 28f, 2f)
                shapeRenderer.rect(fx + 20, fy + 4 + bobbing, 2f, 20f)
                // Opening
                shapeRenderer.color = Color(0.15f * d, 0.1f * d, 0.05f * d, 1f)
                shapeRenderer.rect(fx + 16, fy + 22 + bobbing, 16f, 4f)
                // Rope to shore
                shapeRenderer.color = Color(0.6f * d, 0.5f * d, 0.3f * d, 1f)
                shapeRenderer.rect(fx + 22, fy + 24 + bobbing, 3f, 12f)
                // Stick on shore
                shapeRenderer.rect(fx + 20, fy + 34, 7f, 3f)
            }
            2 -> {
                // 고급 통발 - 철제 그물
                shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
                shapeRenderer.rect(fx + 6, fy, 36f, 4f)
                // Metal frame
                shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.55f * d, 1f)
                shapeRenderer.rect(fx + 8, fy + 4 + bobbing, 32f, 24f)
                // Net mesh pattern
                shapeRenderer.color = Color(0.35f * d, 0.35f * d, 0.4f * d, 1f)
                for (i in 0..3) {
                    shapeRenderer.rect(fx + 8, fy + 8 + i * 5 + bobbing, 32f, 1f)
                    shapeRenderer.rect(fx + 12 + i * 7, fy + 4 + bobbing, 1f, 24f)
                }
                // Float buoy
                shapeRenderer.color = Color(0.9f * d, 0.3f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 18, fy + 28 + bobbing, 12f, 6f)
                // Flag
                shapeRenderer.color = Color(0.9f * d, 0.8f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 36, fy + 24 + bobbing, 2f, 16f)
                shapeRenderer.rect(fx + 38, fy + 34 + bobbing, 8f, 6f)
            }
            3 -> {
                // 자동 낚시선 - 작은 보트
                shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
                shapeRenderer.rect(fx + 4, fy, 40f, 4f)
                // Hull
                shapeRenderer.color = Color(0.4f * d, 0.25f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 6, fy + 4 + bobbing, 36f, 10f)
                shapeRenderer.rect(fx + 10, fy + 2 + bobbing, 28f, 4f)
                // Deck
                shapeRenderer.color = Color(0.55f * d, 0.4f * d, 0.2f * d, 1f)
                shapeRenderer.rect(fx + 8, fy + 14 + bobbing, 32f, 6f)
                // Cabin
                shapeRenderer.color = Color(0.6f * d, 0.55f * d, 0.45f * d, 1f)
                shapeRenderer.rect(fx + 14, fy + 20 + bobbing, 16f, 12f)
                // Window
                shapeRenderer.color = Color(0.5f * d, 0.7f * d, 0.9f * d, 1f)
                shapeRenderer.rect(fx + 18, fy + 26 + bobbing, 8f, 4f)
                // Mast
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f)
                shapeRenderer.rect(fx + 22, fy + 32 + bobbing, 3f, 16f)
                // Flag
                shapeRenderer.color = Color(0.2f * d, 0.5f * d, 0.9f * d, 1f)
                shapeRenderer.rect(fx + 25, fy + 42 + bobbing, 10f, 6f)
                // Fishing rod
                shapeRenderer.color = Color(0.5f * d, 0.4f * d, 0.2f * d, 1f)
                shapeRenderer.rect(fx + 36, fy + 18 + bobbing, 2f, 20f)
                // Line
                shapeRenderer.color = Color(0.8f * d, 0.8f * d, 0.8f * d, 0.6f)
                shapeRenderer.rect(fx + 37, fy + 6 + bobbing, 1f, 12f)
            }
        }

        // Catch animation - fish jumping
        if (catching) {
            shapeRenderer.color = Color(0.4f * d, 0.6f * d, 0.9f * d, 0.8f)
            shapeRenderer.rect(fx + 14 + catchBounce, fy + 30 + Math.abs(catchBounce) * 2, 8f, 4f)
            shapeRenderer.rect(fx + 16 + catchBounce, fy + 28 + Math.abs(catchBounce) * 2, 4f, 8f)
            // Splash
            shapeRenderer.color = Color(0.5f * d, 0.7f * d, 1f * d, 0.5f)
            for (i in 0..2) {
                val sx = fx + 10 + MathUtils.sin(animTimer * 10f + i * 2f) * 12f
                val sy = fy + 6 + MathUtils.cos(animTimer * 8f + i * 1.5f).coerceAtLeast(0f) * 8f
                shapeRenderer.rect(sx, sy, 3f, 3f)
            }
        }

        shapeRenderer.end()
    }

    // ===== HIRED FARMER RENDERING (all farmers) =====

    private fun renderHiredFarmer(daylight: Float) {
        if (world.farmers.isEmpty()) return
        for (farmer in world.farmers) {
            renderSingleFarmer(farmer, daylight)
        }
    }

    private fun renderSingleFarmer(farmer: com.farmgame.world.HiredWorker, daylight: Float) {
        val d = daylight
        val fx = farmer.renderX * tileSize
        val fy = farmer.renderY * tileSize
        val walkCycle = MathUtils.sin(animTimer * 5f + farmer.renderX * 3f)
        val isWorking = farmer.action in listOf("till", "water", "plant", "harvest")
        val workBob = if (isWorking) MathUtils.sin(animTimer * 8f) * 3f else 0f
        val level = farmer.level

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
        shapeRenderer.rect(fx + 8, fy - 1, 32f, 5f)

        // Legs
        shapeRenderer.color = Color(0.45f * d, 0.35f * d, 0.2f * d, 1f)
        val legOff = if (farmer.action == "walk") walkCycle * 2.5f else 0f
        shapeRenderer.rect(fx + 14, fy + legOff.coerceAtLeast(0f), 7f, 12f)
        shapeRenderer.rect(fx + 27, fy + (-legOff).coerceAtLeast(0f), 7f, 12f)

        // Boots
        shapeRenderer.color = Color(0.25f * d, 0.15f * d, 0.08f * d, 1f)
        shapeRenderer.rect(fx + 13, fy + legOff.coerceAtLeast(0f), 9f, 4f)
        shapeRenderer.rect(fx + 26, fy + (-legOff).coerceAtLeast(0f), 9f, 4f)

        // Body (shirt color by level)
        shapeRenderer.color = when (level) {
            1 -> Color(0.25f * d, 0.55f * d, 0.2f * d, 1f)
            2 -> Color(0.3f * d, 0.4f * d, 0.7f * d, 1f)
            3 -> Color(0.7f * d, 0.15f * d, 0.15f * d, 1f)
            else -> Color(0.5f * d, 0.5f * d, 0.5f * d, 1f)
        }
        shapeRenderer.rect(fx + 10, fy + 12 + workBob, 28f, 18f)

        // Arms
        val skinR = 0.85f * d; val skinG = 0.65f * d; val skinB = 0.45f * d
        shapeRenderer.color = Color(skinR, skinG, skinB, 1f)
        if (isWorking) {
            val armSwing = MathUtils.sin(animTimer * 8f) * 6f
            shapeRenderer.rect(fx + 4, fy + 16 + armSwing, 7f, 12f)
            shapeRenderer.rect(fx + 37, fy + 16 - armSwing, 7f, 12f)
        } else {
            shapeRenderer.rect(fx + 4, fy + 13 + legOff, 7f, 12f)
            shapeRenderer.rect(fx + 37, fy + 13 - legOff, 7f, 12f)
        }

        // Head
        shapeRenderer.color = Color(0.9f * d, 0.72f * d, 0.5f * d, 1f)
        shapeRenderer.rect(fx + 13, fy + 30 + workBob, 22f, 14f)

        // Eyes
        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(fx + 16, fy + 36 + workBob, 6f, 4f)
        shapeRenderer.rect(fx + 26, fy + 36 + workBob, 6f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.15f * d, 1f)
        shapeRenderer.rect(fx + 18, fy + 36 + workBob, 3f, 3f)
        shapeRenderer.rect(fx + 28, fy + 36 + workBob, 3f, 3f)

        // Mouth
        shapeRenderer.color = Color(0.75f * d, 0.4f * d, 0.3f * d, 1f)
        shapeRenderer.rect(fx + 19, fy + 32 + workBob, 10f, 2f)

        // Hat (style by level)
        when (level) {
            1 -> {
                shapeRenderer.color = Color(0.85f * d, 0.75f * d, 0.35f * d, 1f)
                shapeRenderer.rect(fx + 6, fy + 43 + workBob, 36f, 4f)
                shapeRenderer.rect(fx + 12, fy + 47 + workBob, 24f, 6f)
                shapeRenderer.color = Color(0.7f * d, 0.6f * d, 0.25f * d, 1f)
                shapeRenderer.rect(fx + 14, fy + 47 + workBob, 20f, 2f)
            }
            2 -> {
                shapeRenderer.color = Color(0.2f * d, 0.35f * d, 0.65f * d, 1f)
                shapeRenderer.rect(fx + 10, fy + 42 + workBob, 28f, 8f)
                shapeRenderer.rect(fx + 12, fy + 40 + workBob, 24f, 4f)
                shapeRenderer.color = Color(0.2f * d, 0.12f * d, 0.05f * d, 1f)
                shapeRenderer.rect(fx + 10, fy + 38 + workBob, 5f, 6f)
                shapeRenderer.rect(fx + 33, fy + 38 + workBob, 5f, 6f)
            }
            3 -> {
                shapeRenderer.color = Color(0.9f * d, 0.75f * d, 0.1f * d, 1f)
                shapeRenderer.rect(fx + 4, fy + 44 + workBob, 40f, 3f)
                shapeRenderer.rect(fx + 10, fy + 47 + workBob, 28f, 7f)
                shapeRenderer.color = Color(0.7f * d, 0.55f * d, 0.05f * d, 1f)
                shapeRenderer.rect(fx + 12, fy + 47 + workBob, 24f, 2f)
            }
        }

        // Tool in hand when working
        if (isWorking) {
            val toolSwing = MathUtils.sin(animTimer * 8f) * 4f
            when (farmer.action) {
                "till" -> {
                    shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.2f * d, 1f)
                    shapeRenderer.rect(fx + 40, fy + 20 + toolSwing, 4f, 18f)
                    shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.6f * d, 1f)
                    shapeRenderer.rect(fx + 38, fy + 36 + toolSwing, 8f, 4f)
                }
                "water" -> {
                    shapeRenderer.color = Color(0.3f * d, 0.45f * d, 0.7f * d, 1f)
                    shapeRenderer.rect(fx + 39, fy + 18 + toolSwing, 8f, 10f)
                    shapeRenderer.rect(fx + 46, fy + 22 + toolSwing, 4f, 3f)
                    if (MathUtils.sin(animTimer * 12f) > 0) {
                        shapeRenderer.color = Color(0.4f * d, 0.6f * d, 0.9f * d, 0.7f)
                        shapeRenderer.rect(fx + 47, fy + 16 + toolSwing, 2f, 3f)
                        shapeRenderer.rect(fx + 50, fy + 14 + toolSwing, 2f, 3f)
                    }
                }
                "plant" -> {
                    shapeRenderer.color = Color(0.6f * d, 0.45f * d, 0.15f * d, 1f)
                    shapeRenderer.rect(fx + 40, fy + 20 + toolSwing, 5f, 5f)
                    if (MathUtils.sin(animTimer * 10f) > 0) {
                        shapeRenderer.color = Color(0.5f * d, 0.4f * d, 0.1f * d, 0.6f)
                        shapeRenderer.rect(fx + 41, fy + 14 + toolSwing, 2f, 2f)
                        shapeRenderer.rect(fx + 44, fy + 12 + toolSwing, 2f, 2f)
                    }
                }
                "harvest" -> {
                    shapeRenderer.color = Color(0.65f * d, 0.5f * d, 0.25f * d, 1f)
                    shapeRenderer.rect(fx + 38, fy + 16 + toolSwing, 10f, 8f)
                    shapeRenderer.rect(fx + 36, fy + 24 + toolSwing, 14f, 2f)
                    shapeRenderer.color = Color(0.3f * d, 0.7f * d, 0.2f * d, 1f)
                    shapeRenderer.rect(fx + 39, fy + 24 + toolSwing, 4f, 4f)
                    shapeRenderer.color = Color(0.9f * d, 0.3f * d, 0.2f * d, 1f)
                    shapeRenderer.rect(fx + 44, fy + 24 + toolSwing, 3f, 3f)
                }
            }
        }

        // Working particle effects
        if (isWorking) {
            val pTime = animTimer * 6f
            when (farmer.action) {
                "till" -> {
                    shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 0.6f)
                    for (i in 0..2) {
                        val px = fx + 20 + MathUtils.sin(pTime + i * 2f) * 8f
                        val py = fy + 4 + MathUtils.cos(pTime + i * 1.5f).coerceAtLeast(0f) * 6f
                        shapeRenderer.rect(px, py, 3f, 3f)
                    }
                }
                "water" -> {
                    shapeRenderer.color = Color(0.3f * d, 0.5f * d, 0.9f * d, 0.5f)
                    for (i in 0..2) {
                        val px = fx + 18 + MathUtils.sin(pTime + i * 2.5f) * 10f
                        val py = fy + 2 + MathUtils.cos(pTime + i * 2f).coerceAtLeast(0f) * 5f
                        shapeRenderer.rect(px, py, 2f, 2f)
                    }
                }
            }
        }

        shapeRenderer.end()
    }

    // ===== HERDER RENDERING (all herders) =====

    private fun renderHerder(daylight: Float) {
        if (world.herders.isEmpty()) return
        for (herder in world.herders) {
            renderSingleHerder(herder, daylight)
        }
    }

    private fun renderSingleHerder(herder: com.farmgame.world.HiredWorker, daylight: Float) {
        val d = daylight
        val hx = herder.renderX * tileSize
        val hy = herder.renderY * tileSize
        val walkCycle = MathUtils.sin(animTimer * 5f + herder.renderX * 3f)
        val isWorking = herder.action in listOf("collect", "feed")
        val workBob = if (isWorking) MathUtils.sin(animTimer * 6f) * 2f else 0f
        val level = herder.level

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.15f)
        shapeRenderer.rect(hx + 8, hy - 1, 32f, 5f)

        // Legs
        val legOff = if (herder.action == "walk") walkCycle * 2.5f else 0f
        shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.2f * d, 1f)
        shapeRenderer.rect(hx + 14, hy + legOff.coerceAtLeast(0f), 7f, 12f)
        shapeRenderer.rect(hx + 27, hy + (-legOff).coerceAtLeast(0f), 7f, 12f)

        // Boots
        shapeRenderer.color = Color(0.3f * d, 0.18f * d, 0.08f * d, 1f)
        shapeRenderer.rect(hx + 13, hy + legOff.coerceAtLeast(0f), 9f, 4f)
        shapeRenderer.rect(hx + 26, hy + (-legOff).coerceAtLeast(0f), 9f, 4f)

        // Body (shirt color by level)
        shapeRenderer.color = when (level) {
            1 -> Color(0.7f * d, 0.45f * d, 0.15f * d, 1f)
            2 -> Color(0.55f * d, 0.25f * d, 0.6f * d, 1f)
            3 -> Color(0.75f * d, 0.65f * d, 0.1f * d, 1f)
            else -> Color(0.5f * d, 0.5f * d, 0.5f * d, 1f)
        }
        shapeRenderer.rect(hx + 10, hy + 12 + workBob, 28f, 18f)

        // Arms
        val skinR = 0.85f * d; val skinG = 0.65f * d; val skinB = 0.45f * d
        shapeRenderer.color = Color(skinR, skinG, skinB, 1f)
        if (isWorking) {
            val armSwing = MathUtils.sin(animTimer * 6f) * 5f
            shapeRenderer.rect(hx + 4, hy + 16 + armSwing, 7f, 12f)
            shapeRenderer.rect(hx + 37, hy + 16 - armSwing, 7f, 12f)
        } else {
            shapeRenderer.rect(hx + 4, hy + 13 + legOff, 7f, 12f)
            shapeRenderer.rect(hx + 37, hy + 13 - legOff, 7f, 12f)
        }

        // Head
        shapeRenderer.color = Color(0.9f * d, 0.72f * d, 0.5f * d, 1f)
        shapeRenderer.rect(hx + 13, hy + 30 + workBob, 22f, 14f)

        // Eyes
        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(hx + 16, hy + 36 + workBob, 6f, 4f)
        shapeRenderer.rect(hx + 26, hy + 36 + workBob, 6f, 4f)
        shapeRenderer.color = Color(0.1f * d, 0.1f * d, 0.15f * d, 1f)
        shapeRenderer.rect(hx + 18, hy + 36 + workBob, 3f, 3f)
        shapeRenderer.rect(hx + 28, hy + 36 + workBob, 3f, 3f)

        // Mouth
        shapeRenderer.color = Color(0.75f * d, 0.4f * d, 0.3f * d, 1f)
        shapeRenderer.rect(hx + 19, hy + 32 + workBob, 10f, 2f)

        // Bandana/hat by level
        when (level) {
            1 -> {
                shapeRenderer.color = Color(0.9f * d, 0.5f * d, 0.1f * d, 1f)
                shapeRenderer.rect(hx + 11, hy + 42 + workBob, 26f, 5f)
                shapeRenderer.rect(hx + 34, hy + 40 + workBob, 8f, 4f)
            }
            2 -> {
                shapeRenderer.color = Color(0.5f * d, 0.2f * d, 0.6f * d, 1f)
                shapeRenderer.rect(hx + 8, hy + 43 + workBob, 32f, 4f)
                shapeRenderer.rect(hx + 12, hy + 47 + workBob, 24f, 6f)
            }
            3 -> {
                shapeRenderer.color = Color(1f * d, 0.85f * d, 0.1f * d, 1f)
                shapeRenderer.rect(hx + 12, hy + 44 + workBob, 24f, 4f)
                shapeRenderer.rect(hx + 14, hy + 48 + workBob, 4f, 5f)
                shapeRenderer.rect(hx + 22, hy + 48 + workBob, 4f, 7f)
                shapeRenderer.rect(hx + 30, hy + 48 + workBob, 4f, 5f)
            }
        }

        // Tool when working
        if (isWorking) {
            val swing = MathUtils.sin(animTimer * 6f) * 3f
            when (herder.action) {
                "collect" -> {
                    shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.55f * d, 1f)
                    shapeRenderer.rect(hx + 39, hy + 16 + swing, 8f, 10f)
                    shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.65f * d, 1f)
                    shapeRenderer.rect(hx + 38, hy + 26 + swing, 10f, 2f)
                }
                "feed" -> {
                    shapeRenderer.color = Color(0.65f * d, 0.55f * d, 0.3f * d, 1f)
                    shapeRenderer.rect(hx + 39, hy + 16 + swing, 9f, 12f)
                    if (MathUtils.sin(animTimer * 10f) > 0) {
                        shapeRenderer.color = Color(0.8f * d, 0.7f * d, 0.2f * d, 0.6f)
                        shapeRenderer.rect(hx + 40, hy + 12 + swing, 2f, 2f)
                        shapeRenderer.rect(hx + 44, hy + 10 + swing, 2f, 2f)
                    }
                }
            }
        }

        shapeRenderer.end()
    }

    // ===== PLAYER RENDERING =====

    private fun renderPlayer(daylight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val px = world.player.renderX * tileSize
        val py = world.player.renderY * tileSize
        val d = daylight

        val isMoving = world.player.renderX != world.player.tileX.toFloat() ||
                        world.player.renderY != world.player.tileY.toFloat()
        val walkCycle = if (isMoving) MathUtils.sin(animTimer * 12f) else 0f

        // Shadow
        shapeRenderer.color = Color(0f, 0f, 0f, 0.2f)
        shapeRenderer.rect(px + 8, py - 1, 32f, 5f)

        // Legs
        shapeRenderer.color = Color(0.3f * d, 0.25f * d, 0.6f * d, 1f)
        when (world.player.facing) {
            Direction.LEFT, Direction.RIGHT -> {
                val legOff = walkCycle * 4f
                shapeRenderer.rect(px + 14, py + legOff.coerceAtLeast(0f), 7f, 14f)
                shapeRenderer.rect(px + 27, py + (-legOff).coerceAtLeast(0f), 7f, 14f)
            }
            else -> {
                val legOff = walkCycle * 3f
                shapeRenderer.rect(px + 13, py + legOff.coerceAtLeast(0f), 7f, 14f)
                shapeRenderer.rect(px + 28, py + (-legOff).coerceAtLeast(0f), 7f, 14f)
            }
        }

        // Shoes
        shapeRenderer.color = Color(0.4f * d, 0.2f * d, 0.1f * d, 1f)
        when (world.player.facing) {
            Direction.LEFT, Direction.RIGHT -> {
                val legOff = walkCycle * 4f
                shapeRenderer.rect(px + 13, py + legOff.coerceAtLeast(0f), 9f, 4f)
                shapeRenderer.rect(px + 26, py + (-legOff).coerceAtLeast(0f), 9f, 4f)
            }
            else -> {
                val legOff = walkCycle * 3f
                shapeRenderer.rect(px + 12, py + legOff.coerceAtLeast(0f), 9f, 4f)
                shapeRenderer.rect(px + 27, py + (-legOff).coerceAtLeast(0f), 9f, 4f)
            }
        }

        // Body
        shapeRenderer.color = Color(0.2f * d, 0.55f * d, 0.3f * d, 1f)
        shapeRenderer.rect(px + 10, py + 14, 28f, 16f)

        // Overalls
        shapeRenderer.color = Color(0.25f * d, 0.5f * d, 0.7f * d, 1f)
        shapeRenderer.rect(px + 14, py + 15, 4f, 14f)
        shapeRenderer.rect(px + 30, py + 15, 4f, 14f)

        // Arms
        val armSwing = walkCycle * 3f
        shapeRenderer.color = Color(0.9f * d, 0.75f * d, 0.6f * d, 1f)
        when (world.player.facing) {
            Direction.UP -> {
                shapeRenderer.rect(px + 4, py + 14 + armSwing, 7f, 14f)
                shapeRenderer.rect(px + 37, py + 14 - armSwing, 7f, 14f)
                renderToolInHand(px + 39, py + 14, d)
            }
            Direction.DOWN -> {
                shapeRenderer.rect(px + 4, py + 14 - armSwing, 7f, 14f)
                shapeRenderer.rect(px + 37, py + 14 + armSwing, 7f, 14f)
                renderToolInHand(px + 39, py + 12, d)
            }
            Direction.LEFT -> {
                shapeRenderer.rect(px + 5, py + 14, 7f, 14f)
                renderToolInHand(px + 1, py + 12, d)
            }
            Direction.RIGHT -> {
                shapeRenderer.rect(px + 36, py + 14, 7f, 14f)
                renderToolInHand(px + 39, py + 12, d)
            }
        }

        // Head
        shapeRenderer.color = Color(0.95f * d, 0.8f * d, 0.65f * d, 1f)
        shapeRenderer.rect(px + 13, py + 30, 22f, 16f)

        // Hair
        shapeRenderer.color = Color(0.35f * d, 0.2f * d, 0.1f * d, 1f)
        when (world.player.facing) {
            Direction.UP -> { shapeRenderer.rect(px + 12, py + 40, 24f, 8f); shapeRenderer.rect(px + 12, py + 32, 24f, 6f) }
            Direction.DOWN -> { shapeRenderer.rect(px + 12, py + 42, 24f, 6f); shapeRenderer.rect(px + 13, py + 40, 22f, 4f) }
            Direction.LEFT -> { shapeRenderer.rect(px + 12, py + 40, 24f, 8f); shapeRenderer.rect(px + 12, py + 30, 6f, 14f) }
            Direction.RIGHT -> { shapeRenderer.rect(px + 12, py + 40, 24f, 8f); shapeRenderer.rect(px + 30, py + 30, 6f, 14f) }
        }

        // Eyes
        when (world.player.facing) {
            Direction.DOWN -> {
                shapeRenderer.color = Color.WHITE
                shapeRenderer.rect(px + 16, py + 36, 6f, 4f)
                shapeRenderer.rect(px + 26, py + 36, 6f, 4f)
                shapeRenderer.color = Color(0.15f * d, 0.15f * d, 0.2f * d, 1f)
                shapeRenderer.rect(px + 18, py + 36, 3f, 3f)
                shapeRenderer.rect(px + 28, py + 36, 3f, 3f)
                shapeRenderer.color = Color(0.8f * d, 0.4f * d, 0.35f * d, 1f)
                shapeRenderer.rect(px + 20, py + 32, 8f, 2f)
            }
            Direction.LEFT -> {
                shapeRenderer.color = Color.WHITE
                shapeRenderer.rect(px + 14, py + 36, 6f, 4f)
                shapeRenderer.color = Color(0.15f * d, 0.15f * d, 0.2f * d, 1f)
                shapeRenderer.rect(px + 14, py + 36, 3f, 3f)
                shapeRenderer.color = Color(0.8f * d, 0.4f * d, 0.35f * d, 1f)
                shapeRenderer.rect(px + 13, py + 32, 6f, 2f)
            }
            Direction.RIGHT -> {
                shapeRenderer.color = Color.WHITE
                shapeRenderer.rect(px + 28, py + 36, 6f, 4f)
                shapeRenderer.color = Color(0.15f * d, 0.15f * d, 0.2f * d, 1f)
                shapeRenderer.rect(px + 31, py + 36, 3f, 3f)
                shapeRenderer.color = Color(0.8f * d, 0.4f * d, 0.35f * d, 1f)
                shapeRenderer.rect(px + 29, py + 32, 6f, 2f)
            }
            Direction.UP -> {}
        }

        // Hat
        shapeRenderer.color = Color(0.9f * d, 0.7f * d, 0.2f * d, 1f)
        shapeRenderer.rect(px + 8, py + 44, 32f, 4f)
        shapeRenderer.rect(px + 13, py + 47, 22f, 5f)

        // Fishing animation
        if (world.player.isFishing) {
            renderFishingLine(px, py, d)
        }

        shapeRenderer.end()
    }

    private fun renderFishingLine(px: Float, py: Float, d: Float) {
        val (fx, fy) = world.player.getFacingTile()
        val targetX = fx * tileSize + tileSize / 2
        val targetY = fy * tileSize + tileSize / 2

        // Fishing line
        shapeRenderer.color = Color(0.7f * d, 0.7f * d, 0.7f * d, 0.8f)
        val handX = px + 24f
        val handY = py + 28f
        // Simple line segments from hand to water
        val midX = (handX + targetX) / 2
        val midY = (handY + targetY) / 2 + 10f
        shapeRenderer.rect(handX, handY, 2f, midY - handY)
        shapeRenderer.rect(midX - 1, midY - 2f, targetX - midX + 2f, 2f)
        shapeRenderer.rect(targetX, targetY, 2f, midY - targetY)

        // Bobber
        val bobble = MathUtils.sin(animTimer * 3f) * 2f
        if (world.player.hasBite) {
            // Bite! Bobber dips
            shapeRenderer.color = Color(1f * d, 0.2f * d, 0.1f * d, 1f)
            shapeRenderer.rect(targetX - 3f, targetY - 4f + MathUtils.sin(animTimer * 10f) * 4f, 6f, 8f)
        } else {
            shapeRenderer.color = Color(1f * d, 0.3f * d, 0.1f * d, 1f)
            shapeRenderer.rect(targetX - 2f, targetY - 2f + bobble, 5f, 6f)
            // White top
            shapeRenderer.color = Color(0.9f * d, 0.9f * d, 0.9f * d, 1f)
            shapeRenderer.rect(targetX - 1f, targetY + 3f + bobble, 3f, 3f)
        }
    }

    private fun renderToolInHand(x: Float, y: Float, d: Float) {
        when (world.player.currentTool) {
            Tool.HOE -> {
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f)
                shapeRenderer.rect(x, y, 3f, 20f)
                shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.5f * d, 1f)
                shapeRenderer.rect(x - 3, y + 17, 9f, 4f)
            }
            Tool.WATERING_CAN -> {
                shapeRenderer.color = Color(0.3f * d, 0.4f * d, 0.8f * d, 1f)
                shapeRenderer.rect(x, y, 9f, 8f)
                shapeRenderer.rect(x + 7, y + 5, 5f, 3f)
            }
            Tool.AXE -> {
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f)
                shapeRenderer.rect(x + 1, y, 3f, 20f)
                shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.65f * d, 1f)
                shapeRenderer.rect(x - 2, y + 16, 9f, 5f)
            }
            Tool.PICKAXE -> {
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f)
                shapeRenderer.rect(x + 1, y, 3f, 20f)
                shapeRenderer.color = Color(0.6f * d, 0.6f * d, 0.65f * d, 1f)
                shapeRenderer.rect(x - 3, y + 17, 12f, 4f)
            }
            Tool.SEED_BAG -> {
                shapeRenderer.color = Color(0.6f * d, 0.5f * d, 0.3f * d, 1f)
                shapeRenderer.rect(x, y + 2, 7f, 10f)
                shapeRenderer.color = Color(0.3f * d, 0.7f * d, 0.2f * d, 1f)
                shapeRenderer.rect(x + 1, y + 10, 5f, 4f)
            }
            Tool.FISHING_ROD -> {
                // Rod handle
                shapeRenderer.color = Color(0.5f * d, 0.35f * d, 0.15f * d, 1f)
                shapeRenderer.rect(x, y, 3f, 24f)
                // Rod tip (thinner, lighter)
                shapeRenderer.color = Color(0.55f * d, 0.45f * d, 0.25f * d, 1f)
                shapeRenderer.rect(x + 1, y + 22f, 2f, 10f)
                // Reel
                shapeRenderer.color = Color(0.5f * d, 0.5f * d, 0.55f * d, 1f)
                shapeRenderer.rect(x - 1, y + 8, 5f, 4f)
            }
            Tool.HAND -> {}
        }
    }

    // ===== EFFECTS =====

    private fun renderFacingIndicator() {
        val (fx, fy) = world.player.getFacingTile()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        val pulse = (MathUtils.sin(animTimer * 3f) + 1) / 2
        shapeRenderer.color = Color(1f, 1f, 1f, 0.3f + 0.3f * pulse)
        shapeRenderer.rect(fx * tileSize + 1, fy * tileSize + 1, tileSize - 2, tileSize - 2)
        shapeRenderer.end()
    }

    private fun renderWeatherEffects() {
        if (raindrops.isEmpty()) return

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        when (world.weatherSystem.current) {
            Weather.RAINY, Weather.STORMY -> {
                for (drop in raindrops) {
                    shapeRenderer.color = Color(0.6f, 0.7f, 1f, 0.35f)
                    shapeRenderer.rect(drop[0], drop[1], 1.5f, 6f)
                }
                val alpha = if (world.weatherSystem.current == Weather.STORMY) 0.1f else 0.05f
                shapeRenderer.color = Color(0.05f, 0.05f, 0.05f, alpha)
                val hw = camera.viewportWidth * zoom / 2
                val hh = camera.viewportHeight * zoom / 2
                shapeRenderer.rect(camera.position.x - hw, camera.position.y - hh, hw * 2, hh * 2)
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

    private fun renderNightOverlay() {
        if (!world.timeSystem.isNight) return
        val nightAlpha = (1f - world.timeSystem.daylight) * 0.12f
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.02f, 0.02f, 0.05f, nightAlpha)
        val hw = camera.viewportWidth * zoom / 2
        val hh = camera.viewportHeight * zoom / 2
        shapeRenderer.rect(camera.position.x - hw, camera.position.y - hh, hw * 2, hh * 2)
        shapeRenderer.end()
    }

    private fun updateRain(delta: Float) {
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

    // ===== INPUT =====

    private fun handleMovementInput(delta: Float) {
        var dx = 0; var dy = 0
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy = 1
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy = -1
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx = -1
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx = 1
        if (dx != 0 || dy != 0) {
            if (world.player.isFishing) world.cancelFishing()
            world.player.tryMove(dx, dy, delta) { x, y -> world.isWalkable(x, y) }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.MINUS) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_SUBTRACT))
            zoom = (zoom + delta).coerceAtMost(3f)
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_ADD))
            zoom = (zoom - delta).coerceAtLeast(0.5f)
    }

    private fun handleActionInput() {
        // Tool selection
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) world.player.currentTool = Tool.HOE
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) world.player.currentTool = Tool.WATERING_CAN
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) world.player.currentTool = Tool.SEED_BAG
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) world.player.currentTool = Tool.HAND
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) world.player.currentTool = Tool.AXE
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) world.player.currentTool = Tool.PICKAXE
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) world.player.currentTool = Tool.FISHING_ROD

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) world.player.cycleSeeds(-1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) world.player.cycleSeeds(1)

        // Save/Load
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            hud.showSaveLoad = true; hud.saveMode = true; hud.saveCursor = 0
            closeOtherMenus("saveload")
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            hud.showSaveLoad = true; hud.saveMode = false; hud.saveCursor = 0
            closeOtherMenus("saveload")
        }

        // Save/Load UI input (check BEFORE toggle keys to avoid same-frame open/close)
        if (hud.showSaveLoad) { handleSaveLoadInput(); return }
        if (hud.showMissions) { handleMissionsInput(); return }
        if (hud.showAchievements) { handleAchievementsInput(); return }

        // Missions
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            hud.showMissions = true; hud.missionScroll = 0
            closeOtherMenus("missions")
            return
        }

        // Achievements
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            hud.showAchievements = true; hud.achievementScroll = 0
            closeOtherMenus("achievements")
            return
        }

        // Actions
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (hud.showShop || hud.showInventory) return
            if (world.isAtMarket() || world.isNearMerchant()) {
                hud.showShop = true; hud.shopCursor = 0; return
            }
            when (world.player.currentTool) {
                Tool.HOE -> world.tillGround()
                Tool.WATERING_CAN -> world.waterGround()
                Tool.SEED_BAG -> world.plantSeed()
                Tool.HAND -> world.harvest()
                Tool.AXE -> world.chopTree()
                Tool.PICKAXE -> world.mineRock()
                Tool.FISHING_ROD -> world.startFishing()
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            if (!hud.showInventory) {
                hud.showInventory = true; hud.inventoryCursor = 0
                closeOtherMenus("inventory")
                return
            }
        }

        if (hud.showInventory) {
            handleInventoryInput()
            return
        }

        if (hud.showShop) handleShopInput()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            hud.showShop = false; hud.showInventory = false
            hud.showSaveLoad = false; hud.showMissions = false
            hud.showAchievements = false
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            val items = world.player.inventory.filter { !it.type.isSeed && it.type.sellPrice > 0 }
            if (items.isNotEmpty()) {
                val item = items.first()
                if (world.economySystem.sellItem(world.player, item.type)) {
                    world.notify("${item.type.koreanName} 판매! (+${item.type.sellPrice}원)")
                    world.checkMissions()
                    world.checkAchievements()
                }
            }
        }
    }

    private fun closeOtherMenus(except: String) {
        if (except != "shop") hud.showShop = false
        if (except != "inventory") hud.showInventory = false
        if (except != "saveload") hud.showSaveLoad = false
        if (except != "missions") hud.showMissions = false
        if (except != "achievements") hud.showAchievements = false
    }

    private fun handleSaveLoadInput() {
        val slotInfos = com.farmgame.system.SaveSystem.getSlotInfos()
        val maxCursor = slotInfos.size - 1

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            hud.saveCursor = (hud.saveCursor - 1).coerceAtLeast(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            hud.saveCursor = (hud.saveCursor + 1).coerceAtMost(maxCursor)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            val selectedSlot = slotInfos[hud.saveCursor]
            val slot = selectedSlot.slot

            if (selectedSlot.isAutoSave && hud.saveMode) {
                world.notify("자동저장 슬롯에는 수동 저장할 수 없습니다.")
                return
            }

            if (hud.saveMode) {
                if (com.farmgame.system.SaveSystem.save(world, slot)) {
                    world.notify("슬롯 ${slot}에 저장했습니다!")
                } else {
                    world.notify("저장에 실패했습니다.")
                }
            } else {
                if (com.farmgame.system.SaveSystem.load(world, slot)) {
                    val label = if (selectedSlot.isAutoSave) "자동저장" else "슬롯 $slot"
                    world.notify("${label}에서 불러왔습니다!")
                    hud.showSaveLoad = false
                } else {
                    world.notify("빈 슬롯입니다.")
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) || Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
            if (hud.saveMode) {
                val selectedSlot = slotInfos[hud.saveCursor]
                if (selectedSlot.isAutoSave) {
                    world.notify("자동저장 슬롯은 삭제할 수 없습니다.")
                } else {
                    val slot = selectedSlot.slot
                    if (com.farmgame.system.SaveSystem.deleteSlot(slot)) {
                        world.notify("슬롯 ${slot} 삭제!")
                    }
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) hud.showSaveLoad = false
    }

    private fun handleMissionsInput() {
        val maxScroll = (com.farmgame.system.MissionSystem.allMissions.size - 12).coerceAtLeast(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            hud.missionScroll = (hud.missionScroll - 1).coerceAtLeast(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            hud.missionScroll = (hud.missionScroll + 1).coerceAtMost(maxScroll)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.J))
            hud.showMissions = false
    }

    private fun handleInventoryInput() {
        val items = world.player.inventory.filter { it.quantity > 0 }
        val maxCursor = (items.size - 1).coerceAtLeast(0)

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            hud.inventoryCursor = (hud.inventoryCursor - 1).coerceAtLeast(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            hud.inventoryCursor = (hud.inventoryCursor + 1).coerceAtMost(maxCursor)

        // Sell single item
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (items.isNotEmpty() && hud.inventoryCursor < items.size) {
                val item = items[hud.inventoryCursor]
                if (item.type.sellPrice > 0) {
                    if (world.economySystem.sellItem(world.player, item.type)) {
                        world.notify("${item.type.koreanName} 판매! (+${item.type.sellPrice}원)")
                        world.checkMissions()
                        world.checkAchievements()
                    }
                } else {
                    world.notify("이 아이템은 판매할 수 없습니다.")
                }
            }
        }

        // Sell all crops
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            val total = world.economySystem.sellAllCrops(world.player)
            if (total > 0) {
                world.notify("작물을 판매했습니다! (+${total}원)")
                world.checkMissions()
                world.checkAchievements()
            } else {
                world.notify("판매할 작물이 없습니다.")
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.I) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            hud.showInventory = false
    }

    private fun handleAchievementsInput() {
        val maxScroll = (com.farmgame.system.AchievementSystem.allAchievements.size - 12).coerceAtLeast(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            hud.achievementScroll = (hud.achievementScroll - 1).coerceAtLeast(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            hud.achievementScroll = (hud.achievementScroll + 1).coerceAtMost(maxScroll)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.K))
            hud.showAchievements = false
    }

    private fun handleShopInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) { hud.shopCategory = ((hud.shopCategory - 1) + 3) % 3; hud.shopCursor = 0 }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) { hud.shopCategory = (hud.shopCategory + 1) % 3; hud.shopCursor = 0 }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            hud.shopCursor = (hud.shopCursor - 1).coerceAtLeast(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            val max = when (hud.shopCategory) { 0 -> CropType.entries.size; 1 -> AnimalType.entries.size; 2 -> hud.getUpgradeList().size; else -> 0 }
            hud.shopCursor = (hud.shopCursor + 1).coerceAtMost(max - 1).coerceAtLeast(0)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            when (hud.shopCategory) {
                0 -> {
                    if (hud.shopCursor in CropType.entries.indices) {
                        val crop = CropType.entries[hud.shopCursor]
                        if (world.economySystem.buySeed(world.player, crop, 5)) world.notify("${crop.koreanName} 씨앗 5개 구매! (-${crop.seedPrice * 5}원)")
                        else world.notify("돈이 부족합니다!")
                    }
                }
                1 -> {
                    if (hud.shopCursor in AnimalType.entries.indices) {
                        world.addAnimal(AnimalType.entries[hud.shopCursor])
                    }
                }
                2 -> {
                    val upgrades = hud.getUpgradeList()
                    if (hud.shopCursor in upgrades.indices) upgrades[hud.shopCursor].third()
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    private fun emergencySave() {
        try {
            if (com.farmgame.system.SaveSystem.save(world, com.farmgame.system.SaveSystem.AUTO_SAVE_SLOT)) {
                println("긴급 자동 저장 완료!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dispose() {
        emergencySave()
        shapeRenderer.dispose(); batch.dispose(); hud.dispose()
    }
}
