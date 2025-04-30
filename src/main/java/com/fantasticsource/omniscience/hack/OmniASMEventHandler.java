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
    public static boolean profileEvents = true, profileEventObjects = true, profileEventMethods = true;
    public static final Object DUMMY_TARGET = 0;
    public static final Method DUMMY_METHOD = ReflectionTool.getMethod(String.class, "contains");

    public ASMEventHandler original;
    public ModContainer modContainer;
    public String callingObject, callingMethod;

    public OmniASMEventHandler(ASMEventHandler original) throws Exception
    {
        super(DUMMY_TARGET, DUMMY_METHOD, (ModContainer) ReflectionTool.get(ASMEventHandler.class, "owner", original));
        this.original = original;
        this.modContainer = (ModContainer) ReflectionTool.get(ASMEventHandler.class, "owner", original);

        callingObject = original.toString();
        if (callingObject.contains(" class ")) callingObject = callingObject.replaceAll(".*: (class [^ ]*) .*", "$1");
        else callingObject = callingObject.replaceAll(".*: ([^ ]*) .*", "$1");
        callingMethod = original.toString().replaceAll(".* ([^(]*.*)", "$1");
        callingMethod = callingMethod.replaceAll("([^(]*.)[^,;]*/([^,;]*).*", "$1$2)");
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
        if (profileEvents && profiler instanceof OmniProfiler)
        {
            profiler.startSection("@Subscribe " + event.getClass().getSimpleName() + "(" + modContainer.getModId() + ")");
            if (profileEventObjects) profiler.startSection(callingObject);
            if (profileEventMethods) profiler.startSection(callingMethod);
        }

        if (original != null) original.invoke(event);
        else super.invoke(event);

        if (profileEvents && profiler instanceof OmniProfiler)
        {
            if (profileEventMethods) profiler.endSection();
            if (profileEventObjects) profiler.endSection();
            profiler.endSection();
        }
    }

    @Override
    public String toString()
    {
        return original.toString();
    }
}