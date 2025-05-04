package com.fantasticsource.omniscience;

import com.fantasticsource.mctools.ServerTickTimer;
import com.fantasticsource.tools.datastructures.SortableTable;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.ThreadMXBean;
import net.minecraft.util.text.TextFormatting;
import org.objectweb.asm.util.ASMifier;
import sun.management.HotSpotDiagnostic;

import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.nio.file.FileSystemException;
import java.util.ArrayList;

public class Debug
{
    public static final Runtime RUNTIME = Runtime.getRuntime();
    public static final ThreadMXBean THREAD_MX_BEAN = ((ThreadMXBean) ManagementFactory.getThreadMXBean());
    public static final RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();
    public static final HotSpotDiagnosticMXBean HOTSPOT_MX_BEAN = new HotSpotDiagnostic();
    protected static long serverThreadID = -1;

    public static void init()
    {
        THREAD_MX_BEAN.setThreadAllocatedMemoryEnabled(true);
        THREAD_MX_BEAN.setThreadCpuTimeEnabled(true);
    }

    public static void serverInit()
    {
        for (long id : THREAD_MX_BEAN.getAllThreadIds())
        {
            if (THREAD_MX_BEAN.getThreadInfo(id).getThreadName().equals("Server thread"))
            {
                serverThreadID = id;
                break;
            }
        }
    }


    public static int threadCount()
    {
        return THREAD_MX_BEAN.getThreadCount();
    }

    public static long[] threadIDs()
    {
        return THREAD_MX_BEAN.getAllThreadIds();
    }

    public static ArrayList<String> threadNames()
    {
        ArrayList<String> names = new ArrayList<>();
        for (long id : threadIDs()) names.add(THREAD_MX_BEAN.getThreadInfo(id).getThreadName());
        return names;
    }

    public static long threadID(String name)
    {
        ThreadInfo info;
        for (long id : threadIDs())
        {
            info = THREAD_MX_BEAN.getThreadInfo(id);
            if (info.getThreadName().equals(name)) return id;
        }

        return -1;
    }

    public static String threadName(long id)
    {
        ThreadInfo info = THREAD_MX_BEAN.getThreadInfo(id);
        return info == null ? null : info.getThreadName();
    }

    public static ArrayList<ThreadGroup> threadGroupLineage()
    {
        return threadGroupLineage(Thread.currentThread());
    }

    public static ArrayList<ThreadGroup> threadGroupLineage(Thread thread)
    {
        ArrayList<ThreadGroup> result = new ArrayList<>();

        ThreadGroup group = thread.getThreadGroup();
        while (group != null)
        {
            result.add(group);
            group = group.getParent();
        }

        return result;
    }

    public static String[] threadGroupLineageStrings()
    {
        return threadGroupLineageStrings(Thread.currentThread());
    }

    public static String[] threadGroupLineageStrings(Thread thread)
    {
        ArrayList<ThreadGroup> groups = threadGroupLineage(thread);
        String[] result = new String[groups.size()];
        int size = result.length;

        for (int i = 0; i < size; i++)
        {
            result[i] = groups.get(size - 1 - i).getName();
        }

        return result;
    }

    public static Thread[] getThreads()
    {
        ThreadGroup group = Thread.currentThread().getThreadGroup();

        for (ThreadGroup parent = group.getParent(); parent != null; parent = group.getParent())
        {
            group = parent;
        }

        Thread[] threads = new Thread[threadCount()];
        group.enumerate(threads);
        return threads;
    }

    public static Thread getThread(long id)
    {
        for (Thread thread : getThreads())
        {
            if (thread.getId() == id) return thread;
        }
        return null;
    }

    public static SortableTable threadData()
    {
        SortableTable result = new SortableTable(Long.class, String.class, Long.class, Long.class, Long.class, Long.class);
        result.labels("ID", "Name", "CPU", "CPU/tick", "Heap", "Heap/tick");
        result.units("", "", "ns", "ns/t", "B", "B/t");
        result.startSorting(4, false);

        for (long id : THREAD_MX_BEAN.getAllThreadIds())
        {
            long cpu = THREAD_MX_BEAN.getThreadCpuTime(id);
            long heap = THREAD_MX_BEAN.getThreadAllocatedBytes(id);
            result.add(id, THREAD_MX_BEAN.getThreadInfo(id).getThreadName(), cpu, (cpu / ServerTickTimer.currentTick()), heap, (heap / ServerTickTimer.currentTick()));
        }

        return result;
    }

    public static String[] threadDataStrings()
    {
        return (threadData().toString() + "\r\n" + TextFormatting.YELLOW + "I highly suggest looking at this in your console/log, not in chat.").split("\r\n");
    }

    public static void printASM(String fullClassname) throws Exception
    {
        ASMifier.main(new String[]{fullClassname});
    }


    public static long freeMemory()
    {
        return RUNTIME.freeMemory();
    }

    public static long allocatedMemory()
    {
        return RUNTIME.totalMemory();
    }

    public static long maxMemory()
    {
        return RUNTIME.maxMemory();
    }

    public static long usedMemory()
    {
        return allocatedMemory() - freeMemory();
    }

    public static long cumulativeServerThreadHeapAllocations()
    {
        return THREAD_MX_BEAN.getThreadAllocatedBytes(serverThreadID);
    }


    public static String memData()
    {
        long max = maxMemory(), allocated = allocatedMemory(), used = usedMemory();
        return "MEMORY ... Current: " + used + "/" + allocated + " (~" + (int) ((double) used / allocated * 100) + "%) ... Max: " + allocated + "/" + max + " (~" + (int) ((double) allocated / max * 100) + "%)";
    }


    public static int gcRuns()
    {
        int gcRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) gcRuns += gcBean.getCollectionCount();
        return gcRuns;
    }

    public static long gcTime()
    {
        long gcTime = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) gcTime += gcBean.getCollectionTime();
        return gcTime;
    }


    public static int processID()
    {
        String s = RUNTIME_MX_BEAN.getName();
        return Integer.parseInt(s.substring(0, s.indexOf("@")));
    }


    //"Live" objects are objects that are still reachable / referenced from other objects, ie. won't be garbage collected
    public static boolean dumpHeap(String filename, boolean onlyLiveObjects)
    {
        try
        {
            File file = new File(filename);
            for (int i = 0; i < 100; i++)
            {
                if (file.mkdirs()) break;
                if (i == 99) throw new FileSystemException("Could not create directory: " + filename);
            }
            HOTSPOT_MX_BEAN.dumpHeap(filename + File.separator + "dump.hprof", onlyLiveObjects);
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }


    public static String getClassPath(Class cls)
    {
        return cls.getResource(cls.getSimpleName() + ".class").toString();
    }
}
