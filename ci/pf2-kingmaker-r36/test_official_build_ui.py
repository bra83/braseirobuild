"""R36 official 73-structure UI transition: synthetic source contract, not emulator."""
import importlib.util
import json
from pathlib import Path
import sys
import pytest
FILE=Path(__file__).with_name("patch_official_build_ui.py")
# A spec-loaded script is not automatically added to sys.path, unlike CLI
# execution. Resolve its sibling catalog module the same way on CI and locally.
sys.path.insert(0,str(FILE.parent))
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
    assert "RESOLVER CONSTRUÇÃO" in output
    assert 'new java.util.Random().nextInt(20)+1' in output
    assert '"d20 de construção"' in output
    assert 'officialKeys[position]' in output
    assert 'defaultSkills[position]' in output
    assert 'officialCosts.setText(officialDetails[position]);' in output
    assert 'DETAILS_TOKEN' not in output
    # Label arrays are emitted as Java-safe ASCII-escaped string literals.
    # Test the serialization actually produced rather than raw Unicode.
    assert json.dumps('Construção: ',ensure_ascii=True)[1:-1] in output
    assert "Pagar com Madeira" in output
    assert "Pagar com Pedra" in output
    assert 'n,skill,value(notes),commodityChoice' in output
    assert json.dumps('Automático (perícia oficial)',ensure_ascii=True)[1:-1] in output
    assert 'Construção oficial:' in output

def test_73_labels_are_from_official_pdf_not_free_text():
    rows=mod.official_manifest()
    output=mod.transform(source())
    assert len(rows)==73
    for name in (mod.key_for(r["official_name"]) for r in rows):
        assert '"'+name+'"' in output
    assert json.dumps('implantação especial',ensure_ascii=True)[1:-1] in output
    assert 'custo alternativo' in output
    assert "new String[]{" in output

def test_no_replay_or_untrusted_manual_form():
    patch=mod.transform(source())
    with pytest.raises(RuntimeError,match="twice"):
        mod.transform(patch)
    with pytest.raises(RuntimeError,match="anchor drift"):
        mod.transform(source().replace(mod.START,"private void removed("))

def test_missing_exact_legacy_route_fails_closed():
    with pytest.raises(RuntimeError,match="manual addStructure"):
        mod.transform(source().replace("kingmakerRepository.addStructure(","db.insert("))
