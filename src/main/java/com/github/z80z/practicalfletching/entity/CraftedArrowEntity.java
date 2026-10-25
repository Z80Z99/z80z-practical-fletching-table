package com.github.z80z.practicalfletching.entity;

import com.github.z80z.practicalfletching.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class CraftedArrowEntity extends AbstractArrow implements IEntityAdditionalSpawnData {
    private CompoundTag arrowData = new CompoundTag();
    /** 弓改装带来的临时加成：不写入箭 NBT，避免污染拾取物导致无法堆叠。 */
    private int bowVelocity;
    private boolean bowFocus;
    /** 命中效果只结算一次（穿透箭可能多次触发）。 */
    private boolean impactApplied;
    /** 弹跳已用次数（轨迹类效果）。 */
    private int bounces;
    /** 返回箭剩余寿命（>0 时递减，归零自毁）。 */
    private int returnLife;

    public CraftedArrowEntity(EntityType<? extends CraftedArrowEntity> type, Level level) {
        super(type, level);
    }

    public CraftedArrowEntity(Level level, LivingEntity shooter) {
        super(GeneratedMod.CRAFTED_ARROW_ENTITY.get(), shooter, level);
    }

    /** 读取效果强化倍率，缺省 1.0。 */
    private static double boost(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getDouble(key) : 1.0D;
    }

    /** 读取递减叠加后的效果 Power；兼容旧存档字段。 */
    private double power(String key, String legacyKey) {
        double value = arrowData.getDouble(key);
        if (value <= 0 && legacyKey != null) value = arrowData.getDouble(legacyKey);
        return value;
    }

    public void configure(ItemStack stack, LivingEntity shooter) {
        if (stack.hasTag()) arrowData = stack.getTag().copy();
        int critical = arrowData.getInt("Critical");
        setBaseDamage(getBaseDamage() + arrowData.getDouble("BonusDamage") + critical * 0.75D);
        setPierceLevel((byte) Math.min(127, arrowData.getInt("Piercing")));
        ItemStack bow = shooter.getUseItem();
        if (bow.hasTag()) {
            bowVelocity = Math.max(0, bow.getTag().getInt("FletchingVelocity"));
            if (bow.getTag().getInt("FletchingFlame") > 0) setSecondsOnFire(160);
            bowFocus = bow.getTag().getInt("FletchingFocus") > 0;
        }
        cacheTickValues();
    }

    // —— 每 tick 用到的数值在箭矢数据确定后解析一次，避免 tick 中反复查 NBT ——
    private double cachedRocket;
    private double cachedDropAccel;
    private double cachedAcceleration;
    private boolean cachedFlame;

    private void cacheTickValues() {
        cachedRocket = power("RocketPower", "Rocket");
        cachedDropAccel = arrowData.getInt("Drop") * 0.05D / 100.0D;
        cachedAcceleration = arrowData.getInt("Acceleration");
        cachedFlame = arrowData.getInt("Flame") > 0;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        double enchantBoost = boost(arrowData, "EnchantBoost");
        int acceleration = arrowData.getInt("Acceleration");
        float speedMultiplier = (float) (1.0F + acceleration * 0.18F * enchantBoost + bowVelocity * 0.18F);
        int stability = arrowData.getInt("Stability") + (bowFocus ? 2 : 0);
        float spreadMultiplier = (float) Math.max(0.05F, 1.0F - stability * 0.2F * enchantBoost);
        super.shoot(x, y, z, velocity * speedMultiplier, inaccuracy * spreadMultiplier);
        if (acceleration >= 3) setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (returnLife > 0 && --returnLife <= 0) {
            discard();
            return;
        }
        // 烟花：平飞（重力随 Power 递减，而非提速）
        if (cachedRocket > 0 && !isInWater()) {
            double factor = Math.max(0.05D, 1.0D - 0.5D * cachedRocket);
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.05D * (1.0D - factor), 0.0D));
        }
        // 箭头重量修正
        if (cachedDropAccel != 0 && !isNoGravity() && !isInWater()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -cachedDropAccel, 0.0D));
        }
        // 客户端尾迹
        if (level().isClientSide && (cachedRocket > 0 || cachedAcceleration > 0)) {
            level().addParticle(cachedFlame ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
                    getX(), getY(), getZ(), -getDeltaMovement().x * 0.05, -getDeltaMovement().y * 0.05, -getDeltaMovement().z * 0.05);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        if (arrowData.getBoolean("PotionOnly") && target instanceof LivingEntity living) {
            CompoundTag potionData = arrowData.getCompound("PotionData");
            List<MobEffectInstance> effects = PotionUtils.getAllEffects(potionData);
            for (MobEffectInstance effect : effects) {
                int duration = Math.max(1, effect.getDuration() / 8);
                living.addEffect(new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
            }
            // 药水箭同样结算冲击/地形效果（制作时已消耗对应材料）
            applyImpactEffects(living);
            discard();
            return;
        }
        super.onHitEntity(hit);
        if (level().isClientSide) return;
        if (target instanceof LivingEntity living) {
            int venom = arrowData.getInt("Venom");
            if (venom > 0) living.addEffect(new MobEffectInstance(MobEffects.POISON, 60 * venom, Math.max(0, venom - 1)));
            int incineration = arrowData.getInt("Incineration");
            if (incineration > 0) living.setSecondsOnFire(3 * incineration);
            int lifesteal = arrowData.getInt("Lifesteal");
            if (lifesteal > 0 && getOwner() instanceof LivingEntity shooter) shooter.heal(0.5F * lifesteal);
            // 粘着：把功率与到期时间写到目标身上，由事件每 20 tick 结算，不依赖箭矢存活
            double stick = power("StickPower", null);
            if (stick > 0 && level() instanceof ServerLevel server) {
                CompoundTag data = living.getPersistentData();
                data.putDouble("FletchingStickPower", stick);
                data.putLong("FletchingStickUntil", server.getGameTime() + 20L * 20L);
            }
        }
        applyImpactEffects(target instanceof LivingEntity living ? living : null);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        double bounce = power("BouncePower", null);
        int maxBounces = (int) Math.floor(bounce);
        if (maxBounces > 0 && bounces < Math.min(2, maxBounces)) {
            bounces++;
            Direction side = hit.getDirection();
            Vec3 motion = getDeltaMovement();
            double keep = 0.6D + 0.2D * bounce;
            setDeltaMovement(side.getAxis() == Direction.Axis.X ? motion.multiply(-keep, keep, keep)
                    : side.getAxis() == Direction.Axis.Y ? motion.multiply(keep, -keep, keep)
                    : motion.multiply(keep, keep, -keep));
            // 推出碰撞箱，避免下一 tick 重复命中同一方块
            setPos(hit.getLocation().add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.5D)));
            setNoPhysics(false);
            return;
        }
        super.onHitBlock(hit);
        if (!level().isClientSide) applyImpactEffects(null);
    }

    /** 在落点附近寻找可站立的安全位置；找不到返回 null。 */
    private static Vec3 findSafeLanding(ServerLevel server, Vec3 at) {
        if (at.y < server.getMinBuildHeight() + 2) return null;
        BlockPos base = BlockPos.containing(at);
        for (int dy = 3; dy >= -3; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    if (!server.isLoaded(pos)) continue;
                    if (server.getBlockState(pos).isAir() && server.getBlockState(pos.above()).isAir()
                            && !server.getBlockState(pos.below()).isAir()
                            && server.getFluidState(pos).isEmpty()) {
                        return new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                    }
                }
            }
        }
        return null;
    }

    /** 冲击/地形类效果：命中实体或方块都会触发，且只结算一次。 */
    private void applyImpactEffects(LivingEntity target) {
        if (impactApplied) return;
        impactApplied = true;
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 at = position();

        // —— 冲击类 ——
        double shock = power("ShockPower", "Explosion");
        if (shock > 0) {
            double radius = 2.0D + Math.round(2.0D * shock);
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
                if (entity == getOwner()) continue;
                Vec3 push = entity.position().subtract(at).normalize().scale(0.5D * shock);
                entity.push(push.x, 0.25D * shock, push.z);
            }
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, 0.4, 0.2, 0.4, 0);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        double horn = power("HornPower", null);
        if (horn > 0) {
            double radius = 4.0D + 2.0D * horn;
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
                if (entity == getOwner()) continue;
                Vec3 push = entity.position().subtract(at).normalize().scale(0.4D * horn);
                entity.push(push.x, 0.15D * horn, push.z);
            }
            server.playSound(null, at.x, at.y, at.z, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        }
        double pierce = power("PiercePower", null);
        if (pierce > 0 && target != null) {
            target.hurt(damageSources().magic(), (float) (1.0D * pierce));
        }
        double resonance = power("ResonancePower", null);
        if (resonance > 0) {
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3.0D))) {
                if (entity == getOwner()) continue;
                entity.hurt(damageSources().magic(), (float) (0.6D * resonance));
            }
            server.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 12, 0.6, 0.3, 0.6, 0.2);
        }
        double cloud = power("CloudPower", null);
        if (cloud > 0) {
            double radius = 1.5D + 0.8D * cloud;
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
                if (entity == getOwner()) continue;
                entity.hurt(damageSources().dragonBreath(), (float) (2.0D * cloud));
            }
            server.sendParticles(ParticleTypes.DRAGON_BREATH, at.x, at.y, at.z, 20, radius * 0.4, 0.2, radius * 0.4, 0.02);
        }

        // —— 实体交互类 ——
        double chorus = power("ChorusPower", null);
        if (chorus > 0 && target != null && !(target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) {
            double distance = 8.0D * chorus;
            double x = target.getX() + (random.nextDouble() - 0.5D) * distance;
            double z = target.getZ() + (random.nextDouble() - 0.5D) * distance;
            target.randomTeleport(x, target.getY(), z, false);
        }
        double yank = power("YankPower", null);
        if (yank > 0 && target != null && getOwner() instanceof LivingEntity shooter) {
            Vec3 pull = shooter.position().subtract(target.position()).normalize().scale(0.7D * yank);
            target.push(pull.x, 0.2D * yank, pull.z);
        }
        double bell = power("BellPower", null);
        if (bell > 0) {
            int duration = (int) (100 * bell);
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(8.0D))) {
                if (!(entity instanceof Mob mob) || entity == getOwner()) continue;
                mob.setTarget(null);
                mob.getNavigation().moveTo(at.x, at.y, at.z, 1.1D);
                mob.setLastHurtByMob(null);
                // 记录临时吸引计时（用现有 AI 的游荡目标近似“嘲讽”）
                mob.getPersistentData().putLong("FletchingTaunt", server.getGameTime() + duration);
            }
            server.playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        double pearl = power("PearlPower", null);
        if (pearl > 0 && getOwner() instanceof LivingEntity shooter && shooter.isAlive() && !shooter.isRemoved()
                && !(shooter instanceof net.minecraft.world.entity.player.Player p && (p.isSpectator() || p.isCreative()))) {
            Vec3 safe = findSafeLanding(server, at);
            if (safe != null) {
                shooter.teleportTo(safe.x, safe.y, safe.z);
                shooter.resetFallDistance();
                server.sendParticles(ParticleTypes.PORTAL, safe.x, safe.y, safe.z, 24, 0.3, 0.5, 0.3, 0.4);
            }
        }

        // —— 地形类 ——
        double fire = power("FirePower", "Flame");
        if (fire > 0) {
            int radius = 1 + (int) Math.round(1.5D * fire);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(at).offset(-radius, -1, -radius),
                    BlockPos.containing(at).offset(radius, 1, radius))) {
                if (server.random.nextDouble() > 0.5D) continue;
                BlockPos above = pos.above();
                if (server.isEmptyBlock(above) && BaseFireBlock.canBePlacedAt(server, above, Direction.UP)) {
                    server.setBlockAndUpdate(above, BaseFireBlock.getState(server, above));
                }
            }
        }
        double web = power("WebPower", null);
        if (web > 0) {
            int count = (int) Math.round(2 * web);
            BlockPos center = BlockPos.containing(at);
            for (int i = 0; i < count; i++) {
                BlockPos pos = center.offset(server.random.nextInt(3) - 1, 1, server.random.nextInt(3) - 1);
                if (server.isEmptyBlock(pos)) server.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
            }
        }
        double bone = power("BonePower", null);
        if (bone > 0) {
            int radius = 1 + (int) Math.floor(bone);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(at).offset(-radius, -1, -radius),
                    BlockPos.containing(at).offset(radius, 1, radius))) {
                BlockState state = server.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock crop) {
                    server.setBlockAndUpdate(pos, crop.getStateForAge(Math.min(crop.getMaxAge(), crop.getAge(state) + 1 + (int) bone)));
                }
            }
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 12, 0.5, 0.3, 0.5, 0);
        }
        double snow = power("SnowPower", null);
        if (snow > 0) {
            int radius = 1 + (int) Math.floor(snow);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(at).offset(-radius, -1, -radius),
                    BlockPos.containing(at).offset(radius, 1, radius))) {
                BlockState state = server.getBlockState(pos);
                if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                    server.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                if (server.getBlockState(pos.above()).is(Blocks.FIRE)) {
                    server.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                }
            }
            server.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 16, 0.5, 0.3, 0.5, 0.01);
        }
        double light = power("LightPower", "Glow");
        if (light > 0) {
            int radius = 1 + (int) Math.floor(light);
            BlockPos center = BlockPos.containing(at);
            if (server.isEmptyBlock(center.above()) && server.getBlockState(center).isSolidRender(server, center)) {
                server.setBlockAndUpdate(center.above(), Blocks.LIGHT.defaultBlockState());
            }
            server.sendParticles(ParticleTypes.GLOW, at.x, at.y, at.z, 8, radius * 0.3, 0.2, radius * 0.3, 0);
        }

        // —— 回旋：命中后生成一支返回箭飞向射手（原箭正常消失，避免依赖实体存活） ——
        double echo = power("EchoPower", null);
        if (echo > 0 && getOwner() instanceof LivingEntity shooter && (echo >= 0.7D || random.nextDouble() < echo)) {
            if (server != null) {
                CraftedArrowEntity back = new CraftedArrowEntity(server, shooter);
                back.setPos(at.x, at.y, at.z);
                back.pickup = Pickup.DISALLOWED;
                back.setNoPhysics(true);
                Vec3 dir = shooter.position().add(0, shooter.getBbHeight() * 0.5D, 0).subtract(at).normalize().scale(1.2D);
                back.setDeltaMovement(dir);
                back.returnLife = 40;
                server.addFreshEntity(back);
                server.sendParticles(ParticleTypes.PORTAL, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.3);
            }
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        ItemStack result = new ItemStack(GeneratedMod.CRAFTED_ARROW.get());
        if (!arrowData.isEmpty()) result.setTag(arrowData.copy());
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("CraftedArrowData", arrowData.copy());
        tag.putInt("Bounces", bounces);
        tag.putInt("ReturnLife", returnLife);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        arrowData = tag.getCompound("CraftedArrowData").copy();
        bounces = tag.getInt("Bounces");
        returnLife = tag.getInt("ReturnLife");
        cacheTickValues();
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeNbt(arrowData);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        CompoundTag received = buffer.readNbt();
        arrowData = received == null ? new CompoundTag() : received;
        cacheTickValues();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
