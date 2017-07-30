package thut.extra;

import net.minecraftforge.fml.common.Mod;
import thut.core.client.render.model.ModelFactory;
import thut.extra.client.smd.SMDModel;

@Mod(modid = Reference.MODID, name = "ThutExtras", dependencies = "required-after:thutcore", version = Reference.VERSION, acceptedMinecraftVersions = Reference.MCVERSIONS)
public class ThutExtras
{
    public ThutExtras()
    {
        ModelFactory.registerIModel("smd", SMDModel.class);
    }
}