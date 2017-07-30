package thut.extra.client.smd.impl;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import thut.core.client.render.model.TextureCoordinate;
import thut.core.client.render.model.Vertex;

public class Face
{
    public DeformableVertex[]  vertices;
    public TextureCoordinate[] textureCoordinates;
    public Vertex              faceNormal;

    public Face(DeformableVertex[] xyz, TextureCoordinate[] uvs)
    {
        this.vertices = xyz;
        this.textureCoordinates = uvs;
    }

    public Face(Face face, ArrayList<DeformableVertex> verts)
    {
        this.vertices = new DeformableVertex[face.vertices.length];
        for (int i = 0; i < this.vertices.length; i++)
        {
            this.vertices[i] = ((DeformableVertex) verts.get(face.vertices[i].ID));
        }
        this.textureCoordinates = new TextureCoordinate[face.textureCoordinates.length];
        System.arraycopy(face.textureCoordinates, 0, this.textureCoordinates, 0, this.textureCoordinates.length);
        if (face.faceNormal != null)
        {
            this.faceNormal = face.faceNormal;
        }
    }

    public void addFaceForRender(boolean smoothShading)
    {
        if ((!smoothShading) && (this.faceNormal == null))
        {
            this.faceNormal = calculateFaceNormal();
        }
        for (int i = 0; i < 3; i++)
        {
            GL11.glTexCoord2f(this.textureCoordinates[i].u, this.textureCoordinates[i].v);
            if (!smoothShading)
            {
                GL11.glNormal3f(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z);
            }
            else
            {
                GL11.glNormal3f(this.vertices[i].xn, this.vertices[i].yn, this.vertices[i].zn);
            }
            GL11.glVertex3d(this.vertices[i].x, this.vertices[i].y, this.vertices[i].z);
        }
    }

    public Vertex calculateFaceNormal()
    {
        return new Vertex(0, 0, 0);
    }
}