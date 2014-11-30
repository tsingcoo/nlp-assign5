package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.mt.Alignment;
import edu.berkeley.nlp.mt.SentencePair;
import edu.berkeley.nlp.mt.WordAligner;
import edu.berkeley.nlp.util.Pair;

import java.util.List;

/**
 * Created by wmusial on 11/29/14.
 */
public abstract class MetaWordAligner implements WordAligner{

    protected void swapLanguages(SentencePair sentencePair) {
        List<String> tmp = sentencePair.englishWords;
        sentencePair.englishWords = sentencePair.frenchWords;
        sentencePair.frenchWords = tmp;
    }

    protected Alignment punctuationAlignment(SentencePair sentencePair) {
        int ne = sentencePair.englishWords.size();
        int nf = sentencePair.frenchWords.size();
        Alignment exclusive = new Alignment();
        exclusive.addAlignment(0, 0, true);
        if (sentencePair.englishWords.get(ne-1) == sentencePair.frenchWords.get(nf-1)) {
            exclusive.addAlignment(ne-1, nf-1, true);
        }
        return exclusive;
    }

    protected Alignment intersect(Alignment a, Alignment b) {

        Alignment ab = new Alignment();
        for (Pair<Integer, Integer> pair : a.getSureAlignments()) {
            if (b.containsSureAlignment(pair.getFirst(), pair.getSecond())) {
                ab.addAlignment(pair.getFirst(), pair.getSecond(), true);
            }
        }
        return ab;
    }

    protected Alignment exclusiveIntersect(Alignment a, Alignment b) {
        return intersect(removeManyToOne(a), removeManyToOne(b));
    }

    private Alignment removeManyToOne(Alignment a) {

        int max_e = -1;
        int max_f = -1;
        for (Pair<Integer, Integer> pair : a.getSureAlignments()) {
            if (pair.getFirst() > max_e)
                max_e = pair.getFirst();
            if (pair.getSecond() > max_f)
                max_f = pair.getSecond();
        }

        int[] counts_e = new int[max_e+1];
        int[] counts_f = new int[max_f+1];

        for (Pair<Integer, Integer> pair : a.getSureAlignments()) {

            counts_e[pair.getFirst()] += 1;
            counts_f[pair.getSecond()] += 1;
        }

        Alignment res = new Alignment();
        for (Pair<Integer, Integer> pair : a.getSureAlignments()) {
            assert counts_e[pair.getFirst()] > 0;
            assert counts_f[pair.getSecond()] > 0;
            if (counts_e[pair.getFirst()] == 1 && counts_f[pair.getSecond()] == 1) {
                res.addAlignment(pair.getFirst(), pair.getSecond(), true);
            }
        }
        return res;
    }
}
