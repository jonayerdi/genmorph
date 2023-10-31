#!/usr/bin/env python3
'''
Generate a test suite for evaluating MRs with PITest
'''

from subprocess import Popen

from config import *

from tools.java import PATH_SEPARATOR, ADD_OPENS

PITEST_GENERATOR_CLASS = 'ch.usi.methodtest.PITestGenerator'

def pitest_generator(classpath, sut, mrs, source_test_inputs, followup_test_inputs, output_test_prefix, add_opens=None):
    if add_opens is None:
        add_opens = ADD_OPENS
    if classpath is None:
        classpath = []
    elif type(classpath) is str:
        classpath = classpath.split(PATH_SEPARATOR)
    classpath = PATH_SEPARATOR.join([*map(realpath, classpath), GASSERT_JAR])
    args = [
        'java', *add_opens, '-cp', classpath, PITEST_GENERATOR_CLASS, sut, mrs, source_test_inputs, followup_test_inputs, output_test_prefix
    ]
    return Popen(args=args).wait()

def main():
    import sys
    if len(sys.argv) != 6:
        print('./pitest_generator.py <CLASSPATH> <SUT> <MRS_DIR> <SOURCE_TEST_INPUTS_DIR> <FOLLOWUP_TEST_INPUTS_DIR> <OUTPUT_TEST_PREFIX>')
        exit(1)
    pitest_generator(*sys.argv[1:])
