from os import listdir
from os.path import join, split,realpath

import tools.pitest as pitest
from filetypes.sut_config import SUTConfig
from states.generate_test_executions import SYSTEM_ID
from states.instrument_method import find_method_lines, format_java_source
from tools.java import java_class_to_filepath
from util.fsutil import isnonemptydir

SUT_CONFIG = SUTConfig.from_filename(join('configs', 'sut-config-commons-math3.json'))
#WORKDIRS = [join('baseline_ICST', f'{i}') for i in range(12)]
WORKDIRS = [join('results-commons-math3', f'randoop_seed_{i}') for i in range(12)]

def inner_dir(root):
    for child in listdir(root):
        c = join(root, child)
        if isnonemptydir(c):
            return c

def main():
    for sut_class, sut_method, sut_method_index in SUT_CONFIG.iter_methods():
        system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
        # Find method lines in source file
        sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
        source_file = realpath(join(SUT_CONFIG.sources, sut_source_relpath))
        format_java_source(source_file=source_file)
        method_lines = set(find_method_lines(source_file=source_file, method_name=sut_method, method_index=sut_method_index))
        # print(f'{sut_method}: {method_lines}')
        # Collect killed mutants
        mutants_killed = {}
        for workdir in WORKDIRS:
            experiment = (split(workdir)[1], '')
            mutations = join(inner_dir(join(workdir, 'pitest', system_id)), 'mutations.csv')
            verdicts = pitest.get_verdicts(mutations=mutations, sut_lines=method_lines)
            killed = [int(verdict in ['TIMED_OUT', 'KILLED']) for verdict in verdicts]
            mutants_killed[experiment] = killed
        pitest.write_mutants_killed(mutants_killed=mutants_killed, output_file=join(split(split(WORKDIRS[0])[0])[1], f'{system_id}.killed.csv'))
