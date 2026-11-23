#!/usr/bin/env python3
"""Report APK payload and ZIP layout differences without modifying either APK."""

import hashlib
import json
import struct
import sys
from pathlib import Path
from zipfile import ZipFile


def digest(data):
    return hashlib.sha256(data).hexdigest()


def entries(path):
    result = {}
    with ZipFile(path) as apk, open(path, "rb") as raw:
        for info in apk.infolist():
            raw.seek(info.header_offset)
            header = raw.read(30)
            name_size, extra_size = struct.unpack_from("<HH", header, 26)
            header += raw.read(name_size + extra_size)
            result[info.filename] = {
                "content": digest(apk.read(info)),
                "compressed_content": digest(raw.read(info.compress_size)),
                "local_header": digest(header),
                "local_extra": header[30 + name_size :].hex(),
                "offset": info.header_offset,
                "compression": info.compress_type,
                "flags": info.flag_bits,
                "timestamp": list(info.date_time),
                "central_extra": info.extra.hex(),
                "external_attr": info.external_attr,
            }
    return result


def compare(reference, candidate):
    left, right = entries(reference), entries(candidate)
    differences = []
    counts = {}
    for name in sorted(left.keys() | right.keys()):
        if name not in left or name not in right:
            changes = {"presence": [name in left, name in right]}
        else:
            changes = {
                key: [left[name][key], right[name][key]]
                for key in left[name]
                if left[name][key] != right[name][key]
            }
        if changes:
            differences.append({"entry": name, "changes": changes})
            for key in changes:
                counts[key] = counts.get(key, 0) + 1
    return {
        "reference": str(reference),
        "candidate": str(candidate),
        "entry_order_matches": list(left) == list(right),
        "difference_counts": counts,
        "differences": differences,
    }


if __name__ == "__main__":
    report = compare(sys.argv[1], sys.argv[2])
    Path(sys.argv[3]).write_text(json.dumps(report, indent=2) + "\n")
    print(f"APK comparison: {json.dumps(report['difference_counts'], sort_keys=True)}")
    for difference in report["differences"][:30]:
        print(f"  {difference['entry']}: {', '.join(difference['changes'])}")
