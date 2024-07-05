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

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandDebug extends CommandBase
{
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
        if (args.length < 1) throw new WrongUsageException("commands.debug.usage");

        if (args[0].equals("start"))
        {
            String error = args.length > 1 ? OmniProfiler.INSTANCE.start(sender, Integer.parseInt(args[1])) : OmniProfiler.INSTANCE.start(sender, 0);
            if (error != null) notifyCommandListener(sender, this, error);
        }
        else if (args[0].equals("stop"))
        {
            String error = OmniProfiler.INSTANCE.stop(sender, pair ->
            {
                String profilerResults = pair.getValue().toString();
                saveProfilerResults(server, profilerResults);
                if (sender instanceof EntityPlayerMP) sendProfilerResults((EntityPlayerMP) sender, profilerResults);
                return true;
            });
            if (error != null) notifyCommandListener(sender, this, error);
        }
        else if (Tools.contains(OmniProfiler.VALID_MODES, args[0]))
        {
            String profilerResults = OmniProfiler.getLastRunResults().toString(args[0]);
            saveProfilerResults(server, profilerResults);
            if (sender instanceof EntityPlayerMP) sendProfilerResults((EntityPlayerMP) sender, profilerResults);
        }
        else throw new WrongUsageException(getUsage(sender));
    }

    private void saveProfilerResults(MinecraftServer server, String profilerResults)
    {
        File file = new File(server.getFile("debug"), "profile-results-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt");
        file.getParentFile().mkdirs();
        Writer writer = null;
        try
        {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write(profilerResults);
            System.out.println("Saved profiler results to " + file);
        }
        catch (Throwable throwable)
        {
            System.err.println("Could not save profiler results to " + file + throwable);
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

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        ArrayList<String> possibilities = new ArrayList<>();
        possibilities.add("start");
        possibilities.add("stop");
        possibilities.addAll(Arrays.asList(OmniProfiler.VALID_MODES));

        if (args.length == 1) return getListOfStringsMatchingLastWord(args, possibilities);
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, "1", "2", "3");
        return new ArrayList<>();
    }
}