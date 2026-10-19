# SPDX-License-Identifier: Apache-2.0
# Stock annibale HAL/blobs are selected by annibale-vendor.mk.
# The generic SM6375 Dolby HAL is not compatible with this 64-bit audio stack.
PRODUCT_SOONG_NAMESPACES += vendor/lunaris/dolby
PRODUCT_PACKAGES += \
    LunarisDolby \
    XiaomiDolbyResCommon \
    LunarisDolbyPrivappPermissions
