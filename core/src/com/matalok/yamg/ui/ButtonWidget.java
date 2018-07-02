// -----------------------------------------------------------------------------
package com.matalok.yamg.ui;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.matalok.yamg.CfgReader;
import com.matalok.yamg.CommonObject;
import com.matalok.yamg.Logger;
import com.matalok.yamg.Obj;
import com.matalok.yamg.Utils;

// -----------------------------------------------------------------------------
public class ButtonWidget extends CommonWidget {
    // =========================================================================
    // Descriptor
    public static class Descriptor {
        // ---------------------------------------------------------------------
        String name;
        TextureRegion tx_region[];

        // ---------------------------------------------------------------------
        public Descriptor(String name) {
            this.name = name;
            this.tx_region = new TextureRegion[ButtonWidget.ST_CNT];
        }

        // ---------------------------------------------------------------------
        public void SetTxRegion(Texture tx, int state, float x0, float y0, float x1, float y1) {
            this.tx_region[state] = new TextureRegion(tx, x0, y0, x1, y1);
        }

        // ---------------------------------------------------------------------
        public void AddButtonState(int state, int idx, Texture tx, int buttons_per_line, 
          Utils.Vector2f button_size) {
            int y = idx / buttons_per_line;
            int x = idx % buttons_per_line;
            float x_rel = x * button_size.x; 
            float y_rel = y * button_size.y;
            this.SetTxRegion(tx, state, x_rel, y_rel, 
              x_rel + button_size.x, y_rel + button_size.y);
        }

        // ---------------------------------------------------------------------
        public TextureRegionDrawable GetTxRegion(int idx) {
            if(this.tx_region[idx] == null) {
                return null;
            } else {
                return new TextureRegionDrawable(this.tx_region[idx]);
            }
        }
    }

    // =========================================================================
    // Cfg
    public static class Cfg {
        // ---------------------------------------------------------------------
        public String path;
        public CommonObject.CommonTexture tx;
        public Array<ButtonWidget.Descriptor> list;
        public ButtonWidget.Descriptor background;
        public Utils.Vector2i size;
        public Utils.Vector2f size_rel;
        public int buttons_per_line;

        // ---------------------------------------------------------------------
        public Cfg(XmlReader.Element cfg) {
            this(
              new CommonObject.CommonTexture(CfgReader.GetAttrib(cfg, "general:path")), 
              Utils.Str2Int(CfgReader.GetAttrib(cfg, "general:width")),
              Utils.Str2Int(CfgReader.GetAttrib(cfg, "general:height")));

            Logger.d(Logger.MOD_UI, "Creating button descriptor :: [path=%s] [width=%d] [height=%d]", 
              this.path, this.size.x, this.size.y);

            // Read button background descriptor
            this.background = this.CreateDesc(
              CfgReader.GetChild(cfg, "background"), this.tx.obj);

            // Read button descriptor
            cfg = CfgReader.GetChild(cfg, "button");
            for(int i = 0; i < cfg.getChildCount(); i++) {
                XmlReader.Element button_cfg = cfg.getChild(i);
                ButtonWidget.Descriptor desc = this.CreateDesc(button_cfg, this.tx.obj);
                this.list.add(desc);
            }
        }

        // ---------------------------------------------------------------------
        public Cfg(CommonObject.CommonTexture tx, int button_width, int button_height) {
            // General config
            this.size = new Utils.Vector2i(button_width, button_height);

            // Read texture
            this.tx = tx;
            this.tx.obj.setFilter(TextureFilter.Linear, TextureFilter.Linear);

            // Button relative size
            this.size_rel = new Utils.Vector2f(
              this.size.x / (float)this.tx.obj.getWidth(),
              this.size.y / (float)this.tx.obj.getHeight());

            // Number of buttons in horizontal line
            this.buttons_per_line = this.tx.obj.getWidth() / this.size.x;

            // Descriptor list
            this.list = new Array<ButtonWidget.Descriptor>();
        }

        // ---------------------------------------------------------------------
        protected ButtonWidget.Descriptor CreateDesc(XmlReader.Element cfg, Texture tx) {
            // Button descriptor
            ButtonWidget.Descriptor desc = 
              new ButtonWidget.Descriptor(cfg.getName());

            // Fill texture region for each state
            for(int i = 0; i < cfg.getChildCount(); i++) {
                // Read button state cfg entry
                XmlReader.Element e = cfg.getChild(i);
                if(CfgReader.IsCommented(e)) continue;

                // Fill button descriptor
                desc.AddButtonState(
                  ButtonWidget.ST_GROUP.GetEntryIdx(CfgReader.GetAttrib(e, "state"), true),
                  Utils.Str2Int(CfgReader.GetAttrib(e, "idx")), 
                  tx, this.buttons_per_line, this.size_rel);
            }
            return desc;
        }

        // ---------------------------------------------------------------------
        public ButtonWidget.Descriptor FindButtonDesc(String name) {
            for(int i = 0; i < this.list.size; i++) {
                ButtonWidget.Descriptor desc = this.list.get(i);
                if(desc.name.equals(name)) return desc;
            }
            return null;
        }

        // ---------------------------------------------------------------------
        public void Dispose() {
            this.tx.Dispose();
        }
    }

    // =========================================================================
    // ButtonWidget
    public static final int ST_UP = 0;
    public static final int ST_DOWN = 1;
    public static final int ST_DISABLED = 2;
    public static final int ST_CNT = 3;
    public static final Obj.Group ST_GROUP = new Obj.Group("button-state", 
      new String [] {"up", "down", "disabled"});

   // -------------------------------------------------------------------------
    public ImageButton button;

    // -------------------------------------------------------------------------
    public ButtonWidget(Descriptor back_desc, Descriptor button_desc) {
        // Button widget constructor
        super(Obj.WIDGET.BUTTON, CommonWidget.INPUT_CLICK);

        // Style
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();

        // Fill background style
        if(back_desc != null) {
            style.up = back_desc.GetTxRegion(ButtonWidget.ST_UP);
            style.down = back_desc.GetTxRegion(ButtonWidget.ST_DOWN);
            style.checked = back_desc.GetTxRegion(ButtonWidget.ST_DOWN);
            style.disabled = back_desc.GetTxRegion(ButtonWidget.ST_DISABLED);
        }

        // Fill image style
        if(button_desc != null) {
            style.imageUp = button_desc.GetTxRegion(ButtonWidget.ST_UP);
            style.imageDown = button_desc.GetTxRegion(ButtonWidget.ST_DOWN);
            style.imageChecked = button_desc.GetTxRegion(ButtonWidget.ST_DOWN);
            style.imageDisabled = button_desc.GetTxRegion(ButtonWidget.ST_DISABLED);
        }

        // Finalize image button
        this.SetActor(this.button = new ImageButton(style));
        this.size.Set((int)this.actor.getWidth(), (int)this.actor.getHeight());
    }

    // -------------------------------------------------------------------------
    public void SetButton(boolean value) { this.button.setChecked(value); }
    public void SetDisabled(boolean value) { this.button.setDisabled(value); }
    public boolean IsDisabled() { return this.button.isDisabled(); };
}
