"""Regression checks for R36 kingdom upkeep consumption patch."""
import importlib.util
from pathlib import Path
import pytest

P=Path(__file__).with_name("patch_consumption_atomic.py")
spec=importlib.util.spec_from_file_location("patch_consumption_atomic",P)
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)

def setup(tmp_path):
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.37\n",encoding="utf-8")
    path=tmp_path/mod.REPO;path.parent.mkdir(parents=True)
    path.write_text("header\n"+mod.START+" // vulnerable}\n"+mod.END+" return null;}\n",encoding="utf-8")
    return path

def test_reject_free_upkeep(tmp_path):
    f=setup(tmp_path);assert mod.apply(tmp_path)
    s=f.read_text(encoding="utf-8")
    assert 'total>0&&!"food".equals(method)&&!"rp".equals(method)&&!"unrest".equals(method)' in s
    assert "Consumo positivo exige alimento, RP ou instabilidade" in s

def test_atomic_state_and_turn_writes(tmp_path):
    f=setup(tmp_path);mod.apply(tmp_path);s=f.read_text(encoding="utf-8")
    a=s.index("db.runInTransactionChecked")
    b=s.index('update("kingmaker_state"')
    c=s.index('update("kingmaker_turns"')
    d=s.index("        }});",a)
    assert a<b<c<d
    assert "changed!=1" in s and "turnChanged!=1" in s

def test_anarchy_recalculated(tmp_path):
    f=setup(tmp_path);mod.apply(tmp_path)
    assert 'values.put("anarchy",rules.isAnarchy(unrest)?1:0)' in f.read_text(encoding="utf-8")

def test_rp_overflow_and_invalid_method_guard(tmp_path):
    f=setup(tmp_path);mod.apply(tmp_path);s=f.read_text(encoding="utf-8")
    assert "if(total>Integer.MAX_VALUE/5)" in s
    assert 'throw new IllegalArgumentException("Consumo positivo exige' in s

def test_repeat_safe(tmp_path):
    f=setup(tmp_path);mod.apply(tmp_path);raw=f.read_bytes()
    assert not mod.apply(tmp_path) and f.read_bytes()==raw

def test_refuse_r35(tmp_path):
    f=setup(tmp_path);before=f.read_bytes()
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.36\n",encoding="utf-8")
    with pytest.raises(RuntimeError):mod.apply(tmp_path)
    assert f.read_bytes()==before

def test_refuse_unrecognized_source(tmp_path):
    f=setup(tmp_path);f.write_text("unknown Java",encoding="utf-8")
    with pytest.raises(ValueError):mod.apply(tmp_path)
