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
import com.matalok.yamg.CommonTask;
import com.matalok.yamg.Obj;
import com.matalok.yamg.ServiceMan;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.TaskMan.Task;
import com.matalok.yamg.ui.UiMan;
import com.matalok.yamg.ui.WindowScroller;

//-----------------------------------------------------------------------------
public class ShutdownState extends StateMan.State {
    // -------------------------------------------------------------------------
    private TaskMan.Task last_task;

    // -------------------------------------------------------------------------
    public ShutdownState() {
        super(Obj.STATE.SHUTDOWN, null);
    }

    // -------------------------------------------------------------------------
    protected void OnStateActivate() {
        // Hide main menu and dummy sleep
        TaskMan.TaskQueue q = new TaskMan.TaskQueue(1, false);
        q.AddTask(UiMan.p.w_menu.get(Obj.UI.MAIN_MENU).Hide());
        q.AddTask(this.last_task = new CommonTask.SleepTask(0, this, 1));
        TaskMan.p.AddTask(TaskMan.POOL_GAME, q);

        // Hide window
        TaskMan.p.AddTask(TaskMan.POOL_GAME, 
          UiMan.p.wnd_scroll.Activate(null, WindowScroller.POS_RIGHT));
    }

    // -------------------------------------------------------------------------
    public void OnTaskTeardown(Task t) {
        if(t.CmpId(this.last_task)) {
            ServiceMan.InitiateShutdown();
        }
    }
}
