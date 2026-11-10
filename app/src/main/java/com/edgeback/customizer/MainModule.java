package com.edgeback.customizer;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.github.libxposed.api.XposedModule;

/**
 * Phase 0 (discovery). Changes no behaviour:
 *  1. dumps fields/methods of the edge-back classes to a file + logcat (tag EBC)
 *  2. scans SystemUI dex for candidate class names
 *  3. installs "probe" hooks that only log (then call the original) so we can
 *     see which methods really run when a back swipe happens.
 */
public class MainModule extends XposedModule {

    static final String TAG = "EBC";
    static final String SYSTEMUI = "com.android.systemui";
    static final String SELF = "com.edgeback.customizer";
    static final File OUT = new File("/data/user/0/com.android.systemui/files/ebc_dump.txt");
    static final int PROBE_LIMIT = 40;

    static final String[] KNOWN = {
            "com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler",
            "com.android.systemui.navigationbar.gestural.BackPanelController",
            "com.android.systemui.navigationbar.gestural.BackPanel",
            "com.android.systemui.navigationbar.gestural.NavigationBarEdgePanel",
            "com.android.systemui.statusbar.VibratorHelper",
            // added in 0.1.0 (second discovery round)
            "com.android.systemui.navigationbar.gestural.DisplayBackGestureHandlerImpl",
            "com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler$5",
            "com.android.systemui.navigationbar.gestural.BackPanel$AnimatedFloat",
            "com.android.systemui.navigationbar.gestural.BackPanelController$GestureState",
            "com.android.systemui.navigationbar.gestural.EdgePanelParams$BackIndicatorDimens",
            "com.android.systemui.plugins.NavigationEdgeBackPlugin$BackCallback",
    };

    static final String[] SCAN_NEEDLES = {
            "EdgeBack", "BackPanel", "EdgePanel", "BackAnimation", "VibratorHelper", "DisplayBackGesture"
    };

    static final String[] PROBE_METHOD_HINTS = {
            "trigger", "within", "vibrat", "onmotionevent", "oninputevent", "edgeheight", "cancelgesture", "allowgesture"
    };

