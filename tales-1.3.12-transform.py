from pathlib import Path
import re
import shutil
import sys

root = Path(sys.argv[1])
repo = Path(__file__).resolve().parent

# Preserve every proven correction through 1.3.11 first.
ns = {"__name__": "__main__", "__file__": str(repo / "tales-1.3.11-transform.py")}
code = (repo / "tales-1.3.11-transform.py").read_text(encoding="utf-8")
old_argv = sys.argv[:]
sys.argv = [ns["__file__"], str(root)]
try:
    exec(compile(code, ns["__file__"], "exec"), ns)
finally:
    sys.argv = old_argv

# Version 1.3.12 / versionCode 16.
gradle = root / "app" / "build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
g = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 16', g, count=1)
g = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.3.12"', g, count=1)
gradle.write_text(g, encoding="utf-8")

# Replace only the MAP screen with the fully interactive implementation.
overlay = repo / "tales-1.3.12-overlay" / "MapScreen.kt"
dst = root / "app/src/main/java/com/braseiro/talesvtt/screens/MapScreen.kt"
if not overlay.is_file():
    raise SystemExit(f"Missing overlay: {overlay}")
shutil.copy2(overlay, dst)

# Add a compact unit contract so CI catches an accidental return to passive markers.
test_dir = root / "app/src/test/java/com/braseiro/talesvtt/screens"
test_dir.mkdir(parents=True, exist_ok=True)
(test_dir / "InteractiveMapContractTest.kt").write_text(r'''package com.braseiro.talesvtt.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InteractiveMapContractTest {
  @Test
  fun mapSourceKeepsInteractiveContracts() {
    val candidates = listOf(
      File("src/main/java/com/braseiro/talesvtt/screens/MapScreen.kt"),
      File("app/src/main/java/com/braseiro/talesvtt/screens/MapScreen.kt")
    )
    val sourceFile = candidates.firstOrNull { it.isFile }
      ?: error("MapScreen.kt não encontrado")
    val source = sourceFile.readText()
    assertTrue(source.contains(".clickable(onClick = onClick)"))
    assertTrue(source.contains("MapControlHotspot"))
    assertTrue(source.contains("SelectedLocationPanel"))
    assertTrue(source.contains("IR PARA"))
    assertTrue(source.contains("MapConnectionsOverlay"))
    assertTrue(source.contains("ANOTAÇÕES DOS KIDS"))
    assertTrue(source.contains("detectTapGestures"))
  }
}
''', encoding="utf-8")

resume = root / "TALES_RETOMADA_CURRENT.md"
r = resume.read_text(encoding="utf-8") if resume.exists() else "# TALES FROM THE LOOP — RETOMADA ATUAL\n"
r += '''\n## Bloco 1.3.12\n- MAPA TOTALMENTE INTERATIVO: pinos agora têm área de toque real e selecionam o local.\n- Ao selecionar um local revelado/visitado aparece ação funcional para solicitar viagem diretamente pelo mapa.\n- FILTROS agora alterna entre revelados, visitados e local atual.\n- CONEXÕES liga/desliga as conexões reveladas entre locais.\n- ANOTAÇÕES liga/desliga pistas e notas dos Kids sobre o mapa.\n- Os cartões de locais revelados também são clicáveis e sincronizam a seleção do mapa.\n- Toques próximos aos pinos também são reconhecidos para melhorar o uso no celular.\n- Rotas dinâmicas e correção de TTS da 1.3.11 foram preservadas.\n'''
resume.write_text(r, encoding="utf-8")

source = dst.read_text(encoding="utf-8")
assert 'versionName = "1.3.12"' in gradle.read_text(encoding="utf-8")
for required in [
    'detectTapGestures',
    'MapControlHotspot',
    'SelectedLocationPanel',
    'MapConnectionsOverlay',
    'ANOTAÇÕES DOS KIDS',
    'IR PARA ${poi.name.uppercase()}',
    '.clickable(onClick = onClick)',
]:
    assert required in source, required
print("TALES 1.3.12 transform: OK")
