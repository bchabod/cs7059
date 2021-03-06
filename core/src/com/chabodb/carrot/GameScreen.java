package com.chabodb.carrot;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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

/**
 * Complex class inheriting Screen that handles the gameplay
 * Physics and graphics are done here, in the show() overridden method
 * @author Benoit Chabod
 */
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
    int score, scoreCarrots;
    int counterJetpack = 0;
    double timeElapsed = 0.0f;
    ParticleEffect carrotParticle, fireParticle;

    // Magic numbers for physics simulation
    static final float SCALE = 0.03f;
    static final float STEP_TIME = 1f / 60f;
    static final int VELOCITY_ITERATIONS = 6;
    static final int POSITION_ITERATIONS = 2;
    float accumulator = 0;

    static final float MAX_JUMP = 30.0f;
    static final float MAX_VELX = 1.5f;
    static final float GRAV = 150.0f;
    static final float BOUNCE_VEL = (float)(Math.sqrt(2*GRAV*MAX_JUMP));
    static final float SPEED_CLOUD = 0.08f;

    /**
     * Simple structure to represent a platform
     */
    private class Platform {
        Vector2 pos;
        // 0 NORMAL, 1 CARROT, 2 SPRING, 3 JETPACK
        int mode;
        Body carrot;
        Body spring;
        Body jetpack;

        Platform(Vector2 v, int m) {
            pos = v;
            mode = m;
        }
    }

    /**
     * Internal class to handle the level generation
     */
    private class Level {
        List<Platform> platforms = new ArrayList<Platform>();
        List<Vector2> clouds = new ArrayList<Vector2>();
        List<Body> enemies = new ArrayList<Body>();
        float threshold = MAX_JUMP/4;
        float lowerBound = 0;
        float platformWidth, platformHeight;
        float carrotWidth, carrotHeight;
        float springWidth, springHeight;
        float jetpackWidth, jetpackHeight;
        float enemyWidth, cloudWidth;

        /**
         * Constructor for the level generator
         */
        Level() {
            platformWidth = sprites.get("ground_grass").getWidth();
            platformHeight = sprites.get("ground_grass").getHeight();
            carrotWidth = sprites.get("carrot").getWidth();
            carrotHeight = sprites.get("carrot").getHeight();
            springWidth = sprites.get("spring").getWidth();
            springHeight = sprites.get("spring").getHeight();
            jetpackWidth = sprites.get("jetpack").getHeight();
            jetpackHeight = sprites.get("jetpack").getHeight();
            cloudWidth = sprites.get("cloud").getWidth();
            enemyWidth = sprites.get("flyMan_jump").getWidth();
        }

        int randomInt(int Min, int Max) {
            return Min + (int) (Math.random() * ((Max - Min) + 1));
        }
        int randomInt(float Min, float Max) {
            return this.randomInt((int) Min, (int) Max);
        }

        /**
         * This method is called whenever new platforms need to be generated
         * Twice the height of the device screen is generated above the player
         */
        void generate() {
            for(float yPos = threshold + 2*platformHeight; yPos < threshold + 2*camera.viewportHeight;) {
                int xPos = randomInt(0, (int)(camera.viewportWidth - platformWidth));
                int mode;
                if (randomInt(0,10)%4 != 0) {
                    mode = 0;
                } else {
                    int rnd = randomInt(0,10)%3;
                    mode = rnd + 1;
                    if (mode == 3) {
                        if (randomInt(0,10)%2 == 0)
                            mode = 0;
                    }
                }
                Platform p = new Platform(new Vector2(xPos, yPos), mode);
                platforms.add(p);

                // Generate associated physics object
                createBody("ground_grass.png", xPos, yPos, 0, BodyDef.BodyType.StaticBody);
                if (p.mode == 1) {
                    float x = p.pos.x + level.platformWidth/2 - level.carrotWidth/1.5f;
                    float y = p.pos.y + level.platformHeight + level.carrotHeight/4;
                    p.carrot = createBody("carrot.png", x, y, 0, BodyDef.BodyType.StaticBody);
                } else if (p.mode == 2) {
                    float x = p.pos.x + level.platformWidth/2 - level.springWidth/1.5f;
                    float y = p.pos.y + level.platformHeight + level.springHeight/4;
                    p.spring = createBody("spring.png", x, y, 0, BodyDef.BodyType.StaticBody);
                } else if (p.mode == 3) {
                    float x = p.pos.x + level.platformWidth/2 - level.jetpackWidth/1.5f;
                    float y = p.pos.y + level.platformHeight + level.jetpackHeight/4;
                    p.jetpack = createBody("jetpack.png", x, y, 0, BodyDef.BodyType.StaticBody);
                }

                yPos += randomInt(3*platformHeight, MAX_JUMP/3);
            }
            lowerBound = camera.position.y - camera.viewportHeight/2;
            threshold += 2*camera.viewportHeight;
            for (Iterator<Platform> iterator = platforms.iterator(); iterator.hasNext(); ) {
                Vector2 platform = iterator.next().pos;
                if (platform.y < lowerBound)
                    iterator.remove();
            }
            if (randomInt(0,10)%2 < 3) {
                float xPos = randomInt(0, camera.viewportWidth/2 + cloudWidth);
                float yPos = threshold + camera.viewportHeight;
                clouds.add(new Vector2(xPos, yPos));
            }
            if (randomInt(0,10)%2 < 3) {
                float xPos = randomInt(0, camera.viewportWidth/2 + cloudWidth);
                float yPos = threshold + camera.viewportHeight/2;
                Body e = createBody("flyMan_jump.png", xPos, yPos, 0, BodyDef.BodyType.StaticBody);
                enemies.add(e);
            }
        }
    }

    /**
     * Physics-related class to handle object collisions
     * There are special cases to handle: bunny with spring, bunny with jetpack, etc
     */
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
            if (sA.equals("bunny1_walk1.png")) {
                if (sB.equals("ground_grass.png")) {
                    contact.setEnabled(handlePlatform(fixtureA, fixtureB));
                } else if (sB.equals("carrot.png")) {
                    contact.setEnabled(false);
                    fixtureB.getBody().setUserData("remove");
                } else if (sB.equals("spring.png")) {
                    boolean isEnabled = handlePlatform(fixtureA, fixtureB);
                    contact.setEnabled(isEnabled);
                    if (isEnabled) {
                        if (fixtureB.getBody().getUserData() != "remove") {
                            fixtureA.getBody().applyLinearImpulse(0.0f, 1000.0f, 0.0f, 0.0f, true);
                            fixtureB.getBody().setUserData("remove");
                        }
                    }
                } else if (sB.equals("jetpack.png")) {
                    contact.setEnabled(false);
                    fixtureA.getBody().setGravityScale(0.1f);
                    fixtureA.getBody().applyLinearImpulse(0.0f, 1000.0f, 0.0f, 0.0f, true);
                    counterJetpack = 100;
                    fireParticle.getEmitters().get(0).getTransparency().setHigh(1.0f);
                    fireParticle.reset();
                    fixtureB.getBody().setUserData("remove");
                } else if (sB.equals("flyMan_jump.png")) {
                    game.switchToLost(score + scoreCarrots);
                }
            }
            else if (sB.equals("bunny1_walk1.png")) {
                if (sA.equals("ground_grass.png")) {
                    contact.setEnabled(handlePlatform(fixtureB, fixtureA));
                } else if (sA.equals("carrot.png")) {
                    contact.setEnabled(false);
                    fixtureA.getBody().setUserData("remove");
                }  else if (sA.equals("spring.png")) {
                    boolean isEnabled = handlePlatform(fixtureB, fixtureA);
                    contact.setEnabled(isEnabled);
                    if (isEnabled) {
                        if (fixtureA.getBody().getUserData() != "remove") {
                            fixtureB.getBody().applyLinearImpulse(0.0f, 1000.0f, 0.0f, 0.0f, true);
                            fixtureA.getBody().setUserData("remove");
                        }
                    }
                } else if (sA.equals("jetpack.png")) {
                    contact.setEnabled(false);
                    fixtureB.getBody().setGravityScale(0.1f);
                    fixtureB.getBody().applyLinearImpulse(0.0f, 1000.0f, 0.0f, 0.0f, true);
                    counterJetpack = 100;
                    fireParticle.getEmitters().get(0).getTransparency().setHigh(1.0f);
                    fireParticle.reset();
                    fixtureA.getBody().setUserData("remove");
                } else if (sA.equals("flyMan_jump.png")) {
                    game.switchToLost(score + scoreCarrots);
                }
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
            if (uA != null && uB != null && uA.toString().equals("bunny1_walk1.png")) {
                if (uB.toString().startsWith("ground")) {
                    Vector2 vBunny = fixtureA.getBody().getLinearVelocity();
                    vBunny.y = BOUNCE_VEL;
                    fixtureA.getBody().setLinearVelocity(vBunny);
                }
            }
            else if (uB != null && uA != null && uB.toString().equals("bunny1_walk1.png")) {
                if (uA.toString().startsWith("ground")) {
                    Vector2 vBunny = fixtureB.getBody().getLinearVelocity();
                    vBunny.y = BOUNCE_VEL;
                    fixtureB.getBody().setLinearVelocity(vBunny);
                }
            }
        }

        @Override
        public void beginContact(Contact contact) {

        }

        @Override
        public void endContact(Contact contact) {

        }
    }

    /**
     * Main constructor for the GameScreen class
     * @param g An instance of the main CarrotGame class
     */
    public GameScreen(CarrotGame g) {
        game = g;
        score = 0;
    }

    /**
     * This function intelligently steps our physics world using a small dt
     */
    private void stepWorld() {
        float delta = Gdx.graphics.getDeltaTime();
        accumulator += Math.min(delta, 0.25f);
        if (accumulator >= STEP_TIME) {
            accumulator -= STEP_TIME;
            world.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        }
    }

    /**
     * Creates an invisible ground for the beginning of the game
     */
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
        ground.setUserData("ground");
        ground.createFixture(fixtureDef);
        ground.setTransform(-camera.viewportWidth, 0, 0);

        shape.dispose();
    }

    /**
     * Grabs the assets needed for the game and creates an array of sprites
     * This is done once only (when the game loads)
     */
    private void generateSprites() {
        Array<TextureAtlas.AtlasRegion> regions = textureAtlas.getRegions();
        for (TextureAtlas.AtlasRegion region : regions) {
            Sprite sprite = textureAtlas.createSprite(region.name);
            float realScale = SCALE;
            if (region.name.equals("carrot"))
                realScale *= 2.0f;
            else if (region.name.equals("cloud"))
                realScale *= 4.0f;
            else if (region.name.equals("flyMan_jump"))
                realScale *= 1.80f;
            float width = sprite.getWidth() * realScale;
            float height = sprite.getHeight() * realScale;
            sprite.setSize(width, height);
            sprite.setOrigin(0, 0);
            sprites.put(region.name, sprite);
        }
    }

    /**
     * Draws a loaded sprite on the screen
     * @param name The name of the sprite to be drawn
     * @param x X coordinate on the screen
     * @param y Y coordinate on the screen
     * @param degrees The rotation angle (in degrees)
     */
    private void drawSprite(String name, float x, float y, float degrees) {
        Sprite sprite = sprites.get(name);
        float alpha = (name.equals("cloud") ? 0.25f : 1.0f);
        sprite.setColor(sprite.getColor().r, sprite.getColor().g, sprite.getColor().b, alpha);
        sprite.setPosition(x, y);
        sprite.setRotation(degrees);
        sprite.setOrigin(0f, 0f);
        sprite.draw(batch);
    }

    /**
     * Creates a Body instance for our physics engine, Box2D
     * @param name The name of this new body
     * @param x X coordinate in the 2D world
     * @param y Y coordinate in the 2D world
     * @param rotation Rotation angle (in degrees)
     * @param bt Body type
     * @return A Body instance ready to be manipulated
     */
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

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pamela.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 80;
        font = generator.generateFont(parameter);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.getData().setScale(0.10f);
        generator.dispose();

        // Generate the beginning of the level
        level = new Level();

        // Prepare particle FX
        carrotParticle = new ParticleEffect();
        carrotParticle.load(Gdx.files.internal("carrot_eaten.party"), Gdx.files.internal(""));
        carrotParticle.start();
        carrotParticle.scaleEffect(0.1f);

        fireParticle = new ParticleEffect();
        fireParticle.load(Gdx.files.internal("fire.party"), Gdx.files.internal(""));
        fireParticle.start();
        fireParticle.scaleEffect(0.1f);

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
        timeElapsed += 0.01f;
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

        if (!carrotParticle.isComplete()) {
            carrotParticle.update(Gdx.graphics.getDeltaTime());
            carrotParticle.draw(batch);
        }

        for (Platform p : level.platforms) {
            drawSprite("ground_grass", p.pos.x, p.pos.y, 0);
            if (p.mode == 1) {
                Vector2 cPos = p.carrot.getPosition();
                if (p.carrot.getUserData() == "remove") {
                    p.mode = 0;
                    p.carrot.setActive(false);
                    p.carrot = null;
                    scoreCarrots += 50;
                    carrotParticle.getEmitters().first().setPosition(cPos.x + level.carrotWidth/2, cPos.y);
                    if (carrotParticle.isComplete())
                        carrotParticle.reset();
                }
                drawSprite("carrot", cPos.x, cPos.y, 0);
            } else if (p.mode == 2) {
                Vector2 cPos = p.spring.getPosition();
                if (p.spring.getUserData() == "remove") {
                    drawSprite("spring_out", cPos.x, cPos.y, 0);
                } else {
                    drawSprite("spring", cPos.x, cPos.y, 0);
                }
            } else if (p.mode == 3) {
                Vector2 cPos = p.jetpack.getPosition();
                if (p.jetpack.getUserData() == "remove") {
                    p.mode = 0;
                    p.jetpack.setActive(false);
                    p.jetpack = null;
                }
                drawSprite("jetpack", cPos.x, cPos.y, 0);
            }
        }

        for (Vector2 c : level.clouds) {
            drawSprite("cloud", c.x, c.y, 0);
            c.x -= SPEED_CLOUD;
        }

        for (Body e : level.enemies) {
            drawSprite("flyMan_jump", e.getPosition().x, e.getPosition().y, 0);
            float deltaX = (float)(Math.sin(timeElapsed) * (viewport.getWorldWidth()/2 - level.enemyWidth));
            float deltaY = (float)Math.sin(timeElapsed*8) * 0.15f;
            e.setTransform(camera.position.x - level.enemyWidth/2 + deltaX , e.getPosition().y + deltaY, 0);
        }

        Vector2 position = bunny.getPosition();
        float degrees = (float) Math.toDegrees(bunny.getAngle());
        if (counterJetpack > 0) {
            counterJetpack--;
            if (counterJetpack == 0)
                bunny.setGravityScale(1.0f);
            if (counterJetpack < 100) {
                float alpha = 1.0f - (100 - counterJetpack) * 0.01f;
                fireParticle.getEmitters().get(0).getTransparency().setHigh(alpha);
            }
            drawSprite("jetpack", position.x - 1, position.y, degrees);
            fireParticle.setPosition(position.x - 1, position.y + level.jetpackHeight);
            fireParticle.update(Gdx.graphics.getDeltaTime());
            fireParticle.draw(batch);
        }

        drawSprite("bunny1_walk1", position.x, position.y, degrees);

        if ((camera.position.y - camera.viewportHeight/2) > score) {
            score = (int)(camera.position.y - camera.viewportHeight/2);
        }
        layout.setText(font, "" + (score + scoreCarrots));
        float textY = camera.position.y + camera.viewportHeight/2 - layout.height*0.5f;
        float textX = camera.position.x + camera.viewportWidth/2 - layout.width*1.2f;
        font.draw(batch, "" + (score + scoreCarrots), textX, textY);

        batch.end();
        if (level.threshold - camera.position.y < camera.viewportHeight) {
            level.generate();
        }

        if (bunny.getPosition().y > camera.position.y) {
            camera.position.y = bunny.getPosition().y;
            camera.update();
            batch.setProjectionMatrix(camera.combined);
        }

        if (bunny.getPosition().y < camera.position.y - camera.viewportHeight/2) {
            game.switchToLost(score + scoreCarrots);
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
