/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package MLNExceptions;

/**
 *
 * @author Alberto García Collado
 */
public class InvalidTypeArgumentsException extends Exception {

    /**
     * Creates a new instance of <code>InvalidTypeArgumentsException</code> without detail message.
     */
    public InvalidTypeArgumentsException() {
    }


    /**
     * Constructs an instance of <code>InvalidTypeArgumentsException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public InvalidTypeArgumentsException(String msg) {
        super(msg);
    }
}
