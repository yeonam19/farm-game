package com.farmgame.world

import com.farmgame.data.*
import com.farmgame.entity.Crop
import com.farmgame.system.AudioSystem
import com.farmgame.system.AudioSystem.SfxType

/**
 * 농사 관련 로직 — 밭갈기, 물주기, 파종, 수확, 나무/바위
 */
class FarmManager(private val world: GameWorld) {

    fun tillGround(): Boolean {
        val (fx, fy) = world.player.getFacingTile()
        val tile = world.getTile(fx, fy) ?: return false
        if (tile.type == TileType.GRASS || tile.type == TileType.DIRT) {
            if (fx in world.farmMinX..world.farmMaxX && fy in world.farmMinY..world.farmMaxY) {
                tile.till()

                AudioSystem.playSfx(SfxType.TILL)
                if (world.player.hasOx) {
                    for (i in 2..6) {
                        val (ex, ey) = when (world.player.facing) {
                            com.farmgame.entity.Direction.UP -> Pair(fx, fy + (i - 1))
                            com.farmgame.entity.Direction.DOWN -> Pair(fx, fy - (i - 1))
                            com.farmgame.entity.Direction.LEFT -> Pair(fx - (i - 1), fy)
                            com.farmgame.entity.Direction.RIGHT -> Pair(fx + (i - 1), fy)
                        }
                        val extraTile = world.getTile(ex, ey)
                        if (extraTile != null && (extraTile.type == TileType.GRASS || extraTile.type == TileType.DIRT)
                            && ex in world.farmMinX..world.farmMaxX && ey in world.farmMinY..world.farmMaxY) {
                            extraTile.till()
                        }
                    }
                    world.notify("소가 힘차게 6칸을 경작했습니다!")
                } else {
                    world.notify("땅을 경작했습니다! 이제 씨앗(3번)으로 심을 수 있어요.")
                }
                return true
            } else {
                world.notify("울타리 안 농장 구역에서만 경작할 수 있습니다.")
            }
        } else if (tile.isFarmable) {
            world.notify("이미 경작된 땅입니다. 씨앗(3번)을 심어보세요!")
        }
        return false
    }

    fun waterGround(): Boolean {
        val (fx, fy) = world.player.getFacingTile()
        val tile = world.getTile(fx, fy) ?: return false
        if (tile.isFarmable) {
            tile.water()

            if (world.player.sprinklerLevel > 0) {
                val range = if (world.player.sprinklerLevel >= 2) 2 else 1
                for (dx in -range..range) {
                    for (dy in -range..range) {
                        world.getTile(fx + dx, fy + dy)?.let {
                            if (it.isFarmable) it.water()
                        }
                    }
                }
            }

            AudioSystem.playSfx(SfxType.WATER)
            world.notify("물을 주었습니다!")
            return true
        }
        return false
    }

    fun plantSeed(): Boolean {
        val (fx, fy) = world.player.getFacingTile()
        val tile = world.getTile(fx, fy) ?: return false
        if (!tile.canPlant) {
            if (tile.type == TileType.GRASS || tile.type == TileType.DIRT) {
                world.notify("먼저 괭이(1번)로 땅을 경작하세요!")
            } else if (!tile.isFarmable) {
                world.notify("경작지에만 심을 수 있습니다. 괭이(1번)로 풀밭을 경작하세요!")
            } else if (tile.crop != null) {
                world.notify("이미 작물이 심어져 있습니다.")
            }
            return false
        }

        val seedStack = world.player.getSelectedSeed()
        if (seedStack == null) {
            world.notify("씨앗이 없습니다! 상점에서 구매하세요. (시장/상인 근처에서 Space)")
            return false
        }

        val cropType = seedStack.type.cropType ?: return false
        if (!cropType.canGrowIn(world.timeSystem.season)) {
            world.notify("${cropType.koreanName}은(는) ${cropType.season.koreanName}에만 심을 수 있습니다.")
            return false
        }

        if (world.player.hasOx) {
            var count = 0
            for (i in 0..5) {
                val (sx, sy) = when (world.player.facing) {
                    com.farmgame.entity.Direction.UP -> Pair(fx, fy + i)
                    com.farmgame.entity.Direction.DOWN -> Pair(fx, fy - i)
                    com.farmgame.entity.Direction.LEFT -> Pair(fx - i, fy)
                    com.farmgame.entity.Direction.RIGHT -> Pair(fx + i, fy)
                }
                val t = world.getTile(sx, sy) ?: continue
                if (t.canPlant) {
                    val seed = world.player.getSelectedSeed() ?: break
                    val ct = seed.type.cropType ?: break
                    if (!ct.canGrowIn(world.timeSystem.season)) break
                    world.player.removeItem(seed.type)
                    t.crop = Crop(ct)
                    count++
                }
            }
            if (count > 0) {
                AudioSystem.playSfx(SfxType.PLANT)
                world.notify("소가 힘차게 ${count}칸에 씨앗을 심었습니다!")
                return true
            }
        } else {
            world.player.removeItem(seedStack.type)
            tile.crop = Crop(cropType)
            AudioSystem.playSfx(SfxType.PLANT)
            world.notify("${cropType.koreanName} 씨앗을 심었습니다!")
            return true
        }
        return false
    }

