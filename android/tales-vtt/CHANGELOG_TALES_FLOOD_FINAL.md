# CHANGELOG: Braseiro Tales & Flood VTT (Versão 1.3.17 - Build Final Atualizado)

**Data de Lançamento:** 26 de Outubro de 2026  
**Identificador do Pacote:** `com.braseiro.talesvtt`  
**Version Code:** `22`  
**Version Name:** `1.3.17`  
**Base Canônica:** Android Studio / Compose 1.3.16 + Motores V2.2

---

## Fechamentos Adicionais Realizados

1. **Navegação e Roteamento Jetpack Compose:**
   - Adicionadas as rotas `composable(InternalRoutes.ADVENTURES)` e `composable(InternalRoutes.VOICE_SETTINGS)` diretamente no `NavHost` de `AppNavGraph.kt`. Isso resolveu a pendência que impedia a transição da tela de Menu para Aventuras e Configurações de Voz.
2. **Ciclo Completo de Aventuras (M07):**
   - Na tela `AdventuresScreen.kt`, foram adicionados controles para mistério em andamento (`Continuar Investigação`, `Encerrar Mistério` com consolidação de marcas de mundo e legado) e exportação do arquivo JSON da aventura via compartilhamento padrão do Android (`Intent.ACTION_SEND`).
3. **M13 — Verificador de Atualizações no Menu:**
   - Adicionada seção visual no `MenuScreen.kt` ("SISTEMA & ATUALIZAÇÕES") com botão funcional para `VERIFICAR ATUALIZAÇÕES` (versionCode 22 / versão 1.3.17).
4. **P1 — Suíte de Fuzz Estocástico com 250.000 Eventos (`DeterministicRulesFuzz250kTest.kt`):**
   - 125.000 iterações em Tales + 125.000 iterações em Flood com validação contínua de invariantes de regras canônicas (0 falhas).
5. **P4 — Suíte de Casos Negativos Inegociáveis (`NegativeCasesVerificationTest.kt`):**
   - Validação automatizada dos 6 casos negativos críticos descritos na especificação (JSON quebrado, firewall Teen, timeout de rede sem crash, integridade de legado e sanitização de áudio TTS).

---

## Verificação e Qualidade

- **164 Testes Unitários:** 100% aprovados (0 falhas, 0 erros).
- **250.000 Eventos de Fuzz:** Executados com sucesso.
- **66 Sessões de Simulação Completa:** 33 sessões Kids + 33 sessões Teens aprovadas.
- **Assinatura:** APK Signature Scheme v2 verificado pelo `apksigner`.
