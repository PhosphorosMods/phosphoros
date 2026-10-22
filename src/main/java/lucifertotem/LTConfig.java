package lucifertotem;

import net.minecraftforge.common.config.Config;

@Config(modid = LuciferTotem.MODID, name = "lucifertotem")
public class LTConfig {
    @Config.Comment("Dimension ID of the Lightfall. Must not collide with other dimensions (pack uses -1,0,1,4 Aether,7 Twilight,111 Lost Cities). Changing it after players visited moves the dimension (old DIM folder stays on disk).")
    @Config.RequiresMcRestart
    public static int dimensionId = 616;

    @Config.Comment("Damage dealt to bosses (entities with isNonBoss()==false) by the purple bolt.")
    @Config.RangeDouble(min = 0, max = 10000)
    public static double bossDamage = 60.0;

    @Config.Comment("Cooldown in ticks after throwing the Totem (20 = 1 second).")
    @Config.RangeInt(min = 0, max = 1200)
    public static int throwCooldownTicks = 20;

    @Config.Comment("If true, smiting a villager opens a portal to the Lightfall.")
    public static boolean villagerPortals = true;

    @Config.Comment("1.2.0 Overworld lore: the Alexandrian galley wrecks, Fallen Altars, the Wandering Philologist and the totem's whispers.")
    public static Lore lore = new Lore();

    @Config.Comment("1.3.1: size of the 3D Alabastron stuck on someone's lower back (1.0 = the old 1.3.0 size).")
    @Config.RangeDouble(min = 0.5, max = 2.5)
    public static double alabastronLodgedScale = 1.5;

    @Config.Comment("1.3.1: forget the last mob you hit / that hit you when you leave a dimension, so the unloaded Lightfall can be freed from memory. Leave on.")
    public static boolean leakGuard = true;

    @Config.Comment("1.3.1 (client): when more than this many light changes are waiting (vanilla only processes them once every chunk mesh is rebuilt, which never happens while you fly), hand them to the chunk renderer at once. Stops the memory climb that ended in 'OutOfMemoryError' in the Lightfall. 0 = vanilla behaviour.")
    @Config.RangeInt(min = 0, max = 1000000)
    public static int lightBacklogCap = 8192;

    @Config.Comment("1.2.1: a laced Alabastron delivered from behind to a villager of these professions turns them into Doctor Ammonius, who rants, opens a purple rift and steps through it.")
    public static Ammon ammon = new Ammon();

    public static class Ammon {
        @Config.Comment("Turn on the villager -> Ammon conversion.")
        public boolean conversionEnabled = true;
        @Config.Comment("Villager professions that convert (registry names; 'librarian' means 'minecraft:librarian'). Adults only.")
        public String[] professions = {"minecraft:librarian"};
        @Config.Comment("How long the new Ammon rants before stepping into his rift (seconds).")
        @Config.RangeInt(min = 2, max = 120) public int talkSeconds = 8;
        @Config.Comment("Rifts opened by converted Ammons stay open as permanent two-way portals into the Lightfall. If false they seal behind him like the Philologist's.")
        public boolean permanentRifts = true;
        @Config.Comment("At most this many permanent rifts per server (beyond it, new ones seal behind him). 0 = no permanent rifts.")
        @Config.RangeInt(min = 0, max = 10000) public int maxPermanentRifts = 64;
        @Config.Comment("Who may close a rift (sneak + use the Totem of Lucifer on it): true = anyone with a totem, false = only the player who made it (and operators).")
        public boolean anyoneMayClose = true;
    }

