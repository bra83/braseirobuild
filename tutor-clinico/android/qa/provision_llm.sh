#!/usr/bin/env bash
set -euo pipefail
root=app/src/main/assets/llm
mkdir -p "$root" app/src/main/jniLibs/arm64-v8a
curl --fail --location --retry 3 --max-time 900 https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/6dd44a1/qwen2.5-0.5b-instruct-q4_k_m.gguf -o "$root/model.gguf"
echo "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db  $root/model.gguf" | sha256sum -c -
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
git clone --depth 1 --branch b5046 https://github.com/ggml-org/llama.cpp.git "$tmp/llama"
cp "$tmp/llama/LICENSE" "$root/llama-license.txt"
# This pinned GGUF revision does not provide README.md at the raw URL.
# Preserve exact model identity and source without blocking on a nonexistent file.
printf '%s\n' 'Qwen2.5-0.5B-Instruct-GGUF — Qwen' 'Source: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF' 'Revision: 6dd44a1' 'File: qwen2.5-0.5b-instruct-q4_k_m.gguf' 'SHA256: 74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db' > "$root/attribution.txt"
cmake -S qa/native -B "$tmp/host" -DLLAMA_SOURCE="$tmp/llama" -DCMAKE_BUILD_TYPE=Release
cmake --build "$tmp/host" -j 2 --target eco_generate
# Genuine generation, no prompt in command-line arguments.
python - "$tmp/host/eco_generate" "$root" <<'PY'
import pathlib,subprocess,sys,json
exe,root=sys.argv[1],pathlib.Path(sys.argv[2])
prompt="<|im_start|>system\nResponda em português usando a fonte.\n<|im_end|>\n<|im_start|>user\n[S1] A pessoa relatou dificuldade para dormir. Qual dificuldade foi relatada? Responda em uma frase.\n<|im_end|>\n<|im_start|>assistant\n"
r=subprocess.run([exe,str(root/"model.gguf")],input=prompt,text=True,capture_output=True,timeout=180,check=True)
out=r.stdout.strip();print("Synthetic generation:",out,flush=True)
assert len(out)>5 and ("dorm" in out.lower() or "sono" in out.lower()),"Generation smoke gate"
(root/"gate.txt").write_text(json.dumps({"runtime":"llama.cpp-b5046","model_sha256":"74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db","output":out,"clinical_validation":False},ensure_ascii=False))
PY
mkdir -p build/eco-advanced-model
advanced=build/eco-advanced-model/qwen2.5-1.5b-instruct-q4_k_m.gguf
curl --fail --location --retry 3 --max-time 900 https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/dd26da4/qwen2.5-1.5b-instruct-q4_k_m.gguf -o "$advanced"
echo "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e  $advanced" | sha256sum -c -
printf '%s\n' 'Qwen2.5-1.5B-Instruct-GGUF — Qwen' 'Apache-2.0' 'Source: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF' 'Revision: dd26da4' 'SHA256: 6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e' > build/eco-advanced-model/ATTRIBUTION.txt
curl --fail --location --retry 3 https://www.apache.org/licenses/LICENSE-2.0.txt -o build/eco-advanced-model/LICENSE.txt
node qa/audit_clinical_generation.mjs "$tmp/host/eco_generate" "$root" "$advanced"
cp "$root/gate.txt" build/eco-advanced-model/gate.json
ndk="$ANDROID_HOME/ndk/27.2.12479018"
test -f "$ndk/build/cmake/android.toolchain.cmake"
cmake -S qa/native -B "$tmp/android" -DLLAMA_SOURCE="$tmp/llama" -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 -DANDROID_STL=c++_static
cmake --build "$tmp/android" -j 2 --target eco_generate
cp "$tmp/android/eco_generate" app/src/main/jniLibs/arm64-v8a/libeco_generate.so
file app/src/main/jniLibs/arm64-v8a/libeco_generate.so
echo "PASS host generation and ARM64 generator compilation"
