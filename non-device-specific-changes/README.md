# Annibale shared-module patches

Evolution-X cnb / Android 17, captured 2026-10-20. This catalog follows
RoadSTR's non-device-specific-changes layout and records the shared changes
needed for Annibale's kernel-header provider and verified NR advanced display.
Device/vendor integration remains in the corresponding device/vendor trees.

[series.json](series.json) records exact base revisions, ordered patches,
source commits where available, reasons, and expected final Git tree IDs.

## Scope

- `vendor/lineage`: inherited generator prerequisites plus opt-in unpacked
  prebuilt UAPI headers. Annibale selects `TARGET_PREBUILT_KERNEL_HEADER_DIR`.
  The provider declares each header output and Python/libclang cleanup input
  so sandboxing retains real cleaned headers rather than only a success stamp.
  Patch 0003 captures the existing **uncommitted** shared-tree changes; it
  does not imply those changes have a source commit.
- `frameworks/opt/telephony`: accept per-phone advanced-icon hints through
  existing carrier restrictions. Qualcomm-specific binding stays in Annibale.
  The source commit also contains Motorola RAT-30 normalization and its tests;
  it overlaps RoadSTR's separate NR_CA supplement. Do not apply both blindly.
- `packages/services/Telephony`: explicitly request Qualcomm's extension
  service permission. A device overlay cannot add uses-permission declarations.
  Annibale installs the privileged allowlist on the same partition as TeleService.

Annibale's device commit `2feec032f93ebf13b728a42e76a1bd0a93cae0b4`
contains the modem callback bridge, injection XML, privileged allowlist, and
China Unicom 5G+ mapping. Keep it paired with both telephony patches.
Modem type 2 supplies an advanced-icon hint; it does not manufacture a CA flag
or prove NR carrier aggregation.

## Apply

Use clean repositories at the exact `base_commit` listed in series.json.
From the Android root, for each repository:

1. Apply each patch in listed order using its absolute path.
2. For `format: mail`, use `git -C <repo> am <patch>`.
3. For `format: diff` (vendor/lineage patch 0003), use
   `git -C vendor/lineage apply --index <patch>`, then commit the staged change
   with the subject recorded in series.json.

Repositories already carrying these changes need no application. If the base
or existing patches differ, review and port deliberately; do not reset or force
application. The vendor/lineage prerequisites are also in RoadSTR's catalog.

Verify without touching worktrees or real indexes:

```sh
python3 device/xiaomi/annibale/non-device-specific-changes/verify.py
```

The verifier needs each recorded base object locally. It does not fetch, reset,
commit, or apply changes to the real checkout. It checks the exact final trees
using temporary indexes; this is patch reproducibility, not another ROM build.

## Validation and limits

The combined Annibale stack built and booted with SELinux Enforcing. A real
primary-slot Qualcomm type 2 callback produced `NR_ADVANCED`; the status bar
visibly showed 5G+. All five focused modem-hint tests passed after flashing.
Kernel-header sandbox validation retained 993 cleaned UAPI headers.

Evidence is recorded in the source checkout under:

- `documents/annibale/nr-ca-flash-20261020/testing.md`
- `documents/annibale/nr-ca-stock-20261020/implementation.md`
- `documents/annibale/build-progress.md`

The full release ZIP predates the final carrier mapping fix; the tested product
image includes it. Rebuild the release before distributing the complete fix.

This is a required-change catalog, not a copy of every local RoadSTR patch.
RoadSTR-specific media, Motorola services, UDFPS, and Dolby adaptations are not
Annibale prerequisites. Optional host Go heap controls already recorded in
RoadSTR's `evo-cnb/build-soong` series are not required for runtime behavior.
Host output placement and build logs also stay outside this source patch set.

Rollback shared patches in reverse order together with dependent device
configuration. Do not remove the hint API while the device bridge uses it,
or unpacked-header support while the device selects that provider.
