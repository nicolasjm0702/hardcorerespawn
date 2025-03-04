package br.nikao.hardcorerespawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public class GraveyardItem extends Item {

    public GraveyardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        Player user = context.getPlayer();
        if (user == null) {
            return InteractionResult.FAIL;
        }

        ItemStack itemStack = context.getItemInHand();
        String itemName = itemStack.getHoverName().getString();

        // If the item is not renamed, prompt renaming
        String localizedGraveyardName = Component.translatable("item.hardcorerespawn.graveyard").getString();
        if (localizedGraveyardName.equals(itemName)) {
            user.sendSystemMessage(Component.literal("Rename the item to the name of the player you want to revive!"));
            user.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
            return InteractionResult.FAIL;
        }

        // Track the clicked position as the origin
        BlockPos origin = context.getClickedPos();
        double x = origin.getX() + 0.5;
        double y = origin.getY() + 1;
        double z = origin.getZ() + 0.5;

        Origin pos = new Origin(x, y, z);

        if (level instanceof ServerLevel serverLevel && revivePlayer(serverLevel, itemName, pos)) {
            itemStack.shrink(1);

            serverLevel.playSound(null, x, y, z, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.0F, 1.0F);

            for (int i = 0; i < 8; i++) {
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask(i * 12, () -> renderParticles(pos, serverLevel)));
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private boolean revivePlayer(Level level, String playerName, Origin origin) {
        for (Player target : level.players()) {
            if (target.getName().getString().equals(playerName) && target instanceof ServerPlayer serverPlayer) {
                serverPlayer.teleportTo(origin.getX(), origin.getY(), origin.getZ());
                serverPlayer.respawn();
                serverPlayer.setHealth(serverPlayer.getMaxHealth());
                serverPlayer.setGameMode(GameType.SURVIVAL);
                serverPlayer.getFoodData().setFoodLevel(20);
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 1));
                return true;
            }
        }

        return false;
    }

    private void renderParticles(Origin origin, ServerLevel serverLevel) {
        // Circle
        int particleCount = 150;
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double offsetX = Math.cos(angle) * 1.5;
            double offsetZ = Math.sin(angle) * 1.5;
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                    origin.getX() + offsetX, origin.getY(), origin.getZ() + offsetZ,
                    1, 0, 0, 0, 0);
        }

        // Star (pentagram)
        int points = 5;
        double radius = 1.5;
        for (int i = 0; i < points; i++) {
            double angle1 = 2 * Math.PI * i / points;
            double angle2 = 2 * Math.PI * ((i + 2) % points) / points;
            double x1 = origin.getX() + Math.cos(angle1) * radius;
            double z1 = origin.getZ() + Math.sin(angle1) * radius;
            double x2 = origin.getX() + Math.cos(angle2) * radius;
            double z2 = origin.getZ() + Math.sin(angle2) * radius;

            // Interpolate line between the two points
            int lineSegments = 20;
            for (int j = 0; j <= lineSegments; j++) {
                double t = (double) j / lineSegments;
                double x = x1 + (x2 - x1) * t;
                double z = z1 + (z2 - z1) * t;
                serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, x, origin.getY(), z, 1, 0, 0, 0, 0);
            }
        }
    }
}

class Origin {
    private final double x;
    private final double y;
    private final double z;

    public Origin(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}