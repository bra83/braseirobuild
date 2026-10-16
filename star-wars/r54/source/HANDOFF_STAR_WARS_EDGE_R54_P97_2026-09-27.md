# HANDOFF — BRASEIRO STAR WARS EDGE R54 P97

Data: 27/09/2026
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P96_SOURCE_CHECKPOINT.zip`.
Princípios: projeto não recriado; GitHub não usado; Android/Gradle não executado; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler e integrações P96 preservados.

## OBJETIVO DESTE BLOCO
Aprofundar a fidelidade das qualidades de armas e do gasto de resultados narrativos sem promover famílias ainda incompletas. O foco foi eliminar gasto duplicado de Vantagens/Triunfos e transformar qualidades ativas em efeitos reais, persistentes e explicitamente escolhidos pela UI.

## IMPLEMENTADO

### Orçamento único de Vantagens / Triunfos
- cada ataque cria `pendingPlayerSpend` com Vantagens/Triunfos restantes;
- crítico e qualidades ativas consomem o MESMO orçamento;
- não é possível usar as mesmas Vantagens para Automática e depois reutilizá-las para crítico/qualidade;
- resultados não gastos são descartados ao encerrar o turno, como estado transitório do ataque.

### Automática e Vinculada
- Automática não gera mais acertos extras automaticamente;
- cada acerto adicional de Automática exige ativação explícita por 2 Vantagens;
- Vinculada exige 2 Vantagens por ativação e respeita o rating máximo.

### Explosão
- em acerto, ativação custa 2 Vantagens;
- a UI exige seleção explícita de quais outros adversários estão Engajados com o alvo original;
- o motor NÃO presume proximidade entre NPCs que o estado não conhece;
- em falha, Explosão custa 3 Vantagens e inclui o alvo original;
- dano de Explosão passa pelo soak dos alvos e persiste no encontro.

### Estados e dano ativo
- Desorientação: `disoriented` pelo rating, com Contratempo real nas rolagens afetadas;
- Concussiva: `staggered` pelo rating, causando perda de ação real;
- Enredante: `immobilized` pelo rating;
- Atordoadora: aplica Tensão igual ao rating, com conversão apropriada para adversários sem pool de Tensão;
- Incendiária: persiste efeito periódico e aplica o dano base no início dos turnos pelo número de rodadas do rating.

### Hendedura / dano de item
- custa 1 Vantagem por ativação e pode repetir;
- item empunhado passa por íntegro → minor → moderate → major → destroyed;
- minor adiciona Contratempo no uso; moderate aumenta dificuldade; major/destroyed torna a arma inutilizável;
- o alvo da Hendedura é escolhido explicitamente pela interface.

### Modo atordoante
- toggle persistente na arma;
- quando ativo, converte dano em Tensão;
- limita o uso ao alcance Curto;
- não altera permanentemente o perfil base da arma.

### Segurança / fronteiras
- nenhum bridge concede equipamento ou resultado gratuitamente;
- Blast não inventa quem está Engajado;
- qualidades sem implementação segura continuam indisponíveis, em vez de serem simuladas por texto.

## MATRIZ MECÂNICA P97
- 55 famílias canônicas.
- JOGÁVEL: **31/55 = 56,4%** — inalterado por critério conservador.
- ENGINE REAL sem UI completa: 4.
- PARCIAL: 3.
- RAG/DECLARAÇÃO: 17.
- engine determinístico real: **35/55 = 63,6%**.
- `EQUIP.QUALITIES` permanece PARCIAL apesar do avanço: ainda faltam Guiada, Derrubar, Iônica/Trator e outros casos/perfis antes de uma promoção legítima.

## EVIDÊNCIA FINAL
- focal P92→P97: **67/67 PASS**;
- Sob um Sol Negro: **106/106 PASS**;
- Além da Orla: **57/57 PASS**;
- regressão regular: **1086/1086 PASS, 0 FAIL**;
- stress: **6/6 PASS, 0 FAIL**;
- total automatizado: **1092/1092 PASS, 0 FAIL**;
- sintaxe controller/weapons/uiBridge/runtime/bundle/builders: PASS;
- bundle offline recompilado com **63 módulos**.

## ANDROID
**NÃO EXECUTADO**, conforme instrução explícita do usuário: compilar Android/APK somente no fechamento final.

## VERSÃO
- Android versionCode: 10097
- Android versionName: 1.0.97
- Runtime/shell: 1.0.97
- Bundle label: 1.0.97-r54-p97

## PENDÊNCIAS REAIS / P98
1. Guiada, Derrubar e Iônica/Trator apenas onde o estado atual comportar os efeitos sem inventar silhueta/proximidade.
2. Attachments/modificações a partir de perfis mecânicos autorizados/machine-readable, com hard points, custo, Mechanics, efeito e persistência.
3. Signature Abilities: catálogo integral por carreira + ativação real; não importar o pacote antigo incompleto como se fosse completo.
4. Ciclos faltantes de remoção/escape de estados e manobras contextuais restantes.
5. Regressão das aventuras e matriz antes de qualquer nova promoção de família.
