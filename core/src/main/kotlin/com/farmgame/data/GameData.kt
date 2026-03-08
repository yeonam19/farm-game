package com.farmgame.data

import com.badlogic.gdx.graphics.Color

// === Crop Definitions ===

enum class CropType(
    val displayName: String,
    val koreanName: String,
    val seedPrice: Int,
    val sellPrice: Int,
    val growthStages: Int,      // number of stages before harvest
    val growthTime: Float,      // seconds per stage
    val waterNeeded: Boolean,
    val seedColor: Color,
    val grownColor: Color,
    val season: Season
) {
    STRAWBERRY("Strawberry", "딸기", 10, 60, 4, 30f, true,
        Color(0.2f, 0.8f, 0.2f, 1f), Color(1f, 0.2f, 0.2f, 1f), Season.SPRING),
    TOMATO("Tomato", "토마토", 12, 70, 5, 35f, true,
        Color(0.3f, 0.7f, 0.2f, 1f), Color(0.9f, 0.1f, 0.1f, 1f), Season.SUMMER),
    CORN("Corn", "옥수수", 15, 90, 6, 40f, true,
        Color(0.2f, 0.6f, 0.1f, 1f), Color(1f, 0.9f, 0.2f, 1f), Season.SUMMER),
    POTATO("Potato", "감자", 8, 50, 4, 25f, true,
        Color(0.3f, 0.5f, 0.2f, 1f), Color(0.7f, 0.5f, 0.3f, 1f), Season.SPRING),
    CARROT("Carrot", "당근", 8, 45, 3, 20f, true,
        Color(0.2f, 0.7f, 0.3f, 1f), Color(1f, 0.5f, 0.1f, 1f), Season.AUTUMN),
    CABBAGE("Cabbage", "배추", 10, 55, 5, 35f, true,
        Color(0.3f, 0.8f, 0.3f, 1f), Color(0.4f, 0.9f, 0.3f, 1f), Season.AUTUMN),
    RADISH("Radish", "무", 5, 35, 3, 18f, true,
        Color(0.2f, 0.6f, 0.2f, 1f), Color(0.95f, 0.95f, 0.9f, 1f), Season.WINTER),
    SPINACH("Spinach", "시금치", 5, 30, 2, 15f, true,
        Color(0.1f, 0.5f, 0.1f, 1f), Color(0.2f, 0.7f, 0.2f, 1f), Season.WINTER);

    fun canGrowIn(season: Season): Boolean = this.season == season || season == this.season
}

// === Season ===

enum class Season(val displayName: String, val koreanName: String, val tint: Color) {
    SPRING("Spring", "봄", Color(0.85f, 1f, 0.85f, 1f)),
    SUMMER("Summer", "여름", Color(1f, 1f, 0.8f, 1f)),
    AUTUMN("Autumn", "가을", Color(1f, 0.9f, 0.75f, 1f)),
    WINTER("Winter", "겨울", Color(0.85f, 0.9f, 1f, 1f));

    fun next(): Season = entries[(ordinal + 1) % entries.size]
}

// === Weather ===

enum class Weather(val koreanName: String, val growthMultiplier: Float, val autoWater: Boolean) {
    SUNNY("맑음", 1.0f, false),
    CLOUDY("흐림", 0.8f, false),
    RAINY("비", 1.2f, true),
    STORMY("폭풍", 0.5f, true),
    SNOWY("눈", 0.3f, false);

    fun canOccurIn(season: Season): Boolean = when (this) {
        SNOWY -> season == Season.WINTER
        STORMY -> season == Season.SUMMER || season == Season.AUTUMN
        else -> true
    }
}

// === Tools ===

enum class Tool(val koreanName: String, val description: String) {
    HOE("괭이", "땅을 경작합니다"),
    WATERING_CAN("물뿌리개", "작물에 물을 줍니다"),
    SEED_BAG("씨앗 주머니", "씨앗을 심습니다"),
    HAND("손", "작물을 수확합니다"),
    AXE("도끼", "나무를 벱니다"),
    PICKAXE("곡괭이", "돌을 캡니다"),
    FISHING_ROD("낚싯대", "물에서 낚시를 합니다");
}

// === Items ===

data class ItemStack(
    val type: ItemType,
    var quantity: Int = 1
)

enum class ItemType(val koreanName: String, val sellPrice: Int, val isSeed: Boolean = false, val cropType: CropType? = null) {
    // Seeds
    STRAWBERRY_SEED("딸기 씨앗", 0, true, CropType.STRAWBERRY),
    TOMATO_SEED("토마토 씨앗", 0, true, CropType.TOMATO),
    CORN_SEED("옥수수 씨앗", 0, true, CropType.CORN),
    POTATO_SEED("감자 씨앗", 0, true, CropType.POTATO),
    CARROT_SEED("당근 씨앗", 0, true, CropType.CARROT),
    CABBAGE_SEED("배추 씨앗", 0, true, CropType.CABBAGE),
    RADISH_SEED("무 씨앗", 0, true, CropType.RADISH),
    SPINACH_SEED("시금치 씨앗", 0, true, CropType.SPINACH),