    public static class Lore {
        @Config.Comment("An Alexandrian galley wreck in 1 of this many ocean/beach chunks of the Overworld (0 = none).")
        @Config.RangeInt(min = 0, max = 100000) public int galleyChance = 40;
        @Config.Comment("A Fallen Altar in 1 of this many land chunks of the Overworld (0 = none).")
        @Config.RangeInt(min = 0, max = 100000) public int altarChance = 200;
        @Config.Comment("Overworld altars (and any 4x5 duskstone-brick frame there) only light at dusk (world time 11000-14500). The Lightfall is always at dusk.")
        public boolean altarDuskOnly = true;
        @Config.Comment("Within this many blocks of a Fallen Altar the distant 'PURPLE' voice comes more often ...")
        @Config.RangeInt(min = 0, max = 512) public int altarAmbienceRadius = 96;
        @Config.Comment("... namely every (distantVoiceMinutesOverworld x this) minutes on average, from the altar's direction.")
        @Config.RangeDouble(min = 0.01, max = 1) public double altarAmbienceFactor = 0.2;
        @Config.Comment("Wandering Philologist: chance per hour of Overworld play (percent) that Doctor Ammonius visits a player.")
        @Config.RangeDouble(min = 0, max = 100) public double philologistChancePercentPerHour = 8;
        @Config.Comment("How many visits each player gets at most (0 = never).")
        @Config.RangeInt(min = 0, max = 100) public int philologistMaxPerPlayer = 1;
        @Config.Comment("He only visits players who have held the Totem of Lucifer.")
        public boolean philologistRequiresTotem = true;
        @Config.Comment("How long he lectures before stepping into his rift (seconds).")
        @Config.RangeInt(min = 10, max = 600) public int philologistTalkSeconds = 60;
        @Config.Comment("A carried Totem of Lucifer whispers a hint toward the next step about every this many seconds (0 = never).")
        @Config.RangeInt(min = 0, max = 3600) public int totemWhisperSeconds = 75;
        @Config.Comment("How far (in chunks) the whispers look for wrecks and altars.")
        @Config.RangeInt(min = 8, max = 160) public int whisperSearchChunks = 64;
    }

    @Config.Comment("Structure chances, per chunk, out of 1000 (only placed where the site fits).")
    public static Structures structures = new Structures();

    public static class Structures {
        @Config.RangeInt(min = 0, max = 1000) public int morningstarCitadel = 2;
        @Config.RangeInt(min = 0, max = 1000) public int fallenSpire = 30;
        @Config.RangeInt(min = 0, max = 1000) public int sunkenShrine = 35;
        @Config.RangeInt(min = 0, max = 1000) public int lightShrine = 50;
    }

    @Config.Comment("Sea set-piece chances, per chunk, out of 1000 (only where the sea is deep enough).")
    public static Sea sea = new Sea();

    public static class Sea {
        @Config.RangeInt(min = 0, max = 1000) public int drownedChoir = 12;
        @Config.RangeInt(min = 0, max = 1000) public int giantSkeleton = 6;
        @Config.RangeInt(min = 0, max = 1000) public int angelStatue = 18;
        @Config.RangeInt(min = 0, max = 1000) public int floatingHalo = 25;
        @Config.RangeInt(min = 0, max = 1000) public int whirlpool = 20;
        @Config.RangeInt(min = 0, max = 1000) public int boneReef = 30;
    }

    @Config.Comment("Medusa spawn weight in the Lightfall sea (WATER_CREATURE, vanilla cap applies). 0 disables.")
    @Config.RequiresMcRestart
    public static int medusaWeight = 10;

    @Config.Comment("Chance (0-1) that a Medusa drops a Priapus Totem; each Looting level adds lootingBonus.")
    public static double priapusDropChance = 0.18;
    public static double priapusLootingBonus = 0.06;

    @Config.Comment("Priapus Totem: ticks between extra half-hearts of healing while carried (40 = 1 heart per 4 s).")
    @Config.RangeInt(min = 5, max = 1200)
    public static int priapusHealInterval = 40;

    @Config.Comment("Land Medusae (winged purple-haired angels) on Lightfall islands: spawn weight; 0 disables natural spawns.")
    @Config.RequiresMcRestart
    public static int landMedusaWeight = 10;

    @Config.Comment("Chance (0-1) that a valid Land Medusa spawn attempt succeeds.")
    public static double landMedusaSpawnChance = 0.02;

    @Config.Comment("At most this many Land Medusae within 32 blocks of a spawn attempt.")
    @Config.RangeInt(min = 0, max = 20)
    public static int landMedusaLocalCap = 3;

