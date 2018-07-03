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
package com.matalok.yamg.ui;

// -----------------------------------------------------------------------------
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Version;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.matalok.yamg.CfgReader;
import com.matalok.yamg.CommonObject;
import com.matalok.yamg.Obj;
import com.matalok.yamg.ServiceMan;
import com.matalok.yamg.TaskMan;
import com.matalok.yamg.Timer;
import com.matalok.yamg.Utils;

// -----------------------------------------------------------------------------
public class UiMan extends ServiceMan.Service {
    // -------------------------------------------------------------------------
    public interface IUiListener {
        // ---------------------------------------------------------------------
        public void OnUiTouchDown(CommonWidget w, float x, float y, int pointer, int button);
        public void OnUiTouchUp(CommonWidget w, float x, float y, int pointer, int button);
        public void OnUiTouchDragged(CommonWidget w, float x, float y, int pointer);
        public void OnUiMouseMoved(CommonWidget w, float x, float y);
        public void OnUiEnter(CommonWidget w, float x, float y, int pointer);
        public void OnUiExit(CommonWidget w, float x, float y, int pointer);
        public void OnUiScrolled(CommonWidget w, float x, float y, int amount);
        public void OnUiKeyDown(CommonWidget w, int keycode);
        public void OnUiKeyUp(CommonWidget w, int keycode);
        public void OnUiKeyTyped(CommonWidget w, char character);
        public void OnUiClick(CommonWidget w, float x, float y);
    }

    // -------------------------------------------------------------------------
    public static final int INPUT_CLICK = 0;
    public static final int INPUT_KEY_DOWN = 1;
    public static final int INPUT_KEY_UP = 2;
    public static final int INPUT_KEY_TYPED = 3;
    public static final int INPUT_TOUCH_DOWN = 4;
    public static final int INPUT_TOUCH_UP = 5;
    public static final int INPUT_TOUCH_DRAGGED = 6;
    public static final int INPUT_MOUSE_MOVE = 7;
    public static final int INPUT_SCROLLED = 8;
    public static final int INPUT_ENTER = 9;
    public static final int INPUT_EXIT = 10;
    public static final Obj.Group INPUT_GROUP = new Obj.Group("ui-input", 
      new String [] {"click", "key-down", "key-up", "key-typed", "touch-down", 
        "touch-up", "touch-dragged", "mouse-move", "scrolled", "enter", "exit"});

    // -------------------------------------------------------------------------
    private Stage stage;
    private IUiListener listener;
    public WindowScroller wnd_scroll;
    public Utils.Vector2i screen_size;
    public Map<Integer, MenuWidget> w_menu;
    public Map<Integer, ButtonWidget> w_button;
    public Map<Integer, ImageWidget> w_image;
    public Map<Integer, ProgressBarWidget> w_progress;
    public Map<Integer, ContainerWidget> w_container;
    public Map<Integer, WindowWidget> w_window;
    public Map<Integer, GridWidget> w_grid;
    public Map<Integer, LevelSelectorWidget> w_selector;
    public Map<Integer, LabelWidget> w_label;
    public Array<CommonWidget> widget;

    private ButtonWidget.Cfg main_menu_button_cfg;
    private CommonObject.CommonTexture progress_load_tx, progress_scroll_tx;
    private Utils.Vector2i main_menu_button_size;

    // -------------------------------------------------------------------------
    public UiMan() {
        super(Obj.SERVICE.UI);
    }

    // -------------------------------------------------------------------------
    // Service pointer
    public static UiMan p;
    protected void AcquireServicePointer() { UiMan.p = this; };
    protected void ReleaseServicePointer() { UiMan.p = null; };

    // -------------------------------------------------------------------------
    protected CommonWidget AddWidget(int type, CommonWidget w, boolean is_top) {
        // Menu
        int w_type = w.GetEntryIdx();
             if(type == Utils.ID_UNDEFINED) { /* ignore */ }
        else if(w_type == Obj.WIDGET.MENU) this.w_menu.put(type, (MenuWidget)w);
        else if(w_type == Obj.WIDGET.BUTTON) this.w_button.put(type, (ButtonWidget)w);
        else if(w_type == Obj.WIDGET.IMAGE) this.w_image.put(type, (ImageWidget)w);
        else if(w_type == Obj.WIDGET.PROGRESS_BAR) this.w_progress.put(type, (ProgressBarWidget)w);
        else if(w_type == Obj.WIDGET.CONTAINER) this.w_container.put(type, (ContainerWidget)w);
        else if(w_type == Obj.WIDGET.WINDOW) this.w_window.put(type, (WindowWidget)w);
        else if(w_type == Obj.WIDGET.GRID) this.w_grid.put(type, (GridWidget)w);
        else if(w_type == Obj.WIDGET.LEVEL_SELECTOR) this.w_selector.put(type, (LevelSelectorWidget)w);
        else if(w_type == Obj.WIDGET.LABEL) this.w_label.put(type, (LabelWidget)w);
        else {
            Utils.Assert(false, "Invalid widget type :: [type=%d]", type);
        }

        // Common array
        this.widget.add(w);

        // Only top-level widgets must be added to stage
        if(is_top) {
            this.stage.addActor(w.actor);
        }
        return w;
    }

