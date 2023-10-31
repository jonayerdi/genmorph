#!/usr/bin/env python3
'''
Split a full MR file into an input relation file and an output relation file.

Example usage:
    ./split_mr.py FullMR.txt MR.ir.txt MR.or.txt
'''

import json
import re

from concurrent.futures import ThreadPoolExecutor
from os import listdir, makedirs
from os.path import join, split, isfile, isdir
from shutil import copyfile
from subprocess import Popen

from config import *

from evaluation.gassert2java import gassert2java_files

SPLIT_MR_CLASS = 'ch.usi.gassert.SplitMR'

REGEX_BASELINE1_MR_FILENAME = re.compile(r'^(.+%.+%\d+)_(MRIP\d+)_Full\.txt$')
REGEX_BASELINE2_MR_FILENAME = re.compile(r'^(.+%.+%\d+)_(.+)\.txt$')
REGEX_TRANSFORM_MR_FILENAME = re.compile(r'^(.+%.+%\d+)_(.+%.+%\d+!.*[^\d]\d+)_Full\.txt$')
REGEX_TRANSFORM_RELAX_MR_FILENAME = re.compile(r'^(.+%.+%\d+)_(.+%.+%\d+!.*[^\d]\d+)\.txt$')

STRATEGY_TO_MR_REGEX = {
    'baseline1': REGEX_BASELINE1_MR_FILENAME,
    'baseline2': REGEX_BASELINE2_MR_FILENAME,
    'transform': REGEX_TRANSFORM_MR_FILENAME,
    'transform_relax': REGEX_TRANSFORM_RELAX_MR_FILENAME,
    'unguided_transform': REGEX_TRANSFORM_MR_FILENAME,
    'unguided_transform_relax': REGEX_TRANSFORM_RELAX_MR_FILENAME,
}

STRATEGY_STATS_FILE = {
    'baseline1': lambda f: f'{f[:-len("_Full.txt")]}.txt.stats.json',
    'baseline2': lambda f: f'{f}.stats.json',
    'transform': lambda f: f'{f[:-len("_Full.txt")]}.txt.stats.json',
    'transform_relax': lambda f: f'{f}.stats.json',
    'unguided_transform': lambda f: f'{f[:-len("_Full.txt")]}.txt.stats.json',
    'unguided_transform_relax': lambda f: f'{f}.stats.json',
}

STRATEGY_FITNESS_FILE = {
    'baseline1': lambda f: f'{f[:-len("_Full.txt")]}.txt.fitness.csv',
    'baseline2': lambda f: f'{f}.fitness.csv',
    'transform': lambda f: f'{f[:-len("_Full.txt")]}.txt.fitness.csv',
    'transform_relax': lambda f: f'{f}.fitness.csv',
    'unguided_transform': lambda f: f'{f[:-len("_Full.txt")]}.txt.fitness.csv',
    'unguided_transform_relax': lambda f: f'{f}.fitness.csv',
}

def enumerate_dir_mrs(mrs_dir, system_id):
    for experiment in filter(lambda f: isdir(join(mrs_dir, f)), listdir(mrs_dir)):
        experiment_sut_dir = join(mrs_dir, experiment, system_id)
        if isdir(experiment_sut_dir):
            for filename in listdir(experiment_sut_dir):
                if filename.endswith('.mr.txt') and isfile(join(experiment_sut_dir, filename)):
                    mr = filename[:-len('.mr.txt')].split('@')[1]
                    yield experiment, mr

def assertions_dir_strategy(assertions_dir):
    assertions_dir = split(assertions_dir)[1]
    if assertions_dir.startswith('assertions_baseline1'):
        return 'baseline1'
    if assertions_dir.startswith('assertions_baseline2'):
        return 'baseline2'
    if assertions_dir.startswith('assertions_transform_relax'):
        return 'transform_relax'
    if assertions_dir.startswith('assertions_transform'):
        return 'transform'
    if assertions_dir.startswith('assertions_unguided_transform_relax'):
        return 'unguided_transform_relax'
    if assertions_dir.startswith('assertions_unguided_transform'):
        return 'unguided_transform'
    return None

