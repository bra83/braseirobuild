package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Source identifiers stay in English internally while action labels shown to players use PT-BR. */
public final class XmmPtBrActionLabelTest {
    @Test public void chargeAndManualRiderActionNamesAreShownInBrazilianPortuguese(){
        String[][] names={
            {"Claws","Garras"},{"Flame Whip","Chicote de Chamas"},
            {"Beard","Barba"},{"Infernal Sting","Ferrão Infernal"},
            {"Dread Scythe","Foice do Pavor"},
            {"Morningstar","Maça-estrela"},{"Light Hammer","Martelo Leve"},
            {"Attach","Aderir"},{"Crush","Esmagar"},{"Chaos Blade","Lâmina do Caos"},
            {"Gore","Chifrada"},{"Sacred Weapon","Arma Sagrada"},{"Claw","Garra"},
            {"Swallow","Engolir"},{"Psychic Warp","Distorção Psíquica"},
            {"Brutal Gore","Chifrada Brutal"},{"Chaos Claw","Garra do Caos"},
            {"Chaos Staff","Cajado do Caos"},{"Ravage","Devastar"},{"Hooves","Cascos"},
            {"Faerie Dust","Pó Feérico"},{"Reel","Puxar"},{"Tentacle","Tentáculo"},
            {"Umbral Dagger","Adaga Umbral"},{"Ram","Investida"},
            {"Avalanche Slam","Golpe da Avalanche"},{"Talons","Garras"},
            {"Tusk (Boar or Hybrid Form Only)","Presa (Apenas Forma de Javali ou Híbrida)"}
        };
        for(String[] row:names)
            assertEquals(row[0],row[1],DndPtBrLocalizer.localizeText(row[0]));
    }
}
