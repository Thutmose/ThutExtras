package thut.extra.client.smd.impl;

import java.util.ArrayList;
import org.lwjgl.util.vector.Matrix4f;

public class Frame
{
    public final int           ID;
    public Animation           owner;
    public ArrayList<Matrix4f> invertTransforms = new ArrayList<Matrix4f>();
    public ArrayList<Matrix4f> transforms       = new ArrayList<Matrix4f>();

    public Frame(Frame anim, Animation parent)
    {
        this.owner = parent;
        this.ID = anim.ID;
        this.transforms = anim.transforms;
        this.invertTransforms = anim.invertTransforms;
    }

    public Frame(Animation parent)
    {
        this.owner = parent;
        this.ID = parent.requestFrameID();
    }

    public void addTransforms(int index, Matrix4f invertedData)
    {
        this.transforms.add(index, invertedData);
        this.invertTransforms.add(index, Matrix4f.invert(invertedData, null));
    }

    public void fixUp(int id, float degrees)
    {
        float radians = (float) Math.toRadians(degrees);
        Matrix4f rotator = MatrixFactory.matrix4FromLocRot(0.0F, 0.0F, 0.0F, radians, 0.0F, 0.0F);
        Matrix4f.mul(rotator, (Matrix4f) this.transforms.get(id), (Matrix4f) this.transforms.get(id));
        Matrix4f.mul(Matrix4f.invert(rotator, null), (Matrix4f) this.invertTransforms.get(id),
                (Matrix4f) this.invertTransforms.get(id));
    }

    public void reform()
    {
        for (int i = 0; i < this.transforms.size(); i++)
        {
            Bone bone = (Bone) this.owner.bones.get(i);
            if (bone.parent != null)
            {
                Matrix4f temp = Matrix4f.mul((Matrix4f) this.transforms.get(bone.parent.ID),
                        (Matrix4f) this.transforms.get(i), null);
                this.transforms.set(i, temp);
                this.invertTransforms.set(i, Matrix4f.invert(temp, null));
            }
        }
    }
}