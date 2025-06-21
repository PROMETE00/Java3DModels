# Java 3D Graphics Engine with OpenGL/JOGL

## 🚀 Getting Started (Linux/VS Code)

### Prerequisites

sudo apt-get install libjogl2-java freeglut3-dev

A complete 3D graphics implementation featuring geometric transformations, interactive models, and real-time rendering using Java OpenGL (JOGL). Optimized for Linux development environments.

## ✨ Key Features

### 🧩 3D Model Implementations
- **Primitive Shapes**: Cube, Sphere, Pyramid, Cylinder with customizable parameters
- **Complex Structures**:
  - Fully textured 3D house (`Casa3DJOGL.java`)
  - Interactive vehicle model (`Carro3DSimplificado.java`)
  - Navigable 3D maze system (`HexLaberintoJOGL.java`)

### 🔧 Geometric Transformations
- **Basic Operations**: Translation, Rotation (Euler angles), Uniform/Non-uniform Scaling
- **Advanced Manipulations**:
  - 3D Shearing/Skewing
  - Matrix stack transformations
  - Object-space vs World-space operations

### 💡 Rendering Techniques
- JOGL-immediate mode rendering (glBegin/glEnd)
- Phong lighting model implementation
- Texture mapping (diffuse/specular)
- Vertex Buffer Objects (VBO) for efficient rendering

## 🛠️ Technical Stack

| Component | Description |
|-----------|-------------|
| **Core** | Java 8+ |
| **Graphics** | JOGL 2.4 |
| **Math** | Custom matrix/vector operations |
| **Platform** | Linux-optimized (Ubuntu/Debian) |

## 📦 Project Structure
src/
├── core/ # Transformation matrices
├── models/ # 3D object implementations
│ ├── Casa3DJOGL.java # House model
│ ├── Carro3DSimplificado.java # Vehicle model
│ └── HexLaberintoJOGL.java # Maze system
resources/
├── textures/ # Texture images
└── shaders/ # GLSL shaders (optional)
