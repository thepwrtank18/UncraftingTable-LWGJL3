package org.jglrxavpok.mods.decraft.client.gui.inventory;

import org.jglrxavpok.mods.decraft.ModUncrafting;
import org.jglrxavpok.mods.decraft.common.network.message.RecipeNavigationMessage;
import org.jglrxavpok.mods.decraft.inventory.ContainerUncraftingTable;
import org.jglrxavpok.mods.decraft.item.uncrafting.UncraftingResult.ResultType;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;


public class GuiUncraftingTable extends GuiContainer
{
	private static final ResourceLocation UNCRAFTING_TABLE_GUI_TEXTURES = new ResourceLocation(ModUncrafting.MODID, "textures/gui/container/uncrafting_table.png");

	public ContainerUncraftingTable container;
	private World worldObj;
	private EntityPlayer player;
	private GuiButton previousRecipeButton;
	private GuiButton nextRecipeButton;

	public GuiUncraftingTable(InventoryPlayer playerInventory, World world)
	{
		super(new ContainerUncraftingTable(playerInventory, world));
		container = (ContainerUncraftingTable)inventorySlots;
		this.worldObj = world;
		this.player = playerInventory.player;
	}


	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	@Override
	public void initGui()
	{
		super.initGui();

		int guiX = (this.width - this.xSize) / 2;
		int guiY = (this.height - this.ySize) / 2;

		this.buttonList.add(this.nextRecipeButton = new GuiButton(1, guiX + 162, guiY + 20, ButtonFacing.RIGHT));
		this.buttonList.add(this.previousRecipeButton = new GuiButton(2, guiX + 95, guiY + 20, ButtonFacing.LEFT));

		this.previousRecipeButton.visible = false;
		this.nextRecipeButton.visible = false;
	}


	/**
	 * Draws the screen and all the components in it.
	 */
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		this.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.renderHoveredToolTip(mouseX, mouseY);
	}

	/**
	 * Called from the main game loop to update the screen.
	 */
	@Override
	public void updateScreen()
	{
		super.updateScreen();

		boolean haveMultipleRecipes = (container.uncraftingResult.getRecipeCount() > 1);
		boolean hasNextRecipe = (container.uncraftingResult.selectedCraftingGrid < (container.uncraftingResult.getRecipeCount() - 1));
		boolean hasPreviousRecipe = (container.uncraftingResult.selectedCraftingGrid > 0);
		boolean canChangeRecipe = (container.uncraftingResult.resultType != ResultType.UNCRAFTED);

		this.nextRecipeButton.visible = (haveMultipleRecipes && canChangeRecipe && hasNextRecipe);
		this.nextRecipeButton.enabled = this.nextRecipeButton.visible;

		this.previousRecipeButton.visible = (haveMultipleRecipes && canChangeRecipe && hasPreviousRecipe);
		this.nextRecipeButton.enabled = this.nextRecipeButton.visible;
	}


	@Override
	protected void actionPerformed(net.minecraft.client.gui.GuiButton button)
	{
		if (button == this.previousRecipeButton || button == this.nextRecipeButton)
		{
			if (button == this.previousRecipeButton)
			{
				if (container.uncraftingResult.selectedCraftingGrid == 0) return;
				container.uncraftingResult.selectedCraftingGrid--;
			}
			if (button == this.nextRecipeButton)
			{
				if (container.uncraftingResult.selectedCraftingGrid == (container.uncraftingResult.getRecipeCount() - 1)) return;
				container.uncraftingResult.selectedCraftingGrid++;
			}
			container.switchRecipe();

			int recipeIndex = container.uncraftingResult.selectedCraftingGrid;
			ModUncrafting.NETWORK.sendToServer(new RecipeNavigationMessage(recipeIndex));
		}
	}


	private void drawUncraftingStatusMessage()
	{
		// get a message to display based on the status of the container
		String statusMessage = ""; int quantityNeeded = 0;
		switch (container.uncraftingResult.resultType)
		{
			// if the uncrafting status is "ready", display the xp cost for the operation
			case VALID:
				if (container.uncraftingResult.experienceCost > 0)
				{
					statusMessage = I18n.format("container.uncrafting.cost", container.uncraftingResult.experienceCost);
				}
				break;

			// if the item cannot be uncrafted, display a message to that effect
			case NOT_UNCRAFTABLE:
				statusMessage = I18n.format("uncrafting.result.impossible");
				break;

			// if there are not enough items in the item stack, display a message to that effect
			case NOT_ENOUGH_ITEMS:
				quantityNeeded = (container.uncraftingResult.getMinStackSize() - container.uncraftIn.getStackInSlot(0).getCount());
				statusMessage = I18n.format("uncrafting.result.needMoreStacks", quantityNeeded);
				break;

			// if the player does not have enough xp, display the xp cost for the operation
			case NOT_ENOUGH_XP:
				statusMessage = I18n.format("container.uncrafting.cost", container.uncraftingResult.experienceCost);
				break;

			// if the crafting recipe requires container items to be present, display a message to that effect
			case NEED_CONTAINER_ITEMS:
				quantityNeeded = container.uncraftOut.missingContainerItemCount();
				statusMessage = I18n.format("uncrafting.result.needMoreStacks", quantityNeeded);
				break;


			default: break;
		}

		// if there is a message to display, render it
		if (!statusMessage.equals(""))
		{
			int textX = 8;
			int textY = ySize - 96 + 2 - fontRenderer.FONT_HEIGHT - 4; // 60

			// *** copied from GuiRepair ***
			// determine the text and shadow colours based on the uncrafting status
			int textColor = (container.uncraftingResult.isError() ? 0xFF6060 : 0x80FF20);
			int shadowColor = -16777216 | (textColor & 16579836) >> 2 | textColor & -16777216;

			// render the string 4 times at different positions in different colours to achieve the desired effect
			this.fontRenderer.drawString(statusMessage, textX, textY + 1, shadowColor);
			this.fontRenderer.drawString(statusMessage, textX + 1, textY, shadowColor);
			this.fontRenderer.drawString(statusMessage, textX + 1, textY + 1, shadowColor);
			this.fontRenderer.drawString(statusMessage, textX, textY, textColor);
			// *** copied from GuiRepair ***
		}
	}


	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
