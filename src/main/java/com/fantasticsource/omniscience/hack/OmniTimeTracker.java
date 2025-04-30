package com.fantasticsource.omniscience.hack;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.server.timings.TimeTracker;

public class OmniTimeTracker<T> extends TimeTracker<T>
{
    public static final OmniTimeTracker<Entity> ENTITY_TIME_TRACKER = new OmniTimeTracker<>();
    public static final OmniTimeTracker<TileEntity> TILE_ENTITY_TIME_TRACKER = new OmniTimeTracker<>();


    public boolean profile = false;

    @Override
    public void trackStart(T toTrack)
    {
        super.trackStart(toTrack);
        if (profile && Thread.currentThread() == OmniProfiler.INSTANCE.activeThread) OmniProfiler.INSTANCE.startSection(toTrack.getClass().getName());
    }

    @Override
    public void trackEnd(T tracking)
    {
        if (profile && Thread.currentThread() == OmniProfiler.INSTANCE.activeThread) OmniProfiler.INSTANCE.endSection();
        super.trackEnd(tracking);
    }
}
