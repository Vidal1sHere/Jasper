package com.vidal1shere.jasper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.JasperAnimations;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        LiteralArgumentBuilder<CommandSourceStack> itemOverlay = Commands.literal("set");
        LiteralArgumentBuilder<CommandSourceStack> itemClear = Commands.literal("clear");
        LiteralArgumentBuilder<CommandSourceStack> playerPlay = Commands.literal("play");
        LiteralArgumentBuilder<CommandSourceStack> playerOverlay = Commands.literal("set");
        for (InteractionHand hand : InteractionHand.values()) {
            String name = hand.name().toLowerCase(Locale.ROOT);
            itemPlay.then(Commands.literal(name).then(animation(context -> play(context, hand, false, false))));
            itemStop.then(Commands.literal(name).executes(context -> stopItem(context, hand)));
            itemOverlay.then(Commands.literal(name).then(animation(context -> play(context, hand, false, true))));
            itemClear.then(Commands.literal(name).executes(context -> clearItemOverlay(context, hand)));
            playerPlay.then(Commands.literal(name).then(animation(context -> play(context, hand, true, false))));
            playerOverlay.then(Commands.literal(name).then(animation(context -> play(context, hand, true, true))));
        }
        event.getDispatcher().register(Commands.literal(Jasper.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("item").then(itemPlay).then(itemStop)
                        .then(Commands.literal("overlay").then(itemOverlay).then(itemClear)))
                .then(Commands.literal("player").then(playerPlay).then(Commands.literal("stop").executes(JasperCommands::stopPlayer))
                        .then(Commands.literal("overlay").then(playerOverlay).then(Commands.literal("clear").executes(JasperCommands::clearPlayerOverlay))))
                .then(Commands.literal("entity")
                        .then(Commands.literal("play").then(Commands.argument("target", EntityArgument.entity()).then(animation(context -> playEntity(context, false)))))
                        .then(Commands.literal("stop").then(Commands.argument("target", EntityArgument.entity()).executes(context -> stopEntity(context, false))))
                        .then(Commands.literal("overlay")
                                .then(Commands.literal("set").then(Commands.argument("target", EntityArgument.entity()).then(animation(context -> playEntity(context, true)))))
                                .then(Commands.literal("clear").then(Commands.argument("target", EntityArgument.entity()).executes(context -> stopEntity(context, true)))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> animation(Command<CommandSourceStack> command) {
        return Commands.argument("animation", ResourceLocationArgument.id()).executes(command)
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0.01F)).executes(command));
    }

    private static int play(CommandContext<CommandSourceStack> context, InteractionHand hand, boolean playerLayer, boolean overlay) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation animation = ResourceLocationArgument.getId(context, "animation");
        float speed = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("speed")) ? FloatArgumentType.getFloat(context, "speed") : 1.0F;
        if (playerLayer) {
            if (overlay) {
                JasperAnimations.setPlayerOverlay(player, hand, animation, speed);
            } else {
                JasperAnimations.playPlayer(player, hand, animation, speed);
            }
        } else if (overlay) {
            JasperAnimations.setItemOverlay(player, hand, animation, speed);
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

    private static int playEntity(CommandContext<CommandSourceStack> context, boolean overlay) throws CommandSyntaxException {
        LivingEntity entity = living(context);
        ResourceLocation animation = ResourceLocationArgument.getId(context, "animation");
        float speed = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("speed")) ? FloatArgumentType.getFloat(context, "speed") : 1.0F;
        if (overlay) {
            JasperAnimations.setEntityOverlay(entity, animation, speed);
        } else {
            JasperAnimations.playEntity(entity, animation, speed);
        }
        context.getSource().sendSuccess(() -> Component.literal("Playing " + animation + " on " + entity.getName().getString()), false);
        return 1;
    }

    private static int stopEntity(CommandContext<CommandSourceStack> context, boolean overlay) throws CommandSyntaxException {
        LivingEntity entity = living(context);
        if (overlay) {
            JasperAnimations.clearEntityOverlay(entity);
        } else {
            JasperAnimations.stopEntity(entity);
        }
        return 1;
    }

    private static LivingEntity living(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        if (!(entity instanceof LivingEntity living)) {
            throw new SimpleCommandExceptionType(Component.literal("Expected a living entity")).create();
        }
        return living;
    }

    private static int clearItemOverlay(CommandContext<CommandSourceStack> context, InteractionHand hand) throws CommandSyntaxException {
        JasperAnimations.clearItemOverlay(context.getSource().getPlayerOrException(), hand);
        return 1;
    }

    private static int clearPlayerOverlay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        JasperAnimations.clearPlayerOverlay(context.getSource().getPlayerOrException());
        return 1;
    }
}
