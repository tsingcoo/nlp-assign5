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

        for (String word : this.word_count_e.keySet()) {
            System.out.println(word + "   " + word_count_e.get(word));
        }

        System.out.println("english words: " + word_count_e.size());
        System.out.println("french words: " + word_count_f.size());
        System.exit(0);
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
        Alignment res = new Alignment();

        //List<Integer> punctuation = new Vector<Integer>();



        Set<Integer> used_f = new HashSet<Integer>();
        Set<Integer> used_e = new HashSet<Integer>();

        int ne = sentencePair.englishWords.size();
        int nf = sentencePair.frenchWords.size();

        if (sentencePair.englishWords.get(ne-1) == "." && sentencePair.frenchWords.get(nf-1) == ".") {
            used_e.add(ne-1);
            used_f.add(nf-1);
            res.addAlignment(ne-1, nf-1, true);
        }

        SimpleMatrix scores = new SimpleMatrix(sentencePair.englishWords.size(), sentencePair.frenchWords.size());

        for (int ei = 0; ei < sentencePair.englishWords.size(); ei++) {
            String word_e = sentencePair.englishWords.get(ei);
            for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {
                String word_f = sentencePair.frenchWords.get(fi);
                Pair<String, String> key = new Pair<String, String>(word_e, word_f);
                if (word_count_e.containsKey(word_e) && word_count_f.containsKey(word_f) && word_count_c.containsKey(key)) {
                    double score = (double) (word_count_c.get(key) * word_count_c.get(key)) / (word_count_e.get(word_e) * word_count_f.get(word_f));
                    scores.set(ei, fi, score);
                }
                else {
                    scores.set(ei, fi, 0);
                }
            }
        }

        (scores.elementLog().plus(20)).print("%4.1f");



        while (true) {
            PairFactory pf;


            Set<Integer>[] french_for_best_english = new Set[sentencePair.englishWords.size()];
            Set<Integer>[] english_for_best_french = new Set[sentencePair.frenchWords.size()];
            int[] best_english_for_french = new int[sentencePair.frenchWords.size()];
            int[] best_french_for_english = new int[sentencePair.englishWords.size()];

            for (int ei = 0; ei < sentencePair.englishWords.size(); ei++) {
                french_for_best_english[ei] = new HashSet<Integer>();
            }
            for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {
                english_for_best_french[fi] = new HashSet<Integer>();
            }



            pf = new PairFactory.ReversingPairFactory();
            for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {
                if (used_f.contains(fi)) {
                    continue;
                }

                //scores.extractMatrix(fi, fi, 0, ne-1).printDimensions();
                String word_f = sentencePair.frenchWords.get(fi);

                int max_ei = this.getMaximumProbability(pf, word_f, sentencePair.englishWords, used_e, word_count_f, word_count_e, word_count_c);

                if (max_ei >= 0) {
                    best_english_for_french[fi] = max_ei;
                    french_for_best_english[max_ei].add(fi);

                }
            }


            int added = 0;
            pf = new PairFactory.NaturalPairFactory();
            for (int ei = 0; ei < sentencePair.englishWords.size(); ei++) {
                if (used_e.contains(ei)) {
                    continue;
                }

                String word_e = sentencePair.englishWords.get(ei);

                int max_fi = this.getMaximumProbability(pf, word_e, sentencePair.frenchWords, used_f, word_count_e, word_count_f, word_count_c);

                if (max_fi >= 0) {
                    if (french_for_best_english[ei].contains(max_fi)) {
                        res.addAlignment(ei, max_fi, true);
                        used_e.add(ei);
                        used_f.add(max_fi);
                        added += 1;
                    }

                }
            }

            if (added == 0) {
                break;
            }
        }

        /*
        PairFactory pf;

        pf = new PairFactory.ReversingPairFactory();
        for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {
            if (used_f.contains(fi)) {
                continue;
            }
            String word_f = sentencePair.frenchWords.get(fi);

            int max_ei = this.getMaximumProbability(pf, word_f, sentencePair.englishWords, used_e, word_count_f, word_count_e, word_count_c);

            if (max_ei >= 0) {

                //System.out.print("!  ");
                res.addAlignment(max_ei, fi, false);
            }
            //System.out.println("french: " + word_f + "   english: " + sentencePair.englishWords.get(max_ei));
        }



        pf = new PairFactory.NaturalPairFactory();
        for (int ei = 0; ei < sentencePair.englishWords.size(); ei++) {
            if (used_e.contains(ei)) {
                continue;
            }
            String word_e = sentencePair.englishWords.get(ei);

            int max_fi = this.getMaximumProbability(pf, word_e, sentencePair.frenchWords, used_f, word_count_e, word_count_f, word_count_c);

            if (max_fi >= 0) {

                //System.out.print("!  ");
                res.addAlignment(ei, max_fi, false);
            }
            //System.out.println("french: " + word_f + "   english: " + sentencePair.englishWords.get(max_ei));
        }
        */


        return res;
    }

    private int getMaximumProbability(PairFactory pf, String word_a, List<String> words_b, Set<Integer> mask_b, Map<String, Integer> word_count_a, Map<String, Integer> word_count_b, Map<Pair<String, String>, Integer> word_count_c) {
        double max_score = Double.NEGATIVE_INFINITY;
        int max_bi = -1;
        for (int bi = 0; bi < words_b.size(); bi++) {
            if (mask_b.contains(bi)) {
                continue;
            }

            String word_b = words_b.get(bi);

            Pair<String, String> key = pf.makePair(word_a, word_b);
            if (word_count_a.containsKey(word_a) && word_count_b.containsKey(word_b) && word_count_c.containsKey(key)) {
                double score = (double) (word_count_c.get(key) * word_count_c.get(key)) / (word_count_a.get(word_a) * word_count_b.get(word_b));

                if (score == max_score) {
                    max_bi = -1;
                }
                if (score > max_score) {
                    max_score = score;
                    max_bi = bi;
                }

            }
        }
        return max_bi;
    }


    /*

    private Map<String, Integer> sentenceToPositionMap(List<String> sentence) {
        Map<String, Integer> res = new HashMap<String, Integer>();
        int i = 0;
        for (String word : sentence) {
            //res.put(word)
        }
    }
    */
}
