"""Build auditable 100-row bases and five explicit lifecycle variants per row.
Rows are editorial templates, not claims of unique story outcomes.
"""
from pathlib import Path
import json, hashlib
ROOT=Path(__file__).resolve().parents[1]
STAGES=[('immediate',0,1,'primeiras repercussões',2),('settling',2,7,'ajuste nos dias seguintes',4),('established',8,30,'efeito que se incorpora à rotina',10),('dormant',31,365,'repercussão após um intervalo',30),('long_term',366,36500,'marca que atravessa os anos',90)]
CAUSES={
'loss':['morte confirmada de alguém próximo','desaparecimento ainda sem desfecho','perda de uma moradia','destruição de um objeto significativo','ruptura de um vínculo importante'],
'victory':['ameaça derrotada','pessoa resgatada','comunidade protegida','objetivo difícil alcançado','injustiça reparada'],
'failure':['objetivo não alcançado','tentativa de resgate malsucedida','acordo que não se sustentou','erro reconhecido em serviço','oportunidade perdida'],
'violence':['exposição a uma agressão','participação em confronto','testemunho de destruição','atendimento às vítimas','descoberta de vestígios violentos'],
'care':['acolhimento recebido','cuidado oferecido','ajuda aceita','proteção durante uma crise','presença de alguém confiável'],
'promise':['promessa assumida','promessa cumprida','promessa adiada','compromisso rompido','obrigação renegociada'],
'betrayal':['confiança violada','segredo divulgado sem consentimento','aliança abandonada','acordo usado como armadilha','fraude descoberta'],
'discovery':['pista confirmada','lugar encontrado','segredo revelado legitimamente','informação corrigida','conhecimento aprendido'],
'journey':['travessia concluída','viagem interrompida','chegada a região desconhecida','retorno ao ponto de partida','separação durante deslocamento'],
'scarcity':['falta de alimentos','falta de abrigo','falta de recursos de trabalho','interrupção de transporte','dificuldade de acesso a cuidados'],
'conflict':['disputa entre vizinhos','conflito institucional','rivalidade entre grupos','desacordo na equipe','pressão de uma autoridade'],
'reputation':['feito testemunhado','notícia recebida','relato distorcido','acusação conhecida','reconhecimento público'],
'work':['tarefa profissional concluída','mudança de função','projeto iniciado','ferramenta perdida','responsabilidade ampliada'],
'home':['retorno ao lar','mudança de residência','reparo concluído','visita recebida','espaço pessoal reorganizado'],
'health':['ferimento confirmado pelas regras','recuperação validada','cansaço após esforço','cuidado médico disponível','limitação reconhecida pelo sistema'],
'recovery':['apoio continuado','dificuldade superada','rotina restabelecida','vínculo reconstruído','segurança recuperada'],
'change':['passagem de estação','mudança de liderança','nova fronteira','transformação econômica','alteração de uma tradição local'],
'justice':['captura confirmada','julgamento realizado','reparação concedida','inocência demonstrada','investigação encerrada'],
'celebration':['festa comunitária','aniversário significativo','reencontro','obra apresentada','tradição celebrada'],
'legacy':['aposentadoria escolhida','aprendiz formado','objeto herdado','história transmitida','responsabilidade recebida']}
DOMAINS={
'sleep':['horário de repouso','despertar','sonhos lembrados','segurança para dormir','ambiente do descanso'],
'memory':['associação sensorial','lembrança de uma pessoa','objeto evocativo','data marcante','lugar reconhecido'],
'emotion':['alívio','saudade','orgulho','inquietação','gratidão'],
'habits':['ritual de partida','organização de pertences','rotina de chegada','forma de pedir ajuda','costume compartilhado'],
'family':['visita familiar','conversa reservada','responsabilidade doméstica','notícia de parentes','celebração em família'],
'intimacy':['tempo a dois','privacidade','conversa sobre confiança','limites de convivência','gesto de carinho'],
'work':['ferramentas','agenda de tarefas','reconhecimento profissional','limites de responsabilidade','projeto em andamento'],
'resources':['estoques','acesso a transporte','moradia','renda disponível','despesas previstas'],
'social':['amizades','encontros cotidianos','convites','convivência comunitária','rede de apoio'],
'reputation':['versão pública do feito','alcance regional','fonte do relato','expectativas de terceiros','reconhecimento individual'],
'npc':['projeto de um conhecido','pedido de um aliado','notícia de um rival','mudança de ofício','vida doméstica de alguém'],
'world':['rota comercial','instituição local','paisagem transformada','distribuição de serviços','organização comunitária'],
'recovery':['apoio acessível','intervalo de descanso','retomada de atividade','progresso gradual','segurança percebida'],
'belief':['interpretação de uma experiência','dúvida reconhecida','convicção questionada','informação incompleta','certeza reconsiderada'],
'goals':['projeto pessoal','aprendizado','promessa pendente','plano de viagem','desejo de estabilidade'],
'body':['sensação sem efeito mecânico','limitação já confirmada','adaptação do ambiente','cuidado disponível','recuperação em curso'],
'home':['lembrança material','espaço de convivência','manutenção','hospitalidade','pertencimento'],
'community':['tradição local','cuidado coletivo','decisão pública','mutirão','acolhimento de recém-chegados'],
'legacy':['aprendizado transmitido','objeto com história','sucessão de função','registro de um feito','memória de alguém ausente'],
'justice':['andamento de um caso','condição de custódia','reparação','relato de testemunha','consequência institucional']}
# Each group has five individually authored manifestations, not serial-number filler.
SCENES={
'memory':('memory',['loss','discovery','journey','legacy'],['Um cheiro familiar evoca uma passagem já vivida.','Um objeto guardado chama atenção durante uma tarefa comum.','Um lugar conhecido parece diferente após o que aconteceu.','Uma frase de alguém lembra uma conversa antiga.','Uma data no calendário traz uma lembrança específica.']),
'sleep':('sleep',['violence','journey','health','home'],['Um ruído interfere no ambiente de descanso.','Uma lembrança aparece em um sonho sem impor interpretação.','O abrigo oferece uma condição de repouso melhor que a anterior.','A tranquilidade contrasta com um período recente de tensão.','Uma conversa cria a oportunidade de ajustar a rotina de repouso.']),
'family':('family',['loss','home','care','change'],['Um parente pede notícias do tempo em que estiveram separados.','Uma visita traz um assunto doméstico ainda pendente.','Uma celebração familiar ocorre enquanto outras preocupações continuam.','Alguém da família oferece ajuda prática.','Uma notícia de parentes exige apenas decidir se haverá resposta.']),
'intimacy':('intimacy',['care','betrayal','home','promise'],['Uma pessoa próxima propõe uma conversa reservada.','Um gesto de cuidado abre espaço para uma resposta livre.','Uma mudança de planos deixa menos tempo para a convivência.','Uma lembrança compartilhada surge numa tarefa cotidiana.','Um limite de convivência é apresentado para discussão.']),
'work':('work',['work','failure','victory','justice'],['Um colega solicita opinião sobre uma tarefa semelhante à anterior.','Uma ferramenta associada ao trabalho reaparece em uso.','Uma responsabilidade pode ser redistribuída por negociação.','Um projeto de terceiro avança e altera a rotina de trabalho.','Uma oportunidade profissional depende de uma resposta do personagem.']),
'resources':('resources',['scarcity','work','change','journey'],['Um fornecedor informa disponibilidade diferente de um recurso.','Uma rota alternativa passa a ser oferecida por alguém.','Uma despesa conhecida chega ao prazo previsto.','Um estoque pode ser conferido antes de uma decisão importante.','Uma ajuda material chega acompanhada de condições explícitas.']),
'social':('social',['celebration','care','reputation','home'],['Um conhecido faz um convite sem relação com a missão atual.','Uma conversa casual mostra que a vida local continuou.','Um grupo mantém uma tradição apesar das mudanças.','Uma amizade oferece companhia para uma atividade comum.','Um encontro breve permite retomar contato sem criar uma obrigação.']),
'reputation':('reputation',['reputation','victory','failure','justice'],['Um relato conhecido é repetido com uma diferença identificável.','Uma pessoa pergunta se o personagem é quem participou do feito.','Um reconhecimento pequeno aparece numa situação cotidiana.','Uma expectativa alheia entra em conflito com a versão de outra pessoa.','Uma testemunha oferece contar o que realmente presenciou.']),
'npc':('npc',['work','care','conflict','legacy'],['Um conhecido apresenta progresso em um projeto pessoal.','Uma pessoa mudou sua rotina por motivos próprios.','Um aliado busca ajuda para uma questão que não envolve o antigo inimigo.','Um rival encontra uma limitação concreta em seus planos.','Um antigo contato passa uma responsabilidade adiante.']),
'world':('world',['change','victory','scarcity','conflict'],['Uma rota mostra sinais de uso diferentes dos anteriores.','Um serviço local passa a funcionar em outro horário.','Uma decisão institucional começa a produzir efeitos observáveis.','Uma obra muda a circulação de pessoas.','Um costume público acompanha uma transformação já confirmada.']),
'recovery':('recovery',['recovery','care','health','loss'],['Uma pessoa confiável se dispõe a ouvir sem pressionar.','Uma atividade habitual pode ser retomada em condições mais favoráveis.','Um apoio prático reduz uma dificuldade cotidiana.','Uma melhora já observada é reconhecida sem prometer cura.','Um intervalo permite escolher entre descanso e outra atividade.']),
'belief':('belief',['discovery','betrayal','justice','failure'],['Um novo testemunho pode esclarecer uma dúvida existente.','Uma interpretação antiga é confrontada com evidência verificável.','Uma pessoa admite que não tinha todas as informações.','Um relato contraditório pode ser investigado sem mudar o fato canônico.','Um detalhe permite distinguir suspeita de certeza.']),
'goals':('goals',['promise','legacy','work','journey'],['Um recurso necessário a um projeto pessoal fica disponível.','Uma promessa pode ser retomada em circunstâncias diferentes.','Um aprendiz pergunta sobre o próximo passo de sua formação.','Uma rota oferece acesso a um objetivo já escolhido.','Um plano pessoal encontra um obstáculo concreto e negociável.']),
'body':('body',['health','journey','recovery','violence'],['O ambiente oferece uma adaptação útil a uma limitação já confirmada.','Um cuidado disponível pode ser considerado sem impor sua aceitação.','Uma tarefa pode ser dividida para respeitar limites conhecidos.','Uma pessoa pergunta como acomodar uma necessidade já registrada.','Um sinal corporal passageiro pode ser narrado sem diagnóstico ou penalidade.']),
'home':('home',['home','legacy','loss','care'],['Um reparo necessário é lembrado ao usar um cômodo.','Um presente antigo permanece onde foi deixado.','Uma visita encontra marcas da história dos moradores.','Uma mudança no espaço comum pede uma decisão dos envolvidos.','Um objeto doméstico evoca alguém que passou por ali.']),
'community':('community',['celebration','change','scarcity','victory'],['Moradores organizam uma tarefa coletiva.','Uma tradição recebe novos participantes.','Uma comunidade decide como usar um recurso recuperado.','Uma pessoa recém-chegada pede orientação sobre costumes locais.','Um pequeno gesto público reconhece uma ajuda recebida.']),
'legacy':('legacy',['legacy','loss','victory','work'],['Um aprendiz usa algo que aprendeu com o personagem.','Um objeto muda de mãos preservando sua origem.','Uma história é contada a alguém que não viveu os acontecimentos.','Uma função pode ser assumida por uma nova pessoa.','Uma marca deixada no mundo aparece numa rotina alheia.']),
'justice':('justice',['justice','discovery','reputation','conflict'],['Uma instituição fornece uma atualização verificável do caso.','Uma testemunha mantém uma versão que pode ser confrontada com evidências.','Uma reparação produz um efeito cotidiano perceptível.','Um contato pergunta sobre o estado real de um procedimento.','Uma notícia distingue situação legal confirmada de rumor.']),
'emotion':('emotion',['victory','loss','celebration','recovery'],['Uma situação dá espaço para expressar alívio, se o jogador quiser.','Uma conquista recebe reconhecimento de alguém próximo.','Uma ausência é percebida durante uma ocasião comum.','Um agradecimento pode ser recebido sem criar dívida obrigatória.','Uma pausa permite ao jogador dizer como o personagem vive a mudança.']),
'habits':('habits',['journey','promise','home','work'],['Um costume já observado volta a ser possível.','Uma rotina encontra uma interrupção com causa identificável.','Um companheiro pergunta se manterão um ritual compartilhado.','Uma tarefa recorrente pode ser realizada de outro modo.','Um hábito escolhido pelo jogador é reconhecido por quem o presenciou.'])}
# Thematic packs add setting-specific situations; they use the same coherence engine.
THEMES={
'gurps-investigacao':('Investigações e vida civil',[
('justice',['justice','discovery'],'Um colega comunica o andamento confirmado de um inquérito'),('work',['work','failure'],'Uma escala de serviço muda a disponibilidade de colegas'),('family',['home','care'],'Uma visita familiar coincide com uma responsabilidade profissional'),('belief',['justice','betrayal'],'Uma testemunha apresenta um detalhe que exige verificação'),('memory',['violence','loss'],'Um elemento do ambiente recorda um caso encerrado'),('sleep',['violence','work'],'O ambiente de repouso é afetado pela rotina de plantão'),('recovery',['care','recovery'],'Um colega oferece cobertura para uma pausa escolhida'),('reputation',['reputation','justice'],'Uma reportagem repete uma versão pública do caso'),('resources',['work','scarcity'],'Um equipamento precisa ser compartilhado entre equipes'),('npc',['work','change'],'Um antigo parceiro assume outra função'),('home',['home','loss'],'Um objeto doméstico guarda uma lembrança de antes do caso'),('goals',['promise','work'],'Um curso desejado abre uma nova turma'),('community',['justice','care'],'Moradores organizam apoio a pessoas afetadas'),('legacy',['legacy','work'],'Um investigador novo consulta um procedimento antigo'),('body',['health','recovery'],'Uma adaptação no posto de trabalho fica disponível'),('intimacy',['care','promise'],'Uma pessoa próxima pede tempo para uma conversa'),('social',['celebration','home'],'Colegas fazem um convite fora do expediente'),('world',['change','justice'],'Uma instituição altera seu atendimento ao público'),('emotion',['loss','recovery'],'Um reconhecimento discreto recorda quem ajudou no caso'),('habits',['work','journey'],'Uma rotina de deslocamento precisa de ajuste')]),
'gurps-outros':('GURPS em outros cenários',[
('journey',['journey','discovery'],'Uma rota conhecida recebe um novo viajante'),('work',['work','change'],'Uma oficina reorganiza as tarefas'),('family',['home','legacy'],'Um parente transmite notícias de outra geração'),('belief',['discovery','failure'],'Uma informação aprendida encontra uma exceção verificável'),('memory',['loss','home'],'Um objeto comum liga dois períodos da vida'),('sleep',['journey','health'],'Um abrigo apresenta condições diferentes de repouso'),('recovery',['care','recovery'],'Uma rede de apoio oferece ajuda prática'),('reputation',['victory','reputation'],'Um feito chega a alguém por uma fonte identificável'),('resources',['scarcity','change'],'Um fornecedor oferece uma alternativa de abastecimento'),('npc',['conflict','work'],'Um contato negocia uma prioridade própria'),('home',['home','care'],'Uma visita altera a rotina de uma casa'),('goals',['promise','legacy'],'Um compromisso pessoal encontra ocasião para avançar'),('community',['celebration','victory'],'Uma comunidade prepara uma ocasião de encontro'),('legacy',['legacy','loss'],'Uma história de família é transmitida'),('body',['health','recovery'],'Uma tarefa recebe adaptação para uma necessidade conhecida'),('intimacy',['care','betrayal'],'Um vínculo importante encontra espaço para conversa'),('social',['home','celebration'],'Um conhecido propõe uma atividade cotidiana'),('world',['change','scarcity'],'Uma mudança econômica altera um serviço'),('emotion',['victory','recovery'],'Uma pequena melhora é percebida por alguém próximo'),('habits',['journey','work'],'Um costume precisa ser combinado entre viajantes')]),
'forbidden-lands':('Forbidden Lands',[
('resources',['scarcity','journey'],'O estoque de provisões exige conferência antes da marcha'),('home',['home','victory'],'A fortaleza mostra o resultado de um reparo confirmado'),('npc',['work','care'],'Um artesão do povoado desenvolve um projeto próprio'),('community',['conflict','change'],'Um clã discute um uso comum da terra'),('memory',['loss','journey'],'Um sinal na trilha evoca uma travessia anterior'),('sleep',['journey','health'],'O acampamento oferece condições de descanso diferentes'),('recovery',['care','recovery'],'Um companheiro oferece ajuda numa tarefa do acampamento'),('reputation',['victory','reputation'],'Uma história do grupo chega a outro povoado'),('world',['change','scarcity'],'Uma rota de comércio sofre um desvio verificável'),('belief',['discovery','betrayal'],'Uma superstição local pode ser confrontada com vestígios'),('family',['home','legacy'],'Uma família guarda uma história do tempo em que se separou'),('goals',['promise','work'],'Um material útil a um projeto pode ser obtido por negociação'),('legacy',['legacy','loss'],'Um objeto de um viajante antigo preserva sua procedência'),('body',['health','journey'],'Uma limitação já confirmada pede adaptação na viagem'),('intimacy',['care','promise'],'A vigília oferece ocasião para uma conversa reservada'),('social',['celebration','home'],'Uma mesa de taverna reúne conhecidos de outra viagem'),('justice',['justice','conflict'],'Uma liderança local esclarece uma cobrança'),('emotion',['victory','recovery'],'Um retorno seguro recebe um acolhimento discreto'),('habits',['journey','home'],'Um costume de preparação de acampamento reaparece'),('work',['work','failure'],'Uma ferramenta precisa de manutenção antes de novo uso')]),
'dnd5e':('D&D 5e',[
('reputation',['victory','reputation'],'Uma guilda recebe notícia de um feito do grupo'),('justice',['justice','conflict'],'Uma autoridade informa o estado confirmado de uma disputa'),('home',['home','care'],'A pousada reserva um espaço conhecido para o retorno'),('npc',['work','legacy'],'Um aprendiz de ofício demonstra o que aprendeu'),('memory',['loss','discovery'],'Um objeto de uma aventura evoca uma passagem vivida'),('sleep',['journey','health'],'Um abrigo altera as condições do repouso'),('recovery',['care','recovery'],'Uma pessoa de confiança oferece companhia'),('resources',['scarcity','work'],'Um comerciante explica uma mudança de disponibilidade'),('world',['change','victory'],'Uma estrada segura volta a receber viajantes'),('belief',['discovery','betrayal'],'Um relato de templo ou guilda exige verificar sua fonte'),('family',['home','legacy'],'Um parente envia notícias de sua vida comum'),('goals',['promise','work'],'Um projeto pessoal encontra um mestre ou ferramenta útil'),('community',['celebration','victory'],'Um bairro prepara uma homenagem modesta'),('legacy',['legacy','loss'],'Um herdeiro pergunta pela história de um objeto'),('body',['health','recovery'],'Uma atividade é adaptada a uma condição já validada'),('intimacy',['care','promise'],'Uma pessoa próxima propõe tempo longe das obrigações'),('social',['celebration','home'],'Um conhecido convida o grupo para uma refeição'),('emotion',['victory','failure'],'Uma conquista ou revés pode ser comentado por quem o viveu'),('habits',['journey','home'],'Um ritual de partida é lembrado por um companheiro'),('work',['work','change'],'Uma guilda altera uma tarefa ou responsabilidade')]),
'pf2e-kingmaker':('Pathfinder 2e Kingmaker',[
('world',['change','victory'],'Um assentamento mostra um efeito de decisão já tomada'),('resources',['scarcity','work'],'Uma necessidade de abastecimento chega ao conselho'),('justice',['justice','conflict'],'Uma petição pede que se examine um fato documentado'),('npc',['work','change'],'Um conselheiro concilia o cargo com um projeto próprio'),('memory',['loss','journey'],'Um marco das Terras Roubadas lembra uma exploração'),('sleep',['journey','health'],'A hospedagem de uma viagem muda as condições de repouso'),('recovery',['care','recovery'],'Um aliado oferece dividir uma responsabilidade'),('reputation',['victory','reputation'],'Uma comunidade comenta uma decisão do governo'),('home',['home','care'],'Um espaço da sede preserva marcas dos primeiros dias'),('belief',['discovery','betrayal'],'Um relatório de fronteira precisa de confirmação'),('family',['home','legacy'],'Uma família acompanha as mudanças de seu assentamento'),('goals',['promise','work'],'Um objetivo pessoal compete por tempo com o governo'),('community',['celebration','change'],'Moradores discutem uma obra ou festividade'),('legacy',['legacy','loss'],'Um aprendiz conhece a origem de uma instituição'),('body',['health','recovery'],'Uma reunião permite acomodar uma necessidade conhecida'),('intimacy',['care','promise'],'Uma pessoa próxima pede um momento fora do conselho'),('social',['celebration','home'],'Uma refeição reúne pessoas com vínculos diferentes'),('emotion',['victory','failure'],'Um resultado do reino é recebido por alguém afetado'),('habits',['work','journey'],'Uma rotina de inspeção encontra uma interrupção explicável'),('work',['work','failure'],'Uma função do conselho precisa de redistribuição')]),
'star-wars':('Star Wars EotE e AoR',[
('resources',['scarcity','journey'],'Uma peça ou provisão da nave exige conferência'),('world',['change','conflict'],'Uma rota ou porto altera um procedimento de acesso'),('justice',['justice','discovery'],'Um registro confirma o estado de uma detenção'),('npc',['work','care'],'Um contato da tripulação avança num objetivo próprio'),('memory',['loss','journey'],'Um som da nave evoca uma viagem anterior'),('sleep',['journey','health'],'A escala de bordo altera o ambiente de repouso'),('recovery',['care','recovery'],'Um tripulante oferece assumir uma tarefa por acordo'),('reputation',['reputation','victory'],'Um relato do grupo alcança um contato de outro porto'),('home',['home','legacy'],'Um canto da nave guarda objetos com história'),('belief',['discovery','betrayal'],'Uma transmissão pode ser conferida com outra fonte'),('family',['home','loss'],'Uma mensagem familiar chega por um canal disponível'),('goals',['promise','work'],'Uma parada oferece recurso para um plano pessoal'),('community',['celebration','care'],'Moradores de um porto organizam ajuda mútua'),('legacy',['legacy','work'],'Um tripulante novo aprende um procedimento antigo'),('body',['health','recovery'],'Uma função de bordo recebe adaptação necessária'),('intimacy',['care','promise'],'Uma pausa na escala permite uma conversa reservada'),('social',['celebration','home'],'Uma cantina reúne contatos de viagens distintas'),('emotion',['victory','failure'],'Um retorno à nave dá espaço para comentar o vivido'),('habits',['journey','work'],'Um ritual de preparação é reconhecido pela tripulação'),('work',['work','conflict'],'Uma missão exige negociar a distribuição de tarefas')]),
'o-um-anel':('O Um Anel 2e',[
('journey',['journey','discovery'],'Um caminho conhecido mostra sinais de passagem recente'),('home',['home','care'],'Um refúgio mantém algo preparado para o retorno'),('npc',['work','legacy'],'Um conhecido transmite uma habilidade a um aprendiz'),('community',['celebration','change'],'Uma comunidade conserva uma celebração entre dificuldades'),('memory',['loss','journey'],'Uma canção ou paisagem evoca uma jornada anterior'),('sleep',['journey','health'],'O abrigo muda a tranquilidade do repouso'),('recovery',['care','recovery'],'Uma companhia confiável oferece presença sem pressão'),('reputation',['victory','reputation'],'Uma história alcança outro salão por viajantes identificáveis'),('resources',['scarcity','journey'],'Provisões são conferidas antes de novo trecho'),('belief',['discovery','betrayal'],'Um rumor de estrada é comparado com testemunho direto'),('family',['home','legacy'],'Um parente conta como os dias passaram durante a ausência'),('goals',['promise','work'],'Uma promessa antiga encontra ocasião de cuidado'),('legacy',['legacy','loss'],'Um objeto herdado mantém a lembrança de quem o usou'),('body',['health','recovery'],'Uma tarefa da jornada é repartida conforme limites conhecidos'),('intimacy',['care','promise'],'Uma conversa à parte permite falar de saudade'),('social',['celebration','home'],'Uma mesa acolhe histórias sem exigir nova missão'),('justice',['justice','conflict'],'Uma liderança escuta um relato antes de tomar posição'),('emotion',['victory','recovery'],'Um gesto de hospitalidade dá espaço a esperança escolhida'),('habits',['journey','home'],'Uma tradição da companhia volta a ser lembrada'),('work',['work','change'],'Um ofício local acompanha as necessidades do povoado')]),
'cyberpunk-red':('Cyberpunk RED',[
('resources',['scarcity','work'],'Um contato informa mudança no acesso a peças ou serviços'),('world',['change','conflict'],'Um quarteirão muda sua circulação por decisão identificável'),('justice',['justice','discovery'],'Um registro pode confirmar a situação de uma pessoa'),('npc',['work','care'],'Um contato de rua persegue um plano próprio'),('memory',['loss','violence'],'Um detalhe urbano evoca um trabalho anterior'),('sleep',['home','health'],'Ruído ou segurança do prédio altera o ambiente de repouso'),('recovery',['care','recovery'],'Uma pessoa de confiança oferece ajuda concreta'),('reputation',['reputation','victory'],'Um feito circula por uma rede de contatos identificável'),('home',['home','legacy'],'Um objeto do apartamento conserva sua história'),('belief',['discovery','betrayal'],'Um arquivo contraditório exige verificação de origem'),('family',['home','loss'],'Uma mensagem de família chega em meio à rotina'),('goals',['promise','work'],'Uma oportunidade permite avançar num projeto escolhido'),('community',['celebration','scarcity'],'Vizinhos organizam um recurso ou encontro coletivo'),('legacy',['legacy','work'],'Um profissional novo consulta a experiência de alguém'),('body',['health','recovery'],'Uma adaptação prática atende uma condição já validada'),('intimacy',['care','promise'],'Uma pessoa próxima negocia tempo e privacidade'),('social',['celebration','home'],'Um convite oferece convivência fora do serviço'),('emotion',['victory','failure'],'Um resultado recente recebe uma reação de quem participou'),('habits',['journey','work'],'Um percurso habitual exige uma escolha diferente'),('work',['work','conflict'],'Um contrato altera a distribuição de tarefas')])}
# General responses are opportunities, never automatic voluntary actions.
RESPONSES={
'sleep':['examinar o ambiente de repouso','pedir uma mudança de escala','comentar o sonho se desejar','escolher uma pausa','manter a rotina atual'],
'memory':['falar da lembrança','guardar o assunto por enquanto','examinar o objeto associado','perguntar a quem também estava presente','registrar a passagem na crônica'],
'emotion':['expressar a reação escolhida','conversar com alguém de confiança','permanecer em silêncio','compartilhar um agradecimento','seguir com a tarefa atual'],
'habits':['manter um costume','propor um ajuste de rotina','perguntar aos companheiros','experimentar uma alternativa','adiar a decisão'],
'family':['responder à notícia','combinar uma visita','pedir mais informações','oferecer ajuda possível','decidir depois'],
'intimacy':['aceitar uma conversa','propor outro momento','explicar um limite','ouvir sem assumir compromisso','preservar privacidade'],
'work':['negociar a tarefa','solicitar um recurso','dividir uma responsabilidade','verificar um procedimento','recusar uma proposta'],
'resources':['conferir o estoque','buscar um fornecedor','negociar uma entrega','adaptar o plano','guardar recursos para depois'],
'social':['aceitar um convite','propor outra atividade','retomar um contato','conversar brevemente','declinar sem hostilidade'],
'reputation':['perguntar pela fonte','contar sua versão','consultar uma testemunha','deixar o relato sem resposta','investigar uma distorção'],
'npc':['perguntar pelo projeto','oferecer ajuda concreta','negociar um favor','acompanhar sem intervir','seguir com seus próprios planos'],
'world':['observar a mudança','consultar moradores','examinar os efeitos','propor uma intervenção','deixar a situação evoluir'],
'recovery':['aceitar apoio','pedir outro tipo de ajuda','escolher uma atividade habitual','buscar informação sobre cuidados','recusar a oferta por enquanto'],
'belief':['buscar confirmação','comparar testemunhos','revisar uma hipótese','manter a dúvida explícita','não concluir sem evidência'],
'goals':['dedicar tempo ao projeto','negociar acesso a um recurso','pedir orientação','mudar o plano por escolha própria','adiar o objetivo'],
'body':['solicitar uma adaptação','consultar cuidado disponível','repartir uma tarefa','explicar um limite conhecido','continuar conforme as regras permitirem'],
'home':['cuidar de um espaço','receber uma visita','examinar um objeto guardado','planejar um reparo','escolher descansar'],
'community':['participar da tarefa','ouvir a proposta coletiva','oferecer um recurso','negociar responsabilidades','acompanhar sem participar'],
'legacy':['contar uma história','ensinar algo conhecido','perguntar sobre a procedência','registrar uma transmissão','manter o objeto consigo'],
'justice':['consultar o registro','ouvir uma testemunha','distinguir rumor de fato','solicitar uma atualização','aguardar informação verificável']}
# GURPS/outros TOR journey domain maps to goals (journey itself is cause, not a life domain).
for _,(_,rows) in THEMES.items():
 for i,(domain,tags,text) in enumerate(rows):
  if domain=='journey':rows[i]=('goals',tags,text)

