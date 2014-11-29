package edu.berkeley.nlp.assignments.align.student;

import edu.berkeley.nlp.mt.Alignment;
import edu.berkeley.nlp.mt.SentencePair;
import edu.berkeley.nlp.mt.WordAligner;
import edu.berkeley.nlp.util.Pair;
import edu.berkeley.nlp.util.StringIndexer;
import org.ejml.simple.SimpleMatrix;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by wmusial on 11/28/14.
 */
public class HmmWordAligner implements WordAligner {


    IndexedDoubleArray trans;

    IntDoubleExpandableMap p;

    private StringIndexer indexer_e;
    private StringIndexer indexer_f;


    public HmmWordAligner(Iterable<SentencePair> training_data, StringIndexer indexer_e, StringIndexer indexer_f, IntDoubleExpandableMap p) {
        this.indexer_e = indexer_e;
        this.indexer_f = indexer_f;
        this.p = p;

        assert (p.data.length == 2);

        this.train(training_data);
    }

    public HmmWordAligner(Iterable<SentencePair> training_data) {

        this.indexer_f = new StringIndexer();
        this.indexer_e = new StringIndexer();
        this.indexer_e.addAndGetIndex("voitek");
        this.indexer_f.addAndGetIndex("voitek");

        p = new IntDoubleExpandableMap(2000000, 2, 0.8f, 1.25f);
        // initialize f to unform 1
        for (SentencePair sentencePair : training_data) {
            for (String word_e : sentencePair.englishWords) {
                for (String word_f : sentencePair.frenchWords) {
                    int key = this.packageWordPair(word_e, word_f);
                    p.put(key, 1, 0);

                }
            }
        }

        this.train(training_data);

    }

    private void train(Iterable<SentencePair> training_data) {
        int max_trans = 10;
        this.trans = new IndexedDoubleArray(max_trans);
        for (int i = -max_trans; i <= max_trans; i++) {
            this.trans.set(i, 1.);
            //this.trans.set(i, Math.exp(-1.*i*i/2/5./5.));
        }

        // compute change of variables
        int[] bin_count = new int[max_trans*2+1];
        for (int i = -200; i <= 200; i++) {
            bin_count[max_trans+flatten(i)] += 1;
        }

        double[] total_g = new double[this.indexer_e.size()];

        int num_iterations = 5;
        for (int i = 0; i < num_iterations; i++) {
            System.out.println("iteration " + (i+1) + " out of " + num_iterations);
            // erase gammas
            Arrays.fill(p.data[1], 0);
            Arrays.fill(total_g, 0);

            double[] eps = new double[max_trans*2+1];
            double eps_norm = 0;

            double likelihood = 0;

            for (SentencePair sentencePair : training_data) {
                int ne = sentencePair.englishWords.size();
                int nf = sentencePair.frenchWords.size();
                double[][] a = new double[ne][nf];
                double[][] b = new double[ne][nf];
                double[][] g = new double[ne][nf];

                // compute transition normalizations
                double[] trans_norm = new double[sentencePair.englishWords.size()];
                for (int pie = 0; pie < ne; pie++) {
                    for (int ie = 0; ie < ne; ie++) {
                        trans_norm[pie] += this.trans.get(flatten(ie - pie));
                    }
                }
                double trans_norm_beg = 0;
                for (int ie = 0; ie < ne; ie++) {
                    trans_norm_beg += this.trans.get(flatten(ie+1));
                }

                // forward
                {
                    String word_f = sentencePair.frenchWords.get(0);
                    for (int ie = 0; ie < ne; ie++) {
                        String word_e = sentencePair.englishWords.get(ie);
                        int key = this.packageWordPair(word_e, word_f);
                        a[ie][0] = this.p.get(key, 0) * this.trans.get(flatten(ie + 1)) / trans_norm_beg;
                    }
                }
                for (int t = 1; t < nf; t++) {
                    String word_f = sentencePair.frenchWords.get(t);
                    for (int ie = 0; ie < ne; ie++) {
                        String word_e = sentencePair.englishWords.get(ie);
                        int key = this.packageWordPair(word_e, word_f);
                        double tp = p.get(key, 0);
                        for (int pie = 0; pie < ne; pie++) {
                            a[ie][t] += tp * a[pie][t - 1] * this.trans.get(flatten(ie - pie)) / trans_norm[pie];
                        }
                    }
                }

                // backward
                {
                    for (int ei = 0; ei < ne; ei++) {
                        b[ei][nf - 1] = 1;
                    }
                }
                for (int t = nf - 2; t >= 0; t--) {
                    String word_f = sentencePair.frenchWords.get(t + 1);
                    for (int ei = 0; ei < ne; ei++) {
                        for (int nei = 0; nei < ne; nei++) {
                            String word_e = sentencePair.englishWords.get(nei);
                            int key = this.packageWordPair(word_e, word_f);

                            b[ei][t] += b[nei][t + 1] * p.get(key, 0) * this.trans.get(flatten(nei - ei)) / trans_norm[ei];
                        }
                    }
                }

                // gamma
                for (int fi = 0; fi < nf; fi++) {
                    double denom = 0;
                    for (int ei = 0; ei < ne; ei++) {
                        denom += a[ei][fi] * b[ei][fi];
                    }
                    for (int ei = 0; ei < ne; ei++) {
                        g[ei][fi] = a[ei][fi] * b[ei][fi] / denom;
                    }
                }

                for (int ei = 0; ei < ne; ei++) {
                    String word_e = sentencePair.englishWords.get(ei);
                    int iei = this.indexer_e.addAndGetIndex(word_e);

                    for (int fi = 0; fi < nf; fi++) {
                        String word_f = sentencePair.frenchWords.get(fi);
                        int key = this.packageWordPair(word_e, word_f);
                        p.put(key, p.get(key, 1) + g[ei][fi], 1);

                        total_g[iei] += g[ei][fi];
                    }
                }

                // eps

                for (int fi = 0; fi < nf-1; fi++) {
                    double denom = 0;
                    for (int ei = 0; ei < ne; ei++) {
                        denom += a[ei][fi] * b[ei][fi];
                    }

                    String word_f = sentencePair.frenchWords.get(fi+1);


                    for (int ei = 0; ei < ne; ei++) {
                        String word_e = sentencePair.englishWords.get(ei);

                        int key = packageWordPair(word_e, word_f);

                        for (int pei = 0; pei < ne; pei++) {
                            eps[max_trans+flatten(ei-pei)] += a[pei][fi] * b[ei][fi+1] * (1+0*p.get(key, 0)) * this.trans.get(flatten(ei-pei)) / trans_norm[pei] / denom;
                        }
                        eps_norm += g[ei][fi];
                    }
                }





                double l = 0;
                for (int ei = 0; ei < ne; ei++) {
                    l += a[ei][nf-1];
                }
                likelihood += Math.log(l);

                //m.print();
            } // end of sentencePair loop
            System.out.println("likelihood: " + likelihood);


            for (int ki = 0; ki < p.keys.length; ki++) {
                if (p.keys[ki] == 0)
                    continue;
                ;
                int iei = (p.keys[ki] & 0xFFFF0000) >> 16;
                p.setAtHashId(ki, 0, p.getAtHashId(ki, 1) / total_g[iei]);
            }


            for (int di = -max_trans; di <= max_trans; di++) {
                System.out.printf(" %5d", di);
            }
            System.out.println();
            double norm = 0;
            for (int di = -max_trans; di <= max_trans; di++) {
                this.trans.set(di, eps[max_trans+di] / eps_norm / bin_count[max_trans+di]);

                System.out.printf(" %.3f",  this.trans.get(di));
                norm += this.trans.get(di);
            }

            System.out.println();

            System.out.println("norm of transitions: " + norm);

        }
    }

