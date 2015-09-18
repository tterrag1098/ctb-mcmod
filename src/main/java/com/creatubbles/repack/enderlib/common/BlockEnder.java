package com.creatubbles.repack.enderlib.common;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.creatubbles.ctbmod.CTBMod;

public abstract class BlockEnder<T extends TileEntityBase> extends Block {

	protected final Class<? extends T> teClass;
	protected final String name;

	protected BlockEnder(String name, Class<? extends T> teClass) {
		this(name, teClass, new Material(MapColor.ironColor));
	}

	protected BlockEnder(String name, Class<? extends T> teClass, Material mat) {
		super(mat);
		this.teClass = teClass;
		this.name = name;
		setHardness(0.5F);
		setUnlocalizedName(name);
		setStepSound(Block.soundTypeMetal);
		setHarvestLevel("pickaxe", 0);
	}

	protected void init() {
		GameRegistry.registerBlock(this, name);
		if (teClass != null) {
			GameRegistry.registerTileEntity(teClass, name + "TileEntity");
		}
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return teClass != null;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		if (teClass != null) {
			try {
				T te = teClass.newInstance();
				te.init();
				return te;
			} catch (Exception e) {
				CTBMod.logger.error("Could not create tile entity for block " + name + " for class " + teClass);
			}
		}
		return null;
	}

	/* Subclass Helpers */

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (playerIn.isSneaking()) {
			return false;
		}

		return openGui(worldIn, pos, playerIn, side);
	}

	protected boolean openGui(World world, BlockPos pos, EntityPlayer entityPlayer, EnumFacing side) {
		return false;
	}

	protected void processDrop(World world, int x, int y, int z, @Nullable T te, ItemStack drop) {
	}

	@SuppressWarnings("unchecked")
	protected T getTileEntity(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (teClass.isInstance(te)) {
			return (T) te;
		}
		return null;
	}

	protected boolean shouldDoWorkThisTick(World world, BlockPos pos, int interval) {
		T te = getTileEntity(world, pos);
		if (te == null) {
			return world.getTotalWorldTime() % interval == 0;
		} else {
			return te.shouldDoWorkThisTick(interval);
		}
	}

	protected boolean shouldDoWorkThisTick(World world, BlockPos pos, int interval, int offset) {
		T te = getTileEntity(world, pos);
		if (te == null) {
			return (world.getTotalWorldTime() + offset) % interval == 0;
		} else {
			return te.shouldDoWorkThisTick(interval, offset);
		}
	}

	// Because the vanilla method takes floats...
	public void setBlockBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public void setBlockBounds(AxisAlignedBB bb) {
		setBlockBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
	}
}