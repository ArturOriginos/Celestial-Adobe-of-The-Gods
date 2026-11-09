package com.example.celestial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Повороты и масштаб вокруг точки (пивота) в пикселях модели Blockbench. */
final class BBAnim {
    private BBAnim() {}

    static void rotX(PoseStack ps, float px, float py, float pz, float deg) {
        ps.translate(px, py, pz);
        ps.mulPose(Axis.XP.rotationDegrees(deg));
        ps.translate(-px, -py, -pz);
    }

    static void rotY(PoseStack ps, float px, float py, float pz, float deg) {
        ps.translate(px, py, pz);
        ps.mulPose(Axis.YP.rotationDegrees(deg));
        ps.translate(-px, -py, -pz);
    }

    static void rotZ(PoseStack ps, float px, float py, float pz, float deg) {
        ps.translate(px, py, pz);
        ps.mulPose(Axis.ZP.rotationDegrees(deg));
        ps.translate(-px, -py, -pz);
    }

    /** Поворот сразу по X и Z вокруг одной точки (руки). */
    static void rotXZ(PoseStack ps, float px, float py, float pz, float xDeg, float zDeg) {
        ps.translate(px, py, pz);
        ps.mulPose(Axis.XP.rotationDegrees(xDeg));
        ps.mulPose(Axis.ZP.rotationDegrees(zDeg));
        ps.translate(-px, -py, -pz);
    }

    static void scaleXZ(PoseStack ps, float px, float py, float pz, float s) {
        ps.translate(px, py, pz);
        ps.scale(s, 1.0F, s);
        ps.translate(-px, -py, -pz);
    }
}
