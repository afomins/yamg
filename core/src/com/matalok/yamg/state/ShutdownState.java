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
