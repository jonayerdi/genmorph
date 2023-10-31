#!/usr/bin/env python3
'''
Get the results for all the MRs (ch.usi.gassert.EvaluateMRs):
'''

import sys
from os.path import join
from subprocess import Popen

from config import *

MAIN_CLASS = 'ch.usi.gassert.EvaluateMRs'

def evaluate_mrs_default_dirs(root_dir, suts, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = [
        join(root_dir, 'mrs'),
        join(root_dir, 'states'),
        join(root_dir, 'states_followup'),
        join(root_dir, 'classifications_regular'),
        join(root_dir, 'classifications_regular_followup'),
        ';'.join(suts),
        join(root_dir, 'results'),
    ]
    return evaluate_mrs(args=args, gassert_jar=gassert_jar, main_class=main_class, stdout=stdout)

def evaluate_mrs(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *args]
    return Popen(args=args, stdout=stdout).wait()

def main():
    if len(sys.argv) == 3:
        evaluate_mrs_default_dirs(root_dir=sys.argv[1], suts=sys.argv[2].split(';'))
    elif len(sys.argv) == 8:
        evaluate_mrs(sys.argv[1:])
    else:
        print('./evaluate.py [ROOT_DIR] [SUTS]')
        print('./evaluate.py [MRS] [SOURCE_STATES] [FOLLOWUP_STATES] [SOURCE_CLASSIFICATIONS] [FOLLOWUP_CLASSIFICATIONS] [SUTS] [RESULTS_DIR]')
