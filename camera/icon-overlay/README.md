# Annibale Camera adaptive icon

Platform-signed RRO for the pinned camera's `mipmap/eji` launcher icon. Replaces
its legacy square tile with independent background/foreground layers so Android
can apply its current icon shape. The lens stays inside the adaptive safe area;
Android 13+ also gets a monochrome layer for themed icons.

Foreground comes from the stock APK `res/9eM.webp`: isolate the circular lens
(bounds 111,111–595,595 of the 705px image), resize to 256px, and center in a
432px transparent canvas. The pinned APK patch appends an `AnnibaleCameraIcon` overlayable declaration
for only resource `0x7f110000`, with same-signature policy. Existing resource
IDs, values, images and other resource chunks stay intact.
The RRO uses the same platform certificate as the camera for signature overlay
policy; it is included only with `TARGET_INCLUDE_ANNIBALE_MIUI_CAMERA`.
