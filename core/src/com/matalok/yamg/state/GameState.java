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
