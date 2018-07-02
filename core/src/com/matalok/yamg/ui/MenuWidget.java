// -----------------------------------------------------------------------------
package com.fomin.yamg.ui;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.fomin.yamg.CommonTask;
import com.fomin.yamg.Obj;
import com.fomin.yamg.TaskMan;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public class MenuWidget extends GridWidget {
    // -------------------------------------------------------------------------
    private ButtonGroup group;

    // -------------------------------------------------------------------------
    public MenuWidget(int x_cnt, int y_cnt, int button_width, int button_height, int gap) {
        // Grid constructor
        super(Obj.WIDGET.MENU, x_cnt, y_cnt, gap, button_width, button_height);

        // Button group
        this.group = new ButtonGroup();
    }

    // -------------------------------------------------------------------------
    public ButtonWidget AddButton(ButtonWidget.Cfg cfg, int x, int y, String name, int state) {
        return this.AddButton(cfg, cfg, x, y, name, state);
    }

    // -------------------------------------------------------------------------
    public ButtonWidget AddButton(ButtonWidget.Cfg back_cfg, ButtonWidget.Cfg front_cfg, 
      int x, int y, String name, int state) {
        // Get front descriptor
        ButtonWidget.Descriptor front_desc = front_cfg.FindButtonDesc(name);
        Utils.Assert(front_desc != null, "Invalid button name :: [name=%s]", name);

        // Create button
        ButtonWidget w = new ButtonWidget(back_cfg.background, front_desc);
        this.SetWidget(x, y, w);

        // Set enabled/disabled
        w.SetDisabled(state == ButtonWidget.ST_DISABLED);

        // Add button to group
        this.group.add((Button)w.actor);
        this.UncheckButtons();
        return w;
    }

    // -------------------------------------------------------------------------
    public void UncheckButtons() {
        this.group.uncheckAll();
    }

    // -------------------------------------------------------------------------
    public TaskMan.Task Scroll(int idx) {
        return this.Scroll(0, idx, UiMan.p.screen_size.x - this.size.x, 0);
    }

    // -------------------------------------------------------------------------
    public TaskMan.Task Hide() {
        return new CommonTask.MovePtpTask(
          this, new Utils.Vector2f(
            UiMan.p.screen_size.x, 
            this.pos.y), 
          UiMan.p.wnd_scroll.GetDuration(), 
          Utils.ID_UNDEFINED, true);
    }
}
