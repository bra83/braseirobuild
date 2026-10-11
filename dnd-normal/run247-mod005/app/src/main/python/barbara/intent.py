import re
from copy import deepcopy


class ActionClassifier:
    """Classifies player input before retrieval, rules, world advancement or narration.

    This layer does not resolve mechanics. It only decides which pipeline is allowed
    to consume the input. Mechanical uncertainty remains the exclusive authority of
    MechanicsAuthority/SystemAdapter.
    """

    RULES_QUERY = 'rules_query'
    DIALOGUE = 'dialogue'
    GAME_ACTION = 'game_action'
    META_COMMAND = 'meta_command'
    UI_COMMAND = 'ui_command'
    CHARACTER_THOUGHT = 'character_thought'

    _RULES = (
        r'^\s*(?:regra|regras|rules?)\b',
        r'^\s*(?:como funciona|como funcionam)\b',
        r'^\s*(?:how does|how do)\b.*\b(?:work|rule|rules)\b',
        r'^\s*(?:d[uú]vida|pergunta)\s+(?:de|sobre)\s+regra\b',
    )
    _META = (
        r'^\s*(?:ooc|meta|fora do jogo)\b',
        r'^\s*(?:planejo|planejamos|quero planejar|vamos planejar)\b',
        r'^\s*(?:i plan|we plan|let.?s plan)\b',
    )
    _UI = (
        r'^\s*/(?:ui|map|mapa|atlas|inventory|invent[aá]rio|sheet|ficha|journal|di[aá]rio)\b',
        r'^\s*(?:abrir|fechar|mostrar|ocultar)\s+(?:o\s+|a\s+)?(?:atlas|invent[aá]rio|ficha|di[aá]rio|menu)\s*$',
    )
    _THOUGHT = (
        r'^\s*(?:penso|considero|reflito|imagino|me pergunto)\b',
        r'^\s*(?:i think|i consider|i wonder|i reflect)\b',
    )
    _DIALOGUE = (
        r'^\s*(?:digo|falo|pergunto|respondo|sussurro|grito)\b',
        r'^\s*(?:i say|i ask|i tell|i answer|i whisper|i shout)\b',
    )

    def _matches(self, patterns, text):
        return any(re.search(pattern, text, re.I) for pattern in patterns)

    CHANNEL_PLAYER_ACTION = 'PLAYER_ACTION'
    CHANNEL_GM_HELP = 'GM_HELP'

    def classify(self, text, input_channel=None):
        if not isinstance(text, str) or not text.strip():
            raise ValueError('invalid_player_input')
        if input_channel not in {None, self.CHANNEL_PLAYER_ACTION, self.CHANNEL_GM_HELP}:
            raise ValueError('invalid_input_channel')
        low = text.strip().lower()
        if input_channel == self.CHANNEL_GM_HELP:
            kind = self.META_COMMAND
        elif input_channel == self.CHANNEL_PLAYER_ACTION:
            # The UI channel is authoritative. A question mark or rules-looking phrase
            # inside the action box must never suspend the fiction as GM help.
            kind = self.DIALOGUE if self._matches(self._DIALOGUE, low) else self.GAME_ACTION
        elif self._matches(self._UI, low):
            kind = self.UI_COMMAND
        elif self._matches(self._RULES, low):
            kind = self.RULES_QUERY
        elif self._matches(self._META, low):
            kind = self.META_COMMAND
        elif self._matches(self._THOUGHT, low):
            kind = self.CHARACTER_THOUGHT
        elif self._matches(self._DIALOGUE, low):
            kind = self.DIALOGUE
        else:
            kind = self.GAME_ACTION

        world_advances = kind in {self.GAME_ACTION, self.DIALOGUE}
        rules_only = kind == self.RULES_QUERY
        presentation = {
            'narrative': kind in {self.GAME_ACTION, self.DIALOGUE, self.CHARACTER_THOUGHT},
            'rules': rules_only,
            'help': kind in {self.RULES_QUERY, self.META_COMMAND, self.UI_COMMAND},
            'tts': kind in {self.GAME_ACTION, self.DIALOGUE},
        }
        return {
            'input_type': kind,
            'world_advances': world_advances,
            'rules_only': rules_only,
            'mechanics_allowed': kind == self.GAME_ACTION,
            'presentation': deepcopy(presentation),
            'input_channel': input_channel,
        }


class IntentCompiler:
    """Deterministically compiles classified natural language into a typed action envelope.

    It intentionally does not choose a difficulty, damage, target number or outcome.
    Those remain the system module / rules authority's responsibility.
    """

    _VERBS = (
        ('attack', ('ataco','atacar','golpeio','disparo','attack','shoot','strike')),
        ('move', ('vou','ando','caminho','corro','entro','saio','move','walk','run','enter','leave')),
        ('search', ('procuro','examino','investigo','vasculho','observo','search','examine','investigate','inspect')),
        ('interact', ('abro','fecho','pego','uso','empurro','puxo','open','close','take','use','push','pull')),
        ('speak', ('digo','falo','pergunto','respondo','say','ask','tell','answer')),
    )

    _SOCIAL_INTENTS = (
        ('persuade', ('convenço','convenco','persuado','persuadir','convencer','persuade','convince')),
        ('deceive', ('minto','engano','blefo','mentir','enganar','lie','deceive','bluff')),
        ('intimidate', ('intimido','ameaço','ameaco','intimidar','ameaçar','ameacar','intimidate','threaten')),
        ('bargain', ('barganho','negocio','negociar','pechincho','bargain','negotiate','haggle')),
    )

    def _social_intent(self, low, classification):
        if classification.get('input_type') != ActionClassifier.DIALOGUE:
            return None
        for name, words in self._SOCIAL_INTENTS:
            if any(re.search(r'\b'+re.escape(word)+r'\b', low, re.I) for word in words):
                return name
        basic_markers=('qual seu nome','como você se chama','como voce se chama','onde fica','que horas','what is your name','where is','who are you')
        if any(marker in low for marker in basic_markers):
            return 'basic_question'
        return 'conversation'

    def compile(self, text, classification=None):
        if not isinstance(text, str) or not text.strip():
            raise ValueError('invalid_player_input')
        classification = deepcopy(classification or ActionClassifier().classify(text))
        low = ' '.join(text.strip().lower().split())
        action = 'freeform'
        for name, words in self._VERBS:
            if any(re.search(r'\b'+re.escape(word)+r'\b', low, re.I) for word in words):
                action = name
                break
        quoted = re.findall(r'["“](.*?)["”]', text)
        social_intent=self._social_intent(low,classification)
        return {
            'intent_type': classification['input_type'],
            'action': action,
            'social_intent': social_intent,
            'normalized_text': low,
            'utterance': quoted[0] if quoted else None,
            'requires_rules_resolution': bool((classification.get('mechanics_allowed') and action in {'attack','move','search','interact'}) or social_intent in {'persuade','deceive','intimidate','bargain'}),
            'authority': 'rules_kernel',
            'mechanical_values': {},
        }
