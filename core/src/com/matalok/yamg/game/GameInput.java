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
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;
import com.matalok.yamg.Map2d;
import com.matalok.yamg.Utils;
import com.matalok.yamg.fallingsand.FallingSand;
import com.matalok.yamg.fallingsand.Material;

// -----------------------------------------------------------------------------
public class GameInput {
    // =========================================================================
    // GestureListenerEx
    public static class GestureListenerEx implements GestureListener {
        // ---------------------------------------------------------------------
        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
            GameMan.p.StartMouseMovement();
            return true;
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean tap(float x, float y, int count, int button) {
            if(count >= 2) {
                Map2d<Float> pattern = GameMan.p.explosion_mesh;
                Utils.Vector2i mouse_pos = GameMan.p.viewer.GetSelectedPixel(x, y);
                int pixel_x = mouse_pos.x - pattern.size.x / 2;
                int pixel_y = mouse_pos.y - pattern.size.y / 2;
                GameMan.p.fs.UpdateBlockMaterial(pixel_x, pixel_y, pattern, 
                  FallingSand.OP_CUT, Material.T_ROCK);
                GameMan.p.fs.UpdateBlockMaterial(pixel_x, pixel_y, pattern, 
                  FallingSand.OP_CUT, Material.T_SAND);
                return true;
            }
            return false;
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean longPress(float x, float y) {
            return false;
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean fling(float velocityX, float velocityY, int button) {
            GameMan.p.StopMouseMovement(velocityX, velocityY);
            return true;
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            return false;
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean panStop(float x, float y, int pointer, int button) { return false; }

        // ---------------------------------------------------------------------
        @Override
        public boolean zoom(float originalDistance, float currentDistance) {
            GameMan.p.UpdateZoom(
              originalDistance, currentDistance, GameMan.p.viewer.GetZoom());
            return true;
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean pinch (Vector2 initialFirstPointer, Vector2 initialSecondPointer, 
          Vector2 firstPointer, Vector2 secondPointer) {
           return false;
        }

        // ---------------------------------------------------------------------
        @Override public void pinchStop() {
        }
    }

    // =========================================================================
    // GestureDetectorEx
    public static class GestureDetectorEx extends GestureDetector {
        // ---------------------------------------------------------------------
        public GestureDetectorEx(GestureListenerEx listener) {
            super(listener);
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean touchUp(int x, int y, int pointer, int button) {
            // Stop movement
            if(button == Buttons.LEFT) {
                GameMan.p.StopMouseMovement(0.0f, 0.0f);
            }

            // Call parent method
            return super.touchUp(x, y, pointer, button);
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean scrolled(int amount) {
            float zoom_step = 5.0f;
            float zoom_val = GameMan.p.viewer.GetZoom();
            float cur_zoom_val = (zoom_val + amount / zoom_step) / zoom_val; 
            GameMan.p.UpdateZoom(1.0f, cur_zoom_val, zoom_val);
            return true;
        }

        // ---------------------------------------------------------------------
        @Override
        public boolean keyDown(int keycode) {
            return false;
        }
    }
}
