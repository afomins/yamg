// -----------------------------------------------------------------------------
package com.matalok.yamg.ui;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.matalok.yamg.Obj;

// -----------------------------------------------------------------------------
public class ContainerWidget extends CommonWidget {
    // -------------------------------------------------------------------------
    protected WidgetGroup group;

    // -------------------------------------------------------------------------
    public ContainerWidget() {
        this(Obj.WIDGET.CONTAINER);
    }

    // -------------------------------------------------------------------------
    public ContainerWidget(int type) {
        super(type);
        this.group = (WidgetGroup) this.SetActor(new WidgetGroup());
    }

    // -------------------------------------------------------------------------
    public CommonWidget AddChild(CommonWidget w) {
        this.AddChild(w.actor);
        return w;
    }

    // -------------------------------------------------------------------------
    public void AddChild(Actor a) {
        this.group.addActor(a);
    }
}
