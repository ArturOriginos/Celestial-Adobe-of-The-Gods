#!/usr/bin/env python3
"""Текстура брони Селестии (слой 1: нагрудник + ботинки) в раскладке ванильной брони 64x32, рисуется в 2x,
и текстура нимба. Запуск: python3 gen_armor.py <папка assets/celestial/textures>"""
import sys, os, math
import numpy as np
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "."
S = 2
def C(h): return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], float)
WHITE, LAV_X, LAV_L, LAV_M, LAV_D = C(0xF4F2FF), C(0xDAD8F8), C(0xB8B4EC), C(0x8F8BD8), C(0x5A56AA)
GOLD_H, GOLD_L, GOLD_M, GOLD_D = C(0xFFF3C4), C(0xF6D77A), C(0xE0A800), C(0x8A5C0A)
GEM_L, GEM_M, GEM_D = C(0xBDF0FF), C(0x40B8FA), C(0x1E6ED2)
rng = np.random.default_rng(11)

tex = np.zeros((32 * S, 64 * S, 4), float)

def mix(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return a * (1 - t) + b * t

def face(x, y, w, h, fn, ymin=0.0):
    """Рисует грань (x,y,w,h в единицах 64x32). fn(X,Y,w,h) -> (rgb, alpha). Y считается от верха грани."""
    X, Y = np.meshgrid((np.arange(int(w * S)) + 0.5) / S, (np.arange(int(h * S)) + 0.5) / S)
    rgb, a = fn(X, Y, w, h)
    rgb = np.clip(rgb + (rng.random(Y.shape + (1,)) - 0.5) * 7, 0, 255)
    x0, y0 = int(x * S), int(y * S)
    tex[y0:y0 + Y.shape[0], x0:x0 + Y.shape[1], :3] = rgb
    tex[y0:y0 + Y.shape[0], x0:x0 + Y.shape[1], 3] = a * 255

def gold(X, Y, t=0.0):
    return mix(GOLD_H, GOLD_M, (Y * 0.12 + t))

def diamond(X, Y, cx, cy, r):
    return (np.abs(X - cx) + np.abs(Y - cy)) < r

def gem(rgb, X, Y, cx, cy, r):
    d = np.abs(X - cx) + np.abs(Y - cy)
    m = d < r
    rgb[m] = mix(GEM_L, GEM_M, d[m] / r)[...] if False else (GEM_M)
    inner = d < r * 0.55
    rgb[inner] = GEM_L
    ring = (d >= r) & (d < r + 0.45)
    rgb[ring] = GOLD_M
    return m | ring

# -------------------------------------------------------------- нагрудник: тело
def body_front(X, Y, w, h):
    rgb = mix(WHITE, LAV_L, Y / h * 0.9 + 0.0)
    a = np.ones_like(Y)
    edge = np.minimum.reduce([X, w - X, Y, h - Y])
    rgb[edge < 0.5] = GOLD_M
    # воротник
    rgb[Y < 1.6] = gold(X, Y)[Y < 1.6]
    v = (np.abs(X - w / 2) < (1.6 - Y) * 1.2) & (Y < 1.6)
    rgb[v] = LAV_D
    # центральная золотая полоса
    m = (np.abs(X - w / 2) < 0.5) & (Y > 1.6)
    rgb[m] = gold(X, Y)[m]
    # большой камень на груди
    gem(rgb, X, Y, w / 2, 4.6, 1.6)
    # ромбы по бокам
    for dx in (-2.4, 2.4):
        rgb[diamond(X, Y, w / 2 + dx, 3.2, 0.9)] = GOLD_L
    # пояс и нижний «подол»
    belt = (Y > 8.6) & (Y < 9.6)
    rgb[belt] = gold(X, Y)[belt]
    gem(rgb, X, Y, w / 2, 9.1, 0.9)
    tip = (Y > 10.2) & (np.abs(X - w / 2) < (12 - Y) * 2.4 + 0.2)
    rgb[tip] = mix(GOLD_L, GOLD_M, (Y[tip] - 10.2) / 1.8)
    return rgb, a

def body_back(X, Y, w, h):
    rgb = mix(LAV_X, LAV_M, Y / h)
    a = np.ones_like(Y)
    edge = np.minimum.reduce([X, w - X, Y, h - Y])
    rgb[edge < 0.5] = GOLD_M
    rgb[Y < 1.4] = gold(X, Y)[Y < 1.4]
    m = (np.abs(X - w / 2) < 0.4) & (Y > 1.4)
    rgb[m] = gold(X, Y)[m]
    # четырёхконечная звезда на спине
    cx, cy = w / 2, 4.3
    star = ((np.abs(X - cx) < 0.35) & (np.abs(Y - cy) < 2.4)) | ((np.abs(Y - cy) < 0.35) & (np.abs(X - cx) < 2.4)) | diamond(X, Y, cx, cy, 1.0)
    rgb[star] = GOLD_H
    rgb[(Y > 9.0) & (Y < 9.9)] = gold(X, Y)[(Y > 9.0) & (Y < 9.9)]
    return rgb, a

def body_side(X, Y, w, h):
    rgb = mix(LAV_L, LAV_M, Y / h)
    a = np.ones_like(Y)
    edge = np.minimum.reduce([X, w - X, Y, h - Y])
    rgb[edge < 0.5] = GOLD_M
    rgb[Y < 1.4] = gold(X, Y)[Y < 1.4]
    rgb[(Y > 8.6) & (Y < 9.6)] = gold(X, Y)[(Y > 8.6) & (Y < 9.6)]
    return rgb, a

def plate(X, Y, w, h):   # верх/низ: воротник и подол
    rgb = mix(GOLD_L, GOLD_D, Y / h * 0.6)
    return rgb, np.ones_like(Y)

# -------------------------------------------------------------- нагрудник: рукава
def arm_outer(X, Y, w, h):
    rgb = mix(WHITE, LAV_L, Y / h)
    a = np.ones_like(Y)
    rgb[Y < 3.2] = mix(GOLD_H, GOLD_M, Y / 3.2)[Y < 3.2]               # наплечник
    rgb[(Y > 2.9) & (Y < 3.4)] = GOLD_D
    gem(rgb, X, Y, w / 2, 1.6, 0.9)
    cuff = (Y > 9.4) & (Y < 10.6)
    rgb[cuff] = gold(X, Y)[cuff]
    rgb[Y > 10.6] = mix(LAV_D, LAV_M, (Y[Y > 10.6] - 10.6) / 1.4)[...] if False else LAV_M
    edge = np.minimum(X, w - X) < 0.4
    rgb[edge & (Y > 3.4)] = LAV_L
    return rgb, a

def arm_front(X, Y, w, h):
    rgb = mix(LAV_X, LAV_L, Y / h)
    a = np.ones_like(Y)
    rgb[Y < 3.2] = mix(GOLD_L, GOLD_M, Y / 3.2)[Y < 3.2]
    rgb[(Y > 2.9) & (Y < 3.3)] = GOLD_D
    cuff = (Y > 9.4) & (Y < 10.6)
    rgb[cuff] = gold(X, Y)[cuff]
    rgb[Y > 10.6] = LAV_M
    m = (np.abs(X - w / 2) < 0.35) & (Y > 3.3) & (Y < 9.4)
    rgb[m] = GOLD_L
    return rgb, a

def arm_inner(X, Y, w, h):
    rgb = mix(LAV_L, LAV_M, Y / h)
    a = np.ones_like(Y)
    rgb[Y < 3.2] = GOLD_M
    cuff = (Y > 9.4) & (Y < 10.6)
    rgb[cuff] = GOLD_M
    return rgb, a

# -------------------------------------------------------------- ботинки (низ ноги, y >= 7)
BOOT_TOP = 7.0
def boot_front(X, Y, w, h):
    a = (Y >= BOOT_TOP).astype(float)
    rgb = mix(WHITE, LAV_L, np.clip((Y - BOOT_TOP) / 5, 0, 1))
    cuff = (Y >= BOOT_TOP) & (Y < BOOT_TOP + 1.2)
    rgb[cuff] = gold(X, Y)[cuff]
    gem(rgb, X, Y, w / 2, 9.0, 1.2)
    sole = Y > 10.9
    rgb[sole] = mix(GOLD_M, GOLD_D, (Y[sole] - 10.9))
    toe = (Y > 9.9) & (np.abs(X - w / 2) > 1.1)
    rgb[toe & (Y <= 10.9)] = LAV_X
    return rgb, a

def boot_side(X, Y, w, h):
    a = (Y >= BOOT_TOP).astype(float)
    rgb = mix(LAV_X, LAV_M, np.clip((Y - BOOT_TOP) / 5, 0, 1))
    cuff = (Y >= BOOT_TOP) & (Y < BOOT_TOP + 1.2)
    rgb[cuff] = gold(X, Y)[cuff]
    sole = Y > 10.9
    rgb[sole] = mix(GOLD_M, GOLD_D, (Y[sole] - 10.9))
    stripe = (Y > 9.3) & (Y < 9.9)
    rgb[stripe & ~cuff & ~sole] = GOLD_L
    return rgb, a

def boot_bottom(X, Y, w, h):
    return mix(GOLD_M, GOLD_D, Y / h), np.ones_like(Y)

# раскладка ванильной брони 64x32 (единицы)
face(20, 20, 8, 12, body_front); face(32, 20, 8, 12, body_back)
face(16, 20, 4, 12, body_side); face(28, 20, 4, 12, body_side)
face(20, 16, 8, 4, plate); face(28, 16, 8, 4, plate)
face(40, 20, 4, 12, arm_outer); face(44, 20, 4, 12, arm_front)
face(48, 20, 4, 12, arm_inner); face(52, 20, 4, 12, arm_front)
face(44, 16, 4, 4, plate); face(48, 16, 4, 4, plate)
face(0, 20, 4, 12, boot_side); face(4, 20, 4, 12, boot_front)
face(8, 20, 4, 12, boot_side); face(12, 20, 4, 12, boot_side)
face(8, 16, 4, 4, boot_bottom)

os.makedirs(os.path.join(OUT, "models", "armor"), exist_ok=True)
Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "models", "armor", "celestia_layer_1.png"))
# слой 2 (штаны) не используется предметами, но файл нужен материалу: прозрачный
Image.new("RGBA", (64 * S, 32 * S), (0, 0, 0, 0)).save(os.path.join(OUT, "models", "armor", "celestia_layer_2.png"))

