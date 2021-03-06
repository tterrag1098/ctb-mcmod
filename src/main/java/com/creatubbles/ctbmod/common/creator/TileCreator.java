package com.creatubbles.ctbmod.common.creator;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.ArrayUtils;

import com.creatubbles.ctbmod.CTBMod;
import com.creatubbles.ctbmod.common.config.Configs;
import com.creatubbles.ctbmod.common.http.CreationRelations;
import com.creatubbles.ctbmod.common.network.MessageDimensionChange;
import com.creatubbles.ctbmod.common.network.PacketHandler;
import com.creatubbles.ctbmod.common.painting.BlockPainting;
import com.creatubbles.repack.endercore.api.common.util.IProgressTile;
import com.creatubbles.repack.endercore.common.TileEntityBase;
import com.creatubbles.repack.endercore.common.util.Bound;

public class TileCreator extends TileEntityBase implements ISidedInventory, ITickable, IProgressTile {

    private static final Bound<Integer> DIMENSION_BOUND = Bound.of(1, 16);

    private final ItemStack[] inventory = new ItemStack[5];
    private CreationRelations creating;
    private int progress;

    @Getter
    private int width = 1, height = 1;

    public void setWidth(int width) {
        this.width = DIMENSION_BOUND.clamp(width);
        dimensionsChanged();
    }

    public void setHeight(int height) {
        this.height = DIMENSION_BOUND.clamp(height);
        dimensionsChanged();
    }

    public int getMinSize() {
        return DIMENSION_BOUND.getMin();
    }

    public int getMaxSize() {
        return DIMENSION_BOUND.getMax();
    }

    private void dimensionsChanged() {
        if (worldObj != null && worldObj.isRemote) {
            PacketHandler.INSTANCE.sendToServer(new MessageDimensionChange(this));
        }
    }

    public ItemStack getOutput() {
        return inventory[4];
    }

    public int getPaperCount() {
        int count = inventory[0] == null ? 0 : inventory[0].stackSize;
        if (!Configs.harderPaintings) {
            for (int i = 1; i < 4; i++) {
                count += inventory[i] == null ? 0 : inventory[i].stackSize;
            }
        }
        return count;
    }

    public int getLowestDyeCount() {
        int ret = Integer.MAX_VALUE;
        for (int i = 1; i < 4; i++) {
            if (inventory[i] != null) {
                ret = Math.min(ret, inventory[i].stackSize);
            }
        }
        return ret == Integer.MAX_VALUE ? 0 : ret;
    }

    public void create(CreationRelations creation) {
        if (!canCreate()) {
            return;
        }
        creating = creation;
        if (Configs.harderPaintings) {
            for (int i = 0; i < 4; i++) {
                inventory[i].stackSize -= i == 0 ? getRequiredPaper() : getRequiredDye();
                if (inventory[i].stackSize == 0) {
                    inventory[i] = null;
                }
            }
        } else {
            int required = getRequiredPaper();
            for (int i = 0; i < 4 && required > 0; i++) {
                if (inventory[i] != null) {
                    if (inventory[i].stackSize < required) {
                        required -= inventory[i].stackSize;
                        inventory[i] = null;
                    } else {
                        inventory[i].stackSize -= required;
                        required = 0;
                        if (inventory[i].stackSize == 0) {
                            inventory[i] = null;
                        }
                    }
                }
            }
        }
    }

    public ItemStack[] getInput() {
        return ArrayUtils.subarray(inventory, 0, inventory.length - 1);
    }

    public boolean canCreate() {
        if (inventory[4] != null || progress > 0) {
            return false;
        }
        return getPaperCount() >= getRequiredPaper() && (!Configs.harderPaintings || getLowestDyeCount() >= getRequiredDye());
    }

    public int getRequiredPaper() {
        return (3 + getWidth() * getHeight()) / 4;
    }

    public int getRequiredDye() {
        return (7 + getWidth() * getHeight()) / 8;
    }

    @Override
    protected void doUpdate() {
        if (!worldObj.isRemote) {
            if (creating == null) {
                progress = 0;
            } else {
                if (progress < 20) {
                    progress++;
                } else {
                    inventory[4] = BlockPainting.create(creating, width, height);
                    markDirty();
                    creating = null;
                }
            }
        }
    }

    @Override
    public float getProgress() {
        return progress / 20f;
    }

    @Override
    public void setProgress(float progress) {
        this.progress = progress < 0 ? 0 : (int) (progress * 20);
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == 4;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return index < 4;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[] { 0, 1, 2, 3, 4 };
    }

    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventory[index % inventory.length];
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (inventory[index] != null) {
            ItemStack itemstack;
            if (inventory[index].stackSize <= count) {
                itemstack = inventory[index];
                inventory[index] = null;
                return itemstack;
            } else {
                itemstack = inventory[index].splitStack(count);
                if (inventory[index].stackSize == 0) {
                    inventory[index] = null;
                }
                return itemstack;
            }
        } else {
            return null;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (inventory[index] != null) {
            ItemStack itemstack = inventory[index];
            inventory[index] = null;
            return itemstack;
        } else {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (isItemValidForSlot(index, stack)) {
            inventory[index] = stack;
            markDirty();
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    @Override
    public void clear() {
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = null;
        }
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    private final String[] colors = new String[] { "dyeRed", "dyeGreen", "dyeBlue" };

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (stack == null) {
            return true;
        }
        if (index == 0 || !Configs.harderPaintings && index < 4) {
            return stack.getItem() == Items.PAPER;
        } else if (index < 4) {
            int[] ids = OreDictionary.getOreIDs(stack);
            String ore = colors[index - 1];
            for (int i : ids) {
                if (OreDictionary.getOreName(i).equals(ore)) {
                    return true;
                }
            }
        } else {
            return Block.getBlockFromItem(stack.getItem()) == CTBMod.painting;
        }
        return false;
    }

    @Override
    protected void writeCustomNBT(NBTTagCompound root) {
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] != null) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }
        root.setTag("Items", nbttaglist);

        root.setInteger("paintingWidth", getWidth());
        root.setInteger("paintingHeight", getHeight());
    }

    @Override
    protected void readCustomNBT(NBTTagCompound root) {
        NBTTagList nbttaglist = root.getTagList("Items", 10);

        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 255;

            inventory[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
        }

        setWidth(root.getInteger("paintingWidth"));
        setHeight(root.getInteger("paintingHeight"));
    }

    // Stupid pointless IInventory methods

    @Override
    public String getName() {
        return getDisplayName().getUnformattedText();
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString("This is stupid");
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }
}
