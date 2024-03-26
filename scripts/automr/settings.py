import numpy as np
import sys

lib = None
if 'JavaPythonBridge' in sys.modules:
    from JavaPythonBridge.client.ClientBridge import ClientBridge
    lib = ClientBridge()

map_index_func = {1:'nextprime', 2:'isprime', 3:'gcd', 4:'pow', 5:'stirling', 6:'acos', 7:'log10', 8:'sin', 9:'sinh', 10:'tan'}

# which programs to infer MRs from
func_indices = list(range(1, 11))

# which type of MRs to infer: NOI_MIR_MOR_DIR_DOR.
# NOI: number of involved inputs
# MIR, MOR: mode of input and output relations. 1-equal, 2-greaterthan, 3-lessthan
# DIR, DOR: degrees of input and output relations. 1-linear, 2-quadratic, etc.
parameters_collection = ["2_1_1_1_1", "2_1_1_1_2", "2_1_1_1_3", "3_1_1_1_1", "3_1_1_1_2", "2_1_2_1_1", "2_1_3_1_1", "2_2_1_1_1", "2_3_1_1_1", "2_2_2_1_1", "2_2_3_1_1", "2_3_2_1_1", "2_3_3_1_1"]

def init():
    lib._connect()

def deinit():
    lib._disconnect()

# program to infer MR from
# user can encapsulate his program under test here and assign it a func_index, which can be used to faciliate processing for multiple programs in a batch
# i is the array containing the input arguments (note that unary input is treated as a special case of multi-variate input)
# o is the array containing the returning results (note that unary output is treated as a special case of multi-variate output)
# func_index is assigned to facilitate batch processing for multiple programs
def program(i, func_index):
    if func_index == 1:
        o = lib.nextprime(i)
    elif func_index == 2:
        o = 1 if lib.isprime(i) else 0
    elif func_index == 3:
        o = lib.gcd(i[0], i[1])
    elif func_index == 4:
        o = lib.pow(i[0], i[1])
    elif func_index == 5:
        o = lib.stirling(i[0], i[1])
    elif func_index == 6:
        o = lib.acos(i)
    elif func_index == 7:
        o = lib.log10(i)
    elif func_index == 8:
        o = lib.sin(i)
    elif func_index == 9:
        o = lib.sinh(i)
    elif func_index == 10:
        o = lib.tan(i)

    return o

# the number of elements of the input for the programs
def getNEI(func_index):
    if func_index in [3, 4, 5]:
        return 2
    else:
        return 1

# the number of elements of the output for the programs
def getNEO(func_index):
    return 1

# domain for each element of the input
INPUT_RANGE = [0, 20]
#INPUT_RANGE = [-20, 20]
def get_input_range(func_index):
    if func_index in [3, 4, 5]:
        return [INPUT_RANGE, INPUT_RANGE]
    else:
        return [INPUT_RANGE]

# datatype for each element of the input:
def get_input_datatype(func_index):
    if func_index in [1, 2]:
        return [int]
    elif func_index in [3, 4, 5]:
        return [int, int]
    else:
        return [float]



# path to store results
output_path_format = "./output/commonsmath/{seed}"
output_path = None

random_seeds = [
    11, 12, 13,
    21, 22, 23,
    31, 32, 33,
    41, 42, 43,
]

# parameters we used with GenMorph, for assigning equivalent timebudgets
genmorph_runs = 4
genmorph_run_timebudget = 30 * 60

# search parameters
pso_runs = 3
pso_run_timebudget = None # genmorph_run_timebudget * (genmorph_runs / pso_runs) / len(parameters_collection)
pso_iterations = 350



# set type and search range for coeff_range, const_range
coeff_type = int
const_type = float
coeff_range = [-2, 2]
const_range = [-10, 10]


# GenMorph stuff

 # Separators we use for file/directory naming schemes.
# Some characters like '$' or '%' can break tools like Maven on Linux.
# Keep in sync with the definitions for Java: ch.usi.gassert.util.FileUtils
SEPARATORS = ["@", "?"]
SYSTEM_ID = lambda class_name, method_name, method_index: f'{class_name}{SEPARATORS[1]}{method_name}{SEPARATORS[1]}{method_index}'

SYSTEM_IDS = [
    SYSTEM_ID('MathClass', 'nextPrime', '0'),
    SYSTEM_ID('MathClass', 'millerRabinPrimeTest', '0'),
    SYSTEM_ID('MathClass', 'gcd', '0'),
    SYSTEM_ID('MathClass', 'pow', '0'),
    SYSTEM_ID('MathClass', 'stirlingS2', '0'),
    SYSTEM_ID('MathClass', 'acos', '0'),
    SYSTEM_ID('MathClass', 'log10', '0'),
    SYSTEM_ID('MathClass', 'sin', '0'),
    SYSTEM_ID('MathClass', 'sinh', '0'),
    SYSTEM_ID('MathClass', 'tan', '0'),
]

FUNC_INDICES = { system_id: i for i, system_id in enumerate(SYSTEM_IDS, start=1) }

def get_func_index(system_id):
    return FUNC_INDICES[system_id]

def get_system_id(func_index):
   return SYSTEM_IDS[func_index - 1]

def get_input_names(func_index):
    if func_index == 1:
        return ['i_n']
    elif func_index == 2:
        return ['i_n']
    elif func_index == 3:
        return ['i_p', 'i_q']
    elif func_index == 4:
        return ['i_k', 'i_e']
    elif func_index == 5:
        return ['i_n', 'i_k']
    elif func_index == 6:
        return ['i_x']
    elif func_index == 7:
        return ['i_x']
    elif func_index == 8:
        return ['i_x']
    elif func_index == 9:
        return ['i_x']
    elif func_index == 10:
        return ['i_x']

def get_output_names(func_index):
    return ['o_return']