# Separate investigative specialisms and Star Wars modes, as requested.
THEMES['gurps-csi']=('GURPS CSI — perícia e investigação forense',[
('justice',['justice','discovery'],'O laboratório informa a situação documentada de um exame'),('work',['work','failure'],'Uma fila de análises exige definir prioridade com a equipe'),('belief',['discovery','justice'],'Um resultado inconclusivo pede distinguir hipótese de evidência'),('resources',['scarcity','work'],'Um reagente ou equipamento precisa de reposição'),('npc',['work','care'],'Um perito desenvolve um projeto profissional próprio'),('memory',['violence','loss'],'Um ambiente de trabalho evoca um caso anterior sem reabri-lo'),('sleep',['work','health'],'Uma escala de laboratório interfere no ambiente de repouso'),('recovery',['care','recovery'],'Um colega oferece ajuda para organizar uma carga de trabalho'),('reputation',['justice','reputation'],'Uma notícia descreve o papel da equipe na apuração'),('home',['home','care'],'Uma rotina doméstica prossegue após um turno exigente'),('family',['home','promise'],'Um compromisso familiar coincide com uma tarefa do laboratório'),('goals',['work','legacy'],'Uma formação técnica desejada abre inscrição'),('community',['justice','care'],'Uma ação de esclarecimento público depende de informação confirmada'),('legacy',['legacy','work'],'Um novo perito consulta um procedimento documentado'),('body',['health','recovery'],'Uma estação de trabalho admite uma adaptação necessária'),('intimacy',['care','promise'],'Uma pessoa próxima pede combinar tempo de convivência'),('social',['celebration','home'],'A equipe organiza um encontro fora do expediente'),('world',['change','justice'],'Uma instituição muda o fluxo de encaminhamento de exames'),('emotion',['failure','recovery'],'Uma revisão de trabalho oferece espaço para reconhecer limites'),('habits',['work','journey'],'Uma rotina de registro encontra uma interrupção verificável')])
THEMES['gurps-serial-killer']=('GURPS — serial killer, investigação e continuidade',[
('justice',['justice','discovery'],'Um registro confirma a condição atual de custódia de um suspeito'),('belief',['discovery','betrayal'],'Uma correspondência atribuída a alguém precisa ter origem verificada'),('work',['work','failure'],'Uma equipe revê o que está comprovado e o que segue em aberto'),('memory',['violence','loss'],'Um detalhe cotidiano recorda um caso sem criar conexão com o atual'),('sleep',['violence','health'],'Um elemento do ambiente interfere na tranquilidade do repouso'),('recovery',['care','recovery'],'Uma pessoa confiável oferece apoio sem presumir diagnóstico'),('family',['home','promise'],'Uma responsabilidade familiar continua durante a investigação'),('intimacy',['care','betrayal'],'Uma pessoa próxima pede conversar sobre limites de convivência'),('reputation',['justice','reputation'],'Uma versão pública do caso chega por uma fonte identificável'),('resources',['scarcity','work'],'Uma necessidade da equipe disputa recursos disponíveis'),('npc',['work','care'],'Um colega cuida de um objetivo independente do caso'),('home',['home','loss'],'Uma lembrança pessoal permanece no espaço doméstico'),('goals',['promise','legacy'],'Um plano de vida pode avançar sem retomar o crime anterior'),('community',['justice','care'],'Uma comunidade procura informação confirmada e apoio'),('legacy',['legacy','loss'],'Uma pessoa preserva a memória de alguém ausente'),('body',['health','recovery'],'Uma tarefa recebe adaptação para uma limitação validada'),('social',['celebration','home'],'Um convite oferece convivência sem discutir o caso'),('world',['change','justice'],'Uma instituição acompanha uma consequência já documentada'),('emotion',['victory','recovery'],'O encerramento de uma etapa permite reação escolhida pelo jogador'),('habits',['work','journey'],'Uma rotina cotidiana pede ajuste por um motivo verificável')])
THEMES['star-wars-aor']=('Star Wars — Age of Rebellion',[
('work',['work','conflict'],'Uma responsabilidade ligada ao Dever exige combinar tarefas na célula rebelde sem alterar sua pontuação'),('resources',['scarcity','journey'],'Uma base rebelde confere suprimentos para uma operação'),('world',['change','conflict'],'Uma comunicação informa mudança confirmada num posto imperial'),('justice',['justice','discovery'],'Um registro pode esclarecer a condição de um prisioneiro'),('npc',['work','care'],'Um integrante da célula mantém um projeto pessoal'),('memory',['loss','journey'],'Um detalhe da base recorda uma operação anterior'),('sleep',['work','health'],'Uma escala de vigília altera o repouso disponível'),('recovery',['care','recovery'],'Um aliado oferece dividir uma responsabilidade da célula'),('reputation',['victory','reputation'],'Um relato de serviço alcança outra unidade por canal identificado'),('home',['home','legacy'],'Um espaço da base guarda marcas de quem passou por ele'),('belief',['discovery','betrayal'],'Uma mensagem de inteligência exige confirmação de origem'),('family',['home','loss'],'Uma mensagem pessoal chega por um canal disponível'),('goals',['promise','work'],'Um objetivo pessoal encontra espaço entre responsabilidades'),('community',['care','victory'],'Civis organizam ajuda cuja aceitação permanece livre'),('legacy',['legacy','work'],'Um recruta consulta a experiência de outro integrante'),('body',['health','recovery'],'Uma função operacional recebe adaptação validada'),('intimacy',['care','promise'],'Uma pausa permite conversar fora das obrigações da célula'),('social',['celebration','home'],'Um encontro informal reúne pessoas da base'),('emotion',['victory','failure'],'Um resultado da operação permite reações individuais'),('habits',['journey','work'],'Um procedimento de preparação é revisto em conjunto')])
THEMES['star-wars-eote']=('Star Wars — Edge of the Empire',[
('resources',['scarcity','journey'],'Uma manutenção da nave disputa o orçamento da tripulação'),('work',['work','promise'],'Um contato relacionado a uma Obrigação solicita negociar condições sem alterar sua pontuação'),('world',['change','conflict'],'Um porto altera taxas ou acesso por motivo verificável'),('justice',['justice','discovery'],'Um documento pode esclarecer uma cobrança ou detenção'),('npc',['work','care'],'Um contato da Orla desenvolve um negócio próprio'),('memory',['loss','journey'],'Um detalhe de uma escala evoca uma viagem anterior'),('sleep',['journey','health'],'O abrigo da tripulação muda as condições de repouso'),('recovery',['care','recovery'],'Um tripulante oferece ajuda sem criar dívida automática'),('reputation',['reputation','victory'],'Um feito circula entre contatos de uma rota identificável'),('home',['home','legacy'],'Um canto da nave reúne lembranças de seus ocupantes'),('belief',['discovery','betrayal'],'Uma oferta de serviço precisa de confirmação de procedência'),('family',['home','loss'],'Uma mensagem pessoal encontra a tripulação em trânsito'),('goals',['promise','work'],'Um plano pessoal exige negociar tempo ou um recurso'),('community',['celebration','care'],'Moradores de um porto mantêm uma rede de apoio'),('legacy',['legacy','work'],'Um tripulante novo aprende um costume de bordo'),('body',['health','recovery'],'Uma tarefa da nave pode ser adaptada a limites conhecidos'),('intimacy',['care','promise'],'Uma parada oferece ocasião para conversa reservada'),('social',['celebration','home'],'Uma cantina reúne antigos e novos contatos'),('emotion',['victory','failure'],'Um contrato encerrado dá espaço para comentar suas consequências'),('habits',['journey','work'],'Uma rotina de partida encontra um obstáculo solucionável')])
REQUESTED=['gurps-investigacao','gurps-csi','gurps-serial-killer','o-um-anel','dnd5e','forbidden-lands','pf2e-kingmaker','star-wars-aor','star-wars-eote']
THEMES={k:THEMES[k] for k in REQUESTED}
# Explicit causal links: violence can evoke memory, conflict can affect work,
# betrayal can raise a question about evidence, victory can affect reputation,
# and failure can alter a work situation. These links never assert new facts.
CAUSAL_BRIDGES={'memory':['violence'],'work':['conflict','failure'],'belief':['betrayal'],'reputation':['victory']}
for pack,(title,seeds) in THEMES.items():
 THEMES[pack]=(title,[(domain,list(dict.fromkeys(tags+CAUSAL_BRIDGES.get(domain,[]))),text) for domain,tags,text in seeds])
