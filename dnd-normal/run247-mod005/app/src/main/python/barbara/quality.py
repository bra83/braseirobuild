import re


class NarrativeQualityGate:
    """Deterministic quality checks separated into canonical errors and style warnings.

    Canonical errors are safe reasons to reject provider prose and fall back without
    rolling back an already committed mechanical transaction. Style warnings are
    telemetry/presentation signals only.
    """
    META=(r'\bcomo ia\b',r'\bcomo modelo\b',r'\bsistema informa\b',r'\bstate\b.*\bjson\b',r'\bprompt do sistema\b')
    REPORT=(r'^(resumo|estado atual|situação atual|contexto)\s*:',r'^(summary|current state|current situation)\s*:')
    RULE_DUMP=(r'\b(dc|difficulty class|classe de dificuldade|target number|tn)\s*[:=]\s*\d+',r'\bregra\s+\d+[.:]')

    def inspect(self,text):
        if not isinstance(text,str) or not text.strip(): raise ValueError('invalid_narration')
        warnings=[]; errors=[]; low=text.lower()
        if any(re.search(p,low,re.I) for p in self.META):
            warnings.append('meta_language')
            errors.append('narrative_meta_language')
        if any(re.search(p,low,re.I) for p in self.REPORT): warnings.append('state_dump')
        if any(re.search(p,low,re.I) for p in self.RULE_DUMP): warnings.append('rule_dump')
        sentences=[x.strip() for x in re.split(r'(?<=[.!?])\s+|\n+',text) if x.strip()]
        if len(sentences)>=4 and len(set(sentences))<len(sentences)*.7: warnings.append('repetition')
        paragraphs=[p.strip() for p in re.split(r'\n\s*\n',text) if p.strip()]
        if len(text)>=500 and len(paragraphs)==1: warnings.append('monolithic_paragraph')
        score=max(0,100-30*len(errors)-12*len(warnings))
        return {'errors':errors,'warnings':warnings,'style_score':score,'paragraphs':len(paragraphs),'sentences':len(sentences)}
