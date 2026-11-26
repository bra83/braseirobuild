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
    for asset,sha in [("rerank/model.onnx","6e57207c62deb625b5751a376f32ac66109281b0d199a2aec09f2671e066acf5"),("llm/model.gguf","74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db")]:
        info=z.getinfo("assets/"+asset)
        assert info.compress_type==zipfile.ZIP_STORED
        with z.open(info) as f:
            d=hashlib.sha256()
            for block in iter(lambda:f.read(1024*1024),b""):d.update(block)
        assert d.hexdigest()==sha,asset
    rg=json.loads(z.read("assets/rerank/gate.txt"))
    assert rg["ranking_correct"]>=5 and rg["clinical_validation"] is False
    assert rg["sha256"]=="6e57207c62deb625b5751a376f32ac66109281b0d199a2aec09f2671e066acf5"
    lg=json.loads(z.read("assets/llm/gate.txt"))
    assert lg["model_sha256"]=="74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db" and lg["clinical_validation"] is False
    assert z.getinfo("assets/rerank/vocab.txt").file_size>0
    for name in ["assets/rerank/attribution.txt","assets/llm/attribution.txt","assets/llm/llama-license.txt"]:
        assert z.getinfo(name).file_size>0
    osd=z.getinfo("assets/overlap/model.onnx")
    assert osd.compress_type==zipfile.ZIP_STORED
    assert hashlib.sha256(z.read(osd)).hexdigest()=="220ad67ca923bef2fa91f2390c786097bf305bceb5e261d4af67b38e938e1079"
    osd_gate=json.loads(z.read("assets/overlap/gate.txt"))
    assert osd_gate["sha256"]=="220ad67ca923bef2fa91f2390c786097bf305bceb5e261d4af67b38e938e1079" and osd_gate["clinical_validation"] is False
    assert osd_gate["contract"]=="1x1x160000 -> 1x589x7"
    assert osd_gate["synthetic_detection_gate"] is True
    assert osd_gate["fixtures"]["mixed"]["frames_at_065"]>=100
    assert osd_gate["fixtures"]["single"]["frames_at_065"]<=5
    assert osd_gate["fixtures"]["silence"]["frames_at_065"]==0
    assert b"MIT License" in z.read("assets/overlap/license.txt")
    assert z.getinfo("assets/overlap/attribution.txt").file_size>0
    sep=z.getinfo("assets/separation/model.onnx")
    assert sep.compress_type==zipfile.ZIP_STORED
    assert hashlib.sha256(z.read(sep)).hexdigest()=="3fa85c7bb2537ced4bb7acb2ea91255c7d6b222fd3d0fae5088e8a3999a963fd"
    sg=json.loads(z.read("assets/separation/gate.txt"))
    assert sg["sha256"]=="3fa85c7bb2537ced4bb7acb2ea91255c7d6b222fd3d0fae5088e8a3999a963fd"
    assert sg["synthetic_separation_gate"] is True and sg["clinical_validation"] is False
    assert len(sg["fixtures"])>=2 and all(min(f["improvement_db"])>0 and sum(f["improvement_db"])/2>=2 for f in sg["fixtures"])
    assert sg["distinct_asr_pairs"]>=1
    assert sg["silence_guard"] is True and sg["irregular_length_gate"] is True
    assert b"Attribution-ShareAlike 4.0" in z.read("assets/separation/license.txt")
    assert z.getinfo("assets/separation/attribution.txt").file_size>0
    exe="lib/arm64-v8a/libeco_generate.so"
    assert exe in libs
    with z.open(exe) as f: elf=f.read(64)
    assert elf[:4]==b"\x7fELF" and elf[4]==2 and elf[5]==1 and int.from_bytes(elf[18:20],"little")==183
    print("PASS APK integrity, ARM64 libraries and all required offline models",flush=True)