//		GlStateManager.disableLighting();

		// render the block name at the top of the gui
		String title = I18n.format("container.uncrafting");
		fontRenderer.drawString(title, xSize / 2 - fontRenderer.getStringWidth(title) / 2, 6, 0x404040);

		// write "inventory" above the player inventory
		fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 0x404040); // y = 72

		// draw a status message in red or green if appropriate for the status of the uncrafting operation
		drawUncraftingStatusMessage();


//		GlStateManager.enableLighting();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
	{
		GlStateManager.pushMatrix();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		// bind the background gui texture
		this.mc.getTextureManager().bindTexture(UNCRAFTING_TABLE_GUI_TEXTURES);

		int guiX = (this.width - this.xSize) / 2;
		int guiY = (this.height - this.ySize) / 2;

		// render the gui background
		this.drawTexturedModalRect(guiX, guiY, 0, 0, this.xSize, this.ySize);

		// if the uncrafting status of the container is "error", render the arrow with the cross over it
		if (container.uncraftingResult.isError())
		{
			this.drawTexturedModalRect(guiX + 71, guiY + 33, 176, 0, 28, 21);
		}


		RenderHelper.enableGUIStandardItemLighting();

		// render a book over the left slot
		int slotX = 20; int slotY = 35;
		itemRender.renderItemIntoGUI(new ItemStack(Items.BOOK), guiX + slotX, guiY + slotY);

		// draw a gray rectangle over the item
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		this.drawRect(guiX + slotX, guiY + slotY, guiX + slotX + 16, guiY + slotY + 16, 0x9f8b8b8b);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_DEPTH_TEST);

		// if the uncrafting result's crafting grids collection isn't empty
		if (container.uncraftingResult.renderBackgroundItems())
		{
			// get the currently selected crafting grid
			NonNullList<ItemStack> craftingGrid = container.uncraftingResult.getCraftingGrid();

			// loop through the slots in the temp inventory
			for ( int i = 0 ; i < craftingGrid.size() ; i++ )
			{
				ItemStack itemStack = craftingGrid.get(i);
				// if the itemstack isn't empty
				if (itemStack != ItemStack.EMPTY)
				{
					// find the screen position of the corresponding slot from the output inventory
					Slot renderSlot = container.getSlotFromInventory(container.uncraftOut, i);
					slotX = renderSlot.xPos;
					slotY = renderSlot.yPos;


					// if the inventory slot is empty, render the item from the crafting recipe as the slot background
					if (!renderSlot.getHasStack())
					{

						// render the item in the position of the slot
						itemRender.renderItemAndEffectIntoGUI(itemStack, guiX + slotX, guiY + slotY);
						if (itemStack.getCount() > 1)
						{
							itemRender.renderItemOverlayIntoGUI(this.fontRenderer, itemStack, guiX + slotX, guiY + slotY, String.valueOf(itemStack.getCount()));
						}

						GL11.glDisable(GL11.GL_LIGHTING);
						GL11.glDisable(GL11.GL_DEPTH_TEST);

						// draw a coloured overlay over the item
						// use a gray overlay for normal items, or a red overlay for this with container items
						int color = 0x9F8B8B8B;
						if (itemStack.getItem().hasContainerItem(itemStack)) // the hasContainerItem parameter is usually ignored, but some mods (Immersive Engineering) need it to be there
						{
							Item containerItem = itemStack.getItem().getContainerItem();
							Item slotItem = (renderSlot.getHasStack() ? renderSlot.getStack().getItem() : null);

							if (slotItem == null || (slotItem != null && containerItem != slotItem))
							{
								color = 0x80FF8B8B;
							}
						}
						this.drawRect(guiX + slotX, guiY + slotY, guiX + slotX + 16, guiY + slotY + 16, color);

						GL11.glEnable(GL11.GL_LIGHTING);
						GL11.glEnable(GL11.GL_DEPTH_TEST);
					}

				}
			}

		}

		RenderHelper.disableStandardItemLighting();

		GlStateManager.popMatrix();
	}



	private class GuiButton extends net.minecraft.client.gui.GuiButton
	{

		private final ButtonFacing facing;

		public GuiButton(int buttonId, int x, int y, ButtonFacing facing)
		{
			super(buttonId, x, y, 7, 11, "");
			this.facing = facing;
		}

		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
		{
			if (this.visible)
			{
				GL11.glPushMatrix();
				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

				mc.getTextureManager().bindTexture(UNCRAFTING_TABLE_GUI_TEXTURES);

				boolean mouseOnButton = (mouseX >= this.x) && (mouseY >= this.y) && (mouseX < this.x + this.width) && (mouseY < this.y + this.height);
				int textureX = (this.facing == ButtonFacing.LEFT ? 177 : 185);
				int textureY = (mouseOnButton ? 35 : 23);

				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glDisable(GL11.GL_DEPTH_TEST);

				drawTexturedModalRect(this.x, this.y, textureX, textureY, this.width, this.height);

				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glEnable(GL11.GL_DEPTH_TEST);

				GL11.glPopMatrix();
			}

		}
	}

	private static enum ButtonFacing
	{
		LEFT,
		RIGHT
	}

}