    // Harvested crops
    STRAWBERRY("딸기", 60),
    TOMATO("토마토", 70),
    CORN("옥수수", 90),
    POTATO("감자", 50),
    CARROT("당근", 45),
    CABBAGE("배추", 55),
    RADISH("무", 35),
    SPINACH("시금치", 30),

    // Resources
    WOOD("나무", 15),
    STONE("돌", 20),

    // Fish
    FISH_SMALL("붕어", 30),
    FISH_MEDIUM("잉어", 55),
    FISH_LARGE("연어", 90),
    FISH_RARE("금붕어", 200),
    FISH_TRASH("낡은 장화", 5);

    companion object {
        fun seedFor(cropType: CropType): ItemType = entries.first { it.cropType == cropType }
        fun harvestFor(cropType: CropType): ItemType = entries.first { it.name == cropType.name && !it.isSeed }
    }
}

// === Animal ===

enum class AnimalType(
    val koreanName: String,
    val buyPrice: Int,
    val productName: String,
    val productSellPrice: Int,
    val productionInterval: Float, // seconds
    val color: Color
) {
    CHICKEN("닭", 200, "달걀", 25, 60f, Color(1f, 1f, 0.8f, 1f)),
    COW("소", 800, "우유", 60, 120f, Color(0.9f, 0.85f, 0.8f, 1f)),
    SHEEP("양", 500, "양털", 45, 90f, Color(0.95f, 0.95f, 0.95f, 1f)),
    PIG("돼지", 600, "트러플", 80, 150f, Color(1f, 0.75f, 0.7f, 1f));
}

// === Upgrade ===

data class UpgradeInfo(
    val name: String,
    val koreanName: String,
    val description: String,
    val cost: Int,
    val level: Int
)

object Upgrades {
    val toolUpgrades = mapOf(
        1 to UpgradeInfo("Bronze Tools", "청동 도구", "작업 속도 20% 증가", 500, 1),
        2 to UpgradeInfo("Iron Tools", "철 도구", "작업 속도 40% 증가", 1500, 2),
        3 to UpgradeInfo("Gold Tools", "금 도구", "작업 속도 60% 증가", 5000, 3)
    )

    val landExpansions = mapOf(
        1 to UpgradeInfo("Small Expansion", "작은 확장", "농장 크기 +5x5", 1000, 1),
        2 to UpgradeInfo("Medium Expansion", "중간 확장", "농장 크기 +10x10", 3000, 2),
        3 to UpgradeInfo("Large Expansion", "큰 확장", "농장 크기 +15x15", 8000, 3)
    )

    val wateringCanUpgrades = mapOf(
        1 to UpgradeInfo("Sprinkler Lv1", "스프링클러 1단계", "매일 아침 농작물 자동 물주기 + 수동 3x3", 2000, 1),
        2 to UpgradeInfo("Sprinkler Lv2", "스프링클러 2단계", "매일 아침 농작물 자동 물주기 + 수동 5x5", 6000, 2)
    )
}

// === NPC ===

data class NPCData(
    val name: String,
    val koreanName: String,
    val role: String,
    val dialogues: List<String>,
    val color: Color
)

object NPCs {
    val villagers = listOf(
        NPCData("Merchant", "상인 민수", "상점 주인",
            listOf("어서오세요! 좋은 물건이 많답니다.", "오늘 씨앗이 잘 나가네요!", "농사가 잘 되고 있나요?"),
            Color(0.4f, 0.3f, 0.8f, 1f)),
        NPCData("Elder", "촌장 할아버지", "마을 촌장",
            listOf("오늘도 날씨가 좋구나.", "열심히 농사짓는 모습이 보기 좋아.", "이 마을의 미래가 밝구나!"),
            Color(0.6f, 0.5f, 0.4f, 1f)),
        NPCData("Fisher", "어부 영희", "낚시꾼",
            listOf("바다에서 큰 물고기를 잡았어요!", "오늘은 낚시하기 좋은 날이에요.", "가끔은 쉬는 것도 중요해요."),
            Color(0.3f, 0.6f, 0.8f, 1f)),
        NPCData("Farmer", "농부 철수", "선배 농부",
            listOf("봄에는 딸기가 최고지!", "물을 꾸준히 줘야 해.", "비 오는 날은 농사하기 좋은 날이야."),
            Color(0.5f, 0.7f, 0.3f, 1f))
    )
}
