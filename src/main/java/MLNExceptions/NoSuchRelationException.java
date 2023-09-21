/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package MLNExceptions;

/**
 *
 * @author Alberto García Collado
 */
public class NoSuchRelationException extends Exception {

    /**
     * Creates a new instance of <code>NoSuchRelationException</code> without detail message.
     */
    public NoSuchRelationException() {
    }


    /**
     * Constructs an instance of <code>NoSuchRelationException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NoSuchRelationException(String msg) {
        super(msg);
    }
}
