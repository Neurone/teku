/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.artemis.state;

import static com.google.common.base.Preconditions.checkArgument;

import tech.pegasys.artemis.datastructures.BeaconChainState.CandidatePoWReceiptRootRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.CrosslinkRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.ForkData;
import tech.pegasys.artemis.datastructures.BeaconChainState.PendingAttestationRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.ShardCommittee;
import tech.pegasys.artemis.datastructures.BeaconChainState.ShardReassignmentRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.ValidatorRecord;
import tech.pegasys.artemis.ethereum.core.Hash;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.annotations.VisibleForTesting;


public class BeaconState {

  // Misc
  private UInt64 slot;
  private UInt64 genesis_time;
  private ForkData fork_data;

  // Validator registry
  public ArrayList<ValidatorRecord> validator_registry;
  private UInt64 validator_registry_latest_change_slot;
  private UInt64 validator_registry_exit_count;
  private Hash validator_registry_delta_chain_tip;

  // Randomness and committees
  private Hash randao_mix;
  private Hash next_seed;
  public ArrayList<ArrayList<ShardCommittee>> shard_committees_at_slots;
  public ArrayList<ArrayList<Integer>> persistent_committees;
  private ArrayList<ShardReassignmentRecord> persistent_committee_reassignments;

  // Finality
  private UInt64 previous_justified_slot;
  private UInt64 justified_slot;
  private UInt64 justification_bitfield;
  private UInt64 finalized_slot;

  // Recent state
  private ArrayList<CrosslinkRecord> latest_crosslinks;
  private ArrayList<Hash> latest_block_roots;
  private ArrayList<UInt64> latest_penalized_exit_balances;
  private ArrayList<PendingAttestationRecord> latest_attestations;
  private ArrayList<Hash> batched_block_roots;

  // PoW receipt root
  private Hash processed_pow_receipt_root;
  private ArrayList<CandidatePoWReceiptRootRecord> candidate_pow_receipt_roots;

  public BeaconState(
      // Misc
      UInt64 slot, UInt64 genesis_time, ForkData fork_data,
      // Validator registry
      ArrayList<ValidatorRecord> validator_registry, UInt64 validator_registry_latest_change_slot,
      UInt64 validator_registry_exit_count, Hash validator_registry_delta_chain_tip,
      // Randomness and committees
      Hash randao_mix, Hash next_seed, ArrayList<ArrayList<ShardCommittee>> shard_committees_at_slots,
      ArrayList<ArrayList<Integer>> persistent_committees,
      ArrayList<ShardReassignmentRecord> persistent_committee_reassignments,
      // Finality
      UInt64 previous_justified_slot, UInt64 justified_slot, UInt64 justification_bitfield,
      UInt64 finalized_slot,
      // Recent state
      ArrayList<CrosslinkRecord> latest_crosslinks, ArrayList<Hash> latest_block_roots,
      ArrayList<UInt64>  latest_penalized_exit_balances, ArrayList<PendingAttestationRecord> latest_attestations,
      ArrayList<Hash> batched_block_roots,
      // PoW receipt root
      Hash processed_pow_receipt_root, ArrayList<CandidatePoWReceiptRootRecord> candidate_pow_receipt_roots) {

    // Misc
    this.slot = slot;
    this. genesis_time = genesis_time;
    this.fork_data = fork_data;

    // Validator registry
    this.validator_registry = validator_registry;
    this.validator_registry_latest_change_slot = validator_registry_latest_change_slot;
    this.validator_registry_exit_count = validator_registry_exit_count;
    this.validator_registry_delta_chain_tip = validator_registry_delta_chain_tip;

    // Randomness and committees
    this.randao_mix = randao_mix;
    this.next_seed = next_seed;
    this.shard_committees_at_slots = shard_committees_at_slots;
    this.persistent_committees = persistent_committees;
    this.persistent_committee_reassignments = persistent_committee_reassignments;

    // Finality
    this.previous_justified_slot = previous_justified_slot;
    this.justified_slot = justified_slot;
    this.justification_bitfield = justification_bitfield;
    this.finalized_slot = finalized_slot;

    // Recent state
    this.latest_crosslinks = latest_crosslinks;
    this.latest_block_roots = latest_block_roots;
    this.latest_penalized_exit_balances = latest_penalized_exit_balances;
    this.latest_attestations = latest_attestations;
    this.batched_block_roots = batched_block_roots;

    // PoW receipt root
    this.processed_pow_receipt_root = processed_pow_receipt_root;
    this.candidate_pow_receipt_roots = candidate_pow_receipt_roots;

  }


