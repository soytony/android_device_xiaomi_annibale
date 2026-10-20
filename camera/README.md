# Annibale HyperOS Camera

This product packages HyperOS Camera 6.3.007190.0 with a small, source-built
`annibale-miui-camera-framework` shared library. No bootclasspath changes.
The APK and its permission files install in system_ext because it uses private
platform JNI dependencies. Aperture remains installed during device validation.

The extraction source is annibale OS3.0.307.0.WPKCNXM (China), not an EU build.
`patch_camera.py` checks the exact original APK SHA256 before changing anything.
It adds required shared-library linkage, selects the existing non-MIUI consent
flow, disables unavailable AI/OZO audio recording extensions and adapts gallery
handoff. It also admits three registered Xiaomi/Qualcomm metadata keys omitted
from the generic request list: the MIVI image filename, QCFA sensor flag and zoom-ratio support flag.
The filename preserves full-image handoff; the QCFA flag selects the supported
front stream mode. The zoom flag selects Android zoom-ratio requests so 0.6×
activates the ultrawide lens. Other capability guards remain unchanged.

The stock MiSys common/core services supply the camera storage handoff needed
for watermark/LUT files and the startup write check. Device policy grants the
app background-capture Binder access and MiSys discovery; MiSys receives scoped
camera storage access. SELinux remains enforcing.

Android runtime permission handling remains intact. Original resource values,
assets and native libraries are preserved. The resource table gains only a
same-signature overlayable declaration for its launcher icon; `icon-overlay/`
provides adaptive foreground/background layers and a monochrome themed icon. Output ZIP ordering/timestamps are
normalized; the ROM build signs and aligns the APK with its platform key.
Xiaomi-signed APK updates cannot directly update this patched camera.

Regenerate from the mounted stock dump using extract-files.py (the camera fixup
runs automatically), then regenerate vendor makefiles. The APK is an extract-only
filegroup so the device module controls shared-library declarations and signing.
The additional stock ARM64 camera_algoup JNI library retains ELF build checks.

Set TARGET_INCLUDE_ANNIBALE_MIUI_CAMERA := false before inheriting device.mk to
omit the camera app. Do not remove Aperture until on-device validation passes.

Compatibility scope:

- Android settings and basic haptics have functional adapters.
- MIUI cloud configuration returns unavailable/default data.
- Proprietary display tuning and MiMotion optimizations are not implemented.
- AI/OZO/wind-noise recording extensions are disabled; ordinary audio recording
  stays with the existing media/audio stack.
- Motion Photo requires `ro.product.camera.livephoto.support=3` in product
  properties. Capture embeds HEVC video and AAC audio in the JPEG; gallery
  playback support depends on the gallery app.
- Generic gallery review is blocked while keyguard is locked.
- MIUI Gallery editors, cloud, CameraMind and advanced modes require separate
  hardware validation; packaging does not establish stock feature parity.

See documents/annibale/hyperos-camera-integration-plan.md and the associated
implementation journal for evidence, tests, limitations and rollback steps.