    // -------------------------------------------------------------------------
    protected ButtonWidget AddMenuButton(ButtonWidget.Cfg cfg, MenuWidget m, int x, int y, int type, 
      String name, int state) {
        return (ButtonWidget) this.AddWidget(type, m.AddButton(cfg, x, y, name, state), false);
    }

    // -------------------------------------------------------------------------
    protected CommonWidget AddContainerChild(ContainerWidget c, CommonWidget w, int type) {
        return this.AddWidget(type, c.AddChild(w), false);
    }

    // -------------------------------------------------------------------------
    protected WindowWidget AddWindow(WindowScroller ws, int type, String title) {
        return (WindowWidget) this.AddWidget(type, ws.CreateWindow(title), true);
    }

    // -------------------------------------------------------------------------
    public InputProcessor GetIp() {
        return stage;
    }

    // -------------------------------------------------------------------------
    public void SetListener(IUiListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    protected boolean HandleInput(int type, CommonWidget w, float x, float y, 
      int pointer, int button, int amount, int keycode, char character) {
        // Input was not processed
        if(this.listener == null) return false;

             if(type == UiMan.INPUT_CLICK) this.listener.OnUiClick(w, x, keycode);
        else if(type == UiMan.INPUT_TOUCH_DOWN) this.listener.OnUiTouchDown(w, x, y, pointer, button);
        else if(type == UiMan.INPUT_TOUCH_UP) this.listener.OnUiTouchUp(w, x, y, pointer, button);
        else if(type == UiMan.INPUT_TOUCH_DRAGGED) this.listener.OnUiTouchDragged(w, x, y, pointer);
        else if(type == UiMan.INPUT_MOUSE_MOVE) this.listener.OnUiMouseMoved(w, x, y);
        else if(type == UiMan.INPUT_ENTER) this.listener.OnUiEnter(w, x, y, pointer);
        else if(type == UiMan.INPUT_EXIT) this.listener.OnUiExit(w, x, y, pointer);
        else if(type == UiMan.INPUT_SCROLLED) this.listener.OnUiScrolled(w, x, y, amount);
        else if(type == UiMan.INPUT_KEY_DOWN) this.listener.OnUiKeyDown(w, keycode);
        else if(type == UiMan.INPUT_KEY_UP) this.listener.OnUiKeyUp(w, keycode);
        else if(type == UiMan.INPUT_KEY_TYPED) this.listener.OnUiKeyTyped(w, character);
        else Utils.Assert(false, "Invalid input :: [type=%d]", type);

        // Input was processed
        return true;
    }

    // -------------------------------------------------------------------------
    protected void OnServiceSetup(XmlReader.Element cfg) {
        // Screen size
        this.screen_size = new Utils.Vector2i(
          Gdx.graphics.getWidth(), 
          Gdx.graphics.getHeight());

        // Stage
        this.stage = new Stage();
        this.stage.setViewport(new ScreenViewport());
        this.stage.getViewport().update(this.screen_size.x, this.screen_size.y, true);
//        this.stage.setViewport(this.screen_size.x, this.screen_size.y, true);

        // Widget array
        this.widget = new Array<CommonWidget>();
        this.w_menu = new HashMap<Integer, MenuWidget>();
        this.w_button = new HashMap<Integer, ButtonWidget>();
        this.w_image = new HashMap<Integer, ImageWidget>();
        this.w_container = new HashMap<Integer, ContainerWidget>();
        this.w_progress = new HashMap<Integer, ProgressBarWidget>();
        this.w_window = new HashMap<Integer, WindowWidget>();
        this.w_grid = new HashMap<Integer, GridWidget>();
        this.w_selector = new HashMap<Integer, LevelSelectorWidget>();
        this.w_label = new HashMap<Integer, LabelWidget>();

        // Button size
        this.main_menu_button_cfg = new ButtonWidget.Cfg(
          CfgReader.Read(CfgReader.GetAttrib(cfg, "menu:path")));

        float scale = UiMan.p.screen_size.y / 5.0f / this.main_menu_button_cfg.size.y;
        this.main_menu_button_size = new Utils.Vector2i(
          (int)(this.main_menu_button_cfg.size.x * scale),
          (int)(this.main_menu_button_cfg.size.y * scale));

        // Main menu
        ButtonWidget.Cfg m_cfg = this.main_menu_button_cfg;
        MenuWidget mw = (MenuWidget) this.AddWidget(Obj.UI.MAIN_MENU, 
          new MenuWidget(1, 18, this.main_menu_button_size.x, this.main_menu_button_size.y, 0), 
          true).SetPos(this.screen_size.x, 0).SetAlpha(1.0f);
        {
            // Buttons
            int i = 0;
            this.AddMenuButton(m_cfg, mw, 0, i++, Utils.ID_UNDEFINED, "empty", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Utils.ID_UNDEFINED, "empty", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Utils.ID_UNDEFINED, "empty",ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_YES, "yes", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_NO, "no", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Utils.ID_UNDEFINED, "empty", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Utils.ID_UNDEFINED, "empty", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Utils.ID_UNDEFINED, "empty",ButtonWidget.ST_UP);

            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_BACK, "back", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_RESET, "reset", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_GAME, "game", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_INFO, "settings", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_QUIT, "quit", ButtonWidget.ST_UP);

            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_MENU, "menu", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_BOMB, "bomb", ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_ACT1, "game-action-01",ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_ACT2, "game-action-02",ButtonWidget.ST_UP);
            this.AddMenuButton(m_cfg, mw, 0, i++, Obj.UI.MAIN_MENU_BUTTON_ACT3, "game-action-03",ButtonWidget.ST_UP);
        }

