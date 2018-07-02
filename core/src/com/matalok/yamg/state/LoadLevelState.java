// -----------------------------------------------------------------------------
package com.matalok.yamg.state;

//-----------------------------------------------------------------------------
import com.matalok.yamg.CommonTask;
import com.matalok.yamg.LevelLoader;
import com.matalok.yamg.Logger;
import com.matalok.yamg.Obj;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.Timer;
import com.matalok.yamg.Utils;
import com.matalok.yamg.TaskMan.Task;
import com.matalok.yamg.game.GameMan;
import com.matalok.yamg.ui.ContainerWidget;
import com.matalok.yamg.ui.UiMan;

// -----------------------------------------------------------------------------
public class LoadLevelState extends StateMan.State {
    // -------------------------------------------------------------------------
    private CommonTask.SleepTask load_level_task, post_sleep_task;
    private long start_time, min_duration;
    private boolean is_first;

    // -------------------------------------------------------------------------
    public LoadLevelState(long min_duration) {
        super(Obj.STATE.LOAD_LEVEL, null);
        this.min_duration = min_duration;
        this.is_first = true;
    }

    // -------------------------------------------------------------------------
    protected void OnStateActivate() {
        // Create task-queue
        TaskMan.TaskQueue q = new TaskMan.TaskQueue(1, false);
        ContainerWidget w = UiMan.p.w_container.get(Obj.UI.LOAD_SCREEN);

        // Level name
        String name = UiMan.p.w_selector.get(Obj.UI.LEVEL_SELECTOR).GetSelectedLevelName();
        name = (name == null) ? GameMan.p.GetStartupLevelName() : name;

        // Fill task-queue
        q.AddTask(new CommonTask.TweenTask(w, 1.0f, 500));                  // Fade-in loading screen 
        q.AddTask(this.load_level_task = LevelLoader.p.LoadLevel(name, this)); // Load level
        q.AddTask(this.post_sleep_task = new CommonTask.SleepTask(0, this, 3));  // Post sleep task
        q.AddTask(new CommonTask.TweenTask(w, 0.0f, 300));                  // Fade-out loading screen

        // Run task-queue
        TaskMan.p.AddTask(TaskMan.POOL_GAME, q);
        this.start_time = Timer.Get();

        // Reset progress bar
        UiMan.p.w_progress.get(Obj.UI.LOAD_SCREEN_PROGRESS).ResetProgress();
    }

    // -------------------------------------------------------------------------
    public void OnTaskTeardown(Task t) {
        float progress = 0.0f;

        // Load-level-task = first 90% of total progress
        if(this.load_level_task != null && t.CmpId(this.load_level_task)) {
            progress = t.GetTaskProgress() * 0.9f;

            // Continue with post-sleep task when level is loaded
            if(t.IsTaskLastIter()) {
                long duration = Timer.Get() - this.start_time;
                long duration_delta = this.min_duration - duration;

                // Update post-sleep task
                Logger.d(Logger.MOD_LVL, "Level is fully loaded :: [post-sleep-duration=%d]", duration_delta);
                if(duration_delta > 0) {
                    this.post_sleep_task.UpdateDuration(duration_delta / 3);
                }
            }

        // Post-sleep-task = remaining 10% of total progress 
        } else if(this.post_sleep_task != null && t.CmpId(this.post_sleep_task)) {
            progress = 0.9f + t.GetTaskProgress() * 0.1f;

            // Pop to previous state
            if(t.IsTaskLastIter()) {
                if(this.is_first) {
                    this.SetPhase(StateMan.PH_POP, Obj.STATE.MAIN_MENU_INFO);
                    this.is_first = false;
                } else {
                    this.SetPhase(StateMan.PH_POP, Obj.STATE.GAME);
                }
            }

        // Blah...
        } else {
            Utils.Assert(false, "Invalid task :: [task=%s]", t.GetObjName());
        }

        // Update progress bar
        UiMan.p.w_progress.get(Obj.UI.LOAD_SCREEN_PROGRESS).SetProgress(progress);
    }
}
