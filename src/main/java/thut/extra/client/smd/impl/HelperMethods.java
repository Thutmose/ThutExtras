package thut.extra.client.smd.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class HelperMethods
{
    public static void ensureIndex(ArrayList<?> a, int i)
    {
        while (a.size() <= i)
        {
            a.add(null);
        }
    }

    @SideOnly(Side.CLIENT)
    public static BufferedInputStream getStream(ResourceLocation resloc)
    {
        try
        {
            return new BufferedInputStream(
                    Minecraft.getMinecraft().getResourceManager().getResource(resloc).getInputStream());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}