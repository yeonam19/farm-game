package com.farmgame.world

import com.farmgame.data.*
import com.farmgame.entity.*
import com.farmgame.system.*

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
    var newAchievementId: String? = null  // For popup display
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
    private var autoFishTimer: Float = 0f
    var autoFishX: Int = 0  // 설치 위치
    var autoFishY: Int = 0
    var autoFishRenderX: Float = 0f
    var autoFishRenderY: Float = 0f
    var autoFishCatchAnim: Float = 0f  // 잡을 때 애니메이션

    // === Worker system (농부 + 목동을 리스트로 관리) ===
    val farmers = mutableListOf<HiredWorker>()   // 농부들
    val herders = mutableListOf<HiredWorker>()   // 목동들
    var farmerLevel: Int = 0  // 고용된 농부 수 (1=철수, 2=+영희, 3=+달인)
    var herderLevel: Int = 0  // 고용된 목동 수
    val hiredFarmer: Boolean get() = farmerLevel > 0
    val hasHerder: Boolean get() = herderLevel > 0

    // Legacy accessors removed - all state is in HiredWorker objects

    init {
        generateMap()
        setupNPCs()
        setupCallbacks()
        giveStartingItems()
        assignFirstMission()
        rebuildWorkers()
        initAutoFishPosition()
    }

    fun rebuildWorkers() {
        // Rebuild farmer list based on farmerLevel
        farmers.clear()
        val farmerDefs = listOf(
            Triple("철수", 4f, 0),
            Triple("영희", 2.5f, 2),
            Triple("달인", 1.5f, 4)
        )
        for (i in 0 until farmerLevel.coerceAtMost(3)) {
            val (name, interval, offset) = farmerDefs[i]
            val sx = (farmMinX + farmMaxX) / 2 + offset - 2
            val sy = (farmMinY + farmMaxY) / 2 + i * 2
            farmers.add(HiredWorker(name, i + 1, interval, sx, sy))
        }

        // Rebuild herder list based on herderLevel
        herders.clear()
        val herderDefs = listOf(
            Triple("민호", 5f, 0),
            Triple("수진", 3f, 3),
            Triple("목축달인", 1.5f, -3)
        )
        for (i in 0 until herderLevel.coerceAtMost(3)) {
            val (name, interval, offset) = herderDefs[i]
            val sx = (farmMinX + farmMaxX) / 2 + offset
            val sy = farmMaxY + 4 + i * 2
            herders.add(HiredWorker(name, i + 1, interval, sx, sy))
        }
    }

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
        player.money = 500
    }

    private fun generateMap() {
        tiles = Array(mapWidth) { x ->
            Array(mapHeight) { y ->
                val tile = Tile(x, y)
                tile.type = when {
                    // Water border
                    x == 0 || y == 0 || x == mapWidth - 1 || y == mapHeight - 1 -> TileType.WATER
                    x == 1 || y == 1 || x == mapWidth - 2 || y == mapHeight - 2 -> TileType.SAND

                    // Pond
                    distSq(x, y, 10, 40) < 9 -> TileType.WATER
                    distSq(x, y, 10, 40) < 16 -> TileType.SAND

                    // Market area (top-right)
                    x in 38..42 && y in 38..42 -> TileType.PATH
                    x == 40 && y == 40 -> TileType.MARKET

                    // Player home
                    x in 24..26 && y in 34..36 -> TileType.PATH
                    x == 25 && y == 35 -> TileType.HOME

                    // Paths
                    x == 25 && y in 14..45 -> TileType.PATH
                    y == 25 && x in 5..45 -> TileType.PATH
                    y == 38 && x in 25..42 -> TileType.PATH

                    // Trees scattered
                    isTreePosition(x, y) -> TileType.TREE

                    // Rocks scattered
                    isRockPosition(x, y) -> TileType.ROCK

                    // Farm area fence
                    (x == farmMinX - 1 || x == farmMaxX + 1) && y in (farmMinY - 1)..(farmMaxY + 1) -> TileType.FENCE
                    (y == farmMinY - 1 || y == farmMaxY + 1) && x in (farmMinX - 1)..(farmMaxX + 1) -> TileType.FENCE

                    // Default grass
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
        if (x == 25 || y == 25 || y == 38) return false // not on paths
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
            // Dry out watered tiles
            for (x in 0 until mapWidth) {
                for (y in 0 until mapHeight) {
                    val tile = tiles[x][y]
                    tile.crop?.onNewDay(tile.isWatered)
                    tile.dryOut()
                }
            }

            // 스프링클러 자동 물주기 (매일 아침)
            if (player.sprinklerLevel > 0) {
                val range = if (player.sprinklerLevel >= 2) 2 else 1
                var watered = 0
                for (x in farmMinX..farmMaxX) {
                    for (y in farmMinY..farmMaxY) {
                        val tile = getTile(x, y) ?: continue
                        if (tile.isFarmable && tile.crop != null) {
                            // 스프링클러 범위 내 물주기
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
            }

            notify("새로운 하루가 시작되었습니다! (${timeSystem.getSeasonDay()})")
        }
        timeSystem.onNewSeason = {
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

                    // Auto-water from rain
                    if (weatherSystem.current.autoWater && tile.isFarmable) {
                        tile.water()
                    }
                }
            }
        }

        // Update fishing
        updateFishing(delta)

        // Auto-fishing device
        if (autoFishLevel > 0) {
            updateAutoFish(delta)
        }

        // Update all hired workers
        for (farmer in farmers) updateWorker(farmer, delta, "farmer")
        for (herder in herders) updateWorker(herder, delta, "herder")

        // Update animals
        animals.forEach { it.update(delta, mapWidth, mapHeight) }

        // Update NPCs
        npcs.forEach { it.update(delta) { x, y -> isWalkable(x, y) } }

        // Notification timer
        if (notificationTimer > 0) {
            notificationTimer -= delta
            if (notificationTimer <= 0) {
                notification = ""
                // Show pending notification if any
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

        // Periodic mission/achievement check (every 2 seconds)
        missionCheckTimer += delta
        if (missionCheckTimer >= 2f) {
            missionCheckTimer = 0f
            checkMissions()
            checkAchievements()
        }

        // Auto-save every 6 game hours (= 180 real seconds, since 720s = 24 game hours)
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
            notify("레벨 업! Lv.${playerLevel} - ${MissionSystem.getLevelTitle(playerLevel)}")
        }
    }

    fun checkMissions() {
        // Update progress for all missions
        updateMissionProgress()

        // Check active mission completion - loop to chain-complete multiple missions
        var safetyCounter = 0
        while (safetyCounter < 5) {  // max 5 chain completions per call
            safetyCounter++
            val activeId = activeMissionId ?: return
            val mission = MissionSystem.getMissionById(activeId) ?: return
            if (completedMissions[activeId] == true) {
                // Already completed but still active - advance to next
                activeMissionId = MissionSystem.getNextMission(activeId)
                continue
            }

            val progress = missionProgress[activeId] ?: 0
            if (progress < mission.target) return  // Not yet complete

            completedMissions[activeId] = true
            addExp(mission.expReward)
            player.money += mission.moneyReward

            // Give item rewards
            val itemRewardText = StringBuilder()
            for (reward in mission.itemRewards) {
                try {
                    val itemType = ItemType.valueOf(reward.itemName)
                    player.addItem(itemType, reward.quantity)
                    itemRewardText.append(" +${itemType.koreanName}x${reward.quantity}")
                } catch (_: Exception) {}
            }

            notify("미션 완료! [${mission.name}] +${mission.expReward}EXP +${mission.moneyReward}원$itemRewardText")
            notificationTimer = 5f

            // Assign next mission
            val nextId = MissionSystem.getNextMission(activeId)
            activeMissionId = nextId
            if (nextId != null) {
                val nextMission = MissionSystem.getMissionById(nextId)
                if (nextMission != null) {
                    pendingNotification = "새 미션! [${nextMission.name}] - ${nextMission.description}"
                }
                // Update progress and loop to check if next mission is also already complete
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
            // Farming
            "first_harvest" to { totalHarvested >= 1 },
            "harvest_100" to { totalHarvested >= 100 },
            "harvest_500" to { totalHarvested >= 500 },
            "all_crops" to { harvestedCropTypes.size >= CropType.entries.size },

            // Fishing
            "first_fish" to { totalFished >= 1 },
            "fish_50" to { totalFished >= 50 },
            "rare_fish" to { player.inventory.any { it.type == ItemType.FISH_RARE && it.quantity > 0 } || totalFished > 0 && unlockedAchievements.contains("rare_fish") },
            "fish_trash" to { totalFishTrash >= 10 },

            // Gathering
            "chop_100" to { totalTreesChopped >= 100 },
            "mine_100" to { totalRocksMined >= 100 },

            // Economy
            "first_1000" to { player.money >= 1000 },
            "rich_10000" to { player.money >= 10000 },
            "rich_50000" to { player.money >= 50000 },
            "rich_100000" to { player.money >= 100000 },
            "earn_total_100000" to { economySystem.totalEarned >= 100000 },

            // Animal
            "first_animal" to { animals.size >= 1 },
            "animal_10" to { animals.size >= 10 },
            "all_animals" to { AnimalType.entries.all { type -> animals.any { it.type == type } } },

            // Time
            "survive_7" to { getTotalDays() >= 7 },
            "survive_28" to { getTotalDays() >= 28 },
            "survive_112" to { getTotalDays() >= 112 },
            "year_3" to { timeSystem.year >= 3 },

            // Special
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
        addExp(20)
        notify("업적 달성! [${achievement.name}] - ${achievement.description}")
        notificationTimer = 4f
    }

    fun getTotalDays(): Int {
        return (timeSystem.year - 1) * timeSystem.daysPerSeason * 4 +
               timeSystem.season.ordinal * timeSystem.daysPerSeason +
               timeSystem.day
    }

    // === Actions ===

    fun tillGround(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false
        if (tile.type == TileType.GRASS || tile.type == TileType.DIRT) {
            if (fx in farmMinX..farmMaxX && fy in farmMinY..farmMaxY) {
                tile.till()

                // 소 아이템: 바라보는 방향으로 6칸 동시 경작
                if (player.hasOx) {
                    for (i in 2..6) {
                        val (ex, ey) = when (player.facing) {
                            com.farmgame.entity.Direction.UP -> Pair(fx, fy + (i - 1))
                            com.farmgame.entity.Direction.DOWN -> Pair(fx, fy - (i - 1))
                            com.farmgame.entity.Direction.LEFT -> Pair(fx - (i - 1), fy)
                            com.farmgame.entity.Direction.RIGHT -> Pair(fx + (i - 1), fy)
                        }
                        val extraTile = getTile(ex, ey)
                        if (extraTile != null && (extraTile.type == TileType.GRASS || extraTile.type == TileType.DIRT)
                            && ex in farmMinX..farmMaxX && ey in farmMinY..farmMaxY) {
                            extraTile.till()
                        }
                    }
                    notify("소가 힘차게 6칸을 경작했습니다!")
                } else {
                    notify("땅을 경작했습니다! 이제 씨앗(3번)으로 심을 수 있어요.")
                }
                return true
            } else {
                notify("울타리 안 농장 구역에서만 경작할 수 있습니다.")
            }
        } else if (tile.isFarmable) {
            notify("이미 경작된 땅입니다. 씨앗(3번)을 심어보세요!")
        }
        return false
    }

    fun waterGround(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false
        if (tile.isFarmable) {
            tile.water()

            // Sprinkler effect
            if (player.sprinklerLevel > 0) {
                val range = if (player.sprinklerLevel >= 2) 2 else 1
                for (dx in -range..range) {
                    for (dy in -range..range) {
                        getTile(fx + dx, fy + dy)?.let {
                            if (it.isFarmable) it.water()
                        }
                    }
                }
            }

            notify("물을 주었습니다!")
            return true
        }
        return false
    }

    fun plantSeed(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false
        if (!tile.canPlant) {
            if (tile.type == TileType.GRASS || tile.type == TileType.DIRT) {
                notify("먼저 괭이(1번)로 땅을 경작하세요!")
            } else if (!tile.isFarmable) {
                notify("경작지에만 심을 수 있습니다. 괭이(1번)로 풀밭을 경작하세요!")
            } else if (tile.crop != null) {
                notify("이미 작물이 심어져 있습니다.")
            }
            return false
        }

        val seedStack = player.getSelectedSeed()
        if (seedStack == null) {
            notify("씨앗이 없습니다! 상점에서 구매하세요. (시장/상인 근처에서 Space)")
            return false
        }

        val cropType = seedStack.type.cropType ?: return false
        if (!cropType.canGrowIn(timeSystem.season)) {
            notify("${cropType.koreanName}은(는) ${cropType.season.koreanName}에만 심을 수 있습니다.")
            return false
        }

        // 소 아이템: 바라보는 방향으로 6칸 동시 파종
        if (player.hasOx) {
            var count = 0
            for (i in 0..5) {
                val (sx, sy) = when (player.facing) {
                    com.farmgame.entity.Direction.UP -> Pair(fx, fy + i)
                    com.farmgame.entity.Direction.DOWN -> Pair(fx, fy - i)
                    com.farmgame.entity.Direction.LEFT -> Pair(fx - i, fy)
                    com.farmgame.entity.Direction.RIGHT -> Pair(fx + i, fy)
                }
                val t = getTile(sx, sy) ?: continue
                if (t.canPlant) {
                    val seed = player.getSelectedSeed() ?: break
                    val ct = seed.type.cropType ?: break
                    if (!ct.canGrowIn(timeSystem.season)) break
                    player.removeItem(seed.type)
                    t.crop = Crop(ct)
                    count++
                }
            }
            if (count > 0) {
                notify("소가 힘차게 ${count}칸에 씨앗을 심었습니다!")
                return true
            }
        } else {
            player.removeItem(seedStack.type)
            tile.crop = Crop(cropType)
            notify("${cropType.koreanName} 씨앗을 심었습니다!")
            return true
        }
        return false
    }

    fun harvest(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false

        // Check for animal product
        for (animal in animals) {
            if (animal.tileX == fx && animal.tileY == fy && animal.hasProduct) {
                animal.collect()
                player.money += animal.type.productSellPrice
                notify("${animal.type.productName}을(를) 수확했습니다! (+${animal.type.productSellPrice}원)")
                return true
            }
        }

        // Check for NPC dialogue
        for (npc in npcs) {
            if (npc.tileX == fx && npc.tileY == fy) {
                notify(npc.getDialogue())
                return true
            }
        }

        // Check for market
        if (tile.type == TileType.MARKET) {
            return false // handled by UI
        }

        if (!tile.canHarvest) {
            if (tile.crop != null && !tile.crop!!.isReady) {
                val crop = tile.crop!!
                val progress = (crop.currentStage.toFloat() / crop.type.growthStages * 100).toInt()
                notify("${crop.type.koreanName}: 성장 중... (${progress}%)")
            }
            return false
        }

        // 낫 아이템: 3x3 범위 동시 수확
        if (player.hasSickle) {
            var count = 0
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val t = getTile(fx + dx, fy + dy) ?: continue
                    if (t.canHarvest) {
                        val c = t.crop!!
                        player.addItem(ItemType.harvestFor(c.type))
                        harvestedCropTypes.add(c.type.name)
                        t.removeCrop()
                        t.type = TileType.FARMLAND
                        totalHarvested++
                        count++
                    }
                }
            }
            if (count > 0) {
                addExp(10 * count)
                checkMissions()
                checkAchievements()
                notify("낫으로 ${count}칸을 한꺼번에 수확했습니다!")
                return true
            }
        } else {
            val crop = tile.crop!!
            val harvestItem = ItemType.harvestFor(crop.type)
            player.addItem(harvestItem)
            tile.removeCrop()
            tile.type = TileType.FARMLAND
            totalHarvested++
            harvestedCropTypes.add(crop.type.name)
            addExp(10)
            checkMissions()
            checkAchievements()
            notify("${crop.type.koreanName}을(를) 수확했습니다!")
            return true
        }
        return false
    }

    fun chopTree(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false
        if (tile.type == TileType.TREE) {
            tile.type = TileType.GRASS
            player.addItem(ItemType.WOOD, 2)
            totalTreesChopped++
            addExp(5)
            checkMissions()
            checkAchievements()
            notify("나무를 베었습니다! (나무 x2)")
            return true
        }
        return false
    }

    fun mineRock(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false
        if (tile.type == TileType.ROCK) {
            tile.type = TileType.GRASS
            player.addItem(ItemType.STONE, 2)
            totalRocksMined++
            addExp(5)
            checkMissions()
            checkAchievements()
            notify("돌을 캤습니다! (돌 x2)")
            return true
        }
        return false
    }

    fun expandLand(): Boolean {
        val nextLevel = player.landLevel + 1
        val upgrade = Upgrades.landExpansions[nextLevel] ?: run {
            notify("더 이상 확장할 수 없습니다!")
            return false
        }
        if (!economySystem.buyUpgrade(player, upgrade.cost)) {
            notify("돈이 부족합니다! (필요: ${upgrade.cost}원)")
            return false
        }

        // Save existing farmland tiles and crops
        data class SavedTile(val x: Int, val y: Int, val type: TileType, val isWatered: Boolean, val crop: Crop?)
        val savedTiles = mutableListOf<SavedTile>()
        for (x in 0 until mapWidth) {
            for (y in 0 until mapHeight) {
                val tile = tiles[x][y]
                if (tile.type == TileType.FARMLAND || tile.type == TileType.FARMLAND_WET || tile.crop != null) {
                    savedTiles.add(SavedTile(x, y, tile.type, tile.isWatered, tile.crop))
                }
            }
        }

        player.landLevel = nextLevel
        val expansion = nextLevel * 5
        farmMinX -= expansion / 2
        farmMinY -= expansion / 2
        farmMaxX += expansion / 2
        farmMaxY += expansion / 2
        farmMinX = farmMinX.coerceAtLeast(3)
        farmMinY = farmMinY.coerceAtLeast(3)
        farmMaxX = farmMaxX.coerceAtMost(mapWidth - 4)
        farmMaxY = farmMaxY.coerceAtMost(mapHeight - 4)

        // Regenerate map (fences, trees, etc.)
        generateMap()

        // Restore saved farmland and crops
        for (saved in savedTiles) {
            if (saved.x in 0 until mapWidth && saved.y in 0 until mapHeight) {
                val tile = tiles[saved.x][saved.y]
                // Only restore if tile is within farm bounds (not overwriting structures)
                if (saved.x in farmMinX..farmMaxX && saved.y in farmMinY..farmMaxY) {
                    tile.type = saved.type
                    tile.isWatered = saved.isWatered
                    tile.crop = saved.crop
                }
            }
        }

        notify("${upgrade.koreanName} 완료! 기존 농작물이 보존되었습니다!")
        return true
    }

    fun addAnimal(type: AnimalType): Boolean {
        if (!economySystem.buyAnimal(player, type)) {
            notify("돈이 부족합니다! (필요: ${type.buyPrice}원)")
            return false
        }
        val ax = (farmMinX + farmMaxX) / 2 + (-3..3).random()
        val ay = farmMaxY + 3
        animals.add(Animal(type, ax, ay))
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

    // === Fishing ===

    fun startFishing(): Boolean {
        val (fx, fy) = player.getFacingTile()
        val tile = getTile(fx, fy) ?: return false
        if (tile.type != TileType.WATER) {
            notify("물가를 향해 낚싯대를 사용하세요!")
            return false
        }
        if (player.isFishing) {
            // Already fishing - try to catch
            return catchFish()
        }
        player.isFishing = true
        player.fishTimer = 0f
        player.fishBiteTimer = 2f + Math.random().toFloat() * 4f // 2~6 seconds for bite
        player.hasBite = false
        notify("낚시를 시작합니다... 입질을 기다리세요!")
        return true
    }

    fun catchFish(): Boolean {
        if (!player.isFishing) return false
        if (player.hasBite) {
            // Successful catch!
            val roll = Math.random()
            val fish = when {
                roll < 0.05 -> ItemType.FISH_RARE
                roll < 0.20 -> ItemType.FISH_LARGE
                roll < 0.50 -> ItemType.FISH_MEDIUM
                roll < 0.85 -> ItemType.FISH_SMALL
                else -> ItemType.FISH_TRASH
            }
            player.addItem(fish)
            player.isFishing = false
            totalFished++
            if (fish == ItemType.FISH_TRASH) totalFishTrash++
            if (fish == ItemType.FISH_RARE) unlockedAchievements.add("rare_fish")
            addExp(15)
            checkMissions()
            checkAchievements()
            notify("${fish.koreanName}을(를) 잡았습니다! (+${fish.sellPrice}원)")
            return true
        } else {
            // Too early - fish got away
            player.isFishing = false
            notify("너무 일찍 당겼습니다! 물고기가 도망갔어요.")
            return false
        }
    }

    fun updateFishing(delta: Float) {
        if (!player.isFishing) return
        player.fishTimer += delta
        if (!player.hasBite && player.fishTimer >= player.fishBiteTimer) {
            player.hasBite = true
            notify("입질이 왔습니다! Space를 누르세요!")
        }
        // If bite came but player didn't catch within 2 seconds, fish escapes
        if (player.hasBite && player.fishTimer > player.fishBiteTimer + 2f) {
            player.isFishing = false
            player.hasBite = false
            notify("물고기가 도망갔습니다...")
        }
    }

    fun cancelFishing() {
        player.isFishing = false
        player.hasBite = false
        player.fishTimer = 0f
    }

    // === Unified Worker Update System ===

    private fun updateWorker(worker: HiredWorker, delta: Float, type: String) {
        // Smooth render position
        val moveSpeed = 4f * delta
        worker.renderX += (worker.x.toFloat() - worker.renderX) * moveSpeed.coerceAtMost(1f)
        worker.renderY += (worker.y.toFloat() - worker.renderY) * moveSpeed.coerceAtMost(1f)

        // Action animation timer
        if (worker.actionTimer > 0) {
            worker.actionTimer -= delta
            if (worker.actionTimer <= 0) {
                performWorkerAction(worker, type)
                worker.action = "idle"
            }
            return
        }

        // Walking to target
        if (worker.x != worker.targetX || worker.y != worker.targetY) {
            worker.moveTimer += delta
            if (worker.moveTimer >= 0.25f) {
                worker.moveTimer = 0f
                worker.action = "walk"
                val dx = worker.targetX.compareTo(worker.x)
                val dy = worker.targetY.compareTo(worker.y)
                worker.x += dx
                worker.y += dy
                if (worker.x == worker.targetX && worker.y == worker.targetY && worker.pendingAction.isNotEmpty()) {
                    worker.action = worker.pendingAction
                    worker.actionTimer = if (type == "farmer") 1.2f else 1.0f
                }
            }
            return
        }

        // Work interval check
        worker.workTimer += delta
        if (worker.workTimer < worker.workInterval) {
            worker.action = "idle"
            return
        }
        worker.workTimer = 0f

        // Find next task
        if (type == "farmer") findFarmerTaskFor(worker) else findHerderTaskFor(worker)
    }

    private fun findFarmerTaskFor(worker: HiredWorker) {
        // Priority 1: Harvest ready crops
        for (x in farmMinX..farmMaxX) {
            for (y in farmMinY..farmMaxY) {
                val tile = getTile(x, y) ?: continue
                if (tile.canHarvest) {
                    setWorkerTarget(worker, x, y, "harvest")
                    return
                }
            }
        }
        // Priority 2: Water dry farmland with crops
        for (x in farmMinX..farmMaxX) {
            for (y in farmMinY..farmMaxY) {
                val tile = getTile(x, y) ?: continue
                if (tile.isFarmable && tile.crop != null && !tile.isWatered) {
                    setWorkerTarget(worker, x, y, "water")
                    return
                }
            }
        }
        // Priority 3: Plant seeds on empty farmland
        val seed = player.getAvailableSeeds().firstOrNull { stack ->
            stack.type.cropType?.canGrowIn(timeSystem.season) == true
        }
        if (seed != null) {
            for (x in farmMinX..farmMaxX) {
                for (y in farmMinY..farmMaxY) {
                    val tile = getTile(x, y) ?: continue
                    if (tile.canPlant) {
                        setWorkerTarget(worker, x, y, "plant")
                        return
                    }
                }
            }
        }
        // Priority 4: Till grass in farm area
        for (x in farmMinX..farmMaxX) {
            for (y in farmMinY..farmMaxY) {
                val tile = getTile(x, y) ?: continue
                if (tile.type == TileType.GRASS || tile.type == TileType.DIRT) {
                    setWorkerTarget(worker, x, y, "till")
                    return
                }
            }
        }
    }

    private fun findHerderTaskFor(worker: HiredWorker) {
        // Priority 1: Collect products from animals
        for (animal in animals) {
            if (animal.hasProduct) {
                setWorkerTarget(worker, animal.tileX, animal.tileY, "collect")
                return
            }
        }
        // Priority 2: Feed unhappy animals
        for (animal in animals) {
            if (animal.happiness < 0.5f) {
                setWorkerTarget(worker, animal.tileX, animal.tileY, "feed")
                return
            }
        }
    }

    private fun setWorkerTarget(worker: HiredWorker, x: Int, y: Int, action: String) {
        worker.targetX = x
        worker.targetY = y
        worker.action = "walk"
        worker.pendingAction = action
    }

    private fun performWorkerAction(worker: HiredWorker, type: String) {
        if (type == "farmer") {
            val tile = getTile(worker.x, worker.y) ?: return
            when (worker.pendingAction) {
                "harvest" -> {
                    if (tile.canHarvest) {
                        val crop = tile.crop!!
                        val harvestItem = ItemType.harvestFor(crop.type)
                        player.addItem(harvestItem)
                        tile.removeCrop()
                        tile.type = TileType.FARMLAND
                        totalHarvested++
                        harvestedCropTypes.add(crop.type.name)
                        addExp(5)
                    }
                }
                "water" -> {
                    if (tile.isFarmable && tile.crop != null && !tile.isWatered) {
                        tile.water()
                    }
                }
                "plant" -> {
                    if (tile.canPlant) {
                        val seed = player.getAvailableSeeds().firstOrNull { stack ->
                            stack.type.cropType?.canGrowIn(timeSystem.season) == true
                        }
                        if (seed != null) {
                            val cropType = seed.type.cropType
                            if (cropType != null) {
                                player.removeItem(seed.type)
                                tile.crop = Crop(cropType)
                            }
                        }
                    }
                }
                "till" -> {
                    if (tile.type == TileType.GRASS || tile.type == TileType.DIRT) {
                        tile.till()
                    }
                }
            }
        } else {
            // Herder actions
            when (worker.pendingAction) {
                "collect" -> {
                    for (animal in animals) {
                        if (animal.hasProduct && Math.abs(animal.tileX - worker.x) <= 1 && Math.abs(animal.tileY - worker.y) <= 1) {
                            animal.collect()
                            player.money += animal.type.productSellPrice
                            economySystem.totalEarned += animal.type.productSellPrice
                            addExp(5)
                            notify("${worker.name}: ${animal.type.productName} 수확! (+${animal.type.productSellPrice}원)")
                            break
                        }
                    }
                }
                "feed" -> {
                    for (animal in animals) {
                        if (animal.happiness < 0.5f && Math.abs(animal.tileX - worker.x) <= 1 && Math.abs(animal.tileY - worker.y) <= 1) {
                            animal.feed()
                            notify("${worker.name}: ${animal.type.koreanName}에게 먹이를 줬습니다!")
                            break
                        }
                    }
                }
            }
        }
        worker.pendingAction = ""
    }

    // === Auto Fishing Device ===

    private fun updateAutoFish(delta: Float) {
        // Animation decay
        if (autoFishCatchAnim > 0) autoFishCatchAnim -= delta

        val interval = when (autoFishLevel) {
            1 -> 15f   // 기본 통발: 15초마다
            2 -> 8f    // 고급 통발: 8초마다
            3 -> 4f    // 자동 낚시선: 4초마다
            else -> 999f
        }

        autoFishTimer += delta
        if (autoFishTimer < interval) return
        autoFishTimer = 0f

        // Better fish odds with higher level
        val roll = Math.random()
        val fish = when (autoFishLevel) {
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

        player.addItem(fish)
        totalFished++
        if (fish == ItemType.FISH_TRASH) totalFishTrash++
        if (fish == ItemType.FISH_RARE) unlockedAchievements.add("rare_fish")
        addExp(5)
        autoFishCatchAnim = 2f  // trigger catch animation
        notify("통발: ${fish.koreanName} 획득!")
    }

}