  static class BeaconStateHelperFunctions {



    /**
     * Converts byte[] to int.
     *
     * @param src   byte[]
     * @param pos   Index in Byte[] array
     * @return      converted int
     * @throws IllegalArgumentException if pos is a negative value.
     */
    @VisibleForTesting
    static int bytes3ToInt(Hash src, int pos) {
      checkArgument(pos >= 0, "Expected positive pos but got %s", pos);
      return ((src.extractArray()[pos] & 0xF) << 16) |
          ((src.extractArray()[pos + 1] & 0xFF) << 8) |
          (src.extractArray()[pos + 2] & 0xFF);
    }


    /**
     * Returns the shuffled ``values`` with seed as entropy.
     *
     * @param values    The array.
     * @param seed      Initial seed value used for randomization.
     * @return          The shuffled array.
     */
    @VisibleForTesting
    static Object[] shuffle(Object[] values, Hash seed) {

      int values_count = values.length;

      // Entropy is consumed from the seed in 3-byte (24 bit) chunks.
      int rand_bytes = 3;
      // The highest possible result of the RNG.
      int rand_max = (int) Math.pow(2, (rand_bytes * 8) - 1);

      // The range of the RNG places an upper-bound on the size of the list that
      // may be shuffled. It is a logic error to supply an oversized list.
      assert values_count < rand_max;

      Object[] output = values.clone();
      Hash source = seed;
      int index = 0;

      while (index < values_count - 1) {
        // Re-hash the `source` to obtain a new pattern of bytes.
        source = Hash.hash(source);

        // List to hold values for swap below.
        Object tmp;

        // Iterate through the `source` bytes in 3-byte chunks
        for (int position = 0; position < (32 - (32 % rand_bytes)); position += rand_bytes) {
          // Determine the number of indices remaining in `values` and exit
          // once the last index is reached.
          int remaining = values_count - index;
          if (remaining == 1) break;

          // Read 3-bytes of `source` as a 24-bit big-endian integer.
          int sample_from_source = bytes3ToInt(source, position);


          // Sample values greater than or equal to `sample_max` will cause
          // modulo bias when mapped into the `remaining` range.
          int sample_max = rand_max - rand_max % remaining;

          // Perform a swap if the consumed entropy will not cause modulo bias.
          if (sample_from_source < sample_max) {
            // Select a replacement index for the current index
            int replacement_position = (sample_from_source % remaining) + index;
            // Swap the current index with the replacement index.
            tmp = output[index];
            output[index] = output[replacement_position];
            output[replacement_position] = tmp;
            index += 1;
          }

        }

      }

      return output;
    }


    /**
     * Splits ``values`` into ``split_count`` pieces.
     *
     * @param values          The original list of validators.
     * @param split_count     The number of pieces to split the array into.
     * @return                The list of validators split into N pieces.
     */
    static Object[] split(Object[] values, int split_count) {
      checkArgument(split_count > 0, "Expected positive split_count but got %s", split_count);

      int list_length = values.length;

      Object[] split_arr = new Object[split_count];

      for (int i = 0; i < split_count; i++) {
        int startIndex = list_length * i / split_count;
        int endIndex = list_length * (i + 1) / split_count;
        Object[] new_split = Arrays.copyOfRange(values, startIndex, endIndex);
        split_arr[i] = new_split;
      }

      return split_arr;

    }

    /**
     * A helper method for readability.
     */
    static int clamp(int minval, int maxval, int x) {
      if (x <= minval) return minval;
      if (x >= maxval) return maxval;
      return x;
    }



  }

}
