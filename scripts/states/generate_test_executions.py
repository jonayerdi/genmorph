#!/usr/bin/env python3
'''
Generate test executions for a system (java source file) and its mutants

Usage: ./generate_test_executions.py <SUT_CONFIG> <EXPERIMENT_CONFIG>
'''

import re
from os.path import join, isfile, splitext, split, dirname, realpath
from os import makedirs, remove, listdir
from random import Random
from shutil import copyfile, move, rmtree

from config import *

import tools.evosuite as evosuite
import tools.java as java
import tools.major as major
import tools.randoop as randoop
from filetypes.experiment_config import ExperimentConfig
from filetypes.sut_config import SUTConfig
from generation.generate_mrips import *
from states.check_gassert_states import check_gassert_states
from states.classify_states import classify_states
from states.instrument_method import find_method_lines, format_java_source, instrument_method, SERIALIZE_INPUTS_VISITOR, SERIALIZE_STATES_VISITOR
from states.method_test_executor import method_test_executor_dir_processes
from states.method_test_processor import method_test_processor
from states.method_test_rename import method_test_rename
from states.method_test_sample import method_test_sample
from tools.evosuite import DEFAULT_EVOSUITE_TESTS_DIR, JUNIT_MAIN_CLASS, EVOSUITE_TEST_CLASS, EVOSUITE_SCAFFOLDING_CLASS
from tools.java import JavaVersion, java_class_to_filepath, ADD_OPENS
from tools.major import MUTANTS_LOGFILE, MUTANTS_LOGFILE_CLASS
from util.fsutil import isnonemptydir
from util.log import log_stdout

REGEX_SYSTEM_ID = re.compile(rf'^(.*){SEPARATORS[1]}(.*){SEPARATORS[1]}(\d+)$')
SYSTEM_ID = lambda class_name, method_name, method_index: f'{class_name}{SEPARATORS[1]}{method_name}{SEPARATORS[1]}{method_index}'
SYSTEM_ID_MUTANT = lambda system_id, mutant_id: f'{system_id}{SEPARATORS[0]}{mutant_id}'
SYSTEM_ID_MUTANT_TEST = lambda system_id, mutant_id, test_id: f'{system_id}{SEPARATORS[0]}{mutant_id}{SEPARATORS[0]}{test_id}'

def get_file_testid(filename, extension):
    # TestClass?factorial?0@test0@test0.Transformation?1.0@2.0?1@2.methodinputs
    # TestClass?factorial?0@original@test0.Transformation?1.0@2.0?1@2.state.json
    without_extension = filename[:-len(extension)]
    parts = without_extension.split(SEPARATORS[1])
    testid = {
        '.methodinputs': lambda: SEPARATORS[0].join(SEPARATORS[1].join(parts[2:]).split(SEPARATORS[0])[1:]),
        '.state.json': lambda: SEPARATORS[0].join(SEPARATORS[1].join(parts[2:]).split(SEPARATORS[0])[2:]),
    }[extension]()
    return testid

def find_successful_method_tests(states_dir):
    # TestClass?factorial?0@original@test0.state.json
    # TestClass?factorial?0@original@test0.BooleanFlip?1.state.json
    # TestClass?factorial?0@original@test0.NumericAddition?1.000000?1.state.json
    # TestClass?factorial?0@original@test0.Transformation?1.0@2.0?1@2.state.json
    for filename in listdir(states_dir):
        filepath = join(states_dir, filename)
        if isfile(filepath) and filename.endswith('.state.json'):
            yield get_file_testid(filename=filename, extension='.state.json')

def remove_corrupt_states(states_dir, log=log_stdout):
    for state_file, problem in check_gassert_states(states_dir):
        log(f'{state_file}: {problem}')
        remove(state_file)

def find_relevant_mutants(source_file, mutants_logfile, method_name, method_index):
    '''
    List mutations in the instrumented method
    '''
    relevant_lines = set(find_method_lines(source_file, method_name, method_index))
    return set(major.filter_mutants(
        mutants_logfile=mutants_logfile,
        lines=relevant_lines,
    ))