# Editorial scene operations. V2.2 uses the first five for each of 100
# distinct situations; the remainder is retained as an optional authoring vocabulary.
BEATS=[
('observe','Um sinal pode ser observado sem exigir uma decisão.',None,False),
('conversation','Uma pessoa envolvida pode esclarecer o que sabe.','related_person',False),
('choice','Há espaço para uma decisão livre do personagem.',None,True),
('verification','A informação pode ser conferida antes de qualquer conclusão.',None,True),
('continuity','A situação pode seguir sem se tornar uma nova missão.',None,False),
('source','A origem de um relato pode ser perguntada sem presumir sua veracidade.','related_person',True),
('witness','Uma testemunha conhecida pode separar o que viu do que apenas ouviu.','witness_available',True),
('object','Um objeto de procedência registrada pode ajudar a situar a conversa.','related_object',False),
('record','Um registro disponível pode ser consultado.','record_available',True),
('two_accounts','Duas versões acessíveis podem ser comparadas sem reescrever o fato.','two_sources',True),
('privacy','O assunto pode ser tratado em particular, se os envolvidos aceitarem.','related_person',True),
('support','Uma ajuda concreta pode ser oferecida por alguém disponível.','support_available',True),
('cost','Um custo conhecido pode ser esclarecido antes de assumir compromisso.','cost_known',True),
('alternative','Uma alternativa já disponível pode ser apresentada com seus limites.','alternative_available',True),
('delay','A questão pode aguardar quando não houver prazo impeditivo.','can_wait',True),
('shared_task','Uma tarefa pode ser repartida por acordo.','related_person',True),
('refusal','Uma proposta pode ser recusada sem inventar uma consequência punitiva.',None,True),
('small_step','Um primeiro passo reversível pode ser considerado.',None,True),
('revisit','Uma decisão anterior pode ser revista se ainda for reversível.','reversible_decision',True),
('boundary','Uma pessoa pode expressar um limite, sem decidir pelo personagem.','related_person',False),
('thanks','Um agradecimento pode reconhecer ajuda que realmente ocorreu.','help_confirmed',False),
('news','Uma notícia só pode circular pelo canal que efetivamente a trouxe.','communication_available',False),
('return','Uma mudança pode ser comparada com o estado registrado na visita anterior.','previous_visit',False),
('handover','Uma responsabilidade pode ser transmitida por acordo explícito.','related_person',True),
('closure','Uma pendência pode ser encerrada quando suas condições forem satisfeitas.','resolution_available',True)]

