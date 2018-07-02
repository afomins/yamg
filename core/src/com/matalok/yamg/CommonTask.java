// -----------------------------------------------------------------------------
package com.matalok.yamg;

// -----------------------------------------------------------------------------
public class CommonTask {
    // =========================================================================
    // SleepTask
    public static class SleepTask extends TaskMan.Task {
        // ---------------------------------------------------------------------
        private long duration, end;

        // ---------------------------------------------------------------------
        public SleepTask(long duration, TaskMan.ITaskListener listener, int repeat_cnt) {
            super(Obj.TASK.SLEEP, null, listener, repeat_cnt);
            this.duration = duration;
            this.end = Utils.ID_UNDEFINED;
        }

        // ---------------------------------------------------------------------
        protected void OnTaskSetup() {
            this.end = Timer.Get() + this.duration;
        }

        // ---------------------------------------------------------------------
        protected boolean OnTaskRun() {
            return (Timer.Get() > this.end);
        }

        // ---------------------------------------------------------------------
        public void UpdateDuration(long duration) {
            Utils.Assert(this.end == Utils.ID_UNDEFINED, "Failed to update running task");
            this.duration = duration;
        }
    }

    // =========================================================================
    // IMoveTaskObject
    public interface IMoveTaskObject extends Obj.ICommonObject {
        // ---------------------------------------------------------------------
        public Utils.Vector2f GetMovePos();
        public void SetMoveRelPos(float x, float y);
        public void SetMoveAbsPos(float x, float y);
    }

    // =========================================================================
    // MoveTask
    public static abstract class MoveTask extends TaskMan.Task 
      implements ITweenTaskObject {
        // ---------------------------------------------------------------------
        protected IMoveTaskObject obj;
        protected Utils.Direction2 direction;
        protected Utils.Vector2f final_pos;
        protected float speed;

        // ---------------------------------------------------------------------
        public MoveTask(int type, IMoveTaskObject obj, Utils.Direction2 direction,
          Utils.Vector2f final_pos, float speed) {
            super(type, obj, null, 1);

            this.obj = obj;
            this.direction = direction;
            this.final_pos = final_pos;
            this.speed = speed;
        }

        // ---------------------------------------------------------------------
        public float GetSpeed() { return this.speed; }
        public void SetSpeed(float val) { this.speed = val; };
        public Utils.Direction2 GetDirection() { return this.direction; };
        public IMoveTaskObject GetMoveTaskObj() { return this.obj; }

        // ---------------------------------------------------------------------
        // ITweenObject
        public float GetTweenValue() { return this.GetSpeed(); }
        public void SetTweenValue(float val) { this.SetSpeed(val); };
    }

    // =========================================================================
    // MovePtpTask :: point-to-point
    public static class MovePtpTask extends CommonTask.MoveTask {
        // ---------------------------------------------------------------------
        private Utils.Vector2f origin_pos;
        private boolean stop_when_over;
        private long update_time;
        private long duration, orig_duration;
        private float orig_speed;

        // ---------------------------------------------------------------------
        public MovePtpTask(IMoveTaskObject obj, Utils.Vector2f final_pos, 
          long duration, float speed, boolean stop_when_over) {
            super(Obj.TASK.MOVE_PTP, obj, null, final_pos, speed);
            this.stop_when_over = stop_when_over;
            this.duration = duration;
            this.orig_duration = this.duration;
            this.orig_speed = this.speed;
        }

        // ---------------------------------------------------------------------
        protected void OnTaskSetup() { 
            this.origin_pos = new Utils.Vector2f();
            this.direction = new Utils.Direction2();
            this.UpdateDestination(this.final_pos);
        }

        // ---------------------------------------------------------------------
        protected boolean OnTaskRun() { 
            // Timing
            long time_delta = Timer.Get() - this.update_time;
            boolean is_over = (time_delta >= this.duration);

            // Calculate new position
            Utils.Vector2f pos = Utils.v2f_tmp;
            if(is_over) {
                pos.Set(this.final_pos);

            } else {
                float step = time_delta * this.speed;
                pos.Set(this.direction).Mul(step).Add(this.origin_pos);
            }

            // Set new position
            this.obj.SetMoveAbsPos(pos.x, pos.y);
            return (this.stop_when_over && is_over);
        }

