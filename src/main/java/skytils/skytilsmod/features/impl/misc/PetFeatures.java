package skytils.skytilsmod.features.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import skytils.skytilsmod.Skytils;
import skytils.skytilsmod.core.structure.FloatPair;
import skytils.skytilsmod.core.structure.GuiElement;
import skytils.skytilsmod.events.SendChatMessageEvent;
import skytils.skytilsmod.events.SendPacketEvent;
import skytils.skytilsmod.utils.ItemUtil;
import skytils.skytilsmod.utils.RenderUtil;
import skytils.skytilsmod.utils.Utils;
import skytils.skytilsmod.utils.graphics.ScreenRenderer;
import skytils.skytilsmod.utils.graphics.SmartFontRenderer;
import skytils.skytilsmod.utils.graphics.colors.CommonColors;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PetFeatures {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static long lastPetConfirmation = 0;
    private static long lastPetLockNotif = 0;

    public static String lastPet = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (!Utils.inSkyblock || event.type == 2) return;
        String message = event.message.getFormattedText();
        if (message.startsWith("§r§aYou despawned your §r§")) {
            lastPet = null;
        } else if (message.startsWith("§r§aYou summoned your §r")) {
            Matcher petMatcher = Pattern.compile("§r§aYou summoned your §r(?<pet>.+)§r§a!§r").matcher(message);
            if (petMatcher.find()) {
                lastPet = StringUtils.stripControlCodes(petMatcher.group("pet"));
            } else mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7cSkytils failed to capture equipped pet."));
        } else if (message.startsWith("§cAutopet §eequipped your §7[Lvl ")) {
            Matcher autopetMatcher = Pattern.compile("§cAutopet §eequipped your §7\\[Lvl (?<level>\\d+)\\] (?<pet>.+)§e! §a§lVIEW RULE§r").matcher(message);
            if (autopetMatcher.find()) {
                lastPet = StringUtils.stripControlCodes(autopetMatcher.group("pet"));
            } else mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7cSkytils failed to capture equipped pet."));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChatLow(ClientChatReceivedEvent event) {
        if (!Utils.inSkyblock) return;
        String unformatted = StringUtils.stripControlCodes(event.message.getUnformattedText());

        if (Skytils.config.hideAutopetMessages) {
            if (unformatted.contains("Autopet equipped your")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (!Utils.inSkyblock) return;

        if (Skytils.config.petItemConfirmation && (event.packet instanceof C02PacketUseEntity || event.packet instanceof C08PacketPlayerBlockPlacement)) {
            ItemStack item = mc.thePlayer.getHeldItem();
            if (item != null) {
                String itemId = ItemUtil.getSkyBlockItemID(item);
                if (itemId != null) {
                    if (itemId.contains("PET_ITEM") || itemId.contains("CARROT_CANDY") || ItemUtil.getItemLore(item).stream().anyMatch(s -> s.contains("PET ITEM"))) {
                        if (System.currentTimeMillis() - lastPetConfirmation > 5000) {
                            event.setCanceled(true);
                            if (System.currentTimeMillis() - lastPetLockNotif > 10000) {
                                lastPetLockNotif = System.currentTimeMillis();
                                ChatComponentText cc = new ChatComponentText("\u00a7cSkytils stopped you from using that pet item! \u00a76Click this message to disable the lock.");
                                cc.setChatStyle(cc.getChatStyle()
                                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/disableskytilspetitemlock"))
                                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to disable the pet item lock for 5 seconds.")))
                                );
                                mc.thePlayer.addChatMessage(cc);
                            }
                        } else {
                            lastPetConfirmation = 0;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendChatMessage(SendChatMessageEvent event) {
        if (event.message.equals("/disableskytilspetitemlock") && !event.addToChat) {
            lastPetConfirmation = System.currentTimeMillis();
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7aYou may now apply pet items for 5 seconds."));
            event.setCanceled(true);
        }
    }

    static {
        new DolphinPetDisplay();
    }

    public static class DolphinPetDisplay extends GuiElement {

        private static final ResourceLocation ICON = new ResourceLocation("skytils", "icons/dolphin.png");

        public DolphinPetDisplay() {
            super("Dolphin Pet Display", new FloatPair(50, 20));
            Skytils.GUIMANAGER.registerElement(this);
        }

        @Override
        public void render() {
            EntityPlayerSP player = mc.thePlayer;
            if (this.getToggled() && Utils.inSkyblock && player != null && mc.theWorld != null) {
                if (!Objects.equals(lastPet, "Dolphin")) return;
                float x = getActualX();
                float y = getActualY();
                GlStateManager.scale(this.getScale(), this.getScale(), 1.0);
                RenderUtil.renderTexture(ICON, (int)x, (int)y);
                List<EntityPlayer> players = mc.theWorld.getPlayers(EntityOtherPlayerMP.class, p -> p.getDistanceToEntity(player) <= 10 && p.getUniqueID().version() != 2 && p != player && Utils.isInTablist(p));
                if (Skytils.config.dolphinCap) {
                    players = players.stream().limit(5).collect(Collectors.toList());
                }
                ScreenRenderer.fontRenderer.drawString(String.valueOf(players.size()), x + 20, y + 5, CommonColors.ORANGE, SmartFontRenderer.TextAlignment.LEFT_RIGHT, SmartFontRenderer.TextShadow.NORMAL);
                GlStateManager.scale(1/this.getScale(), 1/this.getScale(), 1.0F);
            }
        }

        @Override
        public void demoRender() {
            float x = getActualX();
            float y = getActualY();
            RenderUtil.renderTexture(ICON, (int)x, (int)y);
            ScreenRenderer.fontRenderer.drawString("5", x + 20, y + 5, CommonColors.ORANGE, SmartFontRenderer.TextAlignment.LEFT_RIGHT, SmartFontRenderer.TextShadow.NORMAL);
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public int getWidth() {
            return 20 + ScreenRenderer.fontRenderer.getStringWidth("5");
        }

        @Override
        public boolean getToggled() {
            return Skytils.config.dolphinPetDisplay;
        }
    }

}
