package com.fantasticsource.omniscience;

import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class GCMessager
{
    protected static boolean initialized = false;
    protected static int prevGCRuns;
    protected static long prevGCTime;

    public static void init(FMLServerStartedEvent event)
    {
        System.out.println(TextFormatting.YELLOW + "Starting GCMessager");
        if (!initialized)
        {
            MinecraftForge.EVENT_BUS.register(GCMessager.class);
            initialized = true;
        }
        prevGCRuns = Debug.gcRuns();
        prevGCTime = Debug.gcTime();
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        int gcRuns = Debug.gcRuns();
        if (gcRuns > prevGCRuns)
        {
            long gcTime = Debug.gcTime();
            long gcTimeThisTick = gcTime - prevGCTime;
            if (gcTimeThisTick > 50) //Only show message if GC is taking more than a full tick worth of time to complete
            {
                System.out.println(TextFormatting.YELLOW + "Garbage collector(s) just froze the server for ~" + gcTimeThisTick + "ms / ~" + ((double) (gcTimeThisTick / 5) / 10) + " ticks (ran " + (gcRuns - prevGCRuns) + " time(s))");
                System.out.println(TextFormatting.YELLOW + "After GC... " + Debug.memData());
            }
            prevGCRuns = gcRuns;
            prevGCTime = gcTime;
        }
    }
}
