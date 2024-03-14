import os
import pandas as pd
import numpy as np
import itertools
import datetime
from time import time

from scipy import special
import random

import settings

# calculate how many combinations are there for choosing no_of_select elements from no_of_elements
def get_size_of_comb(no_of_elements, no_of_select):
    size_of_comb_with_degree = special.comb(no_of_elements, no_of_select, repetition=True)
    return int(size_of_comb_with_degree)


# given no_of_elements elements, evaluate the number of combinations with a highest_degree
def get_size_of_comb_with_highest_degree(no_of_elements, highest_degree):
    size_of_comb = 0
    degree = 0
    while degree < highest_degree + 1:
        size_of_comb_with_degree = get_size_of_comb(no_of_elements, degree)
        size_of_comb += size_of_comb_with_degree
        degree += 1
    return int(size_of_comb)


# given a vecor, return all the combinations that its elements can form
def comb(vector, highest_degree):
    comb_vector = [1]
    degree = 1
    while degree < highest_degree + 1:
        comb_vector_with_degree = np.array(
            [np.prod(element) for element in itertools.combinations_with_replacement(vector, degree)])
        comb_vector = np.concatenate((comb_vector, comb_vector_with_degree), axis=0)
        degree += 1
    return comb_vector

def match_type(array, types):
    inputcase_typed = []
    for i,e in enumerate(array):
        if types[i] is int:
            inputcase_typed.append(round(e))
        else:
            inputcase_typed.append(e)
    return inputcase_typed

def generate_i0(input_types, input_ranges):
    input_ranges = np.array(input_ranges)
    i0 = np.random.uniform(low=input_ranges[:,0], high=input_ranges[:,1])
    return match_type(i0, input_types)

# generate base inputs (i0) for inputcases
def generate_i0_all(input_types, input_ranges, population_of_inputcases):
    return np.array([generate_i0(input_types, input_ranges) for i in range(population_of_inputcases)])


# given i0 and A, return all the involved inputs for the relation
def generate_i(func_index, i0, comb_i0, A, mode_input_relation):
    i_1_to_end = np.dot(A, comb_i0)

    # equal input relation
    if mode_input_relation == 1:
        i = np.concatenate((i0.reshape(1, -1), i_1_to_end), axis=0)

    # inequality input relation. Only applicable to 2 inputs now.
    else:
        input_range = settings.get_input_range(func_index)
        min_input = np.zeros((i0.shape))
        max_input = np.zeros((i0.shape))
        for index in i0.shape[0]:
            min_input[index] = input_range[index][0]
            max_input[index] = input_range[index][1]

        i_1_to_end_min = np.tile(min_input, (i_1_to_end.shape[0], 1))
        i_1_to_end_max = np.tile(max_input, (i_1_to_end.shape[0], 1))

        if mode_input_relation == 2:
            i_1_to_end = np.random.uniform(low=i_1_to_end, high=i_1_to_end_max, size=i_1_to_end.shape)
            i = np.concatenate((i0.reshape(1, -1), i_1_to_end), axis=0)
        elif mode_input_relation == 3:
            i_1_to_end = np.random.uniform(low=i_1_to_end_min, high=i_1_to_end, size=i_1_to_end.shape)
            i = np.concatenate((i0.reshape(1, -1), i_1_to_end), axis=0)

    input_types = settings.get_input_datatype(func_index)
    for index in range(i.shape[0]):
        i[index] = match_type(i[index], input_types)
    return i


# calculate output(o)
def get_o(program, func_index, i):
    o = []
    for index_i in range(i.shape[0]):
        o.append(program(i[index_i], func_index))
    return o


