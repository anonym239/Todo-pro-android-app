import os
import re

# Mapping: old attr name -> new attr name
replacements = {
    "?attr/colorBackground": "?attr/todoBgColor",
    "?attr/colorSurface": "?attr/todoSurfaceColor",
    "?attr/colorText": "?attr/todoTextColor",
    "?attr/colorHint": "?attr/todoHintColor",
    "?attr/colorDivider": "?attr/todoDividerColor",
    "?attr/colorEmptyTitle": "?attr/todoEmptyTitleColor",
    "?attr/colorCard": "?attr/todoCardColor",
    "?attr/colorInput": "?attr/todoInputColor",
}

# Directories to search
dirs = [
    r"c:\Users\Benutze\Desktop\test\app\src\main\res\layout",
    r"c:\Users\Benutze\Desktop\test\app\src\main\res\drawable",
]

total_changes = 0

for d in dirs:
    for root, dirs_list, files in os.walk(d):
        for fname in files:
            if fname.endswith(".xml"):
                fpath = os.path.join(root, fname)
                with open(fpath, "r", encoding="utf-8") as f:
                    content = f.read()
                
                new_content = content
                for old, new in replacements.items():
                    new_content = new_content.replace(old, new)
                
                if new_content != content:
                    with open(fpath, "w", encoding="utf-8") as f:
                        f.write(new_content)
                    print(f"Fixed: {fpath}")
                    total_changes += 1

print(f"\nTotal files changed: {total_changes}")
