#!/usr/bin/env python3
"""Apply Gemini 3.8 voice transport to the REAL rebuilt R36 Android project.

Runs after the canonical R35 -> R36 patch chain. Fails closed whenever code differs:
never marks an inspection snapshot as installed code and never modifies the R35 ZIP.
"""
from pathlib import Path
import sys

def exactly_once(source: str, before: str, after: str, label: str) -> str:
    count = source.count(before)
    if count != 1:
        raise RuntimeError(f"TTS_V3_BLOCKED_{label}: expected 1 anchor, found {count}")
    return source.replace(before, after, 1)

def patch(source: str) -> str:
    if "BRASEIRO_TTS_V3_REAL_R36" in source:
        raise RuntimeError("TTS_V3_ALREADY_PATCHED")
    if "public SpeechRequest synthesizeSpeech(" not in source:
        raise RuntimeError("TTS_V3_BLOCKED_NO_REAL_GEMINI_SERVICE")
    old_prompt = "String ttsPrompt=NarrationPlaybackPolicy.charonPrompt(text,direction);"
    if old_prompt not in source:
        old_prompt = "String ttsPrompt=NarrationPlaybackPolicy.charonPrompt(text);"
    source = exactly_once(source, old_prompt,
        'String ttsPrompt=PortugueseNarrationSanitizer.sanitize(text==null?"":text).trim();',
        "TRANSCRIPT")
    old = 'return value.contains("gemini-3.1")&&value.contains("tts");'
    new = 'return (value.contains("gemini-3.1")||value.contains("gemini-3.8"))&&value.contains("tts");'
    source = exactly_once(source,old,new,"INTERACTIONS_ROUTE")

    old = '        body.put("input",ttsPrompt);\n        body.put("response_format",new JSONObject().put("type","audio"));'
    new = '''        // BRASEIRO_TTS_V3_REAL_R36: never put director instructions into speech.
        boolean metadata38=normalizeModel(model).startsWith("gemini-3.8-");
        if(metadata38){
            JSONObject style=new JSONObject().put("type","speech_metadata")
                    .put("style","natural Brazilian Portuguese, intimate and restrained");
            JSONObject segment=new JSONObject().put("type","text").put("text",ttsPrompt)
                    .put("annotations",new JSONArray().put(style));
            body.put("input",new JSONArray().put(new JSONObject()
                    .put("type","user_input").put("content",new JSONArray().put(segment))));
            body.put("response_format",new JSONObject().put("type","audio").put("mime_type","audio/wav"));
        }else{
            body.put("input",ttsPrompt);
            body.put("response_format",new JSONObject().put("type","audio"));
        }'''
    source = exactly_once(source,old,new,"INTERACTIONS_PAYLOAD")
    # Legacy generateContent always receives transcription only.
    old = 'JSONObject response=postTtsWithRetry(candidate,selectedKey,body,1,request);\n                            AudioData ad=extractAudio(response);'
    new = '''AudioData ad;
                            if(isInteractionsTtsModel(candidate)){
                                ad=synthesizeWithInteractions(candidate,selectedKey,ttsPrompt,voice,request);
                            }else{
                                JSONObject response=postTtsWithRetry(candidate,selectedKey,body,1,request);
                                ad=extractAudio(response);
                                if(ad==null)throw new Exception(apiError(response,"O modelo não retornou áudio."));
                            }'''
    source = exactly_once(source,old,new,"REAL_RUNTIME_CALL")
    # The original next statement used the old response variable, which is
    # now intentionally scoped only to the legacy-adapter branch.
    source = exactly_once(source,
        '                            if(ad==null)throw new Exception(apiError(response,"O modelo não retornou áudio."));\n'
        '                            if(audioLooksPrematurelyTruncated',
        '                            if(audioLooksPrematurelyTruncated',
        "LEGACY_RESPONSE_SCOPE")


    anchor = '            ordered.add("gemini-3.1-flash-tts-preview");'
    if "gemini-3.8-flash-tts" not in source:
        source = exactly_once(source,anchor,
            '            ordered.add("gemini-3.8-flash-tts");\n'
            '            ordered.add("gemini-3.8-flash-lite-tts");\n'+anchor,
            "MODEL_CANDIDATES")

    old = 'private static void writeWav(File file,byte[] pcm,int sampleRate)throws Exception{FileOutputStream out=new FileOutputStream(file);'
    new = '''private static void writeWav(File file,byte[] pcm,int sampleRate)throws Exception{FileOutputStream out=new FileOutputStream(file);
        // Gemini 3.8 unary returns WAV with an existing RIFF container.
        if(pcm.length>=12&&pcm[0]=='R'&&pcm[1]=='I'&&pcm[2]=='F'&&pcm[3]=='F'
                &&pcm[8]=='W'&&pcm[9]=='A'&&pcm[10]=='V'&&pcm[11]=='E'){
            try{out.write(pcm);}finally{out.close();}return;
        }'''
    source = exactly_once(source,old,new,"WAV_NOT_DOUBLE_WRAPPED")
    return source

