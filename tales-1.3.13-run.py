from pathlib import Path
import re
import sys

repo = Path(__file__).resolve().parent
source_path = repo / "tales-1.3.13-transform.py"
source = source_path.read_text(encoding="utf-8")

# Fix the snapshot-v10 insertion against the actual 1.3.12 codec shape: the current
# decoder constructs KidState directly, so insert kidItems immediately before it.
pattern = re.compile(
    r'''# Insert decoded item list before KidState construction\.\nanchor = .*?\nt = replace_once\(t, anchor, insert, "snapshot decode items"\)''',
    re.DOTALL,
)
replacement = r'''# Insert decoded item list before KidState construction.
anchor = ''' + "'''    val kid = KidState(\n'''" + r'''
insert = '''    val kidItems = if (version >= 10) map["kid.items"].orEmpty().split('|')
      .filter { it.isNotBlank() }
      .mapNotNull { raw ->
        val parts = raw.split('~', limit = 4)
        if (parts.size != 4) null else {
          val kind = runCatching { KidItemKind.valueOf(parts[3]) }.getOrNull() ?: return@mapNotNull null
          val item = KidItemState(dec(parts[0]), dec(parts[1]), parts[2].toIntOrNull() ?: 0, kind)
          item.takeIf(CanonicalTalesRules::validItem)
        }
      } else emptyList()

    val kid = KidState(
'''
t = replace_once(t, anchor, insert, "snapshot decode items")'''

patched, count = pattern.subn(replacement, source, count=1)
if count != 1:
    raise SystemExit("Could not patch 1.3.13 snapshot insertion")

sys.argv = [str(source_path), *sys.argv[1:]]
exec(compile(patched, str(source_path), "exec"), {"__name__": "__main__", "__file__": str(source_path)})
