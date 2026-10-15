package com.braseiro.dnd5e;
import java.util.*;import org.json.*;
/** Source-bound XMM multiattack selection plans, not attack or effect execution. */
public final class Xmm2025MultiattackPlanIndex {
 public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
 public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";
 private static final String[] RAW={
  "{\"id\":\"Aarakocra Aeromancer\",\"ptr\":\"/monster/0/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Aberrant Cultist\",\"ptr\":\"/monster/2/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Aboleth\",\"ptr\":\"/monster/3/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Abominable Yeti\",\"ptr\":\"/monster/4/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\",\"Ice Throw\"],\"c\":2}]]}",
  "{\"id\":\"Adult Black Dragon\",\"ptr\":\"/monster/5/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Blue Dragon\",\"ptr\":\"/monster/6/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Brass Dragon\",\"ptr\":\"/monster/7/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Bronze Dragon\",\"ptr\":\"/monster/8/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Copper Dragon\",\"ptr\":\"/monster/9/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Gold Dragon\",\"ptr\":\"/monster/10/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Green Dragon\",\"ptr\":\"/monster/11/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Red Dragon\",\"ptr\":\"/monster/12/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult Silver Dragon\",\"ptr\":\"/monster/13/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Adult White Dragon\",\"ptr\":\"/monster/14/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Air Elemental\",\"ptr\":\"/monster/15/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Thunderous Slam\"],\"c\":2}]]}",
  "{\"id\":\"Ancient Black Dragon\",\"ptr\":\"/monster/17/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Blue Dragon\",\"ptr\":\"/monster/18/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Brass Dragon\",\"ptr\":\"/monster/19/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Bronze Dragon\",\"ptr\":\"/monster/20/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Copper Dragon\",\"ptr\":\"/monster/21/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Gold Dragon\",\"ptr\":\"/monster/22/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Green Dragon\",\"ptr\":\"/monster/23/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Red Dragon\",\"ptr\":\"/monster/24/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient Silver Dragon\",\"ptr\":\"/monster/25/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ancient White Dragon\",\"ptr\":\"/monster/26/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Animal Lord\",\"ptr\":\"/monster/27/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\",\"Radiant Ray\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Animal Spirit\"],\"c\":1}]]}",
  "{\"id\":\"Animated Armor\",\"ptr\":\"/monster/28/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":2}]]}",
  "{\"id\":\"Ankylosaurus\",\"ptr\":\"/monster/33/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Tail\"],\"c\":2}]]}",
  "{\"id\":\"Ape\",\"ptr\":\"/monster/34/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Fist\"],\"c\":2}]]}",
  "{\"id\":\"Arcanaloth\",\"ptr\":\"/monster/35/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Arch-hag\",\"ptr\":\"/monster/36/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Spectral Claw\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Crackling Wave\"],\"c\":1}]]}",
  "{\"id\":\"Archelon\",\"ptr\":\"/monster/37/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Archmage\",\"ptr\":\"/monster/38/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Arcane Burst\"],\"c\":4}]]}",
  "{\"id\":\"Archpriest\",\"ptr\":\"/monster/39/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Radiant Burst\"],\"c\":3}]]}",
  "{\"id\":\"Assassin\",\"ptr\":\"/monster/40/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Shortsword\",\"Light Crossbow\"],\"c\":3}]]}",
  "{\"id\":\"Azer Pyromancer\",\"ptr\":\"/monster/44/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Flame Burst\"],\"c\":2}]]}",
  "{\"id\":\"Balor\",\"ptr\":\"/monster/48/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Flame Whip\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Lightning Blade\"],\"c\":1}]]}",
  "{\"id\":\"Bandit Captain\",\"ptr\":\"/monster/50/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Scimitar\",\"Pistol\"],\"c\":2}]]}",
  "{\"id\":\"Bandit Crime Lord\",\"ptr\":\"/monster/51/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Scimitar\",\"Pistol\"],\"c\":3}]]}",
  "{\"id\":\"Bandit Deceiver\",\"ptr\":\"/monster/52/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Dagger\"],\"c\":3}]]}",
  "{\"id\":\"Banshee\",\"ptr\":\"/monster/53/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Corrupting Touch\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Horrify\"],\"c\":1}]]}",
  "{\"id\":\"Barbed Devil\",\"ptr\":\"/monster/54/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Barlgura\",\"ptr\":\"/monster/55/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Tormenting Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Thrash\"],\"c\":2}]]}",
  "{\"id\":\"Bearded Devil\",\"ptr\":\"/monster/58/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Beard\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Infernal Glaive\"],\"c\":1}]]}",
  "{\"id\":\"Behir\",\"ptr\":\"/monster/59/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"use\",\"o\":[\"Constrict\"],\"c\":1}]]}",
  "{\"id\":\"Beholder\",\"ptr\":\"/monster/60/action/0/entries\",\"v\":[[{\"k\":\"use\",\"o\":[\"Eye Rays\"],\"c\":3}]]}",
  "{\"id\":\"Beholder Zombie\",\"ptr\":\"/monster/61/action/0/entries\",\"v\":[[{\"k\":\"use\",\"o\":[\"Eye Rays\"],\"c\":2}]]}",
  "{\"id\":\"Berserker Commander\",\"ptr\":\"/monster/63/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Greataxe\",\"Javelin\"],\"c\":3}]]}",
  "{\"id\":\"Black Bear\",\"ptr\":\"/monster/64/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Black Dragon Wyrmling\",\"ptr\":\"/monster/65/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Blob of Annihilation\",\"ptr\":\"/monster/68/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Blue Dragon Wyrmling\",\"ptr\":\"/monster/70/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Blue Slaad\",\"ptr\":\"/monster/71/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Mutating Claw\"],\"c\":3}]]}",
  "{\"id\":\"Bone Devil\",\"ptr\":\"/monster/73/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2},{\"k\":\"attack\",\"o\":[\"Infernal Sting\"],\"c\":1}]]}",
  "{\"id\":\"Bone Naga\",\"ptr\":\"/monster/74/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Brazen Gorgon\",\"ptr\":\"/monster/76/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Gore\"],\"c\":2}]]}",
  "{\"id\":\"Bronze Dragon Wyrmling\",\"ptr\":\"/monster/77/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Brown Bear\",\"ptr\":\"/monster/78/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":1}]]}",
  "{\"id\":\"Bugbear Stalker\",\"ptr\":\"/monster/79/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Javelin\",\"Morningstar\"],\"c\":2}]]}",
  "{\"id\":\"Bulette\",\"ptr\":\"/monster/81/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Bullywug Bog Sage\",\"ptr\":\"/monster/83/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Cambion\",\"ptr\":\"/monster/85/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\",\"Fire Ray\"],\"c\":2}]]}",
  "{\"id\":\"Carrion Crawler\",\"ptr\":\"/monster/87/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1}]]}",
  "{\"id\":\"Centaur Trooper\",\"ptr\":\"/monster/89/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Pike\",\"Longbow\"],\"c\":2}]]}",
  "{\"id\":\"Centaur Warden\",\"ptr\":\"/monster/90/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Forest Staff\",\"Sun Ray\"],\"c\":2}]]}",
  "{\"id\":\"Chain Devil\",\"ptr\":\"/monster/91/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Chain\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Conjure Infernal Chain\"],\"c\":1}]]}",
  "{\"id\":\"Chimera\",\"ptr\":\"/monster/93/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Chuul\",\"ptr\":\"/monster/94/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Pincer\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Paralyzing Tentacles\"],\"c\":1}]]}",
  "{\"id\":\"Clay Golem\",\"ptr\":\"/monster/95/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Cloaker\",\"ptr\":\"/monster/96/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Attach\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tail\"],\"c\":2}]]}",
  "{\"id\":\"Cloud Giant\",\"ptr\":\"/monster/97/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Cockatrice Regent\",\"ptr\":\"/monster/99/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Petrifying Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Talons\"],\"c\":2}]]}",
  "{\"id\":\"Colossus\",\"ptr\":\"/monster/100/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\",\"Radiant Ray\"],\"c\":3}]]}",
  "{\"id\":\"Cultist Hierophant\",\"ptr\":\"/monster/110/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Pact Blade\",\"Radiant Ray\"],\"c\":3}]]}",
  "{\"id\":\"Cyclops Oracle\",\"ptr\":\"/monster/111/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Radiant Strike\",\"Flash of Light\"],\"c\":3}]]}",
  "{\"id\":\"Cyclops Sentry\",\"ptr\":\"/monster/112/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Stone Club\",\"Rock\"],\"c\":2}]]}",
  "{\"id\":\"Dao\",\"ptr\":\"/monster/113/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Earthen Maul\"],\"c\":3}],[{\"k\":\"attack\",\"o\":[\"Earth Burst\"],\"c\":2}]]}",
  "{\"id\":\"Death Cultist\",\"ptr\":\"/monster/115/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Dread Scythe\",\"Deathly Ray\"],\"c\":3}]]}",
  "{\"id\":\"Death Dog\",\"ptr\":\"/monster/116/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Death Knight\",\"ptr\":\"/monster/117/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Dread Blade\"],\"c\":3}]]}",
  "{\"id\":\"Death Knight Aspirant\",\"ptr\":\"/monster/118/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Dread Blade\"],\"c\":3}]]}",
  "{\"id\":\"Death Slaad\",\"ptr\":\"/monster/119/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Chaos Blade\"],\"c\":2}]]}",
  "{\"id\":\"Death Tyrant\",\"ptr\":\"/monster/120/action/0/entries\",\"v\":[[{\"k\":\"use\",\"o\":[\"Eye Rays\"],\"c\":3}]]}",
  "{\"id\":\"Demilich\",\"ptr\":\"/monster/122/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Necrotic Burst\"],\"c\":3}]]}",
  "{\"id\":\"Deva\",\"ptr\":\"/monster/123/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Holy Mace\"],\"c\":2}]]}",
  "{\"id\":\"Dire Worg\",\"ptr\":\"/monster/125/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":3}]]}",
  "{\"id\":\"Displacer Beast\",\"ptr\":\"/monster/126/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tentacle\"],\"c\":1}]]}",
  "{\"id\":\"Djinni\",\"ptr\":\"/monster/127/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Storm Blade\",\"Storm Bolt\"],\"c\":3}]]}",
  "{\"id\":\"Doppelganger\",\"ptr\":\"/monster/128/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Dracolich\",\"ptr\":\"/monster/129/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Dragon Turtle\",\"ptr\":\"/monster/131/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Drider\",\"ptr\":\"/monster/133/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Foreleg\",\"Poison Burst\"],\"c\":3}]]}",
  "{\"id\":\"Druid\",\"ptr\":\"/monster/134/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Vine Staff\",\"Verdant Wisp\"],\"c\":2}]]}",
  "{\"id\":\"Dryad\",\"ptr\":\"/monster/135/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Earth Elemental\",\"ptr\":\"/monster/138/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\",\"Rock Launch\"],\"c\":2}]]}",
  "{\"id\":\"Efreeti\",\"ptr\":\"/monster/139/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Heated Blade\",\"Hurl Flame\"],\"c\":3}]]}",
  "{\"id\":\"Elemental Cataclysm\",\"ptr\":\"/monster/140/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Elemental Burst\"],\"c\":2}]]}",
  "{\"id\":\"Elemental Cultist\",\"ptr\":\"/monster/141/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Elemental Flail\",\"Elemental Claw\"],\"c\":3}]]}",
  "{\"id\":\"Elephant\",\"ptr\":\"/monster/142/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Gore\"],\"c\":2}]]}",
  "{\"id\":\"Empyrean\",\"ptr\":\"/monster/144/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Sacred Weapon\",\"Divine Ray\"],\"c\":2}]]}",
  "{\"id\":\"Erinyes\",\"ptr\":\"/monster/146/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ettercap\",\"ptr\":\"/monster/147/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":1}]]}",
  "{\"id\":\"Ettin\",\"ptr\":\"/monster/148/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Battleaxe\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Morningstar\"],\"c\":1}]]}",
  "{\"id\":\"Fiend Cultist\",\"ptr\":\"/monster/151/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Pact Axe\"],\"c\":3}]]}",
  "{\"id\":\"Fire Elemental\",\"ptr\":\"/monster/152/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Burn\"],\"c\":2}]]}",
  "{\"id\":\"Fire Giant\",\"ptr\":\"/monster/153/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Flame Sword\",\"Hammer Throw\"],\"c\":2}]]}",
  "{\"id\":\"Flameskull\",\"ptr\":\"/monster/154/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Fire Ray\"],\"c\":2}]]}",
  "{\"id\":\"Flaming Skeleton\",\"ptr\":\"/monster/155/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Flame Scepter\",\"Hurl Flame\"],\"c\":2}]]}",
  "{\"id\":\"Flesh Golem\",\"ptr\":\"/monster/156/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":2}]]}",
  "{\"id\":\"Fomorian\",\"ptr\":\"/monster/159/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Frost Giant\",\"ptr\":\"/monster/161/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Frost Axe\",\"Great Bow\"],\"c\":2}]]}",
  "{\"id\":\"Gargoyle\",\"ptr\":\"/monster/163/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Ghast Gravecaller\",\"ptr\":\"/monster/167/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ghost\",\"ptr\":\"/monster/168/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Withering Touch\"],\"c\":2}]]}",
  "{\"id\":\"Ghoul\",\"ptr\":\"/monster/169/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Giant Ape\",\"ptr\":\"/monster/170/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Fist\"],\"c\":2}]]}",
  "{\"id\":\"Giant Axe Beak\",\"ptr\":\"/monster/171/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Sharpened Beak\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Talons\"],\"c\":1}]]}",
  "{\"id\":\"Giant Constrictor Snake\",\"ptr\":\"/monster/176/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"use\",\"o\":[\"Constrict\"],\"c\":1}]]}",
  "{\"id\":\"Giant Crocodile\",\"ptr\":\"/monster/178/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tail\"],\"c\":1}]]}",
  "{\"id\":\"Giant Eagle\",\"ptr\":\"/monster/179/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Giant Scorpion\",\"ptr\":\"/monster/189/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2},{\"k\":\"attack\",\"o\":[\"Sting\"],\"c\":1}]]}",
  "{\"id\":\"Giant Shark\",\"ptr\":\"/monster/191/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Giant Squid\",\"ptr\":\"/monster/193/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tentacle\"],\"c\":1}]]}",
  "{\"id\":\"Githyanki Dracomancer\",\"ptr\":\"/monster/201/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Draconic Strike\"],\"c\":3}]]}",
  "{\"id\":\"Githyanki Knight\",\"ptr\":\"/monster/202/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Githyanki Warrior\",\"ptr\":\"/monster/203/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Psi Blade\"],\"c\":2}]]}",
  "{\"id\":\"Githzerai Monk\",\"ptr\":\"/monster/204/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Psi Strike\"],\"c\":2}]]}",
  "{\"id\":\"Githzerai Psion\",\"ptr\":\"/monster/205/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Psychic Warp\"],\"c\":3}]]}",
  "{\"id\":\"Githzerai Zerth\",\"ptr\":\"/monster/206/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Psi Strike\"],\"c\":2}]]}",
  "{\"id\":\"Glabrezu\",\"ptr\":\"/monster/207/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Gladiator\",\"ptr\":\"/monster/208/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Gnoll Demoniac\",\"ptr\":\"/monster/209/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Abyssal Strike\"],\"c\":2}]]}",
  "{\"id\":\"Gnoll Fang of Yeenoghu\",\"ptr\":\"/monster/210/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Bone Flail\"],\"c\":2}]]}",
  "{\"id\":\"Gnoll Pack Lord\",\"ptr\":\"/monster/211/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Goblin Boss\",\"ptr\":\"/monster/214/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Scimitar\",\"Shortbow\"],\"c\":2}]]}",
  "{\"id\":\"Goblin Hexer\",\"ptr\":\"/monster/215/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Gold Dragon Wyrmling\",\"ptr\":\"/monster/218/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Goristro\",\"ptr\":\"/monster/220/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Brutal Gore\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":2}]]}",
  "{\"id\":\"Graveyard Revenant\",\"ptr\":\"/monster/221/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Suffocate\"],\"c\":2}]]}",
  "{\"id\":\"Gray Slaad\",\"ptr\":\"/monster/223/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Chaos Claw\"],\"c\":2}]]}",
  "{\"id\":\"Green Dragon Wyrmling\",\"ptr\":\"/monster/224/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Green Hag\",\"ptr\":\"/monster/225/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Green Slaad\",\"ptr\":\"/monster/226/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Chaos Staff\"],\"c\":3}]]}",
  "{\"id\":\"Grell\",\"ptr\":\"/monster/227/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Beak\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Paralyzing Tentacles\"],\"c\":1}]]}",
  "{\"id\":\"Grick\",\"ptr\":\"/monster/228/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Beak\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tentacles\"],\"c\":1}]]}",
  "{\"id\":\"Grick Ancient\",\"ptr\":\"/monster/229/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Beak\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tentacles\"],\"c\":1}]]}",
  "{\"id\":\"Griffon\",\"ptr\":\"/monster/230/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Guard Captain\",\"ptr\":\"/monster/233/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Javelin\",\"Longsword\"],\"c\":2}]]}",
  "{\"id\":\"Guardian Naga\",\"ptr\":\"/monster/234/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Gulthias Blight\",\"ptr\":\"/monster/235/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Half-Dragon\",\"ptr\":\"/monster/236/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Haunting Revenant\",\"ptr\":\"/monster/238/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Object Slam\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Invitation\"],\"c\":1}]]}",
  "{\"id\":\"Hell Hound\",\"ptr\":\"/monster/240/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Helmed Horror\",\"ptr\":\"/monster/241/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Arcane Sword\"],\"c\":2}]]}",
  "{\"id\":\"Hezrou\",\"ptr\":\"/monster/242/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Hill Giant\",\"ptr\":\"/monster/243/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Tree Club\",\"Trash Lob\"],\"c\":2}]]}",
  "{\"id\":\"Hippogriff\",\"ptr\":\"/monster/244/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Hippopotamus\",\"ptr\":\"/monster/245/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Hobgoblin Captain\",\"ptr\":\"/monster/246/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Greatsword\",\"Longbow\"],\"c\":2}]]}",
  "{\"id\":\"Hobgoblin Warlord\",\"ptr\":\"/monster/247/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Javelin\",\"Longsword\"],\"c\":3}]]}",
  "{\"id\":\"Hook Horror\",\"ptr\":\"/monster/250/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Hook\"],\"c\":2}]]}",
  "{\"id\":\"Horned Devil\",\"ptr\":\"/monster/251/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Hydra\",\"ptr\":\"/monster/253/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Ice Devil\",\"ptr\":\"/monster/255/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Incubus\",\"ptr\":\"/monster/258/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Restless Touch\"],\"c\":2}]]}",
  "{\"id\":\"Intellect Devourer\",\"ptr\":\"/monster/259/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":1},{\"k\":\"use\",\"o\":[\"Devour Intellect\"],\"c\":1}]]}",
  "{\"id\":\"Invisible Stalker\",\"ptr\":\"/monster/260/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Iron Golem\",\"ptr\":\"/monster/261/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bladed Arm\",\"Fiery Bolt\"],\"c\":2}]]}",
  "{\"id\":\"Jackalwere\",\"ptr\":\"/monster/263/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Juvenile Shadow Dragon\",\"ptr\":\"/monster/264/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Knight\",\"ptr\":\"/monster/267/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Greatsword\",\"Heavy Crossbow\"],\"c\":2}]]}",
  "{\"id\":\"Kraken\",\"ptr\":\"/monster/269/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Kuo-toa Archpriest\",\"ptr\":\"/monster/271/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Strange Scepter\"],\"c\":3}]]}",
  "{\"id\":\"Kuo-toa Monitor\",\"ptr\":\"/monster/272/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bone Whip\"],\"c\":2}]]}",
  "{\"id\":\"Lacedon Ghoul\",\"ptr\":\"/monster/274/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Icy Bite\"],\"c\":2}]]}",
  "{\"id\":\"Lamia\",\"ptr\":\"/monster/275/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Lich\",\"ptr\":\"/monster/278/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Eldritch Burst\",\"Paralyzing Touch\"],\"c\":3}]]}",
  "{\"id\":\"Lion\",\"ptr\":\"/monster/279/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Lizardfolk Geomancer\",\"ptr\":\"/monster/281/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Earth Burst\"],\"c\":2}]]}",
  "{\"id\":\"Lizardfolk Sovereign\",\"ptr\":\"/monster/282/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Earthen Maul\"],\"c\":1}]]}",
  "{\"id\":\"Mage\",\"ptr\":\"/monster/283/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Arcane Burst\"],\"c\":3}]]}",
  "{\"id\":\"Mammoth\",\"ptr\":\"/monster/287/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Gore\"],\"c\":2}]]}",
  "{\"id\":\"Manticore\",\"ptr\":\"/monster/290/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\",\"Tail Spike\"],\"c\":3}]]}",
  "{\"id\":\"Marid\",\"ptr\":\"/monster/291/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Aquatic Lash\"],\"c\":3}]]}",
  "{\"id\":\"Marilith\",\"ptr\":\"/monster/292/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Pact Blade\"],\"c\":6},{\"k\":\"use\",\"o\":[\"Constrict\"],\"c\":1}]]}",
  "{\"id\":\"Medusa\",\"ptr\":\"/monster/294/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Merfolk Wavebender\",\"ptr\":\"/monster/296/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Aquatic Burst\"],\"c\":2}]]}",
  "{\"id\":\"Merrow\",\"ptr\":\"/monster/297/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Mezzoloth\",\"ptr\":\"/monster/298/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claws\",\"Mercurial Trident\"],\"c\":2}]]}",
  "{\"id\":\"Mind Flayer Arcanist\",\"ptr\":\"/monster/301/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Arcane Tentacles\"],\"c\":3}]]}",
  "{\"id\":\"Modron Duodrone\",\"ptr\":\"/monster/304/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Clockwork Blade\"],\"c\":2}]]}",
  "{\"id\":\"Modron Pentadrone\",\"ptr\":\"/monster/306/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":5}],[{\"k\":\"attack\",\"o\":[\"Electrical Discharge\"],\"c\":5}]]}",
  "{\"id\":\"Modron Quadrone\",\"ptr\":\"/monster/307/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":4}],[{\"k\":\"attack\",\"o\":[\"Gears Launcher\"],\"c\":4}]]}",
  "{\"id\":\"Modron Tridrone\",\"ptr\":\"/monster/308/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Clockwork Spear\"],\"c\":3}]]}",
  "{\"id\":\"Mummy\",\"ptr\":\"/monster/311/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rotting Fist\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Dreadful Glare\"],\"c\":1}]]}",
  "{\"id\":\"Mummy Lord\",\"ptr\":\"/monster/312/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Myconid Sovereign\",\"ptr\":\"/monster/314/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":1},{\"k\":\"use\",\"o\":[\"Pacifying Spores\"],\"c\":1}]]}",
  "{\"id\":\"Nalfeshnee\",\"ptr\":\"/monster/317/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Night Hag\",\"ptr\":\"/monster/319/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Noble Prodigy\",\"ptr\":\"/monster/322/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Beguiling Strike\"],\"c\":3}]]}",
  "{\"id\":\"Nothic\",\"ptr\":\"/monster/323/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Nycaloth\",\"ptr\":\"/monster/324/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Mercurial Axe\"],\"c\":2}]]}",
  "{\"id\":\"Oni\",\"ptr\":\"/monster/330/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Otyugh\",\"ptr\":\"/monster/331/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tentacle\"],\"c\":2}]]}",
  "{\"id\":\"Owlbear\",\"ptr\":\"/monster/333/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Performer Legend\",\"ptr\":\"/monster/337/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bejeweled Baton\"],\"c\":3}]]}",
  "{\"id\":\"Performer Maestro\",\"ptr\":\"/monster/338/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rapier\"],\"c\":3}]]}",
  "{\"id\":\"Peryton\",\"ptr\":\"/monster/339/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Gore\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Talons\"],\"c\":1}]]}",
  "{\"id\":\"Phase Spider\",\"ptr\":\"/monster/340/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Pirate\",\"ptr\":\"/monster/343/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Pirate Admiral\",\"ptr\":\"/monster/344/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Scimitar\",\"Pistol\"],\"c\":3}]]}",
  "{\"id\":\"Pirate Captain\",\"ptr\":\"/monster/345/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rapier\",\"Pistol\"],\"c\":3}]]}",
  "{\"id\":\"Pit Fiend\",\"ptr\":\"/monster/346/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Pixie Wonderbringer\",\"ptr\":\"/monster/348/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Faerie Dust\"],\"c\":2}]]}",
  "{\"id\":\"Planetar\",\"ptr\":\"/monster/349/action/0/entries\",\"v\":[[{\"k\":\"use\",\"o\":[\"Holy Burst\"],\"c\":2}]]}",
  "{\"id\":\"Polar Bear\",\"ptr\":\"/monster/351/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Poltergeist\",\"ptr\":\"/monster/352/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Object Slam\"],\"c\":1},{\"k\":\"use\",\"o\":[\"Telekinetic Thrust\"],\"c\":1}]]}",
  "{\"id\":\"Priest\",\"ptr\":\"/monster/354/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Mace\",\"Radiant Flame\"],\"c\":2}]]}",
  "{\"id\":\"Primeval Owlbear\",\"ptr\":\"/monster/356/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Ravage\"],\"c\":2}]]}",
  "{\"id\":\"Pseudodragon\",\"ptr\":\"/monster/357/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":2}]]}",
  "{\"id\":\"Purple Worm\",\"ptr\":\"/monster/360/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tail Stinger\"],\"c\":1}]]}",
  "{\"id\":\"Quaggoth\",\"ptr\":\"/monster/361/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Quaggoth Thonot\",\"ptr\":\"/monster/362/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Questing Knight\",\"ptr\":\"/monster/364/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Greatsword\",\"Longbow\"],\"c\":3}]]}",
  "{\"id\":\"Rakshasa\",\"ptr\":\"/monster/365/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Cursed Touch\"],\"c\":3}]]}",
  "{\"id\":\"Red Dragon Wyrmling\",\"ptr\":\"/monster/368/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Red Slaad\",\"ptr\":\"/monster/369/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Injecting Claw\"],\"c\":3}]]}",
  "{\"id\":\"Revenant\",\"ptr\":\"/monster/372/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":2}]]}",
  "{\"id\":\"Roc\",\"ptr\":\"/monster/375/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Roper\",\"ptr\":\"/monster/376/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Rust Monster\",\"ptr\":\"/monster/377/action/0/entries\",\"v\":[[{\"k\":\"use\",\"o\":[\"Antennae\"],\"c\":2}]]}",
  "{\"id\":\"Saber-Toothed Tiger\",\"ptr\":\"/monster/378/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Sahuagin Baron\",\"ptr\":\"/monster/379/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Trident\"],\"c\":3}]]}",
  "{\"id\":\"Sahuagin Priest\",\"ptr\":\"/monster/380/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Spectral Jaws\"],\"c\":2}]]}",
  "{\"id\":\"Sahuagin Warrior\",\"ptr\":\"/monster/381/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Salamander\",\"ptr\":\"/monster/382/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Salamander Inferno Master\",\"ptr\":\"/monster/384/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Flame Trident\"],\"c\":2}]]}",
  "{\"id\":\"Satyr Revelmaster\",\"ptr\":\"/monster/386/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Prance\"],\"c\":3}]]}",
  "{\"id\":\"Scout\",\"ptr\":\"/monster/389/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Shortsword\",\"Longbow\"],\"c\":2}]]}",
  "{\"id\":\"Scout Captain\",\"ptr\":\"/monster/390/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Shortsword\",\"Longbow\"],\"c\":2}]]}",
  "{\"id\":\"Shadow Dragon\",\"ptr\":\"/monster/395/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Shambling Mound\",\"ptr\":\"/monster/396/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Shield Guardian\",\"ptr\":\"/monster/397/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Fist\"],\"c\":2}]]}",
  "{\"id\":\"Silver Dragon Wyrmling\",\"ptr\":\"/monster/399/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Solar\",\"ptr\":\"/monster/403/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Spectator\",\"ptr\":\"/monster/404/action/0/entries\",\"v\":[[{\"k\":\"use\",\"o\":[\"Eye Rays\"],\"c\":2}]]}",
  "{\"id\":\"Sphinx of Lore\",\"ptr\":\"/monster/406/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":3}]]}",
  "{\"id\":\"Sphinx of Secrets\",\"ptr\":\"/monster/407/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":3}],[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2},{\"k\":\"action\",\"o\":[\"Curse of the Riddle\"],\"c\":1}],[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":1},{\"k\":\"action\",\"o\":[\"Curse of the Riddle\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":1}],[{\"k\":\"action\",\"o\":[\"Curse of the Riddle\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2}]]}",
  "{\"id\":\"Sphinx of Valor\",\"ptr\":\"/monster/408/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Spined Devil\",\"ptr\":\"/monster/411/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Infernal Fork\",\"Tail Spine\"],\"c\":2}]]}",
  "{\"id\":\"Spirit Naga\",\"ptr\":\"/monster/412/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\",\"Necrotic Ray\"],\"c\":3}]]}",
  "{\"id\":\"Spy Master\",\"ptr\":\"/monster/415/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rapier\",\"Hand Crossbow\"],\"c\":3}]]}",
  "{\"id\":\"Stone Giant\",\"ptr\":\"/monster/418/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Stone Club\",\"Boulder\"],\"c\":2}]]}",
  "{\"id\":\"Stone Golem\",\"ptr\":\"/monster/419/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\",\"Force Bolt\"],\"c\":2}]]}",
  "{\"id\":\"Storm Giant\",\"ptr\":\"/monster/420/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Storm Sword\",\"Thunderbolt\"],\"c\":2}]]}",
  "{\"id\":\"Succubus\",\"ptr\":\"/monster/421/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Swarm of Dretches\",\"ptr\":\"/monster/424/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Swarm of Lemures\",\"ptr\":\"/monster/427/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Vile Slime\"],\"c\":2}]]}",
  "{\"id\":\"Tarrasque\",\"ptr\":\"/monster/433/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Thri-kreen Marauder\",\"ptr\":\"/monster/434/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Gythka\",\"Chatkcha\"],\"c\":2}]]}",
  "{\"id\":\"Thri-kreen Psion\",\"ptr\":\"/monster/435/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Psionic Lance\"],\"c\":3}]]}",
  "{\"id\":\"Tough Boss\",\"ptr\":\"/monster/438/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Warhammer\",\"Heavy Crossbow\"],\"c\":2}]]}",
  "{\"id\":\"Treant\",\"ptr\":\"/monster/439/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":2}]]}",
  "{\"id\":\"Tree Blight\",\"ptr\":\"/monster/440/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Branch\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Grasping Root\"],\"c\":1}]]}",
  "{\"id\":\"Triceratops\",\"ptr\":\"/monster/441/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Gore\"],\"c\":2}]]}",
  "{\"id\":\"Troll\",\"ptr\":\"/monster/443/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Tyrannosaurus Rex\",\"ptr\":\"/monster/446/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Tail\"],\"c\":1}]]}",
  "{\"id\":\"Ultroloth\",\"ptr\":\"/monster/447/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Mercurial Whip\"],\"c\":2}]]}",
  "{\"id\":\"Umber Hulk\",\"ptr\":\"/monster/448/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Unicorn\",\"ptr\":\"/monster/449/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Hooves\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Radiant Horn\"],\"c\":1}]]}",
  "{\"id\":\"Vampire Familiar\",\"ptr\":\"/monster/451/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Umbral Dagger\"],\"c\":2}]]}",
  "{\"id\":\"Vampire Nightbringer\",\"ptr\":\"/monster/452/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Shadow Strike\"],\"c\":1}]]}",
  "{\"id\":\"Vampire Spawn\",\"ptr\":\"/monster/453/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":2},{\"k\":\"use\",\"o\":[\"Bite\"],\"c\":1}]]}",
  "{\"id\":\"Vampire Umbral Lord\",\"ptr\":\"/monster/454/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Grave Strike\",\"Sickening Ray\"],\"c\":2}]]}",
  "{\"id\":\"Violet Fungus\",\"ptr\":\"/monster/457/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rotting Touch\"],\"c\":2}]]}",
  "{\"id\":\"Violet Fungus Necrohulk\",\"ptr\":\"/monster/458/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rotting Slam\"],\"c\":2}]]}",
  "{\"id\":\"Vrock\",\"ptr\":\"/monster/459/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Shred\"],\"c\":2}]]}",
  "{\"id\":\"Warrior Commander\",\"ptr\":\"/monster/463/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Greatsword\",\"Longbow\"],\"c\":3}]]}",
  "{\"id\":\"Warrior Veteran\",\"ptr\":\"/monster/465/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Greatsword\",\"Heavy Crossbow\"],\"c\":2}]]}",
  "{\"id\":\"Water Elemental\",\"ptr\":\"/monster/466/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Slam\"],\"c\":2}]]}",
  "{\"id\":\"Werebear\",\"ptr\":\"/monster/469/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Wereboar\",\"ptr\":\"/monster/470/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Wererat\",\"ptr\":\"/monster/471/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Weretiger\",\"ptr\":\"/monster/472/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Werewolf\",\"ptr\":\"/monster/473/action/0/entries\",\"v\":[]}",
  "{\"id\":\"White Dragon Wyrmling\",\"ptr\":\"/monster/474/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":2}]]}",
  "{\"id\":\"Wight\",\"ptr\":\"/monster/475/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Wyvern\",\"ptr\":\"/monster/482/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Sting\"],\"c\":1}]]}",
  "{\"id\":\"Xorn\",\"ptr\":\"/monster/483/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Bite\"],\"c\":1},{\"k\":\"attack\",\"o\":[\"Claw\"],\"c\":3}]]}",
  "{\"id\":\"Yeti\",\"ptr\":\"/monster/484/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Claw\",\"Ice Throw\"],\"c\":2}]]}",
  "{\"id\":\"Yochlol\",\"ptr\":\"/monster/485/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Young Black Dragon\",\"ptr\":\"/monster/486/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Young Blue Dragon\",\"ptr\":\"/monster/487/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Young Brass Dragon\",\"ptr\":\"/monster/488/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Young Bronze Dragon\",\"ptr\":\"/monster/489/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Young Copper Dragon\",\"ptr\":\"/monster/490/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Young Gold Dragon\",\"ptr\":\"/monster/491/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Young Green Dragon\",\"ptr\":\"/monster/492/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Young Red Dragon\",\"ptr\":\"/monster/493/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Young Silver Dragon\",\"ptr\":\"/monster/495/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Young White Dragon\",\"ptr\":\"/monster/496/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Rend\"],\"c\":3}]]}",
  "{\"id\":\"Yuan-ti Abomination\",\"ptr\":\"/monster/497/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Yuan-ti Infiltrator\",\"ptr\":\"/monster/498/action/0/entries\",\"v\":[[{\"k\":\"attack\",\"o\":[\"Scimitar\"],\"c\":2}]]}",
  "{\"id\":\"Yuan-ti Malison (Type 1)\",\"ptr\":\"/monster/499/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Yuan-ti Malison (Type 2)\",\"ptr\":\"/monster/500/action/0/entries\",\"v\":[]}",
  "{\"id\":\"Yuan-ti Malison (Type 3)\",\"ptr\":\"/monster/501/action/0/entries\",\"v\":[]}"
 };
 private static final Map<String,JSONObject> MAP=new LinkedHashMap<>();
 static{try{int ready=0;for(String s:RAW){JSONObject x=new JSONObject(s);if(MAP.put(x.getString("id"),x)!=null)throw new IllegalStateException("duplicate multiattack");if(x.getJSONArray("v").length()>0)ready++;}if(MAP.size()!=306||ready!=225)throw new IllegalStateException("incomplete multiattack index");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 private Xmm2025MultiattackPlanIndex(){}
 private static JSONObject blocked(String reason)throws Exception{return new JSONObject().put("resolved",false).put("status",reason).put("auto_rolls",false).put("auto_mutation_allowed",false).put("actions_applied",false);}
 public static JSONArray ids(){JSONArray a=new JSONArray();for(String id:MAP.keySet())a.put(id);return a;}
 public static JSONObject plan(String id)throws Exception{
  JSONObject r=MAP.get(id);if(r==null)return blocked("source_multiattack_not_registered");
  return new JSONObject(r.toString()).put("resolved",true).put("structured",r.getJSONArray("v").length()>0).put("source","XMM").put("source_blob_sha1",SOURCE_BLOB).put("auto_rolls",false).put("actions_applied",false).put("auto_mutation_allowed",false);
 }
 public static JSONObject resolve(JSONObject in)throws Exception{
  if(in==null)return blocked("missing_request");String id=in.optString("monster");JSONObject r=MAP.get(id);
  if(r==null)return blocked("source_multiattack_not_registered");
  JSONArray v=r.getJSONArray("v");if(v.length()==0)return blocked("complex_multiattack_requires_manual_source_resolution").put("monster",id).put("source_pointer",r.getString("ptr"));
  if(v.length()>1&&!in.has("variant_index"))return blocked("explicit_variant_selection_required");
  if(in.has("variant_index")&&!(in.opt("variant_index") instanceof Integer))return blocked("explicit_integer_variant_index_required");
  int variant=in.optInt("variant_index",0);if(variant<0||variant>=v.length())return blocked("invalid_variant_index");
  JSONArray groups=v.getJSONArray(variant),chosen=in.optJSONArray("selected_actions");int total=0;boolean ambiguous=false;
  for(int i=0;i<groups.length();i++){JSONObject g=groups.getJSONObject(i);total+=g.getInt("c");if(g.getJSONArray("o").length()>1)ambiguous=true;}
  if(chosen==null&&ambiguous)return blocked("explicit_attack_choices_required").put("required_slots",total);
  if(chosen!=null&&chosen.length()!=total)return blocked("attack_choice_count_mismatch").put("required_slots",total);
  if(in.has("selected_actions")&&chosen==null)return blocked("selected_actions_must_be_array");
  JSONArray outcomes=in.optJSONArray("step_outcomes");
  if(in.has("step_outcomes")&&outcomes==null)return blocked("step_outcomes_must_be_array");
  if(outcomes!=null&&outcomes.length()!=total)return blocked("step_outcome_count_mismatch").put("required_slots",total);
  if(!CompiledStatblockEngine.has(id))Xmm2025CompiledBestiaryEngine.install();
  JSONObject installed=CompiledStatblockEngine.get(id);if(installed.length()==0)return blocked("compiled_monster_not_installed");
  JSONArray actions=installed.getJSONArray("actions"),steps=new JSONArray();int slot=0,attacks=0,uses=0;
  for(int i=0;i<groups.length();i++){JSONObject g=groups.getJSONObject(i);JSONArray opts=g.getJSONArray("o");String kind=g.getString("k");
   for(int j=0;j<g.getInt("c");j++){
    if(chosen!=null&&!(chosen.opt(slot) instanceof String))return blocked("explicit_attack_name_required").put("slot",slot);
    String name=chosen==null?opts.getString(0):chosen.getString(slot);boolean allowed=false,registered=false;
    for(int k=0;k<opts.length();k++)if(name.equals(opts.getString(k)))allowed=true;
    if(!allowed)return blocked("attack_choice_not_allowed").put("slot",slot);
    for(int k=0;k<actions.length();k++){
     JSONObject compiledAction=actions.getJSONObject(k);
     if(name.equals(compiledAction.optString("name"))&&compiledAction.optBoolean("source_bound",false)
       &&(!"attack".equals(kind)||compiledAction.optJSONArray("attack_modes")!=null))registered=true;
    }
    if(!registered)return blocked("attack_choice_not_in_compiled_statblock").put("slot",slot);
    JSONObject step=new JSONObject().put("slot",slot).put("kind",kind).put("action",name).put("manual_effect_resolution_required",true);
    if("attack".equals(kind)){
     JSONObject rider=Xmm2025AttackConditionEngine.profile(id,name);
     if(rider.length()>0){
      JSONObject contract=new JSONObject().put("condition",rider.getString("condition"))
       .put("source_pointer",rider.getString("ptr")).put("source_blob_sha1",Xmm2025AttackConditionEngine.SOURCE_BLOB)
       .put("target_max_size",rider.getString("size")).put("requires_explicit_hit_and_target",true)
       .put("additional_source_effects_manual",rider.getBoolean("other")).put("condition_applied",false);
      if("grappled".equals(rider.getString("condition")))contract.put("escape_dc",rider.getInt("dc"));
      step.put("on_hit_condition_contract",contract);
      if(outcomes!=null){
       JSONObject outcome=outcomes.optJSONObject(slot);
       if(outcome==null)return blocked("explicit_step_outcome_required").put("slot",slot);
       JSONObject request=new JSONObject(outcome.toString()).put("monster",id).put("action",name);
       JSONObject resolvedRider=Xmm2025AttackConditionEngine.resolve(request);
       if(!resolvedRider.optBoolean("resolved",false))return blocked("rider_step_unresolved").put("slot",slot)
        .put("reason_detail",resolvedRider.optString("reason"));
       step.put("on_hit_condition_result",resolvedRider);
      }
     }
    }
    steps.put(step);slot++;
    if("attack".equals(kind))attacks++;else uses++;
   }
  }
  return new JSONObject().put("resolved",true).put("status","source_sequence_validated_pending_manual_effect_resolution").put("monster",id).put("source","XMM").put("source_pointer",r.getString("ptr")).put("source_blob_sha1",SOURCE_BLOB).put("variant_index",variant).put("steps",steps).put("attack_slots",attacks).put("use_slots",uses).put("auto_rolls",false).put("auto_mutation_allowed",false).put("actions_applied",false).put("damage_applied",false).put("turn_advanced",false).put("manual_effect_resolution_required",true);
 }
 public static JSONObject coverage()throws Exception{return new JSONObject().put("source","XMM").put("multiattacks",306).put("structured_plans",225).put("manual_source_bound",81).put("source_blob_sha1",SOURCE_BLOB).put("full_combat_automation_certified",false).put("auto_rolls",false);}
}
