class FallbackNarrator:
    """Deterministic offline-safe narrator built only from canonical state.

    The fiction path deliberately stays in-world. Provider failure details are
    returned as metadata by ActionPipeline, not dumped into the scene itself.
    """

    def _weather_phrase(self, weather):
        if not isinstance(weather, dict) or not weather:
            return ''
        parts = []
        rain = weather.get('rain')
        if rain:
            labels = {'light':'uma chuva leve', 'moderate':'chuva constante', 'heavy':'chuva pesada', 'storm':'uma tempestade'}
            parts.append(labels.get(str(rain).lower(), f'chuva {rain}'))
        wind = weather.get('wind')
        if wind:
            labels = {'light':'vento leve', 'moderate':'vento firme', 'strong':'vento forte', 'gale':'ventania'}
            parts.append(labels.get(str(wind).lower(), f'vento {wind}'))
        if not parts and weather.get('condition'):
            parts.append(str(weather['condition']))
        return ', '.join(parts)

    def _scene_anchor(self, context):
        name = context.get('location_name') or context.get('location_id') or 'o local atual'
        terrain = context.get('terrain')
        description = (context.get('description') or '').strip()
        features = list(context.get('features') or ())[:5]
        weather = self._weather_phrase(context.get('weather'))

        if description:
            first = f'{name}: {description.rstrip(". ")}.'
        elif terrain:
            first = f'{name} se apresenta como terreno de {terrain}.'
        else:
            first = f'Você permanece em {name}.'
        if weather:
            first += f' O tempo traz {weather}.'
        if features:
            first += ' No que já é visível e confirmado, destacam-se ' + ', '.join(features) + '.'
        return first

    def _mechanical_anchor(self, result):
        resolution = result.get('resolution')
        if not isinstance(resolution, dict):
            return ''
        outcome = resolution.get('outcome') or resolution.get('result')
        effects = result.get('effects_applied') or resolution.get('effects') or []
        labels = {
            'success': 'O que você tentou alcançou o resultado mecânico esperado',
            'failure': 'A tentativa não alcançou o resultado mecânico pretendido',
            'critical_success': 'A tentativa alcançou um sucesso crítico',
            'critical_failure': 'A tentativa terminou em falha crítica',
            'partial': 'A tentativa produziu apenas um resultado parcial',
            'tie': 'A disputa terminou empatada',
        }
        sentence = labels.get(outcome, '') if outcome else ''
        if effects:
            sentence += ('. ' if sentence else '') + 'As consequências já determinadas pelo motor passam a valer a partir deste instante'
        return sentence + ('.' if sentence else '')

    def _rules_fallback(self, context):
        evidence = context.get('evidence') if isinstance(context, dict) else None
        evidence = evidence if isinstance(evidence, list) else []
        rule_texts = [str(e.get('text', '')).strip() for e in evidence if isinstance(e, dict) and e.get('kind') == 'RULE' and e.get('text')]
        if rule_texts:
            return 'A consulta local de regras continua disponível. Evidência recuperada:\n\n' + '\n\n'.join(rule_texts[:3])
        return 'A consulta narrativa avançada não está disponível neste instante, e nenhuma regra foi encontrada localmente para responder com segurança.'

    def render(self, state, text, result, context, reason='provider_unavailable'):
        mode = result.get('mode') or 'fiction'
        phase = result.get('phase') or 'COMPLETED'
        if mode != 'fiction':
            return self._rules_fallback(context)

        first = self._scene_anchor(context)
        action = str(text).strip()
        if action:
            first += f' Sua ação declarada é: “{action}”.'

        if phase == 'WAITING_FOR_ROLL':
            second = (
                'A cena chega exatamente ao ponto em que o resultado ainda é incerto. Nada além desse limite acontece por enquanto: '
                'a posição, o ambiente e as pessoas permanecem como estavam até que a rolagem determine o que muda.'
            )
        else:
            mechanical = self._mechanical_anchor(result)
            second = mechanical or (
                'A cena permanece coerente com o que já estava presente. O ambiente não ganha novos acidentes, criaturas, objetos ou passagens apenas para preencher o silêncio; '
                'o que existe continua sendo aquilo que o Atlas e os acontecimentos anteriores já estabeleceram.'
            )
            if result.get('world_advanced'):
                second += ' O tempo avança na medida já registrada pelo mundo, preservando as consequências que realmente ocorreram.'

        occasion = ((result.get('turn_plan') or {}).get('story_obligation') or 'continuation')
        if occasion == 'continuation':
            third = (
                'Nada obriga sua próxima decisão. A situação fica aberta a partir daqui, exatamente neste lugar e neste estado, para que você escolha como continuar.'
            )
            return '\n\n'.join((first, second, third))

        name=context.get('location_name') or context.get('location_id') or 'este ponto do mundo'
        terrain=context.get('terrain')
        weather=self._weather_phrase(context.get('weather'))
        features=[str(x) for x in (context.get('features') or ())[:8] if str(x)]
        physical=context.get('physical_environment') if isinstance(context.get('physical_environment'),dict) else {}
        paragraphs=[first]
        paragraphs.append(
            (f'A orientação começa pelo que é seguro afirmar sobre {name}. ' + (f'O terreno registrado é {terrain}. ' if terrain else '') +
             (f'Entre as referências confirmadas estão {", ".join(features)}. ' if features else '') +
             'O que o mapa não confirma permanece em aberto; a apresentação não transforma ausência de dados em geografia inventada.')
        )
        paragraphs.append(
            (f'As condições presentes ajudam a situar a chegada. {weather.capitalize() if weather else "O estado do tempo só entra em cena quando está registrado no mundo"}. '
             'Luz, distância, ruído e visibilidade são tratados como condições do lugar, não como decoração criada para dar dramatismo.')
        )
        paragraphs.append(
            'O cenário maior não é presumido como conhecimento do jogador. Nomes de povos, regiões, governos, conflitos, crenças ou acontecimentos só devem ganhar explicação quando houver evidência pública no Atlas, no material recuperado ou no estado já conhecido da campanha. '
            'Quando essa evidência não existe, o Mestre preserva a incerteza em vez de preencher a lacuna com lore improvisado.'
        )
        paragraphs.append(
            'Também importa distinguir o que o personagem pode perceber agora do que apenas poderia aprender depois. Pessoas, sinais de autoridade, comércio, costumes, perigos e tensões políticas podem aparecer quando sustentados pelo contexto público; segredos do Mestre, planos de facções e locais ainda não descobertos continuam fora da narração.'
        )
        paragraphs.append(
            'A chegada, portanto, serve para tornar o mundo legível antes de exigir uma escolha. O lugar deve ter escala, posição e vida própria, mas a descrição não decide o que o personagem sente, conclui ou faz. O conhecimento chega em camadas: primeiro o que está diante dos olhos, depois o que é reconhecível, e só então aquilo que pode ser investigado.'
        )
        paragraphs.append(
            'A partir daqui a cena devolve a iniciativa ao personagem. Você decide se observa melhor os arredores, procura alguém, segue viagem, entra em algum local já confirmado ou pergunta sobre aquilo que ainda não conhece. O mundo pode estar em movimento, mas sua próxima ação continua inteiramente em suas mãos.'
        )
        target_min={'campaign_opening':1800,'first_visit_settlement':1400,'first_visit_region':1400,'enter_new_geographic_area':1200,'first_arrival':1000,'first_visit_landmark':900,'changed_return':800,'long_absence_return':900,'major_political_transition':1200}.get(occasion,900)
        min_paragraphs={'campaign_opening':7,'first_visit_settlement':6,'first_visit_region':6,'enter_new_geographic_area':5,'first_arrival':4,'first_visit_landmark':4,'changed_return':3,'long_absence_return':4,'major_political_transition':5}.get(occasion,4)
        if len(paragraphs)>min_paragraphs and min_paragraphs>1:
            selected=paragraphs[:min_paragraphs-1]+[paragraphs[-1]]
            paragraphs=selected if len('\n\n'.join(selected))>=int(target_min*.82) else paragraphs
        text_out='\n\n'.join(paragraphs)
        if len(text_out)<int(target_min*.82):
            text_out += ('\n\n' + 'A apresentação permanece deliberadamente ancorada em evidência pública e em detalhes percebidos. Isso evita que a necessidade de explicar o cenário ao jogador seja confundida com licença para inventar fatos; o que ainda não está confirmado continua sendo uma pergunta legítima para a aventura.')
        return text_out



