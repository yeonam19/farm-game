package com.farmgame.world

import com.farmgame.data.*
import com.farmgame.entity.*
import com.farmgame.system.*
import com.farmgame.system.AudioSystem.SfxType

class HiredWorker(
    val name: String,
    val level: Int,          // 1=초급, 2=중급, 3=고급
    val workInterval: Float, // 작업 간격 (초)
    startX: Int, startY: Int
) {
    var x: Int = startX
    var y: Int = startY
    var renderX: Float = x.toFloat()
    var renderY: Float = y.toFloat()
    var targetX: Int = x
    var targetY: Int = y
    var action: String = "idle"
    var actionTimer: Float = 0f
    var pendingAction: String = ""
    var workTimer: Float = 0f
    var moveTimer: Float = 0f
}

class GameWorld {
    var mapWidth: Int = 50
    var mapHeight: Int = 50
    lateinit var tiles: Array<Array<Tile>>

    val player = Player(mapWidth / 2, mapHeight / 2)
    val timeSystem = TimeSystem()
    val weatherSystem = WeatherSystem()
    val economySystem = EconomySystem()

    val animals = mutableListOf<Animal>()
    val npcs = mutableListOf<NPC>()

    var notification: String = ""
    var notificationTimer: Float = 0f
    var pendingNotification: String? = null

    // Farm boundaries (expandable)
    var farmMinX: Int = 18
    var farmMinY: Int = 18
    var farmMaxX: Int = 32
    var farmMaxY: Int = 32

    // Level & mission tracking
    var playerLevel: Int = 1
    var playerExp: Int = 0
    var totalHarvested: Int = 0
    var totalFished: Int = 0
    var totalTreesChopped: Int = 0
    var totalRocksMined: Int = 0
    val completedMissions = mutableMapOf<String, Boolean>()
    val missionProgress = mutableMapOf<String, Int>()
    var activeMissionId: String? = null

    // Achievements
    val unlockedAchievements = mutableSetOf<String>()
    var newAchievementId: String? = null
    var achievementPopupTimer: Float = 0f

    // Harvested crop types tracking
    val harvestedCropTypes = mutableSetOf<String>()
    var totalFishTrash: Int = 0

    // Auto-save
    private var autoSaveTimer: Float = 0f
    var lastAutoSaveTime: String = ""
    var onAutoSave: (() -> Unit)? = null

    // 자동 낚시 장치 (0=없음, 1=기본 통발, 2=고급 통발, 3=자동 낚시선)
    var autoFishLevel: Int = 0
    var autoFishTimer: Float = 0f
    var autoFishX: Int = 0
    var autoFishY: Int = 0
    var autoFishRenderX: Float = 0f
    var autoFishRenderY: Float = 0f
    var autoFishCatchAnim: Float = 0f

    // === Worker system ===
    val farmers = mutableListOf<HiredWorker>()
    val herders = mutableListOf<HiredWorker>()
    var farmerLevel: Int = 0
    var herderLevel: Int = 0
    val hiredFarmer: Boolean get() = farmerLevel > 0
    val hasHerder: Boolean get() = herderLevel > 0

    // Managers
    val farmManager = FarmManager(this)
    val fishingManager = FishingManager(this)
    val workerManager = WorkerManager(this)

    init {
        generateMap()
        setupNPCs()
        setupCallbacks()
        giveStartingItems()
        assignFirstMission()
        workerManager.rebuildWorkers()
        initAutoFishPosition()
    }

    fun rebuildWorkers() = workerManager.rebuildWorkers()

    private fun initAutoFishPosition() {
        autoFishX = 10
        autoFishY = 36
        autoFishRenderX = autoFishX.toFloat()
        autoFishRenderY = autoFishY.toFloat()
    }

    private fun assignFirstMission() {
        if (activeMissionId == null) {
            activeMissionId = MissionSystem.missionChain.firstOrNull()
        }
    }

    private fun giveStartingItems() {
        player.addItem(ItemType.STRAWBERRY_SEED, 10)
        player.addItem(ItemType.POTATO_SEED, 5)
        player.money = 1000
    }

