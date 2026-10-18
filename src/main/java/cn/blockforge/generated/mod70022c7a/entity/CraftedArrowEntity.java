package cn.blockforge.generated.mod70022c7a.entity;

import cn.blockforge.generated.mod70022c7a.GeneratedMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class CraftedArrowEntity extends AbstractArrow implements IEntityAdditionalSpawnData {
    private CompoundTag arrowData = new CompoundTag();

    public CraftedArrowEntity(EntityType<? extends CraftedArrowEntity> type, Level level) {
        super(type, level);
    }

    public CraftedArrowEntity(Level level, LivingEntity shooter) {
        super(GeneratedMod.CRAFTED_ARROW_ENTITY.get(), shooter, level);
    }

    /** 读取效果增幅，缺省 1.0。 */
    private static double boost(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getDouble(key) : 1.0D;
    }

    public void configure(ItemStack stack, LivingEntity shooter) {
        if (stack.hasTag()) arrowData = stack.getTag().copy();
        int critical = arrowData.getInt("Critical");
        setBaseDamage(getBaseDamage() + arrowData.getDouble("BonusDamage") + critical * 0.75D);
        setPierceLevel((byte) Math.min(127, arrowData.getInt("Piercing")));
        setKnockback((int) Math.round(arrowData.getInt("Knockback") * boost(arrowData, "ItemBoost")));
        int flame = arrowData.getInt("Flame");
        if (flame > 0) setSecondsOnFire((int) (100L * flame * boost(arrowData, "ItemBoost")));
        ItemStack bow = shooter.getUseItem();
        if (bow.hasTag()) {
            arrowData.putInt("BowVelocity", bow.getTag().getInt("FletchingVelocity"));
            if (bow.getTag().getInt("FletchingFlame") > 0) setSecondsOnFire(160);
            if (bow.getTag().getInt("FletchingFocus") > 0) arrowData.putInt("Stability", Math.max(2, arrowData.getInt("Stability")));
        }
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        double enchantBoost = boost(arrowData, "EnchantBoost");
        double itemBoost = boost(arrowData, "ItemBoost");
        int acceleration = arrowData.getInt("Acceleration");
        int rocket = arrowData.getInt("Rocket");
        int bowVelocity = arrowData.getInt("BowVelocity");
        float speedMultiplier = (float) (1.0F + acceleration * 0.18F * enchantBoost + rocket * 0.22F * itemBoost + bowVelocity * 0.18F);
        float spreadMultiplier = (float) Math.max(0.05F, 1.0F - arrowData.getInt("Stability") * 0.2F * enchantBoost);
        super.shoot(x, y, z, velocity * speedMultiplier, inaccuracy * spreadMultiplier);
        if (rocket > 0 || acceleration >= 3) setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        // 箭头重量修正：正值下坠更快，负值更飘（无重力箭不受影响）
        if (!isNoGravity() && !isInWater()) {
            int drop = arrowData.getInt("Drop");
            if (drop != 0) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.05D * drop / 100.0D, 0.0D));
            }
        }
        if (level().isClientSide && (arrowData.getInt("Rocket") > 0 || arrowData.getInt("Acceleration") > 0)) {
            level().addParticle(arrowData.getInt("Flame") > 0 ? net.minecraft.core.particles.ParticleTypes.FLAME : net.minecraft.core.particles.ParticleTypes.END_ROD,
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
            if (arrowData.getInt("Glow") > 0) living.setGlowingTag(true);
            discard();
            return;
        }
        super.onHitEntity(hit);
        if (!level().isClientSide) {
            if (arrowData.getInt("Glow") > 0) target.setGlowingTag(true);
            if (target instanceof LivingEntity living) {
                int venom = arrowData.getInt("Venom");
                if (venom > 0) living.addEffect(new MobEffectInstance(MobEffects.POISON, 60 * venom, Math.max(0, venom - 1)));
                int incineration = arrowData.getInt("Incineration");
                if (incineration > 0) living.setSecondsOnFire(3 * incineration);
                int lifesteal = arrowData.getInt("Lifesteal");
                if (lifesteal > 0 && getOwner() instanceof LivingEntity shooter) shooter.heal(0.5F * lifesteal);
            }
            int shatter = arrowData.getInt("Shatter");
            int explosion = arrowData.getInt("Explosion");
            if (shatter > 0 || explosion > 0) {
                double enchantBoost = boost(arrowData, "EnchantBoost");
                double itemBoost = boost(arrowData, "ItemBoost");
                float power = (float) Math.min(3.0F, 0.6F + shatter * 0.3F * enchantBoost + explosion * 0.45F * itemBoost);
                level().explode(this, getX(), getY(), getZ(), power, Level.ExplosionInteraction.NONE);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        arrowData = tag.getCompound("CraftedArrowData").copy();
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeNbt(arrowData);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        CompoundTag received = buffer.readNbt();
        arrowData = received == null ? new CompoundTag() : received;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
