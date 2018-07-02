// -----------------------------------------------------------------------------
package com.matalok.yamg.state;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.InputProcessor;
import com.matalok.yamg.LevelLoader;
import com.matalok.yamg.Obj;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.game.GameMan;
import com.matalok.yamg.ui.CommonWidget;
import com.matalok.yamg.ui.UiMan;
import com.matalok.yamg.ui.WindowScroller;

// -----------------------------------------------------------------------------
public class MainMenuSelectLevelState extends StateMan.State {
    // -------------------------------------------------------------------------
    public MainMenuSelectLevelState(InputProcessor [] ip_list) {
        super(Obj.STATE.MAIN_MENU_SELECT_LEVEL, ip_list);
    }

    // -------------------------------------------------------------------------
    protected void OnStateActivate() {
        // Main-menu
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.w_menu.get(Obj.UI.MAIN_MENU).Scroll(12)); // quit

        // Scroll window
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.wnd_scroll.Activate(
            UiMan.p.w_window.get(Obj.UI.SELECT_LEVEL_WND), WindowScroller.POS_UP));

        // Enable button
        UiMan.p.w_button.get(Obj.UI.MAIN_MENU_BUTTON_GAME).SetButton(true);

        // Start view movement
        GameMan.p.StartMenuMovement();
    }

    // -------------------------------------------------------------------------
    public void OnUiClick(CommonWidget w, float x, float y) {
        StateMan.p.ui_handler.OnWidgetClick(w, x, y);
    }
}
