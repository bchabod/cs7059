package com.chabodb.carrot;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public class CarrotGame extends Game {
    MenuScreen menuScreen;

    @Override
    public void create() {
        menuScreen = new MenuScreen(this);
        switchToMenu();
    }

    public void switchToGame() {
        menuScreen.isMenuDisplayed = false;
        this.setScreen(new GameScreen(this));
    }

    public void switchToMenu() {
        this.setScreen(menuScreen);
        menuScreen.isMenuDisplayed = true;
    }

    public void switchToLost(int s) {
        this.setScreen(new LostScreen(this, s));
    }

}
