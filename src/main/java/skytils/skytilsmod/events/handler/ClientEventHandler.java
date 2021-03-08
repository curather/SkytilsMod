package skytils.skytilsmod.events.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import skytils.skytilsmod.gui.SignSelectionList;

public class ClientEventHandler
{
    private final Minecraft mc;
    public static int ticks;
    public static int rainbowTicks;
    public static int ticksPaused;
    public static float renderPartialTicks;

    public ClientEventHandler()
    {
        this.mc = Minecraft.getMinecraft();
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        SignSelectionList.clearAll();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event)
    {
        if (this.mc.currentScreen instanceof GuiMainMenu)
        {
            ClientEventHandler.ticks = ClientEventHandler.rainbowTicks = ClientEventHandler.ticksPaused = 0;
        }
        if (event.phase == Phase.START)
        {
            ClientEventHandler.rainbowTicks += 5;
            ClientEventHandler.ticks++;
            ClientEventHandler.renderPartialTicks = ClientEventHandler.ticks + this.mc.timer.renderPartialTicks;

            if (!this.mc.isGamePaused())
            {
                ClientEventHandler.ticksPaused++;
            }
        }
    }

}