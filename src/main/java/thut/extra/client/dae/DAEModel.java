package thut.extra.client.dae;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import thut.core.client.render.animation.CapabilityAnimation.IAnimationHolder;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.model.IModel;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.tabula.components.Animation;

public class DAEModel implements IModel
{

    public DAEModel(ResourceLocation model)
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public HashMap<String, IExtendedModelPart> getParts()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getHeadParts()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HeadInfo getHeadInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void preProcessAnimations(Collection<List<Animation>> collection)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void applyAnimation(Entity entity, IAnimationHolder animate, IModelRenderer<?> renderer, float partialTicks,
            float limbSwing)
    {
        // TODO Auto-generated method stub
        
    }

}
