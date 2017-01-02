package com.chabodb.carrot;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

/**
 * Main game class that handles the transitions between different subscreens
 * @author Benoit Chabod
 */
public class CarrotGame extends Game {
    MenuScreen menuScreen;

    @Override
    public void create() {
        menuScreen = new MenuScreen(this);
        switchToMenu();
    }

    /**
     * Launches the game
     */
    public void switchToGame() {
        menuScreen.isMenuDisplayed = false;
        this.setScreen(new GameScreen(this));
    }

    /**
     * Returns to the main menu
     */
    public void switchToMenu() {
        this.setScreen(menuScreen);
        menuScreen.isMenuDisplayed = true;
    }

    /**
     * Displays the highscores submenu
     */
    public void switchToScores() {
        menuScreen.isMenuDisplayed = false;
        this.setScreen(new ScoreScreen(this));
    }

    /**
     * Displays the game over screen
     * @param s The final score of the player
     */
    public void switchToLost(int s) {
        this.setScreen(new LostScreen(this, s));
    }

}
