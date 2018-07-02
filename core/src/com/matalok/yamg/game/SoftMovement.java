// -----------------------------------------------------------------------------
package com.fomin.yamg.game;

// -----------------------------------------------------------------------------
import com.fomin.yamg.CommonTask;
import com.fomin.yamg.Logger;
import com.fomin.yamg.Obj;
import com.fomin.yamg.TaskMan;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public class SoftMovement {
    // -------------------------------------------------------------------------
    public static int ST_IDLE = 0;
    public static int ST_RUN = 1;
    public static int ST_STOP = 2;
    public static final Obj.Group ST_GROUP = new Obj.Group("soft-move-state", 
      new String [] {"idle", "run", "stop"});

    // -------------------------------------------------------------------------
    private TaskMan.Task main_task;
    private CommonTask.MoveTask move_task;
    private CommonTask.IMoveTaskObject move_obj;
    private Utils.Direction2 direction;
    private float stop_multiplier;
    private float default_speed;
    private int state;
    private boolean inherit_speed_if_zero;

    // ---------------------------------------------------------------------
    public SoftMovement(Utils.Direction2 direction, float stop_multiplier, 
      float default_speed, boolean inherit_speed_if_zero) {
        this.state = SoftMovement.ST_IDLE;
        this.direction = direction;
        this.default_speed = default_speed;
        this.stop_multiplier = stop_multiplier;
        this.inherit_speed_if_zero = inherit_speed_if_zero;
    }

    // -------------------------------------------------------------------------
    public int GetState() {
        return this.state;
    }

    // -------------------------------------------------------------------------
    public void Start(CommonTask.IMoveTaskObject obj, boolean is_ptp_task) {
        // Do not start new movement if already moving
        if(this.state == SoftMovement.ST_RUN) return;

        // Save move object 
        this.move_obj = obj;

        // Force stop legacy task
        if(this.state == SoftMovement.ST_STOP) {
            this.main_task.StopTask(true);
        }

        // Set movement task
        this.main_task = this.move_task = (is_ptp_task) ?
          // Point-to-point movement
          new CommonTask.MovePtpTask(this.move_obj,
            new Utils.Vector2f(), Utils.ID_UNDEFINED, this.default_speed, false) :

          // Directional movement 
          new CommonTask.MoveDirTask(
            this.move_obj, this.direction, this.default_speed);

        // Add to task manager
        TaskMan.p.AddTask(TaskMan.POOL_GAME, this.main_task);

        // Select next state
        int old_state = this.state;
        this.state = SoftMovement.ST_RUN;

        // Log
        Logger.d(Logger.MOD_GAME, "Starting soft movement :: [move-obj=%s] [task=%s] [state=%s->%s]",
          this.move_obj.GetObjName(), 
          this.main_task.GetObjName(),
          SoftMovement.ST_GROUP.GetEntryName(old_state),
          SoftMovement.ST_GROUP.GetEntryName(this.state));
    }

    // -------------------------------------------------------------------------
    public void Stop(long duration, float speed, CommonTask.IMoveTaskObject new_move_obj) {
        // Stop only running task
        if(this.state != SoftMovement.ST_RUN) return;

        // Task should be present
        Utils.Assert(this.main_task != null, "Movement task missing");

        // Stop current movement task
        boolean is_running = (this.main_task.GetTaskState() == TaskMan.ST_RUN);
        this.main_task.StopTask(true);

        // Initially return to IDLE state 
        int next_state = SoftMovement.ST_IDLE;

        // Make a graceful stop task - continues same movement direction and smoothly 
        // decreases speed until total stop 
        if(duration > 0 && is_running) {
            // Get new speed
            CommonTask.MoveTask orig_move_task = (CommonTask.MoveTask)this.main_task;
            if(speed == 0.0f && this.inherit_speed_if_zero) {
                speed = ((CommonTask.MoveTask)this.main_task).GetSpeed();
            }

            if(speed > 0.0f) {
                // Switch to STOP state when creating new task
                next_state = SoftMovement.ST_STOP;

                // Update movement object
                if(new_move_obj != null) {
                    this.move_obj = new_move_obj;
                }

                // New direction
                Utils.Direction2 dir = orig_move_task.GetDirection();
                dir.Mul(this.stop_multiplier);

                // New movement task that continues original direction
                this.move_task = new CommonTask.MoveDirTask(
                  this.move_obj, dir, speed); 

                // Task that smoothly decreases speed and stops movment task
                TaskMan.TaskQueue q = new TaskMan.TaskQueue(TaskMan.POOL_GAME, false);
                q.AddTask(new CommonTask.TweenTask(move_task, 0.0f, duration));
                q.AddTask(new CommonTask.StopTask(move_task, true));

                // Put tasks in same pool
                TaskMan.TaskPool p = new TaskMan.TaskPool(TaskMan.POOL_GAME, false);
                p.AddTask(move_task);
                p.AddTask(q);
                this.main_task = TaskMan.p.AddTask(TaskMan.POOL_GAME, p);
            }
        }

        // Log
        Logger.d(Logger.MOD_GAME, "Stopping soft move :: [duration=%d] [move-obj=%s] [task=%s] [state=%s->%s]",
          duration, this.move_obj.GetObjName(), this.main_task.GetObjName(),
          SoftMovement.ST_GROUP.GetEntryName(this.state),
          SoftMovement.ST_GROUP.GetEntryName(next_state));

        // Change state
        this.state = next_state;
        return;
    }

    // -------------------------------------------------------------------------
    public void UpdatePtpDestination(Utils.Vector2f dest) {
        // Update only running tasks
        if(this.state != SoftMovement.ST_RUN) return;

        // Must be a ptp-movement
        Utils.Assert(this.main_task.GetEntryIdx() == Obj.TASK.MOVE_PTP, 
          "Failed to update destination of task :: [name=%s]", this.main_task.GetObjName());

        // Update 
        ((CommonTask.MovePtpTask)this.main_task).UpdateDestination(dest);
    }

    // -------------------------------------------------------------------------
    public void ResetDirection(boolean x, boolean y) {
        // Update only stopping tasks
        if(this.state != SoftMovement.ST_STOP) return;

        // Reset movment by axis
        if(x) this.move_task.GetDirection().x = 0.0f;
        if(y) this.move_task.GetDirection().y = 0.0f;
    }
}