    private fun generateMap() {
        tiles = Array(mapWidth) { x ->
            Array(mapHeight) { y ->
                val tile = Tile(x, y)
                tile.type = when {
                    x == 0 || y == 0 || x == mapWidth - 1 || y == mapHeight - 1 -> TileType.WATER
                    x == 1 || y == 1 || x == mapWidth - 2 || y == mapHeight - 2 -> TileType.SAND

                    distSq(x, y, 10, 40) < 9 -> TileType.WATER
                    distSq(x, y, 10, 40) < 16 -> TileType.SAND

                    x in 38..42 && y in 38..42 -> TileType.PATH
                    x == 40 && y == 40 -> TileType.MARKET

                    x in 24..26 && y in 34..36 -> TileType.PATH
                    x == 25 && y == 35 -> TileType.HOME

                    x == 25 && y in 14..45 -> TileType.PATH
                    y == 25 && x in 5..45 -> TileType.PATH
                    y == 38 && x in 25..42 -> TileType.PATH

                    isTreePosition(x, y) -> TileType.TREE
                    isRockPosition(x, y) -> TileType.ROCK

                    (x == farmMinX - 1 || x == farmMaxX + 1) && y in (farmMinY - 1)..(farmMaxY + 1) -> TileType.FENCE
                    (y == farmMinY - 1 || y == farmMaxY + 1) && x in (farmMinX - 1)..(farmMaxX + 1) -> TileType.FENCE

                    else -> TileType.GRASS
                }
                tile
            }
        }
    }

    private fun distSq(x1: Int, y1: Int, x2: Int, y2: Int): Int =
        (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)

    private fun isTreePosition(x: Int, y: Int): Boolean {
        if (x in (farmMinX - 2)..(farmMaxX + 2) && y in (farmMinY - 2)..(farmMaxY + 2)) return false
        if (x == 25 || y == 25 || y == 38) return false
        val hash = ((x * 7919 + y * 104729) % 100)
        return hash < 8
    }

    private fun isRockPosition(x: Int, y: Int): Boolean {
        if (x in (farmMinX - 2)..(farmMaxX + 2) && y in (farmMinY - 2)..(farmMaxY + 2)) return false
        if (x == 25 || y == 25 || y == 38) return false
        val hash = ((x * 6271 + y * 91813) % 100)
        return hash < 3
    }

    private fun setupNPCs() {
        NPCs.villagers.forEachIndexed { i, data ->
            val positions = listOf(Pair(39, 39), Pair(30, 25), Pair(14, 38), Pair(20, 25))
            val pos = positions[i % positions.size]
            npcs.add(NPC(data, pos.first, pos.second))
        }
    }

