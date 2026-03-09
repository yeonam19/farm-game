package com.farmgame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.farmgame.renderer.WorldRenderer
import com.farmgame.renderer.CharacterRenderer
import com.farmgame.renderer.EffectRenderer
import com.farmgame.input.InputHandler
import com.farmgame.system.AudioSystem
import com.farmgame.ui.FontManager
import com.farmgame.ui.GameHUD
import com.farmgame.world.GameWorld

class GameScreen : ScreenAdapter() {
    private val world = GameWorld()
    private val camera = OrthographicCamera()
    private val shapeRenderer = ShapeRenderer(40000)
    private val batch = SpriteBatch()
    private val hud = GameHUD(world)

    private val tileSize = 48f
    private var animTimer = 0f

    private val worldRenderer = WorldRenderer(shapeRenderer, world, tileSize)
    private val characterRenderer = CharacterRenderer(shapeRenderer, world, tileSize)
    private val effectRenderer = EffectRenderer(shapeRenderer, world, tileSize)
    private val inputHandler = InputHandler(world, hud)

    init {
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        AudioSystem.playBgm(world.timeSystem.season)

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
            try {
                val logFile = java.io.File("crash_log.txt")
                logFile.appendText("=== ${java.time.LocalDateTime.now()} ===\n${e.stackTraceToString()}\n\n")
            } catch (_: Exception) {}
            emergencySave()
        }
    }

    private fun doRender(delta: Float) {
        animTimer += delta
        worldRenderer.animTimer = animTimer
        characterRenderer.animTimer = animTimer
        effectRenderer.animTimer = animTimer

        val zoom = inputHandler.zoom

        if (!hud.showShop && !hud.showInventory && !hud.showSaveLoad && !hud.showMissions && !hud.showAchievements) {
            inputHandler.handleMovementInput(delta)
        }
        inputHandler.handleActionInput()
        world.update(delta)
        effectRenderer.updateRain(delta, camera, zoom)

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
        val camX = camera.position.x
        val camY = camera.position.y
        val viewW = camera.viewportWidth * zoom / 2 + tileSize * 2
        val viewH = camera.viewportHeight * zoom / 2 + tileSize * 2
        worldRenderer.renderTiles(daylight, camX, camY, viewW, viewH)
        worldRenderer.renderCrops(daylight)
        worldRenderer.renderAnimals(daylight)
        worldRenderer.renderAutoFishDevice(daylight)
        characterRenderer.renderNPCs(daylight)
        characterRenderer.renderFarmers(daylight)
        characterRenderer.renderHerders(daylight)
        characterRenderer.renderPlayer(daylight)
        effectRenderer.renderFacingIndicator()
        effectRenderer.renderWeatherEffects()
        effectRenderer.renderStormOverlay(camera, zoom)
        effectRenderer.renderNightOverlay(camera, zoom)

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
