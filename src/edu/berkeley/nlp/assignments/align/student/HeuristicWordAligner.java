package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.mt.Alignment;
import edu.berkeley.nlp.mt.SentencePair;
import edu.berkeley.nlp.mt.WordAligner;
import edu.berkeley.nlp.util.Pair;
import org.ejml.simple.SimpleMatrix;

import java.util.*;

/**
 * Created by wmusial on 11/23/14.
 */
public class HeuristicWordAligner implements WordAligner {
    private Map<String, Integer> word_count_f;
    private Map<String, Integer> word_count_e;
    private Map<Pair<String, String>, Integer> word_count_c;

    public  HeuristicWordAligner(Iterable<SentencePair> trainingData) {
        word_count_f = new HashMap<String, Integer>();
        word_count_e = new HashMap<String, Integer>();
        word_count_c = new HashMap<Pair<String, String>, Integer>();


        int i = 0;
        for (SentencePair pair : trainingData) {
            i += 1;
            //System.out.println("pair: \n" + pair.toString());

            this.count_words(this.word_count_e, pair.englishWords);
            this.count_words(this.word_count_f, pair.frenchWords);
            this.count_cross_words(this.word_count_c, pair.englishWords, pair.frenchWords);
        }
        System.out.println("creating heuristic word aligner with " + i + " training pairs");

        System.out.println("english words: " + word_count_e.size());
        System.out.println("french words: " + word_count_f.size());

    }

    private void count_cross_words(Map<Pair<String, String>, Integer> map, List<String> words_e, List<String> words_f) {
        for (String word_e : words_e) {
            for (String word_f : words_f) {
                Pair<String, String> key = new Pair<String, String>(word_e, word_f);
                if (!map.containsKey(key)) {
                    map.put(key, 0);
                }
                map.put(key, map.get(key) + 1);
            }
        }
    }

    private void count_words(Map<String, Integer> map, List<String> words) {
        for (String s : words) {
            if (!map.containsKey(s)) {
                map.put(s, 0);
            }
            map.put(s, map.get(s) + 1);
        }
    }


    @Override
    public Alignment alignSentencePair(SentencePair sentencePair) {
        return alignSentencePair(sentencePair, new Alignment(), false);
    }

    public Alignment alignSentencePair(SentencePair sentencePair, Alignment known_alignment, boolean exclusive) {
        Alignment res = new Alignment();

        int ne = sentencePair.englishWords.size();
        int nf = sentencePair.frenchWords.size();

        PairFactory pf;

        int[] alignments = new int[nf];
        Arrays.fill(alignments, -1);
        Set<Integer> used_e = new HashSet<Integer>();
        for (Pair<Integer, Integer> amt : known_alignment.getSureAlignments()) {
            used_e.add(amt.getFirst());
            alignments[amt.getSecond()] = amt.getFirst();
        }

        pf = new PairFactory.ReversingPairFactory();
        for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {

            String word_f = sentencePair.frenchWords.get(fi);

            int max_ei = -1;
            {
                double max_score = Double.NEGATIVE_INFINITY;

                for (int ei = 0; ei < sentencePair.englishWords.size(); ei++) {
                    if (exclusive && used_e.contains(ei) && ei != alignments[fi]) {
                        //System.out.println("skipping word_f: " + word_f + " and word_e " + word_e);
                        continue;
                    }
                    if (alignments[fi] >= 0 && alignments[fi] != ei ) {
                        continue;
                    }
                    String word_e = sentencePair.englishWords.get(ei);
                    Pair<String, String> key = pf.makePair(word_e, word_f);
                    if (word_count_f.containsKey(word_f) && word_count_e.containsKey(word_e) && word_count_c.containsKey(key)) {
                        double score = (double) (word_count_c.get(key)) / (word_count_f.get(word_f) * word_count_e.get(word_e));

                        if (score > max_score) {
                            max_score = score;
                            max_ei = ei;
                        }
                    }
                }
            }

            if (max_ei >= 0) {
                res.addAlignment(max_ei, fi, true);
            }
        }


        return res;
    }

}
