"""Provision pinned Pyannote ONNX and run a host inference smoke test on synthetic PT-BR."""
from pathlib import Path
import hashlib, json, subprocess, tempfile, urllib.request, wave
import numpy as np
import onnxruntime as ort

ROOT = Path("app/src/main/assets/overlap")
ROOT.mkdir(parents=True, exist_ok=True)
BASE = "https://huggingface.co/csukuangfj/sherpa-onnx-pyannote-segmentation-3-0/resolve/main/"
SHA = "220ad67ca923bef2fa91f2390c786097bf305bceb5e261d4af67b38e938e1079"

def download(name, path):
    subprocess.run(["curl", "--fail", "--location", "--retry", "3", "--max-time", "180", BASE+name, "-o", str(path)], check=True)

model = ROOT/"model.onnx"
download("model.onnx", model)
assert hashlib.sha256(model.read_bytes()).hexdigest() == SHA
download("LICENSE", ROOT/"license.txt")
assert "MIT License" in (ROOT/"license.txt").read_text() and "CNRS" in (ROOT/"license.txt").read_text()
(ROOT/"attribution.txt").write_text(
    "Pyannote Segmentation 3.0, CNRS (MIT). ONNX export distributed by sherpa-onnx.\n"
    "Source: https://huggingface.co/pyannote/segmentation-3.0\n"
    "Conversion: https://huggingface.co/csukuangfj/sherpa-onnx-pyannote-segmentation-3-0\n"
    "Experimental OSD only; not clinical validation or speaker separation.\n",
    encoding="utf-8")
opts=ort.SessionOptions()
opts.intra_op_num_threads=2
opts.inter_op_num_threads=1
session=ort.InferenceSession(str(model),sess_options=opts,providers=["CPUExecutionProvider"])
assert [x.name for x in session.get_inputs()] == ["x"]
assert [x.name for x in session.get_outputs()] == ["y"]
meta=session.get_modelmeta().custom_metadata_map
for key,expected in {"num_classes":"7","sample_rate":"16000","window_size":"160000","receptive_field_shift":"270","receptive_field_size":"991","powerset_max_classes":"2"}.items():
    assert meta[key]==expected,(key,meta.get(key))

def speech(voice, text):
    with tempfile.TemporaryDirectory() as d:
        p=Path(d)/"synthetic.wav"
        subprocess.run(["espeak-ng","-v",voice,"-s","145","-w",str(p),text],check=True)
        with wave.open(str(p),"rb") as w:
            assert w.getnchannels()==1 and w.getsampwidth()==2
            sr=w.getframerate()
            pcm=np.frombuffer(w.readframes(w.getnframes()),dtype="<i2").astype(np.float32)/32768
        resampled=np.interp(np.arange(round(len(pcm)*16000/sr))*sr/16000,np.arange(len(pcm)),pcm)
        out=np.zeros(160000,np.float32)
        count=min(len(out)-16000,len(resampled))
        out[16000:16000+count]=resampled[:count]
        return out

def predict(x):
    y=session.run(["y"],{"x":x.reshape(1,1,160000)})[0]
    assert y.shape==(1,589,7) and np.isfinite(y).all()
    p=np.exp(y-y.max(axis=-1,keepdims=True))
    p=p/p.sum(axis=-1,keepdims=True)
    overlap=p[0,:,4:].sum(axis=-1)
    return {"peak":float(overlap.max()),"frames_at_065":int((overlap>=0.65).sum()),"mean":float(overlap.mean())},y

a=speech("pt-br+m3","Hoje eu trouxe um texto fictício para testar um modelo de áudio. Não é uma consulta clínica.")
b=speech("pt-br+f3","Esta é uma segunda voz artificial falando junto para testar a sobreposição de vozes no aplicativo.")
results={}
results["silence"],ys=predict(np.zeros(160000,np.float32))
results["single"],ya=predict(a)
results["mixed"],ym=predict(np.clip(0.55*a+0.55*b,-1,1))
assert not np.allclose(ys,ya) and not np.allclose(ya,ym),"Neural output invariant to input"
gate={"sha256":SHA,"contract":"1x1x160000 -> 1x589x7","fixtures":results,"clinical_validation":False,"quality_certified":False}
(ROOT/"gate.txt").write_text(json.dumps(gate,indent=2),encoding="utf-8")
print("PASS Pyannote model SHA, seven-class shape and actual host inference; synthetic metrics:",json.dumps(results),flush=True)