# the cost function for one particle (a pair of A and B)
def get_cost_of_AB(program, func_index, A, B, i0_all, mode_input_relation, mode_output_relation,
                   degree_of_input_relation,
                   degree_of_output_relation, no_of_elements_output):
    if mode_output_relation == 1:
        cost_of_AB = 0.0

        for index_i0 in range(i0_all.shape[0]):
            i0 = i0_all[index_i0]
            comb_i0 = comb(i0, degree_of_input_relation)
            try:
                i = generate_i(func_index, i0, comb_i0, A, mode_input_relation)
                o = get_o(program, func_index, i)
                o_flatten = np.ravel(o)
                comb_o = comb(o_flatten, degree_of_output_relation)

                distance = np.dot(B, comb_o)
                if not np.isnan(distance):
                    cost_of_AB += np.abs(distance)
                else:
                    cost_of_AB = (cost_of_AB + 1.0) * 10.0
            except:
                cost_of_AB = (cost_of_AB + 1.0) * 10.0


    elif mode_output_relation == 2:
        cost_of_AB = 1.0

        for index_i0 in range(i0_all.shape[0]):
            i0 = i0_all[index_i0]
            comb_i0 = comb(i0, degree_of_input_relation)
            try:
                i = generate_i(func_index, i0, comb_i0, A, mode_input_relation)
                o = get_o(program, func_index, i)
                o_flatten = np.ravel(o)
                comb_o = comb(o_flatten, degree_of_output_relation)

                distance = np.dot(B, comb_o)
                if not np.isnan(distance):
                    if distance > 0:
                        cost_of_AB -= 1.0 / i0_all.shape[0]
            except:
                pass

    elif mode_output_relation == 3:
        cost_of_AB = 1.0

        for index_i0 in range(i0_all.shape[0]):
            i0 = i0_all[index_i0]
            comb_i0 = comb(i0, degree_of_input_relation)
            try:
                i = generate_i(func_index ,i0, comb_i0, A, mode_input_relation)
                o = get_o(program, func_index, i)
                o_flatten = np.ravel(o)
                comb_o = comb(o_flatten, degree_of_output_relation)

                distance = np.dot(B, comb_o)
                if not np.isnan(distance):
                    if distance < 0:
                        cost_of_AB -= 1.0 / i0_all.shape[0]
            except:
                pass

    return cost_of_AB


# get the cost for all the particles
def get_cost_of_AB_all(program, func_index, A_all, B_all, i0_all, mode_input_relation, mode_output_relation,
                       degree_of_input_relation, degree_of_output_relation, no_of_elements_output):
    cost_of_AB_all = np.empty(A_all.shape[0])

    for index_particle in range(A_all.shape[0]):
        cost_of_AB_all[index_particle] = get_cost_of_AB(program, func_index, A_all[index_particle],
                                                        B_all[index_particle], i0_all,
                                                        mode_input_relation, mode_output_relation,
                                                        degree_of_input_relation, degree_of_output_relation,
                                                        no_of_elements_output)
    return cost_of_AB_all


# check if all the elements of a matrix is less than the thre. If yes, change them to thre or -thre accordingly
def anti_degrade(a, thre):
    compared_matrix_a_thre = np.less(np.absolute(a), thre)
    if np.prod(compared_matrix_a_thre) == 1:
        compared_matrix_a_zero = np.less_equal(a, 0)
        a_anti_degrade = np.zeros(a.shape) + compared_matrix_a_zero * (-thre) + (1 - compared_matrix_a_zero) * thre
        return a_anti_degrade
    else:
        return a