def expand_common(rows,kind):
 out=[]
 for row in rows:
  for stage,lo,hi,label,cooldown in STAGES:
   out.append(dict(row,id=row['id']+'-'+stage,baseId=row['id'],stage=stage,minElapsedDays=lo,maxElapsedDays=hi,cooldownDays=cooldown,development=label))
 return out

CATALOG=[]
def write(name,base,expanded):
 assert len(base)==100,(name,len(base))
 assert len(expanded)==500 and len({r['id'] for r in expanded})==500
 p=ROOT/'data/matrices';p.mkdir(parents=True,exist_ok=True)
 for suffix,data in [('base100',base),('expanded500',expanded)]:
  (p/f'{name}.{suffix}.json').write_text(json.dumps({'schema':'braseiro.matrix-table/v1','id':name,'version':'2.2.0','rows':data},ensure_ascii=False,indent=2)+'\n')
 result={'id':name,'baseRows':100,'expandedRows':500,'file':f'{name}.expanded500.json','sha256':hashlib.sha256((p/f'{name}.expanded500.json').read_bytes()).hexdigest()}
 CATALOG.append(result)
 return result

rows=[]
for tag,items in CAUSES.items():
 for i,text in enumerate(items):rows.append({'id':f'cause-{tag}-{i+1}','text':text,'causeTags':[tag],'originCode':f'cause-{tag}-{i+1}','agency':'verified_origin'})
