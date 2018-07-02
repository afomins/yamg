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
