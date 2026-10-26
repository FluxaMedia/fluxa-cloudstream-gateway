package android.os;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Handler {
    private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "handler");
        t.setDaemon(true);
        return t;
    });

    private final Map<Runnable, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();

    public Handler(Looper looper) {
    }

    public boolean post(Runnable r) {
        return postDelayed(r, 0);
    }

    public boolean postDelayed(Runnable r, long delayMillis) {
        pending.put(r, pool.schedule(() -> {
            pending.remove(r);
            r.run();
        }, delayMillis, TimeUnit.MILLISECONDS));
        return true;
    }

    public void removeCallbacks(Runnable r) {
        ScheduledFuture<?> f = pending.remove(r);
        if (f != null) {
            f.cancel(false);
        }
    }

    public void removeCallbacksAndMessages(Object token) {
        pending.values().forEach(f -> f.cancel(false));
        pending.clear();
    }
}
