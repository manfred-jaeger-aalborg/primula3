package edu.ucla.belief.ace;
import java.util.*;
import java.io.*;

/**
 * An interface to Ace compilation and evaluation for Primula.  Compilation
 * proceeds by encoding the network into CNF, compiling the CNF using c2d,
 * and then writing the CNF to a file.  Evaluation must first load the
 * compiled AC.  Afterward, one can perform inference by asserting evidence
 * and then looking up the answers for P(e) and marginals.  Asserting
 * evidence and looking up answers may be repeated any number of times.
 * 
 * @author chavira
 */

public interface AceInterfaceForPrimula {

  /**
   * An eumeration for different kinds of encoding methods.
   */
  
  public enum EncodingMethod {
    
    /**
     * The original encoding published in KR 2002.
     */
    
    DARWICHE_02,
    
    /**
     * The encoding developed by the guys at U. of Washington and published
     * in AAAI 2005.
     */
    
    SANG_BEAME_KAUTZ_05,
    
    /**
     * Our own IJCAI 2005 encoding.
     */
    
    CHAVIRA_DARWICHE_05,
    
    /**
     * Our own SAT 2006 encoding.  This is the one you should use most of the
     * time.
     */
    
    CHAVIRA_DARWICHE_06

  };
  
  /**
   * An enumeration for different kinds of dtree methods.
   */
  
  public enum DtreeMethod {
    
    /**
     * Construct dtree from bn dtree constructed from minfill elimination
     * order.  This is the one you should use most of the time.
     */
    
    BN_MINFILL,
    
    /**
     * Use hypergraph partitioning.  This option needs a count as well.
     */
    
    HYPERGRAPH,
    
    /**
     * Construct dtree using clause minfill.
     */
    
    CLAUSE_MINFILL
  
  };
  
  /**
   * Reads the network in the file with the given name and returns a handle
   * to the read network.
   * 
   * @param filename the given name.
   * @return the handle to the network.
   */
  
  Object readNetwork (String filename) throws Exception;;

  /**
   * Returns names of variables in the given network.
   * 
   * @param Object n the given network.
   * @return the names.
   */
  
  HashSet<String> networkVariables (Object n);
  
  /**
   * Computes an elimination order for the given network based on minfill and
   * returns the log base 2 of the maximum cluster size of this order.
   * 
   * @param r a random generator.
   * @param n the given network.
   * @retrun the log base 2 of the maximum cluster size.
   */
  
  double logMaxClusterSize (Random r, Object n) throws Exception;;
  
  /**
   * Runs AC compilation using the given command-line parameters and sends
   * output to the given output stream.  Files <outPrefix>.lmap and
   * <outPrefix>.ac will be created.
   *
   * @param r the random generator.
   * @param net the network as read in by readNetwork ().
   * @param instFile the evidence file name or null if no evidence.
   * @param em the encoding method.
   * @param dtm the dtree method.
   * @param dtCount if dtm indicates hypergraph partitioning, the count to
   *   use; otherwise, ignored.
   * @param retainFiles whether to retain intermediate files (for debugging).
   * @param c2dCompile whether to compile (for debugging).
   * @param outPrefix the prefix to use in generating files.
   * @param out the output stream to which to write status.
   */
  
  public void compile (
   Random r, Object net, String instFile, EncodingMethod em,
   DtreeMethod dtm, int dtCount, boolean retainFiles, boolean c2dCompile,
   String outPrefix, PrintStream out) throws Exception;
  
  /**
   * For the given network, reads an AC from the nnf file with the first
   * given name and the lmap file with the second given name and returns the
   * read AC.
   * 
   * @param n the given network.
   * @param nnfFilename the first given name.
   * @param lmapFilename the second given name.
   * @return the AC.
   */
  
  Object readAc (Object n, String nnfFilename, String lmapFilename)
   throws Exception;;
  
  /**
   * Asserts the given evidence into the given AC, which was constructed for
   * the given network.
   * 
   * @param r a random generator.
   * @param n the given network.
   * @praam ac the given ac.
   * @param e the evidence in the form of a map from variable name to integer
   *   value.
   */
  
  void assertEvidence (Random r, Object n, Object ac, Map<String,Integer> e)
   throws Exception;  

  /**
   * Returns the probability of the most recently asserted evidence for the
   * given ac, which was constructed for the given network.  Before calling
   * this method, evidence must have been asserted.
   * 
   * @param r a random generator.
   * @param n the given network.
   * @param ac the given ac.
   * @return the probability of evidence.
   */
  
  double probOfEvidence (Random r, Object n, Object ac) throws Exception;
  
  /**
   * Returns a map from variable name to double[], where each variable V is
   * mapped to P (V,e) for the evidence most recently asserted into the
   * given ac, which was constructed for the given network.  Before calling
   * this method, evidence must have been asserted.
   * 
   * @param n the given network.
   * @praam ac the given ac.
   * @return the map.
   */
  
  HashMap<String,double[]> marginals (Random r, Object n, Object ac)
   throws Exception;
  
}
