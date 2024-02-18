package com.stylianosgakis.molecule.aacvm

import androidx.compose.runtime.Composable

public fun interface MoleculePresenter<Event, State> {
  @Composable
  public fun MoleculePresenterScope<Event>.present(lastState: State): State
}
