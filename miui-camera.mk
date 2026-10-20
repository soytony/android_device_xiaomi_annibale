# SPDX-License-Identifier: Apache-2.0
# Keep Aperture during hardware validation. Disable this switch for the baseline camera set.
TARGET_INCLUDE_ANNIBALE_MIUI_CAMERA ?= true
ifeq ($(TARGET_INCLUDE_ANNIBALE_MIUI_CAMERA),true)
PRODUCT_PACKAGES += \
    AnnibaleMiuiCamera \
    AnnibaleMiuiCameraIcon \
    init.annibale.misys.rc
# The APK guards these stock optimization APIs; their MIUI services are not present.
PRODUCT_MISSING_USES_LIBRARIES += miui-cameraopt
endif
