package com.mraof.minestuck.world.gen.feature;

import com.mojang.datafixers.Dynamic;
import com.mraof.minestuck.world.gen.feature.structure.blocks.StructureBlockUtil;
import com.mraof.minestuck.world.storage.loot.MSLootTables;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;

import java.util.Random;
import java.util.function.Function;

public class OasisFeature extends Feature<NoFeatureConfig>
{
	public OasisFeature(Function<Dynamic<?>, ? extends NoFeatureConfig> configFactory)
	{
		super(configFactory);
	}
	
	@Override
	public boolean place(IWorld worldIn, ChunkGenerator<? extends GenerationSettings> generator, Random rand, BlockPos pos, NoFeatureConfig config)
	{
		boolean[] blocks = new boolean[16*16*4];
		
		int runthroughs = rand.nextInt(3) + 4;
		
		//Generate water placement
		for(int i = 0; i < runthroughs; i++)
		{
			double xSize = rand.nextDouble() * 5D + 5D;
			double ySize = rand.nextDouble() * 4D + 3D;
			double zSize = rand.nextDouble() * 5D + 5D;
			double xPos = rand.nextDouble() * (16D - xSize - 4D) + 2D + xSize / 2D;
			double yPos = rand.nextDouble() * (5D - ySize / 2 - 2D) + 2D + ySize / 2D;
			double zPos = rand.nextDouble() * (16D - zSize - 4D) + 2D + zSize / 2D;
			
			for (int x = 2; x < 14; x++)
			{
				for (int z = 2; z < 14; z++)
				{
					for (int y = 1; y < 4; y++)
					{
						double xDiff = (x - xPos) / (xSize / 2D);
						double yDiff = (y - yPos) / (ySize / 2D);
						double zDiff = (z - zPos) / (zSize / 2D);
						double distance = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
						if (distance < 1)
							blocks[((x * 16) + z) * 4 + y] = true;
					}
				}
			}
		}
		
		//Figure out water level and if the ground is flat enough
		int yMin = 256, yMax = 0;
		for (int x = 1; x < 15; x++)
		{
			for (int z = 1; z < 15; z++)
			{
				if (!blocks[((x * 16) + z) * 4 + 3] && hasBlock1(blocks, x, 3, z, true))
				{
					BlockPos topPos = worldIn.getHeight(Heightmap.Type.WORLD_SURFACE_WG, pos.add(x - 8, 0, z - 8)).down();
					yMin = Math.min(yMin, topPos.getY());
					yMax = Math.max(yMax, topPos.getY());
				}
			}
		}
		
		if (yMax - yMin > 1)
			return false;
		
		pos = pos.up(yMin - pos.getY());
		
		BlockPos treePos = null;
		int blockCount = 0;	//Cool way of 100% picking a position while iterating through and checking alternatives; same as used by dispensers
		//Place blocks that make up the base of the oasis
		for (int x = 0; x < 16; x++)
		{
			for (int z = 0; z < 16; z++)
			{
				for (int y = 0; y < 4; y++)
				{
					int index = ((x * 16) + z) * 4 + y;
					if (blocks[index])
						setBlockState(worldIn, pos.add(x - 8, y - 3, z - 8), Blocks.WATER.getDefaultState());
					else if (!blocks[index] && hasBlock1(blocks, x, y, z, false))
						setBlockState(worldIn, pos.add(x - 8, y - 3, z - 8), Blocks.DIRT.getDefaultState());
				}
				
				if (!blocks[((x * 16) + z) * 4 + 3] && hasBlock2(blocks, x, 3, z))
				{
					BlockPos surfacePos = worldIn.getHeight(Heightmap.Type.WORLD_SURFACE, pos.add(x - 8, 0, z - 8));
					setBlockState(worldIn, surfacePos.down(), Blocks.GRASS_BLOCK.getDefaultState());
					if (rand.nextInt(5) == 0)
						setBlockState(worldIn, surfacePos, Blocks.GRASS.getDefaultState());
					if (hasBlock1(blocks, x, 3, z, true))
					{
						blockCount++;
						if (rand.nextInt(blockCount) == 0)
							treePos = surfacePos;
					}
				} else if (blocks[((x * 16) + z) * 4 + 3])
					for (int y = 0; y < 4; y++)
						setBlockState(worldIn, pos.add(x - 8, y + 1, z - 8), Blocks.AIR.getDefaultState());
			}
		}
		
		if(treePos != null)	//This should never not be the case
		{
			int posX = treePos.getX() + 8 - pos.getX();
			int posZ = treePos.getZ() + 8 - pos.getZ();
			int topX = Math.max(4, Math.min(11, posX - 2 + rand.nextInt(3)));
			int topZ = Math.max(4, Math.min(11, posZ - 2 + rand.nextInt(3)));
			BlockPos topPos = pos.add(topX - 8, treePos.getY() - pos.getY() + 5 + rand.nextInt(2), topZ - 8);
			BlockState log = Blocks.JUNGLE_LOG.getDefaultState();
			BlockState leaves = Blocks.JUNGLE_LEAVES.getDefaultState();
			
			BlockPos diff = topPos.subtract(treePos);
			int logChecks = 12;
			for (int i = 0; i <= logChecks; i++)
			{
				BlockPos currentPos = treePos.add(diff.getX() * i / logChecks, diff.getY() * i / logChecks, diff.getZ() * i / logChecks);
				setBlockState(worldIn, currentPos, log);
			}
			
			for (int x = -4; x <= 4; x++)
			{
				for (int z = -4; z <= 4; z++)
				{
					int lowerY = 1;
					if (rand.nextDouble() < Math.sqrt(BlockPos.ZERO.distanceSq(x, 0, z, false)) / 4)
						lowerY = 0;
					int upperY = Math.min(4, 4 - Math.max(Math.abs(x), Math.abs(z)) + rand.nextInt(2));
					if (Math.abs(x) == 4 || Math.abs(z) == 4)
						lowerY -= Math.abs(rand.nextInt(4) - rand.nextInt(4));
					for (int y = lowerY; y <= upperY; y++)
						setBlockState(worldIn, topPos.add(x, y, z), leaves);
				}
			}
			setBlockState(worldIn, topPos.up(), log);
		}
		
		if(rand.nextInt(25) == 0)
		{
			BlockPos chestPos = null;
			for (int y = 1; y < 4 && chestPos == null; y++)
			{
				int count = 0;
				for (int x = 1; x < 15; x++)
				{
					for (int z = 1; z < 15; z++)
						if (blocks[((x * 16) + z) * 4 + y])
						{
							count++;
							if(rand.nextInt(count) == 0)
								chestPos = pos.add(x - 8, y - 3, z - 8);
						}
				}
			}
			
			if(chestPos != null)
			{
				StructureBlockUtil.placeLootChest(chestPos, worldIn, null, Direction.Plane.HORIZONTAL.random(rand), MSLootTables.BASIC_MEDIUM_CHEST, rand);
			}
		}
		
		return true;
	}
	
	private boolean hasBlock1(boolean[] blocks, int x, int y, int z, boolean horizontal)
	{
		if(x > 0 && blocks[((x - 1) * 16 + z) * 4 + y] || x < 15 && blocks[((x + 1) * 16 + z) * 4 + y]
				|| z > 0 && blocks[(x * 16 + (z - 1)) * 4 + y] || z < 15 && blocks[(x * 16 + (z + 1)) * 4 + y])
			return true;
		else if(!horizontal && (y > 0 && blocks[(x * 16 + z) * 4 + y - 1] || y < 3 && blocks[(x * 16 + z) * 4 + y + 1]))
			return true;
		else return false;
	}
	
	private boolean hasBlock2(boolean[] blocks, int x, int y, int z)
	{
		if(hasBlock1(blocks, x, y, z, true) || x > 0 && hasBlock1(blocks, x - 1, y, z, true) || x < 15 && hasBlock1(blocks, x + 1, y, z, true)
				|| z > 1 && blocks[(x * 16 + (z - 2)) * 4 + y] || z < 14 && blocks[(x * 16 + (z + 2)) * 4 + y])
			return true;
		else return false;
	}
}