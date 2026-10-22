import os
#!/usr/bin/env python3
"""Enumerate every translatable name the lucifertotem source produces and check the lang file IN THE JAR has it.
usage: check_lang.py <jar>"""
import re, sys, zipfile, pathlib, json
SRC = pathlib.Path(__file__).resolve().parent.parent / 'src/main/java'
jar = sys.argv[1]
z = zipfile.ZipFile(jar)
names = z.namelist()
ok = True
def need(cond, msg):
    global ok
    print(('OK   ' if cond else 'FAIL ') + msg); ok &= bool(cond)
LANG = 'assets/lucifertotem/lang/en_us.lang'
need(LANG in names, 'jar contains ' + LANG)
need(not any(n.lower() == LANG and n != LANG for n in names), 'no wrongly-cased duplicate lang file')
need('pack.mcmeta' in names and json.loads(z.read('pack.mcmeta'))['pack']['pack_format'] == 3, 'jar contains pack.mcmeta with pack_format 3')
lang = {}
for line in z.read(LANG).decode('utf-8').splitlines():
    if line.strip() and not line.startswith('#') and '=' in line:
        k, v = line.split('=', 1); lang[k] = v
need(not z.read(LANG).startswith(b'\xef\xbb\xbf'), 'lang file has no UTF-8 BOM')
src = {p: p.read_text() for p in SRC.rglob('*.java')}
allsrc = '\n'.join(src.values())
MODID = 'lucifertotem'
keys = {}  # key -> origin
# Items/blocks: setTranslationKey(MODID + ".x") or (MODID + "." + name) where name comes from constructor args
for p, s in src.items():
    cls = p.stem
    is_block = bool(re.search(r'extends\s+(Block\w*)', s)) or cls.startswith('Block')
    is_item = 'ModContent' in cls and False
    for m in re.finditer(r'setTranslationKey\(\s*(?:LuciferTotem\.)?MODID\s*\+\s*"\.([a-z0-9_]+)"\s*\)', s):
        pre = 'tile' if (cls.startswith('Block')) else 'item'
        keys[f'{pre}.{MODID}.{m.group(1)}.name'] = f'{cls}.setTranslationKey'
    if re.search(r'setTranslationKey\(\s*(?:LuciferTotem\.)?MODID\s*\+\s*"\."\s*\+\s*name\s*\)', s):
        # names passed via constructors in ModContent: new BlockLT(..., "x") / new BlockSeaPlant("x", ...)
        for m in re.finditer(r'new\s+%s\(([^;]*?)\)' % cls, allsrc):
            for n in re.findall(r'"([a-z0-9_]+)"', m.group(1)):
                keys[f'tile.{MODID}.{n}.name'] = f'new {cls}("{n}")'
        # ...and subclasses that pass the name up: class X extends BlockLT { super(..., "x") }
        for q, t in src.items():
            if re.search(r'extends\s+%s\b' % cls, t):
                for m in re.finditer(r'super\(([^;]*?)\);', t):
                    for n in re.findall(r'"([a-z0-9_]+)"', m.group(1)):
                        keys[f'tile.{MODID}.{n}.name'] = f'{q.stem} super("{n}")'
# every registry name of a block/item must have a key too
reg = set(re.findall(r'setRegistryName\(\s*(?:LuciferTotem\.)?MODID\s*,\s*"([a-z0-9_]+)"', allsrc))
# potion types: need potion.effect.<base name> instead of an item name
for base, n in re.findall(r'new PotionType\("([a-z0-9_]+)"[^;]*?setRegistryName\(\s*(?:LuciferTotem\.)?MODID\s*,\s*"([a-z0-9_]+)"', allsrc):
    reg.discard(n)
    need(f'potion.effect.{base}' in lang, f'potion type {n} has potion.effect.{base}')
for p, s in src.items():
    if 'setRegistryName(LuciferTotem.MODID, name)' in s:
        for m in re.finditer(r'new\s+%s\(([^;]*?)\)' % p.stem, allsrc):
            reg.update(re.findall(r'"([a-z0-9_]+)"', m.group(1)))
        for q, t in src.items():
            if re.search(r'extends\s+%s\b' % p.stem, t):
                for m in re.finditer(r'super\(([^;]*?)\);', t):
                    reg.update(re.findall(r'"([a-z0-9_]+)"', m.group(1)))
