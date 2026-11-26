"""Pinned Conv-TasNet source separation. No patient data; actual two-reference SI-SDR gate."""
from pathlib import Path
import hashlib,json,subprocess,tempfile,wave,itertools
import numpy as np
import onnxruntime as ort
ROOT=Path("app/src/main/assets/separation")
ROOT.mkdir(parents=True,exist_ok=True)
SHA="3fa85c7bb2537ced4bb7acb2ea91255c7d6b222fd3d0fae5088e8a3999a963fd"
BASE="https://huggingface.co/iamdgarcia/ConvTasNet_Libri2Mix_sepnoisy_16k-onnx/resolve/main/convtasnet_sepnoisy_16k.onnx"
model=ROOT/"model.onnx"
subprocess.run(["curl","--fail","--location","--retry","3","--max-time","180",BASE,"-o",str(model)],check=True)
assert hashlib.sha256(model.read_bytes()).hexdigest()==SHA
subprocess.run(["curl","--fail","--location","--retry","3","https://creativecommons.org/licenses/by-sa/4.0/legalcode.txt","-o",str(ROOT/"license.txt")],check=True)
assert "Attribution-ShareAlike 4.0" in (ROOT/"license.txt").read_text()
(ROOT/"attribution.txt").write_text("Conv-TasNet Libri2Mix sepnoisy 16 kHz, Joris Cosentino et al.; ONNX export iamdgarcia. CC BY-SA 4.0. Model unmodified.\nOriginal: https://huggingface.co/JorisCos/ConvTasNet_Libri2Mix_sepnoisy_16k\nExport: https://huggingface.co/iamdgarcia/ConvTasNet_Libri2Mix_sepnoisy_16k-onnx\nLicense: https://creativecommons.org/licenses/by-sa/4.0/\nExperimental, trained on English speech; not clinical validation.\n")
opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
session=ort.InferenceSession(str(model),sess_options=opts,providers=["CPUExecutionProvider"])
assert [i.name for i in session.get_inputs()]==["mixture"]
assert len(session.get_outputs())==1
def speech(voice,text,n):
    with tempfile.TemporaryDirectory() as d:
        f=Path(d)/"speech.wav"
        subprocess.run(["espeak-ng","-v",voice,"-s","155","-w",str(f),text],check=True)
        with wave.open(str(f),"rb") as w:
            assert w.getnchannels()==1 and w.getsampwidth()==2
            sr=w.getframerate();x=np.frombuffer(w.readframes(w.getnframes()),dtype="<i2").astype(np.float32)/32768
        y=np.interp(np.arange(round(len(x)*16000/sr))*sr/16000,np.arange(len(x)),x)
        result=np.zeros(n,np.float32);result[:min(n,len(y))]=y[:n]
        return result/(np.sqrt(np.mean(result**2))+1e-9)*0.08
def sisdr(s,t):
    s=np.asarray(s,dtype=np.float64);t=np.asarray(t,dtype=np.float64)
    s=s-s.mean();t=t-t.mean()
    projected=np.dot(s,t)/(np.dot(t,t)+1e-12)*t
    return float(10*np.log10((np.dot(projected,projected)+1e-12)/(np.dot(s-projected,s-projected)+1e-12)))
def predict(x):
    y=session.run(None,{"mixture":x.astype(np.float32)[None,:]})[0]
    assert y.shape[:2]==(1,2) and y.shape[2]>=len(x) and np.isfinite(y).all()
    return y[0,:,:len(x)]
metrics=[]
for voice_a,voice_b,text_a,text_b,n in [
    ("pt-br+m3","pt-br+f3","Hoje eu trouxe um texto para testar a separação de vozes.","A segunda voz fala junto durante este teste de áudio.",80000),
    ("pt-br+m2","pt-br+f2","O livro está sobre a mesa da sala.","A janela está aberta e o vento sopra.",64000)]:
    a=speech(voice_a,text_a,n);b=speech(voice_b,text_b,n)
    x=a+b;assert np.max(np.abs(x))<1
    y=predict(x)
    baseline=[sisdr(x,a),sisdr(x,b)]
    scores=max(([sisdr(y[i],a),sisdr(y[j],b)] for i,j in [(0,1),(1,0)]),key=sum)
    improvement=[scores[k]-baseline[k] for k in range(2)]
    metrics.append({"samples":n,"mixture_si_sdr":baseline,"separated_si_sdr":scores,"improvement_db":improvement})
    assert not np.allclose(y[0],y[1]),"Duplicated tracks"
    assert min(improvement)>0 and sum(improvement)/2>=2.0,("Separation did not improve both clean references",metrics[-1])
assert np.isfinite(predict(np.zeros(16000,np.float32))).all()
gate={"sha256":SHA,"contract":"1xT -> 1x2xT","fixtures":metrics,"synthetic_separation_gate":True,"clinical_validation":False}
(ROOT/"gate.txt").write_text(json.dumps(gate,indent=2))
print("PASS Conv-TasNet actual two-source inference and SI-SDR improvement:",json.dumps(metrics),flush=True)
