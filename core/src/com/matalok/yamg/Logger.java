// -----------------------------------------------------------------------------
package com.matalok.yamg;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Version;
import com.badlogic.gdx.utils.XmlReader;

// -----------------------------------------------------------------------------
public class Logger extends ServiceMan.Service {
    // -------------------------------------------------------------------------
    public static final int LOG_NON = Application.LOG_NONE;
    public static final int LOG_ERR = Application.LOG_ERROR;
    public static final int LOG_INF = Application.LOG_INFO;
    public static final int LOG_DBG = Application.LOG_DEBUG;
    public static final Obj.Group LOG_GROUP = new Obj.Group("log-level",
      new String [] {"none", "error", "info", "debug"});

    public static final int MOD_NONE = 0;
    public static final int MOD_CFG = 1;
    public static final int MOD_GAME = 2;
    public static final int MOD_LVL = 3;
    public static final int MOD_MISC = 4;
    public static final int MOD_SERV = 5;
    public static final int MOD_SH = 6;
    public static final int MOD_TASK = 7;
    public static final int MOD_ST = 8;
    public static final int MOD_UI = 9;
    public static final int MOD_OBJ = 10;
    public static final int MOD_MEM = 11;
    public static final Obj.Group MOD_GROUP = new Obj.Group("log-module",
      new String [] {"    ", "cfg ", "game", "lvl ", "misc", "serv", "sh  ", 
        "task", "st  ", "ui  ", "obj ", "mem "});

    // -------------------------------------------------------------------------
    private int level;
    private String tag;
    private boolean is_clean;

    // -------------------------------------------------------------------------
    public Logger() {
        super(Obj.SERVICE.LOGGER);
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static Logger p;
    protected void AcquireServicePointer() { Logger.p = this; };
    protected void ReleaseServicePointer() { Logger.p = null; };

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        // Logger tag
        this.tag = CfgReader.GetAttrib(cfg, "config:tag");

        // Log level
        String str = CfgReader.GetAttrib(cfg, "config:log-level");
        this.SelLogLevel(Logger.LOG_GROUP.GetEntryIdx(str, true));

        // Welcome...
        Logger.d(Logger.MOD_MISC, "YAMG :: [version=%s] [libgdx=%s]", 
          Utils.version, Version.VERSION);
    }

    // -------------------------------------------------------------------------
    protected void OnServiceRun() {
        if(!this.is_clean) {
            Logger.d(Logger.MOD_NONE, "");
            this.is_clean = true;
        }
    }

    // -------------------------------------------------------------------------
    public void Log(int mod, int level, String msg) {
        this.is_clean = false;
        long time = Timer.GetReal(), prefix = time / 1000, suffix = time % 1000;

        msg = String.format("%07d:%03d [%s] - %s", 
          prefix, suffix, Logger.MOD_GROUP.GetEntryName(mod, true), msg);

             if(level == LOG_DBG) Gdx.app.debug(this.tag, msg);
        else if(level == LOG_ERR) Gdx.app.error(this.tag, msg); 
        else if(level == LOG_INF) Gdx.app.log(this.tag, msg);
        else Utils.Assert(false, "Unknows log level");
    }

    // -------------------------------------------------------------------------
    public void Log(int mod, int level, String fmt, Object... args) {
        if(this.level < level) return;
        this.Log(mod, level, String.format(fmt, args));
    }

    // -------------------------------------------------------------------------
    public void SelLogLevel(int level) {
        this.level = level;
        Gdx.app.setLogLevel(level);
    }

    // -------------------------------------------------------------------------
    public static void LogSafe(int mod, int level, String fmt, Object... args) {
        if(Logger.p == null) {
            Gdx.app.log("log-safe", String.format(fmt, args));
        } else {
            Logger.p.Log(mod, level, fmt, args);
        }
    }

    // -------------------------------------------------------------------------
    public static void d(int mod, String fmt, Object... args) { Logger.LogSafe(mod, Logger.LOG_DBG, fmt, args); }
    public static void i(int mod, String fmt, Object... args) { Logger.LogSafe(mod, Logger.LOG_INF, fmt, args); }
    public static void e(int mod, String fmt, Object... args) { Logger.LogSafe(mod, Logger.LOG_ERR, fmt, args); }

    public static void d(String fmt, Object... args) { Logger.LogSafe(Logger.MOD_MISC, Logger.LOG_DBG, fmt, args); }
    public static void i(String fmt, Object... args) { Logger.LogSafe(Logger.MOD_MISC, Logger.LOG_INF, fmt, args); }
    public static void e(String fmt, Object... args) { Logger.LogSafe(Logger.MOD_MISC, Logger.LOG_ERR, fmt, args); }
}
