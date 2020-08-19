package flaxbeard.immersivepetroleum.common.lubehandlers;

import com.mojang.blaze3d.platform.GlStateManager;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.IEConfig;
import blusunrize.immersiveengineering.common.blocks.metal.BucketWheelTileEntity;
import blusunrize.immersiveengineering.common.blocks.metal.ExcavatorTileEntity;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.ILubricationHandler;
import flaxbeard.immersivepetroleum.client.model.ModelLubricantPipes;
import flaxbeard.immersivepetroleum.common.IPContent.Fluids;
import flaxbeard.immersivepetroleum.common.blocks.metal.AutoLubricatorNewTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ExcavatorLubricationHandler implements ILubricationHandler<ExcavatorTileEntity>{
	
	@Override
	public TileEntity isPlacedCorrectly(World world, TileEntity lubricator, Direction facing){
		BlockPos initialTarget = lubricator.getPos().offset(facing);
		ExcavatorTileEntity adjacent = (ExcavatorTileEntity) world.getTileEntity(initialTarget);
		Direction f = adjacent.getIsMirrored() ? facing : facing.getOpposite();
		
		BlockPos target = lubricator.getPos().offset(facing, 2).offset(f.rotateY(), 4).up();
		TileEntity te = world.getTileEntity(target);
		
		if(te instanceof ExcavatorTileEntity){
			ExcavatorTileEntity excavator = (ExcavatorTileEntity) te;
			ExcavatorTileEntity master = excavator.master();
			
			if(excavator == master && excavator.getFacing().rotateY() == f){
				return master;
			}
		}
		
		return null;
	}
	
	@Override
	public boolean isMachineEnabled(World world, ExcavatorTileEntity mbte){
		BlockPos wheelPos = mbte.master().getOrigin();
		TileEntity center = world.getTileEntity(wheelPos);
		
		if(center instanceof BucketWheelTileEntity){
			BucketWheelTileEntity wheel = (BucketWheelTileEntity) center;
			
			return wheel.active;
		}
		return false;
	}
	
	@Override
	public void lubricate(World world, int ticks, ExcavatorTileEntity mbte){
		BlockPos wheelPos = mbte.master().getOrigin();// getOrigin();//BlockPosForPos(31);
		TileEntity center = world.getTileEntity(wheelPos);
		
		if(center instanceof BucketWheelTileEntity){
			BucketWheelTileEntity wheel = (BucketWheelTileEntity) center;
			
			if(!world.isRemote && ticks % 4 == 0){
				int consumed = IEConfig.MACHINES.excavator_consumption.get();
				int extracted = mbte.energyStorage.extractEnergy(consumed, true);
				if(extracted >= consumed){
					mbte.energyStorage.extractEnergy(extracted, false);
					wheel.rotation += IEConfig.MACHINES.excavator_speed.get() / 4F;
				}
			}else{
				wheel.rotation += IEConfig.MACHINES.excavator_speed.get() / 4F;
			}
			
		}
	}
	
	@Override
	public void spawnLubricantParticles(World world, TileEntity lubricator, Direction facing, ExcavatorTileEntity mbte){
		Direction f = mbte.getIsMirrored() ? facing : facing.getOpposite();
		
		float location = world.rand.nextFloat();
		
		boolean flip = f.getAxis() == Axis.Z ^ facing.getAxisDirection() == AxisDirection.POSITIVE ^ !mbte.getIsMirrored();
		float xO = 1.2F;
		float zO = -.5F;
		float yO = .5F;
		
		if(location > .5F){
			xO = 0.9F;
			yO = 0.8F;
			zO = 1.75F;
		}
		
		if(facing.getAxisDirection() == AxisDirection.NEGATIVE) xO = -xO + 1;
		if(!flip) zO = -zO + 1;
		
		float x = lubricator.getPos().getX() + (f.getAxis() == Axis.X ? xO : zO);
		float y = lubricator.getPos().getY() + yO;
		float z = lubricator.getPos().getZ() + (f.getAxis() == Axis.X ? zO : xO);
		
		for(int i = 0;i < 3;i++){
			float r1 = (world.rand.nextFloat() - .5F) * 2F;
			float r2 = (world.rand.nextFloat() - .5F) * 2F;
			float r3 = world.rand.nextFloat();
			BlockState n = Fluids.fluidLubricant.block.getDefaultState();
			world.addParticle(new BlockParticleData(ParticleTypes.FALLING_DUST, n), x, y, z, r1 * 0.04F, r3 * 0.0125F, r2 * 0.025F);
		}
	}
	
	private static Object excavator;
	private static Object excavatorM;
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void renderPipes(World world, AutoLubricatorNewTileEntity lubricator, Direction facing, ExcavatorTileEntity mbte){
		if(excavator == null){
			excavatorM = new ModelLubricantPipes.Excavator(true);
			excavator = new ModelLubricantPipes.Excavator(false);
		}
		
		GlStateManager.translatef(0, -1, 0);
		Vec3i offset = mbte.getPos().subtract(lubricator.getPos());
		GlStateManager.translatef(offset.getX(), offset.getY(), offset.getZ());
		
		Direction rotation = mbte.getFacing();
		if(rotation == Direction.NORTH){
			GlStateManager.rotatef(90F, 0, 1, 0);
			
		}else if(rotation == Direction.WEST){
			GlStateManager.rotatef(180F, 0, 1, 0);
			GlStateManager.translatef(0, 0, -1);
			
		}else if(rotation == Direction.SOUTH){
			GlStateManager.rotatef(270F, 0, 1, 0);
			GlStateManager.translatef(1, 0, -1);
		}else if(rotation == Direction.EAST){
			GlStateManager.translatef(1, 0, 0);
			
		}
		
		GlStateManager.translatef(-1, 0, -1);
		ClientUtils.bindTexture("immersivepetroleum:textures/blocks/lube_pipe12");
		if(mbte.getIsMirrored()){
			((ModelLubricantPipes.Excavator) excavatorM).render(0.0625F);
		}else{
			((ModelLubricantPipes.Excavator) excavator).render(0.0625F);
		}
	}
	
	@Override
	public Tuple<BlockPos, Direction> getGhostBlockPosition(World world, ExcavatorTileEntity mbte){
		if(!mbte.isDummy()){
			BlockPos pos = mbte.getPos().offset(mbte.getFacing(), 4).offset(mbte.getIsMirrored() ? mbte.getFacing().rotateYCCW() : mbte.getFacing().rotateY(), 2);
			Direction f = mbte.getIsMirrored() ? mbte.getFacing().rotateY() : mbte.getFacing().rotateYCCW();
			return new Tuple<BlockPos, Direction>(pos, f);
		}
		return null;
	}
	
	private static Vec3i size;
	@Override
	public Vec3i getStructureDimensions(){
		if(size==null)
			size=new Vec3i(3, 6, 3);
		return size;
	}
}