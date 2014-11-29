package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.mt.Alignment;
import edu.berkeley.nlp.mt.SentencePair;
import edu.berkeley.nlp.mt.WordAligner;
import edu.berkeley.nlp.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wmusial on 11/28/14.
 */
public class HmmMetaWordAligner implements WordAligner {

    HmmWordAligner e_to_f;
    HmmWordAligner f_to_e;


    public HmmMetaWordAligner(Iterable<SentencePair> training_data) {

        Model1WordAligner m1ef = new Model1WordAligner(training_data);
        //e_to_f = new HmmWordAligner(training_data);
        e_to_f = new HmmWordAligner(training_data, m1ef.indexer_e, m1ef.indexer_f, m1ef.p);



        // swap data
        for (SentencePair sentencePair : training_data) {
            this.swapLanguages(sentencePair);
        }
        Model1WordAligner m1fe = new Model1WordAligner(training_data);
        f_to_e = new HmmWordAligner(training_data, m1fe.indexer_e, m1fe.indexer_f, m1fe.p);

        // un-swap for good measure
        for (SentencePair sentencePair : training_data) {
            this.swapLanguages(sentencePair);
        }

    }

    private void swapLanguages(SentencePair sentencePair) {
        List<String> tmp = sentencePair.englishWords;
        sentencePair.englishWords = sentencePair.frenchWords;
        sentencePair.frenchWords = tmp;
    }


    @Override
    public Alignment alignSentencePair(SentencePair sentencePair) {

        Alignment exclusive = punctuationAlignment(sentencePair);

        Alignment ef = null;
        Alignment fe = null;
        for (int i = 0; i < 4; i++) {
            ef = e_to_f.alignSentencePair(sentencePair, exclusive);

            this.swapLanguages(sentencePair);
            fe = f_to_e.alignSentencePair(sentencePair, exclusive.getReverseCopy());
            this.swapLanguages(sentencePair);

            fe = fe.getReverseCopy();

            if (i < 2) {
                exclusive = this.exclusiveIntersect(ef, fe);
            }
            else {
                exclusive = this.intersect(ef, fe);
            }
        }

        //return e_to_f.alignSentencePair(sentencePair, exclusive);




        return this.intersect(ef, fe);
    }

    private Alignment punctuationAlignment(SentencePair sentencePair) {
        int ne = sentencePair.englishWords.size();
        int nf = sentencePair.frenchWords.size();
        Alignment exclusive = new Alignment();
        exclusive.addAlignment(0, 0, true);
        if (sentencePair.englishWords.get(ne-1) == sentencePair.frenchWords.get(nf-1)) {
            exclusive.addAlignment(ne-1, nf-1, true);
        }
        return exclusive;
    }



    private Alignment intersect(Alignment a, Alignment b) {

        Alignment ab = new Alignment();
        for (Pair<Integer, Integer> pair : a.getSureAlignments()) {
            if (b.containsSureAlignment(pair.getFirst(), pair.getSecond())) {
                ab.addAlignment(pair.getFirst(), pair.getSecond(), true);
            }
        }
        return ab;
    }



    private Alignment exclusiveIntersect(Alignment a, Alignment b) {
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
