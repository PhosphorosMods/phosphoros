package lucifertotem.hillman;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/**
 * Doctor Ammonius, the Gadfly: a parody classicist in a papal mitre who wanders the Lightfall ranting about
 * ancient pharmacology. Neutral (hits back with his rolled-up scroll if struck).
 *  - 3 emeralds: sells a Bottle of Theriac
 *  - a book: rewrites it as Sibylline Leaves
 *  - empty hand: a gloss from his lexicon and a true pointer to the nearest Pythia
 *  - shouts PURPLE! when anyone drinks Theriac within 16 blocks (HEvents)
 *  - 1.1.1: follows wheat, breeds with wheat (babies grow up in ~20 min), erratic body language synced to his voice
 */
public class EntityAmmonius extends net.minecraft.entity.passive.EntityAnimal {
    public static final int RANTS = 12, GLOSSES = 10;
    public static int rantsSaid = 0, trades = 0;
    private int rantCooldown = 200;
    /** voice: next line countdown, the last clip (never twice in a row), players he has already greeted */
    private int talkIn = 40;
    public int lastClip = -1;
    private final java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
    public static int linesSpoken = 0, greetings = 0, potionBlurts = 0, repeats = 0;

    /** animation sync: idle mode (low 4 bits) + mode counter; talk kind (low 2 bits) + line counter */
    public static final net.minecraft.network.datasync.DataParameter<Integer> ANIM = net.minecraft.network.datasync.EntityDataManager.createKey(EntityAmmonius.class, net.minecraft.network.datasync.DataSerializers.VARINT);
    public static final net.minecraft.network.datasync.DataParameter<Integer> TALK = net.minecraft.network.datasync.EntityDataManager.createKey(EntityAmmonius.class, net.minecraft.network.datasync.DataSerializers.VARINT);
    private int modeTicks = 60, modeCount = 0, talkCount = 0;
    private BlockPos paceAnchor = null; private boolean paceOut = false;
    /** client: when the current talk burst / idle mode started (ticksExisted) */
    public int talkStart = -1000, modeStart = 0;
    public EntityAITempt tempt;
    public static int modeChanges = 0, hops = 0;

