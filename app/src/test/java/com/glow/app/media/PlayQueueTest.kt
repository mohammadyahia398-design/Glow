package com.glow.app.media

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class PlayQueueTest {

    private fun queue() = PlayQueue(Random(42))

    @Test
    fun empty_queue_returns_null_everywhere() {
        val q = queue()
        assertThat(q.currentIndex).isNull()
        assertThat(q.next(auto = false)).isNull()
        assertThat(q.previous()).isNull()
    }

    @Test
    fun load_starts_at_requested_index() {
        val q = queue()
        q.load(size = 5, startIndex = 3)
        assertThat(q.currentIndex).isEqualTo(3)
    }

    @Test
    fun load_coerces_out_of_range_start_index() {
        val q = queue()
        q.load(size = 3, startIndex = 10)
        assertThat(q.currentIndex).isEqualTo(2)
    }

    @Test
    fun next_moves_forward_in_order() {
        val q = queue()
        q.load(size = 3)
        assertThat(q.next(auto = false)).isEqualTo(1)
        assertThat(q.next(auto = false)).isEqualTo(2)
    }

    @Test
    fun next_at_end_with_repeat_off_returns_null_and_keeps_current() {
        val q = queue()
        q.load(size = 2)
        q.next(auto = false)
        assertThat(q.next(auto = false)).isNull()
        assertThat(q.currentIndex).isEqualTo(1)
    }

    @Test
    fun next_at_end_with_repeat_all_wraps_to_start() {
        val q = queue()
        q.repeat = RepeatMode.All
        q.load(size = 2, startIndex = 1)
        assertThat(q.next(auto = false)).isEqualTo(0)
    }

    @Test
    fun repeat_one_replays_same_track_when_auto() {
        val q = queue()
        q.repeat = RepeatMode.One
        q.load(size = 3, startIndex = 1)
        assertThat(q.next(auto = true)).isEqualTo(1)
    }

    @Test
    fun repeat_one_still_advances_on_manual_next() {
        val q = queue()
        q.repeat = RepeatMode.One
        q.load(size = 3, startIndex = 1)
        assertThat(q.next(auto = false)).isEqualTo(2)
    }

    @Test
    fun previous_goes_back_one_track() {
        val q = queue()
        q.load(size = 3, startIndex = 2)
        assertThat(q.previous()).isEqualTo(1)
    }

    @Test
    fun previous_at_start_with_repeat_off_stays_on_first() {
        val q = queue()
        q.load(size = 3)
        assertThat(q.previous()).isEqualTo(0)
    }

    @Test
    fun previous_at_start_with_repeat_all_wraps_to_last() {
        val q = queue()
        q.repeat = RepeatMode.All
        q.load(size = 3)
        assertThat(q.previous()).isEqualTo(2)
    }

    @Test
    fun shuffle_starts_with_chosen_track_and_visits_each_once() {
        val q = queue()
        q.load(size = 5, startIndex = 2, shuffle = true)
        val visited = mutableListOf(q.currentIndex!!)
        repeat(4) { visited += q.next(auto = false)!! }

        assertThat(visited.first()).isEqualTo(2)
        assertThat(visited.toSet()).containsExactly(0, 1, 2, 3, 4)
        assertThat(q.next(auto = false)).isNull()
    }

    @Test
    fun toggling_shuffle_keeps_current_track() {
        val q = queue()
        q.load(size = 5, startIndex = 3)

        q.setShuffle(true)
        assertThat(q.currentIndex).isEqualTo(3)

        q.setShuffle(false)
        assertThat(q.currentIndex).isEqualTo(3)
        assertThat(q.next(auto = false)).isEqualTo(4)
    }

    @Test
    fun move_reorders_items_without_changing_current_track() {
        val q = queue()
        q.load(size = 5, startIndex = 2)

        assertThat(q.move(fromPosition = 4, toPosition = 1)).isTrue()
        assertThat(q.currentIndex).isEqualTo(2)
        assertThat(q.currentPosition).isEqualTo(3)
        assertThat(q.orderedIndices()).containsExactly(0, 4, 1, 2, 3).inOrder()
    }

    @Test
    fun move_rejects_positions_outside_queue() {
        val q = queue()
        q.load(size = 3)

        assertThat(q.move(-1, 1)).isFalse()
        assertThat(q.move(0, 3)).isFalse()
        assertThat(q.orderedIndices()).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun remove_current_selects_the_next_available_item() {
        val q = queue()
        q.load(size = 4, startIndex = 1)

        assertThat(q.removeAt(1)).isEqualTo(1)
        assertThat(q.currentPosition).isEqualTo(1)
        assertThat(q.orderedIndices()).containsExactly(0, 1, 2).inOrder()
        assertThat(q.currentIndex).isEqualTo(1)
    }

    @Test
    fun remove_item_before_current_keeps_current_original_item() {
        val q = queue()
        q.load(size = 5, startIndex = 3)

        assertThat(q.removeAt(1)).isEqualTo(1)
        assertThat(q.currentIndex).isEqualTo(2)
        assertThat(q.currentPosition).isEqualTo(2)
        assertThat(q.orderedIndices()).containsExactly(0, 1, 2, 3).inOrder()
    }

    @Test
    fun remove_last_current_moves_to_previous_item() {
        val q = queue()
        q.load(size = 3, startIndex = 2)

        assertThat(q.removeAt(2)).isEqualTo(2)
        assertThat(q.currentIndex).isEqualTo(1)
        assertThat(q.currentPosition).isEqualTo(1)
    }

    @Test
    fun remove_only_item_leaves_queue_empty() {
        val q = queue()
        q.load(size = 1)

        assertThat(q.removeAt(0)).isEqualTo(0)
        assertThat(q.size).isEqualTo(0)
        assertThat(q.currentIndex).isNull()
        assertThat(q.currentPosition).isEqualTo(-1)
    }

    @Test
    fun append_adds_items_to_the_end_without_duplicates() {
        val q = queue()
        q.load(size = 3, startIndex = 1)

        q.append(listOf(3, 4, 3, -1))

        assertThat(q.orderedIndices()).containsExactly(0, 1, 2, 3, 4).inOrder()
        assertThat(q.currentIndex).isEqualTo(1)
    }

    @Test
    fun append_play_next_places_items_after_current_in_given_order() {
        val q = queue()
        q.load(size = 4, startIndex = 1)

        q.append(listOf(4, 5), playNext = true)

        assertThat(q.orderedIndices()).containsExactly(0, 1, 4, 5, 2, 3).inOrder()
        assertThat(q.currentIndex).isEqualTo(1)
    }

    @Test
    fun select_changes_current_position_and_rejects_invalid_position() {
        val q = queue()
        q.load(size = 4)

        assertThat(q.select(2)).isEqualTo(2)
        assertThat(q.currentPosition).isEqualTo(2)
        assertThat(q.select(9)).isNull()
        assertThat(q.currentIndex).isEqualTo(2)
    }

    @Test
    fun append_to_empty_queue_selects_first_added_item() {
        val q = queue()
        q.append(listOf(8, 9))

        assertThat(q.currentIndex).isEqualTo(8)
        assertThat(q.currentPosition).isEqualTo(0)
        assertThat(q.next(auto = false)).isEqualTo(9)
    }

    @Test
    fun shuffle_can_be_enabled_after_appending_non_contiguous_indices() {
        val q = queue()
        q.append(listOf(8, 12, 20))
        q.setShuffle(true)

        assertThat(q.currentIndex).isEqualTo(8)
        assertThat(q.orderedIndices()).containsExactly(8, 12, 20)
        assertThat(q.currentPosition).isEqualTo(0)
    }

    @Test
    fun disabling_shuffle_restores_original_index_order_after_append() {
        val q = queue()
        q.append(listOf(8, 12, 20))
        q.select(1)
        q.setShuffle(true)
        val current = q.currentIndex

        q.setShuffle(false)

        assertThat(q.currentIndex).isEqualTo(current)
        assertThat(q.orderedIndices()).containsExactly(8, 12, 20).inOrder()
    }

    @Test
    fun repeat_all_after_append_restarts_existing_manual_order() {
        val q = queue()
        q.append(listOf(8, 12, 20))
        q.move(2, 1)
        q.repeat = RepeatMode.All
        q.select(2)

        assertThat(q.next(auto = false)).isEqualTo(8)
        assertThat(q.next(auto = false)).isEqualTo(20)
        assertThat(q.next(auto = false)).isEqualTo(12)
        assertThat(q.next(auto = false)).isEqualTo(8)
    }

    @Test
    fun repeat_mode_cycles_off_all_one_off() {
        assertThat(RepeatMode.Off.cycle()).isEqualTo(RepeatMode.All)
        assertThat(RepeatMode.All.cycle()).isEqualTo(RepeatMode.One)
        assertThat(RepeatMode.One.cycle()).isEqualTo(RepeatMode.Off)
    }
}
