import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

public class EditorFiguras3D extends JFrame implements GLEventListener {
    private JComboBox<String> selectorForma, selectorColor, selectorTransform;
    
    private JPanel panelDegradado;
    private JPanel panelColor;
    private JPanel panelSesgado;
    private JPanel panelPerspectiva;
    private JPanel panelInferior; // Contendrá los paneles de "sesgado", "perspectiva" y uno vacío

    private JSlider sliderSesgoX, sliderSesgoY;
    private JSlider sliderFOV, sliderZoom;

    private GLJPanel glPanel;
    private FPSAnimator animator;
    private GLU glu = new GLU();

    // Variables de transformaciones
    private float rotationX = 0, rotationY = 0;
    private float translateX = 0, translateY = 0, translateZ = -5;
    private float scale = 1;
    private float shearX = 0.0f, shearY = 0.0f;
    private float fov = 45.0f;

    // Índice de figura actual
    private int currentShape = 0;

    // Manejo de color: valores y modelo
    private float[] colorValues = new float[4];  // se ajusta según sea RGB, CMYK, HSL o HSV

    // Manejo de degradado
    private float[] gradientStartColor = {1.0f, 0.0f, 0.0f};  // color inicio (rojo)
    private float[] gradientEndColor   = {0.0f, 0.0f, 1.0f};  // color fin (azul)

    public EditorFiguras3D() {
        configurarVentana();
        inicializarComponentes();
        configurarEventos();
    }

