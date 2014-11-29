package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.util.Pair;

import java.util.AbstractMap;
import java.util.Iterator;

import static java.lang.System.out;

/**
 * Hashmap of long-int key-value pairs with open addressing.
 * Defaults to 0.
 * @author wmusial
 */
public class IntDoubleExpandableMap  {


    private class IntDoubleMapIterator implements Iterator<Pair<Integer, Double>> {
        private IntDoubleExpandableMap m;
        private int i;
        private IntDoubleMapIterator(IntDoubleExpandableMap m)
        {
            this.m = m;
            i = -1;
            i = find_next();
        }

        private int find_next()
        {
            i++;
            while (i < m.keys.length)
            {
                if (m.keys[i] > 0)
                    break;
                i++;
            }
            return i;
        }

        @Override
        public boolean hasNext() {
            return i < m.keys.length;
        }

        @Override
        public Pair<Integer, Double> next() {
            Pair<Integer, Double> r = Pair.makePair(keys[i], data[0][i]);
            i = find_next();
            return r;
        }
    }

    private class IntDoubleMapKeyInterator implements Iterator<Integer> {
        private int i;
        private IntDoubleExpandableMap m;
        private IntDoubleMapKeyInterator(IntDoubleExpandableMap m) {
            this.m = m;
            i = -1;
            i = find_next();
        }

        private int find_next() {
            i++;
            while (i < m.keys.length) {
                if (m.keys[i] > 0)
                    break;
                i++;
            }
            return i;
        }


        @Override
        public boolean hasNext() {
            return i < m.keys.length;
        }

        @Override
        public Integer next() {
            int r = keys[i];
            i = find_next();
            return r;
        }
    }




    int[] keys;
    double[][] data;
    int count = 0;
    private float fill_ratio = 0.8f;
    private float expand_ratio = 1.2f;
    private int data_count = 1;


    public IntDoubleExpandableMap(int initial_size, int data_count, float fill_ratio, float expand_ratio)
    {
        assert data_count >= 1;
        this.fill_ratio = fill_ratio;
        this.expand_ratio = expand_ratio;
        this.data_count = data_count;
        keys = new int[initial_size];
        data = new double[data_count][initial_size];
    }





    public double get(int key, int k) {
        int i = getIndexOfKey(key);
        if (keys[i] == 0) {
            System.out.println("requested key: " + key);
            throw new ArrayIndexOutOfBoundsException();
        }
        return data[k][i];
    }

    public boolean containsKey(int key) {
        int i = getIndexOfKey(key);
        return keys[i] != 0;
    }

    public void put(int key, double value, int k) {
        int i = getIndexOfKey(key);
        this.insertKeyAtIndex(i, key);
        this.setAtHashId(i, k, value);
    }

    /**
     * retrieve value at key, or set if not found.
     * By combining the two operations, we end up doing
     * only one hash lookup.
     */

    public int getHashId(int key)
    {
        return getHashId(key, true);
    }

    public int getHashId(int key, boolean insert)
    {
        int i = getIndexOfKey(key);
        if (keys[i] == 0) // key not found
        {
            if (!insert)
                return -1;
            i = insertKeyAtIndex(i, key);
        }
        return i;
    }

    public void setAtHashId(int i, int k, double value)
    {
        data[k][i] = value;
    }

    public double getAtHashId(int i, int k)
    {
        return data[k][i];
    }

    private int getIndexOfKey(int key)
    {
        assert key > 0;
        //if (key == 0)
        //    throw new IllegalArgumentException();

        // a little sloppy... hash key first (64bit)
        int hash = FnvHash.hash(key);
        int i = hash % keys.length;
        if (keys[i] == 0 || keys[i] == key)
        {
            return i;
        }
        else
        {
            // then hash the hash (32bit)
            int c = 0;
            while (keys[i] != 0 && keys[i] != key) {
                hash = FnvHash.hash(hash);
                i = hash % keys.length;
                c++;
            }
            //out.println("resolved with " + c + " hits");
            return i;
        }
    }

    private int insertKeyAtIndex(int i, int key)
    {
        if (keys[i] == key) {
            return i;
        }
        assert keys[i] == 0;
        keys[i] = key;
        count++;
        if (count >= fill_ratio * keys.length) {
            enlarge();
            return getIndexOfKey(key);
        }
        else
            return i;
    }

    private void enlarge()
    {
        int[] old_keys = keys;

        //out.println("enlarge");

        int new_length = (int)((keys.length) * expand_ratio);

        keys = new int[new_length];
        count = 0;

        int key;
        for (int i = 0; i < old_keys.length; i++)
        {

            key = old_keys[i];
            if (key != 0)
            {
                int id = getIndexOfKey(key);
                id = insertKeyAtIndex(id, key);
                old_keys[i] = id;
            }
            else
            {
                old_keys[i] = -1;
            }
        }

        for (int k = 0; k < this.data_count; k++)
        {
            double[] old_data = data[k];
            data[k] = new double[new_length];
            for (int i = 0; i < old_keys.length; i++)
            {
                if (old_keys[i] != -1)
                {
                    this.setAtHashId((int)old_keys[i], k, old_data[i]);
                }
            }
        }
    }

    public void hashStats()
    {
        int collisions = 0;
        for (int k = 0; k < keys.length; k++)
        {
            if (keys[k] != 0)
            {
                int key = keys[k];
                int hash = FnvHash.hash(key);
                int i = hash % keys.length;
                if (keys[i] == 0 || keys[i] == key)
                {

                }
                else
                {
                    // then hash the hash (32bit)

                    int c = 0;
                    while (keys[i] != 0 && keys[i] != key) {
                        hash = FnvHash.hash(hash);
                        i = hash % keys.length;
                        c++;
                    }
                    collisions += c;
                }

            }
        }
        out.println("hash stats. length: " + count + "/" + keys.length + "=" + (double)count/keys.length + "full, collisions=" + (double)collisions / count + "per entry.");

    }

    public IntDoubleMapIterator iterator()
    {
        return new IntDoubleMapIterator(this);
    }



    public Iterator<Integer> keyInterator() {
        return new IntDoubleMapKeyInterator(this);
    }


}