    private fun setupCallbacks() {
        timeSystem.onNewDay = {
            for (x in 0 until mapWidth) {
                for (y in 0 until mapHeight) {
                    val tile = tiles[x][y]
                    tile.crop?.onNewDay(tile.isWatered)
                    tile.dryOut()
                }
            }

            // 스프링클러 자동 물주기 (Lv1=격일, Lv2=매일)
            if (player.sprinklerLevel > 0) {
                val shouldWater = if (player.sprinklerLevel >= 2) true
                                  else (getTotalDays() % 2 == 0) // Lv1: 격일 작동
                if (shouldWater) {
                    val range = if (player.sprinklerLevel >= 2) 2 else 1
                    var watered = 0
                    for (x in farmMinX..farmMaxX) {
                        for (y in farmMinY..farmMaxY) {
                            val tile = getTile(x, y) ?: continue
                            if (tile.isFarmable && tile.crop != null) {
                                for (dx in -range..range) {
                                    for (dy in -range..range) {
                                        getTile(x + dx, y + dy)?.let {
                                            if (it.isFarmable && !it.isWatered) {
                                                it.water()
                                                watered++
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (watered > 0) notify("스프링클러가 ${watered}칸에 자동으로 물을 주었습니다!")
                } else {
                    notify("스프링클러 휴식일 — 내일 자동으로 물을 줍니다.")
                }
            }

            AudioSystem.playSfx(SfxType.NEW_DAY)
            notify("새로운 하루가 시작되었습니다! (${timeSystem.getSeasonDay()})")
        }
        timeSystem.onNewSeason = {
            AudioSystem.playSfx(SfxType.SEASON_CHANGE)
            AudioSystem.playBgm(timeSystem.season)
            notify("계절이 바뀌었습니다: ${timeSystem.season.koreanName}!")
        }
    }

    fun update(delta: Float) {
        timeSystem.update(delta)
        weatherSystem.update(delta, timeSystem.season)
        player.updateRender(delta)

        // Update crops
        for (x in farmMinX..farmMaxX) {
            for (y in farmMinY..farmMaxY) {
                if (x < mapWidth && y < mapHeight) {
                    val tile = tiles[x][y]
                    tile.crop?.update(delta, tile.isWatered, weatherSystem.current)
                    if (weatherSystem.current.autoWater && tile.isFarmable) {
                        tile.water()
                    }
                }
            }
        }

        fishingManager.updateFishing(delta)
        fishingManager.updateAutoFish(delta)

        for (farmer in farmers) workerManager.updateWorker(farmer, delta, "farmer")
        for (herder in herders) workerManager.updateWorker(herder, delta, "herder")

        animals.forEach { it.update(delta, mapWidth, mapHeight) }
        npcs.forEach { it.update(delta) { x, y -> isWalkable(x, y) } }

        // Notification timer
        if (notificationTimer > 0) {
            notificationTimer -= delta
            if (notificationTimer <= 0) {
                notification = ""
                val pending = pendingNotification
                if (pending != null) {
                    pendingNotification = null
                    notify(pending)
                }
            }
        }

        // Achievement popup timer
        if (achievementPopupTimer > 0) {
            achievementPopupTimer -= delta
            if (achievementPopupTimer <= 0) newAchievementId = null
        }

        // Periodic mission/achievement check
        missionCheckTimer += delta
        if (missionCheckTimer >= 2f) {
            missionCheckTimer = 0f
            checkMissions()
            checkAchievements()
        }

        // Auto-save every 6 game hours
        autoSaveTimer += delta
        if (autoSaveTimer >= 180f) {
            autoSaveTimer = 0f
            onAutoSave?.invoke()
        }
    }
    private var missionCheckTimer: Float = 0f

    fun getTile(x: Int, y: Int): Tile? {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) return null
        return tiles[x][y]
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        val tile = getTile(x, y) ?: return false
        return tile.type.walkable
    }

    fun notify(message: String) {
        notification = message
        notificationTimer = 3f
    }

    fun regenerateMap() {
        generateMap()
    }

    fun addExp(amount: Int) {
        playerExp += amount
        val needed = MissionSystem.getExpForLevel(playerLevel)
        if (playerExp >= needed) {
            playerExp -= needed
            playerLevel++
            AudioSystem.playSfx(SfxType.LEVEL_UP)
            notify("레벨 업! Lv.${playerLevel} - ${MissionSystem.getLevelTitle(playerLevel)}")
        }
    }

    fun checkMissions() {
        updateMissionProgress()

        var safetyCounter = 0
        while (safetyCounter < 5) {
            safetyCounter++
            val activeId = activeMissionId ?: return
            val mission = MissionSystem.getMissionById(activeId) ?: return
            if (completedMissions[activeId] == true) {
                activeMissionId = MissionSystem.getNextMission(activeId)
                continue
            }

            val progress = missionProgress[activeId] ?: 0
            if (progress < mission.target) return

            completedMissions[activeId] = true
            addExp(mission.expReward)
            player.money += mission.moneyReward

            val itemRewardText = StringBuilder()
            for (reward in mission.itemRewards) {
                try {
                    val itemType = ItemType.valueOf(reward.itemName)
                    player.addItem(itemType, reward.quantity)
                    itemRewardText.append(" +${itemType.koreanName}x${reward.quantity}")
                } catch (_: Exception) {}
            }

            AudioSystem.playSfx(SfxType.MISSION_COMPLETE)
            notify("미션 완료! [${mission.name}] +${mission.expReward}EXP +${mission.moneyReward}원$itemRewardText")
            notificationTimer = 5f

            val nextId = MissionSystem.getNextMission(activeId)
            activeMissionId = nextId
            if (nextId != null) {
                val nextMission = MissionSystem.getMissionById(nextId)
                if (nextMission != null) {
                    pendingNotification = "새 미션! [${nextMission.name}] - ${nextMission.description}"
                }
                updateMissionProgress()
            } else {
                pendingNotification = "모든 미션을 완료했습니다!"
                return
            }
        }
    }

    private fun updateMissionProgress() {
        for (mission in MissionSystem.allMissions) {
            val current = when (mission.id) {
                "harvest_10", "harvest_50", "harvest_200", "harvest_500" -> totalHarvested
                "fish_5", "fish_20", "fish_50", "fish_100" -> totalFished
                "chop_10", "chop_50" -> totalTreesChopped
                "mine_10", "mine_50" -> totalRocksMined
                "earn_1000", "earn_5000", "earn_20000", "earn_50000" -> economySystem.totalEarned
                "animal_1", "animal_5" -> animals.size
                "expand_1" -> player.landLevel
                "upgrade_tool" -> player.toolLevel
                else -> 0
            }
            missionProgress[mission.id] = current
        }
    }

    fun checkAchievements() {
        val checks = mapOf<String, () -> Boolean>(
            "first_harvest" to { totalHarvested >= 1 },
            "harvest_100" to { totalHarvested >= 100 },
            "harvest_500" to { totalHarvested >= 500 },
            "all_crops" to { harvestedCropTypes.size >= CropType.entries.size },
            "first_fish" to { totalFished >= 1 },
            "fish_50" to { totalFished >= 50 },
            "rare_fish" to { player.inventory.any { it.type == ItemType.FISH_RARE && it.quantity > 0 } || totalFished > 0 && unlockedAchievements.contains("rare_fish") },
            "fish_trash" to { totalFishTrash >= 10 },
            "chop_100" to { totalTreesChopped >= 100 },
            "mine_100" to { totalRocksMined >= 100 },
            "first_1000" to { player.money >= 1000 },
            "rich_10000" to { player.money >= 10000 },
            "rich_50000" to { player.money >= 50000 },
            "rich_100000" to { player.money >= 100000 },
            "earn_total_100000" to { economySystem.totalEarned >= 100000 },
            "first_animal" to { animals.size >= 1 },
            "animal_10" to { animals.size >= 10 },
            "all_animals" to { AnimalType.entries.all { type -> animals.any { it.type == type } } },
            "survive_7" to { getTotalDays() >= 7 },
            "survive_28" to { getTotalDays() >= 28 },
            "survive_112" to { getTotalDays() >= 112 },
            "year_3" to { timeSystem.year >= 3 },
            "level_5" to { playerLevel >= 5 },
            "level_10" to { playerLevel >= 10 },
            "level_20" to { playerLevel >= 20 },
            "full_expand" to { player.landLevel >= 3 },
            "full_tools" to { player.toolLevel >= 3 },
            "all_missions" to { completedMissions.size >= MissionSystem.allMissions.size }
        )

        for ((id, check) in checks) {
            if (!unlockedAchievements.contains(id) && check()) {
                unlockAchievement(id)
            }
        }
    }

    private fun unlockAchievement(id: String) {
        unlockedAchievements.add(id)
        val achievement = AchievementSystem.getById(id) ?: return
        newAchievementId = id
        achievementPopupTimer = 4f
        AudioSystem.playSfx(SfxType.ACHIEVEMENT)
        addExp(20)
        notify("업적 달성! [${achievement.name}] - ${achievement.description}")
        notificationTimer = 4f
    }

    fun getTotalDays(): Int {
        return (timeSystem.year - 1) * timeSystem.daysPerSeason * 4 +
               timeSystem.season.ordinal * timeSystem.daysPerSeason +
               timeSystem.day
    }

    // === Delegated actions ===

    fun tillGround(): Boolean = farmManager.tillGround()
    fun waterGround(): Boolean = farmManager.waterGround()
    fun plantSeed(): Boolean = farmManager.plantSeed()
    fun harvest(): Boolean = farmManager.harvest()
    fun chopTree(): Boolean = farmManager.chopTree()
    fun mineRock(): Boolean = farmManager.mineRock()
    fun expandLand(): Boolean = farmManager.expandLand()

    fun startFishing(): Boolean = fishingManager.startFishing()
    fun catchFish(): Boolean = fishingManager.catchFish()
    fun cancelFishing() = fishingManager.cancelFishing()

    fun addAnimal(type: AnimalType): Boolean {
        if (!economySystem.buyAnimal(player, type)) {
            notify("돈이 부족합니다! (필요: ${type.buyPrice}원)")
            return false
        }
        val ax = (farmMinX + farmMaxX) / 2 + (-3..3).random()
        val ay = farmMaxY + 3
        animals.add(Animal(type, ax, ay))
        AudioSystem.playSfx(SfxType.ANIMAL_BUY)
        checkMissions()
        checkAchievements()
        notify("${type.koreanName}을(를) 구입했습니다!")
        return true
    }

    fun isAtMarket(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false
        return tile.type == TileType.MARKET
    }

    fun isNearMerchant(): Boolean {
        val px = player.tileX
        val py = player.tileY
        return npcs.any { it.data.name == "Merchant" &&
            Math.abs(it.tileX - px) <= 2 && Math.abs(it.tileY - py) <= 2 }
    }
}
