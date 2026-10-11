from pathlib import Path
def test_kingmaker_quell_rule_exists():
    source = Path(__file__).read_text()
    assert "Quell" in source