    static final ConcurrentHashMap<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        Log.i(TAG, "module loaded");
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        try {
            if (!param.isFirstPackage()) return;
            if (SELF.equals(param.getPackageName())) {
                markActive(param.getClassLoader());
                return;
            }
            if (!SYSTEMUI.equals(param.getPackageName())) return;
            final ClassLoader cl = param.getClassLoader();

            try {
                OUT.delete();
            } catch (Throwable ignored) {
            }
            line("=== EdgeBackCustomizer discovery ===");
            line("SDK_INT=" + Build.VERSION.SDK_INT + " release=" + Build.VERSION.RELEASE
                    + " codename=" + Build.VERSION.CODENAME);
            line("fingerprint=" + Build.FINGERPRINT);
            line("model=" + Build.MANUFACTURER + " " + Build.MODEL);
            // lines below are read by the companion app through root ("status." prefix)
            line("status.framework=" + safe(() -> String.valueOf(getFrameworkName())));
            line("status.version=" + safe(() -> String.valueOf(getFrameworkVersion())));
            line("status.versionCode=" + safe(() -> String.valueOf(getFrameworkVersionCode())));
            line("status.api=" + safe(() -> String.valueOf(getApiVersion())));
            line("status.time=" + System.currentTimeMillis());
            String statusErr = writeStatus();
            line("status.file=" + (statusErr == null ? "ok" : statusErr));

            // Known candidates: dump + probe synchronously.
            for (String name : KNOWN) {
                Class<?> c = load(cl, name);
                if (c == null) {
                    line("MISSING " + name);
                    continue;
                }
                dumpClass(c);
                probeClass(c);
            }

            // Scan the dex in the background (no hooks there).
            Thread t = new Thread(() -> scanAndDump(cl), "EBC-scan");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            line("onPackageReady failed: " + Log.getStackTraceString(t));
        }
    }

    // ------------------------------------------------------- module status

    /** Tells our own app (running in its own process) that the module is injected. */
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

    /** Best-effort copy of the status as its own file. Returns null on success, else the error. */
    private String writeStatus() {
        try {
            File f = new File("/data/user/0/com.android.systemui/files/ebc_status.txt");
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            long boot = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
            StringBuilder sb = new StringBuilder();
            sb.append("framework=").append(safe(() -> String.valueOf(getFrameworkName()))).append('\n');
            sb.append("version=").append(safe(() -> String.valueOf(getFrameworkVersion()))).append('\n');
            sb.append("versionCode=").append(safe(() -> String.valueOf(getFrameworkVersionCode()))).append('\n');
            sb.append("api=").append(safe(() -> String.valueOf(getApiVersion()))).append('\n');
            sb.append("time=").append(System.currentTimeMillis()).append('\n');
            sb.append("boot=").append(boot).append('\n');
            try (FileWriter w = new FileWriter(f, false)) {
                w.write(sb.toString());
            }
            return null;
        } catch (Throwable t) {
            return String.valueOf(t);
        }
    }

    // ---------------------------------------------------------------- probe

    private void probeClass(Class<?> c) {
        int hooked = 0;
        Method[] methods;
        try {
            methods = c.getDeclaredMethods();
        } catch (Throwable t) {
            line("probe: getDeclaredMethods failed for " + c.getName() + ": " + t);
            return;
        }
        for (Method m : methods) {
            if (Modifier.isAbstract(m.getModifiers()) || Modifier.isNative(m.getModifiers())) continue;
            if (!matchesHint(m.getName())) continue;
            final String label = c.getSimpleName() + "." + m.getName() + sig(m);
            try {
                hook((Executable) m).intercept(chain -> {
                    AtomicInteger n = COUNTERS.computeIfAbsent(label, k -> new AtomicInteger());
                    int i = n.incrementAndGet();
                    if (i <= PROBE_LIMIT) {
                        line("CALL " + label + " #" + i);
                    }
                    return chain.proceed();
                });
                hooked++;
            } catch (Throwable t) {
                line("probe hook failed " + label + ": " + t);
            }
        }
        line("probe: hooked " + hooked + " method(s) in " + c.getName());
    }

    private static boolean matchesHint(String methodName) {
        String n = methodName.toLowerCase();
        for (String h : PROBE_METHOD_HINTS) {
            if (n.contains(h)) return true;
        }
        return false;
    }

    // ----------------------------------------------------------------- dump

    private static void scanAndDump(ClassLoader cl) {
        try {
            List<String> names = scanClassNames(cl);
            StringBuilder sb = new StringBuilder();
            sb.append("--- scan: ").append(names.size()).append(" matching class names ---\n");
            for (String n : names) sb.append(n).append('\n');
            append(sb.toString());

            Set<String> extra = new LinkedHashSet<>();
            for (String n : names) {
                if (n.contains("$")) continue;
                String simple = n.substring(n.lastIndexOf('.') + 1);
                if (simple.contains("EdgeBackGestureHandler") || simple.contains("BackPanel")
                        || simple.contains("EdgePanel")) {
                    boolean known = false;
                    for (String k : KNOWN) if (k.equals(n)) known = true;
                    if (!known) extra.add(n);
                }
            }
            int count = 0;
            for (String n : extra) {
                if (count++ >= 12) break;
                Class<?> c = load(cl, n);
                if (c != null) dumpClass(c);
            }
            line("=== scan done ===");
        } catch (Throwable t) {
            line("scanAndDump failed: " + Log.getStackTraceString(t));
        }
    }

    @SuppressWarnings({"unchecked", "JavaReflectionMemberAccess", "PrivateApi"})
    private static List<String> scanClassNames(ClassLoader cl) {
        List<String> res = new ArrayList<>();
        try {
            Class<?> bdcl = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pl = bdcl.getDeclaredField("pathList");
            pl.setAccessible(true);
            Object pathList = pl.get(cl);
            Field de = pathList.getClass().getDeclaredField("dexElements");
            de.setAccessible(true);
            Object[] elements = (Object[]) de.get(pathList);
            for (Object el : elements) {
                Field df = el.getClass().getDeclaredField("dexFile");
                df.setAccessible(true);
                Object dexFile = df.get(el);
                if (dexFile == null) continue;
                Method entries = dexFile.getClass().getMethod("entries");
                Enumeration<String> en = (Enumeration<String>) entries.invoke(dexFile);
                while (en.hasMoreElements()) {
                    String n = en.nextElement();
                    for (String needle : SCAN_NEEDLES) {
                        if (n.contains(needle)) {
                            res.add(n);
                            break;
                        }
                    }
                    if (res.size() >= 400) return res;
                }
            }
        } catch (Throwable t) {
            line("scanClassNames failed: " + t);
        }
        return res;
    }

    private static void dumpClass(Class<?> c) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n##### ").append(c.getName());
        try {
            Class<?> sup = c.getSuperclass();
            if (sup != null) sb.append(" extends ").append(sup.getName());
            Class<?>[] ifs = c.getInterfaces();
            if (ifs.length > 0) {
                sb.append(" implements");
                for (Class<?> i : ifs) sb.append(' ').append(i.getName());
            }
        } catch (Throwable ignored) {
        }
        sb.append('\n');
        try {
            for (Field f : c.getDeclaredFields()) {
                sb.append("F ").append(Modifier.toString(f.getModifiers())).append(' ')
                        .append(f.getType().getName()).append(' ').append(f.getName()).append('\n');
            }
        } catch (Throwable t) {
            sb.append("fields failed: ").append(t).append('\n');
        }
        try {
            for (Method m : c.getDeclaredMethods()) {
                sb.append("M ").append(Modifier.toString(m.getModifiers())).append(' ')
                        .append(m.getReturnType().getName()).append(' ')
                        .append(m.getName()).append(sig(m)).append('\n');
            }
        } catch (Throwable t) {
            sb.append("methods failed: ").append(t).append('\n');
        }
        append(sb.toString());
    }

    // -------------------------------------------------------------- helpers

    private static String sig(Method m) {
        StringBuilder sb = new StringBuilder("(");
        Class<?>[] p = m.getParameterTypes();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(p[i].getSimpleName());
        }
        return sb.append(')').toString();
    }

    private static Class<?> load(ClassLoader cl, String name) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void line(String s) {
        Log.i(TAG, s);
        append(s + "\n");
    }

    private static synchronized void append(String block) {
        try {
            File dir = OUT.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            try (FileWriter w = new FileWriter(OUT, true)) {
                w.write(block);
            }
        } catch (Throwable ignored) {
        }
    }
}
