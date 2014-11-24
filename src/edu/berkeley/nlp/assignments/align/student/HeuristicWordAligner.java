package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.mt.Alignment;
import edu.berkeley.nlp.mt.SentencePair;
import edu.berkeley.nlp.mt.WordAligner;
import edu.berkeley.nlp.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<Pair<String, String>, Double> scores = new HashMap<Pair<String, String>, Double>();


        for (String word_e : sentencePair.englishWords) {
            for (String word_f : sentencePair.frenchWords) {
                Pair<String, String> key = new Pair<String, String>(word_e, word_f);
                double score = 0;
                if (this.word_count_e.containsKey(word_e) && this.word_count_f.containsKey(word_f) && this.word_count_c.containsKey(key)) {
                    score = (double)(this.word_count_c.get(key)) / Math.sqrt(this.word_count_e.get(word_e) * this.word_count_f.get(word_f));

                }
                scores.put(key, score);

            }


        }
        for (Pair<String, String> keyy : scores.keySet()) {
            System.out.println(keyy + "  " + scores.get(keyy));
        }


        return null;
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
