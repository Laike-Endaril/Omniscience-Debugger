package com.fantasticsource.omniscience.hack;

import com.fantasticsource.tools.ReflectionTool;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;

import java.lang.reflect.Method;

public class OmniASMEventHandler extends ASMEventHandler
{
    public static final Object DUMMY_TARGET = 0;
    public static final Method DUMMY_METHOD = ReflectionTool.getMethod(String.class, "contains");

    public ASMEventHandler original;
    public boolean dontProfile;
    public ModContainer modContainer;

    public OmniASMEventHandler(ASMEventHandler original) throws Exception
    {
        super(DUMMY_TARGET, DUMMY_METHOD, (ModContainer) ReflectionTool.get(ASMEventHandler.class, "owner", original));
        this.original = original;
        this.dontProfile = original.toString().contains("OmniProfiler");
        this.modContainer = (ModContainer) ReflectionTool.get(ASMEventHandler.class, "owner", original);
    }

    @Override
    public EventPriority getPriority()
    {
        return original.getPriority();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void invoke(Event event)
    {
        Profiler profiler = Thread.currentThread().getName().equals("Server thread") ? FMLCommonHandler.instance().getMinecraftServerInstance().profiler : null;
        if (!dontProfile && profiler instanceof OmniProfiler) profiler.startSection("@Subscribe " + event.getClass().getSimpleName() + "(" + modContainer.getName() + ")");

        if (original != null) original.invoke(event);
        else super.invoke(event);

        if (!dontProfile && profiler instanceof OmniProfiler) profiler.endSection();
    }
}