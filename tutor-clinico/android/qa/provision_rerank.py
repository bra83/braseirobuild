"""Pin and test real cross-encoder inference and Java tokenizer parity."""
import hashlib,json,pathlib,subprocess,tempfile,urllib.request
import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer
root=pathlib.Path("app/src/main/assets/rerank");root.mkdir(parents=True,exist_ok=True)
base="https://huggingface.co/SugoLabs/mmarco-mMiniLMv2-L12-H384-v1/resolve/6772eee/"
for src,dst in [("onnx/model_quantized.onnx","model.onnx"),("tokenizer.json","tokenizer.json"),("README.md","attribution.txt")]:
    urllib.request.urlretrieve(base+src,root/dst)
sha="6e57207c62deb625b5751a376f32ac66109281b0d199a2aec09f2671e066acf5"
with (root/"model.onnx").open("rb") as f: assert hashlib.file_digest(f,"sha256").hexdigest()==sha
t=json.loads((root/"tokenizer.json").read_text());assert t["model"]["type"]=="Unigram"
v=t["model"]["vocab"]
assert v[0][0]=="<s>" and v[2][0]=="</s>" and v[3][0]=="<unk>"
with (root/"vocab.txt").open("w",encoding="utf8") as f:
    for piece,score in v:
        assert "\n" not in piece and "\r" not in piece and "\t" not in piece
        f.write(f"{piece}\t{score}\n")
tok=Tokenizer.from_file(str(root/"tokenizer.json"))
fixtures=[
 ("Qual a dificuldade de sono relatada?","Não consigo dormir à noite.","Comprei uma bicicleta."),
 ("Como foi a relação com a mãe?","Discuto muito com minha mãe.","O ônibus saiu cedo."),
 ("O paciente perdeu o emprego?","Fui demitido na semana passada.","A parede é azul."),
 ("Quais atividades dão prazer?","Gosto muito de tocar violão.","O relógio marcou oito horas."),
 ("Que apoio a pessoa recebe?","Minha irmã me ajuda quando preciso.","A receita leva farinha."),
 ("O que a pessoa contou sobre pesadelos?","Sonhei que estava caindo de uma ponte.","A loja vende sapatos.")]
with tempfile.TemporaryDirectory() as d:
    p=pathlib.Path(d);runner=p/"RerankParity.java";runner.write_text('''package br.com.tutorclinico;
import java.nio.file.*;import java.util.*;
public class RerankParity {public static void main(String[] a)throws Exception{
RerankTokenizer t=new RerankTokenizer(Files.newBufferedReader(Path.of(a[0])));
System.out.println(Arrays.toString(t.pair(a[1],a[2])));
}}''')
    subprocess.run(["javac","-d",d,"app/src/main/java/br/com/tutorclinico/RerankTokenizer.java",str(runner)],check=True)
    opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
    s=ort.InferenceSession(str(root/"model.onnx"),sess_options=opts,providers=["CPUExecutionProvider"])
    results=[]
    def score(q,p):
        enc=tok.encode(q,p)
        ids=json.loads(subprocess.check_output(["java","-cp",d,"br.com.tutorclinico.RerankParity",str(root/"vocab.txt"),q,p],text=True))
        assert ids==enc.ids, f"Tokenizer parity mismatch: {q!r} {p!r}"
        data={"input_ids":ids,"attention_mask":[1]*len(ids),"token_type_ids":[0]*len(ids)}
        z=s.run(None,{i.name:np.array([data[i.name]],dtype=np.int64) for i in s.get_inputs()})[0]
        assert z.shape==(1,1) and np.isfinite(z).all()
        return float(z[0,0])
    for q,p,n in fixtures:
        a,b=score(q,p),score(q,n);results.append([a,b]);print("rerank PT",a,b,flush=True)
    assert sum(a>b for a,b in results)>=5,"reranker quality smoke gate"
(root/"gate.txt").write_text(json.dumps({"sha256":sha,"ranking_correct":sum(a>b for a,b in results),"total":len(results),"clinical_validation":False,"results":results}))
(root/"tokenizer.json").unlink()
print("PASS real reranker inference and Java pair tokenization",flush=True)
