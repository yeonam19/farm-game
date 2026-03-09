package com.farmgame.system

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.farmgame.data.Season

/**
 * 게임 오디오 시스템 — BGM(계절별) + SFX(액션별) 관리
 */
object AudioSystem {
    // === Volume settings ===
    var bgmVolume: Float = 0.5f
        set(value) { field = value.coerceIn(0f, 1f); currentBgm?.volume = field }
    var sfxVolume: Float = 0.7f
        set(value) { field = value.coerceIn(0f, 1f) }

    // === BGM ===
    private val bgmMap = mutableMapOf<Season, Music?>()
    private var currentBgm: Music? = null
    private var currentSeason: Season? = null

    // === SFX ===
    private val sfxMap = mutableMapOf<SfxType, Sound?>()

    enum class SfxType {
        TILL, WATER, PLANT, HARVEST,
        CHOP, MINE,
        FISH_CAST, FISH_BITE, FISH_CATCH, FISH_FAIL,
        BUY, SELL,
        LEVEL_UP, MISSION_COMPLETE, ACHIEVEMENT,
        UI_OPEN, UI_CLOSE, UI_SELECT,
        ANIMAL_BUY, ANIMAL_COLLECT,
        NEW_DAY, SEASON_CHANGE
    }

    fun init() {
        // Load BGM — 파일이 없으면 null로 유지 (크래시 방지)
        for (season in Season.entries) {
            bgmMap[season] = loadMusic("audio/bgm/${season.name.lowercase()}.ogg")
        }

        // Load SFX
        sfxMap[SfxType.TILL] = loadSound("audio/sfx/till.ogg")
        sfxMap[SfxType.WATER] = loadSound("audio/sfx/water.ogg")
        sfxMap[SfxType.PLANT] = loadSound("audio/sfx/plant.ogg")
        sfxMap[SfxType.HARVEST] = loadSound("audio/sfx/harvest.ogg")
        sfxMap[SfxType.CHOP] = loadSound("audio/sfx/chop.ogg")
        sfxMap[SfxType.MINE] = loadSound("audio/sfx/mine.ogg")
        sfxMap[SfxType.FISH_CAST] = loadSound("audio/sfx/fish_cast.ogg")
        sfxMap[SfxType.FISH_BITE] = loadSound("audio/sfx/fish_bite.ogg")
        sfxMap[SfxType.FISH_CATCH] = loadSound("audio/sfx/fish_catch.ogg")
        sfxMap[SfxType.FISH_FAIL] = loadSound("audio/sfx/fish_fail.ogg")
        sfxMap[SfxType.BUY] = loadSound("audio/sfx/buy.ogg")
        sfxMap[SfxType.SELL] = loadSound("audio/sfx/sell.ogg")
        sfxMap[SfxType.LEVEL_UP] = loadSound("audio/sfx/level_up.ogg")
        sfxMap[SfxType.MISSION_COMPLETE] = loadSound("audio/sfx/mission_complete.ogg")
        sfxMap[SfxType.ACHIEVEMENT] = loadSound("audio/sfx/achievement.ogg")
        sfxMap[SfxType.UI_OPEN] = loadSound("audio/sfx/ui_open.ogg")
        sfxMap[SfxType.UI_CLOSE] = loadSound("audio/sfx/ui_close.ogg")
        sfxMap[SfxType.UI_SELECT] = loadSound("audio/sfx/ui_select.ogg")
        sfxMap[SfxType.ANIMAL_BUY] = loadSound("audio/sfx/animal_buy.ogg")
        sfxMap[SfxType.ANIMAL_COLLECT] = loadSound("audio/sfx/animal_collect.ogg")
        sfxMap[SfxType.NEW_DAY] = loadSound("audio/sfx/new_day.ogg")
        sfxMap[SfxType.SEASON_CHANGE] = loadSound("audio/sfx/season_change.ogg")
    }

    /** 계절에 맞는 BGM 재생. 이미 같은 계절이면 무시. */
    fun playBgm(season: Season) {
        if (season == currentSeason && currentBgm?.isPlaying == true) return

        currentBgm?.stop()
        currentSeason = season
        currentBgm = bgmMap[season]
        currentBgm?.let {
            it.isLooping = true
            it.volume = bgmVolume
            it.play()
        }
    }

    fun stopBgm() {
        currentBgm?.stop()
    }

    fun pauseBgm() {
        currentBgm?.pause()
    }

    fun resumeBgm() {
        currentBgm?.play()
    }

    /** 효과음 재생 */
    fun playSfx(type: SfxType) {
        sfxMap[type]?.play(sfxVolume)
    }

    fun dispose() {
        bgmMap.values.forEach { it?.dispose() }
        bgmMap.clear()
        sfxMap.values.forEach { it?.dispose() }
        sfxMap.clear()
        currentBgm = null
        currentSeason = null
    }

    // === Private helpers ===

    private fun loadMusic(path: String): Music? {
        return try {
            if (Gdx.files.internal(path).exists()) Gdx.audio.newMusic(Gdx.files.internal(path))
            else null
        } catch (_: Exception) { null }
    }

    private fun loadSound(path: String): Sound? {
        return try {
            if (Gdx.files.internal(path).exists()) Gdx.audio.newSound(Gdx.files.internal(path))
            else null
        } catch (_: Exception) { null }
    }
}