    @Config.Comment("Chance (0-1) that a Land Medusa drops a Bottle of Theriac; each Looting level adds theriacLootingBonus.")
    public static double theriacDropChance = 0.17;
    public static double theriacLootingBonus = 0.05;

    @Config.Comment("Aether (aether_legacy) Valkyries, only when the Aether is installed: natural spawn weight on Lightfall islands (0 disables natural spawns).")
    @Config.RequiresMcRestart
    public static int valkyrieWeight = 4;

    @Config.Comment("Chance (0-1) that an otherwise valid natural Valkyrie spawn attempt on an island succeeds; keeps them rare.")
    public static double valkyrieSpawnChance = 0.002;

    @Config.Comment("Valkyries only roost on 1 in N chunks (fixed per world, so the same islands are haunted).")
    @Config.RangeInt(min = 1, max = 1000)
    public static int valkyrieRoostRarity = 6;

    @Config.Comment("At most this many naturally spawned Valkyries within 64 blocks of a spawn attempt.")
    @Config.RangeInt(min = 0, max = 20)
    public static int valkyrieLocalCap = 1;

    @Config.Comment("Use Aether Valkyries in Fallen Spire / Morningstar Citadel spawners when the Aether is installed.")
    public static boolean valkyrieSpawners = true;

    // ------------------------------------------------------------------ 1.1.0 (Hillman content)
    @Config.Comment("PURPLE! shout (chat line + sound + violet burst) on drinking Theriac, killing a Medusa, curing Insania, etc.")
    public static boolean purpleShout = true;

    @Config.Comment("Random PURPLE! shout in the Lightfall: once a minute a 1-in-N chance per player (0 disables).")
    @Config.RangeInt(min = 0, max = 100000)
    public static int purpleRandomOneIn = 240;

    @Config.Comment("PURPLE! when a player kills a Medusa: 1 in N kills (1 = every kill, 0 = never).")
    @Config.RangeInt(min = 0, max = 1000)
    public static int purpleOnMedusaKillOneIn = 3;

    @Config.Comment("Distant ambient voice ('Aww gawwwd... PURPLE'), played client-side far away. Master switch.")
    public static boolean distantAmbient = true;

    @Config.Comment("Average minutes between distant ambient voices in the Lightfall (0 = never there). 1.1.1: replaces distantAmbientMinutesLightfall (was 20, far too rare).")
    @Config.RangeInt(min = 0, max = 10000)
    public static int distantVoiceMinutesLightfall = 6;

    @Config.Comment("Average minutes between distant ambient voices in the Overworld (0 = never there). 1.1.1: replaces distantAmbientMinutesOverworld (was 90).")
    @Config.RangeInt(min = 0, max = 10000)
    public static int distantVoiceMinutesOverworld = 25;

    @Config.Comment("Percent of fishing catches (any dimension) that are a Totem of Lucifer. Luck of the Sea raises it slightly. 0 = off.")
    @Config.RangeDouble(min = 0, max = 50)
    public static double totemFishingChancePercent = 5;

    @Config.Comment("Doctor Ammonius talks (voice only, no chat) every ammoniusTalkTicksMin..Max ticks while a player is within 32 blocks (20 ticks = 1 s; never over his own line).")
    @Config.RangeInt(min = 10, max = 24000)
    public static int ammoniusTalkTicksMin = 40;

    @Config.RangeInt(min = 10, max = 24000)
    public static int ammoniusTalkTicksMax = 80;

    @Config.Comment("Volume of Doctor Ammonius' voice. Above 1.0 he is also heard further away (16 blocks x volume; 2.0 = 32 blocks).")
    @Config.RangeDouble(min = 0.1, max = 4.0)
    public static double ammoniusVolume = 1.8;

    @Config.Comment("Mandrake scream damage to everything within 6 blocks when pulled up the wrong way.")
    @Config.RangeDouble(min = 0, max = 100)
    public static double mandrakeScreamDamage = 8.0;

    @Config.Comment("Henbane madness (Insania) duration in seconds.")
    @Config.RangeInt(min = 1, max = 3600)
    public static int insaniaSeconds = 90;

