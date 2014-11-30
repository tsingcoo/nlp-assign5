package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.mt.Alignment;
import edu.berkeley.nlp.mt.SentencePair;
import edu.berkeley.nlp.mt.WordAligner;
import edu.berkeley.nlp.util.Pair;

/**
 * Created by wmusial on 11/29/14.
 */
public class HeuristicMetaWordAligner extends MetaWordAligner implements WordAligner {

    HeuristicWordAligner e_to_f;
    HeuristicWordAligner f_to_e;

    public HeuristicMetaWordAligner(Iterable<SentencePair> training_data) {
        e_to_f = new HeuristicWordAligner(training_data);
        // swap data
        for (SentencePair sentencePair : training_data) {
            this.swapLanguages(sentencePair);
        }
        f_to_e = new HeuristicWordAligner(training_data);

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
        pair = intersectionHmmAlignment(sentencePair, known, true);
        known = exclusiveIntersect(pair.getFirst(), pair.getSecond());


        pair = intersectionHmmAlignment(sentencePair, known, true);
        known = exclusiveIntersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, true);
        known = intersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, true);
        known = intersect(pair.getFirst(), pair.getSecond());

        pair = intersectionHmmAlignment(sentencePair, known, false);
        known = intersect(pair.getFirst(), pair.getSecond());
        return known;
    }

    private Pair<Alignment, Alignment> intersectionHmmAlignment(SentencePair sentencePair, Alignment known_alignment, boolean exclusive) {
        Alignment ef;
        Alignment fe;

        ef = e_to_f.alignSentencePair(sentencePair, known_alignment, exclusive);

        this.swapLanguages(sentencePair);
        fe = f_to_e.alignSentencePair(sentencePair, known_alignment.getReverseCopy(), exclusive);
        this.swapLanguages(sentencePair);

        fe = fe.getReverseCopy();

        return new Pair<Alignment, Alignment>(ef, fe);
    }
}