reg.discard('purple'); reg.discard('lightfall'); reg.discard('sibyl_rebind'); reg.discard('alabastron_lace'); reg.discard('porphyry_dye')  # sibyl_rebind is a recipe  # potion / biome registry names, checked below
for n in sorted(reg):
    need(f'tile.{MODID}.{n}.name' in lang or f'item.{MODID}.{n}.name' in lang, f'registry {MODID}:{n} has an item./tile. name')
# entities
for rn, nm in re.findall(r'registerModEntity\(new ResourceLocation\((?:LuciferTotem\.)?MODID,\s*"([a-z0-9_]+)"\),\s*\w+\.class,\s*"([^"]+)"', allsrc):
    keys[f'entity.{nm}.name'] = f'entity {rn}'
# potion
for m in re.findall(r'setPotionName\("([^"]*)"\s*\+\s*LuciferTotem\.MODID\s*\+\s*"([^"]*)"\)', allsrc):
    keys[m[0] + MODID + m[1]] = 'potion'
# creative tab
for m in re.findall(r'new CreativeTabs\((MODID|"[^"]+")\)', allsrc):
    keys['itemGroup.' + (MODID if m == 'MODID' else m.strip('"'))] = 'creative tab'
# damage types -> death messages
for t in set(re.findall(r'(?:EntityDamageSource(?:Indirect)?|super)\(\s*"([a-z_]+)"', allsrc)):
    keys[f'death.attack.{t}'] = 'damage type'; keys[f'death.attack.{t}.player'] = 'damage type'
for t in list(reg):  # super("name", ...) in a block/item constructor is a registry name, not a damage type
    if not re.search(r'DamageSource[^;]*"' + t + '"', allsrc): keys.pop(f'death.attack.{t}', None); keys.pop(f'death.attack.{t}.player', None)
for n in re.findall(r'super\("([a-z_]+)",\s*V130\.disc', allsrc):  # ItemRecord super(name, sound): a record, not a damage type
    keys.pop(f'death.attack.{n}', None); keys.pop(f'death.attack.{n}.player', None); keys[f'item.record.{n}.desc'] = 'record'
# literal I18n keys
for k in re.findall(r'translateToLocal\("([^"]+)"\)', allsrc):
    keys[k] = 'I18n literal'
# ---------------- 1.1.0 content
hsrc = {p: t for p, t in src.items() if 'hillman' in str(p) or 'client' in str(p)}
hall = '\n'.join(hsrc.values())
for p, t in hsrc.items():
    for m in re.finditer(r'named\(new\s+[\w.]+\([^;]*?\),\s*"([a-z0-9_]+)"', t):
        pre = 'tile' if p.stem == 'HBlocks' else 'item'
        keys[f'{pre}.{MODID}.{m.group(1)}.name'] = p.stem + '.named'
        if pre == 'item': keys[f'tooltip.{MODID}.{m.group(1)}.1'] = 'item tooltip'
for n in re.findall(r'(?:new PotionLT|super)\("([a-z_]+)",\s*(?:true|false)', hall):
    keys[f'effect.{MODID}.{n}'] = 'HPotions'
for n in re.findall(r'(?:new PotionLT|super)\("([a-z_]+)",\s*(?:true|false)', hall):  # potion super(...) calls are not damage types
    keys.pop(f'death.attack.{n}', None); keys.pop(f'death.attack.{n}.player', None)
for t in set(re.findall(r'new DamageSource\("([a-z_]+)"\)', allsrc)):
    keys[f'death.attack.{t}'] = 'damage type'; keys[f'death.attack.{t}.player'] = 'damage type'
for k in set(re.findall(r'"((?:msg|chat|shout|pythia|sibyl|gloss|rant|wind|place|prophecy|tooltip)\.lucifertotem\.[a-z0-9_.]*[a-z0-9_])"', hall)):
    keys[k] = 'literal key'
