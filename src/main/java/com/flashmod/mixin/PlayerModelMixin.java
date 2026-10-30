package com.flashmod.mixin;

import com.flashmod.ModItems;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animacion de correr de Flash: brazos hacia atras, zancadas largas y rapidas. */
@Mixin(PlayerEntityModel.class)
public class PlayerModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void flashmod$run(LivingEntity e, float limbAngle, float limbDist, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(e instanceof PlayerEntity p) || !p.isSprinting() || limbDist < 0.1f || !ModItems.hasSuit(p)) return;
        PlayerEntityModel<?> m = (PlayerEntityModel<?>) (Object) this;

        float stride = MathHelper.cos(limbAngle * 0.9f) * 1.7f * Math.min(1f, limbDist * 1.5f);
        m.rightLeg.pitch = stride;
        m.leftLeg.pitch = -stride;
        m.rightLeg.yaw = 0; m.leftLeg.yaw = 0;

        // brazos barridos hacia atras, como en la serie
        m.rightArm.pitch = 1.3f; m.leftArm.pitch = 1.3f;
        m.rightArm.yaw = 0; m.leftArm.yaw = 0;
        m.rightArm.roll = 0.12f; m.leftArm.roll = -0.12f;
        m.body.pitch = 0.15f;

        copy(m.leftSleeve, m.leftArm); copy(m.rightSleeve, m.rightArm);
        copy(m.leftPants, m.leftLeg); copy(m.rightPants, m.rightLeg);
        copy(m.jacket, m.body);
    }

    private static void copy(ModelPart to, ModelPart from) { to.copyTransform(from); }
}
