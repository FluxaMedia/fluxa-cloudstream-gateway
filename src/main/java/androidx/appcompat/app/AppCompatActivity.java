package androidx.appcompat.app;

import android.app.Activity;

public class AppCompatActivity extends Activity {
    public boolean isFinishing() {
        return false;
    }

    public boolean isDestroyed() {
        return false;
    }

    public void runOnUiThread(Runnable r) {
        r.run();
    }

    public void finish() {
    }
}
