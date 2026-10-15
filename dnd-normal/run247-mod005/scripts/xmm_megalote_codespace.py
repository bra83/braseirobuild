#!/usr/bin/env python3
"""XMM megalote: source-bound, isolated Codex worktree job generator/launcher.

No Android/DB/UI modification is performed by the generator itself.
Opt-in --execute requires an already authenticated Codex CLI in this Codespace.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs/editorial-evidence/xmm-condition-choice-preview-local-20261015"
SOURCE_PIN = "8c026b807fac21862a309379a0c3228a0683198b"
FAMILY_SIZE = {
    "damage_or_save_branch": 12,
    "other_source_effect": 10,
    "attack_hit_condition": 12,
    "swallow_and_escape": 12,
    "explicit_effect_choice": 12,
    "monster_spell_action": 12,
}
PROTECTED = (
    "app/src/main/java/com/braseiro/dnd5e/MainActivity.java",
    "app/src/main/java/com/braseiro/dnd5e/AppDb.java",
    "scripts/codespace-dnd-autopilot.sh",
    ".github/workflows/",
    "Backup21",
)
BASE_BRANCH = "automation/dnd-xmm-megalote-20261015"


def git(*args: str, cwd: Path = ROOT, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(["git", *args], cwd=cwd, text=True, capture_output=True, check=check)


def read_inventory() -> tuple[Path, dict]:
    options = sorted(EVIDENCE.glob("manual-effect-inventory-*.json"))
    if not options:
        raise RuntimeError(f"MANIFEST_NOT_FOUND in {EVIDENCE}")
    # The branch snapshot should only contain the most recent inventory.
    candidates = []
    for p in options:
        try:
            obj = json.loads(p.read_text(encoding="utf-8"))
            if obj.get("schema") == "XMM_SOURCE_EFFECT_INVENTORY_V1":
                candidates.append((p, obj))
        except (ValueError, OSError):
            continue
    if not candidates:
        raise RuntimeError("VALID_XMM_MANIFEST_NOT_FOUND")
    p, data = max(candidates, key=lambda item: int(item[1].get("manual_unmodeled_effect_count", 0)))
    entries = data.get("entries")
    if not isinstance(entries, list) or len(entries) != data.get("manual_unmodeled_effect_count"):
        raise RuntimeError("XMM_MANIFEST_COUNT_MISMATCH")
    if data.get("source_commit") != SOURCE_PIN:
        raise RuntimeError("SOURCE_PIN_CHANGED_REQUIRES_REVIEW")
    pointers = [e.get("source_pointer") for e in entries]
    if len(set(pointers)) != len(entries):
        raise RuntimeError("DUPLICATED_SOURCE_POINTER")
    return p, data


def build_jobs(data: dict) -> list[dict]:
    jobs = []
    for family, chunk_size in FAMILY_SIZE.items():
        selected = [
            e for e in data["entries"]
            if e.get("family") == family and e.get("coverage_status") == "MANUAL_UNSUPPORTED"
        ]
        selected.sort(key=lambda e: (e.get("monster_id", ""), e.get("action_id", ""), e.get("source_pointer", "")))
        for n, first in enumerate(range(0, len(selected), chunk_size), 1):
            batch = selected[first:first + chunk_size]
            job_id = f"{family}-{n:02d}"
            jobs.append({
                "job_id": job_id,
                "family": family,
                "count": len(batch),
                "source_commit": SOURCE_PIN,
                "entries": batch,
                "status": "UNIMPLEMENTED_NOT_CERTIFIED",
            })
    return jobs


def prompt_for(job: dict, base_sha: str, out: Path) -> str:
    family = job["family"]
    entries = "\n".join(
        f"- {e['monster_id']} / {e['action_id']} / {e['source_pointer']} | "
        f"contrato={json.dumps(e.get('mechanical_contract', {}), ensure_ascii=False, sort_keys=True)}"
        for e in job["entries"]
    )
    family_guidance = {
        "damage_or_save_branch":
            "Consolide branches de dano/salvaguarda em contratos tipados: tipo, fórmula, sucesso/falha, "
            "defesas e múltiplos alvos. Não aplique dano sem todos os campos exigidos pela fonte.",
        "other_source_effect":
            "Agrupe riders/reações/efeitos excepcionais por gatilho, duração, gasto e contexto. "
            "Não presuma efeitos apenas porque a ação foi catalogada.",
        "attack_hit_condition":
            "Reutilize os ledgers de condição e fuga; exija tamanho, alcance, escape DC e duração comprovados.",
        "swallow_and_escape":
            "Trate Swallow como transição de estado conservadora: pré-requisitos, contensão, "
            "dano recorrente, liberação e recuperação sem duplicação.",
        "explicit_effect_choice":
            "Exija escolha explícita e dados oficiais; alternativas mutuamente exclusivas jamais somam dano.",
        "monster_spell_action":
            "Use o motor de magias existente; não invente CD, consumo, concentração ou rolagens.",
    }[family]
    return f"""# Codex / Codespace — XMM MEGALOTE {job['job_id']}

