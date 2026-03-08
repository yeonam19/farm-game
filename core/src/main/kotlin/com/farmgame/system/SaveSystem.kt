package com.farmgame.system

import com.badlogic.gdx.Gdx
import com.farmgame.data.*
import com.farmgame.entity.Animal
import com.farmgame.entity.Crop
import com.farmgame.world.GameWorld
import com.farmgame.world.TileType
import java.io.File

object SaveSystem {
    private const val SAVE_DIR = "saves"
    private const val MAX_SLOTS = 10
    const val AUTO_SAVE_SLOT = 0

    data class SlotInfo(
        val slot: Int,
        val exists: Boolean,
        val summary: String,
        val isAutoSave: Boolean = false
    )

    fun getSlotInfos(): List<SlotInfo> {
        val slots = (1..MAX_SLOTS).map { slot ->
            val file = getSaveFile(slot)
            if (file.exists()) {
                val lines = file.readLines()
                val summary = lines.firstOrNull { it.startsWith("SUMMARY=") }
                    ?.removePrefix("SUMMARY=") ?: "저장 데이터"
                SlotInfo(slot, true, summary)
            } else {
                SlotInfo(slot, false, "빈 슬롯")
            }
        }
        // Add auto-save slot at the end
        val autoFile = getSaveFile(AUTO_SAVE_SLOT)
        val autoSlot = if (autoFile.exists()) {
            val lines = autoFile.readLines()
            val summary = lines.firstOrNull { it.startsWith("SUMMARY=") }
                ?.removePrefix("SUMMARY=") ?: "자동 저장"
            SlotInfo(AUTO_SAVE_SLOT, true, "자동저장: $summary", isAutoSave = true)
        } else {
            SlotInfo(AUTO_SAVE_SLOT, false, "자동저장: 없음", isAutoSave = true)
        }
        return slots + autoSlot
    }

