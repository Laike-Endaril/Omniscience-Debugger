package com.fantasticsource.omniscience.hack;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.tools.ReflectionTool;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OmniEventBus extends EventBus
{
    public ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners2 = (ConcurrentHashMap<Object, ArrayList<IEventListener>>) ReflectionTool.get(EventBus.class, "listeners", this);
    public Map<Object, ModContainer> listenerOwners2 = (Map<Object, ModContainer>) ReflectionTool.get(EventBus.class, "listenerOwners", this);

    public OmniEventBus(EventBus originalBus)
    {
        ReflectionTool.set(EventBus.class, "busID", this, ReflectionTool.get(EventBus.class, "busID", originalBus));
        ConcurrentHashMap<Object, ArrayList<IEventListener>> oldListeners = (ConcurrentHashMap<Object, ArrayList<IEventListener>>) ReflectionTool.get(EventBus.class, "listeners", originalBus);
        try
        {
            for (Map.Entry<Object, ArrayList<IEventListener>> entry : oldListeners.entrySet())
            {
                ArrayList<IEventListener> newList = new ArrayList<>();
                listeners2.put(entry.getKey(), newList);
                for (IEventListener originalListener : entry.getValue())
                {
                    if (originalListener instanceof ASMEventHandler && !originalListener.toString().contains("OmniProfiler")) newList.add(new OmniASMEventHandler((ASMEventHandler) originalListener));
                    else newList.add(originalListener);
                }
            }
        }
        catch (Exception e)
        {
            MCTools.crash(e, true);
        }
        listenerOwners2.putAll((Map<Object, ModContainer>) ReflectionTool.get(EventBus.class, "listenerOwners", originalBus));
    }

    @Override
    public void register(Object target)
    {
        if (listeners2.containsKey(target)) return;

        ModContainer activeModContainer = Loader.instance().activeModContainer();
        if (activeModContainer == null)
        {
            FMLLog.log.error("Unable to determine registrant mod for {}. This is a critical error and should be impossible", target, new Throwable());
            activeModContainer = Loader.instance().getMinecraftModContainer();
        }
        listenerOwners2.put(target, activeModContainer);
        boolean isStatic = target.getClass() == Class.class;
        @SuppressWarnings("unchecked")
        Set<? extends Class<?>> supers = isStatic ? Sets.newHashSet((Class<?>) target) : TypeToken.of(target.getClass()).getTypes().rawTypes();
        for (Method method : (isStatic ? (Class<?>) target : target.getClass()).getMethods())
        {
            if (isStatic && !Modifier.isStatic(method.getModifiers())) continue;
            else if (!isStatic && Modifier.isStatic(method.getModifiers())) continue;

            for (Class<?> cls : supers)
            {
                try
                {
                    Method real = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    if (real.isAnnotationPresent(SubscribeEvent.class))
                    {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length != 1)
                        {
                            throw new IllegalArgumentException(
                                    "Method " + method + " has @SubscribeEvent annotation, but requires " + parameterTypes.length + " arguments.  Event handler methods must require a single argument."
                            );
                        }

                        Class<?> eventType = parameterTypes[0];

                        if (!Event.class.isAssignableFrom(eventType))
                        {
                            throw new IllegalArgumentException("Method " + method + " has @SubscribeEvent annotation, but takes a argument that is not an Event " + eventType);
                        }

                        register(eventType, target, real, activeModContainer, listeners2);
                        break;
                    }
                }
                catch (NoSuchMethodException e)
                {
                    // Eat the error, this is not unexpected
                }
            }
        }
    }

    private void register(Class<?> eventType, Object target, Method method, final ModContainer owner, ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners)
    {
        try
        {
            Constructor<?> ctr = eventType.getConstructor();
            ctr.setAccessible(true);
            Event event = (Event) ctr.newInstance();
            ASMEventHandler original = new ASMEventHandler(target, method, owner, IGenericEvent.class.isAssignableFrom(eventType));
            final ASMEventHandler asm = original.toString().contains("OmniProfiler") ? original : new OmniASMEventHandler(original);

            IEventListener listener = asm;
            if (IContextSetter.class.isAssignableFrom(eventType))
            {
                listener = event1 ->
                {
                    ModContainer old = Loader.instance().activeModContainer();
                    Loader.instance().setActiveModContainer(owner);
                    ((IContextSetter) event1).setModContainer(owner);
                    asm.invoke(event1);
                    Loader.instance().setActiveModContainer(old);
                };
            }

            event.getListenerList().register((int) ReflectionTool.get(EventBus.class, "busID", this), asm.getPriority(), listener);

            ArrayList<IEventListener> others = listeners.computeIfAbsent(target, k -> new ArrayList<>());
            others.add(listener);
        }
        catch (Exception e)
        {
            FMLLog.log.error("Error registering event handler: {} {} {}", owner, eventType, method, e);
        }
    }
}
