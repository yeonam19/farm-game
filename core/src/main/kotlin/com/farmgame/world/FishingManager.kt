package com.farmgame.world

import com.farmgame.data.ItemType
import com.farmgame.system.AudioSystem
import com.farmgame.system.AudioSystem.SfxType

/**
 * 낚시/자동낚시 로직
 */
class FishingManager(private val world: GameWorld) {

    fun startFishing(): Boolean {
        val (fx, fy) = world.player.getFacingTile()
        val tile = world.getTile(fx, fy) ?: return false
        if (tile.type != TileType.WATER) {
            world.notify("물가를 향해 낚싯대를 사용하세요!")
            return false
        }
        if (world.player.isFishing) {
            return catchFish()
        }
        world.player.isFishing = true
        world.player.fishTimer = 0f
        world.player.fishBiteTimer = 2f + Math.random().toFloat() * 4f
        world.player.hasBite = false
        AudioSystem.playSfx(SfxType.FISH_CAST)
        world.notify("낚시를 시작합니다... 입질을 기다리세요!")
        return true
    }

    fun catchFish(): Boolean {
        if (!world.player.isFishing) return false
        if (world.player.hasBite) {
            val roll = Math.random()
            val fish = when {
                roll < 0.08 -> ItemType.FISH_RARE
                roll < 0.23 -> ItemType.FISH_LARGE
                roll < 0.53 -> ItemType.FISH_MEDIUM
                roll < 0.88 -> ItemType.FISH_SMALL
                else -> ItemType.FISH_TRASH
            }
            world.player.addItem(fish)
            world.player.isFishing = false
            world.totalFished++
            if (fish == ItemType.FISH_TRASH) world.totalFishTrash++
            if (fish == ItemType.FISH_RARE) world.unlockedAchievements.add("rare_fish")
            AudioSystem.playSfx(SfxType.FISH_CATCH)
            world.addExp(15)
            world.checkMissions()
            world.checkAchievements()
            world.notify("${fish.koreanName}을(를) 잡았습니다! (+${fish.sellPrice}원)")
            return true
        } else {
            world.player.isFishing = false
            AudioSystem.playSfx(SfxType.FISH_FAIL)
            world.notify("너무 일찍 당겼습니다! 물고기가 도망갔어요.")
            return false
        }
    }

    fun updateFishing(delta: Float) {
        if (!world.player.isFishing) return
        world.player.fishTimer += delta
        if (!world.player.hasBite && world.player.fishTimer >= world.player.fishBiteTimer) {
            world.player.hasBite = true
            AudioSystem.playSfx(SfxType.FISH_BITE)
            world.notify("입질이 왔습니다! Space를 누르세요!")
        }
        if (world.player.hasBite && world.player.fishTimer > world.player.fishBiteTimer + 2f) {
            world.player.isFishing = false
            world.player.hasBite = false
            world.notify("물고기가 도망갔습니다...")
        }
    }

    fun cancelFishing() {
        world.player.isFishing = false
        world.player.hasBite = false
        world.player.fishTimer = 0f
    }

    fun updateAutoFish(delta: Float) {
        if (world.autoFishLevel <= 0) return

        // Animation decay
        if (world.autoFishCatchAnim > 0) world.autoFishCatchAnim -= delta

        val interval = when (world.autoFishLevel) {
            1 -> 15f
            2 -> 8f
            3 -> 4f
            else -> 999f
        }

        world.autoFishTimer += delta
        if (world.autoFishTimer < interval) return
        world.autoFishTimer = 0f

        val roll = Math.random()
        val fish = when (world.autoFishLevel) {
            1 -> when {
                roll < 0.01 -> ItemType.FISH_RARE
                roll < 0.08 -> ItemType.FISH_LARGE
                roll < 0.30 -> ItemType.FISH_MEDIUM
                roll < 0.70 -> ItemType.FISH_SMALL
                else -> ItemType.FISH_TRASH
            }
            2 -> when {
                roll < 0.03 -> ItemType.FISH_RARE
                roll < 0.15 -> ItemType.FISH_LARGE
                roll < 0.45 -> ItemType.FISH_MEDIUM
                roll < 0.85 -> ItemType.FISH_SMALL
                else -> ItemType.FISH_TRASH
            }
            3 -> when {
                roll < 0.08 -> ItemType.FISH_RARE
                roll < 0.25 -> ItemType.FISH_LARGE
                roll < 0.55 -> ItemType.FISH_MEDIUM
                roll < 0.90 -> ItemType.FISH_SMALL
                else -> ItemType.FISH_TRASH
            }
            else -> ItemType.FISH_SMALL
        }

        world.player.addItem(fish)
        world.totalFished++
        if (fish == ItemType.FISH_TRASH) world.totalFishTrash++
        if (fish == ItemType.FISH_RARE) world.unlockedAchievements.add("rare_fish")
        world.addExp(5)
        world.autoFishCatchAnim = 2f
        world.notify("통발: ${fish.koreanName} 획득!")
    }
}