write('causes',rows,expand_common(rows,'causes'))
rows=[]
for domain,items in DOMAINS.items():
 for i,text in enumerate(items):rows.append({'id':f'domain-{domain}-{i+1}','text':text,'domain':domain,'agency':'context_only'})
write('domains',rows,expand_common(rows,'domains'))
rows=[]
for key,(domain,tags,items) in SCENES.items():
 for i,text in enumerate(items):rows.append({'id':f'universal-{key}-{i+1}','text':text,'domain':domain,'causeTags':tags,'agency':'narrative_opportunity','intensity':1 if i in [0,2,4] else 2,'requiresFacts':{},'forbidsFacts':{},'requiredCapabilities':[]})
write('manifestations-universal',rows,expand_common(rows,'manifestations'))
rows=[]
for domain,items in RESPONSES.items():
 for i,text in enumerate(items):rows.append({'id':f'response-{domain}-{i+1}','text':text,'domain':domain,'agency':'player_choice','choiceKey':f'option-{i+1}'})
write('responses',rows,expand_common(rows,'responses'))
rows=[]
MODES=['apresentar uma vez','aguardar uma cena pertinente','aguardar local relacionado','aguardar uma pessoa relacionada','aguardar uma pausa da atividade principal']
for domain in DOMAINS:
 for i,mode in enumerate(MODES):rows.append({'id':f'timing-{domain}-{i+1}','text':mode,'domain':domain,'mode':['once','relevant_scene','related_location','related_person','downtime'][i],'agency':'schedule_only','requiredCapabilities':[None,None,'related_location','related_person','downtime'][i:i+1]})
