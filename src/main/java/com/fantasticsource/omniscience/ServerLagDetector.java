package com.fantasticsource.omniscience;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.tools.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static com.fantasticsource.omniscience.Omniscience.NAME;

public class ServerLagDetector implements Runnable
{
    private final long[] checkTimes = new long[]{1000, 5000, 10000};
    private static long currentTickStartTime = 0;
    private static boolean startedCodePointPrinter = false;

    private boolean go = true;

    private ServerLagDetector()
    {
    }

    public static void init(FMLPostInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(ServerLagDetector.class);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            boolean start = currentTickStartTime == 0;

            currentTickStartTime = System.currentTimeMillis();

            if (start)
            {
                if (startedCodePointPrinter) CodePointPrinter.stop();
                System.out.println(TextFormatting.YELLOW + "Starting ServerLagDetector");
                Thread thread1 = new Thread(new ServerLagDetector());
                thread1.setName(NAME + "-ServerLagDetector");
                thread1.setDaemon(true);
                thread1.start();
            }
        }
    }

    public void run()
    {
        while (go)
        {
            long i = currentTickStartTime;
            long j = MinecraftServer.getCurrentTimeMillis();
            if (MCTools.isClient() && Minecraft.getMinecraft().isGamePaused()) currentTickStartTime = j;
            long tickTime = j - i;

            if (tickTime >= checkTimes[2])
            {
                System.out.println(TextFormatting.RED + "One tick has taken " + ((double) tickTime / 1000) + "seconds so far!");
                System.out.println(Debug.memData());

                for (Thread thread : Thread.getAllStackTraces().keySet())
                {
                    if (thread.getName().contains("Server thread"))
                    {
                        System.out.println();
                        System.out.println("Server thread stacktrace:");
                        System.out.println("===================================================================================================================================");
                        Tools.printStackTrace(thread);
                        System.out.println("===================================================================================================================================");
                        break;
                    }
                }

                go = false;
                currentTickStartTime = 0;

                startedCodePointPrinter = true;
                CodePointPrinter.start();
            }
            else if (tickTime >= checkTimes[1])
            {
                System.out.println(TextFormatting.RED + "One tick has taken " + ((double) tickTime / 1000) + "seconds so far!");
                System.out.println(Debug.memData());

                try
                {
                    Thread.sleep(i + checkTimes[2] - j);
                }
                catch (InterruptedException e)
                {
                }
            }
            else if (tickTime >= checkTimes[0])
            {
                System.out.println(TextFormatting.RED + "One tick has taken " + ((double) tickTime / 1000) + "seconds so far!");
                System.out.println(Debug.memData());


                try
                {
                    Thread.sleep(i + checkTimes[1] - j);
                }
                catch (InterruptedException e)
                {
                }
            }
            else
            {
                try
                {
                    Thread.sleep(i + checkTimes[0] - j);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }
}