def find_relevant_mutants_all(source_file, mutants_logfile, methods):
    '''
    List mutations in the instrumented method
    '''
    relevant_lines = set()
    for method_name, method_index in methods:
        relevant_lines.update(find_method_lines(source_file, method_name, method_index))
    return set(major.filter_mutants(
        mutants_logfile=mutants_logfile,
        lines=relevant_lines,
    ))

def remove_unused_mutants_all(mutants_dir, source_file, mutants_logfile, methods):
    relevant_mutants = find_relevant_mutants_all(source_file=source_file, mutants_logfile=mutants_logfile, methods=methods)
    for dir in listdir(mutants_dir):
        path = join(mutants_dir, dir)
        if isdir(path) and dir.isnumeric() and dir not in relevant_mutants:
            rmtree(path)

def compile_source_file(source_file, sut_class_path, sut_classpath, build_dir, log=log_stdout):
    '''
    Compile a Java source file
    '''
    with JavaVersion(JAVA8):
        if isfile(sut_class_path):
            log(f'* Found existing classfile: {sut_class_path}')
            return False
        else:
            log(f'* Compiling: {source_file}')
            assert java.javac(source_file, classpath=sut_classpath, outdir=build_dir) == 0, 'Failed to compile original source file'
            return True

def generate_evosuite_tests(sut_class, sut_class_relpath, output_dir, build_dir, config, sut_classpath=[], log=log_stdout, **kwargs):
    '''
    Generate Evosuite testcases + compile them
    '''
    if sut_class.endswith('.class'):
        sut_class = sut_class[:-len('.class')]
    sut_classparts = sut_class.split('.')
    sut_classname = sut_classparts[-1]
    sut_relpath, _ = split(sut_class_relpath)
    evosuite_test_class = EVOSUITE_TEST_CLASS.format(sut_classname=sut_classname)
    evosuite_scaffolding_class = EVOSUITE_SCAFFOLDING_CLASS.format(sut_classname=sut_classname)
    evosuite_test_class_source_rel = join(sut_relpath, f'{evosuite_test_class}.java')
    evosuite_scaffolding_class_source_rel = join(sut_relpath, f'{evosuite_scaffolding_class}.java')
    evosuite_workdir = config['workdir']
    evosuite_test_dirs = []
    evosuite_main_test_class = None
    rng = Random(x=int(config['random_seed']))
    num_executions = int(config['num_executions'])
    random_seeds = [rng.randint(10000, 2**31) for _ in range(num_executions)]
    if type(random_seeds) != list:
        random_seeds = [random_seeds]

    with JavaVersion(JAVA8):
        for index, random_seed in enumerate(random_seeds):
            workdir = join(output_dir, evosuite_workdir.format(index=index))
            makedirs(workdir, exist_ok=True)
            evosuite_tests_dir = realpath(join(workdir, DEFAULT_EVOSUITE_TESTS_DIR)) 
            evosuite_test_class_source = join(evosuite_tests_dir, evosuite_test_class_source_rel)
            evosuite_gen_classpath = [*sut_classpath, build_dir]
            if isfile(evosuite_test_class_source):
                log(f'* Found existing Evosuite tests: {evosuite_test_class_source}')
            else:
                log(f'* Running Evosuite on: {sut_class}')
                assert evosuite.gen_testcases(
                    clazz=sut_class,
                    classpath=evosuite_gen_classpath,
                    workdir=workdir,
                    seed=random_seed, 
                    budget=config['time_budget_seconds'],
                    evosuite_home=EVOSUITE_HOME, evosuite_jar=EVOSUITE_JAR,
                    **kwargs,
                ) == 0, 'Evosuite test generation process failed'
            # Postprocess main ESTest file
            evosuite_test_class_code = None
            with open(evosuite_test_class_source, mode='r') as fp:
                evosuite_test_class_code = fp.read()
            evosuite_test_class_code = evosuite_test_class_code.replace('mockJVMNonDeterminism = true', 'mockJVMNonDeterminism = false')
            with open(evosuite_test_class_source, mode='w') as fp:
                fp.write(evosuite_test_class_code)
            # Compile Evosuite tests
            evosuite_sources_rel = [evosuite_test_class_source_rel, evosuite_scaffolding_class_source_rel]
            log(f'* Compiling: {evosuite_sources_rel}')
            assert java.javac(evosuite_sources_rel, classpath=[*EVOSUITE_TEST_DEPENDENCIES, *evosuite_gen_classpath], workdir=evosuite_tests_dir) == 0, 'Failed to compile Evosuite tests'
            evosuite_test_dirs.append(evosuite_tests_dir)
            main_test_class = '.'.join((*sut_classparts[:-1], evosuite_test_class))
            if evosuite_main_test_class is None:
                evosuite_main_test_class = main_test_class
            else:
                assert evosuite_main_test_class == main_test_class, f'Different evosuite main test classes: {evosuite_main_test_class} and {main_test_class}'
    return evosuite_test_dirs, evosuite_main_test_class

