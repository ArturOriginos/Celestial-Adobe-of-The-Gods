"""Генератор воксельной 3D-модели Совершенного Пакселя (JSON-модель Minecraft + палитра 16x16)."""
import json, math
from PIL import Image

RES = '../src/main/resources/assets/celestial/'
PALETTE = [
    (255, 215, 106), (227, 169, 46), (168, 112, 26), (43, 22, 80), (91, 63, 168), (169, 155, 255),
    (216, 208, 255), (255, 251, 234), (18, 10, 38), (58, 34, 102), (255, 179, 71),
]
GOLD_L, GOLD_M, GOLD_D, P_DEEP, P_MID, LAV, LAV_L, WHITE, BLACK, WRAP, ORANGE = range(11)

vox = {}   # (x, y, k) -> [kind, color]

def put(x, y, ks, kind, color=None):
    for k in ks:
        vox[(x, y, k)] = [kind, color]

S2 = math.sqrt(2) / 2
U = (S2, -S2)    # вдоль головки к лезвию топора
V = (S2, S2)     # вдоль рукояти от низа к верху
H = (19.0, 19.0)  # центр головки

def at(o, a, b):
    return (o[0] + U[0] * a + V[0] * b, o[1] + U[1] * a + V[1] * b)

def cell(p):
    return (int(math.floor(p[0] + 0.5)), int(math.floor(p[1] + 0.5)))

# --- рукоять (золото с фиолетовыми обмотками)
s = 0.0
while s <= 27:
    p = (3.2 + V[0] * s, 3.2 + V[1] * s)
    c = cell(p)
    band = int(s) % 6 in (1, 2) and s < 14
    for dx, dy in ((0, 0), (1, 0), (0, 1)):
        put(c[0] + dx, c[1] + dy, (-1, 0, 1) if band else (0,), 'wrap' if band else 'handle')
    s += 0.2

# --- лопата на конце рукояти (овальное лезвие вдоль рукояти)
C_SH = (3.3, 3.3)
for x in range(-1, 9):
    for y in range(-1, 9):
        dx, dy = x - C_SH[0], y - C_SH[1]
        a = dx * V[0] + dy * V[1]          # вдоль рукояти
        b = dx * U[0] + dy * U[1]          # поперёк
        if (a / 4.4) ** 2 + (b / 3.0) ** 2 <= 1.0 and a < 3.4:
            put(x, y, (-1, 0, 1), 'blade')

# --- кирка: изогнутый шип в сторону -U
t = 0.0
while t <= 12.5:
    w = 4.6 * (1 - t / 13.5)
    off = -w / 2
    while off <= w / 2:
        base = at(H, -t, -(t * t) * 0.028)
        p = (base[0] + V[0] * off, base[1] + V[1] * off)
        c = cell(p)
        put(c[0], c[1], (-1, 0, 1) if t < 8 else (0,), 'blade')
        off += 0.35
    t += 0.2

# --- топор: широкое веерное лезвие в сторону +U
for oi in range(-13, 14):
    off = oi * 0.5
    tmax = 11.0 - (off ** 2) * 0.115
    t = 0.5
    while t <= tmax:
        hw = 1.6 + 0.62 * t
        if abs(off) <= hw:
            c = cell(at(H, t, off))
            thick = (-1, 0, 1) if t < tmax - 2.2 else (0,)
            put(c[0], c[1], thick, 'blade')
        t += 0.25

# --- навершие рукояти над головкой
s = 0.0
while s <= 6:
    p = at(H, 0, 1 + s)
    c = cell(p)
    put(c[0], c[1], (0,) if s > 3 else (-1, 0, 1), 'handle')
    if s < 3:
        put(c[0] + 1, c[1], (0,), 'handle')
    s += 0.25

# --- небесная звезда-самоцвет в центре головки
cx, cy = cell(H)
for dx in range(-5, 6):
    for dy in range(-5, 6):
        if abs(dx) * abs(dy) <= 1 and abs(dx) + abs(dy) <= 5:
            d = abs(dx) + abs(dy)
            km = 2 if d <= 1 else (1 if d <= 3 else 0)
            put(cx + dx, cy + dy, tuple(range(-km, km + 1)), 'gem')

# ---------- раскраска
def has(x, y, k):
    return (x, y, k) in vox

