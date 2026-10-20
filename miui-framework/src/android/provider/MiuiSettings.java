// SPDX-License-Identifier: Apache-2.0
package android.provider;

import android.content.ContentResolver;
import java.util.List;

/** Camera-only compatibility. Cloud configuration is absent; retain APK defaults. */
public final class MiuiSettings {
    private MiuiSettings() {}
    public static final class Global {
        public static boolean getBoolean(ContentResolver resolver, String key) {
            return Settings.Global.getInt(resolver, key, 0) != 0;
        }
    }
    public static final class System {
        public static boolean getBoolean(ContentResolver resolver, String key, boolean fallback) {
            return Settings.System.getInt(resolver, key, fallback ? 1 : 0) != 0;
        }
    }
    public static final class SettingsCloudData {
        public static List<Object> getCloudDataList(ContentResolver resolver, String module) {
            // Stock caller treats null as unavailable; an empty list becomes the JSON string [].
            return null;
        }
        public static String getCloudDataString(ContentResolver resolver, String module,
                String key, String fallback) {
            return fallback;
        }
    }
}
