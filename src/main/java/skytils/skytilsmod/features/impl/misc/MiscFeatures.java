package skytils.skytilsmod.features.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import skytils.skytilsmod.Skytils;
import skytils.skytilsmod.core.structure.FloatPair;
import skytils.skytilsmod.core.structure.GuiElement;
import skytils.skytilsmod.events.BossBarEvent;
import skytils.skytilsmod.events.CheckRenderEntityEvent;
import skytils.skytilsmod.events.GuiContainerEvent;
import skytils.skytilsmod.events.ReceivePacketEvent;
import skytils.skytilsmod.utils.ItemUtil;
import skytils.skytilsmod.utils.NumberUtil;
import skytils.skytilsmod.utils.RenderUtil;
import skytils.skytilsmod.utils.Utils;
import skytils.skytilsmod.utils.graphics.ScreenRenderer;
import skytils.skytilsmod.utils.graphics.SmartFontRenderer;
import skytils.skytilsmod.utils.graphics.colors.CommonColors;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MiscFeatures {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static long golemSpawnTime = 0;

    @SubscribeEvent
    public void onBossBarSet(BossBarEvent.Set event) {
        IBossDisplayData displayData = event.displayData;

        if(Utils.inSkyblock) {
            if(Skytils.config.bossBarFix && StringUtils.stripControlCodes(displayData.getDisplayName().getUnformattedText()).equals("Wither")) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (!Utils.inSkyblock) return;
        String unformatted = StringUtils.stripControlCodes(event.message.getUnformattedText()).trim();

        if (unformatted.equals("The ground begins to shake as an Endstone Protector rises from below!")) {
            golemSpawnTime = System.currentTimeMillis() + 20_000;
        }
    }

    @SubscribeEvent
    public void onCheckRender(CheckRenderEntityEvent event) {
        if (!Utils.inSkyblock) return;
        if (event.entity instanceof EntityFallingBlock) {
            EntityFallingBlock entity = (EntityFallingBlock) event.entity;
            if (Skytils.config.hideMidasStaffGoldBlocks && entity.getBlock().getBlock() == Blocks.gold_block) {
                event.setCanceled(true);
            }
        }

        if (event.entity instanceof EntityItem) {
            EntityItem entity = (EntityItem) event.entity;
            if (Skytils.config.hideJerryRune) {
                ItemStack item = entity.getEntityItem();
                if(item.getItem() == Items.spawn_egg && Objects.equals(ItemMonsterPlacer.getEntityName(item), "Villager") && item.getDisplayName().equals("Spawn Villager") && entity.lifespan == 6000) {
                    event.setCanceled(true);
                }
            }
        }

        if (event.entity instanceof EntityLightningBolt) {
            if (Skytils.config.hideLightning) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (!Utils.inSkyblock) return;
        if (event.type == RenderGameOverlayEvent.ElementType.AIR && Skytils.config.hideAirDisplay && !Utils.inDungeons) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!Utils.inSkyblock) return;
        if (event.packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect packet = (S29PacketSoundEffect) event.packet;
            if (Skytils.config.disableCooldownSounds && packet.getSoundName().equals("mob.endermen.portal") && packet.getPitch() == 0 && packet.getVolume() == 8) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onSlotClick(GuiContainerEvent.SlotClickEvent event) {
        if (!Utils.inSkyblock) return;

        if (event.container instanceof ContainerChest) {
            ContainerChest chest = (ContainerChest) event.container;

            IInventory inventory = chest.getLowerChestInventory();
            Slot slot = event.slot;
            if (slot == null) return;
            ItemStack item = slot.getStack();
            String inventoryName = inventory.getDisplayName().getUnformattedText();
            if (item == null) return;
            NBTTagCompound extraAttributes = ItemUtil.getExtraAttributes(item);

            if (inventoryName.equals("Ophelia")) {
                if (Skytils.config.dungeonPotLock > 0) {
                    if (slot.inventory == mc.thePlayer.inventory || slot.slotNumber == 49) return;
                    if (item.getItem() != Items.potionitem || extraAttributes == null || !extraAttributes.hasKey("potion_level")) {
                        event.setCanceled(true);
                        return;
                    }
                    if (extraAttributes.getInteger("potion_level") != Skytils.config.dungeonPotLock) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    static {
        new GolemSpawnTimerElement();
        new LegionPlayerDisplay();
    }

    public static class GolemSpawnTimerElement extends GuiElement {

        public GolemSpawnTimerElement() {
            super("Endstone Protector Spawn Timer", new FloatPair(150, 20));
            Skytils.GUIMANAGER.registerElement(this);
        }

        @Override
        public void render() {
            EntityPlayerSP player = mc.thePlayer;
            if (this.getToggled() && Utils.inSkyblock && player != null && ((golemSpawnTime - System.currentTimeMillis()) > 0)) {
                ScaledResolution sr = new ScaledResolution(mc);

                boolean leftAlign = getActualX() < sr.getScaledWidth() / 2f;

                GlStateManager.scale(this.getScale(), this.getScale(), 1.0);
                String text = "\u00a7cGolem spawn in: \u00a7a" + NumberUtil.round((golemSpawnTime - System.currentTimeMillis()) / 1000d, 1) + "s";
                SmartFontRenderer.TextAlignment alignment = leftAlign ? SmartFontRenderer.TextAlignment.LEFT_RIGHT : SmartFontRenderer.TextAlignment.RIGHT_LEFT;
                ScreenRenderer.fontRenderer.drawString(text, leftAlign ? this.getActualX() : this.getActualX() + getWidth(), this.getActualY(), CommonColors.WHITE, alignment, SmartFontRenderer.TextShadow.NORMAL);
                GlStateManager.scale(1/this.getScale(), 1/this.getScale(), 1.0F);
            }
        }

        @Override
        public void demoRender() {
            ScreenRenderer.fontRenderer.drawString("\u00a7cGolem spawn in: \u00a7a20.0s", this.getActualX(), this.getActualY(), CommonColors.WHITE, SmartFontRenderer.TextAlignment.LEFT_RIGHT, SmartFontRenderer.TextShadow.NORMAL);
        }

        @Override
        public int getHeight() {
            return ScreenRenderer.fontRenderer.FONT_HEIGHT;
        }

        @Override
        public int getWidth() {
            return ScreenRenderer.fontRenderer.getStringWidth("\u00a7cGolem spawn in: \u00a7a20.0s");
        }

        @Override
        public boolean getToggled() {
            return Skytils.config.golemSpawnTimer;
        }
    }

    public static class LegionPlayerDisplay extends GuiElement {

        public LegionPlayerDisplay() {
            super("Legion Player Display", new FloatPair(50, 50));
            Skytils.GUIMANAGER.registerElement(this);
        }

        @Override
        public void render() {
            EntityPlayerSP player = mc.thePlayer;
            if (this.getToggled() && Utils.inSkyblock && player != null && mc.theWorld != null) {
                float x = getActualX();
                float y = getActualY();

                boolean hasLegion = false;
                for (ItemStack armor : player.inventory.armorInventory) {
                    NBTTagCompound extraAttr = ItemUtil.getExtraAttributes(armor);
                    if (extraAttr != null && extraAttr.hasKey("enchantments") && extraAttr.getCompoundTag("enchantments").hasKey("ultimate_legion")) {
                        hasLegion = true;
                        break;
                    }
                }

                if (!hasLegion) return;

                GlStateManager.scale(this.getScale(), this.getScale(), 1.0);
                RenderUtil.renderItem(new ItemStack(Items.enchanted_book), (int)x, (int)y);
                List<EntityPlayer> players = mc.theWorld.getPlayers(EntityOtherPlayerMP.class, p -> p.getDistanceToEntity(player) <= 30 && p.getUniqueID().version() != 2 && p != player && Utils.isInTablist(p));
                if (Skytils.config.legionCap) {
                    players = players.stream().limit(20).collect(Collectors.toList());
                }
                ScreenRenderer.fontRenderer.drawString(String.valueOf(players.size()), x + 20, y + 5, CommonColors.ORANGE, SmartFontRenderer.TextAlignment.LEFT_RIGHT, SmartFontRenderer.TextShadow.NORMAL);
                GlStateManager.scale(1/this.getScale(), 1/this.getScale(), 1.0F);
            }
        }

        @Override
        public void demoRender() {
            float x = getActualX();
            float y = getActualY();
            RenderUtil.renderItem(new ItemStack(Items.enchanted_book), (int)x, (int)y);
            ScreenRenderer.fontRenderer.drawString("30", x + 20, y + 5, CommonColors.ORANGE, SmartFontRenderer.TextAlignment.LEFT_RIGHT, SmartFontRenderer.TextShadow.NORMAL);
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public int getWidth() {
            return 20 + ScreenRenderer.fontRenderer.getStringWidth("30");
        }

        @Override
        public boolean getToggled() {
            return Skytils.config.legionPlayerDisplay;
        }
    }
}
