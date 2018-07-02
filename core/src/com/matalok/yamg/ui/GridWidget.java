// -----------------------------------------------------------------------------
package com.fomin.yamg.ui;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.esotericsoftware.tablelayout.Cell;
import com.fomin.yamg.CommonTask;
import com.fomin.yamg.Obj;
import com.fomin.yamg.TaskMan;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public class GridWidget extends CommonWidget {
    // =========================================================================
    // CellWidget
    public class CellWidget {
        // ---------------------------------------------------------------------
        public Cell cell;
        public CommonWidget w;
        public Utils.Vector2i offset;

        // ---------------------------------------------------------------------
        public CellWidget(Cell c) {
            this.cell = c;
            this.offset = new Utils.Vector2i();
        }

        // ---------------------------------------------------------------------
        public void SetWidget(CommonWidget w) {
            this.w = w; this.cell.setWidget(w.actor);
        }

        // ---------------------------------------------------------------------
        public void SetActor(Actor a) {
            this.w = null; this.cell.setWidget(a);
        }
    }

    // =========================================================================
    // GridWidget
    private Table tbl;
    private CellWidget [][] cell;
    protected Utils.Vector2i grid_size, cell_size;

    // -------------------------------------------------------------------------
    public GridWidget(int x_cnt, int y_cnt, int pad, int cell_width, int cell_height) {
        this(Obj.WIDGET.GRID, x_cnt, y_cnt, pad, cell_width, cell_height);
    }

    // -------------------------------------------------------------------------
    public GridWidget(int type, int x_cnt, int y_cnt, int pad, int cell_width, 
      int cell_height) {
        // Menu widget constructor
        super(type);

        // Args
        this.grid_size = new Utils.Vector2i(x_cnt, y_cnt);
        this.cell_size = new Utils.Vector2i(cell_width + 2 * pad, cell_height + 2 * pad);

        // Table
        this.tbl = (Table) this.SetActor(new Table());
        this.cell = new CellWidget[x_cnt][y_cnt];
        for(int y = 0; y < y_cnt; y++) {
            for(int x = 0; x < x_cnt; x++) {
                this.cell[x][y] = new CellWidget(tbl.add());
            }
            tbl.row();
        }

        // Cell
        this.SetCellSize(cell_width, cell_height, pad);
    }

    // -------------------------------------------------------------------------
    public void SetCellSize(int width, int height, int pad) {
        // Cell size
        this.cell_size.Set(width + 2 * pad, height + 2 * pad);

        // Update cells
        for(int y = 0; y < this.grid_size.y; y++) {
            for(int x = 0; x < grid_size.x; x++) {
                CellWidget c = this.cell[x][y];
                c.cell.size(width, height).pad(pad);
                c.offset.Set(
                  x * this.cell_size.x, 
                  (this.grid_size.y - y - 1) * this.cell_size.y);
            }
        }

        // Widget dimensions 
        this.size.Set(
          this.cell_size.x * this.grid_size.x, 
          this.cell_size.y * this.grid_size.y);
        this.origin.Set(this.size.x / 2, this.size.y / 2);
    }

    // -------------------------------------------------------------------------
    public void SetWidget(int x, int y, CommonWidget w) { this.cell[x][y].SetWidget(w); }
    public void SetWidget(int x, int y, Actor a) { this.cell[x][y].SetActor(a); }
    public CommonWidget GetWidget(int x, int y) { return this.cell[x][y].w; }
    public Utils.Vector2i GetOffset(int x, int y) { return this.cell[x][y].offset; }
    public Utils.Vector2i GetGridSize() { return this.grid_size; }
    public Utils.Vector2i GetCellSize() { return this.cell_size; }

    // -------------------------------------------------------------------------
    public TaskMan.Task Scroll(int x, int y, int offset_x, int offset_y) {
        Utils.Vector2i pos = this.cell[x][y].offset;
        return new CommonTask.MovePtpTask(
          this, new Utils.Vector2f(
            offset_x - pos.x, 
            offset_y - pos.y), 
          UiMan.p.wnd_scroll.GetDuration(), 
          Utils.ID_UNDEFINED, true);
    }
}
