package edu.berkeley.nlp.assignments.align.student;


import edu.berkeley.nlp.util.Pair;

/**
 * Created by wmusial on 11/24/14.
 */
public interface PairFactory {
    Pair<String, String> makePair(String word_a, String word_b);

    public class NaturalPairFactory implements PairFactory {

        @Override
        public Pair<String, String> makePair(String word_a, String word_b) {
            return new Pair<String, String>(word_a, word_b);
        }
    }

    public class ReversingPairFactory implements PairFactory {

        @Override
        public Pair<String, String> makePair(String word_a, String word_b) {
            return new Pair<String, String>(word_b, word_a);
        }
    }
}