    @Config.Comment("Insania: show fake phantom mobs (client side only, harmless).")
    public static boolean insaniaPhantoms = true;

    @Config.Comment("Use vanilla post shaders for Insania (wobble), Petrified (desaturate) and Pneuma (phosphor). Client side.")
    public static boolean effectShaders = true;

    @Config.Comment("Percent chance that opening the Sibylline Leaves outdoors in the Lightfall scatters the pages on the wind.")
    @Config.RangeInt(min = 0, max = 100)
    public static int sibylScatterPercent = 10;

    @Config.Comment("Seconds of uninterrupted chanting the Thessalian witches need to draw down the Morningstar.")
    @Config.RangeInt(min = 10, max = 3600)
    public static int ritualSeconds = 300;

    @Config.Comment("Duration of the Trophonian gloom (chat suffix *(gloomily)*) in ticks; 24000 = one Minecraft day.")
    @Config.RangeInt(min = 0, max = 240000)
    public static int gloomTicks = 24000;

    @Config.Comment("Dipsas viper spawn weight on Lightfall islands (0 disables).")
    @Config.RequiresMcRestart
    public static int dipsasWeight = 6;

    @Config.Comment("Dipsas viper spawn weight in Overworld deserts and beaches (0 disables).")
    @Config.RequiresMcRestart
    public static int dipsasOverworldWeight = 1;

    @Config.Comment("Doctor Ammonius spawn weight in the Lightfall (0 disables natural spawns).")
    @Config.RequiresMcRestart
    public static int ammoniusWeight = 3;

    @Config.Comment("Chance (0-1) that a valid natural Doctor Ammonius spawn succeeds (keeps him rare).")
    public static double ammoniusSpawnChance = 0.05;

    @Config.Comment("Doctor Ammonius very rarely wanders the Overworld (plains/forest), weight; 0 disables.")
    @Config.RequiresMcRestart
    public static int ammoniusOverworldWeight = 1;

    @Config.Comment("Chance (0-1) that a valid Overworld Doctor Ammonius spawn succeeds.")
    public static double ammoniusOverworldChance = 0.004;

    @Config.Comment("A Gorgon guards the heart of each Morningstar Citadel generated from now on.")
    public static boolean gorgonInCitadel = true;

    @Config.Comment("1.1.0 Lightfall structures, per chunk, out of 1000 (rolled after the older structures, which are unchanged).")
    public static Structures110 structures110 = new Structures110();

    public static class Structures110 {
        @Config.RangeInt(min = 0, max = 1000) public int trophoniusPit = 6;
        @Config.RangeInt(min = 0, max = 1000) public int delphicVent = 6;
        @Config.RangeInt(min = 0, max = 1000) public int witchCircle = 8;
    }

    @Config.Comment("The Library of Alexandria: a rare mansion-like library on its own floating island in the Lightfall.")
    public static Library library = new Library();

    public static class Library {
        @Config.Comment("Generate Libraries of Alexandria (affects newly generated Lightfall chunks only).")
        public boolean enabled = true;
        @Config.Comment("Size of the square region (in chunks) that holds at most one library.")
        @Config.RangeInt(min = 12, max = 256) public int regionChunks = 40;
        @Config.Comment("Chance (percent) that a region has a library.")
        @Config.RangeInt(min = 0, max = 100) public int chancePercent = 60;
    }

    @Config.Comment("1.3.0 features (each can be switched off; worldgen switches affect newly generated chunks only).")
    public static V130 v130 = new V130();

