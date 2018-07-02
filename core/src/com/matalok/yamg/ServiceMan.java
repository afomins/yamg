// -----------------------------------------------------------------------------
package com.matalok.yamg;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

// -----------------------------------------------------------------------------
public class ServiceMan {
    // =========================================================================
    // Service
    public static abstract class Service extends Obj.CommonObject {
        // ---------------------------------------------------------------------
        public Service(int type) {
            super(Obj.SERVICE.ptr, type);
        }

        // ---------------------------------------------------------------------
        protected abstract void AcquireServicePointer();
        protected abstract void ReleaseServicePointer();

        // ---------------------------------------------------------------------
        protected void OnServiceSetup(XmlReader.Element cfg) { };
        protected void OnServiceTeardown() { this.Dispose(); };
        protected void OnServiceRun() { };
        protected void OnServiceTimer() { };
    }

    // -------------------------------------------------------------------------
    private XmlReader.Element cfg;
    private Array<Service> service;
    private long timer_last_update, timer_rate;

    // -------------------------------------------------------------------------
    private static boolean shutdown_in_progress;

    // -------------------------------------------------------------------------
    public ServiceMan(String config_path, int generation) {
        this.service = new Array<Service>();
        this.cfg = CfgReader.Read(config_path);

        // Service timer
        this.timer_last_update = 0;
        this.timer_rate = Utils.Str2Int(CfgReader.GetAttrib(cfg, "service-man:config:timer-rate"));

        // Disable blending
//        Pixmap.setBlending(Pixmap.Blending.None);

        Logger.i("Starting service manager :: [generation=%d]", generation);
    }

    // -------------------------------------------------------------------------
    public void Start(Service s) {
        Logger.d(Logger.MOD_SERV, "Starting service :: [name=%s]", s.GetObjName());

        this.service.add(s);
        s.AcquireServicePointer();
        s.OnServiceSetup(CfgReader.GetChild(this.cfg, s.GetEntryName()));
    }

    // -------------------------------------------------------------------------
    public static void InitiateShutdown() {
        Logger.d(Logger.MOD_SERV, "Initiating shutdown sequence");
        ServiceMan.shutdown_in_progress = true;
    }

    // -------------------------------------------------------------------------
    public void Stop() {
        // Cleanup the mess
        Logger.d(Logger.MOD_SERV, "Starting shutdown");

        // Run teardown in reverse order
        this.service.reverse();
        for(Service s: this.service) {
            s.OnServiceTeardown();
            s.ReleaseServicePointer();
        }

        // Quit
        Utils.Assert(Obj.Group.Test(), "Groups are dirty");
        Logger.d(Logger.MOD_SERV, "Groups are clean");
    }

    // -------------------------------------------------------------------------
    public void Run() {
        // Shutdown
        if(ServiceMan.shutdown_in_progress) {
            Gdx.app.exit();
            return;
        }

        // Serice timer
        boolean is_timer_expired = false;
        if(this.timer_rate > 0 && this.timer_last_update + this.timer_rate < Timer.GetReal()) {
            is_timer_expired = true;
            this.timer_last_update = Timer.GetReal();
        }

        // Run
        for(Service s: this.service) {
            if(is_timer_expired) s.OnServiceTimer();
            s.OnServiceRun(); 
        }
    }
}
