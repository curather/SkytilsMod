package skytils.skytilsmod.mixins;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import skytils.skytilsmod.events.SendChatMessageEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import skytils.skytilsmod.gui.SignSelectionList;
import skytils.skytilsmod.utils.IEditSign;

@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    private final GuiScreen that = (GuiScreen) (Object) this;
    private static final List<String> IGNORE_TOOLTIPS = new ArrayList<>(Arrays.asList(" "));

    @Inject(method = "confirmClicked(ZI)V", at = @At("HEAD"))
    private void confirmClicked(boolean result, int id, CallbackInfo info)
    {
        if (this.that instanceof GuiEditSign)
        {
            GuiEditSign sign = (GuiEditSign)this.that;

            if (result)
            {
                String text = sign.tileSign.signText[0].getUnformattedText();
                sign.tileSign.markDirty();
                SignSelectionList.processSignData(sign.tileSign);
                ((IEditSign)sign).getSignSelectionList().add(text);
                this.that.mc.displayGuiScreen(null);
            }
            else
            {
                this.that.mc.displayGuiScreen(this.that);
            }
        }
    }

    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"))
    private void mouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo info) throws IOException
    {
        if (this.that instanceof GuiEditSign)
        {
            GuiEditSign sign = (GuiEditSign)this.that;

            if (((IEditSign)sign).getSignSelectionList() != null)
            {
                ((IEditSign)sign).getSignSelectionList().mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Inject(method = "mouseReleased(III)V", at = @At("HEAD"))
    private void mouseReleased(int mouseX, int mouseY, int state, CallbackInfo info) throws IOException
    {
        if (this.that instanceof GuiEditSign)
        {
            GuiEditSign sign = (GuiEditSign)this.that;

            if (((IEditSign)sign).getSignSelectionList() != null)
            {
                ((IEditSign)sign).getSignSelectionList().mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    @Inject(method = "handleMouseInput()V", at = @At("HEAD"))
    private void handleMouseInput(CallbackInfo info) throws IOException
    {
        if (this.that instanceof GuiEditSign)
        {
            GuiEditSign sign = (GuiEditSign)this.that;

            if (((IEditSign)sign).getSignSelectionList() != null)
            {
                ((IEditSign)sign).getSignSelectionList().handleMouseInput();
            }
         }
    }

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, boolean addToChat, CallbackInfo ci) {
        SendChatMessageEvent event = new SendChatMessageEvent(message, addToChat);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            ci.cancel();
        }
    }

}