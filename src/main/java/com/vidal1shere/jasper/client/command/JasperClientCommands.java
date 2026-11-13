package com.vidal1shere.jasper.client.command;

import com.mojang.brigadier.context.CommandContext;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.loading.ItemAnimationLoader;
import com.vidal1shere.jasper.client.loading.PlayerAnimationLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

// The server /jasper commands still work: NeoForge sends a command the client tree does not know to the server
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class JasperClientCommands {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Jasper.MOD_ID)
                .then(Commands.literal("reload").executes(JasperClientCommands::reload)));
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        int items = ItemAnimationLoader.INSTANCE.reload(manager);
        int players = PlayerAnimationLoader.INSTANCE.reload(manager);
        int armPoses = PlayerAnimationLoader.ARM_POSES.reload(manager);
        context.getSource().sendSuccess(() -> Component.literal("Reloaded " + items + " item, " + players + " player and " + armPoses + " arm pose animations; errors go to the log"), false);
        return items + players + armPoses;
    }
}