for (x, y, k), v in vox.items():
    kind = v[0]
    if kind == 'handle':
        v[1] = GOLD_L if (x + y) % 4 < 2 else GOLD_M
    elif kind == 'wrap':
        v[1] = WRAP if abs(k) == 1 or (x + y) % 2 else P_MID
    elif kind == 'gem':
        d = abs(x - cx) + abs(y - cy)
        v[1] = WHITE if d <= 1 else (GOLD_L if d <= 3 else GOLD_M)
        if d == 0 and abs(k) == 2:
            v[1] = WHITE
    elif kind == 'spark':
        v[1] = WHITE
    elif kind == 'spark2':
        v[1] = LAV_L
    else:  # blade
        edge = not (has(x + 1, y, k) and has(x - 1, y, k) and has(x, y + 1, k) and has(x, y - 1, k))
        surface = not (has(x, y, k + 1) and has(x, y, k - 1))
        if edge:
            v[1] = GOLD_L if (x - y) > 0 else GOLD_M
            if x + y > 30:
                v[1] = ORANGE
        elif surface:
            sw = ((x - y) // 2) % 4
            v[1] = (LAV, P_MID, LAV_L, P_MID)[sw]
        else:
            v[1] = P_DEEP if (x + 2 * y) % 7 else BLACK
# рёбра по толщине: внешний слой золото-тёмное для объёма
for (x, y, k), v in vox.items():
    if v[0] == 'blade' and abs(k) == 1 and v[1] in (P_MID,) and (x + y) % 7 == 0:
        v[1] = LAV_L

# ---------- жадное слияние в боксы
remaining = dict(vox)
boxes = []
for key in sorted(remaining, key=lambda q: (q[2], q[1], q[0])):
    if key not in remaining:
        continue
    x, y, k = key
    col = remaining[key][1]
    def ok(px, py, pk):
        return (px, py, pk) in remaining and remaining[(px, py, pk)][1] == col
    x2 = x
    while ok(x2 + 1, y, k):
        x2 += 1
    y2 = y
    while all(ok(xx, y2 + 1, k) for xx in range(x, x2 + 1)):
        y2 += 1
    k2 = k
    while all(ok(xx, yy, k2 + 1) for xx in range(x, x2 + 1) for yy in range(y, y2 + 1)):
        k2 += 1
    for xx in range(x, x2 + 1):
        for yy in range(y, y2 + 1):
            for kk in range(k, k2 + 1):
                del remaining[(xx, yy, kk)]
    boxes.append((x, y, k, x2, y2, k2, col))

# ---------- JSON
def uv(c):
    px, py = c % 16, c // 16
    return [px, py, px + 1, py + 1]

elements = []
for (x, y, k, x2, y2, k2, col) in boxes:
    def filled(cells):
        return all(c in vox for c in cells)
    faces = {}
    r = lambda a, b: range(a, b + 1)
    if not filled([(x2 + 1, yy, kk) for yy in r(y, y2) for kk in r(k, k2)]):
        faces['east'] = {'texture': '#0', 'uv': uv(col)}
    if not filled([(x - 1, yy, kk) for yy in r(y, y2) for kk in r(k, k2)]):
        faces['west'] = {'texture': '#0', 'uv': uv(col)}
    if not filled([(xx, y2 + 1, kk) for xx in r(x, x2) for kk in r(k, k2)]):
        faces['up'] = {'texture': '#0', 'uv': uv(col)}
    if not filled([(xx, y - 1, kk) for xx in r(x, x2) for kk in r(k, k2)]):
        faces['down'] = {'texture': '#0', 'uv': uv(col)}
    if not filled([(xx, yy, k2 + 1) for xx in r(x, x2) for yy in r(y, y2)]):
        faces['south'] = {'texture': '#0', 'uv': uv(col)}
    if not filled([(xx, yy, k - 1) for xx in r(x, x2) for yy in r(y, y2)]):
        faces['north'] = {'texture': '#0', 'uv': uv(col)}
    if not faces:
        continue
    elements.append({
        'from': [x * 0.5, y * 0.5, 8 + k * 0.5 - 0.25],
        'to': [(x2 + 1) * 0.5, (y2 + 1) * 0.5, 8 + k2 * 0.5 + 0.25],
        'faces': faces,
    })

old = json.load(open('paxel_display.json'))
display = json.loads(json.dumps(old['display']))
BIG = 2.5  # в руке и на земле Совершенный Паксель в 2,5 раза больше обычного (GUI/рамка/голова не меняются)
for slot in ('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand', 'ground'):
    display[slot]['scale'] = [round(v * BIG, 3) for v in display[slot]['scale']]
model = {
    'textures': {'0': 'celestial:item/perfect_paxel_palette', 'particle': 'celestial:item/perfect_paxel_palette'},
    'gui_light': 'front',
    'elements': elements,
    'display': display,
}
json.dump(model, open(RES + 'models/item/perfect_paxel.json', 'w'), separators=(',', ':'))

pal = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for i, c in enumerate(PALETTE):
    pal.putpixel((i % 16, i // 16), c + (255,))
pal.save(RES + 'textures/item/perfect_paxel_palette.png')

print('voxels', len(vox), 'elements', len(elements))
xs = [k[0] for k in vox]; ys = [k[1] for k in vox]
print('bounds x', min(xs), max(xs), 'y', min(ys), max(ys))

# ---------- превью (ортографическая проекция с поворотом)
def render(path, yaw):
    sc = 12
    img = Image.new('RGB', (34 * sc + 60, 34 * sc), (44, 46, 60))
    px = img.load()
    cosr, sinr = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    items = []
    for (x, y, k), v in vox.items():
        X = (x - 16) ; Z = k
        xr = X * cosr + Z * sinr
        zr = -X * sinr + Z * cosr
        items.append((zr, xr, y, v[1]))
    items.sort()
    for zr, xr, y, c in items:
        col = PALETTE[c]
        sx = int((xr + 16) * sc + 30); sy = int((33 - y) * sc)
        for dx in range(sc):
            for dy in range(sc):
                if 0 <= sx + dx < img.width and 0 <= sy + dy < img.height:
                    px[sx + dx, sy + dy] = col
    img.save(path)
render('/home/claude/work/prev0.png', 0)
render('/home/claude/work/prev60.png', 60)
