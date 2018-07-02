// -----------------------------------------------------------------------------
package com.fomin.yamg.ui;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.fomin.yamg.Obj;

// -----------------------------------------------------------------------------
public class LabelWidget extends CommonWidget {
    // -------------------------------------------------------------------------
    public Label label;

    // -------------------------------------------------------------------------
    public LabelWidget(Color color, String text) {
        // Button widget constructor
        super(Obj.WIDGET.LABEL, CommonWidget.INPUT_CLICK);

        // Set layer
        this.SetActor(this.label = 
          new Label(text, new Label.LabelStyle(new BitmapFont(), color)));
        this.label.setAlignment(Align.left, Align.center);
        this.SetText(text);
    }

    // -------------------------------------------------------------------------
    public void SetText(String text) { 
        this.label.setText(text);
        this.size.Set((int)this.actor.getWidth(), (int)this.actor.getHeight());
    }
}
