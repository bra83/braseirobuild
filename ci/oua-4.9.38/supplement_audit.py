from pathlib import Path
import sys

if len(sys.argv) != 3:
    raise SystemExit("usage: supplement_audit.py <project-root> <report>")
root = Path(sys.argv[1]).resolve()
out = Path(sys.argv[2]).resolve()
out.parent.mkdir(parents=True, exist_ok=True)

terms = {
  "Modo Peregrino": ["Modo Peregrino","Tabela de Narração","Tabela de Conhecimento","Sociedade igual a 3","15 pontos","papéis de jornada não são usados"],
  "Valfenda": ["Alto Elfo","Altos Elfos","Artífice de Eregion","Beleza das Estrelas","Poder do Primogênito","Habilidade dos Eldar","Elrond"],
  "Povos Terras Ermas/Selváticas": ["Beorning","Beornings","Furioso","Elfos da Floresta das Trevas","Povo Crepuscular","Homens da Floresta","Madeira - Goer","Sonhos Élficos","Luzir de Ira","A Resolução do Caçador"],
  "Trilhas de Personagens": ["Trilhas de Personagens","Eventos Importantes","antecedente","ANTECEDENTE"],
}
exts={".ts",".tsx",".js",".jsx",".json"}
files=[p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in exts and not any(x in p.parts for x in ("node_modules","build","dist","dist-offline",".offline-vendor"))]
rows=["# OUA 4.9.38 — auditoria de suplementos 2e",""]
for group, needles in terms.items():
    rows += [f"## {group}",""]
    for needle in needles:
        hits=[]
        for p in files:
            txt=p.read_text(encoding="utf-8",errors="ignore")
            for i,line in enumerate(txt.splitlines(),1):
                if needle.lower() in line.lower():
                    hits.append(f"- {p.relative_to(root)}:{i}: {line.strip()[:260]}")
                    if len(hits)>=12: break
            if len(hits)>=12: break
        rows.append(f"### {needle}")
        rows.extend(hits or ["- SEM IMPLEMENTAÇÃO/REFERÊNCIA LOCAL ENCONTRADA"])
        rows.append("")

for rel in ["lib/sourceAuthority.ts","lib/supplementRules.ts","lib/characterFactory.ts","lib/rules/equipment.ts","lib/rules/combat.ts","lib/rules/journey.ts","lib/rules/shadow.ts"]:
    p=root/rel
    rows += [f"## ARQUIVO {rel}",""]
    if p.exists():
        txt=p.read_text(encoding="utf-8",errors="ignore")
        rows += ["```text", txt[:30000], "```", ""]
    else:
        rows += ["- AUSENTE",""]

out.write_text("\n".join(rows),encoding="utf-8")
print(out)