for row in rows:row['requiredCapabilities']=[v for v in row['requiredCapabilities'] if v]
write('timing',rows,expand_common(rows,'timing'))
# 80 newly authored situations per system, stored as editable editorial data.
extras={};current=None
for line in (ROOT/'scripts/extra-situations.txt').read_text().splitlines():
 line=line.strip()
 if not line:continue
 if line.startswith('['):current=line[1:-1];extras[current]=[];continue
 domain,body=line.split('|',1)
 if domain=='aftercare':domain='recovery'
 items=body.split(';');assert len(items)==4,(current,domain)
 for text in items:extras[current].append((domain,SCENES[domain][1],text))
for pack,(title,seeds) in THEMES.items():
 assert len(seeds)==20 and len(extras[pack])==80
 assert len({text for _,_,text in seeds+extras[pack]})==100,pack
 rows=[]
 for j,(domain,tags,text) in enumerate(seeds+extras[pack]):
  for i,(beat,extra,cap,voluntary) in enumerate(BEATS[:5]):
   row={'id':f'{pack}-{j+1}-{beat}','baseSituationId':f'{pack}-situation-{j+1}','text':text+'. '+extra,'domain':domain,'causeTags':tags,'agency':'narrative_opportunity','intensity':1 if i in [0,3,4] else 2,'requiresFacts':{},'forbidsFacts':{},'sceneMode':beat,'requiredCapabilities':[cap] if cap else [],'offersVoluntaryDecision':voluntary,'setting':pack,'allowedStages':[v[0] for v in STAGES],'cooldownDays':14,'valence':'positive' if domain in ['recovery','community','legacy'] else 'neutral'}
   if pack=='gurps-serial-killer' and (j==1 or any(w in text.lower() for w in ['carta disponível','assinatura atribuída'])):row['requiredCapabilities'].append('suspicious_correspondence_verified')
   if pack=='gurps-csi' and (j in [0,2] or any(w in text.lower() for w in ['amostra','laudo','exame'])):row['requiredCapabilities'].append('forensic_evidence_available')
   if j>=20 and any(w in text.lower() for w in ['documento','registro','relatório','laudo']):row['requiredCapabilities'].append('record_available')
   if any(word in text.lower() for word in ['colega','aliad','amigo','pessoa próxima','companheir','conselheir']):row['requiredCapabilities'].append('related_person')
   if any(word in text.lower() for word in ['promessa','compromisso','prometid']):row['requiredCapabilities'].append('promise_exists')
   if 'reparação prometida' in text.lower():row['requiredCapabilities'].append('reparation_promised')
   if any(word in text.lower() for word in ['reparação aprovada','reparação concedida']):row['requiredCapabilities'].append('reparation_approved')
   if 'recompensa já aprovada' in text.lower():row['requiredCapabilities'].append('reward_approved')
   if any(word in text.lower() for word in ['familiar','família','parente']):row['requiredCapabilities'].append('family_contact')
   if any(word in text.lower() for word in ['limitação','limite conhecido','necessidade conhecida','necessidade validada']):row['requiredCapabilities'].append('known_limitation')
   row['requiredCapabilities']=list(dict.fromkeys(row['requiredCapabilities']))
   if domain in ['work','resources'] and any(w in text.lower() for w in ['falta','limita','disputa','atras','lacuna']):row['valence']='challenging'
   rows.append(row)
 assert len({r['baseSituationId'] for r in rows})==100
 write('manifestations-'+pack,rows[:100],rows)