    public static class V130 {
        // stage 1: the apothecary
        @Config.Comment("Brain Soaps (purple soap bar that washes out Insania, Mania, Nausea and Somnus).") public boolean brainSoaps = true;
        @Config.Comment("Dusk Poppies on Lightfall islands (shears give Poppy Tears).") public boolean duskPoppy = true;
        @Config.Comment("Poppy Tears: seconds of Somnus.") @Config.RangeInt(min = 5, max = 3600) public int somnusSeconds = 90;
        @Config.Comment("Galen's Theriac Crock (aging theriac).") public boolean theriacCrock = true;
        @Config.Comment("Ticks of one 'day' of crock aging (Theriac 3 days, Great 5, Imperial 10). 24000 = one Minecraft day.") @Config.RangeInt(min = 20, max = 240000) public int crockDayTicks = 24000;
        @Config.Comment("Tyrian Murex snails in the Lightfall sea: spawn weight (0 = none).") @Config.RequiresMcRestart @Config.RangeInt(min = 0, max = 100) public int murexWeight = 6;
        @Config.Comment("Doctor Ammonius ends 1 in N rants with his sign-off (0 = never).") @Config.RangeInt(min = 0, max = 1000) public int signOffOneIn = 8;
        @Config.Comment("Doctor Ammonius gives a short lecture (informative clip + its words in chat) 1 in N times he speaks (0 = never).") @Config.RangeInt(min = 0, max = 1000) public int lectureOneIn = 5;
        @Config.Comment("A carried totem whispers his sign-off 1 in N times (0 = never).") @Config.RangeInt(min = 0, max = 1000) public int signOffWhisperOneIn = 40;
        // stage 2: the academy
        @Config.Comment("Office Hours: Doctor Ammonius in the Library gives quests (sneak + right-click him with an empty hand).") public boolean officeHours = true;
        @Config.Comment("Quests needed to graduate (Diploma + Mortarboard).") @Config.RangeInt(min = 1, max = 50) public int questsToGraduate = 5;
        @Config.Comment("Peer Reviewers haunt the Library of Alexandria.") public boolean peerReviewers = true;
        // stage 3: wild things
        @Config.Comment("Bacchant packs around Bacchic wine fountains (structure chance per 1000 chunks).") @Config.RangeInt(min = 0, max = 1000) public int bacchicFountain = 6;
        @Config.Comment("Scythian Huntresses (mounted archers) spawn weight on Lightfall islands (0 = none).") @Config.RequiresMcRestart @Config.RangeInt(min = 0, max = 100) public int huntressWeight = 2;
        @Config.Comment("Chance (0-1) that a valid Huntress spawn succeeds.") @Config.RangeDouble(min = 0, max = 1) public double huntressSpawnChance = 0.05;
        @Config.Comment("Hekate's Crossroads Shrines (per 1000 chunks).") @Config.RangeInt(min = 0, max = 1000) public int crossroads = 6;
        // stage 4: mysteries & music
        @Config.Comment("Kykeon and the Mystery Halls (per 1000 chunks).") @Config.RangeInt(min = 0, max = 1000) public int mysteryHall = 5;
        @Config.Comment("Render the ΜΥΣΤΗΣ tattoo on initiated players (client side).") public boolean showMystesTattoo = true;
        @Config.Comment("On Air Studios (per 1000 chunks).") @Config.RangeInt(min = 0, max = 1000) public int onAirStudio = 4;
        @Config.Comment("Lyre-Radio cooldown between clips (ticks).") @Config.RangeInt(min = 0, max = 24000) public int radioCooldownTicks = 100;
        @Config.Comment("Devil's Flight band camps (per 1000 chunks).") @Config.RangeInt(min = 0, max = 1000) public int bandCamp = 4;
        @Config.Comment("Lucifer stands on the roof of new Morningstar Citadels (Interview with Lucifer).") public boolean luciferInterview = true;
        // stage 5: monsters & mazes
        @Config.Comment("The Drakaina coils in a lair beneath new Delphic vents.") public boolean drakaina = true;
        @Config.Comment("Chance (percent) that a new Sunken Shrine (the Sunken Chapel) holds the Hydra.") @Config.RangeInt(min = 0, max = 100) public int hydraPercent = 35;
        @Config.Comment("Labyrinths of the Lady: one per region of this many chunks (0 = none).") @Config.RangeInt(min = 0, max = 256) public int labyrinthRegionChunks = 48;
        @Config.Comment("Chance (percent) that a region has a Labyrinth.") @Config.RangeInt(min = 0, max = 100) public int labyrinthChancePercent = 50;
        @Config.Comment("Asklepieia (per 1000 chunks).") @Config.RangeInt(min = 0, max = 1000) public int asklepieion = 5;
        // stage 4.5: the garden of the gods
        @Config.Comment("Silphium: grows near the (purple) sea on sand, coarse dirt, star grass or farmland; goes extinct locally if over-harvested.") public boolean silphium = true;
        @Config.Comment("Silphium grows on 1 in N random ticks (slow).") @Config.RangeInt(min = 1, max = 100) public int silphiumGrowthOneIn = 6;
        @Config.Comment("Mature Silphium harvests within the window (in one 32x32 area) that make it go extinct there.") @Config.RangeInt(min = 1, max = 1000) public int silphiumExtinctHarvests = 8;
        @Config.Comment("Silphium over-harvesting window (Minecraft days).") @Config.RangeInt(min = 1, max = 100) public int silphiumWindowDays = 3;
        @Config.Comment("How long Silphium stays extinct in that area (Minecraft days).") @Config.RangeInt(min = 0, max = 1000) public int silphiumExtinctDays = 5;
        @Config.Comment("Moly (Hermes' herb): eating or brewing it grants the Moly Ward.") public boolean moly = true;
        @Config.Comment("Moly Ward from eating Moly (seconds).") @Config.RangeInt(min = 10, max = 3600) public int molyWardSeconds = 180;
        @Config.Comment("Eleusinian Barley (and ergot, for the Hierophant's Kykeon).") public boolean eleusinianBarley = true;
        @Config.Comment("Chance (percent) that ripe barley shows ergot.") @Config.RangeInt(min = 0, max = 100) public int ergotPercent = 8;
        @Config.Comment("Purple Ambrosia: grows only in the Lightfall or on Theriac-watered farmland.") public boolean ambrosia = true;
        @Config.Comment("Purple Ambrosia grows on 1 in N random ticks (slow).") @Config.RangeInt(min = 1, max = 100) public int ambrosiaGrowthOneIn = 8;
        @Config.Comment("1 in N servings of Ambrosia leave you glowing ('Ambrosial').") @Config.RangeInt(min = 1, max = 1000) public int ambrosialOneIn = 10;
    }

