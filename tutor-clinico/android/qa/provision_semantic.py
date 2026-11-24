"""Download pinned neural artifacts; verify actual inference on Portuguese fixtures."""
import hashlib, pathlib, urllib.request, tempfile, gc
from onnxruntime.quantization import quantize_dynamic, QuantType
import numpy as np
import onnxruntime as ort
from safetensors.numpy import load_file
from tokenizers import Tokenizer

root=pathlib.Path("app/src/main/assets/semantic")
root.mkdir(parents=True,exist_ok=True)
url="https://huggingface.co/sentence-transformers/distiluse-base-multilingual-cased-v2/resolve/bfe45d0732ca50787611c0fe107ba278c7f3f889/"
for src,dst in [("vocab.txt","vocab.txt"),("tokenizer.json","tokenizer.json"),("2_Dense/model.safetensors","dense.safetensors")]:
    urllib.request.urlretrieve(url+src,root/dst)

dense=load_file(str(root/"dense.safetensors"))
w=dense["linear.weight"]; b=dense["linear.bias"]
assert w.shape==(512,768) and b.shape==(512,)
with (root/"dense.bin").open("wb") as f:
    f.write(w.astype("<f4").tobytes()); f.write(b.astype("<f4").tobytes())
tok=Tokenizer.from_file(str(root/"tokenizer.json"))
tok.enable_truncation(max_length=128)
def encode(session,text):
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
# Compare the original full-precision graph and a portable unsigned-int8 graph.
# Keep the original +0.05 margin; never ship an unqualified model.
fixtures += [
 ("Evito encontrar pessoas e fico em casa.", "Prefiro me isolar e não sair com ninguém.", "A cidade tem um mercado municipal."),
 ("Tenho medo de perder meu trabalho.", "Receio ser despedido da empresa.", "A camisa é de algodão."),
 ("Sinto falta do meu pai.", "Tenho saudade do meu pai.", "O livro está na estante.")
]
hard_fixtures=[
 ("Tenho receio de ser demitido.","A possibilidade de perder meu emprego me preocupa.","Meu trabalho fica perto da minha casa."),
 ("Evito visitar minha família porque sempre brigamos.","Fico longe dos parentes para não entrar em discussões.","Visitei minha família no domingo e almoçamos juntos."),
 ("Sinto saudade do meu pai que morreu.","A falta que meu pai faz é muito grande.","Meu pai comprou um carro novo."),
 ("Não consigo dormir quando estou preocupado.","A preocupação me deixa acordado à noite.","Gosto de ler antes de dormir."),
 ("Tenho medo de ser rejeitado pelos amigos.","Receio que meus colegas não me aceitem.","Meus amigos foram ao cinema."),
 ("Fico irritado quando minha mãe tenta controlar minhas decisões.","Me incomoda quando minha mãe quer decidir tudo por mim.","Minha mãe decidiu viajar para o interior."),
 ("Eu me cobro para não cometer erros.","Tenho dificuldade de aceitar minhas próprias falhas.","Corrigi um erro no endereço do cadastro."),
 ("Sinto que preciso agradar a todos.","Faço de tudo para que as pessoas gostem de mim.","A festa agradou a todos os convidados.")
]
def evaluate(path,label):
    opts=ort.SessionOptions()
    opts.intra_op_num_threads=2
    opts.inter_op_num_threads=1
    session=ort.InferenceSession(str(path),sess_options=opts,providers=["CPUExecutionProvider"])
    scores=[]
    for a,p,n in fixtures:
        v=encode(session,a)
        pos=float(v@encode(session,p));neg=float(v@encode(session,n))
        scores.append((pos,neg))
        print(f"{label} Portuguese retrieval: positive={pos:.3f}, unrelated={neg:.3f}, margin={pos-neg:.3f}",flush=True)
    hard=[]
    for a,p,n in hard_fixtures:
        v=encode(session,a);pos=float(v@encode(session,p));neg=float(v@encode(session,n))
        hard.append((pos,neg));print(f"{label} hard negative: positive={pos:.3f}, negative={neg:.3f}, margin={pos-neg:.3f}",flush=True)
    print(f"{label} hard ranking correct: {sum(p>n for p,n in hard)}/{len(hard)}; recurrence threshold positive coverage: {sum(p>=0.45 for p,n in hard)}/{len(hard)}; hard negatives above threshold: {sum(n>=0.45 for p,n in hard)}/{len(hard)} (exploratory, not clinical validation)",flush=True)
    del session
    gc.collect()
    return np.array(scores)
with tempfile.TemporaryDirectory() as tmp:
    original=pathlib.Path(tmp)/"original.onnx"
    urllib.request.urlretrieve(url+"onnx/model.onnx",original)
    assert hashlib.sha256(original.read_bytes()).hexdigest()=="698b5efe2a2ec26e6ec492906c0efa168659994e9838a131f88994fb6f637520"
    baseline=evaluate(original,"FP32")
    candidate=root/"model.onnx"
    quantize_dynamic(str(original),str(candidate),weight_type=QuantType.QUInt8,per_channel=True)
    scores=evaluate(candidate,"U8U8")
    print("Quantized SHA256:",hashlib.sha256(candidate.read_bytes()).hexdigest(),flush=True)
    assert np.all(baseline[:,0]-baseline[:,1]>0.05), "FP32 baseline did not pass semantic quality gate"
    assert np.all(scores[:,0]-scores[:,1]>0.05), "U8U8 did not pass semantic quality gate"
    assert np.max(np.abs(scores-baseline))<=0.08, "Quantized score drift exceeds gate"
    assert candidate.stat().st_size<200_000_000,"Mobile model size exceeds gate"
(root/"tokenizer.json").unlink();(root/"dense.safetensors").unlink()
print("PASS original and unsigned INT8 inference; six Portuguese ranking fixtures; score parity")
