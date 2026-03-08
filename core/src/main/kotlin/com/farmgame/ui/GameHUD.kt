package com.farmgame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.farmgame.data.*
import com.farmgame.system.AchievementSystem
import com.farmgame.system.MissionSystem
import com.farmgame.system.SaveSystem
import com.farmgame.world.GameWorld

class GameHUD(private val world: GameWorld) {
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()
    private val hudCamera = OrthographicCamera()

    private val fontLarge: BitmapFont get() = FontManager.fontLarge
    private val fontMedium: BitmapFont get() = FontManager.fontMedium
    private val fontSmall: BitmapFont get() = FontManager.fontSmall

    var showShop: Boolean = false
    var showInventory: Boolean = false
    var showSaveLoad: Boolean = false
    var showMissions: Boolean = false
    var showAchievements: Boolean = false
    var saveMode: Boolean = true // true=save, false=load
    var shopCategory: Int = 0
    var shopCursor: Int = 0
    var saveCursor: Int = 0
    var missionScroll: Int = 0
    var achievementScroll: Int = 0
    var inventoryCursor: Int = 0

    private fun updateHudCamera() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        hudCamera.setToOrtho(false, w, h)
        hudCamera.update()
    }

    fun render() {
        updateHudCamera()
        val screenW = Gdx.graphics.width.toFloat()
        val screenH = Gdx.graphics.height.toFloat()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.projectionMatrix = hudCamera.combined
        batch.projectionMatrix = hudCamera.combined

        // Top bar
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        shapeRenderer.rect(0f, screenH - 80f, screenW, 80f)
        shapeRenderer.end()

        batch.begin()

        // Time & Date
        fontMedium.color = Color.YELLOW
        fontMedium.draw(batch, world.timeSystem.getFullDate(), 20f, screenH - 15f)

        // Weather
        fontMedium.color = Color.CYAN
        fontMedium.draw(batch, "날씨: ${world.weatherSystem.current.koreanName}", 20f, screenH - 45f)

        // Level & EXP
        fontMedium.color = Color(0.5f, 1f, 0.5f, 1f)
        val expNeeded = MissionSystem.getExpForLevel(world.playerLevel)
        val title = MissionSystem.getLevelTitle(world.playerLevel)
        fontMedium.draw(batch, "Lv.${world.playerLevel} $title (${world.playerExp}/${expNeeded})", screenW / 2 - 140f, screenH - 15f)

        // Money
        fontLarge.color = Color.GOLD
        fontLarge.draw(batch, "${world.player.money}원", screenW - 200f, screenH - 12f)

        // Current tool
        fontMedium.color = Color.WHITE
        fontMedium.draw(batch, "도구: ${world.player.currentTool.koreanName}", screenW - 200f, screenH - 45f)

        // Selected seed info
        val seed = world.player.getSelectedSeed()
        if (seed != null && world.player.currentTool == Tool.SEED_BAG) {
            fontMedium.color = Color.GREEN
            fontMedium.draw(batch, "씨앗: ${seed.type.koreanName} (${seed.quantity}개)", screenW / 2 - 140f, screenH - 45f)
        }

        batch.end()

        // EXP bar
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val barW = 200f
        val barX = screenW / 2 - 140f
        val barY = screenH - 68f
        shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 0.8f)
        shapeRenderer.rect(barX, barY, barW, 6f)
        val ratio = (world.playerExp.toFloat() / expNeeded).coerceIn(0f, 1f)
        shapeRenderer.color = Color(0.3f, 0.9f, 0.3f, 0.9f)
        shapeRenderer.rect(barX, barY, barW * ratio, 6f)
        shapeRenderer.end()

        // Active mission display (top-left corner, below top bar)
        renderActiveMission(screenW, screenH)

        // Bottom tool bar
        renderToolBar(screenW, screenH)

        // Key help
        renderKeyHelp(screenW, screenH)

        // Notification
        if (world.notification.isNotEmpty()) {
            renderNotification(screenW, screenH)
        }

        // Achievement popup
        if (world.newAchievementId != null) {
            renderAchievementPopup(screenW, screenH)
        }

        // Overlays
        if (showShop) renderShop(screenW, screenH)
        if (showInventory) renderInventory(screenW, screenH)
        if (showSaveLoad) renderSaveLoad(screenW, screenH)
        if (showMissions) renderMissions(screenW, screenH)
        if (showAchievements) renderAchievements(screenW, screenH)
    }

    private fun renderActiveMission(screenW: Float, screenH: Float) {
        val missionId = world.activeMissionId
        val mission = if (missionId != null) MissionSystem.allMissions.find { it.id == missionId } else null

        val panelW = 300f
        val panelH = if (mission != null) 88f else 40f
        val px = 10f
        val py = screenH - 90f - panelH

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.1f, 0.08f, 0.18f, 0.75f)
        shapeRenderer.rect(px, py, panelW, panelH)

        if (mission != null) {
            val progress = world.missionProgress[mission.id] ?: 0
            val ratio = (progress.toFloat() / mission.target).coerceIn(0f, 1f)

            // Progress bar background
            shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 0.8f)
            shapeRenderer.rect(px + 10f, py + 8f, panelW - 20f, 8f)
            // Progress bar fill
            shapeRenderer.color = Color(0.9f, 0.7f, 0.2f, 0.9f)
            shapeRenderer.rect(px + 10f, py + 8f, (panelW - 20f) * ratio, 8f)
        }
        shapeRenderer.end()

        batch.begin()
        if (mission != null) {
            fontSmall.color = Color(0.8f, 0.6f, 1f, 1f)
            fontSmall.draw(batch, "현재 미션", px + 10f, py + panelH - 6f)

            fontMedium.color = Color.WHITE
            fontMedium.draw(batch, mission.name, px + 80f, py + panelH - 6f)

            fontSmall.color = Color(0.8f, 0.8f, 0.7f, 0.9f)
            fontSmall.draw(batch, mission.description, px + 10f, py + panelH - 26f)

            val progress = world.missionProgress[mission.id] ?: 0
            fontSmall.color = Color(0.9f, 0.8f, 0.4f, 1f)
            fontSmall.draw(batch, "$progress/${mission.target}", px + panelW - 70f, py + panelH - 26f)

            // Reward info
            val rewardParts = mutableListOf<String>()
            rewardParts.add("+${mission.expReward}EXP")
            rewardParts.add("+${mission.moneyReward}원")
            for (item in mission.itemRewards) {
                try {
                    val itemType = ItemType.valueOf(item.itemName)
                    rewardParts.add("+${itemType.koreanName}x${item.quantity}")
                } catch (_: Exception) {}
            }
            fontSmall.color = Color(0.6f, 0.85f, 0.4f, 0.9f)
            fontSmall.draw(batch, "보상: ${rewardParts.joinToString(" ")}", px + 10f, py + panelH - 44f)
        } else {
            fontSmall.color = Color(0.5f, 0.9f, 0.5f, 1f)
            fontSmall.draw(batch, "모든 미션 완료!", px + 10f, py + panelH - 12f)
        }
        batch.end()
    }

    private fun renderToolBar(screenW: Float, screenH: Float) {
        val tools = Tool.entries
        val barWidth = tools.size * 80f
        val startX = (screenW - barWidth) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.6f)
        shapeRenderer.rect(startX - 10f, 5f, barWidth + 20f, 55f)

        for ((i, tool) in tools.withIndex()) {
            val x = startX + i * 80f
            if (tool == world.player.currentTool) {
                shapeRenderer.color = Color(0.8f, 0.5f, 0.1f, 0.7f)
                shapeRenderer.rect(x + 1, 8f, 72f, 48f)
            }
        }
        shapeRenderer.end()

        batch.begin()
        for ((i, tool) in tools.withIndex()) {
            val x = startX + i * 80f
            val isSelected = tool == world.player.currentTool
            fontSmall.color = if (isSelected) Color.WHITE else Color(0.7f, 0.7f, 0.7f, 1f)
            fontSmall.draw(batch, "${i + 1}", x + 5f, 52f)
            fontSmall.draw(batch, tool.koreanName, x + 3f, 30f)
        }
        batch.end()
    }

    private fun renderKeyHelp(screenW: Float, screenH: Float) {
        val helpLines = listOf(
            "[WASD] 이동",
            "[Space/E] 도구 사용 / 대화 / 상점",
            "[1~7] 도구 선택 (7=낚싯대)",
            "[[ ]] 씨앗 변경",
            "[I] 인벤토리  [J] 미션  [K] 업적",
            "[M] 작물 전체 판매",
            "[F] 아이템 빠른 판매",
            "[F5] 저장  [F9] 불러오기",
            "[+/-] 줌 인/아웃",
            "[ESC] 메뉴 닫기"
        )

        val lineHeight = 18f
        val panelH = helpLines.size * lineHeight + 16f
        val panelW = 280f
        val px = 10f
        val py = 70f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.45f)
        shapeRenderer.rect(px, py, panelW, panelH)
        shapeRenderer.end()

        batch.begin()
        fontSmall.color = Color(0.8f, 0.8f, 0.8f, 0.9f)
        for ((i, line) in helpLines.reversed().withIndex()) {
            fontSmall.draw(batch, line, px + 8f, py + 10f + (i + 1) * lineHeight)
        }
        batch.end()
    }

    private fun renderNotification(screenW: Float, screenH: Float) {
        val msg = world.notification
        layout.setText(fontMedium, msg)
        val x = (screenW - layout.width) / 2
        val y = screenH / 2 - 100f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        shapeRenderer.rect(x - 15f, y - layout.height - 10f, layout.width + 30f, layout.height + 20f)
        shapeRenderer.end()

        batch.begin()
        fontMedium.color = Color.WHITE
        fontMedium.draw(batch, msg, x, y)
        batch.end()
    }

    // ===== ITEM ICON RENDERING =====

    fun drawItemIcon(sr: ShapeRenderer, type: ItemType, x: Float, y: Float, size: Float) {
        val s = size
        when (type) {
            // Seeds
            ItemType.STRAWBERRY_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.8f, 0.2f, 0.1f, 1f); sr.rect(x+s*0.3f, y+s*0.2f, s*0.4f, s*0.3f) }
            ItemType.TOMATO_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.9f, 0.15f, 0.1f, 1f); sr.rect(x+s*0.3f, y+s*0.2f, s*0.4f, s*0.4f) }
            ItemType.CORN_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.95f, 0.85f, 0.2f, 1f); sr.rect(x+s*0.3f, y+s*0.1f, s*0.3f, s*0.6f) }
            ItemType.POTATO_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.65f, 0.45f, 0.25f, 1f); sr.rect(x+s*0.2f, y+s*0.2f, s*0.5f, s*0.4f) }
            ItemType.CARROT_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.95f, 0.5f, 0.1f, 1f); sr.rect(x+s*0.35f, y+s*0.1f, s*0.3f, s*0.5f) }
            ItemType.CABBAGE_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.4f, 0.8f, 0.3f, 1f); sr.rect(x+s*0.2f, y+s*0.2f, s*0.6f, s*0.5f) }
            ItemType.RADISH_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.9f, 0.9f, 0.85f, 1f); sr.rect(x+s*0.3f, y+s*0.1f, s*0.35f, s*0.55f) }
            ItemType.SPINACH_SEED -> { sr.color = Color(0.2f, 0.6f, 0.2f, 1f); sr.rect(x, y, s, s); sr.color = Color(0.15f, 0.55f, 0.15f, 1f); sr.rect(x+s*0.15f, y+s*0.2f, s*0.7f, s*0.5f) }

            // Harvested crops
            ItemType.STRAWBERRY -> { sr.color = Color(0.9f, 0.15f, 0.1f, 1f); sr.rect(x+s*0.15f, y+s*0.1f, s*0.7f, s*0.7f); sr.color = Color(0.2f, 0.6f, 0.15f, 1f); sr.rect(x+s*0.25f, y+s*0.7f, s*0.5f, s*0.25f); sr.color = Color(0.95f, 0.8f, 0.2f, 1f); sr.rect(x+s*0.35f, y+s*0.35f, s*0.1f, s*0.1f); sr.rect(x+s*0.55f, y+s*0.45f, s*0.1f, s*0.1f) }
            ItemType.TOMATO -> { sr.color = Color(0.9f, 0.12f, 0.08f, 1f); sr.rect(x+s*0.1f, y+s*0.1f, s*0.8f, s*0.7f); sr.color = Color(0.15f, 0.45f, 0.1f, 1f); sr.rect(x+s*0.3f, y+s*0.75f, s*0.4f, s*0.2f); sr.color = Color(1f, 0.3f, 0.2f, 0.5f); sr.rect(x+s*0.25f, y+s*0.45f, s*0.2f, s*0.2f) }
            ItemType.CORN -> { sr.color = Color(0.95f, 0.85f, 0.2f, 1f); sr.rect(x+s*0.25f, y+s*0.05f, s*0.5f, s*0.75f); sr.color = Color(0.3f, 0.55f, 0.15f, 1f); sr.rect(x+s*0.15f, y+s*0.6f, s*0.7f, s*0.35f) }
            ItemType.POTATO -> { sr.color = Color(0.65f, 0.48f, 0.28f, 1f); sr.rect(x+s*0.1f, y+s*0.15f, s*0.8f, s*0.6f); sr.color = Color(0.55f, 0.4f, 0.22f, 0.6f); sr.rect(x+s*0.3f, y+s*0.35f, s*0.15f, s*0.1f); sr.rect(x+s*0.6f, y+s*0.45f, s*0.1f, s*0.1f) }
            ItemType.CARROT -> { sr.color = Color(0.95f, 0.5f, 0.1f, 1f); sr.rect(x+s*0.3f, y+s*0.05f, s*0.4f, s*0.7f); sr.color = Color(0.2f, 0.6f, 0.15f, 1f); sr.rect(x+s*0.2f, y+s*0.65f, s*0.6f, s*0.3f) }
            ItemType.CABBAGE -> { sr.color = Color(0.3f, 0.65f, 0.2f, 1f); sr.rect(x+s*0.05f, y+s*0.1f, s*0.9f, s*0.75f); sr.color = Color(0.5f, 0.8f, 0.4f, 1f); sr.rect(x+s*0.2f, y+s*0.25f, s*0.6f, s*0.5f); sr.color = Color(0.65f, 0.9f, 0.55f, 1f); sr.rect(x+s*0.35f, y+s*0.35f, s*0.3f, s*0.25f) }
            ItemType.RADISH -> { sr.color = Color(0.92f, 0.92f, 0.87f, 1f); sr.rect(x+s*0.25f, y+s*0.05f, s*0.5f, s*0.7f); sr.color = Color(0.2f, 0.6f, 0.15f, 1f); sr.rect(x+s*0.2f, y+s*0.65f, s*0.6f, s*0.3f) }
            ItemType.SPINACH -> { sr.color = Color(0.15f, 0.5f, 0.12f, 1f); sr.rect(x+s*0.05f, y+s*0.1f, s*0.9f, s*0.7f); sr.color = Color(0.2f, 0.6f, 0.16f, 1f); sr.rect(x+s*0.15f, y+s*0.3f, s*0.35f, s*0.35f); sr.rect(x+s*0.5f, y+s*0.2f, s*0.35f, s*0.4f) }

            // Resources
            ItemType.WOOD -> { sr.color = Color(0.45f, 0.3f, 0.12f, 1f); sr.rect(x+s*0.15f, y+s*0.1f, s*0.7f, s*0.7f); sr.color = Color(0.55f, 0.38f, 0.18f, 1f); sr.rect(x+s*0.25f, y+s*0.2f, s*0.5f, s*0.5f); sr.color = Color(0.35f, 0.22f, 0.08f, 1f); sr.rect(x+s*0.4f, y+s*0.35f, s*0.2f, s*0.2f) }
            ItemType.STONE -> { sr.color = Color(0.55f, 0.52f, 0.5f, 1f); sr.rect(x+s*0.1f, y+s*0.1f, s*0.8f, s*0.65f); sr.color = Color(0.65f, 0.62f, 0.58f, 0.6f); sr.rect(x+s*0.2f, y+s*0.35f, s*0.35f, s*0.2f) }

            // Fish
            ItemType.FISH_SMALL -> { sr.color = Color(0.6f, 0.55f, 0.4f, 1f); sr.rect(x+s*0.1f, y+s*0.25f, s*0.7f, s*0.4f); sr.color = Color(0.5f, 0.45f, 0.3f, 1f); sr.rect(x+s*0.7f, y+s*0.15f, s*0.2f, s*0.6f); sr.color = Color(0.1f, 0.1f, 0.1f, 1f); sr.rect(x+s*0.2f, y+s*0.5f, s*0.08f, s*0.08f) }
            ItemType.FISH_MEDIUM -> { sr.color = Color(0.4f, 0.5f, 0.35f, 1f); sr.rect(x+s*0.05f, y+s*0.2f, s*0.75f, s*0.5f); sr.color = Color(0.35f, 0.45f, 0.3f, 1f); sr.rect(x+s*0.7f, y+s*0.1f, s*0.25f, s*0.7f); sr.color = Color(0.1f, 0.1f, 0.1f, 1f); sr.rect(x+s*0.15f, y+s*0.5f, s*0.1f, s*0.1f) }
            ItemType.FISH_LARGE -> { sr.color = Color(0.7f, 0.4f, 0.35f, 1f); sr.rect(x+s*0.05f, y+s*0.15f, s*0.75f, s*0.55f); sr.color = Color(0.6f, 0.35f, 0.3f, 1f); sr.rect(x+s*0.7f, y+s*0.05f, s*0.25f, s*0.8f); sr.color = Color(0.1f, 0.1f, 0.1f, 1f); sr.rect(x+s*0.15f, y+s*0.5f, s*0.1f, s*0.1f) }
            ItemType.FISH_RARE -> { sr.color = Color(1f, 0.7f, 0.1f, 1f); sr.rect(x+s*0.1f, y+s*0.2f, s*0.65f, s*0.5f); sr.color = Color(0.95f, 0.55f, 0.05f, 1f); sr.rect(x+s*0.65f, y+s*0.1f, s*0.25f, s*0.7f); sr.color = Color(0.1f, 0.1f, 0.1f, 1f); sr.rect(x+s*0.2f, y+s*0.5f, s*0.1f, s*0.1f) }
            ItemType.FISH_TRASH -> { sr.color = Color(0.4f, 0.35f, 0.3f, 1f); sr.rect(x+s*0.15f, y+s*0.05f, s*0.5f, s*0.9f); sr.color = Color(0.35f, 0.3f, 0.25f, 1f); sr.rect(x+s*0.1f, y+s*0.7f, s*0.6f, s*0.25f) }
        }
    }

    // ===== SHOP =====

    fun renderShop(screenW: Float, screenH: Float) {
        val panelW = 520f
        val panelH = 480f
        val px = (screenW - panelW) / 2
        val py = (screenH - panelH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.15f, 0.12f, 0.1f, 0.95f)
        shapeRenderer.rect(px, py, panelW, panelH)
        shapeRenderer.color = Color(0.3f, 0.2f, 0.1f, 1f)
        shapeRenderer.rect(px, py + panelH - 45f, panelW, 45f)
        val tabW = panelW / 3
        for (i in 0..2) {
            shapeRenderer.color = if (shopCategory == i) Color(0.4f, 0.3f, 0.15f, 1f) else Color(0.25f, 0.18f, 0.1f, 1f)
            shapeRenderer.rect(px + i * tabW, py + panelH - 90f, tabW - 2f, 42f)
        }
        shapeRenderer.end()

        batch.begin()
        fontLarge.color = Color.GOLD
        fontLarge.draw(batch, "상점  (보유: ${world.player.money}원)", px + 15f, py + panelH - 10f)

        val tabs = listOf("씨앗", "동물", "업그레이드")
        for (i in tabs.indices) {
            fontMedium.color = if (shopCategory == i) Color.YELLOW else Color.GRAY
            fontMedium.draw(batch, tabs[i], px + i * tabW + 15f, py + panelH - 55f)
        }

        val startY = py + panelH - 110f
        when (shopCategory) {
            0 -> renderSeedShop(px, startY)
            1 -> renderAnimalShop(px, startY)
            2 -> renderUpgradeShop(px, startY)
        }

        fontSmall.color = Color.LIGHT_GRAY
        fontSmall.draw(batch, "[Q/E] 카테고리  [W/S] 선택  [Enter] 구매  [ESC] 닫기", px + 15f, py + 25f)
        batch.end()
    }

    private fun renderSeedShop(px: Float, startY: Float) {
        CropType.entries.forEachIndexed { i, crop ->
            val y = startY - i * 35f
            fontMedium.color = if (shopCursor == i) Color.YELLOW else Color.WHITE
            val marker = if (shopCursor == i) "> " else "  "
            val seasonInfo = "(${crop.season.koreanName})"
            fontMedium.draw(batch, "${marker}${crop.koreanName} 씨앗 $seasonInfo - ${crop.seedPrice}원", px + 20f, y)
        }
    }

    private fun renderAnimalShop(px: Float, startY: Float) {
        AnimalType.entries.forEachIndexed { i, animal ->
            val y = startY - i * 35f
            fontMedium.color = if (shopCursor == i) Color.YELLOW else Color.WHITE
            val marker = if (shopCursor == i) "> " else "  "
            fontMedium.draw(batch, "${marker}${animal.koreanName} - ${animal.buyPrice}원 (${animal.productName} 생산)", px + 20f, y)
        }
    }

    // Single source of truth for upgrade list (used by both render and action)
    fun getUpgradeList(): List<Triple<String, Int, () -> Boolean>> {
        val upgrades = mutableListOf<Triple<String, Int, () -> Boolean>>()

        val nextTool = Upgrades.toolUpgrades[world.player.toolLevel + 1]
        if (nextTool != null) upgrades.add(Triple("${nextTool.koreanName} - ${nextTool.cost}원", nextTool.cost) {
            if (world.economySystem.buyUpgrade(world.player, nextTool.cost)) {
                world.player.toolLevel++; world.checkMissions(); world.notify("${nextTool.koreanName} 완료!"); true
            } else { world.notify("돈이 부족합니다!"); false }
        })

        val nextLand = Upgrades.landExpansions[world.player.landLevel + 1]
        if (nextLand != null) upgrades.add(Triple("${nextLand.koreanName} - ${nextLand.cost}원", nextLand.cost) {
            world.expandLand()
        })

        val nextSprinkler = Upgrades.wateringCanUpgrades[world.player.sprinklerLevel + 1]
        if (nextSprinkler != null) upgrades.add(Triple("${nextSprinkler.koreanName} - ${nextSprinkler.cost}원", nextSprinkler.cost) {
            if (world.economySystem.buyUpgrade(world.player, nextSprinkler.cost)) {
                world.player.sprinklerLevel++; world.notify("${nextSprinkler.koreanName} 완료!"); true
            } else { world.notify("돈이 부족합니다!"); false }
        })

        if (!world.player.hasOx) {
            val oxCost = 3000
            upgrades.add(Triple("농사용 소 - ${oxCost}원 (괭이 6칸 경작)", oxCost) {
                if (world.economySystem.buyUpgrade(world.player, oxCost)) {
                    world.player.hasOx = true; world.notify("농사용 소를 구입했습니다! 괭이로 6칸씩 경작 가능!"); true
                } else { world.notify("돈이 부족합니다!"); false }
            })
        }

        if (!world.player.hasSickle) {
            val sickleCost = 4000
            upgrades.add(Triple("수확용 낫 - ${sickleCost}원 (3x3 동시 수확)", sickleCost) {
                if (world.economySystem.buyUpgrade(world.player, sickleCost)) {
                    world.player.hasSickle = true; world.notify("수확용 낫을 구입했습니다! 3x3 범위 동시 수확 가능!"); true
                } else { world.notify("돈이 부족합니다!"); false }
            })
        }

        if (world.autoFishLevel < 3) {
            val nextLevel = world.autoFishLevel + 1
            val (name, cost, desc) = when (nextLevel) {
                1 -> Triple("기본 통발 설치", 2000, "15초마다 자동 낚시")
                2 -> Triple("고급 통발 설치", 8000, "8초마다 자동 낚시")
                3 -> Triple("자동 낚시선", 25000, "4초마다 자동 낚시")
                else -> Triple("", 0, "")
            }
            upgrades.add(Triple("$name - ${cost}원 ($desc)", cost) {
                if (world.economySystem.buyUpgrade(world.player, cost)) {
                    world.autoFishLevel = nextLevel
                    world.notify("${name} 완료! 연못에서 자동으로 물고기를 잡습니다!"); true
                } else { world.notify("돈이 부족합니다!"); false }
            })
        }

        if (world.farmerLevel < 3) {
            val nextLevel = world.farmerLevel + 1
            val (name, cost, desc) = when (nextLevel) {
                1 -> Triple("농부 철수 고용", 5000, "초급 농부 추가")
                2 -> Triple("농부 영희 추가고용", 15000, "중급 농부 추가 (철수+영희)")
                3 -> Triple("달인 농부 추가고용", 40000, "고급 농부 추가 (3명 동시)")
                else -> Triple("", 0, "")
            }
            upgrades.add(Triple("$name - ${cost}원 ($desc)", cost) {
                if (world.economySystem.buyUpgrade(world.player, cost)) {
                    world.farmerLevel = nextLevel
                    world.rebuildWorkers()
                    world.notify("농부를 고용했습니다! 현재 ${world.farmers.size}명이 함께 일합니다!"); true
                } else { world.notify("돈이 부족합니다!"); false }
            })
        }

        if (world.herderLevel < 3) {
            val nextLevel = world.herderLevel + 1
            val (name, cost, desc) = when (nextLevel) {
                1 -> Triple("목동 민호 고용", 4000, "초급 목동 추가")
                2 -> Triple("목동 수진 추가고용", 12000, "중급 목동 추가 (민호+수진)")
                3 -> Triple("목축달인 추가고용", 30000, "고급 목동 추가 (3명 동시)")
                else -> Triple("", 0, "")
            }
            upgrades.add(Triple("$name - ${cost}원 ($desc)", cost) {
                if (world.economySystem.buyUpgrade(world.player, cost)) {
                    world.herderLevel = nextLevel
                    world.rebuildWorkers()
                    world.notify("목동을 고용했습니다! 현재 ${world.herders.size}명이 함께 일합니다!"); true
                } else { world.notify("돈이 부족합니다!"); false }
            })
        }

        return upgrades
    }

    private fun renderUpgradeShop(px: Float, startY: Float) {
        val upgrades = getUpgradeList()
        if (upgrades.isEmpty()) {
            fontMedium.color = Color.GRAY
            fontMedium.draw(batch, "모든 업그레이드를 완료했습니다!", px + 20f, startY)
        } else {
            // Clamp cursor to valid range
            shopCursor = shopCursor.coerceIn(0, (upgrades.size - 1).coerceAtLeast(0))
            upgrades.forEachIndexed { i, (text, _, _) ->
                val y = startY - i * 35f
                fontMedium.color = if (shopCursor == i) Color.YELLOW else Color.WHITE
                val marker = if (shopCursor == i) "> " else "  "
                fontMedium.draw(batch, "$marker$text", px + 20f, y)
            }
        }
    }

    // ===== INVENTORY WITH ICONS =====

    private fun renderInventory(screenW: Float, screenH: Float) {
        val panelW = 460f
        val items = world.player.inventory.filter { it.quantity > 0 }
        val panelH = (items.size * 32f + 120f).coerceAtLeast(170f)
        val px = (screenW - panelW) / 2
        val py = (screenH - panelH) / 2

        if (items.isNotEmpty()) {
            inventoryCursor = inventoryCursor.coerceIn(0, items.size - 1)
        }

        // Background shapes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.1f, 0.15f, 0.1f, 0.95f)
        shapeRenderer.rect(px, py, panelW, panelH)
        shapeRenderer.color = Color(0.2f, 0.35f, 0.2f, 1f)
        shapeRenderer.rect(px, py + panelH - 45f, panelW, 45f)

        // Cursor highlight + Item icons
        if (items.isNotEmpty()) {
            for ((i, item) in items.withIndex()) {
                val iy = py + panelH - 65f - i * 32f
                if (i == inventoryCursor) {
                    shapeRenderer.color = Color(0.3f, 0.5f, 0.3f, 0.5f)
                    shapeRenderer.rect(px + 10f, iy - 16f, panelW - 20f, 30f)
                }
                drawItemIcon(shapeRenderer, item.type, px + 20f, iy - 14f, 20f)
            }
        }
        shapeRenderer.end()

        batch.begin()
        fontLarge.color = Color.GREEN
        fontLarge.draw(batch, "인벤토리", px + 15f, py + panelH - 10f)

        if (items.isEmpty()) {
            fontMedium.color = Color.GRAY
            fontMedium.draw(batch, "아이템이 없습니다", px + 50f, py + panelH - 65f)
        } else {
            items.forEachIndexed { i, item ->
                val y = py + panelH - 60f - i * 32f
                val isSelected = i == inventoryCursor
                fontMedium.color = when {
                    isSelected -> Color.YELLOW
                    item.type.isSeed -> Color.GREEN
                    else -> Color.WHITE
                }
                val marker = if (isSelected) "> " else "  "
                fontMedium.draw(batch, "$marker${item.type.koreanName} x${item.quantity}", px + 48f, y)
                if (item.type.sellPrice > 0) {
                    fontMedium.color = if (isSelected) Color.YELLOW else Color.GOLD
                    fontMedium.draw(batch, "(${item.type.sellPrice}원)", px + 320f, y)
                }
            }
        }

        fontSmall.color = Color.LIGHT_GRAY
        fontSmall.draw(batch, "[W/S] 선택  [Enter] 1개 판매  [M] 전체 판매  [I/ESC] 닫기", px + 15f, py + 15f)
        batch.end()
    }

    // ===== SAVE/LOAD =====

    private fun renderSaveLoad(screenW: Float, screenH: Float) {
        val panelW = 520f
        val panelH = 510f
        val px = (screenW - panelW) / 2
        val py = (screenH - panelH) / 2

        val slots = SaveSystem.getSlotInfos()  // 10 manual + 1 auto-save = 11

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.12f, 0.12f, 0.18f, 0.95f)
        shapeRenderer.rect(px, py, panelW, panelH)
        shapeRenderer.color = Color(0.2f, 0.2f, 0.35f, 1f)
        shapeRenderer.rect(px, py + panelH - 50f, panelW, 50f)

        // Slot highlight
        for ((i, slot) in slots.withIndex()) {
            val sy = py + panelH - 70f - i * 36f
            if (i == saveCursor) {
                shapeRenderer.color = Color(0.3f, 0.3f, 0.55f, 0.7f)
                shapeRenderer.rect(px + 10f, sy - 18f, panelW - 20f, 34f)
            }
            if (slot.isAutoSave) {
                // Separator line before auto-save
                shapeRenderer.color = Color(0.4f, 0.4f, 0.5f, 0.5f)
                shapeRenderer.rect(px + 15f, sy + 16f, panelW - 30f, 1f)
            }
            if (slot.exists) {
                shapeRenderer.color = if (slot.isAutoSave) Color(0.35f, 0.25f, 0.15f, 0.4f) else Color(0.2f, 0.35f, 0.2f, 0.4f)
                shapeRenderer.rect(px + 12f, sy - 16f, 8f, 8f)
            }
        }
        shapeRenderer.end()

        batch.begin()
        fontLarge.color = if (saveMode) Color(0.5f, 0.8f, 1f, 1f) else Color(1f, 0.8f, 0.5f, 1f)
        fontLarge.draw(batch, if (saveMode) "게임 저장" else "게임 불러오기", px + 15f, py + panelH - 12f)

        for ((i, slot) in slots.withIndex()) {
            val sy = py + panelH - 65f - i * 36f
            val label = if (slot.isAutoSave) {
                "  자동저장: ${if (slot.exists) slot.summary.removePrefix("자동저장: ") else "없음"}"
            } else {
                "${if (i == saveCursor) "> " else "  "}슬롯 ${slot.slot}: ${slot.summary}"
            }
            fontMedium.color = when {
                i == saveCursor -> Color.YELLOW
                slot.isAutoSave -> Color(1f, 0.8f, 0.5f, 1f)
                slot.exists -> Color.WHITE
                else -> Color.GRAY
            }
            fontMedium.draw(batch, label, px + 25f, sy)
        }

        fontSmall.color = Color.LIGHT_GRAY
        val helpText = if (saveMode) "[W/S] 선택  [Enter] 저장  [Del] 삭제  [ESC] 닫기"
                        else "[W/S] 선택  [Enter] 불러오기  [ESC] 닫기"
        fontSmall.draw(batch, helpText, px + 15f, py + 18f)
        batch.end()
    }

    // ===== MISSIONS =====

    private fun renderMissions(screenW: Float, screenH: Float) {
        val panelW = 560f
        val panelH = 520f
        val px = (screenW - panelW) / 2
        val py = (screenH - panelH) / 2

        val missions = MissionSystem.allMissions

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.12f, 0.1f, 0.15f, 0.95f)
        shapeRenderer.rect(px, py, panelW, panelH)
        shapeRenderer.color = Color(0.25f, 0.18f, 0.3f, 1f)
        shapeRenderer.rect(px, py + panelH - 50f, panelW, 50f)

        // Mission status icons
        val visibleStart = missionScroll
        val visibleEnd = (missionScroll + 12).coerceAtMost(missions.size)
        for (i in visibleStart until visibleEnd) {
            val mission = missions[i]
            val displayIdx = i - visibleStart
            val my = py + panelH - 75f - displayIdx * 35f
            val isCompleted = world.completedMissions[mission.id] == true

            if (isCompleted) {
                shapeRenderer.color = Color(0.15f, 0.35f, 0.15f, 0.4f)
                shapeRenderer.rect(px + 10f, my - 18f, panelW - 20f, 33f)
            }

            // Progress bar
            val progress = world.missionProgress[mission.id] ?: 0
            val ratio = (progress.toFloat() / mission.target).coerceIn(0f, 1f)
            shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 0.5f)
            shapeRenderer.rect(px + panelW - 140f, my - 14f, 120f, 8f)
            shapeRenderer.color = if (isCompleted) Color(0.3f, 0.8f, 0.3f, 0.8f) else Color(0.8f, 0.6f, 0.2f, 0.8f)
            shapeRenderer.rect(px + panelW - 140f, my - 14f, 120f * ratio, 8f)
        }
        shapeRenderer.end()

        batch.begin()
        fontLarge.color = Color(0.8f, 0.6f, 1f, 1f)
        fontLarge.draw(batch, "미션 (${world.completedMissions.size}/${missions.size})", px + 15f, py + panelH - 12f)

        // Level info
        fontMedium.color = Color(0.5f, 1f, 0.5f, 1f)
        fontMedium.draw(batch, "Lv.${world.playerLevel} ${MissionSystem.getLevelTitle(world.playerLevel)}", px + panelW - 250f, py + panelH - 15f)

        for (i in visibleStart until visibleEnd) {
            val mission = missions[i]
            val displayIdx = i - visibleStart
            val my = py + panelH - 70f - displayIdx * 35f
            val isCompleted = world.completedMissions[mission.id] == true
            val progress = world.missionProgress[mission.id] ?: 0

            if (isCompleted) {
                fontMedium.color = Color(0.5f, 0.9f, 0.5f, 1f)
                fontSmall.color = Color(0.4f, 0.7f, 0.4f, 0.8f)
            } else {
                fontMedium.color = Color.WHITE
                fontSmall.color = Color.LIGHT_GRAY
            }

            val checkMark = if (isCompleted) "[V] " else "[ ] "
            fontMedium.draw(batch, "$checkMark${mission.name}", px + 20f, my)
            fontSmall.draw(batch, mission.description, px + 22f, my - 16f)

            // Progress text
            fontSmall.color = if (isCompleted) Color(0.4f, 0.8f, 0.4f, 1f) else Color(0.8f, 0.7f, 0.4f, 1f)
            fontSmall.draw(batch, "$progress/${mission.target}", px + panelW - 135f, my - 4f)

            // Rewards
            fontSmall.color = Color(0.6f, 0.6f, 0.6f, 0.8f)
            fontSmall.draw(batch, "+${mission.expReward}EXP +${mission.moneyReward}원", px + 200f, my - 16f)
        }

        fontSmall.color = Color.LIGHT_GRAY
        fontSmall.draw(batch, "[W/S] 스크롤  [J/ESC] 닫기", px + 15f, py + 18f)
        batch.end()
    }

    // ===== ACHIEVEMENT POPUP =====

    private fun renderAchievementPopup(screenW: Float, screenH: Float) {
        val achievement = AchievementSystem.getById(world.newAchievementId ?: return) ?: return
        val popW = 320f
        val popH = 50f
        val px = (screenW - popW) / 2
        val py = screenH - 160f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.15f, 0.1f, 0.05f, 0.9f)
        shapeRenderer.rect(px, py, popW, popH)
        // Gold border
        shapeRenderer.color = Color(0.85f, 0.7f, 0.2f, 0.9f)
        shapeRenderer.rect(px, py, popW, 3f)
        shapeRenderer.rect(px, py + popH - 3f, popW, 3f)
        shapeRenderer.rect(px, py, 3f, popH)
        shapeRenderer.rect(px + popW - 3f, py, 3f, popH)
        shapeRenderer.end()

        batch.begin()
        fontMedium.color = Color.GOLD
        fontMedium.draw(batch, "업적 달성!", px + 15f, py + popH - 8f)
        fontSmall.color = Color.WHITE
        fontSmall.draw(batch, "${achievement.name} - ${achievement.description}", px + 15f, py + popH - 30f)
        batch.end()
    }

    // ===== ACHIEVEMENTS =====

    private fun renderAchievements(screenW: Float, screenH: Float) {
        val panelW = 560f
        val panelH = 520f
        val px = (screenW - panelW) / 2
        val py = (screenH - panelH) / 2

        val achievements = AchievementSystem.allAchievements
        val unlocked = world.unlockedAchievements

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.1f, 0.1f, 0.12f, 0.95f)
        shapeRenderer.rect(px, py, panelW, panelH)
        shapeRenderer.color = Color(0.3f, 0.25f, 0.1f, 1f)
        shapeRenderer.rect(px, py + panelH - 50f, panelW, 50f)

        // Achievement status rows
        val visibleStart = achievementScroll
        val visibleEnd = (achievementScroll + 12).coerceAtMost(achievements.size)
        for (i in visibleStart until visibleEnd) {
            val ach = achievements[i]
            val displayIdx = i - visibleStart
            val my = py + panelH - 75f - displayIdx * 35f
            val isUnlocked = unlocked.contains(ach.id)

            if (isUnlocked) {
                shapeRenderer.color = Color(0.2f, 0.3f, 0.1f, 0.4f)
                shapeRenderer.rect(px + 10f, my - 18f, panelW - 20f, 33f)
            }

            // Icon indicator
            shapeRenderer.color = if (isUnlocked) Color(0.85f, 0.7f, 0.2f, 0.9f) else Color(0.3f, 0.3f, 0.3f, 0.6f)
            shapeRenderer.rect(px + 15f, my - 14f, 16f, 16f)
        }
        shapeRenderer.end()

        batch.begin()
        fontLarge.color = Color.GOLD
        fontLarge.draw(batch, "업적 (${unlocked.size}/${achievements.size})", px + 15f, py + panelH - 12f)

        for (i in visibleStart until visibleEnd) {
            val ach = achievements[i]
            val displayIdx = i - visibleStart
            val my = py + panelH - 70f - displayIdx * 35f
            val isUnlocked = unlocked.contains(ach.id)

            if (isUnlocked) {
                fontMedium.color = Color(0.9f, 0.8f, 0.3f, 1f)
                fontSmall.color = Color(0.7f, 0.65f, 0.4f, 0.9f)
            } else {
                fontMedium.color = Color(0.5f, 0.5f, 0.5f, 1f)
                fontSmall.color = Color(0.4f, 0.4f, 0.4f, 0.8f)
            }

            val mark = if (isUnlocked) "[V] " else "[  ] "
            fontMedium.draw(batch, "$mark${ach.name}", px + 38f, my)
            fontSmall.draw(batch, ach.description, px + 40f, my - 16f)

            // Category
            fontSmall.color = Color(0.5f, 0.5f, 0.6f, 0.7f)
            fontSmall.draw(batch, ach.category.koreanName, px + panelW - 80f, my)
        }

        fontSmall.color = Color.LIGHT_GRAY
        fontSmall.draw(batch, "[W/S] 스크롤  [K/ESC] 닫기", px + 15f, py + 18f)
        batch.end()
    }

    fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }
}
