package PyManager;

import RBNpackage.CPModel;
import RBNpackage.VarTermPackage.ArgTerm;

import java.util.*;

public class TypedTorchPf {
    Hashtable<String,List> typedCombine;

    public TypedTorchPf() {
        typedCombine = new Hashtable<>();
    }

    public void addCombine(String name, List<TorchInputPf> combines) {
        if (typedCombine.containsKey(name)) {
            typedCombine.get(name).addAll(combines);
        }
        else {
            typedCombine.put(name,new ArrayList<>(combines));
        }
    }

    public int numTypes() {
        return typedCombine.size();
    }

    public int numCombines(String name) {
        if (typedCombine.containsKey(name))
            return typedCombine.get(name).size();
        else
            return 0;
    }

    public List<String> getTypes() {
        return Collections.unmodifiableList(new ArrayList<>(typedCombine.keySet()));
    }

    public List<TorchInputPf> getCombines() {
        List<TorchInputPf> result = new ArrayList<>();
        for (List<TorchInputPf> tipList: typedCombine.values()) {
            result.addAll(tipList);
        }
        return result;
    }

    public List<TorchInputPf> getCombines(String name) {
        if (typedCombine.containsKey(name)) {
            return Collections.unmodifiableList(typedCombine.get(name));
        }
        else {
            return Collections.emptyList();
        }
    }

    public TypedTorchPf substitute(String[] vars, int[] args) {
        TypedTorchPf newpf = new TypedTorchPf();
        for (String type: this.getTypedNames()) {
            List<TorchInputPf> newtipList = new ArrayList<>();
            for (TorchInputPf tip: this.getCombines(type)) {
                TorchInputPf newtip = tip.substitute(vars, args);
                newtipList.add(newtip);
            }
            newpf.addCombine(type, newtipList);
        }
        return newpf;
    }

    public TypedTorchPf substitute(String[] vars, String[] args) {
        TypedTorchPf newpf = new TypedTorchPf();
        for (String type: this.getTypedNames()) {
            List<TorchInputPf> newtipList = new ArrayList<>();
            for (TorchInputPf tip: this.getCombines(type)) {
                TorchInputPf newtip = tip.substitute(vars, args);
                newtipList.add(newtip);
            }
            newpf.addCombine(type, newtipList);
        }
        return newpf;
    }

    public TypedTorchPf substitute(String[] vars, ArgTerm[] args) {
        TypedTorchPf newpf = new TypedTorchPf();
        for (String type: this.getTypedNames()) {
            List<TorchInputPf> newtipList = new ArrayList<>();
            for (TorchInputPf tip: this.getCombines(type)) {
                TorchInputPf newtip = tip.substitute(vars, args);
                newtipList.add(newtip);
            }
            newpf.addCombine(type, newtipList);
        }
        return newpf;
    }

    public TypedTorchPf substitute(ArgTerm[] vars, int[] args) {
        TypedTorchPf newpf = new TypedTorchPf();
        for (String type: this.getTypedNames()) {
            List<TorchInputPf> newtipList = new ArrayList<>();
            for (TorchInputPf tip: this.getCombines(type)) {
                TorchInputPf newtip = tip.substitute(vars, args);
                newtipList.add(newtip);
            }
            newpf.addCombine(type, newtipList);
        }
        return newpf;
    }

    public TypedTorchPf substitute(ArgTerm[] vars, ArgTerm[] args) {
        TypedTorchPf newpf = new TypedTorchPf();
        for (String type: this.getTypedNames()) {
            List<TorchInputPf> newtipList = new ArrayList<>();
            for (TorchInputPf tip: this.getCombines(type)) {
                TorchInputPf newtip = tip.substitute(vars, args);
                newtipList.add(newtip);
            }
            newpf.addCombine(type, newtipList);
        }
        return newpf;
    }

    public List<String> getTypedNames() {
        return Collections.unmodifiableList(new ArrayList<>(typedCombine.keySet()));
    }

    public boolean containsName(String name) {
        return typedCombine.containsKey(name);
    }

    @Override
    public int hashCode() {
        int result = 1;

        List<String> sortedKeys = new ArrayList<>(typedCombine.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            result = 31 * result + key.hashCode();

            List<TorchInputPf> list = typedCombine.get(key);
            result = 31 * result + (list != null ? list.hashCode() : 0);
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypedTorchPf)) return false;

        TypedTorchPf that = (TypedTorchPf) o;

        if (this.typedCombine.size() != that.typedCombine.size()) return false;

        for (String key : this.typedCombine.keySet()) {
            if (!that.typedCombine.containsKey(key)) return false;

            List<TorchInputPf> thisList = this.typedCombine.get(key);
            List<TorchInputPf> thatList = that.typedCombine.get(key);

            if (!Objects.equals(thisList, thatList)) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "TypedTorchPf{" +
                "typedCombine=" + typedCombine.toString() +
                '}';
    }
}