    @Config.Comment("1.3.2: Dionysus (mask, wooden xoanon follower) and the Crown of Thorns.")
    public static V132 v132 = new V132();

    public static class V132 {
        @Config.Comment("Dionysus: his statue set on a T of four logs (like an iron golem) comes to life and follows you.") public boolean dionysus = true;
        @Config.Comment("Hang the life-sized purple-splashed Dionysus icon over the entrance of newly generated Libraries (breaking it gives the Dionysus Statue).") public boolean libraryIcon = true;
        @Config.Comment("Hang a wooden Dionysus mask (decoration; may hold a Crown of Thorns) on a pergola column of newly generated Bacchic Fountains.") public boolean maskInFountains = true;
        @Config.Comment("Right-clicking your Dionysus with a water bottle turns it into Bacchic Wine.") public boolean wineMiracle = true;
        @Config.Comment("Dionysus' health.") @Config.RangeInt(min = 10, max = 400) public int dionysusHealth = 60;
        @Config.Comment("Crown of Thorns: worn (helmet slot or as a cosmetic) in the Lightfall, its creatures leave you be unless you strike first. The Dipsas does not.") public boolean crownOfThorns = true;
        @Config.Comment("1 in N: a fountain's (worldgen) Dionysus mask drops a Crown of Thorns when broken. 0 = never.") @Config.RangeInt(min = 0, max = 1000) public int crownFromMaskOneIn = 6;
        @Config.Comment("Crown of Thorns can also be crafted (vines, Dusk Poppies, Bacchic Wine).") @Config.RequiresMcRestart public boolean crownRecipe = true;
        @Config.Comment("Ticks of PURPLE a Dipsas bite gives a Crown wearer in the Lightfall.") @Config.RangeInt(min = 20, max = 24000) public int dipsasPurpleTicks = 600;
    }

    @Config.Comment("Developer self-test on server start. Leave false.")
    public static boolean testHarness = false;
}
