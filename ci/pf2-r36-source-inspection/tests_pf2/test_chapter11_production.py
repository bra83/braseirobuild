"""Source-level guard for authentic Kingmaker final-act campaign progression."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CH=ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters"

def chapter(name):
    return json.loads((CH/name).read_text(encoding="utf-8"))

def test_chapter_11_really_requires_nyrissa_resolution():
    before=chapter("chapter_09_sound_thousand_screams.json")
    after=chapter("chapter_10_curse_lantern_king.json")
    resolve=[x for x in before["quests"] if x.get("quest_key")=="q_redencao_espada_briar"]
    opening=[x for x in after["quests"] if x.get("quest_key")=="q_revelacao_rei_lanterna"]
    assert len(resolve)==len(opening)==1
    assert "q_redencao_espada_briar" in opening[0]["prerequisites"]
    assert "Kingmaker 2e pp. 480-484" in opening[0]["book_progression_source"]

def test_official_11_is_not_atlas_asset_11():
    end=chapter("chapter_10_curse_lantern_king.json")
    atlas=chapter("chapter_11_hex_atlas_pois.json")
    assert end["chapter_number"]==10
    assert atlas["chapter_number"]==11
    assert "atlas" in atlas["title"].casefold()
    assert any(x["quest_key"]=="q_revelacao_rei_lanterna" for x in end["quests"])
