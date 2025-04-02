package PyManager;

import RBNpackage.CatGnn;
import jep.MainInterpreter;
import jep.SharedInterpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class JepManager {
    // Thread-local to ensure each thread gets its own interpreter
    private static final ThreadLocal<SharedInterpreter> threadLocalInterpreter = new ThreadLocal<>();

    /**
     * Initializes the Jep interpreter by setting the Jep library path.
     * This method should be called once (per thread) before any interpreter use.
     *
     * @throws IOException if the Python executable or Jep library path cannot be determined
     */
    public static void initializeJep() throws IOException {
        // Check if an interpreter has already been created for this thread
        if (threadLocalInterpreter.get() == null) {
            try {
                String jepLibPath = loadJep();
                MainInterpreter.setJepLibraryPath(jepLibPath);
                System.out.println("Jep initialized with library path: " + jepLibPath);
            } catch (IllegalStateException e) {
                System.err.println("Error in loading JepLibraryPath: " + e.getMessage());
                throw e;
            }
        }
    }

    public static String loadJep() throws IOException {
        String pythonExecutable = detectPythonExecutable();
        if (pythonExecutable == null || pythonExecutable.isEmpty()) {
            throw new IOException("Python executable not found. Please ensure PYTHON_PRIMULA is set or provide a valid pythonHome.");
        }
        // Assuming the helper script is at "python/get_jep_path.py" relative to the working directory
        Process process = Runtime.getRuntime().exec(pythonExecutable + " python/get_jep_path.py");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String jepLibPath = in.readLine();
            if (jepLibPath == null || jepLibPath.isEmpty()) {
                throw new IOException("Failed to retrieve Jep library path from Python script.");
            }
            return jepLibPath;
        }
    }

    private static String detectPythonExecutable() {
        try {
            String envPythonHome = System.getenv("PYTHON_PRIMULA");
            if (envPythonHome != null && !envPythonHome.isEmpty()) {
                String pythonPath = envPythonHome + File.separator + "bin" + File.separator + "python";
                File pythonFile = new File(pythonPath);
                if (pythonFile.exists() && pythonFile.canExecute()) {
                    return pythonPath;
                }
            } else {
                System.err.println("Missing variable PYTHON_PRIMULA!");
                throw new RuntimeException();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error while detecting Python executable: " + e.getMessage());
            e.printStackTrace();
        }
        // Python executable not found
        return null;
    }

    private static String detectPythonExecutable(String pythonHome) {
        try {
            String envPythonHome = System.getenv("PYTHON_PRIMULA");
            if (envPythonHome != null && !envPythonHome.isEmpty()) {
                String pythonPath = envPythonHome + File.separator + "bin" + File.separator + "python";
                File pythonFile = new File(pythonPath);
                if (pythonFile.exists() && pythonFile.canExecute()) {
                    return pythonPath;
                }
            }
            // Fallback: use the provided pythonHome if available
            if (pythonHome != null && !pythonHome.isEmpty()) {
                File pythonFile = new File(pythonHome);
                if (pythonFile.exists() && pythonFile.canExecute()) {
                    return pythonHome;
                }
            }
        } catch (Exception e) {
            System.err.println("Unexpected error while detecting Python executable: " + e.getMessage());
            e.printStackTrace();
        }
        // Python executable not found
        return null;
    }

    public static SharedInterpreter getInterpreter(boolean baseImport) {
        SharedInterpreter interp = threadLocalInterpreter.get();
        if (interp == null) {
            interp = new SharedInterpreter();
            if (baseImport) {
                // Basic initialization for the interpreter
                interp.exec("import torch");
                interp.exec("import numpy as np");
                interp.exec("from torch_geometric.data import Data, HeteroData");
                interp.exec("import sys");
            }
            threadLocalInterpreter.set(interp);
            System.out.println("Interpreter created for thread: " + Thread.currentThread().getName());
        }
        return interp;
    }

    public static void closeInterpreter() {
        SharedInterpreter interp = threadLocalInterpreter.get();
        if (interp != null) {
            try {
                interp.exec("import gc; gc.collect()");
            } finally {
                interp.close();
                threadLocalInterpreter.remove();
                System.out.println("Interpreter closed for thread: " + Thread.currentThread().getName());
            }
        }
    }

    public static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(JepManager::closeInterpreter));
    }
}
