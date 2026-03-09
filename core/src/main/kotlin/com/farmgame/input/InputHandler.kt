package com.farmgame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.farmgame.data.*
import com.farmgame.system.AudioSystem
import com.farmgame.system.AudioSystem.SfxType
import com.farmgame.ui.GameHUD
import com.farmgame.world.GameWorld

class InputHandler(
    private val world: GameWorld,
    private val hud: GameHUD
) {
    var zoom: Float = 1f

    fun handleMovementInput(delta: Float) {
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

    fun handleActionInput() {
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
                hud.showShop = true; hud.shopCursor = 0; AudioSystem.playSfx(SfxType.UI_OPEN); return
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
                AudioSystem.playSfx(SfxType.UI_OPEN)
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
            if (hud.showShop || hud.showInventory || hud.showSaveLoad || hud.showMissions || hud.showAchievements) {
                AudioSystem.playSfx(SfxType.UI_CLOSE)
            }
            hud.showShop = false; hud.showInventory = false
            hud.showSaveLoad = false; hud.showMissions = false
            hud.showAchievements = false
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            val items = world.player.inventory.filter { !it.type.isSeed && it.type.sellPrice > 0 }
            if (items.isNotEmpty()) {
                val item = items.first()
                if (world.economySystem.sellItem(world.player, item.type)) {
                    AudioSystem.playSfx(SfxType.SELL)
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (items.isNotEmpty() && hud.inventoryCursor < items.size) {
                val item = items[hud.inventoryCursor]
                if (item.type.sellPrice > 0) {
                    if (world.economySystem.sellItem(world.player, item.type)) {
                        AudioSystem.playSfx(SfxType.SELL)
                        world.notify("${item.type.koreanName} 판매! (+${item.type.sellPrice}원)")
                        world.checkMissions()
                        world.checkAchievements()
                    }
                } else {
                    world.notify("이 아이템은 판매할 수 없습니다.")
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            val total = world.economySystem.sellAllCrops(world.player)
            if (total > 0) {
                AudioSystem.playSfx(SfxType.SELL)
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
                        if (world.economySystem.buySeed(world.player, crop, 5)) {
                            AudioSystem.playSfx(SfxType.BUY)
                            world.notify("${crop.koreanName} 씨앗 5개 구매! (-${crop.seedPrice * 5}원)")
                        } else world.notify("돈이 부족합니다!")
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
}
