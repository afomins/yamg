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
package com.matalok.yamg;

// -----------------------------------------------------------------------------
import java.util.LinkedList;
import java.util.ListIterator;

import com.badlogic.gdx.utils.XmlReader;

// -----------------------------------------------------------------------------
public class TaskMan extends ServiceMan.Service {
    // =========================================================================
    // Task
    public static abstract class Task extends Obj.CommonObject {
        // ---------------------------------------------------------------------
        private int state;
        private long start_time;
        private int repeat_cnt, iter;
        protected Obj.ICommonObject param;
        private ITaskListener listener;

        // ---------------------------------------------------------------------
        public Task(int type, Obj.ICommonObject param, ITaskListener listener, int repeat_cnt) {
            super(Obj.TASK.ptr, type);
            this.param = param;
            this.listener = listener;
            this.state = TaskMan.ST_SETUP;
            this.repeat_cnt = repeat_cnt;
            this.iter = 0;

            Logger.d(Logger.MOD_TASK, "Creating task :: [name=%s] [param=%s] [listener=%s] [repeat=%d]", 
              this.GetObjName(), this.GetParamStr(), this.GetListenerStr(), this.repeat_cnt);
        }

        // ---------------------------------------------------------------------
        private String GetParamStr() { 
            return (this.param == null) ? "null" : this.param.GetObjName(); 
        }

        // ---------------------------------------------------------------------
        private String GetListenerStr() { 
            return (this.listener == null) ? "null" : this.listener.GetObjName(); 
        }

        // ---------------------------------------------------------------------
        public long GetTaskStartTime() { return this.start_time; };
        public long GetTaskUptime() { return Timer.Get() - this.start_time; }
        public int GetTaskIter() { return this.iter; };
        public int GetTaskRepeat() { return this.repeat_cnt; };
        public float GetTaskProgress() { return (float)(this.iter + 1) / this.repeat_cnt; };
        public boolean IsTaskLastIter() { return (this.iter + 1 == this.repeat_cnt); }

        // ---------------------------------------------------------------------
        public void StopTask(boolean full) {
            // Set STOP state
            this.ChangeState(TaskMan.ST_STOP, "Stopping task");

            // Discard further iterations when fully stopping 
            if(full) {
                this.iter = this.repeat_cnt;
            }
        }

        // ---------------------------------------------------------------------
        private void ChangeState(int new_state, String msg) {
            int prev_state = this.state;    // old
            this.state = new_state;         // new
            Logger.d(Logger.MOD_TASK, "%s :: [task=%s] [state=%s->%s]",
              msg, this.GetObjName(), 
              TaskMan.ST_GROUP.GetEntryName(prev_state, true),
              TaskMan.ST_GROUP.GetEntryName(new_state, true));

        }

        // ---------------------------------------------------------------------
        private void NotifyListener() {
            if(this.listener == null) return;
            else if(this.state == TaskMan.ST_SETUP) this.listener.OnTaskSetup(this);  
            else if(this.state == TaskMan.ST_RUN) this.listener.OnTaskRun(this);
            else if(this.state == TaskMan.ST_TEARDOWN) this.listener.OnTaskTeardown(this);
            else Utils.Assert(false, "Invalid task state :: [state=%d]", this.state);
        }

        // ---------------------------------------------------------------------
        public int GetTaskState() { return this.state; }

        // ---------------------------------------------------------------------
        protected void OnTaskSetup() { };
        protected boolean OnTaskRun() { return true; };
        protected void OnTaskTeardown() { };
    }

    // =========================================================================
    // ITaskListener
    public interface ITaskListener extends Obj.ICommonObject {
        // ---------------------------------------------------------------------
        public abstract void OnTaskSetup(Task t);
        public abstract void OnTaskRun(Task t);
        public abstract void OnTaskTeardown(Task t);
    }

    // =========================================================================
    // TaskContainer
    public static abstract class TaskContainer extends Task {
        // ---------------------------------------------------------------------
        protected LinkedList<Task> container;
        private int priority, cnt;
        private boolean is_permanent;

        // ---------------------------------------------------------------------
        public TaskContainer(int type, int priority, boolean is_permanent) {
            super(type, null, null, 1);
            this.priority = priority;
            this.is_permanent = is_permanent;
            this.cnt = 0;
            this.container = new LinkedList<Task>();
        }

        // ---------------------------------------------------------------------
        public boolean IsReady() {
            return (++this.cnt % this.priority == 0);
        }

        // ---------------------------------------------------------------------
        public boolean IsEmpty() {
            return (this.container.size() == 0);
        }

        // ---------------------------------------------------------------------
        public boolean IsPermanent() {
            return this.is_permanent;
        }

        // ---------------------------------------------------------------------
        public Task AddTask(Task t) {
            this.container.add(t);
            return t;
        }

        // ---------------------------------------------------------------------
        public void StopTask(boolean full) {
            Logger.d(Logger.MOD_TASK, "Stopping task container :: [task=%s] [size=%d]",
              this.GetObjName(), this.container.size());

            for(Task t: this.container) {
                t.StopTask(full);
            }
        }

