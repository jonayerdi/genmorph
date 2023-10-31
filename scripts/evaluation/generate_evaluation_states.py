#!/usr/bin/env python3
'''
Script to generate the source and follow-up states for the evaluation.

Usage: './generate_evaluation_states.py <SUT_CONFIG> <EVALUATION_CONFIG>
'''

import re

from os import listdir
from os.path import join, split, splitext, isfile, isdir

from config import *

from evaluation.generate_mrip_followups import generate_mrip_followups_dir
from filetypes.mrinfo import MRInfo, MRInfoDB
from filetypes.sut_config import SUTConfig
from filetypes.experiment_config import ExperimentConfig
from states.generate_test_executions import generate_test_executions, generate_method_states_sut, generate_classifications, SYSTEM_ID, SYSTEM_ID_MUTANT
from util.fsutil import isnonemptydir
from tools.java import java_class_to_filepath
from util.log import log_stdout

REGEX_MUTANT_ID = re.compile(r'^M(\d+)$')
def find_relevant_mutants_from_classifications(classifications_dir, system_id):
    relevant_mutants = set()
    for filename in listdir(join(classifications_dir, system_id)):
        if filename.endswith('.classifications.csv'):
            filename_without_ext = filename[:-len('.classifications.csv')]
            sut, mutant = filename_without_ext.split('@')
            match = REGEX_MUTANT_ID.match(mutant)
            if match is not None:
                relevant_mutants.add(match.group(1))
    return relevant_mutants

def generate_mrip_followups_mrinfos(followups_dir, mrip=None):
    if mrip is None:
        sut, mrip = split(followups_dir)[1].split('@')
    mrinfos = MRInfoDB()
    for filename in listdir(followups_dir):
        file = join(followups_dir, filename)
        if filename.endswith('.methodinputs') and isfile(file):
            sut, test_id = splitext(filename)[0].split('!')
            source_test_id, followup_id = test_id.split('followup')
            mrinfos.add(MRInfo(mr=mrip, source=source_test_id, followup=test_id))
    mrinfos.write_to_csv(join(followups_dir, MRInfoDB.FILENAME))

def generate_mrip_followups_mrinfos_recursive(followups_dir):
    to_explore = [followups_dir]
    while to_explore:
        directory = to_explore.pop()
        has_methodinputs = False
        for filename in listdir(directory):
            file = join(directory, filename)
            if isdir(file):
                to_explore.append(file)
            elif filename.endswith('.methodinputs') and isfile(file):
                has_methodinputs = True
        if has_methodinputs:
            generate_mrip_followups_mrinfos(followups_dir=directory)

def generate_mrip_followups_all(mrs_dir, suts, test_inputs_dir, states_dir, test_inputs_followup_dir, seed, classpath, log=log_stdout):
    for mrs_group in filter(lambda f: isdir(join(mrs_dir, f)), listdir(mrs_dir)):
        log(f'[[{mrs_group}]]')
        mrs = join(mrs_dir, mrs_group)
        for sut in listdir(mrs):
            if sut in suts:
                sut_mrs = join(mrs, sut)
                sut_test_inputs_dir = join(test_inputs_dir, sut)
                sut_states_dir = join(states_dir, sut)
                sut_test_inputs_followup_dir = join(test_inputs_followup_dir, mrs_group, sut)
                if not isnonemptydir(sut_test_inputs_followup_dir):
                    generate_mrip_followups_dir(
                        mrips_dir=sut_mrs, seed=seed, test_inputs_dir=sut_test_inputs_dir, states_dir=sut_states_dir,
                        output_dir=sut_test_inputs_followup_dir, classpath=classpath, timeout=60, log=log,
                    )

def generate_evaluation_states(sut_config: SUTConfig, config: dict, log=log_stdout):
    # Generate random source test cases
    generate_test_executions(sut_config=sut_config, experiment_config=config, log=log)
    # Init variables
    paths = config['paths']
    output_dir = paths['output_dir']
    build_dir = realpath(join(output_dir, paths['build_dir']))
    mrs_dir = realpath(join(output_dir, paths['mrs_dir']))
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    test_inputs_followup_dir = realpath(join(output_dir, paths['test_inputs_followup_dir']))
    states_dir = realpath(join(output_dir, paths['states_dir']))
    states_followup_dir = realpath(join(output_dir, paths['states_followup_dir']))
    classpath = [*map(realpath, sut_config.classpaths), build_dir]
    system_ids = set((SYSTEM_ID(clazz, method, index) for (clazz, method, index) in sut_config.iter_methods()))
    mutants_dir = realpath(join(output_dir, paths['mutants_dir']))
    classifications_regular_dir = realpath(join(output_dir, paths['classifications_regular_dir']))
    classifications_regular_followup_dir = realpath(join(output_dir, paths['classifications_regular_followup_dir']))
    original_system_name = paths['original_system_name']
    # Generate followup test inputs
    generate_mrip_followups_all(
        mrs_dir=mrs_dir, suts=system_ids, test_inputs_dir=test_inputs_dir, states_dir=states_dir,
        test_inputs_followup_dir=test_inputs_followup_dir, seed=config.get('random_seed', 0), classpath=classpath, log=log,
    )
    generate_mrip_followups_mrinfos_recursive(followups_dir=test_inputs_followup_dir)
    # Execute followups
    relevant_mutants_cache = {}
    for sut_class, method_name, method_index in sut_config.iter_methods():
        sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
        sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
        source_file = realpath(join(sut_config.sources, sut_source_relpath))
        sut_classpath = list(map(realpath, sut_config.classpaths))
        system_id = SYSTEM_ID(sut_class, method_name, method_index)
        # Find out which mutants are relevant for this SUT
        relevant_mutants = relevant_mutants_cache.get(system_id)
        if relevant_mutants is None:
            relevant_mutants = find_relevant_mutants_from_classifications(classifications_dir=classifications_regular_dir, system_id=system_id)
            relevant_mutants_cache[system_id] = relevant_mutants
        # Execute followup tests for each MRIP
        for strategy in listdir(test_inputs_followup_dir):
            strategy_test_inputs_followup_dir = join(test_inputs_followup_dir, strategy)
            strategy_states_followup_dir = join(states_followup_dir, strategy)
            sut_test_inputs_followup_dir = join(strategy_test_inputs_followup_dir, system_id)
            sut_states_followup_dir = join(strategy_states_followup_dir, system_id)
            for mrip_dir in listdir(sut_test_inputs_followup_dir):
                # Execute tests on each mutant
                generate_method_states_sut(source_file=source_file, sut_class=sut_class, sut_classpath=sut_classpath,
                    sut_class_relpath=sut_class_relpath, method_name=method_name, method_index=method_index,
                    mutants_dir=mutants_dir, relevant_mutants=relevant_mutants, states_dir=join(sut_states_followup_dir, mrip_dir),
                    test_inputs_dir=join(sut_test_inputs_followup_dir, mrip_dir), original_system_name=original_system_name, log=log)
                # Generate classifications files
                generate_classifications(states_dir=join(sut_states_followup_dir, mrip_dir),
                    classifications_regular_dir=join(classifications_regular_followup_dir, strategy, system_id, mrip_dir),
                    original_system=SYSTEM_ID_MUTANT(system_id, original_system_name), log=log)

def main():
    import sys
    if len(sys.argv) != 3:
        print('./generate_evaluation_states.py <SUT_CONFIG> <EVALUATION_CONFIG>')
        exit(1)
    generate_evaluation_states(
        sut_config=SUTConfig.from_filename(sys.argv[1]),
        config=ExperimentConfig.from_filename(sys.argv[2]),
        log=log_stdout,
    )
