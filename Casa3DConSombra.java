import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Casa3DConSombra implements GLEventListener,
        KeyListener, MouseListener, MouseMotionListener {

    private final GLU glu = new GLU();

    // ---------------------------
    // Parámetros de la cámara
    // ---------------------------
    private float cameraX = 0f;
    private float cameraY = 10f;  
    private float cameraZ = 40f;  // Cámara muy alejada
    private float rotX = -20f;    // Inclinación
    private float rotY = 0f;      
    private int lastX, lastY;
    private boolean[] keys = new boolean[256];

    // ----------------------------
    // Definición de Ventana/Pared
    // ----------------------------
    class Ventana {
        float x1, y1, x2, y2;
        public Ventana(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    class Pared {
        float translateX, translateY, translateZ;
        float rotationY; 
        float width, height;
        float thickness;
        List<Ventana> ventanas;

        public Pared(float tX, float tY, float tZ,
                     float rotY, float w, float h, float thick) {
            this.translateX = tX;
            this.translateY = tY;
            this.translateZ = tZ;
            this.rotationY = rotY;
            this.width = w;
            this.height = h;
            this.thickness = thick;
            this.ventanas = new ArrayList<>();
        }

        public void agregarVentana(float x1, float y1, float x2, float y2) {
            ventanas.add(new Ventana(x1, y1, x2, y2));
        }
    }

    // Lista de paredes de la casa
    private List<Pared> paredes = new ArrayList<>();

    // ----------------------------------
    // Parámetros de la luz “foco”
    // ----------------------------------
    // Luz principal (ambiental y difusa)
    private final float[] luz0_ambient = {0.3f, 0.3f, 0.3f, 1f};
    private final float[] luz0_diffuse = {0.6f, 0.6f, 0.6f, 1f};
    
    // Luz puntual extra, para crear sombra
    private final float[] luz1_ambient = {0.2f, 0.2f, 0.2f, 1f};
    private final float[] luz1_diffuse = {0.9f, 0.9f, 0.9f, 1f};
    
    // Posición de la luz para la sombra (w=1 => luz puntual)
    private final float[] focoPos = {-15f, 12f, 25f, 1f};

    // -------------------------------
    // main: ejecución de la ventana
    // -------------------------------
    public static void main(String[] args) {
        JFrame frame = new JFrame("Casa 3D con Sombras Planas - Base Pequeña");
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        GLCanvas canvas = new GLCanvas(caps);

        Casa3DConSombra casa = new Casa3DConSombra();
        canvas.addGLEventListener(casa);

        canvas.addKeyListener(casa);
        canvas.addMouseListener(casa);
        canvas.addMouseMotionListener(casa);

        frame.setSize(1280, 800);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas);
        frame.setVisible(true);

        FPSAnimator animator = new FPSAnimator(canvas, 60);
        animator.start();
    }

    // --------------------------
    // GLEventListener overrides
    // --------------------------
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Color de fondo: cielo
        gl.glClearColor(0.53f, 0.81f, 0.99f, 1f);
        gl.glEnable(GL.GL_DEPTH_TEST);

        gl.glShadeModel(GL2.GL_SMOOTH);

        // Activa Color Material y configura para que use color difuso+ambiente
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);

        // Luz 0 (ambiental y difusa)
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT,  luz0_ambient, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE,  luz0_diffuse, 0);

        // Luz 1 (foco puntual)
        gl.glEnable(GL2.GL_LIGHT1);
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT,  luz1_ambient, 0);
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE,  luz1_diffuse, 0);

        // -------------------------------
        // Definir la casa
        // -------------------------------
        float grosorPared = 0.6f; 
        float alto = 8.0f;
        float ancho = 8.0f;

        // Pared frontal
        Pared frontal = new Pared(
                0f, 0f, -ancho/2,
                0f,
                ancho, alto, grosorPared
        );
        // Varias ventanas
        frontal.agregarVentana(-2.5f, 0.5f, 1.0f, 3.5f);
        frontal.agregarVentana(1.5f, 2f, 2.4f, 5f);
        frontal.agregarVentana(-1f, 4.5f, 0.5f, 7f);

        // Pared trasera
        Pared trasera = new Pared(
                0f, 0f, ancho/2,
                180f,
                ancho, alto, grosorPared
        );
        trasera.agregarVentana(-2.0f, 3f, -0.5f, 5.5f);
        trasera.agregarVentana(0.5f, 1f, 2f, 3.5f);

        // Pared izquierda
        Pared izquierda = new Pared(
                -ancho/2, 0f, 0f,
                90f,
                ancho, alto, grosorPared
        );
        izquierda.agregarVentana(-2.5f, 1.5f, -1.2f, 5f);

        // Pared derecha
        Pared derecha = new Pared(
                ancho/2, 0f, 0f,
                -90f,
                ancho, alto, grosorPared
        );
        derecha.agregarVentana(-1.5f, 2f, 1.0f, 4.5f);

        paredes.add(frontal);
        paredes.add(trasera);
        paredes.add(izquierda);
        paredes.add(derecha);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Limpiar buffers
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Configurar cámara
        configurarCamara(gl);

        // Actualizar movimiento (WASD)
        actualizarMovimiento();

        // Posicionar la luz 1 en la escena
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, focoPos, 0);

        // 1) Dibujar base pequeña (para ver la sombra sobre ella)
        dibujarBasePequena(gl);

        // 2) Dibujar la casa normal con iluminación
        gl.glPushMatrix();
        {
            dibujarCasa(gl);
        }
        gl.glPopMatrix();

        // 3) Dibujar la sombra en la base (plano Y=0)
        dibujarSombraCasa(gl);
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int width, int height) {
        GL2 gl = d.getGL().getGL2();
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, (float) width / height, 0.1, 1000.0);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable d) {}

    // -------------------------------------
    // Configuración de cámara
    // -------------------------------------
    private void configurarCamara(GL2 gl) {
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Cámara muy alejada
        glu.gluLookAt(
                cameraX, cameraY, cameraZ,
                cameraX, cameraY, cameraZ - 1,
                0, 1, 0
        );

        // Rotación con mouse
        gl.glRotatef(rotX, 1, 0, 0);
        gl.glRotatef(rotY, 0, 1, 0);
    }

    // -------------------------------------------------------
    // 1) Dibujar la base rectangular (más grande)
    // -------------------------------------------------------
    private void dibujarBasePequena(GL2 gl) {
        // Normal hacia arriba (0,1,0)
        gl.glNormal3f(0f, 1f, 0f);
        gl.glColor3f(0.8f, 0.8f, 0.8f);
        gl.glBegin(GL2.GL_QUADS);
            // Base grande para ver la sombra extendida
            gl.glVertex3f(-70f, -1f, -70f);
            gl.glVertex3f( 70f, -1f, -70f);
            gl.glVertex3f( 70f, -1f,  70f);
            gl.glVertex3f(-70f, -1f,  70f);
        gl.glEnd();
    }

    // -------------------------------------------------------
    // 2) Dibujar la casa normal con sus paredes
    // -------------------------------------------------------
    private void dibujarCasa(GL2 gl) {
        for (Pared p : paredes) {
            gl.glPushMatrix();
            {
                // Trasladar y rotar cada pared
                gl.glTranslatef(p.translateX, p.translateY, p.translateZ);
                gl.glRotatef(p.rotationY, 0f, 1f, 0f);

                dibujarParedConHuecos(gl, p);
            }
            gl.glPopMatrix();
        }
    }

    /**
     * Dibuja una pared gruesa, “recortando” las ventanas para
     * ver el interior (hueco real).
     */
    private void dibujarParedConHuecos(GL2 gl, Pared pared) {
        float halfW = pared.width / 2f;
        float h = pared.height;
        float t = pared.thickness;

        // Cara externa (z=0), normal hacia +Z
        // CAMBIO DE COLOR DE LA CASA:
        gl.glColor3f(0.8f, 0.4f, 0.3f); 
        gl.glNormal3f(0f, 0f, 1f);
        dibujarCaraConVentanas(gl,
                -halfW, halfW, 0f, h,
                0f, 
                pared.ventanas
        );

        // Cara interna (z=-t), normal hacia -Z
        // Misma idea de color (o podrías variar ligeramente)
        gl.glColor3f(0.8f, 0.4f, 0.3f); 
        gl.glNormal3f(0f, 0f, -1f);
        dibujarCaraConVentanas(gl,
                -halfW, halfW, 0f, h,
                -t,
                pared.ventanas
        );

        // Bordes
        // Superior (normal hacia +Y)
        gl.glColor3f(0.8f, 0.4f, 0.3f);
        gl.glNormal3f(0f, 1f, 0f);
        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f(-halfW, h, 0f);
            gl.glVertex3f( halfW, h, 0f);
            gl.glVertex3f( halfW, h, -t);
            gl.glVertex3f(-halfW, h, -t);
        gl.glEnd();

        // Inferior (normal hacia -Y)
        gl.glColor3f(0.8f, 0.4f, 0.3f);
        gl.glNormal3f(0f, -1f, 0f);
        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f(-halfW, 0f, -t);
            gl.glVertex3f( halfW, 0f, -t);
            gl.glVertex3f( halfW, 0f,  0f);
            gl.glVertex3f(-halfW, 0f,  0f);
        gl.glEnd();

        // Izq (normal hacia -X)
        gl.glColor3f(0.8f, 0.4f, 0.3f);
        gl.glNormal3f(-1f, 0f, 0f);
        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f(-halfW, 0f, -t);
            gl.glVertex3f(-halfW, 0f,  0f);
            gl.glVertex3f(-halfW,   h,  0f);
            gl.glVertex3f(-halfW,   h, -t);
        gl.glEnd();

        // Der (normal hacia +X)
        gl.glColor3f(0.8f, 0.4f, 0.3f);
        gl.glNormal3f(1f, 0f, 0f);
        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f( halfW,   h, -t);
            gl.glVertex3f( halfW,   h,  0f);
            gl.glVertex3f( halfW, 0f,  0f);
            gl.glVertex3f( halfW, 0f, -t);
        gl.glEnd();
    }

    /**
     * Dibuja la cara (sin grosor) omitiendo ventanas.
     */
    private void dibujarCaraConVentanas(GL2 gl,
                                        float xMin, float xMax,
                                        float yMin, float yMax,
                                        float z,
                                        List<Ventana> ventanas) {

        // Ordenar ventanas de abajo a arriba
        ventanas.sort((v1, v2) -> Float.compare(v1.y1, v2.y1));

        float currentY = yMin;
        for (Ventana v : ventanas) {
            // Franja [currentY, v.y1]
            if (v.y1 > currentY) {
                dibujarRectangulo(gl, xMin, xMax, currentY, v.y1, z);
            }
            // En [v.y1, v.y2], parte izq y der
            if (v.y2 > v.y1) {
                if (v.x1 > xMin) {
                    dibujarRectangulo(gl, xMin, v.x1, v.y1, v.y2, z);
                }
                if (v.x2 < xMax) {
                    dibujarRectangulo(gl, v.x2, xMax, v.y1, v.y2, z);
                }
            }
            currentY = Math.max(currentY, v.y2);
        }
        // Franja final
        if (currentY < yMax) {
            dibujarRectangulo(gl, xMin, xMax, currentY, yMax, z);
        }
    }

    private void dibujarRectangulo(GL2 gl, float x1, float x2,
                                   float y1, float y2, float z) {
        if (x2 <= x1 || y2 <= y1) return;
        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f(x1, y1, z);
            gl.glVertex3f(x2, y1, z);
            gl.glVertex3f(x2, y2, z);
            gl.glVertex3f(x1, y2, z);
        gl.glEnd();
    }

    // -------------------------------------------------------
    // 3) Dibujar la sombra de la casa en el plano Y=0
    //    usando una matriz de proyección
    // -------------------------------------------------------
    private void dibujarSombraCasa(GL2 gl) {
        // 1) Guardar el estado actual
        gl.glPushMatrix();

        // 2) Desactivar iluminación para la sombra
        gl.glDisable(GL2.GL_LIGHTING);
        
        // Usar alpha blending para que sea una sombra semi-transparente
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        
        // Color negro-gris semi-transparente (más oscuro)
        gl.glColor4f(0.2f, 0.2f, 0.2f, 0.5f);

        // 3) Multiplicar por la matriz de proyección (plano Y=0) con la luz 'focoPos'
        float[] sombraMat = crearShadowMatrix(focoPos, new float[]{0f,1f,0f,0f});
        gl.glMultMatrixf(sombraMat, 0);

        // 4) Dibujar de nuevo la casa (exactamente la misma geometría)
        //    Se proyectará "aplastada" en Y=0.
        dibujarCasa(gl);

        // 5) Restaurar estado
        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopMatrix();
    }

    /**
     * Crea una matriz de proyección de sombras para el plano dado (plane)
     * usando la luz (lightPos).
     * - `plane` y `lightPos` son arreglos de 4 floats: (A,B,C,D) y (x,y,z,w).
     * - Aquí asumimos el plano Y=0 => (0,1,0,0) y la luz con w=1 (posicional).
     */
    private float[] crearShadowMatrix(float[] lightPos, float[] plane) {
        float[] mat = new float[16];
        // plane = (A,B,C,D)
        // lightPos = (lx, ly, lz, lw)
        float A = plane[0];
        float B = plane[1];
        float C = plane[2];
        float D = plane[3];
        float lx = lightPos[0];
        float ly = lightPos[1];
        float lz = lightPos[2];
        float lw = lightPos[3];

        float dot = A*lx + B*ly + C*lz + D*lw;

        // Rellenar la matriz (OpenGL usa column-major order)
        mat[0]  = dot - lx*A;   mat[4]  = -lx*B;       mat[8]  = -lx*C;       mat[12] = -lx*D;
        mat[1]  = -ly*A;        mat[5]  = dot - ly*B;  mat[9]  = -ly*C;       mat[13] = -ly*D;
        mat[2]  = -lz*A;        mat[6]  = -lz*B;       mat[10] = dot - lz*C;  mat[14] = -lz*D;
        mat[3]  = -lw*A;        mat[7]  = -lw*B;       mat[11] = -lw*C;       mat[15] = dot - lw*D;

        return mat;
    }

    // ----------------------------------
    // Movimiento y colisiones
    // ----------------------------------
    private void actualizarMovimiento() {
        float speed = 0.3f;
        if (keys[KeyEvent.VK_W]) cameraZ -= speed;
        if (keys[KeyEvent.VK_S]) cameraZ += speed;
        if (keys[KeyEvent.VK_A]) cameraX -= speed;
        if (keys[KeyEvent.VK_D]) cameraX += speed;

        verificarColisiones();
    }

    private void verificarColisiones() {
        // Limitar X y Z dentro de la casa
        float minX = -4.2f, maxX = 4.2f;
        float minZ = -4.2f, maxZ = 4.2f;
        float techo = 8.1f;

        if (cameraY < techo) {
            if (cameraX < minX) cameraX = minX;
            if (cameraX > maxX) cameraX = maxX;
            if (cameraZ < minZ) cameraZ = minZ;
            if (cameraZ > maxZ) cameraZ = maxZ;
        }
    }

    // --------------------------------------
    // Listeners de teclado y ratón
    // --------------------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    }
    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        lastX = e.getX();
        lastY = e.getY();
    }
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {
        int dx = e.getX() - lastX;
        int dy = e.getY() - lastY;
        rotY += dx * 0.5f;
        rotX += dy * 0.5f;
        lastX = e.getX();
        lastY = e.getY();
    }
}
