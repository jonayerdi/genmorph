import os
import numpy as np
import pandas as pd
import datetime

import settings
import Phase1_PSOSearch


# the cost function for one particle (a pair of A and B)
def get_cost_of_AB(program, func_index, A, B, i0_all, mode_input_relation, mode_output_relation,
                   degree_of_input_relation,
                   degree_of_output_relation, no_of_elements_output):
    if mode_output_relation == 1:
        cost_of_AB = 1.0

        for index_i0 in range(i0_all.shape[0]):
            i0 = i0_all[index_i0]
            comb_i0 = Phase1_PSOSearch.comb(i0, degree_of_input_relation)
            try:
                i = Phase1_PSOSearch.generate_i(func_index, i0, comb_i0, A, mode_input_relation)
                o = Phase1_PSOSearch.get_o(program, func_index, i)
                o_flatten = np.ravel(o)
                comb_o = Phase1_PSOSearch.comb(o_flatten, degree_of_output_relation)

                distance = np.dot(B, comb_o)
                if np.isreal(distance) and not np.isnan(distance):
                    if np.abs(distance) < 0.05:
                        cost_of_AB -= 1.0 / i0_all.shape[0]
            except:
                pass


    elif mode_output_relation == 2:
        cost_of_AB = 1.0

        for index_i0 in range(i0_all.shape[0]):
            i0 = i0_all[index_i0]
            comb_i0 = Phase1_PSOSearch.comb(i0, degree_of_input_relation)
            try:
                i = Phase1_PSOSearch.generate_i(func_index, i0, comb_i0, A, mode_input_relation)
                o = Phase1_PSOSearch.get_o(program, func_index, i)
                o_flatten = np.ravel(o)
                comb_o = Phase1_PSOSearch.comb(o_flatten, degree_of_output_relation)

                distance = np.dot(B, comb_o)
                if np.isreal(distance) and not np.isnan(distance):
                    if distance > 0:
                        cost_of_AB -= 1.0 / i0_all.shape[0]
            except:
                pass
    elif mode_output_relation == 3:
        cost_of_AB = 1.0

        for index_i0 in range(i0_all.shape[0]):
            i0 = i0_all[index_i0]
            comb_i0 = Phase1_PSOSearch.comb(i0, degree_of_input_relation)
            try:
                i = Phase1_PSOSearch.generate_i(func_index, i0, comb_i0, A, mode_input_relation)
                o = Phase1_PSOSearch.get_o(program, func_index, i)
                o_flatten = np.ravel(o)
                comb_o = Phase1_PSOSearch.comb(o_flatten, degree_of_output_relation)

                distance = np.dot(B, comb_o)
                if np.isreal(distance) and not np.isnan(distance):
                    if distance < 0:
                        cost_of_AB -= 1.0 / i0_all.shape[0]
            except:
                pass

    return cost_of_AB

def phase2(output_path, parameters, func_index, output_name):
    no_of_inputcases = 100

    if os.path.isfile(f"{output_path}/"):
        file_statistics = pd.read_csv(f"{output_path}/counts.csv", index_col=0)
    else:
        file_statistics = pd.DataFrame()

    parameters_int = [int(e) for e in parameters.split("_")]
    no_of_inputs = parameters_int[0]
    mode_input_relation = parameters_int[1]
    mode_output_relation = parameters_int[2]
    degree_of_input_relation = parameters_int[3]
    degree_of_output_relation = parameters_int[4]

    no_of_elements_input = settings.getNEI(func_index)
    no_of_elements_output = settings.getNEO(func_index)

    A_candidates_after_filter = []
    B_candidates_after_filter = []
    ini_count = 0
    survive_count = 0

    results_all = np.load('{}/phase1/{}'.format(output_path, output_name))
    min_cost_candidates = results_all['min_cost_candidates']
    A_candidates = results_all['A_candidates']
    B_candidates = results_all['B_candidates']
    all_count = min_cost_candidates.shape[0]

    for index_candidate in range(all_count):
        min_cost = min_cost_candidates[index_candidate]
        A = A_candidates[index_candidate]
        B = B_candidates[index_candidate]

        isPass = True
        isPassPhase1 = False

        if mode_output_relation == 1:
            if min_cost < 5:
                ini_count += 1
                isPassPhase1 = True
        else:
            if min_cost < 0.05:
                ini_count += 1
                isPassPhase1 = True

        if isPassPhase1:
            for index_test in range(100):
                i0_all = Phase1_PSOSearch.generate_i0_all(settings.get_input_datatype(func_index), settings.get_input_range(func_index), no_of_inputcases)
                survive_cost = get_cost_of_AB(settings.program, func_index, A, B, i0_all, mode_input_relation, mode_output_relation, degree_of_input_relation, degree_of_output_relation, no_of_elements_output)
                if survive_cost >= 0.05:
                    isPass = False
                    break

            if isPass:
                survive_count += 1
                A_candidates_after_filter.append(A)
                B_candidates_after_filter.append(B)

    results_all.close()

    A_candidates_after_filter = np.array(A_candidates_after_filter)
    B_candidates_after_filter = np.array(B_candidates_after_filter)

    if not os.path.isdir("{}/phase2".format(output_path)):
        os.mkdir("{}/phase2".format(output_path))

    np.savez(f'{output_path}/phase2/{func_index}_{parameters}_after_filter.npz', A_candidates=A_candidates_after_filter, B_candidates=B_candidates_after_filter)

    file_statistics.loc[f"{func_index}_{parameters}", "pso"] = all_count
    file_statistics.loc[f"{func_index}_{parameters}", "phase1"] = ini_count
    file_statistics.loc[f"{func_index}_{parameters}", "phase2"] = survive_count

    # file_statistics.to_csv(f"{output_path}/counts.csv")

    # print(f"\n----------")
    # print(f"file is {output_name}, func_index is {func_index}, parameters is {parameters}, all count is {all_count}, ini count is {ini_count}, survive count is {survive_count}")


def run_phase2():
    # print("----------")
    # print("start phase2: filtering...")
    output_path = settings.output_path

    if os.path.isfile(f"{output_path}/performance.csv"):
        times = pd.read_csv(f"{output_path}/performance.csv", index_col=0)
    else:
        times = pd.DataFrame()

    output_names = os.listdir(f"{output_path}/phase1")

    if not os.path.isdir(f"{output_path}/phase2"):
        os.mkdir(f"{output_path}/phase2")

    for output_name in output_names:
        if output_name.endswith(".npz") and output_name[:-4]:
            # exapmle: 21_2_1_1_1_1.npz
            func_index = int(output_name[0:-14])
            parameters = output_name[-13:-4]

            t1 = datetime.datetime.now()
            phase2(output_path, parameters, func_index, output_name)
            t2 = datetime.datetime.now()
            cost_time = np.round((t2-t1).total_seconds(), 3)

            times.loc[f"{func_index}_{parameters}", "phase2"] = cost_time

    # times.to_csv(f"{output_path}/performance.csv")

if __name__ == '__main__':
    run_phase2()