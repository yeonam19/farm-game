package com.farmgame.world

import com.farmgame.data.ItemType
import com.farmgame.entity.Crop

/**
 * 고용 일꾼 AI — 농부/목동 작업 할당 및 행동
 */
class WorkerManager(private val world: GameWorld) {

    fun rebuildWorkers() {
        // Rebuild farmer list based on farmerLevel
        world.farmers.clear()
        val farmerDefs = listOf(
            Triple("철수", 4f, 0),
            Triple("영희", 2.5f, 2),
            Triple("달인", 1.5f, 4)
        )
        for (i in 0 until world.farmerLevel.coerceAtMost(3)) {
            val (name, interval, offset) = farmerDefs[i]
            val sx = (world.farmMinX + world.farmMaxX) / 2 + offset - 2
            val sy = (world.farmMinY + world.farmMaxY) / 2 + i * 2
            world.farmers.add(HiredWorker(name, i + 1, interval, sx, sy))
        }

        // Rebuild herder list based on herderLevel
        world.herders.clear()
        val herderDefs = listOf(
            Triple("민호", 5f, 0),
            Triple("수진", 3f, 3),
            Triple("목축달인", 1.5f, -3)
        )
        for (i in 0 until world.herderLevel.coerceAtMost(3)) {
            val (name, interval, offset) = herderDefs[i]
            val sx = (world.farmMinX + world.farmMaxX) / 2 + offset
            val sy = world.farmMaxY + 4 + i * 2
            world.herders.add(HiredWorker(name, i + 1, interval, sx, sy))
        }
    }

    fun updateWorker(worker: HiredWorker, delta: Float, type: String) {
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
        for (x in world.farmMinX..world.farmMaxX) {
            for (y in world.farmMinY..world.farmMaxY) {
                val tile = world.getTile(x, y) ?: continue
                if (tile.canHarvest) {
                    setWorkerTarget(worker, x, y, "harvest")
                    return
                }
            }
        }
        // Priority 2: Water dry farmland with crops
        for (x in world.farmMinX..world.farmMaxX) {
            for (y in world.farmMinY..world.farmMaxY) {
                val tile = world.getTile(x, y) ?: continue
                if (tile.isFarmable && tile.crop != null && !tile.isWatered) {
                    setWorkerTarget(worker, x, y, "water")
                    return
                }
            }
        }
        // Priority 3: Plant seeds on empty farmland
        val seed = world.player.getAvailableSeeds().firstOrNull { stack ->
            stack.type.cropType?.canGrowIn(world.timeSystem.season) == true
        }
        if (seed != null) {
            for (x in world.farmMinX..world.farmMaxX) {
                for (y in world.farmMinY..world.farmMaxY) {
                    val tile = world.getTile(x, y) ?: continue
                    if (tile.canPlant) {
                        setWorkerTarget(worker, x, y, "plant")
                        return
                    }
                }
            }
        }
        // Priority 4: Till grass in farm area
        for (x in world.farmMinX..world.farmMaxX) {
            for (y in world.farmMinY..world.farmMaxY) {
                val tile = world.getTile(x, y) ?: continue
                if (tile.type == TileType.GRASS || tile.type == TileType.DIRT) {
                    setWorkerTarget(worker, x, y, "till")
                    return
                }
            }
        }
    }

    private fun findHerderTaskFor(worker: HiredWorker) {
        // Priority 1: Collect products from animals
        for (animal in world.animals) {
            if (animal.hasProduct) {
                setWorkerTarget(worker, animal.tileX, animal.tileY, "collect")
                return
            }
        }
        // Priority 2: Feed unhappy animals
        for (animal in world.animals) {
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
            val tile = world.getTile(worker.x, worker.y) ?: return
            when (worker.pendingAction) {
                "harvest" -> {
                    if (tile.canHarvest) {
                        val crop = tile.crop!!
                        val harvestItem = ItemType.harvestFor(crop.type)
                        world.player.addItem(harvestItem)
                        tile.removeCrop()
                        tile.type = TileType.FARMLAND
                        world.totalHarvested++
                        world.harvestedCropTypes.add(crop.type.name)
                        world.addExp(5)
                    }
                }
                "water" -> {
                    if (tile.isFarmable && tile.crop != null && !tile.isWatered) {
                        tile.water()
                    }
                }
                "plant" -> {
                    if (tile.canPlant) {
                        val seed = world.player.getAvailableSeeds().firstOrNull { stack ->
                            stack.type.cropType?.canGrowIn(world.timeSystem.season) == true
                        }
                        if (seed != null) {
                            val cropType = seed.type.cropType
                            if (cropType != null) {
                                world.player.removeItem(seed.type)
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
                    for (animal in world.animals) {
                        if (animal.hasProduct && Math.abs(animal.tileX - worker.x) <= 1 && Math.abs(animal.tileY - worker.y) <= 1) {
                            animal.collect()
                            world.player.money += animal.type.productSellPrice
                            world.economySystem.totalEarned += animal.type.productSellPrice
                            world.addExp(5)
                            world.notify("${worker.name}: ${animal.type.productName} 수확! (+${animal.type.productSellPrice}원)")
                            break
                        }
                    }
                }
                "feed" -> {
                    for (animal in world.animals) {
                        if (animal.happiness < 0.5f && Math.abs(animal.tileX - worker.x) <= 1 && Math.abs(animal.tileY - worker.y) <= 1) {
                            animal.feed()
                            world.notify("${worker.name}: ${animal.type.koreanName}에게 먹이를 줬습니다!")
                            break
                        }
                    }
                }
            }
        }
        worker.pendingAction = ""
    }
}
