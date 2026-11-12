package com.edgeback.customizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
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

final class Core {
    private Core() {}

    private static final String CFG_KEY = "ebc_cfg";

    private static volatile int hooks = 1;
    private static volatile int haptic = 1;
    private static volatile int zone = 100;
    private static volatile int look = 0;
    private static volatile int alpha = 100;
    private static volatile int scale100 = 100;
    private static volatile int round = 100;
    private static volatile int bg = 0;
    private static volatile int fg = 0;
    private static volatile int grad = 0;
    private static volatile int bg2 = 0;
    private static volatile int angle = 90;
    private static volatile int pw = 100;
    private static volatile int ph = 100;
    private static volatile int arrowSize = 100;
    private static volatile int stroke10 = 0;
    private static volatile int glow = 0;
    private static volatile int offsetY = 0;
    private static volatile int leftOn = 1;
    private static volatile int rightOn = 1;
    private static volatile int edgeW = 100;
    private static volatile int outline10 = 0;
    private static volatile long lastRead = 0;
    static {
        OUTLINE.setStyle(Paint.Style.STROKE);
    }
    private static boolean dropping = false;
    private static boolean pluginDropping = false;
    private static volatile boolean gestureActive = false;
    private static volatile long gestureEnd = 0;
    private static volatile Boolean stockHaptic = null;
    private static final Paint GLOW = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF RECT = new RectF();

    private static int origL = -1, origR = -1, setL = -1, setR = -1;

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Field>> FIELDS = new ConcurrentHashMap<>();
    private static final AtomicInteger DBG = new AtomicInteger();
    private static final AtomicInteger DBG_DRAW = new AtomicInteger();
    private static final AtomicInteger DBG_PLUGIN = new AtomicInteger();
    private static final Map<Object, int[]> ORIG = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, float[]> ORIG_F = Collections.synchronizedMap(new WeakHashMap<>());

