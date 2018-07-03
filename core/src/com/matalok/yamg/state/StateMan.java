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
package com.matalok.yamg.state;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.matalok.yamg.CfgReader;
import com.matalok.yamg.Logger;
import com.matalok.yamg.Obj;
import com.matalok.yamg.ServiceMan;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.Utils;
import com.matalok.yamg.TaskMan.Task;
import com.matalok.yamg.game.GameMan;
import com.matalok.yamg.ui.CommonWidget;
import com.matalok.yamg.ui.UiMan;

// -----------------------------------------------------------------------------
public class StateMan extends ServiceMan.Service {
    // =========================================================================
    // State
    public static abstract class State extends Obj.CommonObject 
      implements TaskMan.ITaskListener, InputProcessor, UiMan.IUiListener {
        // ---------------------------------------------------------------------
        protected InputProcessor [] ip_list;
        protected int phase, rc;
        protected boolean is_activated;

        // ---------------------------------------------------------------------
        public State(int type, InputProcessor [] ip_list) {
            super(Obj.STATE.ptr, type);
            this.phase = StateMan.PH_IDLE;
            this.ip_list = (ip_list == null) ? new InputProcessor [] {} : ip_list;
        }

        // ---------------------------------------------------------------------
        protected void SetPhase(int phase, int rc) {
            this.phase = phase;
            this.rc = rc;
        }

        // ---------------------------------------------------------------------
        protected void SetPhase(int phase) {
            this.SetPhase(phase, Utils.ID_UNDEFINED);
        }

        // ---------------------------------------------------------------------
        protected void OnStateActivate( ) { };
        protected void OnStateDeactivate( ) { };

        // ---------------------------------------------------------------------
        // ITaskListener
        public void OnTaskSetup(Task t) { }
        public void OnTaskRun(Task t) { }
        public void OnTaskTeardown(Task t) { }

        // ---------------------------------------------------------------------
        private boolean HandleInput(int type, int keycode, char character, 
          int x, int y, int pointer, int button, int amount) {
            boolean was_processed = false;

            for(int i = 0; i < this.ip_list.length; i++) {
                InputProcessor ip = this.ip_list[i];
                     if(type == UiMan.INPUT_KEY_DOWN) was_processed = ip.keyDown(keycode);
                else if(type == UiMan.INPUT_KEY_UP) was_processed = ip.keyUp(keycode);
                else if(type == UiMan.INPUT_KEY_TYPED) was_processed = ip.keyTyped(character);
                else if(type == UiMan.INPUT_TOUCH_DOWN) was_processed = ip.touchDown(x, y, pointer, button);
                else if(type == UiMan.INPUT_TOUCH_UP) was_processed = ip.touchUp(x, y, pointer, button);
                else if(type == UiMan.INPUT_TOUCH_DRAGGED) was_processed = ip.touchDragged(x, y, pointer);
                else if(type == UiMan.INPUT_MOUSE_MOVE) was_processed = ip.mouseMoved(x, y);
                else if(type == UiMan.INPUT_SCROLLED) was_processed = ip.scrolled(amount);
                else Utils.Assert(false, "Invalid input type :: [type=%d]", type);

                // Input event was processed 
                if(was_processed) break;
            }
            return was_processed;
        }

        // ---------------------------------------------------------------------
        // Input handlers towards InputProcessor
        public boolean keyDown(int keycode) { return this.HandleInput(UiMan.INPUT_KEY_DOWN, keycode, '@', 0, 0, 0, 0, 0); }
        public boolean keyUp(int keycode) { return this.HandleInput(UiMan.INPUT_KEY_UP, keycode, '@', 0, 0, 0, 0, 0); }
        public boolean keyTyped(char character) { return this.HandleInput(UiMan.INPUT_KEY_TYPED, 0, character, 0, 0, 0, 0, 0); }
        public boolean touchDown(int screenX, int screenY, int pointer, int button) { return this.HandleInput(UiMan.INPUT_TOUCH_DOWN, 0, '@', screenX, screenY, pointer, button, 0); }
        public boolean touchUp(int screenX, int screenY, int pointer, int button) { return this.HandleInput(UiMan.INPUT_TOUCH_UP, 0, '@', screenX, screenY, pointer, button, 0); }
        public boolean touchDragged(int screenX, int screenY, int pointer) { return this.HandleInput(UiMan.INPUT_TOUCH_DRAGGED, 0, '@', screenX, screenY, pointer, 0, 0); }
        public boolean mouseMoved(int screenX, int screenY) { return this.HandleInput(UiMan.INPUT_MOUSE_MOVE, 0, '@', screenX, screenY, 0, 0, 0); }
        public boolean scrolled(int amount) { return this.HandleInput(UiMan.INPUT_SCROLLED, 0, '@', 0, 0, 0, 0, amount); }

        // ---------------------------------------------------------------------
        // Input handlers from UiManager
        public void OnUiClick(CommonWidget w, float x, float y) { };
        public void OnUiTouchDown(CommonWidget w, float x, float y, int pointer, int button) { };
        public void OnUiTouchUp(CommonWidget w, float x, float y, int pointer, int button) { };
        public void OnUiTouchDragged(CommonWidget w, float x, float y, int pointer) { };
        public void OnUiMouseMoved(CommonWidget w, float x, float y) { };
        public void OnUiEnter(CommonWidget w, float x, float y, int pointer) { };
        public void OnUiExit(CommonWidget w, float x, float y, int pointer) { };
        public void OnUiScrolled(CommonWidget w, float x, float y, int amount) { };
        public void OnUiKeyDown(CommonWidget w, int keycode) { };
        public void OnUiKeyUp(CommonWidget w, int keycode) { };
        public void OnUiKeyTyped(CommonWidget w, char character) { };
    }

