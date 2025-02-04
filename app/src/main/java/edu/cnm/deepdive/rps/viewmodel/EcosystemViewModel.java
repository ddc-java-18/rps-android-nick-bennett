/*
 *  Copyright 2024 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.rps.viewmodel;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import edu.cnm.deepdive.rps.R;
import edu.cnm.deepdive.rps.model.domain.Ecosystem;
import edu.cnm.deepdive.rps.service.EcosystemRepository;
import edu.cnm.deepdive.rps.service.PreferencesRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import javax.inject.Inject;

/**
 * Provides UI controller access to {@link LiveData} containers for ecosystem components. Some of
 * the available components may not be required for the required functionality or user experience,
 * and almost all can be obtained by observing the return value of {@link #getEcosystem()}; however,
 * as a convenience, properties of that ecosystem can be observed directly, using other methods of
 * this class,
 */
@HiltViewModel
public class EcosystemViewModel extends ViewModel implements DefaultLifecycleObserver {

  private static final int TICK_MILLISECONDS = 10;

  private final EcosystemRepository ecosystemRepository;
  private final PreferencesRepository preferencesRepository;
  private final LiveData<int[][]> terrain;
  private final LiveData<Integer> size;
  private final LiveData<Integer> initialBreedCount;
  private final LiveData<Integer> currentBreedCount;
  private final LiveData<int[]> populations;
  private final LiveData<Long> iterationCount;
  private final LiveData<Boolean> absorbed;
  private final MutableLiveData<Throwable> throwable;
  private final CompositeDisposable pending;
  private final String numBreedsKey;
  private final int numBreedsDefault;
  private final String terrainSizeKey;
  private final int terrainSizeDefault;
  private final String toroidalTopologyKey;
  private final boolean toroidalTopologyDefault;

  private final String runSpeedKey;
  private final int runSpeedDefault;

  private final String swapLikelihoodKey;
  private final int swapLikelihoodDefault;

  private int currentTerrainSize;

  @Inject
  EcosystemViewModel(@ApplicationContext Context context,
      EcosystemRepository ecosystemRepository, PreferencesRepository preferencesRepository) {
    this.ecosystemRepository = ecosystemRepository;
    this.preferencesRepository = preferencesRepository;

    LiveData<Ecosystem> source = ecosystemRepository.getEcosystem();
    terrain = Transformations.map(source, Ecosystem::getTerrain);
    size = Transformations.map(source, Ecosystem::getSize);
    initialBreedCount = Transformations.map(source, Ecosystem::getInitialBreedCount);
    currentBreedCount = Transformations.map(source, Ecosystem::getCurrentBreedCount);
    populations = Transformations.map(source, Ecosystem::getPopulations);
    iterationCount = Transformations.map(source, Ecosystem::getIterationCount);
    absorbed = Transformations.map(source, Ecosystem::isAbsorbed);
    throwable = new MutableLiveData<>();
    pending = new CompositeDisposable();

    Resources res = context.getResources();
    numBreedsKey = res.getString(R.string.num_breeds_key);
    numBreedsDefault = res.getInteger(R.integer.num_breeds_default);
    terrainSizeKey = res.getString(R.string.terrain_size_key);
    terrainSizeDefault = res.getInteger(R.integer.terrain_size_default);
    toroidalTopologyKey = res.getString(R.string.toroidal_topology_key);
    toroidalTopologyDefault = res.getBoolean(R.bool.toroidal_topology_default);

    runSpeedKey = res.getString(R.string.run_speed_key);
    runSpeedDefault = res.getInteger(R.integer.run_speed_default);

    swapLikelihoodKey = res.getString(R.string.swap_likelihood_key);
    swapLikelihoodDefault = res.getInteger(R.integer.run_speed_default);

    create();
  }

  /**
   * Creates a new instance of the intransitive ecosystem simulation, using the number of breeds,
   * terrain size, and topology specified in the corresponding property values stored in
   * {@link android.content.SharedPreferences}.
   */
  public void create() {
    int numBreeds = preferencesRepository.get(numBreedsKey, numBreedsDefault);
    int terrainSize = preferencesRepository.get(terrainSizeKey, terrainSizeDefault);
    boolean toroidal = preferencesRepository.get(toroidalTopologyKey, toroidalTopologyDefault);
    execute(ecosystemRepository.create(numBreeds, terrainSize, toroidal),
        (ecosystem) -> currentTerrainSize = ecosystem.getSize());
  }

