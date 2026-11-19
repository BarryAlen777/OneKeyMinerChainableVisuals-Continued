package cn.blockforge.generated.modad7bbe58.client;

import cn.blockforge.generated.modad7bbe58.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

/** Local-only preview chime sequence; never sends gameplay actions to the server. */
final class SoundFeedback {
    private static BlockPos watchedCenter;
    private static int watchedCount;
    private static int sequenceIndex;
    private static int sequenceTotal;
    private static int waitTicks;

    private SoundFeedback() { }

    static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            watchedCenter = null;
            reset();
            return;
        }
        BlockPos current = ChainScanner.center();
        if (watchedCenter != null && !watchedCenter.equals(current)
                && mc.level.isEmptyBlock(watchedCenter) && watchedCount > 1 && Config.PLAY_CHAIN_SOUNDS.get()) {
            // Cap at 10 notes: the old 32 was two-and-a-bit seconds of continuous harp and
            // read as a malfunction, not feedback. Steps of a fifth still climb clearly.
            sequenceTotal = Math.min(watchedCount, 10);
            sequenceIndex = 0;
            waitTicks = 0;
        }
        if (current != null) {
            watchedCenter = current.immutable();
            // displaySize(), not positions().size(): the aim-stability gate can blank the
            // shown list while the chime still needs the last anchored group's size.
            watchedCount = ChainScanner.displaySize();
        } else {
            watchedCenter = null;
            watchedCount = 0;
        }

        if (sequenceTotal <= 0) return;
        if (!Config.PLAY_CHAIN_SOUNDS.get()) {
            reset();
            return;
        }
        if (waitTicks-- > 0) return;
        float volume = Config.SOUND_VOLUME.get().floatValue();
        if (sequenceIndex < sequenceTotal) {
            int semitones = sequenceIndex / 5;
            float pitch = (float) (0.88D * Math.pow(2.0D, semitones / 12.0D));
            mc.player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), volume, pitch);
            sequenceIndex++;
            waitTicks = Math.max(1, 25 / Math.max(1, sequenceTotal));
        }
        if (sequenceIndex >= sequenceTotal) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), volume * 0.8F, 1.15F);
            reset();
        }
    }

    private static void reset() {
        sequenceIndex = 0;
        sequenceTotal = 0;
        waitTicks = 0;
    }
}
