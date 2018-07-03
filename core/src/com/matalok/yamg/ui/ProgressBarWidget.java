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
package com.matalok.yamg.ui;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.matalok.yamg.CommonObject;
import com.matalok.yamg.Logger;
import com.matalok.yamg.Obj;

// -----------------------------------------------------------------------------
public class ProgressBarWidget extends GridWidget {
    // -------------------------------------------------------------------------
    private CommonObject.CommonTexture tx;
    private TextureRegion tx_off, tx_on;
    private float progress;
    private boolean is_forw_dir;

    // -------------------------------------------------------------------------
    public ProgressBarWidget(CommonObject.CommonTexture tx, int x_cnt, int y_cnt, 
      int cell_width, int cell_height, boolean is_forw_dir) {
        super(Obj.WIDGET.PROGRESS_BAR, x_cnt, y_cnt, 0, cell_width, cell_height);

        // Read texture regions
        this.tx = tx;
        this.tx_off = new TextureRegion(this.tx.obj, 0.0f, 0.0f, 0.5f, 1.0f);
        this.tx_on = new TextureRegion(this.tx.obj, 0.5f, 0.0f, 1.0f, 1.0f);
        this.is_forw_dir = is_forw_dir;

        // Reset
        this.ResetProgress();
    }

    // -------------------------------------------------------------------------
    public ProgressBarWidget(CommonObject.CommonTexture tx, int x_cnt, int y_cnt,
      boolean is_forw_dir) {
        this(tx, x_cnt, y_cnt, tx.obj.getWidth() / 2, tx.obj.getHeight(), is_forw_dir);
    }

    // -------------------------------------------------------------------------
    public void SetProgress(float progress) {
        Logger.d(Logger.MOD_UI, 
          "Updating progress-bar :: [name=%s] [old-val=%.2f] [new-val=%.2f]", 
          this.GetObjName(), this.progress, progress);

        // Test progress boundaries
        if(progress > 1.0f) progress = 1.0f;
        else if(progress < 0.0f) progress = 0.0f;

        // Set progress
        this.SetProgress((int)(progress * this.GetGridSize().x * this.GetGridSize().y));
        this.progress = progress;
    }

    // -------------------------------------------------------------------------
    public void SetProgress(int progress) {
        // Total block count
        int cnt = this.GetGridSize().x * this.GetGridSize().y;

        // True
        int start = 0, stop = progress;
        boolean cell_value = true;

        // Run 2 progress iteration(true & false)
        for(int i = 0; i < 2; i++) {
            for(int j = start; j < stop; j++) {
                int idx = (this.is_forw_dir) ? j : cnt - j - 1;
                this.SetCell(
                  idx % this.GetGridSize().x, 
                  idx / this.GetGridSize().x, cell_value);
            }

            // False
            start = progress; stop = cnt;
            cell_value = false;
        }
    }

    // -------------------------------------------------------------------------
    public void ResetProgress() {
        this.progress = 0.0f;
        for(int y = 0; y < this.GetGridSize().y; y++) {
            for(int x = 0; x < this.GetGridSize().x; x++) {
                this.SetCell(x, y, false);
            }
        }
    }

    // -------------------------------------------------------------------------
    private void SetCell(int x, int y, boolean is_on) {
        this.SetWidget(x, y, new Image((is_on) ? this.tx_on : this.tx_off));
    }
}
