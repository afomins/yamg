/*
 * YAMG - Yet Another Mining Game
 * Copyright (C) 2013 Alex Fomins
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

// -----------------------------------------------------------------------------
package com.matalok.yamg;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.XmlReader;

// -----------------------------------------------------------------------------
public class Timer extends ServiceMan.Service {
    // -------------------------------------------------------------------------
    private long start, cur;
    private float delta;

    // -------------------------------------------------------------------------
    public Timer() {
        super(Obj.SERVICE.TIMER);
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static Timer p;
    protected void AcquireServicePointer() { Timer.p = this; };
    protected void ReleaseServicePointer() { Timer.p = null; };

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        this.start = TimeUtils.millis();
        this.cur = 0;
        this.delta = 0.0f;
    }

    // -------------------------------------------------------------------------
    protected void OnServiceTeardown() { 
        super.OnServiceTeardown();
    }

    // -------------------------------------------------------------------------
    protected void OnServiceRun() { 
        this.cur = Timer.GetReal() - this.start;
        this.delta = Gdx.graphics.getDeltaTime();
    }

    // -------------------------------------------------------------------------
    public static long Get() { return Timer.p.cur; }
    public static long GetReal() { return TimeUtils.millis() - Timer.p.start; }
    public static float GetDelta() { return Timer.p.delta; }
}
