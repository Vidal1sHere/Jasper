package com.vidal1shere.jasper.client.effect.sound;

import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Minecraft does not expose the length of a sound, so a counted loop plays it again once it stopped, which can leave a tick between two runs
@OnlyIn(Dist.CLIENT)
final class PlayingSound implements PlayingEffect {
    private final SoundEffect effect;
    private final SoundSource source;
    private Sound current;
    private int plays;
    private boolean started;

    PlayingSound(SoundEffect effect, SoundSource source, EffectContext context) {
        this.effect = effect;
        this.source = source;
        play(context);
    }

    @Override
    public boolean tick(EffectContext context) {
        if (manager().isActive(current)) {
            return true;
        }
        // A sound that could not start, like an unknown one, is not tried again every tick
        if (!started || !effect.loop() || effect.loops() > 0 && plays >= effect.loops()) {
            return false;
        }
        play(context);
        return true;
    }

    @Override
    public void end() {
        manager().stop(current);
    }

    @Override
    public boolean endsByItself() {
        return !effect.loop() || effect.loops() > 0;
    }

    private void play(EffectContext context) {
        current = new Sound(SoundEvent.createVariableRangeEvent(effect.sound()), source, effect, context, effect.loop() && effect.loops() == 0);
        manager().play(current);
        started = manager().isActive(current);
        plays++;
    }

    private static SoundManager manager() {
        return Minecraft.getInstance().getSoundManager();
    }

    private static final class Sound extends AbstractTickableSoundInstance {
        private final EffectContext context;

        private Sound(SoundEvent event, SoundSource source, SoundEffect effect, EffectContext context, boolean looping) {
            super(event, source, SoundInstance.createUnseededRandom());
            this.context = context;
            this.volume = effect.volume();
            this.pitch = effect.pitch();
            this.looping = looping;
            this.delay = 0;
            move();
        }

        @Override
        public void tick() {
            move();
        }

        private void move() {
            Vec3 position = context.position();
            x = position.x;
            y = position.y;
            z = position.z;
        }
    }
}
