#!/usr/bin/env python3

import math

import os
from os import makedirs, listdir
from os.path import isfile, split, join
from random import Random
from shutil import copyfile

from states.cmrip import cmrip
from states.instrument_method_variables import instrument_method_variables
from states.method_test_executor import method_test_executor_dir_processes
from states.method_test_transformer import method_test_transformer
from states.method_test_transformer_config import merge_values_lists_dir, method_test_transformer_config
from states.mrinfo_generator import mrinfo_generator
from tools.java import javac, JavaVersion
from util.fsutil import isnonemptydir, read_lines
from util.log import log_stdout

from config import GASSERT_JAR, JAVA8, SEPARATORS

def constant_weights_from_counts(constant_counts):
    if not constant_counts:
        return {}
    max_count = max(map(float, constant_counts.values()))
    return { float(c): max(float(constant_counts[c]) / max_count, 0.25) for c in constant_counts}

def get_mrip_constant(mrip_name):
    # Transformation?1.0@2.0?1@2.state.json
    parts = mrip_name.split(SEPARATORS[1])
    if len(parts) not in [1, 2, 3]:
        raise Exception(f'Cannot parse MRIP name: "{mrip_name}"')
    if len(parts) == 3:
        params = list(filter(lambda p: p, parts[1].split(SEPARATORS[0])))
        if len(params) > 0:
            # If there are multiple params, we only consider the first one
            return float(params[0])
    return None

MAGIC_CONSTANTS = [math.pi / 2, math.pi, 2 * math.pi, math.e]
CONSTANTS_EPSILON = .01
def select_mrips(mrip_names, rng: Random, number: int, constant_counts: dict):
    constant_weights= constant_weights_from_counts(constant_counts)
    def mrip_weight(mrip):
        constant = get_mrip_constant(mrip)
        if constant is None:
            return 0.5
        if any(map(lambda c: abs(constant - c) < CONSTANTS_EPSILON, MAGIC_CONSTANTS)):
            return 1.0
        matching_constants = list(filter(lambda c: abs(constant - c) < CONSTANTS_EPSILON, constant_weights))
        if matching_constants:
            return max(map(lambda c: constant_weights[c], matching_constants))
        else:
            return 0.25
    mrips = list(mrip_names)
    for _ in range(min(number, len(mrips))):
        weights = [mrip_weight(mrip) for mrip in mrips]
        selected_mrip = rng.choices(population=mrips, weights=weights, k=1)[0]
        mrips.remove(selected_mrip)
        yield selected_mrip

def generate_sampled_transformations(transformations_file: str, sampled_file: str, rng: Random, number: int, constant_counts: dict):
    mrip_names = read_lines(transformations_file)
    selected_mrips = select_mrips(mrip_names=mrip_names, rng=rng, number=number, constant_counts=constant_counts)
    with open(sampled_file, mode='w', encoding='utf-8', newline='\n') as fp:
        for mrip in selected_mrips:
            fp.write(f'{mrip}\n')

def generate_inputs_transformer_config(source_file, sut_class_relpath, instrumented_build_dir, test_inputs_dir,
        sut_classpaths, system_id, method_name, method_index, constants_dir, transformations_file, log=log_stdout):
    constants_file = join(constants_dir, f'{system_id}.constants.txt')
    if isfile(constants_file):
        log(f'* Found: {constants_file}')
    else:
        log(f'* Generating: {constants_file}')
        # Copy source file into instrumented_variables_build dir
        sut_relpath, _ = split(sut_class_relpath)
        method_instrumented_build_dir = join(instrumented_build_dir, system_id)
        makedirs(join(method_instrumented_build_dir, sut_relpath), exist_ok=True)
        instrumented_source_file = join(method_instrumented_build_dir, sut_relpath, split(source_file)[1])
        # Copy + instrument + compile java source
        copyfile(src=source_file, dst=instrumented_source_file)
        with JavaVersion(JAVA8):    
            instrument_method_variables(args=['replace', instrumented_source_file, system_id, method_name, method_index])
            assert javac(
                instrumented_source_file,
                classpath=[method_instrumented_build_dir, *sut_classpaths, GASSERT_JAR], # FIXME: This classpath means that SUT classpaths can override classes used by GAssert!
                outdir=method_instrumented_build_dir
            ) == 0, 'Failed to compile instrumented source file'
        # Execute test suite on instrumented SUT
        os.environ['gassert_outdir'] = constants_dir
        retcode = method_test_executor_dir_processes(dir=test_inputs_dir, classpath=[method_instrumented_build_dir, *sut_classpaths])
        if retcode != 0:
            raise Exception(f'Method tests executor did not exit normally')
        merge_values_lists_dir(constants_dir, constants_file)
    if isfile(transformations_file):
        log(f'* Found: {transformations_file}')
    else:
        log(f'* Generating: {transformations_file}')
        # Generate method transformer config based on extracted constants
        sample_method_test = join(test_inputs_dir, next(filter(
            lambda f: f.endswith('.methodinputs') and isfile(join(test_inputs_dir, f)),
            listdir(test_inputs_dir),
        )))
        assert method_test_transformer_config(
            args=[constants_file, transformations_file, sample_method_test],
            classpath=sut_classpaths,
        ) == 0, 'Error running method_test_transformer_config'

def generate_transformed_method_inputs(test_inputs_dir, state_file, transform_test_inputs_dir, classpath, transformations_file=None, log=log_stdout):
    if isnonemptydir(transform_test_inputs_dir):
        log(f'* Found: {transform_test_inputs_dir}')
    else:
        log(f'* Generating transformed method inputs into: {transform_test_inputs_dir}')
        assert state_file is not None, 'method_test_transformer: state_file is None'
        args = [test_inputs_dir, state_file, transform_test_inputs_dir]
        if transformations_file is not None:
            args.append(transformations_file)
        assert method_test_transformer(
            args=args,
            classpath=classpath,
        ) == 0, 'Error running MethodTestTransformer'

def generate_mrips_and_mrinfos_files(transform_test_inputs_dir, mrips_path, mrinfos_path, states_dir, original_system_id, log=log_stdout):
    if isfile(mrips_path):
        log(f'* Found MRIPs: {mrips_path}')
    else:
        # Piece MRs together from .cmrip files
        log(f'* Writing MRIPs to: {mrips_path}')
        assert cmrip(args=['generateMRIPs', transform_test_inputs_dir, mrips_path]) == 0, 'Error generating MRIPs'
    if isfile(mrinfos_path):
        log(f'* Found MRInfos: {mrinfos_path}')
    else:
        # Generate MRInfos
        log(f'* Writing MRInfos to: {mrinfos_path}')
        assert mrinfo_generator(args=[states_dir, original_system_id, mrips_path, mrinfos_path]) == 0, 'Error generating MRInfos'