class PSO:
    def __init__(self, program, func_index, no_of_inputs, mode_input_relation, mode_output_relation,
                 degree_input_relation, degree_output_relation, no_of_elements_input, no_of_elements_output,
                 no_of_particles, no_of_inputcases, inputcases_range, const_range, coeff_range):
        self.program = program
        self.func_index = func_index
        self.no_of_inputs = no_of_inputs
        self.mode_input_relation = mode_input_relation
        self.mode_output_relation = mode_output_relation
        self.degree_input_relation = degree_input_relation
        self.degree_output_relation = degree_output_relation
        self.no_of_elements_input = no_of_elements_input
        self.no_of_elements_output = no_of_elements_output
        self.no_of_particles = no_of_particles
        self.no_of_inputcases = no_of_inputcases
        self.inputcases_range = inputcases_range
        self.const_range = const_range
        self.coeff_range = coeff_range

        self.size_of_comb_i0 = get_size_of_comb_with_highest_degree(self.no_of_elements_input,
                                                                    self.degree_input_relation)
        self.shape_of_A_all = (
            self.no_of_particles, (self.no_of_inputs - 1), self.no_of_elements_input, self.size_of_comb_i0)
        self.size_of_A_all = np.prod(self.shape_of_A_all)
        self.shape_of_B_all = (self.no_of_particles,
                               get_size_of_comb_with_highest_degree((self.no_of_inputs * self.no_of_elements_output),
                                                                    self.degree_output_relation))
        self.size_of_B_all = np.prod(self.shape_of_B_all)
        self.indices_A_highest_degree = get_size_of_comb(self.no_of_elements_input, self.degree_input_relation)
        self.indices_B_highest_degree = get_size_of_comb((self.no_of_elements_output * self.no_of_inputs),
                                                         self.degree_output_relation)

        # i = [i0, i1, i2 ...], i1 = np.dot(A[0], i0), i2 = np.dot(A[1], i0)
        self.Ashape0 = self.no_of_inputs - 1

    def generate_initial_A_all(self):
        A_all_cons = np.random.uniform(low=self.const_range[0], high=self.const_range[1],
                                       size=(self.no_of_particles, self.Ashape0, self.no_of_elements_input, 1))
        if settings.const_type is int:
            A_all_cons = np.round(A_all_cons)
        A_all_coeff = np.random.uniform(low=self.coeff_range[0], high=self.coeff_range[1], size=(
            self.no_of_particles, self.Ashape0, self.no_of_elements_input, (self.size_of_comb_i0 - 1)))

        # prevent A from degradation
        for index_particle in range(self.no_of_particles):
            for index_input in range(self.no_of_inputs - 1):
                A_all_coeff[index_particle:index_particle + 1, index_input:index_input + 1, :, -self.indices_A_highest_degree:] = anti_degrade(A_all_coeff[index_particle:index_particle + 1, index_input:index_input + 1, :, -self.indices_A_highest_degree:], 1)
        if settings.coeff_type is int:
            A_all_coeff = np.round(A_all_coeff)

        A_all = np.concatenate((A_all_cons, A_all_coeff), axis=3)
        # print(A_all)
        return A_all

    def generate_initial_B_all(self):
        B_all_cons = np.random.uniform(low=self.const_range[0], high=self.const_range[1],
                                       size=(self.shape_of_B_all[0], 1))
        if settings.const_type is int:
            B_all_cons = np.round(B_all_cons)

        B_all_coeff = np.random.uniform(low=self.coeff_range[0], high=self.coeff_range[1],
                                        size=(self.shape_of_B_all[0], (self.shape_of_B_all[1] - 1)))
        # keep B from degradation
        for index_particle in range(self.no_of_particles):
            B_all_coeff[index_particle:index_particle + 1, -self.indices_B_highest_degree:] = anti_degrade(
                B_all_coeff[index_particle:index_particle + 1, -self.indices_B_highest_degree:], 1)
        if settings.coeff_type is int:
            B_all_coeff = np.round(B_all_coeff)

        B_all = np.concatenate((B_all_cons, B_all_coeff), axis=1)
        return B_all

    # def trans_AB_all_to_p_all(self, A_all, B_all):
    #     A_all_flatten = A_all.flatten()
    #     B_all_flatten = B_all.flatten()
    #     p_all = np.concatenate((A_all_flatten, B_all_flatten))

    # def trans_p_all_to_AB_all(self, p_all):
    #     A_all = p_all[0:self.size_of_A_all,].reshape(self.shape_of_A_all)
    #     B_all = p_all[self.size_of_A_all:,].reshape(self.shape_of_B_all)

    def update_AB_all(self, i0_all, A_all, A_v_all, B_all, B_v_all, A_all_p_best, B_all_p_best, index_g_best,
                      omega, cost_of_AB_all_p_best):
        epsilon1 = 1.49445
        epsilon2 = 1.49445
        r1_A_all = np.random.uniform(low=0.0, high=1.0, size=A_all.shape)
        r2_A_all = np.random.uniform(low=0.0, high=1.0, size=A_all.shape)
        r1_B_all = np.random.uniform(low=0.0, high=1.0, size=B_all.shape)
        r2_B_all = np.random.uniform(low=0.0, high=1.0, size=B_all.shape)

        A_all_g_best = A_all_p_best[index_g_best]
        B_all_g_best = B_all_p_best[index_g_best]
        # tile_A_all_g_best = np.tile(A_all_g_best, (A_all.shape[0], 1, 1, 1))
        # tile_B_all_g_best = np.tile(B_all_g_best, (B_all.shape[0], 1))

        A_v_all_next = omega * A_v_all + epsilon1 * r1_A_all * (A_all_p_best - A_all) + epsilon2 * r2_A_all * (
                A_all_g_best - A_all)
        B_v_all_next = omega * B_v_all + epsilon1 * r1_B_all * (B_all_p_best - B_all) + epsilon2 * r2_B_all * (
                B_all_g_best - B_all)

        A_all_next = A_all + A_v_all_next
        B_all_next = B_all + B_v_all_next

        A_all_next_cons = A_all_next[:, :, :, 0:1]
        A_all_next_coeff = A_all_next[:, :, :, 1:]
        B_all_next_cons = B_all_next[:, 0:1]
        B_all_next_coeff = B_all_next[:, 1:]

        # keep A_all_cons in boundary
        compared_matrix_A_all_next_cons_low = np.greater_equal(A_all_next_cons, self.const_range[0])
        if np.prod(compared_matrix_A_all_next_cons_low) != 1:
            A_all_next_cons = A_all_next_cons * compared_matrix_A_all_next_cons_low + (
                    1 - compared_matrix_A_all_next_cons_low) * self.const_range[0]
        compared_matrix_A_all_next_cons_high = np.less_equal(A_all_next_cons, self.const_range[1])
        if np.prod(compared_matrix_A_all_next_cons_high) != 1:
            A_all_next_cons = A_all_next_cons * compared_matrix_A_all_next_cons_high + (
                    1 - compared_matrix_A_all_next_cons_high) * self.const_range[1]

        # keep A_all_coeff in boundary
        compared_matrix_A_all_next_coeff_low = np.greater_equal(A_all_next_coeff, self.coeff_range[0])
        if np.prod(compared_matrix_A_all_next_coeff_low) != 1:
            A_all_next_coeff = A_all_next_coeff * compared_matrix_A_all_next_coeff_low + (
                    1 - compared_matrix_A_all_next_coeff_low) * self.coeff_range[0]
        compared_matrix_A_all_next_coeff_high = np.less_equal(A_all_next_coeff, self.coeff_range[1])
        if np.prod(compared_matrix_A_all_next_coeff_high) != 1:
            A_all_next_coeff = A_all_next_coeff * compared_matrix_A_all_next_coeff_high + (
                    1 - compared_matrix_A_all_next_coeff_high) * self.coeff_range[1]

        # prevent A from degradation
        for index_particle in range(self.no_of_particles):
            for index_input in range(self.no_of_inputs - 1):
                A_all_next_coeff[index_particle:index_particle + 1, index_input:index_input + 1, :, -self.indices_A_highest_degree:] = anti_degrade(A_all_next_coeff[index_particle:index_particle + 1, index_input:index_input + 1, :, -self.indices_A_highest_degree:], 1)

        if settings.const_type is int:
            A_all_next_cons = np.round(A_all_next_cons)
        if settings.coeff_type is int:
            A_all_next_coeff = np.round(A_all_next_coeff)

        A_all_next = np.concatenate((A_all_next_cons, A_all_next_coeff), axis=3)

        # keep B_all_cons in boundary
        compared_matrix_B_all_next_cons_low = np.greater_equal(B_all_next_cons, self.const_range[0])
        if np.prod(compared_matrix_B_all_next_cons_low) != 1:
            B_all_next_cons = B_all_next_cons * compared_matrix_B_all_next_cons_low + (
                    1 - compared_matrix_B_all_next_cons_low) * self.const_range[0]
        compared_matrix_B_all_next_cons_high = np.less_equal(B_all_next_cons, self.const_range[1])
        if np.prod(compared_matrix_B_all_next_cons_high) != 1:
            B_all_next_cons = B_all_next_cons * compared_matrix_B_all_next_cons_high + (
                    1 - compared_matrix_B_all_next_cons_high) * self.const_range[1]

        # keep B_all_coeff in boundary
        compared_matrix_B_all_next_coeff_low = np.greater_equal(B_all_next_coeff, self.coeff_range[0])
        if np.prod(compared_matrix_B_all_next_coeff_low) != 1:
            B_all_next_coeff = B_all_next_coeff * compared_matrix_B_all_next_coeff_low + (
                    1 - compared_matrix_B_all_next_coeff_low) * self.coeff_range[0]
        compared_matrix_B_all_next_coeff_high = np.less_equal(B_all_next_coeff, self.coeff_range[1])
        if np.prod(compared_matrix_B_all_next_coeff_high) != 1:
            B_all_next_coeff = B_all_next_coeff * compared_matrix_B_all_next_coeff_high + (
                    1 - compared_matrix_B_all_next_coeff_high) * self.coeff_range[1]

        # keep B from degradation
        for index_particle in range(self.no_of_particles):
            B_all_next_coeff[index_particle: index_particle + 1, -self.indices_B_highest_degree:] = anti_degrade(
                B_all_next_coeff[index_particle: index_particle + 1, -self.indices_B_highest_degree:], 1)

        if settings.const_type is int:
            B_all_next_cons = np.round(B_all_next_cons)
        if settings.coeff_type is int:
            B_all_next_coeff = np.round(B_all_next_coeff)

        B_all_next = np.concatenate((B_all_next_cons, B_all_next_coeff), axis=1)

        A_v_all_next = A_all_next - A_all
        B_v_all_next = B_all_next - B_all

        # update p_best
        cost_of_AB_all_next = get_cost_of_AB_all(self.program, self.func_index, A_all_next, B_all_next, i0_all,
                                                 self.mode_input_relation,
                                                 self.mode_output_relation, self.degree_input_relation,
                                                 self.degree_output_relation, self.no_of_elements_output)
        compared_matrix_cost_p_all = np.less_equal(cost_of_AB_all_p_best, cost_of_AB_all_next)
        A_all_p_best = A_all_p_best * compared_matrix_cost_p_all.reshape(self.no_of_particles, 1, 1, 1) + A_all_next * (
                1 - compared_matrix_cost_p_all.reshape(self.no_of_particles, 1, 1, 1))
        B_all_p_best = B_all_p_best * compared_matrix_cost_p_all.reshape(self.no_of_particles, 1) + B_all_next * (
                1 - compared_matrix_cost_p_all.reshape(self.no_of_particles, 1))
        cost_of_AB_all_p_best = cost_of_AB_all_p_best * compared_matrix_cost_p_all + cost_of_AB_all_next * (
                1 - compared_matrix_cost_p_all)

        # for index_particle in range(NO_OF_PARTICLES):
        #     cost_of_AB_next = get_cost_of_AB(program, A_all_next[index_particle], B_all_next[index_particle], i0_all, MODE_INPUT_RELATION, MODE_OUTPUT_RELATION, DEGREE_OF_INPUT_RELATION, DEGREE_OF_OUTPUT_RELATION, NO_OF_ELEMENTS_OUTPUT)
        #     cost_of_AB_p_best = cost_of_AB_all_p_best[index_particle]

        #     if cost_of_AB_next < cost_of_AB_p_best:
        #         A_all_p_best[index_particle] = A_all_next[index_particle]
        #         B_all_p_best[index_particle] = B_all_next[index_particle]
        #         cost_of_AB_all_p_best[index_particle] = cost_of_AB_next

        index_g_best = np.argmin(cost_of_AB_all_p_best)

        return A_all_next, B_all_next, A_v_all_next, B_v_all_next, A_all_p_best, B_all_p_best, index_g_best, cost_of_AB_all_p_best

    # def update_p_best_g_best(self, program, i0_all, mode_input_relation, mode_output_relation, degree_of_input_relation, degree_of_output_relation, A_all_p_best, B_all_p_best, A_all_next, B_all_next):
    #     cost_of_p_all_p_best = self.get_cost_of_AB_all(program, A_all_p_best, B_all_p_best, i0_all, mode_input_relation, mode_output_relation, degree_of_input_relation, degree_of_output_relation)
    #     cost_of_p_all_next = self.get_cost_of_AB_all(program, A_all_next, B_all_next, i0_all, mode_input_relation, mode_output_relation, degree_of_input_relation, degree_of_output_relation)
    #     print(cost_of_p_all_next.shape)
    #     print(cost_of_p_all_p_best.shape)
    #     compared_cost = cost_of_p_all_p_best < cost_of_p_all_next
    #     A_all_p_best_next = np.zeros(A_all_p_best.shape)
    #     B_all_p_best_next = np.zeros(B_all_p_best.shape)

    #     for index_particle in range(A_all_next.shape[0]):
    #         A_all_p_best_next[index_particle] = A_all_p_best[index_particle] * compared_cost[index_particle] + A_all_next[index_particle] * (1 - compared_cost[index_particle])
    #         B_all_p_best_next[index_particle] = B_all_p_best[index_particle] * compared_cost[index_particle] + B_all_next[index_particle] * (1 - compared_cost[index_particle])
    #     # print(A_all_p_best_next.shape)
    #     # print(B_all_p_best_next.shape)
    #     cost_of_p_all_next = self.get_cost_of_AB_all(program, A_all_p_best_next, B_all_p_best_next, i0_all, mode_input_relation, mode_output_relation, degree_of_input_relation, degree_of_output_relation)
    #     index_g_best = np.argmin(cost_of_p_all_next)

    #     return A_all_p_best_next, B_all_p_best_next, index_g_best

    def run(self):
        i0_all = generate_i0_all(settings.get_input_datatype(self.func_index), settings.get_input_range(self.func_index), self.no_of_inputcases)
        A_all = self.generate_initial_A_all()
        B_all = self.generate_initial_B_all()

        A_v_all = np.zeros(A_all.shape)
        B_v_all = np.zeros(B_all.shape)
        A_all_p_best = np.copy(A_all)
        B_all_p_best = np.copy(B_all)
        cost_of_AB_all_p_best = get_cost_of_AB_all(self.program, self.func_index, A_all, B_all, i0_all,
                                                   self.mode_input_relation,
                                                   self.mode_output_relation, self.degree_input_relation,
                                                   self.degree_output_relation, self.no_of_elements_output)

        index_g_best = np.argmin(cost_of_AB_all_p_best)

        omega_s = 0.9
        omega_e = 0.4

        iterations = settings.pso_iterations
        iteration = 0
        start_time = time()
        while True:
            if settings.pso_run_timebudget is None:
                if iteration >= iterations:
                    break
            elif time() - start_time >= settings.pso_run_timebudget:
                break
            omega = omega_s - (omega_s - omega_e) * (min((iteration / iterations), 1.0) ** 2)
            A_all, B_all, A_v_all, B_v_all, A_all_p_best, B_all_p_best, index_g_best, cost_of_AB_all_p_best = self.update_AB_all(
                i0_all, A_all, A_v_all, B_all, B_v_all, A_all_p_best, B_all_p_best, index_g_best, omega,
                cost_of_AB_all_p_best)
            min_cost = cost_of_AB_all_p_best[index_g_best]

            ## break the iteration in advance if solution is found
            # if self.mode_output_relation == 1:
            #     if min_cost < 1:
            #         break
            # else:
            #     if min_cost < 0.05:
            #         break
            # print(f'iteration is {iteration}, min_cost is {np.round(min_cost, decimals=3)}')
            # print("A:")
            # print(A_all_p_best[index_g_best])
            # print("B:")
            # print(B_all_p_best[index_g_best])
            # print("----------")

            iteration += 1

        A = A_all_p_best[index_g_best]
        B = B_all_p_best[index_g_best]

        return min_cost, A, B


