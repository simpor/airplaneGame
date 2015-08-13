package se.simpor.helicopter;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by simonrydberg on 10/05/15.
 */
public class Pickup {
    public static final int STAR = 1;
    public static final int SHIELD = 2;
    public static final int FUEL = 3;
    TextureRegion pickupTexture;
    Vector2 pickupPosition = new Vector2();
    int pickupType;
    int pickupValue;
    Sound pickupSound;

    public Pickup(int type, AssetManager manager) {
        TextureAtlas atlas = manager.get("thrustcopterassets.txt", TextureAtlas.class);
        pickupType = type;
        switch (pickupType) {
            case STAR:
                pickupTexture = atlas.findRegion("star_pickup");
                pickupValue = 5;
                pickupSound = manager.get("sounds/star.ogg", Sound.class);
                break;
            case SHIELD:
                pickupTexture = atlas.findRegion("shield_pickup");
                pickupValue = 15;
                pickupSound = manager.get("sounds/shield.ogg", Sound.class);
                break;
            case FUEL:
                pickupTexture = atlas.findRegion("fuel_pickup");
                pickupValue = 100;
                pickupSound = manager.get("sounds/fuel.ogg", Sound.class);
                break;
        }
    }


}
