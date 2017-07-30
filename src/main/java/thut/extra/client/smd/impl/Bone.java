package thut.extra.client.smd.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lwjgl.util.vector.Matrix4f;

public class Bone
{
    public Bone                                 copy               = null;
    public String                               name;
    public int                                  ID;
    public Bone                                 parent;
    public Model                                owner;
    private Boolean                             isDummy;
    public Matrix4f                             rest;
    public Matrix4f                             restInverted;
    public Matrix4f                             modified           = new Matrix4f();
    public Matrix4f                             difference         = new Matrix4f();
    public Matrix4f                             prevInverted       = new Matrix4f();
    private final Matrix4f                      dummy1             = new Matrix4f();
    private final Matrix4f                      dummy2             = new Matrix4f();
    public Matrix4f                             custom;
    public Matrix4f                             customInverted;
    public ArrayList<Bone>                      children           = new ArrayList<Bone>(0);
    public HashMap<DeformableVertex, Float>     verts              = new HashMap<DeformableVertex, Float>();
    public HashMap<String, ArrayList<Matrix4f>> animatedTransforms = new HashMap<String, ArrayList<Matrix4f>>();
    public float[]                              currentVals        = new float[6];

    public Bone(String name, int ID, Bone parent, Model owner)
    {
        this.name = name;
        this.ID = ID;
        this.parent = parent;
        this.owner = owner;
    }

    public Bone(Bone b, Bone parent, Model owner)
    {
        this.name = b.name;
        this.ID = b.ID;
        this.owner = owner;
        this.parent = parent;

        Iterator<Map.Entry<DeformableVertex, Float>> iterator = b.verts.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<DeformableVertex, Float> entry = iterator.next();
            this.verts.put(owner.verts.get(((DeformableVertex) entry.getKey()).ID), entry.getValue());
        }
        this.animatedTransforms = new HashMap<String, ArrayList<Matrix4f>>(b.animatedTransforms);
        this.restInverted = b.restInverted;
        this.rest = b.rest;
        b.copy = this;
    }

    public void setChildren(Bone b, ArrayList<Bone> bones)
    {
        for (int i = 0; i < b.children.size(); i++)
        {
            Bone child = (Bone) b.children.get(i);
            this.children.add(bones.get(child.ID));
            ((Bone) bones.get(child.ID)).parent = this;
        }
    }

    public boolean isDummy()
    {
        return (this.isDummy == null
                ? (this.isDummy = Boolean.valueOf((this.parent == null) && (this.children.isEmpty()))) : this.isDummy)
                        .booleanValue();
    }

    public void setRest(Matrix4f resting)
    {
        this.rest = resting;
    }

    public void addChild(Bone child)
    {
        this.children.add(child);
    }

    public void addVertex(DeformableVertex v, float weight)
    {
        if (this.name.equals("blender_implicit")) { throw new UnsupportedOperationException("NO."); }
        this.verts.put(v, Float.valueOf(weight));
    }

    private void reform(Matrix4f parentMatrix)
    {
        this.rest = Matrix4f.mul(parentMatrix, this.rest, null);
        reformChildren();
    }

    public void reformChildren()
    {
        for (Bone child : this.children)
        {
            child.reform(this.rest);
        }
    }

    public void invertRestMatrix()
    {
        this.restInverted = Matrix4f.invert(this.rest, null);
    }

    public void reset()
    {
        this.modified.setIdentity();
    }

    public void preloadAnimation(Frame key, Matrix4f animated)
    {
        ArrayList<Matrix4f> precalcArray;
        if (this.animatedTransforms.containsKey(key.owner.animationName))
        {
            precalcArray = (ArrayList<Matrix4f>) this.animatedTransforms.get(key.owner.animationName);
        }
        else
        {
            precalcArray = new ArrayList<Matrix4f>();
        }
        HelperMethods.ensureIndex(precalcArray, key.ID);
        precalcArray.set(key.ID, animated);
        this.animatedTransforms.put(key.owner.animationName, precalcArray);
    }

    public void setModified()
    {
        Matrix4f edit = MatrixFactory.matrix4FromFloatArray(this.currentVals);
        Matrix4f real;
        Matrix4f realInverted;
        if ((this.owner.owner.hasAnimations()) && (this.owner.currentAnim != null))
        {
            Frame currentFrame = (Frame) this.owner.currentAnim.frames.get(this.owner.currentAnim.currentFrameIndex);
            realInverted = new Matrix4f(currentFrame.transforms.get(this.ID));

            real = new Matrix4f(currentFrame.invertTransforms.get(this.ID));
        }
        else
        {
            realInverted = this.rest;
            real = this.restInverted;
        }

        Matrix4f delta = Matrix4f.mul(realInverted, edit, this.dummy1);

        Matrix4f.mul(delta, real, delta);

        this.modified = (this.parent != null ? Matrix4f.mul(this.parent.modified, delta, initModified()) : delta);
        Matrix4f absolute = Matrix4f.mul(real, this.modified, this.dummy2);

        Matrix4f.invert(absolute, this.prevInverted);
        for (Bone child : this.children)
        {
            child.setModified();
        }
    }

    protected Matrix4f initModified()
    {
        return this.modified == null ? (this.modified = new Matrix4f()) : this.modified;
    }

    public void applyModified()
    {
        Frame currentFrame = this.owner.currentFrame();
        if (currentFrame != null)
        {
            ArrayList<Matrix4f> precalcArray = (ArrayList<Matrix4f>) this.animatedTransforms
                    .get(currentFrame.owner.animationName);
            Matrix4f animated = (Matrix4f) precalcArray.get(currentFrame.ID);
            Matrix4f animatedChange = Matrix4f.mul(animated, this.restInverted, this.dummy2);
            this.modified = (this.modified == null ? animatedChange
                    : Matrix4f.mul(this.modified, animatedChange, this.modified));
        }
        for (Map.Entry<DeformableVertex, Float> entry : this.verts.entrySet())
        {
            ((DeformableVertex) entry.getKey()).applyModified(this, ((Float) entry.getValue()).floatValue());
        }
        reset();
    }
}