def split_mr(mr_filename, ir_filename, or_filename):
    args = ['java', '-cp', GASSERT_JAR, SPLIT_MR_CLASS, mr_filename, ir_filename, or_filename]
    return Popen(args=args).wait()

def split_mrs_dir(assertions_dir, mrs_dir, regex_mr, stats_file, fitness_file, mr_gassert_stats='mr_gassert_stats.csv', mr_names_mapping='mr_names.csv', mr_names=None, states_dir=None):
    def default_mr_names():
        i = 0
        while True:
            yield f'MR{i}'
            i += 1
    if mr_names is None:
        mr_names = default_mr_names()
    else:
        mr_names = mr_names()
    mr_names_mapping_fps = {}
    mr_gassert_stats_fps = {}
    try:
        for filename in listdir(assertions_dir):
            file = join(assertions_dir, filename)
            filematch = regex_mr.match(filename)
            if isfile(file) and filematch is not None:
                sut = filematch.group(1)
                mr_original = filematch.group(2)
                mr_new = mr_original
                sut_mrs_dir = join(mrs_dir, sut)
                makedirs(sut_mrs_dir, exist_ok=True)
                if mr_names_mapping is not None:
                    mr_new = next(mr_names)
                    mr_names_mapping_file = join(sut_mrs_dir, mr_names_mapping)
                    if mr_names_mapping_file not in mr_names_mapping_fps:
                        mr_names_mapping_fps[mr_names_mapping_file] = open(mr_names_mapping_file, mode='wb')
                        mr_names_mapping_fps[mr_names_mapping_file].write(f'original,new\n'.encode(encoding='utf-8'))
                    mr_names_mapping_fps[mr_names_mapping_file].write(f'{mr_original},{mr_new}\n'.encode(encoding='utf-8'))
                if mr_gassert_stats is not None:
                    mr_gassert_stats_file = join(sut_mrs_dir, mr_gassert_stats)
                    if mr_gassert_stats_file not in mr_gassert_stats_fps:
                        mr_gassert_stats_fps[mr_gassert_stats_file] = open(mr_gassert_stats_file, mode='wb')
                        mr_gassert_stats_fps[mr_gassert_stats_file].write(f'CorrectStates,IncorrectStates,Generations,GenerationBestSolution,TimeGoodSolution,FP,FN,Complexity\n'.encode(encoding='utf-8'))
                    with open(fitness_file(file), mode='r') as csv:
                        columns = next(csv).strip()
                        assert columns.lower() == 'correctstates,incorrectstates,generations,fp,fn,complexity'
                        CorrectStates, IncorrectStates, Generations, Fp, Fn, Complexity = next(csv).strip().split(',')
                    with open(stats_file(file), mode='r') as stats_fp:
                        stats = json.load(stats_fp)
                        GenerationBestSolution = stats['generationBestSolution']
                        TimeGoodSolution = stats['timestampGoodSolution'] - stats['timestampStart']
                    mr_gassert_stats_fps[mr_gassert_stats_file].write(f'{",".join(map(str, (CorrectStates, IncorrectStates, Generations, GenerationBestSolution, TimeGoodSolution, Fp, Fn, Complexity)))}\n'.encode(encoding='utf-8'))
                mr_file = join(sut_mrs_dir, f'{sut}@{mr_new}.mr.txt')
                ir_file = join(sut_mrs_dir, f'{sut}@{mr_new}.ir.txt')
                or_file = join(sut_mrs_dir, f'{sut}@{mr_new}.or.txt')
                java_ir_file = join(sut_mrs_dir, f'{sut}@{mr_new}.jir.txt')
                java_or_file = join(sut_mrs_dir, f'{sut}@{mr_new}.jor.txt')
                copyfile(src=file, dst=mr_file)
                assert split_mr(
                    mr_filename=file,
                    ir_filename=ir_file,
                    or_filename=or_file,
                ) == 0, f'Error running split_mr or {file}'
                if states_dir is not None:
                    states_dir_sut = join(states_dir, sut)
                    assert gassert2java_files(input_file=ir_file, output_file=java_ir_file, states_dir_or_file=states_dir_sut) == 0, f'Error running gassert2java for {[ir_file, java_ir_file, states_dir_sut]}'
                    assert gassert2java_files(input_file=or_file, output_file=java_or_file, states_dir_or_file=states_dir_sut) == 0, f'Error running gassert2java for {[or_file, java_or_file, states_dir_sut]}'
    finally:
        for fp in mr_names_mapping_fps.values():
            fp.close()
        for fp in mr_gassert_stats_fps.values():
            fp.close()