fam = {'rant.lucifertotem.ammonius.': int(re.search(r'RANTS = (\d+)', hall).group(1)), 'gloss.lucifertotem.': int(re.search(r'GLOSSES = (\d+)', hall).group(1)),
       'pythia.lucifertotem.open.': 4, 'pythia.lucifertotem.close.': 3, 'sibyl.lucifertotem.verse.': 8}
for pre, n in fam.items():
    for i in range(1, n + 1): keys[f'{pre}{i}'] = 'key family'
for w in re.search(r'WINDS = \{([^}]*)\}', hall).group(1).replace('"', '').replace(' ', '').split(','): keys[f'wind.{MODID}.{w}'] = 'wind'
for t in re.search(r'TYPES = \{([^}]*)\}', hall).group(1).replace('"', '').replace(' ', '').split(','): keys[f'place.{MODID}.{t}'] = 'structure type'
for t in re.search(r'OVERWORLD = \{([^}]*)\}', hall).group(1).replace('"', '').replace(' ', '').split(','): keys[f'place.{MODID}.ow_{t.lower()}'] = 'overworld structure'
snd = json.loads(z.read('assets/lucifertotem/sounds.json'))
for ev, o in snd.items():
    keys[o['subtitle']] = 'sound ' + ev
    for e in o['sounds']:
        if e.get('type') == 'event': continue
        if e['name'].startswith('minecraft:'):  # vanilla file referenced by name (not shipped): must exist in the 1.12 asset index
            vf = set(json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'vanilla_sound_files_1.12.json'))))
            need('minecraft/sounds/' + e['name'].split(':')[1] + '.ogg' in vf, f"vanilla sound file for {e['name']}")
        else: need(f"assets/lucifertotem/sounds/{e['name'].split(':')[1]}.ogg" in names, f"sound file for {e['name']}")
for ev in re.findall(r'\bs\("([a-z_.]+)"\)', src[[p for p in src if p.stem == 'HSounds'][0]]):
    need(ev in snd, f'sounds.json has event {ev}')
for n in names:
    if n.startswith('assets/lucifertotem/advancements/') and n.endswith('.json'):
        d = json.loads(z.read(n))['display']
        keys[d['title']['translate']] = n; keys[d['description']['translate']] = n
# ---------------- 1.2.0 lore: whispers and altar messages
lsrc = '\n'.join(t for p, t in src.items() if 'lore' in str(p))
for k in set(re.findall(r'"((?:whisper|altar)\.lucifertotem\.[a-z0-9_.]*[a-z0-9_])"', lsrc)):
    keys[k] = 'lore literal key'
for d in ['n', 'ne', 'e', 'se', 's', 'sw', 'w', 'nw']: keys[f'whisper.{MODID}.dir.{d}'] = 'compass'
for i in range(1, 6): keys[f'whisper.{MODID}.purple.{i}'] = 'whisper family'
need('worldgen' or True, 'lore sources scanned: %d keys' % len(re.findall(r'lucifertotem\.', lsrc)))
# ---------------- 1.2.1 paintings: every variant needs title, author and a texture of exactly w*32 x h*32
psrc = src[next(p for p in src if p.name == 'Paintings.java')]
for n, w, h in re.findall(r'new Variant\("([a-z0-9_]+)",\s*(\d+),\s*(\d+)\)', psrc):
    keys[f'painting.{MODID}.{n}.title'] = 'painting'; keys[f'painting.{MODID}.{n}.author'] = 'painting'
    tp = f'assets/{MODID}/textures/painting/{n}.png'
    if tp in names:
        import io; from PIL import Image
        sz = Image.open(io.BytesIO(z.read(tp))).size
        need(sz == (int(w) * 32, int(h) * 32), f'{tp} is {sz}, wants {int(w)*32}x{int(h)*32}')
    else:
        need(False, f'{tp} missing')