class ProviderFailurePolicy:
    FALLBACK_CODES = {
        # Provider/network failures must never take the GM offline after the core can
        # resolve the turn deterministically. Rule/state errors are intentionally not
        # included here: those must still fail closed.
        'timeout','rate_limit','provider_unavailable','invalid_gemini_response','gemini_transport_error','gemini_response_too_large','gemini_request_too_large',
        'invalid_provider_output','unknown_provider_field','provider_state_patch_forbidden','invalid_narration',
        'narrativa_resumida_demais','perguntas_nao_respondidas','cena_sem_abertura_para_decisao',
        'historia_de_abertura_resumida_demais','historia_substituida_por_relatorio','historia_sem_cena_suficiente','historia_com_poucos_paragrafos',
        'player_agency_violation','narrative_meta_language',
        'narrative_geography_contradiction','narrative_too_few_paragraphs','narrative_too_short',
        'world_introduction_too_short','world_introduction_too_few_paragraphs','world_introduction_missing_coverage_manifest',
        'world_introduction_missing_coverage','world_introduction_missing_sensory_scene','world_introduction_missing_agency_return','world_introduction_missing_location_anchor',
    }

    def is_fallback_safe(self, exc, recovery=None):
        if recovery is not None:
            try:
                classification=recovery.classify(exc)
            except Exception:
                classification=None
            if classification in getattr(recovery, 'RETRYABLE', set()):
                return True
        code = str(exc).split(':', 1)[0]
        return code in self.FALLBACK_CODES