  /**
   * Starts (or resumes) execution of the simulation. The speed of execution and the probability
   * of random element swaps in each iteration are derived from property values stored in
   * {@link android.content.SharedPreferences}.
   */
  public void run() {
    int runSpeed = preferencesRepository.get(runSpeedKey, runSpeedDefault);
    int iterationsPerTick = runSpeed * currentTerrainSize * currentTerrainSize / 25;
    int swapLikelihood = preferencesRepository.get(swapLikelihoodKey, swapLikelihoodDefault);
    execute(ecosystemRepository.run(
        iterationsPerTick, swapLikelihood / 100f, TICK_MILLISECONDS), (ignored) -> {}, () -> {});
  }

  /**
   * Suspends execution of the simulation.
   */
  public void pause() {
    execute(ecosystemRepository.pause(), () -> {});
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Ecosystem&gt;} containing the current
   * ecosystem.
   */
  public LiveData<Ecosystem> getEcosystem() {
    return ecosystemRepository.getEcosystem();
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;int[][]&gt;} containing the ecosystem's
   * terrain contents.
   */
  public LiveData<int[][]> getTerrain() {
    return terrain;
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Integer&gt;} containing the ecosystem's
   * size. (Note that in this implementation, the ecosystem's height and width are equal; the size
   * is thus equal to both the height and width.)
   */
  public LiveData<Integer> getSize() {
    return Transformations.distinctUntilChanged(size);
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Integer&gt;} containing the ecosystem's
   * initial number of breeds (it does not reflect any breed extinctions that may have taken place
   * in the simulation).
   */
  public LiveData<Integer> getInitialBreedCount() {
    return Transformations.distinctUntilChanged(initialBreedCount);
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Integer&gt;} containing the ecosystem's
   * current number of breeds. This value reflects all breed extinctions that may have taken place
   * in the simulation so far.
   */
  public LiveData<Integer> getCurrentBreedCount() {
    return Transformations.distinctUntilChanged(currentBreedCount);
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;int[]&gt;} containing the ecosystem's
   * current breed population sizes.
   */
  public LiveData<int[]> getPopulations() {
    return populations;
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Long&gt;} containing the ecosystem's
   * current iteration count.
   */
  public LiveData<Long> getIterationCount() {
    return iterationCount;
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Boolean&gt;} containing a flag indicating
   * whether the ecosystem is in the absorbing state, in which all breeds but one are extinct.
   */
  public LiveData<Boolean> getAbsorbed() {
    return Transformations.distinctUntilChanged(absorbed);
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Boolean&gt;} containing a flag indicating
   * whether the simulation is running or paused.
   */
  public LiveData<Boolean> getRunning() {
    return Transformations.distinctUntilChanged(ecosystemRepository.getRunning());
  }

  /**
   * Returns a reference to a {@link LiveData LiveData&lt;Throwable&gt;} containing the exception
   * thrown (if any) by the most recent operation.
   */
  public LiveData<Throwable> getThrowable() {
    return throwable;
  }

  /**
   * If the lifecycle being observed by this viewmodel reaches the PAUSED state, the simulation is
   * paused.
   *
   * @param owner
   * @noinspection JavadocDeclaration
   */
  @Override
  public void onPause(@NonNull LifecycleOwner owner) {
    pause();
    DefaultLifecycleObserver.super.onPause(owner);
  }

  /**
   * If the lifecycle being observed by this viewmodel reaches the STOPPED state, subscribers are
   * removed from any in-progress asynchronous operations.
   *
   * @param owner
   * @noinspection JavadocDeclaration
   */
  @Override
  public void onStop(@NonNull LifecycleOwner owner) {
    pending.clear();
    DefaultLifecycleObserver.super.onStop(owner);
  }

  private void execute(Completable task, Action action) {
    throwable.postValue(null);
    task.subscribe(action, this::postThrowable, pending);
  }

  private <T> void execute(Single<T> task, Consumer<? super T> consumer) {
    throwable.postValue(null);
    task.subscribe(consumer, this::postThrowable, pending);
  }

  private <T> void execute(Observable<T> task, Consumer<? super T> consumer, Action action) {
    throwable.postValue(null);
    task.subscribe(consumer, this::postThrowable, action, pending);
  }

  private void postThrowable(Throwable throwable) {
    Log.e(getClass().getSimpleName(), throwable.getMessage(), throwable);
    this.throwable.postValue(throwable);
  }

}
