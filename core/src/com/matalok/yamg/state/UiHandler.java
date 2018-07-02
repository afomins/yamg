// -----------------------------------------------------------------------------
package com.fomin.yamg.state;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.fomin.yamg.Obj;
import com.fomin.yamg.TaskMan;
import com.fomin.yamg.Utils;
import com.fomin.yamg.ui.ButtonWidget;
import com.fomin.yamg.ui.CommonWidget;
import com.fomin.yamg.ui.UiMan;

// -----------------------------------------------------------------------------
public class UiHandler implements InputProcessor {
    // -------------------------------------------------------------------------
    private boolean select_next;
    private int next_phase, next_rc;

    // -------------------------------------------------------------------------
    private void SetNextPhase(int phase, int rc) {
        this.select_next = true;
        this.next_phase = phase;
        this.next_rc = rc;
    }

    // -------------------------------------------------------------------------
    private void ApplyNextPhase(StateMan.State s) {
        if(this.select_next && s.GetEntryIdx() != this.next_rc) {
            s.SetPhase(this.next_phase, this.next_rc);
        }
    }

    // -------------------------------------------------------------------------
    private void ResetNextPhase() {
        this.select_next = false;
    }

    // -------------------------------------------------------------------------
    public void OnWidgetClick(CommonWidget w, float x, float y) {
        // Default next state
        StateMan.State s = StateMan.p.GetTop();

        // Ignore disabled buttons
        if(w.GetEntryIdx() == Obj.WIDGET.BUTTON && ((ButtonWidget)w).IsDisabled()) {
            return;
        }

        // Reset next phase
        this.ResetNextPhase();

        // Back
        if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_BACK).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_POP, Obj.STATE.GAME);

        // Select level
        } else if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_GAME).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_POP_PUSH, Obj.STATE.MAIN_MENU_SELECT_LEVEL);
            UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_GAME);

        // Settings
        } else if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_INFO).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_POP_PUSH, Obj.STATE.MAIN_MENU_INFO);

        // Reset
        } else if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_RESET).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.MAIN_MENU_RESET);

        // Quit
        } else if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_QUIT).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.MAIN_MENU_SHUTDOWN);
        }

        // Menu
        else if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_MENU).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.MAIN_MENU_INFO);
        }

        // Level selector next
        else if(UiMan.p.w_button.get(Obj.UI.LEVEL_SELECTOR_NEXT).CmpId(w)) {
            TaskMan.p.AddTask(TaskMan.POOL_GAME, UiMan.p.w_selector.get(Obj.UI.LEVEL_SELECTOR).NextSheet());
        }

        // Level selector prev
        else if(UiMan.p.w_button.get(Obj.UI.LEVEL_SELECTOR_PREV).CmpId(w)) {
            TaskMan.p.AddTask(TaskMan.POOL_GAME, UiMan.p.w_selector.get(Obj.UI.LEVEL_SELECTOR).PrevSheet());
        }

        // Level selector run
        else if(UiMan.p.w_button.get(Obj.UI.LEVEL_SELECTOR_RUN).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.LOAD_LEVEL);
        }

        // Yes
        else if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_YES).CmpId(w)) {
            // Reset level
            if(s.GetEntryIdx() == Obj.STATE.MAIN_MENU_RESET) {
                this.SetNextPhase(StateMan.PH_POP_PUSH, Obj.STATE.LOAD_LEVEL);

            // Quit game
            } else if(s.GetEntryIdx() == Obj.STATE.MAIN_MENU_SHUTDOWN) {
                this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.SHUTDOWN);
            }
        }

        // No
        else if(UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_NO).CmpId(w)) {
            this.SetNextPhase(StateMan.PH_POP, Utils.ID_UNDEFINED);
        }

        // Apply changes
        this.ApplyNextPhase(s);
    }

    // -------------------------------------------------------------------------
    public boolean keyUp(int keycode) {
        StateMan.State s = StateMan.p.GetTop();
        boolean was_processed = false;

        // Reset next phase
        this.ResetNextPhase();

        // Back button
        if(keycode == Keys.BACK) {
            if(s.GetEntryIdx() == Obj.STATE.MAIN_MENU_SHUTDOWN) {
                this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.SHUTDOWN);

            } else {
                this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.MAIN_MENU_SHUTDOWN);
            }

        // Menu button
        } else if(keycode == Keys.MENU || keycode == Keys.ESCAPE) {
            if(s.GetEntryIdx() == Obj.STATE.GAME) {
                this.SetNextPhase(StateMan.PH_PUSH, Obj.STATE.MAIN_MENU_INFO);
            }
        }

        // Apply changes
        this.ApplyNextPhase(s);
        return was_processed;
    }

    // -------------------------------------------------------------------------
    public boolean keyDown(int keycode) { return true; };
    public boolean keyTyped(char character) { return false; };
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; };
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; };
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; };
    public boolean mouseMoved(int screenX, int screenY) { return false; };
    public boolean scrolled(int amount) { return false; };
}
