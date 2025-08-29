package phylospec.base.inference;

import org.phylospec.primitives.Primitive;
import org.phylospec.primitives.Real;
import org.phylospec.types.Simplex;

public class SimplexParam implements Parameter<Simplex> {


    @Override
    public Simplex getValue(int i) {
        return null;
    }

    @Override
    public Simplex getValue() {
        return null;
    }

    @Override
    public void setValue(int i, Simplex value) {

    }

    @Override
    public void setValue(Simplex value) {

    }

    @Override
    public Simplex getLower() {
        return null;
    }

    @Override
    public void setLower(Simplex lower) {

    }

    @Override
    public Simplex getUpper() {
        return null;
    }

    @Override
    public void setUpper(Simplex upper) {

    }

    @Override
    public Simplex[] getValues() {
        return new Simplex[0];
    }

    @Override
    public String getID() {
        return "";
    }

    @Override
    public int getMinorDimension1() {
        return 0;
    }

    @Override
    public int getMinorDimension2() {
        return 0;
    }

    @Override
    public void swap(int i, int j) {

    }

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public int[] shape() {
        return new int[0];
    }

    @Override
    public Object get(int... idx) {
        return null;
    }

    @Override
    public Primitive primitiveType() {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }
}
