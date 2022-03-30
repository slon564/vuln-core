package api

import controllers.APIController
import io.ktor.application.*
import kotlin.properties.Delegates

abstract class ServerController : APIController {
    var call: ApplicationCall by Delegates.notNull()
}
