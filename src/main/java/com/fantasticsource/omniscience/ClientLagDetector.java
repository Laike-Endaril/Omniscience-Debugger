package com.fantasticsource.omniscience;

import com.fantasticsource.tools.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.fantasticsource.omniscience.Omniscience.NAME;

@SideOnly(Side.CLIENT)
public class ClientLagDetector implements Runnable
{
    private final long[] checkTimes = new long[]{1000, 5000, 10000};
    private static long currentTickStartTime = 0;
    private static boolean startedCodePointPrinter = false;

    private boolean go = true;

    private ClientLagDetector()
    {
    }

    public static void init(FMLPostInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(ClientLagDetector.class);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            boolean start = currentTickStartTime == 0;

            currentTickStartTime = System.currentTimeMillis();

            if (start)
            {
                if (startedCodePointPrinter) CodePointPrinter.stop();

                System.out.println(TextFormatting.YELLOW + "Starting ClientLagDetector");
                Thread thread1 = new Thread(new ClientLagDetector());
                thread1.setName(NAME + "-ClientLagDetector");
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
            long j = System.currentTimeMillis();
            if (Minecraft.getMinecraft().isGamePaused() || (FMLCommonHandler.instance().getMinecraftServerInstance() != null && FMLCommonHandler.instance().getMinecraftServerInstance().currentTask != null))
            {
                currentTickStartTime = j;
                i = currentTickStartTime;
            }
            long tickTime = j - i;

            if (tickTime >= checkTimes[2])
            {
                System.out.println(TextFormatting.RED + "One tick has taken " + ((double) tickTime / 1000) + "seconds so far!");
                System.out.println(Debug.memData());

                for (Thread thread : Thread.getAllStackTraces().keySet())
                {
                    if (thread.getName().contains("Client thread"))
                    {
                        System.out.println();
                        System.out.println("Client thread stacktrace:");
                        System.out.println("===================================================================================================================================");
                        Tools.printStackTrace(thread);
                        System.out.println("===================================================================================================================================");
                        break;
                    }
                }

                go = false;
                currentTickStartTime = 0;

                if (CodePointPrinter.enabled)
                {
                    startedCodePointPrinter = true;
                    CodePointPrinter.start();
                }
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
