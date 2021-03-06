package com.creatubbles.repack.endercore.common;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.creatubbles.ctbmod.common.network.PacketHandler;
import com.creatubbles.repack.endercore.api.common.util.IProgressTile;
import com.creatubbles.repack.endercore.common.network.PacketProgress;

public abstract class TileEntityBase extends TileEntity implements ITickable {

    private final int checkOffset = (int) (Math.random() * 20);
    protected final boolean isProgressTile;

    protected int lastProgressScaled = -1;
    protected int ticksSinceLastProgressUpdate;

    public TileEntityBase() {
        isProgressTile = this instanceof IProgressTile;
    }

    @Override
    public final void update() {
        doUpdate();
        if (isProgressTile && !worldObj.isRemote) {
            int curScaled = getProgressScaled(16);
            if (++ticksSinceLastProgressUpdate >= getProgressUpdateFreq() || curScaled != lastProgressScaled) {
                sendTaskProgressPacket();
                lastProgressScaled = curScaled;
            }
        }
    }

    public static int getProgressScaled(int scale, IProgressTile tile) {
        return (int) (tile.getProgress() * scale);
    }

    public final int getProgressScaled(int scale) {
        if (isProgressTile) {
            return getProgressScaled(scale, (IProgressTile) this);
        }
        return 0;
    }

    protected void doUpdate() {

    }

    protected void sendTaskProgressPacket() {
        if (isProgressTile) {
            PacketHandler.sendToAllAround(new PacketProgress((IProgressTile) this), this);
        }
        ticksSinceLastProgressUpdate = 0;
    }

    /**
     * Controls how often progress updates. Has no effect if your TE is not {@link IProgressTile}.
     */
    protected int getProgressUpdateFreq() {
        return 20;
    }

    @Override
    public final void readFromNBT(NBTTagCompound root) {
        super.readFromNBT(root);
        readCustomNBT(root);
    }

    @Override
    public final NBTTagCompound writeToNBT(NBTTagCompound root) {
        root = super.writeToNBT(root);
        writeCustomNBT(root);
        return root;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(super.getUpdateTag());
    }
    
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 1, writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }
    
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readCustomNBT(tag);
    }

    public boolean canPlayerAccess(EntityPlayer player) {
        return !isInvalid() && player.getDistanceSqToCenter(getPos().add(0.5, 0.5, 0.5)) <= 64D;
    }

    protected abstract void writeCustomNBT(NBTTagCompound root);

    protected abstract void readCustomNBT(NBTTagCompound root);

    protected void updateBlock() {
        if (worldObj != null) {
            IBlockState state = getWorld().getBlockState(getPos());
            worldObj.notifyBlockUpdate(pos, state, state, 8);
        }
    }

    protected boolean isPoweredRedstone() {
        return worldObj.isBlockLoaded(getPos()) ? worldObj.getStrongPower(getPos()) > 0 : false;
    }

    /**
     * Called directly after the TE is constructed. This is the place to call non-final methods.
     *
     * Note: This will not be called when the TE is loaded from the save. Hook into the nbt methods for that.
     */
    public void init() {}

    /**
     * Call this with an interval (in ticks) to find out if the current tick is the one you want to do some work. This
     * is staggered so the work of different TEs is stretched out over time.
     *
     * @see #shouldDoWorkThisTick(int, int) If you need to offset work ticks
     */
    protected boolean shouldDoWorkThisTick(int interval) {
        return shouldDoWorkThisTick(interval, 0);
    }

    /**
     * Call this with an interval (in ticks) to find out if the current tick is the one you want to do some work. This
     * is staggered so the work of different TEs is stretched out over time.
     *
     * If you have different work items in your TE, use this variant to stagger your work.
     */
    protected boolean shouldDoWorkThisTick(int interval, int offset) {
        return (worldObj.getTotalWorldTime() + checkOffset + offset) % interval == 0;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }
}
