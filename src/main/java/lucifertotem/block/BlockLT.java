package lucifertotem.block;

import lucifertotem.LuciferTotem;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

public class BlockLT extends Block {
    public BlockLT(Material m, SoundType s, float hardness, float resistance, String name) {
        super(m);
        setSoundType(s);
        setHardness(hardness);
        setResistance(resistance);
        setRegistryName(LuciferTotem.MODID, name);
        setTranslationKey(LuciferTotem.MODID + "." + name);
        setCreativeTab(LuciferTotem.TAB);
        if (m == Material.ROCK) setHarvestLevel("pickaxe", 0);
    }
}
