# Primula
Java implementation of Relational Bayesian Networks

To run the software:

- download primula.jar and dom4j-2.0.3.jar from the JAR directory. Place them in a common folder on your machine
- to get started: download an .rbn and an .rdef file from one of the directories under Examples/, and follow 
  the instructions in the .pdf document for that example. 


### Compile and Run

Primula uses **Gradle** to compile the project and manage external dependencies such as **Dom4J** and **JEP**.  
To compile Primula:

1. Navigate to the `Source` directory.
2. Run the following command:

   ```bash
   ./gradlew jar
   ```
The resulting Primula JAR file will be generated in the JAR folder.

### PyTorch Integration Setup

Primula integrates with PyTorch via JEP, which allows embedding CPython within the JVM.
To set up the PyTorch integration:

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