package com.fantasticsource.omniscience;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.omniscience.client.PathVisualizer;
import com.fantasticsource.omniscience.hack.OmniASMEventHandler;
import com.fantasticsource.omniscience.hack.OmniProfiler;
import com.fantasticsource.omniscience.hack.OmniTimeTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.*;

import static com.fantasticsource.omniscience.Omniscience.MODID;
import static net.minecraft.util.text.TextFormatting.AQUA;
import static net.minecraft.util.text.TextFormatting.WHITE;

public class Commands extends CommandBase
{
    private static ArrayList<String> subcommands = new ArrayList<>();

    static
    {
        subcommands.addAll(Arrays.asList("threads", "nbt", "memory", "entities", "pathing", "ai", "profiling"));
    }


    @Override
    public String getName()
    {
        return "omni";
    }

    @Override
    public List<String> getAliases()
    {
        ArrayList<String> names = new ArrayList<>();

        names.add("omniscience");

        return names;
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return subUsage("");
    }

    public String subUsage(String subcommand)
    {
        if (!subcommands.contains(subcommand))
        {
            StringBuilder s = new StringBuilder(AQUA + "/" + getName() + " <" + subcommands.get(0));
            for (int i = 1; i < subcommands.size(); i++) s.append(" | ").append(subcommands.get(i));
            s.append(">");
            return s.toString();
        }

        switch (subcommand)
        {
            case "threads":
                return AQUA + "/" + getName() + " threads" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.threads.comment")
                        + "\n" + AQUA + "/" + getName() + " threads <id>" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.threads.comment2")
                        + "\n" + AQUA + "/" + getName() + " threads <id> stop" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.threads.comment3");

            case "nbt":
                return AQUA + "/" + getName() + " nbt hand" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.nbt.comment")
                        + "\n" + AQUA + "/" + getName() + " nbt self" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.nbt.comment2")
                        + "\n" + AQUA + "/" + getName() + " nbt nearestentity" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.nbt.comment3");

            case "profiling":
                return AQUA + "/" + getName() + " profiling entities" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.profiling.comment")
                        + "\n" + AQUA + "/" + getName() + " profiling tileentities" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.profiling.comment2")
                        + "\n" + AQUA + "/" + getName() + " profiling events" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.profiling.comment3")
                        + "\n" + AQUA + "/" + getName() + " profiling eventobjects" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.profiling.comment4")
                        + "\n" + AQUA + "/" + getName() + " profiling eventmethods" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd.profiling.comment5");

            default:
                return AQUA + "/" + getName() + " " + subcommand + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".cmd." + subcommand + ".comment");
        }
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length == 0) sender.getCommandSenderEntity().sendMessage(new TextComponentString(subUsage("")));
        else subCommand(sender, args);
    }

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        ArrayList<String> result = new ArrayList<>();

        String partial = args[args.length - 1];
        switch (args.length)
        {
            case 1:
                result.addAll(subcommands);
                break;

            case 2:
                switch (args[0])
                {
                    case "threads":
                        for (long id : Debug.threadIDs()) result.add("" + id);
                        break;

                    case "nbt":
                        result.add("hand");
                        result.add("self");
                        result.add("nearestentity");
                        break;

                    case "profiling":
                        result.add("entities");
                        result.add("tileentities");
                        result.add("events");
                        result.add("eventobjects");
                        result.add("eventmethods");
                }
                break;

            case 3:
                switch (args[0])
                {
//                    case "threads":
//                        result.add("stop");
//                        break;
                }
                break;
        }
        if (partial.length() != 0) result.removeIf(k -> partial.length() > k.length() || !k.substring(0, partial.length()).equalsIgnoreCase(partial));
        return result;
    }

    private void subCommand(ICommandSender sender, String[] args)
    {
        String cmd = args[0];
        switch (cmd)
        {
            case "threads":
                if (args.length == 1)
                {
                    for (String s : Debug.threadDataStrings())
                    {
                        notifyCommandListener(sender, this, s);
                    }
                }
                else if (args.length < 4) //2 <= args.length <= 3, as it cannot be 0 or 1 at this point
                {
                    long id = Debug.threadID(args[1]);
                    if (id == -1)
                    {
                        try
                        {
                            id = Long.parseLong(args[1].trim());
                        }
                        catch (NumberFormatException e)
                        {
                            //Just leave id at -1
                        }
                    }

                    if (id == -1)
                    {
                        notifyCommandListener(sender, this, MODID + ".error.threads.notFound", args[1]);
                        return;
                    }

                    Thread thread = Debug.getThread(id);
                    if (thread == null)
                    {
                        notifyCommandListener(sender, this, MODID + ".error.threads.notFound", args[1]);
                        return;
                    }

                    if (args.length == 2)
                    {
                        notifyCommandListener(sender, this, "Thread \"" + thread.getName() + "\" (ID = " + thread.getId() + ") of class \"" + thread.getClass().getName() + "\"");

                        StringBuilder threadGroups = new StringBuilder("Thread Group Lineage is ... ");
                        String[] strings = Debug.threadGroupLineageStrings(thread);
                        threadGroups.append("(").append(strings[0]);
                        for (int i = 1; i < strings.length; i++)
                        {
                            threadGroups.append(" -> ").append(strings[i]);
                        }
                        threadGroups.append(")");


                        notifyCommandListener(sender, this, threadGroups.toString());
                        for (StackTraceElement element : thread.getStackTrace()) notifyCommandListener(sender, this, element.toString());
                    }
                    else
                    {
//                        if (args[2].equals("stop"))
//                        {
//                            thread.stop();
//                            notifyCommandListener(sender, this, "Stopping thread " + id + " (" + thread.getName() + ")");
//                        }
//                        else
                        notifyCommandListener(sender, this, subUsage(cmd));
                    }
                }
                else notifyCommandListener(sender, this, subUsage(cmd));
                break;


            case "nbt":
                switch (args.length)
                {
                    case 1:
                        notifyCommandListener(sender, this, "hand");
                        notifyCommandListener(sender, this, "self");
                        notifyCommandListener(sender, this, "nearestentity");
                        break;

                    case 2:
                        if (!(sender instanceof EntityPlayerMP))
                        {
                            notifyCommandListener(sender, this, MODID + ".error.notPlayer", cmd);
                            return;
                        }

                        Entity entity = null;
                        NBTTagCompound compound;
                        if (args[1].equals("hand"))
                        {
                            notifyCommandListener(sender, this, "");
                            notifyCommandListener(sender, this, ((EntityPlayerMP) sender).getHeldItemMainhand().getDisplayName());
                            notifyNBT(sender, ((EntityPlayerMP) sender).getHeldItemMainhand().writeToNBT(new NBTTagCompound()));
                            notifyCommandListener(sender, this, "");
                        }
                        else
                        {
                            switch (args[1])
                            {
                                case "self":
                                    entity = (Entity) sender;
                                    break;

                                case "nearestentity":
                                    EntityPlayerMP player = (EntityPlayerMP) sender;
                                    entity = player.world.findNearestEntityWithinAABB(Entity.class, player.getEntityBoundingBox().grow(100), player);
                                    break;
                            }

                            if (entity != null)
                            {
                                notifyCommandListener(sender, this, "");
                                compound = new NBTTagCompound();
                                if (!entity.writeToNBTAtomically(compound))
                                {
                                    if (entity instanceof EntityPlayer) compound.setString("id", "minecraft:player");
                                    entity.writeToNBT(compound);
                                }
                                notifyNBT(sender, compound);
                                notifyCommandListener(sender, this, "");
                            }
                            else notifyCommandListener(sender, this, MODID + ".error.noEntityFound");
                        }
                        break;
                }
                break;


            case "memory":
                notifyCommandListener(sender, this, "omniscience.literal1", Debug.memData());
                break;


            case "entities":
                int players;
                LinkedHashMap<Class, int[]> classCounts = new LinkedHashMap<>();
                for (WorldServer world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds)
                {
                    sender.sendMessage(new TextComponentString(""));

                    players = world.playerEntities.size();
                    sender.sendMessage(new TextComponentString(world.getWorldInfo().getWorldName() + " (" + world.provider.getDimension() + "): " + world.loadedEntityList.size() + " (" + players + (players == 1 ? " player)" : " players)")));

                    classCounts.clear();
                    for (Entity entity : world.loadedEntityList)
                    {
                        classCounts.computeIfAbsent(entity.getClass(), o -> new int[]{0})[0]++;
                    }

                    int max = 0;
                    Class maxClass = null;
                    for (Map.Entry<Class, int[]> entry : classCounts.entrySet())
                    {
                        if (entry.getValue()[0] > max)
                        {
                            max = entry.getValue()[0];
                            maxClass = entry.getKey();
                        }
                    }

                    if (max > 0) sender.sendMessage(new TextComponentString("Highest count: " + maxClass.getName() + " (" + max + ")"));
                }
                break;


            case "pathing":
            {
                if (sender instanceof EntityPlayerMP)
                {
                    EntityPlayerMP player = (EntityPlayerMP) sender;
                    Entity entity = player.world.findNearestEntityWithinAABB(EntityLiving.class, player.getEntityBoundingBox().grow(100), player);
                    if (entity != null)
                    {
                        BlockPos pos = entity.getPosition();
                        ArrayList<Integer> trackedEntities = PathVisualizer.pathTrackedEntities.get(player);
                        if (trackedEntities == null || !trackedEntities.contains(entity.getEntityId()))
                        {
                            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> PathVisualizer.pathTrackedEntities.computeIfAbsent(player, o -> new ArrayList<>()).add(entity.getEntityId()));
                            notifyCommandListener(sender, this, MODID + ".cmd.pathing.start", entity.getDisplayName(), pos.getX(), pos.getY(), pos.getZ());
                        }
                        else
                        {
                            trackedEntities.remove((Integer) entity.getEntityId());
                            notifyCommandListener(sender, this, MODID + ".cmd.pathing.stop", entity.getDisplayName(), pos.getX(), pos.getY(), pos.getZ());
                        }
                    }
                    else notifyCommandListener(sender, this, MODID + ".error.noEntityFound");
                }
                else notifyCommandListener(sender, this, MODID + ".error.notPlayer", cmd);
                break;
            }


            case "ai":
            {
                if (sender instanceof EntityPlayerMP)
                {
                    EntityPlayerMP player = (EntityPlayerMP) sender;
                    Entity entity = player.world.findNearestEntityWithinAABB(EntityLiving.class, player.getEntityBoundingBox().grow(100), player);
                    if (entity != null)
                    {
                        for (String s : MCTools.getAITaskData((EntityLiving) entity)) notifyCommandListener(sender, this, s);
                    }
                    else notifyCommandListener(sender, this, MODID + ".error.noEntityFound");
                }
                else notifyCommandListener(sender, this, MODID + ".error.notPlayer", cmd);
                break;
            }


            case "profiling":
                if (OmniProfiler.INSTANCE.isRunning())
                {
                    notifyCommandListener(sender, this, MODID + ".error.profilerRunning");
                    return;
                }

                switch (args.length)
                {
                    case 1:
                        notifyCommandListener(sender, this, subUsage(cmd));
                        break;

                    case 2:
                        Boolean flag = null;
                        switch (args[1])
                        {
                            case "entities":
                                flag = OmniTimeTracker.ENTITY_TIME_TRACKER.profile = !OmniTimeTracker.ENTITY_TIME_TRACKER.profile;
                                break;

                            case "tileentities":
                                flag = OmniTimeTracker.TILE_ENTITY_TIME_TRACKER.profile = !OmniTimeTracker.TILE_ENTITY_TIME_TRACKER.profile;
                                break;

                            case "events":
                                flag = OmniASMEventHandler.profileEvents = !OmniASMEventHandler.profileEvents;
                                break;

                            case "eventobjects":
                                flag = OmniASMEventHandler.profileEventObjects = !OmniASMEventHandler.profileEventObjects;
                                if (flag && !OmniASMEventHandler.profileEvents)
                                {
                                    OmniASMEventHandler.profileEvents = true;
                                    notifyCommandListener(sender, this, MODID + "." + cmd + ".events.true");
                                }
                                break;

                            case "eventmethods":
                                flag = OmniASMEventHandler.profileEventMethods = !OmniASMEventHandler.profileEventMethods;
                                if (flag && !OmniASMEventHandler.profileEvents)
                                {
                                    OmniASMEventHandler.profileEvents = true;
                                    notifyCommandListener(sender, this, MODID + "." + cmd + ".events.true");
                                }
                                break;

                            default:
                                notifyCommandListener(sender, this, subUsage(cmd));
                        }
                        if (flag != null) notifyCommandListener(sender, this, MODID + "." + cmd + "." + args[1] + "." + flag);
                        break;
                }
                break;


            default:
                notifyCommandListener(sender, this, subUsage(cmd));
        }
    }

    private void notifyNBT(ICommandSender sender, NBTTagCompound compound)
    {
        for (String s : MCTools.legibleNBT(compound))
        {
            notifyCommandListener(sender, this, s);
        }
    }
}
