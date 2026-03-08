package com.farmgame.system

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,  // emoji-like identifier for rendering
    val category: AchievementCategory
)

enum class AchievementCategory(val koreanName: String) {
    FARMING("농사"),
    FISHING("낚시"),
    GATHERING("채집"),
    ECONOMY("경제"),
    ANIMAL("동물"),
    TIME("시간"),
    SPECIAL("특별")
}

object AchievementSystem {
    val allAchievements: List<Achievement> = listOf(
        // Farming
        Achievement("first_harvest", "첫 수확", "처음으로 작물을 수확", "crop", AchievementCategory.FARMING),
        Achievement("harvest_100", "수확의 달인", "작물 100개 수확", "crop", AchievementCategory.FARMING),
        Achievement("harvest_500", "전설의 수확자", "작물 500개 수확", "crop", AchievementCategory.FARMING),
        Achievement("all_crops", "만능 농부", "모든 종류의 작물 수확", "crop", AchievementCategory.FARMING),

        // Fishing
        Achievement("first_fish", "첫 물고기", "처음으로 물고기 낚기", "fish", AchievementCategory.FISHING),
        Achievement("fish_50", "낚시 명인", "물고기 50마리 낚기", "fish", AchievementCategory.FISHING),
        Achievement("rare_fish", "황금 물고기", "금붕어 낚기", "fish", AchievementCategory.FISHING),
        Achievement("fish_trash", "쓰레기 수집가", "낡은 장화 10개 낚기", "fish", AchievementCategory.FISHING),

        // Gathering
        Achievement("chop_100", "벌목왕", "나무 100그루 베기", "wood", AchievementCategory.GATHERING),
        Achievement("mine_100", "채굴왕", "바위 100개 캐기", "stone", AchievementCategory.GATHERING),

        // Economy
        Achievement("first_1000", "첫 천원", "소지금 1,000원 달성", "money", AchievementCategory.ECONOMY),
        Achievement("rich_10000", "부자", "소지금 10,000원 달성", "money", AchievementCategory.ECONOMY),
        Achievement("rich_50000", "대부호", "소지금 50,000원 달성", "money", AchievementCategory.ECONOMY),
        Achievement("rich_100000", "재벌", "소지금 100,000원 달성", "money", AchievementCategory.ECONOMY),
        Achievement("earn_total_100000", "총매출 십만원", "총 수입 100,000원 달성", "money", AchievementCategory.ECONOMY),

        // Animal
        Achievement("first_animal", "첫 동물", "처음으로 동물 구매", "animal", AchievementCategory.ANIMAL),
        Achievement("animal_10", "대목장", "동물 10마리 보유", "animal", AchievementCategory.ANIMAL),
        Achievement("all_animals", "동물원", "모든 종류의 동물 보유", "animal", AchievementCategory.ANIMAL),

        // Time
        Achievement("survive_7", "일주일", "7일 생존", "time", AchievementCategory.TIME),
        Achievement("survive_28", "한 계절", "28일(한 계절) 생존", "time", AchievementCategory.TIME),
        Achievement("survive_112", "일 년", "112일(1년) 생존", "time", AchievementCategory.TIME),
        Achievement("year_3", "베테랑", "3년차 농부", "time", AchievementCategory.TIME),

        // Special
        Achievement("level_5", "성장하는 농부", "레벨 5 달성", "star", AchievementCategory.SPECIAL),
        Achievement("level_10", "숙련 농부", "레벨 10 달성", "star", AchievementCategory.SPECIAL),
        Achievement("level_20", "마스터 농부", "레벨 20 달성", "star", AchievementCategory.SPECIAL),
        Achievement("full_expand", "대지주", "농장 최대 확장", "star", AchievementCategory.SPECIAL),
        Achievement("full_tools", "장인의 도구", "도구 최대 업그레이드", "star", AchievementCategory.SPECIAL),
        Achievement("all_missions", "미션 마스터", "모든 미션 완료", "star", AchievementCategory.SPECIAL)
    )

    fun getById(id: String): Achievement? = allAchievements.find { it.id == id }
}
