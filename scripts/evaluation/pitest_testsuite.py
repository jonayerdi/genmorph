#!/usr/bin/env python3
'''
Run PITest on arbitrary test suites
'''

import re
from os import makedirs, cpu_count
from os.path import join, realpath
from subprocess import PIPE

from config import *

import tools.pitest as pitest
from filetypes.sut_config import SUTConfig
from states.generate_test_executions import SYSTEM_ID
from states.list_methods import list_methods_java_file
from tools.java import java_class_to_filepath, find_classes_in_package, java_proc, javac
from util.fsutil import isnonemptydir

REGEX_RANDOOP_DIR = re.compile(r'^.*randoop_seed_(\d+)[^\d]*.*$')

SUT_CONFIG = SUTConfig.from_filename(join('configs', 'sut-config-commons-math3.json'))
#WORKDIRS = [join('baseline_ICST', f'{i}') for i in range(12)]
#REGEX_TEST_CLASS = lambda clazz: re.compile(f'^{clazz}_ESTest\\d*$')
#TESTS_CLASSPATH = ['evosuite-1.1.0.jar', *SUT_CONFIG.classpaths]
#TEST_SUITE_DIR = lambda workdir, clazz: join(workdir, 'testsuite')
#TARGET_TESTS = lambda clazz: f'{clazz}_ESTest*'
#EXCLUDED_TEST_CLASSES =  '*_scaffolding*'
WORKDIRS = [join('results-commons-math3', f'randoop_seed_{i}') for i in range(12)]
REGEX_TEST_CLASS = lambda clazz: re.compile(f'^RegressionTest\\d*$')
TESTS_CLASSPATH = ['evosuite-1.1.0.jar', *SUT_CONFIG.classpaths]
TEST_SUITE_DIR = lambda workdir, clazz: join(workdir, clazz, f'randoop0_seed_{REGEX_RANDOOP_DIR.match(workdir).group(1)}')
TARGET_TESTS = lambda clazz: f'RegressionTest*'
EXCLUDED_TEST_CLASSES =  '*Driver'

def gen_pitest_poms(workdir):
    '''UNUSED'''
    for sut_class, sut_method, sut_method_index in SUT_CONFIG.iter_methods():
        system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
        test_suite_dir = join(workdir, 'testsuite')
        package = '.'.join(sut_class.split('.')[:-1])
        regex_test_class = REGEX_TEST_CLASS(sut_class)
        is_test_class = lambda clazz: regex_test_class.match(clazz) is not None
        tests_classes = list(filter(is_test_class, find_classes_in_package(root=test_suite_dir, package=package)))
        pitest_workdir = join(workdir, 'pitest', system_id)
        makedirs(pitest_workdir, exist_ok=True)
        sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
        source_file = realpath(join(SUT_CONFIG.sources, sut_source_relpath))
        all_methods = list_methods_java_file(source_file=source_file, container=set)
        excluded_methods = list(filter(lambda m: m != sut_method, all_methods))
        pitest.write_pitest_pom(
            dir=pitest_workdir,
            sources_dir=SUT_CONFIG.sources,
            tests_dir=test_suite_dir,
            target_classes=[sut_class],
            target_tests=tests_classes,
            excluded_methods=excluded_methods,
        )

def run_pitest_mvn(workdir):
    '''UNUSED'''
    for sut_class, sut_method, sut_method_index in SUT_CONFIG.iter_methods():
        system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
        pitest_workdir = join(workdir, 'pitest', system_id)
        pit_reports_sut_dir = join(pitest_workdir, 'target', 'pit-reports')
        if not isnonemptydir(pit_reports_sut_dir):
            assert pitest.pitest(workdir=pitest_workdir) == 0, f'Error executing PITest in {pitest_workdir}'

def group_failing_tests_by_class(test_suite_dir, failing_tests):
    failing_suites = {}
    for test_class, test in failing_tests:
        test_source = join(test_suite_dir, java_class_to_filepath(test_class, ext='.java'))
        failing_suites.setdefault(test_source, set())
        failing_suites[test_source].add(test)
    return failing_suites

def run_pitest(workdir):
    for sut_class, sut_method, sut_method_index in SUT_CONFIG.iter_methods():
        system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
        pitest_workdir = join(workdir, 'pitest', system_id)
        makedirs(pitest_workdir, exist_ok=True)
        while not isnonemptydir(pitest_workdir):
            test_suite_dir = TEST_SUITE_DIR(workdir, sut_class)
            sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
            source_file = realpath(join(SUT_CONFIG.sources, sut_source_relpath))
            all_methods = list_methods_java_file(source_file=source_file, container=set)
            excluded_methods = list(filter(lambda m: m != sut_method, all_methods))
            print(f'* Running PITest on {pitest_workdir}')
            pit_proc = java_proc(
                classpath=[
                    join(PITEST_HOME, PITEST_JAR),
                    test_suite_dir,
                    *TESTS_CLASSPATH,
                ],
                main_class='org.pitest.mutationtest.commandline.MutationCoverageReport',
                args=[
                    '--reportDir', pitest_workdir,
                    '--targetClasses', sut_class,
                    '--targetTests', TARGET_TESTS(sut_class),
                    '--excludedTestClasses', EXCLUDED_TEST_CLASSES,
                    '--sourceDirs', SUT_CONFIG.sources,
                    '--excludedMethods', ','.join(excluded_methods),
                    '--outputFormats', ','.join(['HTML', 'CSV']),
                    '--threads', f'{cpu_count()}',
                ],
                stderr=PIPE,
            )
            failing_tests = list(pitest.stderr_failing_tests(map(lambda l: l.decode(), iter(pit_proc.stderr))))
            retval = pit_proc.wait()
            #failing_tests = [('org.apache.commons.math3.primes.SmallPrimes_ESTest0', 'test11'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest0', 'test12'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest1', 'test10'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest1', 'test11'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest2', 'test08'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest2', 'test10'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest2', 'test11'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest3', 'test06'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest3', 'test09'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest3', 'test10'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest4', 'test12'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest4', 'test13'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest4', 'test14'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest5', 'test06'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest5', 'test08'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest5', 'test09'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest6', 'test10'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest6', 'test11'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest7', 'test09'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest7', 'test10'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest7', 'test13'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest7', 'test14'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest8', 'test08'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest8', 'test11'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest8', 'test12'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest9', 'test09'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest9', 'test12'), ('org.apache.commons.math3.primes.SmallPrimes_ESTest9', 'test13')]
            print(f'* Finished PITest on {pitest_workdir}, found {len(failing_tests)} failing tests')
            if failing_tests:
                raise f'failing_tests: {failing_tests}'
                failing_suites = group_failing_tests_by_class(test_suite_dir=test_suite_dir, failing_tests=failing_tests)
                for test_suite in failing_suites:
                    to_remove = failing_suites[test_suite]
                    pitest.remove_testsuite_methods(test_suite=test_suite, to_remove=to_remove, method_end=lambda l: l.rstrip() == '  }')
                    assert javac(source_files=[test_suite], classpath=TESTS_CLASSPATH) == 0, 'Failed to execute javac'
        print(f'* Done {pitest_workdir}')

def main():
    for workdir in WORKDIRS:
        run_pitest(workdir)
