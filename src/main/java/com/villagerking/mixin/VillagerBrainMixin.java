package com.villagerking.mixin;

import com.villagerking.VillagerCommander;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerBrainMixin {
    // Stops the vanilla villager Brain (sleep, wander, gossip) for villagers the king controls.
    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void king$skipBrain(CallbackInfo ci) {
        if (VillagerCommander.isCommanded((Villager) (Object) this)) ci.cancel();
    }
}
