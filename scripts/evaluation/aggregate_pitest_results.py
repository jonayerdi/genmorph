from glob import glob
from os import listdir
from os.path import join, split, isfile

from util.fsutil import split_path
from util.normalized_number import NormalizedNumber

PITEST_RESULTS_GLOB = 'experiment_results/pitest_seed*'
#PITEST_RESULTS_GLOB = 'experiment_results/baseline_pitest_randoop_seed*'
#PITEST_RESULTS_GLOB = 'experiment_results/baseline_pitest_evosuite_seed*'
FP_FILE = 'experiment_results/mrsNoFP_OASIsResults/oasisFP.txt'

def get_fps(fp_file):
    if fp_file is not None:
        with open(fp_file, mode='r') as fd:
            for line in fd:
                index = line.find('_ESTest.java')
                if index > 0:
                    # assertions_seed43/LangClass?insert?0/oasis_test/LangClass?insert?0@SequenceRemove?0?2/LangClass1689393776.4657643_ESTest.java:  public void test0()  throws Throwable  {
                    experiment, sut, oasis_test, mr, test_class = split_path(line[:line.find(':', index)])
                    assert oasis_test == 'oasis_test', oasis_test
                    assert test_class.endswith('_ESTest.java'), test_class
                    yield experiment, sut, mr

def read_mrs_status_file(mrs_status_file, fps=[]):
    if isfile(mrs_status_file):
        with open(mrs_status_file, mode='r') as fp:
            lines = iter(fp)
            assert next(lines).strip() == 'EXPERIMENT,MR,FP,MS'
            for line in lines:
                line = line.strip()
                if line:
                    data = line.split(',')
                    assert len(data) == 4, line
                    experiment, mr, fp, ms = data[0], data[1], NormalizedNumber.parse(data[2]), NormalizedNumber.parse(data[3])
                    if (experiment, mr) in fps:
                        fp.value += 1
                        fp.divisor += 1
                        ms = NormalizedNumber(0, 0)
                    yield experiment, mr, fp, ms

def read_mutants_killed_file(mutants_killed_file, fps=[]):
    if isfile(mutants_killed_file):
        with open(mutants_killed_file, mode='r') as fp:
            lines = iter(fp)
            header = next(lines).strip()
            assert header.startswith('EXPERIMENT,MR,')
            mutants = len(header.split(',')) - 3
            for line in lines:
                line = line.strip()
                if line:
                    data = line.split(',')
                    assert len(data) == mutants + 3, line
                    experiment, mr, mk = data[0], data[1], [bool(int(x)) for x in data[2:-1]]
                    if experiment != '*' and mr != '*' and (experiment, mr) not in fps:
                        yield experiment, mr, mk

def collect_pitest_results(pitest_result_dirs, fps=[]):
    pzs = {}
    pzo = {}
    mks = {}
    for pitest_result_dir in pitest_result_dirs:
        for sut in listdir(pitest_result_dir):
            fps_sut = list(map(lambda x: (x[0], x[2]), filter(lambda x: x[1] == sut, fps)))
            pzs.setdefault(sut, {})
            pzo.setdefault(sut, {})
            mks.setdefault(sut, {})
            mrs_status_file = join(pitest_result_dir, sut, 'mrs_status.csv')
            for experiment, mr, fp, ms in read_mrs_status_file(mrs_status_file=mrs_status_file):
                pzo[sut].setdefault(experiment, [])
                if fp.value == 0:
                    pzo[sut][experiment].append(NormalizedNumber(value=(experiment, mr) not in fps_sut, divisor=1))
            for experiment, mr, fp, ms in read_mrs_status_file(mrs_status_file=mrs_status_file, fps=fps_sut):
                experiment = f'{split(pitest_result_dir)[1]}/{experiment}'
                pzs[sut].setdefault(experiment, [])
                mks[sut].setdefault(experiment, [])
                pzs[sut][experiment].append(NormalizedNumber(value=int(fp.value == 0), divisor=1))
            mutants_killed_file = join(pitest_result_dir, sut, 'mutants_killed.csv')
            for experiment, mr, mk in read_mutants_killed_file(mutants_killed_file=mutants_killed_file, fps=fps_sut):
                experiment = f'{split(pitest_result_dir)[1]}/{experiment}'
                mks[sut][experiment].append(mk)
    return pzs, pzo, mks

def same_seed(experiment1, experiment2):
    # FIXME: We assume 2-digit seeds
    seed1 = int(experiment1.split('/')[0][-2:])
    seed2 = int(experiment2.split('/')[0][-2:])
    return seed1 == seed2

def aggregate_mks(it, mutants):
    acc = None
    for mk in it:
        if acc is None:
            acc = [x for x in mk]
        else:
            assert len(acc) == len(mk), [acc, mk]
            acc = [x or y for x, y in zip(acc, mk)]
    if acc is None:
        return [False for _ in range(mutants)]
    return acc

def flatten(it):
    for i in it:
        for e in i:
            yield e

def aggregate_pitest_results(pzs, pzo, mks):
    results = { sut: { 'MS': None, 'PZ': None, 'PZO': None } for sut in set((*pzs, *pzo, *mks)) }
    for sut in pzs:
        results[sut]['PZ'] = NormalizedNumber.sum(flatten((pzs[sut][experiment] for experiment in pzs[sut])))
    for sut in pzo:
        results[sut]['PZO'] = NormalizedNumber.sum(flatten((pzo[sut][experiment] for experiment in pzo[sut])))
    for sut in mks:
        mss = []
        mutants = max((max((len(x) for x in mks[sut][experiment]), default=0) for experiment in mks[sut]), default=0)
        if mutants < 1:
            results[sut]['MS'] = NormalizedNumber(value=0, divisor=0)
        else:
            for experiment in mks[sut]:
                mk = aggregate_mks(mks[sut][experiment], mutants=mutants)
                mss.append(NormalizedNumber(value=sum(map(int, mk)), divisor=len(mk)))
            results[sut]['MS'] = NormalizedNumber.sum(mss)
    return results

def main():
    import sys
    if len(sys.argv) not in [1, 2]:
        print('./evaluate_pitest.py [PITEST_RESULTS_GLOB]')
        exit(1)
    pitest_results_glob = PITEST_RESULTS_GLOB
    if len(sys.argv) > 1:
        pitest_results_glob = sys.argv[1]
    pzs, pzo, mks = collect_pitest_results(
        pitest_result_dirs=glob(pitest_results_glob),
        fps=list(get_fps(FP_FILE)),
    )
    results = aggregate_pitest_results(
        pzs=pzs, pzo=pzo, mks=mks,
    )
    print('SUT,MS,PZ,PZO')
    for sut in sorted(results):
        print(f'{sut},{results[sut]["MS"]},{results[sut]["PZ"]},{results[sut]["PZO"]}')
