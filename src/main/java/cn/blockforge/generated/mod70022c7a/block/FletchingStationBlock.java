package cn.blockforge.generated.mod70022c7a.block;

import cn.blockforge.generated.mod70022c7a.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class FletchingStationBlock extends BaseEntityBlock {
    public FletchingStationBlock() {
        super(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).lightLevel(state -> 2));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FletchingStationBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FletchingStationBlockEntity station) {
            NetworkHooks.openScreen((ServerPlayer) player, station, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FletchingStationBlockEntity station) {
                Containers.dropContents(level, pos, station);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FletchingStationBlockEntity station && station.getTier() >= 3 && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                    pos.getY() + 1.05D,
                    pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                    0.0D, 0.08D, 0.0D);
        }
    }
}
