package su.madbox.ardi3;

import android.app.Application;
import android.content.res.Configuration;

/**
 * Created by Mikle on 07.03.2016.
 */
public class Ardi3Application extends Application {

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

}