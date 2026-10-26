package android.content;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class MemoryPreferences implements SharedPreferences {
    private final Map<String, Object> values = new HashMap<>();

    public synchronized Map<String, ?> getAll() {
        return new HashMap<>(values);
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> T get(String key, T def) {
        Object v = values.get(key);
        return v == null ? def : (T) v;
    }

    public String getString(String key, String defValue) {
        return get(key, defValue);
    }

    public Set<String> getStringSet(String key, Set<String> defValues) {
        return get(key, defValues);
    }

    public int getInt(String key, int defValue) {
        return get(key, defValue);
    }

    public long getLong(String key, long defValue) {
        return get(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        return get(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return get(key, defValue);
    }

    public synchronized boolean contains(String key) {
        return values.containsKey(key);
    }

    public Editor edit() {
        return new Editor() {
            private final Map<String, Object> pending = new HashMap<>();
            private boolean clear;

            public Editor putString(String key, String value) {
                pending.put(key, value);
                return this;
            }

            public Editor putStringSet(String key, Set<String> v) {
                pending.put(key, v);
                return this;
            }

            public Editor putInt(String key, int value) {
                pending.put(key, value);
                return this;
            }

            public Editor putLong(String key, long value) {
                pending.put(key, value);
                return this;
            }

            public Editor putFloat(String key, float value) {
                pending.put(key, value);
                return this;
            }

            public Editor putBoolean(String key, boolean value) {
                pending.put(key, value);
                return this;
            }

            public Editor remove(String key) {
                pending.put(key, null);
                return this;
            }

            public Editor clear() {
                clear = true;
                return this;
            }

            public boolean commit() {
                synchronized (MemoryPreferences.this) {
                    if (clear) {
                        values.clear();
                    }
                    pending.forEach((k, v) -> {
                        if (v == null) {
                            values.remove(k);
                        } else {
                            values.put(k, v);
                        }
                    });
                }
                return true;
            }

            public void apply() {
                commit();
            }
        };
    }
}
