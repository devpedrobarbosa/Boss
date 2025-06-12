package org.mineacademy.boss.menu.util;

import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.boss.skill.BossSkill;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.conversation.SimpleStringPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPaged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompPotionEffectType;

import lombok.RequiredArgsConstructor;

/**
 * A utility menu for editing potions in skills.
 */
public final class SkillPotionMenu extends MenuPaged<PotionEffect> {

	private final BossSkill skill;
	private final List<PotionEffect> potions;

	@Position(start = StartPosition.BOTTOM_RIGHT)
	private final Button createButton;

	public SkillPotionMenu(Menu parent, BossSkill skill, List<PotionEffect> potions) {
		super(parent, potions);

		this.skill = skill;
		this.potions = potions;

		this.setTitle("Add Skill Potions");

		this.createButton = new ButtonMenu(new NewPotionSelectMenu(),
				CompMaterial.EMERALD,
				"&aCreate New",
				"",
				"Click to create",
				"a new potion.");
	}

	@Override
	protected ItemStack convertToItemStack(PotionEffect item) {
		return ItemCreator.fromPotion(item,
				ChatUtil.capitalizeFully(item.getType().getName()) + " Effect",
				"",
				"Level: " + (item.getAmplifier() + 1),
				"Duration: " + (item.getDuration() >= 20 ? TimeUtil.formatTimeGeneric(item.getDuration() / 20) : item.getDuration() + " ticks"),
				"",
				"&4&l< &7Left click to remove")
				.make();
	}

	@Override
	protected void onPageClick(Player player, PotionEffect item, ClickType click) {
		if (click == ClickType.LEFT) {
			this.potions.remove(item);
			this.skill.save();

			this.restartMenu("&4Potion has been removed!");
		}
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"This skill can add multiple",
				"potions to the player that",
				"the Boss targets. Edit them",
				"here."
		};
	}

	@Override
	public Menu newInstance() {
		return new SkillPotionMenu(this.getParent(), this.skill, this.potions);
	}

	private class NewPotionSelectMenu extends MenuPaged<PotionEffectType> {

		NewPotionSelectMenu() {
			super(SkillPotionMenu.this, CompPotionEffectType.getPotions(), true);

			this.setTitle("Select Potion Type");
		}

		@Override
		protected ItemStack convertToItemStack(PotionEffectType item) {
			return ItemCreator.fromPotion(item,
					ChatUtil.capitalizeFully(item.getName()) + " Effect",
					"",
					"Click to add the",
					"effect to skill.")
					.make();
		}

		@Override
		protected void onPageClick(Player player, PotionEffectType item, ClickType click) {
			SimplePrompt.show(player, new PotionLevelPrompt(item));
		}

		@Override
		protected String[] getInfo() {
			return new String[] {
					"Click to select which",
					"potion effect to give",
					"each targeted player."
			};
		}

		@Override
		public Menu newInstance() {
			return new NewPotionSelectMenu();
		}
	}

	@RequiredArgsConstructor
	private class PotionLevelPrompt extends SimpleDecimalPrompt {

		private final PotionEffectType type;

		@Override
		protected String getPrompt(ConversationContext ctx) {
			return "Enter the " + ChatUtil.capitalizeFully(this.type.getName()) + " potion level such as 1 or 10.";
		}

		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			if (Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), 1, 20)) {
				context.setSessionData("Level", Integer.parseInt(input));

				return true;
			}

			return false;
		}

		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "Invalid potion level '" + invalidInput + "'! Please enter a whole number from 1-20.";
		}

		@Override
		protected Prompt acceptValidatedInput(ConversationContext context, double input) {
			return new PotionDurationPrompt();
		}

		private class PotionDurationPrompt extends SimpleStringPrompt {

			PotionDurationPrompt() {
				super(false);
			}

			@Override
			protected String getPrompt(ConversationContext ctx) {
				return "Enter the " + ChatUtil.capitalizeFully(PotionLevelPrompt.this.type.getName()) + " potion duration such as '10 seconds' or '2 minutes'.";
			}

			@Override
			protected boolean isInputValid(ConversationContext context, String input) {
				try {
					final SimpleTime duration = SimpleTime.fromString(input);

					context.setSessionData("Duration", duration);
					return true;

				} catch (final IllegalArgumentException ex) {
					return false;
				}
			}

			@Override
			protected String getFailedValidationText(ConversationContext context, String invalidInput) {
				return "Invalid potion level '" + invalidInput + "'! Please enter a whole number from 1-20.";
			}

			@Override
			protected void onValidatedInput(ConversationContext context, String input) {
				final int level = (int) context.getSessionData("Level");
				final SimpleTime duration = (SimpleTime) context.getSessionData("Duration");

				SkillPotionMenu.this.potions.add(new PotionEffect(PotionLevelPrompt.this.type, duration.getTimeTicks(), level - 1));
				SkillPotionMenu.this.skill.save();

				Platform.runTask(2, () -> {
					SkillPotionMenu.this.newInstance().displayTo(this.getPlayer(context));
				});
			}
		}
	}
}