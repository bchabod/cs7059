package com.chabodb.carrot;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import java.util.HashMap;

public class CarrotGame extends ApplicationAdapter {
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

    // Magic numbers for physics simulation
    static final float SCALE = 0.05f;
    static final float STEP_TIME = 1f / 60f;
    static final int VELOCITY_ITERATIONS = 6;
    static final int POSITION_ITERATIONS = 2;
    float accumulator = 0;

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
        shape.setAsBox(camera.viewportWidth, 1);
        fixtureDef.shape = shape;

        ground = world.createBody(bodyDef);
        ground.createFixture(fixtureDef);
        ground.setTransform(0, 0, 0);

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

    private Body createBody(String name, float x, float y, float rotation) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        FixtureDef fd = new FixtureDef();
        fd.density = 2.0f;
        fd.friction = 0.0f;
        fd.restitution = 1.0f;
        Body body = world.createBody(bodyDef);
        float scale = sprites.get(name.split("\\.")[0]).getWidth();
        physicsLoader.attachFixture(body, name, fd, scale);
        body.setTransform(x, y, rotation);
        return body;
    }

    @Override
    public void create() {
        // Prepare viewport
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(50, 50, camera);

        // Prepare physics engine
        Box2D.init();
        debugRenderer = new Box2DDebugRenderer();
        world = new World(new Vector2(0, -200), true);
        physicsLoader = new BodyEditorLoader(Gdx.files.internal("physics.json"));

        // Prepare sprites and drawing tools
        batch = new SpriteBatch();
        textureAtlas = new TextureAtlas("pack.atlas");
        generateSprites();

        bunny = createBody("bunny1_walk1.png", 10, 30, 0);
    }

    @Override
    public void resize(int width, int height) {
        // TODO: we will have to do this everytime we move the camera
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
        createGround();
    }

    @Override
    public void render() {

        Vector2 vBunny = bunny.getLinearVelocity();
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)){
            vBunny.x = -40.0f;
            bunny.setLinearVelocity(vBunny);
        } else if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
            vBunny.x = 40.0f;
        } else {
            vBunny.x = 0.0f;
        }
        bunny.setLinearVelocity(vBunny);

        stepWorld();
        Gdx.gl.glClearColor(0.57f, 0.77f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();

        Vector2 position = bunny.getPosition();
        float degrees = (float) Math.toDegrees(bunny.getAngle());
        drawSprite("bunny1_walk1", position.x, position.y, degrees);

        batch.end();
        debugRenderer.render(world, camera.combined);
    }

    @Override
    public void dispose() {
        textureAtlas.dispose();
        sprites.clear();
        world.dispose();
        debugRenderer.dispose();
    }
}
