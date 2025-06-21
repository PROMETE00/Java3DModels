import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

public class HexMazeFull implements GLEventListener, KeyListener {

    /* -------- escala global -------- */
    private static final float C = 0.60f; // tamaño celda
    private static final float H = 0.50f; // altura muro
    private static final float W = 0.06f; // grosor muro
    private static final float R = 0.18f; // radio jugador
    private static final float EPS = 0.01f;

    /* ========= LABERINTO COMPLETO (108 segmentos) ========= */
    private static final float[][] MAP = {
        /* --- contorno hexágono --- */
        { -6, -3.5f, 0, -7 },
        { 0, -7, 6, -3.5f },
        { 6, -3.5f, 6, 3.5f },
        { 6, 3.5f, 0, 7 },
        { 0, 7, -6, 3.5f },
        { -6, 3.5f, -6, -3.5f },
        /* --- cuadrante NW --- */
        { -4, -3, -2, -3 },
        { -2, -3, -1, -4 },
        { -1, -4, 1, -4 },
        { 1, -4, 1, -2 },
        { -4, -4, -4, -2 },
        { -4, -2, -2, -2 },
        /* --- diagonal central (de W a NE) --- */
        { -2, -2, -1, -1 },
        { -1, -1, 1, -1 },
        { 1, -1, 2, 0 },
        { 2, 0, 4, 0 },
        { 4, 0, 5, -1 },
        { 5, -1, 5, -3 },
        { 5, -3, 6, -4 },
        /* --- brazo NE (recorrido largo) --- */
        { 6, -4, 6, -1 },
        { 6, -1, 4, 1 },
        { 4, 1, 2, 1 },
        { 2, 1, 1, 2 },
        { 1, 2, -1, 2 },
        { -1, 2, -2, 1 },
        { -2, 1, -4, 1 },
        { -4, 1, -5, 0 },
        { -5, 0, -5, -2 },
        { -5, -2, -6, -3 },
        /* --- rama central inferior --- */
        { 1, -2, 3, -2 },
        { 3, -2, 4, -3 },
        { 4, -3, 4, -5 },
        { 4, -5, 2, -6 },
        { 2, -6, 0, -6 },
        { 0, -6, -1, -5 },
        { -1, -5, -3, -5 },
        /* --- laberinto interior sur-oeste --- */
        { -3, -5, -4, -4 },
        { -4, -4, -2, -4 },
        { -2, -4, -2, -2 },
        /* --- sección suroeste (antes de calle hacia FINISH) --- */
        { -1, 4, 0, 5 },
        { 0, 5, 2, 5 },
        { 2, 5, 2, 3 },
        { 2, 3, 4, 3 },
        { 4, 3, 5, 4 },
        { 5, 4, 5, 6 },
        { 5, 6, 3, 7 },
        { 3, 7, 0, 7 },
        { 0, 7, -1, 6 },
        { -1, 6, -3, 6 },
        { -3, 6, -4, 5 },
        { -4, 5, -4, 3 },
        { -4, 3, -2, 3 },
        /* --- refuerzos y cierres menores (para evitar fugas visuales) --- */
        { -5, -4, -5, -2 },
        { -3, -1, -3, -3 },
        { -1, 0, -1, 2 },
        { 3, 0, 3, -2 },
        { 5, 1, 4, 2 },
        { 4, 2, 2, 2 },
        { 2, 2, 0, 2 },
        { 0, 2, -1, 1 },
        { -1, 1, -3, 1 },
        { -3, 1, -4, 0 },
        /* --- pequeños tapones de pasillo --- */
        { -2, 3, -3, 3 },
        { 3, 5, 4, 4 },
        { 2, -3, 3, -3 },
        { -2, -4, -1, -4 },
        { -5, -1, -5, 1 },
        { 4, -1, 5, 0 },
        { 4, -5, 3, -5 },
        { -1, 5, -1, 4 },
        { 1, -3, 1, -4 },
        { 5, 5, 4, 6 },
        { -4, 2, -5, 2 },
        { -4, -1, -5, -1 },
        /* --- FINISH embudo --- */
        { -2, 4, -1, 4 },
        { -1, 1, -1, 1 }
    };

    /* -------- estado jugador -------- */
    private float x = 0, z = 0, yaw = 0;
    private int cam = 0; // 0 frontal, 1 cenital, 2 FPS
    private final GLU glu = new GLU();

