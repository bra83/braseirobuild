package com.braseiro.dnd5e;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** XMM 2025: source-pinned, strictly baseline damage defenses and condition immunities.
 * Raw prose and full monster traits are NOT represented. No state mutation or hidden rolls.
 * All 503 names resolve through Xmm2025SourceIndex; 272 carry explicit defense fields.
 * Six records have conditional/trait-dependent defenses and fail closed where relevant.
 */
public final class Xmm2025DamageDefenseEngine {
 public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
 public static final int MONSTERS=503,EXPLICIT_DEFENSE_RECORDS=272,COMPLEX_DEFENSE_RECORDS=6;
 private static final String[] TYPES={"acid","bludgeoning","cold","fire","force","lightning",
  "necrotic","piercing","poison","psychic","radiant","slashing","thunder"};
 private static final String[] CONDITIONS={"blinded","charmed","deafened","exhaustion",
  "frightened","grappled","incapacitated","paralyzed","petrified","poisoned",
  "prone","restrained","stunned","unconscious"};
 private static final String[] RAW={
  "{\"id\":\"Abominable Yeti\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Black Dragon\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Blue Dragon\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Brass Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Bronze Dragon\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Copper Dragon\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Gold Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Green Dragon\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Red Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult Silver Dragon\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Adult White Dragon\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Air Elemental\",\"r\":[\"bludgeoning\",\"lightning\",\"piercing\",\"slashing\"],\"i\":[\"poison\",\"thunder\"],\"v\":[],\"c\":[\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Black Dragon\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Blue Dragon\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Brass Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Bronze Dragon\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Copper Dragon\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Gold Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Green Dragon\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Red Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient Silver Dragon\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ancient White Dragon\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Animal Lord\",\"r\":[\"cold\",\"fire\",\"necrotic\",\"psychic\",\"radiant\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Animated Armor\",\"r\":[],\"i\":[\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Animated Broom\",\"r\":[],\"i\":[\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Animated Flying Sword\",\"r\":[],\"i\":[\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Animated Rug of Smothering\",\"r\":[],\"i\":[\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Arcanaloth\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"acid\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Arch-hag\",\"r\":[\"cold\",\"fire\",\"psychic\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Archmage\",\"r\":[],\"i\":[\"psychic\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":true}",
  "{\"id\":\"Assassin\",\"r\":[\"poison\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Awakened Shrub\",\"r\":[\"piercing\"],\"i\":[],\"v\":[\"fire\"],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Awakened Tree\",\"r\":[\"bludgeoning\",\"piercing\"],\"i\":[],\"v\":[\"fire\"],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Azer Pyromancer\",\"r\":[],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Azer Sentinel\",\"r\":[],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Badger\",\"r\":[\"poison\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Balor\",\"r\":[\"cold\",\"lightning\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Banshee\",\"r\":[\"acid\",\"bludgeoning\",\"fire\",\"lightning\",\"piercing\",\"slashing\",\"thunder\"],\"i\":[\"cold\",\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Barbed Devil\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Barlgura\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Bearded Devil\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Behir\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Beholder\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Beholder Zombie\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"poisoned\",\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Berserker Commander\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Black Dragon Wyrmling\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Black Pudding\",\"r\":[],\"i\":[\"acid\",\"cold\",\"lightning\",\"slashing\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"grappled\",\"prone\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Blob of Annihilation\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"acid\",\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"stunned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Blue Dragon Wyrmling\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Blue Slaad\",\"r\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"thunder\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Bone Devil\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Bone Naga\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Brass Dragon Wyrmling\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Brazen Gorgon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[\"exhaustion\",\"petrified\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Bronze Dragon Wyrmling\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Cambion\",\"r\":[\"cold\",\"fire\",\"lightning\",\"poison\"],\"i\":[],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Chain Devil\",\"r\":[\"bludgeoning\",\"cold\",\"piercing\",\"slashing\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Chasme\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Chuul\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Clay Golem\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"acid\",\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Cloaker\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Cockatrice\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"petrified\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Cockatrice Regent\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"petrified\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Colossus\",\"r\":[\"necrotic\",\"radiant\"],\"i\":[\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\",\"stunned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Copper Dragon Wyrmling\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Couatl\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"psychic\",\"radiant\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Crawling Claw\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"incapacitated\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Dao\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"petrified\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Death Dog\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"frightened\",\"stunned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Death Knight\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Death Knight Aspirant\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Death Slaad\",\"r\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"thunder\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Death Tyrant\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Demilich\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"necrotic\",\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Deva\",\"r\":[\"radiant\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Djinni\",\"r\":[],\"i\":[\"lightning\",\"thunder\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Doppelganger\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Dracolich\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Dragon Turtle\",\"r\":[\"fire\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Dretch\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Dust Mephit\",\"r\":[],\"i\":[\"poison\"],\"v\":[\"fire\"],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Earth Elemental\",\"r\":[],\"i\":[\"poison\"],\"v\":[\"thunder\"],\"c\":[\"exhaustion\",\"paralyzed\",\"petrified\",\"poisoned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Efreeti\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Elemental Cataclysm\",\"r\":[],\"i\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"poison\",\"thunder\"],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"stunned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Empyrean\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"necrotic\",\"radiant\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Empyrean Iota\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Erinyes\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ettin\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"frightened\",\"stunned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Fire Elemental\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Fire Giant\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Flameskull\",\"r\":[],\"i\":[\"fire\",\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"poisoned\",\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Flaming Skeleton\",\"r\":[],\"i\":[\"fire\",\"poison\"],\"v\":[\"bludgeoning\"],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Flesh Golem\",\"r\":[],\"i\":[\"lightning\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Flumph\",\"r\":[],\"i\":[],\"v\":[\"psychic\"],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Frost Giant\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Galeb Duhr\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gargoyle\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gas Spore Fungus\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"frightened\",\"paralyzed\",\"poisoned\",\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gelatinous Cube\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ghast\",\"r\":[\"necrotic\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ghast Gravecaller\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ghost\",\"r\":[\"acid\",\"bludgeoning\",\"cold\",\"fire\",\"lightning\",\"piercing\",\"slashing\",\"thunder\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ghoul\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Giant Badger\",\"r\":[\"poison\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Giant Eagle\",\"r\":[\"necrotic\",\"radiant\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Giant Elk\",\"r\":[\"necrotic\",\"radiant\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Giant Fire Beetle\",\"r\":[\"fire\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Giant Owl\",\"r\":[\"necrotic\",\"radiant\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Giant Vulture\",\"r\":[\"necrotic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gibbering Mouther\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Glabrezu\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gold Dragon Wyrmling\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gorgon\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"exhaustion\",\"petrified\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Goristro\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Graveyard Revenant\",\"r\":[\"necrotic\",\"psychic\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\",\"stunned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gray Ooze\",\"r\":[\"acid\",\"cold\",\"fire\"],\"i\":[],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"grappled\",\"prone\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gray Slaad\",\"r\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"thunder\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Green Dragon Wyrmling\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Green Slaad\",\"r\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"thunder\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Grell\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[\"blinded\",\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Guardian Naga\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"paralyzed\",\"poisoned\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Gulthias Blight\",\"r\":[\"fire\",\"necrotic\"],\"i\":[],\"v\":[],\"c\":[\"deafened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Half-Dragon\",\"r\":[],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":true,\"complex_condition\":false}",
  "{\"id\":\"Haunting Revenant\",\"r\":[\"necrotic\",\"psychic\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Hell Hound\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Helmed Horror\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Hezrou\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Homunculus\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Horned Devil\",\"r\":[],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Hydra\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"frightened\",\"stunned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ice Devil\",\"r\":[],\"i\":[\"cold\",\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ice Mephit\",\"r\":[],\"i\":[\"cold\",\"poison\"],\"v\":[\"fire\"],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Imp\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Incubus\",\"r\":[\"cold\",\"fire\",\"poison\",\"psychic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Intellect Devourer\",\"r\":[\"psychic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Invisible Stalker\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Iron Golem\",\"r\":[],\"i\":[\"fire\",\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Juvenile Shadow Dragon\",\"r\":[],\"i\":[\"necrotic\"],\"v\":[],\"c\":[],\"complex_damage\":true,\"complex_condition\":false}",
  "{\"id\":\"Knight\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Kraken\",\"r\":[],\"i\":[\"cold\",\"lightning\"],\"v\":[],\"c\":[\"frightened\",\"grappled\",\"paralyzed\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Lacedon Ghoul\",\"r\":[\"cold\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Lemure\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Lich\",\"r\":[\"cold\",\"lightning\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Lizardfolk Sovereign\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Magma Mephit\",\"r\":[],\"i\":[\"fire\",\"poison\"],\"v\":[\"cold\"],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Magmin\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Manes\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Manes Vaporspawn\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"grappled\",\"poisoned\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Marid\",\"r\":[\"acid\",\"cold\",\"lightning\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Marilith\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Merfolk Wavebender\",\"r\":[\"cold\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Mezzoloth\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"acid\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Mimic\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Mind Flayer\",\"r\":[\"psychic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Mind Flayer Arcanist\",\"r\":[],\"i\":[\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Minotaur Skeleton\",\"r\":[],\"i\":[\"poison\"],\"v\":[\"bludgeoning\"],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Modron Duodrone\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Modron Monodrone\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Modron Pentadrone\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Modron Quadrone\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Modron Tridrone\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Mud Mephit\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Mummy\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[\"fire\"],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Mummy Lord\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[\"fire\"],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Myconid Spore Servant\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"frightened\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Nalfeshnee\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Needle Blight\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"deafened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Night Hag\",\"r\":[\"cold\",\"fire\"],\"i\":[],\"v\":[],\"c\":[\"charmed\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Nightmare\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Nycaloth\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"acid\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ochre Jelly\",\"r\":[\"acid\"],\"i\":[\"lightning\",\"slashing\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"grappled\",\"prone\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ogre Zombie\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Oni\",\"r\":[\"cold\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Pit Fiend\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Planetar\",\"r\":[\"radiant\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Polar Bear\",\"r\":[\"cold\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Poltergeist\",\"r\":[\"acid\",\"bludgeoning\",\"cold\",\"fire\",\"lightning\",\"piercing\",\"slashing\",\"thunder\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Psychic Gray Ooze\",\"r\":[\"acid\",\"cold\",\"fire\",\"psychic\"],\"i\":[],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"exhaustion\",\"frightened\",\"grappled\",\"prone\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Quaggoth\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Quaggoth Thonot\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Quasit\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Questing Knight\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Rakshasa\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\"],\"complex_damage\":true,\"complex_condition\":false}",
  "{\"id\":\"Red Dragon Wyrmling\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Red Slaad\",\"r\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"thunder\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Remorhaz\",\"r\":[],\"i\":[\"cold\",\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Revenant\",\"r\":[\"necrotic\",\"psychic\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"poisoned\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Sahuagin Baron\",\"r\":[\"acid\",\"cold\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Sahuagin Priest\",\"r\":[\"acid\",\"cold\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Sahuagin Warrior\",\"r\":[\"acid\",\"cold\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Salamander\",\"r\":[],\"i\":[\"fire\"],\"v\":[\"cold\"],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Salamander Fire Snake\",\"r\":[],\"i\":[\"fire\"],\"v\":[\"cold\"],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Salamander Inferno Master\",\"r\":[],\"i\":[\"fire\"],\"v\":[\"cold\"],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Scarecrow\",\"r\":[],\"i\":[\"poison\"],\"v\":[\"fire\"],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Shadow\",\"r\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"thunder\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[\"radiant\"],\"c\":[\"exhaustion\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Shadow Demon\",\"r\":[\"acid\",\"bludgeoning\",\"cold\",\"fire\",\"lightning\",\"piercing\",\"slashing\",\"thunder\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[\"radiant\"],\"c\":[\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Shadow Dragon\",\"r\":[],\"i\":[\"necrotic\"],\"v\":[],\"c\":[],\"complex_damage\":true,\"complex_condition\":false}",
  "{\"id\":\"Shambling Mound\",\"r\":[\"cold\",\"fire\"],\"i\":[\"lightning\"],\"v\":[],\"c\":[\"deafened\",\"exhaustion\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Shield Guardian\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Shrieker Fungus\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Silver Dragon Wyrmling\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Skeleton\",\"r\":[],\"i\":[\"poison\"],\"v\":[\"bludgeoning\"],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Slaad Tadpole\",\"r\":[\"acid\",\"cold\",\"fire\",\"lightning\",\"thunder\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Smoke Mephit\",\"r\":[],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Solar\",\"r\":[],\"i\":[\"poison\",\"radiant\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Spectator\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"exhaustion\",\"prone\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Specter\",\"r\":[\"acid\",\"bludgeoning\",\"cold\",\"fire\",\"lightning\",\"piercing\",\"slashing\",\"thunder\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Sphinx of Lore\",\"r\":[\"necrotic\",\"radiant\"],\"i\":[\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Sphinx of Secrets\",\"r\":[\"necrotic\",\"radiant\"],\"i\":[\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Sphinx of Valor\",\"r\":[\"necrotic\",\"radiant\"],\"i\":[\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Sphinx of Wonder\",\"r\":[\"necrotic\",\"psychic\",\"radiant\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Spined Devil\",\"r\":[\"cold\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Spirit Naga\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Steam Mephit\",\"r\":[],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Stone Golem\",\"r\":[],\"i\":[\"poison\",\"psychic\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Storm Giant\",\"r\":[\"cold\"],\"i\":[\"lightning\",\"thunder\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Succubus\",\"r\":[\"cold\",\"fire\",\"poison\",\"psychic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Bats\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Crawling Claws\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\",\"grappled\",\"incapacitated\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Dretches\",\"r\":[\"bludgeoning\",\"cold\",\"fire\",\"lightning\",\"piercing\",\"slashing\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Insects\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Larvae\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Lemures\",\"r\":[\"bludgeoning\",\"cold\",\"piercing\",\"slashing\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Piranhas\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Rats\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Ravens\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Stirges\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Swarm of Venomous Snakes\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"grappled\",\"paralyzed\",\"petrified\",\"prone\",\"restrained\",\"stunned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Tarrasque\",\"r\":[\"bludgeoning\",\"piercing\",\"slashing\"],\"i\":[\"fire\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"deafened\",\"frightened\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Thri-kreen Psion\",\"r\":[\"psychic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Treant\",\"r\":[\"bludgeoning\",\"piercing\"],\"i\":[],\"v\":[\"fire\"],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Tree Blight\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"deafened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Twig Blight\",\"r\":[],\"i\":[],\"v\":[\"fire\"],\"c\":[\"deafened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Ultroloth\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"acid\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Unicorn\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"charmed\",\"paralyzed\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Vampire\",\"r\":[\"necrotic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Vampire Familiar\",\"r\":[\"necrotic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":true}",
  "{\"id\":\"Vampire Nightbringer\",\"r\":[],\"i\":[\"cold\",\"necrotic\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Vampire Spawn\",\"r\":[\"necrotic\"],\"i\":[],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Vampire Umbral Lord\",\"r\":[],\"i\":[\"cold\",\"necrotic\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Vine Blight\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"deafened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Violet Fungus\",\"r\":[],\"i\":[],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"frightened\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Violet Fungus Necrohulk\",\"r\":[],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"blinded\",\"charmed\",\"deafened\",\"frightened\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Vrock\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Warhorse Skeleton\",\"r\":[],\"i\":[\"poison\"],\"v\":[\"bludgeoning\"],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Water Elemental\",\"r\":[\"acid\",\"fire\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Water Weird\",\"r\":[\"fire\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"White Dragon Wyrmling\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Wight\",\"r\":[\"necrotic\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Will-o'-Wisp\",\"r\":[\"acid\",\"bludgeoning\",\"cold\",\"fire\",\"necrotic\",\"piercing\",\"slashing\"],\"i\":[\"lightning\",\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Winter Wolf\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Wraith\",\"r\":[\"acid\",\"bludgeoning\",\"cold\",\"fire\",\"piercing\",\"slashing\"],\"i\":[\"necrotic\",\"poison\"],\"v\":[],\"c\":[\"charmed\",\"exhaustion\",\"grappled\",\"paralyzed\",\"petrified\",\"poisoned\",\"prone\",\"restrained\",\"unconscious\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Xorn\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"paralyzed\",\"petrified\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Yeti\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Yochlol\",\"r\":[\"cold\",\"fire\",\"lightning\"],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Black Dragon\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Blue Dragon\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Brass Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Bronze Dragon\",\"r\":[],\"i\":[\"lightning\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Copper Dragon\",\"r\":[],\"i\":[\"acid\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Gold Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Green Dragon\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Red Dragon\",\"r\":[],\"i\":[\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Remorhaz\",\"r\":[],\"i\":[\"cold\",\"fire\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young Silver Dragon\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Young White Dragon\",\"r\":[],\"i\":[\"cold\"],\"v\":[],\"c\":[],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Yuan-ti Abomination\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Yuan-ti Infiltrator\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Yuan-ti Malison (Type 1)\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Yuan-ti Malison (Type 2)\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Yuan-ti Malison (Type 3)\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}",
  "{\"id\":\"Zombie\",\"r\":[],\"i\":[\"poison\"],\"v\":[],\"c\":[\"exhaustion\",\"poisoned\"],\"complex_damage\":false,\"complex_condition\":false}"
 };
 private static final Map<String,JSONObject> BY_NAME=new LinkedHashMap<>();
 static{try{
  for(String s:RAW){JSONObject r=new JSONObject(s);String id=r.getString("id");
   if(BY_NAME.put(id,r)!=null)throw new IllegalStateException("duplicate defense id: "+id);
  }
  if(BY_NAME.size()!=EXPLICIT_DEFENSE_RECORDS)throw new IllegalStateException("incomplete defense index");
 }catch(Exception e){throw new ExceptionInInitializerError(e);}}
 private Xmm2025DamageDefenseEngine(){}
 private static JSONObject blocked(String reason)throws Exception{
  return new JSONObject().put("resolved",false).put("status",reason).put("auto_rolls",false)
   .put("auto_mutation_allowed",false).put("damage_applied",false)
   .put("condition_applied",false).put("turn_advanced",false);
 }
 private static boolean contains(String[] values,String key){for(String x:values)if(x.equals(key))return true;return false;}
 private static boolean includes(JSONArray a,String target)throws Exception{
  for(int i=0;i<a.length();i++)if(target.equals(a.getString(i)))return true;
  return false;
 }
 private static String joined(JSONArray a)throws Exception{
  StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++){
   if(i>0)b.append(", ");b.append(a.getString(i));
  }return b.toString();
 }
 private static JSONObject row(String name)throws Exception{
  if(Xmm2025SourceIndex.get(name).length()==0)return null;
  JSONObject found=BY_NAME.get(name);
  return found==null?new JSONObject().put("id",name).put("r",new JSONArray())
   .put("i",new JSONArray()).put("v",new JSONArray()).put("c",new JSONArray())
   .put("complex_damage",false).put("complex_condition",false):new JSONObject(found.toString());
 }
 /** Validates each source-derived field against the actual compiled statblock.
  * Conditional text is never parsed into unconditional immunity or resistance.
  */
 public static JSONObject profile(String name)throws Exception{
  JSONObject data=row(name);
  if(data==null)return blocked("defender_not_in_pinned_xmm");
  if(!CompiledStatblockEngine.has(name))Xmm2025CompiledBestiaryEngine.install();
  JSONObject compiled=CompiledStatblockEngine.get(name);
  if(compiled.length()==0)return blocked("defender_not_in_authorized_compiled_registry");
  for(String[] pair:new String[][]{{"r","resistances"},{"i","immunities"},
      {"v","vulnerabilities"},{"c","condition_immunities"}}){
   String key=pair[0],actual=compiled.optString(pair[1]).trim();
   boolean complex="c".equals(key)?data.getBoolean("complex_condition"):
    (data.getBoolean("complex_damage") && (actual.startsWith("special:") || actual.contains("(conditional)")));
   String expected=joined(data.getJSONArray(key));
   if(!complex&&!expected.equals(actual))return blocked("compiled_defense_mismatch:"+name+":"+key);
   if(complex&&actual.isEmpty())return blocked("compiled_complex_defense_marker_missing:"+name+":"+key);
  }
  JSONObject pointer=Xmm2025SourceIndex.get(name);
  return data.put("resolved",true).put("source","XMM")
   .put("source_pointer",pointer.getString("ptr"))
   .put("source_blob_sha1",SOURCE_BLOB)
   .put("auto_rolls",false).put("auto_mutation_allowed",false)
   .put("full_trait_effects_certified",false);
 }
 /** Requires a source-bound defender and explicit incoming damage. Computes only
  * the base damage-type adjustments; class features, special traits, effects and
  * target HP must still be adjudicated by the DM.
  */
 public static JSONObject resolve(JSONObject in)throws Exception{
  if(in==null)return blocked("missing_request");
  String defender=in.optString("defender").trim(),type=in.optString("damage_type").trim().toLowerCase(java.util.Locale.ROOT);
  JSONObject data=profile(defender);
  if(!data.optBoolean("resolved",false))return data;
  if(!in.optBoolean("target_confirmed",false))return blocked("manual_target_confirmation_required");
  if(!contains(TYPES,type))return blocked("invalid_damage_type");
  if(!in.has("incoming_damage")||!(in.opt("incoming_damage") instanceof Number))
   return blocked("explicit_incoming_damage_required");
  Number n=(Number)in.get("incoming_damage");double value=n.doubleValue();
  if(!Double.isFinite(value)||value!=Math.floor(value)||value<0||value>10000000)
   return blocked("invalid_incoming_damage");
  if(data.getBoolean("complex_damage"))
   return blocked("conditional_or_trait_dependent_defense_requires_manual_resolution")
    .put("defender",defender).put("source_pointer",data.getString("source_pointer"));
  int before=n.intValue();
  boolean immune=includes(data.getJSONArray("i"),type);
  boolean resist=includes(data.getJSONArray("r"),type);
  boolean vulnerable=includes(data.getJSONArray("v"),type);
  // PHB damage order: immunity => 0, resistance => floor(n/2), vulnerability => x2.
  int after=immune?0:before;
  if(!immune&&resist)after/=2;
  if(!immune&&vulnerable)after*=2;
  return new JSONObject().put("resolved",true)
   .put("status","baseline_source_damage_defenses_calculated_pending_traits_and_hp_application")
   .put("source","XMM").put("source_blob_sha1",SOURCE_BLOB)
   .put("source_pointer",data.getString("source_pointer"))
   .put("defender",defender).put("damage_type",type)
   .put("incoming_damage",before).put("damage_after_basic_defenses",after)
   .put("immune",immune).put("resistant",resist).put("vulnerable",vulnerable)
   .put("other_trait_effects_pending",true)
   .put("damage_applied",false).put("auto_rolls",false)
   .put("auto_mutation_allowed",false).put("turn_advanced",false);
 }
 /** Check only the explicitly listed unconditional condition immunities.
  * Conditional immunities always require DM adjudication.
  */
 public static JSONObject conditionImmunity(JSONObject in)throws Exception{
  if(in==null)return blocked("missing_request");
  String defender=in.optString("defender").trim(),condition=in.optString("condition").trim().toLowerCase(java.util.Locale.ROOT);
  JSONObject data=profile(defender);
  if(!data.optBoolean("resolved",false))return data;
  if(!in.optBoolean("target_confirmed",false))return blocked("manual_target_confirmation_required");
  if(!contains(CONDITIONS,condition))return blocked("invalid_condition");
  if(data.getBoolean("complex_condition"))
   return blocked("conditional_condition_immunity_requires_manual_resolution")
     .put("defender",defender).put("source_pointer",data.getString("source_pointer"));
  boolean immune=includes(data.getJSONArray("c"),condition);
  return new JSONObject().put("resolved",true).put("source","XMM")
   .put("status","source_condition_immunity_checked_pending_other_effects")
   .put("defender",defender).put("condition",condition).put("immune",immune)
   .put("source_pointer",data.getString("source_pointer"))
   .put("source_blob_sha1",SOURCE_BLOB)
   .put("other_trait_effects_pending",true).put("condition_applied",false)
   .put("auto_rolls",false).put("auto_mutation_allowed",false);
 }
 /** Compose the existing 59 verified save-for-half actions with defender defenses.
  * The two stages are independently fail-closed; no damage or spell is applied.
  */
 public static JSONObject resolveSaveAgainstDefender(JSONObject in)throws Exception{
  if(in==null)return blocked("missing_request");
  JSONObject saved=Xmm2025SaveDamageEngine.resolve(in);
  if(!saved.optBoolean("resolved",false))return saved;
  String defender=in.optString("defender").trim();
  if(defender.isEmpty())return blocked("explicit_defender_required");
  JSONObject request=new JSONObject().put("defender",defender)
   .put("damage_type",saved.getString("damage_type"))
   .put("incoming_damage",saved.getInt("damage_before_defenses"))
   .put("target_confirmed",in.optBoolean("target_confirmed",false));
  JSONObject defense=resolve(request);
  if(!defense.optBoolean("resolved",false))return defense;
  return new JSONObject().put("resolved",true)
   .put("status","source_save_and_basic_defenses_calculated_pending_manual_effects")
   .put("save",saved).put("defense",defense)
   .put("damage_after_basic_defenses",defense.getInt("damage_after_basic_defenses"))
   .put("other_trait_effects_pending",true)
   .put("damage_applied",false).put("auto_rolls",false)
   .put("auto_mutation_allowed",false).put("turn_advanced",false);
 }
 public static JSONObject coverage()throws Exception{
  return new JSONObject().put("source","XMM").put("indexed_monsters",MONSTERS)
   .put("explicit_defense_records",EXPLICIT_DEFENSE_RECORDS)
   .put("unconditional_source_records",MONSTERS-COMPLEX_DEFENSE_RECORDS)
   .put("conditional_source_records",COMPLEX_DEFENSE_RECORDS)
   .put("source_blob_sha1",SOURCE_BLOB)
   .put("full_trait_effects_certified",false)
   .put("auto_rolls",false).put("auto_mutation_allowed",false);
 }
}
