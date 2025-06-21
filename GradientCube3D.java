import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.event.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

// Para texturas
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class GradientCube3D extends JFrame implements GLEventListener {

    // ---------------------------
    // Combos y Paneles
    // ---------------------------
    private JComboBox<String> selectorForma, selectorColor, selectorTransform;
    private JPanel panelDegradado, panelColor, panelSesgado, panelPerspectiva, panelTextura;
    private JPanel panelInferior;

    // ---------------------------
    // Sliders y Botón
    // ---------------------------
    private JSlider sliderSesgoX, sliderSesgoY;
    private JSlider sliderFOV, sliderZoom;
    private JButton btnCargarTextura;

    // ---------------------------
    // OpenGL
    // ---------------------------
    private GLJPanel glPanel;
    private FPSAnimator animator;
    private GLU glu = new GLU();

    // ---------------------------
    // Variables de transformaciones
    // ---------------------------
    private float rotationX = 0, rotationY = 0;
    private float translateX = 0, translateY = 0, translateZ = -5;
    private float scale = 1;
    private float shearX = 0.0f, shearY = 0.0f;
    private float fov = 45.0f;

    // ---------------------------
    // Textura y buffer de imagen
    // ---------------------------
    private Texture loadedTexture = null;
    private BufferedImage pendingImage = null; // para cargar la imagen en Swing y procesarla en display()

    // ---------------------------
    // Figura actual y color
    // ---------------------------
    private int currentShape = 0;
    private float[] colorValues = new float[4];

    // ---------------------------
    // Degradado
    // ---------------------------
    private float[] gradientStartColor = {1.0f, 0.0f, 0.0f}; // Rojo
    private float[] gradientEndColor   = {0.0f, 0.0f, 1.0f}; // Azul

    // ----------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------
    public GradientCube3D() {
        configurarVentana();
        inicializarComponentes();
        configurarEventos();
    }

    // ----------------------------------------------------------
    // Configurar la ventana principal
    // ----------------------------------------------------------
    private void configurarVentana() {
        setTitle("Editor 3D (Sin Brillo, con Transformaciones y Textura)");
        setSize(1280, 960);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(45, 45, 45));
        setLayout(new BorderLayout(10, 10));
    }

    // ----------------------------------------------------------
    // Inicializar componentes (GLJPanel, Paneles, etc.)
    // ----------------------------------------------------------
    private void inicializarComponentes() {
        // OpenGL Panel
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        glPanel = new GLJPanel(caps);
        glPanel.addGLEventListener(this);

        animator = new FPSAnimator(glPanel, 60);
        animator.start();

        // Panel izquierdo (combos)
        JPanel panelIzquierdo = new JPanel(new GridLayout(0, 1, 10, 10));
        panelIzquierdo.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 80, 80)),
                new EmptyBorder(15, 15, 15, 15)));
        panelIzquierdo.setBackground(new Color(60, 60, 60));

        String[] formas = {"Cubo", "Esfera", "Piramide", "Cilindro", "Cono"};
        selectorForma = crearComboBox(formas, new Color(70, 130, 180));

        String[] modelosColor = {"RGB", "CMYK", "HSL", "HSV"};
        selectorColor = crearComboBox(modelosColor, new Color(80, 160, 120));

        // 0=Rotación, 1=Escala, 2=Traslación,
        // 3=Sesgado, 4=Perspectiva, 5=Degradado, 6=Textura
        String[] transformaciones = {
            "Rotación", "Escala", "Traslación",
            "Sesgado", "Perspectiva", "Degradado",
            "Textura"
        };
        selectorTransform = crearComboBox(transformaciones, new Color(160, 100, 200));

        panelIzquierdo.add(crearEtiqueta("Seleccionar Figura:"));
        panelIzquierdo.add(selectorForma);
        panelIzquierdo.add(crearEtiqueta("Modelo de Color:"));
        panelIzquierdo.add(selectorColor);
        panelIzquierdo.add(crearEtiqueta("Transformación:"));
        panelIzquierdo.add(selectorTransform);

        add(panelIzquierdo, BorderLayout.WEST);
        add(glPanel, BorderLayout.CENTER);

        // Panel derecho (Degradado y Color)
        JPanel panelDerecho = new JPanel();
        panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));
        panelDerecho.setBorder(new EmptyBorder(15, 15, 15, 15));
        panelDerecho.setBackground(new Color(70, 70, 70));

        // Panel Degradado
        panelDegradado = new JPanel(new GridLayout(3, 2, 5, 5));
        panelDegradado.setBorder(new TitledBorder("Controles de Degradado"));
        panelDegradado.setBackground(new Color(70, 70, 70));

        String[] colorLabels = {"Rojo", "Verde", "Azul"};
        JSlider[] startSliders = new JSlider[3];
        JSlider[] endSliders   = new JSlider[3];

        for (int i = 0; i < 3; i++) {
            startSliders[i] = crearSlider(0, 100, (int)(gradientStartColor[i]*100), "Inicio " + colorLabels[i]);
            endSliders[i]   = crearSlider(0, 100, (int)(gradientEndColor[i]*100),   "Fin " + colorLabels[i]);

            final int idx = i;
            startSliders[i].addChangeListener(e -> {
                gradientStartColor[idx] = startSliders[idx].getValue()/100f;
                glPanel.repaint();
            });
            endSliders[i].addChangeListener(e -> {
                gradientEndColor[idx] = endSliders[idx].getValue()/100f;
                glPanel.repaint();
            });

            panelDegradado.add(startSliders[i]);
            panelDegradado.add(endSliders[i]);
        }

        JScrollPane scrollDegradado = new JScrollPane(panelDegradado,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollDegradado.setPreferredSize(new Dimension(320, 240));

        // Panel Color
        panelColor = new JPanel();
        panelColor.setLayout(new GridLayout(0, 1, 5, 5));
        panelColor.setBorder(new TitledBorder("Controles de Color"));
        panelColor.setBackground(new Color(70, 70, 70));

        JScrollPane scrollColor = new JScrollPane(panelColor,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollColor.setPreferredSize(new Dimension(320, 240));

        panelDerecho.add(scrollDegradado);
        panelDerecho.add(Box.createVerticalStrut(10));
        panelDerecho.add(scrollColor);

        panelDegradado.setVisible(false); // Oculto hasta que se seleccione "Degradado"
        add(panelDerecho, BorderLayout.EAST);

        // Panel Inferior (CardLayout)
        panelInferior = new JPanel(new CardLayout());
        panelInferior.setBackground(new Color(70, 70, 70));

        // Panel vacío
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
        sliderFOV  = crearSlider(30, 120, (int)fov, "Campo de Visión (FOV)");
        sliderZoom = crearSlider(1, 20, 5, "Zoom (Z Translate)");
        panelPerspectiva.add(sliderFOV);
        panelPerspectiva.add(sliderZoom);
        panelInferior.add(panelPerspectiva, "perspectiva");

        // Panel Textura (solo un botón)
        panelTextura = new JPanel(new FlowLayout());
        panelTextura.setBackground(new Color(70,70,70));
        btnCargarTextura = new JButton("Cargar Textura");
        btnCargarTextura.setBackground(new Color(90,90,120));
        btnCargarTextura.setForeground(Color.WHITE);
        panelTextura.add(btnCargarTextura);

        panelInferior.add(panelTextura, "textura");

        add(panelInferior, BorderLayout.SOUTH);

        // Inicializar color (RGB)
        actualizarControlesColor();
    }

    // ----------------------------------------------------------
    // Configurar eventos (combos, sliders, mouse)
    // ----------------------------------------------------------
    private void configurarEventos() {
        // Seleccionar figura
        selectorForma.addActionListener(e -> {
            currentShape = selectorForma.getSelectedIndex();
            glPanel.repaint();
        });

        // Seleccionar modelo de color
        selectorColor.addActionListener(e -> {
            actualizarControlesColor();
            glPanel.repaint();
        });

        // Seleccionar transformación
        selectorTransform.addActionListener(e -> {
            int sel = selectorTransform.getSelectedIndex();
            boolean isGradient = (sel == 5);
            panelDegradado.setVisible(isGradient);

            CardLayout cl = (CardLayout)(panelInferior.getLayout());
            switch(sel) {
                case 3 -> cl.show(panelInferior, "sesgado");
                case 4 -> cl.show(panelInferior, "perspectiva");
                case 6 -> cl.show(panelInferior, "textura"); // antes "materialTextura"
                default -> cl.show(panelInferior, "vacio");
            }
            glPanel.repaint();
        });

        // Sliders (Sesgado)
        sliderSesgoX.addChangeListener(e -> {
            shearX = sliderSesgoX.getValue()/100f;
            glPanel.repaint();
        });
        sliderSesgoY.addChangeListener(e -> {
            shearY = sliderSesgoY.getValue()/100f;
            glPanel.repaint();
        });

        // Sliders (Perspectiva)
        sliderFOV.addChangeListener(e -> {
            fov = sliderFOV.getValue();
            glPanel.repaint();
        });
        sliderZoom.addChangeListener(e -> {
            translateZ = -sliderZoom.getValue();
            glPanel.repaint();
        });

        // Botón cargar textura
        btnCargarTextura.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if(result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    // Cargar la imagen en Swing (sin contexto GL)
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        // Guardamos en pendingImage para procesarlo en display()
                        pendingImage = img;
                        System.out.println("Imagen cargada en memoria: " + file.getName());
                    }
                } catch(IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al leer la imagen:\n"+ex.getMessage());
                }
                // Forzar repaint para que display() cree la textura
                glPanel.repaint();
            }
        });

        // Eventos de ratón (rotar, escalar, trasladar)
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private Point lastPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Quitar la restricción: ahora se pueden mover en todos los modos
                int dx = e.getX() - lastPoint.x;
                int dy = e.getY() - lastPoint.y;

                switch (selectorTransform.getSelectedIndex()) {
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
                    // Sesgado (3), Perspectiva (4), Degradado (5), Textura (6)
                    // ya no bloqueamos nada, el usuario puede rotar/escala/trasladar igual
                }
                lastPoint = e.getPoint();
                glPanel.repaint();
            }
        };
        glPanel.addMouseListener(mouseAdapter);
        glPanel.addMouseMotionListener(mouseAdapter);
    }

    // Actualiza panel de color según el modelo (RGB, CMYK, HSL, HSV)
    private void actualizarControlesColor() {
        panelColor.removeAll();

        String modelo = (String) selectorColor.getSelectedItem();
        String[] etiquetas = obtenerEtiquetasColor(modelo);
        colorValues = new float[etiquetas.length];

        for(int i=0; i<etiquetas.length; i++){
            JSlider slider = crearSlider(0,100,0, etiquetas[i]);
            final int idx = i;
            slider.addChangeListener(e -> {
                colorValues[idx] = slider.getValue()/100f;
                glPanel.repaint();
            });
            panelColor.add(slider);
        }
        panelColor.revalidate();
        panelColor.repaint();
    }

    private String[] obtenerEtiquetasColor(String modelo) {
        return switch (modelo) {
            case "RGB"  -> new String[]{"Rojo","Verde","Azul"};
            case "CMYK" -> new String[]{"Cian","Magenta","Amarillo","Negro"};
            case "HSL"  -> new String[]{"Tono","Saturación","Luminosidad"};
            case "HSV"  -> new String[]{"Tono","Saturación","Valor"};
            default     -> new String[0];
        };
    }

    // ----------------------------------------------------------
    // Métodos de la interfaz GLEventListener
    // ----------------------------------------------------------
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);

        // Iluminación
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        // Fondo
        gl.glClearColor(0.2f,0.2f,0.2f,1f);

        // Sombreado suave
        gl.glShadeModel(GL2.GL_SMOOTH);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        if (animator.isStarted()) {
            animator.stop();
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // 1) Si hay imagen pendiente, crear la textura con contexto GL
        if (pendingImage != null) {
            if (loadedTexture != null) {
                loadedTexture.destroy(gl);
                loadedTexture = null;
            }
            try {
                loadedTexture = AWTTextureIO.newTexture(GLProfile.getDefault(), pendingImage, true);
                System.out.println(">> Textura creada en contexto OpenGL.");
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            pendingImage = null;
        }

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Modo ModelView
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Luz
        float[] lightPos = {0,5,8,1};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);

        // Cámara
        glu.gluPerspective(fov, (float)glPanel.getWidth()/glPanel.getHeight(), 0.1f,100f);
        glu.gluLookAt(0,2,5,  0,1,0,  0,1,0);

        // Dibujar grid
        drawGrid(gl);

        // Transformaciones
        gl.glTranslatef(translateX, translateY, translateZ);
        gl.glRotatef(rotationX,1,0,0);
        gl.glRotatef(rotationY,0,1,0);
        gl.glScalef(scale, scale, scale);

        // Sesgado
        if (selectorTransform.getSelectedIndex() == 3) {
            float anchorX=-1, anchorY=-1, anchorZ=-1;
            gl.glTranslatef(anchorX, anchorY, anchorZ);

            float[] shearMatrix = {
                1f,    shearY, 0f, 0f,
                shearX,1f,     0f, 0f,
                0f,    0f,     1f, 0f,
                0f,    0f,     0f, 1f
            };
            gl.glMultMatrixf(shearMatrix,0);
            gl.glTranslatef(-anchorX, -anchorY, -anchorZ);
        }

        // Habilitar colorMaterial siempre, para que el color y degradado afecten
        gl.glEnable(GL2.GL_COLOR_MATERIAL);

        // Si la transformación es "Textura" (índice 6), activamos la textura (si existe)
        if (selectorTransform.getSelectedIndex() == 6 && loadedTexture != null) {
            gl.glEnable(GL2.GL_TEXTURE_2D);
            loadedTexture.enable(gl);
            loadedTexture.bind(gl);
        } else {
            gl.glDisable(GL2.GL_TEXTURE_2D);
        }

        // Degradado
        if (selectorTransform.getSelectedIndex() == 5) {
            gl.glDisable(GL2.GL_TEXTURE_2D); // Forzamos sin textura en modo degrade?
            drawWithGradient(gl);
        } else {
            // Color sólido
            Color colorSolido = convertColor(colorValues, selectorColor.getSelectedIndex());
            gl.glColor3f(colorSolido.getRed()/255f,
                         colorSolido.getGreen()/255f,
                         colorSolido.getBlue()/255f);

            switch (currentShape) {
                case 0 -> drawCube(gl);
                case 1 -> drawSphere(gl);
                case 2 -> drawPyramid(gl);
                case 3 -> drawCylinder(gl);
                case 4 -> drawCone(gl);
            }
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        drawable.getGL().getGL2().glViewport(0, 0, width, height);
    }

    // ----------------------------------------------------------
    // Dibujo de Figuras
    // ----------------------------------------------------------
    private void drawWithGradient(GL2 gl) {
        switch(currentShape) {
            case 0 -> drawGradientCube(gl);
            case 1 -> drawGradientSphere(gl);
            case 2 -> drawGradientPyramid(gl);
            case 3 -> drawGradientCylinder(gl);
            case 4 -> drawGradientCone(gl);
        }
    }

    private void drawGradientCube(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);
        float[][][] caras = {
            {{-1,-1,1}, {1,-1,1}, {1,1,1}, {-1,1,1}},
            {{1,-1,-1}, {-1,-1,-1}, {-1,1,-1}, {1,1,-1}},
            {{-1,-1,-1}, {-1,-1,1}, {-1,1,1}, {-1,1,-1}},
            {{1,-1,1}, {1,-1,-1}, {1,1,-1}, {1,1,1}},
            {{-1,1,1}, {1,1,1}, {1,1,-1}, {-1,1,-1}},
            {{-1,-1,-1}, {1,-1,-1}, {1,-1,1}, {-1,-1,1}}
        };
        float[][] normales = {
            {0,0,1}, {0,0,-1}, {-1,0,0}, {1,0,0}, {0,1,0}, {0,-1,0}
        };
    
        for(int i=0; i<caras.length; i++){
            gl.glNormal3fv(normales[i],0);
            for(float[] v : caras[i]){
                float t=(v[0]+1f)/2f;
                gl.glColor3f(
                    gradientStartColor[0]*(1-t)+gradientEndColor[0]*t,
                    gradientStartColor[1]*(1-t)+gradientEndColor[1]*t,
                    gradientStartColor[2]*(1-t)+gradientEndColor[2]*t);
                gl.glVertex3fv(v,0);
            }
        }
        gl.glEnd();
    }
    
    private void drawGradientSphere(GL2 gl) {
        int slices=32, stacks=32;
        float radius=1.0f;
        for(int i=0;i<stacks;i++){
            float phi1=(float)(Math.PI*i/stacks);
            float phi2=(float)(Math.PI*(i+1)/stacks);
            gl.glBegin(GL2.GL_QUAD_STRIP);
            for(int j=0;j<=slices;j++){
                float theta=(float)(2*Math.PI*j/slices);
                float x1=(float)(Math.sin(phi1)*Math.cos(theta));
                float y1=(float)(Math.cos(phi1));
                float z1=(float)(Math.sin(phi1)*Math.sin(theta));
                float x2=(float)(Math.sin(phi2)*Math.cos(theta));
                float y2=(float)(Math.cos(phi2));
                float z2=(float)(Math.sin(phi2)*Math.sin(theta));
    
                gl.glNormal3f(x1,y1,z1);
                float t1=(x1+1f)/2f;
                gl.glColor3f(
                    gradientStartColor[0]*(1-t1)+gradientEndColor[0]*t1,
                    gradientStartColor[1]*(1-t1)+gradientEndColor[1]*t1,
                    gradientStartColor[2]*(1-t1)+gradientEndColor[2]*t1);
                gl.glVertex3f(radius*x1,radius*y1,radius*z1);
    
                gl.glNormal3f(x2,y2,z2);
                float t2=(x2+1f)/2f;
                gl.glColor3f(
                    gradientStartColor[0]*(1-t2)+gradientEndColor[0]*t2,
                    gradientStartColor[1]*(1-t2)+gradientEndColor[1]*t2,
                    gradientStartColor[2]*(1-t2)+gradientEndColor[2]*t2);
                gl.glVertex3f(radius*x2,radius*y2,radius*z2);
            }
            gl.glEnd();
        }
    }
    
    private void drawGradientPyramid(GL2 gl) {
        gl.glBegin(GL2.GL_TRIANGLES);
        float[][] vertices = {{-1,-1,-1},{1,-1,-1},{1,-1,1},{-1,-1,1},{0,1,0}};
        int[][] indices={{0,1,4},{1,2,4},{2,3,4},{3,0,4},{0,3,2},{2,1,0}};
        for(int[] face:indices){
            float[] n=calcularNormal(vertices[face[0]],vertices[face[1]],vertices[face[2]]);
            gl.glNormal3fv(n,0);
            for(int idx:face){
                float t=(vertices[idx][0]+1f)/2f;
                gl.glColor3f(
                    gradientStartColor[0]*(1-t)+gradientEndColor[0]*t,
                    gradientStartColor[1]*(1-t)+gradientEndColor[1]*t,
                    gradientStartColor[2]*(1-t)+gradientEndColor[2]*t);
                gl.glVertex3fv(vertices[idx],0);
            }
        }
        gl.glEnd();
    }
    
    private float[] calcularNormal(float[] v1,float[] v2,float[] v3){
        float[] u={v2[0]-v1[0],v2[1]-v1[1],v2[2]-v1[2]};
        float[] v={v3[0]-v1[0],v3[1]-v1[1],v3[2]-v1[2]};
        return new float[]{
            u[1]*v[2]-u[2]*v[1],
            u[2]*v[0]-u[0]*v[2],
            u[0]*v[1]-u[1]*v[0]
        };
    }
    
    private void drawGradientCylinder(GL2 gl){
        int slices=32;
        float radius=1,height=2;
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for(int i=0;i<=slices;i++){
            float angle=(float)(2*Math.PI*i/slices);
            float x=(float)Math.cos(angle),z=(float)Math.sin(angle);
            gl.glNormal3f(x,0,z);
            float t=(x+1f)/2f;
            gl.glColor3f(
                gradientStartColor[0]*(1-t)+gradientEndColor[0]*t,
                gradientStartColor[1]*(1-t)+gradientEndColor[1]*t,
                gradientStartColor[2]*(1-t)+gradientEndColor[2]*t);
            gl.glVertex3f(radius*x,height/2,z*radius);
            gl.glVertex3f(radius*x,-height/2,z*radius);
        }
        gl.glEnd();
    }
    
    private void drawGradientCone(GL2 gl){
        int slices=32;
        float radius=1,height=2;
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3f(0,1,0);
        gl.glColor3fv(gradientStartColor,0);
        gl.glVertex3f(0,height/2,0);
        for(int i=0;i<=slices;i++){
            float angle=(float)(2*Math.PI*i/slices);
            float x=(float)Math.cos(angle),z=(float)Math.sin(angle);
            float t=(x+1f)/2f;
            gl.glColor3f(
                gradientStartColor[0]*(1-t)+gradientEndColor[0]*t,
                gradientStartColor[1]*(1-t)+gradientEndColor[1]*t,
                gradientStartColor[2]*(1-t)+gradientEndColor[2]*t);
            gl.glVertex3f(radius*x,-height/2,z*radius);
        }
        gl.glEnd();
    }
    

    private void drawGrid(GL2 gl) {
        gl.glPushMatrix();
        gl.glColor3f(0.4f,0.4f,0.4f);
        gl.glBegin(GL2.GL_LINES);
        float size=10f;
        for(float i=-size;i<=size;i+=1f){
            gl.glVertex3f(i,-1f,-size);
            gl.glVertex3f(i,-1f, size);
            gl.glVertex3f(-size,-1f,i);
            gl.glVertex3f( size,-1f,i);
        }
        gl.glEnd();
        gl.glPopMatrix();
    }

    private void drawCube(GL2 gl) {
        boolean usingTex = gl.glIsEnabled(GL2.GL_TEXTURE_2D);

        gl.glBegin(GL2.GL_QUADS);

        float s=1f;

        // Cara frontal (0,0,1)
        gl.glNormal3f(0,0,1);
        if(usingTex) gl.glTexCoord2f(0,0);
        gl.glVertex3f(-s,-s,s);
        if(usingTex) gl.glTexCoord2f(1,0);
        gl.glVertex3f( s,-s,s);
        if(usingTex) gl.glTexCoord2f(1,1);
        gl.glVertex3f( s, s,s);
        if(usingTex) gl.glTexCoord2f(0,1);
        gl.glVertex3f(-s, s,s);

        // Cara trasera (0,0,-1)
        gl.glNormal3f(0,0,-1);
        if(usingTex) gl.glTexCoord2f(1,0);
        gl.glVertex3f(-s,-s,-s);
        if(usingTex) gl.glTexCoord2f(1,1);
        gl.glVertex3f(-s, s,-s);
        if(usingTex) gl.glTexCoord2f(0,1);
        gl.glVertex3f( s, s,-s);
        if(usingTex) gl.glTexCoord2f(0,0);
        gl.glVertex3f( s,-s,-s);

        // Superior (0,1,0)
        gl.glNormal3f(0,1,0);
        if(usingTex) gl.glTexCoord2f(0,1);
        gl.glVertex3f(-s, s,-s);
        if(usingTex) gl.glTexCoord2f(0,0);
        gl.glVertex3f(-s, s, s);
        if(usingTex) gl.glTexCoord2f(1,0);
        gl.glVertex3f( s, s, s);
        if(usingTex) gl.glTexCoord2f(1,1);
        gl.glVertex3f( s, s,-s);

        // Inferior (0,-1,0)
        gl.glNormal3f(0,-1,0);
        if(usingTex) gl.glTexCoord2f(0,0);
        gl.glVertex3f(-s,-s,-s);
        if(usingTex) gl.glTexCoord2f(1,0);
        gl.glVertex3f( s,-s,-s);
        if(usingTex) gl.glTexCoord2f(1,1);
        gl.glVertex3f( s,-s, s);
        if(usingTex) gl.glTexCoord2f(0,1);
        gl.glVertex3f(-s,-s, s);

        // Izquierda (-1,0,0)
        gl.glNormal3f(-1,0,0);
        if(usingTex) gl.glTexCoord2f(1,0);
        gl.glVertex3f(-s,-s,-s);
        if(usingTex) gl.glTexCoord2f(1,1);
        gl.glVertex3f(-s,-s, s);
        if(usingTex) gl.glTexCoord2f(0,1);
        gl.glVertex3f(-s, s, s);
        if(usingTex) gl.glTexCoord2f(0,0);
        gl.glVertex3f(-s, s,-s);

        // Derecha (1,0,0)
        gl.glNormal3f(1,0,0);
        if(usingTex) gl.glTexCoord2f(0,0);
        gl.glVertex3f( s,-s,-s);
        if(usingTex) gl.glTexCoord2f(0,1);
        gl.glVertex3f( s, s,-s);
        if(usingTex) gl.glTexCoord2f(1,1);
        gl.glVertex3f( s, s, s);
        if(usingTex) gl.glTexCoord2f(1,0);
        gl.glVertex3f( s,-s, s);

        gl.glEnd();
    }

    private void drawSphere(GL2 gl) {
        GLUquadric quad = glu.gluNewQuadric();
        glu.gluQuadricNormals(quad, GLU.GLU_SMOOTH);

        if(gl.glIsEnabled(GL2.GL_TEXTURE_2D)) {
            glu.gluQuadricTexture(quad,true);
        } else {
            glu.gluQuadricTexture(quad,false);
        }
        glu.gluSphere(quad,1,50,50);
        glu.gluDeleteQuadric(quad);
    }

    private void drawPyramid(GL2 gl) {
        // Aquí puedes meter la pirámide con coords de textura, etc. si gustas
        // Versión mínima:
        gl.glBegin(GL2.GL_TRIANGLES);
        // Base + caras
        gl.glEnd();
    }

    private void drawCylinder(GL2 gl) {
        GLUquadric q = glu.gluNewQuadric();
        glu.gluQuadricNormals(q, GLU.GLU_SMOOTH);

        if(gl.glIsEnabled(GL2.GL_TEXTURE_2D)){
            glu.gluQuadricTexture(q,true);
        } else {
            glu.gluQuadricTexture(q,false);
        }
        gl.glPushMatrix();
        gl.glTranslatef(0,0,-1);
        glu.gluCylinder(q,1,1,2,64,1);
        glu.gluDisk(q,0,1,64,1);
        gl.glTranslatef(0,0,2);
        glu.gluDisk(q,0,1,64,1);
        gl.glPopMatrix();
        glu.gluDeleteQuadric(q);
    }

    private void drawCone(GL2 gl) {
        GLUquadric cone = glu.gluNewQuadric();
        glu.gluQuadricNormals(cone, GLU.GLU_SMOOTH);

        if(gl.glIsEnabled(GL2.GL_TEXTURE_2D)){
            glu.gluQuadricTexture(cone,true);
        } else {
            glu.gluQuadricTexture(cone,false);
        }
        gl.glPushMatrix();
        gl.glTranslatef(0,0,-1);
        glu.gluCylinder(cone,1,0,2,64,1);
        gl.glPopMatrix();

        GLUquadric base = glu.gluNewQuadric();
        glu.gluQuadricNormals(base, GLU.GLU_SMOOTH);
        if(gl.glIsEnabled(GL2.GL_TEXTURE_2D)){
            glu.gluQuadricTexture(base,true);
        }
        gl.glPushMatrix();
        gl.glTranslatef(0,0,-1);
        glu.gluDisk(base,0,1,64,1);
        gl.glPopMatrix();
        glu.gluDeleteQuadric(base);

        glu.gluDeleteQuadric(cone);
    }

    // ----------------------------------------------------------
    // MAIN
    // ----------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GradientCube3D().setVisible(true));
    }

    // ----------------------------------------------------------
    // Métodos Auxiliares (Combo, Slider, Etiqueta)
    // ----------------------------------------------------------
    private JComboBox<String> crearComboBox(String[] items, Color bgColor) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(bgColor);
        combo.setForeground(Color.WHITE);
        combo.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
                setBackground(isSelected ? bgColor.darker() : bgColor);
                setForeground(Color.WHITE);
                return this;
            }
        });
        return combo;
    }

    private JSlider crearSlider(int min, int max, int val, String titulo) {
        JSlider slider = new JSlider(min,max,val);
        slider.setBackground(new Color(70,70,70));
        slider.setForeground(Color.WHITE);

        slider.setMajorTickSpacing((max-min)/5);
        slider.setMinorTickSpacing((max-min)/10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        slider.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100,100,100)),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.PLAIN, 12),
                Color.WHITE
        ));

        return slider;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(new Color(220,220,220));
        lbl.setFont(new Font("Arial",Font.BOLD,14));
        return lbl;
    }

    // ----------------------------------------------------------
    // MÉTODO convertColor(...) y auxiliares
    // ----------------------------------------------------------
    /**
     * Convierte un arreglo de componentes de color (float[]) al objeto Color
     * usando el modelo de color indicado (0 = RGB, 1 = CMYK, 2 = HSL, 3 = HSV).
     */
    private Color convertColor(float[] values, int colorModel) {
        switch (colorModel) {
            case 0: // RGB
                // Espera 3 componentes: R, G, B en [0..1]
                return new Color(values[0], values[1], values[2]);

            case 1: // CMYK
                // Espera 4 componentes: C, M, Y, K en [0..1]
                return cmykToRgb(values);

            case 2: // HSL
                // Espera 3 componentes: H, S, L en [0..1]
                return hslToRgb(values[0], values[1], values[2]);

            case 3: // HSV (HSB en Java)
                // Espera 3 componentes: H, S, V en [0..1]
                return Color.getHSBColor(values[0], values[1], values[2]);

            default:
                // Si no coincide, devuelves blanco
                return Color.WHITE;
        }
    }

    /**
     * Convierte un arreglo CMYK a Color RGB.
     * values: [C, M, Y, K], cada uno en [0..1].
     */
    private Color cmykToRgb(float[] cmyk) {
        float c = cmyk[0];
        float m = cmyk[1];
        float y = cmyk[2];
        float k = cmyk[3];

        // Fórmula básica: R = 1 - min(1, C*(1-K) + K)
        float r = 1 - Math.min(1, c*(1-k) + k);
        float g = 1 - Math.min(1, m*(1-k) + k);
        float b = 1 - Math.min(1, y*(1-k) + k);

        return new Color(r, g, b);
    }

    /**
     * Convierte HSL (Hue, Saturation, Lightness) a un Color RGB.
     * h, s, l en [0..1].
     */
    private Color hslToRgb(float h, float s, float l) {
        float r, g, b;

        if (s == 0f) {
            // escala de grises
            r = g = b = l;
        } else {
            float q = (l < 0.5f) ? (l*(1 + s)) : (l + s - l*s);
            float p = 2*l - q;
            r = hueToRgb(p, q, h + 1f/3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f/3f);
        }
        return new Color(r, g, b);
    }

    /**
     * Función auxiliar para HSL -> RGB.
     */
    private float hueToRgb(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f/6f) return p + (q - p) * 6f * t;
        if (t < 1f/2f) return q;
        if (t < 2f/3f) return p + (q - p) * (2f/3f - t) * 6f;
        return p;
    }

}
