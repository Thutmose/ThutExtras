package thut.extra.client.smd.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import net.minecraft.util.ResourceLocation;

public class SmdModelLoader
{
    public Model                      body;
    public HashMap<String, Animation> anims         = new HashMap<String, Animation>();
    public Bone                       root;
    public ArrayList<Bone>            allBones;
    public Animation                  currentAnimation;
    public ResourceLocation           resource;
    public String                     materialPath;
    public boolean                    hasAnimations = true;
    public boolean                    usesMaterials = true;

    public SmdModelLoader(SmdModelLoader model)
    {
        this.body = new Model(model.body, this);
        Iterator<Map.Entry<String, Animation>> iterator = model.anims.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, Animation> entry = iterator.next();
            this.anims.put(entry.getKey(), new Animation((Animation) entry.getValue(), this));
        }
        this.hasAnimations = model.hasAnimations;
        this.usesMaterials = model.usesMaterials;
        this.resource = model.resource;
        this.currentAnimation = ((Animation) this.anims.get("idle"));
    }

    public SmdModelLoader(ResourceLocation resource) throws Exception
    {
        this.resource = resource;
        loadQC(resource);
        reformBones();
        precalculateAnims();
    }

    private void loadQC(ResourceLocation resloc) throws Exception
    {
        try
        {
            ResourceLocation modelPath = resloc;
            this.body = new Model(this, modelPath);

            List<String> anims = Lists.newArrayList("idle", "walking", "flying", "sleeping", "swimming");
            String resLoc = resloc.toString();
            for (String s : anims)
            {
                String anim = resLoc.endsWith("smd") ? resLoc.replace(".smd", "/" + s + ".smd")
                        : resLoc.replace(".SMD", "/" + s + ".smd");
                ResourceLocation animation = new ResourceLocation(anim);
                try
                {
                    this.anims.put(s, new Animation(this, s, animation));
                    if (s.equalsIgnoreCase("idle"))
                    {
                        this.currentAnimation = ((Animation) this.anims.get(s));
                    }
                }
                catch (Exception e)
                {
                    // e.printStackTrace();
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    public String getType()
    {
        return "pqc";
    }

    private void precalculateAnims()
    {
        for (Animation anim : this.anims.values())
        {
            anim.precalculateAnimation(this.body);
        }
    }

    public void renderAll()
    {
        GL11.glShadeModel(7425);
        this.body.render();
        GL11.glShadeModel(7424);
    }

    void sendBoneData(Model model)
    {
        this.allBones = model.bones;
        if (!model.isBodyGroupPart)
        {
            this.root = model.root;
        }
    }

    private void reformBones()
    {
        this.root.reformChildren();
        for (Bone b : this.allBones)
        {
            b.invertRestMatrix();
        }
    }

    public void animate()
    {
        resetVerts(this.body);
        if (this.body.currentAnim == null)
        {
            setAnimation("idle");
        }
        this.root.setModified();
        for (Bone b : this.allBones)
        {
            b.applyModified();
        }
        applyVertChange(this.body);
    }

    private void resetVerts(Model model)
    {
        if (model == null) { return; }
        for (DeformableVertex v : model.verts)
        {
            v.reset();
        }
    }

    private void applyVertChange(Model model)
    {
        if (model == null) { return; }
        for (DeformableVertex v : model.verts)
        {
            v.applyChange();
        }
    }

    public void setAnimation(String animname)
    {
        Animation old = this.currentAnimation;
        if (this.anims.containsKey(animname))
        {
            this.currentAnimation = ((Animation) this.anims.get(animname));
        }
        else
        {
            this.currentAnimation = ((Animation) this.anims.get("idle"));
        }
        this.body.setAnimation(this.currentAnimation);
        if (old != this.currentAnimation)
        {
        }
    }

    public void renderPart(String partName)
    {
    }

    public boolean hasAnimations()
    {
        return this.hasAnimations;
    }
}