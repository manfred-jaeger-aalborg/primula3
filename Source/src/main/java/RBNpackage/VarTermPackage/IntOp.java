package RBNpackage.VarTermPackage;

import java.util.Set;
// abstract class for int operations in arg
public abstract class IntOp extends ArgTerm {
    ArgTerm left;
    ArgTerm right;
    public abstract Set<int[]> getAllTrue(int tmin, int tmax);

    public ArgTerm getLeft() {
        return left;
    }
    public ArgTerm getRight() {
        return right;
    }
}
