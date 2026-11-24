package com.villagerking;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class VillagerKingMod implements ModInitializer {
    @Override
    public void onInitialize() {
        VillagerCommander.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> king = Commands.literal("king");
        king.then(order("follow", VillagerCommander.Order.FOLLOW));
        king.then(order("stay", VillagerCommander.Order.STAY));
        king.then(order("guard", VillagerCommander.Order.GUARD));
        king.then(order("wood", VillagerCommander.Order.CHOP_WOOD));
        king.then(order("mine", VillagerCommander.Order.MINE));
        king.then(order("farm", VillagerCommander.Order.FARM));
        king.then(order("work", VillagerCommander.Order.NONE));
        king.then(order("castle", VillagerCommander.Order.BUILD_CASTLE));
        king.then(Commands.literal("house").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VillagerCommander.planHouse(p.level(), p);
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Villagers will build a house."), false);
            return 1;
        }));
        king.then(Commands.literal("bring")
                .then(bring("diamond", Items.DIAMOND))
                .then(bring("netherite", Items.NETHERITE_INGOT))
                .then(bring("gold", Items.GOLD_INGOT))
                .then(bring("iron", Items.IRON_INGOT)));
        d.register(king);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> order(String name, VillagerCommander.Order o) {
        return Commands.literal(name).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VillagerCommander.command(p, o, null);
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Yes, my king: " + name), false);
            return 1;
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> bring(String name, Item item) {
        return Commands.literal(name).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VillagerCommander.command(p, VillagerCommander.Order.BRING, item);
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Bringing " + name + ", my king."), false);
            return 1;
        });
    }
}
