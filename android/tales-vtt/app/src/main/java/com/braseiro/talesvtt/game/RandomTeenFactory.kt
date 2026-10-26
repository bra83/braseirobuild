package com.braseiro.talesvtt.game

import kotlin.random.Random

object RandomTeenFactory {
  private val firstNames=listOf("Mette","Helmi","Sanna","Melissa","Tiffany","Erin","Dennis","Lenny","Ola","Jeremy","Kyle","Seth")
  private val lastNames=listOf("Andersson","Berg","Ek","Holm","Järne","Lind","Nilsson","Svensson","Tannenbaum","Östberg")
  private val anchors=listOf("Eva — professora","Gunnar — mecânico","Lena — irmã mais velha","Mats — vizinho","Louise — tia","Sven — treinador")
  private val problems=listOf("Minha família está se desfazendo.","Eu preciso provar que não sou mais criança.","Estou escondendo algo dos meus amigos.","A escola está desmoronando ao meu redor.","Eu confio na pessoa errada.")
  private val drives=listOf("Descobrir o que aconteceu.","Proteger meus amigos.","Sair desta cidade.","Impedir que isso aconteça de novo.","Encontrar alguém que desapareceu.")
  private val shames=listOf("Eu abandonei alguém quando precisava de mim.","Eu menti sobre algo importante.","Tenho medo de ser igual aos meus pais.","Eu causei algo que não consigo desfazer.","Ninguém sabe do que realmente tenho medo.")
  private val iconic=listOf("Sistema de hacking caseiro","Jaqueta de couro","Câmera VHS","Kit de ferramentas","Walkman modificado","Skate gasto","Telefone portátil velho")
  private val portraits=listOf("kid_portrait_01","kid_portrait_02","kid_portrait_03","kid_portrait_04")

  fun create(random:Random=Random.Default):KidState {
    val type=CanonicalFloodRules.types.random(random)
    val attrs=linkedMapOf("CORPO" to 1,"TECNOLOGIA" to 1,"CORAÇÃO" to 1,"MENTE" to 1)
    var remain=CanonicalFloodRules.ATTRIBUTE_TOTAL-4
    while(remain>0){
      val choices=attrs.keys.filter{(attrs[it]?:1)<CanonicalFloodRules.ATTRIBUTE_MAX}
      val chosen=choices.random(random); attrs[chosen]=(attrs[chosen]?:1)+1; remain--
    }
    val skills=linkedMapOf<String,Int>().apply{TalesSkillCatalog.skillToAttribute.keys.forEach{put(it,0)}}
    var points=CanonicalFloodRules.CREATION_SKILL_POINTS
    while(points>0){
      val choices=skills.keys.filter{skill->(skills[skill]?:0)<if(skill in type.keySkills)3 else 1}
      val chosen=choices.random(random); skills[chosen]=(skills[chosen]?:0)+1; points--
    }
    val age=random.nextInt(CanonicalFloodRules.MIN_AGE,CanonicalFloodRules.MAX_AGE+1)
    return KidState(
      name="${firstNames.random(random)} ${lastNames.random(random)}",
      archetype=type.label,
      portraitAssetKey=portraits.random(random),
      description="Teen de $age anos, ${type.label.lowercase()}, criado pelas regras de Things from the Flood.",
      problem=problems.random(random),
      drive=drives.random(random),
      prideText="",
      shameText=shames.random(random),
      shameAvailable=true,
      birthday="${random.nextInt(1,29).toString().padStart(2,'0')}/${random.nextInt(1,13).toString().padStart(2,'0')}",
      anchorName=anchors.random(random),
      iconicItemName=iconic.random(random),
      hooks=listOf("Um gancho será definido pelo grupo."),
      friction="A tensão do grupo ainda será definida em jogo.",
      relationships=listOf(KidRelationship("Outro Teen do grupo","relação que será definida em jogo"),KidRelationship("NPC importante","relação da vida cotidiana")),
      age=age,
      luck=0,
      conditions=emptySet(),
      broken=false,
      prideAvailable=false,
      xp=0,
      attributes=attrs,
      skills=skills
    )
  }

  fun fromKid(kid:KidState,random:Random=Random.Default):KidState {
    val fresh=create(random)
    return fresh.copy(
      name=kid.name,
      portraitAssetKey=kid.portraitAssetKey,
      portraitImageBase64=kid.portraitImageBase64,
      description=kid.description,
      favoriteSong=kid.favoriteSong,
      relationships=kid.relationships.ifEmpty{fresh.relationships}
    )
  }

  fun validateCreation(teen:KidState):Boolean {
    val type=CanonicalFloodRules.type(teen.archetype)?:return false
    if(teen.age !in CanonicalFloodRules.MIN_AGE..CanonicalFloodRules.MAX_AGE)return false
    if(teen.attributes.keys!=setOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE"))return false
    if(teen.attributes.values.any{it !in 1..5}||teen.attributes.values.sum()!=14)return false
    if(teen.skills.keys!=TalesSkillCatalog.skillToAttribute.keys)return false
    if(teen.skills.values.sum()!=10)return false
    if(teen.skills.any{(skill,value)->value !in 0..if(skill in type.keySkills)3 else 1})return false
    if(teen.luck!=0||teen.prideAvailable)return false
    if(teen.shameText.isBlank())return false
    if(teen.items.any{!CanonicalFloodRules.validItem(it)})return false
    return true
  }

  fun validatePersistentState(teen:KidState):Boolean {
    if(teen.name.isBlank()||teen.age !in 14..19)return false
    if(teen.attributes.keys!=setOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE"))return false
    if(teen.attributes.values.any{it !in 1..5}||teen.attributes.values.sum()!=14)return false
    if(teen.skills.keys!=TalesSkillCatalog.skillToAttribute.keys||teen.skills.values.any{it !in 0..5})return false
    if(teen.luck!=0||teen.xp !in 0..10)return false
    if(teen.items.any{!CanonicalFloodRules.validItem(it)})return false
    return true
  }
}
