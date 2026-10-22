"""every vanilla reference in our sounds.json must resolve: type=event -> a vanilla event, plain minecraft: names -> a vanilla .ogg"""
import json, os, sys
here = os.path.dirname(__file__)
ours = json.load(open(os.path.join(here, '../src/main/resources/assets/lucifertotem/sounds.json')))
van = json.load(open(os.path.join(here, 'vanilla_sounds_1.12.json')))
files = set(json.load(open(os.path.join(here, 'vanilla_sound_files_1.12.json'))))
bad = 0; n = 0
for ev, v in ours.items():
    for s in v['sounds']:
        s = s if isinstance(s, dict) else {'name': s}
        name = s['name']
        if not name.startswith('minecraft:'): continue
        n += 1
        path = name.split(':', 1)[1]
        ok = (path in van and len(van[path].get('sounds', [])) > 0) if s.get('type') == 'event' else ('minecraft/sounds/' + path + '.ogg') in files
        if not ok: bad += 1
        print(('OK  ' if ok else 'BAD ') + ev + ' -> ' + name + (' (event)' if s.get('type') == 'event' else ' (file)'))
# vanilla SoundEvents.X used in our code must not be one of 1.12's silent (empty) events
import re, glob
byField = {k.replace('.', '_').upper(): k for k in van}
for f in glob.glob(os.path.join(here, '../src/main/java/**/*.java'), recursive=True):
    for fld in set(re.findall(r'SoundEvents\.([A-Z0-9_]+)', open(f).read())):
        ev = byField.get(fld)
        if ev is None: continue
        n += 1
        if not van[ev].get('sounds'): bad += 1; print('BAD silent vanilla event in code: SoundEvents.' + fld + ' (' + ev + ') in ' + os.path.basename(f))
print('vanilla refs', n, 'bad', bad)
sys.exit(1 if bad else 0)
