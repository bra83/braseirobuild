export function validateProviderEndpoint(endpoint) {
  const value=String(endpoint ?? '').trim();
  if (!value) throw new Error('Endpoint do Mestre não configurado.');
  let url;
  try { url=new URL(value); } catch { throw new Error('Endpoint do Mestre inválido.'); }
  if (url.protocol!=='https:' && !(url.protocol==='http:' && ['127.0.0.1','localhost'].includes(url.hostname))) {
    throw new Error('O endpoint do Mestre deve usar HTTPS; HTTP só é aceito para localhost em desenvolvimento.');
  }
  return url.toString();
}

export function providerRequestBody(context) {
  if (!context || typeof context!=='object') throw new Error('Contexto do Mestre ausente.');
  return {
    protocol:'braseiro.master.v1',
    instruction:'Produza apenas uma proposta estruturada. Este VTT é um sandbox de mundo vivo com narração de Mestre Humano: conduza com presença de mesa ao vivo, peso dramático, enquadramento sensorial (luz, ozônio, fumaça, sons e texturas), ancoragem física dos heróis (nomes próprios, armaduras, armas empunhadas e fivelas), mundo em movimento contínuo com zoom de câmera (Macro -> Meso -> Micro -> Estímulo Focal) e devolução de agência sem muletas genéricas (nunca pergunte "O que você faz?"). Sem roteiro obrigatório, sem missão forçada e sem conteúdo de aventura publicado carregado por padrão. Somente cinco aventuras publicadas são autorizadas: Under a Black Sun, Beyond the Rim, Long Arm of the Hutt, The Jewel of Yavin e Mask of the Pirate Queen. Aventuras fora deste escopo não podem ser propostas como campanhas oficiais. Respeite o grafo publicado da aventura ativa, não revele spoilers futuros e não invente NPCs, recompensas, dados ou eventos oficiais. A arquitetura é híbrida: regras, rolagens, inventários e consequências são autoridade exclusiva dos executores determinísticos locais; RAG fornece apenas referências recuperadas, e o provedor remoto fornece apenas proposta narrativa sujeita a validação. Na ausência de fonte ou executor, solicite decisão do Mestre sem inventar uma regra. Falhas de rede/provedor não podem alterar a campanha. Não altere ficha, combate ou estado diretamente. Para mudanças ficcionais persistentes, inclusive facções, agendas, oportunidades, relógios, mercados e afiliação de PNPCs, use somente worldOperations permitidas; o motor validará IDs e limites. NPCs e facções continuam agindo fora da cena quando o tempo avança. Não invente regra mecânica, preço, raridade, estatística ou valor numérico não fundamentado: para decisões mecânicas cite somente sourceRuleIds presentes em context.rules. Para pedidos explícitos do jogador por arma, armadura, equipamento ou veículo presentes em context.economy.catalog, use acquisitionRequest; não conclua a compra no mesmo turno. Recompensa, saque, presente ou furto de um ativo catalogado usa economyOperations grant_asset. Perda pode usar lose_asset; dano, reparo e venda exigem sourceRuleIds recuperados. Nunca escreva inventário diretamente. Se uma consequência causal de context.causalEncounters for narrada ou descartada, resolva-a com worldOperations resolve_hook. sceneLocationId só pode apontar para um local existente em context.world.locations. Proibido o uso de metalinguagem de software ou IA ("a cena responde", "como uma IA", "status:").',
    context
  };
}

export async function requestHttpMasterProposal({endpoint,context,fetchImpl=globalThis.fetch,timeoutMs=15000,signal=null}={}) {
  if (typeof fetchImpl!=='function') throw new Error('Cliente HTTP indisponível.');
  const url=validateProviderEndpoint(endpoint);
  const controller=new AbortController();
  const timer=setTimeout(()=>controller.abort(new Error('Tempo limite do Mestre excedido.')),Math.max(1,Number(timeoutMs)||15000));
  const abort=()=>controller.abort(signal?.reason ?? new Error('Solicitação do Mestre cancelada.'));
  if (signal) {
    if (signal.aborted) abort();
    else signal.addEventListener('abort',abort,{once:true});
  }
  try {
    const response=await fetchImpl(url,{
      method:'POST',
      headers:{'content-type':'application/json'},
      body:JSON.stringify(providerRequestBody(context)),
      signal:controller.signal
    });
    if (!response?.ok) throw new Error(`Mestre remoto respondeu com HTTP ${response?.status ?? 'desconhecido'}.`);
    const data=await response.json();
    if (!data || typeof data!=='object' || !data.proposal || typeof data.proposal!=='object') throw new Error('Resposta do Mestre remoto não contém uma proposta estruturada.');
    return {provider:'http',proposal:data.proposal,meta:data.meta ?? null};
  } catch (error) {
    if (controller.signal.aborted) throw new Error(signal?.aborted ? 'Solicitação do Mestre cancelada.' : 'Tempo limite do Mestre excedido.');
    throw error;
  } finally {
    clearTimeout(timer);
    if (signal) signal.removeEventListener?.('abort',abort);
  }
}

export function offlineMasterStatus(context) {
  return {provider:'offline',available:false,contextDigest:{corpusVersion:context?.corpusVersion ?? null,grounded:context?.retrieval?.grounded ?? false,ruleIds:(context?.rules ?? []).map(r=>r.id)}};
}