    // =========================================================================
    // StateMan
    public static final int PH_IDLE = 0;
    public static final int PH_RUN = 1;
    public static final int PH_PUSH = 2;
    public static final int PH_POP = 3;
    public static final int PH_POP_PUSH = 4;
    public static final int PH_PAUSE = 5;
    public static final Obj.Group PH_GROUP = new Obj.Group("state-phase", 
      new String [] {"idle", "run", "push", "pop", "pop-push", "pause"});

    // -------------------------------------------------------------------------
    private State[] state;
    private Array<State> stack;
    protected UiHandler ui_handler;

    // -------------------------------------------------------------------------
    public StateMan() {
        super(Obj.SERVICE.STATE);
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static StateMan p;
    protected void AcquireServicePointer() { StateMan.p = this; };
    protected void ReleaseServicePointer() { StateMan.p = null; };

    // -------------------------------------------------------------------------
    protected State GetTop() {
        Utils.Assert(this.stack.size > 0, "Stack is empty");
        return this.stack.get(this.stack.size - 1);
    }

    // -------------------------------------------------------------------------
    protected State GetPrev() {
        Utils.Assert(this.stack.size > 1, "Stack is empty");
        return this.stack.get(this.stack.size - 2);
    }

    // -------------------------------------------------------------------------
    public boolean IsInStack(int type) {
        return !(this.state[type].phase == StateMan.PH_IDLE);
    }

    // -------------------------------------------------------------------------
    private void Pop(int target_type) {
        if(target_type == Utils.ID_UNDEFINED) {
            target_type = this.GetPrev().GetEntryIdx();
        }

        Logger.d(Logger.MOD_ST, "Pop state :: [new-top=%s]:", 
          Obj.STATE.ptr.GetEntryName(target_type, true));

        for(;;) {
            State s = this.GetTop();
            if(s.GetEntryIdx() != target_type) {
                Logger.d(Logger.MOD_ST, "  remove from top :: [name=%s] [phase=%s]", 
                  s.GetEntryName(), StateMan.PH_GROUP.GetEntryName(s.phase, true));

                this.stack.removeIndex(this.stack.size - 1);
                s.SetPhase(StateMan.PH_IDLE);
                s.OnStateDeactivate();

            } else {
                Logger.d(Logger.MOD_ST, "  new-top :: [name=%s] [phase=%s]", 
                  s.GetEntryName(), StateMan.PH_GROUP.GetEntryName(s.phase, true));
                Utils.Assert(s.phase == StateMan.PH_PAUSE, "Popped state should be paused");
                s.SetPhase(StateMan.PH_RUN);
                s.is_activated = false;
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    private State Push(int type) {
        // Log
        State s = this.state[type];
        Logger.d(Logger.MOD_ST, "Push state :: [name=%s] [phase=%s]", 
          Obj.STATE.ptr.GetEntryName(type, true),
          StateMan.PH_GROUP.GetEntryName(s.phase, true));

        // State should be idle or paused
        Utils.Assert(
          s.phase == StateMan.PH_IDLE || s.phase == StateMan.PH_PAUSE, 
          "State not idle :: [state=%s] [phase=%s]",
          s.GetEntryName(), StateMan.PH_GROUP.GetEntryName(s.phase, false));

        // Pause previous state
        if(this.stack.size > 0) {
            State prev_state = this.GetTop();
            prev_state.SetPhase(StateMan.PH_PAUSE);
            prev_state.OnStateDeactivate();
        }

        // Prepare new state for running
        this.stack.add(s);
        s.SetPhase(StateMan.PH_RUN);
        s.is_activated = false;
        return s;
    }

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        // Ui handler
        this.ui_handler = new UiHandler();

        // State
        this.state = new State[] {
          // Shutdown
          new ShutdownState(),

          // Game
          new GameState(
            new InputProcessor [] {UiMan.p.GetIp(), GameMan.p.GetIp(), this.ui_handler}),

          // Game-tnt
          new GameTntState(
            new InputProcessor [] {UiMan.p.GetIp(), GameMan.p.GetIp(), this.ui_handler}),

          // LoadLevel
          new LoadLevelState(
            Utils.Str2Int(CfgReader.GetAttrib(cfg, "config:splash-screen-duration"))),

          // Main menu - select level
          new MainMenuSelectLevelState(
            new InputProcessor [] {UiMan.p.GetIp(), this.ui_handler}),

          // Main menu - info
          new MainMenuInfoState(
            new InputProcessor [] {UiMan.p.GetIp(), this.ui_handler}),

          // Main menu - confirm shutdown
          new MainMenuShutdownState(
            new InputProcessor [] {UiMan.p.GetIp(), this.ui_handler}),

          // Main menu - confirm reset
          new MainMenuResetState(
            new InputProcessor [] {UiMan.p.GetIp(), this.ui_handler})
        };

        // Initial stack
        this.stack = new Array<State>();
        this.Push(Obj.STATE.GAME);
        this.Push(Obj.STATE.MAIN_MENU_INFO);
        this.Push(Obj.STATE.LOAD_LEVEL);
    }

    // -------------------------------------------------------------------------
    protected void OnServiceTeardown() {
        // Dispose states
        for(State s: this.state) {
            s.Dispose();
        }
        super.OnServiceTeardown();

        // Unregister input
        Gdx.input.setInputProcessor(null);
        Gdx.input.setCatchBackKey(true);
    }

    // -------------------------------------------------------------------------
    protected void OnServiceRun() {
        // Get top state
        State s = this.GetTop();

        // Handle running state
        if(s.phase == StateMan.PH_RUN) {
            // Activate state only once
            if(!s.is_activated) {
                s.is_activated = true;
                s.OnStateActivate();
                Gdx.input.setInputProcessor(s);
                Gdx.input.setCatchBackKey(true);
                UiMan.p.SetListener(s);
            }
            return;
        }

        // Pop top state from the stack
        if(s.phase == StateMan.PH_POP) {
            this.Pop(s.rc);

        // Push new state to the top of the stack
        } else if(s.phase == StateMan.PH_PUSH) {
            this.Push(s.rc);

        // Pop current state and push new one
        } else if(s.phase == StateMan.PH_POP_PUSH) {
            int prev_state = this.GetPrev().GetEntryIdx();
            int new_state = s.rc;
            this.Pop(prev_state);
            this.Push(new_state);

        // Handle error
        } else {
            Utils.Assert(false, "Invalid state :: [state=%s] [phase=%d]",
              s.GetEntryName(), s.phase);
        }
    }
}
