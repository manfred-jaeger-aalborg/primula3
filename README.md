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

For integration with PyTorch Geometric, see the separate documentation in Examples/WaterManagement



### Compiling Primula from Source

 **Gradle** can be used to compile Primula and manage external dependencies such as **Dom4J** and **JEP**.  

To compile Primula:

1. Download the repository
2. Navigate to the `Source` directory.
3. Run the following command:

   ```bash
   ./gradlew jar
   ```
(gradlew.bat on Windows)

The resulting Primula JAR file will be generated in the JAR folder.

### Need help? 
Don’t hesitate to reach out to either of these contacts, or use the [Issues](https://github.com/manfred-jaeger-aalborg/primula3/issues) tab on GitHub:
- jaeger@cs.aau.dk
- rafpoj@cs.aau.dk