        // ---------------------------------------------------------------------
        public void UpdateDestination(Utils.Vector2f dest) {
            // Time
            this.update_time = Timer.Get();

            // Origin
            this.origin_pos.Set(this.obj.GetMovePos());

            // Final position
            if(this.final_pos != null) {
                this.final_pos.Set(dest);
            }

            // Direction
            this.direction.Set(dest).Sub(this.obj.GetMovePos());
            this.direction.Update();

            // Duration
            if(this.orig_duration == Utils.ID_UNDEFINED) {
                Utils.Assert(this.speed > 0.0f, "Speed is missing");
                this.duration = (long)(this.direction.len / this.orig_speed);

            // Speed
            } else {
                Utils.Assert(this.duration > 0, "Duration is missing");
                this.speed = this.direction.len / (float)this.orig_duration;
            }
        }
    }

    // =========================================================================
    // MoveDirTask :: directional
    public static class MoveDirTask extends CommonTask.MoveTask {
        // ---------------------------------------------------------------------
        private long prev_time;

        // ---------------------------------------------------------------------
        public MoveDirTask(IMoveTaskObject obj, Utils.Direction2 direction, 
          float speed) {
            super(Obj.TASK.MOVE_DIR, obj, direction, null, speed);
        }

        // ---------------------------------------------------------------------
        protected void OnTaskSetup() { 
            this.prev_time = this.GetTaskStartTime();
        }

        // ---------------------------------------------------------------------
        protected boolean OnTaskRun() {
            // Update time
            long cur_time = Timer.Get();
            long time_delta = cur_time - this.prev_time;
            this.prev_time = cur_time;

            // Set new position
            float step = time_delta * this.speed;
            this.obj.SetMoveRelPos(
              step * this.direction.x, 
              step * this.direction.y);
            return false;
        }
    }

    // =========================================================================
    // ITweenObject
    public interface ITweenTaskObject extends Obj.ICommonObject {
        // ---------------------------------------------------------------------
        public float GetTweenValue();
        public void SetTweenValue(float val);
    }

    // =========================================================================
    // TweenTask
    public static class TweenTask extends TaskMan.Task {
        // ---------------------------------------------------------------------
        protected ITweenTaskObject task_obj;
        protected TweenObject tween;
        private float src_val, dst_val, delta_val;
        private long duration;

        // ---------------------------------------------------------------------
        public TweenTask(ITweenTaskObject obj, float dst_val, long duration) {
            super(Obj.TASK.TWEEN, obj, null, 1);
            this.task_obj = obj;
            this.dst_val = dst_val;
            this.duration = duration;
        }

        // ---------------------------------------------------------------------
        protected void OnTaskSetup() { 
            this.src_val = this.task_obj.GetTweenValue();
            this.delta_val = this.dst_val - this.src_val;

            // Start tweening [0.0 - 1.0]
            this.tween = new TweenObject(0.0f);
            this.tween.Start(0.0f, 1.0f, this.duration);
        }

        // ---------------------------------------------------------------------
        protected boolean OnTaskRun() {
            // Calculate tweening
            float val = this.tween.Continue();
            if(!this.tween.IsActive()) {
                val = this.dst_val;

            } else {
                val = this.src_val + this.delta_val * val;
            }

            // Set tween object value
            this.task_obj.SetTweenValue(val);
            return !this.tween.IsActive();
        }
    }

    // =========================================================================
    // StopTask
    public static class StopTask extends TaskMan.Task {
        // ---------------------------------------------------------------------
        private boolean is_full;

        // ---------------------------------------------------------------------
        public StopTask(TaskMan.Task task, boolean full) {
            super(Obj.TASK.STOP, task, null, 1);
            this.is_full = full;
        }

        // ---------------------------------------------------------------------
        protected boolean OnTaskRun() {
            ((TaskMan.Task)this.param).StopTask(this.is_full);
            return true;
        }
    }
}
