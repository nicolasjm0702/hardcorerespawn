package br.nikao.hardcorerespawn;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            boolean playerRevived = false;
            String itemName = itemStack.getHoverName().getString();

            if ("Graveyard".equals(itemName)) {
                player.sendSystemMessage(Component.literal("Rename the item to the name of the player you want to revive!"));
                return InteractionResultHolder.fail(itemStack);
            }

            player.sendSystemMessage(Component.literal("Attempting to respawn player: " + itemName));

            for (Player playerEntity : world.players()) {
                if (playerEntity.getName().getString().equals(itemName)) {
                    if (playerEntity instanceof ServerPlayer serverPlayer) {
                        serverPlayer.teleportTo(player.getX(), player.getY(), player.getZ());
                        serverPlayer.respawn();
                        serverPlayer.setHealth(serverPlayer.getMaxHealth());
                        serverPlayer.setGameMode(GameType.SURVIVAL);
                        serverPlayer.sendSystemMessage(Component.literal("You have been revived!"));
                        player.sendSystemMessage(Component.literal("Player revived!"));
                        playerRevived = true;
                        break;
                    }
                }
            }

            if (playerRevived) {
                itemStack.shrink(1); // Consume one item
            }
        }
        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
    }
}