    fun harvest(): Boolean {
        val (fx, fy) = world.player.getFacingTile()
        val tile = world.getTile(fx, fy) ?: return false

        // Check for animal product
        for (animal in world.animals) {
            if (animal.tileX == fx && animal.tileY == fy && animal.hasProduct) {
                animal.collect()
                world.player.money += animal.type.productSellPrice
                AudioSystem.playSfx(SfxType.ANIMAL_COLLECT)
                world.notify("${animal.type.productName}을(를) 수확했습니다! (+${animal.type.productSellPrice}원)")
                return true
            }
        }

        // Check for NPC dialogue
        for (npc in world.npcs) {
            if (npc.tileX == fx && npc.tileY == fy) {
                world.notify(npc.getDialogue())
                return true
            }
        }

        // Check for market
        if (tile.type == TileType.MARKET) {
            return false
        }

        if (!tile.canHarvest) {
            if (tile.crop != null && !tile.crop!!.isReady) {
                val crop = tile.crop!!
                val progress = (crop.currentStage.toFloat() / crop.type.growthStages * 100).toInt()
                world.notify("${crop.type.koreanName}: 성장 중... (${progress}%)")
            }
            return false
        }

        // 낫 아이템: 3x3 범위 동시 수확
        if (world.player.hasSickle) {
            var count = 0
            var totalExp = 0
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val t = world.getTile(fx + dx, fy + dy) ?: continue
                    if (t.canHarvest) {
                        val c = t.crop!!
                        world.player.addItem(ItemType.harvestFor(c.type))
                        world.harvestedCropTypes.add(c.type.name)
                        totalExp += c.type.harvestExp
                        t.removeCrop()
                        t.type = TileType.FARMLAND
                        world.totalHarvested++
                        count++
                    }
                }
            }
            if (count > 0) {
                AudioSystem.playSfx(SfxType.HARVEST)
                world.addExp(totalExp)
                world.checkMissions()
                world.checkAchievements()
                world.notify("낫으로 ${count}칸을 한꺼번에 수확했습니다!")
                return true
            }
        } else {
            val crop = tile.crop!!
            val harvestItem = ItemType.harvestFor(crop.type)
            world.player.addItem(harvestItem)
            tile.removeCrop()
            tile.type = TileType.FARMLAND
            world.totalHarvested++
            world.harvestedCropTypes.add(crop.type.name)
            AudioSystem.playSfx(SfxType.HARVEST)
            world.addExp(crop.type.harvestExp)
            world.checkMissions()
            world.checkAchievements()
            world.notify("${crop.type.koreanName}을(를) 수확했습니다! (+${crop.type.harvestExp}EXP)")
            return true
        }
        return false
    }

    fun chopTree(): Boolean {
        val (fx, fy) = world.player.getFacingTile()
        val tile = world.getTile(fx, fy) ?: return false
        if (tile.type == TileType.TREE) {
            tile.type = TileType.GRASS
            world.player.addItem(ItemType.WOOD, 2)
            world.totalTreesChopped++
            AudioSystem.playSfx(SfxType.CHOP)
            world.addExp(5)
            world.checkMissions()
            world.checkAchievements()
            world.notify("나무를 베었습니다! (나무 x2)")
            return true
        }
        return false
    }

    fun mineRock(): Boolean {
        val (fx, fy) = world.player.getFacingTile()
        val tile = world.getTile(fx, fy) ?: return false
        if (tile.type == TileType.ROCK) {
            tile.type = TileType.GRASS
            world.player.addItem(ItemType.STONE, 2)
            world.totalRocksMined++
            AudioSystem.playSfx(SfxType.MINE)
            world.addExp(5)
            world.checkMissions()
            world.checkAchievements()
            world.notify("돌을 캤습니다! (돌 x2)")
            return true
        }
        return false
    }

    fun expandLand(): Boolean {
        val nextLevel = world.player.landLevel + 1
        val upgrade = Upgrades.landExpansions[nextLevel] ?: run {
            world.notify("더 이상 확장할 수 없습니다!")
            return false
        }
        if (!world.economySystem.buyUpgrade(world.player, upgrade.cost)) {
            world.notify("돈이 부족합니다! (필요: ${upgrade.cost}원)")
            return false
        }

        // Save existing farmland tiles and crops
        data class SavedTile(val x: Int, val y: Int, val type: TileType, val isWatered: Boolean, val crop: Crop?)
        val savedTiles = mutableListOf<SavedTile>()
        for (x in 0 until world.mapWidth) {
            for (y in 0 until world.mapHeight) {
                val tile = world.tiles[x][y]
                if (tile.type == TileType.FARMLAND || tile.type == TileType.FARMLAND_WET || tile.crop != null) {
                    savedTiles.add(SavedTile(x, y, tile.type, tile.isWatered, tile.crop))
                }
            }
        }

        world.player.landLevel = nextLevel
        val expansion = nextLevel * 5
        world.farmMinX -= expansion / 2
        world.farmMinY -= expansion / 2
        world.farmMaxX += expansion / 2
        world.farmMaxY += expansion / 2
        world.farmMinX = world.farmMinX.coerceAtLeast(3)
        world.farmMinY = world.farmMinY.coerceAtLeast(3)
        world.farmMaxX = world.farmMaxX.coerceAtMost(world.mapWidth - 4)
        world.farmMaxY = world.farmMaxY.coerceAtMost(world.mapHeight - 4)

        world.regenerateMap()

        // Restore saved farmland and crops
        for (saved in savedTiles) {
            if (saved.x in 0 until world.mapWidth && saved.y in 0 until world.mapHeight) {
                val tile = world.tiles[saved.x][saved.y]
                if (saved.x in world.farmMinX..world.farmMaxX && saved.y in world.farmMinY..world.farmMaxY) {
                    tile.type = saved.type
                    tile.isWatered = saved.isWatered
                    tile.crop = saved.crop
                }
            }
        }

        world.notify("${upgrade.koreanName} 완료! 기존 농작물이 보존되었습니다!")
        return true
    }
}
