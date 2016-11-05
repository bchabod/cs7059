package com.chabodb.carrot;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MenuScreen implements Screen {
    SpriteBatch batch;
    OrthographicCamera camera;
    ExtendViewport viewport;
    Stage stage;
    ImageButton bPlay, bScores;
    Image gameTitle;
    CarrotGame game;
    boolean isMenuDisplayed;

    public MenuScreen(CarrotGame g) {
        super();
        game = g;
        isMenuDisplayed = false;
    }

    private ImageButton createButton(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        TextureRegion region = new TextureRegion(t);
        TextureRegionDrawable drawable = new TextureRegionDrawable(region);
        return new ImageButton(drawable);
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(100, 100, camera);
        batch = new SpriteBatch();
        gameTitle = new Image(new TextureRegion(new Texture(Gdx.files.internal("title.png"))));
        gameTitle.setScale(0.5f);
        bPlay = createButton("button_play.png");

        bPlay.addListener(new ClickListener()
        {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isMenuDisplayed)
                    return;
                game.switchToGame();
            }
        });

        bScores = createButton("button_scores.png");
        stage = new Stage(new ScreenViewport());
        stage.addActor(bPlay);
        stage.addActor(bScores);
        stage.addActor(gameTitle);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.57f, 0.77f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();
        gameTitle.setPosition(w/2 - gameTitle.getWidth()/2 * gameTitle.getScaleX(), h/2 + gameTitle.getHeight()/2);
        bPlay.setPosition(w/2 - bPlay.getWidth()/2, h/2 - bPlay.getHeight()/2);
        bScores.setPosition(w/2 - bScores.getWidth()/2, bPlay.getY() - 2*bScores.getHeight());
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