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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.matalok.yamg.Obj;
import com.matalok.yamg.CommonObject;

// -----------------------------------------------------------------------------
public class WindowWidget extends ContainerWidget {
    // -------------------------------------------------------------------------
    private CommonObject.CommonTexture tx_border, tx_back;
    private Table tbl;
    private LabelWidget title;
    private int header_size;

    // -------------------------------------------------------------------------
    public WindowWidget(Color border_color, Color back_color, int border_size, 
      int header_size, int width, int height, String title_text) {
        super(Obj.WIDGET.WINDOW);

        // Back & border textures
        this.tx_border = new CommonObject.CommonTexture(2, 2, border_color, Pixmap.Format.RGBA8888);
        this.tx_back = new CommonObject.CommonTexture(2, 2, back_color, Pixmap.Format.RGBA8888);
        this.tx_border.obj.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        this.tx_back.obj.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        // Client area
        this.client_rect.Set(
          border_size, border_size, width - border_size * 2, height - border_size - header_size);

        // Header row
        this.header_size = header_size;
        this.tbl = new Table();
        this.tbl.add(new Image(this.tx_border.obj)).colspan(3).size(width, header_size);

        // Middle row
        this.tbl.row();
        this.tbl.add(new Image(this.tx_border.obj)).size(border_size, this.client_rect.height);
        this.tbl.add(new Image(this.tx_back.obj)).size(this.client_rect.width, this.client_rect.height);
        this.tbl.add(new Image(this.tx_border.obj)).size(border_size, this.client_rect.height);

        // Footer row
        this.tbl.row();
        this.tbl.add(new Image(this.tx_border.obj)).colspan(3).size(width, border_size);

        // Dimensions
        this.tbl.setPosition(width / 2, height / 2);
        this.size.Set(width, height);
        this.origin.Set(0, 0);

        // Fill container
        this.AddChild(this.tbl);

        // Title text
        if(title_text != null) {
            this.title = new LabelWidget(Color.WHITE, "eat-my-shorts");
            this.SetTitle(title_text);
            this.AddChild(this.title);
        }
    }

    // -------------------------------------------------------------------------
    public int GetHeaderSize() { return this.header_size; }

    // -------------------------------------------------------------------------
    public void Dispose() {
        this.tx_back.Dispose();
        this.tx_border.Dispose();
        this.title.Dispose();
        super.Dispose();
    }

    // -------------------------------------------------------------------------
    public void SetTitle(String title) {
        this.title.SetText(title);
        this.title.SetPos(this.size.x / 2 - this.title.GetSize().x / 2,
          this.size.y - this.title.GetSize().y);
    }
}