    @Override
    public Alignment alignSentencePair(SentencePair sentencePair) {
        return alignSentencePair(sentencePair, new Alignment());
    }

    public Alignment alignSentencePair(SentencePair sentencePair, Alignment exclusive) {
        Alignment res = new Alignment();


        int ne = sentencePair.englishWords.size();
        int nf = sentencePair.frenchWords.size();



        double[][] a = new double[ne][nf];
        int[][] prev = new int[ne][nf];

        // compute transition normalizations
        double[] trans_norm = new double[sentencePair.englishWords.size()];
        for (int pie = 0; pie < ne; pie++) {
            for (int ie = 0; ie < ne; ie++) {
                trans_norm[pie] += this.trans.get(flatten(ie - pie));
            }
        }
        double trans_norm_beg = 0;
        for (int ie = 0; ie < ne; ie++) {
            trans_norm_beg += this.trans.get(flatten(ie+1));
        }

        int[] alignments = new int[nf];
        Arrays.fill(alignments, -1);
        Set<Integer> used_e = new HashSet<Integer>();
        for (Pair<Integer, Integer> amt : exclusive.getSureAlignments()) {
            used_e.add(amt.getFirst());
            alignments[amt.getSecond()] = amt.getFirst();
        }

        // forward
        {
            String word_f = sentencePair.frenchWords.get(0);
            for (int ie = 0; ie < ne; ie++) {

                String word_e = sentencePair.englishWords.get(ie);
                if (used_e.contains(ie) && ie != alignments[0]) {
                    //System.out.println("skipping word_f: " + word_f + " and word_e " + word_e);
                    continue;
                }
                if (alignments[0] >= 0 && alignments[0] != ie) {
                    continue;
                }

                int key = this.packageWordPair(word_e, word_f);
                try {
                    a[ie][0] = this.p.get(key, 0) * this.trans.get(ie+1) / trans_norm_beg;
                }
                catch (Exception e) {
                    a[ie][0] = 0;
                }
            }
        }
        for (int t = 1; t < nf; t++) {
            String word_f = sentencePair.frenchWords.get(t);

            for (int ie = 0; ie < ne; ie++) {
                String word_e = sentencePair.englishWords.get(ie);
                if (used_e.contains(ie) && ie != alignments[t]) {
                    //System.out.println("skipping word_f: " + word_f + " and word_e " + word_e);
                    continue;
                }
                if (alignments[t] >= 0 && alignments[t] != ie ) {
                    continue;
                }

                int key = this.packageWordPair(word_e, word_f);
                // translation probability

                double tp=0;
                try {
                    tp = p.get(key, 0);
                }
                catch (Exception e) {
                    a[ie][t] = 0;
                }
                double max_score = Double.NEGATIVE_INFINITY;
                int max_pie = -1;

                for (int pie = 0; pie < ne; pie++) {
                    double score = tp * a[pie][t - 1] * this.trans.get(flatten(ie - pie)) / trans_norm[pie];

                    if (score > max_score) {
                        max_score = score;
                        max_pie = pie;
                    }
                }
                assert (max_pie >= 0);

                prev[ie][t] = max_pie;
                a[ie][t] = max_score;
            }
        }

        int current_ie;
        {
            double max_score = Double.NEGATIVE_INFINITY;
            int max_ie = -1;

            for (int ie = 0; ie < ne; ie++) {
                if (a[ie][nf - 1] > max_score) {
                    max_score = a[ie][nf - 1];
                    max_ie = ie;
                }
            }
            assert (max_ie >= 0);
            current_ie = max_ie;
        }
        for (int fi = nf-1; fi >= 0; fi--) {
            res.addAlignment(current_ie, fi, true);
            current_ie = prev[current_ie][fi];
        }

        return res;
    }