def generate_randoop_tests(sut_class, output_dir, build_dir, config, sut_classpath=[], log=log_stdout, **kwargs):
    '''
    Generate Randoop testcases + compile them
    '''
    if sut_class.endswith('.class'):
        sut_class = sut_class[:-len('.class')]
    workdir_fmt = config['workdir']
    rng = Random(x=int(config['random_seed']))
    num_executions = int(config['num_executions'])
    random_seeds = [rng.randint(10000, 2**31) for _ in range(num_executions)]
    if type(random_seeds) != list:
        random_seeds = [random_seeds]
    randoop_workdir = workdir_fmt.format(index='')
    with JavaVersion(JAVA8):
        for index, random_seed in enumerate(random_seeds):
            workdir = join(output_dir, randoop_workdir, sut_class, workdir_fmt.format(index=index))
            makedirs(workdir, exist_ok=True)
            driver_class_source = join(workdir, f'{randoop.REGRESSION_TEST_DRIVER_CLASS}.java')
            gen_classpath = [*sut_classpath, build_dir]
            if isfile(driver_class_source):
                log(f'* Found existing Randoop test driver: {driver_class_source}')
            else:
                log(f'* Running Randoop on: {sut_class}')
                assert randoop.gen_testcases(
                    clazz=sut_class,
                    classpath=gen_classpath,
                    workdir=workdir,
                    seed=random_seed,
                    budget=config.get('time_budget_seconds', 0),
                    max_tests=config.get('max_tests', 100000000),
                    randoop_home=RANDOOP_HOME, randoop_jar=RANDOOP_JAR,
                    **kwargs,
                ) == 0, 'Randoop test generation process failed'
            # Postprocess test files
            test_files = list(randoop.list_test_files(workdir))
            #randoop.strip_assertions(test_files=map(lambda f: join(workdir, f), test_files))
            # Compile Randoop tests
            for test_file in test_files:
                log(f'* Compiling: {test_file}')
                assert java.javac(test_file, classpath=[*RANDOOP_TEST_DEPENDENCIES, *gen_classpath], workdir=workdir) == 0, 'Failed to compile Randoop tests'
                yield workdir, splitext(test_file)[0]

def generate_tests(test_inputs_generator, sut_class, sut_class_relpath, output_dir, build_dir, config, sut_classpath=[], log=log_stdout, **kwargs):
    if test_inputs_generator.lower() == 'evosuite':
        evosuite_test_dirs, evosuite_main_test_class = generate_evosuite_tests(sut_class=sut_class,
            sut_class_relpath=sut_class_relpath, output_dir=output_dir, build_dir=build_dir,
            sut_classpath=sut_classpath, config=config['evosuite'], log=log, **kwargs)
        return ((evosuite_test_dir, evosuite_main_test_class) for evosuite_test_dir in evosuite_test_dirs)
    elif test_inputs_generator.lower() == 'randoop':
        return generate_randoop_tests(sut_class=sut_class,
            output_dir=output_dir, build_dir=build_dir,
            sut_classpath=sut_classpath, config=config['randoop'], log=log, **kwargs)
    else:
        raise Exception(f'Unknown test_inputs_generator: `{test_inputs_generator}`')

