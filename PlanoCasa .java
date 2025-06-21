/*  PlanoCasaJOGL.java  – versión mínima
    ↑ replica EXACTA del plano de 5.85 m × 7.35 m
    autor: ChatGPT (o3)
*/
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;

import java.awt.event.*;
import javax.swing.*;

public class PlanoCasa implements GLEventListener,MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    /*  ––––– constantes –––––  */
    private static final float W = 5.85f;   // ancho exterior (m)
    private static final float D = 7.35f;   // fondo exterior (m)
    private static final float T = 0.15f;   // grosor muro (m)
    private static final float H = 2.50f;   // alto muro   (m)

    /*  cámara orbital + libre  */
    private float camDist = 14f, camRotX = 35f, camRotY = -35f;
    private float camPosX = 0f,  camPosY = 0f,  camPosZ = 0f;
    private final float move = 0.20f;
    private int lastX, lastY;   private boolean drag = false;

    private final GLU  glu  = new GLU();
    private final GLUT glut = new GLUT();

    /* ================= MAIN ================= */
    public static void main(String[] a) {
        SwingUtilities.invokeLater(() -> {
            GLProfile p = GLProfile.get(GLProfile.GL2);
            GLCanvas  c = new GLCanvas(new GLCapabilities(p));
            PlanoCasa app = new PlanoCasa();
            c.addGLEventListener(app);
            c.addMouseListener(app);  c.addMouseMotionListener(app);
            c.addMouseWheelListener(app);  c.addKeyListener(app);

            JFrame f = new JFrame("Plano casa 5.85 m × 7.35 m");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.add(c);  f.setSize(1280, 800);  f.setLocationRelativeTo(null);
            f.setVisible(true);
            new FPSAnimator(c, 60).start();  c.requestFocusInWindow();
        });
    }

    /* ================ INIT ================ */
    @Override public void init(GLAutoDrawable d) {
        GL2 gl = d.getGL().getGL2();
        gl.glClearColor(.94f, .94f, .94f, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glShadeModel(GL2.GL_SMOOTH);
    }
    @Override public void dispose(GLAutoDrawable d) {}

    /* =============== RESHAPE =============== */
    @Override public void reshape(GLAutoDrawable d,int x,int y,int w,int h){
        GL2 gl = d.getGL().getGL2();
        if(h==0) h=1;
        gl.glMatrixMode(GL2.GL_PROJECTION);  gl.glLoadIdentity();
        glu.gluPerspective(45,(float)w/h, .1, 1000);
        gl.glMatrixMode(GL2.GL_MODELVIEW);   gl.glLoadIdentity();
    }

    /* =============== DISPLAY =============== */
    @Override public void display(GLAutoDrawable d){
        GL2 gl = d.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        gl.glTranslatef(0,0,-camDist);
        gl.glRotatef(camRotX,1,0,0); gl.glRotatef(camRotY,0,1,0);
        gl.glTranslatef(-camPosX,-camPosY,-camPosZ);

        drawFloor(gl);
        drawWalls(gl);
    }

    /* -------- piso (una loseta grande) -------- */
    private void drawFloor(GL2 gl){
        float w=W+T*2, d=D+T*2;        // pequeño margen
        gl.glColor3f(.85f,.85f,.85f);
        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0,1,0);
        gl.glVertex3f(-w/2, -0.01f, -d/2);
        gl.glVertex3f(-w/2, -0.01f,  d/2);
        gl.glVertex3f( w/2, -0.01f,  d/2);
        gl.glVertex3f( w/2, -0.01f, -d/2);
        gl.glEnd();
    }

    /* -------- utilidades -------- */
    private void box(GL2 gl,float cx,float cy,float cz,
                     float sx,float sy,float sz){
        gl.glPushMatrix();
        gl.glTranslatef(cx,cy,cz);
        gl.glScalef(sx,sy,sz);
        glut.glutSolidCube(1f);
        gl.glPopMatrix();
    }
    /* ============ MUROS ============ */
    private void drawWalls(GL2 gl){
        gl.glColor3f(.95f,.95f,.95f);          // gris claro
        float h = H, t = T;

        /* desplazamos el plano para que el centro quede en (0,0) */
        float ox = -W/2, oz = -D/2;

        /* ——— muros perimetrales ——— */
        // oeste
        box(gl,  ox+t/2, h/2, oz+D/2,   t,h,D);
        // este
        box(gl,  ox+W-t/2, h/2, oz+D/2, t,h,D);
        // norte
        box(gl,  ox+W/2, h/2, oz+D-t/2, W,h,t);
        // sur (izq) --- 0-3 m
        box(gl,  ox+1.5f, h/2, oz+t/2,  3.0f,h,t);
        // frente corredor (x = 3-5.85, z = 1.50)
        box(gl,  ox+4.425f, h/2, oz+1.50f+t/2, 2.85f,h,t);
        // pared izquierda del corredor (x=3 m, z 0-1.50)
        box(gl,  ox+3.0f+t/2, h/2, oz+0.75f,   t,h,1.50f);

        /* ——— muro divisor largo (dormitorios vs cocina/sala) ——— */
        // arranca en z=1.50 y termina en el fondo
        box(gl,  ox+3.0f+t/2, h/2, oz+1.50f+(D-1.50f)/2,
                t,h, D-1.50f);

        /* ——— muros horizontales internos (según cotas) ——— */
        // entre dormitorio 1 y baño → z = D-2.85 m
        box(gl,  ox+1.50f, h/2, oz+D-2.85f-t/2, 3.0f,h,t);
        // entre baño y dormitorio 2 → z = D-2.85-1.35 m
        box(gl,  ox+1.50f, h/2, oz+D-2.85f-1.35f-t/2, 3.0f,h,t);
    }

    /* =============– RATÓN –============= */
    @Override public void mousePressed(MouseEvent e){ drag=true;
        lastX=e.getX(); lastY=e.getY();}
    @Override public void mouseReleased(MouseEvent e){drag=false;}
    @Override public void mouseDragged(MouseEvent e){
        if(!drag) return;
        camRotY += (e.getX()-lastX)*.5f;
        camRotX += (e.getY()-lastY)*.5f;
        lastX=e.getX(); lastY=e.getY(); }
    @Override public void mouseWheelMoved(MouseWheelEvent e){
        camDist += e.getWheelRotation();
        camDist = Math.max(6, Math.min(camDist, 40)); }
    @Override public void mouseClicked(MouseEvent e){}
    @Override public void mouseEntered(MouseEvent e){}
    @Override public void mouseExited(MouseEvent e){}
    @Override public void mouseMoved(MouseEvent e){}

    /* =============– TECLAS –============= */
    @Override public void keyPressed(KeyEvent e){
        switch(e.getKeyCode()){
            case KeyEvent.VK_W: {float r=(float)Math.toRadians(camRotY);
                camPosX+=Math.sin(r)*move; camPosZ+=-Math.cos(r)*move; break;}
            case KeyEvent.VK_S: {float r=(float)Math.toRadians(camRotY);
                camPosX-=Math.sin(r)*move; camPosZ-=-Math.cos(r)*move; break;}
            case KeyEvent.VK_A: {float r=(float)Math.toRadians(camRotY-90);
                camPosX+=Math.sin(r)*move; camPosZ+=-Math.cos(r)*move; break;}
            case KeyEvent.VK_D: {float r=(float)Math.toRadians(camRotY+90);
                camPosX+=Math.sin(r)*move; camPosZ+=-Math.cos(r)*move; break;}
            case KeyEvent.VK_Q: camPosY+=move; break;  // subir / bajar
            case KeyEvent.VK_E: camPosY-=move; break;
        }}
    @Override public void keyReleased(KeyEvent e){}
    @Override public void keyTyped(KeyEvent e){}
}
