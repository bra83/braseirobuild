# OUA 4.9.38 — Journey / Turn / HUD source probe

### App.tsx — /const handleMapDiscoveryChanged/ — linha 1384

\`\`\`text
1356:     return inspired ? 2 : 1;
1357:   };
1358: 
1359:   const handleAskMasterRules = async (question: string): Promise<void> => {
1360:     if (isStoryLoading || !question.trim()) return;
1361:     setIsStoryLoading(true); setStoryError(null);
1362:     await processTurn(`[DÚVIDA PARA O MESTRE]: ${question.trim()}`, true);
1363:   };
1364: 
1365:   const handleCombatMasterAction = async (action: string): Promise<void> => {
1366:     if (isStoryLoading || !action.trim()) return;
1367:     setIsStoryLoading(true); setStoryError(null);
1368:     await processTurn(`[AÇÃO TÁTICA DA COMPANHIA] ${action.trim()}`, false);
1369:   };
1370: 
1371:   const handleWorldCommands = async (commands: MasterCommand[], reason: string): Promise<void> => {
1372:     const base = campaignRef.current;
1373:     if (!base) throw new Error('Nenhuma crônica ativa.');
1374:     const mechanical = applyMasterCommands(base, characters, allNpcs, commands);
1375:     if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
1376:     const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, reason, mechanical.npcs);
1377:     saveEmergencyCampaignMirror(committed);
1378:     campaignRef.current = committed;
1379:     setCampaign(committed);
1380:     setCharacters(mechanical.characters);
1381:     setAllNpcs(mechanical.npcs);
1382:   };
1383: 
1384:   const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {
1385:     const base = campaignRef.current;
1386:     if (!base) return;
1387: 
1388:     const route = Array.isArray(discovery.plannedRouteHexIds) ? discovery.plannedRouteHexIds.filter(Boolean) : [];
1389:     const partyIds = new Set(base.partyCharacterIds || []);
1390:     const party = characters.filter(character => partyIds.has(character.id));
1391:     let workingCampaign = base;
1392:     let workingCharacters = characters;
1393:     let workingNpcs = allNpcs;
1394:     let audits: any[] = [];
1395: 
1396:     const currentMaster = normalizeMasterState(base);
1397:     const destination = route.at(-1) || null;
1398:     const shouldStartJourney = route.length > 0 && party.length > 0 && (
1399:       currentMaster.journey.status === 'idle'
1400:       || currentMaster.journey.status === 'completed'
1401:       || currentMaster.journey.destinationHexId !== destination
1402:     );
1403: 
1404:     if (shouldStartJourney) {
1405:       const roles = autoJourneyRoles(party);
1406:       const guide = party.find(character => character.id === roles.guide) || party[0];
1407:       const march = rollJourneySkill(guide, 'Viagem');
1408:       const mechanical = applyMasterCommands(base, characters, allNpcs, [
1409:         {
1410:           type: 'start_journey',
1411:           destinationHexId: destination,
1412:           roles,
1413:           routeHexIds: route,
1414:           mounted: false,
1415:           forcedMarch: false,
1416:           reason: 'Rota planejada no Atlas: Jornada iniciada automaticamente.',
1417:         },
1418:         {
1419:           type: 'resolve_journey_march',
1420:           success: Boolean(march.success),
1421:           successIcons: Math.max(0, Number(march.tengwarCount) || 0),
1422:           roll: march,
1423:           reason: 'Primeira Jogada de Marcha automática do Guia ao iniciar a Jornada.',
1424:         },
1425:       ]);
1426:       if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
1427:       workingCampaign = mechanical.campaign;
1428:       workingCharacters = mechanical.characters;
1429:       workingNpcs = mechanical.npcs;
1430:       audits = mechanical.audits;
1431:     } else {
1432:       const masterState = normalizeMasterState(base);
1433:       workingCampaign = {
1434:         ...base,
1435:         masterState: {
1436:           ...masterState,
1437:           journey: {
1438:             ...masterState.journey,
1439:             plannedRouteHexIds: route,
1440:             destinationHexId: destination || masterState.journey.destinationHexId,
1441:           },
1442:           updatedAt: Date.now(),
1443:         },
1444:         updatedAt: Date.now(),
1445:       };
1446:     }
1447: 
1448:     const committed = await saveCanonicalTransaction({
1449:       campaign: workingCampaign,
1450:       characters: workingCharacters,
1451:       npcs: workingNpcs,
1452:       audits,
1453:       mapDiscovery: discovery,
1454:       reason: `estado do atlas: ${reason}`,
1455:     });
1456:     saveEmergencyCampaignMirror(committed);
1457:     campaignRef.current = committed;
1458:     setCampaign(committed);
1459:     setCharacters(workingCharacters);
1460:     setAllNpcs(workingNpcs);
1461:     window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
1462:       detail: { campaignId: discovery.campaignId, mapId: discovery.mapId, currentHexId: discovery.currentHexId },
1463:     }));
1464:   };
1465: 
1466:   const handlePartyRested = async (kind: 'short' | 'long') => {
1467:     const base = campaignRef.current;
1468:     if (!base) return;
1469:     const mechanical = applyMasterCommands(base, characters, allNpcs, [{
1470:       type: 'resolve_rest',
1471:       kind,
1472:       reason: kind === 'short' ? 'Descanso Curto resolvido pelo mapa.' : 'Descanso Prolongado resolvido pelo mapa.',
1473:       source: 'O Um Anel 2e — Descanso',
\`\`\`

### App.tsx — /const handlePartyMoved/ — linha 1491

\`\`\`text
1463:     }));
1464:   };
1465: 
1466:   const handlePartyRested = async (kind: 'short' | 'long') => {
1467:     const base = campaignRef.current;
1468:     if (!base) return;
1469:     const mechanical = applyMasterCommands(base, characters, allNpcs, [{
1470:       type: 'resolve_rest',
1471:       kind,
1472:       reason: kind === 'short' ? 'Descanso Curto resolvido pelo mapa.' : 'Descanso Prolongado resolvido pelo mapa.',
1473:       source: 'O Um Anel 2e — Descanso',
1474:     }]);
1475:     if (mechanical.rejected.length) {
1476:       throw new Error(mechanical.rejected.map(item => item.reason).join(' '));
1477:     }
1478:     const committed = await saveMasterTransaction(
1479:       mechanical.campaign,
1480:       mechanical.characters,
1481:       mechanical.audits,
1482:       kind === 'short' ? 'descanso curto' : 'descanso prolongado',
1483:     );
1484:     saveEmergencyCampaignMirror(committed);
1485:     campaignRef.current = committed;
1486:     setCampaign(committed);
1487:     setCharacters(mechanical.characters);
1488:     setAllNpcs(mechanical.npcs);
1489:   };
1490: 
1491:   const handlePartyMoved = async (move: PartyMapMove) => {
1492:     const base = campaignRef.current;
1493:     if (!base) return;
1494: 
1495:     const beforeMaster = normalizeMasterState(base);
1496:     const initialPlacement = !beforeMaster.position.currentHexId;
1497:     if (!initialPlacement) {
1498:       const activeJourney = ['planned','travelling','paused'].includes(beforeMaster.journey.status);
1499:       const nextRouteHexId = (beforeMaster.journey.plannedRouteHexIds || [])[0] || null;
1500:       if (!activeJourney || !nextRouteHexId) {
1501:         throw new Error('Movimento livre bloqueado: selecione um destino e use Planejar rota. Depois avance a Companhia pela Jornada.');
1502:       }
1503:       if (move.toHexId !== nextRouteHexId) {
1504:         throw new Error('Movimento bloqueado: a Companhia só pode avançar para o próximo hex da rota planejada.');
1505:       }
1506:       if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) {
1507:         throw new Error('Movimento bloqueado: resolva primeiro o Evento de Jornada atual.');
1508:       }
1509:       if (beforeMaster.journey.awaitingEndRoll) {
1510:         throw new Error('Movimento bloqueado: a Companhia alcançou o destino e precisa encerrar a Jornada.');
1511:       }
1512:       if (beforeMaster.journey.stageComplete) {
1513:         throw new Error('Movimento bloqueado: conclua a etapa atual da Jornada antes de avançar.');
1514:       }
1515:       if (Math.max(0, Number(beforeMaster.journey.marchHexesRemaining) || 0) <= 0) {
1516:         throw new Error('Movimento bloqueado: a Jogada de Marcha do Guia precisa ser resolvida antes do próximo hex.');
1517:       }
1518:     }
1519:     const beforeMinutes = Math.max(0, Number(beforeMaster.worldClock.elapsedMinutes) || 0);
1520:     let workingCampaign = base;
1521:     let workingCharacters = characters;
1522:     let workingNpcs = allNpcs;
1523:     let allAudits: any[] = [];
1524:     const journeyNotes: string[] = [];
1525: 
1526:     const applyCommands = (commands: MasterCommand[]) => {
1527:       const mechanical = applyMasterCommands(workingCampaign, workingCharacters, workingNpcs, commands);
1528:       if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
1529:       workingCampaign = mechanical.campaign;
1530:       workingCharacters = mechanical.characters;
1531:       workingNpcs = mechanical.npcs;
1532:       allAudits.push(...mechanical.audits);
1533:     };
1534: 
1535:     applyCommands([{
1536:       type: 'move_party',
1537:       toHexId: move.toHexId,
1538:       locationName: move.locationName,
1539:       distanceKm: move.distanceKm,
1540:       terrain: move.terrain,
1541:       reason: 'Movimento confirmado no Atlas pelo jogador.',
1542:     }]);
1543: 
1544:     const manifest = await loadRuntimeMapManifest().catch(() => null);
1545:     const currentHex = manifest?.hexes.find(hex => hex.id === move.toHexId);
1546:     let master = normalizeMasterState(workingCampaign);
1547: 
1548:     // When a March reaches its Event point, resolve the complete canonical Event
1549:     // immediately so moving on the map can never bypass Journey mechanics.
1550:     if (master.journey.awaitingEventGeneration) {
1551:       const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
1552:       applyCommands([{
1553:         type: 'generate_journey_event',
1554:         region,
1555:         targetDie: secureRoll(1,6),
1556:         featResults: journeyFeatResults(region),
1557:         terrain: move.terrain,
1558:         reason: 'Evento de Jornada disparado no hex alcançado pela Jogada de Marcha.',
1559:       }]);
1560:       master = normalizeMasterState(workingCampaign);
1561:       const event = master.journey.pendingEvent;
1562:       if (event) {
1563:         const targetId = master.company.journeyRoles[event.targetRole];
1564:         const target = workingCharacters.find(character => character.id === targetId);
1565:         if (!target) throw new Error('O Evento de Jornada apontou um papel sem Herói atribuído.');
1566:         const roll = rollJourneySkill(target, event.skill, Number(event.successDiceModifier || 0));
1567:         journeyNotes.push(`${event.description}: ${target.sheet.identity.name || target.name} — ${roll.success ? 'sucesso' : 'falha'}`);
1568:         applyCommands([{
1569:           type: 'resolve_journey_event',
1570:           targetCharacterId: target.id,
1571:           success: Boolean(roll.success),
1572:           successIcons: Math.max(0, Number(roll.tengwarCount) || 0),
1573:           roll,
1574:           reason: `Evento de Jornada ${event.description} resolvido automaticamente pelo motor local.`,
1575:         }]);
1576:       }
1577:     }
1578: 
1579:     master = normalizeMasterState(workingCampaign);
1580: 
\`\`\`

### App.tsx — /const processTurn/ — linha 782

\`\`\`text
754:     if (active.pendingTest) {
755:       setActiveTab('jogo');
756:       window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 40);
757:       return;
758:     }
759:     // A sugestão é apenas uma ação livre pré-escrita. O Diretor decide depois,
760:     // no contexto completo, se a ação realmente exige uma rolagem.
761:     cancelNarrationPlayback();
762:     setIsStoryLoading(true); setStoryError(null); setNarrationError(null);
763:     await processTurn(choice.text, false);
764:   };
765: 
766:   const handleCustomActionSubmit = async (event: React.FormEvent, isDmMode: boolean) => {
767:     event.preventDefault();
768:     const action = customAction.trim();
769:     const active = campaignRef.current;
770:     if (!action || isStoryLoading || !active) return;
771:     if (active.pendingTest && !isDmMode) {
772:       setActiveTab('jogo');
773:       window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 40);
774:       return;
775:     }
776:     cancelNarrationPlayback();
777:     setCustomAction('');
778:     setIsStoryLoading(true); setStoryError(null); setNarrationError(null);
779:     await processTurn(isDmMode ? `[DÚVIDA PARA O MESTRE]: ${action}` : action, isDmMode);
780:   };
781: 
782:   const processTurn = async (userAction: string, isDmMode: boolean, sourceCampaign?: CampaignState) => {
783:     let base = sourceCampaign || campaignRef.current;
784:     if (!base) return;
785:     userAction = String(userAction || '').trim();
786:     if (!userAction) {
787:       setStoryError('A ação estava vazia. A campanha e qualquer resultado pendente continuam preservados.');
788:       setIsStoryLoading(false);
789:       return;
790:     }
791:     base = await reconcileCampaignLocationWithAtlas(base);
792:     if (campaignRef.current?.id === base.id && campaignRef.current !== base) {
793:       campaignRef.current = base;
794:       setCampaign(base);
795:     }
796:     // O espelho é gravado antes de qualquer chamada externa. Toda preparação da
797:     // próxima cena ocorre em memória e só substitui esta revisão após o commit.
798:     saveEmergencyCampaignMirror(base);
799:     const inputChannel: 'PLAYER_ACTION' | 'GM_HELP' = isDmMode ? 'GM_HELP' : 'PLAYER_ACTION';
800:     const cleanUserAction = userAction.replace(/^\[DÚVIDA PARA O MESTRE\]:\s*/i, '');
801:     const narrativeIntent = classifyNarrativeIntent(cleanUserAction, inputChannel);
802:     const rulesOnlyTurn = inputChannel === 'GM_HELP';
803:     let activeTurnId: string | undefined;
804:     let activeTurnKey: string | undefined;
805:     try {
806:       if (!rulesOnlyTurn) {
807:         const begun = await TurnCoordinator.begin(base, userAction, inputChannel);
808:         if (begun.duplicateCommitted) {
809:           setIsStoryLoading(false);
810:           return;
811:         }
812:         if (begun.duplicateInFlight) {
813:           setStoryError('Este mesmo turno já está sendo processado. O segundo toque foi ignorado para não duplicar consequências.');
814:           setIsStoryLoading(false);
815:           return;
816:         }
817:         activeTurnId = begun.transaction.id;
818:         activeTurnKey = begun.transaction.idempotencyKey;
819:       }
820:       const barbaraMapDiscovery = await loadMapDiscovery(base.id).catch(() => null);
821:       const previousFiction = base.history.filter(node => node.narrativeMeta?.advancesFiction !== false);
822:       const currentBarbaraLocation = base.masterState?.position.locationName || base.currentLocation || '';
823:       const isHostOpening = /^\[INICIAR AVENTURA GUIADA\]/i.test(userAction) || previousFiction.length === 0;
824:       const hasVisitedBarbaraLocation = previousFiction.some(node => String(node.location || '').trim().toLocaleLowerCase('pt-BR') === String(currentBarbaraLocation).trim().toLocaleLowerCase('pt-BR'));
825:       const hostStoryOccasion: 'campaign_opening' | 'first_arrival' | 'continuation' = inputChannel === 'GM_HELP'
826:         ? 'continuation'
827:         : isHostOpening
828:           ? 'campaign_opening'
829:           : (!hasVisitedBarbaraLocation && currentBarbaraLocation ? 'first_arrival' : 'continuation');
830:       const projectedStateValidation = await validateBarbaraState(base, characters, allNpcs, barbaraMapDiscovery);
831:       const barbaraProjectedState = projectCampaignForBarbara(base, characters, allNpcs, barbaraMapDiscovery, hostStoryOccasion);
832:       const barbaraInspection = await inspectBarbaraTurn(barbaraProjectedState, cleanUserAction || userAction, isDmMode ? 'routine' : 'normal', inputChannel);
833:       if (projectedStateValidation.valid === false) {
834:         throw new Error(`Barbara recusou a projeção canônica antes do turno: ${projectedStateValidation.reason || projectedStateValidation.error || 'estado incompatível'}`);
835:       }
836:       const barbaraPlan = await getBarbaraPlan({
837:         action: cleanUserAction,
838:         inputChannel,
839:         location: base.masterState?.position.locationName || base.currentLocation,
840:         imprint: base.masterState?.position.currentHexId || '',
841:         narrative: inputChannel === 'PLAYER_ACTION',
842:         nonAdvancing: inputChannel === 'GM_HELP',
843:         importance: isDmMode ? 'routine' : 'normal',
844:         storyOccasion: hostStoryOccasion,
845:       });
846:       const barbaraOccasion = String(barbaraPlan.story_occasion || 'continuation');
847:       const orchestrator = await buildOrchestratorContext(base, userAction);
848:       const { selectedManuals, environment, rulesConsultation } = orchestrator;
849:       const continuityAnchor = buildSceneContinuityAnchor(base, userAction, allNpcs);
850:       const continuityContext = continuityPrompt(continuityAnchor);
851:       const narrativeCheckpoint = buildNarrativeCheckpoint(base, allNpcs);
852:       const narrativeContext = narrativeCheckpointPrompt(narrativeCheckpoint, narrativeIntent, userAction);
853:       const isOpeningScene = /^\[INICIAR AVENTURA GUIADA\]/i.test(userAction);
854:       const initialRhythm = buildNarrativeRhythmPlan(base, narrativeIntent, { isOpening: isOpeningScene });
855:       const rhythmContext = narrativeRhythmPrompt(initialRhythm);
856:       let directorContext = `${orchestrator.directorContext}${continuityContext}${narrativeContext}${rhythmContext}`;
857:       let publicContext = `${orchestrator.publicContext}${continuityContext}${narrativeContext}`;
858:       if (barbaraPlan.available) {
859:         const policy = barbaraPlan.narrative_policy || {};
860:         const supervisorAuthority = barbaraInspection?.authority || {};
861:         const supervisorWorldIntro = barbaraInspection?.world_introduction || {};
862:         const supervisorTurnPlan = barbaraInspection?.turn_plan || {};
863:         const commonContract = `
864: --- SUPERVISÃO BARBARA 1.1.3 (CONTRATO; HOST CONTINUA AUTORIDADE) ---
865: Canal vinculante: ${inputChannel}.
866: Ocasião narrativa: ${barbaraOccasion}.
867: Política de agência: ${String(policy.agency || policy.player_agency || 'preserve a agência do jogador')}.
868: Contrato de descoberta: ${JSON.stringify(barbaraPlan.world_discovery_contract || {})}.
869: Autoridade por domínio: ${JSON.stringify(supervisorAuthority)}.
870: Obrigação de apresentação do mundo: ${JSON.stringify(supervisorWorldIntro)}.
871: Plano supervisor do turno: ${JSON.stringify(supervisorTurnPlan)}.
\`\`\`

### App.tsx — /const handleWorldCommands/ — linha 1371

\`\`\`text
1343:     const base = campaignRef.current;
1344:     const character = characters.find(item => item.id === characterId);
1345:     if (!base || !character || !(base.partyCharacterIds || []).includes(characterId)) return 0;
1346:     const mechanical = applyMasterCommands(base, characters, allNpcs, [{
1347:       type: 'spend_hope', targetCharacterId: characterId, attribute,
1348:       reason: `Esperança gasta antes de uma jogada de ${attribute}.`, source: 'O Um Anel 2e — gasto de Esperança',
1349:     }]);
1350:     if (mechanical.rejected.length) return 0;
1351:     const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, 'gasto de Esperança');
1352:     saveEmergencyCampaignMirror(committed);
1353:     campaignRef.current = committed;
1354:     setCampaign(committed);
1355:     setCharacters(mechanical.characters);
1356:     return inspired ? 2 : 1;
1357:   };
1358: 
1359:   const handleAskMasterRules = async (question: string): Promise<void> => {
1360:     if (isStoryLoading || !question.trim()) return;
1361:     setIsStoryLoading(true); setStoryError(null);
1362:     await processTurn(`[DÚVIDA PARA O MESTRE]: ${question.trim()}`, true);
1363:   };
1364: 
1365:   const handleCombatMasterAction = async (action: string): Promise<void> => {
1366:     if (isStoryLoading || !action.trim()) return;
1367:     setIsStoryLoading(true); setStoryError(null);
1368:     await processTurn(`[AÇÃO TÁTICA DA COMPANHIA] ${action.trim()}`, false);
1369:   };
1370: 
1371:   const handleWorldCommands = async (commands: MasterCommand[], reason: string): Promise<void> => {
1372:     const base = campaignRef.current;
1373:     if (!base) throw new Error('Nenhuma crônica ativa.');
1374:     const mechanical = applyMasterCommands(base, characters, allNpcs, commands);
1375:     if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
1376:     const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, reason, mechanical.npcs);
1377:     saveEmergencyCampaignMirror(committed);
1378:     campaignRef.current = committed;
1379:     setCampaign(committed);
1380:     setCharacters(mechanical.characters);
1381:     setAllNpcs(mechanical.npcs);
1382:   };
1383: 
1384:   const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {
1385:     const base = campaignRef.current;
1386:     if (!base) return;
1387: 
1388:     const route = Array.isArray(discovery.plannedRouteHexIds) ? discovery.plannedRouteHexIds.filter(Boolean) : [];
1389:     const partyIds = new Set(base.partyCharacterIds || []);
1390:     const party = characters.filter(character => partyIds.has(character.id));
1391:     let workingCampaign = base;
1392:     let workingCharacters = characters;
1393:     let workingNpcs = allNpcs;
1394:     let audits: any[] = [];
1395: 
1396:     const currentMaster = normalizeMasterState(base);
1397:     const destination = route.at(-1) || null;
1398:     const shouldStartJourney = route.length > 0 && party.length > 0 && (
1399:       currentMaster.journey.status === 'idle'
1400:       || currentMaster.journey.status === 'completed'
1401:       || currentMaster.journey.destinationHexId !== destination
1402:     );
1403: 
1404:     if (shouldStartJourney) {
1405:       const roles = autoJourneyRoles(party);
1406:       const guide = party.find(character => character.id === roles.guide) || party[0];
1407:       const march = rollJourneySkill(guide, 'Viagem');
1408:       const mechanical = applyMasterCommands(base, characters, allNpcs, [
1409:         {
1410:           type: 'start_journey',
1411:           destinationHexId: destination,
1412:           roles,
1413:           routeHexIds: route,
1414:           mounted: false,
1415:           forcedMarch: false,
1416:           reason: 'Rota planejada no Atlas: Jornada iniciada automaticamente.',
1417:         },
1418:         {
1419:           type: 'resolve_journey_march',
1420:           success: Boolean(march.success),
1421:           successIcons: Math.max(0, Number(march.tengwarCount) || 0),
1422:           roll: march,
1423:           reason: 'Primeira Jogada de Marcha automática do Guia ao iniciar a Jornada.',
1424:         },
1425:       ]);
1426:       if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
1427:       workingCampaign = mechanical.campaign;
1428:       workingCharacters = mechanical.characters;
1429:       workingNpcs = mechanical.npcs;
1430:       audits = mechanical.audits;
1431:     } else {
1432:       const masterState = normalizeMasterState(base);
1433:       workingCampaign = {
1434:         ...base,
1435:         masterState: {
1436:           ...masterState,
1437:           journey: {
1438:             ...masterState.journey,
1439:             plannedRouteHexIds: route,
1440:             destinationHexId: destination || masterState.journey.destinationHexId,
1441:           },
1442:           updatedAt: Date.now(),
1443:         },
1444:         updatedAt: Date.now(),
1445:       };
1446:     }
1447: 
1448:     const committed = await saveCanonicalTransaction({
1449:       campaign: workingCampaign,
1450:       characters: workingCharacters,
1451:       npcs: workingNpcs,
1452:       audits,
1453:       mapDiscovery: discovery,
1454:       reason: `estado do atlas: ${reason}`,
1455:     });
1456:     saveEmergencyCampaignMirror(committed);
1457:     campaignRef.current = committed;
1458:     setCampaign(committed);
1459:     setCharacters(workingCharacters);
1460:     setAllNpcs(workingNpcs);
\`\`\`

### App.tsx — /applyMasterCommands/ — linha 38

\`\`\`text
10: import { SoundLibraryView } from './components/SoundLibraryView';
11: import { ManualLibraryView } from './components/ManualLibraryView';
12: import { CampaignView } from './components/CampaignView';
13: import { SettingsView } from './components/SettingsView';
14: import { TokensView } from './components/TokensView';
15: import { WorldAliveView } from './components/WorldAliveView';
16: import { MasterHubView } from './components/MasterHubView';
17: import { CompanyHubView } from './components/CompanyHubView';
18: import { MechanicsHub } from './components/MechanicsHub';
19: import { CombatTacticalView } from './components/CombatTacticalView';
20: import { OrganizationsView } from './components/OrganizationsView';
21: import { QuickAudioPanel } from './components/QuickAudioPanel';
22: import { answerRulesQuestion, generateDirectorPlan, generateNarration, generateNarrationFromPlan, generateSceneImage } from './services/geminiService';
23: import { composeVisualPrompt } from './utils/visualStyle';
24: import {
25:   CampaignState, Character, CharacterSheet, getActiveCampaign, getCharacters, getStoreData, IndividualToken, listSoundMetadata, Npc, PendingTest,
26:   recoverLatestCampaignSnapshot, createCampaignSnapshot, RollItem, saveEmergencyCampaignMirror, saveStoreData, saveMasterTransaction, saveCanonicalTransaction, SceneNode, SoundItem, SoundKeywordRule, clearStore,
27: } from './lib/db';
28: import { buildOrchestratorContext, buildTurnContext } from './lib/contextBuilder';
29: import { AudioEngine } from './lib/AudioEngine';
30: import { AudioDiagnostics } from './lib/audioDiagnostics';
31: import { ambienceSearchAliases, findBestSoundForEvent, findBestSoundForQuery, musicSearchAliases, planSounds, SoundEffectEvent, SoundPlan } from './lib/soundPlanner';
32: import { selectSceneAudioFromCatalog, SceneAudioContext } from './lib/sceneAudioPlanner';
33: import { applyRulesToSound, listSoundKeywordRules } from './lib/soundKeywordRules';
34: import { ensureSoundKeywordLibrary } from './lib/soundLibraryBootstrap';
35: import { RollResult, rollOneRing, secureRoll } from './lib/dice';
36: import { CampaignEvent, GeminiStoryResponse, SceneAudioPlan, StoryChoice, VoiceSegment, TtsVoiceName } from './types';
37: import type { RuleAnswer, MasterCommand, CompanyState, JourneyRole } from './lib/master/types';
38: import { applyMasterCommands } from './lib/master/commands';
39: import { isAdvisoryMasterRejection, sanitizeDirectorCommands } from './lib/master/commandSanitizer';
40: import { createDefaultMasterState, normalizeMasterState, recordHeroSpotlight } from './lib/master/state';
41: import { rememberDirectorPlan } from './lib/master/memory';
42: import { speakWithAndroid, stopAndroidSpeech } from './lib/narration';
43: import { createSkillTest, shouldSuppressRoutineTest } from './lib/rules/tests';
44: import { findSkill, targetNumberForSkill } from './lib/characterRules';
45: import { listNativeSounds } from './lib/native';
46: import { createRandomCharacter } from './lib/characterFactory';
47: import { normalizeEquipmentMechanics } from './lib/rules/equipment';
48: import { shadowAtLimit } from './lib/rules/shadow';
49: import { importCharacterJson } from './lib/characterPortability';
50: import { loadMapDiscovery, loadRuntimeMapManifest, loadRuntimeTerraMediaLocations, loadTerraMediaRuntimeContext, MAP_DISCOVERY_CHANGED_EVENT, resolveHexNarrativeLocation } from './lib/map';
51: import type { MapDiscovery } from './lib/map';
52: import { reconcileCanonicalState } from './lib/consistency';
53: import { migrateCampaignTo300, needsMigration300 } from './lib/migration300';
54: import { normalizeSceneTransitionCommands } from './lib/master/sceneMachine';
55: import { normalizeUnstartedFormalPhase } from './lib/master/planNormalizer';
56: import { autoAssignNpcTokens, getNpcTokenCatalog, syncBundledNpcTokens } from './lib/npcTokenLibrary';
57: import { AdventureProfileId, getAdventureProfile } from './lib/adventureProfile';
58: import { runStartupSupervisor } from './lib/autonomy/startupSupervisor';
59: import type { HealthReport } from './lib/autonomy/types';
60: import { TurnCoordinator } from './lib/autonomy/turnCoordinator';
61: import { finalizeBarbaraNarrative, getBarbaraDeviceProfile, getBarbaraPlan, inspectBarbaraTurn, projectCampaignForBarbara, validateBarbaraNarration, validateBarbaraState } from './lib/barbaraBridge';
62: import { createLocalDirectorPlan, createLocalNarration, isRecoverableGeminiFailure } from './lib/autonomy/localFallbackDirector';
63: import { buildSceneContinuityAnchor, buildSceneEnvironmentBinding, continuityPrompt, enforceDirectorContinuity, ensureEnvironmentAnchoredResponse, environmentBindingPrompt, narrationNeedsContinuityRepair, timePeriodFromHour } from './lib/sceneContinuity';
64: import { buildAutonomousEnvironmentContext, enrichAudioPlanWithEnvironment, prepareEnvironmentAdvance, prepareEnvironmentNarrativeMemory } from './lib/autonomy/environment';
65: import { MANUALS_DRIVE_FOLDER_ID } from './lib/driveFolders';
66: import { sanitizeNarrativeResponse } from './lib/narrativeSanitizer';
67: import { groundDirectorPlanInManuals } from './lib/ruleEvidence';
68: import { chooseOpeningArchitecture, chooseOpeningClock, openingInstruction, rememberOpening } from './lib/openingPlanner';
69: import { diversifySceneChoices, rememberChoices } from './lib/choiceDiversifier';
70: import { imageTargetFor, sceneImageFingerprint, targetMatchesScene } from './lib/sceneImageBinding';
71: import { enrichNpcPopulation } from './lib/npcPersonalityCatalog';
72: import { assessNarrativeQuality, buildNarrativeCheckpoint, classifyNarrativeIntent, enforceNarrativeIntent, narrativeCheckpointPrompt, professionalRepairBrief } from './lib/narrativeEngine';
73: import { assessNarrativeRhythm, buildNarrativeRhythmPlan, narrativeRhythmPrompt, rhythmRepairBrief } from './lib/narrativeRhythm';
74: import { reconcileSceneNpcPresence, synchronizeNpcPresenceFlags } from './lib/sceneEntitySync';
75: import { buildStoredSceneVisualPrompt } from './lib/sceneVisualPrompt';
76: 
77: 
78: const JOURNEY_ROLE_SKILL: Record<JourneyRole,string> = {
79:   guide: 'Viagem', scout: 'Exploração', hunter: 'Caçada', lookout: 'Vigilância',
80: };
81: 
82: function journeySkillRank(character: Character, skillName: string): number {
83:   return Math.max(0, Number(findSkill(character.sheet, skillName)?.rank) || 0);
84: }
85: 
86: function autoJourneyRoles(party: Character[]): Partial<Record<JourneyRole,string>> {
87:   if (!party.length) return {};
88:   const best = (skill: string) => [...party].sort((a,b) => journeySkillRank(b, skill) - journeySkillRank(a, skill))[0]?.id;
89:   return {
90:     guide: best('Viagem'),
91:     scout: best('Exploração'),
92:     hunter: best('Caçada'),
93:     lookout: best('Vigilância'),
94:   };
95: }
96: 
97: function rollJourneySkill(character: Character, skillName: string, extraDice = 0) {
98:   const skill = findSkill(character.sheet, skillName);
99:   const mode = shadowAtLimit(character.sheet) ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal';
100:   return rollOneRing(
101:     Math.max(0, Number(skill?.rank) || 0) + Math.trunc(extraDice),
102:     0,
103:     targetNumberForSkill(character.sheet, skillName),
104:     skillName,
105:     mode,
106:     Boolean(character.sheet.attributes.weary),
107:     Boolean(character.sheet.attributes.miserable),
108:   );
109: }
110: 
111: function classifyJourneyRegion(region: string, tags: string[] = []): 'border'|'wild'|'dark' {
112:   const text = (region + ' ' + tags.join(' ')).normalize('NFD').replace(/[̀-ͯ]/g,'').toLowerCase();
113:   if (/dark|shadow|sombr|mordor|angmar|barrow|tumular/.test(text)) return 'dark';
114:   if (/border|fronteira|shire|condado|bree|bri|rivendell|valfenda|lindon/.test(text)) return 'border';
115:   return 'wild';
116: }
117: 
118: function journeyFeatResults(region: 'border'|'wild'|'dark'): number[] {
119:   const count = region === 'wild' ? 1 : 2;
120:   return Array.from({length:count},()=>secureRoll(1,12));
121: }
122: 
123: function immersiveJourneyUiEnabled(): boolean {
124:   try {
125:     const query = new URLSearchParams(window.location.search);
126:     if (query.get('classic') === '1' || query.get('immersive') === '0') return false;
127:     if (query.get('immersive') === '1') return true;
\`\`\`

### App.tsx — /handlePartyRested/ — linha 1466

\`\`\`text
1438:             ...masterState.journey,
1439:             plannedRouteHexIds: route,
1440:             destinationHexId: destination || masterState.journey.destinationHexId,
1441:           },
1442:           updatedAt: Date.now(),
1443:         },
1444:         updatedAt: Date.now(),
1445:       };
1446:     }
1447: 
1448:     const committed = await saveCanonicalTransaction({
1449:       campaign: workingCampaign,
1450:       characters: workingCharacters,
1451:       npcs: workingNpcs,
1452:       audits,
1453:       mapDiscovery: discovery,
1454:       reason: `estado do atlas: ${reason}`,
1455:     });
1456:     saveEmergencyCampaignMirror(committed);
1457:     campaignRef.current = committed;
1458:     setCampaign(committed);
1459:     setCharacters(workingCharacters);
1460:     setAllNpcs(workingNpcs);
1461:     window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
1462:       detail: { campaignId: discovery.campaignId, mapId: discovery.mapId, currentHexId: discovery.currentHexId },
1463:     }));
1464:   };
1465: 
1466:   const handlePartyRested = async (kind: 'short' | 'long') => {
1467:     const base = campaignRef.current;
1468:     if (!base) return;
1469:     const mechanical = applyMasterCommands(base, characters, allNpcs, [{
1470:       type: 'resolve_rest',
1471:       kind,
1472:       reason: kind === 'short' ? 'Descanso Curto resolvido pelo mapa.' : 'Descanso Prolongado resolvido pelo mapa.',
1473:       source: 'O Um Anel 2e — Descanso',
1474:     }]);
1475:     if (mechanical.rejected.length) {
1476:       throw new Error(mechanical.rejected.map(item => item.reason).join(' '));
1477:     }
1478:     const committed = await saveMasterTransaction(
1479:       mechanical.campaign,
1480:       mechanical.characters,
1481:       mechanical.audits,
1482:       kind === 'short' ? 'descanso curto' : 'descanso prolongado',
1483:     );
1484:     saveEmergencyCampaignMirror(committed);
1485:     campaignRef.current = committed;
1486:     setCampaign(committed);
1487:     setCharacters(mechanical.characters);
1488:     setAllNpcs(mechanical.npcs);
1489:   };
1490: 
1491:   const handlePartyMoved = async (move: PartyMapMove) => {
1492:     const base = campaignRef.current;
1493:     if (!base) return;
1494: 
1495:     const beforeMaster = normalizeMasterState(base);
1496:     const initialPlacement = !beforeMaster.position.currentHexId;
1497:     if (!initialPlacement) {
1498:       const activeJourney = ['planned','travelling','paused'].includes(beforeMaster.journey.status);
1499:       const nextRouteHexId = (beforeMaster.journey.plannedRouteHexIds || [])[0] || null;
1500:       if (!activeJourney || !nextRouteHexId) {
1501:         throw new Error('Movimento livre bloqueado: selecione um destino e use Planejar rota. Depois avance a Companhia pela Jornada.');
1502:       }
1503:       if (move.toHexId !== nextRouteHexId) {
1504:         throw new Error('Movimento bloqueado: a Companhia só pode avançar para o próximo hex da rota planejada.');
1505:       }
1506:       if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) {
1507:         throw new Error('Movimento bloqueado: resolva primeiro o Evento de Jornada atual.');
1508:       }
1509:       if (beforeMaster.journey.awaitingEndRoll) {
1510:         throw new Error('Movimento bloqueado: a Companhia alcançou o destino e precisa encerrar a Jornada.');
1511:       }
1512:       if (beforeMaster.journey.stageComplete) {
1513:         throw new Error('Movimento bloqueado: conclua a etapa atual da Jornada antes de avançar.');
1514:       }
1515:       if (Math.max(0, Number(beforeMaster.journey.marchHexesRemaining) || 0) <= 0) {
1516:         throw new Error('Movimento bloqueado: a Jogada de Marcha do Guia precisa ser resolvida antes do próximo hex.');
1517:       }
1518:     }
1519:     const beforeMinutes = Math.max(0, Number(beforeMaster.worldClock.elapsedMinutes) || 0);
1520:     let workingCampaign = base;
1521:     let workingCharacters = characters;
1522:     let workingNpcs = allNpcs;
1523:     let allAudits: any[] = [];
1524:     const journeyNotes: string[] = [];
1525: 
1526:     const applyCommands = (commands: MasterCommand[]) => {
1527:       const mechanical = applyMasterCommands(workingCampaign, workingCharacters, workingNpcs, commands);
1528:       if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
1529:       workingCampaign = mechanical.campaign;
1530:       workingCharacters = mechanical.characters;
1531:       workingNpcs = mechanical.npcs;
1532:       allAudits.push(...mechanical.audits);
1533:     };
1534: 
1535:     applyCommands([{
1536:       type: 'move_party',
1537:       toHexId: move.toHexId,
1538:       locationName: move.locationName,
1539:       distanceKm: move.distanceKm,
1540:       terrain: move.terrain,
1541:       reason: 'Movimento confirmado no Atlas pelo jogador.',
1542:     }]);
1543: 
1544:     const manifest = await loadRuntimeMapManifest().catch(() => null);
1545:     const currentHex = manifest?.hexes.find(hex => hex.id === move.toHexId);
1546:     let master = normalizeMasterState(workingCampaign);
1547: 
1548:     // When a March reaches its Event point, resolve the complete canonical Event
1549:     // immediately so moving on the map can never bypass Journey mechanics.
1550:     if (master.journey.awaitingEventGeneration) {
1551:       const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
1552:       applyCommands([{
1553:         type: 'generate_journey_event',
1554:         region,
1555:         targetDie: secureRoll(1,6),
\`\`\`

### App.tsx — /presentNpcIds/ — linha 199

\`\`\`text
171:   skills: [
172:     'Fascínio', 'Atletismo', 'Vigilância', 'Caçada', 'Música', 'Ofício',
173:     'Indução', 'Viagem', 'Discernimento', 'Cura', 'Cortesia', 'Batalha',
174:     'Persuasão', 'Furtividade', 'Busca', 'Exploração', 'Enigma', 'História',
175:   ].map(name => ({ name, rank: 0, specialization: '', favorite: false })),
176:   combat: { parry: 0, armor: 'Nenhuma', helmet: 'Nenhum', shield: 'Nenhum', weapons: [] },
177:   resources: { coins: 0, treasure: 0, experience: 0, adventurePoints: 0, skillPoints: 0 },
178:   inventory: [],
179:   other: { virtues: '', rewards: '', flaws: '', notes: '', narrativeConditions: '' },
180:   tokenId: null,
181:   historyLog: [],
182: };
183: 
184: function newCampaign(): CampaignState {
185:   const now = Date.now();
186:   const campaign: CampaignState = {
187:     id: `campaign_${now}`,
188:     name: 'Nova Crônica de O Um Anel',
189:     systemProfileId: 'the-one-ring-2e',
190:     settingProfileId: 'middle-earth-third-age',
191:     rulesProfileName: 'O Um Anel 2e — Terra-média, Terceira Era',
192:     rulesEdition: '2e',
193:     rulesSourceFolderId: MANUALS_DRIVE_FOLDER_ID,
194:     characterSheet: structuredClone(DEFAULT_CHARACTER_SHEET),
195:     activeCharacterId: null, partyCharacterIds: [],
196:     currentLocation: 'Eriador',
197:     currentChapter: 'Prólogo',
198:     campaignSummary: 'Uma nova crônica aguarda seu primeiro passo.',
199:     recentEvents: '', clues: '', mysteries: '', quests: '', worldFlags: {}, presentNpcIds: [],
200:     history: [], pendingDiceRoll: null, pendingTest: null, events: [], firstImageGenerated: false,
201:     schemaVersion: 350, revision: 1, createdAt: now, updatedAt: now,
202:   };
203:   campaign.masterState = createDefaultMasterState(campaign);
204:   return campaign;
205: }
206: 
207: function normalizeCharacterSheet(input?: Partial<CharacterSheet> | null): CharacterSheet {
208:   const base = structuredClone(DEFAULT_CHARACTER_SHEET);
209:   if (!input) return base;
210:   const skills = Array.isArray(input.skills) && input.skills.length ? input.skills : base.skills;
211:   const merged: CharacterSheet = {
212:     ...base,
213:     ...input,
214:     identity: { ...base.identity, ...(input.identity || {}) },
215:     attributes: { ...base.attributes, ...(input.attributes || {}) },
216:     skills: skills.map(skill => ({ name: skill.name || 'Perícia', rank: Number(skill.rank) || 0, specialization: skill.specialization || '', favorite: Boolean(skill.favorite) })),
217:     combat: { ...base.combat, ...(input.combat || {}), weapons: Array.isArray(input.combat?.weapons) ? input.combat!.weapons : [] },
218:     resources: { ...base.resources, ...(input.resources || {}) },
219:     inventory: Array.isArray(input.inventory) ? input.inventory.map(item => ({
220:       ...item,
221:       id: item.id || `item_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
222:       name: item.name || 'Item sem nome',
223:       quantity: Math.max(0, Number(item.quantity) || 0),
224:       weight: Math.max(0, Number(item.weight) || 0),
225:       load: Math.max(0, Number(item.load ?? item.weight) || 0),
226:       category: item.category || 'Geral',
227:       description: item.description || '',
228:       equipped: Boolean(item.equipped),
229:     })) : [],
230:     other: { ...base.other, ...(input.other || {}) },
231:     tokenId: input.tokenId || null,
232:     historyLog: Array.isArray(input.historyLog) ? input.historyLog : [],
233:   };
234:   return normalizeEquipmentMechanics(merged);
235: }
236: 
237: function normalizeCampaignState(input: CampaignState): CampaignState {
238:   const fallback = newCampaign();
239:   const history = Array.isArray(input.history) ? input.history.filter(node => node?.id && node.atmospheric_narration != null) : [];
240:   const pendingTest = input.pendingTest
241:     ? {
242:         ...input.pendingTest,
243:         skill: String(input.pendingTest.skill || 'Perícia apropriada'),
244:         reason: String(input.pendingTest.reason || 'Teste pendente'),
245:         originalChoice: String(input.pendingTest.originalChoice || input.pendingTest.reason || `Resolver teste de ${input.pendingTest.skill || 'perícia'}`).trim(),
246:         createdAt: Number(input.pendingTest.createdAt) || Date.now(),
247:       }
248:     : null;
249:   const normalized: CampaignState = {
250:     ...fallback,
251:     ...input,
252:     id: input.id || fallback.id,
253:     name: input.name || fallback.name,
254:     systemProfileId: input.systemProfileId || fallback.systemProfileId,
255:     settingProfileId: input.settingProfileId || fallback.settingProfileId,
256:     rulesProfileName: input.rulesProfileName || fallback.rulesProfileName,
257:     rulesEdition: input.rulesEdition || fallback.rulesEdition,
258:     rulesSourceFolderId: input.rulesSourceFolderId || fallback.rulesSourceFolderId,
259:     characterSheet: normalizeCharacterSheet(input.characterSheet),
260:     activeCharacterId: input.activeCharacterId || null,
261:     partyCharacterIds: Array.isArray(input.partyCharacterIds) ? Array.from(new Set(input.partyCharacterIds.filter(Boolean))) : [],
262:     currentLocation: input.currentLocation || history.at(-1)?.location || 'Eriador',
263:     currentChapter: input.currentChapter || 'Prólogo',
264:     campaignSummary: input.campaignSummary || fallback.campaignSummary,
265:     recentEvents: input.recentEvents || '',
266:     clues: input.clues || '',
267:     mysteries: input.mysteries || '',
268:     quests: input.quests || '',
269:     worldFlags: input.worldFlags || {},
270:     presentNpcIds: Array.isArray(input.presentNpcIds) ? input.presentNpcIds : [],
271:     history,
272:     pendingDiceRoll: input.pendingDiceRoll || null,
273:     pendingTest,
274:     events: Array.isArray(input.events) ? input.events : [],
275:     schemaVersion: Number(input.schemaVersion || 0),
276:     createdAt: input.createdAt || Date.now(),
277:     updatedAt: input.updatedAt || Date.now(),
278:   };
279:   normalized.masterState = normalizeMasterState(normalized);
280:   normalized.currentLocation = normalized.masterState.position.locationName || normalized.currentLocation;
281:   return normalized;
282: }
283: 
284: function makeEvent(type: CampaignEvent['type'], sceneId?: string, data?: CampaignEvent['data']): CampaignEvent {
285:   return { id: `event_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, type, timestamp: Date.now(), sceneId, data };
286: }
287: 
288: async function reconcileCampaignLocationWithAtlas(campaign: CampaignState, providedDiscovery?: MapDiscovery): Promise<CampaignState> {
\`\`\`

### App.tsx — /NPCs presentes/ — linha 1028

\`\`\`text
1000:           appliedCommands: mechanical.audits.filter(item => item.status === 'applied').map(item => item.command.type),
1001:           rejectedCommands: mechanical.rejected.length,
1002:           localFallback: localFallbackActive,
1003:         });
1004:       }
1005:       const npcCatalog = narrativeIntent.advancesFiction ? await getNpcTokenCatalog().catch(() => []) : [];
1006:       let mechanicallyUpdatedNpcs = narrativeIntent.advancesFiction
1007:         ? await enrichNpcPopulation(autoAssignNpcTokens(mechanical.npcs, npcCatalog))
1008:         : allNpcs;
1009:       let mechanicallyUpdated = mechanical.campaign;
1010:       if (mechanicallyUpdated.masterState && narrativeIntent.advancesFiction) {
1011:         mechanicallyUpdated.masterState = rememberDirectorPlan(mechanicallyUpdated.masterState, plan, sceneId);
1012:         if (actionHeroId) mechanicallyUpdated.masterState = recordHeroSpotlight(mechanicallyUpdated.masterState, actionHeroId, partyIds);
1013:       }
1014:       const mechanicallyUpdatedCharacters = mechanical.characters;
1015:       const activeAfterMechanics = mechanicallyUpdatedCharacters.find(character => character.id === mechanicallyUpdated.activeCharacterId);
1016:       if (activeAfterMechanics) mechanicallyUpdated.characterSheet = structuredClone(activeAfterMechanics.sheet);
1017: 
1018:       const finalClock = mechanicallyUpdated.masterState?.worldClock || normalizeMasterState(mechanicallyUpdated).worldClock;
1019:       // Releia o ambiente usando o relógio e a posição já validados deste turno.
1020:       // A função apenas reconcilia tempo/posição: clima não avança por narração.
1021:       // Assim cabeçalho, prosa, imagem e áudio partem do MESMO estado canônico.
1022:       const sceneEnvironment = await buildAutonomousEnvironmentContext(mechanicallyUpdated).catch(() => environment);
1023:       const environmentBinding = buildSceneEnvironmentBinding(sceneEnvironment, finalClock.hour, finalClock.minute);
1024:       const sensoryContext = environmentBindingPrompt(environmentBinding);
1025:       const finalSceneKind = plan.sceneKind || mechanicallyUpdated.masterState?.scene?.kind || initialRhythm.sceneKind;
1026:       const turnRhythm = buildNarrativeRhythmPlan(mechanicallyUpdated, narrativeIntent, { isOpening: isOpeningScene, sceneKind: finalSceneKind });
1027:       const finalRhythmContext = narrativeRhythmPrompt(turnRhythm);
1028:       const validatedPublicContext = `${publicContext}${sensoryContext}${finalRhythmContext}\n\n--- RESULTADO MECÂNICO VALIDADO LOCALMENTE ---\nModo: ${mechanicallyUpdated.masterState?.activeMode || 'adventure'}\nLocal canônico: ${mechanicallyUpdated.masterState?.position.locationName || mechanicallyUpdated.currentLocation}\nCena formal: ${JSON.stringify(mechanicallyUpdated.masterState?.scene || null)}\nPróximo herói sugerido para protagonismo: ${mechanicallyUpdated.masterState?.sessionDirector.nextSuggestedHeroId || 'nenhum'}\nComandos aplicados: ${JSON.stringify(mechanical.audits.filter(item => item.status === 'applied').map(item => item.command))}\nCalendário canônico: ano ${finalClock.year}; dia ${finalClock.day}; estação ${finalClock.season}; horário ${String(finalClock.hour).padStart(2,'0')}:${String(finalClock.minute).padStart(2,'0')}. REGRA DURA: nunca mencione outra estação, outro dia ou outro período do dia.\nComandos rejeitados: ${JSON.stringify(mechanical.rejected)}\nNPCs presentes validados: ${JSON.stringify(mechanicallyUpdatedNpcs.filter(npc => mechanicallyUpdated.presentNpcIds.includes(npc.id)).map(npc => ({ id: npc.id, name: npc.name, role: npc.role, appearance: npc.appearance, attitude: npc.attitude, emotionalState: npc.emotionalState, speechStyle: npc.speechStyle, tokenAssigned: Boolean(npc.tokenId) })))}\nRolagem disponível: ${rollAlreadyAvailable ? JSON.stringify(base.pendingDiceRoll) : 'não'}\nNarre apenas o estado validado acima.`;
1029:       let response: GeminiStoryResponse;
1030:       if (localFallbackActive) {
1031:         response = createLocalNarration(mechanicallyUpdated, userAction, sceneEnvironment, selectedManuals as any, mechanicallyUpdatedNpcs);
1032:       } else {
1033:         response = await generateNarrationFromPlan(validatedPublicContext, userAction, plan);
1034:         if (narrationNeedsContinuityRepair(response, continuityAnchor)) {
1035:           response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
1036: [REPARO DE CONTINUIDADE — NÃO MUDE A CENA]`, {
1037:             ...plan,
1038:             narrationBrief: `${plan.narrationBrief || ''}
1039: A resposta anterior perdeu a continuidade. Comece pela reação imediata dos NPCs já presentes à ação do jogador, preserve o assunto e só depois avance a situação.`,
1040:           });
1041:         }
1042:         if (isOpeningScene && openingNarrationNeedsRepair(response, adventureProfile)) {
1043:           const repairedPlan = {
1044:             ...plan,
1045:             requiredTest: null,
1046:             narrationBrief: `${plan.narrationBrief || ''}
1047: REESCREVA A ABERTURA: ela veio curta demais. Entregue uma abertura substancial em batidas reais: imagem espacial clara, mundo já em movimento, um detalhe memorável, situação concreta e uma decisão possível. Não solicite teste e não termine obrigatoriamente com uma pergunta.`,
1048:           };
1049:           response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
1050: [REPARO DE ABERTURA CURTA — NÃO AVANCE O ESTADO]`, repairedPlan);
1051:         }
1052:         let quality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);
1053:         for (let qualityAttempt = 0; quality.repair && qualityAttempt < 2; qualityAttempt += 1) {
1054:           response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
1055: [REPARO DE QUALIDADE — PRESERVE O ESTADO — RESPONDA DIRETAMENTE À AÇÃO ATUAL]`, {
1056:             ...plan,
1057:             narrationBrief: `${plan.narrationBrief || ''}
1058: ${professionalRepairBrief(quality.flags, narrativeIntent)}`,
1059:           });
1060:           quality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);
1061:         }
1062:         let rhythm = assessNarrativeRhythm(response, turnRhythm);
1063:         for (let rhythmAttempt = 0; rhythm.repair && rhythmAttempt < 2; rhythmAttempt += 1) {
1064:           response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}
1065: [REPARO DE CADÊNCIA — NÃO AVANCE ALÉM DO JÁ VALIDADO]`, {
1066:             ...plan,
1067:             narrationBrief: `${plan.narrationBrief || ''}
1068: ${rhythmRepairBrief(turnRhythm, rhythm.flags)}`,
1069:           });
1070:           rhythm = assessNarrativeRhythm(response, turnRhythm);
1071:         }
1072:       }
1073:       if (narrativeIntent.advancesFiction) {
1074:         let barbaraNarrativeValidation = await validateBarbaraNarration({
1075:           response: response as unknown as Record<string, unknown>, action: cleanUserAction, inputChannel, occasion: barbaraOccasion, importance: isOpeningScene ? 'meaningful' : 'normal',
1076:         });
1077:         if (barbaraNarrativeValidation.valid === false && !localFallbackActive) {
1078:           response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}\n[REPARO BARBARA — PRESERVE EXATAMENTE O ESTADO JÁ VALIDADO]`, {
1079:             ...plan,
1080:             narrationBrief: `${plan.narrationBrief || ''}\nA proposta anterior violou agência/cobertura/obrigação narrativa. Reescreva sem decidir ações pelo jogador, cubra integralmente a ação declarada e preserve posição, tempo, NPCs, fatos e consequências já validados.`,
1081:           });
1082:           barbaraNarrativeValidation = await validateBarbaraNarration({
1083:             response: response as unknown as Record<string, unknown>, action: cleanUserAction, inputChannel, occasion: barbaraOccasion, importance: isOpeningScene ? 'meaningful' : 'normal',
1084:           });
1085:         }
1086:         if (barbaraNarrativeValidation.valid === false) {
1087:           throw new Error(`Barbara rejeitou a proposta narrativa sem alterar a campanha: ${barbaraNarrativeValidation.code || barbaraNarrativeValidation.reason || 'violação de agência/cobertura'}`);
1088:         }
1089:         const supervisorFinal = await finalizeBarbaraNarrative({
1090:           hostState: barbaraProjectedState, text: cleanUserAction, requestId: activeTurnId || `oua-${Date.now()}`,
1091:           narration: String((response as any).atmospheric_narration || (response as any).action_result || ''),
1092:           importance: isOpeningScene ? 'meaningful' : 'normal', inputChannel, presentation: (response as any).presentation || null,
1093:         });
1094:         if (supervisorFinal.valid === false) {
1095:           throw new Error(`Motor Barbara 1.1.3 Supervisor rejeitou a narrativa: ${supervisorFinal.reason || 'falha estrutural do supervisor'}`);
1096:         }
1097:         if (supervisorFinal.needs_repair && !localFallbackActive) {
1098:           const supervisorReason = String(supervisorFinal.narrative_fallback_reason || 'qualidade/continuidade');
1099:           response = await generateNarrationFromPlan(validatedPublicContext, `${userAction}\n[REPARO SUPERVISOR BARBARA 1.1.3 — ${supervisorReason} — NÃO MUDE O ESTADO VALIDADO]`, {
1100:             ...plan,
1101:             narrationBrief: `${plan.narrationBrief || ''}\nAtenda a obrigação de apresentação/continuidade indicada pelo Supervisor 1.1.3 sem inventar lore, sem alterar regras e sem decidir pelo jogador.`,
1102:           });
1103:           const repairedSupervisor = await finalizeBarbaraNarrative({
1104:             hostState: barbaraProjectedState, text: cleanUserAction, requestId: `${activeTurnId || `oua-${Date.now()}`}-repair`,
1105:             narration: String((response as any).atmospheric_narration || (response as any).action_result || ''),
1106:             importance: isOpeningScene ? 'meaningful' : 'normal', inputChannel, presentation: (response as any).presentation || null,
1107:           });
1108:           if (repairedSupervisor.valid === false) {
1109:             throw new Error(`Motor Barbara 1.1.3 Supervisor falhou após reparo: ${repairedSupervisor.reason || 'falha estrutural'}`);
1110:           }
1111:           if (repairedSupervisor.needs_repair) console.warn('[BARBARA 1.1.3] reparo ainda recomendado:', repairedSupervisor.narrative_fallback_reason);
1112:         }
1113:       }
1114:       if (!narrativeIntent.advancesFiction) {
1115:         response = {
1116:           ...response,
1117:           atmospheric_narration: '',
\`\`\`

### components/immersive/ImmersiveJourneyView.tsx — /Jornada · rota/ — linha 248

\`\`\`text
220:         followParty={false}
221:         partyCharacters={props.characters}
222:       />
223:       <div style={{ pointerEvents: 'none', position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(6,5,4,.38) 0%, rgba(8,6,4,.02) 25%, rgba(8,6,4,.02) 72%, rgba(8,6,4,.32) 100%)' }} />
224:     </div>
225: 
226:     <header style={{ position: 'absolute', left: 10, right: 10, top: safeTop, zIndex: 80, minHeight: 58, display: 'flex', alignItems: 'stretch', gap: 8, pointerEvents: 'none' }}>
227:       <div style={{ ...glassPanel, flex: 1, minWidth: 0, borderRadius: 12, padding: '7px 10px' }}>
228:         <div style={{ fontSize: 11, opacity: .72, letterSpacing: '.08em', textTransform: 'uppercase' }}>Local atual</div>
229:         <div style={{ fontSize: 17, fontWeight: 700, lineHeight: 1.15, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{locationName}</div>
230:         <div style={{ display: 'flex', gap: 10, marginTop: 4, fontSize: 11, opacity: .82, whiteSpace: 'nowrap', overflow: 'hidden' }}>
231:           <Hud icon={resolveImmersiveAsset(assets, 'hud.weather.' + weatherKey(weather))} text={weather} />
232:           <Hud icon={resolveImmersiveAsset(assets, 'hud.time.' + clock.key)} text={`${clock.label} · Dia ${props.masterState.worldClock.day} · ${String(props.masterState.worldClock.hour).padStart(2,'0')}:${String(props.masterState.worldClock.minute).padStart(2,'0')}`} />
233:           <span>{seasonLabel(props.masterState.worldClock.season)}</span>
234:         </div>
235:       </div>
236:       <div style={{ ...glassPanel, width: 100, borderRadius: 12, padding: '7px 8px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
237:         <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 10, opacity: .78 }}>
238:           {sauronEye && <img src={sauronEye} alt="" style={{ width: 18, height: 18, objectFit: 'contain' }} />}
239:           <span>Olho</span><strong style={{ marginLeft: 'auto', fontSize: 11 }}>{eye.current}/{eye.max}</strong>
240:         </div>
241:         <div style={{ height: 5, borderRadius: 99, overflow: 'hidden', marginTop: 5, background: 'rgba(255,255,255,.12)' }}>
242:           <div style={{ height: '100%', width: Math.max(0, Math.min(100, eye.current / eye.max * 100)) + '%', background: 'linear-gradient(90deg,#8f6e45,#b8793d,#b84026)', borderRadius: 99 }} />
243:         </div>
244:       </div>
245:     </header>
246:     {props.masterState.activeMode === 'journey' && (
247:       <div style={{ position: 'absolute', top: 'calc(' + safeTop + ' + 66px)', left: 12, right: 12, zIndex: 79, pointerEvents: 'none', textAlign: 'center', fontSize: 10, color: '#e6d5b1', opacity: .82 }}>
248:         Jornada · rota {props.masterState.journey.plannedRouteHexIds?.length || 0} hex(es) · marcha {props.masterState.journey.marchHexesRemaining || 0} · fadiga pendente {Object.values(props.masterState.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + Math.max(0, Number(value) || 0), 0)} · {props.masterState.journey.pendingEvent || props.masterState.journey.awaitingEventGeneration ? 'Evento' : props.masterState.journey.awaitingEndRoll ? 'Chegada' : 'Em marcha'}
249:       </div>
250:     )}
251: 
252:     {((startRequested || props.isStoryLoading) && !node) && <div aria-live="polite" style={{
253:       position: 'absolute', left: '50%', bottom: actionPanelBottom, zIndex: 74, transform: 'translateX(-50%)',
254:       width: 'min(86vw, 350px)', borderRadius: 10, border: '1px solid rgba(183,145,91,.62)',
255:       background: 'rgba(49,31,15,.96)', color: '#fff0cf', padding: '11px 14px', textAlign: 'center',
256:       pointerEvents: 'none', boxShadow: '0 8px 20px rgba(0,0,0,.42)', fontSize: 12, fontWeight: 900,
257:       letterSpacing: '.04em'
258:     }}>Gemini criando a cena inicial…</div>}
259: 
260:     {props.storyError && <div role="alert" style={{
261:       position: 'absolute', left: 12, right: 12, bottom: actionPanelBottom, zIndex: 96,
262:       borderRadius: 10, border: '1px solid rgba(185,72,54,.75)', background: 'rgba(52,13,10,.96)',
263:       color: '#ffe4d6', padding: '10px 12px', pointerEvents: 'auto', boxShadow: '0 8px 24px rgba(0,0,0,.42)'
264:     }}>
265:       <div style={{ fontSize: 11, fontWeight: 900, textTransform: 'uppercase', letterSpacing: '.06em' }}>Falha ao iniciar/continuar</div>
266:       <div style={{ marginTop: 4, fontSize: 12, lineHeight: 1.35 }}>{props.storyError}</div>
267:       {props.onDismissStoryError && <button type="button" onClick={props.onDismissStoryError} style={{
268:         ...touchButton, marginTop: 7, minHeight: 34, padding: '5px 10px', borderRadius: 7,
269:         border: '1px solid rgba(255,225,205,.34)', background: 'rgba(255,255,255,.07)', color: '#ffe4d6',
270:         fontFamily: 'inherit', fontSize: 11, fontWeight: 800
271:       }}>Fechar diagnóstico</button>}
272:     </div>}
273: 
274:     {chronicleOpen && node && <aside data-braseiro-chronicle-sheet="true" style={{
275:       position: 'absolute', left: 12, right: 12, bottom: actionPanelBottom, zIndex: 60, maxHeight: '58dvh',
276:       borderRadius: 14, padding: '12px 12px 10px', color: '#2a2116',
277:       background: 'linear-gradient(180deg, rgba(246,225,184,.99), rgba(229,198,143,.99))',
278:       border: '1px solid rgba(93,63,31,.55)', boxShadow: '0 14px 34px rgba(0,0,0,.42)',
279:       overflowY: 'auto', overflowX: 'hidden', pointerEvents: 'auto', touchAction: 'pan-y',
280:       WebkitOverflowScrolling: 'touch',
281:     }}>
282:       <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 5 }}>
283:         <button type="button" onClick={() => setChronicleOpen(false)} aria-label="Fechar crônica" style={{ ...touchButton, width: 38, height: 38, borderRadius: 999, border: '1px solid #6b4a28', background: '#5b3d20', color: '#fff0cf', fontSize: 24, fontWeight: 900, lineHeight: 1 }}>×</button>
284:       </div>
285: 
286:       <div style={{ width: 'min(78vw, 300px)', margin: '0 auto 8px', aspectRatio: '16 / 9', overflow: 'hidden', borderRadius: 10, border: '1px solid rgba(93,63,31,.48)', background: 'rgba(31,22,13,.22)' }}>
287:         {props.currentImage ? <img src={props.currentImage} alt="Ilustração da cena" draggable={false} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#795d38', fontSize: 12, fontWeight: 700 }}>Cena sem ilustração</div>}
288:       </div>
289: 
290:       <button type="button" onClick={() => void props.onShareSceneImagePrompt()} style={{ ...touchButton, width: '100%', minHeight: 42, borderRadius: 8, border: '1px solid #7b562d', background: 'rgba(91,61,32,.13)', color: '#4b321b', fontFamily: 'inherit', fontSize: 12, fontWeight: 800 }}>
291:         {props.currentImage ? 'Trocar ilustração no Gemini' : 'Gerar ilustração 16:9 no Gemini'}
292:       </button>
293: 
294:       {props.presentNpcs.length > 0 && <div style={{ marginTop: 9 }}>
295:         <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#735633' }}>NPCs presentes</div>
296:         <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 3 }}>
297:           {props.presentNpcs.map(npc => {
298:             const effectiveTokenId = npcTokenOverrides[npc.id] || npc.tokenId || '';
299:             const portrait = effectiveTokenId ? npcTokenUrls[effectiveTokenId] : null;
300:             return <button key={npc.id} type="button" onClick={() => portrait ? setSelectedNpc(npc) : void props.onShareNpcPortraitPrompt(npc)} style={{ ...touchButton, width: 68, flex: '0 0 68px', border: 0, padding: 0, background: 'transparent', color: '#3d2a18', fontFamily: 'inherit' }}>
301:               <span style={{ width: 50, height: 50, margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', borderRadius: 999, border: '2px solid #9a713e', background: '#21170e' }}>
302:                 {portrait ? <img src={portrait} alt={npc.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <span style={{ color: '#ddb879', fontSize: 22 }}>+</span>}
303:               </span>
304:               <span style={{ display: 'block', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 10, fontWeight: 800 }}>{npc.name}</span>
305:             </button>;
306:           })}
307:         </div>
308:       </div>}
309: 
310:       <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap', marginTop: 10, marginBottom: 5 }}>
311:         <div style={{ fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Narração do Mestre</div>
312:         {(generationSource || generationModel) && <span style={{ marginLeft: 'auto', fontSize: 9, fontWeight: 700, color: generationSource === 'gemini' ? '#365f45' : '#8a3b2f' }}>
313:           {generationSource === 'gemini' ? 'Gemini' : generationSource === 'local-fallback' ? 'Fallback local' : generationSource}{generationModel ? ' · ' + generationModel : ''}
314:         </span>}
315:         <button type="button" onClick={() => props.isNarrationLoading ? props.onStopNarration() : void props.onPlayNarration()} style={{ ...touchButton, minHeight: 36, padding: '6px 11px', borderRadius: 999, border: '1px solid #7b562d', background: props.isNarrationLoading ? '#5e2b22' : '#5b3d20', color: '#fff0cf', fontFamily: 'inherit', fontSize: 11, fontWeight: 900, whiteSpace: 'nowrap' }}>
316:           {props.isNarrationLoading ? '■ Parar narração' : '🔊 Ouvir narração'}
317:         </button>
318:       </div>
319: 
320:       {props.narrationError && <div role="alert" style={{
321:         marginBottom: 8, borderRadius: 7, border: '1px solid rgba(158,55,42,.5)',
322:         background: 'rgba(94,28,20,.12)', color: '#7a2d22', padding: '7px 9px',
323:         fontSize: 11, lineHeight: 1.35, fontWeight: 700
324:       }}>TTS: {props.narrationError}</div>}
325: 
326:       <div style={{ minHeight: 92, maxHeight: 160, overflowY: 'auto', paddingRight: 4, fontSize: 15, lineHeight: 1.48 }}>
327:         {narrative ? narrative.split(/\n\s*\n/).map((paragraph, index) => <p key={index} style={{ margin: index ? '10px 0 0' : 0 }}>{paragraph}</p>) : <p style={{ margin: 0, fontStyle: 'italic', opacity: .72 }}>A crônica aguarda o próximo passo da Companhia.</p>}
328:       </div>
329: 
330:       {props.pendingTest && <button type="button" onClick={() => { setChronicleOpen(false); props.onOpenDice(); }} style={{ ...touchButton, width: '100%', minHeight: 44, marginTop: 9, borderRadius: 8, border: '1px solid #7b562d', background: '#6a431f', color: '#fff0cf', fontFamily: 'inherit', fontSize: 12, fontWeight: 900 }}>Teste pendente — abrir Dados</button>}
331: 
332:       <div style={{ marginTop: 9 }}>
333:         <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Ações rápidas</div>
334:         <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 6 }}>
335:           {presetActions.map(action => <button key={action.id} type="button" disabled={Boolean(props.isStoryLoading)} onClick={() => dispatchChoice({ id: 'preset-' + action.id, text: action.text })} style={{ ...touchButton, minHeight: 44, borderRadius: 8, border: '1px solid rgba(83,53,25,.42)', background: 'rgba(255,248,231,.34)', color: '#2d2116', fontFamily: 'inherit', fontSize: 12, fontWeight: 800, opacity: props.isStoryLoading ? .5 : 1 }}>{action.label}</button>)}
336:         </div>
337:       </div>
\`\`\`

### components/immersive/ImmersiveJourneyView.tsx — /hud/ — linha 231

\`\`\`text
203:   };
204: 
205:   return <section data-braseiro-immersive-root="true" style={{
206:     position: 'fixed', inset: 0, width: '100%', height: '100dvh', minHeight: '100dvh', overflow: 'hidden',
207:     background: '#0d0a07', color: '#ead9b7', fontFamily: 'Georgia, Cambria, serif', pointerEvents: 'auto',
208:   }}>
209:     <div style={{ position: 'absolute', inset: 0, zIndex: 0, background: '#100d09', pointerEvents: 'auto' }}>
210:       <EriadorMap
211:         currentLocation={locationName}
212:         campaignId={props.campaignId}
213:         currentHexId={props.masterState.position.currentHexId}
214:         onPartyMoved={props.onPartyMoved}
215:         onDiscoveryChanged={props.onMapDiscoveryChanged}
216:         onPartyRested={props.onPartyRested}
217:         strongholds={props.masterState.livingWorld.strongholds || []}
218:         immersive
219:         partyMarkerMode="none"
220:         followParty={false}
221:         partyCharacters={props.characters}
222:       />
223:       <div style={{ pointerEvents: 'none', position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(6,5,4,.38) 0%, rgba(8,6,4,.02) 25%, rgba(8,6,4,.02) 72%, rgba(8,6,4,.32) 100%)' }} />
224:     </div>
225: 
226:     <header style={{ position: 'absolute', left: 10, right: 10, top: safeTop, zIndex: 80, minHeight: 58, display: 'flex', alignItems: 'stretch', gap: 8, pointerEvents: 'none' }}>
227:       <div style={{ ...glassPanel, flex: 1, minWidth: 0, borderRadius: 12, padding: '7px 10px' }}>
228:         <div style={{ fontSize: 11, opacity: .72, letterSpacing: '.08em', textTransform: 'uppercase' }}>Local atual</div>
229:         <div style={{ fontSize: 17, fontWeight: 700, lineHeight: 1.15, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{locationName}</div>
230:         <div style={{ display: 'flex', gap: 10, marginTop: 4, fontSize: 11, opacity: .82, whiteSpace: 'nowrap', overflow: 'hidden' }}>
231:           <Hud icon={resolveImmersiveAsset(assets, 'hud.weather.' + weatherKey(weather))} text={weather} />
232:           <Hud icon={resolveImmersiveAsset(assets, 'hud.time.' + clock.key)} text={`${clock.label} · Dia ${props.masterState.worldClock.day} · ${String(props.masterState.worldClock.hour).padStart(2,'0')}:${String(props.masterState.worldClock.minute).padStart(2,'0')}`} />
233:           <span>{seasonLabel(props.masterState.worldClock.season)}</span>
234:         </div>
235:       </div>
236:       <div style={{ ...glassPanel, width: 100, borderRadius: 12, padding: '7px 8px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
237:         <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 10, opacity: .78 }}>
238:           {sauronEye && <img src={sauronEye} alt="" style={{ width: 18, height: 18, objectFit: 'contain' }} />}
239:           <span>Olho</span><strong style={{ marginLeft: 'auto', fontSize: 11 }}>{eye.current}/{eye.max}</strong>
240:         </div>
241:         <div style={{ height: 5, borderRadius: 99, overflow: 'hidden', marginTop: 5, background: 'rgba(255,255,255,.12)' }}>
242:           <div style={{ height: '100%', width: Math.max(0, Math.min(100, eye.current / eye.max * 100)) + '%', background: 'linear-gradient(90deg,#8f6e45,#b8793d,#b84026)', borderRadius: 99 }} />
243:         </div>
244:       </div>
245:     </header>
246:     {props.masterState.activeMode === 'journey' && (
247:       <div style={{ position: 'absolute', top: 'calc(' + safeTop + ' + 66px)', left: 12, right: 12, zIndex: 79, pointerEvents: 'none', textAlign: 'center', fontSize: 10, color: '#e6d5b1', opacity: .82 }}>
248:         Jornada · rota {props.masterState.journey.plannedRouteHexIds?.length || 0} hex(es) · marcha {props.masterState.journey.marchHexesRemaining || 0} · fadiga pendente {Object.values(props.masterState.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + Math.max(0, Number(value) || 0), 0)} · {props.masterState.journey.pendingEvent || props.masterState.journey.awaitingEventGeneration ? 'Evento' : props.masterState.journey.awaitingEndRoll ? 'Chegada' : 'Em marcha'}
249:       </div>
250:     )}
251: 
252:     {((startRequested || props.isStoryLoading) && !node) && <div aria-live="polite" style={{
253:       position: 'absolute', left: '50%', bottom: actionPanelBottom, zIndex: 74, transform: 'translateX(-50%)',
254:       width: 'min(86vw, 350px)', borderRadius: 10, border: '1px solid rgba(183,145,91,.62)',
255:       background: 'rgba(49,31,15,.96)', color: '#fff0cf', padding: '11px 14px', textAlign: 'center',
256:       pointerEvents: 'none', boxShadow: '0 8px 20px rgba(0,0,0,.42)', fontSize: 12, fontWeight: 900,
257:       letterSpacing: '.04em'
258:     }}>Gemini criando a cena inicial…</div>}
259: 
260:     {props.storyError && <div role="alert" style={{
261:       position: 'absolute', left: 12, right: 12, bottom: actionPanelBottom, zIndex: 96,
262:       borderRadius: 10, border: '1px solid rgba(185,72,54,.75)', background: 'rgba(52,13,10,.96)',
263:       color: '#ffe4d6', padding: '10px 12px', pointerEvents: 'auto', boxShadow: '0 8px 24px rgba(0,0,0,.42)'
264:     }}>
265:       <div style={{ fontSize: 11, fontWeight: 900, textTransform: 'uppercase', letterSpacing: '.06em' }}>Falha ao iniciar/continuar</div>
266:       <div style={{ marginTop: 4, fontSize: 12, lineHeight: 1.35 }}>{props.storyError}</div>
267:       {props.onDismissStoryError && <button type="button" onClick={props.onDismissStoryError} style={{
268:         ...touchButton, marginTop: 7, minHeight: 34, padding: '5px 10px', borderRadius: 7,
269:         border: '1px solid rgba(255,225,205,.34)', background: 'rgba(255,255,255,.07)', color: '#ffe4d6',
270:         fontFamily: 'inherit', fontSize: 11, fontWeight: 800
271:       }}>Fechar diagnóstico</button>}
272:     </div>}
273: 
274:     {chronicleOpen && node && <aside data-braseiro-chronicle-sheet="true" style={{
275:       position: 'absolute', left: 12, right: 12, bottom: actionPanelBottom, zIndex: 60, maxHeight: '58dvh',
276:       borderRadius: 14, padding: '12px 12px 10px', color: '#2a2116',
277:       background: 'linear-gradient(180deg, rgba(246,225,184,.99), rgba(229,198,143,.99))',
278:       border: '1px solid rgba(93,63,31,.55)', boxShadow: '0 14px 34px rgba(0,0,0,.42)',
279:       overflowY: 'auto', overflowX: 'hidden', pointerEvents: 'auto', touchAction: 'pan-y',
280:       WebkitOverflowScrolling: 'touch',
281:     }}>
282:       <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 5 }}>
283:         <button type="button" onClick={() => setChronicleOpen(false)} aria-label="Fechar crônica" style={{ ...touchButton, width: 38, height: 38, borderRadius: 999, border: '1px solid #6b4a28', background: '#5b3d20', color: '#fff0cf', fontSize: 24, fontWeight: 900, lineHeight: 1 }}>×</button>
284:       </div>
285: 
286:       <div style={{ width: 'min(78vw, 300px)', margin: '0 auto 8px', aspectRatio: '16 / 9', overflow: 'hidden', borderRadius: 10, border: '1px solid rgba(93,63,31,.48)', background: 'rgba(31,22,13,.22)' }}>
287:         {props.currentImage ? <img src={props.currentImage} alt="Ilustração da cena" draggable={false} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#795d38', fontSize: 12, fontWeight: 700 }}>Cena sem ilustração</div>}
288:       </div>
289: 
290:       <button type="button" onClick={() => void props.onShareSceneImagePrompt()} style={{ ...touchButton, width: '100%', minHeight: 42, borderRadius: 8, border: '1px solid #7b562d', background: 'rgba(91,61,32,.13)', color: '#4b321b', fontFamily: 'inherit', fontSize: 12, fontWeight: 800 }}>
291:         {props.currentImage ? 'Trocar ilustração no Gemini' : 'Gerar ilustração 16:9 no Gemini'}
292:       </button>
293: 
294:       {props.presentNpcs.length > 0 && <div style={{ marginTop: 9 }}>
295:         <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#735633' }}>NPCs presentes</div>
296:         <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 3 }}>
297:           {props.presentNpcs.map(npc => {
298:             const effectiveTokenId = npcTokenOverrides[npc.id] || npc.tokenId || '';
299:             const portrait = effectiveTokenId ? npcTokenUrls[effectiveTokenId] : null;
300:             return <button key={npc.id} type="button" onClick={() => portrait ? setSelectedNpc(npc) : void props.onShareNpcPortraitPrompt(npc)} style={{ ...touchButton, width: 68, flex: '0 0 68px', border: 0, padding: 0, background: 'transparent', color: '#3d2a18', fontFamily: 'inherit' }}>
301:               <span style={{ width: 50, height: 50, margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', borderRadius: 999, border: '2px solid #9a713e', background: '#21170e' }}>
302:                 {portrait ? <img src={portrait} alt={npc.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <span style={{ color: '#ddb879', fontSize: 22 }}>+</span>}
303:               </span>
304:               <span style={{ display: 'block', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 10, fontWeight: 800 }}>{npc.name}</span>
305:             </button>;
306:           })}
307:         </div>
308:       </div>}
309: 
310:       <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap', marginTop: 10, marginBottom: 5 }}>
311:         <div style={{ fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Narração do Mestre</div>
312:         {(generationSource || generationModel) && <span style={{ marginLeft: 'auto', fontSize: 9, fontWeight: 700, color: generationSource === 'gemini' ? '#365f45' : '#8a3b2f' }}>
313:           {generationSource === 'gemini' ? 'Gemini' : generationSource === 'local-fallback' ? 'Fallback local' : generationSource}{generationModel ? ' · ' + generationModel : ''}
314:         </span>}
315:         <button type="button" onClick={() => props.isNarrationLoading ? props.onStopNarration() : void props.onPlayNarration()} style={{ ...touchButton, minHeight: 36, padding: '6px 11px', borderRadius: 999, border: '1px solid #7b562d', background: props.isNarrationLoading ? '#5e2b22' : '#5b3d20', color: '#fff0cf', fontFamily: 'inherit', fontSize: 11, fontWeight: 900, whiteSpace: 'nowrap' }}>
316:           {props.isNarrationLoading ? '■ Parar narração' : '🔊 Ouvir narração'}
317:         </button>
318:       </div>
319: 
320:       {props.narrationError && <div role="alert" style={{
\`\`\`

### components/EriadorMap.tsx — /planRouteToSelected/ — linha 920

\`\`\`text
892:           const previous = cameFrom.get(cursor);
893:           if (!previous) return [];
894:           cursor = previous;
895:         }
896:         return route.reverse();
897:       }
898:       if (closed.has(currentId)) continue;
899:       closed.add(currentId);
900:       const current = byId.get(currentId);
901:       if (!current) continue;
902:       for (const neighborId of current.neighbors || []) {
903:         const neighbor = byId.get(neighborId);
904:         if (!neighbor || !neighbor.playable || closed.has(neighborId)) continue;
905:         const terrainCost = TERRAIN_COSTS[neighbor.terrain] ?? 2;
906:         if (!Number.isFinite(terrainCost)) continue;
907:         // Estradas e caminhos são favorecidos no planejamento, mas nenhuma
908:         // consequência mecânica é aplicada aqui; a Jornada formal resolve isso.
909:         const roadFactor = neighbor.terrain === 'road' || neighbor.tags?.some(tag => /road|estrada|caminho/i.test(tag)) ? 0.7 : 1;
910:         const nextCost = (cost.get(currentId) || 0) + Math.max(0.65, terrainCost * roadFactor);
911:         if (nextCost >= (cost.get(neighborId) ?? Number.POSITIVE_INFINITY)) continue;
912:         cameFrom.set(neighborId, currentId);
913:         cost.set(neighborId, nextCost);
914:         open.push({ id: neighborId, score: nextCost + heuristic(neighbor) });
915:       }
916:     }
917:     return [];
918:   };
919: 
920:   const planRouteToSelected = async () => {
921:     if (!selected || !discovery?.currentHexId) {
922:       setError('Posicione primeiro a Companhia em um hex para planejar a rota.');
923:       return;
924:     }
925:     const route = findPlannedRoute(discovery.currentHexId, selected.id);
926:     if (!route.length) {
927:       setError(selected.id === discovery.currentHexId ? 'A Companhia já está neste hex.' : 'Não foi encontrada uma rota jogável até o hex selecionado.');
928:       return;
929:     }
930:     const updated: MapDiscovery = { ...discovery, plannedRouteHexIds: route, updatedAt: Date.now() };
931:     await onDiscoveryChanged(updated, 'planeamento da rota de viagem');
932:     setDiscovery(updated);
933:     setError(null);
934:     const estimatedDays = route.reduce((sum, hexId) => sum + journeyTravelDaysForTerrain(byId.get(hexId)?.terrain), 0);
935:     setMessage(`Rota planejada com ${route.length} etapa(s), estimativa de ${estimatedDays} dia(s) pelas regras de terreno. Use “Avançar 1 hex pela rota” para viajar etapa por etapa.`);
936:   };
937: 
938:   const clearPlannedRoute = async () => {
939:     if (!discovery) return;
940:     const updated: MapDiscovery = { ...discovery, plannedRouteHexIds: [], updatedAt: Date.now() };
941:     await onDiscoveryChanged(updated, 'limpeza da rota planeada');
942:     setDiscovery(updated);
943:     setMessage('Rota futura limpa. O histórico já percorrido foi preservado.');
944:   };
945: 
946:   const restGroup = async (kind: 'short' | 'long') => {
947:     if (!onPartyRested) {
948:       setError('O descanso deve ser resolvido pelo motor de regras.');
949:       return;
950:     }
951:     try {
952:       await onPartyRested(kind);
953:       setError(null);
954:       setMessage(kind === 'short'
955:         ? 'Descanso Curto resolvido: pelo menos 1 hora; Feridos não recuperam Resistência.'
956:         : 'Descanso Prolongado resolvido: Resistência recuperada pelas regras; Esperança 0 recupera 1 ponto.');
957:     } catch (error) {
958:       setError(error instanceof Error ? error.message : 'O descanso não pôde ser resolvido.');
959:     }
960:   };
961: 
962:   const moveToHex = async (target: EriadorHex) => {
963:     if (!target || !target.playable || !discovery) return;
964:     if (isMovePending) {
965:       setError('A Jornada ainda está resolvendo o deslocamento anterior.');
966:       return;
967:     }
968:     const current = discovery.currentHexId ? byId.get(discovery.currentHexId) : null;
969:     if (current) {
970:       const expectedNext = discovery.plannedRouteHexIds?.[0] || null;
971:       if (!expectedNext) {
972:         setError('Movimento livre bloqueado: selecione o destino e toque em “Planejar rota”.');
973:         return;
974:       }
975:       if (target.id !== expectedNext) {
976:         setError('Este não é o próximo hex da rota. A Companhia só avança pela Jornada planejada.');
977:         return;
978:       }
979:     }
980:     
981:     if (current) {
982:       console.log(`Attempting move from ${shortHexLabel(current)} (ID: ${current.id}) to ${shortHexLabel(target)} (ID: ${target.id})`);
983:       console.log(`Current neighbors: ${JSON.stringify(current.neighbors)}`);
984:       
985:       if (current.id === target.id) {
986:         setError("A comitiva já está neste local.");
987:         return;
988:       }
989:       
990:       const isNeighbor = current.neighbors?.includes(target.id) || areManifestNeighbors(manifest, current, target);
991:       if (!isNeighbor) {
992:         setError(`${target.name || shortHexLabel(target)} não é adjacente. A viagem ocorre 1 hex por vez: planeje a rota e use “Avançar 1 hex pela rota”.`);
993:         console.warn(`Neighbor check failed. Selected ${target.id} is not a direct neighbor.`);
994:         return;
995:       }
996:     }
997: 
998:     const cost = TERRAIN_COSTS[target.terrain] ?? 2;
999:     if (!Number.isFinite(cost)) {
1000:       setError('Este hex é marítimo e não pode ser atravessado sem um meio apropriado.');
1001:       return;
1002:     }
1003:     const discovered = new Set(discovery.discoveredHexIds);
1004:     const visited = new Set(discovery.visitedHexIds);
1005:     
1006:     // Check if this is a new discovery
1007:     const isNewDiscovery = !discovered.has(target.id);
1008:     
1009:     discovered.add(target.id);
\`\`\`

### components/EriadorMap.tsx — /advancePlannedRoute/ — linha 1067

\`\`\`text
1039:       await onPartyMoved({ toHexId: target.id, locationName, terrain: target.terrain, discovery: updated });
1040:       setDiscovery(updated);
1041:     } catch (error) {
1042:       setError(error instanceof Error ? error.message : 'O movimento não pôde ser gravado de forma atômica.');
1043:       return;
1044:     } finally {
1045:       setIsMovePending(false);
1046:     }
1047:     centerOnHex(target.id);
1048:     if (!current) {
1049:       // Na posição inicial, devolva imediatamente o controle à tela principal
1050:       // para que o botão Começar aventura apareça sem o cartão do hex por cima.
1051:       setSelected(null);
1052:       setSelectedLocation(null);
1053:     }
1054:     setMessage(`Comitiva deslocada para ${locationName}. A distância e a Fadiga serão resolvidas pela Jornada.`);
1055:     setError(null);
1056:   };
1057: 
1058:   const moveGroup = async () => {
1059:     if (!selected) return;
1060:     if (discovery?.currentHexId) {
1061:       setError('Movimento livre bloqueado: escolha o destino, planeje a rota e avance pela Jornada.');
1062:       return;
1063:     }
1064:     await moveToHex(selected);
1065:   };
1066: 
1067:   const advancePlannedRoute = async () => {
1068:     if (!discovery?.plannedRouteHexIds?.length) {
1069:       setError('Não há rota planejada. Selecione um destino e toque em “Planejar rota”.');
1070:       return;
1071:     }
1072:     const nextHex = byId.get(discovery.plannedRouteHexIds[0]);
1073:     if (!nextHex) {
1074:       setError('A próxima etapa da rota não foi encontrada. Planeje a rota novamente.');
1075:       return;
1076:     }
1077:     await moveToHex(nextHex);
1078:   };
1079: 
1080:   const toggleDiscovery = async () => {
1081:     if (!selected || !discovery) return;
1082:     const ids = new Set(discovery.discoveredHexIds);
1083:     const wasDiscovered = ids.has(selected.id);
1084:     if (wasDiscovered) {
1085:       ids.delete(selected.id);
1086:       setNewlyDiscoveredIds(prev => {
1087:         const next = new Set(prev);
1088:         next.delete(selected.id);
1089:         return next;
1090:       });
1091:     } else {
1092:       ids.add(selected.id);
1093:       setNewlyDiscoveredIds(prev => new Set(prev).add(selected.id));
1094:       // A análise por IA é manual para não consumir cota nem disparar chamadas sem pedido.
1095:     }
1096:     const updated = { ...discovery, discoveredHexIds: [...ids] };
1097:     await onDiscoveryChanged(updated, wasDiscovered ? 'ocultação manual de hex' : 'revelação manual de hex');
1098:     setDiscovery(updated);
1099:   };
1100: 
1101:   const updateTerrain = async (terrain: string) => {
1102:     if (!selected || !manifest) return;
1103:     const changed = { ...selected, terrain };
1104:     const next = { ...manifest, hexes: manifest.hexes.map(hex => hex.id === changed.id ? changed : hex), updatedAt: Date.now() };
1105:     setSelected(changed);
1106:     setManifest(next);
1107:     await saveMapManifest(next);
1108:   };
1109: 
1110:   const identifyHexWithAI = async (hexId?: string) => {
1111:     const targetHex = hexId ? byId.get(hexId) : selected;
1112:     if (!targetHex || !imageRef.current || !manifest) return;
1113:     
1114:     try {
1115:       setMessage(`IA analisando ${shortHexLabel(targetHex)}...`);
1116:       const [hexBlob, contextBlob] = await Promise.all([
1117:         createHexCrop(imageRef.current, targetHex),
1118:         createHexAnalysisCrop(imageRef.current, targetHex),
1119:       ]);
1120: 
1121:       const blobToBase64 = (blob: Blob) => new Promise<string>((resolve, reject) => {
1122:         const reader = new FileReader();
1123:         reader.onerror = () => reject(reader.error || new Error('Falha ao ler imagem.'));
1124:         reader.onloadend = () => resolve(String(reader.result).split(',')[1] || '');
1125:         reader.readAsDataURL(blob);
1126:       });
1127:       const [base64, contextBase64] = await Promise.all([
1128:         blobToBase64(hexBlob),
1129:         blobToBase64(contextBlob),
1130:       ]);
1131:       const result = await identifyHex(base64, contextBase64);
1132:       
1133:       if (result.terrain || result.name || result.poi) {
1134:         const nextHex = { ...targetHex };
1135:         if (result.terrain && TERRAIN.some(t => t[0] === result.terrain)) {
1136:           nextHex.terrain = result.terrain as any;
1137:         }
1138:         if (result.name && !targetHex.name) {
1139:           nextHex.name = result.name;
1140:         }
1141:         if (result.poi) nextHex.poi = result.poi;
1142:         if (result.region) nextHex.region = result.region;
1143:         if (result.placeType) nextHex.placeType = result.placeType;
1144:         if (typeof result.confidence === 'number') nextHex.confidence = result.confidence;
1145:         if (result.tags?.length) nextHex.tags = Array.from(new Set([...(nextHex.tags || []), ...result.tags]));
1146:         
1147:         if (selected?.id === targetHex.id) {
1148:           setSelected(nextHex);
1149:         }
1150:         
1151:         const nextManifest = { ...manifest };
1152:         nextManifest.hexes = nextManifest.hexes.map(h => h.id === targetHex.id ? nextHex : h);
1153:         setManifest(nextManifest);
1154:         await saveMapManifest(nextManifest);
1155:         
1156:         let msg = `IA identificou: ${result.terrain}`;
\`\`\`

### components/EriadorMap.tsx — /Movimento livre bloqueado/ — linha 972

\`\`\`text
944:   };
945: 
946:   const restGroup = async (kind: 'short' | 'long') => {
947:     if (!onPartyRested) {
948:       setError('O descanso deve ser resolvido pelo motor de regras.');
949:       return;
950:     }
951:     try {
952:       await onPartyRested(kind);
953:       setError(null);
954:       setMessage(kind === 'short'
955:         ? 'Descanso Curto resolvido: pelo menos 1 hora; Feridos não recuperam Resistência.'
956:         : 'Descanso Prolongado resolvido: Resistência recuperada pelas regras; Esperança 0 recupera 1 ponto.');
957:     } catch (error) {
958:       setError(error instanceof Error ? error.message : 'O descanso não pôde ser resolvido.');
959:     }
960:   };
961: 
962:   const moveToHex = async (target: EriadorHex) => {
963:     if (!target || !target.playable || !discovery) return;
964:     if (isMovePending) {
965:       setError('A Jornada ainda está resolvendo o deslocamento anterior.');
966:       return;
967:     }
968:     const current = discovery.currentHexId ? byId.get(discovery.currentHexId) : null;
969:     if (current) {
970:       const expectedNext = discovery.plannedRouteHexIds?.[0] || null;
971:       if (!expectedNext) {
972:         setError('Movimento livre bloqueado: selecione o destino e toque em “Planejar rota”.');
973:         return;
974:       }
975:       if (target.id !== expectedNext) {
976:         setError('Este não é o próximo hex da rota. A Companhia só avança pela Jornada planejada.');
977:         return;
978:       }
979:     }
980:     
981:     if (current) {
982:       console.log(`Attempting move from ${shortHexLabel(current)} (ID: ${current.id}) to ${shortHexLabel(target)} (ID: ${target.id})`);
983:       console.log(`Current neighbors: ${JSON.stringify(current.neighbors)}`);
984:       
985:       if (current.id === target.id) {
986:         setError("A comitiva já está neste local.");
987:         return;
988:       }
989:       
990:       const isNeighbor = current.neighbors?.includes(target.id) || areManifestNeighbors(manifest, current, target);
991:       if (!isNeighbor) {
992:         setError(`${target.name || shortHexLabel(target)} não é adjacente. A viagem ocorre 1 hex por vez: planeje a rota e use “Avançar 1 hex pela rota”.`);
993:         console.warn(`Neighbor check failed. Selected ${target.id} is not a direct neighbor.`);
994:         return;
995:       }
996:     }
997: 
998:     const cost = TERRAIN_COSTS[target.terrain] ?? 2;
999:     if (!Number.isFinite(cost)) {
1000:       setError('Este hex é marítimo e não pode ser atravessado sem um meio apropriado.');
1001:       return;
1002:     }
1003:     const discovered = new Set(discovery.discoveredHexIds);
1004:     const visited = new Set(discovery.visitedHexIds);
1005:     
1006:     // Check if this is a new discovery
1007:     const isNewDiscovery = !discovered.has(target.id);
1008:     
1009:     discovered.add(target.id);
1010:     visited.add(target.id);
1011:     
1012:     if (isNewDiscovery) {
1013:       console.log(`First time discovering hex: ${target.id}`);
1014:       setNewlyDiscoveredIds(prev => new Set(prev).add(target.id));
1015:       // A análise por IA é manual para não consumir cota nem disparar chamadas sem pedido.
1016:     }
1017:     
1018:     const route = [...(discovery.routeHexIds?.length ? discovery.routeHexIds : discovery.visitedHexIds || [])];
1019:     if (route[route.length - 1] !== target.id) route.push(target.id);
1020:     const updated = {
1021:       ...discovery,
1022:       currentHexId: target.id,
1023:       discoveredHexIds: [...discovered],
1024:       visitedHexIds: [...visited],
1025:       routeHexIds: route,
1026:       plannedRouteHexIds: discovery.plannedRouteHexIds?.[0] === target.id
1027:         ? discovery.plannedRouteHexIds.slice(1)
1028:         : [],
1029:       // Compatibilidade visual apenas. A Fadiga canônica pertence a cada ficha e é
1030:       // alterada somente pelo motor determinístico de Jornadas.
1031:       fatigue: 0,
1032:       updatedAt: Date.now(),
1033:     };
1034: 
1035:     const resolvedLocation = resolveHexNarrativeLocation(manifest, locations, target.id);
1036:     const locationName = resolvedLocation.name;
1037:     setIsMovePending(true);
1038:     try {
1039:       await onPartyMoved({ toHexId: target.id, locationName, terrain: target.terrain, discovery: updated });
1040:       setDiscovery(updated);
1041:     } catch (error) {
1042:       setError(error instanceof Error ? error.message : 'O movimento não pôde ser gravado de forma atômica.');
1043:       return;
1044:     } finally {
1045:       setIsMovePending(false);
1046:     }
1047:     centerOnHex(target.id);
1048:     if (!current) {
1049:       // Na posição inicial, devolva imediatamente o controle à tela principal
1050:       // para que o botão Começar aventura apareça sem o cartão do hex por cima.
1051:       setSelected(null);
1052:       setSelectedLocation(null);
1053:     }
1054:     setMessage(`Comitiva deslocada para ${locationName}. A distância e a Fadiga serão resolvidas pela Jornada.`);
1055:     setError(null);
1056:   };
1057: 
1058:   const moveGroup = async () => {
1059:     if (!selected) return;
1060:     if (discovery?.currentHexId) {
1061:       setError('Movimento livre bloqueado: escolha o destino, planeje a rota e avance pela Jornada.');
\`\`\`

### lib/master/types.ts — /export type JourneyRole/ — linha 6

\`\`\`text
1: import type { TestRequirement } from '../../types';
2: 
3: export type AdventureMode = 'adventure' | 'journey' | 'council' | 'combat' | 'fellowship';
4: export type KnowledgeVisibility = 'master' | 'company' | 'hero';
5: export type KnowledgeKind = 'truth' | 'rumor' | 'lie' | 'clue' | 'interpretation';
6: export type JourneyRole = 'guide' | 'scout' | 'hunter' | 'lookout';
7: export type CombatStance = 'forward' | 'open' | 'defensive' | 'rearward';
8: 
9: export type SceneKind = 'exploration' | 'dialogue' | 'journey' | 'council' | 'combat' | 'rest' | 'fellowship';
10: export type SceneStatus = 'idle' | 'active' | 'resolving' | 'closed';
11: 
12: export interface SceneState {
13:   id: string;
14:   kind: SceneKind;
15:   status: SceneStatus;
16:   title: string;
17:   objective: string;
18:   locationName: string;
19:   participantHeroIds: string[];
20:   participantNpcIds: string[];
21:   spotlightCharacterId: string | null;
22:   spotlightQueue: string[];
23:   pendingActions: Record<string, string>;
24:   tension: number;
25:   endCondition: string;
26:   startedAt: number;
27:   updatedAt: number;
28: }
29: 
30: export interface SessionDirectorState {
31:   sessionObjective: string;
32:   pacing: 'quiet' | 'rising' | 'tense' | 'climax' | 'aftermath';
33:   spotlightDebtByHero: Record<string, number>;
34:   sceneCountByHero: Record<string, number>;
35:   lastActedAtByHero: Record<string, number>;
36:   nextSuggestedHeroId: string | null;
37:   openBeats: Array<{ id: string; label: string; status: 'open' | 'resolved'; updatedAt: number }>;
38:   updatedAt: number;
39: }
40: 
41: export interface NpcDraft {
42:   id?: string;
43:   name: string;
44:   culture?: string;
45:   peoples?: string;
46:   role?: string;
47:   faction?: string;
48:   appearance?: string;
49:   personality?: string;
50:   attitude?: string;
51:   objectives?: string;
52:   fears?: string;
53:   secrets?: string;
54:   notes?: string;
55:   trust?: number;
56:   location?: string;
57:   state?: string;
58:   speechStyle?: string;
59:   emotionalState?: string;
60:   nextAction?: string;
61:   lifeStatus?: 'alive' | 'missing' | 'dead';
62:   wealthIndex?: number;
63:   commitments?: Array<{ id: string; title: string; dueDay?: number; status: 'open' | 'fulfilled' | 'failed'; updatedAt: number }>;
64:   voiceProfile?: string;
65:   presentInScene?: boolean;
66: }
67: 
68: export interface PartyPosition {
69:   currentHexId: string | null;
70:   locationName: string;
71:   routeHexIds: string[];
72:   lastMovedAt?: number;
73: }
74: 
75: export interface WorldClock {
76:   year: number;
77:   season: 'spring' | 'summer' | 'autumn' | 'winter';
78:   day: number;
79:   hour: number;
80:   minute: number;
81:   elapsedMinutes: number;
82: }
83: 
84: export interface CompanyState {
85:   fellowshipCurrent: number;
86:   fellowshipMax: number;
87:   patron: string;
88:   sanctuaryIds: string[];
89:   eyeAwareness: number;
90:   journeyRoles: Partial<Record<JourneyRole, string>>;
91:   fellowshipFocus: Record<string, string>;
92:   spotlightByHero: Record<string, number>;
93:   lastShortRestDayKey?: string;
94:   lastLongRestDayKey?: string;
95:   lastLongRestWorldMinute?: number;
\`\`\`

### lib/master/types.ts — /interface Journey/ — linha 99

\`\`\`text
71:   routeHexIds: string[];
72:   lastMovedAt?: number;
73: }
74: 
75: export interface WorldClock {
76:   year: number;
77:   season: 'spring' | 'summer' | 'autumn' | 'winter';
78:   day: number;
79:   hour: number;
80:   minute: number;
81:   elapsedMinutes: number;
82: }
83: 
84: export interface CompanyState {
85:   fellowshipCurrent: number;
86:   fellowshipMax: number;
87:   patron: string;
88:   sanctuaryIds: string[];
89:   eyeAwareness: number;
90:   journeyRoles: Partial<Record<JourneyRole, string>>;
91:   fellowshipFocus: Record<string, string>;
92:   spotlightByHero: Record<string, number>;
93:   lastShortRestDayKey?: string;
94:   lastLongRestDayKey?: string;
95:   lastLongRestWorldMinute?: number;
96:   fellowshipBonusFromUndertaking?: number;
97: }
98: 
99: export interface JourneySegment {
100:   id: string;
101:   fromHexId: string | null;
102:   toHexId: string;
103:   distanceKm?: number;
104:   terrain?: string;
105:   travelDays?: number;
106:   startedAtWorldMinute?: number;
107:   completedAtWorldMinute?: number;
108:   startedAt: number;
109:   completedAt: number;
110: }
111: 
112: export interface JourneyState {
113:   status: 'idle' | 'planned' | 'travelling' | 'paused' | 'completed';
114:   originHexId: string | null;
115:   destinationHexId: string | null;
116:   startedAt?: number;
117:   day: number;
118:   segments: JourneySegment[];
119:   pendingEvent?: {
120:     id: string;
121:     targetRole: JourneyRole;
122:     skill: string;
123:     fatigueOnFailure: number;
124:     description: string;
125:     eventType?: string;
126:     featResult?: number;
127:     region?: 'border' | 'wild' | 'dark';
128:     terrain?: string;
129:     rollMode?: 'favoured' | 'normal' | 'ill-favoured';
130:     successDiceModifier?: number;
131:   } | null;
132:   plannedRouteHexIds?: string[];
133:   stageRouteHexIds?: string[];
134:   stageNumber?: number;
135:   stageComplete?: boolean;
136:   marchHexesRemaining?: number;
137:   marchRollCount?: number;
138:   marchWillReachDestination?: boolean;
139:   awaitingEventGeneration?: boolean;
140:   awaitingEndRoll?: boolean;
141:   durationAdjustmentDays?: number;
142:   mounted?: boolean;
143:   forcedMarch?: boolean;
144:   dangerEventsRemaining?: number;
145:   travelFatigueByHero?: Record<string, number>;
146: }
147: 
148: export interface CouncilState {
149:   status: 'idle' | 'introduction' | 'interaction' | 'resolved';
150:   id?: string;
151:   objective?: string;
152:   /** Legacy field kept for stored campaigns. In 4.9.21 it mirrors Resistance. */
153:   tolerance: number;
154:   resistance?: number;
155:   timeLimit?: number;
156:   testsUsed?: number;
157:   introductionSucceeded?: boolean;
158:   introductionSuccessIcons?: number;
159:   disasterRisk?: boolean;
160:   successes: number;
161:   failures: number;
162:   participants: string[];
163:   interlocutorNpcIds: string[];
164:   outcome?: 'success' | 'partial' | 'failure';
165: }
166: 
167: export interface CombatPosition { x: number; y: number; }
168: 
169: export interface CombatNarrativeBlock {
170:   id: string;
171:   round: number;
172:   text: string;
173:   createdAt: number;
174: }
175: 
176: export interface CombatRollRecord {
177:   id: string;
178:   actorId?: string;
179:   label: string;
180:   featDie: number | 'gandalf' | 'eye';
181:   successDice: number[];
182:   total: number;
183:   targetNumber?: number;
184:   success?: boolean;
185:   tengwar: number;
186:   createdAt: number;
187: }
188: 
\`\`\`

### lib/master/types.ts — /journey:/ — linha 383

\`\`\`text
355:   companyStyle: LivingCompanyStyle;
356:   troupeName: string;
357:   baseLocation: string;
358:   heroLives: Record<string, HeroLifeState>;
359:   projects: LifeProject[];
360:   organizations?: OrganizationState[];
361:   strongholds?: StrongholdState[];
362:   activityLog?: LivingWorldActivity[];
363:   pendingReview?: LivingWorldPending[];
364:   automationEnabled?: boolean;
365:   lastSimulatedAt?: number;
366:   updatedAt: number;
367: }
368: 
369: export interface HierarchicalMemory {
370:   campaignSummary: string;
371:   sessionSummaries: Array<{ id: string; title: string; summary: string; startedAt: number; endedAt?: number }>;
372:   canonicalFacts: Array<{ id: string; fact: string; tags: string[]; updatedAt: number }>;
373:   promises: Array<{ id: string; fromId: string; toId: string; promise: string; status: 'open' | 'fulfilled' | 'broken'; updatedAt: number }>;
374:   openThreads: Array<{ id: string; title: string; status: 'open' | 'closed'; updatedAt: number }>;
375: }
376: 
377: export interface MasterState {
378:   schemaVersion: 3;
379:   activeMode: AdventureMode;
380:   position: PartyPosition;
381:   company: CompanyState;
382:   worldClock: WorldClock;
383:   journey: JourneyState;
384:   council: CouncilState;
385:   combat: CombatState;
386:   fellowshipPhase: FellowshipPhaseState;
387:   quests: QuestState[];
388:   threatClocks: ThreatClock[];
389:   knowledge: KnowledgeEntry[];
390:   npcActors: NpcActorState[];
391:   livingWorld: LivingWorldState;
392:   scene: SceneState;
393:   sessionDirector: SessionDirectorState;
394:   memory: HierarchicalMemory;
395:   currentSessionId: string;
396:   updatedAt: number;
397: }
398: 
399: export type MasterCommand =
400:   | { type: 'adjust_endurance'; targetCharacterId: string; amount: number; reason: string; source?: string }
401:   | { type: 'adjust_hope'; targetCharacterId: string; amount: number; reason: string; source?: string }
402:   | { type: 'adjust_shadow'; targetCharacterId: string; amount: number; reason: string; source?: string }
403:   | { type: 'adjust_fatigue'; targetCharacterId: string; amount: number; reason: string; source?: string }
404:   | { type: 'spend_hope'; targetCharacterId: string; attribute: 'strength' | 'heart' | 'wits'; reason: string; source?: string }
405:   | { type: 'spend_fellowship_for_hope'; targetCharacterId: string; amount: number; reason: string; source?: string }
406:   | { type: 'adjust_supplies'; targetCharacterId: string; amount: number; reason: string; source?: string }
407:   | { type: 'set_condition'; targetCharacterId: string; condition: 'wounded' | 'weary' | 'miserable'; value: boolean; reason: string; source?: string }
408:   | { type: 'move_party'; toHexId: string; locationName: string; distanceKm?: number; terrain?: string; reason: string }
409:   | { type: 'set_present_npcs'; npcIds: string[]; reason: string }
410:   | { type: 'upsert_npc'; npc: NpcDraft; reason: string }
411:   | { type: 'update_npc_actor'; npcId: string; emotionalState?: string; nextAction?: string; beliefs?: string[]; suspicions?: string[]; location?: string; reason: string }
412:   | { type: 'open_scene'; kind: SceneKind; title: string; objective: string; participantHeroIds?: string[]; participantNpcIds?: string[]; tension?: number; endCondition?: string; reason: string }
413:   | { type: 'close_scene'; reason: string }
414:   | { type: 'set_spotlight'; targetCharacterId: string; reason: string }
415:   | { type: 'upsert_threat'; clock: Partial<ThreatClock> & Pick<ThreatClock, 'id' | 'name'>; reason: string }
416:   | { type: 'advance_threat'; clockId: string; amount: number; reason: string }
417:   | { type: 'upsert_quest'; quest: Partial<QuestState> & Pick<QuestState, 'id' | 'title'>; reason: string }
418:   | { type: 'add_knowledge'; entry: Omit<KnowledgeEntry, 'createdAt' | 'updatedAt'>; reason: string }
419:   | { type: 'set_living_company'; companyStyle: LivingCompanyStyle; troupeName?: string; baseLocation?: string; reason: string }
420:   | { type: 'upsert_hero_life'; characterId: string; life: Partial<Omit<HeroLifeState, 'characterId' | 'updatedAt'>>; reason: string }
421:   | { type: 'upsert_life_project'; project: Partial<LifeProject> & Pick<LifeProject, 'id' | 'kind' | 'title'>; reason: string }
422:   | { type: 'advance_life_project'; projectId: string; amount: number; resolution: string; reason: string }
423:   | { type: 'advance_time'; minutes: number; reason: string; source?: string }
424:   | { type: 'resolve_rest'; kind: 'short' | 'long'; safeRefuge?: boolean; reason: string; source?: string }
425:   | { type: 'set_mode'; mode: AdventureMode; reason: string }
426:   | { type: 'start_journey'; destinationHexId: string | null; roles?: Partial<Record<JourneyRole, string>>; routeHexIds?: string[]; mounted?: boolean; forcedMarch?: boolean; reason: string }
427:   | { type: 'complete_journey'; endRolls?: Record<string, { success: boolean; successIcons: number; mountVigour?: number }>; reason: string }
428:   | { type: 'resolve_journey_march'; success: boolean; successIcons: number; roll?: unknown; reason: string }
429:   | { type: 'generate_journey_event'; region: 'border' | 'wild' | 'dark'; targetDie: number; featResults: number[]; terrain?: string; reason: string }
430:   | { type: 'resolve_journey_event'; targetCharacterId: string; success: boolean; successIcons: number; roll?: unknown; reason: string }
431:   | { type: 'set_journey_danger'; danger: number; reason: string }
432:   | { type: 'continue_journey_stage'; reason: string }
433:   | { type: 'start_council'; objective: string; tolerance: number; participants: string[]; npcIds: string[]; reason: string }
434:   | { type: 'record_council_result'; success: boolean; successIcons?: number; introduction?: boolean; reason: string }
435:   | { type: 'resolve_council'; outcome: 'success' | 'partial' | 'failure'; reason: string }
436:   | { type: 'start_combat'; actorIds: string[]; reason: string }
437:   | { type: 'set_combat_stance'; targetCharacterId: string; stance: CombatStance; reason: string }
438:   | { type: 'next_combat_round'; reason: string }
439:   | { type: 'end_combat'; reason: string }
440:   | { type: 'upsert_combat_actor'; actor: CombatActorState; reason: string }
441:   | { type: 'move_combat_actor'; actorId: string; x: number; y: number; reason: string }
442:   | { type: 'record_combat_narrative'; text: string; reason: string }
443:   | { type: 'record_combat_roll'; roll: CombatRollRecord; reason: string }
444:   | { type: 'set_combat_background'; imageData: string | null; reason: string }
445:   | { type: 'engage_combat_actors'; actorId: string; targetActorId: string; reason: string }
446:   | { type: 'resolve_hero_attack'; attackerId: string; targetActorId: string; weaponIndex: number; spendHope?: boolean; inspired?: boolean; special?: 'heavy' | 'pierce' | 'defend'; reason: string }
447:   | { type: 'resolve_adversary_attack'; attackerId: string; targetCharacterId: string; successDice: number; damage: number; injury: number; ranged?: boolean; reason: string }
448:   | { type: 'resolve_first_aid'; healerCharacterId: string; targetCharacterId: string; reason: string }
449:   | { type: 'upsert_organization'; organization: Partial<OrganizationState> & Pick<OrganizationState, 'id' | 'name' | 'kind'>; reason: string }
450:   | { type: 'archive_organization'; organizationId: string; archived: boolean; reason: string }
451:   | { type: 'delete_organization'; organizationId: string; reason: string }
452:   | { type: 'advance_organization_project'; organizationId: string; projectId: string; amount: number; reason: string }
453:   | { type: 'upsert_stronghold'; stronghold: Partial<StrongholdState> & Pick<StrongholdState, 'id' | 'name'>; reason: string }
454:   | { type: 'claim_stronghold'; strongholdId: string; reason: string }
455:   | { type: 'abandon_stronghold'; strongholdId: string; reason: string }
456:   | { type: 'delete_stronghold'; strongholdId: string; reason: string }
457:   | { type: 'transfer_stronghold_loot'; strongholdId: string; lootItemId: string; targetCharacterId: string; quantity: number; reason: string }
458:   | { type: 'transfer_stronghold_treasure'; strongholdId: string; targetCharacterId: string; amount: number; reason: string }
459:   | { type: 'record_living_activity'; activity: Omit<LivingWorldActivity, 'createdAt'> & { createdAt?: number }; reason: string }
460:   | { type: 'start_fellowship_phase'; locationId?: string; yule?: boolean; reason: string }
461:   | { type: 'end_fellowship_phase'; reason: string }
462:   | { type: 'resolve_shadow_gain'; targetCharacterId: string; amount: number; source: 'dread' | 'greed' | 'sorcery' | 'transgression'; testSuccess: boolean; successIcons: number; reason: string }
463:   | { type: 'firm_will'; targetCharacterId: string; reason: string }
464:   | { type: 'bout_of_madness'; targetCharacterId: string; reason: string }
465:   | { type: 'advance_skill'; targetCharacterId: string; skillName: string; reason: string }
466:   | { type: 'advance_combat_proficiency'; targetCharacterId: string; group: string; reason: string }
467:   | { type: 'advance_valor'; targetCharacterId: string; reward: string; reason: string }
468:   | { type: 'advance_wisdom'; targetCharacterId: string; virtue: string; reason: string }
469:   | { type: 'heal_shadow_scar'; targetCharacterId: string; reason: string }
470:   | { type: 'record_undertaking'; targetCharacterId: string; undertaking: 'strengthen-fellowship' | 'heal-scars' | 'narrative'; reason: string };
471: 
472: export interface DirectorPlan {
\`\`\`

### lib/master/types.ts — /eyeAwareness/ — linha 89

\`\`\`text
61:   lifeStatus?: 'alive' | 'missing' | 'dead';
62:   wealthIndex?: number;
63:   commitments?: Array<{ id: string; title: string; dueDay?: number; status: 'open' | 'fulfilled' | 'failed'; updatedAt: number }>;
64:   voiceProfile?: string;
65:   presentInScene?: boolean;
66: }
67: 
68: export interface PartyPosition {
69:   currentHexId: string | null;
70:   locationName: string;
71:   routeHexIds: string[];
72:   lastMovedAt?: number;
73: }
74: 
75: export interface WorldClock {
76:   year: number;
77:   season: 'spring' | 'summer' | 'autumn' | 'winter';
78:   day: number;
79:   hour: number;
80:   minute: number;
81:   elapsedMinutes: number;
82: }
83: 
84: export interface CompanyState {
85:   fellowshipCurrent: number;
86:   fellowshipMax: number;
87:   patron: string;
88:   sanctuaryIds: string[];
89:   eyeAwareness: number;
90:   journeyRoles: Partial<Record<JourneyRole, string>>;
91:   fellowshipFocus: Record<string, string>;
92:   spotlightByHero: Record<string, number>;
93:   lastShortRestDayKey?: string;
94:   lastLongRestDayKey?: string;
95:   lastLongRestWorldMinute?: number;
96:   fellowshipBonusFromUndertaking?: number;
97: }
98: 
99: export interface JourneySegment {
100:   id: string;
101:   fromHexId: string | null;
102:   toHexId: string;
103:   distanceKm?: number;
104:   terrain?: string;
105:   travelDays?: number;
106:   startedAtWorldMinute?: number;
107:   completedAtWorldMinute?: number;
108:   startedAt: number;
109:   completedAt: number;
110: }
111: 
112: export interface JourneyState {
113:   status: 'idle' | 'planned' | 'travelling' | 'paused' | 'completed';
114:   originHexId: string | null;
115:   destinationHexId: string | null;
116:   startedAt?: number;
117:   day: number;
118:   segments: JourneySegment[];
119:   pendingEvent?: {
120:     id: string;
121:     targetRole: JourneyRole;
122:     skill: string;
123:     fatigueOnFailure: number;
124:     description: string;
125:     eventType?: string;
126:     featResult?: number;
127:     region?: 'border' | 'wild' | 'dark';
128:     terrain?: string;
129:     rollMode?: 'favoured' | 'normal' | 'ill-favoured';
130:     successDiceModifier?: number;
131:   } | null;
132:   plannedRouteHexIds?: string[];
133:   stageRouteHexIds?: string[];
134:   stageNumber?: number;
135:   stageComplete?: boolean;
136:   marchHexesRemaining?: number;
137:   marchRollCount?: number;
138:   marchWillReachDestination?: boolean;
139:   awaitingEventGeneration?: boolean;
140:   awaitingEndRoll?: boolean;
141:   durationAdjustmentDays?: number;
142:   mounted?: boolean;
143:   forcedMarch?: boolean;
144:   dangerEventsRemaining?: number;
145:   travelFatigueByHero?: Record<string, number>;
146: }
147: 
148: export interface CouncilState {
149:   status: 'idle' | 'introduction' | 'interaction' | 'resolved';
150:   id?: string;
151:   objective?: string;
152:   /** Legacy field kept for stored campaigns. In 4.9.21 it mirrors Resistance. */
153:   tolerance: number;
154:   resistance?: number;
155:   timeLimit?: number;
156:   testsUsed?: number;
157:   introductionSucceeded?: boolean;
158:   introductionSuccessIcons?: number;
159:   disasterRisk?: boolean;
160:   successes: number;
161:   failures: number;
162:   participants: string[];
163:   interlocutorNpcIds: string[];
164:   outcome?: 'success' | 'partial' | 'failure';
165: }
166: 
167: export interface CombatPosition { x: number; y: number; }
168: 
169: export interface CombatNarrativeBlock {
170:   id: string;
171:   round: number;
172:   text: string;
173:   createdAt: number;
174: }
175: 
176: export interface CombatRollRecord {
177:   id: string;
178:   actorId?: string;
\`\`\`

### lib/master/types.ts — /CompanyState/ — linha 84

\`\`\`text
56:   location?: string;
57:   state?: string;
58:   speechStyle?: string;
59:   emotionalState?: string;
60:   nextAction?: string;
61:   lifeStatus?: 'alive' | 'missing' | 'dead';
62:   wealthIndex?: number;
63:   commitments?: Array<{ id: string; title: string; dueDay?: number; status: 'open' | 'fulfilled' | 'failed'; updatedAt: number }>;
64:   voiceProfile?: string;
65:   presentInScene?: boolean;
66: }
67: 
68: export interface PartyPosition {
69:   currentHexId: string | null;
70:   locationName: string;
71:   routeHexIds: string[];
72:   lastMovedAt?: number;
73: }
74: 
75: export interface WorldClock {
76:   year: number;
77:   season: 'spring' | 'summer' | 'autumn' | 'winter';
78:   day: number;
79:   hour: number;
80:   minute: number;
81:   elapsedMinutes: number;
82: }
83: 
84: export interface CompanyState {
85:   fellowshipCurrent: number;
86:   fellowshipMax: number;
87:   patron: string;
88:   sanctuaryIds: string[];
89:   eyeAwareness: number;
90:   journeyRoles: Partial<Record<JourneyRole, string>>;
91:   fellowshipFocus: Record<string, string>;
92:   spotlightByHero: Record<string, number>;
93:   lastShortRestDayKey?: string;
94:   lastLongRestDayKey?: string;
95:   lastLongRestWorldMinute?: number;
96:   fellowshipBonusFromUndertaking?: number;
97: }
98: 
99: export interface JourneySegment {
100:   id: string;
101:   fromHexId: string | null;
102:   toHexId: string;
103:   distanceKm?: number;
104:   terrain?: string;
105:   travelDays?: number;
106:   startedAtWorldMinute?: number;
107:   completedAtWorldMinute?: number;
108:   startedAt: number;
109:   completedAt: number;
110: }
111: 
112: export interface JourneyState {
113:   status: 'idle' | 'planned' | 'travelling' | 'paused' | 'completed';
114:   originHexId: string | null;
115:   destinationHexId: string | null;
116:   startedAt?: number;
117:   day: number;
118:   segments: JourneySegment[];
119:   pendingEvent?: {
120:     id: string;
121:     targetRole: JourneyRole;
122:     skill: string;
123:     fatigueOnFailure: number;
124:     description: string;
125:     eventType?: string;
126:     featResult?: number;
127:     region?: 'border' | 'wild' | 'dark';
128:     terrain?: string;
129:     rollMode?: 'favoured' | 'normal' | 'ill-favoured';
130:     successDiceModifier?: number;
131:   } | null;
132:   plannedRouteHexIds?: string[];
133:   stageRouteHexIds?: string[];
134:   stageNumber?: number;
135:   stageComplete?: boolean;
136:   marchHexesRemaining?: number;
137:   marchRollCount?: number;
138:   marchWillReachDestination?: boolean;
139:   awaitingEventGeneration?: boolean;
140:   awaitingEndRoll?: boolean;
141:   durationAdjustmentDays?: number;
142:   mounted?: boolean;
143:   forcedMarch?: boolean;
144:   dangerEventsRemaining?: number;
145:   travelFatigueByHero?: Record<string, number>;
146: }
147: 
148: export interface CouncilState {
149:   status: 'idle' | 'introduction' | 'interaction' | 'resolved';
150:   id?: string;
151:   objective?: string;
152:   /** Legacy field kept for stored campaigns. In 4.9.21 it mirrors Resistance. */
153:   tolerance: number;
154:   resistance?: number;
155:   timeLimit?: number;
156:   testsUsed?: number;
157:   introductionSucceeded?: boolean;
158:   introductionSuccessIcons?: number;
159:   disasterRisk?: boolean;
160:   successes: number;
161:   failures: number;
162:   participants: string[];
163:   interlocutorNpcIds: string[];
164:   outcome?: 'success' | 'partial' | 'failure';
165: }
166: 
167: export interface CombatPosition { x: number; y: number; }
168: 
169: export interface CombatNarrativeBlock {
170:   id: string;
171:   round: number;
172:   text: string;
173:   createdAt: number;
\`\`\`

### lib/master/state.ts — /eyeAwareness/ — linha 52

\`\`\`text
24:   };
25: }
26: 
27: function defaultSessionDirector(now: number): SessionDirectorState {
28:   return {
29:     sessionObjective: '',
30:     pacing: 'quiet',
31:     spotlightDebtByHero: {},
32:     sceneCountByHero: {},
33:     lastActedAtByHero: {},
34:     nextSuggestedHeroId: null,
35:     openBeats: [],
36:     updatedAt: now,
37:   };
38: }
39: 
40: export function createDefaultMasterState(campaign: Pick<CampaignState, 'id' | 'currentLocation' | 'campaignSummary' | 'partyCharacterIds' | 'activeCharacterId'>): MasterState {
41:   const now = Date.now();
42:   return {
43:     schemaVersion: 3,
44:     activeMode: 'adventure',
45:     position: { currentHexId: null, locationName: campaign.currentLocation || 'Eriador', routeHexIds: [] },
46:     worldClock: { year: 2965, season: 'spring', day: 1, hour: 8, minute: 0, elapsedMinutes: 0 },
47:     company: {
48:       fellowshipCurrent: 0,
49:       fellowshipMax: 0,
50:       patron: '',
51:       sanctuaryIds: [],
52:       eyeAwareness: 0,
53:       journeyRoles: {},
54:       fellowshipFocus: {},
55:       spotlightByHero: {},
56:     },
57:     journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEndRoll: false, durationAdjustmentDays: 0, mounted: false, forcedMarch: false, dangerEventsRemaining: 0, travelFatigueByHero: {} },
58:     council: { status: 'idle', tolerance: 0, resistance: 0, timeLimit: 0, testsUsed: 0, successes: 0, failures: 0, participants: [], interlocutorNpcIds: [] },
59:     combat: { status: 'idle', round: 0, actors: [], advantageDice: 0, complicationDice: 0, turnIndex: 0, backgroundImage: null, narrativeBlocks: [], rolls: [], updatedAt: now },
60:     fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {}, isYule: false, skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {} },
61:     quests: [],
62:     threatClocks: [],
63:     knowledge: [],
64:     npcActors: [],
65:     livingWorld: {
66:       companyStyle: 'fellowship',
67:       troupeName: '',
68:       baseLocation: campaign.currentLocation || 'Eriador',
69:       heroLives: {},
70:       projects: [],
71:       organizations: [],
72:       strongholds: [],
73:       activityLog: [],
74:       pendingReview: [],
75:       automationEnabled: true,
76:       lastSimulatedAt: now,
77:       updatedAt: now,
78:     },
79:     scene: defaultScene(campaign, now),
80:     sessionDirector: defaultSessionDirector(now),
81:     memory: {
82:       campaignSummary: campaign.campaignSummary || '',
83:       sessionSummaries: [],
84:       canonicalFacts: [],
85:       promises: [],
86:       openThreads: [],
87:     },
88:     currentSessionId: `session_${campaign.id}_${now}`,
89:     updatedAt: now,
90:   };
91: }
92: 
93: function inferSceneKind(mode: MasterState['activeMode']): SceneKind {
94:   if (mode === 'adventure') return 'exploration';
95:   return mode;
96: }
97: 
98: function normalizeStringArray(value: unknown): string[] {
99:   return Array.isArray(value) ? Array.from(new Set(value.map(String).filter(Boolean))) : [];
100: }
101: 
102: export function normalizeMasterState(campaign: CampaignState): MasterState {
103:   const base = createDefaultMasterState(campaign);
104:   const incoming = campaign.masterState as MasterState | undefined;
105:   if (!incoming) return base;
106:   const partyIds = new Set(campaign.partyCharacterIds || []);
107:   const incomingScene = incoming.scene || base.scene;
108:   const sceneHeroes = normalizeStringArray(incomingScene.participantHeroIds).filter(id => partyIds.has(id));
109:   const allHeroes = normalizeStringArray(campaign.partyCharacterIds);
110:   const participants = sceneHeroes.length ? sceneHeroes : allHeroes;
111:   const spotlight = incomingScene.spotlightCharacterId && partyIds.has(incomingScene.spotlightCharacterId)
112:     ? incomingScene.spotlightCharacterId
113:     : campaign.activeCharacterId && partyIds.has(campaign.activeCharacterId)
114:       ? campaign.activeCharacterId
115:       : participants[0] || null;
116:   const scene: SceneState = {
117:     ...base.scene,
118:     ...incomingScene,
119:     id: incomingScene.id || base.scene.id,
120:     kind: incomingScene.kind || inferSceneKind(incoming.activeMode || base.activeMode),
121:     locationName: incomingScene.locationName || incoming.position?.locationName || campaign.currentLocation || base.scene.locationName,
122:     participantHeroIds: participants,
123:     participantNpcIds: normalizeStringArray(incomingScene.participantNpcIds),
124:     spotlightCharacterId: spotlight,
125:     spotlightQueue: normalizeStringArray(incomingScene.spotlightQueue).filter(id => partyIds.has(id)),
126:     pendingActions: incomingScene.pendingActions || {},
127:     tension: Math.max(0, Math.min(6, Number(incomingScene.tension) || 0)),
128:     startedAt: Number(incomingScene.startedAt) || Date.now(),
129:     updatedAt: Number(incomingScene.updatedAt) || Date.now(),
130:   };
131:   if (!scene.spotlightQueue.length) scene.spotlightQueue = spotlight ? [spotlight, ...participants.filter(id => id !== spotlight)] : participants;
132:   const normalizedCombatActors = (incoming.combat?.actors || []).map(actor => {
133:     if (!actor.position) return actor;
134:     const rawX = Number(actor.position.x);
135:     const rawY = Number(actor.position.y);
136:     const x = Math.max(0.03, Math.min(0.97, Number.isFinite(rawX) ? (Math.abs(rawX) > 1.5 ? rawX / 100 : rawX) : 0.5));
137:     const y = Math.max(0.05, Math.min(0.95, Number.isFinite(rawY) ? (Math.abs(rawY) > 1.5 ? rawY / 100 : rawY) : 0.5));
138:     return { ...actor, position: { x, y } };
139:   });
140: 
141:   const director: SessionDirectorState = {
\`\`\`

### lib/master/state.ts — /journey:/ — linha 57

\`\`\`text
29:     sessionObjective: '',
30:     pacing: 'quiet',
31:     spotlightDebtByHero: {},
32:     sceneCountByHero: {},
33:     lastActedAtByHero: {},
34:     nextSuggestedHeroId: null,
35:     openBeats: [],
36:     updatedAt: now,
37:   };
38: }
39: 
40: export function createDefaultMasterState(campaign: Pick<CampaignState, 'id' | 'currentLocation' | 'campaignSummary' | 'partyCharacterIds' | 'activeCharacterId'>): MasterState {
41:   const now = Date.now();
42:   return {
43:     schemaVersion: 3,
44:     activeMode: 'adventure',
45:     position: { currentHexId: null, locationName: campaign.currentLocation || 'Eriador', routeHexIds: [] },
46:     worldClock: { year: 2965, season: 'spring', day: 1, hour: 8, minute: 0, elapsedMinutes: 0 },
47:     company: {
48:       fellowshipCurrent: 0,
49:       fellowshipMax: 0,
50:       patron: '',
51:       sanctuaryIds: [],
52:       eyeAwareness: 0,
53:       journeyRoles: {},
54:       fellowshipFocus: {},
55:       spotlightByHero: {},
56:     },
57:     journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEndRoll: false, durationAdjustmentDays: 0, mounted: false, forcedMarch: false, dangerEventsRemaining: 0, travelFatigueByHero: {} },
58:     council: { status: 'idle', tolerance: 0, resistance: 0, timeLimit: 0, testsUsed: 0, successes: 0, failures: 0, participants: [], interlocutorNpcIds: [] },
59:     combat: { status: 'idle', round: 0, actors: [], advantageDice: 0, complicationDice: 0, turnIndex: 0, backgroundImage: null, narrativeBlocks: [], rolls: [], updatedAt: now },
60:     fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {}, isYule: false, skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {} },
61:     quests: [],
62:     threatClocks: [],
63:     knowledge: [],
64:     npcActors: [],
65:     livingWorld: {
66:       companyStyle: 'fellowship',
67:       troupeName: '',
68:       baseLocation: campaign.currentLocation || 'Eriador',
69:       heroLives: {},
70:       projects: [],
71:       organizations: [],
72:       strongholds: [],
73:       activityLog: [],
74:       pendingReview: [],
75:       automationEnabled: true,
76:       lastSimulatedAt: now,
77:       updatedAt: now,
78:     },
79:     scene: defaultScene(campaign, now),
80:     sessionDirector: defaultSessionDirector(now),
81:     memory: {
82:       campaignSummary: campaign.campaignSummary || '',
83:       sessionSummaries: [],
84:       canonicalFacts: [],
85:       promises: [],
86:       openThreads: [],
87:     },
88:     currentSessionId: `session_${campaign.id}_${now}`,
89:     updatedAt: now,
90:   };
91: }
92: 
93: function inferSceneKind(mode: MasterState['activeMode']): SceneKind {
94:   if (mode === 'adventure') return 'exploration';
95:   return mode;
96: }
97: 
98: function normalizeStringArray(value: unknown): string[] {
99:   return Array.isArray(value) ? Array.from(new Set(value.map(String).filter(Boolean))) : [];
100: }
101: 
102: export function normalizeMasterState(campaign: CampaignState): MasterState {
103:   const base = createDefaultMasterState(campaign);
104:   const incoming = campaign.masterState as MasterState | undefined;
105:   if (!incoming) return base;
106:   const partyIds = new Set(campaign.partyCharacterIds || []);
107:   const incomingScene = incoming.scene || base.scene;
108:   const sceneHeroes = normalizeStringArray(incomingScene.participantHeroIds).filter(id => partyIds.has(id));
109:   const allHeroes = normalizeStringArray(campaign.partyCharacterIds);
110:   const participants = sceneHeroes.length ? sceneHeroes : allHeroes;
111:   const spotlight = incomingScene.spotlightCharacterId && partyIds.has(incomingScene.spotlightCharacterId)
112:     ? incomingScene.spotlightCharacterId
113:     : campaign.activeCharacterId && partyIds.has(campaign.activeCharacterId)
114:       ? campaign.activeCharacterId
115:       : participants[0] || null;
116:   const scene: SceneState = {
117:     ...base.scene,
118:     ...incomingScene,
119:     id: incomingScene.id || base.scene.id,
120:     kind: incomingScene.kind || inferSceneKind(incoming.activeMode || base.activeMode),
121:     locationName: incomingScene.locationName || incoming.position?.locationName || campaign.currentLocation || base.scene.locationName,
122:     participantHeroIds: participants,
123:     participantNpcIds: normalizeStringArray(incomingScene.participantNpcIds),
124:     spotlightCharacterId: spotlight,
125:     spotlightQueue: normalizeStringArray(incomingScene.spotlightQueue).filter(id => partyIds.has(id)),
126:     pendingActions: incomingScene.pendingActions || {},
127:     tension: Math.max(0, Math.min(6, Number(incomingScene.tension) || 0)),
128:     startedAt: Number(incomingScene.startedAt) || Date.now(),
129:     updatedAt: Number(incomingScene.updatedAt) || Date.now(),
130:   };
131:   if (!scene.spotlightQueue.length) scene.spotlightQueue = spotlight ? [spotlight, ...participants.filter(id => id !== spotlight)] : participants;
132:   const normalizedCombatActors = (incoming.combat?.actors || []).map(actor => {
133:     if (!actor.position) return actor;
134:     const rawX = Number(actor.position.x);
135:     const rawY = Number(actor.position.y);
136:     const x = Math.max(0.03, Math.min(0.97, Number.isFinite(rawX) ? (Math.abs(rawX) > 1.5 ? rawX / 100 : rawX) : 0.5));
137:     const y = Math.max(0.05, Math.min(0.95, Number.isFinite(rawY) ? (Math.abs(rawY) > 1.5 ? rawY / 100 : rawY) : 0.5));
138:     return { ...actor, position: { x, y } };
139:   });
140: 
141:   const director: SessionDirectorState = {
142:     ...base.sessionDirector,
143:     ...(incoming.sessionDirector || {}),
144:     spotlightDebtByHero: incoming.sessionDirector?.spotlightDebtByHero || {},
145:     sceneCountByHero: incoming.sessionDirector?.sceneCountByHero || {},
146:     lastActedAtByHero: incoming.sessionDirector?.lastActedAtByHero || {},
\`\`\`

### lib/master/state.ts — /scene:/ — linha 79

\`\`\`text
51:       sanctuaryIds: [],
52:       eyeAwareness: 0,
53:       journeyRoles: {},
54:       fellowshipFocus: {},
55:       spotlightByHero: {},
56:     },
57:     journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEndRoll: false, durationAdjustmentDays: 0, mounted: false, forcedMarch: false, dangerEventsRemaining: 0, travelFatigueByHero: {} },
58:     council: { status: 'idle', tolerance: 0, resistance: 0, timeLimit: 0, testsUsed: 0, successes: 0, failures: 0, participants: [], interlocutorNpcIds: [] },
59:     combat: { status: 'idle', round: 0, actors: [], advantageDice: 0, complicationDice: 0, turnIndex: 0, backgroundImage: null, narrativeBlocks: [], rolls: [], updatedAt: now },
60:     fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {}, isYule: false, skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {} },
61:     quests: [],
62:     threatClocks: [],
63:     knowledge: [],
64:     npcActors: [],
65:     livingWorld: {
66:       companyStyle: 'fellowship',
67:       troupeName: '',
68:       baseLocation: campaign.currentLocation || 'Eriador',
69:       heroLives: {},
70:       projects: [],
71:       organizations: [],
72:       strongholds: [],
73:       activityLog: [],
74:       pendingReview: [],
75:       automationEnabled: true,
76:       lastSimulatedAt: now,
77:       updatedAt: now,
78:     },
79:     scene: defaultScene(campaign, now),
80:     sessionDirector: defaultSessionDirector(now),
81:     memory: {
82:       campaignSummary: campaign.campaignSummary || '',
83:       sessionSummaries: [],
84:       canonicalFacts: [],
85:       promises: [],
86:       openThreads: [],
87:     },
88:     currentSessionId: `session_${campaign.id}_${now}`,
89:     updatedAt: now,
90:   };
91: }
92: 
93: function inferSceneKind(mode: MasterState['activeMode']): SceneKind {
94:   if (mode === 'adventure') return 'exploration';
95:   return mode;
96: }
97: 
98: function normalizeStringArray(value: unknown): string[] {
99:   return Array.isArray(value) ? Array.from(new Set(value.map(String).filter(Boolean))) : [];
100: }
101: 
102: export function normalizeMasterState(campaign: CampaignState): MasterState {
103:   const base = createDefaultMasterState(campaign);
104:   const incoming = campaign.masterState as MasterState | undefined;
105:   if (!incoming) return base;
106:   const partyIds = new Set(campaign.partyCharacterIds || []);
107:   const incomingScene = incoming.scene || base.scene;
108:   const sceneHeroes = normalizeStringArray(incomingScene.participantHeroIds).filter(id => partyIds.has(id));
109:   const allHeroes = normalizeStringArray(campaign.partyCharacterIds);
110:   const participants = sceneHeroes.length ? sceneHeroes : allHeroes;
111:   const spotlight = incomingScene.spotlightCharacterId && partyIds.has(incomingScene.spotlightCharacterId)
112:     ? incomingScene.spotlightCharacterId
113:     : campaign.activeCharacterId && partyIds.has(campaign.activeCharacterId)
114:       ? campaign.activeCharacterId
115:       : participants[0] || null;
116:   const scene: SceneState = {
117:     ...base.scene,
118:     ...incomingScene,
119:     id: incomingScene.id || base.scene.id,
120:     kind: incomingScene.kind || inferSceneKind(incoming.activeMode || base.activeMode),
121:     locationName: incomingScene.locationName || incoming.position?.locationName || campaign.currentLocation || base.scene.locationName,
122:     participantHeroIds: participants,
123:     participantNpcIds: normalizeStringArray(incomingScene.participantNpcIds),
124:     spotlightCharacterId: spotlight,
125:     spotlightQueue: normalizeStringArray(incomingScene.spotlightQueue).filter(id => partyIds.has(id)),
126:     pendingActions: incomingScene.pendingActions || {},
127:     tension: Math.max(0, Math.min(6, Number(incomingScene.tension) || 0)),
128:     startedAt: Number(incomingScene.startedAt) || Date.now(),
129:     updatedAt: Number(incomingScene.updatedAt) || Date.now(),
130:   };
131:   if (!scene.spotlightQueue.length) scene.spotlightQueue = spotlight ? [spotlight, ...participants.filter(id => id !== spotlight)] : participants;
132:   const normalizedCombatActors = (incoming.combat?.actors || []).map(actor => {
133:     if (!actor.position) return actor;
134:     const rawX = Number(actor.position.x);
135:     const rawY = Number(actor.position.y);
136:     const x = Math.max(0.03, Math.min(0.97, Number.isFinite(rawX) ? (Math.abs(rawX) > 1.5 ? rawX / 100 : rawX) : 0.5));
137:     const y = Math.max(0.05, Math.min(0.95, Number.isFinite(rawY) ? (Math.abs(rawY) > 1.5 ? rawY / 100 : rawY) : 0.5));
138:     return { ...actor, position: { x, y } };
139:   });
140: 
141:   const director: SessionDirectorState = {
142:     ...base.sessionDirector,
143:     ...(incoming.sessionDirector || {}),
144:     spotlightDebtByHero: incoming.sessionDirector?.spotlightDebtByHero || {},
145:     sceneCountByHero: incoming.sessionDirector?.sceneCountByHero || {},
146:     lastActedAtByHero: incoming.sessionDirector?.lastActedAtByHero || {},
147:     nextSuggestedHeroId: incoming.sessionDirector?.nextSuggestedHeroId && partyIds.has(incoming.sessionDirector.nextSuggestedHeroId)
148:       ? incoming.sessionDirector.nextSuggestedHeroId
149:       : null,
150:     openBeats: Array.isArray(incoming.sessionDirector?.openBeats) ? incoming.sessionDirector.openBeats : [],
151:     updatedAt: incoming.sessionDirector?.updatedAt || Date.now(),
152:   };
153:   return {
154:     ...base,
155:     ...incoming,
156:     position: {
157:       ...base.position,
158:       ...(incoming.position || {}),
159:       locationName: incoming.position?.locationName || campaign.currentLocation || base.position.locationName,
160:       routeHexIds: Array.isArray(incoming.position?.routeHexIds) ? incoming.position.routeHexIds : [],
161:     },
162:     worldClock: { ...base.worldClock, ...(incoming.worldClock || {}) },
163:     company: {
164:       ...base.company,
165:       ...(incoming.company || {}),
166:       sanctuaryIds: Array.isArray(incoming.company?.sanctuaryIds) ? incoming.company.sanctuaryIds : [],
167:       journeyRoles: incoming.company?.journeyRoles || {},
168:       fellowshipFocus: incoming.company?.fellowshipFocus || {},
\`\`\`

### lib/master/commands.ts — /case 'start_journey'/ — linha 512

\`\`\`text
484:             ...character.sheet,
485:             identity: {
486:               ...character.sheet.identity,
487:               age: Math.max(0, Number(character.sheet.identity.age) || 0) + advanced.yearsElapsed,
488:             },
489:           },
490:           updatedAt: Date.now(),
491:         }));
492:         nextState.livingWorld = {
493:           ...nextState.livingWorld,
494:           heroLives: Object.fromEntries(Object.entries(nextState.livingWorld.heroLives).map(([characterId, life]) => [
495:             characterId,
496:             {
497:               ...life,
498:               householdMembers: life.householdMembers.map(member => ({ ...member, age: Math.min(150, member.age + advanced.yearsElapsed) })),
499:               updatedAt: Date.now(),
500:             },
501:           ])),
502:           updatedAt: Date.now(),
503:         };
504:       }
505:       break;
506:     }
507:     case 'set_mode':
508:       assertModeTransitionAllowed(nextState, command.mode);
509:       nextState = { ...nextState, activeMode: command.mode, updatedAt: Date.now() };
510:       if (nextState.scene.status === 'idle') nextState.scene = { ...nextState.scene, kind: modeToScene(command.mode), updatedAt: Date.now() };
511:       break;
512:     case 'start_journey': {
513:       const roles = command.roles || nextState.company.journeyRoles;
514:       const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
515:       if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
516:       const route = (command.routeHexIds?.length ? command.routeHexIds : nextState.journey.plannedRouteHexIds || []).filter(Boolean);
517:       if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
518:       nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));
519:       nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
520:       break;
521:     }
522:     case 'resolve_journey_march':
523:       nextState = resolveJourneyMarch(nextState, command.success, command.successIcons);
524:       break;
525:     case 'generate_journey_event':
526:       nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
527:       break;
528:     case 'resolve_journey_event': {
529:       const resolved = resolveJourneyEvent(nextState, command.success, command.successIcons);
530:       nextState = resolved.state;
531:       const fx = resolved.effects;
532:       const pendingFatigue = { ...(nextState.journey.travelFatigueByHero || {}) };
533:       for (const characterId of partyIds) {
534:         const hero = nextCharacters.find(character => character.id === characterId);
535:         if (fx.allFatigue && hero && !rangerIgnoresJourneyFatigue(hero.sheet)) {
536:           const cramReduction = /\bcram\b/i.test(hero.sheet.other.virtues || '') ? 1 : 0;
537:           pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);
538:         }
539:         if (fx.allShadow) updateHero(characterId, sheet => adjustShadowCanonical(sheet, fx.allShadow));
540:         if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
541:       }
542:       if (fx.targetFatigue) {
543:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
544:         if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[command.targetCharacterId] = Math.max(0, Number(pendingFatigue[command.targetCharacterId]) || 0) + fx.targetFatigue;
545:       }
546:       nextState.journey = { ...nextState.journey, travelFatigueByHero: pendingFatigue };
547:       if (fx.targetShadow) updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, fx.targetShadow));
548:       if (fx.woundTarget) updateHero(command.targetCharacterId, sheet => applyHeroWound(sheet).sheet);
549:       if (fx.dayAdjustment > 0) {
550:         const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
551:         nextState = simulated.master; nextNpcs = simulated.npcs;
552:       }
553:       break;
554:     }
555:     case 'set_journey_danger':
556:       nextState = setDangerousArea(nextState, command.danger);
557:       break;
558:     case 'continue_journey_stage':
559:       nextState = continueJourneyStage(nextState);
560:       break;
561:     case 'complete_journey': {
562:       if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
563:       for (const characterId of partyIds) {
564:         const result = command.endRolls?.[characterId];
565:         const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
566:         const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
567:         const remaining = Math.max(0, accumulated - reduction);
568:         if (remaining > 0) updateHero(characterId, sheet => adjustFatigue(sheet, remaining));
569:       }
570:       nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {} } });
571:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
572:       break;
573:     }
574:     case 'start_council':
575:       nextState = startCouncil(nextState, command.objective, command.tolerance, command.participants.filter(id => partyIds.has(id)), command.npcIds);
576:       nextState.scene = modeScene(nextState, 'council', 'Conselho', command.objective, command.participants.filter(id => partyIds.has(id)), command.npcIds);
577:       break;
578:     case 'record_council_result': nextState = recordCouncilResult(nextState, command.success, command.successIcons || 0, command.introduction === true); break;
579:     case 'resolve_council':
580:       nextState = resolveCouncil(nextState, command.outcome);
581:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
582:       break;
583:     case 'start_combat': {
584:       const heroActorIds = command.actorIds.filter(id => partyIds.has(id));
585:       nextState = startCombat(nextState, nextCharacters, heroActorIds);
586:       const requestedNpcIds = command.actorIds.filter(id => !partyIds.has(id));
587:       const sceneNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), ...requestedNpcIds]));
588:       sceneNpcIds.forEach((npcId, index) => {
589:         const npc = nextNpcs.find(item => item.id === npcId);
590:         if (!npc) return;
591:         const preparedActor = nextState.combat.actors.find(actor => actor.actorId === npc.id);
592:         nextState = upsertCombatActor(nextState, {
593:           actorId: npc.id,
594:           actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
595:           engagedWith: [],
596:           endurance: 10,
597:           enduranceMax: 10,
598:           hate: 2,
599:           wounded: false,
600:           defeated: false,
601:           initiative: Math.max(1, 10 - index),
\`\`\`

### lib/master/commands.ts — /case 'resolve_journey_march'/ — linha 522

\`\`\`text
494:           heroLives: Object.fromEntries(Object.entries(nextState.livingWorld.heroLives).map(([characterId, life]) => [
495:             characterId,
496:             {
497:               ...life,
498:               householdMembers: life.householdMembers.map(member => ({ ...member, age: Math.min(150, member.age + advanced.yearsElapsed) })),
499:               updatedAt: Date.now(),
500:             },
501:           ])),
502:           updatedAt: Date.now(),
503:         };
504:       }
505:       break;
506:     }
507:     case 'set_mode':
508:       assertModeTransitionAllowed(nextState, command.mode);
509:       nextState = { ...nextState, activeMode: command.mode, updatedAt: Date.now() };
510:       if (nextState.scene.status === 'idle') nextState.scene = { ...nextState.scene, kind: modeToScene(command.mode), updatedAt: Date.now() };
511:       break;
512:     case 'start_journey': {
513:       const roles = command.roles || nextState.company.journeyRoles;
514:       const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
515:       if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
516:       const route = (command.routeHexIds?.length ? command.routeHexIds : nextState.journey.plannedRouteHexIds || []).filter(Boolean);
517:       if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
518:       nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));
519:       nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
520:       break;
521:     }
522:     case 'resolve_journey_march':
523:       nextState = resolveJourneyMarch(nextState, command.success, command.successIcons);
524:       break;
525:     case 'generate_journey_event':
526:       nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
527:       break;
528:     case 'resolve_journey_event': {
529:       const resolved = resolveJourneyEvent(nextState, command.success, command.successIcons);
530:       nextState = resolved.state;
531:       const fx = resolved.effects;
532:       const pendingFatigue = { ...(nextState.journey.travelFatigueByHero || {}) };
533:       for (const characterId of partyIds) {
534:         const hero = nextCharacters.find(character => character.id === characterId);
535:         if (fx.allFatigue && hero && !rangerIgnoresJourneyFatigue(hero.sheet)) {
536:           const cramReduction = /\bcram\b/i.test(hero.sheet.other.virtues || '') ? 1 : 0;
537:           pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);
538:         }
539:         if (fx.allShadow) updateHero(characterId, sheet => adjustShadowCanonical(sheet, fx.allShadow));
540:         if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
541:       }
542:       if (fx.targetFatigue) {
543:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
544:         if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[command.targetCharacterId] = Math.max(0, Number(pendingFatigue[command.targetCharacterId]) || 0) + fx.targetFatigue;
545:       }
546:       nextState.journey = { ...nextState.journey, travelFatigueByHero: pendingFatigue };
547:       if (fx.targetShadow) updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, fx.targetShadow));
548:       if (fx.woundTarget) updateHero(command.targetCharacterId, sheet => applyHeroWound(sheet).sheet);
549:       if (fx.dayAdjustment > 0) {
550:         const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
551:         nextState = simulated.master; nextNpcs = simulated.npcs;
552:       }
553:       break;
554:     }
555:     case 'set_journey_danger':
556:       nextState = setDangerousArea(nextState, command.danger);
557:       break;
558:     case 'continue_journey_stage':
559:       nextState = continueJourneyStage(nextState);
560:       break;
561:     case 'complete_journey': {
562:       if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
563:       for (const characterId of partyIds) {
564:         const result = command.endRolls?.[characterId];
565:         const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
566:         const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
567:         const remaining = Math.max(0, accumulated - reduction);
568:         if (remaining > 0) updateHero(characterId, sheet => adjustFatigue(sheet, remaining));
569:       }
570:       nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {} } });
571:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
572:       break;
573:     }
574:     case 'start_council':
575:       nextState = startCouncil(nextState, command.objective, command.tolerance, command.participants.filter(id => partyIds.has(id)), command.npcIds);
576:       nextState.scene = modeScene(nextState, 'council', 'Conselho', command.objective, command.participants.filter(id => partyIds.has(id)), command.npcIds);
577:       break;
578:     case 'record_council_result': nextState = recordCouncilResult(nextState, command.success, command.successIcons || 0, command.introduction === true); break;
579:     case 'resolve_council':
580:       nextState = resolveCouncil(nextState, command.outcome);
581:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
582:       break;
583:     case 'start_combat': {
584:       const heroActorIds = command.actorIds.filter(id => partyIds.has(id));
585:       nextState = startCombat(nextState, nextCharacters, heroActorIds);
586:       const requestedNpcIds = command.actorIds.filter(id => !partyIds.has(id));
587:       const sceneNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), ...requestedNpcIds]));
588:       sceneNpcIds.forEach((npcId, index) => {
589:         const npc = nextNpcs.find(item => item.id === npcId);
590:         if (!npc) return;
591:         const preparedActor = nextState.combat.actors.find(actor => actor.actorId === npc.id);
592:         nextState = upsertCombatActor(nextState, {
593:           actorId: npc.id,
594:           actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
595:           engagedWith: [],
596:           endurance: 10,
597:           enduranceMax: 10,
598:           hate: 2,
599:           wounded: false,
600:           defeated: false,
601:           initiative: Math.max(1, 10 - index),
602:           tokenId: npc.tokenId || null,
603:           position: preparedActor?.position || { x: 0.18 + (index % 3) * 0.32, y: 0.20 + Math.floor(index / 3) * 0.13 },
604:           quickSheet: { name: npc.name, role: npc.role, culture: npc.culture || npc.peoples, personality: npc.personality, attitude: npc.attitude, objectives: npc.objectives, fears: npc.fears, nextAction: npc.nextAction },
605:         });
606:       });
607:       nextState.scene = modeScene(nextState, 'combat', 'Combate', 'Superar o confronto', heroActorIds, sceneNpcIds);
608:       appendCampaignEvent('combat_started', { round: 1, heroes: heroActorIds.length, npcs: sceneNpcIds.length, location: nextState.position.locationName || nextCampaign.currentLocation });
609:       appendLivingActivity('world', 'Combate iniciado', `${heroActorIds.length} Herói(s) e ${sceneNpcIds.length} NPC(s)/adversário(s) entraram no confronto em ${nextState.position.locationName || nextCampaign.currentLocation}.`, nextState.combat.id);
610:       break;
611:     }
\`\`\`

### lib/master/commands.ts — /case 'generate_journey_event'/ — linha 525

\`\`\`text
497:               ...life,
498:               householdMembers: life.householdMembers.map(member => ({ ...member, age: Math.min(150, member.age + advanced.yearsElapsed) })),
499:               updatedAt: Date.now(),
500:             },
501:           ])),
502:           updatedAt: Date.now(),
503:         };
504:       }
505:       break;
506:     }
507:     case 'set_mode':
508:       assertModeTransitionAllowed(nextState, command.mode);
509:       nextState = { ...nextState, activeMode: command.mode, updatedAt: Date.now() };
510:       if (nextState.scene.status === 'idle') nextState.scene = { ...nextState.scene, kind: modeToScene(command.mode), updatedAt: Date.now() };
511:       break;
512:     case 'start_journey': {
513:       const roles = command.roles || nextState.company.journeyRoles;
514:       const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
515:       if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
516:       const route = (command.routeHexIds?.length ? command.routeHexIds : nextState.journey.plannedRouteHexIds || []).filter(Boolean);
517:       if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
518:       nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));
519:       nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
520:       break;
521:     }
522:     case 'resolve_journey_march':
523:       nextState = resolveJourneyMarch(nextState, command.success, command.successIcons);
524:       break;
525:     case 'generate_journey_event':
526:       nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
527:       break;
528:     case 'resolve_journey_event': {
529:       const resolved = resolveJourneyEvent(nextState, command.success, command.successIcons);
530:       nextState = resolved.state;
531:       const fx = resolved.effects;
532:       const pendingFatigue = { ...(nextState.journey.travelFatigueByHero || {}) };
533:       for (const characterId of partyIds) {
534:         const hero = nextCharacters.find(character => character.id === characterId);
535:         if (fx.allFatigue && hero && !rangerIgnoresJourneyFatigue(hero.sheet)) {
536:           const cramReduction = /\bcram\b/i.test(hero.sheet.other.virtues || '') ? 1 : 0;
537:           pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);
538:         }
539:         if (fx.allShadow) updateHero(characterId, sheet => adjustShadowCanonical(sheet, fx.allShadow));
540:         if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
541:       }
542:       if (fx.targetFatigue) {
543:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
544:         if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[command.targetCharacterId] = Math.max(0, Number(pendingFatigue[command.targetCharacterId]) || 0) + fx.targetFatigue;
545:       }
546:       nextState.journey = { ...nextState.journey, travelFatigueByHero: pendingFatigue };
547:       if (fx.targetShadow) updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, fx.targetShadow));
548:       if (fx.woundTarget) updateHero(command.targetCharacterId, sheet => applyHeroWound(sheet).sheet);
549:       if (fx.dayAdjustment > 0) {
550:         const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
551:         nextState = simulated.master; nextNpcs = simulated.npcs;
552:       }
553:       break;
554:     }
555:     case 'set_journey_danger':
556:       nextState = setDangerousArea(nextState, command.danger);
557:       break;
558:     case 'continue_journey_stage':
559:       nextState = continueJourneyStage(nextState);
560:       break;
561:     case 'complete_journey': {
562:       if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
563:       for (const characterId of partyIds) {
564:         const result = command.endRolls?.[characterId];
565:         const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
566:         const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
567:         const remaining = Math.max(0, accumulated - reduction);
568:         if (remaining > 0) updateHero(characterId, sheet => adjustFatigue(sheet, remaining));
569:       }
570:       nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {} } });
571:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
572:       break;
573:     }
574:     case 'start_council':
575:       nextState = startCouncil(nextState, command.objective, command.tolerance, command.participants.filter(id => partyIds.has(id)), command.npcIds);
576:       nextState.scene = modeScene(nextState, 'council', 'Conselho', command.objective, command.participants.filter(id => partyIds.has(id)), command.npcIds);
577:       break;
578:     case 'record_council_result': nextState = recordCouncilResult(nextState, command.success, command.successIcons || 0, command.introduction === true); break;
579:     case 'resolve_council':
580:       nextState = resolveCouncil(nextState, command.outcome);
581:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
582:       break;
583:     case 'start_combat': {
584:       const heroActorIds = command.actorIds.filter(id => partyIds.has(id));
585:       nextState = startCombat(nextState, nextCharacters, heroActorIds);
586:       const requestedNpcIds = command.actorIds.filter(id => !partyIds.has(id));
587:       const sceneNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), ...requestedNpcIds]));
588:       sceneNpcIds.forEach((npcId, index) => {
589:         const npc = nextNpcs.find(item => item.id === npcId);
590:         if (!npc) return;
591:         const preparedActor = nextState.combat.actors.find(actor => actor.actorId === npc.id);
592:         nextState = upsertCombatActor(nextState, {
593:           actorId: npc.id,
594:           actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
595:           engagedWith: [],
596:           endurance: 10,
597:           enduranceMax: 10,
598:           hate: 2,
599:           wounded: false,
600:           defeated: false,
601:           initiative: Math.max(1, 10 - index),
602:           tokenId: npc.tokenId || null,
603:           position: preparedActor?.position || { x: 0.18 + (index % 3) * 0.32, y: 0.20 + Math.floor(index / 3) * 0.13 },
604:           quickSheet: { name: npc.name, role: npc.role, culture: npc.culture || npc.peoples, personality: npc.personality, attitude: npc.attitude, objectives: npc.objectives, fears: npc.fears, nextAction: npc.nextAction },
605:         });
606:       });
607:       nextState.scene = modeScene(nextState, 'combat', 'Combate', 'Superar o confronto', heroActorIds, sceneNpcIds);
608:       appendCampaignEvent('combat_started', { round: 1, heroes: heroActorIds.length, npcs: sceneNpcIds.length, location: nextState.position.locationName || nextCampaign.currentLocation });
609:       appendLivingActivity('world', 'Combate iniciado', `${heroActorIds.length} Herói(s) e ${sceneNpcIds.length} NPC(s)/adversário(s) entraram no confronto em ${nextState.position.locationName || nextCampaign.currentLocation}.`, nextState.combat.id);
610:       break;
611:     }
612:     case 'set_combat_stance':
613:       if (!partyIds.has(command.targetCharacterId)) throw new Error('Comando rejeitado: postura para personagem fora da Companhia.');
614:       {
\`\`\`

### lib/master/commands.ts — /case 'resolve_journey_event'/ — linha 528

\`\`\`text
500:             },
501:           ])),
502:           updatedAt: Date.now(),
503:         };
504:       }
505:       break;
506:     }
507:     case 'set_mode':
508:       assertModeTransitionAllowed(nextState, command.mode);
509:       nextState = { ...nextState, activeMode: command.mode, updatedAt: Date.now() };
510:       if (nextState.scene.status === 'idle') nextState.scene = { ...nextState.scene, kind: modeToScene(command.mode), updatedAt: Date.now() };
511:       break;
512:     case 'start_journey': {
513:       const roles = command.roles || nextState.company.journeyRoles;
514:       const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
515:       if (roleErrors.length) throw new Error('Jornada não pode começar: ' + roleErrors.join(' '));
516:       const route = (command.routeHexIds?.length ? command.routeHexIds : nextState.journey.plannedRouteHexIds || []).filter(Boolean);
517:       if (!route.length) throw new Error('Jornada não pode começar sem rota planejada.');
518:       nextState = startJourney(nextState, command.destinationHexId, roles, route, Boolean(command.mounted), Boolean(command.forcedMarch));
519:       nextState.scene = modeScene(nextState, 'journey', 'Jornada', 'Alcançar o destino em segurança', Array.from(partyIds), nextCampaign.presentNpcIds || []);
520:       break;
521:     }
522:     case 'resolve_journey_march':
523:       nextState = resolveJourneyMarch(nextState, command.success, command.successIcons);
524:       break;
525:     case 'generate_journey_event':
526:       nextState = generateJourneyEvent(nextState, command.region, command.targetDie, command.featResults, command.terrain);
527:       break;
528:     case 'resolve_journey_event': {
529:       const resolved = resolveJourneyEvent(nextState, command.success, command.successIcons);
530:       nextState = resolved.state;
531:       const fx = resolved.effects;
532:       const pendingFatigue = { ...(nextState.journey.travelFatigueByHero || {}) };
533:       for (const characterId of partyIds) {
534:         const hero = nextCharacters.find(character => character.id === characterId);
535:         if (fx.allFatigue && hero && !rangerIgnoresJourneyFatigue(hero.sheet)) {
536:           const cramReduction = /\bcram\b/i.test(hero.sheet.other.virtues || '') ? 1 : 0;
537:           pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);
538:         }
539:         if (fx.allShadow) updateHero(characterId, sheet => adjustShadowCanonical(sheet, fx.allShadow));
540:         if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
541:       }
542:       if (fx.targetFatigue) {
543:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
544:         if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[command.targetCharacterId] = Math.max(0, Number(pendingFatigue[command.targetCharacterId]) || 0) + fx.targetFatigue;
545:       }
546:       nextState.journey = { ...nextState.journey, travelFatigueByHero: pendingFatigue };
547:       if (fx.targetShadow) updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, fx.targetShadow));
548:       if (fx.woundTarget) updateHero(command.targetCharacterId, sheet => applyHeroWound(sheet).sheet);
549:       if (fx.dayAdjustment > 0) {
550:         const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
551:         nextState = simulated.master; nextNpcs = simulated.npcs;
552:       }
553:       break;
554:     }
555:     case 'set_journey_danger':
556:       nextState = setDangerousArea(nextState, command.danger);
557:       break;
558:     case 'continue_journey_stage':
559:       nextState = continueJourneyStage(nextState);
560:       break;
561:     case 'complete_journey': {
562:       if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
563:       for (const characterId of partyIds) {
564:         const result = command.endRolls?.[characterId];
565:         const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
566:         const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
567:         const remaining = Math.max(0, accumulated - reduction);
568:         if (remaining > 0) updateHero(characterId, sheet => adjustFatigue(sheet, remaining));
569:       }
570:       nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {} } });
571:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
572:       break;
573:     }
574:     case 'start_council':
575:       nextState = startCouncil(nextState, command.objective, command.tolerance, command.participants.filter(id => partyIds.has(id)), command.npcIds);
576:       nextState.scene = modeScene(nextState, 'council', 'Conselho', command.objective, command.participants.filter(id => partyIds.has(id)), command.npcIds);
577:       break;
578:     case 'record_council_result': nextState = recordCouncilResult(nextState, command.success, command.successIcons || 0, command.introduction === true); break;
579:     case 'resolve_council':
580:       nextState = resolveCouncil(nextState, command.outcome);
581:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
582:       break;
583:     case 'start_combat': {
584:       const heroActorIds = command.actorIds.filter(id => partyIds.has(id));
585:       nextState = startCombat(nextState, nextCharacters, heroActorIds);
586:       const requestedNpcIds = command.actorIds.filter(id => !partyIds.has(id));
587:       const sceneNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), ...requestedNpcIds]));
588:       sceneNpcIds.forEach((npcId, index) => {
589:         const npc = nextNpcs.find(item => item.id === npcId);
590:         if (!npc) return;
591:         const preparedActor = nextState.combat.actors.find(actor => actor.actorId === npc.id);
592:         nextState = upsertCombatActor(nextState, {
593:           actorId: npc.id,
594:           actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
595:           engagedWith: [],
596:           endurance: 10,
597:           enduranceMax: 10,
598:           hate: 2,
599:           wounded: false,
600:           defeated: false,
601:           initiative: Math.max(1, 10 - index),
602:           tokenId: npc.tokenId || null,
603:           position: preparedActor?.position || { x: 0.18 + (index % 3) * 0.32, y: 0.20 + Math.floor(index / 3) * 0.13 },
604:           quickSheet: { name: npc.name, role: npc.role, culture: npc.culture || npc.peoples, personality: npc.personality, attitude: npc.attitude, objectives: npc.objectives, fears: npc.fears, nextAction: npc.nextAction },
605:         });
606:       });
607:       nextState.scene = modeScene(nextState, 'combat', 'Combate', 'Superar o confronto', heroActorIds, sceneNpcIds);
608:       appendCampaignEvent('combat_started', { round: 1, heroes: heroActorIds.length, npcs: sceneNpcIds.length, location: nextState.position.locationName || nextCampaign.currentLocation });
609:       appendLivingActivity('world', 'Combate iniciado', `${heroActorIds.length} Herói(s) e ${sceneNpcIds.length} NPC(s)/adversário(s) entraram no confronto em ${nextState.position.locationName || nextCampaign.currentLocation}.`, nextState.combat.id);
610:       break;
611:     }
612:     case 'set_combat_stance':
613:       if (!partyIds.has(command.targetCharacterId)) throw new Error('Comando rejeitado: postura para personagem fora da Companhia.');
614:       {
615:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
616:         const smallFolk = Boolean(hero && /povo pequeno/i.test(hero.sheet.other.virtues || ''));
617:         nextState = setStance(nextState, command.targetCharacterId, command.stance, smallFolk);
\`\`\`

### lib/master/commands.ts — /case 'continue_journey_stage'/ — linha 558

\`\`\`text
530:       nextState = resolved.state;
531:       const fx = resolved.effects;
532:       const pendingFatigue = { ...(nextState.journey.travelFatigueByHero || {}) };
533:       for (const characterId of partyIds) {
534:         const hero = nextCharacters.find(character => character.id === characterId);
535:         if (fx.allFatigue && hero && !rangerIgnoresJourneyFatigue(hero.sheet)) {
536:           const cramReduction = /\bcram\b/i.test(hero.sheet.other.virtues || '') ? 1 : 0;
537:           pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);
538:         }
539:         if (fx.allShadow) updateHero(characterId, sheet => adjustShadowCanonical(sheet, fx.allShadow));
540:         if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
541:       }
542:       if (fx.targetFatigue) {
543:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
544:         if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[command.targetCharacterId] = Math.max(0, Number(pendingFatigue[command.targetCharacterId]) || 0) + fx.targetFatigue;
545:       }
546:       nextState.journey = { ...nextState.journey, travelFatigueByHero: pendingFatigue };
547:       if (fx.targetShadow) updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, fx.targetShadow));
548:       if (fx.woundTarget) updateHero(command.targetCharacterId, sheet => applyHeroWound(sheet).sheet);
549:       if (fx.dayAdjustment > 0) {
550:         const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
551:         nextState = simulated.master; nextNpcs = simulated.npcs;
552:       }
553:       break;
554:     }
555:     case 'set_journey_danger':
556:       nextState = setDangerousArea(nextState, command.danger);
557:       break;
558:     case 'continue_journey_stage':
559:       nextState = continueJourneyStage(nextState);
560:       break;
561:     case 'complete_journey': {
562:       if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
563:       for (const characterId of partyIds) {
564:         const result = command.endRolls?.[characterId];
565:         const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
566:         const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
567:         const remaining = Math.max(0, accumulated - reduction);
568:         if (remaining > 0) updateHero(characterId, sheet => adjustFatigue(sheet, remaining));
569:       }
570:       nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {} } });
571:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
572:       break;
573:     }
574:     case 'start_council':
575:       nextState = startCouncil(nextState, command.objective, command.tolerance, command.participants.filter(id => partyIds.has(id)), command.npcIds);
576:       nextState.scene = modeScene(nextState, 'council', 'Conselho', command.objective, command.participants.filter(id => partyIds.has(id)), command.npcIds);
577:       break;
578:     case 'record_council_result': nextState = recordCouncilResult(nextState, command.success, command.successIcons || 0, command.introduction === true); break;
579:     case 'resolve_council':
580:       nextState = resolveCouncil(nextState, command.outcome);
581:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
582:       break;
583:     case 'start_combat': {
584:       const heroActorIds = command.actorIds.filter(id => partyIds.has(id));
585:       nextState = startCombat(nextState, nextCharacters, heroActorIds);
586:       const requestedNpcIds = command.actorIds.filter(id => !partyIds.has(id));
587:       const sceneNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), ...requestedNpcIds]));
588:       sceneNpcIds.forEach((npcId, index) => {
589:         const npc = nextNpcs.find(item => item.id === npcId);
590:         if (!npc) return;
591:         const preparedActor = nextState.combat.actors.find(actor => actor.actorId === npc.id);
592:         nextState = upsertCombatActor(nextState, {
593:           actorId: npc.id,
594:           actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
595:           engagedWith: [],
596:           endurance: 10,
597:           enduranceMax: 10,
598:           hate: 2,
599:           wounded: false,
600:           defeated: false,
601:           initiative: Math.max(1, 10 - index),
602:           tokenId: npc.tokenId || null,
603:           position: preparedActor?.position || { x: 0.18 + (index % 3) * 0.32, y: 0.20 + Math.floor(index / 3) * 0.13 },
604:           quickSheet: { name: npc.name, role: npc.role, culture: npc.culture || npc.peoples, personality: npc.personality, attitude: npc.attitude, objectives: npc.objectives, fears: npc.fears, nextAction: npc.nextAction },
605:         });
606:       });
607:       nextState.scene = modeScene(nextState, 'combat', 'Combate', 'Superar o confronto', heroActorIds, sceneNpcIds);
608:       appendCampaignEvent('combat_started', { round: 1, heroes: heroActorIds.length, npcs: sceneNpcIds.length, location: nextState.position.locationName || nextCampaign.currentLocation });
609:       appendLivingActivity('world', 'Combate iniciado', `${heroActorIds.length} Herói(s) e ${sceneNpcIds.length} NPC(s)/adversário(s) entraram no confronto em ${nextState.position.locationName || nextCampaign.currentLocation}.`, nextState.combat.id);
610:       break;
611:     }
612:     case 'set_combat_stance':
613:       if (!partyIds.has(command.targetCharacterId)) throw new Error('Comando rejeitado: postura para personagem fora da Companhia.');
614:       {
615:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
616:         const smallFolk = Boolean(hero && /povo pequeno/i.test(hero.sheet.other.virtues || ''));
617:         nextState = setStance(nextState, command.targetCharacterId, command.stance, smallFolk);
618:       }
619:       break;
620:     case 'next_combat_round': nextState = nextRound(nextState); break;
621:     case 'end_combat': {
622:       const defeated = nextState.combat.actors.filter(actor => actor.defeated).length;
623:       const finalRound = Math.max(1, nextState.combat.round || 1);
624:       for (const characterId of partyIds) updateHero(characterId, sheet => clearModerateWound(sheet));
625:       nextState = endCombat(nextState);
626:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
627:       appendCampaignEvent('combat_ended', { round: finalRound, defeated, location: nextState.position.locationName || nextCampaign.currentLocation });
628:       appendLivingActivity('world', 'Combate encerrado', `Confronto encerrado na rodada ${finalRound}; ${defeated} participante(s) derrotado(s).`, nextState.combat.id);
629:       break;
630:     }
631:     case 'upsert_combat_actor':
632:       nextState = upsertCombatActor(nextState, command.actor);
633:       break;
634:     case 'move_combat_actor':
635:       nextState = moveCombatActor(nextState, command.actorId, command.x, command.y);
636:       break;
637:     case 'record_combat_narrative': {
638:       const text = safeText(command.text, 3000);
639:       if (!text) throw new Error('Comando rejeitado: narrativa de combate vazia.');
640:       nextState = recordCombatNarrative(nextState, text);
641:       appendCampaignEvent('custom', { category: 'combat_narrative', round: Math.max(1, nextState.combat.round || 1), text });
642:       appendLivingActivity('world', `Combate — rodada ${Math.max(1, nextState.combat.round || 1)}`, text, nextState.combat.id);
643:       break;
644:     }
645:     case 'record_combat_roll':
646:       nextState = recordCombatRoll(nextState, command.roll);
647:       break;
\`\`\`

### lib/master/commands.ts — /case 'complete_journey'/ — linha 561

\`\`\`text
533:       for (const characterId of partyIds) {
534:         const hero = nextCharacters.find(character => character.id === characterId);
535:         if (fx.allFatigue && hero && !rangerIgnoresJourneyFatigue(hero.sheet)) {
536:           const cramReduction = /\bcram\b/i.test(hero.sheet.other.virtues || '') ? 1 : 0;
537:           pendingFatigue[characterId] = Math.max(0, Number(pendingFatigue[characterId]) || 0) + Math.max(0, fx.allFatigue - cramReduction);
538:         }
539:         if (fx.allShadow) updateHero(characterId, sheet => adjustShadowCanonical(sheet, fx.allShadow));
540:         if (fx.allHope) updateHero(characterId, sheet => adjustHope(sheet, fx.allHope));
541:       }
542:       if (fx.targetFatigue) {
543:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
544:         if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pendingFatigue[command.targetCharacterId] = Math.max(0, Number(pendingFatigue[command.targetCharacterId]) || 0) + fx.targetFatigue;
545:       }
546:       nextState.journey = { ...nextState.journey, travelFatigueByHero: pendingFatigue };
547:       if (fx.targetShadow) updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, fx.targetShadow));
548:       if (fx.woundTarget) updateHero(command.targetCharacterId, sheet => applyHeroWound(sheet).sheet);
549:       if (fx.dayAdjustment > 0) {
550:         const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, fx.dayAdjustment * 1440);
551:         nextState = simulated.master; nextNpcs = simulated.npcs;
552:       }
553:       break;
554:     }
555:     case 'set_journey_danger':
556:       nextState = setDangerousArea(nextState, command.danger);
557:       break;
558:     case 'continue_journey_stage':
559:       nextState = continueJourneyStage(nextState);
560:       break;
561:     case 'complete_journey': {
562:       if (!nextState.journey.awaitingEndRoll && nextState.journey.status !== 'paused') throw new Error('A Companhia ainda não alcançou o final da Jornada.');
563:       for (const characterId of partyIds) {
564:         const result = command.endRolls?.[characterId];
565:         const accumulated = Math.max(0, Number(nextState.journey.travelFatigueByHero?.[characterId]) || 0);
566:         const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
567:         const remaining = Math.max(0, accumulated - reduction);
568:         if (remaining > 0) updateHero(characterId, sheet => adjustFatigue(sheet, remaining));
569:       }
570:       nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {} } });
571:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
572:       break;
573:     }
574:     case 'start_council':
575:       nextState = startCouncil(nextState, command.objective, command.tolerance, command.participants.filter(id => partyIds.has(id)), command.npcIds);
576:       nextState.scene = modeScene(nextState, 'council', 'Conselho', command.objective, command.participants.filter(id => partyIds.has(id)), command.npcIds);
577:       break;
578:     case 'record_council_result': nextState = recordCouncilResult(nextState, command.success, command.successIcons || 0, command.introduction === true); break;
579:     case 'resolve_council':
580:       nextState = resolveCouncil(nextState, command.outcome);
581:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
582:       break;
583:     case 'start_combat': {
584:       const heroActorIds = command.actorIds.filter(id => partyIds.has(id));
585:       nextState = startCombat(nextState, nextCharacters, heroActorIds);
586:       const requestedNpcIds = command.actorIds.filter(id => !partyIds.has(id));
587:       const sceneNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), ...requestedNpcIds]));
588:       sceneNpcIds.forEach((npcId, index) => {
589:         const npc = nextNpcs.find(item => item.id === npcId);
590:         if (!npc) return;
591:         const preparedActor = nextState.combat.actors.find(actor => actor.actorId === npc.id);
592:         nextState = upsertCombatActor(nextState, {
593:           actorId: npc.id,
594:           actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
595:           engagedWith: [],
596:           endurance: 10,
597:           enduranceMax: 10,
598:           hate: 2,
599:           wounded: false,
600:           defeated: false,
601:           initiative: Math.max(1, 10 - index),
602:           tokenId: npc.tokenId || null,
603:           position: preparedActor?.position || { x: 0.18 + (index % 3) * 0.32, y: 0.20 + Math.floor(index / 3) * 0.13 },
604:           quickSheet: { name: npc.name, role: npc.role, culture: npc.culture || npc.peoples, personality: npc.personality, attitude: npc.attitude, objectives: npc.objectives, fears: npc.fears, nextAction: npc.nextAction },
605:         });
606:       });
607:       nextState.scene = modeScene(nextState, 'combat', 'Combate', 'Superar o confronto', heroActorIds, sceneNpcIds);
608:       appendCampaignEvent('combat_started', { round: 1, heroes: heroActorIds.length, npcs: sceneNpcIds.length, location: nextState.position.locationName || nextCampaign.currentLocation });
609:       appendLivingActivity('world', 'Combate iniciado', `${heroActorIds.length} Herói(s) e ${sceneNpcIds.length} NPC(s)/adversário(s) entraram no confronto em ${nextState.position.locationName || nextCampaign.currentLocation}.`, nextState.combat.id);
610:       break;
611:     }
612:     case 'set_combat_stance':
613:       if (!partyIds.has(command.targetCharacterId)) throw new Error('Comando rejeitado: postura para personagem fora da Companhia.');
614:       {
615:         const hero = nextCharacters.find(character => character.id === command.targetCharacterId);
616:         const smallFolk = Boolean(hero && /povo pequeno/i.test(hero.sheet.other.virtues || ''));
617:         nextState = setStance(nextState, command.targetCharacterId, command.stance, smallFolk);
618:       }
619:       break;
620:     case 'next_combat_round': nextState = nextRound(nextState); break;
621:     case 'end_combat': {
622:       const defeated = nextState.combat.actors.filter(actor => actor.defeated).length;
623:       const finalRound = Math.max(1, nextState.combat.round || 1);
624:       for (const characterId of partyIds) updateHero(characterId, sheet => clearModerateWound(sheet));
625:       nextState = endCombat(nextState);
626:       nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
627:       appendCampaignEvent('combat_ended', { round: finalRound, defeated, location: nextState.position.locationName || nextCampaign.currentLocation });
628:       appendLivingActivity('world', 'Combate encerrado', `Confronto encerrado na rodada ${finalRound}; ${defeated} participante(s) derrotado(s).`, nextState.combat.id);
629:       break;
630:     }
631:     case 'upsert_combat_actor':
632:       nextState = upsertCombatActor(nextState, command.actor);
633:       break;
634:     case 'move_combat_actor':
635:       nextState = moveCombatActor(nextState, command.actorId, command.x, command.y);
636:       break;
637:     case 'record_combat_narrative': {
638:       const text = safeText(command.text, 3000);
639:       if (!text) throw new Error('Comando rejeitado: narrativa de combate vazia.');
640:       nextState = recordCombatNarrative(nextState, text);
641:       appendCampaignEvent('custom', { category: 'combat_narrative', round: Math.max(1, nextState.combat.round || 1), text });
642:       appendLivingActivity('world', `Combate — rodada ${Math.max(1, nextState.combat.round || 1)}`, text, nextState.combat.id);
643:       break;
644:     }
645:     case 'record_combat_roll':
646:       nextState = recordCombatRoll(nextState, command.roll);
647:       break;
648:     case 'set_combat_background':
649:       nextState = setCombatBackground(nextState, command.imageData);
650:       break;
\`\`\`

### lib/master/commands.ts — /case 'move_party'/ — linha 169

\`\`\`text
141:     case 'adjust_supplies':
142:       updateHero(command.targetCharacterId, sheet => {
143:         const next = structuredClone(sheet);
144:         next.attributes.supplies = Math.max(0, Math.min(100, next.attributes.supplies + safeDelta(command.amount)));
145:         return recomputeConditions(next);
146:       });
147:       break;
148:     case 'resolve_shadow_gain': {
149:       updateHero(command.targetCharacterId, sheet => resolveShadowGain(sheet, command.amount, command.source, { success: command.testSuccess, successIcons: command.successIcons }).sheet);
150:       break;
151:     }
152:     case 'firm_will':
153:       updateHero(command.targetCharacterId, sheet => {
154:         const result = firmWill(sheet); if (!result.allowed) throw new Error('Firmar Vontade não está disponível neste estado de Sombra.'); return result.sheet;
155:       });
156:       break;
157:     case 'bout_of_madness':
158:       updateHero(command.targetCharacterId, sheet => {
159:         const result = resolveBoutOfMadness(sheet); if (!result.allowed) throw new Error('Ataque de Loucura só ocorre quando Sombra atinge Esperança máxima.'); return result.sheet;
160:       });
161:       break;
162:     case 'set_condition':
163:       updateHero(command.targetCharacterId, sheet => {
164:         const next = structuredClone(sheet);
165:         next.attributes[command.condition] = Boolean(command.value);
166:         return next;
167:       });
168:       break;
169:     case 'move_party': {
170:       if (!command.toHexId) throw new Error('Comando rejeitado: hex de destino ausente.');
171:       const beforeMinutes = Math.max(0, Number(nextState.worldClock.elapsedMinutes) || 0);
172:       const wasJourney = nextState.journey.status !== 'idle' && nextState.journey.status !== 'completed';
173:       nextState = recordJourneyMove(nextState, command.toHexId, command.locationName || nextState.position.locationName, command.distanceKm, command.terrain);
174:       const elapsed = Math.max(0, Number(nextState.worldClock.elapsedMinutes) || 0) - beforeMinutes;
175:       if (elapsed > 0) {
176:         const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, elapsed);
177:         nextState = simulated.master; nextNpcs = simulated.npcs;
178:       }
179:       if (wasJourney && nextState.journey.forcedMarch && elapsed > 0) {
180:         const forcedDays = Math.max(1, Math.ceil(elapsed / 1440));
181:         const pending = { ...(nextState.journey.travelFatigueByHero || {}) };
182:         for (const characterId of partyIds) {
183:           const hero = nextCharacters.find(character => character.id === characterId);
184:           if (hero && !rangerIgnoresJourneyFatigue(hero.sheet)) pending[characterId] = Math.max(0, Number(pending[characterId]) || 0) + forcedDays;
185:         }
186:         nextState.journey = { ...nextState.journey, travelFatigueByHero: pending };
187:       }
188:       nextCampaign.currentLocation = nextState.position.locationName;
189:       nextState.scene = { ...nextState.scene, locationName: nextState.position.locationName, updatedAt: Date.now() };
190:       appendLivingActivity('journey', 'Trecho de Jornada', `A Companhia alcançou ${nextState.position.locationName}; duração canônica acumulada ${nextState.journey.day || 0} dia(s).`, command.toHexId);
191:       break;
192:     }
193:     case 'set_present_npcs': {
194:       const valid = new Set(nextNpcs.map(npc => npc.id));
195:       nextCampaign.presentNpcIds = Array.from(new Set(command.npcIds.filter(id => valid.has(id))));
196:       nextNpcs = nextNpcs.map(npc => ({ ...npc, presentInScene: nextCampaign.presentNpcIds.includes(npc.id), updatedAt: Date.now() }));
197:       nextState.scene = { ...nextState.scene, participantNpcIds: nextCampaign.presentNpcIds, updatedAt: Date.now() };
198:       break;
199:     }
200:     case 'upsert_npc': {
201:       const draft = command.npc;
202:       if (!draft?.name?.trim()) throw new Error('Comando rejeitado: NPC sem nome.');
203:       const id = resolveNpcId(draft, nextNpcs);
204:       const index = nextNpcs.findIndex(npc => npc.id === id);
205:       const previous = index >= 0 ? nextNpcs[index] : null;
206:       const value = normalizeNpcDraft(draft, previous, nextState.position.locationName, id);
207:       if (index >= 0) nextNpcs[index] = value; else nextNpcs.push(value);
208:       if (value.presentInScene) {
209:         nextCampaign.presentNpcIds = Array.from(new Set([...(nextCampaign.presentNpcIds || []), id]));
210:         nextState.scene = { ...nextState.scene, participantNpcIds: Array.from(new Set([...nextState.scene.participantNpcIds, id])), updatedAt: Date.now() };
211:       }
212:       const actorIndex = nextState.npcActors.findIndex(actor => actor.npcId === id);
213:       const actor = {
214:         npcId: id,
215:         emotionalState: value.emotionalState || value.state || 'neutro',
216:         nextAction: value.nextAction || '',
217:         location: value.location,
218:         faction: value.faction,
219:         beliefs: actorIndex >= 0 ? nextState.npcActors[actorIndex].beliefs : [],
220:         suspicions: actorIndex >= 0 ? nextState.npcActors[actorIndex].suspicions : [],
221:         allies: actorIndex >= 0 ? nextState.npcActors[actorIndex].allies : [],
222:         enemies: actorIndex >= 0 ? nextState.npcActors[actorIndex].enemies : [],
223:         relationshipByHero: value.relationshipByHero || (actorIndex >= 0 ? nextState.npcActors[actorIndex].relationshipByHero : {}),
224:         lastUpdatedAt: Date.now(),
225:       };
226:       if (actorIndex >= 0) nextState.npcActors[actorIndex] = actor; else nextState.npcActors.push(actor);
227:       break;
228:     }
229:     case 'update_npc_actor': {
230:       const npcIndex = nextNpcs.findIndex(npc => npc.id === command.npcId);
231:       if (npcIndex < 0) throw new Error('Comando rejeitado: NPC não encontrado.');
232:       const actorIndex = nextState.npcActors.findIndex(actor => actor.npcId === command.npcId);
233:       const currentActor = actorIndex >= 0 ? nextState.npcActors[actorIndex] : {
234:         npcId: command.npcId, emotionalState: nextNpcs[npcIndex].emotionalState || 'neutro', nextAction: '', location: nextNpcs[npcIndex].location,
235:         faction: nextNpcs[npcIndex].faction, beliefs: [], suspicions: [], allies: [], enemies: [], relationshipByHero: {}, lastUpdatedAt: Date.now(),
236:       };
237:       const actor = {
238:         ...currentActor,
239:         emotionalState: command.emotionalState ?? currentActor.emotionalState,
240:         nextAction: command.nextAction ?? currentActor.nextAction,
241:         beliefs: command.beliefs ?? currentActor.beliefs,
242:         suspicions: command.suspicions ?? currentActor.suspicions,
243:         location: command.location ?? currentActor.location,
244:         lastUpdatedAt: Date.now(),
245:       };
246:       if (actorIndex >= 0) nextState.npcActors[actorIndex] = actor; else nextState.npcActors.push(actor);
247:       nextNpcs[npcIndex] = { ...nextNpcs[npcIndex], emotionalState: actor.emotionalState, nextAction: actor.nextAction, location: actor.location, updatedAt: Date.now() };
248:       break;
249:     }
250:     case 'open_scene': {
251:       assertSceneTransitionAllowed(nextState.scene, command.kind);
252:       if (!sceneCompatibleWithMode(command.kind, nextState.activeMode)) {
253:         throw new Error(`Comando rejeitado: a cena ${command.kind} não pertence ao modo ${nextState.activeMode}.`);
254:       }
255:       const heroIds = (command.participantHeroIds?.length ? command.participantHeroIds : Array.from(partyIds)).filter(id => partyIds.has(id));
256:       if (!heroIds.length) throw new Error('Comando rejeitado: a cena não possui Heróis-jogadores válidos.');
257:       const validNpcs = new Set(nextNpcs.map(npc => npc.id));
258:       const npcIds = (command.participantNpcIds?.length ? command.participantNpcIds : nextCampaign.presentNpcIds || []).filter(id => validNpcs.has(id));
\`\`\`

### lib/master/commands.ts — /presentNpcIds/ — linha 58

\`\`\`text
30:   npcs: Npc[],
31:   commands: MasterCommand[],
32: ): CommandApplicationResult {
33:   let nextCampaign = structuredClone(campaign);
34:   let nextCharacters = structuredClone(characters);
35:   let nextNpcs = structuredClone(npcs);
36:   let masterState = normalizeMasterState(nextCampaign);
37:   const audits: CommandAuditEntry[] = [];
38:   const rejected: Array<{ command: MasterCommand; reason: string }> = [];
39:   const partyIds = new Set(nextCampaign.partyCharacterIds || []);
40: 
41:   for (const command of commands.slice(0, 50)) {
42:     const id = `cmd_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
43:     try {
44:       const before = commandTargetSnapshot(command, nextCampaign, nextCharacters, nextNpcs, masterState);
45:       ({ campaign: nextCampaign, characters: nextCharacters, npcs: nextNpcs, masterState } = applyOne(command, nextCampaign, nextCharacters, nextNpcs, masterState, partyIds));
46:       const after = commandTargetSnapshot(command, nextCampaign, nextCharacters, nextNpcs, masterState);
47:       audits.push({ id, campaignId: campaign.id, timestamp: Date.now(), command, status: 'applied', reason: command.reason, before, after });
48:     } catch (error) {
49:       const reason = error instanceof Error ? error.message : String(error);
50:       rejected.push({ command, reason });
51:       audits.push({ id, campaignId: campaign.id, timestamp: Date.now(), command, status: 'rejected', reason });
52:     }
53:   }
54: 
55:   nextCampaign.masterState = { ...masterState, updatedAt: Date.now() };
56:   nextCampaign.currentLocation = masterState.position.locationName || nextCampaign.currentLocation;
57:   const validNpcIds = new Set(nextNpcs.map(npc => npc.id));
58:   nextCampaign.presentNpcIds = Array.from(new Set((nextCampaign.presentNpcIds || []).filter(id => validNpcIds.has(id))));
59:   nextNpcs = nextNpcs.map(npc => ({ ...npc, presentInScene: nextCampaign.presentNpcIds.includes(npc.id) }));
60:   nextCampaign.updatedAt = Date.now();
61:   return { campaign: nextCampaign, characters: nextCharacters, npcs: nextNpcs, audits, rejected };
62: }
63: 
64: function applyOne(
65:   command: MasterCommand,
66:   campaign: CampaignState,
67:   characters: Character[],
68:   npcs: Npc[],
69:   state: MasterState,
70:   partyIds: Set<string>,
71: ): { campaign: CampaignState; characters: Character[]; npcs: Npc[]; masterState: MasterState } {
72:   const nextCampaign = structuredClone(campaign);
73:   let nextCharacters = structuredClone(characters);
74:   let nextNpcs = structuredClone(npcs);
75:   let nextState = structuredClone(state);
76: 
77:   const appendCampaignEvent = (type: 'combat_started' | 'combat_ended' | 'custom', data: Record<string, string | number | boolean | null>) => {
78:     const event = { id: `event_${type}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, type, timestamp: Date.now(), sceneId: nextState.scene.id || undefined, data };
79:     nextCampaign.events = [...(nextCampaign.events || []), event].slice(-1000);
80:   };
81: 
82:   const appendLivingActivity = (kind: 'npc' | 'organization' | 'journey' | 'project' | 'threat' | 'fellowship' | 'rest' | 'world', title: string, detail: string, sourceId?: string) => {
83:     nextState.livingWorld = {
84:       ...nextState.livingWorld,
85:       activityLog: [...(nextState.livingWorld.activityLog || []), { id: `activity_${kind}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, kind, title, detail, day: nextState.worldClock.day, createdAt: Date.now(), sourceId }].slice(-300),
86:       updatedAt: Date.now(),
87:     };
88:   };
89: 
90:   const updateHero = (characterId: string, updater: (sheet: CharacterSheet) => CharacterSheet) => {
91:     if (!partyIds.has(characterId)) throw new Error('Comando rejeitado: o alvo não pertence à Companhia em jogo.');
92:     let found = false;
93:     nextCharacters = nextCharacters.map(character => {
94:       if (character.id !== characterId) return character;
95:       found = true;
96:       const sheet = normalizeEquipmentMechanics(updater(character.sheet));
97:       return { ...character, name: sheet.identity.name || character.name, culture: sheet.identity.culture || character.culture, vocation: sheet.identity.vocation || character.vocation, sheet, updatedAt: Date.now() };
98:     });
99:     if (!found) throw new Error('Comando rejeitado: personagem não encontrado.');
100:     const updatedHero = nextCharacters.find(character => character.id === characterId);
101:     if (updatedHero && nextState.combat.actors.some(actor => actor.actorId === characterId)) {
102:       const attributes = updatedHero.sheet.attributes;
103:       nextState.combat = {
104:         ...nextState.combat,
105:         actors: nextState.combat.actors.map(actor => actor.actorId === characterId ? {
106:           ...actor,
107:           endurance: attributes.enduranceCurrent,
108:           enduranceMax: attributes.enduranceMax,
109:           wounded: attributes.wounded,
110:           defeated: attributes.enduranceCurrent <= 0,
111:           hope: attributes.hopeCurrent,
112:           shadow: attributes.shadow,
113:           fatigue: attributes.fatigue,
114:           stateNotes: [attributes.weary ? 'Cansado' : '', attributes.miserable ? 'Arrasado' : '', attributes.wounded ? 'Ferido' : ''].filter(Boolean),
115:         } : actor),
116:         updatedAt: Date.now(),
117:       };
118:     }
119:   };
120: 
121:   switch (command.type) {
122:     case 'adjust_endurance': updateHero(command.targetCharacterId, sheet => adjustEndurance(sheet, safeDelta(command.amount))); break;
123:     case 'adjust_hope': updateHero(command.targetCharacterId, sheet => adjustHope(sheet, safeDelta(command.amount))); break;
124:     case 'spend_hope':
125:       updateHero(command.targetCharacterId, sheet => {
126:         const result = spendHope(sheet, command.attribute);
127:         if (!result.allowed) throw new Error('Comando rejeitado: o Herói não possui Esperança para gastar.');
128:         return result.sheet;
129:       });
130:       break;
131:     case 'spend_fellowship_for_hope': {
132:       const amount = Math.max(1, Math.min(5, Math.trunc(Number(command.amount) || 1)));
133:       const spent = spendFellowship(nextState, amount);
134:       if (!spent.spent) throw new Error('Comando rejeitado: Sociedade insuficiente.');
135:       nextState = spent.state;
136:       updateHero(command.targetCharacterId, sheet => adjustHope(sheet, amount));
137:       break;
138:     }
139:     case 'adjust_shadow': updateHero(command.targetCharacterId, sheet => adjustShadowCanonical(sheet, safeDelta(command.amount))); break;
140:     case 'adjust_fatigue': updateHero(command.targetCharacterId, sheet => adjustFatigue(sheet, safeDelta(command.amount))); break;
141:     case 'adjust_supplies':
142:       updateHero(command.targetCharacterId, sheet => {
143:         const next = structuredClone(sheet);
144:         next.attributes.supplies = Math.max(0, Math.min(100, next.attributes.supplies + safeDelta(command.amount)));
145:         return recomputeConditions(next);
146:       });
147:       break;
\`\`\`

### lib/rules/journey.ts — /validateJourneyRoles/ — linha 17

\`\`\`text
1: import type { MasterState, JourneyRole } from '../master/types';
2: import { advanceWorldClock } from '../master/worldClock';
3: 
4: export const JOURNEY_ROLE_SKILLS: Record<JourneyRole,string> = {
5:   guide:'Viagem', scout:'Exploração', hunter:'Caçada', lookout:'Vigilância',
6: };
7: export const JOURNEY_ROLE_LABELS: Record<JourneyRole,string> = {
8:   guide:'Guia', scout:'Batedor', hunter:'Caçador', lookout:'Vigia',
9: };
10: 
11: const DIFFICULT_TERRAINS=new Set(['hills','hill','forest','dense_forest','woods','wood','marsh','swamp','waste','ruins','mountains','mountain','water']);
12: function terrainKey(value?:string):string{return String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim().replace(/[ -]+/g,'_');}
13: export function journeyTravelDaysForTerrain(terrain?:string):number{return DIFFICULT_TERRAINS.has(terrainKey(terrain))?2:1;}
14: export function isDifficultJourneyTerrain(terrain?:string):boolean{return journeyTravelDaysForTerrain(terrain)>1;}
15: export function isRoadTerrain(terrain?:string):boolean{return /road|estrada|caminho/.test(terrainKey(terrain));}
16: 
17: export function validateJourneyRoles(roles:Partial<Record<JourneyRole,string>>,partyIds:string[]):string[]{
18:   const errors:string[]=[]; const party=new Set(partyIds); const required:JourneyRole[]=['guide','scout','hunter','lookout'];
19:   for(const role of required){
20:     const id=roles[role];
21:     if(!id) errors.push('A Companhia precisa definir '+JOURNEY_ROLE_LABELS[role]+' ('+JOURNEY_ROLE_SKILLS[role]+').');
22:     else if(!party.has(id)) errors.push(JOURNEY_ROLE_LABELS[role]+': personagem fora da Companhia.');
23:   }
24:   return errors;
25: }
26: 
27: export function journeyDurationFromSegments(
28:   segments:Array<{terrain?:string}>,
29:   mounted=false,
30:   forcedMarch=false,
31:   adjustment=0,
32: ):number{
33:   const hexes=segments.length;
34:   const difficult=segments.filter(segment=>isDifficultJourneyTerrain(segment.terrain)).length;
35:   let days=forcedMarch?Math.ceil(hexes/2)+difficult:hexes+difficult;
36:   if(mounted) days=Math.ceil(days/2);
37:   return Math.max(0,days+Math.trunc(Number(adjustment)||0));
38: }
39: 
40: export function startJourney(
41:   state:MasterState,
42:   destinationHexId:string|null,
43:   roles:Partial<Record<JourneyRole,string>>,
44:   routeHexIds:string[]=[],
45:   mounted=false,
46:   forcedMarch=false,
47: ):MasterState{
48:   const route=routeHexIds.filter(Boolean).slice();
49:   const stage=route.slice(0,20);
50:   return {
51:     ...state,activeMode:'journey',company:{...state.company,journeyRoles:{...roles}},
52:     journey:{
53:       status:'planned',originHexId:state.position.currentHexId,destinationHexId:destinationHexId||route.at(-1)||null,
54:       startedAt:Date.now(),day:0,segments:[],pendingEvent:null,
55:       plannedRouteHexIds:route,stageRouteHexIds:stage,stageNumber:1,marchHexesRemaining:0,marchRollCount:0,
56:       awaitingEventGeneration:false,awaitingEndRoll:false,durationAdjustmentDays:0,mounted,forcedMarch,dangerEventsRemaining:0,travelFatigueByHero:{},
57:     },
58:     updatedAt:Date.now(),
59:   };
60: }
61: 
62: export function resolveJourneyMarch(state:MasterState,success:boolean,successIcons=0):MasterState{
63:   if(!['planned','travelling','paused'].includes(state.journey.status)) throw new Error('Não há Jornada ativa para uma Jogada de Marcha.');
64:   if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
65:   if(state.journey.awaitingEndRoll) throw new Error('A Companhia já alcançou o fim desta etapa.');
66:   const remaining=(state.journey.plannedRouteHexIds||[]).length;
67:   if(remaining<=0) return {...state,journey:{...state.journey,status:'paused',awaitingEndRoll:true},updatedAt:Date.now()};
68:   const seasonalFailure=(state.worldClock.season==='autumn'||state.worldClock.season==='winter')?1:2;
69:   const distance=success?3+Math.max(0,Math.trunc(Number(successIcons)||0)):seasonalFailure;
70:   const bounded=Math.min(distance,remaining,20);
71:   return {
72:     ...state,
73:     journey:{...state.journey,status:'travelling',marchHexesRemaining:bounded,marchRollCount:Math.max(0,Number(state.journey.marchRollCount)||0)+1,marchWillReachDestination:distance>=remaining},
74:     updatedAt:Date.now(),
75:   };
76: }
77: 
78: export function recordJourneyMove(state:MasterState,toHexId:string,locationName:string,distanceKm?:number,terrain?:string):MasterState{
79:   const fromHexId=state.position.currentHexId; const now=Date.now(); const isTravel=Boolean(fromHexId&&fromHexId!==toHexId);
80:   const active=state.journey.status!=='idle'&&state.journey.status!=='completed';
81:   if(active&&isTravel&&Number(state.journey.marchHexesRemaining||0)<=0){
82:     throw new Error('Faça a Jogada de Marcha do Guia antes de avançar a Jornada.');
83:   }
84:   const previousSegments=state.journey.segments||[];
85:   const rawSegmentDays=isTravel?journeyTravelDaysForTerrain(terrain):0;
86:   const segment=isTravel?{
87:     id:'segment_'+now+'_'+Math.random().toString(36).slice(2,6),fromHexId,toHexId,distanceKm,terrain,
88:     travelDays:rawSegmentDays,startedAtWorldMinute:Math.max(0,Number(state.worldClock.elapsedMinutes)||0),startedAt:now,completedAt:now,
89:   }:null;
90:   const nextSegments=segment?[...previousSegments,segment]:previousSegments;
91:   let effectiveTotal=active?journeyDurationFromSegments(nextSegments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),Number(state.journey.durationAdjustmentDays)||0):rawSegmentDays;
92:   const priorTotal=active?Math.max(0,Number(state.journey.day)||0):0;
93:   const deltaDays=Math.max(0,effectiveTotal-priorTotal);
94:   const advanced=deltaDays>0?advanceWorldClock(state.worldClock,deltaDays*1440):{clock:state.worldClock};
95:   if(segment) segment.completedAtWorldMinute=Math.max(0,Number(advanced.clock.elapsedMinutes)||0);
96: 
97:   const planned=(state.journey.plannedRouteHexIds||[]).slice();
98:   if(planned[0]===toHexId) planned.shift();
99:   const marchRemaining=active&&isTravel?Math.max(0,Number(state.journey.marchHexesRemaining||0)-1):Number(state.journey.marchHexesRemaining||0);
100:   const stageCompleted=active&&nextSegments.length>0&&nextSegments.length%20===0&&planned.length>0;
101:   const reachedDestination=active&&planned.length===0;
102:   const needEvent=active&&isTravel&&marchRemaining===0&&!reachedDestination&&!stageCompleted;
103: 
104:   return {
105:     ...state,worldClock:advanced.clock,
106:     position:{currentHexId:toHexId,locationName,routeHexIds:[...state.position.routeHexIds,toHexId].filter((id,index,all)=>index===0||id!==all[index-1]),lastMovedAt:now},
\`\`\`

### lib/rules/journey.ts — /resolveJourneyMarch/ — linha 62

\`\`\`text
34:   const difficult=segments.filter(segment=>isDifficultJourneyTerrain(segment.terrain)).length;
35:   let days=forcedMarch?Math.ceil(hexes/2)+difficult:hexes+difficult;
36:   if(mounted) days=Math.ceil(days/2);
37:   return Math.max(0,days+Math.trunc(Number(adjustment)||0));
38: }
39: 
40: export function startJourney(
41:   state:MasterState,
42:   destinationHexId:string|null,
43:   roles:Partial<Record<JourneyRole,string>>,
44:   routeHexIds:string[]=[],
45:   mounted=false,
46:   forcedMarch=false,
47: ):MasterState{
48:   const route=routeHexIds.filter(Boolean).slice();
49:   const stage=route.slice(0,20);
50:   return {
51:     ...state,activeMode:'journey',company:{...state.company,journeyRoles:{...roles}},
52:     journey:{
53:       status:'planned',originHexId:state.position.currentHexId,destinationHexId:destinationHexId||route.at(-1)||null,
54:       startedAt:Date.now(),day:0,segments:[],pendingEvent:null,
55:       plannedRouteHexIds:route,stageRouteHexIds:stage,stageNumber:1,marchHexesRemaining:0,marchRollCount:0,
56:       awaitingEventGeneration:false,awaitingEndRoll:false,durationAdjustmentDays:0,mounted,forcedMarch,dangerEventsRemaining:0,travelFatigueByHero:{},
57:     },
58:     updatedAt:Date.now(),
59:   };
60: }
61: 
62: export function resolveJourneyMarch(state:MasterState,success:boolean,successIcons=0):MasterState{
63:   if(!['planned','travelling','paused'].includes(state.journey.status)) throw new Error('Não há Jornada ativa para uma Jogada de Marcha.');
64:   if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
65:   if(state.journey.awaitingEndRoll) throw new Error('A Companhia já alcançou o fim desta etapa.');
66:   const remaining=(state.journey.plannedRouteHexIds||[]).length;
67:   if(remaining<=0) return {...state,journey:{...state.journey,status:'paused',awaitingEndRoll:true},updatedAt:Date.now()};
68:   const seasonalFailure=(state.worldClock.season==='autumn'||state.worldClock.season==='winter')?1:2;
69:   const distance=success?3+Math.max(0,Math.trunc(Number(successIcons)||0)):seasonalFailure;
70:   const bounded=Math.min(distance,remaining,20);
71:   return {
72:     ...state,
73:     journey:{...state.journey,status:'travelling',marchHexesRemaining:bounded,marchRollCount:Math.max(0,Number(state.journey.marchRollCount)||0)+1,marchWillReachDestination:distance>=remaining},
74:     updatedAt:Date.now(),
75:   };
76: }
77: 
78: export function recordJourneyMove(state:MasterState,toHexId:string,locationName:string,distanceKm?:number,terrain?:string):MasterState{
79:   const fromHexId=state.position.currentHexId; const now=Date.now(); const isTravel=Boolean(fromHexId&&fromHexId!==toHexId);
80:   const active=state.journey.status!=='idle'&&state.journey.status!=='completed';
81:   if(active&&isTravel&&Number(state.journey.marchHexesRemaining||0)<=0){
82:     throw new Error('Faça a Jogada de Marcha do Guia antes de avançar a Jornada.');
83:   }
84:   const previousSegments=state.journey.segments||[];
85:   const rawSegmentDays=isTravel?journeyTravelDaysForTerrain(terrain):0;
86:   const segment=isTravel?{
87:     id:'segment_'+now+'_'+Math.random().toString(36).slice(2,6),fromHexId,toHexId,distanceKm,terrain,
88:     travelDays:rawSegmentDays,startedAtWorldMinute:Math.max(0,Number(state.worldClock.elapsedMinutes)||0),startedAt:now,completedAt:now,
89:   }:null;
90:   const nextSegments=segment?[...previousSegments,segment]:previousSegments;
91:   let effectiveTotal=active?journeyDurationFromSegments(nextSegments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),Number(state.journey.durationAdjustmentDays)||0):rawSegmentDays;
92:   const priorTotal=active?Math.max(0,Number(state.journey.day)||0):0;
93:   const deltaDays=Math.max(0,effectiveTotal-priorTotal);
94:   const advanced=deltaDays>0?advanceWorldClock(state.worldClock,deltaDays*1440):{clock:state.worldClock};
95:   if(segment) segment.completedAtWorldMinute=Math.max(0,Number(advanced.clock.elapsedMinutes)||0);
96: 
97:   const planned=(state.journey.plannedRouteHexIds||[]).slice();
98:   if(planned[0]===toHexId) planned.shift();
99:   const marchRemaining=active&&isTravel?Math.max(0,Number(state.journey.marchHexesRemaining||0)-1):Number(state.journey.marchHexesRemaining||0);
100:   const stageCompleted=active&&nextSegments.length>0&&nextSegments.length%20===0&&planned.length>0;
101:   const reachedDestination=active&&planned.length===0;
102:   const needEvent=active&&isTravel&&marchRemaining===0&&!reachedDestination&&!stageCompleted;
103: 
104:   return {
105:     ...state,worldClock:advanced.clock,
106:     position:{currentHexId:toHexId,locationName,routeHexIds:[...state.position.routeHexIds,toHexId].filter((id,index,all)=>index===0||id!==all[index-1]),lastMovedAt:now},
107:     journey:active?{
108:       ...state.journey,status:reachedDestination||stageCompleted?'paused':'travelling',day:effectiveTotal,segments:nextSegments,
109:       plannedRouteHexIds:planned,marchHexesRemaining:marchRemaining,awaitingEventGeneration:needEvent,
110:       awaitingEndRoll:reachedDestination,stageComplete:stageCompleted,
111:     }:state.journey,
112:     updatedAt:now,
113:   };
114: }
115: 
116: function featRank(value:number):number{return value===12?12:value===11?0:value;}
117: function chooseFeat(results:number[],mode:'favoured'|'normal'|'ill-favoured'):number{
118:   if(!results.length) return 1;
119:   if(mode==='normal') return results[0];
120:   return results.reduce((chosen,value)=>mode==='favoured'
121:     ?(featRank(value)>featRank(chosen)?value:chosen)
122:     :(featRank(value)<featRank(chosen)?value:chosen));
123: }
124: export type JourneyRegionKind='border'|'wild'|'dark';
125: export function journeyEventRollMode(region:JourneyRegionKind):'favoured'|'normal'|'ill-favoured'{
126:   return region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
127: }
128: function eventFromFeat(feat:number):{type:string;fatigue:number;description:string}{
129:   if(feat===11) return {type:'terrible-misfortune',fatigue:3,description:'Infortúnio Terrível'};
130:   if(feat===1) return {type:'despair',fatigue:2,description:'Desespero'};
131:   if(feat===2||feat===3) return {type:'ill-choices',fatigue:2,description:'Escolhas Ruins'};
132:   if(feat>=4&&feat<=7) return {type:'mishap',fatigue:2,description:'Azar'};
133:   if(feat===8||feat===9) return {type:'shortcut',fatigue:1,description:'Atalho'};
134:   if(feat===10) return {type:'chance-meeting',fatigue:1,description:'Encontro Fortuito'};
135:   return {type:'joyful-sight',fatigue:0,description:'Visão Alegre'};
136: }
137: export function generateJourneyEvent(state:MasterState,region:JourneyRegionKind,targetDie:number,featResults:number[],terrain?:string):MasterState{
138:   if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
139:   const targetRoll=Math.max(1,Math.min(6,Math.trunc(targetDie)));
140:   const targetRole:JourneyRole=targetRoll<=2?'scout':targetRoll<=4?'lookout':'hunter';
141:   const mode=journeyEventRollMode(region);
142:   const feat=chooseFeat(featResults,mode);
143:   const event=eventFromFeat(feat);
144:   return {
145:     ...state,
146:     journey:{
147:       ...state.journey,awaitingEventGeneration:false,
148:       pendingEvent:{
149:         id:'journey_event_'+Date.now(),targetRole,skill:JOURNEY_ROLE_SKILLS[targetRole],fatigueOnFailure:event.fatigue,
150:         description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,
151:         successDiceModifier:isRoadTerrain(terrain)?1:isDifficultJourneyTerrain(terrain)?-1:0,
\`\`\`

### lib/rules/journey.ts — /generateJourneyEvent/ — linha 137

\`\`\`text
109:       plannedRouteHexIds:planned,marchHexesRemaining:marchRemaining,awaitingEventGeneration:needEvent,
110:       awaitingEndRoll:reachedDestination,stageComplete:stageCompleted,
111:     }:state.journey,
112:     updatedAt:now,
113:   };
114: }
115: 
116: function featRank(value:number):number{return value===12?12:value===11?0:value;}
117: function chooseFeat(results:number[],mode:'favoured'|'normal'|'ill-favoured'):number{
118:   if(!results.length) return 1;
119:   if(mode==='normal') return results[0];
120:   return results.reduce((chosen,value)=>mode==='favoured'
121:     ?(featRank(value)>featRank(chosen)?value:chosen)
122:     :(featRank(value)<featRank(chosen)?value:chosen));
123: }
124: export type JourneyRegionKind='border'|'wild'|'dark';
125: export function journeyEventRollMode(region:JourneyRegionKind):'favoured'|'normal'|'ill-favoured'{
126:   return region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
127: }
128: function eventFromFeat(feat:number):{type:string;fatigue:number;description:string}{
129:   if(feat===11) return {type:'terrible-misfortune',fatigue:3,description:'Infortúnio Terrível'};
130:   if(feat===1) return {type:'despair',fatigue:2,description:'Desespero'};
131:   if(feat===2||feat===3) return {type:'ill-choices',fatigue:2,description:'Escolhas Ruins'};
132:   if(feat>=4&&feat<=7) return {type:'mishap',fatigue:2,description:'Azar'};
133:   if(feat===8||feat===9) return {type:'shortcut',fatigue:1,description:'Atalho'};
134:   if(feat===10) return {type:'chance-meeting',fatigue:1,description:'Encontro Fortuito'};
135:   return {type:'joyful-sight',fatigue:0,description:'Visão Alegre'};
136: }
137: export function generateJourneyEvent(state:MasterState,region:JourneyRegionKind,targetDie:number,featResults:number[],terrain?:string):MasterState{
138:   if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
139:   const targetRoll=Math.max(1,Math.min(6,Math.trunc(targetDie)));
140:   const targetRole:JourneyRole=targetRoll<=2?'scout':targetRoll<=4?'lookout':'hunter';
141:   const mode=journeyEventRollMode(region);
142:   const feat=chooseFeat(featResults,mode);
143:   const event=eventFromFeat(feat);
144:   return {
145:     ...state,
146:     journey:{
147:       ...state.journey,awaitingEventGeneration:false,
148:       pendingEvent:{
149:         id:'journey_event_'+Date.now(),targetRole,skill:JOURNEY_ROLE_SKILLS[targetRole],fatigueOnFailure:event.fatigue,
150:         description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,
151:         successDiceModifier:isRoadTerrain(terrain)?1:isDifficultJourneyTerrain(terrain)?-1:0,
152:       },
153:     },
154:     updatedAt:Date.now(),
155:   };
156: }
157: export interface JourneyEventEffects {
158:   allFatigue:number;targetFatigue:number;allShadow:number;targetShadow:number;allHope:number;woundTarget:boolean;dayAdjustment:number;
159: }
160: export function resolveJourneyEvent(state:MasterState,success:boolean,successIcons=0):{state:MasterState;effects:JourneyEventEffects}{
161:   const pending=state.journey.pendingEvent;
162:   if(!pending) throw new Error('Nenhum Evento de Jornada pendente.');
163:   const type=String(pending.eventType||''); const effects:JourneyEventEffects={allFatigue:Math.max(0,Number(pending.fatigueOnFailure)||0),targetFatigue:0,allShadow:0,targetShadow:0,allHope:0,woundTarget:false,dayAdjustment:0};
164:   if(type==='terrible-misfortune'&&!success) effects.woundTarget=true;
165:   if(type==='despair'&&!success) effects.allShadow=1;
166:   if(type==='ill-choices'&&!success) effects.targetShadow=1;
167:   if(type==='mishap'&&!success){effects.dayAdjustment=1;effects.targetFatigue=1;}
168:   if(type==='shortcut'&&success) effects.dayAdjustment=-1;
169:   if(type==='chance-meeting'&&success) effects.allFatigue=0;
170:   if(type==='joyful-sight'&&success) effects.allHope=1;
171:   const adjustment=(Number(state.journey.durationAdjustmentDays)||0)+effects.dayAdjustment;
172:   const effective=journeyDurationFromSegments(state.journey.segments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),adjustment);
173:   const danger=Math.max(0,Number(state.journey.dangerEventsRemaining||0)-(Number(state.journey.dangerEventsRemaining||0)>0?1:0));
174:   const shifted=effects.dayAdjustment!==0?advanceWorldClock(state.worldClock,effects.dayAdjustment*1440).clock:state.worldClock;
175:   return {
176:     state:{...state,worldClock:shifted,journey:{...state.journey,pendingEvent:null,durationAdjustmentDays:adjustment,day:effective,dangerEventsRemaining:danger,awaitingEventGeneration:danger>0},updatedAt:Date.now()},
177:     effects,
178:   };
179: }
180: export function setDangerousArea(state:MasterState,danger:number):MasterState{
181:   const value=Math.max(0,Math.trunc(Number(danger)||0));
182:   return {...state,journey:{...state.journey,dangerEventsRemaining:value,awaitingEventGeneration:value>0,marchHexesRemaining:0},updatedAt:Date.now()};
183: }
184: export function continueJourneyStage(state:MasterState):MasterState{
185:   if(!state.journey.stageComplete) return state;
186:   return {...state,journey:{...state.journey,status:'planned',stageComplete:false,stageNumber:Math.max(1,Number(state.journey.stageNumber)||1)+1,stageRouteHexIds:(state.journey.plannedRouteHexIds||[]).slice(0,20),marchHexesRemaining:0},updatedAt:Date.now()};
187: }
188: export function completeJourney(state:MasterState):MasterState{
189:   return {...state,activeMode:'adventure',journey:{...state.journey,status:'completed',destinationHexId:state.position.currentHexId,pendingEvent:null,awaitingEndRoll:false,marchHexesRemaining:0},updatedAt:Date.now()};
190: }
\`\`\`

### lib/rules/journey.ts — /resolveJourneyEvent/ — linha 160

\`\`\`text
132:   if(feat>=4&&feat<=7) return {type:'mishap',fatigue:2,description:'Azar'};
133:   if(feat===8||feat===9) return {type:'shortcut',fatigue:1,description:'Atalho'};
134:   if(feat===10) return {type:'chance-meeting',fatigue:1,description:'Encontro Fortuito'};
135:   return {type:'joyful-sight',fatigue:0,description:'Visão Alegre'};
136: }
137: export function generateJourneyEvent(state:MasterState,region:JourneyRegionKind,targetDie:number,featResults:number[],terrain?:string):MasterState{
138:   if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
139:   const targetRoll=Math.max(1,Math.min(6,Math.trunc(targetDie)));
140:   const targetRole:JourneyRole=targetRoll<=2?'scout':targetRoll<=4?'lookout':'hunter';
141:   const mode=journeyEventRollMode(region);
142:   const feat=chooseFeat(featResults,mode);
143:   const event=eventFromFeat(feat);
144:   return {
145:     ...state,
146:     journey:{
147:       ...state.journey,awaitingEventGeneration:false,
148:       pendingEvent:{
149:         id:'journey_event_'+Date.now(),targetRole,skill:JOURNEY_ROLE_SKILLS[targetRole],fatigueOnFailure:event.fatigue,
150:         description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,
151:         successDiceModifier:isRoadTerrain(terrain)?1:isDifficultJourneyTerrain(terrain)?-1:0,
152:       },
153:     },
154:     updatedAt:Date.now(),
155:   };
156: }
157: export interface JourneyEventEffects {
158:   allFatigue:number;targetFatigue:number;allShadow:number;targetShadow:number;allHope:number;woundTarget:boolean;dayAdjustment:number;
159: }
160: export function resolveJourneyEvent(state:MasterState,success:boolean,successIcons=0):{state:MasterState;effects:JourneyEventEffects}{
161:   const pending=state.journey.pendingEvent;
162:   if(!pending) throw new Error('Nenhum Evento de Jornada pendente.');
163:   const type=String(pending.eventType||''); const effects:JourneyEventEffects={allFatigue:Math.max(0,Number(pending.fatigueOnFailure)||0),targetFatigue:0,allShadow:0,targetShadow:0,allHope:0,woundTarget:false,dayAdjustment:0};
164:   if(type==='terrible-misfortune'&&!success) effects.woundTarget=true;
165:   if(type==='despair'&&!success) effects.allShadow=1;
166:   if(type==='ill-choices'&&!success) effects.targetShadow=1;
167:   if(type==='mishap'&&!success){effects.dayAdjustment=1;effects.targetFatigue=1;}
168:   if(type==='shortcut'&&success) effects.dayAdjustment=-1;
169:   if(type==='chance-meeting'&&success) effects.allFatigue=0;
170:   if(type==='joyful-sight'&&success) effects.allHope=1;
171:   const adjustment=(Number(state.journey.durationAdjustmentDays)||0)+effects.dayAdjustment;
172:   const effective=journeyDurationFromSegments(state.journey.segments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),adjustment);
173:   const danger=Math.max(0,Number(state.journey.dangerEventsRemaining||0)-(Number(state.journey.dangerEventsRemaining||0)>0?1:0));
174:   const shifted=effects.dayAdjustment!==0?advanceWorldClock(state.worldClock,effects.dayAdjustment*1440).clock:state.worldClock;
175:   return {
176:     state:{...state,worldClock:shifted,journey:{...state.journey,pendingEvent:null,durationAdjustmentDays:adjustment,day:effective,dangerEventsRemaining:danger,awaitingEventGeneration:danger>0},updatedAt:Date.now()},
177:     effects,
178:   };
179: }
180: export function setDangerousArea(state:MasterState,danger:number):MasterState{
181:   const value=Math.max(0,Math.trunc(Number(danger)||0));
182:   return {...state,journey:{...state.journey,dangerEventsRemaining:value,awaitingEventGeneration:value>0,marchHexesRemaining:0},updatedAt:Date.now()};
183: }
184: export function continueJourneyStage(state:MasterState):MasterState{
185:   if(!state.journey.stageComplete) return state;
186:   return {...state,journey:{...state.journey,status:'planned',stageComplete:false,stageNumber:Math.max(1,Number(state.journey.stageNumber)||1)+1,stageRouteHexIds:(state.journey.plannedRouteHexIds||[]).slice(0,20),marchHexesRemaining:0},updatedAt:Date.now()};
187: }
188: export function completeJourney(state:MasterState):MasterState{
189:   return {...state,activeMode:'adventure',journey:{...state.journey,status:'completed',destinationHexId:state.position.currentHexId,pendingEvent:null,awaitingEndRoll:false,marchHexesRemaining:0},updatedAt:Date.now()};
190: }
\`\`\`

### lib/rules/journey.ts — /completeJourney/ — linha 188

\`\`\`text
160: export function resolveJourneyEvent(state:MasterState,success:boolean,successIcons=0):{state:MasterState;effects:JourneyEventEffects}{
161:   const pending=state.journey.pendingEvent;
162:   if(!pending) throw new Error('Nenhum Evento de Jornada pendente.');
163:   const type=String(pending.eventType||''); const effects:JourneyEventEffects={allFatigue:Math.max(0,Number(pending.fatigueOnFailure)||0),targetFatigue:0,allShadow:0,targetShadow:0,allHope:0,woundTarget:false,dayAdjustment:0};
164:   if(type==='terrible-misfortune'&&!success) effects.woundTarget=true;
165:   if(type==='despair'&&!success) effects.allShadow=1;
166:   if(type==='ill-choices'&&!success) effects.targetShadow=1;
167:   if(type==='mishap'&&!success){effects.dayAdjustment=1;effects.targetFatigue=1;}
168:   if(type==='shortcut'&&success) effects.dayAdjustment=-1;
169:   if(type==='chance-meeting'&&success) effects.allFatigue=0;
170:   if(type==='joyful-sight'&&success) effects.allHope=1;
171:   const adjustment=(Number(state.journey.durationAdjustmentDays)||0)+effects.dayAdjustment;
172:   const effective=journeyDurationFromSegments(state.journey.segments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),adjustment);
173:   const danger=Math.max(0,Number(state.journey.dangerEventsRemaining||0)-(Number(state.journey.dangerEventsRemaining||0)>0?1:0));
174:   const shifted=effects.dayAdjustment!==0?advanceWorldClock(state.worldClock,effects.dayAdjustment*1440).clock:state.worldClock;
175:   return {
176:     state:{...state,worldClock:shifted,journey:{...state.journey,pendingEvent:null,durationAdjustmentDays:adjustment,day:effective,dangerEventsRemaining:danger,awaitingEventGeneration:danger>0},updatedAt:Date.now()},
177:     effects,
178:   };
179: }
180: export function setDangerousArea(state:MasterState,danger:number):MasterState{
181:   const value=Math.max(0,Math.trunc(Number(danger)||0));
182:   return {...state,journey:{...state.journey,dangerEventsRemaining:value,awaitingEventGeneration:value>0,marchHexesRemaining:0},updatedAt:Date.now()};
183: }
184: export function continueJourneyStage(state:MasterState):MasterState{
185:   if(!state.journey.stageComplete) return state;
186:   return {...state,journey:{...state.journey,status:'planned',stageComplete:false,stageNumber:Math.max(1,Number(state.journey.stageNumber)||1)+1,stageRouteHexIds:(state.journey.plannedRouteHexIds||[]).slice(0,20),marchHexesRemaining:0},updatedAt:Date.now()};
187: }
188: export function completeJourney(state:MasterState):MasterState{
189:   return {...state,activeMode:'adventure',journey:{...state.journey,status:'completed',destinationHexId:state.position.currentHexId,pendingEvent:null,awaitingEndRoll:false,marchHexesRemaining:0},updatedAt:Date.now()};
190: }
\`\`\`

### lib/rules/journey.ts — /recordJourneyMove/ — linha 78

\`\`\`text
50:   return {
51:     ...state,activeMode:'journey',company:{...state.company,journeyRoles:{...roles}},
52:     journey:{
53:       status:'planned',originHexId:state.position.currentHexId,destinationHexId:destinationHexId||route.at(-1)||null,
54:       startedAt:Date.now(),day:0,segments:[],pendingEvent:null,
55:       plannedRouteHexIds:route,stageRouteHexIds:stage,stageNumber:1,marchHexesRemaining:0,marchRollCount:0,
56:       awaitingEventGeneration:false,awaitingEndRoll:false,durationAdjustmentDays:0,mounted,forcedMarch,dangerEventsRemaining:0,travelFatigueByHero:{},
57:     },
58:     updatedAt:Date.now(),
59:   };
60: }
61: 
62: export function resolveJourneyMarch(state:MasterState,success:boolean,successIcons=0):MasterState{
63:   if(!['planned','travelling','paused'].includes(state.journey.status)) throw new Error('Não há Jornada ativa para uma Jogada de Marcha.');
64:   if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
65:   if(state.journey.awaitingEndRoll) throw new Error('A Companhia já alcançou o fim desta etapa.');
66:   const remaining=(state.journey.plannedRouteHexIds||[]).length;
67:   if(remaining<=0) return {...state,journey:{...state.journey,status:'paused',awaitingEndRoll:true},updatedAt:Date.now()};
68:   const seasonalFailure=(state.worldClock.season==='autumn'||state.worldClock.season==='winter')?1:2;
69:   const distance=success?3+Math.max(0,Math.trunc(Number(successIcons)||0)):seasonalFailure;
70:   const bounded=Math.min(distance,remaining,20);
71:   return {
72:     ...state,
73:     journey:{...state.journey,status:'travelling',marchHexesRemaining:bounded,marchRollCount:Math.max(0,Number(state.journey.marchRollCount)||0)+1,marchWillReachDestination:distance>=remaining},
74:     updatedAt:Date.now(),
75:   };
76: }
77: 
78: export function recordJourneyMove(state:MasterState,toHexId:string,locationName:string,distanceKm?:number,terrain?:string):MasterState{
79:   const fromHexId=state.position.currentHexId; const now=Date.now(); const isTravel=Boolean(fromHexId&&fromHexId!==toHexId);
80:   const active=state.journey.status!=='idle'&&state.journey.status!=='completed';
81:   if(active&&isTravel&&Number(state.journey.marchHexesRemaining||0)<=0){
82:     throw new Error('Faça a Jogada de Marcha do Guia antes de avançar a Jornada.');
83:   }
84:   const previousSegments=state.journey.segments||[];
85:   const rawSegmentDays=isTravel?journeyTravelDaysForTerrain(terrain):0;
86:   const segment=isTravel?{
87:     id:'segment_'+now+'_'+Math.random().toString(36).slice(2,6),fromHexId,toHexId,distanceKm,terrain,
88:     travelDays:rawSegmentDays,startedAtWorldMinute:Math.max(0,Number(state.worldClock.elapsedMinutes)||0),startedAt:now,completedAt:now,
89:   }:null;
90:   const nextSegments=segment?[...previousSegments,segment]:previousSegments;
91:   let effectiveTotal=active?journeyDurationFromSegments(nextSegments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),Number(state.journey.durationAdjustmentDays)||0):rawSegmentDays;
92:   const priorTotal=active?Math.max(0,Number(state.journey.day)||0):0;
93:   const deltaDays=Math.max(0,effectiveTotal-priorTotal);
94:   const advanced=deltaDays>0?advanceWorldClock(state.worldClock,deltaDays*1440):{clock:state.worldClock};
95:   if(segment) segment.completedAtWorldMinute=Math.max(0,Number(advanced.clock.elapsedMinutes)||0);
96: 
97:   const planned=(state.journey.plannedRouteHexIds||[]).slice();
98:   if(planned[0]===toHexId) planned.shift();
99:   const marchRemaining=active&&isTravel?Math.max(0,Number(state.journey.marchHexesRemaining||0)-1):Number(state.journey.marchHexesRemaining||0);
100:   const stageCompleted=active&&nextSegments.length>0&&nextSegments.length%20===0&&planned.length>0;
101:   const reachedDestination=active&&planned.length===0;
102:   const needEvent=active&&isTravel&&marchRemaining===0&&!reachedDestination&&!stageCompleted;
103: 
104:   return {
105:     ...state,worldClock:advanced.clock,
106:     position:{currentHexId:toHexId,locationName,routeHexIds:[...state.position.routeHexIds,toHexId].filter((id,index,all)=>index===0||id!==all[index-1]),lastMovedAt:now},
107:     journey:active?{
108:       ...state.journey,status:reachedDestination||stageCompleted?'paused':'travelling',day:effectiveTotal,segments:nextSegments,
109:       plannedRouteHexIds:planned,marchHexesRemaining:marchRemaining,awaitingEventGeneration:needEvent,
110:       awaitingEndRoll:reachedDestination,stageComplete:stageCompleted,
111:     }:state.journey,
112:     updatedAt:now,
113:   };
114: }
115: 
116: function featRank(value:number):number{return value===12?12:value===11?0:value;}
117: function chooseFeat(results:number[],mode:'favoured'|'normal'|'ill-favoured'):number{
118:   if(!results.length) return 1;
119:   if(mode==='normal') return results[0];
120:   return results.reduce((chosen,value)=>mode==='favoured'
121:     ?(featRank(value)>featRank(chosen)?value:chosen)
122:     :(featRank(value)<featRank(chosen)?value:chosen));
123: }
124: export type JourneyRegionKind='border'|'wild'|'dark';
125: export function journeyEventRollMode(region:JourneyRegionKind):'favoured'|'normal'|'ill-favoured'{
126:   return region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
127: }
128: function eventFromFeat(feat:number):{type:string;fatigue:number;description:string}{
129:   if(feat===11) return {type:'terrible-misfortune',fatigue:3,description:'Infortúnio Terrível'};
130:   if(feat===1) return {type:'despair',fatigue:2,description:'Desespero'};
131:   if(feat===2||feat===3) return {type:'ill-choices',fatigue:2,description:'Escolhas Ruins'};
132:   if(feat>=4&&feat<=7) return {type:'mishap',fatigue:2,description:'Azar'};
133:   if(feat===8||feat===9) return {type:'shortcut',fatigue:1,description:'Atalho'};
134:   if(feat===10) return {type:'chance-meeting',fatigue:1,description:'Encontro Fortuito'};
135:   return {type:'joyful-sight',fatigue:0,description:'Visão Alegre'};
136: }
137: export function generateJourneyEvent(state:MasterState,region:JourneyRegionKind,targetDie:number,featResults:number[],terrain?:string):MasterState{
138:   if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
139:   const targetRoll=Math.max(1,Math.min(6,Math.trunc(targetDie)));
140:   const targetRole:JourneyRole=targetRoll<=2?'scout':targetRoll<=4?'lookout':'hunter';
141:   const mode=journeyEventRollMode(region);
142:   const feat=chooseFeat(featResults,mode);
143:   const event=eventFromFeat(feat);
144:   return {
145:     ...state,
146:     journey:{
147:       ...state.journey,awaitingEventGeneration:false,
148:       pendingEvent:{
149:         id:'journey_event_'+Date.now(),targetRole,skill:JOURNEY_ROLE_SKILLS[targetRole],fatigueOnFailure:event.fatigue,
150:         description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,
151:         successDiceModifier:isRoadTerrain(terrain)?1:isDifficultJourneyTerrain(terrain)?-1:0,
152:       },
153:     },
154:     updatedAt:Date.now(),
155:   };
156: }
157: export interface JourneyEventEffects {
158:   allFatigue:number;targetFatigue:number;allShadow:number;targetShadow:number;allHope:number;woundTarget:boolean;dayAdjustment:number;
159: }
160: export function resolveJourneyEvent(state:MasterState,success:boolean,successIcons=0):{state:MasterState;effects:JourneyEventEffects}{
161:   const pending=state.journey.pendingEvent;
162:   if(!pending) throw new Error('Nenhum Evento de Jornada pendente.');
163:   const type=String(pending.eventType||''); const effects:JourneyEventEffects={allFatigue:Math.max(0,Number(pending.fatigueOnFailure)||0),targetFatigue:0,allShadow:0,targetShadow:0,allHope:0,woundTarget:false,dayAdjustment:0};
164:   if(type==='terrible-misfortune'&&!success) effects.woundTarget=true;
165:   if(type==='despair'&&!success) effects.allShadow=1;
166:   if(type==='ill-choices'&&!success) effects.targetShadow=1;
167:   if(type==='mishap'&&!success){effects.dayAdjustment=1;effects.targetFatigue=1;}
\`\`\`

### lib/rules/journey.ts — /travelFatigueByHero/ — linha 56

\`\`\`text
28:   segments:Array<{terrain?:string}>,
29:   mounted=false,
30:   forcedMarch=false,
31:   adjustment=0,
32: ):number{
33:   const hexes=segments.length;
34:   const difficult=segments.filter(segment=>isDifficultJourneyTerrain(segment.terrain)).length;
35:   let days=forcedMarch?Math.ceil(hexes/2)+difficult:hexes+difficult;
36:   if(mounted) days=Math.ceil(days/2);
37:   return Math.max(0,days+Math.trunc(Number(adjustment)||0));
38: }
39: 
40: export function startJourney(
41:   state:MasterState,
42:   destinationHexId:string|null,
43:   roles:Partial<Record<JourneyRole,string>>,
44:   routeHexIds:string[]=[],
45:   mounted=false,
46:   forcedMarch=false,
47: ):MasterState{
48:   const route=routeHexIds.filter(Boolean).slice();
49:   const stage=route.slice(0,20);
50:   return {
51:     ...state,activeMode:'journey',company:{...state.company,journeyRoles:{...roles}},
52:     journey:{
53:       status:'planned',originHexId:state.position.currentHexId,destinationHexId:destinationHexId||route.at(-1)||null,
54:       startedAt:Date.now(),day:0,segments:[],pendingEvent:null,
55:       plannedRouteHexIds:route,stageRouteHexIds:stage,stageNumber:1,marchHexesRemaining:0,marchRollCount:0,
56:       awaitingEventGeneration:false,awaitingEndRoll:false,durationAdjustmentDays:0,mounted,forcedMarch,dangerEventsRemaining:0,travelFatigueByHero:{},
57:     },
58:     updatedAt:Date.now(),
59:   };
60: }
61: 
62: export function resolveJourneyMarch(state:MasterState,success:boolean,successIcons=0):MasterState{
63:   if(!['planned','travelling','paused'].includes(state.journey.status)) throw new Error('Não há Jornada ativa para uma Jogada de Marcha.');
64:   if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
65:   if(state.journey.awaitingEndRoll) throw new Error('A Companhia já alcançou o fim desta etapa.');
66:   const remaining=(state.journey.plannedRouteHexIds||[]).length;
67:   if(remaining<=0) return {...state,journey:{...state.journey,status:'paused',awaitingEndRoll:true},updatedAt:Date.now()};
68:   const seasonalFailure=(state.worldClock.season==='autumn'||state.worldClock.season==='winter')?1:2;
69:   const distance=success?3+Math.max(0,Math.trunc(Number(successIcons)||0)):seasonalFailure;
70:   const bounded=Math.min(distance,remaining,20);
71:   return {
72:     ...state,
73:     journey:{...state.journey,status:'travelling',marchHexesRemaining:bounded,marchRollCount:Math.max(0,Number(state.journey.marchRollCount)||0)+1,marchWillReachDestination:distance>=remaining},
74:     updatedAt:Date.now(),
75:   };
76: }
77: 
78: export function recordJourneyMove(state:MasterState,toHexId:string,locationName:string,distanceKm?:number,terrain?:string):MasterState{
79:   const fromHexId=state.position.currentHexId; const now=Date.now(); const isTravel=Boolean(fromHexId&&fromHexId!==toHexId);
80:   const active=state.journey.status!=='idle'&&state.journey.status!=='completed';
81:   if(active&&isTravel&&Number(state.journey.marchHexesRemaining||0)<=0){
82:     throw new Error('Faça a Jogada de Marcha do Guia antes de avançar a Jornada.');
83:   }
84:   const previousSegments=state.journey.segments||[];
85:   const rawSegmentDays=isTravel?journeyTravelDaysForTerrain(terrain):0;
86:   const segment=isTravel?{
87:     id:'segment_'+now+'_'+Math.random().toString(36).slice(2,6),fromHexId,toHexId,distanceKm,terrain,
88:     travelDays:rawSegmentDays,startedAtWorldMinute:Math.max(0,Number(state.worldClock.elapsedMinutes)||0),startedAt:now,completedAt:now,
89:   }:null;
90:   const nextSegments=segment?[...previousSegments,segment]:previousSegments;
91:   let effectiveTotal=active?journeyDurationFromSegments(nextSegments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),Number(state.journey.durationAdjustmentDays)||0):rawSegmentDays;
92:   const priorTotal=active?Math.max(0,Number(state.journey.day)||0):0;
93:   const deltaDays=Math.max(0,effectiveTotal-priorTotal);
94:   const advanced=deltaDays>0?advanceWorldClock(state.worldClock,deltaDays*1440):{clock:state.worldClock};
95:   if(segment) segment.completedAtWorldMinute=Math.max(0,Number(advanced.clock.elapsedMinutes)||0);
96: 
97:   const planned=(state.journey.plannedRouteHexIds||[]).slice();
98:   if(planned[0]===toHexId) planned.shift();
99:   const marchRemaining=active&&isTravel?Math.max(0,Number(state.journey.marchHexesRemaining||0)-1):Number(state.journey.marchHexesRemaining||0);
100:   const stageCompleted=active&&nextSegments.length>0&&nextSegments.length%20===0&&planned.length>0;
101:   const reachedDestination=active&&planned.length===0;
102:   const needEvent=active&&isTravel&&marchRemaining===0&&!reachedDestination&&!stageCompleted;
103: 
104:   return {
105:     ...state,worldClock:advanced.clock,
106:     position:{currentHexId:toHexId,locationName,routeHexIds:[...state.position.routeHexIds,toHexId].filter((id,index,all)=>index===0||id!==all[index-1]),lastMovedAt:now},
107:     journey:active?{
108:       ...state.journey,status:reachedDestination||stageCompleted?'paused':'travelling',day:effectiveTotal,segments:nextSegments,
109:       plannedRouteHexIds:planned,marchHexesRemaining:marchRemaining,awaitingEventGeneration:needEvent,
110:       awaitingEndRoll:reachedDestination,stageComplete:stageCompleted,
111:     }:state.journey,
112:     updatedAt:now,
113:   };
114: }
115: 
116: function featRank(value:number):number{return value===12?12:value===11?0:value;}
117: function chooseFeat(results:number[],mode:'favoured'|'normal'|'ill-favoured'):number{
118:   if(!results.length) return 1;
119:   if(mode==='normal') return results[0];
120:   return results.reduce((chosen,value)=>mode==='favoured'
121:     ?(featRank(value)>featRank(chosen)?value:chosen)
122:     :(featRank(value)<featRank(chosen)?value:chosen));
123: }
124: export type JourneyRegionKind='border'|'wild'|'dark';
125: export function journeyEventRollMode(region:JourneyRegionKind):'favoured'|'normal'|'ill-favoured'{
126:   return region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
127: }
128: function eventFromFeat(feat:number):{type:string;fatigue:number;description:string}{
129:   if(feat===11) return {type:'terrible-misfortune',fatigue:3,description:'Infortúnio Terrível'};
130:   if(feat===1) return {type:'despair',fatigue:2,description:'Desespero'};
131:   if(feat===2||feat===3) return {type:'ill-choices',fatigue:2,description:'Escolhas Ruins'};
132:   if(feat>=4&&feat<=7) return {type:'mishap',fatigue:2,description:'Azar'};
133:   if(feat===8||feat===9) return {type:'shortcut',fatigue:1,description:'Atalho'};
134:   if(feat===10) return {type:'chance-meeting',fatigue:1,description:'Encontro Fortuito'};
135:   return {type:'joyful-sight',fatigue:0,description:'Visão Alegre'};
136: }
137: export function generateJourneyEvent(state:MasterState,region:JourneyRegionKind,targetDie:number,featResults:number[],terrain?:string):MasterState{
138:   if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
139:   const targetRoll=Math.max(1,Math.min(6,Math.trunc(targetDie)));
140:   const targetRole:JourneyRole=targetRoll<=2?'scout':targetRoll<=4?'lookout':'hunter';
141:   const mode=journeyEventRollMode(region);
142:   const feat=chooseFeat(featResults,mode);
143:   const event=eventFromFeat(feat);
144:   return {
145:     ...state,
\`\`\`

### lib/rules/journey.ts — /awaitingEventGeneration/ — linha 64

\`\`\`text
36:   if(mounted) days=Math.ceil(days/2);
37:   return Math.max(0,days+Math.trunc(Number(adjustment)||0));
38: }
39: 
40: export function startJourney(
41:   state:MasterState,
42:   destinationHexId:string|null,
43:   roles:Partial<Record<JourneyRole,string>>,
44:   routeHexIds:string[]=[],
45:   mounted=false,
46:   forcedMarch=false,
47: ):MasterState{
48:   const route=routeHexIds.filter(Boolean).slice();
49:   const stage=route.slice(0,20);
50:   return {
51:     ...state,activeMode:'journey',company:{...state.company,journeyRoles:{...roles}},
52:     journey:{
53:       status:'planned',originHexId:state.position.currentHexId,destinationHexId:destinationHexId||route.at(-1)||null,
54:       startedAt:Date.now(),day:0,segments:[],pendingEvent:null,
55:       plannedRouteHexIds:route,stageRouteHexIds:stage,stageNumber:1,marchHexesRemaining:0,marchRollCount:0,
56:       awaitingEventGeneration:false,awaitingEndRoll:false,durationAdjustmentDays:0,mounted,forcedMarch,dangerEventsRemaining:0,travelFatigueByHero:{},
57:     },
58:     updatedAt:Date.now(),
59:   };
60: }
61: 
62: export function resolveJourneyMarch(state:MasterState,success:boolean,successIcons=0):MasterState{
63:   if(!['planned','travelling','paused'].includes(state.journey.status)) throw new Error('Não há Jornada ativa para uma Jogada de Marcha.');
64:   if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
65:   if(state.journey.awaitingEndRoll) throw new Error('A Companhia já alcançou o fim desta etapa.');
66:   const remaining=(state.journey.plannedRouteHexIds||[]).length;
67:   if(remaining<=0) return {...state,journey:{...state.journey,status:'paused',awaitingEndRoll:true},updatedAt:Date.now()};
68:   const seasonalFailure=(state.worldClock.season==='autumn'||state.worldClock.season==='winter')?1:2;
69:   const distance=success?3+Math.max(0,Math.trunc(Number(successIcons)||0)):seasonalFailure;
70:   const bounded=Math.min(distance,remaining,20);
71:   return {
72:     ...state,
73:     journey:{...state.journey,status:'travelling',marchHexesRemaining:bounded,marchRollCount:Math.max(0,Number(state.journey.marchRollCount)||0)+1,marchWillReachDestination:distance>=remaining},
74:     updatedAt:Date.now(),
75:   };
76: }
77: 
78: export function recordJourneyMove(state:MasterState,toHexId:string,locationName:string,distanceKm?:number,terrain?:string):MasterState{
79:   const fromHexId=state.position.currentHexId; const now=Date.now(); const isTravel=Boolean(fromHexId&&fromHexId!==toHexId);
80:   const active=state.journey.status!=='idle'&&state.journey.status!=='completed';
81:   if(active&&isTravel&&Number(state.journey.marchHexesRemaining||0)<=0){
82:     throw new Error('Faça a Jogada de Marcha do Guia antes de avançar a Jornada.');
83:   }
84:   const previousSegments=state.journey.segments||[];
85:   const rawSegmentDays=isTravel?journeyTravelDaysForTerrain(terrain):0;
86:   const segment=isTravel?{
87:     id:'segment_'+now+'_'+Math.random().toString(36).slice(2,6),fromHexId,toHexId,distanceKm,terrain,
88:     travelDays:rawSegmentDays,startedAtWorldMinute:Math.max(0,Number(state.worldClock.elapsedMinutes)||0),startedAt:now,completedAt:now,
89:   }:null;
90:   const nextSegments=segment?[...previousSegments,segment]:previousSegments;
91:   let effectiveTotal=active?journeyDurationFromSegments(nextSegments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),Number(state.journey.durationAdjustmentDays)||0):rawSegmentDays;
92:   const priorTotal=active?Math.max(0,Number(state.journey.day)||0):0;
93:   const deltaDays=Math.max(0,effectiveTotal-priorTotal);
94:   const advanced=deltaDays>0?advanceWorldClock(state.worldClock,deltaDays*1440):{clock:state.worldClock};
95:   if(segment) segment.completedAtWorldMinute=Math.max(0,Number(advanced.clock.elapsedMinutes)||0);
96: 
97:   const planned=(state.journey.plannedRouteHexIds||[]).slice();
98:   if(planned[0]===toHexId) planned.shift();
99:   const marchRemaining=active&&isTravel?Math.max(0,Number(state.journey.marchHexesRemaining||0)-1):Number(state.journey.marchHexesRemaining||0);
100:   const stageCompleted=active&&nextSegments.length>0&&nextSegments.length%20===0&&planned.length>0;
101:   const reachedDestination=active&&planned.length===0;
102:   const needEvent=active&&isTravel&&marchRemaining===0&&!reachedDestination&&!stageCompleted;
103: 
104:   return {
105:     ...state,worldClock:advanced.clock,
106:     position:{currentHexId:toHexId,locationName,routeHexIds:[...state.position.routeHexIds,toHexId].filter((id,index,all)=>index===0||id!==all[index-1]),lastMovedAt:now},
107:     journey:active?{
108:       ...state.journey,status:reachedDestination||stageCompleted?'paused':'travelling',day:effectiveTotal,segments:nextSegments,
109:       plannedRouteHexIds:planned,marchHexesRemaining:marchRemaining,awaitingEventGeneration:needEvent,
110:       awaitingEndRoll:reachedDestination,stageComplete:stageCompleted,
111:     }:state.journey,
112:     updatedAt:now,
113:   };
114: }
115: 
116: function featRank(value:number):number{return value===12?12:value===11?0:value;}
117: function chooseFeat(results:number[],mode:'favoured'|'normal'|'ill-favoured'):number{
118:   if(!results.length) return 1;
119:   if(mode==='normal') return results[0];
120:   return results.reduce((chosen,value)=>mode==='favoured'
121:     ?(featRank(value)>featRank(chosen)?value:chosen)
122:     :(featRank(value)<featRank(chosen)?value:chosen));
123: }
124: export type JourneyRegionKind='border'|'wild'|'dark';
125: export function journeyEventRollMode(region:JourneyRegionKind):'favoured'|'normal'|'ill-favoured'{
126:   return region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
127: }
128: function eventFromFeat(feat:number):{type:string;fatigue:number;description:string}{
129:   if(feat===11) return {type:'terrible-misfortune',fatigue:3,description:'Infortúnio Terrível'};
130:   if(feat===1) return {type:'despair',fatigue:2,description:'Desespero'};
131:   if(feat===2||feat===3) return {type:'ill-choices',fatigue:2,description:'Escolhas Ruins'};
132:   if(feat>=4&&feat<=7) return {type:'mishap',fatigue:2,description:'Azar'};
133:   if(feat===8||feat===9) return {type:'shortcut',fatigue:1,description:'Atalho'};
134:   if(feat===10) return {type:'chance-meeting',fatigue:1,description:'Encontro Fortuito'};
135:   return {type:'joyful-sight',fatigue:0,description:'Visão Alegre'};
136: }
137: export function generateJourneyEvent(state:MasterState,region:JourneyRegionKind,targetDie:number,featResults:number[],terrain?:string):MasterState{
138:   if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
139:   const targetRoll=Math.max(1,Math.min(6,Math.trunc(targetDie)));
140:   const targetRole:JourneyRole=targetRoll<=2?'scout':targetRoll<=4?'lookout':'hunter';
141:   const mode=journeyEventRollMode(region);
142:   const feat=chooseFeat(featResults,mode);
143:   const event=eventFromFeat(feat);
144:   return {
145:     ...state,
146:     journey:{
147:       ...state.journey,awaitingEventGeneration:false,
148:       pendingEvent:{
149:         id:'journey_event_'+Date.now(),targetRole,skill:JOURNEY_ROLE_SKILLS[targetRole],fatigueOnFailure:event.fatigue,
150:         description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,
151:         successDiceModifier:isRoadTerrain(terrain)?1:isDifficultJourneyTerrain(terrain)?-1:0,
152:       },
153:     },
\`\`\`

### components/SettingsView.tsx — /campanha/ — linha 195

\`\`\`text
167:             <select value={androidTtsCatalog?.selectedEnginePackage||''} onChange={e=>void changeAndroidTtsEngine(e.target.value)} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50">{(androidTtsCatalog?.engines||[{packageName:'',label:'Padrão do Android'}]).map((x:any)=><option key={x.packageName||'__default__'} value={x.packageName}>{x.label}{x.neuralProfile?' · neural':''}</option>)}</select>
168:             <label className="mt-3 block text-xs font-bold text-violet-100">Voz do motor selecionado</label>
169:             <select value={androidTtsCatalog?.selectedVoice||''} onChange={e=>void changeAndroidTtsVoice(e.target.value)} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50"><option value="">Automática / padrão do motor</option>{(androidTtsCatalog?.voices||[]).map((v:any)=><option key={v.name} value={v.name}>{v.label||[v.name,v.locale,v.networkRequired?'rede':'local'].filter(Boolean).join(' · ')}</option>)}</select>
170:             {androidTtsCatalog?.neuralProfile&&<div className="mt-3 rounded border border-cyan-900/70 bg-cyan-950/20 p-3"><div className="font-bold text-cyan-200">TTS local neural / Kokoro</div><p className="mt-1 text-[11px] text-slate-400">Blocos ~450 caracteres (250–650), maxNumSentences 2, silenceScale 0,06, 4 threads; próximo bloco somente após onDone real.</p><label className="mt-3 block text-xs font-bold text-cyan-100">Velocidade Kokoro: {Number(androidTtsCatalog?.neuralRate||1).toFixed(2)}×</label><input type="range" min="0.65" max="1.5" step="0.05" value={Number(androidTtsCatalog?.neuralRate||1)} onChange={e=>void changeNeuralRate(Number(e.target.value))} className="mt-1 w-full"/><div className="mt-1 text-[10px] text-slate-500">1.00× = velocidade neutra da engine; sem multiplicação dupla.</div></div>}
171:             <div className="mt-3 grid grid-cols-2 gap-2"><button type="button" onClick={()=>void refreshAndroidTts()} className="min-h-10 rounded border border-slate-700 bg-slate-950 text-xs font-bold text-slate-300">Atualizar lista</button><button type="button" onClick={()=>void testAndroidTts()} className="min-h-10 rounded border border-violet-700 bg-violet-950/40 text-xs font-bold text-violet-100">Testar voz local</button></div>
172:             {androidTtsStatus&&<p className="mt-2 text-xs text-slate-300">{androidTtsStatus}</p>}
173:           </div>
174: 
175:           <label className="block rounded border border-blue-900/60 bg-slate-950/65 p-4">
176:             <span className="flex items-center gap-2 font-bold text-blue-200"><MoonStar className="h-4 w-4" /> Atmosfera visual</span>
177:             <span className="mt-1 block text-xs leading-relaxed text-slate-400">Mantém o tema medieval noturno em preto, azul profundo e prata.</span>
178:             <span className="mt-3 flex items-center gap-2">
179:               <input type="checkbox" checked={atmosphereActive} onChange={event => onAtmosphereChange(event.target.checked)} className="h-5 w-5 accent-blue-500" />
180:               Névoa e textura de Eriador
181:             </span>
182:           </label>
183: 
184:           <div className="rounded border border-blue-900/60 bg-slate-950/65 p-4">
185:             <span className="flex items-center gap-2 font-bold text-blue-200"><Image className="h-4 w-4" /> Imagens sob demanda</span>
186:             <p className="mt-1 text-xs leading-relaxed text-slate-400">Nenhuma imagem é gerada automaticamente. O modelo só é usado quando você pede a ilustração.</p>
187:             <div className="mt-3 grid grid-cols-2 gap-2">
188:               <button type="button" onClick={() => onImageQualityChange('fast')} className={`min-h-12 rounded border px-3 text-xs font-bold ${imageQuality === 'fast' ? 'border-cyan-400 bg-cyan-950/50 text-cyan-100' : 'border-slate-700 bg-slate-950 text-slate-400'}`}>Rápida</button>
189:               <button type="button" onClick={() => onImageQualityChange('quality')} className={`min-h-12 rounded border px-3 text-xs font-bold ${imageQuality === 'quality' ? 'border-blue-400 bg-blue-950/60 text-blue-100' : 'border-slate-700 bg-slate-950 text-slate-400'}`}>Qualidade</button>
190:             </div>
191:           </div>
192: 
193:           <div className="rounded border border-emerald-900/60 bg-emerald-950/15 p-4">
194:             <span className="flex items-center gap-2 font-bold text-emerald-200"><DatabaseBackup className="h-4 w-4" /> Backup e restauração</span>
195:             <p className="mt-1 text-xs leading-relaxed text-slate-400">Exporta e restaura campanha, configuração e tokens sem apagar silenciosamente o estado atual.</p>
196:             <button type="button" onClick={onOpenBackup} className="mt-3 min-h-12 w-full rounded border border-emerald-600 bg-emerald-950/40 font-bold text-emerald-100">Abrir cofre da campanha</button>
197:           </div>
198:         </div>
199:       </section>
200: 
201:       <section className="medieval-panel p-5">
202:         <div className="mb-5 flex items-center gap-2 border-b border-blue-900/60 pb-3">
203:           <Sparkles className="h-5 w-5 text-cyan-300" />
204:           <h2 className="medieval-title text-lg">Diagnóstico do Mestre</h2>
205:         </div>
206:         <div className="space-y-4 text-sm leading-relaxed text-slate-300">
207:           <div className="rounded border border-cyan-900/60 bg-cyan-950/10 p-4">
208:             <h3 className="flex items-center gap-2 font-bold text-cyan-200"><CheckCircle className="h-4 w-4" /> Aberturas e cenas importantes</h3>
209:             <p className="mt-1">Todas as cenas usam o mesmo modelo do Mestre. A narração segue o Motor Barbara: cinematográfica, substancial, sensorial e falada como Mestre para os jogadores, sem decidir ações pela Companhia.</p>
210:           </div>
211:           <div className="rounded border border-blue-900/60 bg-blue-950/10 p-4">
212:             <h3 className="flex items-center gap-2 font-bold text-blue-200"><Volume2 className="h-4 w-4" /> TTS</h3>
213:             <p className="mt-1">O TTS usa primeiro o ID manual configurado acima. Charon continua sendo a voz padrão; o app mantém fallback compatível se o modelo escolhido falhar.</p>
214:           </div>
215:           <HealthCenter report={healthReport} isChecking={isHealthChecking} onCheck={onRunHealthCheck} />
216:           <p className="text-xs text-slate-500">O acesso ao Google Drive permanece no fluxo nativo já configurado para este aplicativo.</p>
217:         </div>
218:       </section>
219: 
220:       <footer className="lg:col-span-2 border-t border-blue-950/70 pt-4 text-center text-xs text-slate-500">
221:         O Um Anel · versão <strong className="text-blue-300">{APP_VERSION}</strong> · código {APP_VERSION_CODE}
222:       </footer>
223:     </div>
224:   );
225: };
\`\`\`

### components/MechanicsHub.tsx — /Journey/ — linha 41

\`\`\`text
13: function heroName(character?:Character){return character?.sheet.identity.name||character?.name||'Herói';}
14: function rollSkill(character:Character,skillName:string,extraDice=0,hopeDice=0){
15:   const skill=findSkill(character.sheet,skillName);
16:   const rank=Math.max(0,Number(skill?.rank)||0);
17:   const mode=shadowAtLimit(character.sheet)?'ill-favoured':skill?.favorite?'favoured':'normal';
18:   return rollOneRing(Math.max(0,rank+extraDice+hopeDice),0,targetNumberForSkill(character.sheet,skillName),skillName,mode,Boolean(character.sheet.attributes.weary),Boolean(character.sheet.attributes.miserable));
19: }
20: function featResults(mode:'favoured'|'normal'|'ill-favoured'){
21:   const count=mode==='normal'?1:2;return Array.from({length:count},()=>secureRoll(1,12));
22: }
23: 
24: export const MechanicsHub:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
25:   const [busy,setBusy]=useState(false);
26:   const [message,setMessage]=useState('');
27:   const [hope,setHope]=useState(false);
28:   const [inspired,setInspired]=useState(false);
29:   const [region,setRegion]=useState<'border'|'wild'|'dark'>('wild');
30:   const [danger,setDanger]=useState(1);
31:   const [mounted,setMounted]=useState(false);
32:   const [forcedMarch,setForcedMarch]=useState(false);
33:   const [mountVigourByHero,setMountVigourByHero]=useState<Record<string,number>>({});
34:   const [shadowHero,setShadowHero]=useState(characters[0]?.id||'');
35:   const [shadowSource,setShadowSource]=useState<'dread'|'greed'|'sorcery'|'transgression'>('dread');
36:   const [shadowAmount,setShadowAmount]=useState(1);
37:   const [progressHero,setProgressHero]=useState(characters[0]?.id||'');
38:   const [progressSkill,setProgressSkill]=useState('Viagem');
39:   const [progressProf,setProgressProf]=useState('Espadas');
40:   const [choice,setChoice]=useState('');
41:   const planned=masterState.journey.plannedRouteHexIds||[];
42:   const guide=characters.find(c=>c.id===masterState.company.journeyRoles.guide);
43:   const event=masterState.journey.pendingEvent;
44:   const eventTarget=event?characters.find(c=>c.id===masterState.company.journeyRoles[event.targetRole]):undefined;
45:   const selectedShadow=characters.find(c=>c.id===shadowHero)||characters[0];
46:   const selectedProgress=characters.find(c=>c.id===progressHero)||characters[0];
47:   const run=async(commands:MasterCommand[],reason:string)=>{
48:     setBusy(true);setMessage('');
49:     try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
50:     catch(error){setMessage(error instanceof Error?error.message:String(error));}
51:     finally{setBusy(false);}
52:   };
53:   const hopeCommands=(character:Character|undefined):MasterCommand[]=>{
54:     if(!character||!hope)return[];
55:     return [{type:'spend_hope',targetCharacterId:character.id,attribute:'heart',reason:'Esperança gasta na resolução mecânica.'} as MasterCommand];
56:   };
57:   const hopeDice=()=>hope?(inspired?2:1):0;
58: 
59:   const startJourney=()=>run([{
60:     type:'start_journey',destinationHexId:planned.at(-1)||masterState.journey.destinationHexId||null,
61:     roles:masterState.company.journeyRoles,routeHexIds:planned,mounted,forcedMarch,
62:     reason:'Jornada iniciada pela rota planejada do Atlas.',
63:   } as MasterCommand],'iniciar Jornada');
64: 
65:   const march=()=>{
66:     if(!guide){setMessage('Defina o Guia da Jornada na Companhia.');return;}
67:     const roll=rollSkill(guide,'Viagem',0,hopeDice());
68:     void run([...hopeCommands(guide),{type:'resolve_journey_march',success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Jogada de Marcha do Guia.'} as MasterCommand],
69:       'Jogada de Marcha: '+(roll.success?'sucesso':'falha')+'; '+(roll.tengwarCount||0)+' ícone(s)');
70:   };
71:   const generateEvent=()=>{
72:     const mode=region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
73:     void run([{type:'generate_journey_event',region,targetDie:secureRoll(1,6),featResults:featResults(mode),terrain:masterState.journey.segments.at(-1)?.terrain||'',reason:'Geração canônica de Evento de Jornada.'} as MasterCommand],'gerar Evento de Jornada');
74:   };
75:   const resolveEvent=()=>{
76:     if(!event||!eventTarget){setMessage('O papel alvo do Evento não possui Herói atribuído.');return;}
77:     const mod=Number(event.successDiceModifier||0);
78:     const roll=rollSkill(eventTarget,event.skill,mod,hopeDice());
79:     void run([...hopeCommands(eventTarget),{type:'resolve_journey_event',targetCharacterId:eventTarget.id,success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Resolução do Evento '+event.description+'.'} as MasterCommand],
80:       'Evento '+event.description+': '+(roll.success?'sucesso':'falha'));
81:   };
82:   const finishJourney=()=>{
83:     const endRolls:Record<string,{success:boolean;successIcons:number;mountVigour:number}>={};
84:     for(const hero of characters){
85:       const roll=rollSkill(hero,'Viagem');
86:       endRolls[hero.id]={success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,mountVigour:mounted?Math.max(0,Number(mountVigourByHero[hero.id])||0):0};
87:     }
88:     void run([{type:'complete_journey',endRolls,reason:'Encerramento canônico da Jornada: montaria e jogadas de Viagem reduzem Fadiga.'} as MasterCommand],'encerrar Jornada');
89:   };
90:   const shadowTest=()=>{
91:     if(!selectedShadow)return;
92:     const skillName=shadowSource==='dread'?'Valor':'Sabedoria';
93:     const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
94:     const favoured=shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'');
95:     const mode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':favoured?'favoured':'normal';
96:     const roll=shadowSource==='transgression'?null:rollOneRing(rating,0,14,skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));
97:     void run([{type:'resolve_shadow_gain',targetCharacterId:selectedShadow.id,amount:shadowAmount,source:shadowSource,testSuccess:Boolean(roll?.success),successIcons:Number(roll?.tengwarCount)||0,reason:'Ganho de Sombra: '+shadowSource} as MasterCommand],'resolver ganho de Sombra');
98:   };
99: 
100:   return <div className="space-y-4">
101:     <section className="rounded-lg border border-cyan-900/70 bg-zinc-950/80 p-4">
102:       <h2 className="text-sm font-black uppercase text-cyan-200">Jornada — sequência oficial</h2>
103:       <p className="mt-1 text-xs text-slate-400">Rota restante: {planned.length} hex(es) · etapa {masterState.journey.stageNumber||1} · {masterState.journey.day||0} dia(s) · marcha: {masterState.journey.marchHexesRemaining||0} hex(es).</p>
104:       <div className="mt-3 grid grid-cols-2 gap-2">
105:         {masterState.journey.status==='idle'||masterState.journey.status==='completed'
106:           ?<button disabled={busy||!planned.length} onClick={startJourney} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Iniciar pela rota</button>
107:           :<button disabled={busy||Boolean(event)||Boolean(masterState.journey.awaitingEventGeneration)||Boolean(masterState.journey.awaitingEndRoll)||Number(masterState.journey.marchHexesRemaining||0)>0} onClick={march} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Jogada de Marcha</button>}
108:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={hope} onChange={e=>setHope(e.target.checked)}/> Esperança</label>
109:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={inspired} disabled={!hope} onChange={e=>setInspired(e.target.checked)}/> Inspirado</label>
110:         {(masterState.journey.status==='idle'||masterState.journey.status==='completed')&&<>
111:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={mounted} onChange={e=>setMounted(e.target.checked)}/> Toda a Companhia montada</label>
112:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={forcedMarch} onChange={e=>setForcedMarch(e.target.checked)}/> Marcha Forçada</label>
113:         </>}
114:         {masterState.journey.awaitingEventGeneration&&<><select value={region} onChange={e=>setRegion(e.target.value as any)} className="rounded border border-slate-700 bg-black px-2 py-2 text-xs"><option value="border">Terra de Fronteira</option><option value="wild">Terra Erma</option><option value="dark">Terra Sombria</option></select><button disabled={busy} onClick={generateEvent} className="rounded border border-amber-700 px-3 py-2 text-amber-100">Gerar Evento</button></>}
115:         {event&&<button disabled={busy||!eventTarget} onClick={resolveEvent} className="col-span-2 rounded border border-amber-600 bg-amber-950/30 px-3 py-2 text-amber-100">Resolver {event.description} · {event.skill} · {eventTarget?heroName(eventTarget):'papel sem Herói'}</button>}
116:         {masterState.journey.stageComplete&&<button disabled={busy} onClick={()=>void run([{type:'continue_journey_stage',reason:'Nova etapa após 20 hexes.'} as MasterCommand],'continuar etapa da Jornada')} className="col-span-2 rounded border border-violet-700 px-3 py-2">Continuar próxima etapa</button>}
117:         {masterState.journey.awaitingEndRoll&&<>
118:           {Boolean(masterState.journey.mounted)&&<div className="col-span-2 rounded border border-emerald-900/60 p-2"><div className="mb-1 text-[10px] uppercase text-emerald-300">Vigor das montarias</div>{characters.map(hero=><label key={hero.id} className="mb-1 flex items-center justify-between gap-2 text-xs"><span>{heroName(hero)}</span><input type="number" min={0} max={12} value={mountVigourByHero[hero.id]||0} onChange={e=>setMountVigourByHero(current=>({...current,[hero.id]:Math.max(0,Number(e.target.value)||0)}))} className="w-16 rounded bg-black p-1"/></label>)}</div>}
119:           <button disabled={busy} onClick={finishJourney} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-emerald-200">Encerrar Jornada e aplicar Fadiga restante</button>
120:         </>}
121:       </div>
122:       <div className="mt-3 flex items-center gap-2"><input type="number" min={1} max={9} value={danger} onChange={e=>setDanger(Math.max(1,Number(e.target.value)||1))} className="w-16 rounded bg-black p-2"/><button disabled={busy} onClick={()=>void run([{type:'set_journey_danger',danger,reason:'Área Perigosa encontrada.'} as MasterCommand],'Área Perigosa '+danger)} className="rounded border border-red-900 px-3 py-2 text-xs text-red-200">Área Perigosa</button></div>
123:     </section>
124: 
125:     <section className="rounded-lg border border-violet-900/70 bg-zinc-950/80 p-4">
126:       <h2 className="text-sm font-black uppercase text-violet-200">Sombra</h2>
127:       <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
128:         <select value={selectedShadow?.id||''} onChange={e=>setShadowHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
129:         <select value={shadowSource} onChange={e=>setShadowSource(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2"><option value="dread">Pavor · Valor</option><option value="greed">Ganância · Sabedoria</option><option value="sorcery">Feitiçaria · Sabedoria</option><option value="transgression">Transgressão · sem teste</option></select>
130:         <input type="number" min={1} max={9} value={shadowAmount} onChange={e=>setShadowAmount(Math.max(1,Number(e.target.value)||1))} className="rounded border border-slate-700 bg-black p-2"/>
\`\`\`

### components/MechanicsHub.tsx — /Jornada/ — linha 62

\`\`\`text
34:   const [shadowHero,setShadowHero]=useState(characters[0]?.id||'');
35:   const [shadowSource,setShadowSource]=useState<'dread'|'greed'|'sorcery'|'transgression'>('dread');
36:   const [shadowAmount,setShadowAmount]=useState(1);
37:   const [progressHero,setProgressHero]=useState(characters[0]?.id||'');
38:   const [progressSkill,setProgressSkill]=useState('Viagem');
39:   const [progressProf,setProgressProf]=useState('Espadas');
40:   const [choice,setChoice]=useState('');
41:   const planned=masterState.journey.plannedRouteHexIds||[];
42:   const guide=characters.find(c=>c.id===masterState.company.journeyRoles.guide);
43:   const event=masterState.journey.pendingEvent;
44:   const eventTarget=event?characters.find(c=>c.id===masterState.company.journeyRoles[event.targetRole]):undefined;
45:   const selectedShadow=characters.find(c=>c.id===shadowHero)||characters[0];
46:   const selectedProgress=characters.find(c=>c.id===progressHero)||characters[0];
47:   const run=async(commands:MasterCommand[],reason:string)=>{
48:     setBusy(true);setMessage('');
49:     try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
50:     catch(error){setMessage(error instanceof Error?error.message:String(error));}
51:     finally{setBusy(false);}
52:   };
53:   const hopeCommands=(character:Character|undefined):MasterCommand[]=>{
54:     if(!character||!hope)return[];
55:     return [{type:'spend_hope',targetCharacterId:character.id,attribute:'heart',reason:'Esperança gasta na resolução mecânica.'} as MasterCommand];
56:   };
57:   const hopeDice=()=>hope?(inspired?2:1):0;
58: 
59:   const startJourney=()=>run([{
60:     type:'start_journey',destinationHexId:planned.at(-1)||masterState.journey.destinationHexId||null,
61:     roles:masterState.company.journeyRoles,routeHexIds:planned,mounted,forcedMarch,
62:     reason:'Jornada iniciada pela rota planejada do Atlas.',
63:   } as MasterCommand],'iniciar Jornada');
64: 
65:   const march=()=>{
66:     if(!guide){setMessage('Defina o Guia da Jornada na Companhia.');return;}
67:     const roll=rollSkill(guide,'Viagem',0,hopeDice());
68:     void run([...hopeCommands(guide),{type:'resolve_journey_march',success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Jogada de Marcha do Guia.'} as MasterCommand],
69:       'Jogada de Marcha: '+(roll.success?'sucesso':'falha')+'; '+(roll.tengwarCount||0)+' ícone(s)');
70:   };
71:   const generateEvent=()=>{
72:     const mode=region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
73:     void run([{type:'generate_journey_event',region,targetDie:secureRoll(1,6),featResults:featResults(mode),terrain:masterState.journey.segments.at(-1)?.terrain||'',reason:'Geração canônica de Evento de Jornada.'} as MasterCommand],'gerar Evento de Jornada');
74:   };
75:   const resolveEvent=()=>{
76:     if(!event||!eventTarget){setMessage('O papel alvo do Evento não possui Herói atribuído.');return;}
77:     const mod=Number(event.successDiceModifier||0);
78:     const roll=rollSkill(eventTarget,event.skill,mod,hopeDice());
79:     void run([...hopeCommands(eventTarget),{type:'resolve_journey_event',targetCharacterId:eventTarget.id,success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Resolução do Evento '+event.description+'.'} as MasterCommand],
80:       'Evento '+event.description+': '+(roll.success?'sucesso':'falha'));
81:   };
82:   const finishJourney=()=>{
83:     const endRolls:Record<string,{success:boolean;successIcons:number;mountVigour:number}>={};
84:     for(const hero of characters){
85:       const roll=rollSkill(hero,'Viagem');
86:       endRolls[hero.id]={success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,mountVigour:mounted?Math.max(0,Number(mountVigourByHero[hero.id])||0):0};
87:     }
88:     void run([{type:'complete_journey',endRolls,reason:'Encerramento canônico da Jornada: montaria e jogadas de Viagem reduzem Fadiga.'} as MasterCommand],'encerrar Jornada');
89:   };
90:   const shadowTest=()=>{
91:     if(!selectedShadow)return;
92:     const skillName=shadowSource==='dread'?'Valor':'Sabedoria';
93:     const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
94:     const favoured=shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'');
95:     const mode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':favoured?'favoured':'normal';
96:     const roll=shadowSource==='transgression'?null:rollOneRing(rating,0,14,skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));
97:     void run([{type:'resolve_shadow_gain',targetCharacterId:selectedShadow.id,amount:shadowAmount,source:shadowSource,testSuccess:Boolean(roll?.success),successIcons:Number(roll?.tengwarCount)||0,reason:'Ganho de Sombra: '+shadowSource} as MasterCommand],'resolver ganho de Sombra');
98:   };
99: 
100:   return <div className="space-y-4">
101:     <section className="rounded-lg border border-cyan-900/70 bg-zinc-950/80 p-4">
102:       <h2 className="text-sm font-black uppercase text-cyan-200">Jornada — sequência oficial</h2>
103:       <p className="mt-1 text-xs text-slate-400">Rota restante: {planned.length} hex(es) · etapa {masterState.journey.stageNumber||1} · {masterState.journey.day||0} dia(s) · marcha: {masterState.journey.marchHexesRemaining||0} hex(es).</p>
104:       <div className="mt-3 grid grid-cols-2 gap-2">
105:         {masterState.journey.status==='idle'||masterState.journey.status==='completed'
106:           ?<button disabled={busy||!planned.length} onClick={startJourney} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Iniciar pela rota</button>
107:           :<button disabled={busy||Boolean(event)||Boolean(masterState.journey.awaitingEventGeneration)||Boolean(masterState.journey.awaitingEndRoll)||Number(masterState.journey.marchHexesRemaining||0)>0} onClick={march} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Jogada de Marcha</button>}
108:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={hope} onChange={e=>setHope(e.target.checked)}/> Esperança</label>
109:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={inspired} disabled={!hope} onChange={e=>setInspired(e.target.checked)}/> Inspirado</label>
110:         {(masterState.journey.status==='idle'||masterState.journey.status==='completed')&&<>
111:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={mounted} onChange={e=>setMounted(e.target.checked)}/> Toda a Companhia montada</label>
112:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={forcedMarch} onChange={e=>setForcedMarch(e.target.checked)}/> Marcha Forçada</label>
113:         </>}
114:         {masterState.journey.awaitingEventGeneration&&<><select value={region} onChange={e=>setRegion(e.target.value as any)} className="rounded border border-slate-700 bg-black px-2 py-2 text-xs"><option value="border">Terra de Fronteira</option><option value="wild">Terra Erma</option><option value="dark">Terra Sombria</option></select><button disabled={busy} onClick={generateEvent} className="rounded border border-amber-700 px-3 py-2 text-amber-100">Gerar Evento</button></>}
115:         {event&&<button disabled={busy||!eventTarget} onClick={resolveEvent} className="col-span-2 rounded border border-amber-600 bg-amber-950/30 px-3 py-2 text-amber-100">Resolver {event.description} · {event.skill} · {eventTarget?heroName(eventTarget):'papel sem Herói'}</button>}
116:         {masterState.journey.stageComplete&&<button disabled={busy} onClick={()=>void run([{type:'continue_journey_stage',reason:'Nova etapa após 20 hexes.'} as MasterCommand],'continuar etapa da Jornada')} className="col-span-2 rounded border border-violet-700 px-3 py-2">Continuar próxima etapa</button>}
117:         {masterState.journey.awaitingEndRoll&&<>
118:           {Boolean(masterState.journey.mounted)&&<div className="col-span-2 rounded border border-emerald-900/60 p-2"><div className="mb-1 text-[10px] uppercase text-emerald-300">Vigor das montarias</div>{characters.map(hero=><label key={hero.id} className="mb-1 flex items-center justify-between gap-2 text-xs"><span>{heroName(hero)}</span><input type="number" min={0} max={12} value={mountVigourByHero[hero.id]||0} onChange={e=>setMountVigourByHero(current=>({...current,[hero.id]:Math.max(0,Number(e.target.value)||0)}))} className="w-16 rounded bg-black p-1"/></label>)}</div>}
119:           <button disabled={busy} onClick={finishJourney} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-emerald-200">Encerrar Jornada e aplicar Fadiga restante</button>
120:         </>}
121:       </div>
122:       <div className="mt-3 flex items-center gap-2"><input type="number" min={1} max={9} value={danger} onChange={e=>setDanger(Math.max(1,Number(e.target.value)||1))} className="w-16 rounded bg-black p-2"/><button disabled={busy} onClick={()=>void run([{type:'set_journey_danger',danger,reason:'Área Perigosa encontrada.'} as MasterCommand],'Área Perigosa '+danger)} className="rounded border border-red-900 px-3 py-2 text-xs text-red-200">Área Perigosa</button></div>
123:     </section>
124: 
125:     <section className="rounded-lg border border-violet-900/70 bg-zinc-950/80 p-4">
126:       <h2 className="text-sm font-black uppercase text-violet-200">Sombra</h2>
127:       <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
128:         <select value={selectedShadow?.id||''} onChange={e=>setShadowHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
129:         <select value={shadowSource} onChange={e=>setShadowSource(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2"><option value="dread">Pavor · Valor</option><option value="greed">Ganância · Sabedoria</option><option value="sorcery">Feitiçaria · Sabedoria</option><option value="transgression">Transgressão · sem teste</option></select>
130:         <input type="number" min={1} max={9} value={shadowAmount} onChange={e=>setShadowAmount(Math.max(1,Number(e.target.value)||1))} className="rounded border border-slate-700 bg-black p-2"/>
131:         <button disabled={busy||!selectedShadow} onClick={shadowTest} className="rounded border border-violet-700 px-3 py-2">Resolver Sombra</button>
132:         <button disabled={busy||!selectedShadow} onClick={()=>selectedShadow&&void run([{type:'firm_will',targetCharacterId:selectedShadow.id,reason:'Firmar Vontade.'} as MasterCommand],'Firmar Vontade')} className="rounded border border-slate-700 px-3 py-2">Firmar Vontade</button>
133:         <button disabled={busy||!selectedShadow||!shadowAtLimit(selectedShadow.sheet)} onClick={()=>selectedShadow&&void run([{type:'bout_of_madness',targetCharacterId:selectedShadow.id,reason:'Ataque de Loucura.'} as MasterCommand],'Ataque de Loucura')} className="rounded border border-red-800 px-3 py-2 text-red-200 disabled:opacity-40">Ataque de Loucura</button>
134:       </div>
135:     </section>
136: 
137:     <section className="rounded-lg border border-emerald-900/70 bg-zinc-950/80 p-4">
138:       <h2 className="text-sm font-black uppercase text-emerald-200">Fase de Sociedade e progressão</h2>
139:       <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
140:         {masterState.fellowshipPhase.status!=='active'?<>
141:           <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:false,reason:'Início da Fase de Sociedade.'} as MasterCommand],'iniciar Fase de Sociedade')} className="rounded border border-emerald-700 px-3 py-2">Iniciar</button>
142:           <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:true,reason:'Início da Fase de Sociedade de Yule.'} as MasterCommand],'iniciar Yule')} className="rounded border border-amber-700 px-3 py-2">Yule</button>
143:         </>:<button disabled={busy} onClick={()=>void run([{type:'end_fellowship_phase',reason:'Fim da Fase de Sociedade.'} as MasterCommand],'encerrar Fase de Sociedade')} className="col-span-2 rounded border border-red-900 px-3 py-2">Encerrar fase</button>}
144:         <select value={selectedProgress?.id||''} onChange={e=>setProgressHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
145:         <select value={progressSkill} onChange={e=>setProgressSkill(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{(selectedProgress?.sheet.skills||[]).map(s=><option key={s.name}>{s.name}</option>)}</select>
146:         <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_skill',targetCharacterId:selectedProgress.id,skillName:progressSkill,reason:'Treinamento na Fase de Sociedade.'} as MasterCommand],'elevar Perícia')} className="rounded border border-emerald-800 px-3 py-2">Elevar Perícia</button>
147:         <select value={progressProf} onChange={e=>setProgressProf(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{['Machados','Arcos','Lanças','Espadas'].map(x=><option key={x}>{x}</option>)}</select>
148:         <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_combat_proficiency',targetCharacterId:selectedProgress.id,group:progressProf,reason:'Treinamento de Proficiência.'} as MasterCommand],'elevar Proficiência')} className="rounded border border-emerald-800 px-3 py-2">Elevar Proficiência</button>
149:         <input value={choice} onChange={e=>setChoice(e.target.value)} placeholder="Recompensa ou Virtude escolhida" className="col-span-2 rounded border border-slate-700 bg-black p-2"/>
150:         <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_valor',targetCharacterId:selectedProgress.id,reward:choice,reason:'Elevação de Valor.'} as MasterCommand],'elevar Valor')} className="rounded border border-amber-800 px-3 py-2">Valor + Recompensa</button>
151:         <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_wisdom',targetCharacterId:selectedProgress.id,virtue:choice,reason:'Elevação de Sabedoria.'} as MasterCommand],'elevar Sabedoria')} className="rounded border border-cyan-800 px-3 py-2">Sabedoria + Virtude</button>
\`\`\`

### components/MechanicsHub.tsx — /Marcha/ — linha 68

\`\`\`text
40:   const [choice,setChoice]=useState('');
41:   const planned=masterState.journey.plannedRouteHexIds||[];
42:   const guide=characters.find(c=>c.id===masterState.company.journeyRoles.guide);
43:   const event=masterState.journey.pendingEvent;
44:   const eventTarget=event?characters.find(c=>c.id===masterState.company.journeyRoles[event.targetRole]):undefined;
45:   const selectedShadow=characters.find(c=>c.id===shadowHero)||characters[0];
46:   const selectedProgress=characters.find(c=>c.id===progressHero)||characters[0];
47:   const run=async(commands:MasterCommand[],reason:string)=>{
48:     setBusy(true);setMessage('');
49:     try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
50:     catch(error){setMessage(error instanceof Error?error.message:String(error));}
51:     finally{setBusy(false);}
52:   };
53:   const hopeCommands=(character:Character|undefined):MasterCommand[]=>{
54:     if(!character||!hope)return[];
55:     return [{type:'spend_hope',targetCharacterId:character.id,attribute:'heart',reason:'Esperança gasta na resolução mecânica.'} as MasterCommand];
56:   };
57:   const hopeDice=()=>hope?(inspired?2:1):0;
58: 
59:   const startJourney=()=>run([{
60:     type:'start_journey',destinationHexId:planned.at(-1)||masterState.journey.destinationHexId||null,
61:     roles:masterState.company.journeyRoles,routeHexIds:planned,mounted,forcedMarch,
62:     reason:'Jornada iniciada pela rota planejada do Atlas.',
63:   } as MasterCommand],'iniciar Jornada');
64: 
65:   const march=()=>{
66:     if(!guide){setMessage('Defina o Guia da Jornada na Companhia.');return;}
67:     const roll=rollSkill(guide,'Viagem',0,hopeDice());
68:     void run([...hopeCommands(guide),{type:'resolve_journey_march',success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Jogada de Marcha do Guia.'} as MasterCommand],
69:       'Jogada de Marcha: '+(roll.success?'sucesso':'falha')+'; '+(roll.tengwarCount||0)+' ícone(s)');
70:   };
71:   const generateEvent=()=>{
72:     const mode=region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
73:     void run([{type:'generate_journey_event',region,targetDie:secureRoll(1,6),featResults:featResults(mode),terrain:masterState.journey.segments.at(-1)?.terrain||'',reason:'Geração canônica de Evento de Jornada.'} as MasterCommand],'gerar Evento de Jornada');
74:   };
75:   const resolveEvent=()=>{
76:     if(!event||!eventTarget){setMessage('O papel alvo do Evento não possui Herói atribuído.');return;}
77:     const mod=Number(event.successDiceModifier||0);
78:     const roll=rollSkill(eventTarget,event.skill,mod,hopeDice());
79:     void run([...hopeCommands(eventTarget),{type:'resolve_journey_event',targetCharacterId:eventTarget.id,success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Resolução do Evento '+event.description+'.'} as MasterCommand],
80:       'Evento '+event.description+': '+(roll.success?'sucesso':'falha'));
81:   };
82:   const finishJourney=()=>{
83:     const endRolls:Record<string,{success:boolean;successIcons:number;mountVigour:number}>={};
84:     for(const hero of characters){
85:       const roll=rollSkill(hero,'Viagem');
86:       endRolls[hero.id]={success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,mountVigour:mounted?Math.max(0,Number(mountVigourByHero[hero.id])||0):0};
87:     }
88:     void run([{type:'complete_journey',endRolls,reason:'Encerramento canônico da Jornada: montaria e jogadas de Viagem reduzem Fadiga.'} as MasterCommand],'encerrar Jornada');
89:   };
90:   const shadowTest=()=>{
91:     if(!selectedShadow)return;
92:     const skillName=shadowSource==='dread'?'Valor':'Sabedoria';
93:     const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
94:     const favoured=shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'');
95:     const mode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':favoured?'favoured':'normal';
96:     const roll=shadowSource==='transgression'?null:rollOneRing(rating,0,14,skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));
97:     void run([{type:'resolve_shadow_gain',targetCharacterId:selectedShadow.id,amount:shadowAmount,source:shadowSource,testSuccess:Boolean(roll?.success),successIcons:Number(roll?.tengwarCount)||0,reason:'Ganho de Sombra: '+shadowSource} as MasterCommand],'resolver ganho de Sombra');
98:   };
99: 
100:   return <div className="space-y-4">
101:     <section className="rounded-lg border border-cyan-900/70 bg-zinc-950/80 p-4">
102:       <h2 className="text-sm font-black uppercase text-cyan-200">Jornada — sequência oficial</h2>
103:       <p className="mt-1 text-xs text-slate-400">Rota restante: {planned.length} hex(es) · etapa {masterState.journey.stageNumber||1} · {masterState.journey.day||0} dia(s) · marcha: {masterState.journey.marchHexesRemaining||0} hex(es).</p>
104:       <div className="mt-3 grid grid-cols-2 gap-2">
105:         {masterState.journey.status==='idle'||masterState.journey.status==='completed'
106:           ?<button disabled={busy||!planned.length} onClick={startJourney} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Iniciar pela rota</button>
107:           :<button disabled={busy||Boolean(event)||Boolean(masterState.journey.awaitingEventGeneration)||Boolean(masterState.journey.awaitingEndRoll)||Number(masterState.journey.marchHexesRemaining||0)>0} onClick={march} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Jogada de Marcha</button>}
108:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={hope} onChange={e=>setHope(e.target.checked)}/> Esperança</label>
109:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={inspired} disabled={!hope} onChange={e=>setInspired(e.target.checked)}/> Inspirado</label>
110:         {(masterState.journey.status==='idle'||masterState.journey.status==='completed')&&<>
111:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={mounted} onChange={e=>setMounted(e.target.checked)}/> Toda a Companhia montada</label>
112:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={forcedMarch} onChange={e=>setForcedMarch(e.target.checked)}/> Marcha Forçada</label>
113:         </>}
114:         {masterState.journey.awaitingEventGeneration&&<><select value={region} onChange={e=>setRegion(e.target.value as any)} className="rounded border border-slate-700 bg-black px-2 py-2 text-xs"><option value="border">Terra de Fronteira</option><option value="wild">Terra Erma</option><option value="dark">Terra Sombria</option></select><button disabled={busy} onClick={generateEvent} className="rounded border border-amber-700 px-3 py-2 text-amber-100">Gerar Evento</button></>}
115:         {event&&<button disabled={busy||!eventTarget} onClick={resolveEvent} className="col-span-2 rounded border border-amber-600 bg-amber-950/30 px-3 py-2 text-amber-100">Resolver {event.description} · {event.skill} · {eventTarget?heroName(eventTarget):'papel sem Herói'}</button>}
116:         {masterState.journey.stageComplete&&<button disabled={busy} onClick={()=>void run([{type:'continue_journey_stage',reason:'Nova etapa após 20 hexes.'} as MasterCommand],'continuar etapa da Jornada')} className="col-span-2 rounded border border-violet-700 px-3 py-2">Continuar próxima etapa</button>}
117:         {masterState.journey.awaitingEndRoll&&<>
118:           {Boolean(masterState.journey.mounted)&&<div className="col-span-2 rounded border border-emerald-900/60 p-2"><div className="mb-1 text-[10px] uppercase text-emerald-300">Vigor das montarias</div>{characters.map(hero=><label key={hero.id} className="mb-1 flex items-center justify-between gap-2 text-xs"><span>{heroName(hero)}</span><input type="number" min={0} max={12} value={mountVigourByHero[hero.id]||0} onChange={e=>setMountVigourByHero(current=>({...current,[hero.id]:Math.max(0,Number(e.target.value)||0)}))} className="w-16 rounded bg-black p-1"/></label>)}</div>}
119:           <button disabled={busy} onClick={finishJourney} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-emerald-200">Encerrar Jornada e aplicar Fadiga restante</button>
120:         </>}
121:       </div>
122:       <div className="mt-3 flex items-center gap-2"><input type="number" min={1} max={9} value={danger} onChange={e=>setDanger(Math.max(1,Number(e.target.value)||1))} className="w-16 rounded bg-black p-2"/><button disabled={busy} onClick={()=>void run([{type:'set_journey_danger',danger,reason:'Área Perigosa encontrada.'} as MasterCommand],'Área Perigosa '+danger)} className="rounded border border-red-900 px-3 py-2 text-xs text-red-200">Área Perigosa</button></div>
123:     </section>
124: 
125:     <section className="rounded-lg border border-violet-900/70 bg-zinc-950/80 p-4">
126:       <h2 className="text-sm font-black uppercase text-violet-200">Sombra</h2>
127:       <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
128:         <select value={selectedShadow?.id||''} onChange={e=>setShadowHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
129:         <select value={shadowSource} onChange={e=>setShadowSource(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2"><option value="dread">Pavor · Valor</option><option value="greed">Ganância · Sabedoria</option><option value="sorcery">Feitiçaria · Sabedoria</option><option value="transgression">Transgressão · sem teste</option></select>
130:         <input type="number" min={1} max={9} value={shadowAmount} onChange={e=>setShadowAmount(Math.max(1,Number(e.target.value)||1))} className="rounded border border-slate-700 bg-black p-2"/>
131:         <button disabled={busy||!selectedShadow} onClick={shadowTest} className="rounded border border-violet-700 px-3 py-2">Resolver Sombra</button>
132:         <button disabled={busy||!selectedShadow} onClick={()=>selectedShadow&&void run([{type:'firm_will',targetCharacterId:selectedShadow.id,reason:'Firmar Vontade.'} as MasterCommand],'Firmar Vontade')} className="rounded border border-slate-700 px-3 py-2">Firmar Vontade</button>
133:         <button disabled={busy||!selectedShadow||!shadowAtLimit(selectedShadow.sheet)} onClick={()=>selectedShadow&&void run([{type:'bout_of_madness',targetCharacterId:selectedShadow.id,reason:'Ataque de Loucura.'} as MasterCommand],'Ataque de Loucura')} className="rounded border border-red-800 px-3 py-2 text-red-200 disabled:opacity-40">Ataque de Loucura</button>
134:       </div>
135:     </section>
136: 
137:     <section className="rounded-lg border border-emerald-900/70 bg-zinc-950/80 p-4">
138:       <h2 className="text-sm font-black uppercase text-emerald-200">Fase de Sociedade e progressão</h2>
139:       <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
140:         {masterState.fellowshipPhase.status!=='active'?<>
141:           <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:false,reason:'Início da Fase de Sociedade.'} as MasterCommand],'iniciar Fase de Sociedade')} className="rounded border border-emerald-700 px-3 py-2">Iniciar</button>
142:           <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:true,reason:'Início da Fase de Sociedade de Yule.'} as MasterCommand],'iniciar Yule')} className="rounded border border-amber-700 px-3 py-2">Yule</button>
143:         </>:<button disabled={busy} onClick={()=>void run([{type:'end_fellowship_phase',reason:'Fim da Fase de Sociedade.'} as MasterCommand],'encerrar Fase de Sociedade')} className="col-span-2 rounded border border-red-900 px-3 py-2">Encerrar fase</button>}
144:         <select value={selectedProgress?.id||''} onChange={e=>setProgressHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
145:         <select value={progressSkill} onChange={e=>setProgressSkill(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{(selectedProgress?.sheet.skills||[]).map(s=><option key={s.name}>{s.name}</option>)}</select>
146:         <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_skill',targetCharacterId:selectedProgress.id,skillName:progressSkill,reason:'Treinamento na Fase de Sociedade.'} as MasterCommand],'elevar Perícia')} className="rounded border border-emerald-800 px-3 py-2">Elevar Perícia</button>
147:         <select value={progressProf} onChange={e=>setProgressProf(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{['Machados','Arcos','Lanças','Espadas'].map(x=><option key={x}>{x}</option>)}</select>
148:         <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_combat_proficiency',targetCharacterId:selectedProgress.id,group:progressProf,reason:'Treinamento de Proficiência.'} as MasterCommand],'elevar Proficiência')} className="rounded border border-emerald-800 px-3 py-2">Elevar Proficiência</button>
149:         <input value={choice} onChange={e=>setChoice(e.target.value)} placeholder="Recompensa ou Virtude escolhida" className="col-span-2 rounded border border-slate-700 bg-black p-2"/>
150:         <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_valor',targetCharacterId:selectedProgress.id,reward:choice,reason:'Elevação de Valor.'} as MasterCommand],'elevar Valor')} className="rounded border border-amber-800 px-3 py-2">Valor + Recompensa</button>
151:         <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_wisdom',targetCharacterId:selectedProgress.id,virtue:choice,reason:'Elevação de Sabedoria.'} as MasterCommand],'elevar Sabedoria')} className="rounded border border-cyan-800 px-3 py-2">Sabedoria + Virtude</button>
152:         <button disabled={busy||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'heal_shadow_scar',targetCharacterId:selectedProgress.id,reason:'Empreendimento Curar Cicatrizes.'} as MasterCommand],'Curar Cicatriz de Sombra')} className="col-span-2 rounded border border-violet-800 px-3 py-2">Curar Cicatriz · 5 PA · Yule</button>
153:         <button disabled={busy} onClick={()=>void run([{type:'record_undertaking',targetCharacterId:selectedProgress?.id||'',undertaking:'strengthen-fellowship',reason:'Empreendimento Fortalecer Sociedade.'} as MasterCommand],'Fortalecer Sociedade')} className="col-span-2 rounded border border-cyan-800 px-3 py-2">Empreendimento: Fortalecer Sociedade</button>
154:         <button disabled={busy} onClick={()=>void run([{type:'resolve_rest',kind:'long',safeRefuge:true,reason:'Descanso Prolongado em refúgio protegido e seguro.'} as MasterCommand],'Descanso em refúgio seguro')} className="col-span-2 rounded border border-blue-800 px-3 py-2">Descanso Prolongado em refúgio seguro · −1 Fadiga</button>
155:       </div>
156:     </section>
157:     {message&&<div className="rounded border border-slate-700 bg-black/60 p-3 text-xs text-slate-200">{message}</div>}
\`\`\`

### components/MechanicsHub.tsx — /Evento/ — linha 73

\`\`\`text
45:   const selectedShadow=characters.find(c=>c.id===shadowHero)||characters[0];
46:   const selectedProgress=characters.find(c=>c.id===progressHero)||characters[0];
47:   const run=async(commands:MasterCommand[],reason:string)=>{
48:     setBusy(true);setMessage('');
49:     try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
50:     catch(error){setMessage(error instanceof Error?error.message:String(error));}
51:     finally{setBusy(false);}
52:   };
53:   const hopeCommands=(character:Character|undefined):MasterCommand[]=>{
54:     if(!character||!hope)return[];
55:     return [{type:'spend_hope',targetCharacterId:character.id,attribute:'heart',reason:'Esperança gasta na resolução mecânica.'} as MasterCommand];
56:   };
57:   const hopeDice=()=>hope?(inspired?2:1):0;
58: 
59:   const startJourney=()=>run([{
60:     type:'start_journey',destinationHexId:planned.at(-1)||masterState.journey.destinationHexId||null,
61:     roles:masterState.company.journeyRoles,routeHexIds:planned,mounted,forcedMarch,
62:     reason:'Jornada iniciada pela rota planejada do Atlas.',
63:   } as MasterCommand],'iniciar Jornada');
64: 
65:   const march=()=>{
66:     if(!guide){setMessage('Defina o Guia da Jornada na Companhia.');return;}
67:     const roll=rollSkill(guide,'Viagem',0,hopeDice());
68:     void run([...hopeCommands(guide),{type:'resolve_journey_march',success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Jogada de Marcha do Guia.'} as MasterCommand],
69:       'Jogada de Marcha: '+(roll.success?'sucesso':'falha')+'; '+(roll.tengwarCount||0)+' ícone(s)');
70:   };
71:   const generateEvent=()=>{
72:     const mode=region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
73:     void run([{type:'generate_journey_event',region,targetDie:secureRoll(1,6),featResults:featResults(mode),terrain:masterState.journey.segments.at(-1)?.terrain||'',reason:'Geração canônica de Evento de Jornada.'} as MasterCommand],'gerar Evento de Jornada');
74:   };
75:   const resolveEvent=()=>{
76:     if(!event||!eventTarget){setMessage('O papel alvo do Evento não possui Herói atribuído.');return;}
77:     const mod=Number(event.successDiceModifier||0);
78:     const roll=rollSkill(eventTarget,event.skill,mod,hopeDice());
79:     void run([...hopeCommands(eventTarget),{type:'resolve_journey_event',targetCharacterId:eventTarget.id,success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Resolução do Evento '+event.description+'.'} as MasterCommand],
80:       'Evento '+event.description+': '+(roll.success?'sucesso':'falha'));
81:   };
82:   const finishJourney=()=>{
83:     const endRolls:Record<string,{success:boolean;successIcons:number;mountVigour:number}>={};
84:     for(const hero of characters){
85:       const roll=rollSkill(hero,'Viagem');
86:       endRolls[hero.id]={success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,mountVigour:mounted?Math.max(0,Number(mountVigourByHero[hero.id])||0):0};
87:     }
88:     void run([{type:'complete_journey',endRolls,reason:'Encerramento canônico da Jornada: montaria e jogadas de Viagem reduzem Fadiga.'} as MasterCommand],'encerrar Jornada');
89:   };
90:   const shadowTest=()=>{
91:     if(!selectedShadow)return;
92:     const skillName=shadowSource==='dread'?'Valor':'Sabedoria';
93:     const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
94:     const favoured=shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'');
95:     const mode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':favoured?'favoured':'normal';
96:     const roll=shadowSource==='transgression'?null:rollOneRing(rating,0,14,skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));
97:     void run([{type:'resolve_shadow_gain',targetCharacterId:selectedShadow.id,amount:shadowAmount,source:shadowSource,testSuccess:Boolean(roll?.success),successIcons:Number(roll?.tengwarCount)||0,reason:'Ganho de Sombra: '+shadowSource} as MasterCommand],'resolver ganho de Sombra');
98:   };
99: 
100:   return <div className="space-y-4">
101:     <section className="rounded-lg border border-cyan-900/70 bg-zinc-950/80 p-4">
102:       <h2 className="text-sm font-black uppercase text-cyan-200">Jornada — sequência oficial</h2>
103:       <p className="mt-1 text-xs text-slate-400">Rota restante: {planned.length} hex(es) · etapa {masterState.journey.stageNumber||1} · {masterState.journey.day||0} dia(s) · marcha: {masterState.journey.marchHexesRemaining||0} hex(es).</p>
104:       <div className="mt-3 grid grid-cols-2 gap-2">
105:         {masterState.journey.status==='idle'||masterState.journey.status==='completed'
106:           ?<button disabled={busy||!planned.length} onClick={startJourney} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Iniciar pela rota</button>
107:           :<button disabled={busy||Boolean(event)||Boolean(masterState.journey.awaitingEventGeneration)||Boolean(masterState.journey.awaitingEndRoll)||Number(masterState.journey.marchHexesRemaining||0)>0} onClick={march} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Jogada de Marcha</button>}
108:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={hope} onChange={e=>setHope(e.target.checked)}/> Esperança</label>
109:         <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={inspired} disabled={!hope} onChange={e=>setInspired(e.target.checked)}/> Inspirado</label>
110:         {(masterState.journey.status==='idle'||masterState.journey.status==='completed')&&<>
111:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={mounted} onChange={e=>setMounted(e.target.checked)}/> Toda a Companhia montada</label>
112:           <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={forcedMarch} onChange={e=>setForcedMarch(e.target.checked)}/> Marcha Forçada</label>
113:         </>}
114:         {masterState.journey.awaitingEventGeneration&&<><select value={region} onChange={e=>setRegion(e.target.value as any)} className="rounded border border-slate-700 bg-black px-2 py-2 text-xs"><option value="border">Terra de Fronteira</option><option value="wild">Terra Erma</option><option value="dark">Terra Sombria</option></select><button disabled={busy} onClick={generateEvent} className="rounded border border-amber-700 px-3 py-2 text-amber-100">Gerar Evento</button></>}
115:         {event&&<button disabled={busy||!eventTarget} onClick={resolveEvent} className="col-span-2 rounded border border-amber-600 bg-amber-950/30 px-3 py-2 text-amber-100">Resolver {event.description} · {event.skill} · {eventTarget?heroName(eventTarget):'papel sem Herói'}</button>}
116:         {masterState.journey.stageComplete&&<button disabled={busy} onClick={()=>void run([{type:'continue_journey_stage',reason:'Nova etapa após 20 hexes.'} as MasterCommand],'continuar etapa da Jornada')} className="col-span-2 rounded border border-violet-700 px-3 py-2">Continuar próxima etapa</button>}
117:         {masterState.journey.awaitingEndRoll&&<>
118:           {Boolean(masterState.journey.mounted)&&<div className="col-span-2 rounded border border-emerald-900/60 p-2"><div className="mb-1 text-[10px] uppercase text-emerald-300">Vigor das montarias</div>{characters.map(hero=><label key={hero.id} className="mb-1 flex items-center justify-between gap-2 text-xs"><span>{heroName(hero)}</span><input type="number" min={0} max={12} value={mountVigourByHero[hero.id]||0} onChange={e=>setMountVigourByHero(current=>({...current,[hero.id]:Math.max(0,Number(e.target.value)||0)}))} className="w-16 rounded bg-black p-1"/></label>)}</div>}
119:           <button disabled={busy} onClick={finishJourney} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-emerald-200">Encerrar Jornada e aplicar Fadiga restante</button>
120:         </>}
121:       </div>
122:       <div className="mt-3 flex items-center gap-2"><input type="number" min={1} max={9} value={danger} onChange={e=>setDanger(Math.max(1,Number(e.target.value)||1))} className="w-16 rounded bg-black p-2"/><button disabled={busy} onClick={()=>void run([{type:'set_journey_danger',danger,reason:'Área Perigosa encontrada.'} as MasterCommand],'Área Perigosa '+danger)} className="rounded border border-red-900 px-3 py-2 text-xs text-red-200">Área Perigosa</button></div>
123:     </section>
124: 
125:     <section className="rounded-lg border border-violet-900/70 bg-zinc-950/80 p-4">
126:       <h2 className="text-sm font-black uppercase text-violet-200">Sombra</h2>
127:       <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
128:         <select value={selectedShadow?.id||''} onChange={e=>setShadowHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
129:         <select value={shadowSource} onChange={e=>setShadowSource(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2"><option value="dread">Pavor · Valor</option><option value="greed">Ganância · Sabedoria</option><option value="sorcery">Feitiçaria · Sabedoria</option><option value="transgression">Transgressão · sem teste</option></select>
130:         <input type="number" min={1} max={9} value={shadowAmount} onChange={e=>setShadowAmount(Math.max(1,Number(e.target.value)||1))} className="rounded border border-slate-700 bg-black p-2"/>
131:         <button disabled={busy||!selectedShadow} onClick={shadowTest} className="rounded border border-violet-700 px-3 py-2">Resolver Sombra</button>
132:         <button disabled={busy||!selectedShadow} onClick={()=>selectedShadow&&void run([{type:'firm_will',targetCharacterId:selectedShadow.id,reason:'Firmar Vontade.'} as MasterCommand],'Firmar Vontade')} className="rounded border border-slate-700 px-3 py-2">Firmar Vontade</button>
133:         <button disabled={busy||!selectedShadow||!shadowAtLimit(selectedShadow.sheet)} onClick={()=>selectedShadow&&void run([{type:'bout_of_madness',targetCharacterId:selectedShadow.id,reason:'Ataque de Loucura.'} as MasterCommand],'Ataque de Loucura')} className="rounded border border-red-800 px-3 py-2 text-red-200 disabled:opacity-40">Ataque de Loucura</button>
134:       </div>
135:     </section>
136: 
137:     <section className="rounded-lg border border-emerald-900/70 bg-zinc-950/80 p-4">
138:       <h2 className="text-sm font-black uppercase text-emerald-200">Fase de Sociedade e progressão</h2>
139:       <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
140:         {masterState.fellowshipPhase.status!=='active'?<>
141:           <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:false,reason:'Início da Fase de Sociedade.'} as MasterCommand],'iniciar Fase de Sociedade')} className="rounded border border-emerald-700 px-3 py-2">Iniciar</button>
142:           <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:true,reason:'Início da Fase de Sociedade de Yule.'} as MasterCommand],'iniciar Yule')} className="rounded border border-amber-700 px-3 py-2">Yule</button>
143:         </>:<button disabled={busy} onClick={()=>void run([{type:'end_fellowship_phase',reason:'Fim da Fase de Sociedade.'} as MasterCommand],'encerrar Fase de Sociedade')} className="col-span-2 rounded border border-red-900 px-3 py-2">Encerrar fase</button>}
144:         <select value={selectedProgress?.id||''} onChange={e=>setProgressHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
145:         <select value={progressSkill} onChange={e=>setProgressSkill(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{(selectedProgress?.sheet.skills||[]).map(s=><option key={s.name}>{s.name}</option>)}</select>
146:         <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_skill',targetCharacterId:selectedProgress.id,skillName:progressSkill,reason:'Treinamento na Fase de Sociedade.'} as MasterCommand],'elevar Perícia')} className="rounded border border-emerald-800 px-3 py-2">Elevar Perícia</button>
147:         <select value={progressProf} onChange={e=>setProgressProf(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{['Machados','Arcos','Lanças','Espadas'].map(x=><option key={x}>{x}</option>)}</select>
148:         <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_combat_proficiency',targetCharacterId:selectedProgress.id,group:progressProf,reason:'Treinamento de Proficiência.'} as MasterCommand],'elevar Proficiência')} className="rounded border border-emerald-800 px-3 py-2">Elevar Proficiência</button>
149:         <input value={choice} onChange={e=>setChoice(e.target.value)} placeholder="Recompensa ou Virtude escolhida" className="col-span-2 rounded border border-slate-700 bg-black p-2"/>
150:         <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_valor',targetCharacterId:selectedProgress.id,reward:choice,reason:'Elevação de Valor.'} as MasterCommand],'elevar Valor')} className="rounded border border-amber-800 px-3 py-2">Valor + Recompensa</button>
151:         <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_wisdom',targetCharacterId:selectedProgress.id,virtue:choice,reason:'Elevação de Sabedoria.'} as MasterCommand],'elevar Sabedoria')} className="rounded border border-cyan-800 px-3 py-2">Sabedoria + Virtude</button>
152:         <button disabled={busy||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'heal_shadow_scar',targetCharacterId:selectedProgress.id,reason:'Empreendimento Curar Cicatrizes.'} as MasterCommand],'Curar Cicatriz de Sombra')} className="col-span-2 rounded border border-violet-800 px-3 py-2">Curar Cicatriz · 5 PA · Yule</button>
153:         <button disabled={busy} onClick={()=>void run([{type:'record_undertaking',targetCharacterId:selectedProgress?.id||'',undertaking:'strengthen-fellowship',reason:'Empreendimento Fortalecer Sociedade.'} as MasterCommand],'Fortalecer Sociedade')} className="col-span-2 rounded border border-cyan-800 px-3 py-2">Empreendimento: Fortalecer Sociedade</button>
154:         <button disabled={busy} onClick={()=>void run([{type:'resolve_rest',kind:'long',safeRefuge:true,reason:'Descanso Prolongado em refúgio protegido e seguro.'} as MasterCommand],'Descanso em refúgio seguro')} className="col-span-2 rounded border border-blue-800 px-3 py-2">Descanso Prolongado em refúgio seguro · −1 Fadiga</button>
155:       </div>
156:     </section>
157:     {message&&<div className="rounded border border-slate-700 bg-black/60 p-3 text-xs text-slate-200">{message}</div>}
158:   </div>;
159: };
\`\`\`

## Cross-reference

### eyeAwareness
- \`components\CompanyHubView.tsx:16\` — <article className="rounded border border-violet-900 bg-black/25 p-3"><Sparkles className="h-5 w-5 text-violet-400" /><b className="mt-2 block">Consciência do Olho</b><span className="text-sm text-slate-300">{company.eyeAwareness}</span></article>
- \`components\CompanyManager.tsx:37\` — eyeAwareness: Math.max(0, Number(draft.eyeAwareness) || 0),
- \`components\CompanyManager.tsx:52\` — <label className="text-slate-400">Vigilância do Olho<input type="number" min={0} value={draft.eyeAwareness} onChange={event => setDraft({ ...draft, eyeAwareness: Number(event.target.value) || 0 })} className="mt-1 w-full rounded border border-blue-900 bg-zinc-
- \`lib\master\state.ts:52\` — eyeAwareness: 0,
- \`lib\master\types.ts:89\` — eyeAwareness: number;
- \`android\app\src\main\assets\esm\components\CompanyHubView.js:7\` — return _jsxs("div", { className: "space-y-4", children: [_jsxs("section", { className: "medieval-panel p-4", children: [_jsxs("div", { className: "flex items-center gap-3", children: [_jsx(UsersRound, { className: "h-7 w-7 text-cyan-400" }), _jsxs("div", { chi
- \`android\app\src\main\assets\esm\components\CompanyManager.js:27\` — eyeAwareness: Math.max(0, Number(draft.eyeAwareness) || 0),
- \`android\app\src\main\assets\esm\components\CompanyManager.js:32\` — return (_jsxs("details", { className: "mt-3 rounded border border-blue-900/70 bg-black/25 p-3", children: [_jsx("summary", { className: "cursor-pointer list-none text-xs font-bold uppercase tracking-wider text-blue-300", children: _jsxs("span", { className: "i
- \`android\app\src\main\assets\esm\lib\master\state.js:47\` — eyeAwareness: 0,

### journeyRoles
- \`App.tsx:86\` — function autoJourneyRoles(party: Character[]): Partial<Record<JourneyRole,string>> {
- \`App.tsx:1325\` — const validRoles = Object.fromEntries(Object.entries(company.journeyRoles || {}).filter(([, id]) => Boolean(id) && party.has(String(id))));
- \`App.tsx:1331\` — company: { ...company, journeyRoles: validRoles, fellowshipFocus: validFocus },
- \`App.tsx:1405\` — const roles = autoJourneyRoles(party);
- \`App.tsx:1563\` — const targetId = master.company.journeyRoles[event.targetRole];
- \`App.tsx:1599\` — const guideId = master.company.journeyRoles.guide;
- \`components\CompanyManager.tsx:26\` — setDraft(current => ({ ...current, journeyRoles: { ...current.journeyRoles, [role]: characterId || undefined } }));
- \`components\CompanyManager.tsx:58\` — {ROLE_LABELS.map(([role, label]) => <label key={role} className="text-slate-400">{label}<select value={draft.journeyRoles[role] || ''} onChange={event => updateRole(role, event.target.value)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px
- \`components\MechanicsHub.tsx:42\` — const guide=characters.find(c=>c.id===masterState.company.journeyRoles.guide);
- \`components\MechanicsHub.tsx:44\` — const eventTarget=event?characters.find(c=>c.id===masterState.company.journeyRoles[event.targetRole]):undefined;
- \`components\MechanicsHub.tsx:61\` — roles:masterState.company.journeyRoles,routeHexIds:planned,mounted,forcedMarch,
- \`lib\master\commands.ts:8\` — import { completeJourney, continueJourneyStage, generateJourneyEvent, recordJourneyMove, resolveJourneyEvent, resolveJourneyMarch, setDangerousArea, startJourney, validateJourneyRoles } from '../rules/journey';
- \`lib\master\commands.ts:513\` — const roles = command.roles || nextState.company.journeyRoles;
- \`lib\master\commands.ts:514\` — const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
- \`lib\master\state.ts:53\` — journeyRoles: {},
- \`lib\master\state.ts:167\` — journeyRoles: incoming.company?.journeyRoles || {},
- \`lib\master\types.ts:90\` — journeyRoles: Partial<Record<JourneyRole, string>>;
- \`lib\rules\journey.ts:17\` — export function validateJourneyRoles(roles:Partial<Record<JourneyRole,string>>,partyIds:string[]):string[]{
- \`lib\rules\journey.ts:51\` — ...state,activeMode:'journey',company:{...state.company,journeyRoles:{...roles}},
- \`android\app\src\main\assets\esm\App.js:75\` — function autoJourneyRoles(party) {
- \`android\app\src\main\assets\esm\App.js:1316\` — const validRoles = Object.fromEntries(Object.entries(company.journeyRoles || {}).filter(([, id]) => Boolean(id) && party.has(String(id))));
- \`android\app\src\main\assets\esm\App.js:1322\` — company: { ...company, journeyRoles: validRoles, fellowshipFocus: validFocus },
- \`android\app\src\main\assets\esm\App.js:1395\` — const roles = autoJourneyRoles(party);
- \`android\app\src\main\assets\esm\App.js:1522\` — const targetId = master.company.journeyRoles[event.targetRole];
- \`android\app\src\main\assets\esm\App.js:1554\` — const guideId = master.company.journeyRoles.guide;
- \`android\app\src\main\assets\esm\components\CompanyManager.js:16\` — setDraft(current => ({ ...current, journeyRoles: { ...current.journeyRoles, [role]: characterId || undefined } }));
- \`android\app\src\main\assets\esm\components\CompanyManager.js:32\` — return (_jsxs("details", { className: "mt-3 rounded border border-blue-900/70 bg-black/25 p-3", children: [_jsx("summary", { className: "cursor-pointer list-none text-xs font-bold uppercase tracking-wider text-blue-300", children: _jsxs("span", { className: "i
- \`android\app\src\main\assets\esm\components\MechanicsHub.js:35\` — const guide = characters.find(c => c.id === masterState.company.journeyRoles.guide);
- \`android\app\src\main\assets\esm\components\MechanicsHub.js:37\` — const eventTarget = event ? characters.find(c => c.id === masterState.company.journeyRoles[event.targetRole]) : undefined;
- \`android\app\src\main\assets\esm\components\MechanicsHub.js:62\` — roles: masterState.company.journeyRoles, routeHexIds: planned, mounted, forcedMarch,
- \`android\app\src\main\assets\esm\lib\master\commands.js:7\` — import { completeJourney, continueJourneyStage, generateJourneyEvent, recordJourneyMove, resolveJourneyEvent, resolveJourneyMarch, setDangerousArea, startJourney, validateJourneyRoles } from '../rules/journey.js';
- \`android\app\src\main\assets\esm\lib\master\commands.js:541\` — const roles = command.roles || nextState.company.journeyRoles;
- \`android\app\src\main\assets\esm\lib\master\commands.js:542\` — const roleErrors = validateJourneyRoles(roles, Array.from(partyIds));
- \`android\app\src\main\assets\esm\lib\master\state.js:48\` — journeyRoles: {},
- \`android\app\src\main\assets\esm\lib\master\state.js:153\` — journeyRoles: incoming.company?.journeyRoles || {},
- \`android\app\src\main\assets\esm\lib\rules\journey.js:13\` — export function validateJourneyRoles(roles, partyIds) {
- \`android\app\src\main\assets\esm\lib\rules\journey.js:38\` — ...state, activeMode: 'journey', company: { ...state.company, journeyRoles: { ...roles } },

### awaitingEventGeneration
- \`App.tsx:1506\` — if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) {
- \`App.tsx:1550\` — if (master.journey.awaitingEventGeneration) {
- \`App.tsx:1583\` — if (master.journey.stageComplete && !master.journey.pendingEvent && !master.journey.awaitingEventGeneration) {
- \`App.tsx:1593\` — && !master.journey.awaitingEventGeneration
- \`components\MechanicsHub.tsx:107\` — :<button disabled={busy||Boolean(event)||Boolean(masterState.journey.awaitingEventGeneration)||Boolean(masterState.journey.awaitingEndRoll)||Number(masterState.journey.marchHexesRemaining||0)>0} onClick={march} className="rounded border border-cyan-700 px-3 py
- \`components\MechanicsHub.tsx:114\` — {masterState.journey.awaitingEventGeneration&&<><select value={region} onChange={e=>setRegion(e.target.value as any)} className="rounded border border-slate-700 bg-black px-2 py-2 text-xs"><option value="border">Terra de Fronteira</option><option value="wild">
- \`lib\master\state.ts:57\` — journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEnd
- \`lib\master\types.ts:139\` — awaitingEventGeneration?: boolean;
- \`lib\rules\journey.ts:56\` — awaitingEventGeneration:false,awaitingEndRoll:false,durationAdjustmentDays:0,mounted,forcedMarch,dangerEventsRemaining:0,travelFatigueByHero:{},
- \`lib\rules\journey.ts:64\` — if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
- \`lib\rules\journey.ts:109\` — plannedRouteHexIds:planned,marchHexesRemaining:marchRemaining,awaitingEventGeneration:needEvent,
- \`lib\rules\journey.ts:138\` — if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
- \`lib\rules\journey.ts:147\` — ...state.journey,awaitingEventGeneration:false,
- \`lib\rules\journey.ts:176\` — state:{...state,worldClock:shifted,journey:{...state.journey,pendingEvent:null,durationAdjustmentDays:adjustment,day:effective,dangerEventsRemaining:danger,awaitingEventGeneration:danger>0},updatedAt:Date.now()},
- \`lib\rules\journey.ts:182\` — return {...state,journey:{...state.journey,dangerEventsRemaining:value,awaitingEventGeneration:value>0,marchHexesRemaining:0},updatedAt:Date.now()};
- \`components\immersive\ImmersiveJourneyView.tsx:248\` — Jornada · rota {props.masterState.journey.plannedRouteHexIds?.length || 0} hex(es) · marcha {props.masterState.journey.marchHexesRemaining || 0} · fadiga pendente {Object.values(props.masterState.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + 
- \`android\app\src\main\assets\esm\App.js:1509\` — if (master.journey.awaitingEventGeneration) {
- \`android\app\src\main\assets\esm\App.js:1541\` — if (master.journey.stageComplete && !master.journey.pendingEvent && !master.journey.awaitingEventGeneration) {
- \`android\app\src\main\assets\esm\App.js:1549\` — && !master.journey.awaitingEventGeneration
- \`android\app\src\main\assets\esm\components\MechanicsHub.js:106\` — : _jsx("button", { disabled: busy || Boolean(event) || Boolean(masterState.journey.awaitingEventGeneration) || Boolean(masterState.journey.awaitingEndRoll) || Number(masterState.journey.marchHexesRemaining || 0) > 0, onClick: march, className: "rounded border 
- \`android\app\src\main\assets\esm\lib\master\state.js:52\` — journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEnd
- \`android\app\src\main\assets\esm\lib\rules\journey.js:43\` — awaitingEventGeneration: false, awaitingEndRoll: false, durationAdjustmentDays: 0, mounted, forcedMarch, dangerEventsRemaining: 0, travelFatigueByHero: {},
- \`android\app\src\main\assets\esm\lib\rules\journey.js:51\` — if (state.journey.pendingEvent || state.journey.awaitingEventGeneration)
- \`android\app\src\main\assets\esm\lib\rules\journey.js:100\` — plannedRouteHexIds: planned, marchHexesRemaining: marchRemaining, awaitingEventGeneration: needEvent,
- \`android\app\src\main\assets\esm\lib\rules\journey.js:135\` — if (!state.journey.awaitingEventGeneration && Number(state.journey.dangerEventsRemaining || 0) <= 0)
- \`android\app\src\main\assets\esm\lib\rules\journey.js:145\` — ...state.journey, awaitingEventGeneration: false,
- \`android\app\src\main\assets\esm\lib\rules\journey.js:182\` — state: { ...state, worldClock: shifted, journey: { ...state.journey, pendingEvent: null, durationAdjustmentDays: adjustment, day: effective, dangerEventsRemaining: danger, awaitingEventGeneration: danger > 0 }, updatedAt: Date.now() },
- \`android\app\src\main\assets\esm\lib\rules\journey.js:188\` — return { ...state, journey: { ...state.journey, dangerEventsRemaining: value, awaitingEventGeneration: value > 0, marchHexesRemaining: 0 }, updatedAt: Date.now() };

### pendingEvent
- \`App.tsx:1506\` — if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) {
- \`App.tsx:1561\` — const event = master.journey.pendingEvent;
- \`App.tsx:1583\` — if (master.journey.stageComplete && !master.journey.pendingEvent && !master.journey.awaitingEventGeneration) {
- \`App.tsx:1592\` — && !master.journey.pendingEvent
- \`components\MechanicsHub.tsx:43\` — const event=masterState.journey.pendingEvent;
- \`lib\master\state.ts:57\` — journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEnd
- \`lib\master\types.ts:119\` — pendingEvent?: {
- \`lib\rules\journey.ts:54\` — startedAt:Date.now(),day:0,segments:[],pendingEvent:null,
- \`lib\rules\journey.ts:64\` — if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
- \`lib\rules\journey.ts:148\` — pendingEvent:{
- \`lib\rules\journey.ts:161\` — const pending=state.journey.pendingEvent;
- \`lib\rules\journey.ts:176\` — state:{...state,worldClock:shifted,journey:{...state.journey,pendingEvent:null,durationAdjustmentDays:adjustment,day:effective,dangerEventsRemaining:danger,awaitingEventGeneration:danger>0},updatedAt:Date.now()},
- \`lib\rules\journey.ts:189\` — return {...state,activeMode:'adventure',journey:{...state.journey,status:'completed',destinationHexId:state.position.currentHexId,pendingEvent:null,awaitingEndRoll:false,marchHexesRemaining:0},updatedAt:Date.now()};
- \`components\immersive\ImmersiveJourneyView.tsx:248\` — Jornada · rota {props.masterState.journey.plannedRouteHexIds?.length || 0} hex(es) · marcha {props.masterState.journey.marchHexesRemaining || 0} · fadiga pendente {Object.values(props.masterState.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + 
- \`android\app\src\main\assets\esm\App.js:1520\` — const event = master.journey.pendingEvent;
- \`android\app\src\main\assets\esm\App.js:1541\` — if (master.journey.stageComplete && !master.journey.pendingEvent && !master.journey.awaitingEventGeneration) {
- \`android\app\src\main\assets\esm\App.js:1548\` — && !master.journey.pendingEvent
- \`android\app\src\main\assets\esm\components\MechanicsHub.js:36\` — const event = masterState.journey.pendingEvent;
- \`android\app\src\main\assets\esm\lib\master\state.js:52\` — journey: { status: 'idle', originHexId: null, destinationHexId: null, day: 0, segments: [], pendingEvent: null, plannedRouteHexIds: [], stageRouteHexIds: [], stageNumber: 1, marchHexesRemaining: 0, marchRollCount: 0, awaitingEventGeneration: false, awaitingEnd
- \`android\app\src\main\assets\esm\lib\rules\journey.js:41\` — startedAt: Date.now(), day: 0, segments: [], pendingEvent: null,
- \`android\app\src\main\assets\esm\lib\rules\journey.js:51\` — if (state.journey.pendingEvent || state.journey.awaitingEventGeneration)
- \`android\app\src\main\assets\esm\lib\rules\journey.js:146\` — pendingEvent: {
- \`android\app\src\main\assets\esm\lib\rules\journey.js:156\` — const pending = state.journey.pendingEvent;
- \`android\app\src\main\assets\esm\lib\rules\journey.js:182\` — state: { ...state, worldClock: shifted, journey: { ...state.journey, pendingEvent: null, durationAdjustmentDays: adjustment, day: effective, dangerEventsRemaining: danger, awaitingEventGeneration: danger > 0 }, updatedAt: Date.now() },
- \`android\app\src\main\assets\esm\lib\rules\journey.js:196\` — return { ...state, activeMode: 'adventure', journey: { ...state.journey, status: 'completed', destinationHexId: state.position.currentHexId, pendingEvent: null, awaitingEndRoll: false, marchHexesRemaining: 0 }, updatedAt: Date.now() };

### turn summary
- sem ocorrência

### FIM DO TURNO
- sem ocorrência

### fim do turno
- sem ocorrência

### processTurn(
- \`App.tsx:763\` — await processTurn(choice.text, false);
- \`App.tsx:779\` — await processTurn(isDmMode ? `[DÚVIDA PARA O MESTRE]: ${action}` : action, isDmMode);
- \`App.tsx:1362\` — await processTurn(`[DÚVIDA PARA O MESTRE]: ${question.trim()}`, true);
- \`App.tsx:1368\` — await processTurn(`[AÇÃO TÁTICA DA COMPANHIA] ${action.trim()}`, false);
- \`App.tsx:1715\` — await processTurn(
- \`App.tsx:2268\` — await processTurn(`[RESOLVER TESTE PENDENTE] ${originalChoice}`, false, base);
- \`App.tsx:2453\` — await processTurn(`[INICIAR AVENTURA GUIADA] A Companhia já está no hex ${discovery.currentHexId}; use exclusivamente o contexto cartográfico desse hex como ponto inicial.${companyText} Perfil: ${profile.label}. ${profile.prompt} ${openingInstruction(architect
- \`android\app\src\main\assets\esm\App.js:757\` — await processTurn(choice.text, false);
- \`android\app\src\main\assets\esm\App.js:775\` — await processTurn(isDmMode ? `[DÚVIDA PARA O MESTRE]: ${action}` : action, isDmMode);
- \`android\app\src\main\assets\esm\App.js:1355\` — await processTurn(`[DÚVIDA PARA O MESTRE]: ${question.trim()}`, true);
- \`android\app\src\main\assets\esm\App.js:1362\` — await processTurn(`[AÇÃO TÁTICA DA COMPANHIA] ${action.trim()}`, false);
- \`android\app\src\main\assets\esm\App.js:1661\` — await processTurn(`[TRECHO DE JORNADA JÁ RESOLVIDO PELO MOTOR] A Companhia acabou de chegar a ${move.locationName}. ${mechanicalSummary}. Não mova a Companhia novamente. Não preserve NPCs do hex anterior. Narre o trecho percorrido, a passagem real do tempo, qu
- \`android\app\src\main\assets\esm\App.js:2211\` — await processTurn(`[RESOLVER TESTE PENDENTE] ${originalChoice}`, false, base);
- \`android\app\src\main\assets\esm\App.js:2414\` — await processTurn(`[INICIAR AVENTURA GUIADA] A Companhia já está no hex ${discovery.currentHexId}; use exclusivamente o contexto cartográfico desse hex como ponto inicial.${companyText} Perfil: ${profile.label}. ${profile.prompt} ${openingInstruction(architect

### presentNpcIds
- \`App.tsx:199\` — recentEvents: '', clues: '', mysteries: '', quests: '', worldFlags: {}, presentNpcIds: [],
- \`App.tsx:270\` — presentNpcIds: Array.isArray(input.presentNpcIds) ? input.presentNpcIds : [],
- \`App.tsx:388\` — function updateCampaignMemory(base: CampaignState, node: SceneNode, events: CampaignEvent[], narrativeLog?: string): Pick<CampaignState, 'campaignSummary' | 'recentEvents' | 'clues' | 'presentNpcIds'> {
- \`App.tsx:397\` — return { campaignSummary, recentEvents, clues, presentNpcIds: Array.from(new Set([...(base.presentNpcIds || []), ...npcIds])) };
- \`App.tsx:527\` — const updated: CampaignState = { ...base, presentNpcIds: visible.map(item => item.id), updatedAt: Date.now() };
- \`App.tsx:592\` — setPresentNpcs(storedNpcs.filter(npc => active.presentNpcIds.includes(npc.id) || npc.presentInScene));
- \`App.tsx:642\` — setPresentNpcs(supervised.npcs.filter(npc => maintainedActive.presentNpcIds.includes(npc.id) || npc.presentInScene));
- \`App.tsx:674\` — setPresentNpcs(supervised.npcs.filter(npc => supervised.campaign.presentNpcIds.includes(npc.id) || npc.presentInScene));
- \`App.tsx:1028\` — const validatedPublicContext = `${publicContext}${sensoryContext}${finalRhythmContext}\n\n--- RESULTADO MECÂNICO VALIDADO LOCALMENTE ---\nModo: ${mechanicallyUpdated.masterState?.activeMode || 'adventure'}\nLocal canônico: ${mechanicallyUpdated.masterState?.po
- \`App.tsx:1153\` — canonicalIds: mechanicallyUpdated.presentNpcIds || [],
- \`App.tsx:1161\` — : mechanicallyUpdated.presentNpcIds || [];
- \`App.tsx:1164\` — mechanicallyUpdated = { ...mechanicallyUpdated, presentNpcIds: synchronizedNpcIds, updatedAt: Date.now() };
- \`App.tsx:1206\` — presentNpcIds: synchronizedNpcIds,
- \`App.tsx:1227\` — ? diversifySceneChoices(mechanicallyUpdated, newNode, response.choices || [], mechanicallyUpdatedNpcs.filter(npc => newNode.presentNpcIds.includes(npc.id)))
- \`App.tsx:1243\` — presentNpcIds: rememberedCampaign.presentNpcIds,
- \`App.tsx:1248\` — presentNpcIds: newNode.presentNpcIds,
- \`App.tsx:1289\` — setPresentNpcs(mechanicallyUpdatedNpcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
- \`App.tsx:1641\` — presentNpcIds: [],
- \`App.tsx:2501\` — setPresentNpcs(verifiedNpcs.filter(npc => persisted.presentNpcIds.includes(npc.id)));
- \`App.tsx:2635\` — setPresentNpcs(updatedNpcs.filter(npc => persisted.presentNpcIds.includes(npc.id)));
- \`App.tsx:2671\` — const sceneNpcs = allNpcs.filter(npc => (campaign.presentNpcIds || []).includes(npc.id) || npc.presentInScene);
- \`App.tsx:2674\` — case 'jogo': return <GameView campaignId={campaign.id} history={campaign.history} currentNode={campaign.history.at(-1) || null} currentImage={currentImage} isStoryLoading={isStoryLoading} isImageLoading={isImageLoading} isNarrationLoading={isNarrationLoading} 
- \`App.tsx:2689\` — default: return <GameView campaignId={campaign.id} history={campaign.history} currentNode={campaign.history.at(-1) || null} currentImage={currentImage} isStoryLoading={false} isImageLoading={false} isNarrationLoading={false} storyError={null} imageError={null}
- \`components\GameView.tsx:170\` — ? (activeNode?.presentNpcIds ? allNpcs.filter(n => activeNode.presentNpcIds.includes(n.id)) : [])
- \`lib\backup.ts:100\` — ...(campaign.presentNpcIds || []),
- \`lib\backup.ts:101\` — ...(campaign.history || []).flatMap(scene => scene.presentNpcIds || []),
- \`lib\backup.ts:164\` — ...(data.campaign.presentNpcIds || []),
- \`lib\backup.ts:165\` — ...(data.campaign.history || []).flatMap(scene => scene.presentNpcIds || []),
- \`lib\backup.ts:242\` — ...(current?.presentNpcIds || []),
- \`lib\backup.ts:243\` — ...(current?.history || []).flatMap(scene => scene.presentNpcIds || []),
- \`lib\barbaraBridge.ts:20\` — npcs.filter(npc => (campaign.presentNpcIds || []).includes(npc.id)).map((npc: any) => [npc.id, {
- \`lib\barbaraBridge.ts:57\` — facts: { current_location: campaign.currentLocation, present_npc_ids: [...(campaign.presentNpcIds || [])], supervisor_story_occasion: hostStoryOccasion },
- \`lib\consistency.ts:58\` — const presence = unique(campaign.presentNpcIds || []);
- \`lib\consistency.ts:163\` — ...(campaign.presentNpcIds || []),
- \`lib\consistency.ts:168\` — ? state.scene.participantNpcIds.filter(id => npcIds.has(id)) : campaign.presentNpcIds.filter(id => npcIds.has(id));
- \`lib\consistency.ts:170\` — campaign.presentNpcIds = presence;
- \`lib\contextBuilder.ts:272\` — NPCs presentes: ${(node.presentNpcIds || []).join(', ') || 'nenhum registrado'}
- \`lib\contextBuilder.ts:371\` — const activeNpcs = npcs.filter(npc => campaign.presentNpcIds?.includes(npc.id) || npc.presentInScene);
- \`lib\db.ts:52\` — presentNpcIds: string[];
- \`lib\db.ts:228\` — presentNpcIds: string[];

### participantNpcIds
- \`App.tsx:1652\` — participantNpcIds: [],
- \`components\MasterVirtualPanel.tsx:58\` — <div className="mt-2 flex items-center gap-2 text-[10px] text-slate-400"><Activity className="h-3.5 w-3.5" /> Tensão {scene.tension}/6 · {scene.participantHeroIds.length} heróis · {scene.participantNpcIds.length} NPCs</div>
- \`lib\consistency.ts:93\` — && presence.every(id => state.scene.participantNpcIds.includes(id))
- \`lib\consistency.ts:164\` — ...(state.scene.participantNpcIds || []),
- \`lib\consistency.ts:168\` — ? state.scene.participantNpcIds.filter(id => npcIds.has(id)) : campaign.presentNpcIds.filter(id => npcIds.has(id));
- \`lib\consistency.ts:171\` — state.scene.participantNpcIds = presence;
- \`lib\contextBuilder.ts:434\` — participantNpcIds: master.scene.participantNpcIds,
- \`lib\narrativeEngine.ts:137\` — ...(master?.scene?.participantNpcIds || []),
- \`lib\sceneContinuity.ts:42\` — ...(master?.scene?.participantNpcIds || []),
- \`services\geminiService.ts:202\` — participantNpcIds: { type: Type.ARRAY, items: { type: Type.STRING } },
- \`lib\autonomy\localFallbackDirector.ts:141\` — const presentIds = new Set([...(campaign.presentNpcIds || []), ...(campaign.masterState?.scene?.participantNpcIds || [])]);
- \`lib\master\commands.ts:197\` — nextState.scene = { ...nextState.scene, participantNpcIds: nextCampaign.presentNpcIds, updatedAt: Date.now() };
- \`lib\master\commands.ts:210\` — nextState.scene = { ...nextState.scene, participantNpcIds: Array.from(new Set([...nextState.scene.participantNpcIds, id])), updatedAt: Date.now() };
- \`lib\master\commands.ts:258\` — const npcIds = (command.participantNpcIds?.length ? command.participantNpcIds : nextCampaign.presentNpcIds || []).filter(id => validNpcs.has(id));
- \`lib\master\commands.ts:269\` — participantNpcIds: npcIds,
- \`lib\master\commands.ts:1009\` — participantNpcIds: Array.from(new Set(npcs)),
- \`lib\master\commandSanitizer.ts:140\` — const npcIds = (command.participantNpcIds || [])
- \`lib\master\commandSanitizer.ts:146\` — participantNpcIds: npcIds.length ? Array.from(new Set(npcIds)) : presentIds,
- \`lib\master\sceneMachine.ts:67\` — participantNpcIds: [],
- \`lib\master\state.ts:16\` — participantNpcIds: [],
- \`lib\master\state.ts:123\` — participantNpcIds: normalizeStringArray(incomingScene.participantNpcIds),
- \`lib\master\types.ts:20\` — participantNpcIds: string[];
- \`lib\master\types.ts:412\` — | { type: 'open_scene'; kind: SceneKind; title: string; objective: string; participantHeroIds?: string[]; participantNpcIds?: string[]; tension?: number; endCondition?: string; reason: string }
- \`android\app\src\main\assets\esm\App.js:1604\` — participantNpcIds: [],
- \`android\app\src\main\assets\esm\components\MasterVirtualPanel.js:16\` — return (_jsxs("section", { className: "rounded-md border-2 border-cyan-900/70 bg-slate-950/85 p-3 shadow-lg", children: [_jsxs("div", { className: "flex flex-wrap items-center justify-between gap-2 border-b border-cyan-950/70 pb-2", children: [_jsxs("div", { c
- \`android\app\src\main\assets\esm\lib\consistency.js:64\` — && presence.every(id => state.scene.participantNpcIds.includes(id))
- \`android\app\src\main\assets\esm\lib\consistency.js:127\` — ...(state.scene.participantNpcIds || []),
- \`android\app\src\main\assets\esm\lib\consistency.js:131\` — ? state.scene.participantNpcIds.filter(id => npcIds.has(id)) : campaign.presentNpcIds.filter(id => npcIds.has(id));
- \`android\app\src\main\assets\esm\lib\consistency.js:134\` — state.scene.participantNpcIds = presence;
- \`android\app\src\main\assets\esm\lib\contextBuilder.js:411\` — participantNpcIds: master.scene.participantNpcIds,
- \`android\app\src\main\assets\esm\lib\narrativeEngine.js:99\` — ...(master?.scene?.participantNpcIds || []),
- \`android\app\src\main\assets\esm\lib\sceneContinuity.js:17\` — ...(master?.scene?.participantNpcIds || []),
- \`android\app\src\main\assets\esm\services\geminiService.js:189\` — participantNpcIds: { type: Type.ARRAY, items: { type: Type.STRING } },
- \`android\app\src\main\assets\esm\lib\autonomy\localFallbackDirector.js:153\` — const presentIds = new Set([...(campaign.presentNpcIds || []), ...(campaign.masterState?.scene?.participantNpcIds || [])]);
- \`android\app\src\main\assets\esm\lib\master\commands.js:190\` — nextState.scene = { ...nextState.scene, participantNpcIds: nextCampaign.presentNpcIds, updatedAt: Date.now() };
- \`android\app\src\main\assets\esm\lib\master\commands.js:207\` — nextState.scene = { ...nextState.scene, participantNpcIds: Array.from(new Set([...nextState.scene.participantNpcIds, id])), updatedAt: Date.now() };
- \`android\app\src\main\assets\esm\lib\master\commands.js:263\` — const npcIds = (command.participantNpcIds?.length ? command.participantNpcIds : nextCampaign.presentNpcIds || []).filter(id => validNpcs.has(id));
- \`android\app\src\main\assets\esm\lib\master\commands.js:274\` — participantNpcIds: npcIds,
- \`android\app\src\main\assets\esm\lib\master\commands.js:1109\` — participantNpcIds: Array.from(new Set(npcs)),
- \`android\app\src\main\assets\esm\lib\master\commandSanitizer.js:132\` — const npcIds = (command.participantNpcIds || [])

### activeCharacterId
- \`App.tsx:195\` — activeCharacterId: null, partyCharacterIds: [],
- \`App.tsx:260\` — activeCharacterId: input.activeCharacterId || null,
- \`App.tsx:574\` — const selected = list.find(character => character.id === active.activeCharacterId) || list[0];
- \`App.tsx:580\` — activeCharacterId: selected.id,
- \`App.tsx:734\` — activeCharacterId: requestedCharacter?.id || base.activeCharacterId,
- \`App.tsx:738\` — events: [...(base.events || []), makeEvent('test_requested', undefined, { skill: test.skill, reason: test.reason, characterId: test.characterId || base.activeCharacterId || '' })],
- \`App.tsx:900\` — const declaredHero = characters.find(character => character.id === base.activeCharacterId);
- \`App.tsx:929\` — const requestedActorId = [plan.requiredTest?.characterId, plan.actingCharacterId, base.activeCharacterId, partyIds[0]]
- \`App.tsx:975\` — const sanitizedPlan = sanitizeDirectorCommands(transitionNormalized, base, allNpcs, requestedActorId || base.activeCharacterId || partyIds[0]);
- \`App.tsx:978\` — const actionHeroId = requestedActorId || base.activeCharacterId || partyIds[0];
- \`App.tsx:1015\` — const activeAfterMechanics = mechanicallyUpdatedCharacters.find(character => character.id === mechanicallyUpdated.activeCharacterId);
- \`App.tsx:1149\` — characterId: base.pendingDiceRoll!.characterId || base.pendingTest!.characterId || base.activeCharacterId || '',
- \`App.tsx:2234\` — characterId: roll.characterId || base.activeCharacterId || undefined, characterName: roll.characterName || base.characterSheet.identity.name,
- \`App.tsx:2283\` — activeCharacterId: selected.id,
- \`App.tsx:2305\` — let activeCharacterId = base.activeCharacterId;
- \`App.tsx:2308\` — if (!activeCharacterId || !partyCharacterIds.includes(activeCharacterId)) {
- \`App.tsx:2309\` — activeCharacterId = partyCharacterIds[0] || characterId;
- \`App.tsx:2310\` — const nextActive = list.find(character => character.id === activeCharacterId) || selected;
- \`App.tsx:2316\` — activeCharacterId,
- \`App.tsx:2326\` — const activeId = base.activeCharacterId || characters[0]?.id;
- \`App.tsx:2360\` — campaign: { ...base, activeCharacterId: character.id, partyCharacterIds: Array.from(new Set([...(base.partyCharacterIds || []), character.id])), characterSheet: sheet, updatedAt: now },
- \`App.tsx:2374\` — campaign: { ...base, activeCharacterId: character.id, partyCharacterIds: Array.from(new Set([...(base.partyCharacterIds || []), character.id])), characterSheet: character.sheet, updatedAt: Date.now() },
- \`App.tsx:2387\` — campaign: { ...base, activeCharacterId: character.id, partyCharacterIds: Array.from(new Set([...(base.partyCharacterIds || []), character.id])), characterSheet: character.sheet, updatedAt: Date.now() },
- \`App.tsx:2399\` — const campaignUpdate = base.activeCharacterId === character.id ? { ...base, characterSheet: updated.sheet, updatedAt: Date.now() } : { ...base, updatedAt: Date.now() };
- \`App.tsx:2412\` — const activeCharacterId = base.activeCharacterId && validIds.has(base.activeCharacterId) ? base.activeCharacterId : partyCharacterIds[0];
- \`App.tsx:2413\` — const activeCharacter = list.find(character => character.id === activeCharacterId) || list[0];
- \`App.tsx:2415\` — campaign: { ...base, activeCharacterId: activeCharacter.id, partyCharacterIds: Array.from(new Set([activeCharacter.id, ...partyCharacterIds])), characterSheet: normalizeCharacterSheet(activeCharacter.sheet), updatedAt: Date.now() },
- \`App.tsx:2674\` — case 'jogo': return <GameView campaignId={campaign.id} history={campaign.history} currentNode={campaign.history.at(-1) || null} currentImage={currentImage} isStoryLoading={isStoryLoading} isImageLoading={isImageLoading} isNarrationLoading={isNarrationLoading} 
- \`App.tsx:2675\` — case 'mestre': return <MasterHubView masterState={masterState} characters={partyCharacters} presentNpcs={sceneNpcs} activeCharacterId={campaign.activeCharacterId} onActiveCharacterChange={activateCharacter} onCompanySave={handleCompanySave} ruleAnswer={ruleAns
- \`App.tsx:2681\` — case 'personagens': return <CharacterRegistryView characters={characters} activeCharacterId={campaign.activeCharacterId} partyCharacterIds={campaign.partyCharacterIds || []} onSelectCharacter={activateCharacter} onToggleCharacterInGame={toggleCharacterInGame} 
- \`App.tsx:2682\` — case 'dados': return <DiceView onSendRollToMaster={handleSendRollToMaster} pendingRoll={campaign.pendingDiceRoll} onClearPendingRoll={handleClearPendingRoll} pendingTest={campaign.pendingTest} characters={characters.filter(character => (campaign.partyCharacter
- \`App.tsx:2689\` — default: return <GameView campaignId={campaign.id} history={campaign.history} currentNode={campaign.history.at(-1) || null} currentImage={currentImage} isStoryLoading={false} isImageLoading={false} isNarrationLoading={false} storyError={null} imageError={null}
- \`App.tsx:2697\` — <AppHeader characterSheet={campaign.characterSheet} characters={characters} activeCharacterId={campaign.activeCharacterId} partyCharacterIds={campaign.partyCharacterIds || []} onActiveCharacterChange={activateCharacter} locationName={campaign.currentLocation} 
- \`components\AppHeader.tsx:8\` — activeCharacterId?: string | null;
- \`components\AppHeader.tsx:23\` — activeCharacterId,
- \`components\AppHeader.tsx:72\` — value={activeCharacterId || ''}
- \`components\CharacterRegistryView.tsx:10\` — activeCharacterId?: string | null;
- \`components\CharacterRegistryView.tsx:23\` — activeCharacterId,
- \`components\CharacterRegistryView.tsx:74\` — const active = editing.id === activeCharacterId;
- \`components\CharacterRegistryView.tsx:127\` — const active = character.id === activeCharacterId;
