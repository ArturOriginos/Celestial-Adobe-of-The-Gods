#!/usr/bin/env python3
"""
Конвертер Blockbench (.bbmodel, box UV / свободная модель) в формат мода:
  assets/celestial/bbdata/NAME.txt       - кубы (группа, from, to, UV шести граней)
  assets/celestial/bbdata/NAME.rig.json  - кости (иерархия, origin, базовый поворот) и анимации

Использование: python3 tools/bbmodel_to_bbdata.py модель.bbmodel имя_в_моде [папка_bbdata]
Кубы пишутся по одному на строку: "группа x0 y0 z0 x1 y1 z1 + UV граней north east south west up down (u0 v0 u1 v1)".
Если у куба есть поворот по одной оси, в конец добавляется "ox oy oz ось угол".
"""
import json
import sys

FACES = ["north", "east", "south", "west", "up", "down"]


def num(v):
    return float(v) if not isinstance(v, str) else float(v.strip() or 0)


def fmt(x):
    x = round(float(x), 4)
    return str(int(x)) if x == int(x) else str(x)


def main():
    src, name = sys.argv[1], sys.argv[2]
    out = sys.argv[3] if len(sys.argv) > 3 else "src/main/resources/assets/celestial/bbdata"
    d = json.load(open(src, encoding="utf-8"))
    els = {e["uuid"]: e for e in d["elements"]}
    tw, th = d["resolution"]["width"], d["resolution"]["height"]

    groups = []   # {n, p, o, r}
    cube_group = {}

    def walk(nodes, parent):
        for n in nodes:
            if isinstance(n, str):
                if n in els:
                    cube_group[n] = parent
                continue
            idx = len(groups)
            groups.append({"n": n["name"], "p": parent,
                           "o": [num(v) for v in n.get("origin", [0, 0, 0])],
                           "r": [num(v) for v in (n.get("rotation") or [0, 0, 0])]})
            walk(n.get("children", []), idx)

    walk(d["outliner"], -1)
    if not groups:
        groups.append({"n": "root", "p": -1, "o": [0, 0, 0], "r": [0, 0, 0]})
    lost = [u for u in els if u not in cube_group]
    for u in lost:
        cube_group[u] = 0

    lines = [f"{fmt(tw)} {fmt(th)}"]
    skipped_rot = 0
    for e in d["elements"]:
        if e.get("type", "cube") != "cube":
            continue
        row = [cube_group[e["uuid"]]]
        row += [num(v) for v in e["from"]] + [num(v) for v in e["to"]]
        for f in FACES:
            face = e.get("faces", {}).get(f) or {}
            uv = face.get("uv") or [0, 0, 0, 0]
            if face.get("texture") is None and not face.get("uv"):
                uv = [0, 0, 0, 0]
            row += [num(v) for v in uv]
        rot = e.get("rotation") or [0, 0, 0]
        axes = [i for i, v in enumerate(rot) if num(v) != 0]
        if len(axes) == 1:
            o = e.get("origin", [0, 0, 0])
            row += [num(o[0]), num(o[1]), num(o[2]), axes[0], num(rot[axes[0]])]
        elif len(axes) > 1:
            skipped_rot += 1
        lines.append(" ".join(fmt(v) for v in row))
    open(f"{out}/{name}.txt", "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")

    anims = {}
    gname = {g["n"]: i for i, g in enumerate(groups)}
    for a in d.get("animations", []):
        ga = {}
        for an in a.get("animators", {}).values():
            if an.get("name") not in gname:
                continue
            chans = {}
            for kf in an.get("keyframes", []):
                ch = kf["channel"]
                if ch not in ("rotation", "position", "scale"):
                    continue
                dp = kf["data_points"][0]
                chans.setdefault(ch, []).append(
                    [num(kf["time"]), num(dp.get("x", 0)), num(dp.get("y", 0)), num(dp.get("z", 0)),
                     1 if kf.get("interpolation") == "catmullrom" else 0])
            for ch in chans:
                chans[ch].sort(key=lambda k: k[0])
            if chans:
                ga[an["name"]] = chans
        anims[a["name"]] = {"len": num(a["length"]), "loop": "loop" if a.get("loop") == "loop" else "once", "g": ga}
    json.dump({"groups": groups, "anims": anims}, open(f"{out}/{name}.rig.json", "w", encoding="utf-8"),
              separators=(",", ":"))
    print(f"{name}: {len(lines) - 1} кубов, {len(groups)} групп, анимации: {list(anims)}"
          + (f", потеряно кубов вне групп: {len(lost)}" if lost else "")
          + (f", кубов с поворотом по нескольким осям (без поворота): {skipped_rot}" if skipped_rot else ""))


if __name__ == "__main__":
    main()
