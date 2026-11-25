#!/usr/bin/env python3
"""
ECO 0.18.0 — Sonda Independente de Isolamento Relacional e Proteção Anti-Cascade
Verifica a resolução dos bloqueadores B01 (Apagamento em cascata no REPLACE)
e B02 (Isolamento relacional e rejeição de mismatch cross-case).
Somente dados fictícios.
"""
import re
import sqlite3
import sys
from pathlib import Path

# Localiza EcoDatabaseHelper.java
src_file = Path(__file__).parent / "tutor-clinico/android/app/src/main/java/br/com/tutorclinico/EcoDatabaseHelper.java"
if not src_file.exists():
    src_file = Path("tutor-clinico/android/app/src/main/java/br/com/tutorclinico/EcoDatabaseHelper.java")
assert src_file.exists(), f"Fonte não encontrada: {src_file}"

content = src_file.read_text(encoding="utf-8")

c = sqlite3.connect(":memory:")
c.execute("PRAGMA foreign_keys=ON")

# Extrai e executa comandos DDL
statements = re.findall(r'db\.execSQL\((.*?)\);\s*(?:\n|$)', content, re.S)
executed_count = 0
for raw in statements:
    clean = raw.strip()
    # Concatena strings literais do Java ("..." + "...")
    parts = re.findall(r'"([^"\n]*)"', clean)
    if not parts:
        continue
    sql = "".join(parts).strip()
    if sql.startswith("CREATE"):
        c.execute(sql)
        executed_count += 1

print(f"DDL executado com sucesso: {executed_count} declarações CREATE (tabelas, índices, views e triggers).")

# 1. Popula paciente fictício A e entidades filhas
c.execute("insert into pacientes(id,nome,created_at,updated_at,payload_enc) values('A','Fictício',1,1,x'01')")
c.execute("insert into sessoes(id,case_id,data,origem,created_at,updated_at,payload_enc) values('S','A','2026-01-01','manual',1,1,x'01')")
c.execute("insert into rascunhos(case_id,updated_at,draft_enc) values('A',1,x'01')")
c.execute("insert into diario_privado(id,case_id,data,created_at,entry_enc) values('D','A','2026-01-01',1,x'01')")
c.execute("insert into segmentos(id,job_id,session_id,case_id,speaker,start_ms,end_ms,text_enc,checksum,created_at) values('T','J','S','A','paciente',0,1000,x'01','x',1)")

tables = ['sessoes', 'rascunhos', 'diario_privado', 'segmentos']
counts = lambda: {x: c.execute('select count(*) from ' + x).fetchone()[0] for x in tables}

antes = counts()
print('ANTES DO REPLACE:', antes)
assert antes == {'sessoes': 1, 'rascunhos': 1, 'diario_privado': 1, 'segmentos': 1}

# 2. Executa INSERT OR REPLACE no paciente A (simula SecureStore.save)
c.execute("insert or replace into pacientes(id,nome,created_at,updated_at,payload_enc) values('A','Alterado',1,2,x'02')")
depois = counts()
print('DEPOIS DO REPLACE:', depois)

# B01 GATE CHECK: Nenhum registro filho pode ter desaparecido!
assert depois == antes, f"ERRO B01: Registros foram apagados pelo REPLACE! Antes: {antes}, Depois: {depois}"
print("VERIFICAÇÃO B01: APROVADA ✅ (Nenhum registro em cascata foi apagado pelo REPLACE)")

# 3. Testa B02: Associação cruzada (Cross-case mismatch)
# Cria paciente B com sessão SB
c.execute("insert into pacientes(id,nome,created_at,updated_at,payload_enc) values('B','Outro fictício',1,1,x'01')")
c.execute("insert into sessoes(id,case_id,data,origem,created_at,updated_at,payload_enc) values('SB','B','2026-01-01','manual',1,1,x'01')")

# Tenta associar segmento com case_id='A' e session_id='SB' (que pertence a 'B')
cross_case_rejected = False
try:
    c.execute("insert into segmentos(id,job_id,session_id,case_id,speaker,start_ms,end_ms,text_enc,checksum,created_at) values('T2','J','SB','A','paciente',0,1000,x'01','x',1)")
except sqlite3.IntegrityError as e:
    cross_case_rejected = True
    print(f"Sucesso: SQLite rejeitou inserção cruzada com: {e}")

# B02 GATE CHECK: A inserção cruzada DEVE ser estritamente rejeitada pelo SQLite
assert cross_case_rejected, "ERRO B02: SQLite aceitou segmento com mismatch entre case_id e session_id!"
print("VERIFICAÇÃO B02: APROVADA ✅ (Mismatch cross-case rejeitado por restrição de chave estrangeira composta)")

print("\n" + "=" * 70)
print("TODAS AS SONDAS INDEPENDENTES B01 E B02 FORAM APROVADAS COM SUCESSO!")
print("=" * 70)
