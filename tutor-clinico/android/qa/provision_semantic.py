"""Download pinned neural artifacts; verify actual inference on Portuguese fixtures."""
import hashlib, pathlib, urllib.request
import numpy as np
import onnxruntime as ort
from safetensors.numpy import load_file
from tokenizers import Tokenizer

root=pathlib.Path("app/src/main/assets/semantic")
root.mkdir(parents=True,exist_ok=True)
url="https://huggingface.co/sentence-transformers/distiluse-base-multilingual-cased-v2/resolve/bfe45d0732ca50787611c0fe107ba278c7f3f889/"
for src,dst in [("onnx/model_qint8_arm64.onnx","model.onnx"),("vocab.txt","vocab.txt"),("tokenizer.json","tokenizer.json"),("2_Dense/model.safetensors","dense.safetensors")]:
    urllib.request.urlretrieve(url+src,root/dst)
assert hashlib.sha256((root/"model.onnx").read_bytes()).hexdigest()=="c78c64bb0446ceb7086d2de24c2f4297e7a909eb457b4f68e5fdb6ab19f285c0"
dense=load_file(str(root/"dense.safetensors"))
w=dense["linear.weight"]; b=dense["linear.bias"]
assert w.shape==(512,768) and b.shape==(512,)
with (root/"dense.bin").open("wb") as f:
    f.write(w.astype("<f4").tobytes()); f.write(b.astype("<f4").tobytes())
tok=Tokenizer.from_file(str(root/"tokenizer.json"))
tok.enable_truncation(max_length=128)
session=ort.InferenceSession(str(root/"model.onnx"),providers=["CPUExecutionProvider"])
def encode(text):
    t=tok.encode(text)
    ids=np.array([t.ids],dtype=np.int64)
    mask=np.ones_like(ids)
    y=session.run(None,{"input_ids":ids,"attention_mask":mask})[0]
    assert y.shape==(1,len(t.ids),768)
    v=np.tanh(w@y[0].mean(axis=0)+b)
    assert np.isfinite(v).all() and np.linalg.norm(v)>0
    return v/np.linalg.norm(v)
fixtures=[
 ("Estou sem emprego e procuro uma vaga.", "Fui demitido e busco trabalho.", "A receita leva farinha e leite."),
 ("Minha mãe e eu discutimos muito.", "Tenho brigas frequentes com a minha mãe.", "O ônibus saiu do terminal."),
 ("Não consigo dormir à noite.", "Passo a madrugada acordado.", "Comprei um sapato azul.")
]
for a,p,n in fixtures:
    v=encode(a); pos=float(v@encode(p));neg=float(v@encode(n))
    print(f"Portuguese retrieval: positive={pos:.3f}, unrelated={neg:.3f}")
    assert pos>neg+0.05
(root/"tokenizer.json").unlink();(root/"dense.safetensors").unlink()
print("PASS real quantized ONNX inference and 3 Portuguese ranking fixtures")
