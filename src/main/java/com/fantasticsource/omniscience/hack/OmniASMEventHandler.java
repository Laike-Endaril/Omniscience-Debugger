package com.fantasticsource.omniscience.hack;

import com.fantasticsource.tools.ReflectionTool;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.lang.reflect.Method;

public class OmniASMEventHandler extends ASMEventHandler
{
    public static final Object DUMMY_TARGET = 0;
    public static final Method DUMMY_METHOD = ReflectionTool.getMethod(String.class, "contains");

    public ModContainer modContainer;
    public ASMEventHandler original = null;
    public boolean dontProfile;

    public OmniASMEventHandler(ASMEventHandler original) throws Exception
    {
        this(DUMMY_TARGET, DUMMY_METHOD, (ModContainer) ReflectionTool.get(ASMEventHandler.class, "owner", original));
    }

    @Deprecated
    public OmniASMEventHandler(Object target, Method method, ModContainer owner) throws Exception
    {
        this(target, method, owner, false);
    }

    public OmniASMEventHandler(Object target, Method method, ModContainer owner, boolean isGeneric) throws Exception
    {
        super(target, method, owner, isGeneric);

        dontProfile = target == OmniProfiler.class;
        modContainer = owner;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void invoke(Event event)
    {
        OmniProfiler profiler = Thread.currentThread().getName().equals("Server thread") ? (OmniProfiler) FMLCommonHandler.instance().getMinecraftServerInstance().profiler : null;
        if (!dontProfile && profiler != null) profiler.startSection("@Subscribe " + event.getClass().getSimpleName() + "(" + modContainer.getName() + ")");

        if (original != null) original.invoke(event);
        else super.invoke(event);

        if (!dontProfile && profiler != null) profiler.endSection();
    }
}