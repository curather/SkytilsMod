package skytils.skytilsmod.features.impl.mining;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import skytils.skytilsmod.Skytils;
import skytils.skytilsmod.core.DataFetcher;
import skytils.skytilsmod.core.GuiManager;
import skytils.skytilsmod.events.BossBarEvent;
import skytils.skytilsmod.utils.*;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiningFeatures {

    public static LinkedHashMap<String, String> fetchurItems = new LinkedHashMap<>();

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Pattern EVENT_PATTERN = Pattern.compile("(?:PASSIVE )?EVENT (?<event>.+) (?:(?:ACTIVE IN (?<location>.+))|(?:RUNNING)) FOR (?<min>\\d+):(?<sec>\\d+)");

    private static BlockPos lastJukebox = null;
    private static BlockPos puzzlerSolution = null;
    private static BlockPos raffleBox = null;

    private static boolean inRaffle = false;

    @SubscribeEvent
    public void onBossBar(BossBarEvent.Set event) {
        if (!Utils.inSkyblock) return;
        String unformatted = event.displayData.getDisplayName().getUnformattedText();
        if (Skytils.config.raffleWarning) {
            if (unformatted.startsWith("EVENT RAFFLE ACTIVE IN")) {
                Matcher matcher = EVENT_PATTERN.matcher(unformatted);
                if (matcher.find()) {
                    int seconds = Integer.parseInt(matcher.group("min")) * 60 + Integer.parseInt(matcher.group("sec"));
                    if (seconds <= 15) {
                        GuiManager.createTitle("\u00a7cRaffle ending in \u00a7a" + seconds + "s", 20);
                    }
                    if (seconds > 1) {
                        inRaffle = true;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (!Utils.inSkyblock || event.type == 2) return;

        String unformatted = StringUtils.stripControlCodes(event.message.getUnformattedText());

        if (Skytils.config.powerGhastPing) {
            if (unformatted.startsWith("Find the Powder Ghast near the")) {
                GuiManager.createTitle("\u00a7cPOWER GHAST", 20);
            }
        }

        if (Skytils.config.raffleWaypoint && inRaffle) {
            if ((unformatted.startsWith("You registered") && unformatted.contains("in the raffle event!")) || unformatted.startsWith("No tickets to put in the box...")) {
                raffleBox = lastJukebox;
            }
            if (unformatted.trim().startsWith("RAFFLE ENDED!")) {
                inRaffle = false;
            }
        }


        if (Skytils.config.puzzlerSolver && unformatted.contains("[NPC]") && unformatted.contains("Puzzler")) {
            if (unformatted.contains("Nice")) {
                puzzlerSolution = null;
                return;
            }
            if (unformatted.contains("Wrong") || unformatted.contains("Come") || (!unformatted.contains("▶") && !unformatted.contains("▲") && !unformatted.contains("◀") && !unformatted.contains("▼"))) return;
            if (ScoreboardUtil.getSidebarLines().stream().anyMatch(line -> ScoreboardUtil.cleanSB(line).contains("Dwarven Mines"))) {
                puzzlerSolution = new BlockPos(181, 195, 135);
                String msg = unformatted.substring(15).trim();
                Matcher matcher = Pattern.compile("([▶▲◀▼]+)").matcher(unformatted);
                if (matcher.find()) {
                    String sequence = matcher.group(1).trim();
                    if (sequence.length() != msg.length()) {
                        System.out.println(String.format("%s - %s | %s - %s", sequence, msg, sequence.length(), unformatted.length()));
                    }
                    for (char c : sequence.toCharArray()) {
                        switch (String.valueOf(c)) {
                            case "▲":
                                puzzlerSolution = puzzlerSolution.south();
                                break;
                            case "▶":
                                puzzlerSolution = puzzlerSolution.west();
                                break;
                            case "◀":
                                puzzlerSolution = puzzlerSolution.east();
                                break;
                            case "▼":
                                puzzlerSolution = puzzlerSolution.north();
                                break;
                            default:
                                System.out.println("Invalid Puzzler character: " + c);
                        }
                    }
                    System.out.println("Puzzler Solution: " + puzzlerSolution);
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Mine the block highlighted in " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "RED" + EnumChatFormatting.GREEN + "!"));
                }
            }
        }

        if (Skytils.config.fetchurSolver && unformatted.contains("[NPC]") && unformatted.contains("Fetchur")) {
            if (fetchurItems.size() == 0) {
                mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7cSkytils did not load any solutions."));
                DataFetcher.reloadData();
                return;
            }
            String solution = fetchurItems.keySet().stream().filter(unformatted::contains).findFirst().map(fetchurItems::get).orElse(null);
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (solution != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Fetchur needs: " + EnumChatFormatting.DARK_GREEN + EnumChatFormatting.BOLD + solution + EnumChatFormatting.GREEN + "!"));
                } else {
                    if (unformatted.contains("its") || unformatted.contains("theyre")) {
                        System.out.println("Missing Fetchur item: " + unformatted);
                        mc.thePlayer.addChatMessage(new ChatComponentText(String.format("\u00a7cSkytils couldn't determine the Fetchur item. There were %s solutions loaded.", fetchurItems.size())));
                    }
                }

            }).start();
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!Utils.inSkyblock) return;
        if (event.entity != mc.thePlayer) return;
        ItemStack item = event.entityPlayer.getHeldItem();

        String itemId = ItemUtil.getSkyBlockItemID(item);

        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            if (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().startsWith("dynamic")) {
                if (Skytils.config.onlyPickaxeAbilitiesInMines && itemId != null && (itemId.contains("PICKAXE") || itemId.contains("DRILL"))) {
                    event.setCanceled(event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.pos == null || mc.theWorld.getBlockState(event.pos).getBlock() != Blocks.chest);
                }
            }
            if (Skytils.config.raffleWaypoint && inRaffle && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
                IBlockState block = event.world.getBlockState(event.pos);
                if (block.getBlock() == Blocks.jukebox) {
                    lastJukebox = event.pos;
                }
            }
        }

    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!Utils.inSkyblock) return;
        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * event.partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * event.partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * event.partialTicks;
        if (Skytils.config.puzzlerSolver && puzzlerSolution != null) {
            double x = puzzlerSolution.getX() - viewerX;
            double y = puzzlerSolution.getY() - viewerY;
            double z = puzzlerSolution.getZ() - viewerZ;
            GlStateManager.enableCull();
            RenderUtil.drawFilledBoundingBox(new AxisAlignedBB(x, y, z, x + 1, y + 1.01, z + 1), new Color(255, 0, 0, 200), 1f);
            GlStateManager.disableCull();
        }
        if (Skytils.config.raffleWaypoint && inRaffle && raffleBox != null) {
            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            RenderUtil.renderWaypointText("Raffle Box", raffleBox, event.partialTicks);
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
        }
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event) {
        if (!Utils.inSkyblock) return;
        if (ScoreboardUtil.getSidebarLines().stream().anyMatch(l -> ScoreboardUtil.cleanSB(l).contains("The Mist"))) {
            if (Skytils.config.showGhosts && event.entity.isInvisible() && event.entity instanceof EntityCreeper) {
                event.entity.setInvisible(false);
            }
        }
    }

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post event) {
        if (!Utils.inSkyblock) return;
        if (ScoreboardUtil.getSidebarLines().stream().anyMatch(l -> ScoreboardUtil.cleanSB(l).contains("The Mist"))) {
            if (Skytils.config.showGhostHealth && event.entity instanceof EntityCreeper) {
                String healthText = String.format("\u00a7cGhost \u00a7a%s\u00a7f/\u00a7a1M\u00a7c ❤", NumberUtil.format((long) event.entity.getHealth()));
                RenderUtil.draw3DString(new Vec3(event.entity.getPosition().add(0, event.entity.getEyeHeight() + 0.5, 0)), healthText, new Color(255, 255, 255), 1f);
            }
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        puzzlerSolution = null;
        lastJukebox = null;
        raffleBox = null;
        inRaffle = false;
    }

}
