package com.farmgame.system

import com.farmgame.data.*
import com.farmgame.entity.Player

class EconomySystem {
    var totalEarned: Int = 0
    var totalSpent: Int = 0

    fun sellItem(player: Player, itemType: ItemType, quantity: Int = 1): Boolean {
        if (!player.hasItem(itemType, quantity)) return false
        if (itemType.sellPrice <= 0) return false

        val total = itemType.sellPrice * quantity
        player.removeItem(itemType, quantity)
        player.money += total
        totalEarned += total
        return true
    }

    fun sellAllCrops(player: Player): Int {
        var totalSold = 0
        val sellable = player.inventory.filter { !it.type.isSeed && it.type.sellPrice > 0 && it.type != ItemType.WOOD && it.type != ItemType.STONE }
            .toList() // copy to avoid concurrent modification

        for (item in sellable) {
            val amount = item.type.sellPrice * item.quantity
            totalSold += amount
            player.money += amount
            totalEarned += amount
            player.removeItem(item.type, item.quantity)
        }
        return totalSold
    }

    fun buySeed(player: Player, cropType: CropType, quantity: Int = 1): Boolean {
        val cost = cropType.seedPrice * quantity
        if (player.money < cost) return false

        player.money -= cost
        totalSpent += cost
        player.addItem(ItemType.seedFor(cropType), quantity)
        return true
    }

    fun buyAnimal(player: Player, animalType: AnimalType): Boolean {
        if (player.money < animalType.buyPrice) return false
        player.money -= animalType.buyPrice
        totalSpent += animalType.buyPrice
        return true
    }

    fun buyUpgrade(player: Player, cost: Int): Boolean {
        if (player.money < cost) return false
        player.money -= cost
        totalSpent += cost
        return true
    }
}
