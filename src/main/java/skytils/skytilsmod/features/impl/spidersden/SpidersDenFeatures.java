package skytils.skytilsmod.features.impl.spidersden;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import skytils.skytilsmod.Skytils;
import skytils.skytilsmod.core.structure.FloatPair;
import skytils.skytilsmod.core.structure.GuiElement;
import skytils.skytilsmod.utils.RenderUtil;
import skytils.skytilsmod.utils.SBInfo;
import skytils.skytilsmod.utils.Utils;
import skytils.skytilsmod.utils.graphics.ScreenRenderer;
import skytils.skytilsmod.utils.graphics.SmartFontRenderer;
import skytils.skytilsmod.utils.graphics.colors.CommonColors;

import java.util.List;

public class SpidersDenFeatures {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static boolean shouldShowArachneSpawn = false;

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Utils.inSkyblock) return;
        String unformatted = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (unformatted.startsWith("☄") && (unformatted.contains("placed an Arachne Fragment! (") || unformatted.contains("placed an Arachne Crystal! Something is awakening!"))) {
            shouldShowArachneSpawn = true;
        }
        if (unformatted.trim().startsWith("ARACHNE DOWN!")) {
            shouldShowArachneSpawn = false;
        }
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (shouldShowArachneSpawn && Skytils.config.showArachneSpawn) {
            BlockPos spawnPos = new BlockPos(-282, 49, -178);

            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            RenderUtil.renderWaypointText("Arachne Spawn", spawnPos, event.partialTicks);
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        shouldShowArachneSpawn = false;
    }


    static {
        new ArachneHPElement();
    }

    public static class ArachneHPElement extends GuiElement {

        public ArachneHPElement() {
            super("Show Arachne HP", new FloatPair(200, 30));
            Skytils.GUIMANAGER.registerElement(this);
        }

        @Override
        public void render() {
            EntityPlayerSP player = mc.thePlayer;
            World world = mc.theWorld;
            if (this.getToggled() && Utils.inSkyblock && player != null && world != null) {
                if (SBInfo.getInstance().getLocation() == null || !SBInfo.getInstance().getLocation().equalsIgnoreCase("combat_1")) return;

                List<EntityArmorStand> arachneNames = world.getEntities(EntityArmorStand.class, (entity) -> {
                    String name = entity.getDisplayName().getFormattedText();
                    if (name.contains("❤")) {
                        if (name.contains("§cArachne §")) {
                            return true;
                        }
                    }
                    return false;
                });

                ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());

                boolean leftAlign = getActualX() < sr.getScaledWidth() / 2f;

                GlStateManager.scale(this.getScale(), this.getScale(), 1.0);
                for (int i = 0; i < arachneNames.size(); i++) {
                    SmartFontRenderer.TextAlignment alignment = leftAlign ? SmartFontRenderer.TextAlignment.LEFT_RIGHT : SmartFontRenderer.TextAlignment.RIGHT_LEFT;
                    ScreenRenderer.fontRenderer.drawString(arachneNames.get(i).getDisplayName().getFormattedText(), leftAlign ? this.getActualX() : this.getActualX() + getWidth(), this.getActualY() + i * ScreenRenderer.fontRenderer.FONT_HEIGHT, CommonColors.WHITE, alignment, SmartFontRenderer.TextShadow.NORMAL);
                }
                GlStateManager.scale(1/this.getScale(), 1/this.getScale(), 1.0F);
            }
        }

        @Override
        public void demoRender() {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());

            boolean leftAlign = getActualX() < sr.getScaledWidth() / 2f;

            String text = "§8[§7Lv500§8] §cArachne §a17.6M§f/§a20M§c❤§r";
            SmartFontRenderer.TextAlignment alignment = leftAlign ? SmartFontRenderer.TextAlignment.LEFT_RIGHT : SmartFontRenderer.TextAlignment.RIGHT_LEFT;
            ScreenRenderer.fontRenderer.drawString(text, leftAlign ? this.getActualX() : this.getActualX() + getWidth(), this.getActualY(), CommonColors.WHITE, alignment, SmartFontRenderer.TextShadow.NORMAL);
        }

        @Override
        public int getHeight() {
            return ScreenRenderer.fontRenderer.FONT_HEIGHT;
        }

        @Override
        public int getWidth() {
            return ScreenRenderer.fontRenderer.getStringWidth("§8[§7Lv500§8] §cArachne §a17.6M§f/§a20M§c❤§r");
        }

        @Override
        public boolean getToggled() {
            return Skytils.config.showArachneHP;
        }
    }

}
