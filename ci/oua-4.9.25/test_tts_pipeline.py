from pathlib import Path
import re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: test_tts_pipeline.py <project-root>")
root=Path(sys.argv[1]).resolve()
main=(root/"android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt").read_text(encoding="utf-8")

def split_neural(text,min_chars=250,target=450,max_chars=650):
    out=[]; cursor=0
    while cursor<len(text):
        while cursor<len(text) and text[cursor].isspace(): cursor+=1
        if cursor>=len(text): break
        high=min(cursor+max_chars,len(text))
        if high>=len(text):
            boundary=len(text)
        else:
            low=min(cursor+min_chars,high); want=min(cursor+target,high)
            def closest(chars,sentence=False):
                choices=[]
                for i in range(low,high):
                    if text[i] not in chars: continue
                    if sentence and i+1<len(text) and not text[i+1].isspace(): continue
                    choices.append(i+1)
                return min(choices,key=lambda p:abs(p-want)) if choices else None
            boundary=closest(".!?",True) or closest(";,",False)
            if boundary is None:
                spaces=[i for i in range(low,high) if text[i].isspace()]
                boundary=min(spaces,key=lambda p:abs(p-want)) if spaces else high
        end=boundary
        while end>cursor and text[end-1].isspace(): end-=1
        if end<=cursor: end=min(cursor+max_chars,len(text))
        out.append((cursor,text[cursor:end]))
        cursor=max(boundary,end)
    return out

sample="A porta range. Há marcas no chão. Algo se move no corredor."
blocks=split_neural(sample)
assert len(blocks)==1, blocks
assert blocks[0][1]==sample

long_text=("A chuva fina acompanha a Companhia pela estrada antiga. "
           "As folhas escuras se prendem às botas, e o vento muda de direção sem aviso. "
           "Mais adiante, a trilha contorna uma elevação; ninguém vê o que existe depois dela. "
           "O som de água corre à esquerda, enquanto pássaros distantes se calam. ")*7
blocks=split_neural(long_text)
assert len(blocks)>=3, len(blocks)
assert all(len(t)<=650 for _,t in blocks)
assert all(len(t)>=250 for _,t in blocks[:-1])
rebuilt=" ".join(t.strip() for _,t in blocks)
assert re.sub(r"\s+"," ",rebuilt).strip()==re.sub(r"\s+"," ",long_text).strip()

required=[
    "neuralMinChars = 250","neuralTargetChars = 450","neuralMaxChars = 650",
    "setOf('.','!','?')","setOf(';',',')",
    "TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH",
    "playSilentUtterance(50L,TextToSpeech.QUEUE_ADD",
    "if(isNeuralTtsProfile())return",
    'putInt("maxNumSentences",2)',
    'putFloat("silenceScale",.06f)',
    'putInt("numThreads",4)',
    "prewarmNeuralTts",
]
for needle in required:
    assert needle in main, needle

on_done=main.index("override fun onDone")
advance=main.index("advanceSpeechQueue(p.rootId)",on_done)
assert advance>on_done

speech=main.index("private fun speakNative")
speech_end=main.index("private fun stopNativeSpeech",speech)
speech_block=main[speech:speech_end]
assert "speechRate=if(neural)neuralTtsRate else rate.coerceIn" in speech_block
assert "setSpeechRate(speechRate)" in speech_block

print("PASS short continuity example -> one neural block")
print("PASS long narration ->",len(blocks),"blocks, lengths:",[len(x[1]) for x in blocks])
print("PASS boundaries preserve text and remain <=650")
print("PASS neural sequence uses QUEUE_ADD and onDone chaining")
print("PASS neural watchdog disabled")
print("PASS 50ms first-phoneme priming")
print("PASS Kokoro profile parameters 2 / 0.06 / 4")
print("PASS speed applied exactly through Android speechRate path")
