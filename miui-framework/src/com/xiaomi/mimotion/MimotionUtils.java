// SPDX-License-Identifier: Apache-2.0
package com.xiaomi.mimotion;

/** Proprietary MiMotion is unavailable; let the application's normal path run. */
public final class MimotionUtils {
    public static boolean isEnabled() { return false; }
    public static boolean setPreferredRefreshRate(Object target, int rate) { return false; }
}
