#!/usr/bin/env python3
"""400x400 project icon: violet morning star over floating Lightfall islands and a glowing sea, 'Phosphoros' title.
Procedural (no external images); saved without metadata chunks."""
import math, random, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont
random.seed(1333)
S = 400; SS = 4; W = S * SS
out = sys.argv[1] if len(sys.argv) > 1 else 'icon.png'
img = Image.new('RGB', (W, W))
px = img.load()
# dusk gradient: deep indigo top -> violet -> magenta glow at the horizon
top, mid, hor = (14, 6, 34), (58, 20, 104), (170, 70, 200)
HY = int(W * 0.66)
for y in range(W):
    t = y / HY
    if y <= HY:
        c = [int(top[i] + (mid[i] - top[i]) * min(1, t * 1.2)) for i in range(3)] if t < 0.8 else \
            [int(mid[i] + (hor[i] - mid[i]) * ((t - 0.8) / 0.2) ** 1.5) for i in range(3)]
    else:
        u = (y - HY) / (W - HY); c = [int((100, 30, 160)[i] * (1 - u) + (36, 10, 70)[i] * u) for i in range(3)]
    for x in range(W): px[x, y] = tuple(c)
d = ImageDraw.Draw(img)
# stars
for _ in range(140):
    x, y = random.randrange(W), random.randrange(int(W * 0.55)); r = random.choice([2, 3, 3, 4, 5])
    b = random.randint(150, 255); d.ellipse((x - r, y - r, x + r, y + r), fill=(b, int(b * 0.85), 255))
# morning star glow
cx, cy = W // 2, int(W * 0.36)
glow = Image.new('L', (W, W)); gd = ImageDraw.Draw(glow)
for r in range(520, 0, -8): gd.ellipse((cx - r, cy - r, cx + r, cy + r), fill=int(255 * (1 - r / 520) ** 2.2))
img = Image.composite(Image.new('RGB', (W, W), (235, 190, 255)), img, glow.filter(ImageFilter.GaussianBlur(30)))
d = ImageDraw.Draw(img)
def star(cx, cy, R, r, n, rot, fill):
    pts = []
    for i in range(2 * n):
        a = rot + math.pi * i / n; rr = R if i % 2 == 0 else r
        pts.append((cx + rr * math.sin(a), cy - rr * math.cos(a)))
    d.polygon(pts, fill=fill)
star(cx, cy, 300, 46, 4, 0, (225, 170, 255))          # long 4-point rays
star(cx, cy, 170, 40, 4, math.pi / 4, (200, 130, 255))   # short diagonal rays
star(cx, cy, 120, 60, 8, math.pi / 8, (245, 215, 255))
d.ellipse((cx - 58, cy - 58, cx + 58, cy + 58), fill=(255, 245, 255))
# sea shimmer
for i in range(26):
    y = HY + 20 + i * 18; w = int(260 * (1 - i / 30)) + random.randint(-30, 30)
    d.rectangle((cx - w // 2, y, cx + w // 2, y + 5), fill=(225, 160, 255))
# floating islands (blocky, Minecraft-like)
def island(x0, y0, w, h, seed):
    rnd = random.Random(seed); g, s, u = (120, 70, 180), (70, 40, 110), (44, 24, 74)
    d.rectangle((x0, y0, x0 + w, y0 + 24), fill=g)
    d.rectangle((x0, y0 + 24, x0 + w, y0 + 60), fill=s)
    step = 32; rows = h // step
    for k in range(rows):
        inset = int((k + 1) * w / (2 * rows + 1)) + rnd.randint(-8, 8)
        d.rectangle((x0 + inset, y0 + 60 + k * step, x0 + w - inset, y0 + 60 + (k + 1) * step), fill=u)
island(130, 1010, 360, 230, 1); island(1120, 960, 340, 210, 2); island(640, 1120, 300, 170, 3)
# little ruined spire on the right island
d.rectangle((1250, 820, 1290, 960), fill=(56, 30, 90)); d.rectangle((1240, 800, 1300, 830), fill=(56, 30, 90))
img = img.resize((S, S), Image.LANCZOS)
# title
d = ImageDraw.Draw(img)
f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf', 52)
t = 'Phosphoros'; bw = d.textbbox((0, 0), t, font=f); tw = bw[2] - bw[0]
x = (S - tw) // 2 - bw[0]; y = 318
for dx, dy in [(-2, 0), (2, 0), (0, -2), (0, 2), (-2, -2), (2, 2), (-2, 2), (2, -2)]:
    d.text((x + dx, y + dy), t, font=f, fill=(30, 8, 50))
d.text((x, y), t, font=f, fill=(255, 226, 140))
img.save(out, 'PNG', optimize=True)   # no text/EXIF chunks are written
print(out, img.size)
