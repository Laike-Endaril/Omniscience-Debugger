package com.fantasticsource.omniscience.hack;

import com.fantasticsource.mctools.ServerTickTimer;
import com.fantasticsource.omniscience.Debug;
import com.fantasticsource.omniscience.Omniscience;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Pair;
import net.minecraft.command.ICommandSender;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class OmniProfiler extends Profiler
{
    public static final OmniProfiler INSTANCE = new OmniProfiler();
    public static final String[] VALID_MODES = new String[]{"total", "average", "peak"};

    public static final String ROOT_NAME = "ROOT";
    public static final String FILENAME = OmniProfiler.class.getSimpleName() + ".java";

    public final LinkedHashMap<Long, SectionNode> PER_TICK_DATA = new LinkedHashMap<>();
    public SectionNode currentNode = null;
    public int startingLevel = -1, activeLevel = -1;
    public ArrayList<StringBuilder> stackComparisons = new ArrayList<>();
    public ArrayList<Predicate<Pair<ICommandSender, Results>>> stoppingCallbacks = new ArrayList<>();
    public Results lastRunResults = null;
    public HashSet<ICommandSender> listeners = new HashSet<>();

    public long startNanos, startHeapAllocated, startGCNanos;
    public int startGCRuns;

    protected OmniProfiler()
    {
    }

    public boolean isRunning()
    {
        return activeLevel != -1;
    }

    public void reset()
    {
        PER_TICK_DATA.clear();
        currentNode = null;
        activeLevel = -1;
        startingLevel = -1;
        stoppingCallbacks.clear();
        listeners.clear();
        stackComparisons.clear();
        //lastRunResults can be kept
    }

    public String start(ICommandSender starter, int level)
    {
        if (!(starter instanceof MinecraftServer)) listeners.add(starter);

        if (activeLevel > -1) return "Profiler is already running";
        if (startingLevel >= level)
        {
            if (level == 0) return "Profiler is already starting";
            else return "Profiler is already starting (debug level " + startingLevel + ")";
        }

        startingLevel = level;
        info("Starting profiler");
        return null;
    }


    public String stop(ICommandSender stopper, Predicate<Pair<ICommandSender, Results>> callback)
    {
        if (activeLevel == -1 && startingLevel == -1) return "Profiler is not running or starting";

        listeners.add(stopper);
        stoppingCallbacks.add(callback);
        info("Stopping profiler");
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START) INSTANCE.tick();
    }

    protected void info(String info)
    {
        System.out.println(info);
        for (ICommandSender listener : listeners)
        {
            listener.sendMessage(new TextComponentString(info));
        }
    }

    protected void error(String error)
    {
        System.err.println(error);
        for (ICommandSender listener : listeners)
        {
            listener.sendMessage(new TextComponentString(error));
        }
    }

    protected void tick()
    {
        if (activeLevel > -1)
        {
            if (startingLevel > -1) throw new IllegalStateException("Profiler tried to start while already active!");


            if (currentNode.parent != null)
            {
                error("profiler.startSection() was called more times this tick than profiler.endSection()!  Stopping profiling and resetting profiler state!");
                for (StringBuilder stackComparison : stackComparisons) error(stackComparison.toString());
                reset();
                return;
            }
            stackComparisons.clear();


            RuntimeState transitionState = endSection(true);


            if (stoppingCallbacks.size() > 0)
            {
                Predicate<Pair<ICommandSender, Results>>[] callbacks = stoppingCallbacks.toArray(new Predicate[0]);
                ICommandSender[] listeners = this.listeners.toArray(new ICommandSender[0]);
                lastRunResults = new Results(PER_TICK_DATA);

                reset();

                for (ICommandSender listener : listeners)
                {
                    for (Predicate<Pair<ICommandSender, Results>> callback : callbacks) callback.test(new Pair<>(listener, lastRunResults));
                }
            }
            else
            {
                currentNode = new SectionNode(transitionState);
                PER_TICK_DATA.put(ServerTickTimer.currentTick(), currentNode);
            }
        }


        if (startingLevel > -1)
        {
            activeLevel = startingLevel;

            PER_TICK_DATA.clear();
            currentNode = new SectionNode();
            PER_TICK_DATA.put(ServerTickTimer.currentTick(), currentNode);

            startNanos = currentNode.startState.nanos;
            startGCRuns = currentNode.startState.gcRuns;
            startGCNanos = currentNode.startState.gcNanos;
            startHeapAllocated = currentNode.startState.heapAllocated;

            startingLevel = -1;
        }
    }

    @Override
    public String getNameOfLastSection()
    {
        return currentNode == null ? "[UNKNOWN]" : currentNode.name;
    }

    @Override
    public void startSection(String name)
    {
        if (activeLevel > -1)
        {
            if (activeLevel > 0)
            {
                if (!Thread.currentThread().getName().equals("Server thread"))
                {
                    error("profiler.startSection() was called from somewhere besides the server thread!  Stopping profiling and resetting profiler state!");
                    for (StackTraceElement element : Thread.currentThread().getStackTrace()) error(element.toString());
                    reset();
                    return;
                }
            }


            currentNode = currentNode.children.computeIfAbsent(name, o -> new SectionNode(name, currentNode));
            currentNode.startState = activeLevel > 0 ? new DebugRuntimeState() : new RuntimeState();
        }
    }

    @Override
    public void endSection()
    {
        endSection(false);
    }

    public RuntimeState endSection(boolean isTickTransition)
    {
        if (activeLevel > -1)
        {
            if (currentNode == null || (!isTickTransition && currentNode.parent == null))
            {
                error("profiler.endSection() was called more times this tick than profiler.startSection()!  Stopping profiling and resetting profiler state!");
                for (StringBuilder stackComparison : stackComparisons) error(stackComparison.toString());
                reset();
                return null;
            }


            if (activeLevel > 0)
            {
                if (!Thread.currentThread().getName().equals("Server thread"))
                {
                    error("profiler.endSection() was called from somewhere besides the server thread!  Stopping profiling and resetting profiler state!");
                    for (StackTraceElement element : Thread.currentThread().getStackTrace()) error(element.toString());
                    reset();
                    return null;
                }


                StringBuilder stackComparison = new StringBuilder();
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                DebugRuntimeState startState = (DebugRuntimeState) currentNode.startState;
                for (int i = 0; i < Tools.min(stackTrace.length, startState.stackTrace.length); i++)
                {
                    StackTraceElement before = startState.stackTrace[startState.stackTrace.length - 1 - i], after = stackTrace[stackTrace.length - 1 - i];
                    if (activeLevel < 3 && FILENAME.equals(before.getFileName()) && FILENAME.equals(after.getFileName())) break;
                    if (activeLevel < 2 && before.getClassName().equals(after.getClassName()) && before.getMethodName().equals(after.getMethodName())) continue;


                    stackComparison.append("\n");
                    stackComparison.append("Caller: ").append(currentNode.fullName).append("\n\n");

                    stackComparison.append("Before:\n");
                    boolean found = false;
                    for (StackTraceElement element : startState.stackTrace)
                    {
                        if (found) stackComparison.append("* ").append(element).append("\n");
                        else
                        {
                            stackComparison.append(element.toString()).append("\n");
                            if (element.equals(before)) found = true;
                        }
                    }

                    stackComparison.append("\n\nAfter:\n");
                    found = false;
                    for (StackTraceElement element : stackTrace)
                    {
                        if (found) stackComparison.append("* ").append(element).append("\n");
                        else
                        {
                            stackComparison.append(element.toString()).append("\n");
                            if (element.equals(after)) found = true;
                        }
                    }

                    stackComparison.append("\n\n\n");
                }
                stackComparisons.add(stackComparison);
            }


            RuntimeState startState = currentNode.startState, endState = activeLevel > 0 ? new DebugRuntimeState() : new RuntimeState();
            currentNode.nanos += endState.nanos - startState.nanos;
            currentNode.gcRuns += endState.gcRuns - startState.gcRuns;
            currentNode.gcNanos += endState.gcNanos - startState.gcNanos;
            currentNode.heapAllocated += endState.heapAllocated - startState.heapAllocated;
            currentNode.executions++;

            currentNode.startState = null;

            currentNode = currentNode.parent;

            return endState;
        }

        return null;
    }

    @Override
    public void endStartSection(String name)
    {
        endSection();
        startSection(name);
    }

    @Override
    public void func_194340_a(Supplier<String> stringSupplier)
    {
        startSection(stringSupplier.get());
    }

    public static Results getLastRunResults()
    {
        return INSTANCE.lastRunResults;
    }


    public static class RuntimeState
    {
        public long nanos, heapAllocated, gcNanos;
        public int gcRuns;

        public RuntimeState()
        {
            this.nanos = System.nanoTime();
            this.heapAllocated = Debug.cumulativeServerThreadHeapAllocations();
            this.gcRuns = Debug.gcRuns();
            this.gcNanos = Debug.gcTime() * 1000000;
        }
    }

    public static class DebugRuntimeState extends RuntimeState
    {
        public StackTraceElement[] stackTrace;

        public DebugRuntimeState()
        {
            super();
            this.stackTrace = Thread.currentThread().getStackTrace();
        }
    }

    public static class SectionNode
    {
        public String name, fullName, mode = null;
        public SectionNode parent = null;
        public HashMap<String, SectionNode> children = new HashMap<>();
        public RuntimeState startState = null;
        public long nanos = 0, heapAllocated = 0, gcNanos = 0;
        public int gcRuns = 0, divisor = 1, executions = 0;

        public SectionNode()
        {
            this(ROOT_NAME, true);
        }

        public SectionNode(RuntimeState startState)
        {
            this(ROOT_NAME, false);
            this.startState = startState;
        }

        public SectionNode(String fullName, boolean initStartState)
        {
            this.fullName = fullName;
            this.name = fullName;
            if (initStartState) startState = INSTANCE.activeLevel > 0 ? new DebugRuntimeState() : new RuntimeState();
        }

        public SectionNode(String name, SectionNode parent)
        {
            this.name = name;
            fullName = parent.fullName + "." + name;
            this.parent = parent;
            this.mode = parent.mode;
            this.divisor = parent.divisor;
        }

        public SectionNode(String mode)
        {
            this(mode, INSTANCE.lastRunResults.perTickData);
        }

        public SectionNode(String mode, LinkedHashMap<Long, SectionNode> perTickData)
        {
            this(ROOT_NAME, false);
            this.mode = mode;

            if (!Tools.contains(VALID_MODES, mode)) throw new IllegalArgumentException("Mode is invalid: " + mode);

            switch (mode)
            {
                case "total":
                    for (SectionNode root : perTickData.values()) addRecursive(root);
                    break;


                case "average":
                    divisor = perTickData.size();
                    for (SectionNode root : perTickData.values()) addRecursive(root);
                    break;


                case "peak":
                    SectionNode max = this;
                    for (SectionNode root : perTickData.values())
                    {
                        if (root.nanos > max.nanos) max = root;
                    }
                    addRecursive(max);
                    break;
            }
        }


        public SectionNode addRecursive(SectionNode other)
        {
            if (!fullName.equals(other.fullName)) throw new IllegalArgumentException("Names should match, but don't (" + fullName + " vs " + other.fullName + ")");

            nanos += other.nanos;
            heapAllocated += other.heapAllocated;
            gcRuns += other.gcRuns;
            gcNanos += other.gcNanos;
            executions += other.executions;

            for (Map.Entry<String, SectionNode> entry : other.children.entrySet())
            {
                String name = entry.getKey();
                SectionNode node = entry.getValue();
                children.computeIfAbsent(entry.getKey(), o -> new SectionNode(name, this)).addRecursive(node);
            }

            return this;
        }


        @Override
        public String toString()
        {
            return toString(-7, "", 0, 0);
        }

        public String toString(int depth, String cumulativePrefix, float gcNanosPerHeap, float rootNanos)
        {
            if (parent == null)
            {
                gcNanosPerHeap = (float) gcNanos / heapAllocated;
                rootNanos = (float) nanos / divisor;
            }

            StringBuilder stringBuilder = new StringBuilder();
            float nanos = (float) this.nanos, heapAllocated = (float) this.heapAllocated, gcNanos = (float) this.gcNanos, executions = (float) this.executions, direct, fromGC;
            String line;
            if (mode == null) return fullName;
            else switch (mode)
            {
                case "total":
                    if (parent == null) stringBuilder.append("--- START OF TOTAL RESULTS ---\n\n");

                    direct = 100f * (nanos - gcNanos) / rootNanos;
                    fromGC = 100f * gcNanosPerHeap * heapAllocated / rootNanos;
                    if (direct + fromGC < 0.1) return "";


                    line = "[" + String.format("%02d", depth) + "] " + cumulativePrefix + name + " ";
                    stringBuilder.append(line);
                    for (int i = line.length(); i < 100; i++) stringBuilder.append('-');

                    stringBuilder.append(String.format("%1$6s", " " + String.format("%.1f", direct))).append("% direct     ~")
                            .append(String.format("%1$5s", String.format("%.1f", fromGC))).append("% in GC     ~")
                            .append(String.format("%1$5s", String.format("%.1f", direct + fromGC))).append("% total     ")
                            .append(String.format("%1$9s", String.format("%.1f", executions))).append(" executions     ")
                            .append(String.format("%1$10s", String.format("%.1f", nanos))).append(" nanos     ")
                            .append(String.format("%1$15s", String.format("%.1f", heapAllocated))).append(" bytes\n");

                    for (SectionNode node : children.values()) stringBuilder.append(node.toString(depth + 1, cumulativePrefix + "|   ", gcNanosPerHeap, rootNanos));
                    if (parent == null) stringBuilder.append("\n--- END OF TOTAL RESULTS ---");
                    return stringBuilder.toString();


                case "average":
                    nanos /= divisor;
                    heapAllocated /= divisor;
                    gcNanos /= divisor;
                    executions /= divisor;

                    if (parent == null) stringBuilder.append("--- START OF AVERAGED RESULTS ---\n\n");

                    direct = 100f * (nanos - gcNanos) / rootNanos;
                    fromGC = 100f * gcNanosPerHeap * heapAllocated / rootNanos;
                    if (direct + fromGC < 0.1) return "";


                    line = "[" + String.format("%02d", depth) + "] " + cumulativePrefix + name + " ";
                    stringBuilder.append(line);
                    for (int i = line.length(); i < 100; i++) stringBuilder.append('-');

                    stringBuilder.append(String.format("%1$6s", " " + String.format("%.1f", direct))).append("% direct     ~")
                            .append(String.format("%1$5s", String.format("%.1f", fromGC))).append("% in GC     ~")
                            .append(String.format("%1$5s", String.format("%.1f", direct + fromGC))).append("% total     ")
                            .append(String.format("%1$9s", String.format("%.1f", executions))).append(" executions/t     ")
                            .append(String.format("%1$10s", String.format("%.1f", nanos))).append(" nanos/t     ")
                            .append(String.format("%1$15s", String.format("%.1f", heapAllocated))).append(" bytes/t\n");

                    for (SectionNode node : children.values()) stringBuilder.append(node.toString(depth + 1, cumulativePrefix + "|   ", gcNanosPerHeap, rootNanos));
                    if (parent == null) stringBuilder.append("\n--- END OF AVERAGED RESULTS ---");
                    return stringBuilder.toString();


                case "peak":
                    if (parent == null) stringBuilder.append("--- START OF PEAK RESULTS ---\n\n");

                    direct = 100f * (nanos - gcNanos) / rootNanos;
                    fromGC = 100f * gcNanosPerHeap * heapAllocated / rootNanos;
                    if (direct + fromGC < 0.1) return "";


                    line = "[" + String.format("%02d", depth) + "] " + cumulativePrefix + name + " ";
                    stringBuilder.append(line);
                    for (int i = line.length(); i < 100; i++) stringBuilder.append('-');

                    stringBuilder.append(String.format("%1$6s", " " + String.format("%.1f", direct))).append("% direct     ~")
                            .append(String.format("%1$5s", String.format("%.1f", fromGC))).append("% in GC     ~")
                            .append(String.format("%1$5s", String.format("%.1f", direct + fromGC))).append("% total     ")
                            .append(String.format("%1$9s", String.format("%.1f", executions))).append(" executions     ")
                            .append(String.format("%1$10s", String.format("%.1f", nanos))).append(" nanos     ")
                            .append(String.format("%1$15s", String.format("%.1f", heapAllocated))).append(" bytes\n");

                    for (SectionNode node : children.values()) stringBuilder.append(node.toString(depth + 1, cumulativePrefix + "|   ", gcNanosPerHeap, rootNanos));
                    if (parent == null) stringBuilder.append("\n--- END OF PEAK RESULTS ---");
                    return stringBuilder.toString();


                default:
                    throw new IllegalStateException("This should never happen!");
            }
        }
    }

    public static class Results
    {
        public final LinkedHashMap<Long, SectionNode> perTickData;
        public final SectionNode totaledData;

        public Results(LinkedHashMap<Long, SectionNode> perTickData)
        {
            this.perTickData = (LinkedHashMap<Long, SectionNode>) perTickData.clone();
            totaledData = new SectionNode("total", perTickData);
        }

        @Override
        public String toString()
        {
            return toString("average");
        }

        public String toString(String mode)
        {
            return "\n==== " + Omniscience.NAME.toUpperCase() + " PROFILER RESULTS ====\n\n" +
                    "Time span: " + String.format("%,d", totaledData.nanos) + " nanos\n" +
                    "Tick span: " + perTickData.size() + " ticks\n" +
                    "Ticks per second: " + String.format("%.2f", (float) perTickData.size() * 1_000_000_000 / totaledData.nanos) + " (should be ~20)\n" +
                    "Average tick time: " + (totaledData.nanos / perTickData.size()) + " nanos\n" +
                    "Garbage collectors ran " + totaledData.gcRuns + " times during profiling (~" + String.format("%,d", totaledData.gcNanos) + " nanos / " + String.format("%.1f", 100f * totaledData.gcNanos / totaledData.nanos) + "% of total time)\n" +
                    "\n\n" + new SectionNode(mode.toLowerCase(), perTickData) + "\n";
        }
    }
}