        // ---------------------------------------------------------------------
        public boolean RunTask(Task t) {
            // Notify listener about event
            t.NotifyListener();

            // SETUP
            boolean select_next_state = true;
            if(t.state == TaskMan.ST_SETUP) {
                if(t.iter == 0) TaskMan.p.active_task_cnt++;

                Logger.d(Logger.MOD_TASK, "Starting task :: [task=%s] [cont=%s] [param=%s] [iter=%d/%d]",
                  t.GetObjName(), this.GetObjName(), t.GetParamStr(), t.iter + 1, t.repeat_cnt);

                t.start_time = Timer.Get();
                t.OnTaskSetup();

            // RUN
            } else if(t.state == TaskMan.ST_RUN) {
                select_next_state = t.OnTaskRun();

            // STOP
            } else if(t.state == TaskMan.ST_STOP) {
                select_next_state = true;

            // TEARDOWN
            } else if(t.state == TaskMan.ST_TEARDOWN) {
                Logger.d(Logger.MOD_TASK, "Ending task :: [task=%s] [cont=%s] [param=%s] [iter=%d/%d] [run-time=%d]",
                  t.GetObjName(), this.GetObjName(), t.GetParamStr(), t.iter + 1, t.repeat_cnt, t.GetTaskUptime());

                t.OnTaskTeardown();
                t.iter++;
            }

            // Select next state
            if(select_next_state) {
                int step = (t.state == TaskMan.ST_STOP - 1) ? 2 : 1; // Step over the STOP state
                t.ChangeState((t.state + step) % TaskMan.ST_CNT, "Changing task state");
            }

            // Task is finished if:
            //   1) State has changed: TEARDOWN->SETUP
            //   2) It has repeated correct number of times
            boolean is_finished = (t.state == TaskMan.ST_SETUP && t.iter >= t.repeat_cnt); 
            if(is_finished) TaskMan.p.active_task_cnt--;

            return is_finished;
        }

        // ---------------------------------------------------------------------
        public void Dispose() {
            for(Task t: this.container) {
                t.Dispose();
            }
            super.Dispose();
        }

        // ---------------------------------------------------------------------
        protected boolean IsReadyForTeardown() {
            return (!this.IsPermanent() && this.IsEmpty());
        }

        // ---------------------------------------------------------------------
        public abstract boolean OnTaskRun();
    }

    // =========================================================================
    // TaskQueue
    public static class TaskQueue extends TaskContainer {
        // ---------------------------------------------------------------------
        public TaskQueue(int priority, boolean is_permanent) {
            super(Obj.TASK.QUEUE, priority, is_permanent);
        }

        // ---------------------------------------------------------------------
        public boolean OnTaskRun() {
            // Ignore empty container
            if(this.IsEmpty()) return true;

            // Run task and exit if it is not over
            Task t = this.container.getFirst();
            if(!this.RunTask(t)) return false;

            //
            // Task is over 
            //

            // Remove task from the queue
            this.container.removeFirst();
            t.Dispose();

            // Queue is empty
            if(this.IsEmpty()) {
                Logger.d(Logger.MOD_TASK, "Queue is empty :: [queue=%s]", this.GetObjName());
            }
            return this.IsReadyForTeardown();
        }
    }

    // =========================================================================
    // TaskPool
    public static class TaskPool extends TaskContainer {
        // ---------------------------------------------------------------------
        public TaskPool(int priority, boolean is_permanent) {
            super(Obj.TASK.POOL, priority, is_permanent);
        }

        // ---------------------------------------------------------------------
        public boolean OnTaskRun() {
            // Ignore empty container
            if(this.IsEmpty()) return this.IsReadyForTeardown();

            // Run tasks 
            ListIterator<Task> it = this.container.listIterator();
            while(it.hasNext()) {
                Task t = it.next();
                if(this.RunTask(t)) {
                    it.remove();
                    t.Dispose();
                }
            }

            // Pool is empty
            if(this.IsEmpty()) {
                Logger.d(Logger.MOD_TASK, "Pool is empty :: [pool=%s]", this.GetObjName());
            }
            return this.IsReadyForTeardown();
        }
    }

    // =========================================================================
    // TaskMasn
    public static final int ST_SETUP = 0;
    public static final int ST_RUN = 1;
    public static final int ST_STOP = 2;
    public static final int ST_TEARDOWN = 3;
    public static final int ST_CNT = 4;
    public static final Obj.Group ST_GROUP = new Obj.Group("task-state", 
      new String [] {"setup", "run", "stop", "teardown"});

    public static final int POOL_UI = 0;
    public static final int POOL_GAME = 1;
    public static final Obj.Group POOL_GROUP = new Obj.Group("task-pool",
      new String [] { "ui", "game"});

    // -------------------------------------------------------------------------
    private TaskPool [] pool;
    private int active_task_cnt;

    // -------------------------------------------------------------------------
    public TaskMan() {
        super(Obj.SERVICE.TASK);
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static TaskMan p;
    protected void AcquireServicePointer() { TaskMan.p = this; };
    protected void ReleaseServicePointer() { TaskMan.p = null; };

    // -------------------------------------------------------------------------
    public Task AddTask(int pool_id, Task t) {
        if(t == null) { return null; }
        this.pool[pool_id].AddTask(t);
        return t;
    }

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        // Create queues
        this.pool = new TaskPool[] {
          new TaskPool(2, true),
          new TaskPool(1, true)
        };
    }

    // -------------------------------------------------------------------------
    protected void OnServiceTeardown() {
        for(TaskPool p: this.pool) {
            p.Dispose();
        }
        super.OnServiceTeardown();
    }

    // -------------------------------------------------------------------------
    public void OnServiceRun() {
        for(int i = 0; i < this.pool.length; i++) {
            TaskPool p = this.pool[i];
            if(p.IsReady()) p.OnTaskRun();
        }
    }

    // -------------------------------------------------------------------------
    public void OnServiceTimer() {
        Logger.d(Logger.MOD_TASK, "Task-man :: [active-cnt=%d]", this.active_task_cnt);
    }
}
