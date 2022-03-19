package flaxbeard.immersivepetroleum.client.render.debugging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartBlockEntity;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import blusunrize.immersiveengineering.common.util.inventory.MultiFluidTank;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.LubricatedTileInfo;
import flaxbeard.immersivepetroleum.api.crafting.reservoir.ReservoirHandler;
import flaxbeard.immersivepetroleum.api.crafting.reservoir.ReservoirIsland;
import flaxbeard.immersivepetroleum.client.render.IPRenderTypes;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.CokingChamber;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.DerrickTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.DistillationTowerTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.HydrotreaterTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.OilTankTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.OilTankTileEntity.Port;
import flaxbeard.immersivepetroleum.common.entity.MotorboatEntity;
import flaxbeard.immersivepetroleum.common.items.DebugItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class DebugRenderHandler{
	public DebugRenderHandler(){
	}
	
	private boolean isHoldingDebugItem(Player player){
		ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
		
		return (main != ItemStack.EMPTY && main.getItem() == IPContent.DEBUGITEM.get()) || (off != ItemStack.EMPTY && off.getItem() == IPContent.DEBUGITEM.get());
	}
	
	@SubscribeEvent
	public void renderDebuggingOverlay(RenderGameOverlayEvent.Post event){
		Minecraft mc = Minecraft.getInstance();
		
		if(mc.player != null && event.getType() == RenderGameOverlayEvent.ElementType.TEXT){
			Player player = mc.player;
			
			if(isHoldingDebugItem(player)){
				HitResult rt = mc.hitResult;
				if(rt != null){
					switch(rt.getType()){
						case BLOCK:{
							BlockHitResult result = (BlockHitResult) rt;
							Level world = player.level;
							
							List<Component> debugOut = new ArrayList<>();
							BlockEntity te = world.getBlockEntity(result.getBlockPos());
							boolean isMBPart = te instanceof MultiblockPartBlockEntity<?>;
							if(isMBPart){
								MultiblockPartBlockEntity<?> multiblock = (MultiblockPartBlockEntity<?>) te;

								if(!multiblock.offsetToMaster.equals(BlockPos.ZERO)){
									multiblock = multiblock.master();
								}
								
								if(te instanceof DistillationTowerTileEntity){
									distillationtower(debugOut, (DistillationTowerTileEntity) multiblock);
									
								}else if(te instanceof CokerUnitTileEntity){
									cokerunit(debugOut, (CokerUnitTileEntity) multiblock);
									
								}else if(te instanceof HydrotreaterTileEntity){
									hydrotreater(debugOut, (HydrotreaterTileEntity) multiblock);
									
								}else if(te instanceof OilTankTileEntity){
									oiltank(debugOut, (OilTankTileEntity) multiblock);
									
								}else if(te instanceof DerrickTileEntity){
									DerrickTileEntity derrick = (DerrickTileEntity) multiblock;
									
									IFluidTank[] tanks = derrick.getInternalTanks();
									if(tanks != null && tanks.length > 0){
										for(int i = 0;i < tanks.length;i++){
											FluidStack fs = tanks[i].getFluid();
											debugOut.add(toText("Tank " + i + ": " + (fs.getAmount() + "/" + tanks[i].getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
										}
									}
								}
							}
							
							if(!debugOut.isEmpty() || isMBPart){
								if(isMBPart){
									MultiblockPartBlockEntity<?> generic = (MultiblockPartBlockEntity<?>) te;
									BlockPos tPos = generic.posInMultiblock;
									
									if(!generic.offsetToMaster.equals(BlockPos.ZERO)){
										generic = generic.master();
									}
									
									BlockPos hit = result.getBlockPos();
									Block block = generic.getBlockState().getBlock();
									
									debugOut.add(0, toText("World XYZ: " + hit.getX() + ", " + hit.getY() + ", " + hit.getZ()));
									debugOut.add(1, toText("Template XYZ: " + tPos.getX() + ", " + tPos.getY() + ", " + tPos.getZ()));
									
									MutableComponent name = toTranslation(block.getDescriptionId()).withStyle(ChatFormatting.GOLD);
									
									try{
										name.append(toText(generic.isRSDisabled() ? " (Redstoned)" : "").withStyle(ChatFormatting.RED));
									}catch(UnsupportedOperationException e){
										// Don't care, skip if this is thrown
									}
									
									if(generic instanceof PoweredMultiblockBlockEntity<?,?> poweredGeneric){

										name.append(toText(poweredGeneric.shouldRenderAsActive() ? " (Active)" : "").withStyle(ChatFormatting.GREEN));
										
										debugOut.add(2, toText(poweredGeneric.energyStorage.getEnergyStored() + "/" + poweredGeneric.energyStorage.getMaxEnergyStored() + "RF"));
									}
									
									synchronized(LubricatedHandler.lubricatedTiles){
										for(LubricatedTileInfo info:LubricatedHandler.lubricatedTiles){
											if(info.pos.equals(generic.getBlockPos())){
												name.append(toText(" (Lubricated " + info.ticks + ")").withStyle(ChatFormatting.YELLOW));
											}
										}
									}
									
									debugOut.add(2, name);
								}
								
								renderOverlay(event.getMatrixStack(), debugOut);
							}
							break;
						}
						case ENTITY:{
							EntityHitResult result = (EntityHitResult) rt;
							
							if(result.getEntity() instanceof MotorboatEntity){
								MotorboatEntity boat = (MotorboatEntity) result.getEntity();
								
								List<Component> debugOut = new ArrayList<>();
								
								debugOut.add(toText("").append(boat.getDisplayName()).withStyle(ChatFormatting.GOLD));
								
								FluidStack fluid = boat.getContainedFluid();
								if(fluid == FluidStack.EMPTY){
									debugOut.add(toText("Tank: Empty"));
								}else{
									debugOut.add(toText("Tank: " + fluid.getAmount() + "/" + boat.getMaxFuel() + "mB of ").append(fluid.getDisplayName()));
								}
								
								NonNullList<ItemStack> upgrades = boat.getUpgrades();
								int i = 0;
								for(ItemStack upgrade:upgrades){
									if(upgrade == null || upgrade == ItemStack.EMPTY){
										debugOut.add(toText("Upgrade " + (++i) + ": Empty"));
									}else{
										debugOut.add(toText("Upgrade " + (++i) + ": ").append(upgrade.getHoverName()));
									}
								}
								
								renderOverlay(event.getMatrixStack(), debugOut);
							}
							break;
						}
						default:
							break;
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void reservoirDebuggingRenderLast(RenderLevelLastEvent event){
		if(ReservoirHandler.generator == null){
			return;
		}
		
		Player player = ClientUtils.mc().player;
		
		ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
		
		if((main != ItemStack.EMPTY && main.getItem() == IPContent.DEBUGITEM.get()) || (off != ItemStack.EMPTY && off.getItem() == IPContent.DEBUGITEM.get())){
			DebugItem.Modes mode = null;
			if(main != ItemStack.EMPTY){
				mode = DebugItem.getMode(main);
			}
			if(off != ItemStack.EMPTY){
				mode = DebugItem.getMode(off);
			}
			
			if(mode == DebugItem.Modes.SEEDBASED_RESERVOIR || mode == DebugItem.Modes.SEEDBASED_RESERVOIR_AREA_TEST){
				PoseStack matrix = event.getPoseStack();
				Level world = player.getCommandSenderWorld();
				BlockPos playerPos = player.blockPosition();
				
				matrix.pushPose();
				{
					// Anti-Jiggle when moving
					Vec3 renderView = ClientUtils.mc().gameRenderer.getMainCamera().getPosition();
					matrix.translate(-renderView.x, -renderView.y, -renderView.z);
					
					matrix.pushPose();
					{
						MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
						
						int radius = 12;
						for(int i = -radius;i <= radius;i++){
							for(int j = -radius;j <= radius;j++){
								ChunkPos cPos = new ChunkPos(playerPos.offset(16*i, 0, 16*j));
								int chunkX = cPos.getMinBlockX();
								int chunkZ = cPos.getMinBlockZ();
								
								for(int cX = 0;cX < 16;cX++){
									for(int cZ = 0;cZ < 16;cZ++){
										int x = chunkX + cX;
										int z = chunkZ + cZ;
										
										matrix.pushPose();
										{
											DyeColor color = DyeColor.BLACK;
											
											double n = ReservoirHandler.noiseFor(world, x, z);
											if(n > -1){
												int c = (int) Math.round(9 * n);
												
												if(c <= 0){
													color = DyeColor.BLACK;
												}else if(c == 1){
													color = DyeColor.BLUE;
												}else if(c == 2){
													color = DyeColor.CYAN;
												}else if(c == 3){
													color = DyeColor.GREEN;
												}else if(c == 4){
													color = DyeColor.LIME;
												}else if(c == 5){
													color = DyeColor.YELLOW;
												}else if(c == 6){
													color = DyeColor.ORANGE;
												}else if(c == 7){
													color = DyeColor.RED;
												}else if(c > 7){
													color = DyeColor.WHITE;
												}
												
												int r = (color.getTextColor() & 0xFF0000) >> 16;
												int g = (color.getTextColor() & 0x00FF00) >> 8;
												int b = (color.getTextColor() & 0x0000FF);

												r/=1.5F;
												g/=1.5F;
												b/=1.5F;
												
												int height = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z)).getY();
												for(;height > 0;height--){
													if(world.getBlockState(new BlockPos(x, height - 1, z)).isSolidRender(world, new BlockPos(x, height - 1, z))){
														break;
													}
												}
												
												matrix.translate(x, Math.max(63, height) + 0.0625, z);
												
												Matrix4f mat = matrix.last().pose();
												
												VertexConsumer builder = buffer.getBuffer(IPRenderTypes.ISLAND_DEBUGGING_POSITION_COLOR);
												builder.vertex(mat, 0, 0, 0).color(r, g, b, 255).endVertex();
												builder.vertex(mat, 0, 0, 1).color(r, g, b, 255).endVertex();
												builder.vertex(mat, 1, 0, 1).color(r, g, b, 255).endVertex();
												builder.vertex(mat, 1, 0, 0).color(r, g, b, 255).endVertex();
											}
										}
										matrix.popPose();
									}
								}
							}
						}
						buffer.endBatch();
					}
					matrix.popPose();
					
					matrix.pushPose();
					{
						synchronized(ReservoirHandler.getReservoirIslandList()){
							MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
							
							Collection<ReservoirIsland> islands = ReservoirHandler.getReservoirIslandList().get(player.getCommandSenderWorld().dimension());
							
							if(islands != null && !islands.isEmpty()){
								int radius = 128;
								radius = radius * radius + radius * radius;
								for(ReservoirIsland island:islands){
									BlockPos p = new BlockPos(playerPos.getX(), 0, playerPos.getZ());
									BlockPos center = island.getBoundingBox().getCenter();
									
									if(center.distSqr(p) <= radius){
										List<ColumnPos> poly = island.getPolygon();
										
										if(poly != null && !poly.isEmpty()){
											VertexConsumer builder = buffer.getBuffer(IPRenderTypes.TRANSLUCENT_LINES);
											Matrix4f mat = matrix.last().pose();
											
											// Draw polygon as line
											int j = poly.size() - 1;
											for(int i = 0;i < poly.size();i++){
												ColumnPos a = poly.get(j);
												ColumnPos b = poly.get(i);
												float f = i / (float) poly.size();
												
												builder.vertex(mat, a.x + .5F, 128.5F, a.z + .5F).color(f, 0.0F, 1 - f, 0.5F).endVertex();
												builder.vertex(mat, b.x + .5F, 128.5F, b.z + .5F).color(f, 0.0F, 1 - f, 0.5F).endVertex();
												
												j = i;
											}
											
											// Center Marker
											builder.vertex(mat, center.getX() + .5F, 128F, center.getZ() + .5F).color(0.0F, 1.0F, 0.0F, 0.5F).endVertex();
											builder.vertex(mat, center.getX() + .5F, 129F, center.getZ() + .5F).color(0.0F, 1.0F, 0.0F, 0.5F).endVertex();
											builder.vertex(mat, center.getX(), 128.5F, center.getZ() + .5F).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
											builder.vertex(mat, center.getX() + 1, 128.5F, center.getZ() + .5F).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
											builder.vertex(mat, center.getX() + .5F, 128.5F, center.getZ()).color(0.0F, 0.0F, 1.0F, 0.5F).endVertex();
											builder.vertex(mat, center.getX() + .5F, 128.5F, center.getZ() + 1).color(0.0F, 0.0F, 1.0F, 0.5F).endVertex();
											
										}
									}
								}
							}
							
							buffer.endBatch();
						}
					}
					matrix.popPose();
				}
				matrix.popPose();
				
			}
		}
	}
		
	private static void renderOverlay(PoseStack matrix, List<Component> debugOut){
		Minecraft mc = Minecraft.getInstance();
		
		matrix.pushPose();
		{
			MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
			for(int i = 0;i < debugOut.size();i++){
				int w = mc.font.width(debugOut.get(i).getString());
				int yOff = i * (mc.font.lineHeight + 2);
				
				matrix.pushPose();
				{
					matrix.translate(0, 0, 1);
					GuiHelper.drawColouredRect(1, 1 + yOff, w + 1, 10, 0xAF_000000, buffer, matrix);
					buffer.endBatch();
					// Draw string without shadow
					mc.font.draw(matrix, debugOut.get(i), 2, 2 + yOff, -1);
				}
				matrix.popPose();
			}
		}
		matrix.popPose();
	}
	
	private static void distillationtower(List<Component> text, DistillationTowerTileEntity tower){
		for(int i = 0;i < tower.tanks.length;i++){
			text.add(toText("Tank " + (i + 1)).withStyle(ChatFormatting.UNDERLINE));
			
			MultiFluidTank tank = tower.tanks[i];
			if(tank.fluids.size() > 0){
				for(int j = 0;j < tank.fluids.size();j++){
					FluidStack fstack = tank.fluids.get(j);
					text.add(toText("  " + fstack.getDisplayName().getString() + " (" + fstack.getAmount() + "mB)"));
				}
			}else{
				text.add(toText("  Empty"));
			}
		}
	}
	
	private static void cokerunit(List<Component> text, CokerUnitTileEntity coker){
		{
			FluidTank tank = coker.bufferTanks[CokerUnitTileEntity.TANK_INPUT];
			FluidStack fs = tank.getFluid();
			text.add(toText("In Buffer: " + (fs.getAmount() + "/" + tank.getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
		}
		
		{
			FluidTank tank = coker.bufferTanks[CokerUnitTileEntity.TANK_OUTPUT];
			FluidStack fs = tank.getFluid();
			text.add(toText("Out Buffer: " + (fs.getAmount() + "/" + tank.getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
		}
		
		for(int i = 0;i < coker.chambers.length;i++){
			CokingChamber chamber = coker.chambers[i];
			FluidTank tank = chamber.getTank();
			FluidStack fs = tank.getFluid();
			
			float completed = chamber.getTotalAmount() > 0 ? 100 * (chamber.getOutputAmount() / (float) chamber.getTotalAmount()) : 0;
			
			text.add(toText("Chamber " + i).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.AQUA));
			text.add(toText("State: " + chamber.getState().toString()));
			text.add(toText("  Tank: " + (fs.getAmount() + "/" + tank.getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
			text.add(toText("  Content: " + chamber.getTotalAmount() + " / " + chamber.getCapacity()).append(" (" + chamber.getInputItem().getHoverName().getString() + ")"));
			text.add(toText("  Out: " + chamber.getOutputItem().getHoverName().getString()));
			text.add(toText("  " + Mth.floor(completed) + "% Completed. (Raw: " + completed + ")"));
		}
	}
	
	private static void hydrotreater(List<Component> text, HydrotreaterTileEntity treater){
		IFluidTank[] tanks = treater.getInternalTanks();
		if(tanks != null && tanks.length > 0){
			for(int i = 0;i < tanks.length;i++){
				FluidStack fs = tanks[i].getFluid();
				text.add(toText("Tank " + i + ": " + (fs.getAmount() + "/" + tanks[i].getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
			}
		}
	}
	
	private static void oiltank(List<Component> text, OilTankTileEntity tank){
		{
			BlockPos mbpos = tank.posInMultiblock;
			Port port = null;
			for(Port p:Port.values()){
				if(p.matches(mbpos)){
					port = p;
					break;
				}
			}
			
			if(port != null){
				OilTankTileEntity.PortState portState = tank.portConfig.get(port);
				boolean isInput = portState == OilTankTileEntity.PortState.INPUT;
				text.add(toText("Port: ")
						.append(toText(port != null ? port.getSerializedName() : "None"))
						.append(toText(" " + portState.getSerializedName())
								.withStyle(isInput ? ChatFormatting.AQUA : ChatFormatting.GOLD)));
			}
		}
		
		FluidStack fs = tank.tank.getFluid();
		text.add(toText("Fluid: " + (fs.getAmount() + "/" + tank.tank.getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
	}
	
	static MutableComponent toText(String string){
		return new TextComponent(string);
	}
	
	static MutableComponent toTranslation(String translationKey, Object... args){
		return new TranslatableComponent(translationKey, args);
	}
}
