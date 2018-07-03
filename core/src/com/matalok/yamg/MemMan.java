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
import com.badlogic.gdx.utils.XmlReader;
import com.matalok.yamg.ui.UiMan;

// -----------------------------------------------------------------------------
public class MemMan extends ServiceMan.Service {
    // -------------------------------------------------------------------------
    public long free, max, total, used;
    public float freef, maxf, totalf, usedf;
    private boolean do_logging;

    // -------------------------------------------------------------------------
    public MemMan() {
        super(Obj.SERVICE.MEM_MAN);
    }

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        this.do_logging = Utils.Str2Bool(CfgReader.GetAttrib(cfg, "config:logging"));
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static MemMan p;
    protected void AcquireServicePointer() { MemMan.p = this; };
    protected void ReleaseServicePointer() { MemMan.p = null; };

    // -------------------------------------------------------------------------
    protected void OnServiceTimer() {
        // Memory counters
        float M1 = 1000000.0f;
        this.free = Runtime.getRuntime().freeMemory(); this.freef = this.free / M1;
        this.max = Runtime.getRuntime().maxMemory();  this.maxf = this.max / M1;
        this.total = Runtime.getRuntime().totalMemory();  this.totalf = this.total / M1;
        this.used = this.total - this.free;  this.usedf = this.totalf - this.freef;

        // Log
        if(this.do_logging) {
            Logger.d(Logger.MOD_MEM, 
              "Mem-stats : [max=%.1fM] [total=%.1fM] [used=%.1fM] [free=%.1fM]", 
              this.maxf, this.totalf, this.usedf, this.freef);
        }

        // Ui
        UiMan.p.w_label.get(Obj.UI.MEM_STATS_LABEL).SetText(
          String.format("Mem-stats: %.1fM/%.1fM/%.1fM/%.1fM", 
            this.freef, this.usedf, this.totalf, this.maxf));
    }
}
