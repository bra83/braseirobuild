# HANDOFF — BRASEIRO STAR WARS EDGE R54 P92

Data: 27/09/2026
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P91_SOURCE_CHECKPOINT.zip` materializada do Google Drive.
Princípios: projeto não recriado; GitHub não usado; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler, TTS/Android P91 preservados.

## OBJETIVO DESTE BLOCO
Executar a primeira grande onda P0 da matriz mecânica: converter engines reais sem UI em mecânicas realmente alcançáveis na shell R46, corrigindo ao mesmo tempo falhas de integração encontradas na auditoria.

## IMPLEMENTADO

### Combate pessoal e qualidades de armas
- `Accurate/Precisa`: Boost entra no pool ANTES da rolagem.
- `Auto-fire/Automática`: +1 dificuldade quando ativada e hits extras por Advantage são aplicados de verdade.
- `Breach/Brecha` e `Pierce/Perfurante`: chegam ao cálculo real de soak/dano.
- `Vicious/Letal/Viciosa`: modifica o d100 real do crítico.
- crítico de rival/nêmesis passa pela tabela real e persiste metadados do ferimento.
- painel R46 COMBATE PESSOAL usa encontro/alvos reais, arma equipada, Aim, ataque, crítico, turno e encerramento; não cria inimigos grátis.

### Equipamento ativo
- `equipmentOwnedSummary`, `equipWeapon`, `equipArmor`, `unequipArmor` integrados e persistentes.
- armadura inicial legada guardada em `gear` é reconhecida como armadura sem duplicar inventário.
- painel R46 EQUIPAMENTO ATIVO permite equipar somente itens realmente possuídos.

### Veículos
- painel R46 de combate veicular usa veículo real existente.
- expõe estado, velocidade, hull/system strain, distância, acelerar/frear, aproximação/recuo, evasivas, stay on target, full throttle e ataque.
- `addVehicle` NÃO é público no bridge: nenhum botão pode conceder veículo gratuitamente.

### Força
- painel R46 aparece somente para personagem realmente Force-sensitive.
- permite rolagem de dado da Força e compra de poderes/upgrades usando engine/XP reais.
- não concede Force Rating nem sensibilidade por atalho.

### Duty / Contribution
- painel de progressão mostra Duty e Contribution.
- `resolveCampaignMilestone()` expõe apenas resolução legítima de marco já elegível.
- não há mutação pública bruta de Duty/AoR no shared shell.

### Hiperespaço
- `astrogation.mjs` integrado ao controller/bridge.
- painel R46 calcula plano, rota/dificuldade, rola Astronavegação, resolve mishap e persiste `vehicles.travel.lastHyperspaceJump`.
- NÃO marcado como totalmente jogável: ainda falta avançar relógio do Mundo Vivo e mover local/cena ao destino.

### Cibernéticos
- engine real de capacidade/instalação/remoção e efeitos persistentes integrado internamente.
- bridge público é leitura apenas (`cyberneticsSummary`).
- instalação/remoção NÃO foram expostas para não contornar aquisição/custo. Isso permanece pendente até ciclo econômico transacional.

### Bridge/UI
- `uiBridge.mjs` ampliado apenas com operações legítimas.
- removidas superfícies públicas perigosas: `installCybernetic`, `removeCybernetic`, `addVehicle`, `beginPresetEncounter`, `beginCombat`.
- contracts TypeScript R46 atualizados.
- `final-runtime.js` ganhou painéis P92 para rotas 02/03/04/06/07/08.
- `uiBridge.bundle.js` regenerado com 63 módulos.

## MATRIZ MECÂNICA P92
- 55 famílias canônicas.
- JOGÁVEL: 23/55 = 41,8% (era 13/55 = 23,6%).
- ENGINE REAL: 10.
- PARCIAL: 6.
- RAG/DECLARAÇÃO: 16.
- Engine real sem RAG total continua 33/55 = 60,0%; P92 não inflou esse número, converteu engines existentes em jogabilidade real.
- `FORCE.CORE` (fora das 55 do índice original) passou a JOGÁVEL.
- `CYBERNETICS.CORE` registrado como ENGINE REAL, não jogável por decisão de segurança/economia.

## EVIDÊNCIA FINAL
- P92 focal final + contracts: 27/27 PASS.
- Sob um Sol Negro: 106/106 PASS.
- Além da Orla: 57/57 PASS.
- Regressão não-stress: 1031/1031 PASS, 0 FAIL.
- Stress: 6/6 PASS, 0 FAIL.
- Total automatizado: 1037/1037 PASS, 0 FAIL.
- Sintaxe dos módulos alterados/runtime/bundle: PASS.
- Bundle: 63 módulos, `1.0.92-r54-p92`.

## ANDROID
Tentativa de marco executada com `./gradlew :app:assembleDebug --no-daemon`.
Resultado: SEM EVIDÊNCIA DE COMPILAÇÃO. O wrapper parou antes de compilar por `java.net.UnknownHostException: downloads.gradle.org`.
Não declarar Android PASS e não atribuir essa falha ao código P92.

## VERSÃO
- Android versionCode: 10092
- Android versionName: 1.0.92
- Runtime/shell: 1.0.92
- Bundle label: 1.0.92-r54-p92

## PRÓXIMO BLOCO P93
1. Fechar `hiperespaço → tempo → posição/local/cena` com persistência e sem teleporte inconsistente.
2. Implementar attachments/modificações ponta a ponta: hard points, custo/teste, efeito, persistência, remoção/modificação e UI.
3. Expor talents/specializations e ações pessoais restantes (Medicine, Stimpack, condições/manobras) usando engine existente.
4. Criar aquisição transacional de cibernéticos antes de liberar instalação/removal na UI.
5. Completar qualidades de arma restantes e só então promover `EQUIP.QUALITIES` de PARCIAL.