    fun save(world: GameWorld, slot: Int): Boolean {
        try {
            val dir = File(Gdx.files.local(SAVE_DIR).file().absolutePath)
            if (!dir.exists()) dir.mkdirs()

            val sb = StringBuilder()
            val p = world.player
            val t = world.timeSystem

            // Summary line
            sb.appendLine("SUMMARY=${t.year}년 ${t.season.koreanName} ${t.day}일 | ${p.money}원 | Lv.${world.playerLevel}")

            // Player
            sb.appendLine("PLAYER=${p.tileX},${p.tileY},${p.money},${p.toolLevel},${p.sprinklerLevel},${p.landLevel},${p.currentTool.name},${p.hasOx},${p.hasSickle}")

            // Inventory
            for (item in p.inventory) {
                if (item.quantity > 0) {
                    sb.appendLine("ITEM=${item.type.name},${item.quantity}")
                }
            }

            // Time
            sb.appendLine("TIME=${t.timeOfDay},${t.day},${t.season.name},${t.year}")

            // Weather
            sb.appendLine("WEATHER=${world.weatherSystem.current.name}")

            // Economy
            sb.appendLine("ECONOMY=${world.economySystem.totalEarned},${world.economySystem.totalSpent}")

            // Farm boundaries
            sb.appendLine("FARM=${world.farmMinX},${world.farmMinY},${world.farmMaxX},${world.farmMaxY}")

            // Tiles (only non-default)
            for (x in 0 until world.mapWidth) {
                for (y in 0 until world.mapHeight) {
                    val tile = world.tiles[x][y]
                    if (tile.type == TileType.FARMLAND || tile.type == TileType.FARMLAND_WET) {
                        sb.appendLine("TILE=$x,$y,${tile.type.name},${tile.isWatered}")
                    }
                    tile.crop?.let { crop ->
                        sb.appendLine("CROP=$x,$y,${crop.type.name},${crop.currentStage},${crop.growthProgress},${crop.isDead}")
                    }
                }
            }

            // Animals
            for (animal in world.animals) {
                sb.appendLine("ANIMAL=${animal.type.name},${animal.tileX},${animal.tileY},${animal.hasProduct},${animal.happiness}")
            }

            // Level & missions
            sb.appendLine("LEVEL=${world.playerLevel},${world.playerExp}")
            for ((id, completed) in world.completedMissions) {
                if (completed) sb.appendLine("MISSION_DONE=$id")
            }
            for ((id, progress) in world.missionProgress) {
                sb.appendLine("MISSION_PROG=$id,$progress")
            }

            // Active mission
            if (world.activeMissionId != null) {
                sb.appendLine("ACTIVE_MISSION=${world.activeMissionId}")
            }

            // Stats
            sb.appendLine("STATS=${world.totalHarvested},${world.totalFished},${world.totalTreesChopped},${world.totalRocksMined},${world.totalFishTrash}")

            // Achievements
            for (id in world.unlockedAchievements) {
                sb.appendLine("ACHIEVEMENT=$id")
            }

            // Harvested crop types
            if (world.harvestedCropTypes.isNotEmpty()) {
                sb.appendLine("CROP_TYPES=${world.harvestedCropTypes.joinToString(",")}")
            }

            // Auto fish
            sb.appendLine("AUTO_FISH=${world.autoFishLevel}")

            // Herder
            sb.appendLine("HERDER=${world.herderLevel}")

            // Hired farmer
            sb.appendLine("HIRED_FARMER=${world.farmerLevel}")

            getSaveFile(slot).writeText(sb.toString())
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun load(world: GameWorld, slot: Int): Boolean {
        try {
            val file = getSaveFile(slot)
            if (!file.exists()) return false

            val lines = file.readLines()

            // Reset world state
            world.animals.clear()
            world.player.inventory.clear()
            world.completedMissions.clear()
            world.missionProgress.clear()
            world.unlockedAchievements.clear()
            world.harvestedCropTypes.clear()
            world.activeMissionId = null
            world.farmerLevel = 0
            world.herderLevel = 0
            world.autoFishLevel = 0

            for (line in lines) {
                val parts = line.split("=", limit = 2)
                if (parts.size < 2) continue
                val key = parts[0]
                val value = parts[1]

                when (key) {
                    "PLAYER" -> {
                        val p = value.split(",")
                        world.player.tileX = p[0].toInt()
                        world.player.tileY = p[1].toInt()
                        world.player.renderX = world.player.tileX.toFloat()
                        world.player.renderY = world.player.tileY.toFloat()
                        world.player.money = p[2].toInt()
                        world.player.toolLevel = p[3].toInt()
                        world.player.sprinklerLevel = p[4].toInt()
                        world.player.landLevel = p[5].toInt()
                        world.player.currentTool = Tool.valueOf(p[6])
                        if (p.size > 7) world.player.hasOx = p[7].toBoolean()
                        if (p.size > 8) world.player.hasSickle = p[8].toBoolean()
                    }
                    "ITEM" -> {
                        val p = value.split(",")
                        world.player.addItem(ItemType.valueOf(p[0]), p[1].toInt())
                    }
                    "TIME" -> {
                        val p = value.split(",")
                        world.timeSystem.timeOfDay = p[0].toFloat()
                        world.timeSystem.day = p[1].toInt()
                        world.timeSystem.season = Season.valueOf(p[2])
                        world.timeSystem.year = p[3].toInt()
                    }
                    "WEATHER" -> {
                        world.weatherSystem.current = Weather.valueOf(value)
                    }
                    "ECONOMY" -> {
                        val p = value.split(",")
                        world.economySystem.totalEarned = p[0].toInt()
                        world.economySystem.totalSpent = p[1].toInt()
                    }
                    "FARM" -> {
                        val p = value.split(",")
                        world.farmMinX = p[0].toInt()
                        world.farmMinY = p[1].toInt()
                        world.farmMaxX = p[2].toInt()
                        world.farmMaxY = p[3].toInt()
                        world.regenerateMap()
                    }
                    "TILE" -> {
                        val p = value.split(",")
                        val x = p[0].toInt(); val y = p[1].toInt()
                        world.tiles[x][y].type = TileType.valueOf(p[2])
                        world.tiles[x][y].isWatered = p[3].toBoolean()
                    }
                    "CROP" -> {
                        val p = value.split(",")
                        val x = p[0].toInt(); val y = p[1].toInt()
                        val crop = Crop(CropType.valueOf(p[2]))
                        crop.currentStage = p[3].toInt()
                        crop.growthProgress = p[4].toFloat()
                        crop.isDead = p[5].toBoolean()
                        world.tiles[x][y].crop = crop
                    }
                    "ANIMAL" -> {
                        val p = value.split(",")
                        val animal = Animal(AnimalType.valueOf(p[0]), p[1].toInt(), p[2].toInt())
                        animal.hasProduct = p[3].toBoolean()
                        animal.happiness = p[4].toFloat()
                        world.animals.add(animal)
                    }
                    "LEVEL" -> {
                        val p = value.split(",")
                        world.playerLevel = p[0].toInt()
                        world.playerExp = p[1].toInt()
                    }
                    "MISSION_DONE" -> {
                        world.completedMissions[value] = true
                    }
                    "MISSION_PROG" -> {
                        val p = value.split(",")
                        world.missionProgress[p[0]] = p[1].toInt()
                    }
                    "ACTIVE_MISSION" -> {
                        world.activeMissionId = value
                    }
                    "STATS" -> {
                        val p = value.split(",")
                        world.totalHarvested = p[0].toInt()
                        world.totalFished = p[1].toInt()
                        world.totalTreesChopped = p[2].toInt()
                        world.totalRocksMined = p[3].toInt()
                        if (p.size > 4) world.totalFishTrash = p[4].toInt()
                    }
                    "ACHIEVEMENT" -> {
                        world.unlockedAchievements.add(value)
                    }
                    "CROP_TYPES" -> {
                        world.harvestedCropTypes.addAll(value.split(","))
                    }
                    "HERDER" -> {
                        val p = value.split(",")
                        world.herderLevel = p[0].toInt()
                    }
                    "AUTO_FISH" -> {
                        world.autoFishLevel = value.toInt()
                    }
                    "HIRED_FARMER" -> {
                        val p = value.split(",")
                        // Support both old boolean format and new level format
                        world.farmerLevel = try { p[0].toInt() } catch (_: Exception) { if (p[0].toBoolean()) 1 else 0 }
                    }
                }
            }

            // If no active mission was loaded, find the first uncompleted mission in chain
            if (world.activeMissionId == null) {
                world.activeMissionId = MissionSystem.missionChain.firstOrNull { world.completedMissions[it] != true }
            }

            // Rebuild worker lists from saved levels
            world.rebuildWorkers()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun deleteSlot(slot: Int): Boolean {
        val file = getSaveFile(slot)
        return if (file.exists()) file.delete() else false
    }

    private fun getSaveFile(slot: Int): File {
        val dir = Gdx.files.local(SAVE_DIR).file()
        return File(dir, "save_$slot.dat")
    }
}