REPOSITÓRIO bra83/braseirobuild | projeto dnd-normal/run247-mod005
BASE ISOLADA: {BASE_BRANCH}, SHA inicial {base_sha}
FONTE PINADA: {SOURCE_PIN}
ARQUIVO DE LOTE: {out / 'jobs' / (job['job_id'] + '.json')}
NÃO ALTERAR PR #39, PR #40, branch canônica ou worktrees externas.

MISSÃO: Implementar código real, compilável e testes para este lote de {job['count']} ações
XMM. Primeiro compare o estado atual com as ações fonte-pinadas. Se um efeito já estiver
implementado e comprovado, marque-o como coberto sem duplicar. Não faça catálogo decorativo,
previews adicionais sem executor nem novas classes sem necessidade.

ESTRATÉGIA DA FAMÍLIA: {family_guidance}

AÇÃO/JANELA DE EXECUÇÃO:
1. Investigue executores e testes já existentes e compare com a fonte pinada.
2. Construa executores determinísticos reutilizáveis com proposta/validação de contexto.
3. Para efeitos exigindo UI/SQLite protegidos, implemente só o núcleo independente e
   declare bloqueio de integração; não sinalize como executado no combate.
4. Comprove fonte/resultado para cada ponteiro abaixo. Nenhuma seleção por
   similaridade textual substitui verificação do contrato da ação.
5. Teste sucesso, falha, tipo de dano, imunidade, duração, economia de ação,
   consumo/replay, persistência só se já houver API pública permitida.
6. Rode verificadores Python aplicáveis; rode Gradle/Robolectric se o ambiente
   permitir. Não afirme testes executados se dependências faltarem.
7. Faça commits reais NA BRANCH DESTA WORKTREE, com lista de ponteiros
   realmente concluídos e bloqueados. Não faça merge nem push para PR #39/#40.

ARQUIVOS PROIBIDOS:
- MainActivity.java, AppDb.java, Backup21, scripts/codespace-dnd-autopilot.sh
- .github/workflows/, Mystara, aventuras, suplementos, fontes pinadas
- Qualquer arquivo modificado por outro job do megalote.
Prefira arquivos novos com nomes distintos desta família e testes correspondentes.
Não altere o comparador comum sem justificar e documentar o potencial conflito.
Sem chamadas Gemini/RAG para decidir mecânicas. Sem rolagens automáticas.
Sem chaves pagas. Sem enfraquecer testes e critérios de homologação.
Não compilar APK nem declarar XMM completo nesta frente.

ENTRADAS DESTE LOTE (fonte, monstro, ação e contrato):
{entries}

