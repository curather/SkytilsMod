package skytils.skytilsmod.mixins;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import skytils.skytilsmod.Skytils;
import skytils.skytilsmod.events.GuiContainerEvent;
import skytils.skytilsmod.utils.ColorUtils;
import skytils.skytilsmod.utils.Utils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(GuiContainer.class)
public class MixinGuiContainer extends GuiScreen {

    private final GuiContainer that = (GuiContainer) (Object) this;

    @Shadow public Container inventorySlots;

    @Inject(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"))
    private void renderBids(Slot slot, CallbackInfo info)
    {
        if (this.that instanceof GuiChest)
        {
            GuiChest chest = (GuiChest)this.that;
            this.drawBids(slot);
            this.drawCommissions(slot);
            this.drawCurrentSelectedPet(slot);
        }
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(Slot slot, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.DrawSlotEvent.Pre(inventorySlots, slot))) ci.cancel();
    }

    @Inject(method = "handleMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.SlotClickEvent(inventorySlots, slot, slotId, clickedButton, clickType))) ci.cancel();
    }

    private void drawBids(Slot slot)
    {
        if (slot.getStack() != null && slot.getStack().hasTagCompound())
        {
            NBTTagCompound compound = slot.getStack().getTagCompound().getCompoundTag("display");

            if (compound.getTagId("Lore") == 9)
            {
                NBTTagList list = compound.getTagList("Lore", 8);

                if (list.tagCount() > 0)
                {
                    for (int j1 = 0; j1 < list.tagCount(); ++j1)
                    {
                        int slotLeft = slot.xDisplayPosition;
                        int slotTop = slot.yDisplayPosition;
                        int slotRight = slotLeft + 16;
                        int slotBottom = slotTop + 16;
                        String lore = EnumChatFormatting.getTextWithoutFormattingCodes(list.getStringTagAt(j1));
                        Matcher matcher = Pattern.compile("(?:(?:Top|Starting) bid|Buy it now): (?<coin>[0-9,]+) coins").matcher(lore);
                        int red = ColorUtils.to32BitColor(128, 255, 85, 85);
                        int green = ColorUtils.to32BitColor(128, 85, 255, 85);
                        int yellow = ColorUtils.to32BitColor(128, 255, 255, 85);

                        if (lore.startsWith("Status: Sold!"))
                        {
                            this.drawGradientRect(slotLeft, slotTop, slotRight, slotBottom, yellow, yellow);
                        }
                    }
                }
            }
        }
    }
    private void drawCommissions(Slot slot)
    {
        if (slot.getStack() != null && slot.getStack().hasTagCompound())
        {
            NBTTagCompound compound = slot.getStack().getTagCompound().getCompoundTag("display");

            if (compound.getTagId("Lore") == 9)
            {
                NBTTagList list = compound.getTagList("Lore", 8);

                if (list.tagCount() > 0)
                {
                    for (int j1 = 0; j1 < list.tagCount(); ++j1)
                    {
                        int slotLeft = slot.xDisplayPosition;
                        int slotTop = slot.yDisplayPosition;
                        int slotRight = slotLeft + 16;
                        int slotBottom = slotTop + 16;
                        String lore = EnumChatFormatting.getTextWithoutFormattingCodes(list.getStringTagAt(j1));
                        int green = ColorUtils.to32BitColor(150, 85, 255, 85);

                        if (lore.startsWith("COMPLETED"))
                        {
                            this.drawGradientRect(slotLeft, slotTop, slotRight, slotBottom, green, green);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void drawCurrentSelectedPet(Slot slot)
    {
        if (slot.getStack() != null && slot.getStack().hasTagCompound())
        {
            NBTTagCompound compound = slot.getStack().getTagCompound().getCompoundTag("display");

            if (compound.getTagId("Lore") == 9)
            {
                NBTTagList list = compound.getTagList("Lore", 8);

                if (list.tagCount() > 0)
                {
                    for (int j1 = 0; j1 < list.tagCount(); ++j1)
                    {
                        int slotLeft = slot.xDisplayPosition;
                        int slotTop = slot.yDisplayPosition;
                        int slotRight = slotLeft + 16;
                        int slotBottom = slotTop + 16;
                        String lore = EnumChatFormatting.getTextWithoutFormattingCodes(list.getStringTagAt(j1));
                        int green = ColorUtils.to32BitColor(150, 85, 255, 85);

                        if (lore.startsWith("Click to despawn"))
                        {
                            this.drawGradientRect(slotLeft, slotTop, slotRight, slotBottom, green, green);
                            break;
                        }
                    }
                }
            }
        }
    }
}