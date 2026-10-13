package com.braseiro.dnd5e;
import java.util.*;
import org.json.*;

/** Source-pinned 2025 XMM traits. Explicit DM input only; no RNG or state mutation.
 * Indexed metadata is mechanical and contains no copyrighted trait prose.
 */
public final class Xmm2025TraitMechanicsEngine {
 public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
 public static final int VERIFIED_TRAITS=178,VERIFIED_FAMILIES=8;
 private static final String[] RAW={
  "{\"id\":\"Aboleth\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":2,\"ptr\":\"/monster/3/trait/2/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Black Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/5/trait/1/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Blue Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/6/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Brass Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/7/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Bronze Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/8/trait/1/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Copper Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/9/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Gold Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/10/trait/1/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Green Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/11/trait/1/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Red Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/12/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult Silver Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/13/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Adult White Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/14/trait/1/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Ancient Black Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/17/trait/1/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Blue Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/18/trait/0/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Brass Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/19/trait/0/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Bronze Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/20/trait/1/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Copper Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/21/trait/0/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Gold Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/22/trait/1/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Green Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/23/trait/1/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Red Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/24/trait/0/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient Silver Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/25/trait/0/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Ancient White Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/26/trait/1/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Animal Lord\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day)\",\"slot\":1,\"ptr\":\"/monster/27/trait/1/entries/0\",\"uses\":4,\"lair_uses\":4}",
  "{\"id\":\"Animal Lord\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":3,\"ptr\":\"/monster/27/trait/3/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Animated Broom\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/29/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Arcanaloth\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/35/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Arch-hag\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/36/trait/0/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Arch-hag\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/36/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Archmage\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/38/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Assassin\",\"kind\":\"Evasion\",\"trait\":\"Evasion\",\"slot\":0,\"ptr\":\"/monster/40/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Baboon\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/46/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Balor\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day)\",\"slot\":2,\"ptr\":\"/monster/48/trait/2/entries/0\",\"uses\":3,\"lair_uses\":3}",
  "{\"id\":\"Balor\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":3,\"ptr\":\"/monster/48/trait/3/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Bandit Crime Lord\",\"kind\":\"Evasion\",\"trait\":\"Evasion\",\"slot\":0,\"ptr\":\"/monster/51/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Barbed Devil\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":2,\"ptr\":\"/monster/54/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Bearded Devil\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/58/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Beholder\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/60/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Beholder Zombie\",\"kind\":\"Undead Fortitude\",\"trait\":\"Undead Fortitude\",\"slot\":0,\"ptr\":\"/monster/61/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Blob of Annihilation\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day)\",\"slot\":1,\"ptr\":\"/monster/68/trait/1/entries/0\",\"uses\":4,\"lair_uses\":4}",
  "{\"id\":\"Blob of Annihilation\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":2,\"ptr\":\"/monster/68/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Blood Hawk\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/69/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Blue Slaad\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/71/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Bone Devil\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/73/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Chain Devil\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/91/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Chasme\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/92/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Clay Golem\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":3,\"ptr\":\"/monster/95/trait/3/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Cockatrice Regent\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/99/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Colossus\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day)\",\"slot\":1,\"ptr\":\"/monster/100/trait/1/entries/0\",\"uses\":4,\"lair_uses\":4}",
  "{\"id\":\"Colossus\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":2,\"ptr\":\"/monster/100/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Colossus\",\"kind\":\"Siege Monster\",\"trait\":\"Siege Monster\",\"slot\":3,\"ptr\":\"/monster/100/trait/3/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Dao\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":2,\"ptr\":\"/monster/113/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Death Knight\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day)\",\"slot\":0,\"ptr\":\"/monster/117/trait/0/entries/0\",\"uses\":3,\"lair_uses\":3}",
  "{\"id\":\"Death Knight\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/117/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Death Knight Aspirant\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/118/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Death Slaad\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/119/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Death Tyrant\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/120/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Demilich\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/122/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Deva\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/123/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Dire Wolf\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/124/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Dire Worg\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/125/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Djinni\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/127/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Dracolich\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/129/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Dracolich\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":2,\"ptr\":\"/monster/129/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Drider\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/133/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Dryad\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/135/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Earth Elemental\",\"kind\":\"Siege Monster\",\"trait\":\"Siege Monster\",\"slot\":1,\"ptr\":\"/monster/138/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Efreeti\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/139/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Elemental Cataclysm\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day)\",\"slot\":1,\"ptr\":\"/monster/140/trait/1/entries/0\",\"uses\":4,\"lair_uses\":4}",
  "{\"id\":\"Elemental Cataclysm\",\"kind\":\"Siege Monster\",\"trait\":\"Siege Monster\",\"slot\":2,\"ptr\":\"/monster/140/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Empyrean\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day)\",\"slot\":0,\"ptr\":\"/monster/144/trait/0/entries/0\",\"uses\":4,\"lair_uses\":4}",
  "{\"id\":\"Empyrean\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/144/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Empyrean Iota\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/145/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Erinyes\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/146/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Faerie Dragon Adult\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/149/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Faerie Dragon Youth\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/150/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Flameskull\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/154/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Flesh Golem\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":4,\"ptr\":\"/monster/156/trait/4/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Flying Snake\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/158/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Gargoyle\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/163/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Giant Owl\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/187/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Giant Rat\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/188/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Giant Vulture\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/196/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Giant Wasp\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/197/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Glabrezu\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/207/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Goristro\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/220/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Goristro\",\"kind\":\"Siege Monster\",\"trait\":\"Siege Monster\",\"slot\":2,\"ptr\":\"/monster/220/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Gray Slaad\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/223/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Green Slaad\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/226/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Hell Hound\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/240/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Helmed Horror\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/241/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Hezrou\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/242/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Hippogriff\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/244/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Hobgoblin Warrior\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/248/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Horned Devil\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/251/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Hyena\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/254/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Ice Devil\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/255/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Imp\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/257/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Iron Golem\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":2,\"ptr\":\"/monster/261/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Jackalwere\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/263/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Juvenile Shadow Dragon\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/264/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Kobold Warrior\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/268/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Kobold Warrior\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/268/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Kraken\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/269/trait/1/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Kraken\",\"kind\":\"Siege Monster\",\"trait\":\"Siege Monster\",\"slot\":2,\"ptr\":\"/monster/269/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Kuo-toa\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/270/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Kuo-toa Archpriest\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/271/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Kuo-toa Monitor\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/272/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Kuo-toa Whip\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/273/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Lich\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day, or 5/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/278/trait/0/entries/0\",\"uses\":4,\"lair_uses\":5}",
  "{\"id\":\"Lion\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/279/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Marilith\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/292/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Mezzoloth\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/298/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Mind Flayer\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/300/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Mind Flayer Arcanist\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/301/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Mummy Lord\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/312/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Mummy Lord\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/312/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Nalfeshnee\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/317/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Night Hag\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/319/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Nycaloth\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/324/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Ogre Zombie\",\"kind\":\"Undead Fortitude\",\"trait\":\"Undead Fortitude\",\"slot\":0,\"ptr\":\"/monster/328/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Owl\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/332/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Peryton\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/339/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Pit Fiend\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day)\",\"slot\":2,\"ptr\":\"/monster/346/trait/2/entries/0\",\"uses\":4,\"lair_uses\":4}",
  "{\"id\":\"Pit Fiend\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":3,\"ptr\":\"/monster/346/trait/3/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Pixie\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/347/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Pixie Wonderbringer\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/348/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Planetar\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":2,\"ptr\":\"/monster/349/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Primeval Owlbear\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/356/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Pseudodragon\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/357/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Pteranodon\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/359/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Quasit\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/363/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Red Slaad\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/369/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Reef Shark\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/370/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Salamander Inferno Master\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/384/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Satyr\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/385/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Satyr Revelmaster\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/386/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Shadow Dragon\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/395/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Shadow Dragon\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":2,\"ptr\":\"/monster/395/trait/2/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Slaad Tadpole\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/401/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Solar\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (4/Day)\",\"slot\":2,\"ptr\":\"/monster/403/trait/2/entries/0\",\"uses\":4,\"lair_uses\":4}",
  "{\"id\":\"Solar\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":3,\"ptr\":\"/monster/403/trait/3/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Specter\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/405/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Sphinx of Lore\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/406/trait/1/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Sphinx of Secrets\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/407/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Sphinx of Valor\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":1,\"ptr\":\"/monster/408/trait/1/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Sphinx of Wonder\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/409/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Spined Devil\",\"kind\":\"Flyby\",\"trait\":\"Flyby\",\"slot\":0,\"ptr\":\"/monster/411/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Spined Devil\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/411/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Stone Golem\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/419/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Tarrasque\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (6/Day)\",\"slot\":0,\"ptr\":\"/monster/433/trait/0/entries/0\",\"uses\":6,\"lair_uses\":6}",
  "{\"id\":\"Tarrasque\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/433/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Tarrasque\",\"kind\":\"Siege Monster\",\"trait\":\"Siege Monster\",\"slot\":3,\"ptr\":\"/monster/433/trait/3/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Tough\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/437/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Tough Boss\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/438/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Treant\",\"kind\":\"Siege Monster\",\"trait\":\"Siege Monster\",\"slot\":0,\"ptr\":\"/monster/439/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Troglodyte\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/442/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Twig Blight\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/445/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Ultroloth\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/447/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Unicorn\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day)\",\"slot\":0,\"ptr\":\"/monster/449/trait/0/entries/0\",\"uses\":3,\"lair_uses\":3}",
  "{\"id\":\"Unicorn\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/449/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Vampire\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/450/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Vampire Umbral Lord\",\"kind\":\"Legendary Resistance\",\"trait\":\"Legendary Resistance (3/Day, or 4/Day in Lair)\",\"slot\":0,\"ptr\":\"/monster/454/trait/0/entries/0\",\"uses\":3,\"lair_uses\":4}",
  "{\"id\":\"Vrock\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/459/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Vulture\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/460/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Warrior Infantry\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/464/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Werewolf\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/473/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Wight\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":0,\"ptr\":\"/monster/475/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Winged Kobold\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/477/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Winged Kobold\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/477/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Winter Wolf\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/478/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Wolf\",\"kind\":\"Pack Tactics\",\"trait\":\"Pack Tactics\",\"slot\":0,\"ptr\":\"/monster/479/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Wraith\",\"kind\":\"Sunlight Sensitivity\",\"trait\":\"Sunlight Sensitivity\",\"slot\":1,\"ptr\":\"/monster/481/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Yochlol\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":1,\"ptr\":\"/monster/485/trait/1/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Yuan-ti Abomination\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/497/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Yuan-ti Infiltrator\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/498/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Yuan-ti Malison (Type 1)\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/499/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Yuan-ti Malison (Type 2)\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/500/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Yuan-ti Malison (Type 3)\",\"kind\":\"Magic Resistance\",\"trait\":\"Magic Resistance\",\"slot\":0,\"ptr\":\"/monster/501/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}",
  "{\"id\":\"Zombie\",\"kind\":\"Undead Fortitude\",\"trait\":\"Undead Fortitude\",\"slot\":0,\"ptr\":\"/monster/502/trait/0/entries/0\",\"uses\":0,\"lair_uses\":0}"
 };
 private static final Map<String,JSONObject> INDEX=new LinkedHashMap<>();
 static{
  try{
   for(String raw:RAW){JSONObject r=new JSONObject(raw);
    if(INDEX.put(r.getString("id")+"::"+r.getString("trait"),r)!=null)
     throw new IllegalStateException("DUPLICATE_XMM_TRAIT");
   }
   if(INDEX.size()!=VERIFIED_TRAITS)throw new IllegalStateException("INCOMPLETE_XMM_TRAIT_INDEX");
  }catch(Exception e){throw new ExceptionInInitializerError(e);}
 }
 private Xmm2025TraitMechanicsEngine(){}
 public static JSONArray entries(){
  JSONArray a=new JSONArray();
  for(JSONObject r:INDEX.values()){
   try{a.put(new JSONObject(r.toString()));}catch(Exception e){throw new IllegalStateException(e);}
  }
  return a;
 }
 public static JSONObject profile(String id,String trait){
  JSONObject r=INDEX.get(id+"::"+trait);
  if(r==null)return new JSONObject();
  try{return new JSONObject(r.toString());}catch(Exception e){return new JSONObject();}
 }
 public static JSONObject coverage()throws Exception{
  JSONObject families=new JSONObject();
  for(JSONObject r:INDEX.values()){String k=r.getString("kind");families.put(k,families.optInt(k)+1);}
  return new JSONObject().put("source","XMM").put("verified_trait_contracts",INDEX.size())
   .put("verified_trait_families",families.length()).put("family_counts",families)
   .put("source_blob_sha1",SOURCE_BLOB).put("full_trait_effects_certified",false)
   .put("auto_rolls",false).put("auto_mutation_allowed",false);
 }
 private static boolean bool(JSONObject in,String key){
  if(!(in.opt(key) instanceof Boolean))throw new IllegalArgumentException("explicit_boolean_"+key+"_required");
  return in.optBoolean(key);
 }
 private static int integer(JSONObject in,String key,int min,int max){
  Object v=in.opt(key);
  if(!(v instanceof Number))throw new IllegalArgumentException("explicit_integer_"+key+"_required");
  double d=((Number)v).doubleValue();
  if(!Double.isFinite(d)||d!=Math.floor(d)||d<min||d>max)
   throw new IllegalArgumentException("invalid_integer_"+key);
  return (int)d;
 }
 private static String choice(JSONObject in,String key,String... allowed){
  Object v=in.opt(key);
  if(!(v instanceof String))throw new IllegalArgumentException("explicit_"+key+"_required");
  String s=((String)v).trim().toLowerCase(Locale.ROOT);
  for(String a:allowed)if(a.equals(s))return s;
  throw new IllegalArgumentException("invalid_"+key);
 }
 private static JSONObject blocked(String why)throws Exception{
  return new JSONObject().put("resolved",false).put("status","blocked").put("reason",why)
   .put("auto_rolls",false).put("auto_mutation_allowed",false).put("effect_applied",false);
 }
 public static JSONObject resolve(JSONObject in){
  try{
   if(in==null)return blocked("explicit_input_required");
   String id=in.optString("monster").trim(),trait=in.optString("trait").trim();
   JSONObject row=profile(id,trait);
   if(row.length()==0)return blocked("trait_not_in_verified_source_index");
   if(!CompiledStatblockEngine.has(id)&&
      !Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"))
    return blocked("statblock_not_authorized");
   JSONObject compiled=CompiledStatblockEngine.get(id);
   JSONArray traits=compiled.optJSONArray("traits");int slot=row.getInt("slot");
   if(traits==null||slot<0||slot>=traits.length()||
      !trait.equals(traits.getJSONObject(slot).optString("name")))
    return blocked("trait_not_bound_to_compiled_statblock");
   String kind=row.getString("kind");
   JSONObject out=new JSONObject().put("resolved",true)
    .put("status","trait_decision_pending_manual_application").put("source","XMM")
    .put("monster",id).put("trait",trait).put("kind",kind)
    .put("source_pointer",row.getString("ptr")).put("source_blob_sha1",SOURCE_BLOB)
    .put("auto_rolls",false).put("auto_mutation_allowed",false)
    .put("effect_applied",false).put("turn_advanced",false);
   if("Magic Resistance".equals(kind)){
    choice(in,"roll_kind","saving_throw");
    boolean active=bool(in,"spell_or_magical_effect");
    out.put("advantage_on_save",active).put("trigger_satisfied",active)
       .put("other_roll_modifiers_pending",true);
   }else if("Legendary Resistance".equals(kind)){
    boolean lair=bool(in,"in_lair"),failed=bool(in,"save_failed"),use=bool(in,"choose_to_use");
    int maximum=lair?row.getInt("lair_uses"):row.getInt("uses");
    int remaining=integer(in,"uses_remaining",0,maximum);
    if(use&&!failed)return blocked("cannot_spend_on_successful_save");
    if(use&&remaining==0)return blocked("legendary_resistance_exhausted");
    out.put("daily_maximum",maximum).put("uses_remaining_proposed",use?remaining-1:remaining)
       .put("save_success_proposed",use||!failed).put("resource_consumed",false)
       .put("trigger_satisfied",use);
   }else if("Pack Tactics".equals(kind)){
    boolean near=bool(in,"ally_within_5ft"),incapacitated=bool(in,"ally_incapacitated");
    boolean active=near&&!incapacitated;
    out.put("attack_advantage",active).put("trigger_satisfied",active)
       .put("other_roll_modifiers_pending",true);
   }else if("Sunlight Sensitivity".equals(kind)){
    boolean sun=bool(in,"in_sunlight");
    String roll=choice(in,"roll_kind","attack_roll","ability_check","saving_throw");
    boolean active=sun&&!"saving_throw".equals(roll);
    out.put("disadvantage",active).put("trigger_satisfied",active)
       .put("other_roll_modifiers_pending",true);
   }else if("Flyby".equals(kind)){
    boolean flight=bool(in,"flight_used"),leaving=bool(in,"leaving_enemy_reach");
    boolean active=flight&&leaving;
    out.put("opportunity_attack_prevented",active).put("trigger_satisfied",active);
   }else if("Siege Monster".equals(kind)){
    String target=choice(in,"target_kind","object","structure","creature");
    int damage=integer(in,"damage_before_trait",0,1000000);
    boolean active=!"creature".equals(target);
    out.put("damage_before_defenses",active?damage*2:damage)
       .put("damage_applied",false).put("trigger_satisfied",active);
   }else if("Evasion".equals(kind)){
    boolean dex=bool(in,"dexterity_save_for_half"),success=bool(in,"save_succeeded"),
      incapacitated=bool(in,"incapacitated");
    int damage=integer(in,"incoming_damage",0,1000000);
    boolean active=dex&&!incapacitated;
    out.put("trigger_satisfied",active);
    if(active)out.put("damage_before_defenses",success?0:damage/2)
       .put("damage_applied",false).put("other_defenses_pending",true);
    else out.put("normal_save_resolution_required",true);
   }else if("Undead Fortitude".equals(kind)){
    boolean zero=bool(in,"damage_reduces_to_zero"),radiant=bool(in,"radiant_damage"),
      critical=bool(in,"critical_hit");
    int damage=integer(in,"damage_taken",0,1000000);
    boolean active=zero&&!radiant&&!critical;
    out.put("trigger_satisfied",active).put("hp_applied",false);
    if(active){
     int total=integer(in,"constitution_save_total",-1000,1000000),dc=5+damage;
     boolean success=total>=dc;
     out.put("save_dc",dc).put("save_total",total).put("save_success",success)
       .put("proposed_hp_after",success?1:0);
    }else out.put("normal_zero_hp_resolution_required",true);
   }else return blocked("trait_family_not_implemented");
   return out;
  }catch(Exception e){try{return blocked(e.getMessage()==null?"trait_runtime_error":e.getMessage());}
   catch(Exception ignored){return new JSONObject();}}
 }
}
