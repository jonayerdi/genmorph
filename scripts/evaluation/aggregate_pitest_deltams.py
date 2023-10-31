from glob import glob
from os import listdir
from os.path import join, split

from evaluation.aggregate_pitest_results import read_mutants_killed_file, same_seed, aggregate_mks, get_fps
from util.normalized_number import NormalizedNumber

PITEST_RESULTS_GLOB_GENMORPH = 'experiment_results/pitest_seed*'
#PITEST_RESULTS_GLOB_GENMORPH = 'experiment_results/pitest_results_math_previous/pitest_seed*'
PITEST_RESULTS_GLOB_BASELINE = 'experiment_results/baseline_pitest_*_seed*'
#PITEST_RESULTS_GLOB_BASELINE = 'experiment_results/baseline_pitest_randoop_seed*'
#PITEST_RESULTS_GLOB_BASELINE = 'experiment_results/baseline_pitest_evosuite_seed*'
FP_FILE = 'experiment_results/mrsNoFP_OASIsResults/oasisFP.txt'

COMBINE_BASELINES = True

def collect_pitest_mutants_killed(pitest_result_dirs, fps=[]):
    results = {}
    for pitest_result_dir in pitest_result_dirs:
        for sut in listdir(pitest_result_dir):
            fps_sut = list(map(lambda x: (x[0], x[2]), filter(lambda x: x[1] == sut, fps)))
            results.setdefault(sut, {})
            mutants_killed_file = join(pitest_result_dir, sut, 'mutants_killed.csv')
            for experiment, mr, mk in read_mutants_killed_file(mutants_killed_file=mutants_killed_file, fps=fps_sut):
                if COMBINE_BASELINES and (experiment in ['randoop', 'evosuite']):
                    experiment = f'{split(pitest_result_dir)[1][-len("seedXX"):]}/baseline'
                else:
                    experiment = f'{split(pitest_result_dir)[1]}/{experiment}'
                results[sut].setdefault(experiment, [])
                results[sut][experiment].append(mk)
    return results

def deltams(mk_genmorph, mk_baseline):
    assert len(mk_genmorph) == len(mk_baseline), [mk_genmorph, mk_baseline]
    new_genmorph = 0
    missing_baseline = 0
    for genmorph, baseline in zip(mk_genmorph, mk_baseline):
        if not baseline:
            missing_baseline += 1
            if genmorph:
                new_genmorph += 1
    return NormalizedNumber(value=new_genmorph, divisor=missing_baseline)

def aggregate_pitest_deltams(pitest_results_genmorph, pitest_results_baseline):
    results = { sut: [] for sut in pitest_results_genmorph }
    for sut in pitest_results_genmorph:
        mutants = max((max((len(x) for x in pitest_results_baseline[sut][experiment])) for experiment in pitest_results_baseline[sut]), default=0)
        for experiment_genmorph in pitest_results_genmorph[sut]:
            for experiment_baseline in pitest_results_baseline[sut]:
                if same_seed(experiment_genmorph, experiment_baseline):
                    mk_genmorph = aggregate_mks(pitest_results_genmorph[sut][experiment_genmorph], mutants=mutants)
                    mk_baseline = aggregate_mks(pitest_results_baseline[sut][experiment_baseline], mutants=mutants)
                    results[sut].append(deltams(mk_genmorph=mk_genmorph, mk_baseline=mk_baseline))
    for sut in results:
        results[sut] = NormalizedNumber.sum(results[sut])
    return results

def main():
    import sys
    if len(sys.argv) not in [1, 3]:
        print('./evaluate_pitest.py [PITEST_RESULTS_GLOB_GENMORPH PITEST_RESULTS_GLOB_BASELINE]')
        exit(1)
    pitest_results_glob_genmorph = PITEST_RESULTS_GLOB_GENMORPH
    pitest_results_glob_baseline = PITEST_RESULTS_GLOB_BASELINE
    if len(sys.argv) > 1:
        pitest_results_glob_genmorph = sys.argv[1]
        pitest_results_glob_baseline = sys.argv[2]
    pitest_results_genmorph = collect_pitest_mutants_killed(
        pitest_result_dirs=glob(pitest_results_glob_genmorph),
        fps=list(get_fps(FP_FILE))
    )
    pitest_results_baseline= collect_pitest_mutants_killed(
        pitest_result_dirs=glob(pitest_results_glob_baseline),
    )
    results = aggregate_pitest_deltams(
        pitest_results_genmorph=pitest_results_genmorph,
        pitest_results_baseline=pitest_results_baseline,
    )
    print('SUT,DELTAMS')
    for sut in sorted(results):
        print(f'{sut},{results[sut]}')
