package com.cappleapple.veilvolumelights.content;

import com.cappleapple.veilvolumelights.VeilVolumeLightsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(VeilVolumeLightsMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VeilVolumeLightsMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, VeilVolumeLightsMod.MOD_ID);

    public static final DeferredBlock<PointTestLightSourceBlock> TEST_POINT_LIGHT =
            BLOCKS.registerBlock("test_point_light", PointTestLightSourceBlock::new,
                    properties().noOcclusion());
    public static final DeferredBlock<SpotTestLightSourceBlock> TEST_SPOT_LIGHT =
            BLOCKS.registerBlock("test_spot_light", SpotTestLightSourceBlock::new, properties());
    public static final DeferredBlock<AreaTestLightSourceBlock> TEST_AREA_LIGHT =
            BLOCKS.registerBlock("test_area_light", AreaTestLightSourceBlock::new, properties());

    public static final DeferredItem<BlockItem> TEST_POINT_LIGHT_ITEM = ITEMS.registerSimpleBlockItem(TEST_POINT_LIGHT);
    public static final DeferredItem<BlockItem> TEST_SPOT_LIGHT_ITEM = ITEMS.registerSimpleBlockItem(TEST_SPOT_LIGHT);
    public static final DeferredItem<BlockItem> TEST_AREA_LIGHT_ITEM = ITEMS.registerSimpleBlockItem(TEST_AREA_LIGHT);

    public static final Supplier<BlockEntityType<TestLightSourceBlockEntity>> TEST_LIGHT_SOURCE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("test_light_source", () -> BlockEntityType.Builder.of(
                    TestLightSourceBlockEntity::new,
                    TEST_POINT_LIGHT.get(), TEST_SPOT_LIGHT.get(), TEST_AREA_LIGHT.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(ModContent::addCreativeTabContents);
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TEST_POINT_LIGHT_ITEM.get());
            event.accept(TEST_SPOT_LIGHT_ITEM.get());
            event.accept(TEST_AREA_LIGHT_ITEM.get());
        }
    }

    private ModContent() {
    }
}
