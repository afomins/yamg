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

// -----------------------------------------------------------------------------
import com.badlogic.gdx.ApplicationListener;
import com.matalok.yamg.game.GameMan;
import com.matalok.yamg.state.StateMan;
import com.matalok.yamg.ui.UiMan;

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
