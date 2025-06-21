import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.*;
import java.awt.event.*;
import javax.swing.*;

public class Casa3DJOGL
    implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    /* ---------- cámara orbital + libre ---------- */
    private Texture pisoTex;
    private Texture envTex;
    private float camDist = 25f; // zoom (rueda)
    private float camRotX = 35f; // pitch  (°)
    private float camRotY = -35f; // yaw    (°)

    private float camPosX = 0f, camPosY = 0f, camPosZ = 0f; // desplazamiento libre
    private final float moveSpeed = 0.5f;

    private int lastX, lastY;
    private boolean dragging = false;
    private boolean lucesOn = true; // F1 apaga / F2 enciende

    private final GLU glu = new GLU();
    private final GLUT glut = new GLUT();

    /* ===================== MAIN ===================== */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GLProfile profile = GLProfile.get(GLProfile.GL2);
            GLCapabilities caps = new GLCapabilities(profile);
            GLCanvas canvas = new GLCanvas(caps);
            Casa3DJOGL app = new Casa3DJOGL();

            canvas.addGLEventListener(app);
            canvas.addMouseListener(app);
            canvas.addMouseMotionListener(app);
            canvas.addMouseWheelListener(app);
            canvas.addKeyListener(app);
            canvas.setFocusable(true);

            JFrame f = new JFrame("Casa 3D");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.add(canvas);
            f.setSize(1280, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            new FPSAnimator(canvas, 60).start();
            canvas.requestFocusInWindow();
        });
    }

    /* ===================== INIT ===================== */
    @Override
    public void init(GLAutoDrawable d) {
        GL2 gl = d.getGL().getGL2();
        gl.glClearColor(0.95f, 0.95f, 0.95f, 1f);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glShadeModel(GL2.GL_SMOOTH);

        float[] lightPos = { 20f, 30f, 20f, 1f };
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);

        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        /* -------- carga de texturas (con try-catch) -------- */
        try {
            pisoTex = TextureIO.newTexture(getClass().getResourceAsStream("/madera.jpg"), false, "jpg");
            pisoTex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
            pisoTex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        } catch (Exception e) {
            System.err.println("⚠ No se encontró madera.jpg – se usará color gris.");
        }

        try {
            envTex = TextureIO.newTexture(getClass().getResourceAsStream("/pared.jpg"), false, "jpg");
            envTex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
            envTex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        } catch (Exception e) {
            System.err.println("⚠ No se encontró env.jpg – espejos sin reflejo.");
        }
    }

    @Override
    public void dispose(GLAutoDrawable d) {}

    /* ================= RESHAPE ================= */
    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        GL2 gl = d.getGL().getGL2();
        if (h == 0) h = 1;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45, (float) w / h, 0.1, 1000);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /* ================== DISPLAY ================== */
    @Override
    public void display(GLAutoDrawable d) {
        GL2 gl = d.getGL().getGL2();
        if (lucesOn) {
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_LIGHT0);
        } else {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_LIGHT0);
        }

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -camDist);
        gl.glRotatef(camRotX, 1, 0, 0);
        gl.glRotatef(camRotY, 0, 1, 0);
        gl.glTranslatef(-camPosX, -camPosY, -camPosZ);

        drawScene(gl);
    }

    /* ======================================================
        UTILIDADES: cubo() y box() (con soporte double + 0-255)
        ====================================================== */
    private void cubo(GL2 gl, float sx, float sy, float sz, float r, float g, float b) {
        gl.glColor3f(r, g, b);
        gl.glPushMatrix();
        gl.glScalef(sx, sy, sz);
        glut.glutSolidCube(1f);
        gl.glPopMatrix();
    }

    private void drawGlassPane(GL2 gl, double x, double y, double z, double sx, double sy) { // grosor ~0.02
        float[] amb = { 0.05f, 0.05f, 0.08f, 0.4f }; // 40 % de transparencia
        float[] diff = { 0.15f, 0.18f, 0.25f, 0.4f };
        float[] spec = { 0.9f, 0.9f, 0.9f, 0.4f };
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, amb, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diff, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, spec, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 96); // brillo alto

        gl.glPushMatrix();
        gl.glTranslatef((float) x, (float) y, (float) z);
        gl.glScalef((float) sx, (float) sy, 0.02f);
        glut.glutSolidCube(1f);
        gl.glPopMatrix();
    }

    private void cubo(GL2 gl, double sx, double sy, double sz, double r, double g, double b) {
        cubo(gl, (float) sx, (float) sy, (float) sz, (float) r, (float) g, (float) b);
    }

    private void box(GL2 gl, double x, double y, double z, double sx, double sy, double sz, float r, float g, float b) {
        gl.glPushMatrix();
        gl.glTranslatef((float) x, (float) y, (float) z);
        cubo(gl, sx, sy, sz, r, g, b);
        gl.glPopMatrix();
    }

    private void box(GL2 gl, double x, double y, double z, double sx, double sy, double sz, int r, int g, int b) {
        box(gl, x, y, z, sx, sy, sz, r / 255f, g / 255f, b / 255f);
    }

    /* ==================== ESCENA ==================== */
    private void drawScene(GL2 gl) {
        drawFloor(gl, 25, 25);
        drawMirrors(gl); // se omite si envTex==null
        drawPerimeterWalls(gl); // …el resto es tu código original
        drawInternalWalls(gl);
        drawKitchen(gl);
        drawDiningRoom(gl);
        drawLivingRoom(gl);
        drawTvSet(gl);
        drawMasterBedroom(gl);
        drawSingleBedroom(gl);
        drawBathroom(gl);
        drawGarden(gl);
    }

    private void drawMirrors(GL2 gl) {
        if (envTex == null) return; // ← archivo faltante → salimos sin dibujar
        gl.glEnable(GL2.GL_TEXTURE_2D);
        envTex.bind(gl);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
        gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_SPHERE_MAP);
        gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_SPHERE_MAP);
        gl.glEnable(GL2.GL_TEXTURE_GEN_S);
        gl.glEnable(GL2.GL_TEXTURE_GEN_T);

        float[] amb = { 0.05f, 0.05f, 0.05f, 1 }, diff = { 0.25f, 0.25f, 0.25f, 1 }, spec = { 0.9f, 0.9f, 0.9f, 1 };
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, amb, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diff, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, spec, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 128);

        drawMirror(gl, 5.0, 1.30, -6.60, 2.8, 2.0, 0.04); // principal
        drawMirror(gl, -3.55, 1.30, -6.60, 2.8, 2.0, 0.04); // principal

        drawMirror(gl, -8.85, 1.30, 5.70, 0.04, 2.0, 2.8); // principal

        drawMirror(gl, -5.85, 1.30, 1.60, 0.04, 0.9, 0.9); // principal chiquito

        //drawMirror(gl, -5.85, 1.30, 1.60, 0.04, 0.9, 0.9); // principal chiquito
        drawMirror(gl, 0.5, 1.30, -4.75, 0.9, 0.9, 0.04); // principal 2
        //drawMirror(gl, -6.2, 1.20, -1.80, 1.6, 1.8, 0.04); // individual

        gl.glDisable(GL2.GL_TEXTURE_GEN_S);
        gl.glDisable(GL2.GL_TEXTURE_GEN_T);
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private void drawMirror(GL2 gl, double x, double y, double z, double sx, double sy, double sz) {
        gl.glPushMatrix();
        gl.glTranslatef((float) x, (float) y, (float) z);
        gl.glScalef((float) sx, (float) sy, (float) sz);
        glut.glutSolidCube(1);
        gl.glPopMatrix();
    }

    private void glass(GL2 gl, double x, double y, double z, double sx, double sy, double sz) {
        float[] amb = { 0.05f, 0.05f, 0.08f, 0.35f };
        float[] diff = { 0.15f, 0.18f, 0.25f, 0.35f };
        float[] spec = { 0.9f, 0.9f, 0.9f, 0.35f };
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, amb, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diff, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, spec, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 96);

        gl.glPushMatrix();
        gl.glTranslatef((float) x, (float) y, (float) z);
        gl.glScalef((float) sx, (float) sy, (float) sz);
        glut.glutSolidCube(1);
        gl.glPopMatrix();
    }

    /* --------- PISO --------- */
    /* --------- PISO (con textura) --------- */
    /* --------- PISO (con textura orientada hacia arriba) --------- */
    private void drawFloor(GL2 gl, float w, float d) {
        if (pisoTex != null) {
            gl.glEnable(GL2.GL_TEXTURE_2D);
            pisoTex.bind(gl);
            gl.glColor3f(1, 1, 1);
        } else {
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glColor3f(0.8f, 0.8f, 0.8f); // gris claro
        }
        gl.glPushMatrix();
        gl.glTranslatef(-w / 2, -0.50f, -d / 2);
        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0, 1, 0);
        if (pisoTex != null) gl.glTexCoord2f(0, 0);
        gl.glVertex3f(0, 0, 0);
        if (pisoTex != null) gl.glTexCoord2f(0, 2);
        gl.glVertex3f(0, 0, d);
        if (pisoTex != null) gl.glTexCoord2f(2, 2);
        gl.glVertex3f(w, 0, d);
        if (pisoTex != null) gl.glTexCoord2f(2, 0);
        gl.glVertex3f(w, 0, 0);
        gl.glEnd();
        gl.glPopMatrix();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private void drawFloor(GL2 gl, double w, double d) {
        drawFloor(gl, (float) w, (float) d);
    }

    /* =====================================================
        ========== PAREDES PERIMETRALES (sin cambio) ==========
        ===================================================== */
    private void drawPerimeterWalls(GL2 gl) {
        /* izquierda */
        drawWall(gl, -9, 1, 6, 0.2, 3, 6);
        drawWall(gl, -7.6, 1, 3, 3, 3, 0.2);
        drawWall(gl, -6, 1, 1.6, 0.2, 3, 3);
        drawWall(gl, -6.6, 1, 0.2, 1, 3, 0.2);
        drawWall(gl, -7.0, 1, -1.9, 0.2, 3, 4);
        drawWall(gl, -6.6, 1, -3.8, 1, 3, 0.2);
        drawWall(gl, -6, 1, -5.2, 0.2, 3, 3);
        /* atrás */
        drawWall(gl, -3.6, 1, -6.7, 5, 3, 0.2);
        drawWall(gl, -1, 1, -5.8, 0.2, 3, 2);
        drawWall(gl, 0.6, 1, -4.9, 3, 3, 0.2);
        drawWall(gl, 2, 1, -5.8, 0.2, 3, 2);
        drawWall(gl, 5.1, 1, -6.7, 6, 3, 0.2);
        /* derecha */
        drawWall(gl, 8, 0, -3.2, 0.2, 1, 7);
        drawWall(gl, 8.2, 0, 1.6, 0.2, 1, 3);
        drawWall(gl, 9.3, -0.25, 3, 2, 0.5, 0.2);
        drawWall(gl, 10.2, -0.25, 6, 0.2, 0.5, 6.2);
        drawWall(gl, 9, -0.25, 9, 2.5, 0.5, 0.2);
        /* frente */
        drawWall(gl, -6, 0, 9, 6, 1, 0.2);
        drawWall(gl, -0.4, 0, 9, 2.5, 1, 0.2);
        drawWall(gl, -3.1, -0.25, 10, 0.2, 0.5, 2.2); //frente alejado 1
        drawWall(gl, -0.4, 0, 10, 0.2, 1, 2.2); //frente alejado 2
        drawWall(gl, -0.4, 0, 11, 5.5, 1, 0.2); //frente alejado 3
        drawWall(gl, 5.2, 0, 9, 6, 1, 0.2);
    }

    /* ========== PAREDES INTERNAS (sin cambio) ========== */
    private void drawInternalWalls(GL2 gl) {
        drawWall(gl, 5.3, 0, 3, 6, 1, 0.2); // pasillo TV
        drawWall(gl, -1, 0, -2.3, 0.2, 1, 5); // bloque baño
        drawWall(gl, 0.6, 0, -0.5, 3, 1, 0.2);
        drawWall(gl, 2, 0, -3.7, 0.2, 1, 4);
        drawWall(gl, -4.2, 0, 3, 4, 1, 0.2); // división recámara individual
        drawWall(gl, -3, 0, 2.3, 0.2, 1, 1.5);
        drawWall(gl, -4.2, 0, 0.2, 4, 1, 0.2);
        drawWall(gl, 7.3, 0, 3.85, 0.2, 1, 1.5); // jardineras
        drawWall(gl, 7.3, 0, 8.15, 0.2, 1, 1.5);
    }

    private void drawWall(GL2 gl, double x, double y, double z, double sx, double sy, double sz) {
        gl.glPushMatrix();
        gl.glTranslatef((float) x, (float) y, (float) z);
        cubo(gl, sx, sy, sz, 1f, 1f, 1f);
        gl.glPopMatrix();
    }

    private void drawGlassObjects(GL2 gl) {
        /* Ejemplo de ventana frontal */
        glass(gl, -19.8, 1.0, 9.01, 3.0, 1.5, 0.04);
        /* Ejemplo de espejo interior en recámara ppal */
        glass(gl, 7.2, 1.25, -2.45, 0.8, 2.5, 0.03);
        /* Agrega aquí más glass(gl, x,y,z,sx,sy,sz);  */
    }

    /* =================== MUEBLES =================== */
    private void drawKitchen(GL2 gl) {
        box(gl, -7.9, 0, 8.35, 2.0, 1.0, 1.0, 192, 166, 141); //c1
        box(gl, -8.4, 0, 5.8, 1.0, 1.0, 5.0, 192, 166, 141); //c2
        box(gl, -7.9, 0, 3.6, 2.0, 1.0, 1.0, 192, 166, 141); //c3
        box(gl, -7.9, 0.5, 8.3, 2, 0.1, 1, 0.10f, 0.10f, 0.10f); // c4
        box(gl, -8.4, 0.5, 5.5, 1, 0.1, 4.8, 0.10f, 0.10f, 0.10f); // c5
        box(gl, -7.9, 0.5, 3.55, 2, 0.1, 1, 0.10f, 0.10f, 0.10f); // c6
        box(gl, -3.5, 0.7, 8.3, 1.2, 2.4, 1.2, 0.10f, 0.10f, 0.10f); // refrigerador
        box(gl, -3.9, 1.4, 7.7, 0.1, 0.5, 0.1, 255, 255, 255); //palanca 1
        box(gl, -3.9, 0.7, 7.7, 0.1, 0.5, 0.1, 255, 255, 255); //palanca 2

        box(gl, -3.1, 0.27, 10, 0.1, 0.05, 2, 0.10f, 0.10f, 0.10f); //frente alejado 1 reja
        box(gl, -3.1, 0.17, 10, 0.1, 0.05, 2, 0.10f, 0.10f, 0.10f); //frente alejado 2 reja
        box(gl, -3.1, 0.07, 10, 0.1, 0.05, 2, 0.10f, 0.10f, 0.10f); //frente alejado 2 reja

        box(gl, 2.3, 1, 10.0, 0.2, 3.0, 1.8, 128, 82, 45); //puerta
        box(gl, 2.4, 0.8, 9.3, 0.1, 0.3, 0.1, 255, 255, 255); //palanca 3
    }

    /* ==================== COMEDOR ==================== */
    /* ==================== COMEDOR – ajustado al suelo y al hueco ==================== */
    private void drawDiningRoom(GL2 gl) {
        /* --- traslación global: coloca y “apoya” el conjunto --- */
        final float groundY = -0.50f; // cota superior del piso (ya lo usas)
        gl.glPushMatrix();
        gl.glTranslatef(-2.30f, groundY, 5.20f); // <-- ajusta X-Z aquí si quieres afinar

        /* ---------- mesa ---------- */
        double tW = 3.0, tD = 1.5, tT = 0.08; // ancho, fondo, grosor tablero
        double boardTop = 0.72; // 72 cm sobre el suelo
        double boardCtrY = boardTop - tT / 2; // centro del tablero
        int woodR = 115, woodG = 60, woodB = 20;

        // tablero
        box(gl, 0, boardCtrY, 0, tW, tT, tD, woodR, woodG, woodB);

        // patas (4)
        double legS = 0.12;
        double legH = boardCtrY - tT / 2; // llega justo al suelo local (y=0)
        double lx = tW / 2 - legS / 2, lz = tD / 2 - legS / 2;
        box(gl, -lx, legH / 2, -lz, legS, legH, legS, woodR, woodG, woodB);
        box(gl, -lx, legH / 2, lz, legS, legH, legS, woodR, woodG, woodB);
        box(gl, lx, legH / 2, -lz, legS, legH, legS, woodR, woodG, woodB);
        box(gl, lx, legH / 2, lz, legS, legH, legS, woodR, woodG, woodB);

        /* ---------- sillas (6) ---------- */
        double seatTop = 0.45; // asiento a 45 cm del suelo
        // posiciones relativas a la mesa
        double dx = 1.45, dz = 1.05, dzMid = 1.45; // separación cómoda
        drawChair(gl, -dx, -dz, 90, seatTop); // izquierda-abajo
        drawChair(gl, -dx, dz, 90, seatTop); // izquierda-arriba
        drawChair(gl, dx, -dz, -90, seatTop); // derecha-abajo
        drawChair(gl, dx, dz, -90, seatTop); // derecha-arriba
        drawChair(gl, 0, -dzMid, 180, seatTop); // centro-abajo
        drawChair(gl, 0, dzMid, 0, seatTop); // centro-arriba

        /* ---------- platos ---------- */
        double py = boardTop + 0.02; // apenas sobre el tablero
        drawPlate(gl, -1.0, py, -0.50);
        drawPlate(gl, -1.0, py, 0.50);
        drawPlate(gl, 1.0, py, -0.50);
        drawPlate(gl, 1.0, py, 0.50);
        drawPlate(gl, 0.0, py, -0.65);
        drawPlate(gl, 0.0, py, 0.65);

        gl.glPopMatrix(); // fin traslación global
    }

    /* ======= silla con altura de asiento parametrizable ======= */
    private void drawChair(GL2 gl, double offX, double offZ, float rotY, double seatTop) {
        int r = 140, g = 72, b = 26; // madera
        double seatT = 0.05, seatW = 0.60;
        double legS = 0.06;
        double legH = seatTop; // llega al suelo local
        double backH = 0.50, backT = 0.05;

        gl.glPushMatrix();
        gl.glTranslatef((float) offX, 0f, (float) offZ); //  y=0 es el suelo local
        gl.glRotatef(rotY, 0f, 1f, 0f);

        /* patas */
        double ldx = seatW / 2 - legS / 2, ldz = seatW / 2 - legS / 2;
        box(gl, -ldx, legH / 2, -ldz, legS, legH, legS, r, g, b);
        box(gl, -ldx, legH / 2, ldz, legS, legH, legS, r, g, b);
        box(gl, ldx, legH / 2, -ldz, legS, legH, legS, r, g, b);
        box(gl, ldx, legH / 2, ldz, legS, legH, legS, r, g, b);

        /* asiento */
        double seatCtrY = seatTop - seatT / 2;
        box(gl, 0, seatCtrY, 0, seatW, seatT, seatW, r, g, b);

        /* respaldo */
        double backCtrY = seatTop + backH / 2;
        box(gl, 0, backCtrY, -seatW / 2 + backT / 2, seatW, backH, backT, r, g, b);

        gl.glPopMatrix();
    }

    /* ======= plato ======= */
    private void drawPlate(GL2 gl, double x, double y, double z) {
        gl.glPushMatrix();
        gl.glTranslatef((float) x, (float) y, (float) z);
        gl.glRotatef(-90, 1f, 0f, 0f);
        gl.glColor3f(0.95f, 0.95f, 0.95f);
        glut.glutSolidTorus(0.02, 0.12, 12, 24);
        gl.glPopMatrix();
    }

    private void drawLivingRoom(GL2 gl) {
        box(gl, 5.1, -0.25, 8.25, 4.2, 0.5, 1.2, 0.235f, 0.204f, 0.173f); // sofá p1
        box(gl, 6.6, -0.25, 7.25, 1.2, 0.5, 2.2, 0.235f, 0.204f, 0.173f); // sofá p2
        box(gl, 4.6, 0.3, 8.8, 3.2, 0.5, 0.2, 0.235f, 0.204f, 0.173f); // sofá arriba 1
        box(gl, 6.1, 0.3, 8.1, 0.2, 0.5, 1.2, 0.235f, 0.204f, 0.173f); // sofá arriba 2
        box(gl, 6.6, 0.3, 7.5, 1.2, 0.5, 0.2, 0.235f, 0.204f, 0.173f); // sofá arriba 3
        box(gl, 7.1, 0.3, 6.8, 0.2, 0.5, 1.2, 0.235f, 0.204f, 0.173f); // sofá arriba 4
        box(gl, 4.8, 0, 6.7, 1.8, 0.7, 1.2, 128, 82, 45); //mesa 1
        box(gl, 4.8, 0.31, 6.7, 2.1, 0.02, 1.4, 166, 128, 95); //mesa 2
        box(gl, 4.6, 0.1, 8.6, 3.1, 0.15, 0.2, 255, 255, 255); //almohada
    }

    private void drawTvSet(GL2 gl) {
        box(gl, 4.8, -0.2, 3.4, 2.4, 0.5, 0.6, 240, 240, 240); //tablero tv
        box(gl, 4.8, 1.2, 3.22, 1.6, 1.2, 0.1, 0, 0, 0);
        box(gl, 4.8, 1.2, 3.2, 2.3, 1.5, 0.1, 240, 240, 240); //respaldo tv
        box(gl, 4.3, 0.1, 3.4, 0.8, 0.1, 0.5, 240, 240, 240); //reproductor dvd 1
        box(gl, 5.3, 0.1, 3.4, 0.8, 0.1, 0.5, 240, 240, 240); //reproductor dvd 2
    }

    private void drawMasterBedroom(GL2 gl) {
        box(gl, 5.0, 0, -3.9, 3.0, 0.8, 5.0, 153, 77, 64); //base cama 2
        box(gl, 5.0, 0.45, -3.9, 3.0, 0.2, 5.0, 255, 255, 255); //cobija 1
        box(gl, 4.3, 0.65, -5.5, 1.0, 0.2, 1.0, 229, 232, 187); //almohada 1
        box(gl, 5.6, 0.65, -5.5, 1.0, 0.2, 1.0, 229, 232, 187); //almohada 2
        box(gl, 5.0, 0.45, -3.3, 2.99, 0.22, 2.99, 25, 255, 255); //cobija 2
        box(gl, 5, 0.3, -6.5, 3, 1.6, 0.1, 204, 140, 90); //atras cama
        box(gl, 3.0, 0, -6.2, 0.8, 0.8, 0.8, 204, 140, 90); //cajon
        box(gl, 3.0, 0.19, -6.2, 0.8, 0.3, 0.82, 217, 204, 115); //cajon p2
        box(gl, 3.0, -0.19, -6.2, 0.8, 0.3, 0.82, 217, 204, 115); //cajon p3
        box(gl, 7.5, 0.8, -1.88, 0.8, 2.5, 9.4, 217, 204, 115); //mueble
    }

    private void drawSingleBedroom(GL2 gl) {
        box(gl, -3.8, -0.1, -1.8, 2, 0.4, 3.7, 153, 77, 64); //base cama
        box(gl, -3.8, 0.3, -1.8, 2, 0.4, 3.7, 255, 255, 255); //tapa cama
        box(gl, -3.8, 0.5, -0.7, 1.5, 0.4, 1, 255, 255, 255); //almohada cama
        box(gl, -3.8, 0.32, -2.4, 1.99, 0.4, 1.67, 25, 255, 255); //cobija cama
        box(gl, -6.5, 0.55, -1.8, 0.8, 2.1, 4.0, 217, 204, 115); //mueble
    }

    private void drawBathroom(GL2 gl) {
        box(gl, 0.4, -0.2, -4.5, 0.8, 0.5, 0.8, 220, 220, 220);
        box(gl, 0.4, 0.3, -4.7, 0.8, 0.7, 0.2, 230, 230, 230);
        box(gl, -0.8, 1, -3.0, 0.1, 3.0, 0.1, 190, 190, 190); //regadera 1
        box(gl, -0.4, 2.5, -3.0, 1, 0.1, 1, 190, 190, 190); // regadera 1.1
        box(gl, -4.3, 1, 2.8, 0.1, 3.0, 0.1, 190, 190, 190); //regadera 2
        box(gl, -4.3, 2.5, 2.4, 1, 0.1, 1, 190, 190, 190); // regadera 2.1
    }

    /* ===================== JARDÍN ===================== */
    private void drawGarden(GL2 gl) {
        /* macetero / jardinera */
        box(gl, 8.85, -0.30, 6.00, 2.80, 0.02, 6.00, 238, 234, 203);

        /* === ARBUSTOS (mismo código anterior) === */
        drawPlant(gl, 9.90, -0.30, 4.90);
        drawPlant(gl, 9.90, -0.30, 6.00);
        drawPlant(gl, 9.90, -0.30, 7.10);
        drawPlant(gl, 9.10, -0.30, 8.20);
        drawPlant(gl, 8.0, -0.30, 8.20);

        drawPlant(gl, 9.10, -0.30, 3.70);
        drawPlant(gl, 8.0, -0.30, 3.70);

        /* === ÁRBOLES INDIVIDUALES ===
           —- modifica la posición de cada uno editando su línea —
           ejemplo: drawTree(gl, x, y, z);  */
        drawTree(gl, 9.90, -0.30, 3.30); // Árbol #1
        drawTree(gl, 9.90, -0.30, 8.80); // Árbol #2
    }

    /* tronco + dos esferas grandes para la copa */
    private void drawTree(GL2 gl, double x, double y, double z) {
        /* tronco */
        box(gl, x, y + 0.60, z, 0.30, 1.20, 0.30, 101, 67, 33);

        /* copa */
        drawLeafSphere(gl, x, y + 1.80, z, 0.80); // base de la copa
        drawLeafSphere(gl, x, y + 2.40, z, 0.60); // parte superior
    }

    /* tronco + tres esferas de follaje, proporciones estilo “arbusto” */
    private void drawPlant(GL2 gl, double x, double y, double z) {
        /* tronco */
        box(gl, x, y + 0.25, z, 0.18, 0.50, 0.18, 101, 67, 33);

        /* follaje escalonado */
        drawLeafSphere(gl, x, y + 0.80, z, 0.50);
        drawLeafSphere(gl, x, y + 1.10, z, 0.40);
        drawLeafSphere(gl, x, y + 1.35, z, 0.30);
    }

    /* esfera de hojas (verde intenso) */
    private void drawLeafSphere(GL2 gl, double x, double y, double z, double r) {
        gl.glPushMatrix();
        gl.glTranslatef((float) x, (float) y, (float) z);
        gl.glColor3f(0.10f, 0.55f, 0.18f);
        glut.glutSolidSphere(r, 14, 14);
        gl.glPopMatrix();
    }

    /* ============= controles de ratón ============= */
    @Override
    public void mousePressed(MouseEvent e) {
        dragging = true;
        lastX = e.getX();
        lastY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) return;
        int dx = e.getX() - lastX, dy = e.getY() - lastY;
        camRotY += dx * 0.5f;
        camRotX += dy * 0.5f;
        lastX = e.getX();
        lastY = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        camDist += e.getWheelRotation();
        camDist = Math.max(8, Math.min(camDist, 60));
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}

    /* ================= TECLADO ================= */
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_F1:
                lucesOn = false;
                break; // luces OFF
            case KeyEvent.VK_F2:
                lucesOn = true;
                break; // luces ON
            /* desplazamiento libre (local a la cámara) */
            case KeyEvent.VK_W: { // adelante
                float rad = (float) Math.toRadians(camRotY);
                camPosX += Math.sin(rad) * moveSpeed;
                camPosZ += -Math.cos(rad) * moveSpeed;
                break;
            }
            case KeyEvent.VK_S: { // atrás
                float rad = (float) Math.toRadians(camRotY);
                camPosX -= Math.sin(rad) * moveSpeed;
                camPosZ -= -Math.cos(rad) * moveSpeed;
                break;
            }
            case KeyEvent.VK_A: { // izquierda
                float rad = (float) Math.toRadians(camRotY - 90);
                camPosX += Math.sin(rad) * moveSpeed;
                camPosZ += -Math.cos(rad) * moveSpeed;
                break;
            }
            case KeyEvent.VK_D: { // derecha
                float rad = (float) Math.toRadians(camRotY + 90);
                camPosX += Math.sin(rad) * moveSpeed;
                camPosZ += -Math.cos(rad) * moveSpeed;
                break;
            }
            case KeyEvent.VK_Q:
                camPosY += moveSpeed;
                break; // subir
            case KeyEvent.VK_E:
                camPosY -= moveSpeed;
                break; // bajar
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}
