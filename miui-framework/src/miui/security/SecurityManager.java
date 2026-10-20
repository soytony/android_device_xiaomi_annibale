// SPDX-License-Identifier: Apache-2.0
package miui.security;

/** No MIUI app-lock service is registered. Fail closed if called unexpectedly. */
public final class SecurityManager {
    public boolean getApplicationAccessControlEnabled(String packageName) { return true; }
    public boolean checkAccessControlPass(String packageName) { return false; }
}
