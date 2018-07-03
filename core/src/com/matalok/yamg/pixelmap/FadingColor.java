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
package com.matalok.yamg.pixelmap;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.Color;
import com.matalok.yamg.TweenObject;

// -----------------------------------------------------------------------------
public class FadingColor extends TweenObject {
    public Color cur_color, start_color, stop_color;

    // -------------------------------------------------------------------------
    public FadingColor(Color initial_color) {
        super(1.0f);
        this.cur_color = new Color(initial_color);
        this.start_color = new Color();
        this.stop_color = new Color();
    }

    // -------------------------------------------------------------------------
    public void Start(Color dest_color, long duration) {
        this.start_color.set(this.cur_color);
        this.stop_color.set(dest_color);
        this.Start(0.0f, 1.0f, duration);
    }

    // -------------------------------------------------------------------------
    public Color Continue(Color src_color, Color dst_color) {
        // Calculate color
        float value = super.Continue();
        this.cur_color.r = src_color.r + (dst_color.r - src_color.r) * value;
        this.cur_color.g = src_color.g + (dst_color.g - src_color.g) * value;
        this.cur_color.b = src_color.b + (dst_color.b - src_color.b) * value;
        this.cur_color.a = 1.0f;

        // Return new color
        return this.cur_color; 
    }
}
