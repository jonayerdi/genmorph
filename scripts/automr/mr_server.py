#!/usr/bin/env python3
from os import listdir
from os.path import join, isdir, basename
from threading import Thread
import socket
import traceback

import numpy as np

from automr import Phase1_PSOSearch

MRS_PATH = join('output_dir_automr_math', 'automrs_commonsmath2')
PORT = 1337

def get_verdict_of_AB(program, func_index, A, B, i0, mode_input_relation, mode_output_relation,
                   degree_of_input_relation,
                   degree_of_output_relation,
                   #log=print,
                   log=lambda *args, **kwargs: None,
                   ):
    # i0 should be a one-dimensional np.array with settings.getNEI(func_index) input values
    comb_i0 = Phase1_PSOSearch.comb(i0, degree_of_input_relation)
    try:
        log(f'A={A}')
        i = Phase1_PSOSearch.generate_i(func_index, i0, comb_i0, A, mode_input_relation)
        log(f'i={i}')
        o = Phase1_PSOSearch.get_o(program, func_index, i)
        o_flatten = np.ravel(o)
        comb_o = Phase1_PSOSearch.comb(o_flatten, degree_of_output_relation)
        log(f'o_flatten={o_flatten}')
        log(f'B={B}')
        log(f'comb_o={comb_o}')
        distance = np.dot(B, comb_o)
        log(f'distance={distance}')
        log(f'mode_output_relation={mode_output_relation}')
        verdict = {
            1: np.abs(distance) < 0.05, # equals
            2: distance > 0,            # greater-than
            3: distance < 0,            # less-than
        }[mode_output_relation]
        log(f'verdict={verdict}')
        return verdict
    except Exception:
        traceback.print_exc()
        return False

def load_results_file(result_path):
    if result_path.endswith(".npz"):
        file_name = basename(result_path)
        func_index = int(file_name[0:file_name.find('_')])
        parameters = file_name[-26:-17]
        candidates_all = np.load(result_path)
        A_candidates = candidates_all["A_candidates"]
        B_candidates = candidates_all["B_candidates"]
        MRs = {(func_index, parameters): [A_candidates, B_candidates]}
        return MRs
    return {}

def load_results_dir(result_dir, condition=lambda f: True):
    MRs = {}
    for file in listdir(result_dir):
        if condition(join(result_dir, file)):
            MRs_file = load_results_file(join(result_dir, file))
            for key in MRs_file:
                MRs[key] = MRs_file[key]
    return MRs

def load_results(results_root, condition=lambda f: True):
    MRs = {}
    for seed in listdir(results_root):
        seed_dir = join(results_root, seed, 'phase2')
        if isdir(seed_dir):
            MRs[seed] = load_results_dir(result_dir=seed_dir, condition=condition)
    return MRs

def read_message(socket):
    # Receive response
    data = bytes()
    while True:
        data += socket.recv(512)
        if data[-1] == b'\n'[-1]:
            break
    # Decode UTF-8 and return
    return bytes.decode(data, encoding="utf-8").strip()

def write_message(socket, args):
    socket.sendall(f"{';'.join(map(str, args))}\n".encode('utf-8'))

def to_float(s):
    value = {
        'true': 1,
        'false': 0,
    }.get(s)
    if value is None:
        value = float(s)
    return value

def program_fn(socket, i):
    write_message(socket=socket, args=['call', *i])
    response = read_message(socket=socket)
    return [to_float(i) for i in response.split(';')]

def client_loop(mrs, socket):
    message = read_message(socket)
    split = message.split(';')
    seed = split[0]
    func_index = int(split[1])
    parameters = split[2]
    mr_index = int(split[3])
    input = split[4:]
    
    A_all, B_all = mrs[seed][(func_index, parameters)]
    A, B= A_all[mr_index], B_all[mr_index]
    i0 = np.array([float(i) for i in input])

    parameters_int = [int(e) for e in parameters.split("_")]
    NOI = parameters_int[0]
    MIR = parameters_int[1]
    MOR = parameters_int[2]
    DIR = parameters_int[3]
    DOR = parameters_int[4]

    program = lambda i, func_index, socket=socket: program_fn(socket=socket, i=i)

    verdict = get_verdict_of_AB(
        program=program, func_index=func_index,
        A=A, B=B, i0=i0,
        mode_input_relation=MIR,
        mode_output_relation=MOR,
        degree_of_input_relation=DIR,
        degree_of_output_relation=DOR,
    )

    write_message(socket=socket, args=['verdict', '1' if verdict else '0'])

def mr_server(mrs_path=MRS_PATH, host='0.0.0.0', port=PORT,
              #log=print,
              log=lambda *args, **kwargs: None,
              ):
    MRs = load_results(mrs_path)
    serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    serversocket.bind((host, port))
    serversocket.listen(5)
    print(f'[mr_server.py] Listening at {host}:{port}')
    while True:
        address = None
        try:
            (clientsocket, address) = serversocket.accept()
            log(f'[mr_server.py] Client connected from: {address}')
            ct = Thread(target=client_loop, kwargs={ 'mrs': MRs, 'socket': clientsocket })
            ct.run()
            clientsocket.close()
        except Exception:
            log(f'[mr_server.py] Exception: {traceback.format_exc()}')
        log(f'[mr_server.py] Client disconnected: {address}')

def main():
    mr_server()

if __name__ == '__main__':
    main()
