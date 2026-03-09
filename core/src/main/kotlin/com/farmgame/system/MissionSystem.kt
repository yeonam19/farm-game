package com.farmgame.system

data class MissionRewardItem(
    val itemName: String,  // ItemType.name
    val quantity: Int
)

data class Mission(
    val id: String,
    val name: String,
    val description: String,
    val target: Int,
    val expReward: Int,
    val moneyReward: Int,
    val category: MissionCategory,
    val itemRewards: List<MissionRewardItem> = emptyList()
)

enum class MissionCategory(val koreanName: String) {
    FARMING("농사"),
    FISHING("낚시"),
    GATHERING("채집"),
    ECONOMY("경제"),
    ANIMAL("동물"),
    EXPLORATION("탐험")
}

object MissionSystem {
    // Ordered mission chain - player gets these one at a time
    val missionChain: List<String> = listOf(
        "harvest_10",   // 1. First farming
        "fish_5",       // 2. Try fishing
        "chop_10",      // 3. Gather wood
        "mine_10",      // 4. Mine rocks
        "earn_1000",    // 5. Make some money
        "animal_1",     // 6. Buy first animal
        "harvest_50",   // 7. More farming
        "fish_20",      // 8. More fishing
        "expand_1",     // 9. Expand land
        "upgrade_tool", // 10. Upgrade tools
        "earn_5000",    // 11. More money
        "chop_50",      // 12. Lots of wood
        "mine_50",      // 13. Lots of rocks
        "animal_5",     // 14. Build a ranch
        "harvest_200",  // 15. Expert farmer
        "fish_50",      // 16. Expert fisher
        "earn_20000",   // 17. Getting rich
        "harvest_500",  // 18. Legendary farmer
        "fish_100",     // 19. King of the sea
        "earn_50000"    // 20. Tycoon
    )

    fun getNextMission(currentId: String?): String? {
        if (currentId == null) return missionChain.firstOrNull()
        val idx = missionChain.indexOf(currentId)
        if (idx < 0 || idx + 1 >= missionChain.size) return null
        return missionChain[idx + 1]
    }

    fun getMissionById(id: String): Mission? = allMissions.find { it.id == id }

    val allMissions: List<Mission> = listOf(
        // Farming missions
        Mission("harvest_10", "초보 농부", "작물 10개 수확", 10, 50, 100, MissionCategory.FARMING,
            listOf(MissionRewardItem("TOMATO_SEED", 10), MissionRewardItem("CORN_SEED", 5))),
        Mission("harvest_50", "숙련 농부", "작물 50개 수확", 50, 150, 500, MissionCategory.FARMING,
            listOf(MissionRewardItem("CABBAGE_SEED", 15), MissionRewardItem("CARROT_SEED", 15))),
        Mission("harvest_200", "농업 달인", "작물 200개 수확", 200, 500, 2000, MissionCategory.FARMING,
            listOf(MissionRewardItem("STRAWBERRY_SEED", 30), MissionRewardItem("TOMATO_SEED", 30))),
        Mission("harvest_500", "전설의 농부", "작물 500개 수확", 500, 1000, 5000, MissionCategory.FARMING,
            listOf(MissionRewardItem("CORN_SEED", 50))),

        // Fishing missions
        Mission("fish_5", "낚시 입문", "물고기 5마리 낚기", 5, 40, 80, MissionCategory.FISHING,
            listOf(MissionRewardItem("POTATO_SEED", 10))),
        Mission("fish_20", "낚시 중급", "물고기 20마리 낚기", 20, 120, 400, MissionCategory.FISHING,
            listOf(MissionRewardItem("CARROT_SEED", 20))),
        Mission("fish_50", "낚시 고수", "물고기 50마리 낚기", 50, 300, 1000, MissionCategory.FISHING,
            listOf(MissionRewardItem("RADISH_SEED", 25), MissionRewardItem("SPINACH_SEED", 25))),
        Mission("fish_100", "바다의 왕", "물고기 100마리 낚기", 100, 600, 3000, MissionCategory.FISHING,
            listOf(MissionRewardItem("CABBAGE_SEED", 40))),

        // Gathering missions
        Mission("chop_10", "나무꾼 견습", "나무 10그루 베기", 10, 30, 60, MissionCategory.GATHERING,
            listOf(MissionRewardItem("STRAWBERRY_SEED", 8))),
        Mission("chop_50", "벌목 전문가", "나무 50그루 베기", 50, 150, 400, MissionCategory.GATHERING,
            listOf(MissionRewardItem("WOOD", 30))),
        Mission("mine_10", "채광 입문", "바위 10개 캐기", 10, 30, 60, MissionCategory.GATHERING,
            listOf(MissionRewardItem("POTATO_SEED", 8))),
        Mission("mine_50", "채광 전문가", "바위 50개 캐기", 50, 150, 400, MissionCategory.GATHERING,
            listOf(MissionRewardItem("STONE", 30))),

        // Economy missions
        Mission("earn_1000", "돈 모으기", "총 1,000원 벌기", 1000, 60, 200, MissionCategory.ECONOMY,
            listOf(MissionRewardItem("TOMATO_SEED", 10))),
        Mission("earn_5000", "부자 되기", "총 5,000원 벌기", 5000, 200, 800, MissionCategory.ECONOMY,
            listOf(MissionRewardItem("CORN_SEED", 20), MissionRewardItem("CABBAGE_SEED", 20))),
        Mission("earn_20000", "거부", "총 20,000원 벌기", 20000, 500, 3000, MissionCategory.ECONOMY,
            listOf(MissionRewardItem("STRAWBERRY_SEED", 40))),
        Mission("earn_50000", "재벌", "총 50,000원 벌기", 50000, 1000, 10000, MissionCategory.ECONOMY,
            listOf(MissionRewardItem("CORN_SEED", 50), MissionRewardItem("TOMATO_SEED", 50))),

        // Animal missions
        Mission("animal_1", "첫 동물", "동물 1마리 구매", 1, 40, 100, MissionCategory.ANIMAL,
            listOf(MissionRewardItem("STRAWBERRY_SEED", 15))),
        Mission("animal_5", "목장 주인", "동물 5마리 보유", 5, 200, 800, MissionCategory.ANIMAL,
            listOf(MissionRewardItem("CORN_SEED", 30), MissionRewardItem("CARROT_SEED", 30))),

        // Exploration
        Mission("expand_1", "땅 넓히기", "농장 1단계 확장", 1, 80, 200, MissionCategory.EXPLORATION,
            listOf(MissionRewardItem("CABBAGE_SEED", 10), MissionRewardItem("RADISH_SEED", 10))),
        Mission("upgrade_tool", "도구 업그레이드", "도구 1단계 업그레이드", 1, 80, 200, MissionCategory.EXPLORATION,
            listOf(MissionRewardItem("SPINACH_SEED", 10), MissionRewardItem("CARROT_SEED", 10)))
    )

    fun getExpForLevel(level: Int): Int = 100 + level * 60

    fun getLevelTitle(level: Int): String = when {
        level < 3 -> "초보 농부"
        level < 6 -> "견습 농부"
        level < 10 -> "숙련 농부"
        level < 15 -> "베테랑 농부"
        level < 20 -> "마스터 농부"
        level < 30 -> "전설의 농부"
        else -> "농업의 신"
    }
}