ENTREGA: código + testes + SHA(s) dos commits + contagem de ações efetivamente
implementadas, restantes e dependentes de integração; não basta produzir relatório.
"""


def write_jobs(out: Path, inventory_file: Path, data: dict, jobs: list[dict], sha: str) -> None:
    folder = out / "jobs"
    folder.mkdir(parents=True, exist_ok=True)
    status_counts: dict[str, int] = {}
    family_counts: dict[str, int] = {}
    for e in data["entries"]:
        status = e.get("coverage_status", "UNKNOWN")
        status_counts[status] = status_counts.get(status, 0) + 1
        family = e.get("family", "UNKNOWN")
        family_counts[family] = family_counts.get(family, 0) + 1
    manifest = {
        "format": "BRASEIRO_XMM_MEGALOTE_JOBS_V1",
        "snapshot_sha": sha,
        "inventory_file": str(inventory_file.relative_to(ROOT)),
        "source_commit": SOURCE_PIN,
        "total_effects_in_inventory": len(data["entries"]),
        "by_status": status_counts,
        "by_family": family_counts,
        "jobs_created": len(jobs),
        "jobs": [{"id": j["job_id"], "family": j["family"], "count": j["count"]} for j in jobs],
        "excluded_from_codespace_jobs": {
            "multiattack": "PR #40 Codex continua dono desse mecanismo",
            "blocked_previews": "integração só após PR #39 verde",
            "source_review": "não inferir lacunas do livro",
        },
        "note": "Preparar job nao equivale a implementar efeito; só teste de execução real certifica.",
    }
    (out / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for job in jobs:
        (folder / (job["job_id"] + ".json")).write_text(json.dumps(job, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (folder / (job["job_id"] + ".md")).write_text(prompt_for(job, sha, out), encoding="utf-8")
    print("INVENTORY_TOTAL=", len(data["entries"]))
    print("STATUS_COUNTS=", json.dumps(status_counts, ensure_ascii=False, sort_keys=True))
    print("FAMILY_COUNTS=", json.dumps(family_counts, ensure_ascii=False, sort_keys=True))
    print("JOBS_CREATED=", len(jobs))
    print("ACTIONS_TARGETED=", sum(j["count"] for j in jobs))
    print("JOBS_DIR=", folder)
    print("BASE_SHA=", sha)


def safe_job_name(name: str) -> str:
    return re.sub(r"[^a-z0-9-]+", "-", name.lower()).strip("-")[:80]


def execute_one(job: dict, output: Path, repo_root: Path, base_sha: str) -> tuple[str, str]:
    tag = safe_job_name(job["job_id"])
    workspace = repo_root.parent / ("dnd-xmm-lote-" + tag)
    branch = "automation/dnd-xmm-lote-" + tag + "-20261015"
    if workspace.exists():
        return job["job_id"], f"SKIPPED_WORKTREE_ALREADY_EXISTS: {workspace}"
    # This repository includes Star Wars, OSE and build artifacts: a full
    # checkout takes ~2.4 GB PER JOB. Sparse checkout keeps only D&D files.
    free = shutil.disk_usage(repo_root.parent).free
    if free < 1_500_000_000:
        return job["job_id"], "BLOCKED_LOW_DISK_FREE_BYTES=" + str(free)
    created = git("worktree", "add", "--no-checkout", "-b", branch, str(workspace),
                  base_sha, cwd=repo_root, check=False)
    if created.returncode != 0:
        return job["job_id"], "WORKTREE_ERROR: " + (created.stderr or created.stdout)[-650:]
    sparse = git("sparse-checkout", "set", "--cone", "dnd-normal/run247-mod005",
                 cwd=workspace, check=False)
    if sparse.returncode != 0:
        return job["job_id"], "SPARSE_CHECKOUT_ERROR: " + (sparse.stderr or sparse.stdout)[-650:]
    checkout = git("reset", "--hard", "HEAD", cwd=workspace, check=False)
    if checkout.returncode != 0:
        return job["job_id"], "SPARSE_POPULATE_ERROR: " + (checkout.stderr or checkout.stdout)[-650:]
    if (workspace / "star-wars").exists() or (workspace / "ose").exists():
        return job["job_id"], "SPARSE_CHECKOUT_LEAKED_UNRELATED_PROJECTS"
    cli = shutil.which("codex")
    log_path = output / ("run-" + tag + ".log")
    with (output / "jobs" / (job["job_id"] + ".md")).open("r", encoding="utf-8") as prompt_file, \
            log_path.open("w", encoding="utf-8") as log:
        call = subprocess.run([cli, "exec", "--sandbox", "workspace-write", "-"],
                              stdin=prompt_file, stdout=log, stderr=subprocess.STDOUT,
                              cwd=workspace, text=True, check=False)
    diff = git("diff", "--name-only", base_sha + "..HEAD", cwd=workspace, check=False)
    local = git("status", "--porcelain", cwd=workspace, check=False)
    changed = diff.stdout.splitlines()
    changed += [line[3:] for line in local.stdout.splitlines() if len(line) > 3]
    unsafe = [
        p for p in changed if any(term.lower() in p.lower() for term in PROTECTED)
        or "mystara" in p.lower() or "/adventure/" in p.lower()
    ]
    result = {
        "job": job["job_id"], "exit_code": call.returncode, "branch": branch,
        "worktree": str(workspace), "log": str(log_path),
        "protected_paths_touched": unsafe,
        "changed_paths": sorted(set(changed)),
        "head": git("rev-parse", "HEAD", cwd=workspace, check=False).stdout.strip(),
    }
    (output / ("result-" + tag + ".json")).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if unsafe:
        return job["job_id"], "BLOCKED_PROTECTED_FILES_CHANGED; veja " + str(output / ("result-" + tag + ".json"))
    return job["job_id"], ("DONE" if call.returncode == 0 else "CODEX_FAILED") + "; " + str(log_path)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="Diretório externo para prompts e logs")
    parser.add_argument("--execute", action="store_true", help="Iniciar Codex CLI nas worktrees isoladas")
    parser.add_argument("--workers", type=int, default=1, help="Execução conservadora; no máximo 1 job por invocação")
    parser.add_argument("--next", action="store_true", help="Executar apenas o PRIMEIRO lote sem worktree existente")
    parser.add_argument("--job", type=str, help="Executar apenas este job_id exato")
    parser.add_argument("--list", action="store_true", help="Mostrar lotes e status de worktree sem executar")
    args = parser.parse_args()
    if args.workers != 1:
        parser.error("Este Codespace de 8 GiB só permite --workers 1, um lote por execução.")
    if args.execute and (args.next == bool(args.job)):
        parser.error("Para --execute, informe exatamente um: --next ou --job JOB_ID.")
    if args.execute and not shutil.which("codex"):
        print("CODEX_CLI_UNAVAILABLE: geração de jobs é possível; execução automática requer Codex CLI autenticado.", file=sys.stderr)
        return 2
    top = git("rev-parse", "--show-toplevel").stdout.strip()
    if Path(top).resolve() != ROOT.parents[1].resolve():
        raise RuntimeError("RUN_FROM_ISOLATED_MEGA_WORKTREE_PROJECT_ROOT")
    sha = git("rev-parse", "HEAD").stdout.strip()
    inventory_file, data = read_inventory()
    jobs = build_jobs(data)
    if not jobs:
        raise RuntimeError("NO_MANUAL_UNSUPPORTED_NON_MULTIATTACK_JOBS")
    expected = sum(e.get("coverage_status") == "MANUAL_UNSUPPORTED"
                   and e.get("family") != "multiattack_sequence" for e in data["entries"])
    if expected != sum(j["count"] for j in jobs):
        raise RuntimeError("SOME_NON_MULTIATTACK_FAMILIES_NOT_ASSIGNED")
    output = args.output or Path("/tmp") / ("braseiro-xmm-megalote-" + sha[:8])
    output = output.resolve()
    write_jobs(output, inventory_file, data, jobs, sha)
    state = []
    for j in jobs:
        path = ROOT.parents[1].parent / ("dnd-xmm-lote-" + safe_job_name(j["job_id"]))
        state.append((j, path, path.exists()))
    if args.list:
        for j, path, exists in state:
            print("JOB_STATUS", j["job_id"], "PRESERVED_EXISTING_WORKTREE" if exists else "NOT_STARTED",
                  "ACTIONS", j["count"], "PATH", path)
        print("NEXT_NEW_JOB=", next((j["job_id"] for j, _, exists in state if not exists), "NONE"))
    if not args.execute:
        print("PREPARED_ONLY: executar somente com --execute --next OU --execute --job JOB_ID.")
        return 0
    if args.job:
        matches = [(j, p, exists) for j, p, exists in state if j["job_id"] == args.job]
        if len(matches) != 1:
            parser.error("JOB_ID inexistente no inventário fonte-pinando.")
        target, path, exists = matches[0]
        if exists:
            print("PRESERVED_EXISTING_WORKTREE=", path)
            print("BLOCKED_EXISTING_JOB: não sobrescrever; revisar logs, commits e teste antes de retomar.")
            return 2
    else:
        target = next((j for j, _, exists in state if not exists), None)
        if target is None:
            print("NO_NEW_JOBS: todos os lotes já possuem worktree; não sobrescrever.")
            return 0
    # One job per invocation; Codex CLI quotas and 8 GiB RAM require explicit
    # review before starting another. Previously created worktrees remain untouched.
    print("EXECUTION_SELECTED_JOB=", target["job_id"], "ACTIONS=", target["count"], flush=True)
    name, status = execute_one(target, output, ROOT.parents[1], sha)
    print("RESULT", name, status, flush=True)
    print("EXECUTION_SUMMARY: selected_jobs=1, untouched_jobs=", len(jobs) - 1)
    return 0 if status.startswith("DONE") else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print("MEGALOTE_ABORTED:", type(exc).__name__, str(exc), file=sys.stderr)
        sys.exit(2)
