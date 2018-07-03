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

//-----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.matalok.yamg.Obj;

// -----------------------------------------------------------------------------
public class LabelWidget extends CommonWidget {
    // -------------------------------------------------------------------------
    public Label label;

    // -------------------------------------------------------------------------
    public LabelWidget(Color color, String text) {
        // Button widget constructor
        super(Obj.WIDGET.LABEL, CommonWidget.INPUT_CLICK);

        // Set layer
        this.SetActor(this.label = 
          new Label(text, new Label.LabelStyle(new BitmapFont(), color)));
        this.label.setAlignment(Align.left, Align.center);
        this.SetText(text);
    }

    // -------------------------------------------------------------------------
    public void SetText(String text) { 
        this.label.setText(text);
        this.size.Set((int)this.actor.getWidth(), (int)this.actor.getHeight());
    }
}
