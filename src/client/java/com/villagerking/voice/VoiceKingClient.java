package com.villagerking.voice;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.nio.file.Files;
import java.nio.file.Path;

public class VoiceKingClient implements ClientModInitializer {
    private static VoiceListener listener;
    private static boolean wasDown = false;

    @Override
    public void onInitializeClient() {
        // NOTE: if the KeyMapping constructor shows an error in your version, see README.
        KeyMapping voiceKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.villagerking.voice", InputConstants.KEY_V, KeyMapping.Category.MISC));

        Path modelDir = FabricLoader.getInstance().getGameDir().resolve("villager_king").resolve("model");
        if (Files.isDirectory(modelDir)) {
            try { listener = new VoiceListener(modelDir); }
            catch (Throwable t) { t.printStackTrace(); }
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            boolean down = voiceKey.isDown();
            if (down && !wasDown) {
                if (listener == null) {
                    mc.player.displayClientMessage(Component.literal(
                            "Voice model not found. Put it in .minecraft/villager_king/model"), false);
                } else {
                    mc.player.displayClientMessage(Component.literal("Listening, my king..."), true);
                    listener.start(text -> mc.execute(() -> handle(mc, text)));
                }
            } else if (!down && wasDown && listener != null) {
                listener.stop();
            }
            wasDown = down;
        });
    }

    private static void handle(Minecraft mc, String said) {
        if (mc.player == null) return;
        String cmd = VoiceCommandParser.parse(said);
        if (cmd == null) {
            mc.player.displayClientMessage(Component.literal("I heard \"" + said + "\" but no order matched."), false);
            return;
        }
        mc.player.displayClientMessage(Component.literal("You said: " + said), false);
        mc.player.connection.sendCommand("king " + cmd);   // runs the same /king command
    }
}
