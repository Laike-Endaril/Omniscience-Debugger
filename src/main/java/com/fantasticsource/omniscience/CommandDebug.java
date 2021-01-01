package com.fantasticsource.omniscience;

import com.fantasticsource.omniscience.hack.OmniProfiler;
import com.fantasticsource.tools.Tools;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CommandDebug extends CommandBase
{
    private static final Logger LOGGER = LogManager.getLogger();
    protected static long profileStartTime;
    protected static int profileStartTick;
    protected static int profileStartGCRuns;
    protected static long profileStartGCTime;
    public static long totalHeapUsage;

    public String getName()
    {
        return "debug";
    }

    public int getRequiredPermissionLevel()
    {
        return 3;
    }

    public String getUsage(ICommandSender sender)
    {
        return "commands.debug.usage";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            throw new WrongUsageException("commands.debug.usage");
        }
        else
        {
            if ("start".equals(args[0]))
            {
                if (args.length != 1)
                {
                    throw new WrongUsageException("commands.debug.usage");
                }

                notifyCommandListener(sender, this, "commands.debug.start");
                server.enableProfiling();
                profileStartTime = MinecraftServer.getCurrentTimeMillis();
                profileStartTick = server.getTickCounter();
                profileStartGCRuns = GCMessager.prevGCRuns;
                profileStartGCTime = GCMessager.prevGCTime;
                totalHeapUsage = 0;
            }
            else
            {
                if (!"stop".equals(args[0]))
                {
                    throw new WrongUsageException("commands.debug.usage");
                }

                if (args.length != 1)
                {
                    throw new WrongUsageException("commands.debug.usage");
                }

                if (!server.profiler.profilingEnabled)
                {
                    throw new CommandException("commands.debug.notStarted");
                }

                long timeSpan = MinecraftServer.getCurrentTimeMillis() - profileStartTime;
                int tickSpan = server.getTickCounter() - profileStartTick;
                String profilerResults = getProfilerResults(timeSpan, tickSpan, server);
                server.profiler.profilingEnabled = false;

                saveProfilerResults(server, profilerResults);

                notifyCommandListener(sender, this, "commands.debug.stop", String.format("%.2f", (float) timeSpan / 1000.0F), tickSpan);

                if (sender instanceof EntityPlayerMP) sendProfilerResults((EntityPlayerMP) sender, profilerResults);
            }
        }
    }

    private void saveProfilerResults(MinecraftServer server, String profilerResults)
    {
        File file1 = new File(server.getFile("debug"), "profile-results-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt");
        file1.getParentFile().mkdirs();
        Writer writer = null;

        try
        {
            writer = new OutputStreamWriter(new FileOutputStream(file1), StandardCharsets.UTF_8);
            writer.write(profilerResults);
        }
        catch (Throwable throwable)
        {
            LOGGER.error("Could not save profiler results to {}", file1, throwable);
        }
        finally
        {
            IOUtils.closeQuietly(writer);
        }
    }

    private void sendProfilerResults(EntityPlayerMP player, String profilerResults)
    {
        for (String s : Tools.fixedSplit(profilerResults, "\n"))
        {
            player.sendMessage(new TextComponentString(s));
        }
    }

    private String getProfilerResults(long timeSpan, int tickSpan, MinecraftServer server)
    {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append("---- " + Omniscience.NAME + " Profiler Results ----\n");
        stringbuilder.append("// ");
        stringbuilder.append(getWittyComment());
        stringbuilder.append("\n\n");
        stringbuilder.append("Time span: ").append(timeSpan).append(" ms\n");
        stringbuilder.append("Tick span: ").append(tickSpan).append(" ticks\n");
        stringbuilder.append("// This is approximately ").append(String.format("%.2f", Tools.min((float) (tickSpan + 1) / ((float) timeSpan / 1000), 20))).append(" ticks per second. It should be 20 ticks per second\n");
        stringbuilder.append("// Garbage collectors ran ").append(GCMessager.prevGCRuns - profileStartGCRuns).append(" time(s) during profiling\n");
        stringbuilder.append("// Approximate total heap allocations during profiling - ").append(totalHeapUsage).append("\n\n");
        stringbuilder.append("--- BEGIN PROFILE DUMP ---\n\n");
        appendProfilerResults(0, "root", stringbuilder, server, tickSpan);
        stringbuilder.append("--- END PROFILE DUMP ---\n\n");
        return stringbuilder.toString();
    }

    private void appendProfilerResults(int depth, String sectionName, StringBuilder builder, MinecraftServer server, int tickSpan)
    {
        List<OmniProfiler.Result> list = ((OmniProfiler) server.profiler).getProfilingData(sectionName, tickSpan, GCMessager.prevGCRuns - profileStartGCRuns, (GCMessager.prevGCTime - profileStartGCTime) * 1000000L, totalHeapUsage);

        for (int i = 1; i < list.size(); i++)
        {
            OmniProfiler.Result profilerResult = list.get(i);
            builder.append(String.format("[%02d] ", depth));

            for (int j = 0; j < depth; ++j) builder.append("|   ");

            if (profilerResult.gcRuns == 0) builder.append(profilerResult.profilerName).append(" - CPU: ").append(String.format("%.2f", profilerResult.tickUsePercentage)).append("%, Heap: ").append(profilerResult.heapUsage).append("\n");
            else builder.append(profilerResult.profilerName).append(" - ").append(String.format("%.2f", profilerResult.tickUsePercentage)).append("%, Heap: ").append(profilerResult.heapUsage).append(" (").append(profilerResult.gcRuns).append(" GC run(s))\n");

            if (!"unspecified".equals(profilerResult.profilerName))
            {
                try
                {
                    appendProfilerResults(depth + 1, sectionName + "." + profilerResult.profilerName, builder, server, tickSpan);
                }
                catch (Exception exception)
                {
                    builder.append("[[ EXCEPTION ").append(exception).append(" ]]");
                }
            }
        }
    }

    private static String getWittyComment()
    {
        String[] astring = new String[]{"Shiny numbers!", "Am I not running fast enough? :(", "I'm working as hard as I can!", "Will I ever be good enough for you? :(", "Speedy. Zoooooom!", "Hello world", "40% better than a crash report.", "Now with extra numbers", "Now with less numbers", "Now with the same numbers", "You should add flames to things, it makes them go faster!", "Do you feel the need for... optimization?", "*cracks redstone whip*", "Maybe if you treated it better then it'll have more motivation to work faster! Poor server."};

        try
        {
            return astring[(int) (System.nanoTime() % (long) astring.length)];
        }
        catch (Throwable var2)
        {
            return "Witty comment unavailable :(";
        }
    }

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "start", "stop") : Collections.emptyList();
    }
}