// -----------------------------------------------------------------------------
package com.fomin.yamg.fallingsand;

// -----------------------------------------------------------------------------
import com.badlogic.gdx.math.MathUtils;
import com.fomin.yamg.Timer;
import com.fomin.yamg.Utils;

// -----------------------------------------------------------------------------
public class Block {
    // =========================================================================
    // CommonDescriptor
    private static class CommonDescriptor extends Utils.ActiveListObject {
        // ---------------------------------------------------------------------
        public Block owner;

        // ---------------------------------------------------------------------
        public CommonDescriptor(Block owner) {
            this.owner = owner;
        }
    }
    
    // =========================================================================
    // UpdateDescriptor
    public static class UpdateDescriptor extends CommonDescriptor {
        // ---------------------------------------------------------------------
        public Material material;
        public float opacity;

        // ---------------------------------------------------------------------
        public UpdateDescriptor(Block block) {
            super(block);
        }

        // ---------------------------------------------------------------------
        public boolean MaterialHasChanged() {
            return (this.material != this.owner.material) ? true : false;
        }

        // ---------------------------------------------------------------------
        public void Finalize() {
            this.owner.material = this.material;
            this.owner.opacity = this.opacity;
            this.RemoveFromActiveList();
        }
    }

    // =========================================================================
    // UpdateDescriptorAl
    public static class UpdateDescriptorAl extends Utils.ActiveList {
        // ---------------------------------------------------------------------
        public UpdateDescriptorAl(int size) {
            super(new UpdateDescriptor[size]);
        }
    }

    // =========================================================================
    // ActiveDescriptor
    public static class ActiveDescriptor extends CommonDescriptor {
        // ---------------------------------------------------------------------
        public long start_time, prev_time, delay, min_delay;
        public byte dir;

        // ---------------------------------------------------------------------
        public ActiveDescriptor(Block block) {
            super(block);
            this.dir = Block.DIR_DOWN;
        }

        // ---------------------------------------------------------------------
        public void Start(long delay, long min_delay, byte dir) {
            this.start_time = this.prev_time = Timer.Get();
            this.delay = delay;
            this.min_delay = min_delay;
            this.dir = dir;
        }

        // ---------------------------------------------------------------------
        public void Continue(ActiveDescriptor desc, long acceleration) {
            this.prev_time = Timer.Get();

            this.start_time = desc.start_time;
            this.delay = desc.delay;
            this.min_delay = desc.min_delay;

            if(this.delay > this.min_delay) {
                this.delay -= acceleration;
                if(this.delay < this.min_delay) {
                    this.delay = this.min_delay;
                }
            }
            this.dir = desc.dir;
        }

        // ---------------------------------------------------------------------
        public boolean IsDelayOver() {
            return (Timer.Get() >= this.prev_time + this.delay) ? true : false;
        }

        // ---------------------------------------------------------------------
        public void Finalize() {
        }
    }

    // =========================================================================
    // ActiveDescriptorAl
    public static class ActiveDescriptorAl extends Utils.ActiveList {
        // ---------------------------------------------------------------------
        public ActiveDescriptorAl(int size) {
            super(new ActiveDescriptor[size]);
        }
    }

    // =========================================================================
    // ActiveDescriptorAdl
    public static class ActiveDescriptorAdl extends Utils.ActiveDoubleList {
        // ---------------------------------------------------------------------
        public ActiveDescriptorAdl(int size) {
            super(new ActiveDescriptorAl(size), new ActiveDescriptorAl(size));
        }
    }

    // =========================================================================
    // Block
    public static final byte DIR_LEFT = 0;
    public static final byte DIR_RIGHT = 1;
    public static final byte DIR_UP = 2;
    public static final byte DIR_DOWN = 3;
    public static final byte DIR_OPPOSITE[] = new byte[] {DIR_RIGHT, DIR_LEFT, DIR_DOWN, DIR_UP};

    // -------------------------------------------------------------------------
    public static final byte AXIS_H = 0;
    public static final byte AXIS_V = 1;

    // -------------------------------------------------------------------------
    public int x, y;
    public Material material;
    public float opacity;

    public UpdateDescriptor update_desc;
    public ActiveDescriptor active_desc;

    // -------------------------------------------------------------------------
    public Block(int x, int y, Material material) {
        this.x = x; this.y = y;
        this.material = material;
        this.update_desc = new Block.UpdateDescriptor(this);
        this.active_desc = new Block.ActiveDescriptor(this);
    }

    // -------------------------------------------------------------------------
    public Material GetFinalMaterial() {
        return (this.update_desc.InActiveList()) ? this.update_desc.material : this.material;
    }

    // -------------------------------------------------------------------------
    public float GetFinalOpacity() {
        return (this.update_desc.InActiveList()) ? this.update_desc.opacity : this.opacity;
    }

    // -------------------------------------------------------------------------
    public void ResetTriggerDelay() {
        this.active_desc.delay =  this.material.delay;
    }

    // -------------------------------------------------------------------------
    public static byte GetRandomDir(byte axis) {
        if(axis == Block.AXIS_H) {
            return MathUtils.randomBoolean() ? Block.DIR_LEFT : Block.DIR_RIGHT;
        } else {
            return MathUtils.randomBoolean() ? Block.DIR_DOWN : Block.DIR_UP;
        }
    }
}
