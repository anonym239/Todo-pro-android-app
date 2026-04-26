import os

# Fix all XML layout issues
replacements = {
    # Fix hintTextColor -> textColorHint
    'android:hintTextColor=': 'android:textColorHint=',
    # Fix layout_marginHorizontal -> use paddingStart/End (API 26+ should be fine with minSdk 31)
    # These are actually fine with minSdk 31, but let's check for other issues
}

dirs = [
    r'app/src/main/res/layout',
    r'app/src/main/res/drawable',
]

total = 0
for d in dirs:
    for root, _, files in os.walk(d):
        for fname in files:
            if fname.endswith('.xml'):
                fpath = os.path.join(root, fname)
                with open(fpath, 'r', encoding='utf-8') as f:
                    content = f.read()
                new_content = content
                for old, new in replacements.items():
                    new_content = new_content.replace(old, new)
                if new_content != content:
                    with open(fpath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f'Fixed: {fpath}')
                    total += 1

print(f'Total: {total}')

# Also add suppressUnsupportedCompileSdk to gradle.properties
props_path = 'gradle.properties'
with open(props_path, 'r', encoding='utf-8') as f:
    props = f.read()

if 'android.suppressUnsupportedCompileSdk' not in props:
    props += '\nandroid.suppressUnsupportedCompileSdk=35\n'
    with open(props_path, 'w', encoding='utf-8') as f:
        f.write(props)
    print('Added suppressUnsupportedCompileSdk to gradle.properties')
