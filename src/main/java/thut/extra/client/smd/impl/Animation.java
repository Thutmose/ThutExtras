package thut.extra.client.smd.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.lwjgl.util.vector.Matrix4f;

import net.minecraft.util.ResourceLocation;

public class Animation
{
    public final SmdModelLoader owner;
    public ArrayList<Frame>       frames            = new ArrayList<Frame>();
    public ArrayList<Bone>        bones             = new ArrayList<Bone>();
    public int                    currentFrameIndex = 0;
    public int                    lastFrameIndex;
    public int                    totalFrames;
    public int                    increase          = 0;
    public String                 animationName;
    private int                   frameIDBank       = 0;

    public Animation(SmdModelLoader owner, String animationName, ResourceLocation resloc) throws Exception
    {
        this.owner = owner;
        this.animationName = animationName;
        loadSmdAnim(resloc);
        setBoneChildren();
        reform();
    }

    public Animation(Animation anim, SmdModelLoader owner)
    {
        this.owner = owner;
        this.animationName = anim.animationName;
        for (Bone b : anim.bones)
        {
            this.bones.add(new Bone(b, b.parent != null ? (Bone) this.bones.get(b.parent.ID) : null, null));
        }
        for (Frame f : anim.frames)
        {
            this.frames.add(new Frame(f, this));
        }
        this.totalFrames = anim.totalFrames;
    }

    private void loadSmdAnim(ResourceLocation resloc) throws Exception
    {
        InputStream inputStream = HelperMethods.getStream(resloc);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String currentLine = null;
        int lineCount = 0;
        try
        {
            while ((currentLine = reader.readLine()) != null)
            {
                lineCount++;
                if (!currentLine.startsWith("version"))
                {
                    if (currentLine.startsWith("nodes"))
                    {
                        lineCount++;
                        while (!(currentLine = reader.readLine()).startsWith("end"))
                        {
                            lineCount++;
                            parseBone(currentLine, lineCount);
                        }
                    }
                    if (currentLine.startsWith("skeleton"))
                    {
                        startParsingAnimation(reader, lineCount, resloc);
                    }
                }
            }
        }
        catch (IOException e)
        {
            if (lineCount == -1) { throw new Exception("there was a problem opening the model file : " + resloc, e); }
            throw new Exception("an error occurred reading the SMD file \"" + resloc + "\" on line #" + lineCount, e);
        }
    }

    private void parseBone(String line, int lineCount)
    {
        String[] params = line.split("\\s+");
        int id = Integer.parseInt(params[0]);
        String boneName = params[1].replaceAll("\"", "");
        int parentID = Integer.parseInt(params[2]);
        Bone parent = parentID >= 0 ? (Bone) this.bones.get(parentID) : null;
        this.bones.add(id, new Bone(boneName, id, parent, null));
    }

    private void startParsingAnimation(BufferedReader reader, int count, ResourceLocation resloc) throws Exception
    {
        int lineCount = count;
        int currentTime = 0;
        lineCount++;
        String currentLine = null;
        try
        {
            while ((currentLine = reader.readLine()) != null)
            {
                lineCount++;
                String[] params = currentLine.split("\\s+");
                if (params[0].equalsIgnoreCase("time"))
                {
                    currentTime = Integer.parseInt(params[1]);
                    this.frames.add(currentTime, new Frame(this));
                }
                else
                {
                    if (currentLine.startsWith("end"))
                    {
                        this.totalFrames = this.frames.size();
                        return;
                    }
                    int boneIndex = Integer.parseInt(params[0]);
                    float[] locRots = new float[6];
                    for (int i = 1; i < 7; i++)
                    {
                        locRots[(i - 1)] = Float.parseFloat(params[i]);
                    }
                    Matrix4f animated = MatrixFactory.matrix4FromLocRot(locRots[0], -locRots[1], -locRots[2],
                            locRots[3], -locRots[4], -locRots[5]);
                    ((Frame) this.frames.get(currentTime)).addTransforms(boneIndex, animated);
                }
            }
        }
        catch (Exception e)
        {
            throw new Exception("an error occurred reading the SMD file \"" + resloc + "\" on line #" + lineCount, e);
        }
    }

    public int requestFrameID()
    {
        int result = this.frameIDBank;
        this.frameIDBank += 1;
        return result;
    }

    private void setBoneChildren()
    {
        Bone theBone;
        for (int i = 0; i < this.bones.size(); i++)
        {
            theBone = (Bone) this.bones.get(i);
            for (Bone child : this.bones)
            {
                if (child.parent == theBone)
                {
                    theBone.addChild(child);
                }
            }
        }
    }

    public void reform()
    {
        int rootID = this.owner.body.root.ID;
        for (int i = 0; i < this.frames.size(); i++)
        {
            Frame frame = (Frame) this.frames.get(i);
            frame.fixUp(rootID, 0.0F);
            frame.reform();
        }
    }

    public void precalculateAnimation(Model model)
    {
        for (int i = 0; i < this.frames.size(); i++)
        {
            model.resetVerts();
            Frame frame = (Frame) this.frames.get(i);
            for (int j = 0; j < model.bones.size(); j++)
            {
                Bone bone = (Bone) model.bones.get(j);
                Matrix4f animated = (Matrix4f) frame.transforms.get(j);
                bone.preloadAnimation(frame, animated);
            }
        }
    }

    public void nextFrameDebugMode()
    {
        if (this.increase + 1 >= 20)
        {
            this.increase = 0;
        }
        else
        {
            this.increase += 1;
        }
        this.currentFrameIndex = (this.increase / 10);
    }

    public void nextFrame()
    {
        if (this.currentFrameIndex == this.totalFrames - 1)
        {
            this.currentFrameIndex = 0;
        }
        else
        {
            this.currentFrameIndex += 1;
        }
    }

    public int getNumFrames()
    {
        return this.frames.size();
    }

    public void setCurrentFrame(int i)
    {
        if (this.lastFrameIndex != i)
        {
            this.currentFrameIndex = i;
            this.lastFrameIndex = i;
        }
    }
}