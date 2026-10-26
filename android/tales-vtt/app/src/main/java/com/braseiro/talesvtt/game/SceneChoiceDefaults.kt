package com.braseiro.talesvtt.game

/**
 * Player-facing suggestions are conveniences, never mechanical commands.
 * They keep the narrative playable even when a provider omits optional choices.
 */
object SceneChoiceDefaults {
  fun standard(): List<SceneChoice> = listOf(
    SceneChoice(
      "OBSERVAR COM CALMA",
      "Parar por um instante e prestar atenção aos detalhes do ambiente antes de agir."
    ),
    SceneChoice(
      "EXAMINAR DE PERTO",
      "Aproximar-se do que parece mais estranho ou importante e investigar melhor."
    ),
    SceneChoice(
      "DECIDIR COM OS KIDS",
      "Trocar ideias com o grupo e escolher juntos uma abordagem para o próximo passo."
    )
  )
}
