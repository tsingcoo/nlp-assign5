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
public class HmmMetaWordAligner extends MetaWordAligner implements WordAligner {

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


    @Override
    public Alignment alignSentencePair(SentencePair sentencePair) {

        Alignment known = punctuationAlignment(sentencePair);
        Pair<Alignment, Alignment> pair;

        // null + exclusive
        pair = intersectionHmmAlignment(sentencePair, known, 0.05, true);
        known = exclusiveIntersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, 0.01, true);
        known = exclusiveIntersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, 0.05, true);
        known = intersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, 0.01, true);
        known = intersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, 0, true);
        known = intersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, 0.05, false);
        known = intersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, 0.01, false);
        known = intersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, 0.0, false);
        known = intersect(pair.getFirst(), pair.getSecond());

        return known;
    }

    private Pair<Alignment, Alignment> intersectionHmmAlignment(SentencePair sentencePair, Alignment known_alignment, double null_probability, boolean exclusive) {
        Alignment ef;
        Alignment fe;

        ef = e_to_f.alignSentencePair(sentencePair, known_alignment, null_probability, exclusive);

        this.swapLanguages(sentencePair);
        fe = f_to_e.alignSentencePair(sentencePair, known_alignment.getReverseCopy(), null_probability, exclusive);
        this.swapLanguages(sentencePair);

        fe = fe.getReverseCopy();

        return new Pair<Alignment, Alignment>(ef, fe);
    }



}