def split_mrs_experiment(root_dir, mrs_dir, mr_names_mapping='mr_names.csv', mr_names=None, states_dir=None):
    executor = ThreadPoolExecutor(max_workers=8)
    futures = {}
    for child in listdir(root_dir):
        childpath = join(root_dir, child)
        if isdir(childpath):
            strategy = assertions_dir_strategy(child)
            if strategy is not None:
                futures[child] = executor.submit(split_mrs_dir, **{
                    'assertions_dir': childpath,
                    'mrs_dir': join(mrs_dir, child),
                    'regex_mr': STRATEGY_TO_MR_REGEX[strategy],
                    'stats_file': STRATEGY_STATS_FILE[strategy],
                    'fitness_file': STRATEGY_FITNESS_FILE[strategy],
                    'mr_names_mapping': mr_names_mapping,
                    'mr_names': mr_names,
                    'states_dir': states_dir,
                })
    for assertion_dir in futures:
        futures[assertion_dir].result()

def main():
    import sys
    if len(sys.argv) not in [4, 5]:
        print('./split_mr.py <MR_FILE> <IR_FILE> <OR_FILE> [STATES_DIR_OR_FILE]')
        print('./split_mr.py -d <ASSERTIONS_DIR> <SPLIT_MRS_DIR> [STATES_DIR]')
        print('./split_mr.py -e <ROOT_DIR> <SPLIT_MRS_DIR> [STATES_DIR]')
        exit(1)
    mr_filename = sys.argv[1]
    if mr_filename == '-e':
        root_dir = sys.argv[2]
        mrs_dir = sys.argv[3]
        states_dir = None
        if len(sys.argv) > 4:
            states_dir = sys.argv[4]
        split_mrs_experiment(root_dir=root_dir, mrs_dir=mrs_dir, states_dir=states_dir)
    elif mr_filename == '-d':
        assertions_dir = sys.argv[2]
        mrs_dir = sys.argv[3]
        states_dir = None
        if len(sys.argv) > 4:
            states_dir = sys.argv[4]
        strategy = assertions_dir_strategy(assertions_dir)
        regex_mr = STRATEGY_TO_MR_REGEX.get(strategy)
        fitness_file = STRATEGY_FITNESS_FILE.get(strategy)
        stats_file = STRATEGY_STATS_FILE.get(strategy)
        if regex_mr is None:
            print(f'Cannot infer strategy from assertions dir name: {assertions_dir}')
            exit(1) 
        split_mrs_dir(assertions_dir=assertions_dir, mrs_dir=mrs_dir,
                      stats_file=stats_file, fitness_file=fitness_file,
                      regex_mr=regex_mr, states_dir=states_dir)
    else:
        ir_filename = sys.argv[2]
        or_filename = sys.argv[3]
        states_dir_or_file = None
        if len(sys.argv) > 4:
            states_dir_or_file = sys.argv[4]
        split_mr(mr_filename=mr_filename, ir_filename=ir_filename, or_filename=or_filename)
        if states_dir_or_file is not None:
            assert ir_filename.endswith('.ir.txt'), f'Invalid IR filename: "{ir_filename}"'
            assert or_filename.endswith('.or.txt'), f'Invalid OR filename: "{or_filename}"'
            java_ir_filename = ir_filename[:-len('.ir.txt')] + '.jir.txt'
            java_or_filename = or_filename[:-len('.or.txt')] + '.jor.txt'
            assert gassert2java_files(input_file=ir_filename, output_file=java_ir_filename, states_dir_or_file=states_dir_or_file) == 0, f'Error running gassert2java for {[ir_filename, java_ir_filename, states_dir_or_file]}'
            assert gassert2java_files(input_file=or_filename, output_file=java_or_filename, states_dir_or_file=states_dir_or_file) == 0, f'Error running gassert2java for {[or_filename, java_or_filename, states_dir_or_file]}'
