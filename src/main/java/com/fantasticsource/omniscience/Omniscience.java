package com.fantasticsource.omniscience;

import com.fantasticsource.mctools.ServerTickTimer;
import com.fantasticsource.omniscience.client.ClientCommands;
import com.fantasticsource.omniscience.client.PathVisualizer;
import com.fantasticsource.omniscience.client.ScreenDebug;
import com.fantasticsource.omniscience.hack.OmniEventBus;
import com.fantasticsource.omniscience.hack.OmniProfiler;
import com.fantasticsource.tools.ReflectionTool;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Omniscience.MODID, name = Omniscience.NAME, version = Omniscience.VERSION, dependencies = "required-after:fantasticlib@[1.12.2.044o,)", acceptableRemoteVersions = "*")
public class Omniscience
{
    public static final String MODID = "omnisciencedebugger";
    public static final String NAME = "Omniscience Debugger";
    public static final String VERSION = "1.12.2.002c";

    static
    {
        ReflectionTool.set(MinecraftForge.class, "EVENT_BUS", null, new OmniEventBus(MinecraftForge.EVENT_BUS));


        MinecraftForge.EVENT_BUS.register(Omniscience.class);
        MinecraftForge.EVENT_BUS.register(ServerTickTimer.class);
        MinecraftForge.EVENT_BUS.register(OmniProfiler.class);
        MinecraftForge.EVENT_BUS.register(PathVisualizer.class);
        Debug.init();
        Network.init();

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            ClientCommandHandler.instance.registerCommand(new ClientCommands());
            MinecraftForge.EVENT_BUS.register(ScreenDebug.class);
            PathVisualizer.renderManagerRenderOutlinesField = ReflectionTool.getField(RenderManager.class, "field_178639_r", "renderOutlines");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID)) ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }

    @Mod.EventHandler
    public static void serverAboutToStart(FMLServerAboutToStartEvent event)
    {
        Debug.serverInit();
        ReflectionTool.set(MinecraftServer.class, new String[]{"field_71304_b", "profiler"}, event.getServer(), OmniProfiler.INSTANCE);
        for (WorldServer world : event.getServer().worlds) ReflectionTool.set(World.class, new String[]{"field_72984_F", "profiler"}, world, OmniProfiler.INSTANCE);
    }

    @Mod.EventHandler
    public static void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new Commands());
        event.registerServerCommand(new CommandDebug());

        System.out.println(Debug.memData());
    }

    @Mod.EventHandler
    public static void serverStarted(FMLServerStartedEvent event)
    {
        GCMessager.init(event);
    }

    @Mod.EventHandler
    public static void serverStopped(FMLServerStoppedEvent event)
    {
        Debug.serverThreadID = -1;
    }

    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event)
    {
        ServerLagDetector.init(event);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            ClientLagDetector.init(event);
        }
    }
//
//    @SubscribeEvent
//    public static void test(LivingEvent.LivingJumpEvent event) throws InterruptedException
//    {
//        if (event.getEntityLiving() instanceof EntityPlayerMP)
//        {
//            System.out.println("test");
//            Thread.sleep(2000);
//        }
//    }
}
