package com.mraof.minestuck.entity;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.ArrayList;

/**
 * Created by mraof on 2017 January 26 at 9:31 PM.
 */
public class EntityBigPart extends Entity implements IEntityAdditionalSpawnData
{
	PartGroup group;
	private int partId;
	EntitySize size;
	
	EntityBigPart(EntityType<?> type, World worldIn, PartGroup group, float width, float height)
	{
		super(type, worldIn);
		this.group = group;
		this.size = EntitySize.flexible(width, height);
	}

	void setPartId(int id)
	{
		this.partId = id;
	}
	
	@Override
	protected void registerData()
	{
	
	}
	
	@Override
	protected void readAdditional(CompoundNBT compound)
	{
	
	}
	
	@Override
	protected void writeAdditional(CompoundNBT compound)
	{
	
	}
	
	@Override
	public void baseTick()
	{
		if(this.group == null || this.group.parent == null || this.group.parent.removed)
		{
			this.remove();
		}
		super.baseTick();
		//world.getHeight(this.getPosition());
	}

	@Override
	public boolean attackEntityFrom(DamageSource damageSource, float amount)
	{
		return this.group != null && this.group.attackFrom(damageSource, amount);
	}
	
	@Override
	public EntitySize getSize(Pose poseIn)
	{
		return super.getSize(poseIn);
	}
	
	@Override
	public void writeSpawnData(PacketBuffer buffer)
	{
		buffer.writeInt(this.group.parent.getEntityId());
		buffer.writeFloat(this.getWidth());
		buffer.writeFloat(this.getHeight());
	}

	@Override
	public void readSpawnData(PacketBuffer additionalData)
	{
		Entity entity = world.getEntityByID(additionalData.readInt());
		if(entity instanceof IBigEntity)
		{
			ArrayList<EntityBigPart> parts = ((IBigEntity) entity).getGroup().parts;
			int index = parts.size() - 1;
			while(index > 0 && parts.get(index).partId > this.partId)
			{
				index--;
			}
			parts.add(index, this);
		}
	}

	@Override
	public boolean isEntityEqual(Entity entityIn)
	{
		return entityIn == this || this.group != null && (entityIn == this.group.parent || entityIn instanceof EntityBigPart && ((EntityBigPart) entityIn).group == this.group);
	}

	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	@Override
	public boolean isEntityInsideOpaqueBlock()
	{
		return false;
	}
	
	@Override
	public void move(MoverType typeIn, Vector3d pos)
	{
		this.setBoundingBox(this.getBoundingBox().offset(pos));
		this.resetPositionToBB();
	}
	
	@Override
	public IPacket<?> createSpawnPacket()
	{
		throw new UnsupportedOperationException();
	}
}