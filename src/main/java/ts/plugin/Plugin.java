package ts.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Objects;

public final class Plugin extends JavaPlugin implements Listener {

    Server thisServer;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getLogger().info("Swords loaded");
        getServer().getPluginManager().registerEvents(this, this);
        thisServer = getServer();
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Swords unloaded");
    }

    @EventHandler
    public void onWalk(PlayerMoveEvent e){
        try{
        Player p = e.getPlayer();
        ItemStack i = p.getInventory().getBoots();

        if (i == null){return;}

        if (i.containsEnchantment(Enchantment.FIRE_ASPECT)) {
            if (p.getHealth() < 10) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3, 5));
                if (p.getLocation().getBlock().getType().equals(Material.AIR)){
                    p.getLocation().getBlock().setType(Material.FIRE);
                }

                Damageable meta = (Damageable) i.getItemMeta();
                meta.setDamage(meta.getDamage() + 1 );
                i.setItemMeta((ItemMeta) meta);
                p.updateInventory();
            }
        }
        }
        catch (Exception err){
            this.getLogger().info(err.toString());
        }
    }

    public Boolean ChannelEXP(Player p, int levels, boolean slow){
        if (p.hasPotionEffect(PotionEffectType.SLOW)){return false;}
        if (p.getLevel() < levels){
            p.playSound(p.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            return false;
        }
        else{
            if (slow) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 10 * levels, 5));
            }
            for (int i = 0; levels > i; i++){
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    p.setLevel(p.getLevel() - 1);
                    p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1, 1 );
                }, i * 10L);
            }
        return true;
        }
    }

    @EventHandler
    public void smite(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack is = p.getInventory().getItemInMainHand();
        try {
            if (is.getType().equals(Material.DIAMOND_SWORD) && is.containsEnchantment(Enchantment.RIPTIDE) && ChannelEXP(p, 5, true)) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    for(int i = 0; i < 20; i++) {
                        Block target = p.getTargetBlockExact(300);
                        Location tl = target.getLocation();
                        tl.setY(tl.getY() + 1);
                        tl.getWorld().strikeLightning(tl);
                    }
                }, 5 * 10L);
            }
        }
        catch(Exception err){
            thisServer.broadcastMessage(err.toString());
        }
    }


    @EventHandler
    public void enderCrossbow(ProjectileHitEvent e){

        Projectile shotArrow = e.getEntity();
        if (!(shotArrow instanceof Arrow)){
            return;
        }
        ProjectileSource shooter = shotArrow.getShooter();
        if (!(shooter instanceof Player)){return;}
        Player shooterPlayer = (Player) shooter;

        ItemStack heldItem = shooterPlayer.getInventory().getItemInMainHand();
        if (heldItem == null){return;}
        if (heldItem.containsEnchantment(Enchantment.CHANNELING) && heldItem.getType().equals(Material.CROSSBOW)){
            if (ChannelEXP(shooterPlayer, 1, true)){
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    shooterPlayer.teleport(shotArrow.getLocation());
                    shooterPlayer.playSound(shooterPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    shooterPlayer.playEffect(EntityEffect.TELEPORT_ENDER);
                }, 10L);
            }
        }

    }

    @EventHandler
    public void ExplosiveTrident(ProjectileHitEvent e){

        Projectile p = e.getEntity();
        if (!(p instanceof Trident)){
            return;
        }
        Trident shotTrident = (Trident) p;
        ProjectileSource shooter = shotTrident.getShooter();
        if (!(shooter instanceof Player)){return;}
        ItemStack tridentItem = shotTrident.getItem();
        Player shooterPlayer = (Player) shooter;
        if (tridentItem.containsEnchantment(Enchantment.ARROW_DAMAGE)){
            int level = tridentItem.getEnchantmentLevel(Enchantment.ARROW_DAMAGE);
            if (shooterPlayer.getLevel() >= 3 + level){
                Location target = shotTrident.getLocation();
                target.getWorld().createExplosion(target, 5 + level, true, false);
                shotTrident.remove();
                ChannelEXP(shooterPlayer ,3 + level, false);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    shooterPlayer.getInventory().addItem(tridentItem);
                }, (3 + level) * 10L);
            }
        }

    }

    @EventHandler
    public void inspireShield(PlayerInteractEvent e){
        Player p = e.getPlayer();
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR)){return;}
        ItemStack i = p.getInventory().getItemInOffHand();
        if (i == null){return;}
        if (i.getType().equals(Material.SHIELD) && i.containsEnchantment(Enchantment.LOYALTY)){
            if(ChannelEXP(p, 5, true)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 1));
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 8, 1);
                List<Entity> nearbyEntities = p.getNearbyEntities(12, 8 ,12);
                for (Entity entity: nearbyEntities){
                    if (entity instanceof Player){
                        Player targetPlayer = (Player) entity;
                        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 2));
                        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 2));
                        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                    }
                }
            }
        }
    }
}
