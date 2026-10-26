package com.mmob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Faz um mob criado pelo /mmob atacar o mob mais proximo de OUTRO tipo
 * (tambem criado pelo /mmob). Mobs do mesmo tipo nunca se atacam.
 *
 * - Mobs hostis (zumbi, esqueleto, etc.): so definimos o alvo, e a IA normal deles ataca.
 * - Mobs nao hostis (ovelha, vaca, etc.): este goal faz eles andarem ate o alvo e darem dano.
 */
public class MMobFightGoal extends Goal {
    private static final double SEARCH_RADIUS = 24.0;
    private static final float ANIMAL_DAMAGE = 3.0f;

    private final Mob mob;
    private final boolean hostile;
    private LivingEntity target;
    private int attackCooldown;

    public MMobFightGoal(Mob mob) {
        this.mob = mob;
        this.hostile = mob instanceof Monster;
        if (!hostile) {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        } else {
            this.setFlags(EnumSet.noneOf(Flag.class));
        }
    }

    private boolean isValidTarget(LivingEntity e) {
        return e != mob
                && e.isAlive()
                && e.getType() != mob.getType()
                && e.getTags().contains(MMobMod.TAG);
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % 10 != 0) {
            return false;
        }
        AABB box = mob.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Mob> candidates = mob.level().getEntitiesOfClass(Mob.class, box, this::isValidTarget);
        target = candidates.stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null
                && isValidTarget(target)
                && mob.distanceToSqr(target) < (SEARCH_RADIUS + 8) * (SEARCH_RADIUS + 8);
    }

    @Override
    public void start() {
        attackCooldown = 0;
        mob.setTarget(target);
    }

    @Override
    public void stop() {
        if (mob.getTarget() == target) {
            mob.setTarget(null);
        }
        if (!hostile) {
            mob.getNavigation().stop();
        }
        target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        mob.setTarget(target);

        if (hostile) {
            return; // a IA normal do mob hostil cuida do ataque
        }

        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        mob.getNavigation().moveTo(target, 1.3);

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        double reach = mob.getBbWidth() * 2.0 + target.getBbWidth();
        if (attackCooldown <= 0 && mob.distanceToSqr(target) <= reach * reach
                && mob.level() instanceof ServerLevel level) {
            attackCooldown = 20;
            mob.swing(InteractionHand.MAIN_HAND);
            target.hurtServer(level, mob.damageSources().mobAttack(mob), ANIMAL_DAMAGE);
        }
    }
}
