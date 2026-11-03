package nl.adminpanel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AdminMenu extends ChestMenu {

    private static final Random RANDOM = new Random();

    // Volgorde bepaalt de plek in het menu (slot = ordinal + 9)
    private enum Troll {
        MINI(Items.RABBIT_FOOT, "Mini", "Maakt de speler superklein"),
        GROOT(Items.IRON_BLOCK, "Groot", "Maakt de speler gigantisch"),
        FLY(Items.FEATHER, "Fly", "Laat de speler 5 seconden zweven"),
        RAGDOLL(Items.BONE, "Ragdoll", "Gooit de speler rond als een lappenpop"),
        BLACKOUT(Items.BLACK_CONCRETE, "Blackout", "Scherm wordt zwart"),
        LAUNCH(Items.FIREWORK_ROCKET, "Launch", "Schiet de speler de lucht in"),
        FREEZE(Items.PACKED_ICE, "Freeze", "Speler kan niet bewegen"),
        SPIN(Items.COMPASS, "Spin", "Laat de camera ronddraaien"),
        SCARE(Items.CREEPER_HEAD, "Scare", "Speelt enge geluiden af"),
        DRUNK(Items.FERMENTED_SPIDER_EYE, "Drunk", "Misselijkheid-effect");

        final Item icon;
        final String label;
        final String lore;

        Troll(Item icon, String label, String lore) {
            this.icon = icon;
            this.label = label;
            this.lore = lore;
        }

        int slot() {
            return ordinal() + 9;
        }
    }

    private static final int SLOT_RESET = 22;
    private static final int SLOT_BACK = 26;

    private final SimpleContainer container;
    private final boolean playerList;
    private final UUID target;
    private final Map<Integer, UUID> slotTargets;

    private AdminMenu(MenuType<?> type, int rows, int id, Inventory inv, SimpleContainer container,
                      boolean playerList, UUID target, Map<Integer, UUID> slotTargets) {
        super(type, id, inv, container, rows);
        this.container = container;
        this.playerList = playerList;
        this.target = target;
        this.slotTargets = slotTargets;
    }

    // ---------------- Menu's openen ----------------

    public static void openPlayerList(ServerPlayer admin) {
        SimpleContainer c = new SimpleContainer(54);
        Map<Integer, UUID> map = new HashMap<>();

        int i = 0;
        for (ServerPlayer sp : admin.getServer().getPlayerList().getPlayers()) {
            if (i >= 54) break;
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(sp.getGameProfile()));
            head.set(DataComponents.CUSTOM_NAME, Component.literal(sp.getGameProfile().getName())
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withItalic(false)));
            c.setItem(i, head);
            map.put(i, sp.getUUID());
            i++;
        }

        admin.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new AdminMenu(MenuType.GENERIC_9x6, 6, id, inv, c, true, null, map),
                Component.literal("Admin Panel - Kies speler")));
    }

    public static void openTrollMenu(ServerPlayer admin, ServerPlayer target) {
        SimpleContainer c = new SimpleContainer(27);
        for (Troll t : Troll.values()) {
            c.setItem(t.slot(), button(t.icon, t.label, t.lore, ChatFormatting.GOLD));
        }
        c.setItem(SLOT_RESET, button(Items.MILK_BUCKET, "Reset", "Haalt alle effecten weg", ChatFormatting.GREEN));
        c.setItem(SLOT_BACK, button(Items.ARROW, "Terug", "Andere speler kiezen", ChatFormatting.GRAY));

        UUID targetId = target.getUUID();
        admin.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new AdminMenu(MenuType.GENERIC_9x3, 3, id, inv, c, false, targetId, null),
                Component.literal("Troll: " + target.getGameProfile().getName())));
    }

    private static ItemStack button(Item item, String name, String lore, ChatFormatting color) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME,
                Component.literal(name).withStyle(Style.EMPTY.withColor(color).withItalic(false)));
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(lore).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))));
        return s;
    }

    // ---------------- Kliks afhandelen (niets mag uit het menu gepakt worden) ----------------

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        try {
            if (!(player instanceof ServerPlayer admin) || !admin.hasPermissions(2)) return;
            if (slotId < 0 || slotId >= container.getContainerSize()) return;

            if (playerList) {
                UUID id = slotTargets.get(slotId);
                if (id == null) return;
                ServerPlayer t = admin.getServer().getPlayerList().getPlayer(id);
                if (t == null) {
                    admin.sendSystemMessage(Component.literal("Die speler is offline.").withStyle(ChatFormatting.RED));
                    return;
                }
                openTrollMenu(admin, t);
                return;
            }

            if (slotId == SLOT_BACK) {
                openPlayerList(admin);
                return;
            }

            ServerPlayer t = admin.getServer().getPlayerList().getPlayer(target);
            if (t == null) {
                admin.sendSystemMessage(Component.literal("Die speler is offline.").withStyle(ChatFormatting.RED));
                openPlayerList(admin);
                return;
            }

            if (slotId == SLOT_RESET) {
                reset(t);
                admin.sendSystemMessage(Component.literal("Effecten van " + t.getGameProfile().getName()
                        + " gereset.").withStyle(ChatFormatting.GREEN));
                return;
            }

            for (Troll troll : Troll.values()) {
                if (troll.slot() == slotId) {
                    apply(troll, t);
                    admin.sendSystemMessage(Component.literal(troll.label + " toegepast op "
                            + t.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
                    return;
                }
            }
        } finally {
            sendAllDataToRemote(); // client terug synchroniseren
        }
    }

    // ---------------- Troll acties ----------------

    private static void apply(Troll troll, ServerPlayer p) {
        switch (troll) {
            case MINI -> toggleScale(p, 0.3);
            case GROOT -> toggleScale(p, 3.0);
            case FLY -> p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 4));
            case RAGDOLL -> Scheduler.repeat(8, 5, () -> {
                if (p.isRemoved()) return;
                Vec3 v = new Vec3(RANDOM.nextDouble() - 0.5, 0.5 + RANDOM.nextDouble() * 0.5,
                        RANDOM.nextDouble() - 0.5).scale(1.2);
                p.setDeltaMovement(v);
                p.hurtMarked = true;
            });
            case BLACKOUT -> {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
            }
            case LAUNCH -> {
                p.setDeltaMovement(0, 4, 0);
                p.hurtMarked = true;
            }
            case FREEZE -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 255));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 128));
            }
            case SPIN -> Scheduler.repeat(60, 1, () -> {
                if (p.isRemoved()) return;
                p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot() + 30f, p.getXRot());
            });
            case SCARE -> {
                p.playNotifySound(SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1f, 1f);
                p.playNotifySound(SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1f, 0.6f);
            }
            case DRUNK -> p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 1));
        }
    }

    private static void toggleScale(ServerPlayer p, double scale) {
        AttributeInstance attr = p.getAttribute(Attributes.SCALE);
        if (attr == null) return;
        attr.setBaseValue(attr.getBaseValue() == scale ? 1.0 : scale);
    }

    private static void reset(ServerPlayer p) {
        p.removeAllEffects();
        AttributeInstance attr = p.getAttribute(Attributes.SCALE);
        if (attr != null) attr.setBaseValue(1.0);
    }
}
