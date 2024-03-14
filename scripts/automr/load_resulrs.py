import settings
import os
import numpy as np
import pandas as pd
import pickle
from Phase3_RemoveRedundancy import *
import re
import ntpath

# MRs should be a dict: {parameters: [A, B], parameters: [A, B]}
def load_npz_to_pandas(result_path):
    file_name = ntpath.basename(result_path)
    func_index = int(file_name[0:file_name.find('_')])
    parameters = file_name[-26:-17]
    parameters_int = [int(e) for e in parameters.split("_")]

    candidates_all = np.load(result_path)
    A_candidates = candidates_all["A_candidates"]
    B_candidates = candidates_all["B_candidates"]
    MRs = {parameters: [A_candidates, B_candidates]}

    hNOI = parameters_int[0]
    hDIR = parameters_int[3]
    hDOR = parameters_int[4]

    # to store all distinct As
    x_all = {}

    NEI = settings.getNEI(func_index)
    NEO = settings.getNEO(func_index)

    hu = str_comb([f"i0_{i+1}" for i in range(NEI)], hDIR)

    MR_all = []
    for parameters, AB_after_CS in MRs.items():
        parameters_int = [int(e) for e in parameters.split("_")]
        NOI = parameters_int[0]
        MIR = parameters_int[1]
        MOR = parameters_int[2]
        DIR = parameters_int[3]
        DOR = parameters_int[4]

        As = AB_after_CS[0]
        Bs = AB_after_CS[1]

        u = str_comb([f"i0_{i+1}" for i in range(NEI)], DIR)

        for i_A in range(As.shape[0]):
            A = As[i_A]

            # store the fx
            o_orig = ["o0"]

            for i_NOI in range(A.shape[0]):
                A_iNOI = A[i_NOI]

                # check whether add a new x or not
                x_temp = pd.DataFrame(columns=hu)
                for i_EOI in range(NEI):
                    x_temp_iEOI = pd.DataFrame([A_iNOI[i_EOI]], columns=u, index=[f"e{i_EOI+1}"])
                    x_temp = pd.concat([x_temp, x_temp_iEOI], ignore_index=False, sort=False)
                    #x_temp = x_temp.append(x_temp_iEOI, ignore_index=False, sort=False)
                    x_temp = x_temp.fillna(0)
                # print(x_temp)
                isNew = True
                for x, A_x in x_all.items():
                    # print(A_x)
                    # print(x_temp.values)
                    isExist = np.allclose(A_x, x_temp.values,atol=0.05, rtol=0.1, equal_nan=True)
                    # print(isExist)
                    if isExist:
                        o_orig.append(f"o{x}")
                        isNew = False
                        break
                if isNew:
                    # print(len(x_all))
                    number_of_x = len(x_all)
                    x_all[f"i{number_of_x + 1}"] = x_temp.values
                    o_orig.append(f"o{number_of_x + 1}")
                    # print(o_orig)

            # create corresponding output elemets
            o = []
            for i in range(len(o_orig)):
                for i_ele in range(NEO):
                    o.append(f"{o_orig[i]}_{i_ele + 1}")
            # print(o)

            # create v
            v = str_comb(o, DOR)
            # print(v)

            MR = pd.DataFrame([Bs[i_A]], columns=v)
            MR = MR.groupby(MR.columns, axis=1).sum()
            # print(MR.columns)
            MR_all.append(MR)

    # for i in range(len(MR_all)):
    #     print(MR_all[i].columns)

    MR_all_df = pd.concat((df for df in MR_all), ignore_index=True, sort=True)
    y_all = MR_all_df.columns
    # print(len(y_all))
    MR_all_df = MR_all_df.fillna(0)

    df_x_all = pd.DataFrame(columns=hu)
    for k, v in x_all.items():
        for idx_e in range(NEI):
            df_x_all.loc[f'{k}_{idx_e+1}'] = v[idx_e]

    MR_all_df.index = [f'MR{i+1}' for i in MR_all_df.index.values]
    return parameters, df_x_all, MR_all_df

def load_phase3_results(result_path):
    file_name = ntpath.basename(result_path)
    if result_path.endswith(".npz"):
        parameters, df_x_all, df_y_all = load_npz_to_pandas(result_path)
        print(df_x_all.to_string())
        print(df_y_all.to_string())
        return {parameters:[df_x_all, df_y_all]}

    # for the MRs stored in pkl format
    elif result_path.endswith(".pkl"):
        dict_MRs = {}
        with open(result_path, "rb") as f:
            MRs_dict = pickle.load(f)
        func_index = int(file_name[0:file_name.find('_')])
        NEI = settings.getNEI(func_index)
        for parameters, MRs in MRs_dict.items():
            print('-'*10)
            print(f"func_index is {func_index}, NOI_MIR_MOR_DIR_DOR is {parameters}:")
            parameters_int = [int(e) for e in parameters.split("_")]
            NOI = parameters_int[0]
            MIR = parameters_int[1]
            MOR = parameters_int[2]
            DIR = parameters_int[3]
            DOR = parameters_int[4]

            x_all_dict = MRs[0]
            u = str_comb([f"i0_{i+1}" for i in range(NEI)], DIR)

            df_x_all = pd.DataFrame(columns=u)
            for k, v in x_all_dict.items():
                for idx_e in range(NEI):
                    df_x_all.loc[f'{k}_{idx_e+1}'] = v[idx_e]

            y_all_df = MRs[1]
            # print(type(y_all_df))
            y_all_df.index = [f'MR{i+1}' for i in y_all_df.index.values]
            print(df_x_all.to_string())
            print(y_all_df.to_string())
            dict_MRs[parameters] = [df_x_all, y_all_df]
        return dict_MRs


if __name__ == '__main__':
    MRs_path = settings.output_path
    func_indices = settings.func_indices
    result_files = os.listdir(f"{MRs_path}/phase3")
    for result_file in result_files:
        print(f'result file is {result_file}')
        func_index = int(result_file[0:result_file.find('_')])
        MRs = load_phase3_results(f"{MRs_path}/phase3/{result_file}")
        for k, v in MRs.items():
            parameters_int = [int(e) for e in k.split("_")]
            NOI = parameters_int[0]
            MIR = parameters_int[1]
            MOR = parameters_int[2]
            DIR = parameters_int[3]
            DOR = parameters_int[4]

            map_relation = {1:"=", 2:">", 3:"<"}

            with open(f"{MRs_path}/{k}.html", 'w') as _file:
                _file.write(f'<p>func index is {func_index} </p>')
                _file.write(f'<p>Mode of Input Relation is "{map_relation[MIR]}" </p>')
                _file.write(f'<p>Mode of Output Relation is "{map_relation[MOR]}" </p>')
                _file.write(f'<p>Input Relation is: </p>')
                orig_columns = v[0].columns
                fixed_columns = [col.replace(",", "") if col.endswith(",)") else col for col in orig_columns ]
                v[0].columns = fixed_columns
                _file.write(v[0].round(2).to_html())
                _file.write(r"</br>")
                _file.write(f'<p>Output Relation is: </p>')
                y_orig_columns = v[1].columns
                y_fixed_columns = [col.replace(",", "") if col.endswith(",)") else col for col in y_orig_columns]
                v[1].columns = y_fixed_columns
                _file.write(v[1].round(2).to_html())
