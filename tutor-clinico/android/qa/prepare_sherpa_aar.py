"""Remove redundant shared ORT from the upstream *static-link* Sherpa AAR only
after verifying no packaged native library depends on it. Keep the official ORT
Android dependency for the Java semantic engine. Fail closed if not static."""
import pathlib, re, subprocess, sys, tempfile, zipfile

aar=pathlib.Path(sys.argv[1])
out=aar.with_suffix(".clean.aar")
with tempfile.TemporaryDirectory() as tmp, zipfile.ZipFile(aar) as src:
    libs=[n for n in src.namelist() if n.startswith("jni/") and n.endswith(".so")]
    duplicates=[n for n in libs if n.endswith("/libonnxruntime.so")]
    assert libs and any(n.endswith("/libsherpa-onnx-jni.so") for n in libs), "Sherpa JNI missing"
    for i,name in enumerate(libs):
        if name in duplicates:
            continue
        f=pathlib.Path(tmp)/f"{i}.so"
        f.write_bytes(src.read(name))
        dynamic=subprocess.check_output(["readelf","-d",str(f)],text=True)
        needed=re.findall(r"Shared library: \[([^\]]+)\]",dynamic)
        print(name, "NEEDED", needed, flush=True)
        assert not any(x.startswith("libonnxruntime.so") for x in needed), f"Shared ORT dependency in {name}; cannot remove safely"
    with zipfile.ZipFile(out,"w") as dst:
        for info in src.infolist():
            if info.filename not in duplicates:
                dst.writestr(info,src.read(info.filename))
out.replace(aar)
print("PASS Sherpa static-link native dependency check; removed redundant packaged ORT:", duplicates,flush=True)
