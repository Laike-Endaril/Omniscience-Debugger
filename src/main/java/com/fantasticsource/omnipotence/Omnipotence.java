package com.fantasticsource.omnipotence;

import com.fantasticsource.mctools.ServerTickTimer;
import com.fantasticsource.omnipotence.client.ClientCommands;
import com.fantasticsource.omnipotence.client.PathVisualizer;
import com.fantasticsource.omnipotence.client.ScreenDebug;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Omnipotence.MODID, name = Omnipotence.NAME, version = Omnipotence.VERSION, dependencies = "required-after:fantasticlib@[1.12.2.021e,)", acceptableRemoteVersions = "*")
public class Omnipotence
{
    public static final String MODID = "omnipotence";
    public static final String NAME = "Omnipotence";
    public static final String VERSION = "1.12.2.000i";

    static
    {
        MinecraftForge.EVENT_BUS.register(Omnipotence.class);
        MinecraftForge.EVENT_BUS.register(ServerTickTimer.class);
        MinecraftForge.EVENT_BUS.register(PathVisualizer.class);
        Debug.init();
        Network.init();

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            ClientCommandHandler.instance.registerCommand(new ClientCommands());
            MinecraftForge.EVENT_BUS.register(ScreenDebug.class);
        }
    }

    @SubscribeEvent
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID)) ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }

    @Mod.EventHandler
    public static void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new Commands());

        System.out.println(Debug.memData());
    }

    @Mod.EventHandler
    public static void serverStarted(FMLServerStartedEvent event)
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server.isDedicatedServer())
        {
            if (LagDetector.init(server)) System.out.println("Starting LagDetector");
            else System.out.println("NOT starting LagDetector; the watchdog timeout setting is 0");
        }
        else System.out.println("NOT starting LagDetector; we are not running in dedicated server mode");
    }
}
