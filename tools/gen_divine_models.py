"""Конвертер Blockbench (.bbmodel) -> Java-модели Minecraft (LayerDefinition) для брони Бога и ауры."""
import json, math, re, sys

SRC = 'divine-models/models/'  # положите папку divine-models рядом со скриптом

def f(v):
    s = ('%.4f' % v).rstrip('0').rstrip('.')
    if s in ('-0', ''): s = '0'
    if '.' not in s: s += '.0'
    return s + 'F'

def rad(deg): return f(math.radians(deg))

def conv_rot(rot):
    rx, ry, rz = rot
    return (-rx, -ry, rz)

def sanitize(n): return re.sub(r'[^a-zA-Z0-9_]', '_', n)

def gen(model_file, root_names, var_prefix, indent='        ', skip_parts=()):
    d = json.load(open(SRC + model_file))
    byid = {e['uuid']: e for e in d['elements']}
    lines = []
    used = set()

    def cube_call(e, origin):
        fx, fy, fz = e['from']; tx, ty, tz = e['to']
        x = -(tx - origin[0]); y = origin[1] - ty; z = fz - origin[2]
        w = tx - fx; h = ty - fy; dd = tz - fz
        u, v = e['uv_offset']
        inf = e.get('inflate', 0) or 0
        mirror = e.get('mirror_uv', False)
        s = 'texOffs(%d, %d)' % (u, v)
        if mirror: s += '.mirror()'
        s += '.addBox(%s, %s, %s, %s, %s, %s' % (f(x), f(y), f(z), f(w), f(h), f(dd))
        s += (', new CubeDeformation(%s))' % f(inf)) if inf else ')'
        if mirror: s += '.mirror(false)'
        return s

    def walk(group, parent_origin, parent_var, depth):
        o = group['origin']
        name = sanitize(group['name'])
        var = var_prefix + name
        while var in used: var += '_'
        used.add(var)
        rot = conv_rot(group.get('rotation', [0, 0, 0]))
        off = (-(o[0] - parent_origin[0]), parent_origin[1] - o[1], o[2] - parent_origin[2])
        # кубы без собственного поворота кладём прямо в группу, остальные - отдельными дочерними частями
        plain, rotated = [], []
        for c in group['children']:
            if isinstance(c, str):
                e = byid[c]
                r = e.get('rotation', [0, 0, 0])
                if any(abs(a) > 1e-9 for a in r): rotated.append(e)
                else: plain.append(e)
        builder = 'CubeListBuilder.create()'
        # у куба без поворота origin может отличаться от группы: считаем координаты относительно группы
        for e in plain: builder += '.' + cube_call(e, o)
        pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % (f(off[0]), f(off[1]), f(off[2]), rad(math.degrees(0)) if False else rad(rot[0]), rad(rot[1]), rad(rot[2]))
        if all(abs(a) < 1e-9 for a in rot):
            pose = 'PartPose.offset(%s, %s, %s)' % (f(off[0]), f(off[1]), f(off[2]))
        if group['name'] in skip_parts:
            return
        if parent_var is None:
            lines.append('%sPartDefinition %s = root.addOrReplaceChild("%s", %s, %s);' % (indent, var, group['name'], builder, pose))
        else:
            lines.append('%sPartDefinition %s = %s.addOrReplaceChild("%s", %s, %s);' % (indent, var, parent_var, group['name'], builder, pose))
        for e in rotated:
            eo = e.get('origin', o)
            r = conv_rot(e['rotation'])
            eoff = (-(eo[0] - o[0]), o[1] - eo[1], eo[2] - o[2])
            lines.append('%s%s.addOrReplaceChild("%s", CubeListBuilder.create().%s, PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));' % (
                indent, var, e['name'], cube_call(e, eo), f(eoff[0]), f(eoff[1]), f(eoff[2]), rad(r[0]), rad(r[1]), rad(r[2])))
        for c in group['children']:
            if isinstance(c, dict):
                walk(c, o, var, depth + 1)

    for g in d['outliner']:
        if isinstance(g, dict) and (root_names is None or g['name'] in root_names):
            walk(g, (0, 24, 0), None, 0)
    return '\n'.join(lines), d

if __name__ == '__main__':
    chest, _ = gen('divine_chestplate.bbmodel', None, 'c_')
    boots, _ = gen('divine_boots.bbmodel', None, 'b_')
    aura, d = gen('divine_aura.bbmodel', None, 'a_')
    EMPTY = '''        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);'''
    def missing(parts_present):
        out=[]
        for n in ['body','right_arm','left_arm','right_leg','left_leg']:
            if n not in parts_present:
                out.append('        root.addOrReplaceChild("%s", CubeListBuilder.create(), PartPose.ZERO);' % n)
        return '\n'.join(out)
    java = '''package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Модели брони Бога и ауры. Сгенерировано из Blockbench-моделей (tools/gen_divine_models.py):
 * divine_chestplate.bbmodel, divine_boots.bbmodel, divine_aura.bbmodel. Ручные правки лучше вносить в .bbmodel и перегенерировать.
 */
public final class DivineModels {
    public static final ModelLayerLocation CHEST_LAYER = layer("divine_chestplate");
    public static final ModelLayerLocation BOOTS_LAYER = layer("divine_boots");
    public static final ModelLayerLocation AURA_LAYER = layer("divine_aura");

    private DivineModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MODID, name), "main");
    }

    /** Нагрудник: тело, руки (рукава, наплечники, рога, перья). Текстура 128x128. */
    public static LayerDefinition createChestLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
''' + EMPTY + '\n' + missing(['body','right_arm','left_arm']) + '\n' + chest + '''
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Ботинки: ноги (голенище, носок, ремешок) и крылья на лодыжках. Текстура 128x128. */
    public static LayerDefinition createBootsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
''' + EMPTY + '\n' + missing(['right_leg','left_leg']) + '\n' + boots + '''
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Аура: нимб со шпилем над головой и два вращающихся кольца вокруг тела. Текстура 64x64. */
    public static LayerDefinition createAuraLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
''' + aura + '''
        return LayerDefinition.create(mesh, 64, 64);
    }
}
'''
    open('DivineModels.java', 'w', encoding='utf-8').write(java)
    print(len(java.splitlines()), 'lines')
