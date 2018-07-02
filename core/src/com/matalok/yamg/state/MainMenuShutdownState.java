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

// -----------------------------------------------------------------------------
public class MainMenuShutdownState extends StateMan.State {
    // -------------------------------------------------------------------------
    public MainMenuShutdownState(InputProcessor [] ip_list) {
        super(Obj.STATE.MAIN_MENU_SHUTDOWN, ip_list);
    }

    // -------------------------------------------------------------------------
    protected void OnStateActivate() {
        // Scroll window
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.wnd_scroll.Activate(
            UiMan.p.w_window.get(Obj.UI.CONFIRM_WND), WindowScroller.POS_UP));

        // Scroll menu
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.w_menu.get(Obj.UI.MAIN_MENU).Scroll(4)); // yes/no

        // Change window title 
        UiMan.p.w_window.get(Obj.UI.CONFIRM_WND).SetTitle("Quit game");

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
