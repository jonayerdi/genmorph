#!/usr/bin/env python3
'''
Automatically generate multiple MRIPs based on set coverage of the test cases + individual coverage limits for each MRIP.

Arguments:
    Manager class
    Manager args
    Algorithm (random|hillclimbing)
    MRIP count
    Min test case coverage percent per MRIP
    Max test case coverage percent per MRIP
    Random seed (integer)
    Time budget in minutes
    Output MRIPs file
    Output MRInfos file

Example usage:
    ./generate_mrips.py ch.usi.gassert.data.manager.method.MethodRegularDataManager GASSERT;demo/gassert_states_clean;demo/classifications_regular hillclimbing 4 0.05 0.5 0 5 demo/gassert_states_clean/MRIP.txt demo/gassert_states_clean/MRInfo.csv
'''

from os.path import isfile
from subprocess import Popen

from config import *
from util.log import log_stdout

MAIN_CLASS = 'ch.usi.gassert.mrip.MRIPGenerator'

'''
Default configuration for MRIPGenerator
'''
GENERATE_MRIPS_DATA_MANAGER = 'ch.usi.gassert.data.manager.method.MethodRegularDataManager'
GENERATE_MRIPS_DATA_MANAGER_ARGS = 'GASSERT;{states_dir};{classifications_regular_dir}'

def generate_mrips_config(
        classifications_regular_dir, states_dir,
        generate_mrips_config, mrinfos_path, mrips_path,
        log=log_stdout):
    if isfile(mrinfos_path):
        log(f'* Found: {mrinfos_path}')
        return 0
    else:
        # Generate MRIPs
        log(f'* Generating MRIPs')
        return generate_mrips(args=[
            GENERATE_MRIPS_DATA_MANAGER,
            GENERATE_MRIPS_DATA_MANAGER_ARGS.format(states_dir=states_dir, classifications_regular_dir=classifications_regular_dir),
            generate_mrips_config['algorithm'],
            generate_mrips_config['mrip_count'],
            generate_mrips_config['min_coverage'],
            generate_mrips_config['max_coverage'],
            generate_mrips_config['random_seed'],
            generate_mrips_config['time_budget_minutes'],
            mrips_path,
            mrinfos_path,
        ])

def generate_mrips(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *map(str, args)]
    return Popen(args=args, stdout=stdout).wait()

def main():
    import sys
    generate_mrips(args=sys.argv[1:])

