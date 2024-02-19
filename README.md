# Integration for molecule along with Android Architecure Components ViewModel

[![Maven Central](https://img.shields.io/maven-central/v/com.stylianosgakis.molecule.aacvm/molecule-aacvm)](https://repo1.maven.org/maven2/com/stylianosgakis/molecule/aacvm/)

This is a library that was inspired from [this discussion in the molecule repo](https://github.com/cashapp/molecule/pull/274) about how to properly use molecule with Android's ViewModel.
The code was something we have been using over at [the Hedvig App](https://github.com/HedvigInsurance/android) for quite some time which has worked well for us

The core concept is that you use Molecule for your presenter, with one extra step, a `lastState` parameter coming in to your presenter's `present` function.

That `lastState` will initially be the `initialState` that was passed to the `MoleculeViewModel`.
But importantly, if the a ViewModel which is still alive in-memory (for example was an entry in the backstack that you've come back to) starts having a new consumer again, since `SharingStarted.WhileSubscribed(5.seconds)` is used by default, the `moleculeFlow` will start emitting new values again, but this time the `lastState` will be exactly what the last emission was.
This helps with scenarios where you come back into a view which was on the backstack, to immediatelly have the last state available to you, as opposed to starting from a Loading state and having to fetch everything from scratch, which often results in a more poor user experience.

### Sample usage for a feature named "Dogs":

State, Event and Data
```kotlin
sealed interface DogsEvent {
  data object ReloadData : DogsEvent
}

sealed interface DogsUiState {
  data object Loading : DogsUiState
  data object Error : DogsUiState
  data class Success(val dogs: List<Dog>) : DogsUiState
}

interface DogsRepository {
  suspend fun getDogs(): Either<SomeError, List<Dog>>
}

data class Dog(
  val name: String,
  val age: Int,
)
```

ViewModel 
```kotlin
// ViewModel completely delegates to the MoleculePresenter, but we do need to pass in an initial state which shows when the ViewModel first is constructed
internal class DogsViewModel(
  dogsRepository: DogsRepository,
) : MoleculeViewModel<DogsEvent, DogsUiState>(
    initialState = DogsUiState.Loading,
    presenter = DogsPresenter(dogsRepository),
  )
```

Presenter
```kotlin
internal class DogsPresenter(
  private val dogsRepository: DogsRepository,
) : MoleculePresenter<DogsEvent, DogsUiState> {
  @Composable
  override fun MoleculePresenterScope<DogsEvent>.present(lastState: DogsUiState): DogsUiState {
    var loadIteration by remember { mutableIntStateOf(0) }
    // We need to make sure that we use our `lastState` object in our presenter in order to have the last known state immediatelly available to the user
    var currentState by remember { mutableStateOf(lastState) }

    // We get a convenience `CollectEvents` function where the lambda has the signature of `block: CoroutineScope.(Event) -> Unit`.
    // This allows us to react to the events in a non-blocking way, to not clog the event stream.
    // If we need to do some async action we can either asign the right parameters and use normal compose side effect APIs to handle our work, or use `launch {}` and do something else instead.
    CollectEvents { event: DogsEvent ->
      when (event) {
        DogsEvent.ReloadData -> loadIteration++
      }
    }

    LaunchedEffect(loadIteration) {
      // In the scenario where we come back to this screen and we have had a successful list from `lastState`, we can
      //  simply notify the UI that we are still refreshing the data, but we still keep the last known state available
      //  to the user.
      if (currentState is DogsUiState.Error) {
        currentState = DogsUiState.Loading
      } else if (currentState is DogsUiState.Success) {
        currentState = (currentState as DogsUiState.Success).copy(isRefreshing = true)
      }
      // If we do not need to refresh the data at all if we already had a Success state before, we could instead write:
      //  ```
      //  if (currentState is DogsUiState.Error) {
      //    currentState = DogsUiState.Loading
      //  } else if (currentState is DogsUiState.Success) {
      //    return@LaunchedEffect
      //  }
      //  ```

      // Or if we still want to be able to refresh the data even if we had a success state before, but we only do not
      //  want to automatically fetch again as soon as the presenter has come back online from being in the backstack
      //  we could write something like this:
      //  ```
      //  if (currentState is DogsUiState.Error) {
      //    currentState = DogsUiState.Loading
      //  } else if (currentState is DogsUiState.Success && loadIteration == 0) {
      //    return@LaunchedEffect
      //  }
      //  ```

      dogsRepository.getDogs().fold(
        ifLeft = { currentState = DogsUiState.Error },
        ifRight = { dogList -> currentState = DogsUiState.Success(dogs = dogList, isRefreshing = false) },
      )
    }
    return currentState
  }
}
```

More real life usages can be found [here](https://github.com/search?q=repo%3AHedvigInsurance%2Fandroid+%3A+MoleculePresenter%3C&type=code)
