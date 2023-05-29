package net.quantum625.networks.listener;

import net.quantum625.config.lang.Language;
import net.quantum625.config.util.exceptions.InvalidNodeException;
import net.quantum625.networks.Network;
import net.quantum625.networks.NetworkManager;
import net.quantum625.networks.component.InputContainer;
import net.quantum625.networks.component.MiscContainer;
import net.quantum625.networks.component.SortingContainer;
import net.quantum625.networks.component.BaseComponent;
import net.quantum625.networks.data.Config;
import net.quantum625.networks.data.CraftingManager;
import net.quantum625.networks.utils.Location;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExplosionListener implements Listener {

    private Config config;
    private Language lang;
    private NetworkManager net;
    private CraftingManager craftingManager;

    public ExplosionListener(Config config, Language lang, NetworkManager net, CraftingManager craftingManager) {
        this.config = config;
        this.lang = lang;
        this.net = net;
        this.craftingManager = craftingManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(EntityExplodeEvent event) throws InvalidNodeException, SerializationException {

        ArrayList<Block> removeLater = new ArrayList<>();

        for (Block block : event.blockList()) {

            if (net.getComponentByLocation(new Location(block)) != null) {
                removeLater.add(block);
            }
        }

        for (Block block : removeLater) {
            if (!config.blastProofComponents()) {
                BaseComponent component = net.getComponentByLocation(new Location(block));
                if (component instanceof InputContainer) {
                    Bukkit.getServer().getWorld(component.getPos().getDim()).dropItem(component.getPos().getBukkitLocation(), craftingManager.getInputContainer(block.getType()));
                }
                if (component instanceof SortingContainer) {

                    String items = "";
                    List<String> itemslist = new ArrayList<String>();
                    itemslist.addAll(0, lang.getList("sorting"));
                    for (String item : ((SortingContainer) component).getItems()) {
                        items += item + ",";
                        itemslist.add("§r§f" + item);
                    }

                    ItemStack sortingContainer = craftingManager.getSortingContainer(block.getType());
                    ItemMeta meta = sortingContainer.getItemMeta();
                    PersistentDataContainer data = meta.getPersistentDataContainer();
                    data.set(new NamespacedKey("networks", "filter_items"), PersistentDataType.STRING, items);
                    sortingContainer.setItemMeta(meta);
                    Bukkit.getServer().getWorld(component.getPos().getDim()).dropItem(component.getPos().getBukkitLocation(), sortingContainer);
                }
                if (component instanceof MiscContainer) {
                    Bukkit.getServer().getWorld(component.getPos().getDim()).dropItem(component.getPos().getBukkitLocation(),craftingManager.getMiscContainer(block.getType()));
                }

                for (ItemStack stack : component.getInventory()) {
                    if (stack != null) {
                        Bukkit.getServer().getWorld(component.getPos().getDim()).dropItem(component.getPos().getBukkitLocation(), stack);
                    }
                }

                event.blockList().remove(block);
                block.setType(Material.AIR);
                Network network = net.getNetworkWithComponent(new Location(block));
                network.removeComponent(new Location(block));
                ArrayList<UUID> users = (ArrayList<UUID>) network.getUsers().clone();
                users.add(network.getOwner());

                for (UUID uid : users) {
                    if (Bukkit.getPlayer(uid).isOnline()) {
                        lang.message(Bukkit.getPlayer(uid), "component.exploded", network.getID(), new Location(block).toString());
                    }
                }
            }

            event.blockList().remove(block);
        }
    }
}