def main():
    if len(sys.argv)!=2:
        raise SystemExit("usage: patch_tts_universal_v3.py REBUILT_R36_PROJECT")
    project=Path(sys.argv[1]).resolve()
    paths=[
        project/"android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java",
        project/"app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java",
    ]
    matches=[path for path in paths if path.is_file()]
    if len(matches)!=1:
        raise RuntimeError(f"TTS_V3_BLOCKED_REAL_ANDROID_SOURCE_PATH: {len(matches)}")
    original=matches[0].read_text(encoding="utf-8")
    updated=patch(original)
    if updated==original:
        raise RuntimeError("TTS_V3_BLOCKED_NO_CHANGES")
    matches[0].write_text(updated,encoding="utf-8")
    board_paths=[
        project/"android/app/src/main/java/com/braseiro/pathfinder2e/MapBoardView.java",
        project/"app/src/main/java/com/braseiro/pathfinder2e/MapBoardView.java",
    ]
    boards=[p for p in board_paths if p.is_file()]
    if len(boards)!=1:
        raise RuntimeError("R36_MAP_VIEW_NOT_FOUND")
    board=boards[0].read_text(encoding="utf-8")
    if "Typeface." in board and "import android.graphics.Typeface;" not in board:
        board=exactly_once(board,"import android.graphics.Paint;",
                           "import android.graphics.Paint;\nimport android.graphics.Typeface;",
                           "MAP_TYPEFACE_IMPORT")
        boards[0].write_text(board,encoding="utf-8")
        print("R36_TYPEFACE_IMPORT_FIXED=PASS")

    # Update the obsolete source-contract assertion, NOT disable the regression test.
    # The new assertions reject speaking director instructions and require typed metadata.
    contract=project/"tests_pf2/test_r23_narrative_tts_director.py"
    if not contract.is_file():
        raise RuntimeError("TTS_V3_BLOCKED_NARRATOR_TEST_MISSING")
    old=contract.read_text(encoding="utf-8")
    prior="    assert 'NarrationPlaybackPolicy.charonPrompt(text,direction)' in GEM"
    replacement="""    assert 'PortugueseNarrationSanitizer.sanitize(text==null?' in GEM
    assert 'NarrationPlaybackPolicy.charonPrompt(text,direction)' not in GEM
    assert 'speech_metadata' in GEM
    assert 'body.put("input",new JSONArray().put' in GEM"""
    if old.count(prior)!=1:
        raise RuntimeError("TTS_V3_BLOCKED_OLD_NARRATOR_TEST_CONTRACT_CHANGED")
    contract.write_text(old.replace(prior,replacement),encoding="utf-8")
    print("TTS_V3_HIDDEN_INSTRUCTIONS_TEST_UPDATED=PASS")
    print("TTS_V3_REAL_R36_APPLIED=PASS path="+str(matches[0]))

if __name__=="__main__":
    main()