    static boolean beforeInput(Object handler, Object event) {
        Object ctxObj = get(handler, "mContext");
        refresh(ctxObj instanceof Context ? (Context) ctxObj : null);

        Object plugin = get(handler, "mEdgeBackPlugin");
        if (plugin != null) {
            Object system = get(handler, "mIsEdgeHapticEnabled");
            if (system instanceof Boolean) stockHaptic = (Boolean) system;
            applyHaptic(plugin);
        }

        applyEdgeWidth(handler);

        if (!(event instanceof MotionEvent)) return false;
        MotionEvent m = (MotionEvent) event;
        int action = m.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            dropping = shouldDrop(get(handler, "mDisplaySize"), m);
            markGesture(true);
            if (DBG.getAndIncrement() < 12) {
                MainModule.line("input DOWN x=" + m.getRawX() + " y=" + m.getRawY() + " zone=" + zone
                        + " left=" + leftOn + " right=" + rightOn + " edgeW=" + edgeW + " drop=" + dropping);
            }
        }
        boolean drop = dropping;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            dropping = false;
            markGesture(false);
        }
        return drop;
    }

    static boolean pluginGate(Object controller, Object event) {
        Object view = get(controller, "mView");
        if (view instanceof View) refresh(((View) view).getContext());
        applyHaptic(controller);

        if (!(event instanceof MotionEvent)) return false;
        MotionEvent m = (MotionEvent) event;
        int action = m.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            pluginDropping = shouldDrop(get(controller, "displaySize"), m);
            markGesture(true);
            if (DBG_PLUGIN.getAndIncrement() < 12) {
                MainModule.line("plugin DOWN x=" + m.getRawX() + " y=" + m.getRawY() + " drop=" + pluginDropping);
            }
        }
        boolean drop = pluginDropping;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            pluginDropping = false;
            markGesture(false);
        }
        return drop;
    }

    private static boolean shouldDrop(Object size, MotionEvent m) {
        if (hooks != 1 || !(size instanceof Point)) return false;
        Point sz = (Point) size;
        if (sz.x <= 0 || sz.y <= 0) return false;
        boolean outside = zone < 100 && m.getRawY() < sz.y * (1f - zone / 100f);
        boolean left = m.getRawX() < sz.x / 2f;
        boolean disabled = (left && leftOn == 0) || (!left && rightOn == 0);
        return outside || disabled;
    }

    private static void markGesture(boolean down) {
        gestureActive = down;
        gestureEnd = SystemClock.uptimeMillis() + 700;
    }

    static void rememberHaptic(Object enabled) {
        if (enabled instanceof Boolean) stockHaptic = (Boolean) enabled;
    }

    static void applyHaptic(Object controller) {
        if (controller == null) return;
        if (hooks == 1 && haptic == 0) {
            set(controller, "edgeHapticEnabled", Boolean.FALSE);
        } else if (stockHaptic != null) {
            set(controller, "edgeHapticEnabled", stockHaptic);
        }
    }

    private static boolean silenced() {
        return hooks == 1 && haptic == 0;
    }

    static boolean muteVibration() {
        return silenced() && (gestureActive || SystemClock.uptimeMillis() < gestureEnd);
    }

    static boolean muteViewHaptic(Object view) {
        return silenced() && view != null && view.getClass().getName().endsWith(".BackPanel");
    }

    private static void applyEdgeWidth(Object handler) {
        Object l = get(handler, "mEdgeWidthLeft");
        Object r = get(handler, "mEdgeWidthRight");
        if (!(l instanceof Integer) || !(r instanceof Integer)) return;
        int curL = (Integer) l;
        int curR = (Integer) r;
        if (curL != setL) origL = curL;
        if (curR != setR) origR = curR;
        float f = (hooks == 1) ? edgeW / 100f : 1f;
        int nl = Math.max(1, Math.round(origL * f));
        int nr = Math.max(1, Math.round(origR * f));
        setL = nl;
        setR = nr;
        if (nl != curL) set(handler, "mEdgeWidthLeft", Integer.valueOf(nl));
        if (nr != curR) set(handler, "mEdgeWidthRight", Integer.valueOf(nr));
    }

    static final class DrawState {
        int saveCount = -1;
        Object edge, far, arrowLen, arrowHei;
        float edgeOrig, farOrig, lenOrig, heiOrig;
        View view;
        float bw, bh, corner, density;
    }

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
        float[] origF = ORIG_F.get(view);
        if (orig == null || origF == null) {
            orig = new int[]{bgPaint.getColor(), fgPaint.getColor(), 0};
            origF = new float[]{fgPaint.getStrokeWidth()};
            ORIG.put(view, orig);
            ORIG_F.put(view, origF);
        }
        boolean active = hooks == 1 && look == 1;

        if (!active) {
            if (orig[2] == 1) {
                bgPaint.setColor(orig[0]);
                fgPaint.setColor(orig[1]);
                fgPaint.setStrokeWidth(origF[0]);
                bgPaint.setShader(null);
                bgPaint.clearShadowLayer();
                orig[2] = 0;
            } else {
                orig[0] = bgPaint.getColor();
                orig[1] = fgPaint.getColor();
                origF[0] = fgPaint.getStrokeWidth();
            }
            return null;
        }

        if (orig[2] == 0) {
            orig[0] = bgPaint.getColor();
            orig[1] = fgPaint.getColor();
            origF[0] = fgPaint.getStrokeWidth();
            orig[2] = 1;
        }
        float density = v.getResources().getDisplayMetrics().density;

        bgPaint.setColor(bg);
        fgPaint.setColor(fg);
        fgPaint.setStrokeWidth(stroke10 > 0 ? stroke10 / 10f * density : origF[0]);
        bgPaint.clearShadowLayer();

        Object bw = get(view, "backgroundWidth");
        Object bh = get(view, "backgroundHeight");
        float bwv = getFloat(bw, "pos", 0f);
        float bhv = getFloat(bh, "pos", 0f);

        if (grad == 1 && bwv > 0f && bhv > 0f) {
            double a = Math.toRadians(angle);
            float dx = (float) (Math.cos(a) * bwv / 2f);
            float dy = (float) (Math.sin(a) * bhv / 2f);
            float cx = bwv / 2f;
            bgPaint.setShader(new LinearGradient(cx - dx, -dy, cx + dx, dy, bg, bg2, Shader.TileMode.CLAMP));
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
        if (offsetY != 0) canvas.translate(0f, offsetY * density);
        float sx = scale100 / 100f * pw / 100f;
        float sy = scale100 / 100f * ph / 100f;
        if (sx != 1f || sy != 1f) {
            boolean left = Boolean.TRUE.equals(get(view, "isLeftPanel"));
            canvas.scale(sx, sy, left ? 0f : w, h * 0.5f);
        }

        if (glow > 0 && bwv > 0f && bhv > 0f) drawGlow(view, canvas, v, bwv, bhv, density);

        st.edge = get(view, "backgroundEdgeCornerRadius");
        st.far = get(view, "backgroundFarCornerRadius");
        st.edgeOrig = getFloat(st.edge, "pos", 0f);
        st.farOrig = getFloat(st.far, "pos", 0f);
        float target = Math.min(bwv, bhv) / 2f * (round / 100f);
        if (bwv > 0f && bhv > 0f) {
            setFloat(st.edge, "pos", target);
            setFloat(st.far, "pos", target);
        }
        st.view = v;
        st.bw = bwv;
        st.bh = bhv;
        st.corner = target;
        st.density = density;

        if (arrowSize != 100) {
            st.arrowLen = get(view, "arrowLength");
            st.arrowHei = get(view, "arrowHeight");
            st.lenOrig = getFloat(st.arrowLen, "pos", 0f);
            st.heiOrig = getFloat(st.arrowHei, "pos", 0f);
            setFloat(st.arrowLen, "pos", st.lenOrig * arrowSize / 100f);
            setFloat(st.arrowHei, "pos", st.heiOrig * arrowSize / 100f);
        }

        if (DBG_DRAW.getAndIncrement() < 6) {
            MainModule.line("draw look=1 alpha=" + alpha + " scale=" + scale100 + " pw=" + pw + " ph=" + ph
                    + " round=" + round + " grad=" + grad + " angle=" + angle + " arrow=" + arrowSize
                    + " stroke10=" + stroke10 + " glow=" + glow + " bw=" + bwv + " bh=" + bhv);
        }
        return st;
    }

    private static float enterPillSpace(Object view, Canvas canvas, View v) {
        boolean left = Boolean.TRUE.equals(get(view, "isLeftPanel"));
        float ht = getFloat(get(view, "horizontalTranslation"), "pos", 0f);
        float vt = getFloat(get(view, "verticalTranslation"), "pos", 0f);
        float sc = getFloat(get(view, "scale"), "pos", 1f);
        float px = getFloat(get(view, "scalePivotX"), "pos", 0f);
        canvas.save();
        if (!left) canvas.scale(-1f, 1f, v.getWidth() / 2f, 0f);
        canvas.translate(ht, v.getHeight() * 0.5f + vt);
        canvas.scale(sc, sc, px, 0f);
        return Math.min(getFloat(get(view, "backgroundAlpha"), "pos", 1f),
                getFloat(get(view, "arrowAlpha"), "pos", 1f));
    }

    private static void drawGlow(Object view, Canvas canvas, View v, float bw, float bh, float density) {
        float fade = enterPillSpace(view, canvas, v);
        float corner = Math.min(bw, bh) / 2f * (round / 100f);
        int steps = 7;
        float reach = glow * density;
        for (int i = steps; i >= 1; i--) {
            float grow = reach * i / steps;
            GLOW.setColor(bg);
            GLOW.setAlpha(Math.round(255f * fade * 0.10f));
            RECT.set(-grow, -bh / 2f - grow, bw + grow, bh / 2f + grow);
            canvas.drawRoundRect(RECT, corner + grow, corner + grow, GLOW);
        }
        canvas.restore();
    }

    private static void drawOutline(DrawState st, Canvas canvas) {
        float fade = enterPillSpace(st.view, canvas, st.view);
        float w = outline10 / 10f * st.density;
        float in = w / 2f;
        OUTLINE.setColor(fg);
        OUTLINE.setAlpha(Math.round(255f * fade));
        OUTLINE.setStrokeWidth(w);
        RECT.set(in, -st.bh / 2f + in, st.bw - in, st.bh / 2f - in);
        float r = Math.max(0f, st.corner - in);
        canvas.drawRoundRect(RECT, r, r, OUTLINE);
        canvas.restore();
    }

    static void afterDraw(Canvas canvas, DrawState st) {
        if (st == null) return;
        if (st.view != null && outline10 > 0 && st.bw > 0f && st.bh > 0f) drawOutline(st, canvas);
        if (st.saveCount >= 0) canvas.restoreToCount(st.saveCount);
        setFloat(st.edge, "pos", st.edgeOrig);
        setFloat(st.far, "pos", st.farOrig);
        if (st.arrowLen != null) {
            setFloat(st.arrowLen, "pos", st.lenOrig);
            setFloat(st.arrowHei, "pos", st.heiOrig);
        }
    }

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
            zone = clamp(num(p, 2, 100), 20, 100);
            look = num(p, 3, 0);
            alpha = clamp(num(p, 4, 100), 20, 100);
            scale100 = clamp(num(p, 5, 100), 30, 250);
            round = clamp(num(p, 6, 100), 0, 100);
            bg = num(p, 7, 0);
            fg = num(p, 8, 0);
            grad = num(p, 9, 0);
            bg2 = num(p, 10, 0);
            angle = num(p, 11, 90);
            pw = clamp(num(p, 12, 100), 30, 300);
            ph = clamp(num(p, 13, 100), 30, 300);
            arrowSize = clamp(num(p, 14, 100), 30, 300);
            stroke10 = clamp(num(p, 15, 0), 0, 200);
            glow = clamp(num(p, 16, 0), 0, 60);
            offsetY = clamp(num(p, 17, 0), -400, 400);
            leftOn = num(p, 18, 1);
            rightOn = num(p, 19, 1);
            edgeW = clamp(num(p, 20, 100), 30, 400);
            outline10 = clamp(num(p, 21, 0), 0, 120);
        } catch (Throwable ignored) {
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int num(String[] p, int i, int def) {
        try {
            return i < p.length ? Integer.parseInt(p[i].trim()) : def;
        } catch (Throwable t) {
            return def;
        }
    }

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
        ConcurrentHashMap<String, Field> perClass = FIELDS.get(c);
        if (perClass == null) {
            perClass = new ConcurrentHashMap<>();
            FIELDS.put(c, perClass);
        }
        Field cached = perClass.get(name);
        if (cached != null) return cached;
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                perClass.put(name, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