    /*
    @Override
    public Alignment alignSentencePair(SentencePair sentencePair) {
        Alignment res = new Alignment();


        int ne = sentencePair.englishWords.size();
        int nf = sentencePair.frenchWords.size();



        double[][] a = new double[ne][nf];
        int[][] prev = new int[ne][nf];

        // compute transition normalizations
        double[] trans_norm = new double[sentencePair.englishWords.size()];
        for (int pie = 0; pie < ne; pie++) {
            for (int ie = 0; ie < ne; ie++) {
                trans_norm[pie] += this.trans.get(flatten(ie - pie));
            }
        }

        // forward
        {
            String word_f = sentencePair.frenchWords.get(0);
            for (int ie = 0; ie < ne; ie++) {
                String word_e = sentencePair.englishWords.get(ie);
                int key = this.packageWordPair(word_e, word_f);
                try {
                    a[ie][0] = 1. / ne * this.p.get(key, 0);
                }
                catch (Exception e) {
                    a[ie][0] = 0;
                }
            }
        }
        for (int t = 1; t < nf; t++) {
            String word_f = sentencePair.frenchWords.get(t);

            for (int ie = 0; ie < ne; ie++) {
                String word_e = sentencePair.englishWords.get(ie);
                int key = this.packageWordPair(word_e, word_f);
                // translation probability
                double tp = p.get(key, 0);

                double max_score = Double.NEGATIVE_INFINITY;
                int max_pie = -1;

                for (int pie = 0; pie < ne; pie++) {
                    double score = tp * a[pie][t - 1] * this.trans.get(flatten(ie - pie)) / trans_norm[pie];

                    if (score > max_score) {
                        max_score = score;
                        max_pie = pie;
                    }
                }
                assert (max_pie >= 0);

                prev[ie][t] = max_pie;
                a[ie][t] = max_score;
            }
        }

        int current_ie;
        {
            double max_score = Double.NEGATIVE_INFINITY;
            int max_ie = -1;

            for (int ie = 0; ie < ne; ie++) {
                if (a[ie][nf - 1] > max_score) {
                    max_score = a[ie][nf - 1];
                    max_ie = ie;
                }
            }
            assert (max_ie >= 0);
            current_ie = max_ie;
        }
        for (int fi = nf-1; fi >= 0; fi--) {
            res.addAlignment(current_ie, fi, true);
            current_ie = prev[current_ie][fi];
        }

        return res;
    }
    */

    private int packageWordPair(String word_e, String word_f) {
        return ((this.indexer_e.addAndGetIndex(word_e) & 0xFFFF) << 16) | (this.indexer_f.addAndGetIndex(word_f) & 0xFFFF);
    }

    private int flatten(int d) {

        int sign = 1;
        if (d < 0) {
            sign = -1;
            d = -d;
        }
        d = (int)(sign * Math.round(10.*d / (10.+d)));

        return d;
    }
}
