# ETAPA 03 XMM — incremento de multiattack (2026-10-13)

Fonte pinada: 5etools-mirror-3/5etools-src@8c026b807fac21862a309379a0c3228a0683198b, data/bestiary/bestiary-xmm.json (blob 9a4137b508ef37562ed948894a70b1594b57e85c).

Foram indexadas 306 sequências Multiattack, 224 com planejamento estruturado de escolhas e contagens; 82 complexas são explicitamente bloqueadas para decisão manual. Contrato não rola dados, não aplica efeitos, não consome recursos nem avança turno. Testes parametrizados exercitam cada uma das 306 entradas.

A execução integral de combate NÃO está certificada. Pendências: cerca de 549 riders, 525 traits, 82 sequências complexas, integração das 132 fichas de conjuração com aplicação de efeitos. CI anterior 38373873687: 1332 testes, 1 falha em XmmSpellUsageGatewayTest.androidGatewayRoutesCatalogAndEligibility, pois OfficialBookRuntimeGateway não roteia spell_options/spell_usage_check. Essa pendência não foi resolvida neste incremento. Etapas 01/02 e Mystara congelados, ETAPA 04 não autorizada.
