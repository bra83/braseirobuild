# Relatório E2E: Importação Universal de Aventuras (braseiro.adventure/v1)

Data: 26 de Outubro de 2026  
Status: **100% IMPLEMENTADO & APROVADO**  
Componentes: `AdventureModels.kt`, `AdventureRepository.kt`, `AdventuresScreen.kt`, `GameViewModel.kt`

---

## 1. O Formato Universal `braseiro.adventure/v1`

O VTT adota o schema aberto `braseiro.adventure/v1`, permitindo importar campanhas completas para Tales from the Loop ou Things from the Flood geradas externamente ou pela comunidade.

### Estrutura do Pacote:
```json
{
  "schema": "braseiro.adventure/v1",
  "id": "misterio-estacao-perdida",
  "system": "tales-kids",
  "worldId": "malaren-islands",
  "title": "A Estação Perdida da Särö",
  "author": "Comunidade / IA Mestre",
  "description": "Um mistério envolvendo cabos de alta tensão que desaparecem na floresta.",
  "start": {
    "locationId": "escola",
    "hook": "Bicicletas começam a atrair estática inexplicável após a aula de física."
  },
  "story": {
    "premise": "Crianças investigam um gerador clandestino construído com restos do acelerador.",
    "background": "[RESERVADO AO MESTRE] O gerador atrai criaturas da dimensão de transição.",
    "playerIntroduction": "Uma névoa metálica cobre os trilhos velhos perto do lago."
  },
  "locations": [
    { "id": "escola", "name": "Escola Mälaren", "type": "school" },
    { "id": "estacao_velha", "name": "Antiga Estação de Força", "type": "industrial" }
  ],
  "npcs": [
    {
      "id": "zelador_lars",
      "name": "Lars Lindqvist",
      "role": "Zelador da Subestação",
      "goals": ["Esconder as peças roubadas", "Evitar curiosos"]
    }
  ],
  "secrets": [
    {
      "id": "sec_01",
      "visibility": "gm_only",
      "truth": "O transformador não usa diesel, mas núcleos de gravítons."
    }
  ],
  "enemies": [],
  "encounters": [],
  "objectives": [
    { "id": "obj_01", "description": "Desligar o capacitor antes do anoitecer" }
  ]
}
```

---

## 2. Validação Rigorosa e Robustez

- **Classe:** `AdventurePackageValidator.kt`
- **Comportamento:** Valida sintaxe JSON, schema version (`braseiro.adventure/v1`), presença de campos obrigatórios (`id`, `title`, `system`, `start.locationId`).
- **Resiliência:** Em caso de erro, retorna `ValidationResult.Invalid(errors)` contendo a lista descritiva dos pontos inválidos, renderizados diretamente na interface do usuário sem quebrar ou fechar o app.

---

## 3. Firewall Anti-Spoiler

1. **Player UI:** Na tela `AdventuresScreen.kt`, a pré-visualização apresenta exclusivamente a premissa pública, autor, tags e locais conhecidos.
2. **Isolamento de Segredos:** Ao iniciar a aventura através do botão *Jogar Este Mistério*, o método `GameViewModel.startAdventureFromPackage()` extrai os segredos marcados como `gm_only` e os insere isoladamente em `GmPrivateState.worldFacts`.
3. **Mestre Dinâmico:** As informações privadas são consultadas exclusivamente pelo `MotorBarbaraFirewall` durante a resolução de ganchos narrativos e pistas, garantindo surpresa total para os jogadores.

---

## 4. Testes E2E e Resultados

- `AdventureAndLifePersistenceEngineTest.kt`:
  - `testAdventurePackageValidation_Valid`: Sucesso na decodificação e parsing.
  - `testAdventurePackageValidation_InvalidSchema`: Rejeição segura de esquemas incompatíveis.
  - `testAdventurePackageStorageAndRetrieval`: Persistência em disco, listagem e recuperação.
  - `testGmOnlySecretsDoNotLeak`: Comprovação de que segredos reservados ao Mestre não vazam para a camada pública de UI.
