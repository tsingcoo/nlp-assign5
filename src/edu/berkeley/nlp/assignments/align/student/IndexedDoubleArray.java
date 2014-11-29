package edu.berkeley.nlp.assignments.align.student;

/**
 * Created by wmusial on 11/28/14.
 */
public class IndexedDoubleArray {
     double[] data;
    private int size;
    public IndexedDoubleArray(int size) {
        this.size = size;
        this.data = new double[size*2+1];
    }

    public double get(int i) {
        return this.data[size+i];
    }

    public void set(int i, double value) {
        this.data[size+i] = value;
    }
}
