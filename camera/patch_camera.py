#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Patch only the pinned annibale APK; fail on a changed input or patch context."""
import argparse
import hashlib
import re
import subprocess
import tempfile
import struct
import os
import zipfile
from pathlib import Path

STOCK_SHA256 = '852de06604764d15b591e9c31863370f903545e14765d549eba4b6a953bf6b6f'

def add_library(data):
    """Append one uses-library node without recompiling the stock resource table."""
    chunks = []
    offset = 8
    while offset < len(data):
        kind, header, size = struct.unpack_from('<HHI', data, offset)
        chunks.append((kind, data[offset:offset + size]))
        offset += size
    assert offset == len(data)
    strings = None
    mapping = None
    library_index = None
    for i, (kind, chunk) in enumerate(chunks):
        if kind == 1:
            header = struct.unpack_from('<H', chunk, 2)[0]
            count, styles, flags, start, style_start = struct.unpack_from('<IIIII', chunk, 8)
            offsets = list(struct.unpack_from('<' + 'I' * count, chunk, header))
            def length(buf, pos, wide=False):
                if wide:
                    n = struct.unpack_from('<H', buf, pos)[0]
                    return ((n & 0x7fff) << 16 | struct.unpack_from('<H', buf, pos + 2)[0], pos + 4) if n & 0x8000 else (n, pos + 2)
                n = buf[pos]
                return ((n & 0x7f) << 8 | buf[pos + 1], pos + 2) if n & 0x80 else (n, pos + 1)
            strings = []
            for item in offsets:
                n, ptr = length(chunk, start + item, not bool(flags & 0x100))
                if flags & 0x100:
                    n, ptr = length(chunk, ptr)
                    strings.append(chunk[ptr:ptr + n].decode('utf-8'))
                else:
                    strings.append(chunk[ptr:ptr + 2*n].decode('utf-16le'))
            value = 'annibale-miui-camera-framework'
            assert value not in strings
            library_index = count
            raw = chunk[start:style_start or len(chunk)]
            offsets.append(len(raw))
            if flags & 0x100:
                raw += bytes([len(value), len(value)]) + value.encode() + b'\0'
            else:
                raw += struct.pack('<H', len(value)) + value.encode('utf-16le') + b'\0\0'
            raw += b'\0' * (-len(raw) % 4)
            style_offsets = chunk[header + count*4:start]
            new_start = header + (count + 1)*4 + len(style_offsets)
            style_data = chunk[style_start:] if style_start else b''
            new_size = new_start + len(raw) + len(style_data)
            rebuilt = bytearray(chunk[:header])
            struct.pack_into('<I', rebuilt, 4, new_size)
            struct.pack_into('<IIIII', rebuilt, 8, count + 1, styles, flags & ~1,
                             new_start, new_start + len(raw) if style_start else 0)
            rebuilt += struct.pack('<' + 'I' * len(offsets), *offsets) + style_offsets + raw + style_data
            chunks[i] = (kind, bytes(rebuilt))
        elif kind == 0x180:
            mapping = list(struct.unpack_from('<' + 'I' * ((len(chunk)-8)//4), chunk, 8))
    assert strings is not None and mapping is not None
    ns = strings.index('http://schemas.android.com/apk/res/android')
    tag = strings.index('uses-library')
    name = mapping.index(0x01010003)
    required = mapping.index(0x0101028e)
    def attr(key, raw, typ, value):
        return struct.pack('<IIIHBBI', ns, key, raw, 8, 0, typ, value)
    node = struct.pack('<HHIII', 0x102, 16, 76, 0, 0xffffffff)
    node += struct.pack('<IIHHHHHH', 0xffffffff, tag, 20, 20, 2, 0, 0, 0)
    node += attr(name, library_index, 3, library_index) + attr(required, 0xffffffff, 0x12, 0xffffffff)
    end = struct.pack('<HHIIIII', 0x103, 16, 24, 0, 0xffffffff, 0xffffffff, tag)
    inserted = False
    output = bytearray()
    for kind, chunk in chunks:
        if kind == 0x103 and strings[struct.unpack_from('<I', chunk, 20)[0]] == 'application':
            assert not inserted
            output += node + end
            inserted = True
        output += chunk
    assert inserted
    return struct.pack('<HHI', 3, 8, len(output) + 8) + output

def add_icon_overlay(data):
    """Allow a same-certificate overlay of only the pinned launcher icon.

    Append one overlayable chunk. Preserve resource IDs, values and all original
    chunks; only the enclosing table/package sizes change.
    """
    assert struct.unpack_from('<H', data)[0] == 2
    name = 'AnnibaleCameraIcon'.encode('utf-16le').ljust(512, b'\0')
    policy = struct.pack('<HHIII', 0x205, 16, 20, 0x10, 1)
    policy += struct.pack('<I', 0x7f110000)  # mipmap/eji in the pinned stock APK
    extra = struct.pack('<HHI', 0x204, 1032, 1032 + len(policy))
    extra += name + bytes(512) + policy
    header = struct.unpack_from('<H', data, 2)[0]
    offset = header
    output = bytearray(data[:header])
    count = 0
    while offset < len(data):
        kind, _, size = struct.unpack_from('<HHI', data, offset)
        chunk = bytearray(data[offset:offset + size])
        if kind == 0x200:
            assert struct.unpack_from('<I', chunk, 8)[0] == 0x7f
            assert 'AnnibaleCameraIcon'.encode('utf-16le') not in chunk
            struct.pack_into('<I', chunk, 4, size + len(extra))
            chunk += extra
            count += 1
        output += chunk
        offset += size
    assert offset == len(data) and count == 1
    struct.pack_into('<I', output, 4, len(output))
    return bytes(output)

def patch(source, output, repo):
    if hashlib.sha256(source.read_bytes()).hexdigest() != STOCK_SHA256:
        raise ValueError('Unknown MiuiCamera APK: review patches before accepting another version')
    tool = repo / 'prebuilts/extract-tools/common/apktool/apktool.jar'
    with tempfile.TemporaryDirectory(prefix='annibale-camera-') as temp:
        work = Path(temp) / 'decoded'
        subprocess.run(['java', '-Xmx4g', '-jar', str(tool), 'd', '-r',
                        '-j', '4', '-o', str(work), str(source)], check=True)
        manifest = work / 'AndroidManifest.xml'
        manifest.write_bytes(add_library(manifest.read_bytes()))
        def method(file, signature, body):
            path = work / file
            text = path.read_text()
            pattern = r'(?ms)^\.method ' + re.escape(signature) + r'\n.*?^\.end method'
            text, count = re.subn(pattern, '.method ' + signature + '\n' + body + '\n.end method', text)
            assert count == 1, (file, signature, count)
            path.write_text(text)
        # Select the APK's non-MIUI consent implementation; runtime permission handling remains.
        method('smali_classes3/Df/b.smali', 'public static a()LDf/b$c;',
               '    .locals 1\n    sget-object v0, LDf/b$c;->c:LDf/b$c;\n    return-object v0')
        # Do not let reflection on the compatibility class advertise an OZO implementation.
        method('smali/G1/a$a.smali', 'public final k()Ljava/lang/Integer;',
               '    .locals 1\n    const/4 v0, 0x0\n    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;\n    move-result-object v0\n    return-object v0')
        method('smali/C6/a.smali', 'public static e()Z',
               '    .locals 1\n    const/4 v0, 0x0\n    return v0')
        # Generic ROMs omit registered Xiaomi routing and sensor tags from advertised keys.
        # Include only filename, QCFA and zoom capability keys; enumerating all keys enables
        # unsupported features and crashes this device's camera provider.
        capabilities = work / 'smali/s8/e.smali'
        text = capabilities.read_text()
        context = '    :cond_2\n    new-instance p2, Ljava/util/HashSet;'
        extra = """    :cond_2
    invoke-static {p1}, Lch/b;->c(Landroid/hardware/camera2/CameraCharacteristics;)Landroid/hardware/camera2/impl/CameraMetadataNative;
    move-result-object p2
    const-class v0, Landroid/hardware/camera2/CaptureRequest$Key;
    invoke-virtual {p2, v0}, Landroid/hardware/camera2/impl/CameraMetadataNative;->getAllVendorKeys(Ljava/lang/Class;)Ljava/util/ArrayList;
    move-result-object p2
    if-eqz p2, :annibale_keys_done
    invoke-interface {p2}, Ljava/util/List;->iterator()Ljava/util/Iterator;
    move-result-object p2
    :annibale_keys_next
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z
    move-result v0
    if-eqz v0, :annibale_keys_done
    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Landroid/hardware/camera2/CaptureRequest$Key;
    invoke-virtual {v0}, Landroid/hardware/camera2/CaptureRequest$Key;->getName()Ljava/lang/String;
    move-result-object v0
    const-string v1, "xiaomi.snapshot.imageName"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-nez v1, :annibale_keys_add
    const-string v1, "org.codeaurora.qcamera3.quadra_cfa.is_qcfa_sensor"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-nez v1, :annibale_keys_add
    const-string v1, "xiaomi.capabilities.isZoomRatioSupported"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :annibale_keys_next
    :annibale_keys_add
    iget-object v1, p0, Ls8/e;->f:Ljava/util/HashSet;
    invoke-virtual {v1, v0}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z
    goto :annibale_keys_next
    :annibale_keys_done
    new-instance p2, Ljava/util/HashSet;"""
        assert text.count(context) == 1, 'Vendor-key enumeration context changed'
        capabilities.write_text(text.replace(context, extra, 1))
        camera = work / 'smali/com/android/camera/Camera.smali'
        text = camera.read_text()
        pattern = r'(?ms)(^\.method public final Vd\(Z\)V\n)(.*?)(^\.end method)'
        matches = list(re.finditer(pattern, text)); assert len(matches) == 1
        block = matches[0].group(2)
        block, count = re.subn(
            r'invoke-virtual \{([^}]+)\}, Landroid/content/Context;->startActivity\(Landroid/content/Intent;\)V',
            r'invoke-static {\1}, Lorg/lineageos/annibale/camera/GalleryCompat;->startActivity(Landroid/content/Context;Landroid/content/Intent;)V', block)
        assert count >= 1, 'Gallery handoff context changed'
        text = text[:matches[0].start(2)] + block + text[matches[0].end(2):]
        camera.write_text(text)
        print('Gallery call sites adapted:', count, flush=True)
        output.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(['java', '-Xmx4g', '-jar', str(tool), 'b', '-j', '4', str(work), '-o', str(Path(temp) / 'patched.apk')], check=True)
        built = Path(temp) / 'patched.apk'
        with zipfile.ZipFile(source) as before, zipfile.ZipFile(built) as after:
            assert 'AndroidManifest.xml' in after.namelist()
            for name in before.namelist():
                if name == 'resources.arsc' or name.startswith(('assets/', 'lib/', 'res/')):
                    assert before.read(name) == after.read(name), name
        # Stable ordering/timestamps make extraction reproducible. Soong signs/aligns later.
        with tempfile.NamedTemporaryFile(dir=output.parent, suffix='.apk', delete=False) as tmp:
            normalized = Path(tmp.name)
        try:
            with zipfile.ZipFile(built) as src, zipfile.ZipFile(normalized, 'w') as dst:
                for entry in sorted(src.infolist(), key=lambda x: x.filename):
                    item = zipfile.ZipInfo(entry.filename, (2009, 1, 1, 0, 0, 0))
                    item.compress_type = entry.compress_type
                    item.external_attr = (0o100644 << 16)
                    payload = src.read(entry)
                    if entry.filename == 'resources.arsc':
                        payload = add_icon_overlay(payload)
                    dst.writestr(item, payload, compresslevel=9)
            normalized.chmod(0o644)
            os.replace(normalized, output)
        finally:
            normalized.unlink(missing_ok=True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[4])
    args = parser.parse_args()
    patch(args.source.resolve(), args.output.resolve(), args.repo.resolve())
