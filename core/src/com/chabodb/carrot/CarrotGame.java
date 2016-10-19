package com.chabodb.carrot;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.HashMap;

public class CarrotGame extends ApplicationAdapter {
    final HashMap<String, Sprite> sprites = new HashMap<String, Sprite>();
    TextureAtlas textureAtlas;
    Sprite bunny;
    SpriteBatch batch;
    OrthographicCamera camera;
    ExtendViewport viewport;

    private void generateSprites() {
        Array<TextureAtlas.AtlasRegion> regions = textureAtlas.getRegions();
        for (TextureAtlas.AtlasRegion region : regions) {
            Sprite sprite = textureAtlas.createSprite(region.name);
            sprites.put(region.name, sprite);
        }
    }

    private void drawSprite(String name, float x, float y) {
        Sprite sprite = sprites.get(name);
        sprite.setPosition(x, y);
        sprite.draw(batch);
    }

    @Override
    public void create() {
        // Prepare viewport
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(800, 600, camera);

        // Prepare sprites and drawing tools
        batch = new SpriteBatch();
        textureAtlas = new TextureAtlas("pack.atlas");
        generateSprites();
    }


    @Override
    public void resize(int width, int height) {
        // TODO: we will have to do this everytime we move the camera
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.57f, 0.77f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        drawSprite("bunny1_walk1", 0, 0);
        drawSprite("bunny2_walk2", 200, 300);
        drawSprite("bunny1_ready", 400, 300);
        batch.end();
    }

    @Override
    public void dispose() {
        textureAtlas.dispose();
        sprites.clear();
    }
}
