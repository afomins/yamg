// -----------------------------------------------------------------------------
package com.matalok.yamg.state;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.InputProcessor;
import com.matalok.yamg.Obj;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.game.GameMan;
import com.matalok.yamg.ui.CommonWidget;
import com.matalok.yamg.ui.UiMan;
import com.matalok.yamg.ui.WindowScroller;

//-----------------------------------------------------------------------------
public class GameState extends StateMan.State {
    // -------------------------------------------------------------------------
    public GameState(InputProcessor [] ip_list) {
        super(Obj.STATE.GAME, ip_list);
    }

    // -------------------------------------------------------------------------
    protected void OnStateActivate( ) {
        // Scroll menu
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.w_menu.get(Obj.UI.MAIN_MENU).Scroll(17)); // last

        // Reset menu buttons
        UiMan.p.w_menu.get(Obj.UI.MAIN_MENU).UncheckButtons();

        // Scroll window
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.wnd_scroll.Activate(null, WindowScroller.POS_DOWN));

        // Stop view movement
        GameMan.p.StopMenuMovement();
    }

    // -------------------------------------------------------------------------
    public void OnUiClick(CommonWidget w, float x, float y) {
        StateMan.p.ui_handler.OnWidgetClick(w, x, y);
    }
}
