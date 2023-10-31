#!/usr/bin/env python3
'''
Utility script to run Major (2.0.0) tool

Example Major 2.0.0 invocation:
$MAJOR_HOME/bin/major --mml $MAJOR_HOME/mml/all.mml.bin --export export.mutants:true -cp SUT_CLASSPATH SUT_CLASS_SOURCE

The mutator plugin supports the following configuration options:
 * mml:<FILE>: run mutators specified in the compiled mml file
 * mutants.log:<FILE>: The location of the mutants.log file (default: ./mutants.log)
 * export.mutants: If set, Major exports each generated mutant as a source code file
 * mutants.directory:<DIR>: The export directory for the source-code mutants (default: ./mutants)
 * strict.checks: If set, Major discards mutants that would not be compilable if created at the source-code level.
'''

from subprocess import Popen
from os import listdir
from os.path import join, realpath, isdir

from tools.java import PATH_SEPARATOR

DEFAULT_MAJOR_HOME = '.' #join('C:/', 'tools', 'major')

MUTANTS_LOGFILE = 'mutants.log'
MUTANTS_LOGFILE_CLASS = 'mutants_{sut_class}.log'

def list_mutants(mutants_dir):
    for child in listdir(mutants_dir):
        mutant_id = child
        mutant_path = join(mutants_dir, child)
        if isdir(mutant_path):
            yield mutant_id, mutant_path

def filter_mutants(mutants_logfile, lines):
    with open(mutants_logfile, mode='r') as fd:
        for line in fd.readlines():
            data = line.split(':')
            if len(data) >= 7: # data[6] is a Java expression which may contain ":", but we only care up to data[5]
                mutant_id = data[0]
                mutation_line = int(data[5])
                if mutation_line in lines:
                    yield mutant_id
            elif len(data) != 0:
                raise Exception(f'Error parsing line:\n{line}')

def run(source_file, classpath=None, workdir=None, mutants_dir=None, major_home=DEFAULT_MAJOR_HOME):
    java_args = [
        # Recommended JVM options for Major:
        #'-XX:ReservedCodeCacheSize=256M',
        #'-XX:MaxPermSize=1G',
        # Stuff that matters:
         source_file,
    ]
    major_args = [
        f'export.mutants:true',
        f'strict.checks:true',
    ]
    if mutants_dir is not None:
        major_args.append(f'mutants.directory:{mutants_dir}')
    return run_major_process(
        java_args=java_args,
        major_args=major_args,
        classpath=classpath,
        workdir=workdir,
        major_home=major_home,
        mml=None,
    )

def run_major_process(major_args=None, java_args=None, classpath=None, workdir=None, major_home=DEFAULT_MAJOR_HOME, mml=None):
    if type(classpath) == str:
        classpath = [classpath]
    if not classpath:
        classpath = []
    cp = [
        join(major_home, 'lib', 'major.jar'),
        *classpath,
    ]
    if mml is None:
        mml = join(major_home, 'mml', 'all.mml.bin')
    if major_args is None:
        major_args = []
    major_args = [
        f'-Xplugin:MajorPlugin',
        f'mml:{mml}',
        *major_args,
    ]
    if java_args is None:
        java_args = []
    command = [
        f'javac',
        f'-cp', PATH_SEPARATOR.join(map(realpath, cp)),
        ' '.join(major_args),
        *java_args
    ]
    # javac -cp /mnt/0BCE1EC8A0AA117E/tools/major/lib/major.jar:/mnt/0BCE1EC8A0AA117E/Users/jayerdi/Projects/gasssert-mrs/configs/commons-math3-3.6.1/target/classes "-Xplugin:MajorPlugin mml:/mnt/0BCE1EC8A0AA117E/tools/major/mml/all.mml.bin export.mutants:true" ../configs/commons-math3-3.6.1/src/main/java/org/apache/commons/math3/util/FastMath.java
    # javac -cp /mnt/0BCE1EC8A0AA117E/tools/major/lib/major.jar:../configs/commons-math3-3.6.1/target/classes -Xplugin:MajorPlugin  export.mutants:true ../configs/commons-math3-3.6.1/src/main/java/org/apache/commons/math3/util/FastMath.java
    return Popen(shell=False, cwd=workdir, args=command).wait()

def parse_major_args(args: list):
    java_args = []
    major_args = []
    classpath = []
    mml = None
    i = 0
    while i < len(args):
        if args[i] == '--mml':
            mml = args[i+1]
            i += 2
        elif args[i] == '--export':
            major_args.append(args[i+1])
            i += 2
        elif args[i] == '-cp':
            classpath.append(args[i+1])
            i += 2
        else:
            java_args.append(args[i])
            i += 1
    return java_args, major_args, classpath, mml

def main():
    '''
    ../scripts/run/major.py --export export.mutants:true -cp ../configs/commons-math3-3.6.1/target/classes ../configs/commons-math3-3.6.1/src/main/java/org/apache/commons/math3/util/FastMath.java
    '''
    import sys
    import os
    major_home = DEFAULT_MAJOR_HOME
    if 'MAJOR_HOME' in os.environ:
        major_home = os.environ['MAJOR_HOME']
    java_args, major_args, classpath, mml = parse_major_args(sys.argv[1:])
    run_major_process(java_args=java_args, major_args=major_args, classpath=classpath, major_home=major_home, mml=mml)
