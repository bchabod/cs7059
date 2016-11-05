package com.chabodb.carrot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LostScreen implements Screen {
    SpriteBatch batch;
    OrthographicCamera camera;
    ExtendViewport viewport;
    Stage stage;
    ImageButton bPlay, bScores;
    Image gameTitle;
    CarrotGame game;
    boolean isMenuDisplayed;
    BitmapFont font;
    GlyphLayout layout;
    int score;

    public LostScreen(CarrotGame g, int s) {
        super();
        game = g;
        score = s;
        isMenuDisplayed = false;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(100, 100, camera);
        batch = new SpriteBatch();
        gameTitle = new Image(new TextureRegion(new Texture(Gdx.files.internal("game_over.png"))));
        gameTitle.setScale(0.5f);
        stage = new Stage(new ScreenViewport());
        stage.addActor(gameTitle);
        Gdx.input.setInputProcessor(stage);

        layout = new GlyphLayout();
        font = new BitmapFont();
        font.getData().setScale(0.4f);
        font.setColor(Color.ORANGE);
    }

    @Override
    public void render(float delta) {
        if(Gdx.input.isTouched()) {
            game.switchToMenu();
        }
        Gdx.gl.glClearColor(0.57f, 0.77f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        batch.end();

        batch.begin();
        String text = "Your bunny died! \nYour final score was: " + score + "\n";
        text += "Please press screen to continue...";
        layout.setText(font, text);
        float textY = camera.position.y - layout.height/2;
        float textX = camera.position.x - layout.width/2;
        font.draw(batch, text, textX, textY);
        batch.end();

    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();
        gameTitle.setPosition(w/2 - gameTitle.getWidth()/2 * gameTitle.getScaleX(), h/2 + gameTitle.getHeight()/2);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
