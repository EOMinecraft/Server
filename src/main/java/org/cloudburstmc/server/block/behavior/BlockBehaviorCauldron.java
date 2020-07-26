package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Cauldron;
import org.cloudburstmc.server.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.server.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemBucket;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.CAULDRON;

public class BlockBehaviorCauldron extends BlockBehaviorSolid {

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    public boolean isFull() {
        return this.getMeta() == 0x06;
    }

    public boolean isEmpty() {
        return this.getMeta() == 0x00;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        BlockEntity be = this.level.getBlockEntity(this.getPosition());

        if (!(be instanceof Cauldron)) {
            return false;
        }

        Cauldron cauldron = (Cauldron) be;

        Identifier itemType = item.getId();

        if (itemType == ItemIds.BUCKET) {
            if (item.getMeta() == 0) {//empty bucket
                if (!isFull() || cauldron.hasCustomColor() || cauldron.getPotionId() != 0) {
                    return true;
                }

                ItemBucket bucket = (ItemBucket) item.clone();
                bucket.setCount(1);
                bucket.setMeta(8);//water bucket

                PlayerBucketFillEvent ev = new PlayerBucketFillEvent(player, this, null, item, bucket);
                this.level.getServer().getPluginManager().callEvent(ev);
                if (!ev.isCancelled()) {
                    replaceBucket(item, player, ev.getItem());
                    this.setMeta(0);//empty
                    this.level.setBlock(this.getPosition(), this, true);
                    cauldron.setCustomColor(null);
                    this.getLevel().addSound(this.getPosition().toFloat().add(0.5, 1, 0.5), Sound.CAULDRON_TAKEWATER);
                }
            } else if (item.getMeta() == 8) {//water bucket

                if (isFull() && !cauldron.hasCustomColor() && cauldron.getPotionId() == 0) {
                    return true;
                }

                ItemBucket bucket = (ItemBucket) item.clone();
                bucket.setCount(1);
                bucket.setMeta(0);//empty bucket

                PlayerBucketEmptyEvent ev = new PlayerBucketEmptyEvent(player, this, null, item, bucket);
                this.level.getServer().getPluginManager().callEvent(ev);
                if (!ev.isCancelled()) {
                    replaceBucket(item, player, ev.getItem());

                    if (cauldron.getPotionId() != 0) {//if has potion
                        this.setMeta(0);//empty
                        cauldron.setPotionId(0xffff);//reset potion
                        cauldron.setSplash(false);
                        cauldron.setCustomColor(null);
                        this.level.setBlock(this.getPosition(), this, true);
                        this.level.addSound(this.getPosition(), Sound.CAULDRON_EXPLODE);
                    } else {
                        this.setMeta(6);//fill
                        cauldron.setCustomColor(null);
                        this.level.setBlock(this.getPosition(), this, true);
                        this.getLevel().addLevelSoundEvent(this.getPosition().toFloat().add(0.5, 1, 0.5), SoundEvent.BUCKET_FILL_WATER);
                    }
                    //this.update();
                }
            }
        } else if (itemType == ItemIds.DYE) {
            // todo
        } else if (itemType == ItemIds.LEATHER_HELMET || itemType == ItemIds.LEATHER_CHESTPLATE ||
                itemType == ItemIds.LEATHER_LEGGINGS || itemType == ItemIds.LEATHER_BOOTS) {
            // todo
        } else if (itemType == ItemIds.POTION) {
            if (isFull()) {
                return true;
            }
            this.setMeta(this.getMeta() + 1);
            if (this.getMeta() > 0x06)
                this.setMeta(0x06);

            if (item.getCount() == 1) {
                player.getInventory().clear(player.getInventory().getHeldItemIndex());
            } else if (item.getCount() > 1) {
                item.setCount(item.getCount() - 1);
                player.getInventory().setItemInHand(item);

                Item bottle = Item.get(ItemIds.GLASS_BOTTLE);
                if (player.getInventory().canAddItem(bottle)) {
                    player.getInventory().addItem(bottle);
                } else {
                    player.getLevel().dropItem(player.getPosition().toFloat().add(0, 1.3, 0), bottle, player.getDirectionVector().mul(0.4));
                }
            }

            this.level.addSound(this.getPosition(), Sound.CAULDRON_FILLPOTION);
        } else if (itemType == ItemIds.GLASS_BOTTLE) {
            if (isEmpty()) {
                return true;
            }

            this.setMeta(this.getMeta() - 1);
            if (this.getMeta() < 0x00)
                this.setMeta(0x00);

            if (item.getCount() == 1) {
                player.getInventory().setItemInHand(Item.get(ItemIds.POTION));
            } else if (item.getCount() > 1) {
                item.setCount(item.getCount() - 1);
                player.getInventory().setItemInHand(item);

                Item potion = Item.get(ItemIds.POTION);
                if (player.getInventory().canAddItem(potion)) {
                    player.getInventory().addItem(potion);
                } else {
                    player.getLevel().dropItem(player.getPosition().toFloat().add(0, 1.3, 0), potion, player.getDirectionVector().mul(0.4));
                }
            }

            this.level.addSound(this.getPosition(), Sound.CAULDRON_TAKEPOTION);
        } else {
            return true;
        }

        this.level.updateComparatorOutputLevel(this.getPosition());
        return true;
    }

    protected void replaceBucket(Item oldBucket, Player player, Item newBucket) {
        if (player.isSurvival() || player.isAdventure()) {
            if (oldBucket.getCount() == 1) {
                player.getInventory().setItemInHand(newBucket);
            } else {
                oldBucket.setCount(oldBucket.getCount() - 1);
                if (player.getInventory().canAddItem(newBucket)) {
                    player.getInventory().addItem(newBucket);
                } else {
                    player.getLevel().dropItem(player.getPosition().add(0, 1.3, 0), newBucket, player.getDirectionVector().mul(0.4));
                }
            }
        }
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Cauldron cauldron = BlockEntityRegistry.get().newEntity(CAULDRON, this.getChunk(), this.getPosition());
        cauldron.loadAdditionalData(item.getTag());
        cauldron.setPotionId(0xffff);
        this.getLevel().setBlock(blockState.getPosition(), this, true, true);
        return true;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{Item.get(ItemIds.CAULDRON)};
        }

        return new Item[0];
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.CAULDRON);
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    public int getComparatorInputOverride(Block block) {
        return this.getMeta();
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
