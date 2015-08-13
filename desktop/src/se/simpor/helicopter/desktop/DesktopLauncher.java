package se.simpor.helicopter.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import se.simpor.helicopter.ThrustCopter;
import se.simpor.helicopter.ThrustCopterScene;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

        config.width = 800;
        config.height = 480;

		new LwjglApplication(new ThrustCopter(), config);
	}
}
