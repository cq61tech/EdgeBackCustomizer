package com.edgeback.customizer;

public final class ModuleStatus {
    private ModuleStatus() {}

    public static volatile boolean active = false;
    public static volatile String frameworkName = "";
    public static volatile String frameworkVersion = "";
    public static volatile String frameworkVersionCode = "";
    public static volatile String apiVersion = "";
}
