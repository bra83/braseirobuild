"""Fetch fixed TAGARELA weights with SHA256 and execute a real Portuguese speech smoke test."""
import hashlib,pathlib,urllib.request
import numpy as np
import sherpa_onnx
import wave,subprocess,tempfile
root=pathlib.Path("app/src/main/assets/tagarela")
root.mkdir(parents=True,exist_ok=True)
url="https://huggingface.co/joaorura/parakeet-tdt-0.6b-v3-ptBR-sherpa-onnx-int8/resolve/bf31b612b9ea9602c21b9faf0dec038ff2c9a3fc/"
files={
"encoder.int8.onnx":"260aff7bf778b016b42385e186d88a57224bd6958001993dbb6070a9a75b6690",
"decoder.int8.onnx":"6b77e877b1dc29ebead07c0f461a7385d1ac5f8eff5be6422fdddb390d4a9967",
"joiner.int8.onnx":"eb365a33945103cb787aa849b344cbf2abc093b43211e3b969ff97be3fdfc0e4",
"tokens.txt":"d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d"}
for name,expected in files.items():
 p=root/name
 urllib.request.urlretrieve(url+name,p)
 with p.open("rb") as f: actual=hashlib.file_digest(f,"sha256").hexdigest()
 assert actual==expected,(name,actual)
recognizer=sherpa_onnx.OfflineRecognizer.from_transducer(encoder=str(root/"encoder.int8.onnx"),decoder=str(root/"decoder.int8.onnx"),joiner=str(root/"joiner.int8.onnx"),tokens=str(root/"tokens.txt"),model_type="nemo_transducer",num_threads=4,sample_rate=16000,feature_dim=128)
with tempfile.TemporaryDirectory() as tmp:
 wav=pathlib.Path(tmp)/"fixture.wav"
 subprocess.run(["espeak-ng","-v","pt-br","-s","135","-w",str(wav),"Bom dia. Hoje vamos conversar sobre a família e o trabalho."],check=True)
 with wave.open(str(wav),"rb") as f:
  rate=f.getframerate();x=np.frombuffer(f.readframes(f.getnframes()),dtype="<i2").astype(np.float32)/32768
 x=np.interp(np.arange(round(len(x)*16000/rate))*rate/16000,np.arange(len(x)),x).astype(np.float32)
 stream=recognizer.create_stream();stream.accept_waveform(16000,x);recognizer.decode_stream(stream)
 result=stream.result
 print("TAGARELA fixture:",result.text,flush=True)
 assert len(result.text.strip())>8 and sum(word in result.text.lower() for word in ("bom","dia","família","trabalho","conversar"))>=2
 print("PASS real TAGARELA Portuguese inference; synthetic speech smoke test, not clinical WER.")
