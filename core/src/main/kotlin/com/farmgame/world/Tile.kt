package com.farmgame.world

import com.badlogic.gdx.graphics.Color
import com.farmgame.entity.Crop

enum class TileType(val koreanName: String, val color: Color, val walkable: Boolean, val farmable: Boolean) {
    GRASS("풀밭", Color(0.4f, 0.75f, 0.3f, 1f), true, true),
    DIRT("흙", Color(0.55f, 0.4f, 0.25f, 1f), true, false),
    FARMLAND("경작지", Color(0.45f, 0.3f, 0.15f, 1f), true, false),
    FARMLAND_WET("젖은 경작지", Color(0.35f, 0.22f, 0.1f, 1f), true, false),
    WATER("물", Color(0.2f, 0.5f, 0.9f, 1f), false, false),
    SAND("모래", Color(0.9f, 0.85f, 0.6f, 1f), true, false),
    PATH("길", Color(0.7f, 0.65f, 0.5f, 1f), true, false),
    TREE("나무", Color(0.15f, 0.5f, 0.15f, 1f), false, false),
    ROCK("바위", Color(0.5f, 0.5f, 0.5f, 1f), false, false),
    FENCE("울타리", Color(0.6f, 0.45f, 0.25f, 1f), false, false),
    BUILDING("건물", Color(0.7f, 0.6f, 0.5f, 1f), false, false),
    MARKET("시장", Color(0.8f, 0.6f, 0.2f, 1f), true, false),
    HOME("집", Color(0.8f, 0.4f, 0.3f, 1f), true, false);
}

class Tile(
    val x: Int,
    val y: Int,
    var type: TileType = TileType.GRASS
) {
    var crop: Crop? = null
    var isWatered: Boolean = false

    val isFarmable: Boolean get() = type == TileType.FARMLAND || type == TileType.FARMLAND_WET
    val canPlant: Boolean get() = isFarmable && crop == null
    val canHarvest: Boolean get() = crop?.isReady == true

    fun till() {
        if (type == TileType.GRASS || type == TileType.DIRT) {
            type = TileType.FARMLAND
        }
    }

    fun water() {
        if (isFarmable) {
            isWatered = true
            type = TileType.FARMLAND_WET
        }
    }

    fun dryOut() {
        if (type == TileType.FARMLAND_WET) {
            isWatered = false
            type = TileType.FARMLAND
        }
    }

    fun removeCrop() {
        crop = null
    }
}
