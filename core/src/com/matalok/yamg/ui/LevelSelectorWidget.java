// -----------------------------------------------------------------------------
package com.fomin.yamg.ui;

//-----------------------------------------------------------------------------
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.fomin.yamg.CfgReader;
import com.fomin.yamg.CommonObject;
import com.fomin.yamg.Logger;
import com.fomin.yamg.Obj;
import com.fomin.yamg.TaskMan;
import com.fomin.yamg.UserMan;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public class LevelSelectorWidget extends ContainerWidget {
    // =========================================================================
    // Sheet
    public static class Sheet {
        // ---------------------------------------------------------------------
        protected String name;
        protected Utils.Vector2i pos;
        protected Array<String> level_name; 

        // ---------------------------------------------------------------------
        public Sheet(String name, int x, int y) {
            this.name = name;
            this.pos = new Utils.Vector2i(x, y);
            this.level_name = new Array<String>();
        }

        // ---------------------------------------------------------------------
        public void AddLevel(String name) {
            this.level_name.add(name);
        }
    }

    // =========================================================================
    // ClickInputHandler
    public static class ClickInputHandler extends CommonWidget.ClickInputHandler {
        // ---------------------------------------------------------------------
        private int action;
        private String arg;
        private LevelSelectorWidget selector;

        // ---------------------------------------------------------------------
        public ClickInputHandler(LevelSelectorWidget selector, CommonWidget w, 
          int action, String arg) {
            super(w);
            this.action = action;
            this.arg = arg;
            this.selector = selector;
        }

        // ---------------------------------------------------------------------
        public void clicked(InputEvent event, float x, float y) {
            // Action button should be owner of the click 
            this.owner = this.selector.action_buttons[this.action];

            // Set selected level name
            if(this.action == LevelSelectorWidget.CLICK_ACTION_RUN) {
                if(UserMan.p.IsOpen(this.arg)) {
                    this.selector.selected_level_name = this.arg;
                }

                ((ButtonWidget)this.owner).SetDisabled(!UserMan.p.IsOpen(this.arg));
            }

            // Run common click method
            super.clicked(event, x, y);
        }
    }

    // =========================================================================
    // LevelSelectorWidget
    public static int CLICK_ACTION_PREV = 0;
    public static int CLICK_ACTION_NEXT = 1;
    public static int CLICK_ACTION_RUN = 2;

    // -------------------------------------------------------------------------
    private MenuWidget sheet_menu, action_menu;
    private WindowWidget parent_wnd;
    private ProgressBarWidget left_progress, right_progress; 
    private ButtonWidget.Cfg button_cfg, icon_cfg;
    private Array<Sheet> sheet;
    private Set<String> level_name;
    private Utils.Vector2i button_cnt_per_sheet, normal_pos;
    private int current_sheet_idx, level_button_cnt_per_sheet;

    protected String selected_level_name;
    protected ButtonWidget [] action_buttons;

    // -------------------------------------------------------------------------
    public LevelSelectorWidget(XmlReader.Element cfg, Utils.Vector2i button_size, 
      WindowWidget wnd, CommonObject.CommonTexture progress_tx) {
        super(Obj.WIDGET.LEVEL_SELECTOR);

        // Parent window
        this.parent_wnd = wnd;

        // Level-selector config
        cfg = CfgReader.Read(CfgReader.GetAttrib(cfg, "path"));

        // Button config
        this.button_cfg = new ButtonWidget.Cfg(
          CfgReader.Read(CfgReader.GetAttrib(cfg, "config:buttons")));

        // Button size with gap (+5% from each side)
        int gap = (int)(button_size.x * 0.05f); 
        int button_gap_width = button_size.x + gap * 2;
        int button_gap_height = button_size.y + gap * 2;

        // Button per sheet
        this.button_cnt_per_sheet = new Utils.Vector2i(
          this.parent_wnd.GetClientRect().width / button_gap_width, 
          this.parent_wnd.GetClientRect().height / button_gap_height);

        // Button count on sheet (reserve space for prev/next buttons)
        this.level_button_cnt_per_sheet = this.button_cnt_per_sheet.x * 
          this.button_cnt_per_sheet.y - 2; 

        // Unique level names
        this.level_name = new HashSet<String>();

        // Read sheet config
        this.ReadSheetData(CfgReader.GetChild(cfg, "level-pack"), progress_tx);

        // Calculate normal position 
        this.normal_pos = new Utils.Vector2i(
          (this.parent_wnd.GetClientRect().width - button_gap_width * this.button_cnt_per_sheet.x) / 2,
          (this.parent_wnd.GetClientRect().height - button_gap_height * this.button_cnt_per_sheet.y) / 2);

        // Invisible action buttons PREV/NEXT/RUN
        this.AddChild(this.action_menu = new MenuWidget(3, 1, 16, 16, 1))
          .SetPos(this.normal_pos.x + gap, this.normal_pos.y - this.action_menu.GetSize().y);
        this.action_buttons = new ButtonWidget [] {
          (ButtonWidget) UiMan.p.AddMenuButton(this.button_cfg, this.action_menu, 0, 0, 
            Obj.UI.LEVEL_SELECTOR_PREV, "dummy", ButtonWidget.ST_UP).SetAlpha(0.0f),
          (ButtonWidget) UiMan.p.AddMenuButton(this.button_cfg, this.action_menu, 1, 0, 
            Obj.UI.LEVEL_SELECTOR_NEXT, "dummy", ButtonWidget.ST_UP).SetAlpha(0.0f),
          (ButtonWidget) UiMan.p.AddMenuButton(this.button_cfg, this.action_menu, 2, 0, 
            Obj.UI.LEVEL_SELECTOR_RUN, "dummy", ButtonWidget.ST_UP).SetAlpha(0.0f)
        };

        // Fill sheets with buttons
        this.CreateSheetButtons(button_size, gap);
    }

    // -------------------------------------------------------------------------
    private void ReadSheetData(XmlReader.Element cfg, CommonObject.CommonTexture progress_tx) {
        // Read level packs
        this.sheet = new Array<Sheet>();
        for(int i = 0; i < cfg.getChildCount(); i++) {
            // Level pack
            XmlReader.Element level_pack_cfg = cfg.getChild(i);

            // Calculate number sheets
            int level_cnt = level_pack_cfg.getChildCount();
            int local_sheet_cnt = level_cnt / this.level_button_cnt_per_sheet + 
              ((level_cnt % this.level_button_cnt_per_sheet == 0) ? 0 : 1);

            // Fill sheets
            int new_sheet_cnt = 0; Sheet sh = null;
            for(int j = 0; j < level_cnt; j++) {
                // Create new sheet
                if(sh == null || sh.level_name.size == this.level_button_cnt_per_sheet) {
                    // Sheet name
                    String sheet_name = level_pack_cfg.getName();
                    if(local_sheet_cnt > 1) {
                        sheet_name += 
                          String.format(" (%d of %d)", ++new_sheet_cnt, local_sheet_cnt);
                    }

                    // Create new sheet
                    int sheet_idx = this.sheet.size;
                    this.sheet.add(sh = new Sheet(sheet_name, 
                      sheet_idx * this.button_cnt_per_sheet.x + sheet_idx, 
                      this.button_cnt_per_sheet.y - 1));
                }

                // Add level name to sheet
                XmlReader.Element lvl = level_pack_cfg.getChild(j);
                if(CfgReader.IsCommented(lvl)) continue;
                String name = CfgReader.GetAttrib(lvl, "name"); 
                sh.AddLevel(name);
                this.level_name.add(name);
            }
        }

        // Create progress widgets
        if(this.sheet.size > 1) {
            Utils.Recti rect = this.parent_wnd.GetClientRect();
            int header_size = this.parent_wnd.GetHeaderSize();
            int size = (int)(header_size * 0.6f); // 60% of the header
            int offset = (header_size - size) / 2;

            this.left_progress = new ProgressBarWidget(progress_tx, this.sheet.size - 1, 1, size, size, true);
            this.right_progress = new ProgressBarWidget(progress_tx, this.sheet.size - 1, 1, size, size, false);

            int y_pos = rect.y + rect.height + offset + 1;
            this.AddChild(this.left_progress).SetPos(rect.x + offset, y_pos);
            this.AddChild(this.right_progress).SetPos(rect.x + rect.width - offset - this.right_progress.GetSize().x, y_pos);

            this.left_progress.SetProgress(0);
            this.right_progress.SetProgress(this.sheet.size - 1);
        }

        // Debug dump
        for(int i = 0; i < this.sheet.size; i++) {
            Sheet sh = this.sheet.get(i);
            Logger.d("Select level sheet :: [name=%s] [cnt=%d] [pos=%d:%d]", 
              sh.name, sh.level_name.size, sh.pos.x, sh.pos.y);
            for(int j = 0; j < sh.level_name.size; j++) {
                Logger.d("         [name=%s]", sh.level_name.get(j));
            }
        }
    }

    // -------------------------------------------------------------------------
    private void CreateSheetButtons(Utils.Vector2i button_size, int gap) {
        // Create level icons
        this.icon_cfg = this.CreateIconCfg(this.button_cfg.size, 512, 512);

        // Menu widget
        this.AddChild(
          this.sheet_menu = new MenuWidget(
            this.sheet.size * this.button_cnt_per_sheet.x + this.sheet.size - 1, 
            this.button_cnt_per_sheet.y, button_size.x, button_size.y, gap))
          .SetPos(this.normal_pos.x, this.normal_pos.y);

        // Fill buttons
        for(int i = 0; i < this.sheet.size; i++) {
            Sheet sh = this.sheet.get(i);

            // Prev button
            ButtonWidget b_prev = this.sheet_menu.AddButton(this.button_cfg, sh.pos.x, 
              this.button_cnt_per_sheet.y - 1, "prev", 
              (i == 0) ? ButtonWidget.ST_DISABLED : ButtonWidget.ST_UP);
            b_prev.RegisterInputListener(
              new ClickInputHandler(this, b_prev, LevelSelectorWidget.CLICK_ACTION_PREV, null));

            // Next button
            ButtonWidget b_next = this.sheet_menu.AddButton(
              this.button_cfg, sh.pos.x + this.button_cnt_per_sheet.x - 1, 
              this.button_cnt_per_sheet.y - 1, "next", 
              (i == this.sheet.size - 1) ? ButtonWidget.ST_DISABLED : ButtonWidget.ST_UP);
            b_next.RegisterInputListener(
              new ClickInputHandler(this, b_next, LevelSelectorWidget.CLICK_ACTION_NEXT, null));

            // Level buttons
            for(int j = 0; j < sh.level_name.size; j++) {
                int x = j % this.button_cnt_per_sheet.x;
                int y = j / this.button_cnt_per_sheet.x;
                if(y == this.button_cnt_per_sheet.y - 1) x++;

                // Create button
                String level_name = sh.level_name.get(j); 
                int button_state = (UserMan.p.IsOpen(level_name)) ? 
                  ButtonWidget.ST_UP : ButtonWidget.ST_DISABLED;
                ButtonWidget b = this.sheet_menu.AddButton(this.button_cfg, this.icon_cfg,
                  x + sh.pos.x, y, level_name, button_state);

                // Register button input handler
                b.RegisterInputListener(
                  new ClickInputHandler(
                    this, b, LevelSelectorWidget.CLICK_ACTION_RUN, sh.level_name.get(j)));
            }
        }
    }

    // -------------------------------------------------------------------------
    public ButtonWidget.Cfg CreateIconCfg(Utils.Vector2i icon_size, int tx_width, int tx_height) {
        // Texture pixmap
        CommonObject.CommonPixmap pm_icon = null, pm = 
          new CommonObject.CommonPixmap(tx_width, tx_height, Pixmap.Format.RGBA8888);

        // Icon config
        ButtonWidget.Cfg cfg = new ButtonWidget.Cfg(
          new CommonObject.CommonTexture(pm.obj), icon_size.x, icon_size.y);

        // Fill icon cfg
        Utils.Vector2i pos = new Utils.Vector2i(), pos_next = new Utils.Vector2i();
        int i = 0; for(String name: this.level_name) {
            // Icon position in texture
            int i2 = (i++) * 2;
            pos.Set(i2 % cfg.buttons_per_line, i2 / cfg.buttons_per_line);
            pos_next.Set((i2 + 1) % cfg.buttons_per_line, (i2 + 1) / cfg.buttons_per_line);

            // Button descriptor
            ButtonWidget.Descriptor desc = new ButtonWidget.Descriptor(name);
            cfg.list.add(desc);

            // Read icon pixmap
            String icon_path = String.format("level/%s/icon.tga", name);
            pm_icon = (pm_icon == null) ? new CommonObject.CommonPixmap(icon_path) : 
              pm_icon.Read(icon_path);

            // Validate icon size 
            Utils.Assert(pm_icon.obj.getWidth() == icon_size.x && 
              pm_icon.obj.getHeight() == icon_size.y, "Invalid icon size :: [name=%s]", name);

            // Write normal icon texture
            cfg.tx.obj.draw(pm_icon.obj, pos.x * icon_size.x, pos.y * icon_size.y);
            desc.AddButtonState(ButtonWidget.ST_UP, i2, cfg.tx.obj, cfg.buttons_per_line, cfg.size_rel);

            // Write disabled icon texture
            pm_icon.Multiply(0.6f, 0.6f, 0.6f, 0.9f);
            cfg.tx.obj.draw(pm_icon.obj, pos_next.x * icon_size.x, pos_next.y * icon_size.y);
            desc.AddButtonState(ButtonWidget.ST_DISABLED, i2 + 1, cfg.tx.obj, cfg.buttons_per_line, cfg.size_rel);
        }

        // Cleanup
        pm.Dispose();
        pm_icon.Dispose();
        return cfg;
    }

    // -------------------------------------------------------------------------
    public void Dispose() {
        MenuWidget [] m_arr = new MenuWidget [] { this.sheet_menu };
        for(MenuWidget m: m_arr) {
            for(int y = 0; y < m.grid_size.y; y++) {
                for(int x = 0; x < m.grid_size.x; x++) {
                    CommonWidget w = m.GetWidget(x, y);
                    if(w != null) w.Dispose();
                }
            }
            m.Dispose();
        }

        this.action_menu.Dispose();
        this.button_cfg.Dispose();
        this.icon_cfg.Dispose();
        this.left_progress.Dispose();
        this.right_progress.Dispose();
        super.Dispose();
    }

    // -------------------------------------------------------------------------
    public String GetSelectedLevelName() {
        return this.selected_level_name;
    }

    // -------------------------------------------------------------------------
    public TaskMan.Task NextSheet() {
        return this.Scroll(this.current_sheet_idx + 1);
    }

    // -------------------------------------------------------------------------
    public TaskMan.Task PrevSheet() {
        return this.Scroll(this.current_sheet_idx - 1);
    }

    // -------------------------------------------------------------------------
    public TaskMan.Task Scroll(int idx) {
        if(idx < 0 || idx >= this.sheet.size) return null;

        this.left_progress.SetProgress(idx);
        this.right_progress.SetProgress(this.sheet.size - idx - 1);

        // Uncheck buttons
        this.sheet_menu.UncheckButtons();

        // Chnage window title
        Sheet sh = this.sheet.get(this.current_sheet_idx = idx);
        this.parent_wnd.SetTitle(String.format("Select level :: %s", sh.name));

        // Scroll to new sheet
        return this.sheet_menu.Scroll(sh.pos.x, sh.pos.y, 
          this.normal_pos.x, this.normal_pos.y);
    }
}
