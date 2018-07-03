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
package com.matalok.yamg.game;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.matalok.yamg.CommonTask;
import com.matalok.yamg.Obj;
import com.matalok.yamg.Utils;
import com.matalok.yamg.pixelmap.Surface;
import com.matalok.yamg.ui.UiMan;

// -----------------------------------------------------------------------------
public abstract class Viewer extends Obj.CommonObject 
  implements CommonTask.IMoveTaskObject {
    // =========================================================================
    // Orthographic
    public static class Orthographic extends Viewer {
        // ---------------------------------------------------------------------
        public Orthographic(Surface surface, Vector3 pos, Vector3 dir, Vector3 up, Vector3 rotation_axis) {
            // Construct base viewer
            super(surface, pos, dir, up, rotation_axis);

            // Camera
            this.camera = new OrthographicCamera(this.viewport_size.x, this.viewport_size.y);
            this.InitCamera(pos, dir, up, 10.0f, 1000.0f);
        }

        // ---------------------------------------------------------------------
        public void ValidateSurfacePos() {
            // Make sure that there is no gap between left border of the surface 
            // and client window
            Utils.Vector2i size = this.surface.GetVisibleSize();
            if(this.rel_pos.x > size.x) {
                this.Move(-size.x, 0.0f, 0.0f, true);
            } else if(this.rel_pos.x < 0) {
                this.Move(size.x, 0.0f, 0.0f, true);
            }

            // Calculate necessary copy count
            int occupied = (int)(this.surface.GetVisibleSize().x - this.rel_pos.x);
            int free = this.viewport_size.x - occupied;
            this.surface_copy_cnt = free / this.surface.GetVisibleSize().x + 1;
        }

        // ---------------------------------------------------------------------
        public Utils.Vector2i GetSelectedPixel(float x, float y, Utils.Vector2i dest) {
            dest.x = (int)((x + this.rel_pos.x) / this.zoom_factor) % this.surface.GetSize().x;
            dest.y = (int)((y + this.rel_pos.y) / this.zoom_factor);
            return dest;
        }
    }
/*
    // =========================================================================
    // Perspective
    public static class Perspective extends Viewer {
        // ---------------------------------------------------------------------
        private float fov, pixels_per_degree;

        // ---------------------------------------------------------------------
        public Perspective(Vector3 pos, Vector3 dir, Vector3 up, Surface surface, 
          float fov, Vector3 rotation_axis) {
            // Construct base viewer
            super(surface, pos, dir, up, rotation_axis);
            Utils.Assert(false, "eat-my-shorts");
        }

        // ---------------------------------------------------------------------
        public void ValidateSurfacePos() {
            Utils.Assert(false, "eat-my-shorts");
        }

        // ---------------------------------------------------------------------
        public Utils.Vector2i GetSelectedPixel(float x, float y, Utils.Vector2i dest) {
            Utils.Assert(false, "eat-my-shorts");
            return null;
        }
    }
*/
    // =========================================================================
    // Viewer
    protected Camera camera;
    protected Surface surface;

    protected Matrix4 vp_matrix; // view-projection
    protected Utils.Vector2i viewport_size;
    protected Utils.Vector2f posf;
    protected Vector3 init_pos, init_dir, init_up;
    protected Vector3 pos, dir, up, rel_pos;
    protected float zoom_factor, zoom_factor_min, zoom_factor_max;
    protected boolean is_updated;
    protected int surface_copy_cnt;

    // -------------------------------------------------------------------------
    public Viewer(Surface surface, Vector3 pos, Vector3 dir, Vector3 up, Vector3 rotation_axis) {
        super(Obj.MISC.ptr, Obj.MISC.VIEWER);

        // Viewport
        this.viewport_size = new Utils.Vector2i(UiMan.p.screen_size.x, UiMan.p.screen_size.y);

        // Camera matrix 
        this.zoom_factor = 1.0f;
        this.zoom_factor_min = 1.0f;
        this.zoom_factor_max = 8.0f;
        this.vp_matrix = new Matrix4();
        this.posf = new Utils.Vector2f();

        Gdx.gl20.glViewport(0, 0, this.viewport_size.x, this.viewport_size.y);

        // Viewer attributes
        this.surface = surface;

        // GL context
        Gdx.gl.glClearDepthf(1.0f);
        Gdx.gl.glFrontFace(GL20.GL_CW);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
    }

    // -------------------------------------------------------------------------
    public int GetWidth() { return this.viewport_size.x; }
    public int GetHeight() { return this.viewport_size.y; }
    public float GetZoom() { return this.zoom_factor; }
    public int GetCloneCnt() { return this.surface_copy_cnt; }
    public Vector3 GetPos() { return this.pos; }
    public Vector3 GetInitPos() { return this.init_pos; }
    public Vector3 GetRelPos() { return this.rel_pos; }
    public Matrix4 GetMatrixVP() { return this.vp_matrix; }

    // -------------------------------------------------------------------------
    protected void InitCamera(Vector3 pos, Vector3 dir, Vector3 up, float near, float far) {
        // Camera vectors
        this.rel_pos = new Vector3();
        this.pos = this.camera.position;    this.init_pos = pos;    this.pos.set(pos);
        this.dir = this.camera.direction;   this.init_dir = dir;    this.dir.set(dir);
        this.up = this.camera.up;           this.init_up = up;      this.up.set(up);

        // Near/far clipping frames
        this.camera.near = near;
        this.camera.far = far;
        this.is_updated = true;
    }

    // -------------------------------------------------------------------------
    public void Apply(Color back_color) {
        // Update clear color
        if(back_color != null) {
            Gdx.gl.glClearColor(back_color.r * 0.7f, back_color.g * 0.7f, back_color.b * 0.7f, 1.0f);
        }

        // Clear GL buffers
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera
        if(this.is_updated) {
            // Build combined camera
            this.camera.update();

            // Apply zoom factor
            this.vp_matrix.set(this.camera.combined);
            if(this.zoom_factor != 1.0f) {
                this.vp_matrix.scale(this.zoom_factor, this.zoom_factor, 1.0f);
            }

            // Update is done
            this.is_updated = false;
        }
    }

    // -------------------------------------------------------------------------
    public void Move(float x, float y, float z, boolean do_validation) {
        this.pos.add(x, y, z);
        this.RefreshPosition(do_validation);
    }

    // -------------------------------------------------------------------------
    public void Set(float x, float y, float z, boolean do_validation) {
        this.pos.set(x, y, z);
        this.RefreshPosition(do_validation);
    }

    // -------------------------------------------------------------------------
    private void RefreshPosition(boolean do_validation) {
        this.rel_pos.set(this.pos).sub(this.init_pos);
        this.is_updated = true;

        if(do_validation) {
            this.ValidateSurfacePos();
        }
    }

    // -------------------------------------------------------------------------
    public void ZoomSet(float val) {
        this.zoom_factor = val;

        if(this.zoom_factor < this.zoom_factor_min) this.zoom_factor = this.zoom_factor_min;
        if(this.zoom_factor > this.zoom_factor_max) this.zoom_factor = this.zoom_factor_max;
        this.is_updated = true;

        this.surface.UpdateVisibleSize(this.zoom_factor);
        this.ValidateSurfacePos();
    }

    // -------------------------------------------------------------------------
    public Utils.Vector2i GetSelectedPixel(float x, float y) {
        return this.GetSelectedPixel(x, y, Utils.v2i_tmp);
    }

    // ---------------------------------------------------------------------
    public Utils.Vector2f GetMovePos() {
        this.posf.Set(this.pos.x, this.pos.y);
        return this.posf; 
    }

    // ---------------------------------------------------------------------
    public void SetMoveRelPos(float x, float y) {
        this.Move(x, y, 0.0f, true);
    }

    // ---------------------------------------------------------------------
    public void SetMoveAbsPos(float x, float y) {
        this.Set(x, y, 0.0f, true);
    }

    // -------------------------------------------------------------------------
    public abstract void ValidateSurfacePos();
    public abstract Utils.Vector2i GetSelectedPixel(float x, float y, Utils.Vector2i dest);
}
