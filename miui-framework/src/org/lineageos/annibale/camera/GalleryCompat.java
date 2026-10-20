// SPDX-License-Identifier: Apache-2.0
package org.lineageos.annibale.camera;

import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Retains the camera's capture flow while replacing Xiaomi-only gallery intents. */
public final class GalleryCompat {
    public static void startActivity(Context context, Intent original) {
        String target = original.getPackage();
        if (original.getComponent() != null) target = original.getComponent().getPackageName();
        boolean gallery = "com.miui.gallery".equals(target)
                || "com.miui.mediaviewer".equals(target)
                || (original.getAction() != null
                    && original.getAction().startsWith("com.miui.gallery."));
        if (!gallery) {
            context.startActivity(original);
            return;
        }
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        if (keyguard != null && keyguard.isKeyguardLocked()) {
            // A generic gallery must not expose the media collection from secure capture.
            throw new ActivityNotFoundException("Unlock before opening the gallery");
        }
        Uri uri = original.getData();
        Intent view;
        if (uri != null && "content".equals(uri.getScheme())) {
            view = new Intent(Intent.ACTION_VIEW);
            String type = original.getType();
            if (type == null) type = context.getContentResolver().getType(uri);
            view.setDataAndType(uri, type);
            view.setClipData(original.getClipData());
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            view = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY);
        }
        context.startActivity(view);
    }
}
