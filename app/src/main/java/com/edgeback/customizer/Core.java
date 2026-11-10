package com.edgeback.customizer;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 1 logic, runs inside System UI on every edge-gesture input event:
 *  - vertical active zone: edge swipes that start above the zone are dropped
 *  - haptics: turns the back-arrow vibration off when the app says so
 * Settings come from Settings.Global "ebc_cfg" = "hooks,haptic,zone" (written by the app).
 */
final class Core {
    private Core() {}

    private static final String CFG_KEY = "ebc_cfg";

    private static volatile int hooks = 1;   // 1 = module active
    private static volatile int haptic = 1;  // 1 = keep haptics, 0 = silence
    private static volatile int zone = 100;  // % of screen height, measured from the bottom
    private static volatile long lastRead = 0;
    private static boolean dropping = false;

    private static final ConcurrentHashMap<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final AtomicInteger DBG = new AtomicInteger();

    /** Returns true when this input event must be swallowed. */
    static boolean beforeInput(Object handler, Object event) {
        Object ctxObj = get(handler, "mContext");
        refresh(ctxObj instanceof Context ? (Context) ctxObj : null);

        // haptics: follow the system value unless we are told to silence it
        Object plugin = get(handler, "mEdgeBackPlugin");
        if (plugin != null) {
            boolean system = Boolean.TRUE.equals(get(handler, "mIsEdgeHapticEnabled"));
            boolean want = (hooks == 1 && haptic == 0) ? false : system;
            set(plugin, "edgeHapticEnabled", want);
        }

        if (!(event instanceof MotionEvent)) return false;
        MotionEvent m = (MotionEvent) event;
        int action = m.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            dropping = false;
            if (hooks == 1 && zone < 100) {
                Object size = get(handler, "mDisplaySize");
                if (size instanceof Point && ((Point) size).y > 0) {
                    float limit = ((Point) size).y * (1f - zone / 100f);
                    dropping = m.getRawY() < limit;
                }
            }
            if (DBG.getAndIncrement() < 20) {
                MainModule.line("input DOWN y=" + m.getRawY() + " hooks=" + hooks + " haptic=" + haptic
                        + " zone=" + zone + " drop=" + dropping);
            }
        }
        boolean drop = dropping;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) dropping = false;
        return drop;
    }

    private static void refresh(Context ctx) {
        long now = SystemClock.uptimeMillis();
        if (now - lastRead < 1500) return;
        lastRead = now;
        if (ctx == null) return;
        try {
            String v = Settings.Global.getString(ctx.getContentResolver(), CFG_KEY);
            if (v == null) return;
            String[] p = v.split(",");
            hooks = num(p, 0, 1);
            haptic = num(p, 1, 1);
            zone = Math.max(20, Math.min(100, num(p, 2, 100)));
        } catch (Throwable ignored) {
        }
    }

    private static int num(String[] p, int i, int def) {
        try {
            return i < p.length ? Integer.parseInt(p[i].trim()) : def;
        } catch (Throwable t) {
            return def;
        }
    }

    private static Object get(Object o, String name) {
        try {
            Field f = field(o.getClass(), name);
            return f == null ? null : f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void set(Object o, String name, Object value) {
        try {
            Field f = field(o.getClass(), name);
            if (f != null) f.set(o, value);
        } catch (Throwable ignored) {
        }
    }

    private static Field field(Class<?> c, String name) {
        String key = c.getName() + "#" + name;
        Field cached = FIELDS.get(key);
        if (cached != null) return cached;
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                FIELDS.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
