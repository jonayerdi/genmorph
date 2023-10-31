#!/usr/bin/env python3
'''
Generate Java MR files: input relations (*.jir.txt) and an output relations (*.jor.txt).
'''

from os import listdir, makedirs
from os.path import join, isfile, isdir

from config import *

from evaluation.gassert2java import gassert2java_files

def enumerate_dir_mrs(mrs_dir, system_id):
    for experiment in listdir(mrs_dir):
        experiment_sut_dir = join(mrs_dir, experiment, system_id)
        if isdir(experiment_sut_dir):
            for filename in listdir(experiment_sut_dir):
                if filename.endswith('.jor.txt') and isfile(join(experiment_sut_dir, filename)):
                    mr = filename[:-len('.jor.txt')]
                    yield experiment, mr

def generate_mrs_dir(assertions_dir, states_dir, system_id, out_dir):
    mr_names_mapping_fps = {}
    mr_gassert_stats_fps = {}
    try:
        for filename in listdir(assertions_dir):
            # TestClass?factorial?0@NumericAddition?-1.000000?1.txt
            file = join(assertions_dir, filename)
            if filename.startswith(f'{system_id}{SEPARATORS[0]}') and filename.endswith('.txt.fitness.csv') and isfile(file):
                mr = filename[:-len('.txt.fitness.csv')]
                makedirs(out_dir, exist_ok=True)
                ir_file = join(assertions_dir, f'{system_id}.mrip.txt')
                or_file = join(assertions_dir, f'{mr}.txt')
                java_ir_file = join(out_dir, f'{mr}.jir.txt')
                java_or_file = join(out_dir, f'{mr}.jor.txt')
                states_dir_sut = join(states_dir, system_id)
                assert gassert2java_files(input_file=ir_file, mrip=mr, output_file=java_ir_file, states_dir_or_file=states_dir_sut) == 0, f'Error running gassert2java for {[ir_file, mr, java_ir_file, states_dir_sut]}'
                assert gassert2java_files(input_file=or_file, output_file=java_or_file, states_dir_or_file=states_dir_sut) == 0, f'Error running gassert2java for {[or_file, java_or_file, states_dir_sut]}'
    finally:
        for fp in mr_names_mapping_fps.values():
            fp.close()
        for fp in mr_gassert_stats_fps.values():
            fp.close()
