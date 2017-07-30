package thut.extra.client.smd;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.util.ResourceLocation;
import thut.core.client.render.model.IAnimationChanger;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.model.IModel;
import thut.core.client.render.model.IModelCustom;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.model.IPartTexturer;
import thut.core.client.render.model.IRetexturableModel;
import thut.core.client.render.tabula.components.Animation;
import thut.extra.client.smd.impl.Bone;
import thut.extra.client.smd.impl.SmdModelLoader;

public class SMDModel implements IModelCustom, IModel, IRetexturableModel, IFakeExtendedPart
{
    private final HashMap<String, IExtendedModelPart> nullPartsMap = Maps.newHashMap();
    private final HashMap<String, IExtendedModelPart> subPartsMap  = Maps.newHashMap();
    private final Set<String>                         nullHeadSet  = Sets.newHashSet();
    private final HeadInfo                            info         = new HeadInfo();
    SmdModelLoader                                    wrapped;
    IPartTexturer                                     texturer;
    IAnimationChanger                                 changer;

    public SMDModel()
    {
        nullPartsMap.put(getName(), this);
    }

    public SMDModel(ResourceLocation model)
    {
        this();

        try
        {
            wrapped = new SmdModelLoader(model);
            wrapped.usesMaterials = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void render(IModelRenderer<?> renderer)
    {
        if (wrapped != null)
        {
            if (renderer != null)
            {
                wrapped.setAnimation(renderer.getAnimation());
            }
            wrapped.body.setTexturer(texturer);
            wrapped.body.setAnimationChanger(changer);
            GL11.glScaled(0.165, 0.165, 0.165);
            GL11.glRotated(180, 0, 1, 0);

            // only increment frame if a tick has passed.
            if (wrapped.body.currentAnim != null && info.currentTick != info.lastTick)
            {
                wrapped.body.currentAnim.nextFrame();
                wrapped.animate();
            }
            // Check head parts for rendering rotations of them.
            for (String s : getHeadParts())
            {
                Bone bone = wrapped.body.getBoneByName(s);
                if (bone != null)
                {
                    if (bone.custom == null)
                    {
                        bone.custom = new Matrix4f(bone.rest);
                        bone.customInverted = new Matrix4f(bone.restInverted);
                    }
                    float yaw = Math.max(Math.min(info.headYaw, info.yawCapMax), info.yawCapMin);
                    yaw = (float) Math.toRadians(yaw) * info.yawDirection;
                    float pitch = Math.max(Math.min(info.headPitch, info.pitchCapMax), info.pitchCapMin);
                    pitch = (float) Math.toRadians(pitch) * info.pitchDirection;
                    // Rotate Yaw
                    Matrix4f headRot = new Matrix4f();

                    float cosT = (float) Math.cos(pitch);
                    float sinT = (float) Math.sin(pitch);

                    float cosA = (float) Math.cos(yaw);
                    float sinA = (float) Math.sin(yaw);

                    Matrix4f rotT = new Matrix4f();
                    Matrix4f rotA = new Matrix4f();

                    switch (info.yawAxis)
                    {
                    case 0:
                        rotA.m00 = cosA;
                        rotA.m01 = sinA;
                        rotA.m10 = -sinA;
                        rotA.m11 = cosA;
                        break;
                    case 1:
                        rotA.m00 = cosA;
                        rotA.m02 = sinA;
                        rotA.m20 = -sinA;
                        rotA.m22 = cosA;
                        break;
                    default:
                        rotA.m11 = cosA;
                        rotA.m12 = sinA;
                        rotA.m21 = -sinA;
                        rotA.m22 = cosA;
                    }

                    switch (info.pitchAxis)
                    {
                    case 2:
                        rotT.m11 = cosT;
                        rotT.m12 = sinT;
                        rotT.m21 = -sinT;
                        rotT.m22 = cosT;
                        break;
                    case 0:
                        rotT.m00 = cosT;
                        rotT.m01 = sinT;
                        rotT.m10 = -sinT;
                        rotT.m11 = cosT;
                        break;
                    default:
                        rotT.m00 = cosT;
                        rotT.m02 = sinT;
                        rotT.m20 = -sinT;
                        rotT.m22 = cosT;
                    }
                    headRot = Matrix4f.mul(rotT, rotA, headRot);
                    // Apply the rotation matricies.
                    bone.rest = Matrix4f.mul(bone.custom, headRot, bone.rest);
                    // Apply inversion.
                    bone.restInverted = Matrix4f.invert(bone.rest, bone.restInverted);
                    bone.reformChildren();
                }
            }
            wrapped.renderAll();
        }
    }

    @Override
    public void setAnimationChanger(IAnimationChanger changer)
    {
        this.changer = changer;
    }

    @Override
    public void setTexturer(IPartTexturer texturer)
    {
        this.texturer = texturer;
    }

    @Override
    public HashMap<String, IExtendedModelPart> getParts()
    {
        // SMD Renders whole thing at once, so no part rendering.
        return nullPartsMap;
    }

    @Override
    public void preProcessAnimations(Collection<Animation> animations)
    {
        // SMD handles animations differently, so nothing here.
    }

    @Override
    public void renderAll(IModelRenderer<?> renderer)
    {
        render(renderer);
    }

    @Override
    public void renderAll()
    {
        render(null);
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames)
    {
        // SMD Renders whole thing at once, so no part rendering.
        render(null);
    }

    @Override
    public void renderOnly(String... groupNames)
    {
        // SMD Renders whole thing at once, so no part rendering.
        render(null);
    }

    @Override
    public void renderPart(String partName)
    {
        // SMD Renders whole thing at once, so no part rendering.
        render(null);
    }

    @Override
    public Set<String> getHeadParts()
    {
        return nullHeadSet;
    }

    @Override
    public HeadInfo getHeadInfo()
    {
        return info;
    }

    @Override
    public String getName()
    {
        return "main";
    }

    @Override
    public HashMap<String, IExtendedModelPart> getSubParts()
    {
        return subPartsMap;
    }

    @Override
    public String getType()
    {
        return "smd";
    }

}
