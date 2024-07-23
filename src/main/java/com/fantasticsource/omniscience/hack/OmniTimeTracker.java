package com.fantasticsource.omniscience.hack;

import net.minecraftforge.server.timings.TimeTracker;

public class OmniTimeTracker<T> extends TimeTracker<T>
{
    @Override
    public void trackStart(T toTrack)
    {
        super.trackStart(toTrack);
        if (Thread.currentThread() == OmniProfiler.INSTANCE.activeThread) OmniProfiler.INSTANCE.startSection(toTrack.getClass().getName());
    }

    @Override
    public void trackEnd(T tracking)
    {
        if (Thread.currentThread() == OmniProfiler.INSTANCE.activeThread) OmniProfiler.INSTANCE.endSection();
        super.trackEnd(tracking);
    }
}
