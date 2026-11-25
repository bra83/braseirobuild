"""Pinned PT-BR NLI assets; run real ONNX inference and Java tokenizer parity before packaging."""
import hashlib,json,pathlib,subprocess,tempfile,urllib.request
import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer
root=pathlib.Path("app/src/main/assets/nli");root.mkdir(parents=True,exist_ok=True)
base="https://huggingface.co/rchuluc/bertimbau-large-inferbr-nli-onnx/resolve/6c9d7e7b6bfa8a27da4375dec4e575808f36f318/"
for source,target in [("onnx/model_quantized.onnx","model.onnx"),("vocab.txt","vocab.txt"),("tokenizer.json","tokenizer.json"),("config.json","config.json"),("README.md","attribution.txt")]:
    urllib.request.urlretrieve(base+source,root/target)
model=root/"model.onnx"
assert model.stat().st_size==336269909
with model.open("rb") as f: assert hashlib.file_digest(f,"sha256").hexdigest()=="fc4f10b69a842b6a314195822d370e4c9380776051be38978e2ec45f0425d50b"
assert json.loads((root/"config.json").read_text())["id2label"]=={"0":"contradiction","1":"entailment","2":"neutral"}
tok=Tokenizer.from_file(str(root/"tokenizer.json"))
fixtures=[
 ("Eu trabalho em uma farmácia.","Eu não trabalho em uma farmácia.",0),
 ("Minha mãe está viva.","Minha mãe morreu.",0),
 ("Tenho dois irmãos.","Não tenho irmãos.",0),
 ("Meu pai mora em Ubá.","Meu pai mora em Minas Gerais.",1),
 ("Fui demitido ontem.","Perdi meu emprego.",1),
 ("Estou casado com Ana.","Ana é minha esposa.",1),
 ("Minha mãe mora longe.","Meu irmão estuda à noite.",2),
 ("Estou cansado hoje.","Tenho uma bicicleta.",2),
 ("Tenho medo de viajar.","Minha irmã gosta de cozinhar.",2),
 ("Em 2020 eu morava em Ubá.","Em 2025 eu morava em Juiz de Fora.",2),
 ("Meu irmão toma remédio.","Eu tomo remédio.",2),
 ("Se eu perder o emprego, vou me mudar.","Perdi o emprego.",2)]
# Java tokenizer parity on original, non-truncated pairs with accented PT-BR and punctuation.
with tempfile.TemporaryDirectory() as d:
    p=pathlib.Path(d);runner=p/"NliParity.java"
    runner.write_text('''package br.com.tutorclinico;
import java.nio.file.*;import java.util.*;
public class NliParity {public static void main(String[] a)throws Exception{
Map<String,Integer> v=new HashMap<>();int n=0;for(String s:Files.readAllLines(Path.of(a[0])))v.put(s,n++);
long[][] x=NliWordPiece.pair(a[1],a[2],v);
for(long[] r:x){System.out.println(Arrays.toString(r));}
}}''')
    subprocess.run(["javac","-d",d,"app/src/main/java/br/com/tutorclinico/NliWordPiece.java",str(runner)],check=True)
    opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
    s=ort.InferenceSession(str(model),sess_options=opts,providers=["CPUExecutionProvider"])
    counts=[0,0,0];correct=[0,0,0];results=[]
    for a,b,label in fixtures:
        t=tok.encode(a,b)
        raw=subprocess.check_output(["java","-cp",d,"br.com.tutorclinico.NliParity",str(root/"vocab.txt"),a,b],text=True)
        ids,mask,types=map(json.loads,raw.splitlines())
        assert ids==t.ids and mask==t.attention_mask and types==t.type_ids,"Java tokenization mismatch"
        inputs={"input_ids":ids,"attention_mask":mask,"token_type_ids":types}
        logits=s.run(None,{i.name:np.array([inputs[i.name]],dtype=np.int64) for i in s.get_inputs()})[0]
        assert logits.shape==(1,3) and np.isfinite(logits).all()
        pred=int(logits.argmax());counts[label]+=1;correct[label]+=int(pred==label)
        results.append({"expected":label,"predicted":pred,"logits":logits[0].tolist()})
        print("PT-BR NLI",label,pred,flush=True)
    print("NLI smoke test",correct,counts,flush=True)
    # Smoke gate only; not clinical validation. Fail if a class is never correctly recognized.
    assert all(n>=1 for n in correct) and sum(correct)>=8,"NLI candidate did not pass experimental smoke gate"
(root/"gate.txt").write_text(json.dumps({"model_sha256":"fc4f10b69a842b6a314195822d370e4c9380776051be38978e2ec45f0425d50b","correct":correct,"counts":counts,"fixtures":results,"clinical_validation":False},indent=2))
(root/"tokenizer.json").unlink();(root/"config.json").unlink()
print("PASS NLI ONNX inference and Android Java tokenization parity; experimental only")
