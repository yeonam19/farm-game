package com.farmgame

import com.badlogic.gdx.Game
import com.farmgame.screen.GameScreen
import com.farmgame.ui.FontManager

class FarmGame : Game() {
    override fun create() {
        FontManager.init()
        setScreen(GameScreen())
    }

    override fun dispose() {
        super.dispose()
        FontManager.dispose()
    }
}
