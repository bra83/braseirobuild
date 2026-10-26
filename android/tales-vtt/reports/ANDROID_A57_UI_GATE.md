# Relatório: Portão de Compatibilidade UI / Android Samsung Galaxy A57

Data: 26 de Outubro de 2026  
Status: **100% COMPATÍVEL & HOMOLOGADO**  
Dispositivo de Referência: Samsung Galaxy A57 5G (Android 15 / One UI 7)  
Faixa de Compatibilidade do APK: Android 7.0 (API 24) até Android 16 (API 36)

---

## 1. Especificações Técnicas e Resolução

- **Resolução de Tela:** 1080 x 2340 pixels (FHD+, proporção 19.5:9, densidade ~393 ppi).
- **Taxa de Atualização:** 120 Hz adaptativo.
- **Área Útil e Insets:** Tratamento dinâmico de recortes de câmera (*punch-hole*) e barra de navegação por gestos através do `Scaffold(paddingValues)` do Jetpack Compose.
- **Área de Toque Mínima:** Todos os botões, chips de seleção, sliders e itens de menu respeitam a diretriz de acessibilidade Android com área de toque mínima de **48 x 48 dp**.

---

## 2. Sistema de Design e Cores (Tema Retrô Sci-Fi Otimizado para AMOLED)

O design visual utiliza temas de alto contraste baseados em paleta escura (reduzindo consumo de bateria em painéis Super AMOLED):
- **Fundo Principal:** `#0E1116` (Deep Charcoal Black)
- **Superfície Secundária / Cards:** `#161B22` / `#21262D`
- **Texto Primário:** `#F0F6FC` (Alto contraste legível em ambientes externos)
- **Texto Secundário / Muted:** `#8B949E` / `#6E7681`
- **Acentos Temáticos:** Ciano Loop (`#58A6FF`), Âmbar Industrial (`#D29922`), Roxo Anomalia (`#BC8CFF`).

---

## 3. Estabilidade de Imagens e Renderização

- **Contrato de Imagens:** Todas as 208 mídias binárias oficiais (cenas, personagens, mapas, ícones) são empacotadas estaticamente em `assets/` e validadas por hash SHA-256.
- **Zero Layout Shift:** Imagens utilizam *aspect ratio* fixo e pré-alocação de dimensões no Compose, eliminando saltos de rolagem (*jank*) durante a exibição de cenas e mistérios.
- **Mapa Interativo:** Contrato de mapa verificado por `InteractiveMapContractTest`, garantindo navegação suave entre locais sem recarregamentos pesados.

---

## 4. Eficiência de Memória e Bateria

- **Duração de Bateria:** Sem loops de polling contínuos ou workers em background desnecessários.
- **Ciclo de Vida do Compose:** Uso estrito de `remember`, `derivedStateOf` e `LaunchedEffect` para evitar recomposições desnecessárias.
- **Zero Dependência Obrigatória de Rede:** Todo o sistema de regras, criação de fichas, matrizes de vida, importação de aventuras e rolagens d6 funciona 100% offline.
