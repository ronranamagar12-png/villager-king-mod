package com.villagerking;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;
import java.util.function.Predicate;

public class VillagerCommander {
    public enum Order { NONE, FOLLOW, STAY, GUARD, CHOP_WOOD, FARM, MINE, BRING, BUILD_CASTLE }

    public static final Map<UUID, Order> ORDERS = new HashMap<>();
    static final Map<UUID, Integer> ROLE = new HashMap<>();   // 0 farm, 1 wood, 2 mine, 3 guard, 4 builder
    static final Set<UUID> MANAGED = new HashSet<>();
    static final Deque<Object[]> castleQueue = new ArrayDeque<>(); // {BlockPos, BlockState}
    static final Random RNG = new Random();
    static final int MAX_POP = 150;
    static Item bringItem = Items.DIAMOND;
    static int nextRole = 0;
    static int popTimer = 0;

    public static boolean isCommanded(Villager v) { return MANAGED.contains(v.getUUID()); }

    /** Give an order to every villager within 48 blocks of the king. */
    public static void command(ServerPlayer king, Order order, Item item) {
        ServerLevel level = king.level();
        if (item != null) bringItem = item;
        for (Villager v : level.getEntitiesOfClass(Villager.class, king.getBoundingBox().inflate(48)))
            ORDERS.put(v.getUUID(), order);
        if (order == Order.BUILD_CASTLE) planCastle(king.blockPosition().offset(6, 0, 6));
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) return;
            for (ServerPlayer king : server.getPlayerList().getPlayers()) {
                ServerLevel level = king.level();
                List<Villager> all = level.getEntitiesOfClass(Villager.class, king.getBoundingBox().inflate(64));

                for (Villager v : all) {
                    UUID id = v.getUUID();
                    MANAGED.add(id);
                    int role = ROLE.computeIfAbsent(id, k -> nextRole++ % 5);
                    switch (ORDERS.getOrDefault(id, Order.NONE)) {
                        case FOLLOW -> { if (v.distanceTo(king) > 3) go(v, king.blockPosition()); }
                        case STAY -> v.getNavigation().stop();
                        case GUARD -> guard(level, v, king);
                        case CHOP_WOOD -> breakNearest(level, v, king, s -> s.is(BlockTags.LOGS), 20);
                        case MINE -> breakNearest(level, v, king, VillagerCommander::isOre, 20);
                        case FARM -> breakNearest(level, v, king, VillagerCommander::isRipeCrop, 20);
                        case BRING -> bring(level, v, king);
                        case BUILD_CASTLE -> build(level, v);
                        default -> work(level, v, king, role);
                    }
                }

                // Population grows about once a minute
                if (++popTimer >= 6 && all.size() < MAX_POP) {
                    popTimer = 0;
                    BlockPos p = king.blockPosition().offset(RNG.nextInt(11) - 5, 0, RNG.nextInt(11) - 5);
                    if (!all.isEmpty()) all.get(0).getType().spawn(level, p, EntitySpawnReason.MOB_SUMMONED);
                }
            }
        });
    }

    static boolean isOre(BlockState s) {
        return s.is(Blocks.DIAMOND_ORE) || s.is(Blocks.DEEPSLATE_DIAMOND_ORE)
            || s.is(Blocks.COAL_ORE) || s.is(Blocks.DEEPSLATE_COAL_ORE)
            || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.GOLD_ORES);
    }

    static boolean isRipeCrop(BlockState s) {
        return s.getBlock() instanceof CropBlock c && c.isMaxAge(s);
    }

    static void go(Villager v, BlockPos p) {
        v.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.7);
    }

    /** Default job when the king gave no order. */
    static void work(ServerLevel level, Villager v, ServerPlayer king, int role) {
        switch (role) {
            case 0 -> breakNearest(level, v, king, VillagerCommander::isRipeCrop, 24);
            case 1 -> breakNearest(level, v, king, s -> s.is(BlockTags.LOGS), 24);
            case 2 -> breakNearest(level, v, king, VillagerCommander::isOre, 24);
            case 3 -> guard(level, v, king);
            case 4 -> {
                if (castleQueue.isEmpty() && RNG.nextInt(60) == 0) planHouse(level, king);
                build(level, v);
            }
        }
    }

    static void guard(ServerLevel level, Villager v, ServerPlayer king) {
        List<Monster> mobs = level.getEntitiesOfClass(Monster.class, v.getBoundingBox().inflate(14));
        if (mobs.isEmpty()) { if (v.distanceTo(king) > 5) go(v, king.blockPosition()); return; }
        Monster t = mobs.get(0);
        go(v, t.blockPosition());
        if (v.distanceTo(t) < 2.5) t.hurtServer(level, level.damageSources().mobAttack(v), 6f);
    }

    /** Breaks the nearest matching block; drops go into a chest near the king (or on the ground). */
    static void breakNearest(ServerLevel level, Villager v, ServerPlayer king,
                             Predicate<BlockState> match, int r) {
        BlockPos best = null; double bd = 1e9;
        for (BlockPos p : BlockPos.betweenClosed(v.blockPosition().offset(-r, -4, -r),
                                                 v.blockPosition().offset(r, 4, r))) {
            if (!match.test(level.getBlockState(p))) continue;
            double d = p.distSqr(v.blockPosition());
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        if (best == null) { if (v.distanceTo(king) > 20) go(v, king.blockPosition()); return; }
        if (bd >= 6) { go(v, best); return; }

        BlockState st = level.getBlockState(best);
        List<ItemStack> drops = Block.getDrops(st, level, best, level.getBlockEntity(best));
        level.destroyBlock(best, false, v);
        if (st.getBlock() instanceof CropBlock c) level.setBlockAndUpdate(best, c.getStateForAge(0)); // replant
        for (ItemStack d : drops) store(level, king, d, v.blockPosition());
    }

    static void store(ServerLevel level, ServerPlayer king, ItemStack stack, BlockPos fallback) {
        BlockPos kp = king.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(kp.offset(-30, -8, -30), kp.offset(30, 8, 30))) {
            if (level.getBlockEntity(p) instanceof Container c) {
                stack = HopperBlockEntity.addItem(null, c, stack, null);
                if (stack.isEmpty()) return;
            }
        }
        Block.popResource(level, fallback, stack);
    }

    static void bring(ServerLevel level, Villager v, ServerPlayer king) {
        if (v.getMainHandItem().is(bringItem)) {
            go(v, king.blockPosition());
            if (v.distanceTo(king) < 3) {
                Block.popResource(level, v.blockPosition(), v.getMainHandItem().copy());
                v.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                ORDERS.put(v.getUUID(), Order.NONE);
            }
            return;
        }
        BlockPos kp = king.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(kp.offset(-30, -8, -30), kp.offset(30, 8, 30))) {
            if (!(level.getBlockEntity(p) instanceof Container c)) continue;
            for (int i = 0; i < c.getContainerSize(); i++) {
                if (!c.getItem(i).is(bringItem)) continue;
                if (v.blockPosition().distSqr(p) < 6) {
                    v.setItemInHand(InteractionHand.MAIN_HAND, c.removeItem(i, 1));
                } else go(v, p.immutable());
                return;
            }
        }
    }

    static void planCastle(BlockPos o) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        int s = 21;
        for (int h = 0; h < 6; h++)
            for (int i = 0; i < s; i++)
                for (int j = 0; j < s; j++) {
                    boolean edge = i == 0 || j == 0 || i == s - 1 || j == s - 1;
                    boolean gate = i == s / 2 && j == 0 && h < 3;
                    if (edge && !gate) castleQueue.add(new Object[]{o.offset(i, h, j), wall});
                }
        int[][] corners = {{0, 0}, {s - 1, 0}, {0, s - 1}, {s - 1, s - 1}};
        for (int[] c : corners)
            for (int h = 6; h < 11; h++)
                castleQueue.add(new Object[]{o.offset(c[0], h, c[1]), wall});
    }

    public static void planHouse(ServerLevel level, ServerPlayer king) {
        BlockPos c = king.blockPosition().offset(RNG.nextInt(41) - 20, 0, RNG.nextInt(41) - 20);
        BlockPos o = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, c);
        BlockState wall = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int h = 0; h < 4; h++)
            for (int i = 0; i < 5; i++)
                for (int j = 0; j < 5; j++) {
                    boolean edge = i == 0 || j == 0 || i == 4 || j == 4;
                    boolean door = i == 2 && j == 0 && h < 2;
                    BlockState s = (h == 3 || (edge && !door)) ? wall : air;
                    castleQueue.add(new Object[]{o.offset(i, h, j), s});
                }
    }

    /** Builders walk toward the next block and place a few per pass. */
    static void build(ServerLevel level, Villager v) {
        if (castleQueue.isEmpty()) return;
        BlockPos next = (BlockPos) castleQueue.peek()[0];
        if (v.blockPosition().distSqr(next) > 100) { go(v, next); return; }
        for (int n = 0; n < 5 && !castleQueue.isEmpty(); n++) {
            Object[] b = castleQueue.poll();
            level.setBlockAndUpdate((BlockPos) b[0], (BlockState) b[1]);
        }
    }
}
