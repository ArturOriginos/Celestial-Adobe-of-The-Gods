#!/usr/bin/env python3
"""Генерирует текстуры и блок Java-кода модели «Бога Селестии».
Единый источник правды: список PARTS. Скрипт раскладывает UV, рисует текстуру (2x),
свечение и подставляет код в CelestiaGodModel.java между маркерами GENERATED."""
import math, sys, re, os
import numpy as np
from PIL import Image

SS = 2                                   # во сколько раз PNG больше раскладки
OUT = sys.argv[1] if len(sys.argv) > 1 else "."
JAVA = sys.argv[2] if len(sys.argv) > 2 else None
LAYOUT_W = 128

# ---------------------------------------------------------------- палитра
def C(h): return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], float)
GOLD_D, GOLD_M, GOLD_L, GOLD_H = C(0x7A4E08), C(0xE0A800), C(0xF6D77A), C(0xFFF3C4)
LAV_D, LAV_M, LAV_L, LAV_X = C(0x4A4690), C(0x7C78C8), C(0xB8B4EC), C(0xDAD8F8)
WHITE, VOID = C(0xF4F2FF), C(0x1A1838)
CRY_D, CRY_M, CRY_L = C(0x5C6CE0), C(0x9AA8FF), C(0xE6EAFF)

# ---------------------------------------------------------------- модель
# name, parent, uvkey, style, (x,y,z), (w,h,d), pose(px,py,pz), rot(rx,ry,rz)
PARTS = []
def P(name, parent, uvkey, style, org, dims, pose=(0, 0, 0), rot=(0, 0, 0)):
    PARTS.append(dict(name=name, parent=parent, uv=uvkey, style=style, org=org, dims=dims, pose=pose, rot=rot))
def G(name, parent, pose=(0, 0, 0), rot=(0, 0, 0)):   # пустая группа
    PARTS.append(dict(name=name, parent=parent, uv=None, style=None, org=None, dims=None, pose=pose, rot=rot))

# корневые (заменяют ванильные)
P("hat", "root", None, None, None, None)
PARTS[-1]["uv"] = None
P("head", "root", "head", "helmet", (-4, -8, -4), (8, 8, 8))
P("body", "root", "body", "torso", (-5, 0, -3), (10, 12, 6))
P("right_arm", "root", "arm_r", "sleeve", (-3, -2, -2), (4, 12, 4), (-6, 2, 0))
P("left_arm", "root", "arm_l", "sleeve", (-1, -2, -2), (4, 12, 4), (6, 2, 0))
# голова
P("crest", "head", "crest", "gold", (-1, -11, -3), (2, 4, 6))
for side, sg in (("l", 1), ("r", -1)):
    P(f"antler_{side}", "head", "ant1", "antler", (-1, -9, -1), (2, 9, 2), (3 * sg, -6, 0), (0, 0, 0.35 * sg))
    P(f"antler_{side}2", f"antler_{side}", "ant2", "antler", (-1, -7, -1), (2, 7, 2), (0, -8, 0), (0, 0, 0.75 * sg))
    P(f"antler_{side}3", f"antler_{side}", "ant3", "antler", (-1, -5, -1), (2, 5, 2), (0, -4, 0), (0.7, 0, -0.5 * sg))
