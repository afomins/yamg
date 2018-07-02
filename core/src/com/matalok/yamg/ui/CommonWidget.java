// -----------------------------------------------------------------------------
package com.fomin.yamg.ui;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fomin.yamg.CommonTask;
import com.fomin.yamg.Logger;
import com.fomin.yamg.Obj;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public abstract class CommonWidget extends Obj.CommonObject 
  implements CommonTask.IMoveTaskObject, CommonTask.ITweenTaskObject {
    // =========================================================================
    // CommonInputHandler
    public static class CommonInputHandler extends InputListener {
        // ---------------------------------------------------------------------
        protected CommonWidget owner;

        // ---------------------------------------------------------------------
        public CommonInputHandler(CommonWidget w) {
            this.owner = w;
        }

        // ---------------------------------------------------------------------
        // Input handlers
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) { return UiMan.p.HandleInput(UiMan.INPUT_TOUCH_DOWN, this.owner, x, y, pointer, button, 0, 0, '@'); }
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) { UiMan.p.HandleInput(UiMan.INPUT_TOUCH_UP, this.owner, x, y, pointer, button, 0, 0, '@'); }
        public void touchDragged(InputEvent event, float x, float y, int pointer) { UiMan.p.HandleInput(UiMan.INPUT_TOUCH_DRAGGED, this.owner, x, y, pointer, 0, 0, 0, '@'); }
        public boolean mouseMoved(InputEvent event, float x, float y) { return UiMan.p.HandleInput(UiMan.INPUT_MOUSE_MOVE, this.owner, x, y, 0, 0, 0, 0, '@'); }
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) { UiMan.p.HandleInput(UiMan.INPUT_ENTER, this.owner, x, y, pointer, 0, 0, 0, '@'); }
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) { UiMan.p.HandleInput(UiMan.INPUT_EXIT, this.owner, x, y, pointer, 0, 0, 0, '@'); }
        public boolean scrolled(InputEvent event, float x, float y, int amount) { return UiMan.p.HandleInput(UiMan.INPUT_SCROLLED, this.owner, x, y, 0, 0, amount, 0, '@'); }
        public boolean keyDown(InputEvent event, int keycode) { return UiMan.p.HandleInput(UiMan.INPUT_KEY_DOWN, this.owner, 0.0f, 0.0f, 0, 0, 0, keycode, '@'); }
        public boolean keyUp(InputEvent event, int keycode) { return UiMan.p.HandleInput(UiMan.INPUT_KEY_UP, this.owner, 0.0f, 0.0f, 0, 0, 0, keycode, '@'); }
        public boolean keyTyped (InputEvent event, char character) { return UiMan.p.HandleInput(UiMan.INPUT_KEY_UP, this.owner, 0.0f, 0.0f, 0, 0, 0, 0, character); }
    }

    // =========================================================================
    // ClickInputHandler
    public static class ClickInputHandler extends ClickListener {
        // ---------------------------------------------------------------------
        protected CommonWidget owner;

        // ---------------------------------------------------------------------
        public ClickInputHandler(CommonWidget w) {
            this.owner = w;
        }

        // ---------------------------------------------------------------------
        // Input handlers
        public void clicked(InputEvent event, float x, float y) {
            UiMan.p.HandleInput(UiMan.INPUT_CLICK, this.owner, x, y, 0, 0, 0, 0, '@');
        }
    }

    // =========================================================================
    // CommonWidget
    public static final int INPUT_COMMON = 0;
    public static final int INPUT_CLICK = 1;
    public static final Obj.Group INPUT_GROUP = new Obj.Group("input-type",
      new String [] {"common", "click"});

    public static final int ARG_DURATION = 0;
    public static final int ARG_SPEED = 1;

    public static final int POS_RELATIVE = 0;
    public static final int POS_ABSOLUTE = 1;

    // -------------------------------------------------------------------------
    protected Utils.Vector2i origin, pos, size;
    private Utils.Vector2f posf;
    protected Utils.Recti client_rect;
    protected Actor actor;
    protected InputListener input_listener;

    // -------------------------------------------------------------------------
    public CommonWidget(int type) {
        super(Obj.WIDGET.ptr, type);
        this.pos = new Utils.Vector2i();
        this.posf = new Utils.Vector2f();
        this.origin = new Utils.Vector2i();
        this.size = new Utils.Vector2i();
        this.client_rect = new Utils.Recti();
    }

    // -------------------------------------------------------------------------
    public CommonWidget(int type, int h_input) {
        this(type);

        // Common input handler
        if(h_input == CommonWidget.INPUT_COMMON) {
            this.input_listener = new CommonInputHandler(this);

        // Click input handler
        } else if(h_input == CommonWidget.INPUT_CLICK) {
            this.input_listener = new ClickInputHandler(this);
        }
    }

    // -------------------------------------------------------------------------
    public CommonWidget(int type, InputListener input_listener) {
        this(type);
        this.input_listener = input_listener;
    }

    // -------------------------------------------------------------------------
    public CommonWidget SetPos(int x, int y) {
        this.pos.Set(x, y);
        this.actor.setPosition(x + this.origin.x, y + this.origin.y);
        return this; 
    }

    // -------------------------------------------------------------------------
    public CommonWidget SetAlpha(float alpha) {
        Color c = this.actor.getColor();

        if(c.a == 0.0f && alpha > 0.0f) {
            Logger.d("Making widget visible :: [name=%s]", this.GetObjName());

        } else if(c.a > 0.0f && alpha == 0.0f) {
            Logger.d("Making widget invisible :: [name=%s]", this.GetObjName());
        }

        this.actor.setColor(c.r, c.g, c.b, alpha);
        this.actor.setVisible((alpha > 0.0f));
        return this;
    }

    // -------------------------------------------------------------------------
    public Utils.Vector2i GetSize() { return this.size; }
    public Utils.Vector2i GetPos() { return this.pos; }
    public Utils.Recti GetClientRect() { return this.client_rect; }
    public float GetAlpha() { return this.actor.getColor().a; }

    // -------------------------------------------------------------------------
    protected Actor SetActor(Actor a) {
        // Save actor
        this.actor = a;

        // Register input handler
        this.RegisterInputListener(this.input_listener);
        return this.actor;
    }

    // -------------------------------------------------------------------------
    protected void RegisterInputListener(InputListener listener) {
        // Remove old listener
        this.actor.removeListener(this.input_listener);

        // Register new listener
        this.input_listener = listener;
        if(this.input_listener != null) {
            this.actor.addListener(this.input_listener);
        }
    }

    // -------------------------------------------------------------------------
    // IMoveTaskObject
    public Utils.Vector2f GetMovePos() {
        this.posf.Set((float)this.pos.x, (float)this.pos.y);
        return this.posf; 
    }

    // -------------------------------------------------------------------------
    public void SetMoveRelPos(float x, float y) { this.SetPos(this.pos.x + (int)x, this.pos.y + (int)y); };
    public void SetMoveAbsPos(float x, float y) { this.SetPos((int)x, (int)y); };

    // -------------------------------------------------------------------------
    // ITweenTaskObject
    public float GetTweenValue() { return this.actor.getColor().a; }
    public void SetTweenValue(float val) { this.SetAlpha(val); };
}
