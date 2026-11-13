package com.vidal1shere.jasper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.JasperAnimations;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

@EventBusSubscriber(modid = Jasper.MOD_ID)
public final class JasperCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> itemPlay = Commands.literal("play");
        LiteralArgumentBuilder<CommandSourceStack> itemStop = Commands.literal("stop");
        LiteralArgumentBuilder<CommandSourceStack> playerPlay = Commands.literal("play");
        for (InteractionHand hand : InteractionHand.values()) {
            String name = hand.name().toLowerCase(Locale.ROOT);
            itemPlay.then(Commands.literal(name).then(animation(context -> play(context, hand, false))));
            itemStop.then(Commands.literal(name).executes(context -> stopItem(context, hand)));
            playerPlay.then(Commands.literal(name).then(animation(context -> play(context, hand, true))));
        }
        event.getDispatcher().register(Commands.literal(Jasper.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("item").then(itemPlay).then(itemStop))
                .then(Commands.literal("player").then(playerPlay).then(Commands.literal("stop").executes(JasperCommands::stopPlayer))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> animation(Command<CommandSourceStack> command) {
        return Commands.argument("animation", ResourceLocationArgument.id()).executes(command)
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0.01F)).executes(command));
    }

    private static int play(CommandContext<CommandSourceStack> context, InteractionHand hand, boolean playerLayer) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation animation = ResourceLocationArgument.getId(context, "animation");
        float speed = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("speed")) ? FloatArgumentType.getFloat(context, "speed") : 1.0F;
        if (playerLayer) {
            JasperAnimations.playPlayer(player, hand, animation, speed);
        } else {
            JasperAnimations.playItem(player, hand, animation, speed);
        }
        context.getSource().sendSuccess(() -> Component.literal("Playing " + animation), false);
        return 1;
    }

    private static int stopItem(CommandContext<CommandSourceStack> context, InteractionHand hand) throws CommandSyntaxException {
        JasperAnimations.stopItem(context.getSource().getPlayerOrException(), hand);
        return 1;
    }

    private static int stopPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        JasperAnimations.stopPlayer(context.getSource().getPlayerOrException());
        return 1;
    }
}
