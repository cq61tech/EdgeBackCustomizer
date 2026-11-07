package com.edgeback.customizer;

/**
 * Filled in by the Xposed module (MainModule) when it is loaded into this very app.
 * If {@link #active} is still false, the module is not injected.
 */
public final class ModuleStatus {
    private ModuleStatus() {}

    public static volatile boolean active = false;
    public static volatile String frameworkName = "";
    public static volatile String frameworkVersion = "";
    public static volatile String frameworkVersionCode = "";
    public static volatile String apiVersion = "";
}
