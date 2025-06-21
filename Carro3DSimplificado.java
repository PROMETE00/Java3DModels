import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import javax.swing.*;

public class Carro3DSimplificado implements GLEventListener {
    private GLU glu;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Carro 3D Simplificado");
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        GLCanvas canvas = new GLCanvas(caps);
        canvas.addGLEventListener(new Carro3DSimplificado());
        
        frame.add(canvas);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.8f, 0.9f, 1.0f, 1.0f);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        
        glu.gluLookAt(6.0f, 10.0f, 14.0f, 0.0f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(25.0f, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
        
        dibujarCarro(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, (float)w/h, 1.0f, 100.0f);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {}

    private void dibujarCarro(GL2 gl) {
        // Carrocería (cubo rojo)
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        gl.glPushMatrix();
        gl.glScalef(1.3f, 0.5f, 2.2f);
        dibujarCuboSolido(gl);
        gl.glPopMatrix();

        // Techo (pirámide amarilla)
        gl.glColor3f(1.0f, 1.0f, 0.0f);
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.6f, 0.0f);
        gl.glScalef(1.6f, 0.8f, 1.0f);
        dibujarPiramideSolida(gl);
        gl.glPopMatrix();

        // Ruedas negras (cilindros)
        gl.glColor3f(0.1f, 0.1f, 0.1f);
        dibujarRuedaCompleta(gl, -1.5f, -0.3f, 1.5f);
        dibujarRuedaCompleta(gl,  1.3f, -0.3f, 1.5f);
        dibujarRuedaCompleta(gl, -1.5f, -0.3f, -1.5f);
        dibujarRuedaCompleta(gl,  1.3f, -0.3f, -1.5f);
    }

    private void dibujarCuboSolido(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);
        // Frontal
        gl.glVertex3f(-1.0f, -1.0f,  1.0f);
        gl.glVertex3f( 1.0f, -1.0f,  1.0f);
        gl.glVertex3f( 1.0f,  1.0f,  1.0f);
        gl.glVertex3f(-1.0f,  1.0f,  1.0f);
        // Trasera
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glVertex3f(-1.0f,  1.0f, -1.0f);
        gl.glVertex3f( 1.0f,  1.0f, -1.0f);
        gl.glVertex3f( 1.0f, -1.0f, -1.0f);
        // Superior
        gl.glVertex3f(-1.0f,  1.0f, -1.0f);
        gl.glVertex3f( 1.0f,  1.0f, -1.0f);
        gl.glVertex3f( 1.0f,  1.0f,  1.0f);
        gl.glVertex3f(-1.0f,  1.0f,  1.0f);
        // Inferior
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glVertex3f( 1.0f, -1.0f, -1.0f);
        gl.glVertex3f( 1.0f, -1.0f,  1.0f);
        gl.glVertex3f(-1.0f, -1.0f,  1.0f);
        // Izquierda
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glVertex3f(-1.0f, -1.0f,  1.0f);
        gl.glVertex3f(-1.0f,  1.0f,  1.0f);
        gl.glVertex3f(-1.0f,  1.0f, -1.0f);
        // Derecha
        gl.glVertex3f( 1.0f, -1.0f, -1.0f);
        gl.glVertex3f( 1.0f,  1.0f, -1.0f);
        gl.glVertex3f( 1.0f,  1.0f,  1.0f);
        gl.glVertex3f( 1.0f, -1.0f,  1.0f);
        gl.glEnd();
    }

    private void dibujarPiramideSolida(GL2 gl) {
        // Base
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex3f(-1.0f, -1.0f,  1.0f);
        gl.glVertex3f( 1.0f, -1.0f,  1.0f);
        gl.glVertex3f( 1.0f, -1.0f, -1.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glEnd();

        // Caras
        gl.glBegin(GL2.GL_TRIANGLES);
        gl.glVertex3f(0.0f, 1.0f,  0.0f);
        gl.glVertex3f(-1.0f, -1.0f,  1.0f);
        gl.glVertex3f( 1.0f, -1.0f,  1.0f);

        gl.glVertex3f(0.0f,  1.0f,  0.0f);
        gl.glVertex3f(1.0f, -1.0f,  1.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);

        gl.glVertex3f(0.0f,  1.0f,  0.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);

        gl.glVertex3f(0.0f,  1.0f,  0.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glVertex3f(-1.0f, -1.0f,  1.0f);
        gl.glEnd();
    }

    private void dibujarRuedaCompleta(GL2 gl, float x, float y, float z) {
        GLUquadric quadric = glu.gluNewQuadric();
        gl.glPushMatrix();
        gl.glTranslatef(x, y, z);
        gl.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
        
        // Cilindro principal
        glu.gluCylinder(quadric, 0.3f, 0.3f, 0.2f, 32, 1);
        // Disco frontal
        glu.gluDisk(quadric, 0.0f, 0.3f, 32, 1);
        // Disco trasero
        gl.glTranslatef(0.0f, 0.0f, 0.2f);
        glu.gluDisk(quadric, 0.0f, 0.3f, 32, 1);
        
        gl.glPopMatrix();
        glu.gluDeleteQuadric(quadric);
    }
}