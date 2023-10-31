#!/usr/bin/env python3
'''
Convert a GAssert assertion into a Java assertion.
'''

from os import listdir
from os.path import isfile, join
from subprocess import Popen

from config import *

GASSERT2JAVA_CLASS = 'ch.usi.gassert.data.tree.converter.GAssert2Java'

def gassert2java(args, gassert_jar=GASSERT_JAR, main_class=GASSERT2JAVA_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *map(str, args)]
    return Popen(args=args, stdout=stdout).wait()

def gassert2java_files(input_file, output_file, states_dir_or_file, mrip=None):
    if not isfile(states_dir_or_file):
        states_file = get_any_state_file(states_dir_or_file)
    else:
        states_file = states_dir_or_file
    args = [input_file, output_file, states_file]
    if mrip:
        args.append(mrip)
    return gassert2java(args=args)

def get_any_state_file(states_dir):
    return join(states_dir, next(filter(lambda f: f.endswith('.state.json'), listdir(states_dir))))

def main():
    import sys
    if len(sys.argv) != 4:
        print('./gassert2java.py <INPUT_FILE> <OUTPUT_FILE> <STATES_DIR_OR_FILE>')
        exit(1)
    gassert2java_files(*sys.argv[1:])
