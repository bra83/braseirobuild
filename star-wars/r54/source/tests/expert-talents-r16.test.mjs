import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { expertSkillTalentSpec } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function gameWithTalent({talent,skill}){
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),`r16-${skill}`));
  const c=STARTER_PRESETS.zal(); c.talents=[...(c.talents??[]),talent]; c.skills[skill]=Math.max(1,c.skills[skill]??0); game.setCharacter(c); return game;
}

test('Navegador mestre custa 2 Tensão e reduz Astrogation em 1, mínimo Fácil',()=>{
  const game=gameWithTalent({talent:'Navegador mestre',skill:'astronavegacion'});
  const before=game.state.player.character.derived.strain.current;
  const result=game.resolveSkillCheck({skillKey:'astronavegacion',difficulty:3,useExpertTalent:true,rng:()=>0});
  assert.equal(result.pool.difficulty,2); assert.equal(result.expertTalent.label,'Navegador mestre');
  assert.equal(game.state.player.character.derived.strain.current,before+2);
  const game2=gameWithTalent({talent:'Navegador mestre',skill:'astronavegacion'});
  const min=game2.resolveSkillCheck({skillKey:'astronavegacion',difficulty:1,useExpertTalent:true,rng:()=>0});
  assert.equal(min.pool.difficulty,1);
});

test('perfil EotE Core não oferece talento especialista genérico para Liderança',()=>{
  const game=gameWithTalent({talent:'Comando',skill:'liderazgo'});
  const spec=expertSkillTalentSpec(game.state.player.character,'liderazgo'); assert.equal(spec.available,false);
  assert.throws(()=>game.resolveSkillCheck({skillKey:'liderazgo',difficulty:2,useExpertTalent:true,rng:()=>0}),/não possui um talento especialista/i);
});

test('interface PT-BR expõe Navegador mestre somente como opção explícita',async()=>{
  const fs=await import('node:fs/promises');
  const html=await fs.readFile(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
  const ui=await fs.readFile(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
  assert.match(html,/useExpertTalent/); assert.match(ui,/Navegador mestre/); assert.doesNotMatch(ui,/Líder experiente/); assert.match(ui,/2 de Tensão/);
});
