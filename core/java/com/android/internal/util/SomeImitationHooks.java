/*
 * Copyright (C) 2024 SomethingOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.Application;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.res.Resources;
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemProperties;
import android.os.Build;
import android.os.Binder;
import android.os.Process;
import android.util.Log;
import android.text.TextUtils;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SomeImitationHooks {

    private static final String TAG = "SomeImitationHooks";
    private static final boolean DEBUG = false;

    // Packages
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";
    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    // Apps
    private static volatile boolean sIsGms, sIsFinsky, sIsGPhotos;

    // Props
    private static volatile boolean sUnlimitedGPhotos = SystemProperties.getBoolean("persist.sys.sih.unlimited_gphotos", false);
    private static volatile boolean sPixelFeatures = SystemProperties.getBoolean("persist.sys.sih.pixel_features", false);
    private static volatile boolean sIntegritySpoof = SystemProperties.getBoolean("persist.sys.sih.integrity_spoof", true);
    private static volatile boolean sBlockECC = SystemProperties.getBoolean("persist.sys.sih.block_ecc", true);

    private static final String PROP_SECURITY_PATCH = "persist.sys.somethingos.gms.SECURITY_PATCH";
    private static final String PROP_FIRST_API_LEVEL = "persist.sys.somethingos.gms.PROP_FIRST_API_LEVEL";

    // Pixels
    private static final Map<String, String> LatestPixelProps;
    private static final Map<String, String> PixelXLProps;

    // Certified props
    private static final String[] certifiedProps = {
        "MANUFACTURER",
        "BRAND",
        "DEVICE",
        "MODEL",
        "PRODUCT",
        "FINGERPRINT",
        "SECURITY_PATCH",
        "FIRST_API_LEVEL"
    };

    // Fill Devices
    static {
        LatestPixelProps = new HashMap<>();
        LatestPixelProps.put("BRAND", "google");
        LatestPixelProps.put("MANUFACTURER", "Google");
        LatestPixelProps.put("DEVICE", "caiman");
        LatestPixelProps.put("PRODUCT", "caiman");
        LatestPixelProps.put("HARDWARE", "caiman");
        LatestPixelProps.put("MODEL", "Pixel 9 Pro");
        LatestPixelProps.put("ID", "AD1A.240530.047.U1");
        LatestPixelProps.put("FINGERPRINT", "google/caiman/caiman:14/AD1A.240530.047.U1/12150698:user/release-keys");
        PixelXLProps = new HashMap<>();
        PixelXLProps.put("BRAND", "google");
        PixelXLProps.put("MANUFACTURER", "Google");
        PixelXLProps.put("DEVICE", "marlin");
        PixelXLProps.put("PRODUCT", "marlin");
        PixelXLProps.put("HARDWARE", "marlin");
        PixelXLProps.put("MODEL", "Pixel XL");
        PixelXLProps.put("ID", "QP1A.191005.007.A3");
        PixelXLProps.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }

    // Apps by device
    private static final String[] latestPixelPackages = {
        "com.android.vending",
        "com.google.android.apps.customization.pixel",
        "com.google.android.apps.emojiwallpaper",
        "com.google.android.apps.privacy.wildlife",
        "com.google.android.apps.subscriptions.red",
        "com.google.android.apps.wallpaper",
        "com.google.android.apps.wallpaper.pixel",
        "com.google.android.googlequicksearchbox",
        "com.google.android.wallpaper.effects",
        "com.google.android.apps.bard",
        "com.google.pixel.livewallpaper",
        "com.nhs.online.nhsonline",
        "com.netflix.mediaclient"
    };

    // Methods

    public static void setProps(Application app) {
        final String packageName = app.getPackageName();
        final String processName = app.getProcessName();

        if (packageName == null || processName == null) {
            return;
        }

        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);
        sIsGPhotos = packageName.equals(PACKAGE_GPHOTOS);

        if (sIsGPhotos && sUnlimitedGPhotos) {
            applyProps(PixelXLProps);
        } else if (sPixelFeatures && Arrays.asList(latestPixelPackages).contains(packageName)) {
            applyProps(LatestPixelProps);
        } else if (sIsGms && sIntegritySpoof) {
            applyGMSSpoof();
        }
    }

    private static void applyProps(Map<String, String> props) {
        for (Map.Entry<String, String> prop : props.entrySet()) {
            String key = prop.getKey();
            String value = prop.getValue();
            setPropValue(key, value);
        }
    }

    private static void setPropValue(String key, Object value){
        try {
            dlog("Setting prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        if ((isCallerSafetyNet() || sIsFinsky) && sBlockECC) {
            dlog("Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }

    private static void setCertifiedProps() {
        for (String key : certifiedProps) {
            String value = SystemProperties.get("persist.sys.somethingos.gms." + key);
            if (value != null && !value.isEmpty()) {
                if (key.equals("SECURITY_PATCH")) {
                    setSystemProperty(PROP_SECURITY_PATCH, value);
                } else if (key.equals("FIRST_API_LEVEL")) {
                    setSystemProperty(PROP_FIRST_API_LEVEL, value);
                } else {
                    setPropValue(key, value);
                }
            }
        }
    }

    private static void setSystemProperty(String name, String value) {
        try {
            SystemProperties.set(name, value);
            dlog("Set system prop " + name + "=" + value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set system prop " + name + "=" + value, e);
        }
    }

    private static void applyGMSSpoof() {
        final boolean was = isGmsAddAccountActivityOnTop();
        final TaskStackListener taskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                final boolean is = isGmsAddAccountActivityOnTop();
                if (is ^ was) {
                    dlog("GmsAddAccountActivityOnTop is:" + is + " was:" + was +
                            ", killing myself!");
                    Process.killProcess(Process.myPid());
                }
            }
        };
        if (!was) {
            dlog("Spoofing build for GMS");
            setCertifiedProps();
        } else {
            dlog("Skip spoofing build for GMS, because GmsAddAccountActivityOnTop");
        }
        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener!", e);
        }
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        if (!sIntegritySpoof) {
            return false;
        }

        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final int gmsUid;
        try {
            gmsUid = context.getPackageManager().getApplicationInfo(PACKAGE_GMS, 0).uid;
            dlog("shouldBypassTaskPermission: gmsUid:" + gmsUid + " callingUid:" + callingUid);
        } catch (Exception e) {
            Log.e(TAG, "shouldBypassTaskPermission: unable to get gms uid", e);
            return false;
        }
        return gmsUid == callingUid;
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static void dlog(String msg) {
      if (DEBUG) Log.d(TAG, msg);
    }
}
