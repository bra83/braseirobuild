from pathlib import Path
root=Path(__file__).resolve().parents[1]
f=root/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java'
s=f.read_text(encoding='utf-8')
assert 'import android.net.Uri;' in s, 'GeminiService usa Uri sem importar android.net.Uri'
assert 'Uri durable=' in s and 'Uri.fromFile(temp)' in s
print('OK: import android.net.Uri presente e uso de Uri coberto.')
