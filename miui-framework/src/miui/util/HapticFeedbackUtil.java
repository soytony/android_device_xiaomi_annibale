// SPDX-License-Identifier: Apache-2.0
package miui.util;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

/** Maps camera haptic requests onto Android's supported click effect. */
public final class HapticFeedbackUtil {
    private final Context context;
    private final Vibrator vibrator;
    public HapticFeedbackUtil(Context context, boolean unused) {
        this.context = context;
        vibrator = context.getSystemService(Vibrator.class);
    }
    // Do not advertise Xiaomi extended motor effects or firmware-specific effect IDs.
    public static boolean isSupportLinearMotorVibrate() { return false; }
    public static boolean isSupportLinearMotorVibrate(int effect) { return false; }
    private boolean click() {
        if (vibrator == null || !vibrator.hasVibrator()
                || Settings.System.getInt(context.getContentResolver(),
                        Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return false;
        try {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
                    new VibrationAttributes.Builder()
                            .setUsage(VibrationAttributes.USAGE_TOUCH).build());
            return true;
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }
    public boolean performExtHapticFeedback(int effect) { return click(); }
    public boolean performExtHapticFeedback(int effect, double strength, String reason) { return click(); }
    public boolean performHapticFeedback(int effect, double strength, String reason) { return click(); }
    public boolean performHapticFeedback(int effect, boolean always) { return click(); }
    public boolean performHapticFeedback(int effect, boolean always, int strength) { return click(); }
    public boolean performHapticFeedback(VibrationAttributes attrs, int effect, double strength, String reason) { return click(); }
    public boolean performHapticFeedback(VibrationAttributes attrs, int effect, boolean always) { return click(); }
    public boolean performHapticFeedback(VibrationAttributes attrs, int effect, boolean always, int strength) { return click(); }
    public void release() {}
}