# -------------------------------------------------------------- нимб (32x16)
halo = np.zeros((16 * 2, 32 * 2, 4), float)
def paint(x, y, w, h):
    X, Y = np.meshgrid((np.arange(w * 2) + 0.5) / 2, (np.arange(h * 2) + 0.5) / 2)
    return mix(GOLD_H, GOLD_M, Y / max(h, 1) * 0.8)
# bar 4x1x1 at (0,0): раскладка 10x2; gem 2x2x2 at (12,0): 8x4
for (x, y, w, h) in ((0, 0, 10, 2), (12, 0, 8, 4)):
    halo[y * 2:(y + h) * 2, x * 2:(x + w) * 2, :3] = paint(x, y, w, h)
    halo[y * 2:(y + h) * 2, x * 2:(x + w) * 2, 3] = 255
Image.fromarray(halo.astype(np.uint8), "RGBA").save(os.path.join(OUT, "entity", "celestia_halo.png"))

# превью: 3 нагрудника/ботинок на плоскости
bg = Image.new("RGBA", tex.shape[1::-1], (50, 50, 60, 255))
bg.alpha_composite(Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA"))
bg.resize((bg.width * 6, bg.height * 6), Image.NEAREST).save(os.path.join(OUT, "..", "..", "..", "..", "..", "..", "..", "work", "prev_armor_tex.png")) if False else bg.resize((bg.width * 6, bg.height * 6), Image.NEAREST).save("/home/claude/work/prev_armor_tex.png")
