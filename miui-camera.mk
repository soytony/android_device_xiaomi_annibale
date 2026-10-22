# SPDX-License-Identifier: Apache-2.0
# Disable this switch to omit the stock MIUI Camera package set.
TARGET_INCLUDE_ANNIBALE_MIUI_CAMERA ?= true
ifeq ($(TARGET_INCLUDE_ANNIBALE_MIUI_CAMERA),true)
PRODUCT_PACKAGES += \
    AnnibaleMiuiCamera \
    AnnibaleMiuiCameraIcon \
    init.annibale.misys.rc
# The APK guards these stock optimization APIs; their MIUI services are not present.
PRODUCT_MISSING_USES_LIBRARIES += miui-cameraopt
endif
