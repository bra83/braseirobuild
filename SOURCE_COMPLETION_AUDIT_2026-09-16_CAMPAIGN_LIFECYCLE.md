# WWN — fechamento transversal: ciclo de campanhas

- Adicionado gerenciamento de múltiplas campanhas no produto: listar, criar e ativar.
- Nova campanha nasce com estado WWN isolado e segue para criação do primeiro personagem.
- IDs duplicados recebem sufixo seguro; nenhuma campanha existente é sobrescrita.
- Troca de campanha usa o mesmo active campaign contract do runtime Android.
- Backup/restauração e ficha portátil permanecem separados do lifecycle de campanha.
- Gradle/APK não foram usados como critério de fechamento.
- QA acumulado: 21 suítes `.mjs`, 0 falhas; 145 arquivos JS, 0 falhas de sintaxe.
