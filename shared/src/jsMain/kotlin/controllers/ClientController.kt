package controllers

import api.Transport
import kotlin.coroutines.CoroutineContext

abstract class ClientController(
    val coroutineContext: CoroutineContext
) : APIController {
    protected val transport = Transport(coroutineContext)
}