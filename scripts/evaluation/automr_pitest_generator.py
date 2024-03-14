import itertools
from os import listdir, makedirs
from os.path import join, isdir, isfile

from automr.load_resulrs import load_phase3_results
from automr.settings import get_input_names, get_output_names, get_system_id, getNEI, getNEO

import numpy as np

def comb_str(vector, highest_degree):
    comb_vector = ['1']
    degree = 1
    while degree < highest_degree + 1:
        comb_vector_with_degree = np.array([
            '*'.join(element) for element in itertools.combinations_with_replacement(vector, degree)
        ])
        comb_vector.extend(comb_vector_with_degree)
        degree += 1
    return comb_vector

def comb_o(NOI, DOR, element=lambda index: f'o{index}'):
    # comb_str(['sin(x1)','sin(x2)','sin(x3)'], 2)
    # ['1', 'sin(x1)', 'sin(x2)', 'sin(x3)', 'sin(x1)*sin(x1)', 'sin(x1)*sin(x2)', 'sin(x1)*sin(x3)', 'sin(x2)*sin(x2)', 'sin(x2)*sin(x3)', 'sin(x3)*sin(x3)']
    # comb_str(['o1','o2','o3'], 2)
    # ['1', 'o1', 'o2', 'o3', 'o1*o1', 'o1*o2', 'o1*o3', 'o2*o2', 'o2*o3', 'o3*o3']
    return comb_str(vector=[element(i) for i in range(NOI)], highest_degree=DOR)

def _get_cost_of_AB(program, func_index, A, B, i0, mode_input_relation, mode_output_relation,
                   degree_of_input_relation,
                   degree_of_output_relation):
    def distance_ok(distance):
        if np.isreal(distance) and not np.isnan(distance):
            return {
                1: lambda d: np.abs(d) < 0.05,
                2: lambda d: d > 0,
                3: lambda d: d < 0,
            }[mode_output_relation](distance)
        return True
    comb_i0 = comb(i0, degree_of_input_relation)
    i = generate_i(func_index, i0, comb_i0, A, mode_input_relation)
    o = get_o(program, func_index, i)
    o_flatten = np.ravel(o)
    comb_o = comb(o_flatten, degree_of_output_relation)
    distance = np.dot(B, comb_o)
    return distance_ok(distance)

def _expr_var(is_input, function_index, variable_index, source_followup):
    var_name = {
        True: get_input_names,
        False: get_output_names,
    }[is_input](function_index)[variable_index]
    return f'{var_name}_{source_followup}'

def _expr_param():
    ...

def _expr_op(operator, left, right, epsilon=0.05):
    if operator == '=':
        # We use epsilon=0.05 to match AutoMRs implementation
        return f'(Math.abs({left} - {right}) < {epsilon})'
    else:
        f'({left} {operator} {right})'

def _automr2java(operator, params):
    return _expr_op(
        operator=operator,
        left=f'({" + ".join((_expr_param(p) for p in params))})',
        right=f'0.0',
    )

def automr2java(A, B, NOI, MIR, MOR, DIR, DOR, NEI, NEO):
    print(A)
    print(B)
    # Declare inputs
    for i in range(NOI):
        for i in range(NEI):
            value = 
            print(f'double i{i}_1 = ((double){value})')
    # Declare outputs

    # Generate metamorphic relation


def automr2java_files(result_file, system_id, out_dir):
    MRs = load_phase3_results(result_file)

    func_index = int(result_file[0:result_file.find('_')])

    if get_system_id(func_index) != system_id:
        return

    for k, v in MRs.items():

        mr = f'{func_index}_{k}'
        java_or_file = join(out_dir, f'{mr}.jor.txt')

        parameters_int = [int(e) for e in k.split("_")]
        NOI = parameters_int[0]
        MIR = parameters_int[1]
        MOR = parameters_int[2]
        DIR = parameters_int[3]
        DOR = parameters_int[4]
        
        # Input relation
        orig_columns = v[0].columns
        fixed_columns = [col.replace(",", "") if col.endswith(",)") else col for col in orig_columns]
        v[0].columns = fixed_columns
        # Output relation
        y_orig_columns = v[1].columns
        y_fixed_columns = [col.replace(",", "") if col.endswith(",)") else col for col in y_orig_columns]
        v[1].columns = y_fixed_columns
        with open(java_or_file, mode='w', encoding='utf-8', newline='\n') as fd:
            output_relation = automr2java(
                A=v[0], B=v[1],
                NOI=NOI, MIR=MIR, MOR=MOR, DIR=DIR, DOR=DOR,
                NEI=getNEI(func_index), NEO=getNEO(func_index),
            )
            fd.write(output_relation)

def automr_enumerate_dir_mrs(mrs_dir, system_id):
    for experiment in listdir(mrs_dir):
        experiment_sut_dir = join(mrs_dir, experiment, system_id)
        if isdir(experiment_sut_dir):
            for filename in listdir(experiment_sut_dir):
                if filename.endswith('.jor.txt') and isfile(join(experiment_sut_dir, filename)):
                    mr = filename[:-len('.jor.txt')]
                    yield experiment, mr

def automr_generate_mrs_dir(assertions_dir, system_id, out_dir):
    for filename in listdir(assertions_dir):
        file = join(assertions_dir, filename)
        if isfile(file):
            makedirs(out_dir, exist_ok=True)
            automr2java_files(result_file=file, system_id=system_id, out_dir=out_dir)

def automr_pitest_generator():
    ...
