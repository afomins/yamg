// -----------------------------------------------------------------------------
package com.fomin.yamg;

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
