// -----------------------------------------------------------------------------
package com.fomin.yamg;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.ApplicationListener;
import com.fomin.yamg.game.GameMan;
import com.fomin.yamg.state.StateMan;
import com.fomin.yamg.ui.UiMan;

// -----------------------------------------------------------------------------
public class Main implements ApplicationListener {
    // -------------------------------------------------------------------------
    private ServiceMan sm;
    private int generation;

    // -------------------------------------------------------------------------
    public Main(boolean is_desktop) {
        Utils.is_desktop = is_desktop;
    }

    // -------------------------------------------------------------------------
    @Override
    public void create() {
        this.StartServices();
    }

    // -------------------------------------------------------------------------
    @Override
    public void render() {
        this.RunServices();
    }

    // -------------------------------------------------------------------------
    @Override public void resume() {
        // Restart service manager
        this.StopServices();
        this.StartServices();

        // XXX: Restore game state
    }

    // -------------------------------------------------------------------------
    @Override public void dispose() { 
        this.StopServices();
    }

    // -------------------------------------------------------------------------
    @Override public void pause() { 
        // XXX: Save game state
    }

    // -------------------------------------------------------------------------
    @Override public void resize(int width, int height) {
    }

    // -------------------------------------------------------------------------
    private void StartServices() {
        Utils.Assert(this.sm == null, "Old service not stopped");

        this.sm = new ServiceMan("config.xml", this.generation++);
        this.sm.Start(new Timer());
        this.sm.Start(new Logger());
        this.sm.Start(new TaskMan());
        this.sm.Start(new LevelLoader());
        this.sm.Start(new GameMan());
        this.sm.Start(new UserMan());
        this.sm.Start(new UiMan());
        this.sm.Start(new StateMan());
        this.sm.Start(new MemMan());
    }

    // -------------------------------------------------------------------------
    private void StopServices() {
        if(this.sm != null) {
            this.sm.Stop();
            this.sm = null;
        }
    }

    // -------------------------------------------------------------------------
    private void RunServices() {
        if(this.sm != null) {
            this.sm.Run();
        }
    }
}
