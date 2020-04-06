package org.cyclops.cyclopscore.helper;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helpers related to guis.
 */
public class GuiHelpers {

    /**
     * The default item slot size. Width and height are equal.
     */
    public static int SLOT_SIZE = 18;
    /**
     * The default inner item slot size. Width and height are equal.
     */
    public static int SLOT_SIZE_INNER = 16;

    /**
     * Render a fluid tank in a gui.
     *
     * @param gui The gui to render in.
     * @param fluidStack The fluid to render.
     * @param capacity The tank capacity.
     * @param x The gui x position, including gui left.
     * @param y The gui y position, including gui top.
     * @param width The tank width.
     * @param height The tank height.
     */
    public static void renderFluidTank(AbstractGui gui, @Nullable FluidStack fluidStack, int capacity,
                                       int x, int y, int width, int height) {
        if (fluidStack != null && capacity > 0) {
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            int level = (int) (height * (((double) fluidStack.getAmount()) / capacity));
            TextureAtlasSprite icon = RenderHelpers.getFluidIcon(fluidStack, Direction.UP);
            int verticalOffset = 0;
            while(level > 0) {
                int textureHeight;
                if(level > 16) {
                    textureHeight = 16;
                    level -= 16;
                } else {
                    textureHeight = level;
                    level = 0;
                }

                RenderHelpers.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

                // Fluids can have a custom overlay color, use this to render.
                Triple<Float, Float, Float> colorParts = Helpers.intToRGB(fluidStack.getFluid().getAttributes().getColor(fluidStack));
                RenderSystem.color3f(colorParts.getLeft(), colorParts.getMiddle(), colorParts.getRight());

                AbstractGui.blit(x, y - textureHeight - verticalOffset + height, 0, width, textureHeight, icon);

                // Reset color when done
                GlStateManager.color4f(1, 1, 1, 1);

                verticalOffset = verticalOffset + 16;
            }

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
            textureManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }

    /**
     * Render the given fluid in a standard slot.
     * @param gui The gui to render in.
     * @param fluidStack The fluid to render.
     * @param x The slot X position.
     * @param y The slot Y position.
     */
    public static void renderFluidSlot(AbstractGui gui, @Nullable FluidStack fluidStack, int x, int y) {
        if (fluidStack != null) {
            GuiHelpers.renderFluidTank(gui, fluidStack, fluidStack.getAmount(), x, y, SLOT_SIZE_INNER, SLOT_SIZE_INNER);
        }
    }

    /**
     * Render a fluid tank in a gui with a tank overlay.
     * This assumes that the tank overlay has the provided width and height.
     *
     * @param gui The gui to render in.
     * @param fluidStack The fluid to render.
     * @param capacity The tank capacity.
     * @param x The gui x position, including gui left.
     * @param y The gui y position, including gui top.
     * @param width The tank width.
     * @param height The tank height.
     * @param textureOverlay The texture of the tank overlay.
     * @param overlayTextureX The overlay x texture position.
     * @param overlayTextureY The overlay y texture position.
     */
    public static void renderOverlayedFluidTank(AbstractGui gui, @Nullable FluidStack fluidStack, int capacity,
                                                int x, int y, int width, int height,
                                                ResourceLocation textureOverlay, int overlayTextureX, int overlayTextureY) {
        renderFluidTank(gui, fluidStack, capacity, x, y, width, height);
        if (fluidStack != null && capacity > 0) {
            GlStateManager.enableBlend();
            RenderHelpers.bindTexture(textureOverlay);
            gui.blit(x, y, overlayTextureX, overlayTextureY, width, height);
        }
    }

    /**
     * Render a progress bar in a certain direction.
     * The currently bound texture will be used to render the progress bar.
     *
     * @param gui The gui to render in.
     * @param x The gui x position, including gui left.
     * @param y The gui y position, including gui top.
     * @param width The progress bar width.
     * @param height The progress bar height.
     * @param textureX The texture x position.
     * @param textureY The texture y position.
     * @param direction The direction to progress in.
     * @param progress The current progress.
     * @param progressMax The maximum progress.
     */
    public static void renderProgressBar(AbstractGui gui, int x, int y, int width, int height, int textureX, int textureY,
                                         ProgressDirection direction, int progress, int progressMax) {
        if (progressMax > 0 && progress > 0) {
            int scaledWidth = width;
            int scaledHeight = height;

            // Scale the width and/or height
            if (direction.getIncrementX() != 0) {
                scaledWidth = (int) (scaledWidth * (((double) progress) / progressMax));
            }
            if (direction.getIncrementY() != 0) {
                scaledHeight = (int) (scaledHeight * (((double) progress) / progressMax));
            }

            // If increments happen inversely, make sure we start incrementing from the other end of the progress bar
            if (direction.getIncrementX() < 0) {
                int offset = width - scaledWidth;
                x += offset;
                textureX += offset;
            }
            if (direction.getIncrementY() < 0) {
                int offset = height - scaledHeight;
                y += offset;
                textureY += offset;
            }

            gui.blit(x, y, textureX, textureY, scaledWidth, scaledHeight);
        }
    }

    /**
     * Draw a tooltip.
     * @param gui The gui to draw in.
     * @param lines A list of lines.
     * @param x Tooltip X.
     * @param y Tooltip Y.
     */
    public static void drawTooltip(ContainerScreen gui, List<ITextComponent> lines, int x, int y) {
        int guiLeft = gui.getGuiLeft();
        int guiTop = gui.getGuiTop();
        int width = gui.width;
        int height = gui.height;
        Minecraft mc = Minecraft.getInstance();

        GlStateManager.pushMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();

        int tooltipWidth = 0;
        int tempWidth;
        int xStart;
        int yStart;

        for(ITextComponent line : lines) {
            tempWidth = mc.fontRenderer.getStringWidth(line.getFormattedText());

            if(tempWidth > tooltipWidth) {
                tooltipWidth = tempWidth;
            }
        }

        xStart = x + 12;
        yStart = y - 12;
        int tooltipHeight = 8;

        if(lines.size() > 1) {
            tooltipHeight += 2 + (lines.size() - 1) * 10;
        }

        if(guiLeft + xStart + tooltipWidth + 6 > width) {
            xStart = width - tooltipWidth - guiLeft - 6;
        }

        if(guiTop + yStart + tooltipHeight + 6 > height) {
            yStart = height - tooltipHeight - guiTop - 6;
        }

        float zLevel = 300.0F;
        mc.getItemRenderer().zLevel = 300.0F;
        int color1 = -267386864;
        fillGradient(xStart - 3, yStart - 4, xStart + tooltipWidth + 3, yStart - 3, color1, color1, zLevel);
        fillGradient(xStart - 3, yStart + tooltipHeight + 3, xStart + tooltipWidth + 3, yStart + tooltipHeight + 4, color1, color1, zLevel);
        fillGradient(xStart - 3, yStart - 3, xStart + tooltipWidth + 3, yStart + tooltipHeight + 3, color1, color1, zLevel);
        fillGradient(xStart - 4, yStart - 3, xStart - 3, yStart + tooltipHeight + 3, color1, color1, zLevel);
        fillGradient(xStart + tooltipWidth + 3, yStart - 3, xStart + tooltipWidth + 4, yStart + tooltipHeight + 3, color1, color1, zLevel);
        int color2 = 1347420415;
        int color3 = (color2 & 16711422) >> 1 | color2 & -16777216;
        fillGradient(xStart - 3, yStart - 3 + 1, xStart - 3 + 1, yStart + tooltipHeight + 3 - 1, color2, color3, zLevel);
        fillGradient(xStart + tooltipWidth + 2, yStart - 3 + 1, xStart + tooltipWidth + 3, yStart + tooltipHeight + 3 - 1, color2, color3, zLevel);
        fillGradient(xStart - 3, yStart - 3, xStart + tooltipWidth + 3, yStart - 3 + 1, color2, color2, zLevel);
        fillGradient(xStart - 3, yStart + tooltipHeight + 2, xStart + tooltipWidth + 3, yStart + tooltipHeight + 3, color3, color3, zLevel);

        for(int stringIndex = 0; stringIndex < lines.size(); ++stringIndex) {
            ITextComponent line = lines.get(stringIndex);

            if(stringIndex == 0) {
                line = new StringTextComponent("\u00a7" + Integer.toHexString(15)).appendSibling(line);
            } else {
                line = new StringTextComponent("\u00a77").appendSibling(line);
            }

            mc.fontRenderer.drawStringWithShadow(line.getFormattedText(), xStart, yStart, -1);

            if(stringIndex == 0) {
                yStart += 2;
            }

            yStart += 10;
        }

        GlStateManager.popMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        mc.getItemRenderer().zLevel = 0.0F;
    }

    /**
     * Render a rectangle.
     *
     * Static variant of AbstractGui#fillGradient(int, int, int, int, int, int)}.
     *
     * @param left Left X.
     * @param top Top Y.
     * @param right Right X.
     * @param bottom Bottom Y.
     * @param startColor Start gradient color.
     * @param endColor End gradient color.
     * @param zLevel The Z level to render at.
     */
    public static void fillGradient(int left, int top, int right, int bottom, int startColor, int endColor, float zLevel) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos((double)right, (double)top, (double)zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos((double)left, (double)top, (double)zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos((double)left, (double)bottom, (double)zLevel).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos((double)right, (double)bottom, (double)zLevel).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }

    /**
     * Render a tooltip if the mouse if in the bounding box defined by the given position, width and height.
     * The tooltip lines supplier can return an optional list.
     * @param gui The gui to render in.
     * @param x The gui x position, excluding gui left.
     * @param y The gui y position, excluding gui top.
     * @param width The area width.
     * @param height The area height.
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     * @param linesSupplier A supplier for the optional tooltip lines to render.
     *                      No tooltip will be rendered when the optional value is absent.
     *                      This will only be called when needed.
     */
    public static void renderTooltipOptional(ContainerScreen gui, int x, int y, int width, int height,
                                             int mouseX, int mouseY, Supplier<Optional<List<ITextComponent>>> linesSupplier) {
        if(RenderHelpers.isPointInRegion(x, y, width, height, mouseX - gui.getGuiLeft(), mouseY - gui.getGuiTop())) {
            linesSupplier.get().ifPresent(
                    lines -> drawTooltip(gui, lines, mouseX - gui.getGuiLeft(), mouseY - gui.getGuiTop()));
        }
    }

    /**
     * Render a tooltip if the mouse if in the bounding box defined by the given position, width and height.
     * @param gui The gui to render in.
     * @param x The gui x position, excluding gui left.
     * @param y The gui y position, excluding gui top.
     * @param width The area width.
     * @param height The area height.
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     * @param linesSupplier A supplier for the tooltip lines to render.
     *                      This will only be called when needed.
     */
    public static void renderTooltip(ContainerScreen gui, int x, int y, int width, int height,
                                     int mouseX, int mouseY, Supplier<List<ITextComponent>> linesSupplier) {
        renderTooltipOptional(gui, x, y, width, height, mouseX, mouseY, () -> Optional.of(linesSupplier.get()));
    }

    private static final List<Pair<Long, String>> COUNT_SCALES = Lists.newArrayList(
            Pair.of(1000000000000000000L, "E"),
            Pair.of(1000000000000000L, "P"),
            Pair.of(1000000000000L, "T"),
            Pair.of(1000000000L, "G"),
            Pair.of(1000000L, "M"),
            Pair.of(1000L, "K")
    );

    /**
     * Stringify a (potentially large) quantity to a scaled string.
     *
     * For example, 123765 will be converted as 1.23M.
     *
     * @param quantity A quantity.
     * @return A scaled quantity string.
     */
    public static String quantityToScaledString(long quantity) {
        for (Pair<Long, String> countScale : COUNT_SCALES) {
            long scale = countScale.getLeft();
            if (quantity >= scale) {
                long division = quantity / scale;
                String divisionString = String.valueOf(division);

                // Add digits if string is short
                if (division < 10) {
                    long mod = quantity % scale;
                    if (mod > 0) {
                        long digits = mod * 100 / scale;
                        divisionString += "." + (digits < 10 ? "0" : "") + String.valueOf(digits);
                    }
                } else if (division < 100) {
                    long mod = quantity % scale;
                    if (mod > 0) {
                        long digits = mod * 10 / scale;
                        divisionString += "." + String.valueOf(digits);
                    }
                }

                return divisionString + countScale.getRight();
            }
        }
        return String.valueOf(quantity);
    }

    /**
     * Represents the direction of a progress bar.
     */
    public static enum ProgressDirection {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0),

        UP_LEFT(-1, -1),
        UP_RIGHT(1, -1),
        DOWN_LEFT(-1, 1),
        DOWN_RIGHT(1, 1);

        private final int incrementX;
        private final int incrementY;

        private ProgressDirection(int incrementX, int incrementY) {
            this.incrementX = incrementX;
            this.incrementY = incrementY;
        }

        public int getIncrementX() {
            return incrementX;
        }

        public int getIncrementY() {
            return incrementY;
        }
    }

}
