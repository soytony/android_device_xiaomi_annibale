// SPDX-License-Identifier: Apache-2.0
package miui.hardware.display;

import android.util.Log;

/** MIUI display tuning has no equivalent service on this product. */
public final class DisplayFeatureManager {
    private static final DisplayFeatureManager INSTANCE = new DisplayFeatureManager();
    public static DisplayFeatureManager getInstance() { return INSTANCE; }
    public void setScreenEffect(int mode, int value) {
        Log.d("AnnibaleCamera", "Unsupported MIUI display effect: " + mode + "/" + value);
    }
}
