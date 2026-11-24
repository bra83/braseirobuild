"""Prepare upstream Sherpa static-link AAR for the ECO ARM64 Android build.
Upstream v1.13.3 includes shared ORT dependencies in x86 despite the static
label. ARM64 JNI is statically linked, as confirmed by readelf in CI.
Keep the independent official ORT Android library used by the semantic engine.
Never silently remove a needed library or resolve duplicate binaries via pickFirst.
"""
import pathlib, re, subprocess, sys, tempfile, zipfile

aar=pathlib.Path(sys.argv[1])
out=aar.with_suffix(".clean.aar")
prefix="jni/arm64-v8a/"
with tempfile.TemporaryDirectory() as tmp, zipfile.ZipFile(aar) as src:
    libs=[n for n in src.namelist() if n.startswith(prefix) and n.endswith(".so")]
    duplicates=[n for n in libs if n.endswith("/libonnxruntime.so")]
    keep=[n for n in libs if n not in duplicates]
    assert prefix+"libsherpa-onnx-jni.so" in keep, "ARM64 Sherpa JNI missing"
    for i,name in enumerate(keep):
        f=pathlib.Path(tmp)/f"{i}.so"
        f.write_bytes(src.read(name))
        header=subprocess.check_output(["readelf","-h",str(f)],text=True)
        assert "AArch64" in header, f"Wrong ABI: {name}"
        dynamic=subprocess.check_output(["readelf","-d",str(f)],text=True)
        needed=re.findall(r"Shared library: \[([^\]]+)\]",dynamic)
        print(name,"NEEDED",needed,flush=True)
        assert not any(x.startswith("libonnxruntime.so") for x in needed), f"Shared ORT dependency in {name}; cannot remove safely"
    with zipfile.ZipFile(out,"w") as dst:
        for info in src.infolist():
            name=info.filename
            if name.startswith("jni/") and not name.startswith(prefix):
                continue
            if name in duplicates:
                continue
            dst.writestr(info,src.read(name))
with zipfile.ZipFile(out) as checked:
    assert checked.testzip() is None
    native=[n for n in checked.namelist() if n.startswith("jni/") and n.endswith(".so")]
    assert sorted(native)==sorted(keep)
out.replace(aar)
print("PASS ARM64 Sherpa ELF and native dependency checks; redundant ORT removed:",duplicates,flush=True)