def phase1(pso_runs, output_path, func_index, parameters, inputcases_range, const_range, coeff_range):
    if not os.path.isdir(output_path):
        os.mkdir(output_path)
    if not os.path.isdir(f"{output_path}/phase1"):
        os.mkdir(f"{output_path}/phase1")

    no_of_elements_input = settings.getNEI(func_index)
    no_of_elements_output = settings.getNEO(func_index)


    no_of_particles = 30
    no_of_inputcases = 100

    min_cost_candidates = []
    A_candidates = []
    B_candidates = []

    parameters_int = [int(e) for e in parameters.split("_")]
    no_of_inputs = parameters_int[0]
    mode_input_relation = parameters_int[1]
    mode_output_relation = parameters_int[2]
    degree_of_input_relation = parameters_int[3]
    degree_of_output_relation = parameters_int[4]

    pso_run = 0
    while True:
        print("====================")
        print(f"    searching: func_index is {func_index}, parameters is {parameters}, pso_run is {pso_run+1}.")
        print(f'running pso run {pso_run+1}')
        AutoMR = PSO(settings.program, func_index, no_of_inputs, mode_input_relation, mode_output_relation,
                     degree_of_input_relation, degree_of_output_relation, no_of_elements_input,
                     no_of_elements_output, no_of_particles, no_of_inputcases, inputcases_range,
                     const_range, coeff_range)
        min_cost, A, B = AutoMR.run()

        min_cost_candidates.append(np.round(min_cost, decimals=3))
        A_candidates.append(A)
        B_candidates.append(B)
        np.savez('{}/phase1/{}_{}_{}_{}_{}_{}.npz'.format(output_path, func_index,
                                                          no_of_inputs,
                                                          mode_input_relation,
                                                          mode_output_relation,
                                                          degree_of_input_relation,
                                                          degree_of_output_relation),
                 min_cost_candidates=min_cost_candidates, A_candidates=np.array(A_candidates),
                 B_candidates=np.array(B_candidates))
        # print(f"search results:")
        # print("A:")
        # print(A)
        # print("B:")
        # print(B)
        # print(f"Corresponding cost is {min_cost}")
        # print("----------\n")

        pso_run += 1
        if pso_run >= pso_runs:
            break


def run_phase1():
    # print("start phase1: searching for MRs...")
    func_indices = settings.func_indices
    parameters_collection = settings.parameters_collection

    output_path = settings.output_path
    if not os.path.isdir("{}".format(output_path)):
        os.makedirs(output_path)
    pso_runs = settings.pso_runs
    pso_iterations = settings.pso_iterations

    coeff_range = settings.coeff_range
    const_range = settings.const_range

    if os.path.isfile(f"{output_path}/performance.csv"):
        times = pd.read_csv(f"{output_path}/performance.csv", index_col=0)
    else:
        times = pd.DataFrame()

    for func_index in func_indices:
        inputcases_range = settings.get_input_range(func_index)
        for parameters in parameters_collection:
            t1 = datetime.datetime.now()
            phase1(pso_runs, output_path, func_index, parameters, inputcases_range, const_range, coeff_range)
            t2 = datetime.datetime.now()
            cost_time = np.round((t2-t1).total_seconds(), decimals=3)

            # times.loc[f"{func_index}_{parameters}", "pso_iterations"] = pso_iterations
            times.loc[f"{func_index}_{parameters}", "phase1"] = cost_time

    # times.to_csv(f"{output_path}/performance.csv")

if __name__ == '__main__':
    run_phase1()