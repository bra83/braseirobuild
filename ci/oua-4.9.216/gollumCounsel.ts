/** Gollum: strictly public, read-only guidance. No GM secrets or game mutations. */
export type GollumVisibleState={pendingTest:{skill:string}|null;pendingRollResolved:boolean;journeyPending:boolean;heroName:string;hope:number|null;currentTab:string;locationName:string;storyBusy:boolean};
export type GollumTip={id:string;priority:'important'|'tip'|'quiet';heading:string;speech:string;reason:string;source:string};
export function publicGollumTips(s:GollumVisibleState):GollumTip[]{
  const tips:GollumTip[]=[];
  if(s.pendingTest)tips.push({id:'pending-test:'+String(s.pendingTest.skill).slice(0,45),priority:'important',heading:'Uma rolagem espera por você',
    speech:s.pendingRollResolved?'O resultado espera por sua confirmação, precioso.':'Os dados precisam rolar antes que a história avance, precioso.',
    reason:'Há um teste pendente na ficha visível. Resolva-o na aba Dados; não prossiga automaticamente.',
    source:'O Um Anel 2e — testes e estado atual do VTT'});
  if(s.journeyPending&&!s.pendingTest)tips.push({id:'journey-roll',priority:'important',heading:'A estrada pede atenção',
    speech:'A estrada é longa... precisamos resolver esta etapa antes de seguir, sim.',
    reason:'A Jornada mostra uma etapa pendente. Consulte o painel da Jornada.',
    source:'O Um Anel 2e — Jornada'});
  if(s.hope===0)tips.push({id:'hope-zero',priority:'important',heading:'Esperança esgotada',
    speech:'Sem esperança? Isso é perigoso, precioso. Tenha cuidado.',
    reason:'A ficha pública indica zero Esperança. Consulte os efeitos e opções aplicáveis.',
    source:'O Um Anel 2e — Esperança'});
  const contexts:Record<string,GollumTip>={
    dados:{id:'tab-dados',priority:'tip',heading:'Conheça os dados',
      speech:'Os dados de Proeza e de Sucesso não são iguais, precioso.',
      reason:'Os dados têm papéis diferentes. Confira a configuração do teste antes de lançar.',
      source:'O Um Anel 2e — testes'},
    personagens:{id:'tab-personagens',priority:'tip',heading:'Seu herói',
      speech:'Conhecer nossas forças, oh, isso ajuda muito.',
      reason:'Consulte os atributos e perícias. O guia não edita a ficha por você.',
      source:'Interface do VTT — personagens'},
    campanha:{id:'tab-campanha',priority:'tip',heading:'Preserve a aventura',
      speech:'Guarde bem nossas lembranças, precioso. Não queremos perdê-las.',
      reason:'Faça uma exportação antes de atualizações, sobretudo se a assinatura Android for diferente.',
      source:'Interface do VTT — backup'},
    mestre:{id:'tab-mestre',priority:'tip',heading:'Pergunte sobre regras',
      speech:'Podemos perguntar pelas regras, sim... mas não pelos segredos.',
      reason:'A seção Mestre IA explica regras sem que o guia revele futuros encontros ou planos ocultos.',
      source:'Interface do VTT — Mestre IA'}
  };
  const page=s.currentTab==='ficha'?'personagens':s.currentTab;
  if(contexts[page])tips.push(contexts[page]);
  if(!tips.length)tips.push({id:'welcome',priority:'quiet',heading:'Uma voz nas sombras',
    speech:'Estou aqui se precisar de ajuda, precioso.',
    reason:'Nenhum aviso importante detectado no estado visível.',
    source:'Guia da interface'});
  return tips;
}
export function highlightedGollumTip(s:GollumVisibleState,dismissed:string[],mode:'important'|'all'|'off'):GollumTip|null{
  if(mode==='off')return null;
  return publicGollumTips(s).find(t=>t.priority!=='quiet'&&(mode==='all'||t.priority==='important')&&!dismissed.includes(t.id))||null;
}
