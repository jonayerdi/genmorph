#!/usr/bin/env python3
'''
Collect the FN fitnesses obtained by GAssertMRs for some MRs
'''

from os import listdir
from os.path import join
from statistics import mode

from evaluation.split_mr import assertions_dir_strategy

def collect_fns(mrs_dir, out_file):
    with open(out_file, mode='wb') as out_fp:
        out_fp.write('SUT,STRATEGY,FN\n'.encode(encoding='utf-8'))
        for experiment in listdir(mrs_dir):
            experiment_dir = join(mrs_dir, experiment)
            for sut in listdir(experiment_dir):
                gassert_stats_file = join(experiment_dir, sut, 'mr_gassert_stats.csv')
                with open(gassert_stats_file, mode='r') as stats_csv:
                    columns = next(stats_csv).strip()
                    assert columns.lower() == 'correctstates,incorrectstates,generations,generationbestsolution,timegoodsolution,fp,fn,complexity'
                    for row in filter(lambda l: l, map(lambda l: l.strip().split(','), stats_csv)):
                        CorrectStates, IncorrectStates, Generations, GenerationBestSolution, TimeGoodSolution, Fp, Fn, Complexity = row
                        fp_num = None
                        try:
                            fp_num = float(Fp)
                        except: pass
                        if fp_num != 0.0:
                            print(gassert_stats_file)
                        else:
                            strategy = assertions_dir_strategy(experiment)
                            out_fp.write(f'{sut},{strategy},{Fn}\n'.encode(encoding='utf-8'))

def main():
    import sys
    if len(sys.argv) != 3:
        print('./collect_fns.py <MRS_DIR> <OUT_FILE>')
        exit(1)
    mrs_dir = sys.argv[1]
    out_file = sys.argv[2]
    collect_fns(mrs_dir=mrs_dir, out_file=out_file)
