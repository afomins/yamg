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
public class TweenObject {
    // -------------------------------------------------------------------------
    public static final byte DIR_FORW = 0;
    public static final byte DIR_BACK = 1;

    // -------------------------------------------------------------------------
    private float progress, value, stop_value;
    private long start_time, duration;
    private int direction;
    private boolean is_active;

    // -------------------------------------------------------------------------
    public TweenObject(float value) {
        this.value = value;
        this.is_active = false;
    }

    // -------------------------------------------------------------------------
    public float GetValue() { return this.value; }
    public boolean IsActive() { return this.is_active; }

    // -------------------------------------------------------------------------
    public void Start(float start_value, float stop_value, long duration) {
        // Validate
        Utils.Assert(start_value >= 0.0f && start_value <= 1.0f, 
          "Start value out of range [val=%f]", start_value);
        Utils.Assert(stop_value >= 0.0f && stop_value <= 1.0f, 
          "Stop value out of range [val=%f]", stop_value);
        Utils.Assert(duration >= 0, "Wrong duration [val=%d]", duration);

        // Save initial state
        this.stop_value = stop_value;
        this.duration = duration;
        this.is_active = (duration > 0) ? true : false;
        this.value = (this.is_active) ? start_value : stop_value;

        if(stop_value > start_value) {
            this.direction = DIR_FORW;
            this.start_time = Timer.GetReal() - (long)((float)duration * start_value);
        } else {
            this.direction = DIR_BACK;
            this.start_time = Timer.GetReal() - (long)((float)duration * (1.0f - start_value));
        }
    }

    // -------------------------------------------------------------------------
    public float Continue() {
        // Should not be finished
        if(!this.is_active) return this.value;

        // Get elapsed time
        long time_delta = Timer.GetReal() - this.start_time;
        Utils.Assert(time_delta >= 0, "Wrong time delta [val=%d]", time_delta);

        // Get tweeing progress
        this.progress = (float)time_delta / (float)this.duration;
        if(this.direction == DIR_FORW) {
            this.value = this.progress;
            if(this.value >= this.stop_value) {
                this.value = this.stop_value;
                this.is_active = false;
            }
        } else {
            this.value = 1.0f - this.progress;
            if(this.value <= this.stop_value) {
                this.value = this.stop_value;
                this.is_active = false;
            }
        }

        // Return value
        return this.value;
    }
}
