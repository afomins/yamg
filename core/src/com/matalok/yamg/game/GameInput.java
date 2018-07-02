// -----------------------------------------------------------------------------
package com.fomin.yamg.game;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;
import com.fomin.yamg.Map2d;
import com.fomin.yamg.Utils;
import com.fomin.yamg.fallingsand.FallingSand;
import com.fomin.yamg.fallingsand.Material;

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