def generate_method_test_inputs(source_file, sut_class, sut_class_relpath, sut_classpath, method_name, method_index,
        instrumented_build_dir, test_inputs_dir, test_classes, max_tests=None, sample_seed=0, log=log_stdout, add_opens=None):
    '''
    Instrument + Compile + Execute JUnit tests with the original system
    '''
    with JavaVersion(JAVA8):
        if add_opens is None:
            add_opens = ADD_OPENS
        system_id = SYSTEM_ID(sut_class, method_name, method_index)
        method_tests_dir = join(test_inputs_dir, system_id)
        if isnonemptydir(method_tests_dir):
            log(f'* Found existing method test inputs: {method_tests_dir}')
            return False
        else:
            log(f'* Generating method test inputs into: {method_tests_dir}')
            sut_relpath, _ = split(sut_class_relpath)
            method_instrumented_build_dir = join(instrumented_build_dir, system_id)
            makedirs(join(method_instrumented_build_dir, sut_relpath), exist_ok=True)
            instrumented_source_file = join(method_instrumented_build_dir, sut_relpath, split(source_file)[1])
            # Copy + instrument + compile java source
            copyfile(src=source_file, dst=instrumented_source_file)
            assert instrument_method(args=[
                'replace',
                instrumented_source_file,
                system_id,
                method_name, method_index,
                SERIALIZE_INPUTS_VISITOR
            ]) == 0, f'Failed to instrument method {system_id}'
            assert java.javac(
                instrumented_source_file,
                classpath=[*sut_classpath, GASSERT_JAR], # FIXME: This classpath means that classpath can override classes used by GAssert!
                outdir=method_instrumented_build_dir
            ) == 0, 'Failed to compile instrumented source file'
            # Run Evosuite tests on instrumented classfile to generate serialized inputs
            os.environ['gassert_outdir'] = method_tests_dir
            if type(test_classes) is str:
                test_classes = [test_classes]
            else:
                test_classes = list(test_classes)
            for i, (test_dir, test_class) in enumerate(test_classes, start=1):
                log(f'* Running JUnit tests for {sut_class} ({i}/{len(test_classes)})')
                tests_classpath = [method_instrumented_build_dir, *sut_classpath, *EVOSUITE_RUN_DEPENDENCIES, test_dir]
                java.java(main_class=JUNIT_MAIN_CLASS, 
                    jvm_args=add_opens,
                    classpath=tests_classpath, args=[test_class], workdir=test_dir)
            # Process MethodTests to remove corrupt or duplicate files, and rename them
            method_test_processor(args=[method_tests_dir], classpath=[method_instrumented_build_dir])
            if max_tests is not None:
                method_test_sample(tests_dir=method_tests_dir, max_tests=max_tests, seed=sample_seed)
            method_test_rename(tests_dir=method_tests_dir)
            return True

def generate_mutants(source_file, sut_class, sut_classpath, output_dir, mutants_dir, sut_class_relpath, original_system_name, log=print):
    '''
    Generate mutant source files with Major
    '''
    sut_dir_relpath = dirname(sut_class_relpath)
    source_filename = split(source_file)[1]
    original_root = join(mutants_dir, original_system_name)
    original_dir= join(original_root, sut_dir_relpath)
    original_source = join(original_dir, source_filename)
    mutants_logfile = join(output_dir, MUTANTS_LOGFILE_CLASS.format(sut_class=sut_class))
    if isfile(mutants_logfile):
        log(f'* Found mutants logfile: {mutants_logfile}')
        return False
    else:
        log(f'* Running Major on: {source_file}')
        with JavaVersion(JAVA8):
            # Generate mutants with Major
            assert major.run(source_file=source_file, classpath=sut_classpath, workdir=output_dir, major_home=MAJOR_HOME) == 0, 'Failed to run Major on original source file'
            remove(f'{splitext(source_file)[0]}.class')
            # Copy original source file to mutants dir
            makedirs(original_dir, exist_ok=True)
            copyfile(source_file, original_source)
            # Move mutants log file to class-specific path
            move(src=join(output_dir, MUTANTS_LOGFILE), dst=mutants_logfile)
            return True

