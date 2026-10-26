package com.mmob;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MMobMod implements ModInitializer {
    /** Tag colocada em todo mob criado pelo /mmob. Fica salva no mundo. */
    public static final String TAG = "mmob";

    private static final double SPACING = 1.5;
    private static final double START_DISTANCE = 3.0;

    /** goalSelector e protected em Mob; no 26.x o jogo nao e ofuscado, entao reflexao funciona direto. */
    private static Field goalSelectorField;

    private static void addFightGoal(Mob mob) {
        try {
            if (goalSelectorField == null) {
                goalSelectorField = Mob.class.getDeclaredField("goalSelector");
                goalSelectorField.setAccessible(true);
            }
            net.minecraft.world.entity.ai.goal.GoalSelector selector =
                    (net.minecraft.world.entity.ai.goal.GoalSelector) goalSelectorField.get(mob);
            selector.addGoal(1, new MMobFightGoal(mob));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("MMob: nao consegui acessar goalSelector", e);
        }
    }

    @Override
    public void onInitialize() {
        // Sempre que um mob com a tag carrega (spawn, reabrir o mundo, etc.), recebe a IA de briga.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Mob mob && mob.getTags().contains(TAG)) {
                addFightGoal(mob);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
                dispatcher.register(Commands.literal("mmob")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("mob", EntitySummonArgument.id())
                                        .suggests(net.minecraft.commands.synchronization.SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .then(Commands.argument("quantidade", IntegerArgumentType.integer(1, 200))
                                                .then(Commands.literal("reto")
                                                        .executes(ctx -> spawn(ctx, false)))
                                                .then(Commands.literal("lado")
                                                        .executes(ctx -> spawn(ctx, true))))))
                        .then(Commands.literal("clear")
                                .executes(MMobMod::clear))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, boolean lado) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        Holder.Reference<EntityType<?>> holder = EntitySummonArgument.getSummonableEntity(ctx, "mob");
        EntityType<?> type = holder.value();
        String name = holder.key().identifier().getPath();
        int quantidade = IntegerArgumentType.getInteger(ctx, "quantidade");

        float yaw = player.getYRot();
        Vec3 forward = Vec3.directionFromRotation(0, yaw);       // para onde o jogador olha
        Vec3 right = new Vec3(-Math.cos(Math.toRadians(yaw)), 0, -Math.sin(Math.toRadians(yaw)));
        Vec3 origin = player.position().add(forward.scale(START_DISTANCE));

        int spawned = 0;
        for (int i = 0; i < quantidade; i++) {
            Vec3 pos;
            if (lado) {
                // fila lateral, centralizada na frente do jogador
                double offset = (i - (quantidade - 1) / 2.0) * SPACING;
                pos = origin.add(right.scale(offset));
            } else {
                // fila reta, andando para frente
                pos = origin.add(forward.scale(i * SPACING));
            }

            Entity entity = type.create(level, EntitySpawnReason.COMMAND);
            if (entity == null) {
                continue;
            }
            entity.snapTo(pos.x, pos.y, pos.z, yaw + 180.0f, 0.0f);
            entity.addTag(TAG);

            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                        EntitySpawnReason.COMMAND, null);
                mob.setPersistenceRequired();
            }
            if (level.addFreshEntityWithPassengers(entity)) {
                spawned++;
            }
        }

        final int total = spawned;
        source.sendSuccess(() -> Component.literal("MMob: " + total + "x " + name
                + (lado ? " (fila de lado)" : " (fila reta)")), true);
        return total;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<Entity> toRemove = new ArrayList<>();
        for (Entity e : source.getLevel().getAllEntities()) {
            if (e.getTags().contains(TAG)) {
                toRemove.add(e);
            }
        }
        toRemove.forEach(Entity::discard);
        final int n = toRemove.size();
        source.sendSuccess(() -> Component.literal("MMob: " + n + " mobs removidos."), true);
        return n;
    }
}