# Remove only builder-owned files for obsolete, unrequested settings.
for old in ['gurps-outros','cyberpunk-red','star-wars']:
 for suffix in ['base100','expanded500']:
  (ROOT/f'data/matrices/manifestations-{old}.{suffix}.json').unlink(missing_ok=True)
manifest={'schema':'braseiro.matrix-catalog/v1','version':'2.2.0','baselineExplanation':'A V2 não tinha tabelas extensas. As bases explícitas têm 100 entradas cada; as tabelas comuns possuem cinco estágios temporais por entrada. Cada tabela de sistema tem 100 situações distintas com cinco operações de cena: preserva as 20 situações-base (100 entradas) e acrescenta 80 situações (400 entradas). Nenhuma contagem representa histórias únicas.','stages':[{'id':a,'minDays':b,'maxDays':c,'description':d,'cooldownDays':e} for a,b,c,d,e in STAGES],'tables':CATALOG,'baseRows':sum(t['baseRows'] for t in CATALOG),'expandedRows':sum(t['expandedRows'] for t in CATALOG),'factor':5,'packs':[]}
for pack,(title,_) in THEMES.items():
 system='gurps' if pack.startswith('gurps-') else 'star-wars' if pack.startswith('star-wars-') else pack
 mode=pack.removeprefix('star-wars-') if system=='star-wars' else pack.removeprefix('gurps-') if system=='gurps' else 'default'
 manifest['packs'].append({'id':pack,'title':title,'system':system,'mode':mode,'tables':{'cause':'causes','domain':'domains','manifestation':'manifestations-'+pack,'timing':'timing','response':'responses'}})
# Each system has a self-contained five-axis table bundle for installation.
pack_dir=ROOT/'data/matrices/packs';pack_dir.mkdir(exist_ok=True)
for pack in manifest['packs']:
 tables={axis:json.loads((ROOT/f'data/matrices/{table_id}.expanded500.json').read_text())['rows'] for axis,table_id in pack['tables'].items()}
 bundle={'schema':'braseiro.matrix-bundle/v1','pack':dict(pack,version=manifest['version']),'tables':tables}
 body=(json.dumps(bundle,ensure_ascii=False,indent=2)+'\n').encode()
 (pack_dir/f'{pack["id"]}.json').write_bytes(body)
 pack['bundleFile']=f'packs/{pack["id"]}.json'
 pack['bundleSha256']=hashlib.sha256(body).hexdigest()
(ROOT/'data/matrices/catalog.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'tables':len(CATALOG),'baseline':manifest['baseRows'],'expanded':manifest['expandedRows'],'factor':5,'packs':[p['id'] for p in manifest['packs']]}))
