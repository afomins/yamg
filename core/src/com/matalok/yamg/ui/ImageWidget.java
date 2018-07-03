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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.matalok.yamg.CommonObject;
import com.matalok.yamg.Obj;

// -----------------------------------------------------------------------------
public class ImageWidget extends CommonWidget {
    // -------------------------------------------------------------------------
    private CommonObject.CommonTexture tx;

    // -------------------------------------------------------------------------
    public ImageWidget(CommonObject.CommonTexture tx)  {
        super(Obj.WIDGET.IMAGE);
        this.tx = tx;
        this.SetActor(new Image(this.tx.obj));
        this.size.Set((int)this.actor.getWidth(), (int)this.actor.getHeight());
    }

    // -------------------------------------------------------------------------
    public ImageWidget(String tx_path) {
        this(new CommonObject.CommonTexture(tx_path));
    }

    // -------------------------------------------------------------------------
    public ImageWidget(String tx_path, int width, int height) {
        this(tx_path);
        this.actor.setSize(width, height);
        this.size.Set(width,  height);
    }

    // -------------------------------------------------------------------------
    public void Dispose() { 
        this.tx.Dispose();
        this.tx = null;
        super.Dispose();
    } 

    // -------------------------------------------------------------------------
    public void Scale(float val) {
        this.actor.setSize(this.actor.getWidth() * val, this.actor.getHeight() * val);
        this.size.Set((int)this.actor.getWidth(), (int)this.actor.getHeight());
    }
}
