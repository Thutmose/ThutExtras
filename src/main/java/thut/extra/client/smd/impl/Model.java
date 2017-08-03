package thut.extra.client.smd.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector3f;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import net.minecraft.util.ResourceLocation;
import thut.core.client.render.model.IAnimationChanger;
import thut.core.client.render.model.IPartTexturer;
import thut.core.client.render.model.IRetexturableModel;
import thut.core.client.render.model.TextureCoordinate;
import thut.core.client.render.x3d.Material;

public class Model implements IRetexturableModel
{
    public final SmdModelLoader               owner;
    public ArrayList<Face>                    faces             = new ArrayList<Face>(0);
    public ArrayList<DeformableVertex>        verts             = new ArrayList<DeformableVertex>(0);
    public ArrayList<Bone>                    bones             = new ArrayList<Bone>(0);
    public HashMap<String, Bone>              nameToBoneMapping = new HashMap<String, Bone>();
    public HashMap<String, Material>          materialsByName;
    public HashMap<Material, ArrayList<Face>> facesByMaterial;
    public HashMap<Material, Integer>         materialMeshIDs   = Maps.newHashMap();
    public Animation                          currentAnim;
    public String                             fileName;
    private int                               vertexIDBank      = 0;
    protected boolean                         isBodyGroupPart;
    int                                       lineCount         = -1;
    public Bone                               root;
    IPartTexturer                             texturer;
    IAnimationChanger                         changer;
    private double[]                          uvShift           = { 0, 0 };

    public Model(Model model, SmdModelLoader owner)
    {
        this.owner = owner;
        this.isBodyGroupPart = model.isBodyGroupPart;
        for (Face face : model.faces)
        {
            DeformableVertex[] vertices = new DeformableVertex[face.vertices.length];
            for (int i = 0; i < vertices.length; i++)
            {
                DeformableVertex d = new DeformableVertex(face.vertices[i]);
                HelperMethods.ensureIndex(this.verts, d.ID);
                this.verts.set(d.ID, d);
            }
        }
        for (Face face : model.faces)
        {
            this.faces.add(new Face(face, this.verts));
        }
        for (int i = 0; i < model.bones.size(); i++)
        {
            Bone b = (Bone) model.bones.get(i);
            this.bones.add(new Bone(b, null, this));
        }
        for (int i = 0; i < model.bones.size(); i++)
        {
            Bone b = (Bone) model.bones.get(i);
            b.copy.setChildren(b, this.bones);
        }
        this.root = model.root.copy;
        owner.sendBoneData(this);
    }

    public Model(SmdModelLoader owner, ResourceLocation resloc) throws Exception
    {
        this.owner = owner;
        this.isBodyGroupPart = false;
        loadSmdModel(resloc, null);
        setBoneChildren();
        determineRoot();
        owner.sendBoneData(this);
    }

    public Model(SmdModelLoader owner, ResourceLocation resloc, Model body) throws Exception
    {
        this.owner = owner;
        this.isBodyGroupPart = true;
        loadSmdModel(resloc, body);
        setBoneChildren();
        determineRoot();
        owner.sendBoneData(this);
    }