P("halo_disc", "head", "disc", "halo_disc", (-12, -12, 0), (24, 24, 1), (0, -4, 6))
G("star", "head", (0, -27, 0))
P("star_v", "star", "star_v", "star", (-1, -8, -1), (2, 16, 2))
P("star_h", "star", "star_h", "star", (-5, -1, -1), (10, 2, 2))
P("star_c", "star", "star_c", "star", (-2, -2, -1.5), (4, 4, 3), (0, 0, 0), (0, 0, 0.785))
# торс
P("chest", "body", "chest", "chest", (-4, 0, -4), (8, 8, 1))
P("gem", "body", "gem", "gem", (-2, -2, -1), (4, 4, 1), (0, 10, -3), (0, 0, 0.785))
P("cape", "body", "cape", "robe", (-5, 0, 0), (10, 28, 1), (0, 1, 3), (0.07, 0, 0))
P("cloak_l", "body", "cloak_l", "robe", (-1, 0, 0), (3, 24, 4), (5, 2, 1), (0, 0, -0.2))
P("cloak_r", "body", "cloak_r", "robe", (-2, 0, 0), (3, 24, 4), (-5, 2, 1), (0, 0, 0.2))
P("robe1", "body", "r1", "robe", (-5, 0, -3), (10, 8, 6), (0, 12, 0))
P("robe2", "body", "r2", "robe", (-4, 0, -2.5), (8, 8, 5), (0, 19, 0))
P("robe3", "body", "r3", "robe", (-3, 0, -2), (6, 8, 4), (0, 26, 0))
P("robe4", "body", "r4", "robe", (-2, 0, -1.5), (4, 7, 3), (0, 33, 0))
P("robe5", "body", "r5", "tip", (-1, 0, -1), (2, 6, 2), (0, 39, 0))
for i in range(6):
    P(f"shard{i}", "body", "shard", "crystal", (-1, -4, -1), (2, 8, 2), (0, 0, 0))
# кольца (восьмиугольники из планок)
for name, y, a, L in (("ring1", -5, 9, 8), ("ring2", 9, 13, 11), ("ring3", 26, 17, 14)):
    G(name, "body", (0, y, 0))
    for k in range(8):
        ph = k * math.pi / 4
        P(f"{name}_{k}", name, f"bar{L}", "ringbar", (-L / 2, 0, -0.5), (L, 1, 1),
          (-a * math.sin(ph), 0, -a * math.cos(ph)), (0, ph, 0))
# наплечники, шипы, наручи
for side, sg in (("r", -1), ("l", 1)):
    P(f"pauldron_{side}", f"{'right' if side=='r' else 'left'}_arm", "pauld", "armor",
      ((-4 if side == "r" else -3), -4, -3), (7, 3, 6))
    P(f"pspike_{side}", f"{'right' if side=='r' else 'left'}_arm", "pspike", "armor",
      (-1, -8, -1), (2, 8, 2), (3 * sg, -3, 0), (0, 0, 0.55 * -sg))
    P(f"cuff_{side}", f"{'right' if side=='r' else 'left'}_arm", "cuff", "gold",
      ((-3.5 if side == "r" else -1.5), 8, -2.5), (5, 2, 5))

# ---------------------------------------------------------------- раскладка UV
def face_size(d):
    w, h, dd = d
    return int(2 * (w + dd)), int(h + dd)
uniq = {}
for p in PARTS:
    if p["uv"] and p["uv"] not in uniq:
        uniq[p["uv"]] = p
order = sorted(uniq.values(), key=lambda p: -face_size(p["dims"])[1])
x = y = shelf = 0
UV = {}
for p in order:
    fw, fh = face_size(p["dims"])
    if x + fw > LAYOUT_W:
        x, y, shelf = 0, y + shelf, 0
    UV[p["uv"]] = (x, y)
    x += fw; shelf = max(shelf, fh)
LAYOUT_H = y + shelf
LAYOUT_H = int(math.ceil(LAYOUT_H / 8.0) * 8)
print("layout", LAYOUT_W, LAYOUT_H)

# ---------------------------------------------------------------- рисование
W, H = LAYOUT_W * SS, LAYOUT_H * SS
tex = np.zeros((H, W, 4), float)      # RGBA
glow = np.zeros((H, W, 4), float)
rng = np.random.default_rng(7)

