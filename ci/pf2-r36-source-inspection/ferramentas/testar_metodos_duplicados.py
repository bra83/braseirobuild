from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []
pattern = re.compile(
    r'^    (?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\],.?]+\s+'
    r'([A-Za-z_$][\w$]*)\s*\(([^)]*)\)',
    re.MULTILINE,
)

def canonical_params(raw: str) -> tuple[str, ...]:
    if not raw.strip():
        return ()
    result = []
    for part in raw.split(','):
        part = re.sub(r'\bfinal\s+', '', part.strip())
        part = re.sub(r'@[A-Za-z_$][\w$]*(?:\([^)]*\))?\s*', '', part)
        tokens = part.split()
        if len(tokens) < 2:
            result.append(part)
        else:
            result.append(' '.join(tokens[:-1]).replace(' ...', '...'))
    return tuple(result)

for java in (ROOT / 'android/app/src/main/java').rglob('*.java'):
    text = java.read_text('utf-8', errors='replace')
    seen = {}
    for match in pattern.finditer(text):
        signature = (match.group(1), canonical_params(match.group(2)))
        line = text.count('\n', 0, match.start()) + 1
        previous = seen.get(signature)
        if previous is not None:
            params = ', '.join(signature[1])
            errors.append(f'{java.relative_to(ROOT)}:{line}: método duplicado {signature[0]}({params}); primeira declaração na linha {previous}')
        else:
            seen[signature] = line

if errors:
    print('FALHA — métodos de classe duplicados encontrados:')
    for error in errors:
        print('- ' + error)
    sys.exit(1)
print('OK — nenhuma assinatura de método de classe duplicada foi encontrada.')
