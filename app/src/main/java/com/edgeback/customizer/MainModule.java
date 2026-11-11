package com.edgeback.customizer;

import android.graphics.Canvas;
import android.os.Build;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import io.github.libxposed.api.XposedModule;

public class MainModule extends XposedModule {

    private static final String TAG = "EBC";
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String SELF = "com.edgeback.customizer";
    private static final File LOG = new File("/data/user/0/com.android.systemui/files/ebc.log");

    private static final String GESTURE_PKG = "com.android.systemui.navigationbar.gestural.";
    private static final String HANDLER = GESTURE_PKG + "EdgeBackGestureHandler";
    private static final String PLUGIN = GESTURE_PKG + "BackPanelController";
    private static final String PANEL = GESTURE_PKG + "BackPanel";
    private static final String VIBRATOR = "com.android.systemui.statusbar.VibratorHelper";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        Log.i(TAG, "module loaded");
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        try {
            if (!param.isFirstPackage()) return;
            String pkg = param.getPackageName();
            if (SELF.equals(pkg)) {
                markActive(param.getClassLoader());
                return;
            }
            if (!SYSTEMUI.equals(pkg)) return;

            ClassLoader cl = param.getClassLoader();
            LOG.delete();
            line("sdk=" + Build.VERSION.SDK_INT + " device=" + Build.MANUFACTURER + " " + Build.MODEL);
            line("status.framework=" + safe(() -> String.valueOf(getFrameworkName())));
            line("status.version=" + safe(() -> String.valueOf(getFrameworkVersion())));
            line("status.versionCode=" + safe(() -> String.valueOf(getFrameworkVersionCode())));
            line("status.api=" + safe(() -> String.valueOf(getApiVersion())));
            line("status.time=" + System.currentTimeMillis());

            hookInput(cl);
            hookPlugin(cl);
            hookDraw(cl);
            hookVibration(cl);
        } catch (Throwable t) {
            line("load failed: " + Log.getStackTraceString(t));
        }
    }

    private void markActive(ClassLoader cl) {
        final Class<?> c;
        try {
            c = Class.forName("com.edgeback.customizer.ModuleStatus", true, cl);
            c.getField("active").setBoolean(null, true);
        } catch (Throwable t) {
            Log.e(TAG, "markActive failed", t);
            return;
        }
        put(c, "frameworkName", () -> String.valueOf(getFrameworkName()));
        put(c, "frameworkVersion", () -> String.valueOf(getFrameworkVersion()));
        put(c, "frameworkVersionCode", () -> String.valueOf(getFrameworkVersionCode()));
        put(c, "apiVersion", () -> String.valueOf(getApiVersion()));
    }

    private void hookInput(ClassLoader cl) {
        Method m = find(load(cl, HANDLER), "onInputEvent", 1);
        if (m == null) {
            line("input: onInputEvent not found");
            return;
        }
        try {
            hook((Executable) m).intercept(chain -> {
                boolean drop = false;
                try {
                    drop = Core.beforeInput(chain.getThisObject(), chain.getArg(0));
                } catch (Throwable ignored) {
                }
                if (drop) return null;
                return chain.proceed();
            });
            line("input: hooked");
        } catch (Throwable t) {
            line("input: " + t);
        }
    }

    private void hookPlugin(ClassLoader cl) {
        Class<?> c = load(cl, PLUGIN);
        Method motion = find(c, "onMotionEvent", 1);
        Method haptic = find(c, "setEdgeHapticEnabled", 1);
        try {
            if (motion != null) {
                hook((Executable) motion).intercept(chain -> {
                    boolean drop = false;
                    try {
                        drop = Core.pluginGate(chain.getThisObject(), chain.getArg(0));
                    } catch (Throwable ignored) {
                    }
                    if (drop) return null;
                    return chain.proceed();
                });
                line("plugin: motion hooked");
            }
            if (haptic != null) {
                hook((Executable) haptic).intercept(chain -> {
                    try {
                        Core.rememberHaptic(chain.getArg(0));
                    } catch (Throwable ignored) {
                    }
                    Object result = chain.proceed();
                    try {
                        Core.applyHaptic(chain.getThisObject());
                    } catch (Throwable ignored) {
                    }
                    return result;
                });
                line("plugin: haptic hooked");
            }
        } catch (Throwable t) {
            line("plugin: " + t);
        }
    }

    private void hookDraw(ClassLoader cl) {
        Method m = find(load(cl, PANEL), "onDraw", 1);
        if (m == null) {
            line("draw: onDraw not found");
            return;
        }
        try {
            hook((Executable) m).intercept(chain -> {
                Object view = chain.getThisObject();
                Object arg = chain.getArg(0);
                Canvas canvas = arg instanceof Canvas ? (Canvas) arg : null;
                Core.DrawState state = null;
                if (canvas != null) {
                    try {
                        state = Core.beforeDraw(view, canvas);
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    return chain.proceed();
                } finally {
                    if (canvas != null) {
                        try {
                            Core.afterDraw(canvas, state);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            });
            line("draw: hooked");
        } catch (Throwable t) {
            line("draw: " + t);
        }
    }

    private void hookVibration(ClassLoader cl) {
        try {
            Class<?> helper = load(cl, VIBRATOR);
            if (helper != null) {
                for (Method m : helper.getDeclaredMethods()) {
                    if (!m.getName().equals("vibrate")) continue;
                    hook((Executable) m).intercept(chain -> {
                        if (Core.muteVibration()) return null;
                        return chain.proceed();
                    });
                }
            }
            for (Method m : View.class.getDeclaredMethods()) {
                if (!m.getName().equals("performHapticFeedback")) continue;
                hook((Executable) m).intercept(chain -> {
                    if (Core.muteViewHaptic(chain.getThisObject())) return Boolean.FALSE;
                    return chain.proceed();
                });
            }
            line("vibration: hooked");
        } catch (Throwable t) {
            line("vibration: " + t);
        }
    }

    private static Method find(Class<?> c, String name, int params) {
        if (c == null) return null;
        try {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == params) return m;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Class<?> load(ClassLoader cl, String name) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void put(Class<?> c, String field, Supplier<String> value) {
        try {
            c.getField(field).set(null, value.get());
        } catch (Throwable t) {
            Log.w(TAG, "markActive " + field, t);
        }
    }

    private static String safe(Supplier<String> s) {
        try {
            return s.get();
        } catch (Throwable t) {
            return "";
        }
    }

    static synchronized void line(String s) {
        Log.i(TAG, s);
        try {
            File dir = LOG.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            try (FileWriter w = new FileWriter(LOG, true)) {
                w.write(s + "\n");
            }
        } catch (Throwable ignored) {
        }
    }
}
