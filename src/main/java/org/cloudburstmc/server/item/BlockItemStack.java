package org.cloudburstmc.server.item;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.ToString;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ToString(callSuper = true)
public class BlockItemStack extends CloudItemStack {

    protected final BlockState blockState;

    public BlockItemStack(BlockState state, int amount) {
        super(state.getType().getId(), state.getType(), amount);
        this.blockState = state;
    }

    public BlockItemStack(Identifier id, BlockState state, int amount, String itemName, List<String> itemLore, Map<EnchantmentType, EnchantmentInstance> enchantments, Collection<Identifier> canDestroy, Collection<Identifier> canPlaceOn, Map<Class<?>, Object> data, NbtMap nbt, NbtMap dataTag, ItemData networkData, int stackId) {
        super(id, state.getType(), amount, itemName, itemLore, enchantments, canDestroy, canPlaceOn, data, nbt, dataTag, networkData, stackId);
        this.blockState = state;
    }

    @Override
    public BlockState getBlockState() {
        return this.blockState;
    }

    @Override
    public boolean isBlock() {
        return true;
    }

    //@Override
    public boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData) {
        if (this == other) return true;
        if (!(other instanceof BlockItemStack)) return false;
        BlockItemStack that = (BlockItemStack) other;

        return this.blockState == that.blockState && (!checkAmount || this.amount == that.amount) &&
                (!checkData || Objects.equals(this.getDataTag(), that.getDataTag()));
    }
}
