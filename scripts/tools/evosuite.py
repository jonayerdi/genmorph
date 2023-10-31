#!/usr/bin/env python3
'''
Utility script to run Evosuite tool
'''

from os.path import realpath, join
from subprocess import Popen

from tools.java import PATH_SEPARATOR

DEFAULT_EVOSUITE_HOME = '.'# join('C:/', 'tools', 'evosuite')
DEFAULT_EVOSUITE_JAR = 'evosuite-1.1.0.jar'
DEFAULT_EVOSUITE_RUNTIME = 'evosuite-standalone-runtime-1.1.0.jar'
DEFAULT_EVOSUITE_TESTS_DIR = 'evosuite-tests'
EVOSUITE_TEST_CLASS = '{sut_classname}_ESTest'
EVOSUITE_SCAFFOLDING_CLASS = '{sut_classname}_ESTest_scaffolding'

JUNIT_MAIN_CLASS = 'org.junit.runner.JUnitCore'

def gen_testcases(clazz, classpath, seed, budget=600, workdir=None, assertions=False, evosuite_home=DEFAULT_EVOSUITE_HOME, evosuite_jar=DEFAULT_EVOSUITE_JAR):
    if classpath is None:
        classpath = ''
    elif type(classpath) is not str:
        classpath = PATH_SEPARATOR.join(map(realpath, classpath))
    return run_evosuite_process(
        workdir, evosuite_home, evosuite_jar,
        '-projectCP', classpath, '-class', clazz, '-seed', seed, 
        f'-Dsearch_budget={budget}', '-Dstopping_condition=MaxTime',
        f'-Dassertions={str(assertions).lower()}', '-Dminimize=true', '-Djunit_check=FALSE',
        '-Dsandbox=false', '-Dsandbox_mode=OFF', '-Dvirtual_fs=false',
    )

def run_evosuite_process(workdir, evosuite_home, evosuite_jar, *args):
    return Popen(shell=False, cwd=workdir, args=['java', '-jar', realpath(join(evosuite_home, evosuite_jar)), *map(str, args)]).wait()

def main():
    import sys
    import os
    evosuite_home = DEFAULT_EVOSUITE_HOME
    evosuite_jar = DEFAULT_EVOSUITE_JAR
    if 'EVOSUITE_HOME' in os.environ:
        evosuite_home = os.environ['EVOSUITE_HOME']
    if 'EVOSUITE_JAR' in os.environ:
        evosuite_jar = os.environ['EVOSUITE_JAR']
    run_evosuite_process(None, evosuite_home, evosuite_jar, *sys.argv[1:])
