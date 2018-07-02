// -----------------------------------------------------------------------------
package com.fomin.yamg.fallingsand;

//-----------------------------------------------------------------------------
import com.badlogic.gdx.math.MathUtils;
import com.fomin.yamg.Map2d;
import com.fomin.yamg.Utils;
import com.fomin.yamg.pixelmap.Pixelmap;

// -----------------------------------------------------------------------------
public class FallingSand {
    // -------------------------------------------------------------------------
    public static final byte OP_CUT = 0;
    public static final byte OP_GROW = 1;

    // -------------------------------------------------------------------------
    private static Block[] neigh_blocks = new Block[3]; // middle/left/right

    // -------------------------------------------------------------------------
    private Pixelmap pm;
    private float visibility_thresh;

    private Material [] material;
    private Block [][] block;
    private Block.UpdateDescriptorAl update_list;
    private Block.ActiveDescriptorAdl active_list;

    // -------------------------------------------------------------------------
    public FallingSand(Pixelmap pm, Material[] material, float visibility_thresh) {
        // Pixelmap
        this.pm = pm;
        this.visibility_thresh = visibility_thresh;

        // Material
        this.material = material;

        // Block array
        int w = this.pm.GetSize().x, h = this.pm.GetSize().y;
        this.block = new Block[w][h];
        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                this.block[x][y] = new Block(x, y, this.material[Material.T_EMPTY]);
            }
        }

        // Update and active lists
        int block_cnt = w * h;
        this.update_list = new Block.UpdateDescriptorAl(block_cnt);
        this.active_list = new Block.ActiveDescriptorAdl(block_cnt);
    }

    // -------------------------------------------------------------------------
    public int GetActiveCnt() { return this.active_list.primary.cnt; }
    public int GetUpdateCnt() { return this.update_list.cnt; }

    // -------------------------------------------------------------------------
    public void UpdateBlockMaterial(int x, int y, Material material, float opacity) {
        // Set type/opacity
        Block b = this.block[x][y];
        b.update_desc.material = material;
        b.update_desc.opacity = opacity;

        // Add to update list
        if(!b.update_desc.InActiveList()) {
            this.update_list.Append(b.update_desc);
        }
    }

    // -------------------------------------------------------------------------
    public void UpdateBlockMaterial(Map2d<Byte> map) {
        // Get trim context
        Map2d.TrimContext trim_ctx = Map2d.GetTrimContext(map.size, this.pm.GetSize(), 0, 0);
        if(trim_ctx == null) return;

        // Update all blocks in the map
        for(int y = trim_ctx.bottom; y <= trim_ctx.top; y++) {
            for(int x = trim_ctx.left; x <= trim_ctx.right; x++) {
                byte material_type = map.data[x][y].val;
                Material material = this.material[material_type];
                this.UpdateBlockMaterial(x, y, material, material.alpha);
            }
        }
    }

    // -------------------------------------------------------------------------
    public void UpdateBlockMaterial(int pos_x, int pos_y, Map2d<Float> pattern, 
      byte op, byte material_type) {
        for(int i = 0; i < pattern.active_cnt; i++) {
            // Get map entry
            Map2d.Entry<Float> e = pattern.active[i];
            int x = pos_x + e.x, y = pos_y + e.y;

            // Ignore invalid coordinates
            if(y < 0 || y >= this.pm.GetSize().y) {
                continue;
            }

            // Wrap X coordinate
            if(x < 0 || x >= this.pm.GetSize().x) {
                x = this.WrapX(x);
            }

            // Perform operation
            Block b = this.block[x][y];
            float new_opacity = b.opacity;
            Material new_material = this.material[material_type];
            switch(op) {
                // -------------------------------------------------------------
                case FallingSand.OP_CUT:
                    // Cut only matching blocks
                    if(b.material != new_material) {
                        continue;
                    }

                    new_opacity -= e.val;
                    if(new_opacity <= this.visibility_thresh) {
                        // Block becomes empty
                        new_opacity = 0.0f;
                        new_material = this.material[Material.T_EMPTY];

                        // Activate blocks that are above 
                        Block[] blocks = this.GetNeighbors(Block.AXIS_H, x, y - 1);
                        this.ActivateBlocks(true, blocks, Block.DIR_DOWN);

                        // Activate blocks that are left/right
                        blocks = this.GetNeighbors(Block.AXIS_H, x, y);
                        this.ActivateBlocks(true, blocks, Block.DIR_DOWN);
                    }
                break;

                // -------------------------------------------------------------
                case FallingSand.OP_GROW:
                    new_opacity += e.val;
                    if(new_opacity > 1.0f) {
                        new_opacity = 1.0f;
                    } else if(new_opacity < this.visibility_thresh) {
                        new_opacity = this.visibility_thresh;
                    }
                break;
            }

            // Update block
            this.UpdateBlockMaterial(x, y, new_material, new_opacity);
        }
    }

    // -------------------------------------------------------------------------
    public void FinalizeBlockMaterial() {
        for(int i = 0; i < this.update_list.cnt; i++) {
            // Get update context
            Block.UpdateDescriptor upd = (Block.UpdateDescriptor)this.update_list.data[i];
            Block b = upd.owner; 
            Material new_material = upd.material, old_material = b.material;
            float new_opacity = upd.opacity;

            // Clear non-empty old block
            if(new_material != old_material && !old_material.IsEmpty()) {
                this.SetPixel(b.x, b.y, old_material, 0.0f);
            }

            // Set new block
            if(!new_material.IsEmpty()) {
                this.SetPixel(b.x, b.y, new_material, new_opacity);
            }

            // Finalize block update
            upd.Finalize();
        }

        // Reset update list
        this.update_list.Reset();
    }

    // -------------------------------------------------------------------------
    private void SetPixel(int x, int y, Material material, float opacity) {
        this.pm.SetPixel(material.linked_layer_id, x, y, opacity);
    }

    // -------------------------------------------------------------------------
    private void ActivateBlock(boolean is_primary, Block b, byte dir) {
        if(b.active_desc.InActiveList()) {
            return;
        }

        Utils.ActiveList l = (is_primary) ? this.active_list.primary : this.active_list.secondary;
        l.Append(b.active_desc);
        b.active_desc.Start(b.material.delay, b.material.min_delay, dir);
    }

    // -------------------------------------------------------------------------
    private void ActivateBlocks(boolean is_primary, Block[] blocks, byte dir) {
        for(int j = 0; j < blocks.length; j++) {
            Block b = blocks[j];
            if(b != null && !b.GetFinalMaterial().IsStatic()) {
                this.ActivateBlock(is_primary, b, dir);
            }
        }
    }

    // -------------------------------------------------------------------------
    public void FinalizeBlockActivity() {
        for(int i = 0; i < this.active_list.primary.cnt; i++) {
            // Get source block
            Block.ActiveDescriptor b_src_desc = (Block.ActiveDescriptor)this.active_list.primary.data[i];
            Block b_src = b_src_desc.owner, b_dst = null;

            // Remove source block from active list
            b_src_desc.RemoveFromActiveList();

            // Check whether delay timeout is over
            if(b_src.active_desc.IsDelayOver()) {
                // Initial source type/opacity
                Material src_material = b_src.GetFinalMaterial();
                float src_opacity = b_src.GetFinalOpacity();

                //
                // Select destination block
                //

                // 1st candidate is empty block one line below 
                Block[] neigh = this.GetNeighbors(Block.AXIS_H, b_src.x, b_src.y + 1);
                FallingSand.RandomizeNeigthbors(neigh);
                b_dst = FallingSand.GetBlockByMaterial(neigh, Material.T_EMPTY);

                // For loose materials 2nd candidate is liquid block one line below
                if(b_dst == null && src_material.IsLoose()) {
                    b_dst = FallingSand.GetBlockByDensity(neigh, Material.DENSITY_LIQUID);
                }

                // For liquid materials 2nd candidate is empty block on the left/right
                // side from the source block
                if(b_dst == null && src_material.IsLiquid()) {
                    // Chose random horizontal direction if not yet chosen
                    if(b_src_desc.dir == Block.DIR_DOWN) {
                        b_src_desc.dir = Block.GetRandomDir(Block.AXIS_H);
                    }

                    // Select neighbor on the same line. Try both directions
                    // Blocks order = [middle, left, right]
                    this.GetNeighbors(Block.AXIS_H, b_src.x, b_src.y);
                    Block left = neigh[1], right = neigh[2];
                    for(int j = 0; j < 2; j++) {
                        // Find empty block in current direction
                        if(b_src_desc.dir == Block.DIR_LEFT && left != null && 
                          left.GetFinalMaterial().IsEmpty()) {
                            b_dst = left;

                        } else if(b_src_desc.dir == Block.DIR_RIGHT && right != null && 
                          right.GetFinalMaterial().IsEmpty()) {
                            b_dst = right;
                        }

                        // Exit if found, otherwise select opposite direction
                        if(b_dst != null) break;
                        b_src_desc.dir = Block.DIR_OPPOSITE[b_src_desc.dir];
                    }
                }

                // If destination was selected then swap source/destination blocks
                if(b_dst != null) {
                    // Slow down when moving in not strict vertical line 
                    if(b_src.x != b_dst.x) { 
                        b_src.ResetTriggerDelay();
                    }

                    // Initial destination type/opacity
                    Material dst_material = b_dst.GetFinalMaterial(); 
                    float dst_opacity = b_dst.GetFinalOpacity();

                    // Set destination block
                    this.UpdateBlockMaterial(b_dst.x, b_dst.y, src_material, src_opacity);
                    b_dst.active_desc.Continue(b_src.active_desc, src_material.accel);

                    // Set source block
                    this.UpdateBlockMaterial(b_src.x, b_src.y, dst_material, dst_opacity);

                    // Activate blocks above the source
                    this.GetNeighbors(Block.AXIS_H, b_src.x, b_src.y - 1);
                    this.ActivateBlocks(false, neigh, Block.DIR_DOWN);

                    // If destination block was not empty then it must be activated
                    if(!dst_material.IsEmpty()) {
                        this.ActivateBlock(false, b_src, Block.DIR_DOWN);
                    }

                    // Liquid materials must activate blocks on the left/right
                    // side from the source block
                    if(src_material.IsLiquid()) {
                        // Get left/right water blocks. 
                        this.GetNeighbors(Block.AXIS_H, b_src.x, b_src.y);

                        // Activate left/right neighbors
                        // Blocks order = [middle, left, right]
                        Block left = neigh[1], right = neigh[2];
                        if(left != null && !left.material.IsStatic() && 
                          left.active_desc.dir == Block.DIR_DOWN) {
                            this.ActivateBlock(false, left, Block.DIR_RIGHT);
                        }

                        if(right != null && !right.material.IsStatic() && 
                          right.active_desc.dir == Block.DIR_DOWN) {
                            this.ActivateBlock(false, right, Block.DIR_LEFT);
                        }
                    }
                }
            } else {
                // Delay is not over yet - block should remain active
                b_dst = b_src;
            }

            // Put active blocks to secondary list
            if(b_dst == null) {
                b_src.active_desc.RemoveFromActiveList();
                b_src_desc.dir = Block.DIR_DOWN;
            } else {
                this.active_list.secondary.Append(b_dst.active_desc);
            }
        }

        // Swap primary/secondary buffer
        this.active_list.Swap();
    }

    // -------------------------------------------------------------------------
    private Block[] GetNeighbors(byte axis, int x, int y) {
        // Clear neighbor array 
        Block[] b = FallingSand.neigh_blocks;
        b[0] = null; b[1] = null; b[2] = null;

        int w = this.pm.GetSize().x;
        int h = this.pm.GetSize().y;
        if(axis == Block.AXIS_H) {
            if(y < 0 || y >= h) return b;

            // Neighbors are not stretched over the map boundaries
            if(x > 1 && x < w - 1) {
                if(x >= 0 && x < w) b[0] = this.block[x][y]; x--;       // middle
                if(x >= 0 && x < w) b[1] = this.block[x][y]; x += 2;    // left
                if(x >= 0 && x < w) b[2] = this.block[x][y];            // right

            // Neighbors are stretched over the map boundaries
            } else {
                x = this.WrapX(x);      b[0] = this.block[x][y];        // middle
                x = this.WrapX(x - 1);  b[1] = this.block[x][y];        // left
                x = this.WrapX(x + 2);  b[2] = this.block[x][y];        // right
            }
        } else {
            x = this.WrapX(x);
            if(y >= 0 && y < h) b[0] = this.block[x][y]; y--;           // middle
            if(y >= 0 && y < h) b[1] = this.block[x][y]; y += 2;        // up
            if(y >= 0 && y < h) b[2] = this.block[x][y];                // down
        }
        return b;
    }

    // -------------------------------------------------------------------------
    public int WrapX(int x) {
        if(x < 0) return x + this.pm.GetSize().x;
        else      return x % this.pm.GetSize().x;
    }

    // -------------------------------------------------------------------------
    public void Stop() {
        this.update_list.Reset();
        this.active_list.primary.Reset();
        this.active_list.secondary.Reset();

        for(int y = 0; y < this.pm.GetSize().y; y++) {
            for(int x = 0; x < this.pm.GetSize().x; x++) {
                this.block[x][y].active_desc.RemoveFromActiveList();
                this.block[x][y].update_desc.RemoveFromActiveList();
            }
        }
    }

    // -------------------------------------------------------------------------
    private static void RandomizeNeigthbors(Block[] b){
        if(MathUtils.randomBoolean()) {
            Block tmp = b[1]; b[1] = b[2]; b[2] = tmp;
        }
    }

    // -------------------------------------------------------------------------
    public static Block GetBlockByMaterial(Block[] blocks, byte type) {
        for(int j = 0; j < blocks.length; j++) {
            Block b = blocks[j];
            if(b != null && b.GetFinalMaterial().type == type) {
                return b;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    public static Block GetBlockByDensity(Block[] blocks, byte density) {
        for(int j = 0; j < blocks.length; j++) {
            Block b = blocks[j];
            if(b != null && b.GetFinalMaterial().density == density) {
                return b;
            }
        }
        return null;
    }
}
