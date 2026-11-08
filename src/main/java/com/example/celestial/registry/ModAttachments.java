package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /** Есть ли у игрока мини-душа на плече. Сохраняется в мире, видна другим игрокам, не пропадает после смерти. */
    public static final Supplier<AttachmentType<Boolean>> MINI_SOUL = ATTACHMENTS.register("mini_soul",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    /** Душа уже рассказала вступление (первые 5 фраз): повторно оно не проигрывается. */
    public static final Supplier<AttachmentType<Boolean>> MINI_SOUL_INTRO_DONE = ATTACHMENTS.register("mini_soul_intro_done",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
