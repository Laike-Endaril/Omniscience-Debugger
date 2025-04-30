package com.fantasticsource.omniscience.client;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.omniscience.CodePointPrinter;
import com.fantasticsource.omniscience.Omniscience;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.IClientCommand;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fantasticsource.omniscience.Omniscience.MODID;
import static net.minecraft.util.text.TextFormatting.AQUA;
import static net.minecraft.util.text.TextFormatting.WHITE;

public class ClientCommands extends CommandBase implements IClientCommand
{
    private static ArrayList<String> subcommands = new ArrayList<>();

    static
    {
        subcommands.addAll(Arrays.asList("screens", "nbt", "codeprinter"));
    }


    @Override
    public String getName()
    {
        return "omniclient";
    }

    @Override
    public List<String> getAliases()
    {
        ArrayList<String> names = new ArrayList<>();

        names.add("omniscienceclient");

        return names;
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }

    @Override
    public boolean allowUsageWithoutPrefix(ICommandSender sender, String message)
    {
        return false;
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
            case "nbt":
                return AQUA + "/" + getName() + " nbt hand" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".ccmd.nbt.comment")
                        + "\n" + AQUA + "/" + getName() + " nbt self" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".ccmd.nbt.comment2")
                        + "\n" + AQUA + "/" + getName() + " nbt nearestentity" + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".ccmd.nbt.comment3");

            default:
                return AQUA + "/" + getName() + " " + subcommand + WHITE + " - " + I18n.translateToLocalFormatted(MODID + ".ccmd." + subcommand + ".comment");
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length == 0) sender.getCommandSenderEntity().sendMessage(new TextComponentString(subUsage("")));
        else subCommand(sender, args);
    }

    @Override
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
                    case "screens":
                        result.add("live");
                        break;

                    case "nbt":
                        result.add("hand");
                        result.add("self");
                        result.add("nearestentity");
                        break;
                }
                break;

            case 3:
                switch (args[0])
                {
                    case "threads":
                        result.add("stop");
                        break;
                }
                break;
        }
        if (partial.length() != 0) result.removeIf(k -> partial.length() > k.length() || !k.substring(0, partial.length()).equalsIgnoreCase(partial));
        return result;
    }

    private void subCommand(ICommandSender sender, String[] args)
    {
        EntityPlayer player = (EntityPlayer) sender;
        String cmd = args[0];
        switch (cmd)
        {
            case "screens":
                if (args.length == 2)
                {
                    if (args[1].equals("live"))
                    {
                        ScreenDebug.live = !ScreenDebug.live;
                        if (ScreenDebug.live)
                        {
                            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Started live GuiScreen debugging"));
                        }
                        else
                        {
                            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Stopped live GuiScreen debugging"));
                        }
                    }
                    else sender.sendMessage(new TextComponentString(getUsage(sender)));
                }
                else
                {
                    ScreenDebug.showRecent();
                }
                break;


            case "nbt":
                switch (args.length)
                {
                    case 1:
                        sender.sendMessage(new TextComponentString("hand"));
                        sender.sendMessage(new TextComponentString("self"));
                        sender.sendMessage(new TextComponentString("nearestentity"));
                        break;

                    case 2:
                        Entity entity = null;
                        NBTTagCompound compound;
                        if (args[1].equals("hand"))
                        {
                            sender.sendMessage(new TextComponentString(""));
                            sender.sendMessage(new TextComponentString(((EntityPlayer) sender).getHeldItemMainhand().getDisplayName()));
                            notifyNBT(sender, ((EntityPlayer) sender).getHeldItemMainhand().writeToNBT(new NBTTagCompound()));
                            sender.sendMessage(new TextComponentString(""));
                        }
                        else
                        {
                            switch (args[1])
                            {
                                case "self":
                                    entity = (Entity) sender;
                                    break;

                                case "nearestentity":
                                    entity = player.world.findNearestEntityWithinAABB(Entity.class, player.getEntityBoundingBox().grow(100), player);
                                    break;
                            }

                            if (entity != null)
                            {
                                sender.sendMessage(new TextComponentString(""));
                                compound = new NBTTagCompound();
                                if (!entity.writeToNBTAtomically(compound))
                                {
                                    if (entity instanceof EntityPlayer) compound.setString("id", "minecraft:player");
                                    entity.writeToNBT(compound);
                                }
                                notifyNBT(sender, compound);
                                sender.sendMessage(new TextComponentString(""));
                            }
                            else sender.sendMessage(new TextComponentString(I18n.translateToLocalFormatted(MODID + ".error.noEntityFound")));
                        }
                        break;
                }
                break;


            case "codeprinter":
                boolean flag = CodePointPrinter.enabled = !CodePointPrinter.enabled;
                sender.sendMessage(new TextComponentString(I18n.translateToLocalFormatted(MODID + "." + cmd + "." + flag)));
                break;


            default:
                sender.sendMessage(new TextComponentString(subUsage(cmd)));
        }
    }

    private void notifyNBT(ICommandSender sender, NBTTagCompound compound)
    {
        for (String s : MCTools.legibleNBT(compound))
        {
            sender.sendMessage(new TextComponentString(s));
        }
    }
}
