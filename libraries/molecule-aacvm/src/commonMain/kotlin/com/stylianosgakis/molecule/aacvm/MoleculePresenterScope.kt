package com.stylianosgakis.molecule.aacvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

public class MoleculePresenterScope<Event>(
  private val events: Flow<Event>,
) {
  @Composable
  public fun CollectEvent(block: CoroutineScope.(Event) -> Unit) {
    LaunchedEffect(events) {
      events.collect { events: Event ->
        block(events)
      }
    }
  }
}
