function resultText(event){
  const r=event?.resolution??{};
  if(event.intent==='observe')return r.success?`Você observa sem se expor. O teste totalizou ${r.total} contra dificuldade ${r.difficulty}; posições e movimento do contato ficam claros.`:`Você tenta ler o movimento entre as ruínas, mas o teste totalizou ${r.total} contra dificuldade ${r.difficulty}; o contato continua difícil de avaliar.`;
  if(event.intent==='approach')return `Você se apresenta sem atacar. A reação inicial resulta em ${r.disposition?.label??'uma atitude incerta'} (total ${r.total}).`;
  if(event.intent==='ambush')return r.success?`Você se posiciona sem ser percebido. A oposição terminou ${r.actorTotal} contra ${r.defenderTotal}; a emboscada está preparada.`:`O movimento denuncia o grupo. A oposição terminou ${r.actorTotal} contra ${r.defenderTotal}; os adversários perceberam a ameaça.`;
  if(event.intent==='retreat')return r.success?(r.rngAlgorithm==='NO_ROLL_REQUIRED'?'Você recua antes que o contato perceba o grupo. Nenhuma perseguição se inicia.':`Você rompe o contato e escapa. A oposição terminou ${r.actorTotal} contra ${r.defenderTotal}.`):`A tentativa de fuga é alcançada pelos perseguidores (${r.actorTotal} contra ${r.defenderTotal}).`;
  return `O Mestre narra a consequência do evento ${event?.eventId??'mecânico'} sem alterar o resultado já resolvido.`;
}
export class DeterministicBarbaraTestAdapter {
  generate({ mechanicalEvent }) {
    if(!mechanicalEvent?.resolution)throw new Error('Mechanical resolution required');
    return Object.freeze({adapter:'DETERMINISTIC_BARBARA_TEST',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:resultText(mechanicalEvent)});
  }
}
export class FailingBarbaraTestAdapter { generate() { throw new Error('Injected narrative provider failure'); } }
