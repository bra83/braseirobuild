#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parents[1]
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
policy=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/SceneImageLayoutPolicy.java').read_text(encoding='utf-8')
checks={
 'prompt pede 9:16':'proporção 9:16' in main,
 'preview usa política dedicada':'sceneImagePreviewLayoutParams()' in main and 'SceneImageLayoutPolicy.heightForWidth(width)' in main,
 'altura é largura vezes 16/9':'widthPx*(16.0/9.0)' in policy,
 'preview não usa quadro horizontal fixo':'sharedImageUri==null?dp(130):dp(280)' not in main,
 'preview não tem fundo preto':'sceneImage.setBackgroundColor(Color.BLACK)' not in main,
 'preview usa recorte proporcional':'sceneImage.setScaleType(ImageView.ScaleType.CENTER_CROP)' in main,
 'preview fica centralizado':'params.gravity=Gravity.CENTER_HORIZONTAL' in main,
 'imagem abre em tela cheia':'showSceneImageFullscreen()' in main,
 'metadados 9:16 persistidos':'putInt("scene_image_aspect_w",9)' in main and 'putInt("scene_image_aspect_h",16)' in main,
 'preferência horizontal antiga removida':'.remove("scene_image_fill")' in main,
}
failed=[]
for name,ok in checks.items():
 print(('OK  ' if ok else 'FALHA  ')+name)
 if not ok: failed.append(name)
print(f'RESULTADO IMAGEM 9:16: {len(checks)-len(failed)}/{len(checks)}')
raise SystemExit(1 if failed else 0)