        // Window scroller
        this.wnd_scroll = new WindowScroller(CfgReader.GetChild(cfg, "window"), 
          new Utils.Recti(0, 0, this.screen_size.x - mw.size.x, this.screen_size.y));

        // Info window
        LabelWidget l = null;
        WindowWidget wnd = this.AddWindow(this.wnd_scroll, Obj.UI.INFO_WND, "Main menu");
        ImageWidget img = new ImageWidget("ui/main-menu-title-wvga.tga");
        img.Scale(scale);
        this.AddContainerChild(wnd, img, Utils.ID_UNDEFINED).SetPos(
          wnd.GetClientRect().width / 2 - img.GetSize().x / 2, 
          wnd.GetClientRect().height - img.GetSize().y - this.main_menu_button_size.y / 2);
        {
            l = new LabelWidget(Color.WHITE, "(c) 2013 Alex Fomins, afomins@gmail.com");
            wnd.AddChild(l).SetPos(4, 2);
            this.AddWidget(Utils.ID_UNDEFINED, l, false);
            int label_height = l.GetSize().y;

            l = new LabelWidget(Color.WHITE, String.format("Libgdx version: %s", Version.VERSION));
            wnd.AddChild(l).SetPos(4, 2 + label_height * 3);
            this.AddWidget(Utils.ID_UNDEFINED, l, false);

            l = new LabelWidget(Color.WHITE, String.format("Yamg! version: %s", Utils.version));
            wnd.AddChild(l).SetPos(4, 2 + label_height * 4);
            this.AddWidget(Utils.ID_UNDEFINED, l, false);

            l = new LabelWidget(Color.WHITE, "-");
            wnd.AddChild(l).SetPos(4, 2 + label_height * 2);
            this.AddWidget(Obj.UI.MEM_STATS_LABEL, l, false);
        }

        // Confirm window
        wnd  = this.AddWindow(this.wnd_scroll, Obj.UI.CONFIRM_WND, "Confirm");
        l = new LabelWidget(Color.WHITE, "Confirm (y/n)");
        wnd.AddChild(l).SetPos(wnd.size.x / 2 - l.size.x / 2, wnd.size.y / 2 - l.size.y / 2);
        this.AddWidget(Utils.ID_UNDEFINED, l, false);

        // Progress-bar texture
        this.progress_load_tx = new CommonObject.CommonTexture("ui/progress-bar-16x16.tga");
        this.progress_scroll_tx = new CommonObject.CommonTexture("ui/progress-bar-2-16x16.tga");

        // Level selector
        wnd = this.AddWindow(this.wnd_scroll, Obj.UI.SELECT_LEVEL_WND, "Select level");
        this.AddContainerChild(
          wnd, 
          new LevelSelectorWidget(CfgReader.GetChild(cfg, "level-selector"), 
            new Utils.Vector2i(this.main_menu_button_size.y, this.main_menu_button_size.y), 
            wnd, this.progress_scroll_tx), 
          Obj.UI.LEVEL_SELECTOR);

        // Loading screen
        ContainerWidget cw = (ContainerWidget) this.AddWidget(Obj.UI.LOAD_SCREEN, 
          new ContainerWidget(), true).SetPos(0, 0).SetAlpha(0.0f);
        this.AddContainerChild(cw, 
          new ImageWidget("ui/splash-screen-800x480.tga", this.screen_size.x, this.screen_size.y), 
            Obj.UI.LOAD_SCREEN_IMAGE).SetPos(0, 0);
        this.AddContainerChild(cw, 
          new ProgressBarWidget(this.progress_load_tx, 6, 6, true), 
            Obj.UI.LOAD_SCREEN_PROGRESS).SetPos(10, 10);

        // XXX: HACK - menu is on the top but splash-screen is even topper :)
        mw.actor.setZIndex(100500);
        cw.actor.setZIndex(100500 + 1);
    }

    // -------------------------------------------------------------------------
    protected void OnServiceTeardown() {
        this.main_menu_button_cfg.Dispose();
        this.progress_load_tx.Dispose();
        this.progress_scroll_tx.Dispose();

        // Clear widget
        for(CommonWidget w: this.widget) {
            w.Dispose();
        }

        // Clear stage
        this.stage.dispose();
        super.OnServiceTeardown();
    }

    // -------------------------------------------------------------------------
    protected void OnServiceRun() {
        this.stage.act(Timer.GetDelta());
        this.stage.draw();
    }
}
