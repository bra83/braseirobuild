"""R36 official 73-structure UI transition: synthetic source contract, not emulator."""
import importlib.util
from pathlib import Path
import pytest
FILE=Path(__file__).with_name("patch_official_build_ui.py")
s=importlib.util.spec_from_file_location("build_ui",FILE)
mod=importlib.util.module_from_spec(s)
s.loader.exec_module(mod)

def source():
    return (mod.START+"\n"
       '    kingmakerRepository.addStructure(a,b,c,d,e,f,g,h,i);\n'
       "    }\n"
       +mod.END+"{}\n")

def test_r35_manual_construction_is_replaced_by_rules_engine():
    output=mod.transform(source())
    assert "kingmakerRepository.addStructure(" not in output
    assert "kingmakerRepository.resolveBuildStructure(" in output
    assert "R36_OFFICIAL_BUILD_UI_RULES_ENGINE" in output
    assert "INICIAR" not in output or "RESOLVER CONSTRUÇÃO" in output
    assert 'new java.util.Random().nextInt(20)+1' in output
    assert '"d20 de construção"' in output
    assert 'officialKeys[position]' in output
    assert 'defaultSkills[position]' in output
    assert 'Automático (perícia oficial)' in output
    assert 'Construção oficial:' in output

def test_73_labels_are_from_official_pdf_not_free_text():
    rows=mod.official_manifest()
    output=mod.transform(source())
    assert len(rows)==73
    for name in (mod.key_for(r["official_name"]) for r in rows):
        assert '"'+name+'"' in output
    assert 'implantação especial' in output
    assert 'custo alternativo' in output
    assert "0 RP" not in output or '"DUMP' in output

def test_no_replay_or_untrusted_manual_form():
    patch=mod.transform(source())
    with pytest.raises(RuntimeError,match="twice"):
        mod.transform(patch)
    with pytest.raises(RuntimeError,match="anchor drift"):
        mod.transform(source().replace(mod.START,"private void removed("))

def test_missing_exact_legacy_route_fails_closed():
    with pytest.raises(RuntimeError,match="manual addStructure"):
        mod.transform(source().replace("kingmakerRepository.addStructure(","db.insert("))
