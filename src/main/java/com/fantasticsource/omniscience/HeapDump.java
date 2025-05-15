package com.fantasticsource.omniscience;

import com.fantasticsource.tools.ReflectionTool;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class HeapDump
{
    private static final Class JAVA_9_IBM_DUMP_CLASS = ReflectionTool.getClassByName("com.ibm.jvm.Dump");
    private static final String DIAGNOSTIC_BEAN = "com.sun.management:type=HotSpotDiagnostic";

    public static void dumpHeap(String filename, boolean live)
    {
        if (JAVA_9_IBM_DUMP_CLASS != null) ReflectionTool.invoke(JAVA_9_IBM_DUMP_CLASS, "heapDumpToFile", null, filename);
        else dumpHotspot(filename, live);
    }

    private static void dumpHotspot(String outputPathString, boolean live)
    {
        try
        {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(DIAGNOSTIC_BEAN);
            HotSpotDiagnosticMXBean proxy = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, HotSpotDiagnosticMXBean.class);
            proxy.dumpHeap(outputPathString, live);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public interface HotSpotDiagnosticMXBean
    {
        void dumpHeap(String outputFile, boolean live) throws IOException;
    }
}