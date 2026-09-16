function clean(v,max=700){return String(v??'').replace(/\s+/g,' ').trim().slice(0,max)}
export function buildSceneImagePrompt({scene,world,character,narrative}={}){
  const title=clean(scene?.title||world?.position?.name||'cena atual',160);
  const summary=clean(narrative?.text||scene?.summary||'',1200);
  const place=clean(world?.position?.name||world?.position?.label||title,180);
  const weather=clean(world?.weather?.label||world?.weather||'',120);
  const time=clean(world?.timeOfDay||world?.dayPhase||'',80);
  const hero=clean(character?.name||'',100);
  const parts=[
    `Crie uma ilustração de fantasia sombria para uma cena de Worlds Without Number chamada “${title}”.`,
    `Local: ${place}.`, summary&&`Situação visível: ${summary}.`, weather&&`Clima: ${weather}.`, time&&`Hora do dia: ${time}.`,
    hero&&`Se o protagonista estiver visível, preserve a identidade de ${hero}, sem acrescentar equipamento ou poderes não informados.`,
    'Composição cinematográfica horizontal ou vertical adaptável a celular, ambiente legível, textura pictórica detalhada, sem HUD, sem texto na imagem, sem logotipos.',
    'Não revele segredos do Mestre, criaturas escondidas, locais ainda não descobertos ou informações que o personagem não poderia perceber.'
  ].filter(Boolean);
  return parts.join(' ');
}
