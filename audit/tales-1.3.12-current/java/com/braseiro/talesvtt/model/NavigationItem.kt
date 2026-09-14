package com.braseiro.talesvtt.model

enum class AppDestination(val route: String, val title: String) {
  SCENE("scene", "CENA"),
  KID("kid", "KID"),
  GROUP("group", "GRUPO"),
  MYSTERY("mystery", "MISTÉRIO"),
  MAP("map", "MAPA"),
  BACKPACK("backpack", "MOCHILA"),
  MENU("menu", "MENU")
}
