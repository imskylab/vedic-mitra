/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.profile

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.datastore.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MagicNumber")
class ProfileViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the stored profile into the form as text`() =
        runTest {
            val stored =
                UserProfile(
                    name = "Leo",
                    dateOfBirth = LocalDate.of(1995, 3, 14),
                    timeOfBirth = LocalTime.of(9, 30),
                    placeOfBirth = "Hyderabad, India",
                )
            val viewModel = ProfileViewModel(FakeProfileRepository(stored))

            val state = viewModel.uiState.value
            assertThat(state.name).isEqualTo("Leo")
            assertThat(state.dateOfBirth).isEqualTo("1995-03-14")
            assertThat(state.timeOfBirth).isEqualTo("09:30")
            assertThat(state.placeOfBirth).isEqualTo("Hyderabad, India")
        }

    @Test
    fun `save persists a valid profile and signals done`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = ProfileViewModel(repository)
            viewModel.onNameChange("Leo")
            viewModel.onDateOfBirthChange("1995-03-14")
            viewModel.onTimeOfBirthChange("09:30")
            viewModel.onPlaceOfBirthChange("Hyderabad, India")

            viewModel.saved.test {
                viewModel.save()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val expected =
                UserProfile(
                    name = "Leo",
                    dateOfBirth = LocalDate.of(1995, 3, 14),
                    timeOfBirth = LocalTime.of(9, 30),
                    placeOfBirth = "Hyderabad, India",
                )
            assertThat(repository.profile.first()).isEqualTo(expected)
        }

    @Test
    fun `save with an unparseable date reports an error and does not persist`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = ProfileViewModel(repository)
            viewModel.onDateOfBirthChange("14/03/1995")

            viewModel.messages.test {
                viewModel.save()
                assertThat(awaitItem()).contains("YYYY-MM-DD")
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(repository.profile.first()).isEqualTo(UserProfile())
        }
}

private class FakeProfileRepository(
    initial: UserProfile = UserProfile(),
) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<UserProfile> = state.asStateFlow()

    override suspend fun setProfile(profile: UserProfile) {
        state.value = profile
    }
}
