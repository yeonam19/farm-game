package com.farmgame.entity

import com.farmgame.data.ItemStack
import com.farmgame.data.ItemType
import com.farmgame.data.Tool

class Player(
    var tileX: Int = 25,
    var tileY: Int = 25
) {
    var renderX: Float = tileX.toFloat()
    var renderY: Float = tileY.toFloat()
    var facing: Direction = Direction.DOWN
    var currentTool: Tool = Tool.HOE
    var money: Int = 500
    var toolLevel: Int = 0
    var sprinklerLevel: Int = 0
    var landLevel: Int = 0
    var hasOx: Boolean = false  // 소 아이템 - 괭이 6칸 동시 경작
    var hasSickle: Boolean = false  // 낫 아이템 - 3x3 동시 수확

    // Fishing state
    var isFishing: Boolean = false
    var fishTimer: Float = 0f
    var fishBiteTimer: Float = 0f
    var hasBite: Boolean = false

    val inventory = mutableListOf<ItemStack>()
    private var moveTimer: Float = 0f
    private val moveDelay: Float = 0.12f

    val selectedSeedType: ItemType?
        get() = inventory.firstOrNull { it.type.isSeed && it.quantity > 0 }?.type

    var selectedSeedIndex: Int = 0

    fun getAvailableSeeds(): List<ItemStack> = inventory.filter { it.type.isSeed && it.quantity > 0 }

    fun cycleSeeds(direction: Int) {
        val seeds = getAvailableSeeds()
        if (seeds.isEmpty()) return
        selectedSeedIndex = ((selectedSeedIndex + direction) % seeds.size + seeds.size) % seeds.size
    }

    fun getSelectedSeed(): ItemStack? {
        val seeds = getAvailableSeeds()
        if (seeds.isEmpty()) return null
        selectedSeedIndex = selectedSeedIndex.coerceIn(0, seeds.size - 1)
        return seeds[selectedSeedIndex]
    }

    fun addItem(type: ItemType, quantity: Int = 1) {
        val existing = inventory.find { it.type == type }
        if (existing != null) {
            existing.quantity += quantity
        } else {
            inventory.add(ItemStack(type, quantity))
        }
    }

    fun removeItem(type: ItemType, quantity: Int = 1): Boolean {
        val existing = inventory.find { it.type == type } ?: return false
        if (existing.quantity < quantity) return false
        existing.quantity -= quantity
        if (existing.quantity <= 0) inventory.remove(existing)
        return true
    }

    fun hasItem(type: ItemType, quantity: Int = 1): Boolean {
        val existing = inventory.find { it.type == type } ?: return false
        return existing.quantity >= quantity
    }

    fun tryMove(dx: Int, dy: Int, delta: Float, isWalkable: (Int, Int) -> Boolean): Boolean {
        moveTimer += delta
        if (moveTimer < moveDelay) return false
        moveTimer = 0f

        if (dx != 0 || dy != 0) {
            facing = when {
                dy > 0 -> Direction.UP
                dy < 0 -> Direction.DOWN
                dx > 0 -> Direction.RIGHT
                dx < 0 -> Direction.LEFT
                else -> facing
            }
        }

        val newX = tileX + dx
        val newY = tileY + dy
        if (isWalkable(newX, newY)) {
            tileX = newX
            tileY = newY
            return true
        }
        return false
    }

    fun updateRender(delta: Float) {
        val speed = 10f * delta
        renderX += (tileX.toFloat() - renderX) * speed.coerceAtMost(1f)
        renderY += (tileY.toFloat() - renderY) * speed.coerceAtMost(1f)
    }

    fun getFacingTile(): Pair<Int, Int> = when (facing) {
        Direction.UP -> Pair(tileX, tileY + 1)
        Direction.DOWN -> Pair(tileX, tileY - 1)
        Direction.LEFT -> Pair(tileX - 1, tileY)
        Direction.RIGHT -> Pair(tileX + 1, tileY)
    }

    fun getToolSpeedMultiplier(): Float = 1f + toolLevel * 0.2f
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
