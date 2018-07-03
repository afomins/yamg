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
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

// -----------------------------------------------------------------------------
public class CommonObject {
    // =========================================================================
    // CommonPixmap
    public static class CommonPixmap extends Obj.CommonObject {
        // ---------------------------------------------------------------------
        public Pixmap obj;

        // ---------------------------------------------------------------------
        public CommonPixmap(int width, int height, Pixmap.Format fmt) {
            super(Obj.MISC.ptr, Obj.MISC.PIXMAP);
            this.obj = new Pixmap(width, height, fmt);
            this.obj.setBlending(Pixmap.Blending.None);
        }

        // ---------------------------------------------------------------------
        public CommonPixmap(String path) {
            super(Obj.MISC.ptr, Obj.MISC.PIXMAP);
            this.Read(path);
        }

        // ---------------------------------------------------------------------
        public void Multiply(float r, float g, float b, float a) {
            for(int y = 0; y < this.obj.getHeight(); y++) {
                for(int x = 0; x < this.obj.getWidth(); x++) {
                    int pixel_color = this.obj.getPixel(x, y);

                    float val_a = ((pixel_color >> 0) & 0xff) * a;
                    float val_b = ((pixel_color >> 8) & 0xff) * b;
                    float val_g = ((pixel_color >> 16) & 0xff) * g;
                    float val_r = ((pixel_color >> 24) & 0xff) * r;

                    this.obj.setColor(val_r / 255.0f, val_g / 255.0f, 
                      val_b / 255.0f, val_a / 255.0f);
                    this.obj.drawPixel(x, y);
                }
            }
        }

        // ---------------------------------------------------------------------
        public CommonPixmap Read(String path) {
            if(this.obj != null) this.obj.dispose();
            this.obj = new Pixmap(Gdx.files.internal(path));
            this.obj.setBlending(Pixmap.Blending.None);
            return this;
        }

        // ---------------------------------------------------------------------
        public void Dispose() {
            this.obj.dispose();
            this.obj = null;
            super.Dispose();
        }
    }

    // =========================================================================
    // CommonTexture
    public static class CommonTexture extends Obj.CommonObject {
        // ---------------------------------------------------------------------
        public Texture obj;

        // ---------------------------------------------------------------------
        public CommonTexture(String path) {
            super(Obj.MISC.ptr, Obj.MISC.TEXTURE);
            this.obj = new Texture(Gdx.files.internal(path));
        }

        // ---------------------------------------------------------------------
        public CommonTexture(Pixmap pm) {
            super(Obj.MISC.ptr, Obj.MISC.TEXTURE);
            this.obj = new Texture(pm, false);
        }

        // ---------------------------------------------------------------------
        public CommonTexture(int width, int height, Color c, Pixmap.Format fmt) {
            super(Obj.MISC.ptr, Obj.MISC.TEXTURE);

            Pixmap pm = new Pixmap(width, height, fmt);
            pm.setBlending(Pixmap.Blending.None);
            pm.setColor(c); pm.fill();
            this.obj = new Texture(pm, false);
            pm.dispose();
        }

        // ---------------------------------------------------------------------
        public void Dispose() {
            this.obj.dispose();
            this.obj = null;
            super.Dispose();
        }
    }

    // =========================================================================
    // CommonFbo
    public static class CommonFbo extends Obj.CommonObject {
        // ---------------------------------------------------------------------
        public FrameBuffer obj;

        // ---------------------------------------------------------------------
        public CommonFbo(int width, int height, Pixmap.Format fmt) {
            super(Obj.MISC.ptr, Obj.MISC.FRAME_BUFFER);
            this.obj = new FrameBuffer(fmt, width, height, false);
        }

        // ---------------------------------------------------------------------
        public void Dispose() {
            this.obj.dispose();
            this.obj = null;
            super.Dispose();
        }
    }

    // =========================================================================
    // CommonMesh
    public static class CommonMesh extends Obj.CommonObject {
        // ---------------------------------------------------------------------
        public Mesh obj;

        // ---------------------------------------------------------------------
        public CommonMesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttribute... attributes) {
            super(Obj.MISC.ptr, Obj.MISC.MESH);
            this.obj = new Mesh(isStatic, maxVertices, maxIndices, attributes);
        }

        // ---------------------------------------------------------------------
        public void Dispose() {
            this.obj.dispose();
            this.obj = null;
            super.Dispose();
        }
    }
}
