# Primula
Java implementation of Relational Bayesian Networks

### To run the software:

- Download the primula.jar file from the JAR directory
- To get started: download an .rbn and an .rdef file from one of the directories under Examples/, and follow 
  the instructions in the .pdf document for that example
- You can run the .jar file with the following command:
```bash
  java -jar primula.jar
```

### Requirements

Before running or compiling Primula, please ensure that the following prerequisites are met:

- Java Development Kit (JDK) 17
- Python Environment (for PyTorch Geometric Integration)

### PyTorch Geometric Integration Setup

Primula integrates with PyTorch via JEP, which allows embedding CPython within the JVM. Make sure that your Python environment has [PyTorch Geometric](pytorch-geometric.readthedocs.io/en/latest/install/installation.html) installed.

Install JEP in your Python environment:
   
  ```bash
    pip install jep
  ```
  
Set the PYTHONHOME environment variable to the path of your Python interpreter
(the same one where JEP and PyTorch are installed). For example:
  
  ```bash
    export PYTHONHOME=/path/to/your/python
  ```

Ensure that your Python environment has both PyTorch and JEP installed for seamless integration.

### Compile Primula

Primula uses **Gradle** to compile the project and manage external dependencies such as **Dom4J** and **JEP**.  
To compile Primula:

1. Download the repository
2. Navigate to the `Source` directory.
3. Run the following command:

   ```bash
   ./gradlew jar
   ```
(gradlew.bat on Windows)

The resulting Primula JAR file will be generated in the JAR folder.
