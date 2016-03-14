package su.madbox.ardi3;

import android.app.Application;
import android.content.res.Configuration;
import android.os.Handler;

/**
 * Created by Mikle on 07.03.2016.
 */
public class Ardi3Application extends Application {

    Handler.Callback realCallback = null;
    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (realCallback != null) {
                realCallback.handleMessage(msg);
            }
        }
    };

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

    public Handler getHandler() {
        return handler;
    }
    public void setCallBack(Handler.Callback callback) {
        this.realCallback = callback;
    }

}