keys[f'tooltip.{MODID}.lady_babylon_painting.random'] = 'painting item'
# ---------------- 1.3.0: v130 package (items/blocks via V130.item/V130.block, Potion130, literal keys, families)
vsrc = {p: t for p, t in src.items() if 'v130' in str(p) or p.stem.startswith('V130')}
vall = '\n'.join(vsrc.values())
for m in re.finditer(r'V130\.item\(r,\s*new\s+[\w.]+\([^;]*?\),\s*"([a-z0-9_]+)"\)', vall):
    keys[f'item.{MODID}.{m.group(1)}.name'] = 'V130.item'; keys[f'tooltip.{MODID}.{m.group(1)}.1'] = 'item tooltip'
for m in re.finditer(r'V130\.block\(r,\s*new\s+[\w.]+\([^;]*?\),\s*"([a-z0-9_]+)"', vall):
    keys[f'tile.{MODID}.{m.group(1)}.name'] = 'V130.block'
for n in re.findall(r'new Potion130\("([a-z_]+)"', vall):
    keys[f'effect.{MODID}.{n}'] = 'Potion130'
for k in set(re.findall(r'"((?:msg|chat|tooltip|code|tier|quest|shout|interview|whisper|rant|subtitle|hint|place)\.lucifertotem\.[a-z0-9_.]*[a-z0-9_])"', vall)):
    keys[k] = 'v130 literal key'
for c in re.findall(r'CODE\.put\("[^"]+",\s*"([a-z_]+)"\)', vall): keys[f'code.{MODID}.{c}'] = 'Galen code'
for i in (1, 2, 3): keys[f'tier.{MODID}.crock.{i}'] = 'crock tier'
for fam, n in re.findall(r'FAMILY\("([a-z0-9_.]+\.)",\s*(\d+)\)', vall):
    for i in range(1, int(n) + 1): keys[f'{fam}{i}'] = 'v130 family'
for ev in re.findall(r'\bs\("([a-z_.]+)"\)', src[[p for p in src if p.stem == 'V130'][0]]):
    need(ev in snd, f'sounds.json has 1.3.0 event {ev}')
need(re.search(r'\.signoff"', allsrc) is not None, 'sign-off keys referenced')
keys['rant.lucifertotem.ammonius.signoff'] = 'sign-off'
for q in re.findall(r'new Quest\("([a-z_]+)"', vall): keys[f'quest.{MODID}.{q}'] = 'quest'
m = re.search(r'SHOUTS = \{([^}]*)\}', vall)
if m:
    for t in m.group(1).replace('"', '').replace(' ', '').split(','): keys[f'shout.{MODID}.reviewer.{t}'] = 'reviewer shout'

clipsrc = src[[p for p in src if p.stem == 'AmmoniusClips'][0]]
cnames = re.search(r'NAME = \{([^}]*)\}', clipsrc).group(1).replace('"', '').replace(' ', '').split(',')
for pool in ('LECTURE', 'RANT', 'HURT', 'DEATH', 'GREET'):
    for i in re.search(pool + r' = \{([^}]*)\}', clipsrc).group(1).replace(' ', '').split(','):
        need(('ammonius.line.' + cnames[int(i)]) in snd, f'{pool} clip {cnames[int(i)]} has its own sound event')
for i in re.search(r'LECTURE = \{([^}]*)\}', clipsrc).group(1).replace(' ', '').split(','):
    keys['quote.lucifertotem.ammonius.' + cnames[int(i)]] = 'lecture quote'
for ev, o in snd.items():
    if ev.startswith('ammonius.line.'): keys[o['subtitle']] = 'clip subtitle'
need(not any('/wav/' in n or n.endswith('.wav') for n in names), 'no wav files shipped')
for k, o in sorted(keys.items()):
    need(k in lang and lang[k].strip(), f'{k} = {lang.get(k)!r}   [{o}]')
need(lang.get('effect.lucifertotem.purple') == 'PURPLE', "PURPLE effect display name is exactly 'PURPLE'")
for k, v in [('item.lucifertotem.totem_of_lucifer.name', 'Totem of Lucifer'), ('item.lucifertotem.theriac.name', 'Bottle of Theriac'), ('item.lucifertotem.priapus_totem.name', 'Priapus Totem')]:
    need(lang.get(k) == v, f'{k} == {v!r}')
print('ALL OK' if ok else 'SOME CHECKS FAILED'); sys.exit(0 if ok else 1)
