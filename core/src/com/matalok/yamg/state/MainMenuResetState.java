// -----------------------------------------------------------------------------
package com.fomin.yamg.state;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.InputProcessor;
import com.fomin.yamg.Obj;
import com.fomin.yamg.TaskMan;
import com.fomin.yamg.game.GameMan;
import com.fomin.yamg.ui.CommonWidget;
import com.fomin.yamg.ui.UiMan;
import com.fomin.yamg.ui.WindowScroller;

// -----------------------------------------------------------------------------
public class MainMenuResetState extends StateMan.State {
    // -------------------------------------------------------------------------
    public MainMenuResetState(InputProcessor [] ip_list) {
        super(Obj.STATE.MAIN_MENU_RESET, ip_list);
    }

    // -------------------------------------------------------------------------
    protected void OnStateActivate() {
        // Scroll window
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.wnd_scroll.Activate(
            UiMan.p.w_window.get(Obj.UI.CONFIRM_WND), WindowScroller.POS_UP));

        // Scroll menu
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.w_menu.get(Obj.UI.MAIN_MENU).Scroll(7)); // yes/no

        // Change window title 
        UiMan.p.w_window.get(Obj.UI.CONFIRM_WND).SetTitle("Reset current level");

        // Start view movement
        GameMan.p.StartMenuMovement();
    }

    // -------------------------------------------------------------------------
    protected void OnStateDeactivate() { 
        // Main-menu
//        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
//          UiMan.p.w_menu.get(Obj.UI.MAIN_MENU).Scroll(12)); // quit
    }

    // -------------------------------------------------------------------------
    public void OnUiClick(CommonWidget w, float x, float y) {
        StateMan.p.ui_handler.OnWidgetClick(w, x, y);
    }
}