    private void configurarVentana() {
        setTitle("Editor de Figuras 3D (Sesgado, Perspectiva, Degradado y Color)");
        setSize(1080, 920);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(45, 45, 45));
        setLayout(new BorderLayout(10, 10));
    }

    private void inicializarComponentes() {
        // Panel OpenGL
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        glPanel = new GLJPanel(capabilities);
        glPanel.addGLEventListener(this);
        animator = new FPSAnimator(glPanel, 60);
        animator.start();

        // -----------------------------
        // Panel Izquierdo (Combos)
        // -----------------------------
        JPanel panelIzquierdo = new JPanel(new GridLayout(0, 1, 10, 10));
        panelIzquierdo.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 80, 80)),
                new EmptyBorder(15, 15, 15, 15)));
        panelIzquierdo.setBackground(new Color(60, 60, 60));

        String[] formas = {"Cubo", "Esfera", "Piramide", "Cilindro", "Cono"};
        selectorForma = crearComboBox(formas, new Color(70, 130, 180));

        String[] modelosColor = {"RGB", "CMYK", "HSL", "HSV"};
        selectorColor = crearComboBox(modelosColor, new Color(80, 160, 120));

        String[] transformaciones = {"Rotación", "Escala", "Traslación", "Sesgado", "Perspectiva", "Degradado"};
        selectorTransform = crearComboBox(transformaciones, new Color(160, 100, 200));

        panelIzquierdo.add(crearEtiqueta("Seleccionar Figura:"));
        panelIzquierdo.add(selectorForma);
        panelIzquierdo.add(crearEtiqueta("Modelo de Color:"));
        panelIzquierdo.add(selectorColor);
        panelIzquierdo.add(crearEtiqueta("Transformación:"));
        panelIzquierdo.add(selectorTransform);

        add(panelIzquierdo, BorderLayout.WEST);
        add(glPanel, BorderLayout.CENTER);

        // -----------------------------
        // Panel Derecho (Degradado y Color)
        // -----------------------------
        JPanel panelDerecho = new JPanel();
        panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));
        panelDerecho.setBorder(new EmptyBorder(15, 15, 15, 15));
        panelDerecho.setBackground(new Color(70, 70, 70));

        // --- Panel de Degradado ---
        panelDegradado = new JPanel(new GridLayout(3, 2, 5, 5));
        panelDegradado.setBorder(new TitledBorder("Controles de Degradado"));
        panelDegradado.setBackground(new Color(70, 70, 70));

        String[] colorLabels = {"Rojo", "Verde", "Azul"};
        JSlider[] startSliders = new JSlider[3];
        JSlider[] endSliders   = new JSlider[3];

        for (int i = 0; i < 3; i++) {
            startSliders[i] = crearSlider(0, 100, (int)(gradientStartColor[i] * 100), "Inicio " + colorLabels[i]);
            endSliders[i]   = crearSlider(0, 100, (int)(gradientEndColor[i]   * 100), "Fin " + colorLabels[i]);

            final int idx = i;
            startSliders[i].addChangeListener(e -> {
                gradientStartColor[idx] = startSliders[idx].getValue() / 100f;
                glPanel.repaint();
            });
            endSliders[i].addChangeListener(e -> {
                gradientEndColor[idx] = endSliders[idx].getValue() / 100f;
                glPanel.repaint();
            });

            panelDegradado.add(startSliders[i]);
            panelDegradado.add(endSliders[i]);
        }

        // Envolvemos en JScrollPane para que tenga scroll
        JScrollPane scrollDegradado = new JScrollPane(panelDegradado,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollDegradado.setPreferredSize(new Dimension(300, 200));

        // --- Panel de Color ---
        panelColor = new JPanel();
        panelColor.setLayout(new GridLayout(0, 1, 5, 5));
        panelColor.setBorder(new TitledBorder("Controles de Color"));
        panelColor.setBackground(new Color(70, 70, 70));

        // Para que también tenga scroll
        JScrollPane scrollColor = new JScrollPane(panelColor,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollColor.setPreferredSize(new Dimension(300, 200));

        // Agregamos ambos scrolls al panelDerecho
        panelDerecho.add(scrollDegradado);
        panelDerecho.add(Box.createVerticalStrut(10)); // pequeña separación
        panelDerecho.add(scrollColor);

        // Por defecto, ocultamos el degradado hasta que se seleccione "Degradado" en combobox
        panelDegradado.setVisible(false);

        add(panelDerecho, BorderLayout.EAST);

        // -----------------------------
        // Panel Inferior (Sesgado, Perspectiva, etc.)
        // -----------------------------
        panelInferior = new JPanel(new CardLayout());
        panelInferior.setBackground(new Color(70, 70, 70));

        // Panel vacío (default)
        JPanel panelVacio = new JPanel();
        panelVacio.setBackground(new Color(70, 70, 70));
        panelInferior.add(panelVacio, "vacio");

        // Panel Sesgado
        panelSesgado = new JPanel(new GridLayout(1, 2, 10, 10));
        panelSesgado.setBackground(new Color(70, 70, 70));
        sliderSesgoX = crearSlider(-100, 100, 0, "Sesgo X");
        sliderSesgoY = crearSlider(-100, 100, 0, "Sesgo Y");
        panelSesgado.add(sliderSesgoX);
        panelSesgado.add(sliderSesgoY);
        panelInferior.add(panelSesgado, "sesgado");

        // Panel Perspectiva
        panelPerspectiva = new JPanel(new GridLayout(1, 2, 10, 10));
        panelPerspectiva.setBackground(new Color(70, 70, 70));
        sliderFOV = crearSlider(30, 120, (int)fov, "Campo de Visión (FOV)");
        sliderZoom = crearSlider(1, 20, 5, "Zoom (Z Translate)");
        panelPerspectiva.add(sliderFOV);
        panelPerspectiva.add(sliderZoom);
        panelInferior.add(panelPerspectiva, "perspectiva");

        add(panelInferior, BorderLayout.SOUTH);

        // Cargamos por primera vez los sliders de color según el modelo inicial (RGB)
        actualizarControlesColor();
    }

    private void configurarEventos() {
        // --- Seleccionar Figura ---
        selectorForma.addActionListener(e -> {
            currentShape = selectorForma.getSelectedIndex();
            glPanel.repaint();
        });

        // --- Seleccionar Modelo de Color ---
        selectorColor.addActionListener(e -> {
            actualizarControlesColor();
            glPanel.repaint();
        });

        // --- Seleccionar Transformación ---
        selectorTransform.addActionListener(e -> {
            int selected = selectorTransform.getSelectedIndex();

            // Mostramos/ocultamos panelDegradado
            boolean isGradient = (selected == 5);
            panelDegradado.setVisible(isGradient);

            // Mostramos en el panelInferior la opción que corresponda (sesgado/perspectiva/vacio)
            CardLayout cl = (CardLayout)(panelInferior.getLayout());
            switch(selected) {
                case 3 -> cl.show(panelInferior, "sesgado");     // Sesgado
                case 4 -> cl.show(panelInferior, "perspectiva"); // Perspectiva
                default -> cl.show(panelInferior, "vacio");
            }
            glPanel.repaint();
        });

        // --- Sliders Sesgado ---
        sliderSesgoX.addChangeListener(e -> {
            shearX = sliderSesgoX.getValue() / 100.0f;
            glPanel.repaint();
        });
        sliderSesgoY.addChangeListener(e -> {
            shearY = sliderSesgoY.getValue() / 100.0f;
            glPanel.repaint();
        });

        // --- Sliders Perspectiva ---
        sliderFOV.addChangeListener(e -> {
            fov = sliderFOV.getValue();
            glPanel.repaint();
        });
        sliderZoom.addChangeListener(e -> {
            translateZ = -sliderZoom.getValue();
            glPanel.repaint();
        });

        // --- Eventos de Ratón (Rotar/Escalar/Trasladar) ---
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private Point lastPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int transformIndex = selectorTransform.getSelectedIndex();
                // Si estamos en sesgado(3), perspectiva(4) o degradado(5) NO hacemos nada con el ratón
                if (transformIndex == 3 || transformIndex == 4 || transformIndex == 5) return;

                int dx = e.getX() - lastPoint.x;
                int dy = e.getY() - lastPoint.y;

                switch (transformIndex) {
                    case 0: // Rotación
                        rotationX += dy * 0.5f;
                        rotationY += dx * 0.5f;
                        break;
                    case 1: // Escala
                        scale += dy * 0.01f;
                        break;
                    case 2: // Traslación
                        translateX += dx * 0.01f;
                        translateY -= dy * 0.01f;
                        break;
                }
                lastPoint = e.getPoint();
                glPanel.repaint();
            }
        };
        glPanel.addMouseListener(mouseAdapter);
        glPanel.addMouseMotionListener(mouseAdapter);
    }

    // ------------------------------------------
    // ACTUALIZAR PANELES DE COLOR
    // ------------------------------------------
    private void actualizarControlesColor() {
        // Eliminamos sliders anteriores
        panelColor.removeAll();

        // Definimos etiquetas según modelo
        String modelo = (String) selectorColor.getSelectedItem();
        String[] etiquetas = obtenerEtiquetasColor(modelo);
        // Aseguramos que colorValues tenga el tamaño adecuado (3 o 4 componentes)
        colorValues = new float[etiquetas.length];

        // Creamos un slider por cada componente de color
        for (int i = 0; i < etiquetas.length; i++) {
            JSlider slider = crearSlider(0, 100, 0, etiquetas[i]);
            final int index = i;

            // Cada slider ajusta la componente de 0 a 1
            slider.addChangeListener(e -> {
                colorValues[index] = slider.getValue() / 100f;
                glPanel.repaint();
            });

            // Lo agregamos al panel
            panelColor.add(slider);
        }

        // Repintamos
        panelColor.revalidate();
        panelColor.repaint();
    }

    private String[] obtenerEtiquetasColor(String modelo) {
        return switch (modelo) {
            case "RGB"  -> new String[]{"Rojo", "Verde", "Azul"};
            case "CMYK" -> new String[]{"Cian", "Magenta", "Amarillo", "Negro"};
            case "HSL"  -> new String[]{"Tono", "Saturación", "Luminosidad"};
            case "HSV"  -> new String[]{"Tono", "Saturación", "Valor"};
            default     -> new String[0];
        };
    }

    // ------------------------------------------
    // Métodos OpenGL
    // ------------------------------------------
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glLoadIdentity();
        glu.gluPerspective(fov, (float) glPanel.getWidth() / glPanel.getHeight(), 0.1f, 100.0f);

        // Cámara
        glu.gluLookAt(0, 2, 5,   // posición de la cámara
                      0, 1, 0,   // punto al que se mira
                      0, 1, 0);  // vector "up"

        // Dibujar grid base
        drawGrid(gl);

        // Aplicar transformaciones
        gl.glTranslatef(translateX, translateY, translateZ);
        gl.glRotatef(rotationX, 1, 0, 0);
        gl.glRotatef(rotationY, 0, 1, 0);
        gl.glScalef(scale, scale, scale);

        // Sesgado (solo si transform == 3)
        if (selectorTransform.getSelectedIndex() == 3) {
            float anchorX = -1.0f;
            float anchorY = -1.0f;
            float anchorZ = -1.0f;
            gl.glTranslatef(anchorX, anchorY, anchorZ);

            float[] shearMatrix = {
                1.0f,   shearY, 0.0f,   0.0f,
                shearX, 1.0f,   0.0f,   0.0f,
                0.0f,   0.0f,   1.0f,   0.0f,
                0.0f,   0.0f,   0.0f,   1.0f
            };
            gl.glMultMatrixf(shearMatrix, 0);
            gl.glTranslatef(-anchorX, -anchorY, -anchorZ);
        }

        // Si la transformación elegida es "Degradado", dibujamos con degradado
        if (selectorTransform.getSelectedIndex() == 5) {
            drawWithGradient(gl);
        } else {
            // Color sólido según el modelo de color
            Color colorSolido = convertColor(colorValues, selectorColor.getSelectedIndex());
            gl.glColor3f(colorSolido.getRed()/255f, colorSolido.getGreen()/255f, colorSolido.getBlue()/255f);

            switch (currentShape) {
                case 0 -> drawCube(gl);
                case 1 -> drawSphere(gl);
                case 2 -> drawPyramid(gl);
                case 3 -> drawCylinder(gl);
                case 4 -> drawCone(gl);
            }
        }
    }

    private void drawWithGradient(GL2 gl) {
        switch (currentShape) {
            case 0 -> drawGradientCube(gl);
            case 1 -> drawGradientSphere(gl);
            case 2 -> drawGradientPyramid(gl);
            case 3 -> drawGradientCylinder(gl);
            case 4 -> drawGradientCone(gl);
        }
    }

    // -----------------------------
    // Figuras con Degradado
    // -----------------------------
    private void drawGradientCube(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);

        float[][][] faces = {
            { {-1, -1,  1}, {-1,  1,  1}, { 1,  1,  1}, { 1, -1,  1} }, // frontal
            { { 1, -1, -1}, { 1,  1, -1}, {-1,  1, -1}, {-1, -1, -1} }, // trasera
            { {-1, -1, -1}, {-1,  1, -1}, {-1,  1,  1}, {-1, -1,  1} }, // izquierda
            { { 1, -1,  1}, { 1,  1,  1}, { 1,  1, -1}, { 1, -1, -1} }, // derecha
            { {-1,  1,  1}, {-1,  1, -1}, { 1,  1, -1}, { 1,  1,  1} }, // arriba
            { {-1, -1, -1}, {-1, -1,  1}, { 1, -1,  1}, { 1, -1, -1} }, // abajo
        };

        for (float[][] face : faces) {
            for (float[] v : face) {
                float t = (v[0] + 1f) / 2f; 
                float r = gradientStartColor[0]*(1 - t) + gradientEndColor[0]*t;
                float g = gradientStartColor[1]*(1 - t) + gradientEndColor[1]*t;
                float b = gradientStartColor[2]*(1 - t) + gradientEndColor[2]*t;
                gl.glColor3f(r, g, b);
                gl.glVertex3f(v[0], v[1], v[2]);
            }
        }
        gl.glEnd();
    }

    private void drawGradientSphere(GL2 gl) {
        int slices = 30, stacks = 30;
        float radius = 1.0f;

        for (int i = 0; i < stacks; i++) {
            float phi1 = (float) (Math.PI * i / stacks);
            float phi2 = (float) (Math.PI * (i + 1) / stacks);

            gl.glBegin(GL2.GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                float theta = (float) (2.0 * Math.PI * j / slices);

                float x1 = radius*(float)(Math.sin(phi1)*Math.cos(theta));
                float y1 = radius*(float)(Math.sin(phi1)*Math.sin(theta));
                float z1 = radius*(float)(Math.cos(phi1));

                float x2 = radius*(float)(Math.sin(phi2)*Math.cos(theta));
                float y2 = radius*(float)(Math.sin(phi2)*Math.sin(theta));
                float z2 = radius*(float)(Math.cos(phi2));

                // Color en X
                float t1 = (x1 + 1f)/2f;
                float r1 = gradientStartColor[0]*(1 - t1) + gradientEndColor[0]*t1;
                float g1 = gradientStartColor[1]*(1 - t1) + gradientEndColor[1]*t1;
                float b1 = gradientStartColor[2]*(1 - t1) + gradientEndColor[2]*t1;

                float t2 = (x2 + 1f)/2f;
                float r2 = gradientStartColor[0]*(1 - t2) + gradientEndColor[0]*t2;
                float g2 = gradientStartColor[1]*(1 - t2) + gradientEndColor[1]*t2;
                float b2 = gradientStartColor[2]*(1 - t2) + gradientEndColor[2]*t2;

                gl.glColor3f(r1, g1, b1);
                gl.glVertex3f(x1, y1, z1);

                gl.glColor3f(r2, g2, b2);
                gl.glVertex3f(x2, y2, z2);
            }
            gl.glEnd();
        }
    }

    private void drawGradientPyramid(GL2 gl) {
        gl.glBegin(GL2.GL_TRIANGLES);

        // Base (2 triángulos para un cuadrado)
        float[][] baseVertices = {
            {-1, -1, -1}, {1, -1, -1}, {1, -1, 1},
            {1, -1, 1},   {-1, -1, 1}, {-1, -1, -1}
        };
        for (float[] v : baseVertices) {
            float t = (v[0] + 1f)/2f;
            float r = gradientStartColor[0]*(1 - t) + gradientEndColor[0]*t;
            float g = gradientStartColor[1]*(1 - t) + gradientEndColor[1]*t;
            float b = gradientStartColor[2]*(1 - t) + gradientEndColor[2]*t;
            gl.glColor3f(r, g, b);
            gl.glVertex3f(v[0], v[1], v[2]);
        }

        // Caras laterales (4 triángulos)
        float[] apex = {0, 1, 0};
        float[][] sideVertices = {
            {-1, -1, -1}, {1, -1, -1}, apex,
            {1, -1, -1},  {1, -1,  1}, apex,
            {1, -1,  1},  {-1, -1, 1}, apex,
            {-1, -1,  1}, {-1, -1, -1}, apex
        };
        for (float[] v : sideVertices) {
            float t = (v[0] + 1f)/2f;
            float r = gradientStartColor[0]*(1 - t) + gradientEndColor[0]*t;
            float g = gradientStartColor[1]*(1 - t) + gradientEndColor[1]*t;
            float b = gradientStartColor[2]*(1 - t) + gradientEndColor[2]*t;
            gl.glColor3f(r, g, b);
            gl.glVertex3f(v[0], v[1], v[2]);
        }

        gl.glEnd();
    }

    private void drawGradientCylinder(GL2 gl) {
        int slices = 30;
        float radius = 1.0f;
        float height = 2.0f;

        // Parte lateral
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= slices; i++) {
            float theta = (float) (2.0 * Math.PI * i / slices);
            float x = radius*(float)Math.cos(theta);
            float z = radius*(float)Math.sin(theta);

            float t = (x + 1f)/2f;
            float r = gradientStartColor[0]*(1 - t) + gradientEndColor[0]*t;
            float g = gradientStartColor[1]*(1 - t) + gradientEndColor[1]*t;
            float b = gradientStartColor[2]*(1 - t) + gradientEndColor[2]*t;

            gl.glColor3f(r, g, b);
            gl.glVertex3f(x, -height/2, z);
            gl.glVertex3f(x,  height/2, z);
        }
        gl.glEnd();

        // Tapas superior e inferior
        drawGradientDisk(gl,  height/2,  radius, slices);
        drawGradientDisk(gl, -height/2, radius, slices);
    }

    private void drawGradientDisk(GL2 gl, float y, float radius, int slices) {
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        // Centro
        float tCenter = (0 + 1f)/2f;
        float rC = gradientStartColor[0]*(1 - tCenter) + gradientEndColor[0]*tCenter;
        float gC = gradientStartColor[1]*(1 - tCenter) + gradientEndColor[1]*tCenter;
        float bC = gradientStartColor[2]*(1 - tCenter) + gradientEndColor[2]*tCenter;
        gl.glColor3f(rC, gC, bC);
        gl.glVertex3f(0, y, 0);

        for (int i = 0; i <= slices; i++) {
            float theta = (float) (2.0 * Math.PI * i / slices);
            float x = radius*(float)Math.cos(theta);
            float z = radius*(float)Math.sin(theta);

            float t = (x + 1f)/2f;
            float rr = gradientStartColor[0]*(1 - t) + gradientEndColor[0]*t;
            float gg = gradientStartColor[1]*(1 - t) + gradientEndColor[1]*t;
            float bb = gradientStartColor[2]*(1 - t) + gradientEndColor[2]*t;

            gl.glColor3f(rr, gg, bb);
            gl.glVertex3f(x, y, z);
        }
        gl.glEnd();
    }

    private void drawGradientCone(GL2 gl) {
        int slices = 30;
        float radius = 1.0f;
        float height = 2.0f;

        // Parte lateral
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        // Ápice
        float tApex = (0 + 1f)/2f;
        float rA = gradientStartColor[0]*(1 - tApex) + gradientEndColor[0]*tApex;
        float gA = gradientStartColor[1]*(1 - tApex) + gradientEndColor[1]*tApex;
        float bA = gradientStartColor[2]*(1 - tApex) + gradientEndColor[2]*tApex;
        gl.glColor3f(rA, gA, bA);
        gl.glVertex3f(0, height/2, 0);

        for (int i = 0; i <= slices; i++) {
            float theta = (float) (2.0 * Math.PI * i / slices);
            float x = radius*(float)Math.cos(theta);
            float z = radius*(float)Math.sin(theta);

            float t = (x + 1f)/2f;
            float rr = gradientStartColor[0]*(1 - t) + gradientEndColor[0]*t;
            float gg = gradientStartColor[1]*(1 - t) + gradientEndColor[1]*t;
            float bb = gradientStartColor[2]*(1 - t) + gradientEndColor[2]*t;

            gl.glColor3f(rr, gg, bb);
            gl.glVertex3f(x, -height/2, z);
        }
        gl.glEnd();

        // Base
        drawGradientDisk(gl, -height/2, radius, slices);
    }

    // -----------------------------
    // Figuras SIN Degradado
    // -----------------------------
    private void drawGrid(GL2 gl) {
        gl.glPushMatrix();
        gl.glColor3f(0.4f, 0.4f, 0.4f);
        gl.glBegin(GL2.GL_LINES);
        float size = 10.0f;
        for (float i = -size; i <= size; i += 1.0f) {
            gl.glVertex3f(i, -1.0f, -size);
            gl.glVertex3f(i, -1.0f,  size);
            gl.glVertex3f(-size, -1.0f, i);
            gl.glVertex3f( size, -1.0f, i);
        }
        gl.glEnd();
        gl.glPopMatrix();
    }

    private Color convertColor(float[] values, int colorModel) {
        return switch (colorModel) {
            case 0 -> new Color(values[0], values[1], values[2]);            // RGB
            case 1 -> cmykToRgb(values);                                      // CMYK
            case 2 -> hslToRgb(values[0], values[1], values[2]);             // HSL
            case 3 -> Color.getHSBColor(values[0], values[1], values[2]);    // HSV
            default -> Color.WHITE;
        };
    }

    private Color cmykToRgb(float[] cmyk) {
        float c = cmyk[0];
        float m = cmyk[1];
        float y = cmyk[2];
        float k = cmyk[3];

        float r = 1 - Math.min(1, c*(1-k) + k);
        float g = 1 - Math.min(1, m*(1-k) + k);
        float b = 1 - Math.min(1, y*(1-k) + k);
        return new Color(r, g, b);
    }

    private Color hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0f) {
            r = g = b = l;  // escala de grises
        } else {
            float q = (l < 0.5f) ? (l*(1 + s)) : (l + s - l*s);
            float p = 2*l - q;
            r = hueToRgb(p, q, h + 1f/3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f/3f);
        }
        return new Color(r, g, b);
    }

    private float hueToRgb(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f/6f) return p + (q - p)*6f*t;
        if (t < 1f/2f) return q;
        if (t < 2f/3f) return p + (q - p)*(2f/3f - t)*6f;
        return p;
    }

    private void drawCube(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);
        float size = 1.0f;

        // Frontal
        gl.glVertex3f(-size, -size, size);
        gl.glVertex3f( size, -size, size);
        gl.glVertex3f( size,  size, size);
        gl.glVertex3f(-size,  size, size);

        // Trasera
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1,  1, -1);
        gl.glVertex3f( 1,  1, -1);
        gl.glVertex3f( 1, -1, -1);

        // Superior
        gl.glVertex3f(-1, 1, -1);
        gl.glVertex3f(-1, 1,  1);
        gl.glVertex3f( 1, 1,  1);
        gl.glVertex3f( 1, 1, -1);

        // Inferior
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f( 1, -1, -1);
        gl.glVertex3f( 1, -1,  1);
        gl.glVertex3f(-1, -1,  1);

        // Izquierda
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1, -1,  1);
        gl.glVertex3f(-1,  1,  1);
        gl.glVertex3f(-1,  1, -1);

        // Derecha
        gl.glVertex3f( 1, -1, -1);
        gl.glVertex3f( 1,  1, -1);
        gl.glVertex3f( 1,  1,  1);
        gl.glVertex3f( 1, -1,  1);

        gl.glEnd();
    }

    private void drawSphere(GL2 gl) {
        GLUquadric sphere = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_LINE);
        glu.gluSphere(sphere, 1, 50, 50);
        glu.gluDeleteQuadric(sphere);
    }

    private void drawPyramid(GL2 gl) {
        gl.glBegin(GL2.GL_TRIANGLES);

        // Base
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f( 1, -1, -1);
        gl.glVertex3f( 0, -1,  1);

        // Caras laterales
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f( 0,  1,  0);
        gl.glVertex3f( 0, -1,  1);

        gl.glVertex3f( 0, -1,  1);
        gl.glVertex3f( 0,  1,  0);
        gl.glVertex3f( 1, -1, -1);

        gl.glVertex3f( 1, -1, -1);
        gl.glVertex3f( 0,  1,  0);
        gl.glVertex3f(-1, -1, -1);

        gl.glEnd();
    }

    private void drawCylinder(GL2 gl) {
        GLUquadric quadric = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(quadric, GLU.GLU_FILL);

        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.0f, -1.0f);
        glu.gluCylinder(quadric, 1, 1, 2, 64, 1);

        // Tapa inferior
        glu.gluDisk(quadric, 0, 1, 64, 1);

        // Tapa superior
        gl.glPushMatrix();
        gl.glTranslatef(0, 0, 2);
        glu.gluDisk(quadric, 0, 1, 64, 1);
        gl.glPopMatrix();

        gl.glPopMatrix();
        glu.gluDeleteQuadric(quadric);
    }

    private void drawCone(GL2 gl) {
        GLUquadric cone = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(cone, GLU.GLU_FILL);

        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.0f, -1.0f);
        glu.gluCylinder(cone, 1, 0, 2, 64, 1);
        gl.glPopMatrix();

        glu.gluDeleteQuadric(cone);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        drawable.getGL().getGL2().glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        animator.stop();
    }

    // ------------------------------------------------------
    // MAIN
    // ------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EditorFiguras3D().setVisible(true));
    }

    // ------------------------------------------------------
    // MÉTODOS AUXILIARES (ComboBox, Slider, Etiquetas)
    // ------------------------------------------------------
    private JComboBox<String> crearComboBox(String[] items, Color colorFondo) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(colorFondo);
        combo.setForeground(Color.WHITE);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? colorFondo.darker() : colorFondo);
                setForeground(Color.WHITE);
                return this;
            }
        });
        return combo;
    }

    private JSlider crearSlider(int min, int max, int valor, String titulo) {
        JSlider slider = new JSlider(min, max, valor);
        slider.setBackground(new Color(70, 70, 70));
        slider.setForeground(Color.WHITE);

        // Marcas de escala
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setMinorTickSpacing((max - min) / 10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        slider.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.PLAIN, 12),
                Color.WHITE));

        return slider;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(new Color(220, 220, 220));
        etiqueta.setFont(new Font("Arial", Font.BOLD, 14));
        return etiqueta;
    }
}
