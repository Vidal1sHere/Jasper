package com.vidal1shere.jasper.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.ServerAnimations;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.PlayOptions;
import com.vidal1shere.jasper.util.EnumNames;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Jasper.MOD_ID)
public final class JasperCommands {
    private static final SuggestionProvider<CommandSourceStack> LAYERS = (context, builder) -> SharedSuggestionProvider.suggestResource(JasperLayers.sorted().stream().map(Layer::id), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> play = Commands.literal("play");
        LiteralArgumentBuilder<CommandSourceStack> stop = Commands.literal("stop");
        var playLayer = Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS);
        var stopLayer = Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS);
        for (AnimationTarget target : AnimationTarget.values()) {
            String name = EnumNames.name(target);
            playLayer.then(Commands.literal(name).then(Commands.argument("animation", ResourceLocationArgument.id())
                    .executes(context -> play(context, target))
                    .then(Commands.argument("speed", FloatArgumentType.floatArg(0.01F)).executes(context -> play(context, target))
                            .then(Commands.argument("loop", BoolArgumentType.bool()).executes(context -> play(context, target))))));
            stopLayer.then(Commands.literal(name).executes(context -> stop(context, target)));
        }
        event.getDispatcher().register(Commands.literal(Jasper.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(play.then(Commands.argument("entities", EntityArgument.entities()).then(playLayer)))
                .then(stop.then(Commands.argument("entities", EntityArgument.entities()).then(stopLayer))));
    }

    private static int play(CommandContext<CommandSourceStack> context, AnimationTarget target) throws CommandSyntaxException {
        Layer layer = layer(context);
        ResourceLocation animation = ResourceLocationArgument.getId(context, "animation");
        float speed = has(context, "speed") ? FloatArgumentType.getFloat(context, "speed") : 1.0F;
        boolean loop = has(context, "loop") && BoolArgumentType.getBool(context, "loop");
        PlayOptions options = new PlayOptions(speed, target.hand(), loop);
        int count = 0;
        for (Entity entity : EntityArgument.getEntities(context, "entities")) {
            if (entity instanceof LivingEntity living) {
                ServerAnimations.play(living, layer, target, animation, options);
                count++;
            }
        }
        int played = count;
        context.getSource().sendSuccess(() -> Component.literal("Playing " + animation + " on " + layer.id() + " (" + EnumNames.name(target) + (loop ? ", looped" : "") + (layer.policy().persistent() ? ", saved" : "") + ") for " + played + " entities"), false);
        return played;
    }

    private static boolean has(CommandContext<CommandSourceStack> context, String argument) {
        return context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(argument));
    }

    private static int stop(CommandContext<CommandSourceStack> context, AnimationTarget target) throws CommandSyntaxException {
        Layer layer = layer(context);
        int count = 0;
        for (Entity entity : EntityArgument.getEntities(context, "entities")) {
            if (entity instanceof LivingEntity living) {
                ServerAnimations.stop(living, layer, target);
                count++;
            }
        }
        int stopped = count;
        context.getSource().sendSuccess(() -> Component.literal("Stopped " + layer.id() + " on " + stopped + " entities (drivers keep running)"), false);
        return stopped;
    }

    private static Layer layer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "layer");
        Layer layer = JasperLayers.get(id);
        if (layer == null) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown animation layer " + id)).create();
        }
        return layer;
    }
}
