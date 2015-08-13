package se.simpor.helicopter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class ThrustCopterScene extends ScreenAdapter {
    SpriteBatch batch;
    private FPSLogger fpsLogger;
    private OrthographicCamera camera;
    private TextureRegion background;
    private TextureRegion terrainBelow;
    private TextureRegion terrainAbove;
    private TextureRegion pillarUp;
    private TextureRegion pillarDown;
    private Texture fuelIndicator;
    private int terrainOffset;
    private int backgroundOffset;
    private Animation plane;
    private Animation shield;
    private float planeAnimTime;

    Vector2 planeVelocity = new Vector2();
    Vector2 planePosition = new Vector2();
    Vector2 planeDefaultPosition = new Vector2();
    Vector2 gravity = new Vector2();
    Vector2 scrollVelocity = new Vector2();
    Vector2 lastPillarPosition = new Vector2();

    private static final Vector2 damping = new Vector2(0.99f, 0.99f);
    private TextureAtlas atlas;
    private FitViewport viewport;

    Vector3 touchPosition = new Vector3();
    Vector2 tmpVector = new Vector2();
    private static final int TOUCH_IMPULSE = 500;
    TextureRegion tapIndicator;
    float tapDrawTime;
    private static final float TAP_DRAW_TIME_MAX = 1.0f;
    private TextureAtlas.AtlasRegion tap1;
    private Texture gameover;

    private Music music;
    private Sound tapSound;
    private Sound alarmSound;
    private Sound crashSound;

    private ParticleEffect smoke;
    private ParticleEffect explosion;

    Rectangle planeRect = new Rectangle();
    Rectangle obstacleRect = new Rectangle();

    // meteor
    Array<TextureAtlas.AtlasRegion> meteorTextures = new Array<TextureAtlas.AtlasRegion>();
    TextureRegion selectedMeteorTexture;
    boolean meteorInScene;
    private static final int METEOR_SPEED = 60;
    Vector2 meteorPosition = new Vector2();
    Vector2 meteorVelocity = new Vector2();
    float nextMeteorIn;

    Vector3 pickupTiming = new Vector3();
    Array<Pickup> pickupsInScene = new Array<Pickup>();
    float starCount = 0;
    float shieldCount = 0;
    float fuelCount = 0;

    int fuelPercentage;
    float score = 0;


    ThrustCopter game;

    Array<Vector2> pillars = new Array<Vector2>();
    private final BitmapFont font;

    static enum GameState {
        INIT, ACTION, GAME_OVER
    }

    GameState gameState = GameState.INIT;


    public ThrustCopterScene(ThrustCopter thrustCopter) {
        game = thrustCopter;
        batch = game.batch;
        camera = game.camera;
        atlas = game.atlas;
        background = atlas.findRegion("background");
        terrainBelow = atlas.findRegion("groundGrass");
        pillarDown = atlas.findRegion("rockGrassDown");
        pillarUp = atlas.findRegion("rockGrassUp");
        terrainAbove = new TextureRegion(terrainBelow);
        terrainAbove.flip(true, true);

        plane = new Animation(0.05f,
                atlas.findRegion("planeRed1"),
                atlas.findRegion("planeRed2"),
                atlas.findRegion("planeRed3"),
                atlas.findRegion("planeRed2"));
        shield = new Animation(0.1f,
                atlas.findRegion("shield1"),
                atlas.findRegion("shield2"),
                atlas.findRegion("shield3"),
                atlas.findRegion("shield2")
        );
        plane.setPlayMode(Animation.PlayMode.LOOP);
        shield.setPlayMode(Animation.PlayMode.LOOP);

        tapIndicator = atlas.findRegion("tap2");
        tap1 = atlas.findRegion("tap1");
        gameover = new Texture("gameover.png");
        fuelIndicator = game.manager.get("life.png", Texture.class);


        music = game.manager.get("sounds/journey.mp3", Music.class);
        music.setLooping(true);
        music.play();

        tapSound = game.manager.get("sounds/pop.ogg", Sound.class);
        crashSound = game.manager.get("sounds/crash.ogg", Sound.class);
        alarmSound = game.manager.get("sounds/alarm.ogg", Sound.class);

        meteorTextures.add(atlas.findRegion("meteorBrown_med1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_med2"));
        meteorTextures.add(atlas.findRegion("meteorBrown_small1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_small2"));
        meteorTextures.add(atlas.findRegion("meteorBrown_tiny1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_tiny2"));

        font = game.manager.get("impact-40.fnt", BitmapFont.class);

        smoke = game.manager.get("Smoke", ParticleEffect.class);
        explosion = game.manager.get("Explosion", ParticleEffect.class);

        resetScene();


        Gdx.input.setInputProcessor(new InputProcessor() {
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.R:
                        resetScene();
                        break;
                    case Input.Keys.NUM_1:
                        shieldCount++;
                        break;
                    case Input.Keys.NUM_2:
                        shieldCount--;
                        break;
                    default:
                        return false;
                }
                return true;
            }

            @Override
            public boolean keyUp(int keycode) {
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (gameState == GameState.INIT) {
                    gameState = GameState.ACTION;
                    return true;
                }
                if (gameState == GameState.GAME_OVER) {
                    gameState = GameState.INIT;
                    resetScene();
                    return true;
                }
                touchPosition.set(screenX, screenY, 0);
                camera.unproject(touchPosition);
                tmpVector.set(planePosition.x, planePosition.y);
                tmpVector.sub(touchPosition.x, touchPosition.y).nor();
                final float clamp = MathUtils.clamp(
                        Vector2.dst(touchPosition.x, touchPosition.y, planePosition.x, planePosition.y)
                        , 0, TOUCH_IMPULSE);
                planeVelocity.mulAdd(tmpVector, TOUCH_IMPULSE - clamp);
                tapDrawTime = TAP_DRAW_TIME_MAX;
                tapSound.play();
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return false;
            }

            @Override
            public boolean scrolled(int amount) {
                return false;
            }
        });
    }

    private void resetScene() {
        terrainOffset = 0;
        backgroundOffset = 0;
        planeAnimTime = 0;
        planeVelocity.set(0, 0);
        gravity.set(0, -2);
        planeDefaultPosition.set(400 - 88 / 2, 240 - 73 / 2);
        planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);
        scrollVelocity.set(4, 0);
        lastPillarPosition.set(0, 0);
        pillars.clear();

        meteorInScene = false;
        nextMeteorIn = (float) Math.random() * 5;

        starCount = 0;
        shieldCount = 15;
        fuelCount = 100;
        fuelPercentage = 114;
        score = 0;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        updateScene();
        drawScene();
    }

    private void updateScene() {
        if (gameState == GameState.INIT || gameState == GameState.GAME_OVER) {
            explosion.update(Gdx.graphics.getDeltaTime());
            return;
        }
        final float deltaTime = Gdx.graphics.getDeltaTime();

        tapDrawTime -= deltaTime;

        planeAnimTime += deltaTime;
        planeVelocity.scl(damping);
        planeVelocity.add(gravity);
        planeVelocity.add(scrollVelocity);
        planePosition.mulAdd(planeVelocity, deltaTime);

        final float deltaPosition = planePosition.x - planeDefaultPosition.x;

        terrainOffset -= deltaPosition;
        backgroundOffset -= (deltaPosition) / 5;
        planePosition.x = planeDefaultPosition.x;
        if (terrainOffset * -1 > terrainBelow.getRegionWidth()) {
            terrainOffset = 0;
        }
        if (terrainOffset > 0) {
            terrainOffset = -terrainBelow.getRegionWidth();
        }
        if (backgroundOffset * -1 > background.getRegionWidth()) {
            backgroundOffset = 0;
        }
        if (backgroundOffset > 0) {
            backgroundOffset = -background.getRegionWidth();
        }

        if (planePosition.y < terrainBelow.getRegionHeight() - 35 ||
                planePosition.y + 73 > 480 - terrainBelow.getRegionHeight() + 35) {
            endGame();
        }

        planeRect.set(planePosition.x + 16, planePosition.y, 50, 73);
        for (Vector2 vec : pillars) {
            vec.x -= deltaPosition;
            if (vec.x + pillarUp.getRegionWidth() < -10) {
                pillars.removeValue(vec, false);
            }
            if (vec.y == 1) {
                obstacleRect.set(vec.x + 10, 0, pillarUp.getRegionWidth() - 20,
                        pillarUp.getRegionHeight() - 10);
            } else {
                obstacleRect.set(vec.x + 10,
                        480 - pillarDown.getRegionHeight() + 10,
                        pillarUp.getRegionWidth() - 20, pillarUp.getRegionHeight());
            }
            if (planeRect.overlaps(obstacleRect)) {
                endGame();
            }
        }
        if (lastPillarPosition.x < 400) {
            addPillar();
        }

        if (meteorInScene) {
            meteorPosition.mulAdd(meteorVelocity, deltaTime);
            meteorPosition.x -= deltaPosition;
            if (meteorPosition.x < -10) {
                meteorInScene = false;
            }
        }

        if (meteorInScene) {
            obstacleRect.set(meteorPosition.x + 2, meteorPosition.y + 2,
                    selectedMeteorTexture.getRegionWidth() - 4,
                    selectedMeteorTexture.getRegionHeight() - 4);
            if (shieldCount < 0) {
                if (planeRect.overlaps(obstacleRect)) {
                    endGame();
                }
            }
        }

        nextMeteorIn -= deltaTime;
        if (nextMeteorIn <= 0) {
            launchMeteor();
        }

        checkAndCreatePickup(deltaTime);

        for (Pickup pickup : pickupsInScene) {
            pickup.pickupPosition.x -= deltaPosition;
            obstacleRect.set(pickup.pickupPosition.x, pickup.pickupPosition.y,
                    pickup.pickupTexture.getRegionWidth(),
                    pickup.pickupTexture.getRegionHeight());
            if (planeRect.overlaps(obstacleRect)) {
                pickIt(pickup);
            }
        }

        fuelCount -= 6 * deltaTime;
        fuelPercentage = (int) (114 * fuelCount / 100);

        shieldCount -= deltaTime;
        score += deltaTime;

        smoke.setPosition(planePosition.x + 20, planePosition.y + 30);
        smoke.update(deltaTime);

    }

    private void launchMeteor() {
        nextMeteorIn = 1.5f + (float) Math.random() * 5;
        if (meteorInScene) {
            return;
        }
        meteorInScene = true;
        int id = (int) (Math.random() * meteorTextures.size);
        selectedMeteorTexture = meteorTextures.get(id);
        meteorPosition.x = 810;
        meteorPosition.y = (float) (80 + Math.random() * 320);
        Vector2 destination = new Vector2();
        destination.x = -10;
        destination.y = (float) (80 + Math.random() * 320);
        destination.sub(meteorPosition).nor();
        meteorVelocity.mulAdd(destination, METEOR_SPEED);
    }

    private void addPillar() {
        Vector2 pillarPosition = new Vector2();
        if (pillars.size == 0) {
            pillarPosition.x = (float) (800 + Math.random() * 600);
        } else {
            pillarPosition.x = lastPillarPosition.x + (float) (600 +
                    Math.random() * 600);
        }
        if (MathUtils.randomBoolean()) {
            pillarPosition.y = 1;
        } else {
            pillarPosition.y = -1;//upside down
        }
        lastPillarPosition = pillarPosition;
        pillars.add(pillarPosition);
    }


    private void drawScene() {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        batch.disableBlending();
        batch.draw(background, backgroundOffset, 0);
        batch.draw(background, backgroundOffset + background.getRegionWidth(), 0);
        batch.enableBlending();

        for (Vector2 vec : pillars) {
            if (vec.y == 1) {
                batch.draw(pillarUp, vec.x, 0);
            } else {
                batch.draw(pillarDown, vec.x,
                        480 - pillarDown.getRegionHeight());
            }
        }

        batch.draw(terrainBelow, terrainOffset, 0);
        batch.draw(terrainBelow, terrainOffset + terrainBelow.getRegionWidth(), 0);

        batch.draw(terrainAbove, terrainOffset, 480 - terrainAbove.getRegionHeight());
        batch.draw(terrainAbove, terrainOffset + terrainAbove.getRegionWidth(), 480 - terrainAbove.getRegionHeight());

        batch.draw(plane.getKeyFrame(planeAnimTime), planePosition.x, planePosition.y);

        if (shieldCount > 0) {
            batch.draw(shield.getKeyFrame(planeAnimTime), planePosition.x - 20, planePosition.y);
        }

        smoke.draw(batch);

        if (meteorInScene) {
            batch.draw(selectedMeteorTexture, meteorPosition.x,
                    meteorPosition.y);
        }

        if (tapDrawTime > 0) {
            batch.draw(tapIndicator, touchPosition.x - 29.5f,
                    touchPosition.y - 29.5f);
            //29.5 is half width/height of the image
        }

        if (gameState == GameState.INIT) {
            batch.draw(tap1, planePosition.x, planePosition.y - 80);
        }

        if (gameState == GameState.GAME_OVER) {
            batch.draw(gameover, 400 - 206, 240 - 80);
            explosion.draw(batch);
        }

        for (Pickup pickup : pickupsInScene) {
            batch.draw(pickup.pickupTexture, pickup.pickupPosition.x, pickup.pickupPosition.y);
        }

        batch.setColor(Color.BLACK);
        batch.draw(fuelIndicator, 10, 350);
        batch.setColor(Color.WHITE);
        batch.draw(fuelIndicator, 10, 350, 0, 0, fuelPercentage, 119);

        font.draw(batch, "" + ((int) shieldCount), 390, 450);
        font.draw(batch, "" + (int) (starCount + score), 700, 450);

        batch.end();

    }

    private void checkAndCreatePickup(float delta) {
        pickupTiming.sub(delta);
        if (pickupTiming.x <= 0) {
            pickupTiming.x = (float) (0.5 + Math.random() * 0.5);
            if (addPickup(Pickup.STAR)) pickupTiming.x = 1 + (float) Math.random() * 2;
        }
        if (pickupTiming.y <= 0) {
            pickupTiming.y = (float) (0.5 + Math.random() * 0.5);
            if (addPickup(Pickup.FUEL))
                pickupTiming.y = 3 + (float) Math.random() * 2;
        }
        if (pickupTiming.z <= 0) {
            pickupTiming.z = (float) (0.5 + Math.random() * 0.5);
            if (addPickup(Pickup.SHIELD))
                pickupTiming.z = 10 + (float) Math.random() * 3;
        }
    }

    private boolean addPickup(int pickupType) {
        Vector2 randomPosition = new Vector2();
        randomPosition.x = 820;
        randomPosition.y = (float) (80 + Math.random() * 320);
        for (Vector2 vec : pillars) {
            if (vec.y == 1) {
                obstacleRect.set(vec.x, 0,
                        pillarUp.getRegionWidth(), pillarUp.getRegionHeight());
            } else {
                obstacleRect.set(vec.x, 480 - pillarDown.getRegionHeight(),
                        pillarUp.getRegionWidth(), pillarUp.getRegionHeight());
            }
            if (obstacleRect.contains(randomPosition)) {
                return false;
            }
        }
        Pickup tempPickup = new Pickup(pickupType, game.manager);
        tempPickup.pickupPosition.set(randomPosition);
        pickupsInScene.add(tempPickup);
        return true;
    }

    private void pickIt(Pickup pickup) {
        pickup.pickupSound.play();
        switch (pickup.pickupType) {
            case Pickup.STAR:
                starCount += pickup.pickupValue;
                break;
            case Pickup.SHIELD:
                shieldCount = pickup.pickupValue;
                break;
            case Pickup.FUEL:
                fuelCount = pickup.pickupValue;
                break;
        }
        pickupsInScene.removeValue(pickup, false);
    }

    private void endGame() {
        if (gameState != GameState.GAME_OVER) {
            crashSound.play();
            gameState = GameState.GAME_OVER;
            explosion.reset();
            explosion.setPosition(planePosition.x + 40, planePosition.y + 40);
        }
    }

    @Override
    public void dispose () {
        tapSound.dispose();
        crashSound.dispose();
        music.dispose();
        pillars.clear();
        meteorTextures.clear();
        smoke.dispose();
        explosion.dispose();
    }


}
