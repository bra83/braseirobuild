# Testar a nova mesa universal (ao dispor de build Android)

1. Abrir **Aventura → MESA UNIVERSAL · GITHUB · JOGAR**.
2. Escolher Forbidden Lands / Year Zero, Star Wars narrativo ou D&D 5e; preencher personagem, atributos e recursos existentes em JSON (`{}` quando não se deseja adicionar valores).
3. Informar a ação e usar **Pedir cena ao Gemini**. É utilizada a chave Gemini já configurada no Braseiro.
4. Se vier pedido de teste, confirmar **Sucesso**, **Fracasso** ou enviar resultados explícitos dos dados em JSON. O aplicativo não inventa dados nem aplica HP ou inventário apenas porque o Mestre narrou.
5. Ao retomar a mesa, as cenas e os recibos confirmados são recarregados. O prompt seguinte inclui as decisões do jogador e evita nova rolagem do mesmo teste.
6. Para reabrir, selecionar novamente o mesmo sistema (uma ficha inicial persistente por perfil nesta etapa). A campanha D&D tradicional não é afetada.

**Atenção:** apenas o fluxo lógico passou por testes locais nesta entrega; ainda NÃO foi provado no Android real. A UI é um primeiro fluxo jogável sujeito a homologação, não VTT final. Consulte `CHECKPOINT_GITHUB_MODULOS_003.md`.