    public EntityAmmonius(World w) { super(w); setSize(0.6f, 1.95f); experienceValue = 0; }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ANIM, 0);
        dataManager.register(TALK, 0);
        dataManager.register(MORTARBOARD, false);
    }

    @Override
    public void notifyDataManagerChange(net.minecraft.network.datasync.DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (TALK.equals(key)) talkStart = ticksExisted;
        else if (ANIM.equals(key)) modeStart = ticksExisted;
    }

    public int animMode() { return dataManager.get(ANIM) & 15; }
    public int animSeed() { return dataManager.get(ANIM) >>> 4; }
    public int talkKind() { return dataManager.get(TALK) & 3; }
    public int talkSeed() { return dataManager.get(TALK) >>> 2; }

    /** the pose for this moment (client render; also usable in tests) */
    public float[] pose(float partial) {
        float age = ticksExisted + partial;
        return AmmoniusPose.compute(age, age - talkStart, talkKind(), talkSeed(), animMode(), age - modeStart, animSeed());
    }

    // ------------------------------------------------------------ wheat: tempting and breeding
    @Override public boolean isBreedingItem(ItemStack s) { return !philologist && (s.getItem() == Items.WHEAT || s.getItem() == lucifertotem.v130.Crops.BARLEY); }

    // ------------------------------------------------------------ 1.2.0: the Wandering Philologist (a visit, not a mob)
    /** a visiting Ammonius: lectures one player for a minute, leaves a page of his notes, walks into a purple rift */
    public boolean philologist = false;
    public java.util.UUID philoFor = null;
    public int philoAge = 0;
    public BlockPos rift = null;
    boolean vanishNow = false;
    public static int philoVisits = 0, philoPages = 0, philoVanished = 0;

    public void makePhilologist(EntityPlayer target) {
        philologist = true; philoFor = target.getUniqueID(); philoAge = 0;
        setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WRITABLE_BOOK));
        setDropChance(EntityEquipmentSlot.MAINHAND, 0f);
        setDropChance(EntityEquipmentSlot.OFFHAND, 0f);
        enablePersistence();
        philoVisits++;
    }

    int talkTicks() { return riftMaker ? Math.max(40, LTConfig.ammon.talkSeconds * 20) : Math.max(200, LTConfig.lore.philologistTalkSeconds * 20); }

    // ------------------------------------------------------------ 1.2.1: a converted librarian (an "Ammon")
    /** made from a dosed librarian: rants briefly, then opens a PERMANENT rift and steps through it */
    public boolean riftMaker = false;
    public boolean riftOpened = false;
    public static int ammonsMade = 0, ammonsGone = 0;

    public void makeRiftMaker(EntityPlayer by) {
        philologist = true; riftMaker = true; philoFor = by.getUniqueID(); philoAge = 0;
        setDropChance(EntityEquipmentSlot.MAINHAND, 0f);
        setDropChance(EntityEquipmentSlot.OFFHAND, 0f);
        enablePersistence();
        ammonsMade++;
    }

    /** a permanent rift near pref (or null: the caller falls back to a temporary one) */
    BlockPos openPermanentRift(BlockPos pref, EntityPlayer t) {
        if (riftOpened || !lucifertotem.world.RiftLogic.mayOpenPermanent()) return null;
        BlockPos b = lucifertotem.world.RiftLogic.findSpot(world, pref);
        if (b == null) b = lucifertotem.world.RiftLogic.findSpot(world, new BlockPos(this));
        if (b == null) return null;
        double dx = b.getX() + 0.5 - posX, dz = b.getZ() + 0.5 - posZ;   // the rift faces the way he walks into it
        net.minecraft.util.EnumFacing.Axis ax = Math.abs(dx) > Math.abs(dz) ? net.minecraft.util.EnumFacing.Axis.Z : net.minecraft.util.EnumFacing.Axis.X;
        EntityPlayer maker = t != null ? t : (philoFor == null ? null : world.getPlayerEntityByUUID(philoFor));
        if (lucifertotem.world.RiftLogic.open(world, b, ax, maker) == null) return null;
        riftOpened = true;
        return b;
    }

    void philoTick() {
        if (vanishNow) { vanish(); return; }
        philoAge++;
        EntityPlayer t = philoFor == null ? null : world.getPlayerEntityByUUID(philoFor);
        int talk = talkTicks();
        if (philoAge < talk) {
            if (t == null || t.dimension != dimension || t.getDistanceSq(this) > 64 * 64) { if (philoAge > 100) philoAge = talk; return; }
            getLookHelper().setLookPositionWithEntity(t, 30f, 30f);
            if (philoAge % 30 == 0 && getDistanceSq(t) > 25) getNavigator().tryMoveToEntityLiving(t, 0.55);
            if (riftMaker ? (philoAge == 30 || (talk > 160 && philoAge == talk / 2)) : (philoAge == 40 || philoAge == talk / 2)) rant();
            return;
        }
        if (philoAge == talk && riftMaker) {
            speak(AmmoniusClips.PURPLE);
            double ang = t == null ? rand.nextDouble() * Math.PI * 2 : Math.atan2(posZ - t.posZ, posX - t.posX);
            BlockPos r0 = new BlockPos(posX + Math.cos(ang) * 3, posY, posZ + Math.sin(ang) * 3);
            rift = openPermanentRift(r0, t);
            if (rift == null) {   // nowhere to leave a door (or permanent rifts are off/capped): a rift that seals behind him
                rift = r0;
                for (int dy = 2; dy >= -3; dy--) { BlockPos q = r0.up(dy); if (world.isAirBlock(q) && world.isAirBlock(q.up()) && world.getBlockState(q.down()).getMaterial().isSolid()) { rift = q; break; } }
                world.playSound(null, rift, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.7f, 1.6f);
            }
            return;
        }
        if (philoAge == talk) {
            // the page, tossed toward the listener, and a rift opens behind him
            net.minecraft.entity.item.EntityItem page = entityDropItem(lucifertotem.lore.LoreBooks.page(), 1.2f);
            if (page != null) {
                page.setPickupDelay(10);
                if (t != null) { double dx = t.posX - posX, dz = t.posZ - posZ, d = Math.max(0.1, Math.sqrt(dx * dx + dz * dz)); page.motionX = dx / d * 0.25; page.motionZ = dz / d * 0.25; page.motionY = 0.25; }
            }
            philoPages++;
            speak(AmmoniusClips.PURPLE);
            double ang = t == null ? rand.nextDouble() * Math.PI * 2 : Math.atan2(posZ - t.posZ, posX - t.posX);
            BlockPos r0 = new BlockPos(posX + Math.cos(ang) * 4, posY, posZ + Math.sin(ang) * 4);
            rift = r0;
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos q = r0.up(dy);
                if (world.isAirBlock(q) && world.isAirBlock(q.up()) && world.getBlockState(q.down()).getMaterial().isSolid()) { rift = q; break; }
            }
            world.playSound(null, rift, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.7f, 1.6f);
            return;
        }
        if (rift == null) { vanish(); return; }
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) world;
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL, rift.getX() + 0.5, rift.getY() + 1.0, rift.getZ() + 0.5, 24, 0.25, 0.9, 0.25, 0.4);
            if (philoAge % 2 == 0) ws.spawnParticle(net.minecraft.util.EnumParticleTypes.SPELL_WITCH, rift.getX() + 0.5, rift.getY() + 1.0, rift.getZ() + 0.5, 6, 0.2, 0.9, 0.2, 0.02);
            if (philoAge % 40 == 0) world.playSound(null, rift, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, 0.6f, 1.5f);
        }
        if (philoAge % 10 == 0) getNavigator().tryMoveToXYZ(rift.getX() + 0.5, rift.getY(), rift.getZ() + 0.5, 0.6);
        if (getDistanceSq(rift.getX() + 0.5, rift.getY(), rift.getZ() + 0.5) < 1.6 || philoAge > talk + 240) vanish();
    }

    /** into the rift: it seals behind him. No drops, no XP, no corpse. */
    public void vanish() {
        if (riftMaker && !riftOpened && !world.isRemote && !isDead) {   // saved mid-rant: he leaves his door anyway
            BlockPos b = openPermanentRift(new BlockPos(this), null);
            if (b != null) rift = b;
        }
        if (riftMaker) ammonsGone++;
        if (world instanceof net.minecraft.world.WorldServer) {
            BlockPos at = rift != null ? rift : new BlockPos(this);
            ((net.minecraft.world.WorldServer) world).spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 120, 0.4, 1.0, 0.4, 1.2);
            ((net.minecraft.world.WorldServer) world).spawnParticle(net.minecraft.util.EnumParticleTypes.SPELL_WITCH, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 40, 0.4, 1.0, 0.4, 0.1);
            world.playSound(null, at, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 1f, 0.7f);
            world.playSound(null, at, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1f, 0.6f);
        }
        philoVanished++;
        setDead();
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        if (philologist && src != DamageSource.OUT_OF_WORLD) {
            if (!world.isRemote && src.getTrueSource() instanceof EntityPlayer && rand.nextInt(3) == 0) speak(AmmoniusClips.GREET);
            return false;   // a visitor: not hurt, not farmable
        }
        return super.attackEntityFrom(src, amount);
    }

    @Override
    public net.minecraft.entity.EntityAgeable createChild(net.minecraft.entity.EntityAgeable mate) {
        EntityAmmonius baby = new EntityAmmonius(world);
        baby.enablePersistence();
        return baby;
    }

    /** idle body language: pacing, sudden freezes then lurches, looking around, crouch-and-spring, lecturing to nobody */
    void idleAnim() {
        if (--modeTicks > 0) {
            int m = animMode();
            if (m == AmmoniusPose.FREEZE || m == AmmoniusPose.LECTURE) { getNavigator().clearPath(); motionX = 0; motionZ = 0; }
            else if (m == AmmoniusPose.PACE && ticksExisted % 30 == 0 && getAttackTarget() == null && getNavigator().noPath()) {
                if (paceAnchor == null || paceAnchor.distanceSq(getPosition()) > 100) paceAnchor = getPosition();
                paceOut = !paceOut;
                double ang = Math.toRadians(renderYawOffset + 90);
                double d = paceOut ? 3.5 : -3.5;
                getNavigator().tryMoveToXYZ(paceAnchor.getX() + 0.5 + Math.cos(ang) * d, paceAnchor.getY(), paceAnchor.getZ() + 0.5 + Math.sin(ang) * d, 0.55);
            }
            return;
        }
        int prev = animMode(), next;
        if (prev == AmmoniusPose.FREEZE) next = AmmoniusPose.LURCH;              // freezes end in a lurch
        else {
            float r = rand.nextFloat();
            next = r < 0.4f ? AmmoniusPose.PACE : r < 0.55f ? AmmoniusPose.FREEZE : r < 0.7f ? AmmoniusPose.LOOK : r < 0.82f ? AmmoniusPose.CROUCH : AmmoniusPose.LECTURE;
        }
        if (prev == AmmoniusPose.CROUCH && onGround) { motionY = 0.5; hops++; }    // ...and springs up
        if (next == AmmoniusPose.LURCH) {
            float yaw = (float) Math.toRadians(rotationYaw);
            motionX += -Math.sin(yaw) * 0.45; motionZ += Math.cos(yaw) * 0.45;
        }
        setMode(next);
    }

    void setMode(int m) {
        modeCount++; modeChanges++;
        dataManager.set(ANIM, (modeCount << 4) | (m & 15));
        switch (m) {
            case AmmoniusPose.FREEZE: modeTicks = 25 + rand.nextInt(45); break;
            case AmmoniusPose.LURCH: modeTicks = 12; break;
            case AmmoniusPose.LOOK: modeTicks = 30 + rand.nextInt(40); break;
            case AmmoniusPose.CROUCH: modeTicks = 20 + rand.nextInt(30); break;
            case AmmoniusPose.LECTURE: modeTicks = 60 + rand.nextInt(80); break;
            default: modeTicks = 60 + rand.nextInt(120);
        }
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIMate(this, 0.6));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.0, true));
        tasks.addTask(3, tempt = new EntityAITempt(this, 0.6, false, lucifertotem.v130.Crops.BARLEY == null ? com.google.common.collect.Sets.newHashSet(Items.WHEAT) : com.google.common.collect.Sets.newHashSet(Items.WHEAT, lucifertotem.v130.Crops.BARLEY)));
        tasks.addTask(3, new EntityAIFollowParent(this, 0.7));
        tasks.addTask(3, new EntityAIMoveTowardsRestriction(this, 0.6));
        tasks.addTask(4, new EntityAIWanderAvoidWater(this, 0.5));
        tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0f, 0.5f));
        tasks.addTask(6, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        // 1.3.0: he fights Peer Reviewers on sight
        targetTasks.addTask(2, new net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, lucifertotem.v130.EntityPeerReviewer.class, true));
    }

    // 1.3.0: a mortarboard on top of the mitre once he has graduated someone (synced, saved)
    private static final net.minecraft.network.datasync.DataParameter<Boolean> MORTARBOARD = net.minecraft.network.datasync.EntityDataManager.createKey(EntityAmmonius.class, net.minecraft.network.datasync.DataSerializers.BOOLEAN);
    public boolean mortarboard() { return dataManager.get(MORTARBOARD); }
    public void setMortarboard(boolean b) { dataManager.set(MORTARBOARD, b); }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(20.0);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance d, IEntityLivingData data) {
        data = super.onInitialSpawn(d, data);
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.PAPER)); // his notes
        setDropChance(EntityEquipmentSlot.MAINHAND, 0f);
        return data;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
        if (hit) { target.addVelocity(-Math.sin(rotationYaw * 0.0174533) * 0.4, 0.1, Math.cos(rotationYaw * 0.0174533) * 0.4); say("chat.lucifertotem.ammonius.whack"); playSound(HSounds.AMMONIUS_ATTACK, getSoundVolume(), 1f); }
        return hit;
    }

    /** chat line from Doctor Ammonius to everyone within 16 blocks */
    public void say(String key, Object... args) {
        ITextComponent t = new TextComponentTranslation("chat.lucifertotem.ammonius_says", new TextComponentTranslation(key, args));
        t.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
        for (EntityPlayer p : world.playerEntities) if (p.getDistanceSq(this) < 256) p.sendMessage(t);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) return;
        voice();
        idleAnim();
        if (philologist) { philoTick(); return; }
        if (--rantCooldown <= 0) {
            rantCooldown = 900 + rand.nextInt(1200);
            EntityPlayer near = world.getClosestPlayerToEntity(this, 12);
            if (near != null && getAttackTarget() == null) rant();
        }
    }

    // ------------------------------------------------------------ the voice (an assault on the senses)
    static int talkGap(java.util.Random r) {
        int lo = Math.max(10, LTConfig.ammoniusTalkTicksMin), hi = Math.max(lo, LTConfig.ammoniusTalkTicksMax);
        return lo + r.nextInt(hi - lo + 1);
    }

    void voice() {
        if (!isEntityAlive()) return;
        // first sight: greet each player once when they come within 12 blocks and he can see them
        if (ticksExisted % 5 == 0) {
            for (EntityPlayer p : world.playerEntities) {
                double d = p.getDistanceSq(this);
                if (d > 32 * 32) { seen.remove(p.getUniqueID()); continue; }
                if (d <= 12 * 12 && !p.isSpectator() && canEntityBeSeen(p) && seen.add(p.getUniqueID())) {
                    getLookHelper().setLookPositionWithEntity(p, 30f, 30f);
                    speak(AmmoniusClips.GREET);
                    greetings++;
                    if (!riftMaker) HAdv.grant(p, philologist ? "philologist" : "meet_ammonius");
                    return;
                }
            }
        }
        if (--talkIn > 0) return;
        if (world.getClosestPlayerToEntity(this, 32) == null) { talkIn = 20; return; }
        int lo = lucifertotem.LTConfig.v130.lectureOneIn;
        if (forceLecture || (!lucifertotem.LTConfig.testHarness && lo > 0 && rand.nextInt(lo) == 0)) { forceLecture = false; lecture(); return; }
        speak(AmmoniusClips.TALK);
    }

    /** says one line from the pool (never the same clip twice in a row), with purple notes and sparkles; no chat */
    public int speak(int[] pool) {
        int c = pool[rand.nextInt(pool.length)];
        for (int tries = 0; c == lastClip && tries < 16; tries++) c = pool[rand.nextInt(pool.length)];
        if (c == lastClip) for (int k : pool) if (k != lastClip) { c = k; break; }
        if (c == lastClip) repeats++;
        lastClip = c;
        linesSpoken++;
        if (c < HSounds.LINES.length && HSounds.LINES[c] != null)
            world.playSound(null, posX, posY + getEyeHeight(), posZ, HSounds.LINES[c], getSoundCategory(), getSoundVolume(), (isChild() ? 1.4f : 1.0f) * (0.95f + rand.nextFloat() * 0.1f));
        int kind = pool == AmmoniusClips.PURPLE ? AmmoniusPose.PURPLE : pool == AmmoniusClips.GREET ? AmmoniusPose.GREET : AmmoniusPose.TALK;
        talkCount++;
        dataManager.set(TALK, (talkCount << 2) | kind);      // the client starts a gesture burst
        if (onGround && (kind == AmmoniusPose.PURPLE || rand.nextInt(4) == 0)) { motionY = 0.36; hops++; }
        flourish();
        talkIn = Math.max(talkGap(rand), AmmoniusClips.TICKS[c] + 4);
        return c;
    }

    void flourish() {
        if (!(world instanceof net.minecraft.world.WorldServer)) return;
        net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) world;
        double y = posY + height + 0.3;
        for (int i = 0; i < 3; i++) // purple-ish notes (note particles take their colour from the x speed when count is 0)
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.NOTE, posX + (rand.nextDouble() - 0.5) * 0.8, y + rand.nextDouble() * 0.4, posZ + (rand.nextDouble() - 0.5) * 0.8, 0, 0.75 + rand.nextDouble() * 0.15, 0, 0, 1.0);
        ws.spawnParticle(net.minecraft.util.EnumParticleTypes.SPELL_WITCH, posX, posY + height * 0.8, posZ, 8, 0.35, 0.3, 0.35, 0.02);
    }

    /** someone within 16 blocks drank a potion */
    public void blurtPurple() { potionBlurts++; speak(AmmoniusClips.PURPLE); }

    @Override public float getSoundVolume() { return (float) LTConfig.ammoniusVolume; }
    @Override protected float getSoundPitch() { return (isChild() ? 1.4f : 1.0f) + (rand.nextFloat() - rand.nextFloat()) * 0.05f; }

    public void rant() {
        rantsSaid++;
        say("rant.lucifertotem.ammonius." + (1 + rand.nextInt(RANTS)));
        speak(rand.nextBoolean() ? AmmoniusClips.RANT : AmmoniusClips.TALK);
        int n = lucifertotem.LTConfig.v130.signOffOneIn;
        if (n > 0 && rand.nextInt(n) == 0) signOff();
    }

    /** 1.3.0: a short lecture: an informative clip, its words in chat for everyone within 16 blocks */
    public static int lectures = 0;
    /** self-test: the next idle line is a lecture */
    public static boolean forceLecture = false;
    public int lecture() { return lecture(-1); }
    public int lecture(int clip) {
        int c = clip >= 0 ? speakClip(clip) : speak(AmmoniusClips.LECTURE);
        lectures++;
        say("chat.lucifertotem.ammonius.lecture", new net.minecraft.util.text.TextComponentTranslation("quote.lucifertotem.ammonius." + AmmoniusClips.NAME[c]));
        return c;
    }
    /** plays one given clip (quest hand-ins pick a fitting line) */
    public int speakClip(int c) { return speak(new int[]{c}); }

    /** 1.3.0: the sign-off that ends a proper rant; everyone in earshot earns "And As Always..." */
    public static int signOffs = 0;
    public int signOff() {
        signOffs++;
        say("rant.lucifertotem.ammonius.signoff");
        int n = 0;
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(16))) { lucifertotem.hillman.HAdv.grant(p, "sign_off"); n++; }
        return n;
    }

    @Override
    public boolean processInteract(EntityPlayer p, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND || getAttackTarget() == p) return false;
        if (lucifertotem.v130.Stage2.officeHours(this, p, hand) != null) return true;   // 1.3.0: Office Hours
        if (philologist) { if (!world.isRemote) rant(); return true; }   // he is lecturing, not trading
        if (isBreedingItem(p.getHeldItem(hand)) || isChild()) return super.processInteract(p, hand);   // wheat feeds/breeds; babies don't trade
        if (world.isRemote) return true;
        ItemStack held = p.getHeldItem(hand);
        if (held.getItem() == Items.EMERALD) {
            if (held.getCount() < 3 && !p.capabilities.isCreativeMode) { say("chat.lucifertotem.ammonius.price"); return true; }
            if (!p.capabilities.isCreativeMode) held.shrink(3);
            give(p, new ItemStack(ModContent.THERIAC));
            trades++;
            say("chat.lucifertotem.ammonius.sold");
            playSound(SoundEvents.ENTITY_VILLAGER_YES, 1f, 0.8f);
            return true;
        }
        if (held.getItem() == Items.MAP) {
            ItemStack map = alexandrianMap(world, new BlockPos(this));
            if (map.isEmpty()) { say("chat.lucifertotem.ammonius.map_none"); return true; }
            if (!p.capabilities.isCreativeMode) held.shrink(1);
            give(p, map);
            trades++;
            say("chat.lucifertotem.ammonius.map");
            playSound(SoundEvents.ENTITY_VILLAGER_YES, 1f, 0.9f);
            return true;
        }
        if (held.getItem() == Items.BOOK) {
            if (!p.capabilities.isCreativeMode) held.shrink(1);
            ItemStack leaves = new ItemStack(HItems.SIBYLLINE);
            HItems.ItemSibylline.setPages(leaves, Prophecies.sibylPages(world, p), rand.nextLong());
            give(p, leaves);
            trades++;
            say("chat.lucifertotem.ammonius.sibyl");
            return true;
        }
        if (held.isEmpty()) {
            say("gloss.lucifertotem." + (1 + rand.nextInt(GLOSSES)));
            ITextComponent dir = Prophecies.pointTo(world, new BlockPos(this), "delphic_vent", 2);
            if (dir != null) say("chat.lucifertotem.ammonius.pythia", dir);
            else say("chat.lucifertotem.ammonius.no_pythia");
            ITextComponent lib = Prophecies.pointTo(world, new BlockPos(this), "library", 0);
            if (lib != null) say("chat.lucifertotem.ammonius.library", lib);
            speak(AmmoniusClips.TALK);
            return true;
        }
        return false;
    }

    /** an explorer-style map with the nearest Library of Alexandria marked (Lightfall only) */
    public static ItemStack alexandrianMap(World w, BlockPos from) {
        if (!(w instanceof net.minecraft.world.WorldServer) || w.provider.getDimension() != lucifertotem.LuciferTotem.dimId) return ItemStack.EMPTY;
        BlockPos lib = StructureIndex.locate((net.minecraft.world.WorldServer) w, "library", from, 160, 0);
        if (lib == null) return ItemStack.EMPTY;
        ItemStack map = net.minecraft.item.ItemMap.setupNewMap(w, lib.getX(), lib.getZ(), (byte) 2, true, true);
        net.minecraft.item.ItemMap.renderBiomePreviewMap(w, map);
        net.minecraft.world.storage.MapData.addTargetDecoration(map, lib, "+", net.minecraft.world.storage.MapDecoration.Type.MANSION);
        map.setTranslatableName("item.lucifertotem.alexandrian_map.name");
        return map;
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound t) {
        super.writeEntityToNBT(t);
        if (philologist) t.setBoolean("LtPhilologist", true);
        if (riftMaker) { t.setBoolean("LtRiftMaker", true); t.setBoolean("LtRiftOpened", riftOpened); }
        if (hasHome()) { t.setLong("LtHome", getHomePosition().toLong()); t.setInteger("LtHomeR", (int) getMaximumHomeDistance()); }
        if (mortarboard()) t.setBoolean("LtMortarboard", true);
    }
    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound t) {
        super.readEntityFromNBT(t);
        if (t.getBoolean("LtPhilologist")) { philologist = true; vanishNow = true; }
        if (t.getBoolean("LtRiftMaker")) { riftMaker = true; riftOpened = t.getBoolean("LtRiftOpened"); }   // a visit never outlasts a save
        if (t.hasKey("LtHome")) setHomePosAndDistance(BlockPos.fromLong(t.getLong("LtHome")), t.getInteger("LtHomeR"));
        setMortarboard(t.getBoolean("LtMortarboard"));
    }

    /** soft dependency: Villager Drop's villager chunks (villagerdrop:piece_of_villager), null if that mod isn't installed */
    public static final net.minecraft.util.ResourceLocation VILLAGER_CHUNK = new net.minecraft.util.ResourceLocation("villagerdrop", "piece_of_villager");
    public static net.minecraft.item.Item villagerChunk() {
        net.minecraft.item.Item i = net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(VILLAGER_CHUNK);
        return i == null || i == Items.AIR ? null : i;
    }

    /** test hook: the death drops without dying */
    public void dropForTest(int looting) { dropFewItems(true, looting); }
    public boolean canDespawnForTest() { return canDespawn(); }

    static void give(EntityPlayer p, ItemStack s) { if (!p.inventory.addItemStackToInventory(s)) p.dropItem(s, false); }

    /** he hears someone drink Theriac nearby */
    public static EntityAmmonius nearest(World w, Entity e, double r) {
        EntityAmmonius best = null; double bd = r * r;
        for (EntityAmmonius a : w.getEntitiesWithinAABB(EntityAmmonius.class, e.getEntityBoundingBox().grow(r))) {
            double d = a.getDistanceSq(e);
            if (d <= bd && a.isEntityAlive()) { bd = d; best = a; }
        }
        return best;
    }

    @Override
    public boolean getCanSpawnHere() {
        BlockPos p = new BlockPos(posX, getEntityBoundingBox().minY, posZ);
        Block below = world.getBlockState(p.down()).getBlock();
        if (!world.canSeeSky(p) || !isNotColliding()) return false;
        boolean lightfall = world.provider.getDimension() == LuciferTotem.dimId;
        if (lightfall) { if (below != ModContent.STAR_GRASS) return false; if (rand.nextDouble() >= LTConfig.ammoniusSpawnChance) return false; }
        else { if (below != Blocks.GRASS || !world.isDaytime()) return false; if (rand.nextDouble() >= LTConfig.ammoniusOverworldChance) return false; }
        return world.getEntitiesWithinAABB(EntityAmmonius.class, new AxisAlignedBB(p).grow(96)).isEmpty();
    }

    @Override public int getMaxSpawnedInChunk() { return 1; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected SoundEvent getAmbientSound() { return null; } // he rants with HSounds.AMMONIUS_IDLE
    private int hurtQuiet;
    /** 1.3.0: hurt and death lines are single clips with their own subtitles; a long hurt line is not restarted on every hit */
    @Override protected SoundEvent getHurtSound(DamageSource s) {
        if (ticksExisted < hurtQuiet) return HSounds.AMMONIUS_HURT;
        int c = AmmoniusClips.HURT[rand.nextInt(AmmoniusClips.HURT.length)];
        hurtQuiet = ticksExisted + AmmoniusClips.TICKS[c] + 10;
        return HSounds.LINES[c];
    }
    @Override protected SoundEvent getDeathSound() { return HSounds.LINES[AmmoniusClips.DEATH[rand.nextInt(AmmoniusClips.DEATH.length)]]; }

    @Override
    protected void dropFewItems(boolean hit, int looting) {
        if (isChild() || philologist) return;                                    // babies drop nothing
        entityDropItem(new ItemStack(Items.BOOK), 0.5f);
        if (rand.nextInt(3) == 0) entityDropItem(new ItemStack(ModContent.THERIAC), 0.5f);
        int lb = Math.max(0, looting);
        net.minecraft.item.Item chunk = villagerChunk();
        if (chunk != null) entityDropItem(new ItemStack(chunk, 1 + rand.nextInt(3) + rand.nextInt(lb + 1)), 0.5f);
        entityDropItem(new ItemStack(Items.EXPERIENCE_BOTTLE, 1 + rand.nextInt(3) + rand.nextInt(lb + 1)), 0.5f);
    }
}
