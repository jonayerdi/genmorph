#!/usr/bin/env python3
'''
Script to generate followups for the given test inputs that satisfy the given MRIPs.

Usage: './generate_mrip_followups.py <TEST_INPUTS_DIR> <STATES_DIR> <FOLLOWUPS_DIR> <INPUT_RELATION_FILE_OR_DIR> <CLASSPATH>
'''

import sys

from os import listdir
from os.path import join, isfile
from subprocess import Popen, TimeoutExpired

from config import *

from tools.java import PATH_SEPARATOR, ADD_OPENS
from util.log import log_stdout
from util.process import kill_proc_tree

MRIP_FOLLOWUP_GENERATOR_CLASS = 'ch.usi.methodtest.MRIPFollowupGenerator'

def generate_mrip_followups(test_inputs_dir, states_dir, output_dir, mrip_file, seed, classpath=[], timeout=None, stdout=None, add_opens=None):
    if add_opens is None:
        add_opens = ADD_OPENS
    if classpath is None:
        classpath = [GASSERT_JAR]
    else:
        classpath = [GASSERT_JAR, *classpath]
    classpath = PATH_SEPARATOR.join(map(realpath, classpath))
    mrip_followup_generator_args = [mrip_file, seed, test_inputs_dir, states_dir, output_dir]
    args = [
        'java', *add_opens, '-cp', classpath, MRIP_FOLLOWUP_GENERATOR_CLASS, *map(str, mrip_followup_generator_args)
    ]
    popen = Popen(args=args, stdout=stdout)
    try:
        return popen.wait(timeout=timeout)
    except TimeoutExpired:
        kill_proc_tree(pid=popen.pid)
        raise

def generate_mrip_followups_dir(test_inputs_dir, states_dir, output_dir, mrips_dir, seed, classpath=[], timeout=None, stdout=None, log=log_stdout):
    count_testcases = lambda d: sum((1 for _ in filter(lambda f: f.endswith('.methodinputs'), listdir(d))))
    source_testcases = count_testcases(test_inputs_dir)
    for filename in listdir(mrips_dir):
        file = join(mrips_dir, filename)
        if isfile(file) and filename.endswith('.ir.txt'):
            mrip_name = filename[:-len('.ir.txt')]
            mrip_output_dir = join(output_dir, mrip_name)
            log(f'{mrip_name}: ', end='')
            sys.stdout.flush()
            try:
                retval = generate_mrip_followups(
                    test_inputs_dir=test_inputs_dir,
                    states_dir=states_dir,
                    output_dir=mrip_output_dir,
                    mrip_file=file,
                    seed=seed,
                    classpath=classpath,
                    timeout=timeout,
                    stdout=stdout,
                )
                log(f'Generated {count_testcases(mrip_output_dir)}/{source_testcases} followups')
                assert retval == 0, f'generate_mrip_followups failed for MRIP: {file}'
            except TimeoutExpired:
                log('timeout')

def main():
    if len(sys.argv) != 6:
        print('./generate_mrip_followups.py <INPUT_RELATION_FILE_OR_DIR> <RANDOM_SEED> <TEST_INPUTS_DIR> <STATES_DIR> <FOLLOWUPS_DIR> <CLASSPATH>')
        exit(1)
    input_relation_file_or_dir = sys.argv[1]
    seed = int(sys.argv[2])
    test_inputs_dir = sys.argv[3]
    states_dir = sys.argv[4]
    output_dir = sys.argv[5]
    classpath = sys.argv[6].split(PATH_SEPARATOR)
    if isfile(input_relation_file_or_dir):
        generate_mrip_followups(
            test_inputs_dir=test_inputs_dir,
            states_dir=states_dir,
            output_dir=output_dir,
            mrip_file=input_relation_file_or_dir,
            seed=seed,
            classpath=classpath,
        )
    else:
        generate_mrip_followups_dir(
            test_inputs_dir=test_inputs_dir,
            states_dir=states_dir,
            output_dir=output_dir,
            mrips_dir=input_relation_file_or_dir,
            seed=seed,
            classpath=classpath,
        )