def mix(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return a * (1 - t) + b * t

def faces(u, v, d):
    w, h, dd = d
    return {
        "top":   (u + dd, v, w, dd),
        "bottom": (u + dd + w, v, w, dd),
        "right": (u, v + dd, dd, h),
        "front": (u + dd, v + dd, w, h),
        "left":  (u + dd + w, v + dd, dd, h),
        "back":  (u + 2 * dd + w, v + dd, w, h),
    }

def put(arr, rgb, alpha, fx, fy, fw, fh):
    x0, y0, x1, y1 = int(fx * SS), int(fy * SS), int((fx + fw) * SS), int((fy + fh) * SS)
    arr[y0:y1, x0:x1, :3] = rgb
    arr[y0:y1, x0:x1, 3] = alpha

def grid(fw, fh):
    xs = (np.arange(int(fw * SS)) + 0.5) / SS
    ys = (np.arange(int(fh * SS)) + 0.5) / SS
    X, Y = np.meshgrid(xs, ys)
    return X, Y            # в единицах раскладки

def noise(shape, k=0.06):
    return (rng.random(shape) - 0.5) * 2 * k

def paint_face(style, face, fw, fh, dims):
    """Возвращает (rgb, alpha, glow_rgb, glow_alpha) для грани."""
    X, Y = grid(fw, fh)
    sh = Y.shape
    rgb = np.zeros(sh + (3,)); a = np.ones(sh); g = np.zeros(sh + (3,)); ga = np.zeros(sh)
    T = Y / max(fh, 1)               # 0 сверху → 1 снизу
    edge = np.minimum.reduce([X, Y, fw - X, fh - Y])
    gold_edge = edge < 0.55

    def trim(mask, strength=1.0):
        nonlocal rgb, g, ga
        col = mix(GOLD_M, GOLD_H, 0.7 - T * 0.5)
        rgb[mask] = col[mask]
        g[mask] = (col * 0.35)[mask]; ga[mask] = 1

    if style == "robe":
        folds = 0.5 + 0.5 * np.sin(X * (2 * math.pi / 2.6) + (fw * 0.7) + np.sin(Y * 0.3) * 0.8)
        base = mix(mix(WHITE, LAV_X, T * 0.9), mix(LAV_L, LAV_M, T), folds * 0.75)
        base = base * (1 - 0.0) 
        rgb = base + noise(sh + (1,), 6) * 1.0
        if face in ("top", "bottom"):
            rgb = LAV_M + noise(sh + (1,), 6)
        # золотая кайма по низу и бокам
        bottom = (Y > fh - 0.9) & (face not in ("top", "bottom"))
        trim(bottom | ((X < 0.45) | (X > fw - 0.45)) & (face in ("front", "back")))
        # центральная золотая линия на фронте
        if face == "front" and fw >= 4:
            m = np.abs(X - fw / 2) < 0.4
            trim(m)
    elif style == "tip":
        base = mix(LAV_L, LAV_D, T)
        rgb = base + noise(sh + (1,), 5)
        trim(T > 0.82)
    elif style == "torso":
        folds = 0.5 + 0.5 * np.sin(X * 2.2)
        rgb = mix(mix(WHITE, LAV_L, T * 0.6), LAV_M, folds * 0.35 + 0.1) + noise(sh + (1,), 5)
        if face in ("top", "bottom"):
            rgb = LAV_M + noise(sh + (1,), 5)
        trim(gold_edge & (face not in ("top", "bottom")))
        if face in ("front", "back"):
            trim(np.abs(X - fw / 2) < 0.4)
            # пояс
            trim((Y > fh - 2.6) & (Y < fh - 1.7))
    elif style == "chest":
        rgb = mix(WHITE, LAV_L, T * 0.7) + noise(sh + (1,), 4)
        trim(gold_edge)
        if face == "front":
            # крест-звезда
            cx, cy = fw / 2, fh / 2
            cross = ((np.abs(X - cx) < 0.5) & (np.abs(Y - cy) < 3.4)) | ((np.abs(Y - cy) < 0.5) & (np.abs(X - cx) < 3.0))
            dia = (np.abs(X - cx) + np.abs(Y - cy)) < 1.7
            m = cross | dia
            rgb[m] = mix(GOLD_L, GOLD_H, 1.0 - (np.abs(X - cx) + np.abs(Y - cy)) / 4)[m]
            g[m] = rgb[m]; ga[m] = 1
    elif style == "gem":
        rgb = mix(CRY_M, CRY_L, 1 - edge / 2)
        trim(edge < 0.5)
        g[:] = rgb * 0.8; ga[:] = 1
    elif style in ("sleeve",):
        folds = 0.5 + 0.5 * np.sin(Y * 1.4 + X)
        rgb = mix(mix(LAV_X, LAV_L, T), LAV_M, folds * 0.5) + noise(sh + (1,), 5)
        if face in ("top", "bottom"):
            rgb = LAV_M + noise(sh + (1,), 5)
        trim((Y > fh - 1.0) & (face not in ("top", "bottom")))
        # рука-перчатка: тёмная нижняя часть
        hand = (Y > fh - 4.2) & (face not in ("top", "bottom"))
        rgb[hand] = mix(LAV_D, VOID, T * 0.5)[hand]
    elif style == "armor":
        rgb = mix(WHITE, LAV_L, T * 0.8) + noise(sh + (1,), 4)
        trim(gold_edge)
    elif style == "gold":
        rgb = mix(GOLD_H, GOLD_M, T * 0.9 + (np.sin(X * 3) * 0.1)) + noise(sh + (1,), 5)
        g[:] = rgb * 0.45; ga[:] = 1
    elif style == "antler":
        rgb = mix(GOLD_L, GOLD_D, T * 0.9) + noise(sh + (1,), 6)
        rgb[(X % 3 < 0.5) & (face in ("front", "back", "left", "right"))] *= 0.8
        g[:] = rgb * 0.3; ga[:] = 1
    elif style == "crystal":
        rgb = mix(CRY_L, CRY_D, np.abs(X / fw - 0.5) * 1.3 + T * 0.4) + noise(sh + (1,), 5)
        g[:] = rgb * 0.9; ga[:] = 1
    elif style == "ringbar":
        rgb = mix(GOLD_H, GOLD_L, np.abs(Y / max(fh, 1) - 0.5) * 2)
        g[:] = rgb * 1.0; ga[:] = 1
    elif style == "star":
        rgb = mix(GOLD_H, GOLD_L, T * 0.5)
        g[:] = rgb; ga[:] = 1
    elif style == "halo_disc":
        if face in ("front", "back"):
            cx, cy = fw / 2, fh / 2
            r = np.hypot(X - cx, Y - cy)
            ring = (r < 11.6) & (r > 10.0)
            thin = (r < 9.2) & (r > 8.7)
            soft = (r < 10.0) & (r > 6.0)
            a = np.zeros(sh)
            a[ring] = 1; a[thin] = 1
            rgb = mix(GOLD_H, GOLD_L, np.abs(r - 10.8) / 0.8)
            # лучи-кристаллы внутри
            ang = np.arctan2(Y - cy, X - cx)
            spikes = (r < 9.2) & (r > 5.5) & (np.abs(np.sin(ang * 6)) > 0.93)
            rgb[spikes] = LAV_L; a[spikes] = 1
            g[:] = rgb * 0.9; ga = a.copy()
        else:
            rgb[:] = GOLD_M; a = np.ones(sh) * 1.0; g[:] = rgb; ga[:] = 1
    elif style == "helmet":
        rgb = mix(LAV_L, LAV_M, T * 0.9) + noise(sh + (1,), 5)
        if face in ("top",):
            rgb = mix(GOLD_L, GOLD_M, 0.5) + noise(sh + (1,), 5)
        trim(gold_edge & (face != "front"))
        if face == "front":
            trim(edge < 0.55)
            visor = (X > 1.1) & (X < fw - 1.1) & (Y > 1.6) & (Y < fh - 0.9)
            rgb[visor] = mix(VOID, LAV_D, (Y[visor] / fh) * 0.6 + 0.1 * np.sin(X[visor] * 2))
            # золотая линия по центру
            trim((np.abs(X - fw / 2) < 0.25) & (Y < 2.0) & (Y > 0.4))
            # узкие светящиеся «глаза»
            eye = (Y > 3.6) & (Y < 4.3) & (((X > 1.8) & (X < 3.4)) | ((X > fw - 3.4) & (X < fw - 1.8)))
            rgb[eye] = WHITE; g[eye] = C(0xBFD4FF); ga[eye] = 1
        elif face in ("left", "right"):
            m = (X > 0.9) & (X < fw - 0.9) & (Y > 2.2) & (Y < 5.2)
            rgb[m] = mix(LAV_D, VOID, 0.4)
        elif face == "back":
            trim(np.abs(X - fw / 2) < 0.3)
    else:
        rgb[:] = LAV_M
    rgb = np.clip(rgb, 0, 255)
    return rgb, a, np.clip(g, 0, 255), ga

def draw_part(p):
    u, v = UV[p["uv"]]
    d = p["dims"]
    for face, (fx, fy, fw, fh) in faces(u, v, d).items():
        if fw <= 0 or fh <= 0: continue
        rgb, a, g, ga = paint_face(p["style"], face, fw, fh, d)
        x0, y0 = int(fx * SS), int(fy * SS)
        h_, w_ = a.shape
        tex[y0:y0 + h_, x0:x0 + w_, :3] = rgb
        tex[y0:y0 + h_, x0:x0 + w_, 3] = a * 255
        glow[y0:y0 + h_, x0:x0 + w_, :3] = g
        glow[y0:y0 + h_, x0:x0 + w_, 3] = ga * 255

for k, p in uniq.items():
    draw_part(p)

def save(arr, name):
    im = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    im.save(os.path.join(OUT, name))
save(tex, "celestia_god.png")
# glow: чёрный фон без альфы (EyesLayer – аддитивный)
g2 = glow.copy(); g2[..., :3] *= (g2[..., 3:4] / 255.0); g2[..., 3] = 255
Image.fromarray(np.clip(g2, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "celestia_god_glow.png"))

# ---------------------------------------------------------------- Java
def f(x):
    s = ("%.4f" % x).rstrip("0").rstrip(".")
    if s in ("-0", ""): s = "0"
    return s + "F"
lines = []
for p in PARTS:
    n = p["name"]; par = "root" if p["parent"] == "root" else p["parent"]
    if p["dims"] is None and p["uv"] is None and p["org"] is None:
        cubes = "CubeListBuilder.create()"
    else:
        u, v = UV[p["uv"]]
        o, d = p["org"], p["dims"]
        cubes = f"CubeListBuilder.create().texOffs({u}, {v}).addBox({f(o[0])}, {f(o[1])}, {f(o[2])}, {f(d[0])}, {f(d[1])}, {f(d[2])})"
    ps, r = p["pose"], p["rot"]
    if r == (0, 0, 0):
        pose = f"PartPose.offset({f(ps[0])}, {f(ps[1])}, {f(ps[2])})"
    else:
        pose = f"PartPose.offsetAndRotation({f(ps[0])}, {f(ps[1])}, {f(ps[2])}, {f(r[0])}, {f(r[1])}, {f(r[2])})"
    lines.append(f'        PartDefinition {n} = {par}.addOrReplaceChild("{n}", {cubes}, {pose});')
code = "\n".join(lines)
open(os.path.join(OUT, "model_parts.java.txt"), "w").write(code)
open(os.path.join(OUT, "layout.txt"), "w").write(f"{LAYOUT_W} {LAYOUT_H}\n")
if JAVA:
    s = open(JAVA).read()
    s = re.sub(r"(// BEGIN GENERATED\n).*?([ \t]*// END GENERATED)", lambda m: m.group(1) + code + "\n" + m.group(2), s, flags=re.S)
    s = re.sub(r"LayerDefinition\.create\(mesh, \d+, \d+\)", f"LayerDefinition.create(mesh, {LAYOUT_W}, {LAYOUT_H})", s)
    open(JAVA, "w").write(s)
print("parts", len(PARTS), "uv blocks", len(UV))
