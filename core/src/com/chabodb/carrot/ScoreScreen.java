package com.chabodb.carrot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class ScoreScreen implements Screen {
    SpriteBatch batch;
    OrthographicCamera camera;
    ExtendViewport viewport;
    CarrotGame game;
    BitmapFont font;
    Image scoreTitle;
    GlyphLayout layout;
    perfComparator c = new perfComparator();
    ShapeRenderer shapeRenderer;
    List<Performance> scores = new ArrayList<Performance>();

    private class perfComparator implements Comparator<Performance>
    {
        public int compare(Performance p1, Performance p2)
        {
            return (p2.score - p1.score);
        }
    }

    private class Performance {
        String date;
        int score;

        Performance(String s) {
            System.out.println(s);
            String[] chunks = s.split("\\|");
            try {
                date = chunks[0];
                System.out.println(chunks[1]);
                score = Integer.parseInt(chunks[1].trim());
                System.out.println("Performance created with score is " + score);
            } catch (Exception e) {}
        }
    }

    public ScoreScreen(CarrotGame g) {
        super();

        scoreTitle = new Image(new TextureRegion(new Texture(Gdx.files.internal("scores.png"))));
        scoreTitle.setOrigin(scoreTitle.getWidth()/2, scoreTitle.getHeight()/2);

        shapeRenderer = new ShapeRenderer();
        FileHandle file = Gdx.files.local("scores.txt");
        try {
            BufferedReader br = file.reader(1024);
            for(String line; (line = br.readLine()) != null; ) {
                if (line.length() > 1) {
                    scores.add(new Performance(line));
                }
            }
        } catch (Exception e) {}
        Collections.sort(scores, c);
        game = g;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(1000, 1000, camera);
        batch = new SpriteBatch();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pamela.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 80;
        font = generator.generateFont(parameter);
        generator.dispose();

        layout = new GlyphLayout();
    }

    @Override
    public void render(float delta) {
        if(Gdx.input.isTouched()) {
            game.switchToMenu();
        }
        Gdx.gl.glClearColor(0.57f, 0.77f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        font.setColor(Color.ORANGE);

        float textY = camera.position.y + viewport.getWorldHeight() * 0.15f;

        float xRank = camera.position.x - viewport.getWorldWidth() * 0.35f;
        font.draw(batch, "Rank", xRank, textY);
        float xDate = camera.position.x - viewport.getWorldWidth() * 0.05f;
        font.draw(batch, "Date", xDate, textY);
        float xScore = camera.position.x + viewport.getWorldWidth() * 0.25f;
        font.draw(batch, "Score", xScore, textY);

        scoreTitle.draw(batch, 1.0f);

        String text = "Press screen to return to main menu...";
        layout.setText(font, text);
        float returnY = camera.position.y - viewport.getWorldHeight() * 0.35f;
        float returnX = camera.position.x - layout.width/2;
        font.draw(batch, text, returnX, returnY);

        font.setColor(Color.WHITE);
        Iterator it = scores.iterator();
        int rank = 1;
        while (it.hasNext() && rank <= 3){
            Performance p = (Performance) it.next();
            font.draw(batch, Integer.toString(rank), xRank + 100, textY - rank*120);
            font.draw(batch, p.date, xDate - 100, textY - rank*120);
            font.draw(batch, Integer.toString(p.score), xScore + 50, textY - rank*120);
            rank++;
        }

        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rectLine(xRank - 100, textY - 100, xScore + 250, textY - 100, 5);
        shapeRenderer.rectLine((xRank + xDate)/2, textY + 50, (xRank + xDate)/2, camera.position.y - viewport.getWorldHeight() * 0.30f, 5);
        xScore += 200;
        shapeRenderer.rectLine((xScore + xDate)/2, textY + 50, (xScore + xDate)/2, camera.position.y - viewport.getWorldHeight() * 0.30f, 5);
        shapeRenderer.end();

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight() + 500;
        scoreTitle.setPosition(w/2 - scoreTitle.getWidth()/2 * scoreTitle.getScaleX(), h/2 + scoreTitle.getHeight()/4);
        batch.setProjectionMatrix(camera.combined);
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
