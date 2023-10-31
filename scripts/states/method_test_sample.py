#!/usr/bin/env python3
'''
Utility script to sample method test files.

./method_test_sample.py <TEST_INPUTS_DIR> <MAX_TESTS>
'''

from os.path import isfile, join
from random import Random

from util.fsutil import sample_dir

def method_test_sample(tests_dir, max_tests, seed=0):
    def is_test_file(file):
        return file.endswith('.methodinputs') and isfile(join(tests_dir, file))
    sample_dir(dir=tests_dir, samples=max_tests, rng=Random(x=seed), condition=is_test_file)

def main():
    import sys
    tests_dir = sys.argv[1]
    max_tests = sys.argv[2]
    method_test_sample(tests_dir=tests_dir, max_tests=max_tests)