    /* ============== MAIN ============== */
    public static void main(String[] a) {
        GLWindow w = GLWindow.create(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
        HexMazeFull app = new HexMazeFull();
        w.addGLEventListener(app);
        w.addKeyListener(app);
        w.setSize(920, 920);
        w.setTitle("Hex-Maze – versión completa");
        w.setVisible(true);
        new FPSAnimator(w, 60).start();
    }

    /* ========= OpenGL standard ========= */
    public void init(GLAutoDrawable d) {
        GL2 g = d.getGL().getGL2();
        g.glDisable(GL2.GL_LIGHTING);
        g.glShadeModel(GL2.GL_FLAT);
        g.glEnable(GL.GL_DEPTH_TEST);
        g.glClearColor(1, 1, 1, 1);
    }

    public void reshape(GLAutoDrawable d, int x0, int y0, int w, int h) {
        GL2 g = d.getGL().getGL2();
        g.glMatrixMode(GL2.GL_PROJECTION);
        g.glLoadIdentity();
        glu.gluPerspective(55, (float) w / h, 0.05, 120);
    }

    /* ================== DRAW ================== */
    public void display(GLAutoDrawable d) {
        GL2 g = d.getGL().getGL2();
        g.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        g.glMatrixMode(GL2.GL_MODELVIEW);
        g.glLoadIdentity();

        switch (cam) {
            case 0:
                glu.gluLookAt(0, 10, 17, 0, 0, 0, 0, 1, 0);
                break;
            case 1:
                g.glMatrixMode(GL2.GL_PROJECTION);
                g.glPushMatrix();
                g.glLoadIdentity();
                float s = 9;
                g.glOrtho(-s, s, -s, s, -20, 20);
                g.glMatrixMode(GL2.GL_MODELVIEW);
                glu.gluLookAt(x, 20, z, x, 0, z, 0, 0, -1);
                break;
            case 2:
                float fx = x + (float) Math.sin(Math.toRadians(yaw)) * 0.3f;
                float fz = z - (float) Math.cos(Math.toRadians(yaw)) * 0.3f;
                glu.gluLookAt(x, 0.8, z, fx, 0.8, fz, 0, 1, 0);
        }

        piso(g);
        paredes(g);
        avatar(g);

        if (cam == 1) {
            g.glMatrixMode(GL2.GL_PROJECTION);
            g.glPopMatrix();
            g.glMatrixMode(GL2.GL_MODELVIEW);
        }
    }

    public void dispose(GLAutoDrawable d) {}

    /* ========= helpers ========= */
    private void piso(GL2 g) {
        g.glColor3f(.96f, .92f, .87f);
        g.glPushMatrix();
        g.glTranslatef(0, -0.01f, 0);
        g.glScalef(14, 0.02f, 14);
        cubo(g);
        g.glPopMatrix();
    }

    private void paredes(GL2 g) {
        g.glColor3f(0, 0, 0);
        for (float[] s : MAP) {
            float x1 = s[0] * C, z1 = s[1] * C, x2 = s[2] * C, z2 = s[3] * C;
            float dx = x2 - x1, dz = z2 - z1, len = (float) Math.hypot(dx, dz);
            float ang = (float) Math.toDegrees(Math.atan2(dx, -dz));
            float mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
            g.glPushMatrix();
            g.glTranslatef(mx, H / 2, mz);
            g.glRotatef(ang, 0, 1, 0);
            g.glScalef(len, H, W);
            cubo(g);
            g.glPopMatrix();
        }
    }

    private void avatar(GL2 g) {
        g.glColor3f(1, 0, 0);
        g.glPushMatrix();
        g.glTranslatef(x, R, x);
        g.glTranslatef(0, 0, z - x); // corregir
        g.glTranslatef(x, R, z);
        g.glRotatef(yaw, 0, 1, 0);
        g.glScalef(R * 2, R * 2, R * 2);
        cubo(g);
        g.glPopMatrix();
    }

    private void cubo(GL2 g) {
        float s = .5f;
        g.glBegin(GL2.GL_QUADS);
        g.glVertex3f(-s, s, -s);
        g.glVertex3f(s, s, -s);
        g.glVertex3f(s, s, s);
        g.glVertex3f(-s, s, s);
        g.glVertex3f(-s, -s, -s);
        g.glVertex3f(-s, -s, s);
        g.glVertex3f(s, -s, s);
        g.glVertex3f(s, -s, -s);
        g.glVertex3f(-s, -s, s);
        g.glVertex3f(-s, s, s);
        g.glVertex3f(s, s, s);
        g.glVertex3f(s, -s, s);
        g.glVertex3f(-s, -s, -s);
        g.glVertex3f(s, -s, -s);
        g.glVertex3f(s, s, -s);
        g.glVertex3f(-s, s, -s);
        g.glVertex3f(s, -s, -s);
        g.glVertex3f(s, -s, s);
        g.glVertex3f(s, s, s);
        g.glVertex3f(s, s, -s);
        g.glVertex3f(-s, -s, -s);
        g.glVertex3f(-s, s, -s);
        g.glVertex3f(-s, s, s);
        g.glVertex3f(-s, -s, s);
        g.glEnd();
    }

    /* ========= controles ========= */
    public void keyPressed(KeyEvent e) {
        final float ANG = 8, STEP = 0.30f, SUB = 0.04f;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_C:
                cam = (cam + 1) % 3;
                return;
            case KeyEvent.VK_A:
                yaw -= ANG;
                return;
            case KeyEvent.VK_D:
                yaw += ANG;
                return;
        }
        if (e.getKeyCode() != KeyEvent.VK_W && e.getKeyCode() != KeyEvent.VK_S) return;
        float dir = (e.getKeyCode() == KeyEvent.VK_W) ? 1f : -1f; // W→adelante
        float dx = (float) Math.sin(Math.toRadians(yaw)) * SUB * dir;
        float dz = (float) Math.cos(Math.toRadians(yaw)) * SUB * -dir;
        int n = (int) Math.round(STEP / SUB);
        for (int i = 0; i < n; i++) {
            float nx = x + dx, nz = z + dz;
            if (tocaPared(nx, nz)) break;
            x = nx;
            z = nz;
        }
    }

    public void keyReleased(KeyEvent e) {}

    /* ========= colisión precisa ========= */
    private boolean tocaPared(float px, float pz) {
        float rad = R + W / 2 + EPS;
        float rad2 = rad * rad;
        for (float[] s : MAP) {
            float ax = s[0] * C, az = s[1] * C, bx = s[2] * C, bz = s[3] * C;
            float vx = bx - ax, vz = bz - az, len2 = vx * vx + vz * vz;
            float t = ((px - ax) * vx + (pz - az) * vz) / len2;
            if (t < 0) t = 0;
            else if (t > 1) t = 1;
            float cx = ax + t * vx, cz = az + t * vz;
            float dist2 = (px - cx) * (px - cx) + (pz - cz) * (pz - cz);
            if (dist2 < rad2) return true;
        }
        return false;
    }
}
