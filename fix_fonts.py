import os

# Remove font references from all XML files - replace with system fonts
replacements = {
    'android:fontFamily="@font/inter_regular"': 'android:fontFamily="sans-serif"',
    'android:fontFamily="@font/inter_bold"': 'android:fontFamily="sans-serif-medium"',
    'android:fontFamily="@font/inter_black"': 'android:fontFamily="sans-serif-black"',
    'app:fontFamily="@font/inter_regular"': 'app:fontFamily="sans-serif"',
    'app:fontFamily="@font/inter_bold"': 'app:fontFamily="sans-serif-medium"',
    'app:fontFamily="@font/inter_black"': 'app:fontFamily="sans-serif-black"',
}

dirs = [
    r"c:\Users\Benutze\Desktop\test\app\src\main\res\layout",
    r"c:\Users\Benutze\Desktop\test\app\src\main\res\drawable",
]

total = 0
for d in dirs:
    for root, _, files in os.walk(d):
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
                    print(f"Fixed fonts: {fpath}")
                    total += 1

print(f"Total: {total}")

# Delete the broken font XML files (they reference non-existent .ttf files)
font_dir = r"c:\Users\Benutze\Desktop\test\app\src\main\res\font"
for fname in os.listdir(font_dir):
    if fname.endswith(".xml"):
        fpath = os.path.join(font_dir, fname)
        os.remove(fpath)
        print(f"Deleted: {fpath}")
