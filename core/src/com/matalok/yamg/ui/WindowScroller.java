// -----------------------------------------------------------------------------
package com.matalok.yamg.ui;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.XmlReader;
import com.matalok.yamg.CfgReader;
import com.matalok.yamg.CommonTask;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.Utils;
import com.matalok.yamg.Utils.Vector2f;

// -----------------------------------------------------------------------------
public class WindowScroller {
    // -------------------------------------------------------------------------
    public static int POS_CENTER = 0;
    public static int POS_LEFT = 1;
    public static int POS_RIGHT = 2;
    public static int POS_UP = 3;
    public static int POS_DOWN = 4;
    public static int [] POS_OPPOSITE = new int[] { POS_CENTER, POS_RIGHT, POS_LEFT, POS_DOWN, POS_UP };

    // -------------------------------------------------------------------------
    private WindowWidget current;
    private Utils.Recti parent_rect, rect;
    private Utils.Vector2i [] wnd_pos;
    private Color back_col, border_col;
    private int border_size, gap_size, scroll_duration;
    private int header_size;

    // -------------------------------------------------------------------------
    public WindowScroller(XmlReader.Element cfg, Utils.Recti parent_rect) {
        this.gap_size = Utils.Str2Int(CfgReader.GetAttrib(cfg, "gap-size"));
        this.border_size = Utils.Str2Int(CfgReader.GetAttrib(cfg, "border-size"));
        this.scroll_duration = Utils.Str2Int(CfgReader.GetAttrib(cfg, "scroll-duration"));
        this.back_col = Utils.Str2Color(CfgReader.GetAttrib(cfg, "back-color"));
        this.border_col = Utils.Str2Color(CfgReader.GetAttrib(cfg, "border-color"));

        this.parent_rect = parent_rect;
        this.wnd_pos = new Utils.Vector2i [] { 
          new Utils.Vector2i(this.gap_size, this.gap_size),                             // center
          new Utils.Vector2i(this.gap_size - this.parent_rect.width, this.gap_size),    // left
          new Utils.Vector2i(this.gap_size + this.parent_rect.width, this.gap_size),    // right
          new Utils.Vector2i(this.gap_size, this.gap_size + this.parent_rect.height),   // up
          new Utils.Vector2i(this.gap_size, this.gap_size - this.parent_rect.height)};  // down

        LabelWidget l = new LabelWidget(Color.WHITE, "DUMMY");
        this.header_size = l.GetSize().y;
        l.Dispose();

        this.rect = new Utils.Recti(
          this.gap_size, this.gap_size, 
          this.parent_rect.width - this.gap_size * 2,
          this.parent_rect.height - this.gap_size * 2);
    }

    // -------------------------------------------------------------------------
    public WindowWidget CreateWindow(String title) {
        WindowWidget wnd = new WindowWidget(this.border_col, this.back_col, 
          this.border_size, (title == null) ? 1 : this.header_size, this.rect.width,
          this.rect.height, title);
        wnd.SetAlpha(0.0f);
        return wnd;
    }

    // -------------------------------------------------------------------------
    public TaskMan.Task Activate(WindowWidget w, int from) {
        // If currently active window and new widow are the same - nothing to do 
        if(this.current != null && w != null && this.current.CmpId(w)) {
            return null;
        }

        // Continue with activation
        TaskMan.TaskPool p = new TaskMan.TaskPool(1, false);
        Utils.Vector2i pos_to = this.wnd_pos[POS_OPPOSITE[from]];
        Utils.Vector2i pos_from = this.wnd_pos[from];
        Utils.Vector2i pos_center = this.wnd_pos[POS_CENTER];

        // Show new window
        if(w != null) {
            w.SetAlpha(1.0f);
            w.SetPos(pos_from.x, pos_from.y);
            p.AddTask(new CommonTask.MovePtpTask(
              w, new Utils.Vector2f(pos_center.x, pos_center.y), 
              this.scroll_duration, 
              Utils.ID_UNDEFINED, true));
        }

        // Hide current window
        if(this.current != null) {
            TaskMan.TaskQueue q = (TaskMan.TaskQueue)p.AddTask(new TaskMan.TaskQueue(1, false));
            q.AddTask(new CommonTask.MovePtpTask(
              this.current, new Utils.Vector2f(pos_to.x, pos_to.y), 
              this.scroll_duration, 
              Utils.ID_UNDEFINED, true));
            q.AddTask(new CommonTask.TweenTask(this.current, 0.0f, 1));
        }

        // Save current window
        this.current = w;
        return p;
    }

    // -------------------------------------------------------------------------
    public long GetDuration()  { return this.scroll_duration; }
}