def generate_method_states_sut(source_file, sut_class, sut_classpath, sut_class_relpath,
        method_name, method_index, mutants_dir,
        relevant_mutants, states_dir, test_inputs_dir, original_system_name, log=log_stdout):
    system_id = SYSTEM_ID(sut_class, method_name, method_index)
    if isnonemptydir(states_dir):
        log(f'* Found existing GAssert states: {states_dir}')
        return False
    else:
        log(f'  * SUT {system_id} has {len(relevant_mutants)} relevant mutants')
        # Process original SUT
        log(f'  * Processing original SUT...')
        original_root = join(mutants_dir, original_system_name)
        generate_method_states_system(sut_class, sut_classpath, method_name, method_index, original_system_name, original_root,
                    source_file, sut_class_relpath, states_dir, test_inputs_dir, log)
        if not isnonemptydir(states_dir):
            log(f'* GAssert states were not created: {states_dir}')
            makedirs(states_dir, exist_ok=True)
            return False
        # Remove corrupt states from the original system
        remove_corrupt_states(states_dir=states_dir, log=log)
        # Discard test inputs that did not run successfully in the original SUT
        successful_method_tests = set(find_successful_method_tests(states_dir=states_dir))
        method_test_inputs = filter(
            lambda f: f.endswith('.methodinputs') and isfile(join(test_inputs_dir, f)),
            listdir(test_inputs_dir),
        )
        for test_inputs in filter(lambda t: get_file_testid(filename=t, extension='.methodinputs') not in successful_method_tests, method_test_inputs):
            log(f'* Method test "{test_inputs}" discarded')
            remove(join(test_inputs_dir, test_inputs))
        # For each mutant
        log(f'  * Processing mutants...')
        for mutant_id, mutant_root in major.list_mutants(mutants_dir):
            if mutant_id in relevant_mutants:
                # Rename mutant
                mutant_id = f'M{mutant_id}'
                # Process mutant
                generate_method_states_system(sut_class, sut_classpath, method_name, method_index, mutant_id, mutant_root,
                    source_file, sut_class_relpath, states_dir, test_inputs_dir, log)
        # Remove corrupt states
        remove_corrupt_states(states_dir=states_dir, log=log)
        return True
                
def generate_method_states_system(sut_class, sut_classpath, method_name, method_index, mutant_id, mutant_root,
        source_file, sut_class_relpath, states_dir, test_inputs_dir, log=log_stdout):
    '''
    Instrument + Compile + Execute method call tests on a single mutant
    '''
    system_id = SYSTEM_ID(sut_class, method_name, method_index)
    system_id_mutant = SYSTEM_ID_MUTANT(system_id, mutant_id)
    sut_dir_relpath = dirname(sut_class_relpath)
    source_filename = split(source_file)[1]
    mutant_dir = join(mutant_root, sut_dir_relpath)
    mutant_source = join(mutant_dir, source_filename)
    sut_mutant_root = join(mutant_root, system_id)
    sut_mutant_dir = join(sut_mutant_root, sut_dir_relpath)
    sut_mutant_source = join(sut_mutant_dir, source_filename)
    makedirs(sut_mutant_dir, exist_ok=True)
    copyfile(src=mutant_source, dst=sut_mutant_source)
    with JavaVersion(JAVA8):
        # Instrument + compile source files
        log(f'  * Instrument + compile: {system_id_mutant}')
        assert instrument_method(args=['replace', sut_mutant_source, system_id_mutant, method_name, method_index, SERIALIZE_STATES_VISITOR]) == 0, f'Failed to instrument {system_id_mutant}'
        # Major can generate mutants which do not compile (e.g. unreachable branches (e.g. while(false)) or assigning null to primitive variables)
        compiled = java.javac(sut_mutant_source, classpath=[sut_mutant_root, *sut_classpath, GASSERT_JAR], outdir=sut_mutant_root) == 0
        # Setup + run method call tests
        if compiled: # Just ignore mutants which could not be compiled.
            log(f'  * Running method call tests with classpath: {sut_mutant_root}')
            os.environ['gassert_outdir'] = states_dir
            retcode = method_test_executor_dir_processes(dir=test_inputs_dir, classpath=[sut_mutant_root, *sut_classpath])
            if retcode != 0:
                raise Exception(f'Method tests executor did not exit normally')
        log(f'  * Finished processing: {mutant_id}')

def generate_classifications(states_dir, classifications_regular_dir, original_system, log=log_stdout):
    if isnonemptydir(classifications_regular_dir):
        log(f'* Found: {classifications_regular_dir}')
        return False
    else:
        # Generate state classifications
        log(f'* Generating classifications into: {classifications_regular_dir}')
        classify_states(states_dir=states_dir, output_dir=classifications_regular_dir, original_system=original_system)
        return True

