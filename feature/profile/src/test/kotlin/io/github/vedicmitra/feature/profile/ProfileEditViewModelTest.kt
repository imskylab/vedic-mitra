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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.Gender
import io.github.vedicmitra.core.datastore.ProfileRelation
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.location.GeocodeResult
import io.github.vedicmitra.core.location.GeocodingClient
import io.github.vedicmitra.core.location.TimeZoneResolver
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
class ProfileEditViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `adds a new profile and signals done`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository)
            viewModel.onNameChange("Leo")
            viewModel.onRelationChange(ProfileRelation.SELF)
            viewModel.onDateOfBirthChange("1995-03-14")
            viewModel.onTimeOfBirthChange("09:30")

            viewModel.saved.test {
                viewModel.save()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val saved = repository.profiles.first().single()
            assertThat(saved.name).isEqualTo("Leo")
            assertThat(saved.relation).isEqualTo(ProfileRelation.SELF)
            assertThat(saved.dateOfBirth).isEqualTo(LocalDate.of(1995, 3, 14))
            assertThat(saved.timeOfBirth).isEqualTo(LocalTime.of(9, 30))
        }

    @Test
    fun `saves the chosen gender and clears it when set to null`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository)
            viewModel.onNameChange("Mia")

            viewModel.onGenderChange(Gender.FEMALE)
            assertThat(viewModel.uiState.value.gender).isEqualTo(Gender.FEMALE)
            viewModel.onGenderChange(null)
            assertThat(viewModel.uiState.value.gender).isNull()

            viewModel.onGenderChange(Gender.MALE)
            viewModel.saved.test {
                viewModel.save()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(
                repository.profiles
                    .first()
                    .single()
                    .gender,
            ).isEqualTo(Gender.MALE)
        }

    @Test
    fun `a blank name reports an error and does not save`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository)

            viewModel.messages.test {
                viewModel.save()
                assertThat(awaitItem()).contains("name")
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(repository.profiles.first()).isEmpty()
        }

    @Test
    fun `edits an existing profile in place`() =
        runTest {
            val existing = BirthProfile(id = "a", name = "Leo", relation = ProfileRelation.SELF)
            val repository = FakeProfileRepository(listOf(existing), primary = "a")
            val viewModel = viewModel(repository, SavedStateHandle(mapOf(PROFILE_ID_ARG to "a")))

            assertThat(viewModel.uiState.value.name).isEqualTo("Leo")
            assertThat(viewModel.uiState.value.isEditing).isTrue()

            viewModel.onNameChange("Leo Prime")
            viewModel.saved.test {
                viewModel.save()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val profiles = repository.profiles.first()
            assertThat(profiles).hasSize(1)
            assertThat(profiles.single().id).isEqualTo("a")
            assertThat(profiles.single().name).isEqualTo("Leo Prime")
        }

    @Test
    fun `selecting a searched place resolves coordinates and zone and saves them`() =
        runTest {
            val result = GeocodeResult("Hyderabad, India", GeoCoordinates(17.385, 78.4867))
            val repository = FakeProfileRepository()
            val viewModel =
                viewModel(
                    repository = repository,
                    geocoding = FakeGeocodingClient(listOf(result)),
                    timeZone = FakeTimeZoneResolver("Asia/Kolkata"),
                )

            viewModel.onPlaceOfBirthChange("Hyderabad")
            viewModel.searchPlace()
            assertThat(viewModel.uiState.value.placeResults).containsExactly(result)

            viewModel.selectPlace(result)
            assertThat(viewModel.uiState.value.placeOfBirth).isEqualTo("Hyderabad, India")
            assertThat(viewModel.uiState.value.birthCoordinates).isEqualTo(GeoCoordinates(17.385, 78.4867))
            assertThat(viewModel.uiState.value.birthZoneId).isEqualTo("Asia/Kolkata")

            viewModel.onNameChange("Leo")
            viewModel.saved.test {
                viewModel.save()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            val saved = repository.profiles.first().single()
            assertThat(saved.birthCoordinates).isEqualTo(GeoCoordinates(17.385, 78.4867))
            assertThat(saved.birthZoneId).isEqualTo("Asia/Kolkata")
        }

    private fun viewModel(
        repository: ProfileRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        geocoding: GeocodingClient = FakeGeocodingClient(),
        timeZone: TimeZoneResolver = FakeTimeZoneResolver(),
    ) = ProfileEditViewModel(repository, geocoding, timeZone, savedStateHandle)
}

private class FakeProfileRepository(
    initial: List<BirthProfile> = emptyList(),
    primary: String? = null,
) : ProfileRepository {
    private val profileState = MutableStateFlow(initial)
    private val primaryState = MutableStateFlow(primary)
    override val profiles: Flow<List<BirthProfile>> = profileState.asStateFlow()
    override val primaryProfileId: Flow<String?> = primaryState.asStateFlow()

    override suspend fun upsert(profile: BirthProfile) {
        profileState.value = profileState.value.filterNot { it.id == profile.id } + profile
        if (primaryState.value == null) primaryState.value = profile.id
    }

    override suspend fun remove(id: String) {
        profileState.value = profileState.value.filterNot { it.id == id }
        if (primaryState.value == id) primaryState.value = profileState.value.firstOrNull()?.id
    }

    override suspend fun setPrimary(id: String) {
        primaryState.value = id
    }
}

private class FakeGeocodingClient(
    private val results: List<GeocodeResult> = emptyList(),
) : GeocodingClient {
    override suspend fun search(
        query: String,
        maxResults: Int,
    ): AppResult<List<GeocodeResult>> = AppResult.Success(results)
}

private class FakeTimeZoneResolver(
    private val zone: String = "Asia/Kolkata",
) : TimeZoneResolver {
    override suspend fun resolve(coordinates: GeoCoordinates): String = zone
}
