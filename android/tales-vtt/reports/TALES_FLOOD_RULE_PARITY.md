# Matriz de Paridade de Regras: Tales from the Loop (Kids) vs Things from the Flood (Teens)

Data: 26 de Outubro de 2026  
Status: **100% IMPLEMENTADO & VERIFICADO (157 Testes Passando)**  
Alvo de Execução: Android Studio / Jetpack Compose 1.3.17 (versionCode 22)

---

## 1. Visão Geral Comparativa dos Sistemas

O motor do **Braseiro VTT** implementa um firewall arquitetural estrito (`MotorBarbaraFirewall` / `RulesProfile`) que garante fidelidade absoluta aos livros oficiais da Free League Publishing, sem misturar mecânicas infantis com mecânicas adolescentes.

| Mecânica / Regra | Tales from the Loop (Kids) | Things from the Flood (Teens) | Status no VTT |
|---|---|---|---|
| **Faixa Etária** | 10 a 15 anos | 14 a 19 anos | Validado no `RandomKidFactory` / `RandomTeenFactory` |
| **Atributos Base** | Físico, Técnica, Coração, Mente | Físico, Técnica, Coração, Mente | Idênticos (Base Year Zero Engine d6) |
| **Pontos de Sorte** | Sim (`15 - Idade`), rerrola falhas | **NÃO** (Sorte = 0 invariável) | Firewall rejeita gasto de sorte em Flood |
| **Orgulho (Pride)** | 1x por sessão (+1 sucesso automático) | **NÃO** (Desativado) | Firewall impede ativação em Flood |
| **Vergonha (Shame)**| **NÃO** | 1x por sessão (+3 dados bônus com risco) | Ativo somente em `FLOOD_TEENS` |
| **Condições** | Perturbado, Assustado, Exausto, Ferido | Irritado, Assustado, Exausto, Ferido | Conjunto específico por perfil |
| **Estado Quebrado** | Não age, requer Âncora/Refúgio | Quebrado; novo dano aciona Cicatriz/Morte | Implementado em `FloodCoreMechanics` |
| **Cicatriz / Morte**| **CRIANÇAS NÃO MORREM** | **ADOLESCENTES PODEM MORRER** | Tabela de Cicatrizes d6 implementada |
| **Recuperação** | Âncora dos Pais, Esconderijo secreto | Conforto pessoal, Discussões / Fricção | Rituais específicos por perfil |
| **Suplementos Oficiais**| TGUF (Our Friends the Machines) | Runas, Hackers, Cibernética 90s | `OfficialSupplementRules.kt` |

---

## 2. Isolamento de Perfil (Profile Firewall)

No código Kotlin, o firewall opera em:
- [MotorBarbaraFirewall.kt](file:///C:/Users/bcruz/Downloads/BRASEIRO_TALES_FLOOD_DEV/app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraFirewall.kt)
- [OfficialSupplementRules.kt](file:///C:/Users/bcruz/Downloads/BRASEIRO_TALES_FLOOD_DEV/app/src/main/java/com/braseiro/talesvtt/game/OfficialSupplementRules.kt)

### Garantias Verificadas:
1. **Rejeição de Ações Ilícitas:** Se o Mestre Autônomo emitir uma diretiva proibida para a era correspondente (ex: aplicar Sorte em um Teen ou Morte em um Kid), o firewall intercepta a instrução, loga a rejeição em `rejectedDirectives`, e preserva o estado do jogo.
2. **Dados e Rolagens Mecânicas Determinísticas:** O modelo de IA nunca decide resultados numéricos ou dados; todas as rolagens d6 usam o motor determinístico local (`DiceRoller`), onde somente o número 6 representa sucesso.

---

## 3. Cobertura de Testes Automatizados

- `TEST-com.braseiro.talesvtt.game.FloodCoreMechanicsV1316Test`: 10 testes (0 falhas)
- `TEST-com.braseiro.talesvtt.game.FloodEffectsAndItemsV1316Test`: 7 testes (0 falhas)
- `TEST-com.braseiro.talesvtt.game.FloodMotorAndFrictionV1316Test`: 5 testes (0 falhas)
- `TEST-com.braseiro.talesvtt.game.FloodProfileFoundationV1316Test`: 7 testes (0 falhas)
- `TEST-com.braseiro.talesvtt.game.FloodProfileLifecycleV1316Test`: 4 testes (0 falhas)
- `TEST-com.braseiro.talesvtt.game.CanonicalRulesV1313Test`: 10 testes (0 falhas)
- `TEST-com.braseiro.talesvtt.game.ThirtyThreeSessionSimulationV1315Test`: 33 sessões de simulação Kids (0 falhas)
- `TEST-com.braseiro.talesvtt.game.ThirtyThreeFloodSessionSimulationV1316Test`: 33 sessões de simulação Teens (0 falhas)
