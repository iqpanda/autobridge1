package example.autobridge;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Auto speed-bridge: walk backwards off an edge and the mod handles
 * sneaking at the edge, aiming down, and placing blocks.
 * Works with ANY placeable block (anything that is a BlockItem).
 *
 * Toggle with the R key (rebindable in Controls).
 *
 * NOTE: Written against the Mojang-named, unobfuscated 26.x codebase.
 * Fabric API method names shift between versions; if something fails to
 * compile, check the Fabric API changelog for 26.2.
 */
public class AutoBridgeClient implements ClientModInitializer {

    private static final float BRIDGE_PITCH = 79.5f; // classic bridging angle
    private static final boolean LOCK_PITCH = true;

    private static KeyMapping toggleKey;
    private static boolean enabled = false;
    private static boolean weSetSneak = false;
    private static boolean weSetUse = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autobridge.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal("AutoBridge: " + (enabled ? "ON" : "OFF")));
            }
            if (!enabled) releaseKeys(mc);
        }

        LocalPlayer player = mc.player;
        if (!enabled || player == null || mc.level == null || !mc.mouseHandler.isMouseGrabbed()) {
            if (!enabled) releaseKeys(mc);
            return;
        }

        // 1. Make sure we're holding a block. Any block will do.
        if (!(player.getMainHandItem().getItem() instanceof BlockItem)) {
            int slot = findBlockSlot(player);
            if (slot == -1) {
                releaseKeys(mc);
                return;
            }
            player.getInventory().setSelectedSlot(slot);
        }

        // 2. Aim down at the bridging angle.
        if (LOCK_PITCH) {
            player.setXRot(BRIDGE_PITCH);
        }

        // 3. Sneak when the spot just ahead of our feet has no solid block.
        Vec3 pos = player.position();
        Vec3 motion = player.getDeltaMovement();
        BlockPos ahead = BlockPos.containing(
                pos.x + motion.x * 3.0, pos.y - 1.0, pos.z + motion.z * 3.0);
        boolean atEdge = player.onGround()
                && mc.level.getBlockState(ahead).canBeReplaced();

        if (atEdge) {
            mc.options.keyShift.setDown(true);
            weSetSneak = true;
        } else if (weSetSneak) {
            mc.options.keyShift.setDown(false);
            weSetSneak = false;
        }

        // 4. Place when the crosshair is on the side face of the block under us
        //    and the space next to it is empty.
        boolean shouldPlace = false;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos target = bhr.getBlockPos();
            BlockPos placeAt = target.relative(bhr.getDirection());
            boolean horizontalFace = bhr.getDirection().getAxis().isHorizontal();
            boolean belowFeet = target.getY() == player.blockPosition().getY() - 1;
            shouldPlace = horizontalFace && belowFeet
                    && mc.level.getBlockState(placeAt).canBeReplaced();
        }

        if (shouldPlace) {
            mc.options.keyUse.setDown(true);
            weSetUse = true;
        } else if (weSetUse) {
            mc.options.keyUse.setDown(false);
            weSetUse = false;
        }
    }

    private static int findBlockSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private static void releaseKeys(Minecraft mc) {
        if (weSetSneak) { mc.options.keyShift.setDown(false); weSetSneak = false; }
        if (weSetUse)   { mc.options.keyUse.setDown(false);   weSetUse = false; }
    }
}