def generate_test_executions_sut_class(sut_config: SUTConfig, sut_class: str, config: dict, methods: list=None, log=log_stdout):
    output_dir = config['paths']['output_dir']
    build_dir = realpath(join(output_dir, config['paths']['build_dir']))
    makedirs(build_dir, exist_ok=True)
    instrumented_build_dir = realpath(join(output_dir, config['paths']['instrumented_build_dir']))
    test_inputs_dir = realpath(join(output_dir, config['paths']['test_inputs_dir']))
    mutants_dir = realpath(join(output_dir, config['paths']['mutants_dir']))
    states_dir = realpath(join(output_dir, config['paths']['states_dir']))
    original_system_name = config['paths']['original_system_name']
    if sut_class.endswith('.class'):
        sut_class = sut_class[:-len('.class')]
    sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    sut_class_path = join(build_dir, sut_class_relpath)
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    sut_classpath = list(map(realpath, sut_config.classpaths))
    if methods is None:
        methods=list(sut_config.iter_class_methods(sut_class))
    # Format the original source file so that it matches the formatting after instrumentation
    assert format_java_source(source_file) == 0, f'Error formatting source file: "{source_file}"'
    # Compile original source file
    compile_source_file(source_file=source_file, sut_class_path=sut_class_path, sut_classpath=sut_classpath, build_dir=build_dir, log=log)
    # Generate + run tests
    test_classes = list(generate_tests(test_inputs_generator=config['test_inputs_generator'], 
            sut_class=sut_class, sut_class_relpath=sut_class_relpath, output_dir=output_dir,
            build_dir=build_dir, sut_classpath=sut_classpath, config=config, log=log))
    # Generate mutants
    if generate_mutants(source_file=source_file, sut_class=sut_class, sut_classpath=sut_classpath, output_dir=output_dir,
        mutants_dir=mutants_dir, sut_class_relpath=sut_class_relpath,
        original_system_name=original_system_name, log=log):
        pass # FIXME: remove_unused_mutants_all deletes mutants for other classes
        # mutants_logfile = join(output_dir, MUTANTS_LOGFILE_CLASS.format(sut_class=sut_class))
        # remove_unused_mutants_all(mutants_dir=mutants_dir, source_file=source_file, mutants_logfile=mutants_logfile, methods=list(sut_config.iter_class_methods(sut_class)))
    for method_name, method_index in methods:
        system_id = SYSTEM_ID(sut_class, method_name, method_index)
        test_inputs_dir_system = join(test_inputs_dir, system_id)
        states_dir_system = join(states_dir, system_id)
        # Generate test inputs
        generate_method_test_inputs(source_file=source_file, sut_class=sut_class, 
            sut_class_relpath=sut_class_relpath, sut_classpath=sut_classpath,
            method_name=method_name, method_index=method_index,
            instrumented_build_dir=instrumented_build_dir,
            test_inputs_dir=test_inputs_dir,
            test_classes=test_classes, max_tests=config.get('max_tests'), log=log)
        # Find out which mutants are relevant for this SUT
        relevant_mutants = find_relevant_mutants(source_file=source_file,
            mutants_logfile=join(output_dir, MUTANTS_LOGFILE_CLASS.format(sut_class=sut_class)),
            method_name=method_name, method_index=method_index)
        # Execute tests on each mutant
        generate_method_states_sut(source_file=source_file, sut_class=sut_class, sut_classpath=sut_classpath,
            sut_class_relpath=sut_class_relpath, method_name=method_name, method_index=method_index,
            mutants_dir=mutants_dir, relevant_mutants=relevant_mutants, states_dir=states_dir_system,
            test_inputs_dir=test_inputs_dir_system, original_system_name=original_system_name, log=log)

def generate_test_executions(sut_config: SUTConfig, experiment_config: dict, log=log_stdout):
    for sut_class in sut_config.suts:
        generate_test_executions_sut_class(sut_config=sut_config, sut_class=sut_class, config=experiment_config, log=log)

def main():
    import sys
    if len(sys.argv) != 3:
        print('./generate_test_executions.py <SUT_CONFIG> <EXPERIMENT_CONFIG>')
        exit(1)
    generate_test_executions(
        sut_config=SUTConfig.from_filename(sys.argv[1]),
        experiment_config=ExperimentConfig.from_filename(sys.argv[2]),
    )
