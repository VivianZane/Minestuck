package com.mraof.minestuck.world.gen;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mraof.minestuck.entity.MSEntityTypes;
import com.mraof.minestuck.skaianet.UnderlingController;
import com.mraof.minestuck.world.biome.LandBiomeSource;
import com.mraof.minestuck.world.biome.RegistryBackedBiomeSet;
import com.mraof.minestuck.world.biome.WorldGenBiomeSet;
import com.mraof.minestuck.world.gen.structure.MSStructurePlacements;
import com.mraof.minestuck.world.gen.structure.blocks.StructureBlockRegistry;
import com.mraof.minestuck.world.gen.structure.gate.GateStructure;
import com.mraof.minestuck.world.lands.LandTypePair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LandChunkGenerator extends CustomizableNoiseChunkGenerator
{
	public static final Codec<LandChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RegistryOps.retrieveGetter(Registries.NOISE),
			RegistryOps.retrieveGetter(Registries.DENSITY_FUNCTION),
			LandTypePair.Named.CODEC.fieldOf("named_land_types").forGetter(generator -> generator.namedTypes),
			RegistryOps.retrieveGetter(Registries.BIOME),
			RegistryOps.retrieveGetter(Registries.PLACED_FEATURE),
			RegistryOps.retrieveGetter(Registries.CONFIGURED_CARVER)
	).apply(instance, instance.stable(LandChunkGenerator::create)));
	
	public final LandTypePair.Named namedTypes;
	public final StructureBlockRegistry blockRegistry;
	public final WorldGenBiomeSet biomeSet;
	public final GateStructure.PieceFactory gatePiece;
	
	public static LandChunkGenerator create(HolderGetter<NormalNoise.NoiseParameters> noises, HolderGetter<DensityFunction> densityFunctions, LandTypePair.Named namedTypes,
											HolderGetter<Biome> biomes, HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers)
	{
		RegistryBackedBiomeSet biomeSetWrapper = new RegistryBackedBiomeSet(namedTypes.landTypes().getTerrain().getBiomeSet(), biomes);
		LandGenSettings genSettings = new LandGenSettings(namedTypes.landTypes());
		
		WorldGenBiomeSet biomeHolder = new WorldGenBiomeSet(biomeSetWrapper, genSettings, features, carvers);
		
		return new LandChunkGenerator(noises, densityFunctions, namedTypes, biomeHolder, genSettings);
	}
	
	private LandChunkGenerator(HolderGetter<NormalNoise.NoiseParameters> noises, HolderGetter<DensityFunction> densityFunctions, LandTypePair.Named namedTypes, WorldGenBiomeSet biomes, LandGenSettings genSettings)
	{
		super(new LandBiomeSource(biomes.baseBiomes, genSettings), biome -> biomes.getBiomeFromBase(biome).get().getGenerationSettings(),
				genSettings.createDimensionSettings(noises, densityFunctions));
		
		this.biomeSet = biomes;
		this.namedTypes = namedTypes;
		this.blockRegistry = genSettings.getBlockRegistry();
		this.gatePiece = genSettings.getGatePiece();
	}
	
	@Override
	protected Codec<? extends LandChunkGenerator> codec()
	{
		return CODEC;
	}
	
	@Override
	public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup, RandomState randomState, long seed)
	{
		List<Holder<StructureSet>> list = structureSetLookup.listElements().filter(structureSet -> hasStructureSet(structureSet.value()))
				.<Holder<StructureSet>>map(holder -> holder).toList();
		return new LandStructureState(randomState, biomeSource, seed, list);
	}
	
	private boolean hasStructureSet(StructureSet structureSet)
	{
		return structureSet.structures().stream().flatMap(entry -> entry.structure().value().biomes().stream()).anyMatch(biomeSource.possibleBiomes()::contains);
	}
	
	@Override
	public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> biome, StructureManager structures, MobCategory category, BlockPos pos)
	{
		if(category == MSEntityTypes.UNDERLING)
			return UnderlingController.getUnderlingList(pos);
		else return biomeSet.getBiomeFromBase(biome).value().getMobSettings().getMobs(category);
	}
	
	@Nullable
	@Override
	public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel level, HolderSet<Structure> structureSet, BlockPos pos, int searchRadius, boolean skipKnownStructures)
	{
		var state = level.getChunkSource().getGeneratorState();
		var result = super.findNearestMapStructure(level, structureSet, pos, searchRadius, skipKnownStructures);
		
		if(!(level.getChunkSource().getGeneratorState() instanceof LandStructureState landStructureState))
			return result;
		
		Optional<Holder<Structure>> optionalGateStructure = structureSet.stream().filter(structure -> hasGatePlacement(state, structure)).findAny();
		
		return optionalGateStructure.map(gateStructure -> {
			BlockPos gatePos = landStructureState.getOrFindLandGatePosition().getBlockAt(8, 64, 8);
			if(result != null && pos.distSqr(result.getFirst()) < pos.distSqr(gatePos))
				return result;
			else
				return Pair.of(gatePos, gateStructure);
		}).orElse(result);
	}
	
	private static boolean hasGatePlacement(ChunkGeneratorStructureState state, Holder<Structure> structure)
	{
		return state.getPlacementsForStructure(structure).stream().anyMatch(placement -> placement.type() == MSStructurePlacements.LAND_GATE.get());
	}
}