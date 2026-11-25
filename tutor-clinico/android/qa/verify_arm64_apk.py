"""APK publication gate: model assets, JNI architecture, unique entries, ZIP integrity."""
import sys,zipfile,collections,hashlib,json
with zipfile.ZipFile(sys.argv[1]) as z:
    names=z.namelist()
    assert z.testzip() is None, "APK ZIP integrity failed"
    duplicates=[n for n,c in collections.Counter(names).items() if c>1]
    assert not duplicates,duplicates
    libs=[n for n in names if n.startswith("lib/") and n.endswith(".so")]
    assert libs and all(n.startswith("lib/arm64-v8a/") for n in libs),libs
    for name in ["libsherpa-onnx-jni.so","libonnxruntime.so","libonnxruntime4j_jni.so"]:
        assert "lib/arm64-v8a/"+name in libs, f"Missing {name}"
    for name in ["tagarela/encoder.int8.onnx","tagarela/decoder.int8.onnx","tagarela/joiner.int8.onnx","tagarela/tokens.txt","semantic/model.onnx","semantic/vocab.txt","semantic/dense.bin","whisper/ggml-base.bin","whisper/ggml-small-q5_1.bin","model-spk/final.ext.raw"]:
        info=z.getinfo("assets/"+name)
        assert info.file_size>0,name
        if name.endswith(".onnx") or name=="tagarela/tokens.txt":
            assert info.compress_type==zipfile.ZIP_STORED, f"Asset must support openFd: {name}"
    nli=z.getinfo("assets/nli/model.onnx")
    assert nli.file_size==336269909 and nli.compress_type==zipfile.ZIP_STORED
    with z.open(nli) as f:
        digest=hashlib.sha256()
        for block in iter(lambda:f.read(1024*1024),b""):digest.update(block)
    assert digest.hexdigest()=="fc4f10b69a842b6a314195822d370e4c9380776051be38978e2ec45f0425d50b"
    assert z.getinfo("assets/nli/vocab.txt").file_size>0
    gate=json.loads(z.read("assets/nli/gate.txt"))
    assert gate["model_sha256"]==digest.hexdigest() and sum(gate["correct"])>=8 and all(x>=1 for x in gate["correct"]) and gate["clinical_validation"] is False
    assert z.getinfo("assets/nli/attribution.txt").file_size>0
    print("PASS APK integrity, ARM64 libraries and all required offline models",flush=True)
