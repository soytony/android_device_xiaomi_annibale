// SPDX-License-Identifier: Apache-2.0
package org.lineageos.annibale.telephony;

import com.android.internal.telephony.DisplayInfoController;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.flags.FeatureFlags;

/** Adds the device's modem display bridge without replacing standard radio behavior. */
public final class AnnibaleTelephonyComponentFactory extends TelephonyComponentFactory {
    @Override
    public DisplayInfoController makeDisplayInfoController(Phone phone, FeatureFlags flags) {
        DisplayInfoController controller = super.makeDisplayInfoController(phone, flags);
        // Post after the phone has installed its display controller and NTC has registered.
        controller.post(() -> new NrIconBridge(phone, controller));
        return controller;
    }
}
