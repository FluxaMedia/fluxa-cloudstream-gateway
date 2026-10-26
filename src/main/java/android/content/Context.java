package android.content;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private static final Map<String, SharedPreferences> prefs = new HashMap<>();

    public Context getApplicationContext() {
        return this;
    }

    public String getPackageName() {
        return "com.lagradost.cloudstream3";
    }

    public synchronized SharedPreferences getSharedPreferences(String name, int mode) {
        return prefs.computeIfAbsent(name, k -> new MemoryPreferences());
    }

    public Object getSystemService(String name) {
        return null;
    }
}
