#!/usr/bin/env python3
'''
Get the results for the given assertion:
'''

import sys
from os.path import join, split, splitext
from subprocess import Popen

from config import *

MAIN_CLASS = 'ch.usi.gassert.EvaluateAssertion'

def results_assertion(manager_class, manager_args, assertion, assertion_id, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, manager_class, manager_args, assertion, assertion_id]
    return Popen(args=args, stdout=stdout).wait()

def results_assertion_file(manager_class, manager_args, assertion_file, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    assertion_id = splitext(split(assertion_file)[1])[0]
    assertion = None
    with open(assertion_file, mode='r') as fd:
        assertion = fd.read()
    return results_assertion(manager_class=manager_class, manager_args=manager_args, assertion=assertion, assertion_id=assertion_id, gassert_jar=gassert_jar, main_class=main_class, stdout=stdout)

def main():
    results_assertion_file(manager_class=sys.argv[1], manager_args=sys.argv[2], assertion_file=sys.argv[3])
