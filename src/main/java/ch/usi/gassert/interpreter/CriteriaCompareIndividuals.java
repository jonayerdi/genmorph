package ch.usi.gassert.interpreter;

import ch.usi.gassert.Config;
import ch.usi.gassert.evolutionary.Individual;

import java.util.Comparator;


public enum CriteriaCompareIndividuals {

    FP_FN_complexity {
        public Comparator<Individual> getComparator(int gen) {
            final double reward = Config.COMPUTE_ELITE_VALID_REWARD.apply(gen);
            return (s1, s2) -> {
                // Adjust fitnesses
                Double fp1 = s1.fitnessValueFP;
                Double fp2 = s2.fitnessValueFP;
                Double fn1 = s1.fitnessValueFN;
                Double fn2 = s2.fitnessValueFN;
                if (Config.IS_ELITE_VALID_REWARDED && (s1.isEliteValid() != s1.isEliteValid())) {
                    if (s1.isEliteValid()) {
                        fp1 *= reward;
                        fn1 *= reward;
                    } else {
                        fp2 *= reward;
                        fn2 *= reward;
                    }
                }
                // Compare fitnesses
                int res = fp1.compareTo(fp2);
                if (res != 0) {
                    return res;
                }
                res = fn1.compareTo(fn2);
                return (res != 0 ? res : s1.complexity.compareTo(s2.complexity));
            };
        }

    }, FN_FP_complexity {
        public Comparator<Individual> getComparator(int gen) {
            final double reward = Config.COMPUTE_ELITE_VALID_REWARD.apply(gen);
            return (s1, s2) -> {
                // Adjust fitnesses
                Double fp1 = s1.fitnessValueFP;
                Double fp2 = s2.fitnessValueFP;
                Double fn1 = s1.fitnessValueFN;
                Double fn2 = s2.fitnessValueFN;
                if (Config.IS_ELITE_VALID_REWARDED && (s1.isEliteValid() != s1.isEliteValid())) {
                    if (s1.isEliteValid()) {
                        fp1 *= reward;
                        fn1 *= reward;
                    } else {
                        fp2 *= reward;
                        fn2 *= reward;
                    }
                }
                // Compare fitnesses
                int res = fn1.compareTo(fn2);
                if (res != 0) {
                    return res;
                }
                res = fp1.compareTo(fp2);
                return (res != 0 ? res : s1.complexity.compareTo(s2.complexity));
            };
        }

    }, FNplusFP_complexity {
        public Comparator<Individual> getComparator(int gen) {
            final double reward = Config.COMPUTE_ELITE_VALID_REWARD.apply(gen);
            return (s1, s2) -> {
                // Adjust fitnesses
                Double fp1 = s1.fitnessValueFP;
                Double fp2 = s2.fitnessValueFP;
                Double fn1 = s1.fitnessValueFN;
                Double fn2 = s2.fitnessValueFN;
                if (Config.IS_ELITE_VALID_REWARDED && (s1.isEliteValid() != s1.isEliteValid())) {
                    if (s1.isEliteValid()) {
                        fp1 *= reward;
                        fn1 *= reward;
                    } else {
                        fp2 *= reward;
                        fn2 *= reward;
                    }
                }
                // Compare fitnesses
                final Double sums1 = fp1 + fn1;
                final Double sums2 = fp2 + fn2;
                final int res = sums1.compareTo(sums2);
                return (res != 0 ? res : s1.complexity.compareTo(s2.complexity));
            };
        }

    }, FNmultiplyFP_complexity {
        public Comparator<Individual> getComparator(int gen) {
            final double reward = Config.COMPUTE_ELITE_VALID_REWARD.apply(gen);
            return (s1, s2) -> {
                // Adjust fitnesses
                Double fp1 = s1.fitnessValueFP;
                Double fp2 = s2.fitnessValueFP;
                Double fn1 = s1.fitnessValueFN;
                Double fn2 = s2.fitnessValueFN;
                if (Config.IS_ELITE_VALID_REWARDED && (s1.isEliteValid() != s1.isEliteValid())) {
                    if (s1.isEliteValid()) {
                        fp1 *= reward;
                        fn1 *= reward;
                    } else {
                        fp2 *= reward;
                        fn2 *= reward;
                    }
                }
                // Compare fitnesses
                // if I have FP = 0.0 anf FN = 1.0 I don't want 0.0*1.0 = 0.0 I want 1.0 which is the max
                final Double prod1 = fp1 == 0 || fn1 == 0 ? Math.max(fp1, fn1) : (fp1 * fn1);
                final Double prod2 = fp2 == 0 || fn2 == 0 ? Math.max(fp2, fn2) : (fp2 * fn2);
                final int res = (prod1).compareTo(prod2);
                return (res != 0 ? res : s1.complexity.compareTo(s2.complexity));
            };
        }

    };

    public abstract Comparator<Individual> getComparator(int gen);

}






