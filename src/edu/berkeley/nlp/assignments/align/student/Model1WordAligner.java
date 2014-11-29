package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.mt.Alignment;
import edu.berkeley.nlp.mt.SentencePair;
import edu.berkeley.nlp.mt.WordAligner;
import edu.berkeley.nlp.util.Pair;
import edu.berkeley.nlp.util.StringIndexer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by wmusial on 11/25/14.
 */
public class Model1WordAligner implements WordAligner {

    //private Map<Integer, Double> p;
    IntDoubleExpandableMap p;

    StringIndexer indexer_e;
    StringIndexer indexer_f;

    public Model1WordAligner(Iterable<SentencePair> training_data) {

        this.indexer_f = new StringIndexer();
        this.indexer_e = new StringIndexer();
        this.indexer_e.addAndGetIndex("voitek");
        this.indexer_f.addAndGetIndex("voitek");

        // discover how many english words there are
        for (SentencePair sentencePair : training_data) {
            for (String word_e : sentencePair.englishWords) {
                indexer_e.addAndGetIndex(word_e);
            }
            for (String word_f : sentencePair.frenchWords) {
                indexer_f.addAndGetIndex(word_f);
            }
        }

        System.out.println("english words: " + indexer_e.size());
        System.out.println("french words: " + indexer_f.size());

        if (indexer_e.size() > 65536 || indexer_f.size() > 65536) {
            throw new RuntimeException("too many words in training data");
        }

        System.out.println("chamber: " + indexer_e.addAndGetIndex("chamber"));
        System.out.println("bisous:  " + indexer_f.addAndGetIndex("bisous"));

        /*
        // check that packaging works correctly
        int r = this.packageWordPair("chamber", "bisous");
        System.out.println(r);
        System.out.println(r & 0xFFFF);
        System.out.println((r & 0xFFFF0000) >> 16);
        */


        //p = new HashMap<Integer, Double>();
        // p doubles as the probability of f|e, and the count
        p = new IntDoubleExpandableMap(2000000, 2, 0.8f, 1.25f);
        // initialize
        for (SentencePair sentencePair : training_data) {
            for (String word_e : sentencePair.englishWords) {
                for (String word_f : sentencePair.frenchWords) {
                    int key = this.packageWordPair(word_e, word_f);
                    p.put(key, 1, 0);
                }
            }
        }
        System.out.println("initialized hashtables. ");
        System.out.println("hashtable size: " + p.count);
        int num_iterations = 5;
        for (int i = 0; i < num_iterations; i++) {
            System.out.println("iteration " + (i+1) + " out of " + num_iterations);
            // repeat
            //Map<Integer, Double> count = new HashMap<Integer, Double>();
            //Map<String, Double> total_count = new HashMap<String, Double>();

            double[] total_count = new double[this.indexer_e.size()];

            // initialize
            // reset ''count''
            Arrays.fill(total_count, 0);
            Arrays.fill(p.data[1], 0);

            for (SentencePair sentencePair : training_data) {


                double[] total_s = new double[sentencePair.frenchWords.size()];
                for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {
                    String word_f = sentencePair.frenchWords.get(fi);
                    for (String word_e : sentencePair.englishWords) {
                        int key = this.packageWordPair(word_e, word_f);
                        total_s[fi] += p.get(key, 0);
                    }
                }

                for (String word_e : sentencePair.englishWords) {
                    int iei = indexer_e.addAndGetIndex(word_e);
                    for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {
                        String word_f = sentencePair.frenchWords.get(fi);
                        int key = this.packageWordPair(word_e, word_f);
                        // update count
                        p.put(key, p.get(key, 1) + p.get(key, 0) / total_s[fi], 1);
                        total_count[iei] += p.get(key, 0) / total_s[fi];
                    }
                }
            }
            for (int ki = 0; ki < p.keys.length; ki++) {
                if (p.keys[ki] == 0)
                    continue;;
                int iei = (p.keys[ki] & 0xFFFF0000) >> 16;
                p.setAtHashId(ki, 0, p.getAtHashId(ki, 1) / total_count[iei]);

            }
        }
    }

    @Override
    public Alignment alignSentencePair(SentencePair sentencePair) {

        Alignment res = new Alignment();

        PairFactory pf;

        pf = new PairFactory.ReversingPairFactory();
        for (int ei = 0; ei < sentencePair.englishWords.size(); ei++) {
            String word_e = sentencePair.englishWords.get(ei);
            Double max_p = Double.NEGATIVE_INFINITY;
            int max_fi = -1;
            for (int fi = 0; fi < sentencePair.frenchWords.size(); fi++) {
                String word_f = sentencePair.frenchWords.get(fi);

                int key = this.packageWordPair(word_e, word_f);
                if (p.containsKey(key)) {
                    double fp = p.get(key, 0);
                    if (fp > max_p) {
                        max_p = fp;
                        max_fi = fi;
                    }
                }
            }
            if (max_fi >= 0) {
                res.addAlignment(ei, max_fi, true);
            }
        }
        return res;
    }


    private int packageWordPair(String word_e, String word_f) {
        return ((this.indexer_e.addAndGetIndex(word_e) & 0xFFFF) << 16) | (this.indexer_f.addAndGetIndex(word_f) & 0xFFFF);
    }

}
