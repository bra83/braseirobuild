"""Regression gates: transactional phase changes and fail-closed turn completion."""
from pathlib import Path
import importlib.util
import pytest
spec=importlib.util.spec_from_file_location("phasepatch",Path(__file__).with_name("patch_turn_phase_atomic.py"))
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)

def setup(tmp_path):
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.37\n",encoding="utf-8")
    src=tmp_path/mod.SRC;src.parent.mkdir(parents=True)
    src.write_text("PREFIX\n"+mod.START+" vulnerable();}\n"+mod.END+"\n  "+mod.OLD_COMPLETE_PRE+"final JSONObject[] out={new JSONObject()};"+mod.OLD_COMPLETE_TX+" return;}});}\n",encoding="utf-8")
    return src

def test_phase_and_realm_update_share_transaction(tmp_path):
    f=setup(tmp_path);mod.apply(tmp_path);s=f.read_text(encoding="utf-8")
    method=s[s.index(mod.START):s.index(mod.END)]
    assert "db.runInTransactionChecked" in method
    assert 'update("kingmaker_turns"' in method and 'update("kingmaker_state"' in method
    assert "changed!=1" in method and "stateChanged!=1" in method

def test_no_early_double_completion(tmp_path):
    f=setup(tmp_path);mod.apply(tmp_path);s=f.read_text(encoding="utf-8")
    assert '"EVENT".equals(turn.optString("phase"))' in s
    assert '"active".equals(live.optString("status"))' in s
    assert '"consumption_resolved"' in s

def test_idempotent(tmp_path):
    f=setup(tmp_path);mod.apply(tmp_path);snap=f.read_bytes()
    assert mod.apply(tmp_path) is False and f.read_bytes()==snap

def test_r35_rejected(tmp_path):
    f=setup(tmp_path);snap=f.read_bytes()
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.36\n",encoding="utf-8")
    with pytest.raises(RuntimeError):mod.apply(tmp_path)
    assert f.read_bytes()==snap

def test_unrecognized_complete_refused(tmp_path):
    f=setup(tmp_path);f.write_text(mod.START+" x }"+mod.END+" y }",encoding="utf-8")
    with pytest.raises(ValueError):mod.apply(tmp_path)
