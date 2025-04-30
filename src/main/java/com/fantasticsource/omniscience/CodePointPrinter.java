package com.fantasticsource.omniscience;

import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Pair;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.fantasticsource.omniscience.Omniscience.NAME;

public class CodePointPrinter implements Runnable
{
    public static boolean enabled = false;
    private static final HashMap<Thread, Pair<StackTraceElement, Integer>> currentCodePoints = new HashMap<>();
    private static boolean go = false;
    private static int sameMethodIterationPrintCycle = 1;

    public static void start()
    {
        start(1);
    }

    public static void start(int sameMethodIterationPrintCycle)
    {
        if (!go)
        {
            CodePointPrinter.sameMethodIterationPrintCycle = Tools.max(1, sameMethodIterationPrintCycle);
            go = true;

            System.out.println(TextFormatting.YELLOW + "Starting CodePointPrinter");
            Thread thread1 = new Thread(new CodePointPrinter());
            thread1.setName(NAME + "-CodePointPrinter");
            thread1.setDaemon(true);
            thread1.start();
        }
    }

    public static void stop()
    {
        go = false;
    }

    @Override
    public void run()
    {
        while (go)
        {
            for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
            {
                Thread thread = entry.getKey();
                if (thread == Thread.currentThread()) continue;

                StackTraceElement[] elements = entry.getValue();
                StackTraceElement element = elements.length == 0 ? null : elements[0];
                Pair<StackTraceElement, Integer> data = currentCodePoints.get(thread);

                if (data == null || !allButLineMatch(element, data.getKey()))
                {
                    currentCodePoints.put(thread, new Pair<>(element, 1));
                    System.out.println(thread.getName() + " -> " + element);
                }
                else if (!Objects.equals(element, data.getKey()))
                {
                    int i = data.getValue() + 1;
                    data.setValue(i);
                    if (i % sameMethodIterationPrintCycle == 0)
                    {
                        System.out.println(thread.getName() + " -> " + element + " (" + i + " consecutive calls in this method)");
                    }
                }
            }

//            try
//            {
//                Thread.sleep(1);
//            }
//            catch (InterruptedException e)
//            {
//            }
        }
    }

    public static boolean allButLineMatch(StackTraceElement element1, StackTraceElement element2)
    {
        if (element1 == null) return element2 == null;
        if (element2 == null) return false;
        return element1.getClassName().equals(element2.getClassName()) && Objects.equals(element1.getMethodName(), element2.getMethodName()) && Objects.equals(element1.getFileName(), element2.getFileName());
    }
}
