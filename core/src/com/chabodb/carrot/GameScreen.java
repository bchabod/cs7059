package com.chabodb.carrot;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class GameScreen implements Screen {
    final HashMap<String, Sprite> sprites = new HashMap<String, Sprite>();
    TextureAtlas textureAtlas;
    SpriteBatch batch;
    OrthographicCamera camera;
    ExtendViewport viewport;
    World world;
    BodyEditorLoader physicsLoader;
    Box2DDebugRenderer debugRenderer;
    Body bunny;
    Body ground;
    Level level;
    CustomListener collisionFilter;
    CarrotGame game;
    BitmapFont font;
    GlyphLayout layout;
    int score;

    // Magic numbers for physics simulation
    static final float SCALE = 0.03f;
    static final float STEP_TIME = 1f / 60f;
    static final int VELOCITY_ITERATIONS = 6;
    static final int POSITION_ITERATIONS = 2;
    float accumulator = 0;

    static final float MAX_JUMP = 30.0f;
    static final float MAX_VELX = 1.5f;
    static final float GRAV = 200.0f;
    static final float BOUNCE_VEL = (float)(Math.sqrt(2*GRAV*MAX_JUMP));

    private class Level {
        List<Vector2> platforms = new ArrayList<Vector2>();
        float threshold = MAX_JUMP/4;
        float lowerBound = 0;
        float platformWidth, platformHeight;

        Level() {
            platformWidth = sprites.get("ground_grass").getWidth();
            platformHeight = sprites.get("ground_grass").getHeight();
        }

        int randomInt(int Min, int Max) {
            return Min + (int) (Math.random() * ((Max - Min) + 1));
        }
        int randomInt(float Min, float Max) {
            return this.randomInt((int) Min, (int) Max);
        }

        void generate() {
            for(float yPos = threshold + 2*platformHeight; yPos < threshold + 2*camera.viewportHeight;) {
                int xPos = randomInt(0, (int)(camera.viewportWidth - platformWidth));
                platforms.add(new Vector2(xPos, yPos));

                // Generate associated physics object
                createBody("ground_grass.png", xPos, yPos, 0, BodyDef.BodyType.StaticBody);

                yPos += randomInt(2*platformHeight, MAX_JUMP/3);
            }
            lowerBound = camera.position.y - camera.viewportHeight/2;
            threshold += 2*camera.viewportHeight;
            for (Iterator<Vector2> iterator = platforms.iterator(); iterator.hasNext(); ) {
                Vector2 platform = iterator.next();
                if (platform.y < lowerBound)
                    iterator.remove();
            }
        }
    }

    private class CustomListener implements ContactListener {

        private boolean handlePlatform(Fixture bunny, Fixture platform) {
            return bunny.getBody().getLinearVelocity().y < 0;
        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {
            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            if (fixtureA.getBody().getUserData() == null || fixtureB.getBody().getUserData() == null)
                return;
            String sA = fixtureA.getBody().getUserData().toString();
            String sB = fixtureB.getBody().getUserData().toString();
            if (sA.equals("bunny1_walk1.png") && sB.equals("ground_grass.png")) {
                contact.setEnabled(handlePlatform(fixtureA, fixtureB));
            }
            else if (sB.equals("bunny1_walk1.png") && sA.equals("ground_grass.png")) {
                contact.setEnabled(handlePlatform(fixtureB, fixtureA));
            }
        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {
            if (!contact.isEnabled())
                return;
            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            Object uA = fixtureA.getBody().getUserData();
            Object uB = fixtureB.getBody().getUserData();
            if (uA != null && uA.toString().equals("bunny1_walk1.png")) {
                Vector2 vBunny = fixtureA.getBody().getLinearVelocity();
                vBunny.y = BOUNCE_VEL;
                fixtureA.getBody().setLinearVelocity(vBunny);
            }
            else if (uB != null && uB.toString().equals("bunny1_walk1.png")) {
                Vector2 vBunny = fixtureB.getBody().getLinearVelocity();
                vBunny.y = BOUNCE_VEL;
                fixtureB.getBody().setLinearVelocity(vBunny);
            }
        }

        @Override
        public void beginContact(Contact contact) {

        }

        @Override
        public void endContact(Contact contact) {

        }
    }

    public GameScreen(CarrotGame g) {
        game = g;
        score = 0;
    }

    // This function intelligently steps our physics world using a small dt
    private void stepWorld() {
        float delta = Gdx.graphics.getDeltaTime();
        accumulator += Math.min(delta, 0.25f);
        if (accumulator >= STEP_TIME) {
            accumulator -= STEP_TIME;
            world.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        }
    }

    private void createGround() {
        if (ground != null)
            world.destroyBody(ground);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.friction = 1;
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(camera.viewportWidth*3, 1);
        fixtureDef.shape = shape;
        ground = world.createBody(bodyDef);
        ground.createFixture(fixtureDef);
        ground.setTransform(-camera.viewportWidth, 0, 0);

        shape.dispose();
    }

    private void generateSprites() {
        Array<TextureAtlas.AtlasRegion> regions = textureAtlas.getRegions();
        for (TextureAtlas.AtlasRegion region : regions) {
            Sprite sprite = textureAtlas.createSprite(region.name);
            float width = sprite.getWidth() * SCALE;
            float height = sprite.getHeight() * SCALE;
            sprite.setSize(width, height);
            sprite.setOrigin(0, 0);
            sprites.put(region.name, sprite);
        }
    }

    private void drawSprite(String name, float x, float y, float degrees) {
        Sprite sprite = sprites.get(name);
        sprite.setPosition(x, y);
        sprite.setRotation(degrees);
        sprite.setOrigin(0f, 0f);
        sprite.draw(batch);
    }

    private Body createBody(String name, float x, float y, float rotation, BodyDef.BodyType bt) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bt;
        bodyDef.fixedRotation = true;
        FixtureDef fd = new FixtureDef();
        fd.density = 2.0f;
        fd.friction = 0.0f;
        fd.restitution = 1.0f;
        Body body = world.createBody(bodyDef);
        body.setUserData(name);
        float scale = sprites.get(name.split("\\.")[0]).getWidth();
        physicsLoader.attachFixture(body, name, fd, scale);
        body.setTransform(x, y, rotation);
        return body;
    }

    @Override
    public void show() {
        // Prepare viewport
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(65, 65, camera);

        // Prepare physics engine
        Box2D.init();
        debugRenderer = new Box2DDebugRenderer();
        world = new World(new Vector2(0, -GRAV), true);
        physicsLoader = new BodyEditorLoader(Gdx.files.internal("physics.json"));
        collisionFilter = new CustomListener();
        world.setContactListener(collisionFilter);

        // Prepare sprites and drawing tools
        batch = new SpriteBatch();
        textureAtlas = new TextureAtlas("pack.atlas");
        generateSprites();
        layout = new GlyphLayout();
        font = new BitmapFont();
        font.getData().setScale(0.25f);
        font.setColor(Color.ORANGE);

        // Generate the beginning of the level
        level = new Level();

        bunny = createBody("bunny1_walk1.png", 10, 10, 0, BodyDef.BodyType.DynamicBody);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
        createGround();
    }

    @Override
    public void render(float delta) {
        Vector2 vBunny = bunny.getLinearVelocity();

        boolean available = Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer);
        if (!available) {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)){
                vBunny.x = -60.0f;
            } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
                vBunny.x = 60.0f;
            } else {
                vBunny.x = 0.0f;
            }
        } else {
            Matrix4 matrix = new Matrix4();
            Gdx.input.getRotationMatrix(matrix.val);
            float pitch = matrix.getValues()[9];
            Vector2 pBunny = bunny.getPosition();
            pBunny.x += Math.max(Math.min(5.0f * pitch, MAX_VELX), -MAX_VELX);
            bunny.setTransform(pBunny, bunny.getAngle());
        }

        bunny.setLinearVelocity(vBunny);

        Vector2 pBunny = bunny.getPosition();
        float halfBunny = sprites.get("bunny1_walk1").getWidth()/2;
        if (pBunny.x > camera.viewportWidth - halfBunny) {
            pBunny.x = - halfBunny;
            bunny.setTransform(pBunny, bunny.getAngle());
        } else if (pBunny.x < - halfBunny) {
            pBunny.x = camera.viewportWidth - halfBunny;
            bunny.setTransform(pBunny, bunny.getAngle());
        }

        stepWorld();
        Gdx.gl.glClearColor(0.57f, 0.77f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();

        for (Vector2 p : level.platforms) {
            drawSprite("ground_grass", p.x, p.y, 0);
        }

        Vector2 position = bunny.getPosition();
        float degrees = (float) Math.toDegrees(bunny.getAngle());
        drawSprite("bunny1_walk1", position.x, position.y, degrees);

        if ((camera.position.y - camera.viewportHeight/2) > score) {
            score = (int)(camera.position.y - camera.viewportHeight/2);
        }
        layout.setText(font, "" + score);
        float textY = camera.position.y + camera.viewportHeight/2 - layout.height;
        float textX = camera.position.x + camera.viewportWidth/2 - layout.width*1.2f;
        font.draw(batch, "" + score, textX, textY);

        batch.end();
        debugRenderer.render(world, camera.combined);
        if (level.threshold - camera.position.y < camera.viewportHeight) {
            level.generate();
        }

        if (bunny.getPosition().y > camera.position.y) {
            camera.position.y = bunny.getPosition().y;
            camera.update();
            batch.setProjectionMatrix(camera.combined);
        }

        if (bunny.getPosition().y < camera.position.y - camera.viewportHeight/2) {
            game.switchToLost(score);
        }
    }

    @Override
    public void dispose() {
        textureAtlas.dispose();
        sprites.clear();
        world.dispose();
        debugRenderer.dispose();
    }

    @Override
    public void hide() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }
}