    private void loadSmdModel(ResourceLocation resloc, Model body) throws Exception
    {
        InputStream inputStream = HelperMethods.getStream(resloc);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String currentLine = null;
        try
        {
            this.lineCount = 0;
            while ((currentLine = reader.readLine()) != null)
            {
                this.lineCount += 1;
                if (!currentLine.startsWith("version"))
                {
                    if (currentLine.startsWith("nodes"))
                    {
                        this.lineCount += 1;
                        while (!(currentLine = reader.readLine()).startsWith("end"))
                        {
                            this.lineCount += 1;
                            parseBone(currentLine.trim(), this.lineCount, body);
                        }
                    }
                    else if (currentLine.startsWith("skeleton"))
                    {
                        this.lineCount += 1;
                        reader.readLine();
                        this.lineCount += 1;
                        while (!(currentLine = reader.readLine()).startsWith("end"))
                        {
                            this.lineCount += 1;
                            if (!this.isBodyGroupPart)
                            {
                                parseBoneValues(currentLine.trim(), this.lineCount);
                            }
                        }
                    }
                    else if (currentLine.startsWith("triangles"))
                    {
                        this.lineCount += 1;
                        while (!(currentLine = reader.readLine()).startsWith("end"))
                        {
                            Material mat = this.owner.usesMaterials ? requestMaterial(currentLine) : null;
                            String[] params = new String[3];
                            for (int i = 0; i < 3; i++)
                            {
                                this.lineCount += 1;
                                params[i] = reader.readLine().trim();
                            }
                            parseFace(params, this.lineCount, mat);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (this.lineCount == -1) { throw new Exception("there was a problem opening the model file : " + resloc,
                    e); }
            throw new Exception("an error occurred reading the SMD file \"" + resloc + "\" on line #" + this.lineCount,
                    e);
        }
    }

    public Material requestMaterial(String materialName) throws Exception
    {
        if (!this.owner.usesMaterials) { return null; }
        if (this.materialsByName == null)
        {
            this.materialsByName = new HashMap<String, Material>();
        }
        Material result = (Material) this.materialsByName.get(materialName);
        if (result != null) { return result; }
        try
        {
            result = new Material(materialName, materialName, new Vector3f(), new Vector3f(), new Vector3f(), 1, 1, 0);
            this.materialsByName.put(materialName, result);
            return result;
        }
        catch (Exception e)
        {
            throw new Exception(e);
        }
    }

    private void parseBone(String line, int lineCount, Model body)
    {
        String[] params = line.split("\\s+");
        int id = Integer.parseInt(params[0]);
        String boneName = params[1].replaceAll("\"", "");
        Bone theBone = body != null ? body.getBoneByName(boneName) : null;
        if (theBone == null)
        {
            int parentID = Integer.parseInt(params[2]);
            Bone parent = parentID >= 0 ? (Bone) this.bones.get(parentID) : null;
            theBone = new Bone(boneName, id, parent, this);
        }
        HelperMethods.ensureIndex(this.bones, id);
        this.bones.set(id, theBone);
        this.nameToBoneMapping.put(boneName, theBone);
    }

    private void parseBoneValues(String line, int lineCount)
    {
        String[] params = line.split("\\s+");
        int id = Integer.parseInt(params[0]);

        float[] locRots = new float[6];
        for (int i = 1; i < 7; i++)
        {
            locRots[(i - 1)] = Float.parseFloat(params[i]);
        }
        Bone theBone = (Bone) this.bones.get(id);
        theBone.setRest(MatrixFactory.matrix4FromLocRot(locRots[0], -locRots[1], -locRots[2], locRots[3], -locRots[4],
                -locRots[5]));
    }

    private void parseFace(String[] params, int lineCount, Material mat)
    {
        DeformableVertex[] faceVerts = new DeformableVertex[3];
        TextureCoordinate[] uvs = new TextureCoordinate[3];
        for (int i = 0; i < 3; i++)
        {
            String[] values = params[i].split("\\s+");
            float x = Float.parseFloat(values[1]);
            float y = -Float.parseFloat(values[2]);
            float z = -Float.parseFloat(values[3]);
            float xn = Float.parseFloat(values[4]);
            float yn = -Float.parseFloat(values[5]);
            float zn = -Float.parseFloat(values[6]);

            DeformableVertex v = getExisting(x, y, z);
            if (v == null)
            {
                faceVerts[i] = new DeformableVertex(x, y, z, xn, yn, zn, this.vertexIDBank);
                HelperMethods.ensureIndex(this.verts, this.vertexIDBank);
                this.verts.set(this.vertexIDBank, faceVerts[i]);
                this.vertexIDBank += 1;
            }
            else
            {
                faceVerts[i] = v;
            }
            uvs[i] = new TextureCoordinate(Float.parseFloat(values[7]), 1.0F - Float.parseFloat(values[8]));
            if (values.length > 10)
            {
                doBoneWeights(values, faceVerts[i]);
            }
        }
        Face face = new Face(faceVerts, uvs);
        face.vertices = faceVerts;

        face.textureCoordinates = uvs;
        this.faces.add(face);
        if (mat != null)
        {
            if (this.facesByMaterial == null)
            {
                this.facesByMaterial = new HashMap<Material, ArrayList<Face>>();
            }
            ArrayList<Face> list = (ArrayList<Face>) this.facesByMaterial.get(mat);
            if (list == null)
            {
                this.facesByMaterial.put(mat, list = new ArrayList<Face>());
            }
            list.add(face);
        }
    }

    private DeformableVertex getExisting(float x, float y, float z)
    {
        for (DeformableVertex v : this.verts)
        {
            if (v.equals(x, y, z)) { return v; }
        }
        return null;
    }

    private void doBoneWeights(String[] values, DeformableVertex vert)
    {
        int links = Integer.parseInt(values[9]);
        float[] weights = new float[links];
        float sum = 0.0F;
        for (int i = 0; i < links; i++)
        {
            weights[i] = Float.parseFloat(values[(i * 2 + 11)]);
            sum += weights[i];
        }
        for (int i = 0; i < links; i++)
        {
            int boneID = Integer.parseInt(values[(i * 2 + 10)]);
            float weight = weights[i] / sum;
            ((Bone) this.bones.get(boneID)).addVertex(vert, weight);
        }
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

    private void determineRoot()
    {
        for (Bone b : this.bones)
        {
            if ((b.parent == null) && (!b.children.isEmpty()))
            {
                this.root = b;
                break;
            }
        }
        if (this.root == null)
        {
            for (Bone b : this.bones)
            {
                if (!b.name.equals("blender_implicit"))
                {
                    this.root = b;
                    break;
                }
            }
        }
    }

    public void setAnimation(Animation anim)
    {
        this.currentAnim = anim;
    }

    public Bone getBoneByID(int id)
    {
        try
        {
            return (Bone) this.bones.get(id);
        }
        catch (IndexOutOfBoundsException e)
        {
        }
        return null;
    }

    public Bone getBoneByName(String name)
    {
        for (Bone b : this.bones)
        {
            if (b.name.equals(name)) { return b; }
        }
        return null;
    }

    public Frame currentFrame()
    {
        return this.currentAnim == null ? null
                : this.currentAnim.frames == null ? null
                        : this.currentAnim.frames.isEmpty() ? null
                                : (Frame) this.currentAnim.frames.get(this.currentAnim.currentFrameIndex);
    }

    public void resetVerts()
    {
        for (DeformableVertex v : this.verts)
        {
            v.reset();
        }
    }

    public void render()
    {
        GL11.glPushMatrix();
        boolean smooth = true;
        if (!this.owner.usesMaterials)
        {
            GL11.glBegin(4);
            for (Face f : this.faces)
            {
                f.addFaceForRender(smooth);
            }
            GL11.glEnd();
        }
        else
        {
            for (Map.Entry<Material, ArrayList<Face>> entry : this.facesByMaterial.entrySet())
            {
                Material mat;
                if ((mat = entry.getKey()) != null)
                {
                    String tex = mat.name;
                    boolean textureShift = false;
                    if (texturer != null)
                    {
                        texturer.applyTexture(tex);
                        if (textureShift = texturer.shiftUVs(tex, uvShift))
                        {
                            GL11.glMatrixMode(GL11.GL_TEXTURE);
                            GL11.glTranslated(uvShift[0], uvShift[1], 0.0F);
                            GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        }
                    }
                    render(entry, smooth);
                    if (textureShift)
                    {
                        GL11.glMatrixMode(GL11.GL_TEXTURE);
                        GL11.glLoadIdentity();
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    }
                }
            }
        }
        GL11.glPopMatrix();
    }

    private void render(Map.Entry<Material, ArrayList<Face>> entry, boolean smooth)
    {
        GL11.glBegin(4);
        for (Face face : entry.getValue())
        {
            face.addFaceForRender(smooth);
        }
        GL11.glEnd();
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
}