// -----------------------------------------------------------------------------
package com.fomin.yamg.ui;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.fomin.yamg.CommonObject;
import com.fomin.yamg.Obj;

// -----------------------------------------------------------------------------
public class ImageWidget extends CommonWidget {
    // -------------------------------------------------------------------------
    private CommonObject.CommonTexture tx;

    // -------------------------------------------------------------------------
    public ImageWidget(CommonObject.CommonTexture tx)  {
        super(Obj.WIDGET.IMAGE);
        this.tx = tx;
        this.SetActor(new Image(this.tx.obj));
        this.size.Set((int)this.actor.getWidth(), (int)this.actor.getHeight());
    }

    // -------------------------------------------------------------------------
    public ImageWidget(String tx_path) {
        this(new CommonObject.CommonTexture(tx_path));
    }

    // -------------------------------------------------------------------------
    public ImageWidget(String tx_path, int width, int height) {
        this(tx_path);
        this.actor.setSize(width, height);
        this.size.Set(width,  height);
    }

    // -------------------------------------------------------------------------
    public void Dispose() { 
        this.tx.Dispose();
        this.tx = null;
        super.Dispose();
    } 

    // -------------------------------------------------------------------------
    public void Scale(float val) {
        this.actor.setSize(this.actor.getWidth() * val, this.actor.getHeight() * val);
        this.size.Set((int)this.actor.getWidth(), (int)this.actor.getHeight());
    }
}
