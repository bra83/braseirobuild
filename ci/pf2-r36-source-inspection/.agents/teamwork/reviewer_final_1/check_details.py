import json
import glob
import os
import re
import unicodedata

def strip_accents(text):
    return ''.join(c for c in unicodedata.normalize('NFD', text) if unicodedata.category(c) != 'Mn').lower()

def check_details():
    chapters_dir = os.path.abspath(r'android/app/src/main/assets/pathfinder/kingmaker/chapters')
    chapter_files = sorted(glob.glob(os.path.join(chapters_dir, 'chapter_*.json')))

    print("Checking schemas across chapters:")
    for cpath in chapter_files:
        cname = os.path.basename(cpath)
        with open(cpath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        print(f"  {cname}: schema='{data.get('schema')}'")

    print("\nChecking exact uppercase 'TODO', 'TBD', 'WIP', 'FIXME':")
    for cpath in chapter_files:
        cname = os.path.basename(cpath)
        with open(cpath, 'r', encoding='utf-8') as f:
            content = f.read()
        for marker in ['TODO', 'TBD', 'WIP', 'FIXME', 'PLACEHOLDER', 'Lorem ipsum', 'lorem ipsum']:
            # Search case-sensitive or word boundary for acronyms
            pattern = rf'\b{marker}\b'
            matches = re.findall(pattern, content)
            if matches:
                print(f"  FOUND {marker} in {cname}: count={len(matches)}")
                for m in re.finditer(pattern, content):
                    start = max(0, m.start() - 40)
                    end = min(len(content), m.end() + 40)
                    print(f"    Snippet: {content[start:end]}")

    print("\nChecking hazard skills with accent stripping:")
    valid_skills = {
        "ladroagem", "atletismo", "acrobacia", "religiao", "ocultismo", "arcanismo",
        "natureza", "percepcao", "sobrevivencia", "furtividade", "manufatura",
        "diplomacia", "intimidacao", "enganacao", "sociedade", "medicina", "desempenho"
    }
    for cpath in chapter_files:
        cname = os.path.basename(cpath)
        with open(cpath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        for h in data.get("hazards", []):
            for ds in h.get("disable_skills", []):
                norm_skill = strip_accents(ds.get("skill", ""))
                if norm_skill not in valid_skills:
                    print(f"  {cname} hazard {h.get('hazard_key')}: unrecognized normalized skill '{norm_skill}' (raw: '{ds.get('skill')}')")

if __name__ == '__main__':
    check_details()
