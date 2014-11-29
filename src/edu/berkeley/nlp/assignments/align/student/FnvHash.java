package edu.berkeley.nlp.assignments.align.student;

import static java.lang.System.out;

/**
 * computes FNV-1a hash
 * @author wmusial
 */
public class FnvHash {
    static final int init32 = 0x811c9dc5;
    static final int prime32 = 0x01000193;

    public static int hash(int data) {
        int hash = init32;
        for (int i = 0; i < 4; i++) {
            byte b = (byte)(data >>> ((3-i) * 8));
            //out.print(b + " ");
            hash = hash ^ (b & 0xff);
            hash = hash * prime32;
        }
        return hash & 0x7FFFFFFF;
    }

    public static int hash(long data) {
        int hash = init32;
        for (int i = 0; i < 8; i++) {
            byte b = (byte)(data >>> ((7-i) * 8));
            hash = hash ^ (b & 0xff);
            hash = hash * prime32;

        }
        return hash & 0x7FFFFFFF;
    }
}


