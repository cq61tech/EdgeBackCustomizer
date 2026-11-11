package com.edgeback.customizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logic that runs inside System UI.
 *  - input: vertical active zone + haptics (Phase 1)
 *  - draw : colors, gradient, opacity, size and roundness of the back pill (Phase 2)
 * Settings come from Settings.Global "ebc_cfg", written by the app:
 *   hooks,haptic,zone,look,alpha,scale100,round,bg,fg,grad,bg2
 */
final class Core {
    private Core() {}

    private static final String CFG_KEY = "ebc_cfg";

    private static volatile int hooks = 1;     // 1 = module active
    private static volatile int haptic = 1;    // 1 = keep haptics, 0 = silence
    private static volatile int zone = 100;    // % of screen height, from the bottom
    private static volatile int look = 0;      // 1 = apply the custom pill look
    private static volatile int alpha = 100;   // %
    private static volatile int scale100 = 100;
    private static volatile int round = 100;   // % (100 = fully round)
    private static volatile int bg = 0;
    private static volatile int fg = 0;
    private static volatile int grad = 0;
    private static volatile int bg2 = 0;
    private static volatile long lastRead = 0;
    private static boolean dropping = false;

    private static final ConcurrentHashMap<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final AtomicInteger DBG = new AtomicInteger();
    private static final AtomicInteger DBG_DRAW = new AtomicInteger();
    /** per panel view: [stock bg color, stock arrow color, applied(0/1)] */
    private static final Map<Object, int[]> ORIG = Collections.synchronizedMap(new WeakHashMap<>());

    // ------------------------------------------------------------------ input

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

    // ------------------------------------------------------------------- draw

    static final class DrawState {
        int saveCount = -1;
        Object edge;
        Object far;
        float edgeOrig;
        float farOrig;
    }

    /** Called right before BackPanel.onDraw. Returns state to undo after the draw. */
    static DrawState beforeDraw(Object view, Canvas canvas) {
        if (!(view instanceof View)) return null;
        View v = (View) view;
        refresh(v.getContext());

        Object bgObj = get(view, "arrowBackgroundPaint");
        Object fgObj = get(view, "arrowPaint");
        if (!(bgObj instanceof Paint) || !(fgObj instanceof Paint)) return null;
        Paint bgPaint = (Paint) bgObj;
        Paint fgPaint = (Paint) fgObj;

        int[] orig = ORIG.get(view);
        if (orig == null) {
            orig = new int[]{bgPaint.getColor(), fgPaint.getColor(), 0};
            ORIG.put(view, orig);
        }
        boolean active = hooks == 1 && look == 1;

        if (!active) {
            if (orig[2] == 1) { // put the stock look back
                bgPaint.setColor(orig[0]);
                fgPaint.setColor(orig[1]);
                bgPaint.setShader(null);
                orig[2] = 0;
            } else { // keep tracking the stock colors (they change with the theme)
                orig[0] = bgPaint.getColor();
                orig[1] = fgPaint.getColor();
            }
            return null;
        }

        if (orig[2] == 0) { // first custom draw: remember what stock looked like
            orig[0] = bgPaint.getColor();
            orig[1] = fgPaint.getColor();
            orig[2] = 1;
        }
        bgPaint.setColor(bg);
        fgPaint.setColor(fg);

        Object bw = get(view, "backgroundWidth");
        Object bh = get(view, "backgroundHeight");
        float bwv = getFloat(bw, "pos", 0f);
        float bhv = getFloat(bh, "pos", 0f);

        if (grad == 1 && bhv > 0f) {
            bgPaint.setShader(new LinearGradient(0f, -bhv / 2f, 0f, bhv / 2f, bg, bg2, Shader.TileMode.CLAMP));
        } else {
            bgPaint.setShader(null);
        }

        DrawState st = new DrawState();
        int w = v.getWidth();
        int h = v.getHeight();
        if (alpha < 100) {
            st.saveCount = canvas.saveLayerAlpha(0f, 0f, w, h, Math.round(255f * alpha / 100f));
        } else {
            st.saveCount = canvas.save();
        }
        if (scale100 != 100) {
            float s = scale100 / 100f;
            boolean left = Boolean.TRUE.equals(get(view, "isLeftPanel"));
            canvas.scale(s, s, left ? 0f : w, h * 0.5f);
        }

        st.edge = get(view, "backgroundEdgeCornerRadius");
        st.far = get(view, "backgroundFarCornerRadius");
        st.edgeOrig = getFloat(st.edge, "pos", 0f);
        st.farOrig = getFloat(st.far, "pos", 0f);
        float target = Math.min(bwv, bhv) / 2f * (round / 100f);
        if (bwv > 0f && bhv > 0f) {
            setFloat(st.edge, "pos", target);
            setFloat(st.far, "pos", target);
        }
        if (DBG_DRAW.getAndIncrement() < 6) {
            MainModule.line("draw look=1 alpha=" + alpha + " scale=" + scale100 + " round=" + round
                    + " grad=" + grad + " bw=" + bwv + " bh=" + bhv + " target=" + target);
        }
        return st;
    }

    static void afterDraw(Canvas canvas, DrawState st) {
        if (st == null) return;
        if (st.saveCount >= 0) canvas.restoreToCount(st.saveCount);
        setFloat(st.edge, "pos", st.edgeOrig);
        setFloat(st.far, "pos", st.farOrig);
    }

    // ----------------------------------------------------------------- config

    private static void refresh(Context ctx) {
        long now = SystemClock.uptimeMillis();
        if (now - lastRead < 1000) return;
        lastRead = now;
        if (ctx == null) return;
        try {
            String v = Settings.Global.getString(ctx.getContentResolver(), CFG_KEY);
            if (v == null) return;
            String[] p = v.split(",");
            hooks = num(p, 0, 1);
            haptic = num(p, 1, 1);
            zone = Math.max(20, Math.min(100, num(p, 2, 100)));
            look = num(p, 3, 0);
            alpha = Math.max(20, Math.min(100, num(p, 4, 100)));
            scale100 = Math.max(50, Math.min(200, num(p, 5, 100)));
            round = Math.max(0, Math.min(100, num(p, 6, 100)));
            bg = num(p, 7, 0);
            fg = num(p, 8, 0);
            grad = num(p, 9, 0);
            bg2 = num(p, 10, 0);
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

    // ------------------------------------------------------------- reflection

    private static Object get(Object o, String name) {
        if (o == null) return null;
        try {
            Field f = field(o.getClass(), name);
            return f == null ? null : f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void set(Object o, String name, Object value) {
        if (o == null) return;
        try {
            Field f = field(o.getClass(), name);
            if (f != null) f.set(o, value);
        } catch (Throwable ignored) {
        }
    }

    private static float getFloat(Object o, String name, float def) {
        Object v = get(o, name);
        return v instanceof Float ? (Float) v : def;
    }

    private static void setFloat(Object o, String name, float value) {
        set(o, name, Float.valueOf(value));
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
