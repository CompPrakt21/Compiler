package compiler.utils;

import java.util.*;

public class BiMap<L, R> {

    private final Map<L, R> leftToRight;
    private final Map<R, L> rightToLeft;

    public BiMap() {
        this.leftToRight = new HashMap<>();
        this.rightToLeft = new HashMap<>();
    }

    public R getRight(L key) {
        return this.leftToRight.get(key);
    }

    public L getLeft(R key) {
        return this.rightToLeft.get(key);
    }

    public void put(L l, R r) {
        var currentR = this.leftToRight.get(l);
        if (currentR != null) {
            this.rightToLeft.remove(currentR);
        }

        var currentL = this.rightToLeft.get(r);
        if (currentL != null) {
            this.leftToRight.remove(currentL);
        }

        this.leftToRight.put(l, r);
        this.rightToLeft.put(r, l);
    }

    public Optional<R> removeLeft(L l) {
        var removedR = this.leftToRight.remove(l);

        if (removedR == null) {
            return Optional.empty();
        }

        var removedL = this.rightToLeft.remove(removedR);

        assert l == removedL;

        return Optional.of(removedR);
    }

    public Optional<L> removeRight(R r) {
        var removedL = this.rightToLeft.remove(r);

        if (removedL == null) {
            return Optional.empty();
        }

        var removedR = this.leftToRight.remove(removedL);

        assert r == removedR;

        return Optional.of(removedL);
    }

    public Collection<L> leftSet() {
        return this.leftToRight.keySet();
    }

    public Collection<R> rightSet() {
        return this.rightToLeft.keySet();
    }

    public Collection<Map.Entry<L, R>> entrySet(){
        return this.leftToRight.entrySet();
    }

    public void clear() {
        this.leftToRight.clear();
        this.rightToLeft.clear();
    }
}
