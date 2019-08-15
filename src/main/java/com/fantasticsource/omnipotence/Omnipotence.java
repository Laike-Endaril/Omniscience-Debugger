package com.fantasticsource.omnipotence;

import com.fantasticsource.mctools.ServerTickTimer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = Omnipotence.MODID, name = Omnipotence.NAME, version = Omnipotence.VERSION, dependencies = "required-after:fantasticlib@[1.12.2.021a,)", acceptableRemoteVersions = "*")
public class Omnipotence
{
    public static final String MODID = "omnipotence";
    public static final String NAME = "Omnipotence";
    public static final String VERSION = "1.12.2.000e";

    static
    {
        MinecraftForge.EVENT_BUS.register(Omnipotence.class);
        MinecraftForge.EVENT_BUS.register(ServerTickTimer.class);
        Debug.init();
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

//        java.lang.ArrayIndexOutOfBoundsException: -1
//        at java.util.ArrayList.elementData(ArrayList.java:422)
//        at java.util.ArrayList.get(ArrayList.java:435)
//        at net.minecraft.profiler.Profiler.endSection(Profiler.java:76)
//        at net.minecraft.profiler.Profiler.endStartSection(Profiler.java:149)
//        at net.minecraft.server.MinecraftServer.updateTimeLightAndEntities(MinecraftServer.java:787)
//        at net.minecraft.server.dedicated.DedicatedServer.updateTimeLightAndEntities(DedicatedServer.java:397)
//        at net.minecraft.server.MinecraftServer.tick(MinecraftServer.java:668)
//        at net.minecraft.server.MinecraftServer.run(MinecraftServer.java:526)
//        at java.lang.Thread.run(Thread.java:748)

        //TODO Need to fix this before re-enabling it!  See stacktrace above!  Might be due to cross-thread dysync in the profiler calls
//        if (ServerHangWatchdogDebugger.init(event.getServer())) System.out.println("Starting ServerHangWatchdogDebugger");
//        else System.out.println("NOT starting ServerHangWatchdogDebugger; either we are not running in dedicated server mode, or the watchdog timeout setting is 0");
    }
}
